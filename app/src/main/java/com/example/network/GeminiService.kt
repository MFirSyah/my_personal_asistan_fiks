package com.example.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// AI parsing response envelope
data class GeminiParserResponse(
    val intent: String,
    val replyBubbles: List<String>,
    val financeExtracted: ExtractedFinance?,
    val activityExtracted: ExtractedActivity?
)

data class ExtractedFinance(
    val category: String,
    val merchant: String,
    val amount: Double,
    val paymentMethod: String,
    val note: String,
    val customAttributes: Map<String, String>? = null
)

data class ExtractedActivity(
    val description: String,
    val timeString: String?,
    val dateString: String?,
    val context: String, // mood or location
    val activityType: String, // appointment, task, meeting
    val durationMinutes: Int
)

object GeminiService {
    var customApiKey: String? = null

    private fun getActiveApiKey(): String {
        return if (!customApiKey.isNullOrBlank()) customApiKey!! else BuildConfig.GEMINI_API_KEY
    }

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // 1. Unified Intelligence Parser Chat call
    suspend fun parseAndReply(
        history: List<Pair<String, String>>, // role to message
        userMessage: String,
        currentSlangPreference: String = "friendly professional",
        userMemoryItems: String = "",
        appLanguage: String = "id",
        userName: String = "Owner",
        aiName: String = "FinAct AI"
    ): GeminiParserResponse = withContext(Dispatchers.IO) {
        val apiKey = getActiveApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext GeminiParserResponse(
                intent = "CHAT",
                replyBubbles = listOf(
                    "Hello! I am ready to help, but your Gemini API key is currently missing or placeholder.",
                    "Please open the Secrets panel in AI Studio and enter your real GEMINI_API_KEY to enable AI chat capabilities. Running in sandbox mode!"
                ),
                financeExtracted = null,
                activityExtracted = null
            )
        }

        val systemInstruction = """
            You are the unified AI intelligence core of a personal finance and activity tracker.
            The user can log financial transactions or scheduled activities simply by chatting in natural language.
            Your job is to parse the user's input, extract details if they log a transaction or schedule/track an activity, and reply.
            
            You MUST respond in JSON format conforming EXACTLY to this schema:
            {
              "intent": "FINANCE" | "ACTIVITY" | "STRATEGY" | "CHAT" | "PERSONA",
              "replyBubbles": ["Greeting/Intro bubble text", "Detail/Context breakdown bubble text", ...],
              "financeExtracted": null or {
                 "category": "Food/Beverage" | "Transportation" | "Education" | "Entertainment" | "Shopping" | "Salary" | "Other",
                 "merchant": "extracted name",
                 "amount": double_amount,
                 "paymentMethod": "Cash" | "Card" | "E-Wallet" | "Other",
                 "note": "brief note or summary",
                 "customAttributes": null or {
                    "tax": "extracted tax value if mentioned",
                    "tip": "extracted tip value if mentioned",
                    "location": "extracted location if mentioned",
                    "warranty": "extracted warranty if mentioned",
                    "any_custom_key": "extracted value"
                 }
              },
              "activityExtracted": null or {
                 "description": "extracted description",
                 "timeString": "HH:MM AM/PM or null",
                 "dateString": "today / tomorrow / yesterday / yyyy-mm-dd or null",
                 "context": "extracted mood/location/lecturer, e.g. Stres, Lelah, Tomoro Cafe",
                 "activityType": "appointment" | "task" | "meeting" | "habit" | "other",
                 "durationMinutes": integer_minutes
              }
            }

            Rules:
            1. Response format MUST contain "replyBubbles" as a JSON array of at least 2 distinct strings (bubbles). Avoid a single dense block of text.
            2. Adapt your tone and language slang to: $currentSlangPreference.
            3. Response language (replyBubbles text) MUST be in: ${if (appLanguage == "id") "Indonesian (Bahasa Indonesia)" else "English"}.
            4. Consider these long-term memory points of the user: $userMemoryItems.
            5. Keep transaction numbers parsed carefully (e.g. "18,000" = 18000.0, "250K" = 250000.0, "one hundred thousand" = 100000.0).
            6. Ensure any text responses are warm, modern, and highly styled.
            7. If the user mentions any additional transaction attributes (e.g. tax, tip, location, receipt, warranty), capture them as key-value pairs inside "customAttributes" in "financeExtracted".
            8. Your AI assistant name is: $aiName. Identify yourself as $aiName if asked or when introducing yourself.
            9. The user / owner's name is: $userName. Address them as $userName.
        """.trimIndent()

        // Build request body manually with JSONObject to prevent escaping issues
        val requestJson = JSONObject()
        
        // System instruction
        val sysInstructObj = JSONObject()
        val sysPartsArray = JSONArray().put(JSONObject().put("text", systemInstruction))
        sysInstructObj.put("parts", sysPartsArray)
        requestJson.put("systemInstruction", sysInstructObj)

