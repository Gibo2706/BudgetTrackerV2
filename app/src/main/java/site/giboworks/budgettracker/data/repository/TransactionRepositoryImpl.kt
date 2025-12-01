package site.giboworks.budgettracker.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import site.giboworks.budgettracker.data.local.dao.TransactionDao
import site.giboworks.budgettracker.data.local.entity.TransactionEntity
import site.giboworks.budgettracker.domain.model.Transaction
import site.giboworks.budgettracker.domain.model.TransactionCategory
import site.giboworks.budgettracker.domain.model.TransactionSource
import site.giboworks.budgettracker.domain.model.TransactionType
import site.giboworks.budgettracker.domain.repository.TransactionRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TransactionRepository using Room database.
 */
@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionRepository {
    
    // ========== WRITE OPERATIONS ==========
    
    override suspend fun insert(transaction: Transaction) {
        transactionDao.insert(TransactionEntity.fromDomain(transaction))
    }
    
    override suspend fun insertAll(transactions: List<Transaction>) {
        transactionDao.insertAll(transactions.map { TransactionEntity.fromDomain(it) })
    }
    
    override suspend fun update(transaction: Transaction) {
        transactionDao.update(TransactionEntity.fromDomain(transaction))
    }
    
    override suspend fun delete(transaction: Transaction) {
        transactionDao.softDelete(transaction.id)
    }
    
    override suspend fun deleteById(id: String) {
        transactionDao.softDelete(id)
    }
    
    // ========== READ OPERATIONS ==========
    
    override suspend fun getById(id: String): Transaction? {
        return transactionDao.getById(id)?.toDomain()
    }
    
    override fun observeById(id: String): Flow<Transaction?> {
        return transactionDao.observeById(id).map { it?.toDomain() }
    }
    
    override fun observeAll(): Flow<List<Transaction>> {
        return transactionDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun observeRecent(limit: Int): Flow<List<Transaction>> {
        return transactionDao.observeRecent(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun observeByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Transaction>> {
        val startTimestamp = startDate.atStartOfDay().toEpochMilli()
        val endTimestamp = endDate.plusDays(1).atStartOfDay().toEpochMilli()
        return transactionDao.observeByDateRange(startTimestamp, endTimestamp).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun observeToday(): Flow<List<Transaction>> {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay().toEpochMilli()
        val endOfDay = today.atTime(LocalTime.MAX).toEpochMilli()
        return transactionDao.observeToday(startOfDay, endOfDay).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun observeThisMonth(): Flow<List<Transaction>> {
        val now = YearMonth.now()
        val startOfMonth = now.atDay(1).atStartOfDay().toEpochMilli()
        val endOfMonth = now.atEndOfMonth().atTime(LocalTime.MAX).toEpochMilli()
        return transactionDao.observeByDateRange(startOfMonth, endOfMonth).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun observeByCategory(category: TransactionCategory): Flow<List<Transaction>> {
        return transactionDao.observeByCategory(category.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun observeByType(type: TransactionType): Flow<List<Transaction>> {
        return transactionDao.observeByType(type.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    // ========== AGGREGATIONS ==========
    
    override fun observeTotalExpensesToday(): Flow<Double> {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay().toEpochMilli()
        val endOfDay = today.atTime(LocalTime.MAX).toEpochMilli()
        return transactionDao.observeDailyExpenses(startOfDay, endOfDay)
    }
    
    override fun observeTotalExpensesThisMonth(): Flow<Double> {
        val now = YearMonth.now()
        val startOfMonth = now.atDay(1).atStartOfDay().toEpochMilli()
        val endOfMonth = now.atEndOfMonth().atTime(LocalTime.MAX).toEpochMilli()
        return transactionDao.observeTotalExpenses(startOfMonth, endOfMonth)
    }
    
    override fun observeTotalIncomeThisMonth(): Flow<Double> {
        val now = YearMonth.now()
        val startOfMonth = now.atDay(1).atStartOfDay().toEpochMilli()
        val endOfMonth = now.atEndOfMonth().atTime(LocalTime.MAX).toEpochMilli()
        return transactionDao.observeTotalIncome(startOfMonth, endOfMonth)
    }
    
    override fun observeTotalSavingsThisMonth(): Flow<Double> {
        val now = YearMonth.now()
        val startOfMonth = now.atDay(1).atStartOfDay().toEpochMilli()
        val endOfMonth = now.atEndOfMonth().atTime(LocalTime.MAX).toEpochMilli()
        return transactionDao.observeTotalSavings(startOfMonth, endOfMonth)
    }
    
    override fun observeDailyExpenses(date: LocalDate): Flow<Double> {
        val startOfDay = date.atStartOfDay().toEpochMilli()
        val endOfDay = date.atTime(LocalTime.MAX).toEpochMilli()
        return transactionDao.observeDailyExpenses(startOfDay, endOfDay)
    }
    
    override fun observeTransactionCount(): Flow<Int> {
        return transactionDao.observeTransactionCount()
    }
    
    override fun observeCreditsEarnedToday(): Flow<Int> {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay().toEpochMilli()
        val endOfDay = today.atTime(LocalTime.MAX).toEpochMilli()
        return transactionDao.observeCreditsEarned(startOfDay, endOfDay)
    }
    
    override fun observeCreditsEarnedThisMonth(): Flow<Int> {
        val now = YearMonth.now()
        val startOfMonth = now.atDay(1).atStartOfDay().toEpochMilli()
        val endOfMonth = now.atEndOfMonth().atTime(LocalTime.MAX).toEpochMilli()
        return transactionDao.observeCreditsEarned(startOfMonth, endOfMonth)
    }
    
    override fun observeTotalExpensesByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<Double> {
        val startTimestamp = startDate.atStartOfDay().toEpochMilli()
        val endTimestamp = endDate.plusDays(1).atStartOfDay().toEpochMilli()
        return transactionDao.observeTotalExpenses(startTimestamp, endTimestamp)
    }
    
    // ========== EMERGENCY EXPENSE HANDLING ==========
    
    override fun observeNormalExpensesToday(): Flow<Double> {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay().toEpochMilli()
        val endOfDay = today.atTime(LocalTime.MAX).toEpochMilli()
        return transactionDao.observeNormalDailyExpenses(startOfDay, endOfDay)
    }
    
    override fun observeNormalExpensesThisMonth(): Flow<Double> {
        val now = YearMonth.now()
        val startOfMonth = now.atDay(1).atStartOfDay().toEpochMilli()
        val endOfMonth = now.atEndOfMonth().atTime(LocalTime.MAX).toEpochMilli()
        return transactionDao.observeNormalMonthlyExpenses(startOfMonth, endOfMonth)
    }
    
    override fun observeEmergencyExpensesThisMonth(): Flow<Double> {
        val now = YearMonth.now()
        val startOfMonth = now.atDay(1).atStartOfDay().toEpochMilli()
        val endOfMonth = now.atEndOfMonth().atTime(LocalTime.MAX).toEpochMilli()
        return transactionDao.observeEmergencyExpensesThisMonth(startOfMonth, endOfMonth)
    }
    
    // ========== SEARCH ==========
    
    override fun search(query: String): Flow<List<Transaction>> {
        return transactionDao.search(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    // ========== DEDUPLICATION ==========
    
    override suspend fun getRecentBySource(
        sinceTimestamp: Long,
        source: TransactionSource
    ): List<Transaction> {
        return transactionDao.getRecentBySource(sinceTimestamp, source.name)
            .map { it.toDomain() }
    }
    
    override suspend fun getLastAutoTransaction(): Transaction? {
        return transactionDao.getLastAutoTransaction()?.toDomain()
    }
    
    // ========== HELPER ==========
    
    private fun java.time.LocalDateTime.toEpochMilli(): Long {
        return this.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
