package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// 1. Transaction Entity (Finance)
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String,
    val merchant: String,
    val amount: Double,
    val paymentMethod: String,
    val timestamp: Long,
    val note: String,
    val synced: Boolean = false
)

// 2. Activity Entity (Calendar/Timeline)
@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val description: String,
    val timestamp: Long,
    val context: String, // mood, special info
    val type: String, // appointment, task, meeting
    val durationMinutes: Int = 30,
    val synced: Boolean = false
)

// 3. AI Memories & Long-term Preferences
@Entity(tableName = "preferences")
data class PreferenceEntity(
    @PrimaryKey val key: String,
    val value: String
)

// 4. Chat Messages (Managed weekly, cleared once a week)
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val role: String, // "user" or "model" (for multi-bubble, we store them as separate rows)
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

// DAOs
@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Int)

    @Query("SELECT * FROM transactions WHERE synced = 0")
    suspend fun getUnsyncedTransactions(): List<TransactionEntity>

    @Query("UPDATE transactions SET synced = 1 WHERE id = :id")
    suspend fun markTransactionSynced(id: Int)
}

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities ORDER BY timestamp DESC")
    fun getAllActivities(): Flow<List<ActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity)

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteActivity(id: Int)

    @Query("SELECT * FROM activities WHERE synced = 0")
    suspend fun getUnsyncedActivities(): List<ActivityEntity>

    @Query("UPDATE activities SET synced = 1 WHERE id = :id")
    suspend fun markActivitySynced(id: Int)
}

@Dao
interface PreferenceDao {
    @Query("SELECT * FROM preferences")
    fun getAllPreferences(): Flow<List<PreferenceEntity>>

    @Query("SELECT value FROM preferences WHERE `key` = :key LIMIT 1")
    suspend fun getPreferenceValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreference(preference: PreferenceEntity)

    @Query("DELETE FROM preferences WHERE `key` = :key")
    suspend fun deletePreference(key: String)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getChatHistory(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages")
    suspend fun clearHistory()

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    suspend fun getChatHistorySnapshot(): List<ChatMessageEntity>
}

// 5. Custom Financial Categories and Subcategories
@Entity(tableName = "custom_categories")
data class CustomCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val parentCategory: String? = null // For subcategories, e.g. "Groceries" under "Food/Beverage"
)

// 6. Recurring Transactions
@Entity(tableName = "recurring_transactions")
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val category: String,
    val merchant: String,
    val paymentMethod: String,
    val note: String,
    val frequency: String, // "Daily", "Weekly", "Monthly", "Yearly"
    val startDate: Long,
    val endDate: Long,
    val lastLoggedDate: Long
)

// 7. Dynamic Attributes identified from the AI conversation
@Entity(tableName = "dynamic_attributes")
data class DynamicAttributeEntity(
    @PrimaryKey val name: String
)

// DAOs
@Dao
interface CustomCategoryDao {
    @Query("SELECT * FROM custom_categories ORDER BY name ASC")
    fun getAllCustomCategories(): Flow<List<CustomCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomCategory(category: CustomCategoryEntity)

    @Query("DELETE FROM custom_categories WHERE id = :id")
    suspend fun deleteCustomCategory(id: Int)
}

@Dao
interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions ORDER BY startDate DESC")
    fun getAllRecurringTransactions(): Flow<List<RecurringTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringTransaction(recurring: RecurringTransactionEntity)

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun deleteRecurringTransaction(id: Int)
}

@Dao
interface DynamicAttributeDao {
    @Query("SELECT * FROM dynamic_attributes ORDER BY name ASC")
    fun getAllDynamicAttributes(): Flow<List<DynamicAttributeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDynamicAttribute(attribute: DynamicAttributeEntity)

    @Query("DELETE FROM dynamic_attributes WHERE name = :name")
    suspend fun deleteDynamicAttribute(name: String)
}

// Database holder
@Database(
    entities = [
        TransactionEntity::class,
        ActivityEntity::class,
        PreferenceEntity::class,
        ChatMessageEntity::class,
        CustomCategoryEntity::class,
        RecurringTransactionEntity::class,
        DynamicAttributeEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun activityDao(): ActivityDao
    abstract fun preferenceDao(): PreferenceDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun customCategoryDao(): CustomCategoryDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun dynamicAttributeDao(): DynamicAttributeDao
}
