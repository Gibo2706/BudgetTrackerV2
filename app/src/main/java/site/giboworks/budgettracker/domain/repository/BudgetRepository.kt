package site.giboworks.budgettracker.domain.repository

import kotlinx.coroutines.flow.Flow
import site.giboworks.budgettracker.domain.model.Budget
import java.time.YearMonth

/**
 * Repository interface for Budget operations
 */
interface BudgetRepository {
    
    suspend fun insert(budget: Budget)
    suspend fun update(budget: Budget)
    suspend fun delete(id: String)
    
    suspend fun getById(id: String): Budget?
    suspend fun getByMonth(yearMonth: YearMonth): Budget?
    
    fun observeByMonth(yearMonth: YearMonth): Flow<Budget?>
    fun observeCurrentMonth(): Flow<Budget?>
    fun observeAll(): Flow<List<Budget>>
}
