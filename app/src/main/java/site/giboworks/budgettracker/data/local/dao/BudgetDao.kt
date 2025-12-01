package site.giboworks.budgettracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import site.giboworks.budgettracker.data.local.entity.BudgetEntity

/**
 * Data Access Object for Budget operations
 */
@Dao
interface BudgetDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity)
    
    @Update
    suspend fun update(budget: BudgetEntity)
    
    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun delete(id: String)
    
    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getById(id: String): BudgetEntity?
    
    @Query("SELECT * FROM budgets WHERE yearMonth = :yearMonth LIMIT 1")
    suspend fun getByMonth(yearMonth: String): BudgetEntity?
    
    @Query("SELECT * FROM budgets WHERE yearMonth = :yearMonth LIMIT 1")
    fun observeByMonth(yearMonth: String): Flow<BudgetEntity?>
    
    @Query("SELECT * FROM budgets ORDER BY yearMonth DESC")
    fun observeAll(): Flow<List<BudgetEntity>>
    
    @Query("SELECT * FROM budgets ORDER BY yearMonth DESC LIMIT 1")
    fun observeLatest(): Flow<BudgetEntity?>
}
