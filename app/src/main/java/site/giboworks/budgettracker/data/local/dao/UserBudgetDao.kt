package site.giboworks.budgettracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import site.giboworks.budgettracker.data.local.entity.UserBudgetEntity

/**
 * Data Access Object for UserBudget operations.
 * 
 * Note: UserBudget is a singleton - only one record exists for the user.
 */
@Dao
interface UserBudgetDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(userBudget: UserBudgetEntity)
    
    @Update
    suspend fun update(userBudget: UserBudgetEntity)
    
    /**
     * Get the user's budget configuration.
     * Returns null if onboarding hasn't been completed.
     */
    @Query("SELECT * FROM user_budget WHERE id = :id LIMIT 1")
    suspend fun get(id: String = UserBudgetEntity.SINGLETON_ID): UserBudgetEntity?
    
    /**
     * Observe the user's budget configuration reactively.
     */
    @Query("SELECT * FROM user_budget WHERE id = :id LIMIT 1")
    fun observe(id: String = UserBudgetEntity.SINGLETON_ID): Flow<UserBudgetEntity?>
    
    /**
     * Check if budget has been configured (onboarding completed).
     */
    @Query("SELECT EXISTS(SELECT 1 FROM user_budget WHERE id = :id)")
    suspend fun exists(id: String = UserBudgetEntity.SINGLETON_ID): Boolean
    
    /**
     * Delete the user's budget (for testing/reset).
     */
    @Query("DELETE FROM user_budget WHERE id = :id")
    suspend fun delete(id: String = UserBudgetEntity.SINGLETON_ID)
}
