package site.giboworks.budgettracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import site.giboworks.budgettracker.data.local.entity.TransactionEntity

/**
 * Data Access Object for Transaction operations.
 * Provides reactive streams via Flow for real-time updates.
 */
@Dao
interface TransactionDao {
    
    // ========== INSERT ==========
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)
    
    // ========== UPDATE ==========
    
    @Update
    suspend fun update(transaction: TransactionEntity)
    
    // ========== DELETE ==========
    
    @Delete
    suspend fun delete(transaction: TransactionEntity)
    
    @Query("UPDATE transactions SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun hardDelete(id: String)
    
    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
    
    // ========== QUERIES - Single ==========
    
    @Query("SELECT * FROM transactions WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: String): TransactionEntity?
    
    @Query("SELECT * FROM transactions WHERE id = :id AND isDeleted = 0")
    fun observeById(id: String): Flow<TransactionEntity?>
    
    // ========== QUERIES - Lists ==========
    
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>
    
    @Query("""
        SELECT * FROM transactions 
        WHERE isDeleted = 0 
        AND timestamp >= :startTimestamp 
        AND timestamp < :endTimestamp 
        ORDER BY timestamp DESC
    """)
    fun observeByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<TransactionEntity>>
    
    @Query("""
        SELECT * FROM transactions 
        WHERE isDeleted = 0 
        AND timestamp >= :startOfDay 
        AND timestamp < :endOfDay 
        ORDER BY timestamp DESC
    """)
    fun observeToday(startOfDay: Long, endOfDay: Long): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND category = :category ORDER BY timestamp DESC")
    fun observeByCategory(category: String): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND type = :type ORDER BY timestamp DESC")
    fun observeByType(type: String): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND source = :source ORDER BY timestamp DESC")
    fun observeBySource(source: String): Flow<List<TransactionEntity>>
    
    // ========== AGGREGATIONS ==========
    
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions 
        WHERE isDeleted = 0 
        AND type = 'EXPENSE' 
        AND timestamp >= :startTimestamp 
        AND timestamp < :endTimestamp
    """)
    fun observeTotalExpenses(startTimestamp: Long, endTimestamp: Long): Flow<Double>
    
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions 
        WHERE isDeleted = 0 
        AND type = 'INCOME' 
        AND timestamp >= :startTimestamp 
        AND timestamp < :endTimestamp
    """)
    fun observeTotalIncome(startTimestamp: Long, endTimestamp: Long): Flow<Double>
    
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions 
        WHERE isDeleted = 0 
        AND type IN ('SAVINGS', 'INVESTMENT', 'MICRO_SAVINGS')
        AND timestamp >= :startTimestamp 
        AND timestamp < :endTimestamp
    """)
    fun observeTotalSavings(startTimestamp: Long, endTimestamp: Long): Flow<Double>
    
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions 
        WHERE isDeleted = 0 
        AND type = 'EXPENSE' 
        AND timestamp >= :startOfDay 
        AND timestamp < :endOfDay
    """)
    fun observeDailyExpenses(startOfDay: Long, endOfDay: Long): Flow<Double>
    
    @Query("SELECT COUNT(*) FROM transactions WHERE isDeleted = 0")
    fun observeTransactionCount(): Flow<Int>
    
    @Query("""
        SELECT COALESCE(SUM(creditsEarned), 0) FROM transactions 
        WHERE isDeleted = 0 
        AND timestamp >= :startTimestamp 
        AND timestamp < :endTimestamp
    """)
    fun observeCreditsEarned(startTimestamp: Long, endTimestamp: Long): Flow<Int>
    
    // ========== EMERGENCY EXPENSE QUERIES ==========
    
    /**
     * Get normal (non-emergency) expenses for daily limit calculation.
     * Emergency expenses don't affect the Daily Safe-to-Spend.
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions 
        WHERE isDeleted = 0 
        AND type = 'EXPENSE' 
        AND isEmergency = 0
        AND timestamp >= :startOfDay 
        AND timestamp < :endOfDay
    """)
    fun observeNormalDailyExpenses(startOfDay: Long, endOfDay: Long): Flow<Double>
    
    /**
     * Get normal (non-emergency) expenses for the month.
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions 
        WHERE isDeleted = 0 
        AND type = 'EXPENSE' 
        AND isEmergency = 0
        AND timestamp >= :startTimestamp 
        AND timestamp < :endTimestamp
    """)
    fun observeNormalMonthlyExpenses(startTimestamp: Long, endTimestamp: Long): Flow<Double>
    
    /**
     * Get total emergency expenses for the month.
     * These are deducted from Savings (Shield Ring) instead of daily budget.
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions 
        WHERE isDeleted = 0 
        AND type = 'EXPENSE' 
        AND isEmergency = 1
        AND timestamp >= :startTimestamp 
        AND timestamp < :endTimestamp
    """)
    fun observeEmergencyExpensesThisMonth(startTimestamp: Long, endTimestamp: Long): Flow<Double>
    
    // ========== SEARCH ==========
    
    @Query("""
        SELECT * FROM transactions 
        WHERE isDeleted = 0 
        AND (description LIKE '%' || :query || '%' OR merchantName LIKE '%' || :query || '%')
        ORDER BY timestamp DESC
    """)
    fun search(query: String): Flow<List<TransactionEntity>>
    
    // ========== DEDUPLICATION ==========
    
    /**
     * Get recent transactions for deduplication check.
     * Used by BankNotificationService to prevent duplicate entries.
     * 
     * @param sinceTimestamp Minimum timestamp (typically now - 5 minutes)
     * @param source Transaction source (NOTIFICATION, SMS)
     * @return List of recent transactions from the specified source
     */
    @Query("""
        SELECT * FROM transactions 
        WHERE isDeleted = 0 
        AND timestamp >= :sinceTimestamp 
        AND source = :source
        ORDER BY timestamp DESC
        LIMIT 10
    """)
    suspend fun getRecentBySource(sinceTimestamp: Long, source: String): List<TransactionEntity>
    
    /**
     * Get the most recent transaction for quick dedup check.
     */
    @Query("""
        SELECT * FROM transactions 
        WHERE isDeleted = 0 
        AND source IN ('NOTIFICATION', 'SMS')
        ORDER BY timestamp DESC
        LIMIT 1
    """)
    suspend fun getLastAutoTransaction(): TransactionEntity?
    
    // ========== SYNC HELPERS ==========
    
    @Query("SELECT * FROM transactions WHERE updatedAt > :sinceTimestamp")
    suspend fun getModifiedSince(sinceTimestamp: Long): List<TransactionEntity>
    
    @Query("UPDATE transactions SET syncedAt = :timestamp WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>, timestamp: Long = System.currentTimeMillis())
    
    // ========== ANALYTICS QUERIES ==========
    
    /**
     * Get expenses for a date range (full entities for velocity chart).
     * Used for "Spending Velocity" chart - shows spending pace.
     */
    @Query("""
        SELECT * FROM transactions 
        WHERE isDeleted = 0 
        AND type = 'EXPENSE' 
        AND timestamp >= :startTimestamp 
        AND timestamp < :endTimestamp
        ORDER BY timestamp ASC
    """)
    suspend fun getExpensesInRange(startTimestamp: Long, endTimestamp: Long): List<TransactionEntity>
    
    /**
     * Get total spending by category for a date range.
     * Used for "Top Leaks" breakdown.
     */
    @Query("""
        SELECT category, SUM(amount) as total FROM transactions 
        WHERE isDeleted = 0 
        AND type = 'EXPENSE' 
        AND timestamp >= :startTimestamp 
        AND timestamp < :endTimestamp
        GROUP BY category
        ORDER BY total DESC
    """)
    suspend fun getSpendingByCategory(startTimestamp: Long, endTimestamp: Long): List<CategorySpending>
    
    /**
     * Get daily spending totals for consistency calendar.
     * Returns spending amount per day.
     */
    @Query("""
        SELECT 
            (timestamp / 86400000) as dayEpoch,
            SUM(amount) as total
        FROM transactions 
        WHERE isDeleted = 0 
        AND type = 'EXPENSE' 
        AND timestamp >= :startTimestamp 
        AND timestamp < :endTimestamp
        GROUP BY dayEpoch
        ORDER BY dayEpoch ASC
    """)
    suspend fun getDailySpendingTotals(startTimestamp: Long, endTimestamp: Long): List<DailySpending>
    
    /**
     * Get total expenses for today (non-flow version for Workers).
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions 
        WHERE isDeleted = 0 
        AND type = 'EXPENSE' 
        AND isEmergency = 0
        AND timestamp >= :startOfDay 
        AND timestamp < :endOfDay
    """)
    suspend fun getTotalNormalExpensesToday(startOfDay: Long, endOfDay: Long): Double
}

/**
 * Data class for category spending aggregation.
 */
data class CategorySpending(
    val category: String,
    val total: Double
)

/**
 * Data class for daily spending aggregation.
 */
data class DailySpending(
    val dayEpoch: Long,
    val total: Double
)
