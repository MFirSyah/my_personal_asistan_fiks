package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.TransactionEntity
import com.example.data.ActivityEntity
import com.example.data.PreferenceEntity
import com.example.data.ChatMessageEntity
import com.example.data.CustomCategoryEntity
import com.example.data.RecurringTransactionEntity
import com.example.data.DynamicAttributeEntity
import com.example.network.GeminiService
import com.example.network.GeminiParserResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Navigation tabs
enum class AppTab {
    FINANCE,
    CALENDAR,
    CHAT,
    ANALYTICS,
    PROFILE
}

// User Profile state
data class AuthState(
    val isLoggedIn: Boolean = false,
    val email: String? = null,
    val displayName: String? = null,
    val isSandboxMode: Boolean = true
)

class FinanceActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "fin_act_tracker_db"
    ).fallbackToDestructiveMigration().build()

    val repository = AppRepository(db, application)

    // Navigation State
    private val _currentTab = MutableStateFlow(AppTab.CHAT)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // Auth State
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // UI Input fields
    private val _chatInput = MutableStateFlow("")
    val chatInput = _chatInput.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Slang & Persona settings
    private val _personaSlang = MutableStateFlow("Friendly Professional & Supportive")
    val personaSlang = _personaSlang.asStateFlow()

    private val _memoryText = MutableStateFlow("")
    val memoryText = _memoryText.asStateFlow()

    // TTS voice reply toggle
    private val _isTtsEnabled = MutableStateFlow(false)
    val isTtsEnabled = _isTtsEnabled.asStateFlow()

    // Search Grounding toggle
    private val _isSearchGroundingEnabled = MutableStateFlow(false)
    val isSearchGroundingEnabled = _isSearchGroundingEnabled.asStateFlow()

    // App Language preference: "id" (Indonesian, default) or "en" (English)
    private val _appLanguage = MutableStateFlow("id")
    val appLanguage = _appLanguage.asStateFlow()

    private val _customApiKey = MutableStateFlow("")
    val customApiKey = _customApiKey.asStateFlow()

    private val _onboardingCompleted = MutableStateFlow(false)
    val onboardingCompleted = _onboardingCompleted.asStateFlow()

    private val _userName = MutableStateFlow("Owner")
    val userName = _userName.asStateFlow()

    private val _aiName = MutableStateFlow("FinAct AI")
    val aiName = _aiName.asStateFlow()

    // Media Player
    private var mediaPlayer: MediaPlayer? = null

    // Room Observed Data
    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activities: StateFlow<List<ActivityEntity>> = repository.allActivities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessageEntity>> = repository.chatHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customCategories: StateFlow<List<CustomCategoryEntity>> = repository.allCustomCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringTransactions: StateFlow<List<RecurringTransactionEntity>> = repository.allRecurringTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dynamicAttributes: StateFlow<List<DynamicAttributeEntity>> = repository.allDynamicAttributes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Category Management
    fun addCustomCategory(name: String, parentCategory: String? = null) {
        viewModelScope.launch {
            repository.insertCustomCategory(name, parentCategory)
        }
    }

    fun deleteCustomCategory(id: Int) {
        viewModelScope.launch {
            repository.deleteCustomCategory(id)
        }
    }

    // Recurring Transaction Management
    fun addRecurringTransaction(
        amount: Double,
        category: String,
        merchant: String,
        paymentMethod: String,
        note: String,
        frequency: String,
        startDate: Long,
        endDate: Long
    ) {
        viewModelScope.launch {
            repository.insertRecurringTransaction(
                RecurringTransactionEntity(
                    amount = amount,
                    category = category,
                    merchant = merchant,
                    paymentMethod = paymentMethod,
                    note = note,
                    frequency = frequency,
                    startDate = startDate,
                    endDate = endDate,
                    lastLoggedDate = 0L
                )
            )
        }
    }

    fun deleteRecurringTransaction(id: Int) {
        viewModelScope.launch {
            repository.deleteRecurringTransaction(id)
        }
    }

    // Dynamic Attribute Management
    fun addDynamicAttribute(name: String) {
        viewModelScope.launch {
            repository.insertDynamicAttribute(name)
        }
    }

    fun deleteDynamicAttribute(name: String) {
        viewModelScope.launch {
            repository.deleteDynamicAttribute(name)
        }
    }

    private fun processRecurringTransactions(list: List<RecurringTransactionEntity>) {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            for (rec in list) {
                if (now < rec.startDate) continue
                
                var tempTime = if (rec.lastLoggedDate == 0L) rec.startDate else rec.lastLoggedDate
                val calendar = Calendar.getInstance().apply { timeInMillis = tempTime }
                
                var loggedAny = false
                while (true) {
                    when (rec.frequency.lowercase(Locale.US)) {
                        "daily" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                        "weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                        "monthly" -> calendar.add(Calendar.MONTH, 1)
                        "yearly" -> calendar.add(Calendar.YEAR, 1)
                        else -> break
                    }
                    
                    val nextTime = calendar.timeInMillis
                    if (nextTime <= now && nextTime <= rec.endDate) {
                        repository.insertTransaction(
                            category = rec.category,
                            merchant = rec.merchant,
                            amount = rec.amount,
                            paymentMethod = rec.paymentMethod,
                            timestamp = nextTime,
                            note = "${rec.note} (Recurring: ${rec.frequency})"
                        )
                        tempTime = nextTime
                        loggedAny = true
                    } else {
                        break
                    }
                }
                if (loggedAny) {
                    repository.insertRecurringTransaction(rec.copy(lastLoggedDate = tempTime))
                }
            }
        }
    }

    // AI generated dynamic insights state
    private val _aiInsights = MutableStateFlow("Enter some transactions or log activities in chat. Your advanced lifestyle and finance dashboard will automatically build itself here using Gemini AI!")
    val aiInsights = _aiInsights.asStateFlow()

    // Morning Briefing state
    private val _morningBriefing = MutableStateFlow<String?>(null)
    val morningBriefing = _morningBriefing.asStateFlow()

    init {
        // Load stored settings from Room preferences on startup
        viewModelScope.launch {
            _appLanguage.value = repository.getPreference("app_language") ?: "id"
            _personaSlang.value = repository.getPreference("persona_slang") ?: "Friendly Jakarta Slang"
            _memoryText.value = repository.getPreference("user_memories") ?: "Active side-hustler, loves coffee and Jakarta."
            _isTtsEnabled.value = (repository.getPreference("tts_enabled") ?: "false").toBoolean()
            _isSearchGroundingEnabled.value = (repository.getPreference("search_grounding") ?: "false").toBoolean()
            _customApiKey.value = repository.getPreference("custom_gemini_api_key") ?: ""
            _onboardingCompleted.value = (repository.getPreference("onboarding_completed") ?: "false").toBoolean()
            _userName.value = repository.getPreference("user_name") ?: "Owner"
            _aiName.value = repository.getPreference("ai_name") ?: "FinAct AI"
            GeminiService.customApiKey = _customApiKey.value.ifBlank { null }

            // Initialize Auth status (checks Firebase Auth or defaults to local sandbox)
            checkFirebaseUser()
            
            // Check if there are no messages in the chat history, pre-populate with a warm welcome!
            viewModelScope.launch {
                val snapshot = repository.getChatHistorySnapshot()
                if (snapshot.isEmpty()) {
                    if (_appLanguage.value == "id") {
                        repository.addChatMessage("model", "Selamat datang di **FinAct AI**! 🌟")
                        repository.addChatMessage("model", "Saya adalah asisten finansial dan pelatih aktivitas harian bertenaga Gemini AI. Coba catat transaksi atau rencana Anda dengan bahasa santai:\n\n*\"Baru saja habis Rp 18.000 buat beli kopi di Kopi Kenangan pakai Cash\"*\n*\"Besok jam 09:00 pagi saya ada janji temu sama klien di Tomoro Cafe\"*")
                    } else {
                        repository.addChatMessage("model", "Welcome to **FinAct AI**! 🌟")
                        repository.addChatMessage("model", "I'm your Gemini-powered personal finance and activity coach. Try logging something naturally:\n\n*\"Just spent Rp 18,000 on a coffee at Kopi Kenangan using Cash\"*\n*\"Tomorrow at 09:00 AM I have an appointment with a client at Tomoro Cafe\"*")
                    }
                }
            }

            // Observe & process recurring transactions
            viewModelScope.launch {
                repository.allRecurringTransactions.collect { list ->
                    processRecurringTransactions(list)
                }
            }

            // Generate initial insights & morning briefing based on any existing data
            refreshMorningBriefing()
        }
    }

    private suspend fun checkFirebaseUser() {
        val auth = repository.auth
        if (auth != null && auth.currentUser != null) {
            val user = auth.currentUser!!
            _authState.value = AuthState(
                isLoggedIn = true,
                email = user.email,
                displayName = user.displayName ?: user.email?.substringBefore("@"),
                isSandboxMode = false
            )
            // Trigger background cloud sync on login
            viewModelScope.launch { repository.syncWithCloud() }
        } else {
            // Local user sandbox (default experience)
            val localLoggedIn = repository.getPreference("local_logged_in") == "true"
            if (localLoggedIn) {
                val localEmail = repository.getPreference("local_email") ?: "sandbox@finactai.local"
                _authState.value = AuthState(
                    isLoggedIn = true,
                    email = localEmail,
                    displayName = _userName.value,
                    isSandboxMode = true
                )
            } else {
                _authState.value = AuthState(
                    isLoggedIn = false,
                    email = null,
                    displayName = null,
                    isSandboxMode = true
                )
            }
        }
    }

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun setChatInput(text: String) {
        _chatInput.value = text
    }

    fun setTtsEnabled(enabled: Boolean) {
        _isTtsEnabled.value = enabled
        viewModelScope.launch { repository.savePreference("tts_enabled", enabled.toString()) }
    }

    fun setSearchGroundingEnabled(enabled: Boolean) {
        _isSearchGroundingEnabled.value = enabled
        viewModelScope.launch { repository.savePreference("search_grounding", enabled.toString()) }
    }

    fun setAppLanguage(lang: String) {
        _appLanguage.value = lang
        viewModelScope.launch { repository.savePreference("app_language", lang) }
    }

    fun updatePersonaSlang(slang: String) {
        _personaSlang.value = slang
        viewModelScope.launch { repository.savePreference("persona_slang", slang) }
    }

    fun updateMemoryText(text: String) {
        _memoryText.value = text
        viewModelScope.launch { repository.savePreference("user_memories", text) }
    }

    fun updateCustomApiKey(key: String) {
        _customApiKey.value = key
        GeminiService.customApiKey = key.ifBlank { null }
        viewModelScope.launch { repository.savePreference("custom_gemini_api_key", key) }
    }

    fun completeOnboarding() {
        _onboardingCompleted.value = true
        viewModelScope.launch { repository.savePreference("onboarding_completed", "true") }
    }

    fun updateUserName(name: String) {
        _userName.value = name
        viewModelScope.launch { repository.savePreference("user_name", name) }
    }

    fun updateAiName(name: String) {
        _aiName.value = name
        viewModelScope.launch { repository.savePreference("ai_name", name) }
    }

    // AUTH ACTIONS
    fun handleRegister(email: String, pss: String) {
        val auth = repository.auth
        if (auth != null) {
            _isLoading.value = true
            auth.createUserWithEmailAndPassword(email, pss)
                .addOnCompleteListener { task ->
                    _isLoading.value = false
                    if (task.isSuccessful) {
                        viewModelScope.launch { checkFirebaseUser() }
                    } else {
                        Log.e("Auth", "Registration failed: ${task.exception?.message}")
                    }
                }
        } else {
            // Local user registration in sandbox mode
            _isLoading.value = true
            viewModelScope.launch {
                repository.savePreference("local_email", email)
                repository.savePreference("local_password", pss)
                repository.savePreference("local_logged_in", "true")
                _authState.value = AuthState(
                    isLoggedIn = true,
                    email = email,
                    displayName = _userName.value,
                    isSandboxMode = true
                )
                _isLoading.value = false
            }
        }
    }

    fun handleLogin(email: String, pss: String) {
        val auth = repository.auth
        if (auth != null) {
            _isLoading.value = true
            auth.signInWithEmailAndPassword(email, pss)
                .addOnCompleteListener { task ->
                    _isLoading.value = false
                    if (task.isSuccessful) {
                        viewModelScope.launch { checkFirebaseUser() }
                    } else {
                        Log.e("Auth", "Login failed: ${task.exception?.message}")
                    }
                }
        } else {
            // Local user login in sandbox mode
            _isLoading.value = true
            viewModelScope.launch {
                val localEmail = repository.getPreference("local_email") ?: ""
                val localPassword = repository.getPreference("local_password") ?: ""
                if (email == localEmail && pss == localPassword) {
                    repository.savePreference("local_logged_in", "true")
                    _authState.value = AuthState(
                        isLoggedIn = true,
                        email = email,
                        displayName = _userName.value,
                        isSandboxMode = true
                    )
                } else {
                    Log.e("Auth", "Local Login failed: credentials mismatch")
                }
                _isLoading.value = false
            }
        }
    }

    fun handleSandboxBypass() {
        _isLoading.value = true
        viewModelScope.launch {
            repository.savePreference("local_logged_in", "true")
            _authState.value = AuthState(
                isLoggedIn = true,
                email = "sandbox@finactai.local",
                displayName = _userName.value,
                isSandboxMode = true
            )
            _isLoading.value = false
        }
    }

    fun handleLogout() {
        repository.auth?.signOut()
        viewModelScope.launch {
            repository.savePreference("local_logged_in", "false")
            _authState.value = AuthState(
                isLoggedIn = false,
                email = null,
                displayName = null,
                isSandboxMode = true
            )
        }
    }

    // MANUAL DATABASE INSERTIONS & CORRECTIONS
    fun addManualTransaction(category: String, merchant: String, amount: Double, paymentMethod: String, date: Long, note: String) {
        viewModelScope.launch {
            repository.insertTransaction(category, merchant, amount, paymentMethod, date, note)
            generateAiInsights()
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            generateAiInsights()
        }
    }

    fun addManualActivity(description: String, date: Long, context: String, type: String, duration: Int) {
        viewModelScope.launch {
            repository.insertActivity(description, date, context, type, duration)
            generateAiInsights()
        }
    }

    fun deleteActivity(id: Int) {
        viewModelScope.launch {
            repository.deleteActivity(id)
            generateAiInsights()
        }
    }

    // CHAT CORE INTERACTION
    fun sendChatMessage() {
        val message = _chatInput.value.trim()
        if (message.isEmpty()) return

        _chatInput.value = ""
        _isLoading.value = true

        viewModelScope.launch {
            // 1. Record user's query in chat database
            repository.addChatMessage("user", message)

            // 2. Load the recent 10 turns of history to maintain context
            val currentHist = repository.getChatHistorySnapshot()
            val historyTurns = currentHist.takeLast(10).map { it.role to it.message }

            // 3. Call Gemini Parser
            val parserResponse = if (_isSearchGroundingEnabled.value) {
                // If grounding is enabled, first fetch search context
                val searchContext = GeminiService.runSearchGrounding(message)
                val fullQueryWithSearch = "$message\n\n(Context from Google Search: $searchContext)"
                GeminiService.parseAndReply(
                    history = historyTurns.dropLast(1), // remove last turn as we send it customized
                    userMessage = fullQueryWithSearch,
                    currentSlangPreference = _personaSlang.value,
                    userMemoryItems = _memoryText.value,
                    appLanguage = _appLanguage.value,
                    userName = _userName.value,
                    aiName = _aiName.value
                )
            } else {
                GeminiService.parseAndReply(
                    history = historyTurns.dropLast(1),
                    userMessage = message,
                    currentSlangPreference = _personaSlang.value,
                    userMemoryItems = _memoryText.value,
                    appLanguage = _appLanguage.value,
                    userName = _userName.value,
                    aiName = _aiName.value
                )
            }

            // 4. Save model's response bubbles
            for (bubble in parserResponse.replyBubbles) {
                repository.addChatMessage("model", bubble)
            }

            // 5. Play Audio if TTS is active
            if (_isTtsEnabled.value && parserResponse.replyBubbles.isNotEmpty()) {
                val combinedText = parserResponse.replyBubbles.joinToString(". ")
                val audioFile = GeminiService.generateSpeech(combinedText, getApplication())
                if (audioFile != null) {
                    playAudio(audioFile)
                }
            }

            // 6. Insert Extracted Transactions / Activities automatically
            parserResponse.financeExtracted?.let { f ->
                val attributesString = f.customAttributes?.entries?.joinToString { "${it.key}: ${it.value}" }
                val formattedNote = if (!attributesString.isNullOrEmpty()) {
                    if (f.note.isEmpty()) "Attributes: $attributesString" else "${f.note} | $attributesString"
                } else {
                    f.note
                }

                repository.insertTransaction(
                    category = f.category,
                    merchant = f.merchant,
                    amount = f.amount,
                    paymentMethod = f.paymentMethod,
                    timestamp = System.currentTimeMillis(),
                    note = formattedNote
                )

                // Add newly discovered attributes dynamically to database
                f.customAttributes?.keys?.forEach { attrName ->
                    repository.insertDynamicAttribute(attrName)
                }
            }

            parserResponse.activityExtracted?.let { a ->
                // Parse optional time/date offset or default to current
                val dateMillis = System.currentTimeMillis() // simple default
                repository.insertActivity(
                    description = a.description,
                    timestamp = dateMillis,
                    context = a.context,
                    type = a.activityType,
                    durationMinutes = a.durationMinutes
                )
            }

            _isLoading.value = false
            
            // 7. Re-generate analytics models automatically after logs
            generateAiInsights()
            refreshMorningBriefing()
        }
    }

    // Weekly Purge of raw chat histories & memory compilation
    fun performWeeklyPurgeAndMemoryCompilation() {
        viewModelScope.launch {
            _isLoading.value = true
            val history = repository.getChatHistorySnapshot()
            if (history.isNotEmpty()) {
                val historyText = history.joinToString("\n") { "${it.role}: ${it.message}" }
                val prompt = """
                    You are a semantic memory compression engine.
                    Analyze this full week's chat transcripts between the user and the finance/activity tracker:
                    
                    $historyText
                    
                    Extract crucial long-term memory points, user goals, repeated corrections, and favorite habits.
                    Consolidate them into a short, concise list of key points to guide future personalization.
                    Return ONLY the consolidated points in plain text.
                """.trimIndent()

                val newMemories = GeminiService.generateStrategicAnalysis(prompt)
                val currentMemories = _memoryText.value
                val updatedMemories = "$currentMemories\n\n[Compiled Memory - ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}]:\n$newMemories"
                
                updateMemoryText(updatedMemories)
                repository.clearChatHistory()
                repository.addChatMessage("model", "🧹 **Weekly Chat History Purged Successfully!**")
                repository.addChatMessage("model", "I analyzed your weekly logs, compressed key behavioral patterns, and permanently added them to your memory profile in your Settings tab. Memory successfully consolidated!")
            }
            _isLoading.value = false
        }
    }

    // Play TTS speech audio file safely
    private fun playAudio(file: File) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "MediaPlayer playback failed: ${e.message}")
        }
    }

    // ADVANCED ANALYTICS INTERSECTIONS
    fun generateAiInsights() {
        viewModelScope.launch {
            val txList = transactions.value
            val actList = activities.value
            
            if (txList.isEmpty() && actList.isEmpty()) {
                _aiInsights.value = "Once you log transactions and calendar events, Gemini will analyze correlations such as **Cost of Habits**, **Mood vs. Impulsive Purchases**, and **Work ROI**."
                return@launch
            }

            val txStr = txList.joinToString("\n") { "Spent ${it.amount} at ${it.merchant} on ${it.category} (${it.note})" }
            val actStr = actList.joinToString("\n") { "Scheduled ${it.description} (${it.durationMinutes} mins) at context/mood: ${it.context}" }

            val langName = if (_appLanguage.value == "id") "Indonesian (Bahasa Indonesia)" else "English"
            val prompt = """
                You are a premium financial analytics engine. Analyze these two data logs:
                
                FINANCE LEDGER:
                $txStr
                
                ACTIVITY LOGS:
                $actStr
                
                Provide three specialized analyses in rich, modern formatting:
                1. COMBINED LIFESTYLE ANALYTICS:
                   - "Cost of Habits": Correlate time spent at places (like coffee shops, cafes) with cumulative expense.
                   - "Mood vs. Impulsive Buying": Correlate high-stress, late activities with impulsive purchases.
                   - "Work-Time ROI": Evaluate Side-Hustles, projects time vs revenue.
                2. STANDALONE INSIGHTS:
                   - Financial Deep Dive (portfolio strategy, snowball avalanche debt advice).
                   - Activity Peak Productivity Hours (circadian energy levels, Deep vs Shallow work ratios).
                3. FORWARD PLANNING STRATEGY:
                   - Future activity impacts (predicting cash flow spikes based on upcoming tasks/dates).
                   
                Be concise, direct, supportive, and write in highly formatted Markdown suitable for beautiful display.
                You MUST write the entire analysis response in $langName.
            """.trimIndent()

            _isLoading.value = true
            val response = GeminiService.generateStrategicAnalysis(prompt)
            _aiInsights.value = response
            _isLoading.value = false
        }
    }

    // Autonomous late-night Morning Briefing generator
    fun refreshMorningBriefing() {
        viewModelScope.launch {
            val txList = transactions.value
            val actList = activities.value
            val currentSlang = _personaSlang.value

            val langName = if (_appLanguage.value == "id") "Indonesian (Bahasa Indonesia)" else "English"
            val prompt = """
                Generate a refreshing, late-night synthetic **"Morning Briefing"** for the user waking up today.
                Use their current slang preference: $currentSlang.
                
                Here is their active tracking record:
                - Recent finances: ${txList.take(5).joinToString(", ") { "${it.merchant} (${it.amount})" }}
                - Scheduled activities: ${actList.take(5).joinToString(", ") { "${it.description} at ${it.context}" }}
                
                Structure it as:
                1. **Agenda Reminders**: Highlights of scheduled activities for today.
                2. **Budget Alerts**: Insights on remaining limits and cautions on past habits.
                3. **Strategic Encouragement**: A positive, custom productivity & financial tip to start their day.
                
                Keep it highly structured, visual, short, and styled with warm energy.
                You MUST write the entire briefing response in $langName.
            """.trimIndent()

            _isLoading.value = true
            val briefing = GeminiService.generateStrategicAnalysis(prompt)
            _morningBriefing.value = briefing
            _isLoading.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        db.close()
    }
}
