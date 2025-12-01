package site.giboworks.budgettracker.domain.repository

import kotlinx.coroutines.flow.Flow
import site.giboworks.budgettracker.domain.model.Transaction
import site.giboworks.budgettracker.domain.model.TransactionCategory
import site.giboworks.budgettracker.domain.model.TransactionSource
import site.giboworks.budgettracker.domain.model.TransactionType
import java.time.LocalDate

/**
 * Repository interface for Transaction operations.
 * Defines the contract for data operations - implementation is in the data layer.
 */
interface TransactionRepository {
    
    // ========== WRITE OPERATIONS ==========
    
    suspend fun insert(transaction: Transaction)
    suspend fun insertAll(transactions: List<Transaction>)
    suspend fun update(transaction: Transaction)
    suspend fun delete(transaction: Transaction)
    suspend fun deleteById(id: String)
    
    // ========== READ OPERATIONS ==========
    
    suspend fun getById(id: String): Transaction?
    fun observeById(id: String): Flow<Transaction?>
    
    fun observeAll(): Flow<List<Transaction>>
    fun observeRecent(limit: Int): Flow<List<Transaction>>
    
    fun observeByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Transaction>>
    fun observeToday(): Flow<List<Transaction>>
    fun observeThisMonth(): Flow<List<Transaction>>
    
    fun observeByCategory(category: TransactionCategory): Flow<List<Transaction>>
    fun observeByType(type: TransactionType): Flow<List<Transaction>>
    
    // ========== AGGREGATIONS ==========
    
    fun observeTotalExpensesToday(): Flow<Double>
    fun observeTotalExpensesThisMonth(): Flow<Double>
    fun observeTotalIncomeThisMonth(): Flow<Double>
    fun observeTotalSavingsThisMonth(): Flow<Double>
    
    fun observeDailyExpenses(date: LocalDate): Flow<Double>
    
    fun observeTransactionCount(): Flow<Int>
    fun observeCreditsEarnedToday(): Flow<Int>
    fun observeCreditsEarnedThisMonth(): Flow<Int>
    fun observeTotalExpensesByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<Double>
    
    // ========== EMERGENCY EXPENSE HANDLING ==========
    
    /**
     * Get normal (non-emergency) expenses for today.
     * Emergency expenses don't affect the Daily Safe-to-Spend limit.
     */
    fun observeNormalExpensesToday(): Flow<Double>
    
    /**
     * Get normal (non-emergency) expenses for the month.
     */
    fun observeNormalExpensesThisMonth(): Flow<Double>
    
    /**
     * Get emergency expenses for the month.
     * These are deducted from Savings (Shield Ring) instead of daily budget.
     */
    fun observeEmergencyExpensesThisMonth(): Flow<Double>
    
    // ========== SEARCH ==========
    
    fun search(query: String): Flow<List<Transaction>>
    
    // ========== DEDUPLICATION ==========
    
    /**
     * Get recent transactions from a specific source for deduplication.
     * Used by notification/SMS parsers to prevent duplicate entries.
     * 
     * @param sinceTimestamp Minimum timestamp (typically now - 5 minutes)
     * @param source Transaction source (NOTIFICATION, SMS)
     * @return List of recent transactions
     */
    suspend fun getRecentBySource(sinceTimestamp: Long, source: TransactionSource): List<Transaction>
    
    /**
     * Get the most recent auto-captured transaction (from notification or SMS).
     */
    suspend fun getLastAutoTransaction(): Transaction?
}
