package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AppRepository(
    private val db: AppDatabase,
    private val context: Context
) {
    private val transactionDao = db.transactionDao()
    private val activityDao = db.activityDao()
    private val preferenceDao = db.preferenceDao()
    private val chatMessageDao = db.chatMessageDao()
    private val customCategoryDao = db.customCategoryDao()
    private val recurringTransactionDao = db.recurringTransactionDao()
    private val dynamicAttributeDao = db.dynamicAttributeDao()

    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allActivities: Flow<List<ActivityEntity>> = activityDao.getAllActivities()
    val allPreferences: Flow<List<PreferenceEntity>> = preferenceDao.getAllPreferences()
    val chatHistory: Flow<List<ChatMessageEntity>> = chatMessageDao.getChatHistory()
    val allCustomCategories: Flow<List<CustomCategoryEntity>> = customCategoryDao.getAllCustomCategories()
    val allRecurringTransactions: Flow<List<RecurringTransactionEntity>> = recurringTransactionDao.getAllRecurringTransactions()
    val allDynamicAttributes: Flow<List<DynamicAttributeEntity>> = dynamicAttributeDao.getAllDynamicAttributes()

    // Custom categories
    suspend fun insertCustomCategory(name: String, parentCategory: String? = null) {
        customCategoryDao.insertCustomCategory(CustomCategoryEntity(name = name, parentCategory = parentCategory))
    }

    suspend fun deleteCustomCategory(id: Int) {
        customCategoryDao.deleteCustomCategory(id)
    }

    // Recurring transactions
    suspend fun insertRecurringTransaction(recurring: RecurringTransactionEntity) {
        recurringTransactionDao.insertRecurringTransaction(recurring)
    }

    suspend fun deleteRecurringTransaction(id: Int) {
        recurringTransactionDao.deleteRecurringTransaction(id)
    }

    // Dynamic attributes
    suspend fun insertDynamicAttribute(name: String) {
        dynamicAttributeDao.insertDynamicAttribute(DynamicAttributeEntity(name = name))
    }

    suspend fun deleteDynamicAttribute(name: String) {
        dynamicAttributeDao.deleteDynamicAttribute(name)
    }

    val isFirebaseAvailable: Boolean
        get() = try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            false
        }

    val firestore: FirebaseFirestore?
        get() = if (isFirebaseAvailable) {
            try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }
        } else null

    val auth: FirebaseAuth?
        get() = if (isFirebaseAvailable) {
            try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
        } else null

    // Transactions
    suspend fun insertTransaction(
        category: String,
        merchant: String,
        amount: Double,
        paymentMethod: String,
        timestamp: Long,
        note: String
    ) {
        val entity = TransactionEntity(
            category = category,
            merchant = merchant,
            amount = amount,
            paymentMethod = paymentMethod,
            timestamp = timestamp,
            note = note,
            synced = false
        )
        transactionDao.insertTransaction(entity)
        syncWithCloud()
    }

    suspend fun updateTransaction(entity: TransactionEntity) {
        transactionDao.insertTransaction(entity.copy(synced = false))
        syncWithCloud()
    }

    suspend fun deleteTransaction(id: Int) {
        transactionDao.deleteTransaction(id)
        // Sync delete with firestore if online
        try {
            val currentUser = auth?.currentUser
            val fs = firestore
            if (currentUser != null && fs != null) {
                fs.collection("users").document(currentUser.uid)
                    .collection("transactions").document(id.toString())
                    .delete()
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Failed to delete from Firestore: ${e.message}")
        }
    }

    // Activities
    suspend fun insertActivity(
        description: String,
        timestamp: Long,
        context: String,
        type: String,
        durationMinutes: Int
    ) {
        val entity = ActivityEntity(
            description = description,
            timestamp = timestamp,
            context = context,
            type = type,
            durationMinutes = durationMinutes,
            synced = false
        )
        activityDao.insertActivity(entity)
        syncWithCloud()
    }

    suspend fun updateActivity(entity: ActivityEntity) {
        activityDao.insertActivity(entity.copy(synced = false))
        syncWithCloud()
    }

    suspend fun deleteActivity(id: Int) {
        activityDao.deleteActivity(id)
        // Sync delete with firestore if online
        try {
            val currentUser = auth?.currentUser
            val fs = firestore
            if (currentUser != null && fs != null) {
                fs.collection("users").document(currentUser.uid)
                    .collection("activities").document(id.toString())
                    .delete()
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Failed to delete from Firestore: ${e.message}")
        }
    }

    // Preferences / Memory
    suspend fun getPreference(key: String): String? {
        return preferenceDao.getPreferenceValue(key)
    }

    suspend fun savePreference(key: String, value: String) {
        preferenceDao.insertPreference(PreferenceEntity(key, value))
    }

    // Chat History
    suspend fun addChatMessage(role: String, message: String) {
        chatMessageDao.insertMessage(ChatMessageEntity(role = role, message = message))
    }

    suspend fun clearChatHistory() {
        chatMessageDao.clearHistory()
    }

    suspend fun getChatHistorySnapshot(): List<ChatMessageEntity> {
        return chatMessageDao.getChatHistorySnapshot()
    }

    // Synchronize Room data to Firebase Firestore (Offline-First Architecture)
    suspend fun syncWithCloud() = withContext(Dispatchers.IO) {
        val currentUser = auth?.currentUser ?: return@withContext
        val fs = firestore ?: return@withContext
        val userId = currentUser.uid

        try {
            // 1. Sync unsynced transactions
            val unsyncedTx = transactionDao.getUnsyncedTransactions()
            for (tx in unsyncedTx) {
                val data = mapOf(
                    "userId" to userId,
                    "category" to tx.category,
                    "merchant" to tx.merchant,
                    "amount" to tx.amount,
                    "paymentMethod" to tx.paymentMethod,
                    "timestamp" to tx.timestamp,
                    "note" to tx.note
                )
                fs.collection("users").document(userId)
                    .collection("transactions").document(tx.id.toString())
                    .set(data)
                
                transactionDao.markTransactionSynced(tx.id)
            }

            // 2. Sync unsynced activities
            val unsyncedAct = activityDao.getUnsyncedActivities()
            for (act in unsyncedAct) {
                val data = mapOf(
                    "userId" to userId,
                    "description" to act.description,
                    "timestamp" to act.timestamp,
                    "context" to act.context,
                    "type" to act.type,
                    "durationMinutes" to act.durationMinutes
                )
                fs.collection("users").document(userId)
                    .collection("activities").document(act.id.toString())
                    .set(data)
                
                activityDao.markActivitySynced(act.id)
            }
            Log.d("AppRepository", "Sync successful!")
        } catch (e: Exception) {
            Log.e("AppRepository", "Sync failed: ${e.message}")
        }
    }
}