        // Contents (including history)
        val contentsArray = JSONArray()
        for (turn in history) {
            val contentObj = JSONObject()
            contentObj.put("role", if (turn.first == "user") "user" else "model")
            val partsArray = JSONArray().put(JSONObject().put("text", turn.second))
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
        }
        // Current user message
        val currentTurnObj = JSONObject()
        currentTurnObj.put("role", "user")
        val currentPartsArray = JSONArray().put(JSONObject().put("text", userMessage))
        currentTurnObj.put("parts", currentPartsArray)
        contentsArray.put(currentTurnObj)
        
        requestJson.put("contents", contentsArray)

        // Force JSON response
        val genConfig = JSONObject()
        genConfig.put("responseMimeType", "application/json")
        requestJson.put("generationConfig", genConfig)

        val model = "gemini-2.0-flash"
        val requestUrl = "$BASE_URL$model:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(requestUrl)
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e("GeminiService", "API Error: ${response.code} - $bodyStr")
                    if (response.code == 429) {
                        val limitMessage = if (appLanguage == "id") {
                            "⚠️ **Batas Kecepatan Terlampaui (Error 429: Too Many Requests)**\n\nLayanan Gemini API bersama saat ini sedang sibuk karena terlalu banyak permintaan.\n\n**Solusi:**\nAnda dapat memasukkan API Key Gemini pribadi Anda secara gratis di **Profile & Settings** (Tab Akun) aplikasi ini untuk menghindari batas kecepatan dari pengguna lain."
                        } else {
                            "⚠️ **Rate Limit Exceeded (Error 429: Too Many Requests)**\n\nThe shared Gemini API key is currently experiencing heavy traffic.\n\n**Solution:**\nYou can enter your own personal Gemini API key for free in the **Profile & Settings** tab to enjoy uninterrupted service."
                        }
                        return@withContext GeminiParserResponse(
                            intent = "CHAT",
                            replyBubbles = listOf(limitMessage),
                            financeExtracted = null,
                            activityExtracted = null
                        )
                    }
                    return@withContext defaultErrorResponse("API error: ${response.code}")
                }
                
                val rootJson = JSONObject(bodyStr)
                val textCandidate = rootJson.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                // Parse extracted text
                val parsedJson = JSONObject(textCandidate)
                val intent = parsedJson.optString("intent", "CHAT")
                val replyBubblesArray = parsedJson.getJSONArray("replyBubbles")
                val replyBubbles = mutableListOf<String>()
                for (i in 0 until replyBubblesArray.length()) {
                    replyBubbles.add(replyBubblesArray.getString(i))
                }

                // Finance
                var financeExtracted: ExtractedFinance? = null
                if (parsedJson.has("financeExtracted") && !parsedJson.isNull("financeExtracted")) {
                    val fObj = parsedJson.getJSONObject("financeExtracted")
                    val attrsMap = mutableMapOf<String, String>()
                    if (fObj.has("customAttributes") && !fObj.isNull("customAttributes")) {
                        val attrsObj = fObj.getJSONObject("customAttributes")
                        val keys = attrsObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            attrsMap[key] = attrsObj.optString(key)
                        }
                    }
                    financeExtracted = ExtractedFinance(
                        category = fObj.optString("category", "Other"),
                        merchant = fObj.optString("merchant", "Unknown"),
                        amount = fObj.optDouble("amount", 0.0),
                        paymentMethod = fObj.optString("paymentMethod", "Other"),
                        note = fObj.optString("note", ""),
                        customAttributes = if (attrsMap.isNotEmpty()) attrsMap else null
                    )
                }

                // Activity
                var activityExtracted: ExtractedActivity? = null
                if (parsedJson.has("activityExtracted") && !parsedJson.isNull("activityExtracted")) {
                    val aObj = parsedJson.getJSONObject("activityExtracted")
                    activityExtracted = ExtractedActivity(
                        description = aObj.optString("description", ""),
                        timeString = if (aObj.isNull("timeString")) null else aObj.optString("timeString"),
                        dateString = if (aObj.isNull("dateString")) null else aObj.optString("dateString"),
                        context = aObj.optString("context", ""),
                        activityType = aObj.optString("activityType", "other"),
                        durationMinutes = aObj.optInt("durationMinutes", 30)
                    )
                }

                GeminiParserResponse(intent, replyBubbles, financeExtracted, activityExtracted)
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Network/Parsing failed: ${e.message}")
            defaultErrorResponse("Could not reach Gemini or parse answer. Standard mode: ${e.localizedMessage}")
        }
    }

    // 2. Search Grounding call
    suspend fun runSearchGrounding(query: String): String = withContext(Dispatchers.IO) {
        val apiKey = getActiveApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API key not configured. Cannot perform live Google Search."
        }

        val requestJson = JSONObject()
        val contentsArray = JSONArray().put(
            JSONObject().put("parts", JSONArray().put(JSONObject().put("text", query)))
        )
        requestJson.put("contents", contentsArray)

        // Add googleSearch tool
        val toolsArray = JSONArray().put(
            JSONObject().put("googleSearch", JSONObject())
        )
        requestJson.put("tools", toolsArray)

        val model = "gemini-2.0-flash"
        val requestUrl = "$BASE_URL$model:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(requestUrl)
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                val rootJson = JSONObject(bodyStr)
                rootJson.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
            }
        } catch (e: Exception) {
            "Google Search failed: ${e.message}"
        }
    }

    // 3. Strategy / Analytics Generator (gemini-3.1-pro-preview)
    suspend fun generateStrategicAnalysis(
        analysisPrompt: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getActiveApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API key not configured. Standard mock analytics are active."
        }

        val requestJson = JSONObject()
        val contentsArray = JSONArray().put(
            JSONObject().put("parts", JSONArray().put(JSONObject().put("text", analysisPrompt)))
        )
        requestJson.put("contents", contentsArray)

        val model = "gemini-2.0-flash"
        val requestUrl = "$BASE_URL$model:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(requestUrl)
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                val rootJson = JSONObject(bodyStr)
                rootJson.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
            }
        } catch (e: Exception) {
            "Analysis Generation Failed: ${e.message}"
        }
    }

    // 4. Image Generation (gemini-3.1-flash-image-preview)
    suspend fun generateImage(
        prompt: String,
        aspectRatio: String // 1:1, 16:9, etc.
    ): Bitmap? = withContext(Dispatchers.IO) {
        val apiKey = getActiveApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext null
        }

        val requestJson = JSONObject()
        val contentsArray = JSONArray().put(
            JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
        )
        requestJson.put("contents", contentsArray)

        val genConfig = JSONObject()
        val imgConfig = JSONObject()
        imgConfig.put("aspectRatio", aspectRatio)
        imgConfig.put("imageSize", "1K")
        genConfig.put("imageConfig", imgConfig)
        
        val modalities = JSONArray().put("TEXT").put("IMAGE")
        genConfig.put("responseModalities", modalities)
        
        requestJson.put("generationConfig", genConfig)

        val model = "gemini-2.0-flash"
        val requestUrl = "$BASE_URL$model:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(requestUrl)
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                val rootJson = JSONObject(bodyStr)
                val parts = rootJson.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")

                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    if (part.has("inlineData")) {
                        val inlineData = part.getJSONObject("inlineData")
                        val mimeType = inlineData.getString("mimeType")
                        if (mimeType.startsWith("image/")) {
                            val base64Data = inlineData.getString("data")
                            val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
                            return@withContext BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        }
                    }
                }
                null
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Image generation failed: ${e.message}")
            null
        }
    }

    // 5. TTS - Text to Speech (gemini-3.1-flash-tts-preview)
    suspend fun generateSpeech(
        textToSpeak: String,
        context: android.content.Context
    ): File? = withContext(Dispatchers.IO) {
        val apiKey = getActiveApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext null
        }

        val requestJson = JSONObject()
        val contentsArray = JSONArray().put(
            JSONObject().put("parts", JSONArray().put(JSONObject().put("text", textToSpeak)))
        )
        requestJson.put("contents", contentsArray)

        val genConfig = JSONObject()
        val modalities = JSONArray().put("AUDIO")
        genConfig.put("responseModalities", modalities)

        val speechConfig = JSONObject()
        val voiceConfig = JSONObject()
        val prebuiltVoiceConfig = JSONObject()
        prebuiltVoiceConfig.put("voiceName", "Kore") // Warm vocal profile
        voiceConfig.put("prebuiltVoiceConfig", prebuiltVoiceConfig)
        speechConfig.put("voiceConfig", voiceConfig)
        genConfig.put("speechConfig", speechConfig)

        requestJson.put("generationConfig", genConfig)

        val model = "gemini-2.0-flash"
        val requestUrl = "$BASE_URL$model:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(requestUrl)
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                val rootJson = JSONObject(bodyStr)
                val parts = rootJson.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")

                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    if (part.has("inlineData")) {
                        val inlineData = part.getJSONObject("inlineData")
                        val mimeType = inlineData.getString("mimeType")
                        if (mimeType.startsWith("audio/")) {
                            val base64Data = inlineData.getString("data")
                            val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)

                            // Save to cache dir and return file
                            val outFile = File(context.cacheDir, "speech_reply.mp3")
                            FileOutputStream(outFile).use { fos ->
                                fos.write(audioBytes)
                            }
                            return@withContext outFile
                        }
                    }
                }
                null
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "TTS generation failed: ${e.message}")
            null
        }
    }

    private fun defaultErrorResponse(errorMessage: String): GeminiParserResponse {
        return GeminiParserResponse(
            intent = "CHAT",
            replyBubbles = listOf(
                "I had trouble parsing your request.",
                errorMessage
            ),
            financeExtracted = null,
            activityExtracted = null
        )
    }
}
