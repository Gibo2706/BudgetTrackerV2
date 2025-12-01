package site.giboworks.budgettracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import site.giboworks.budgettracker.data.local.entity.UserProfileEntity

/**
 * Data Access Object for UserProfile operations
 */
@Dao
interface UserProfileDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: UserProfileEntity)
    
    @Update
    suspend fun update(profile: UserProfileEntity)
    
    @Query("SELECT * FROM user_profiles LIMIT 1")
    suspend fun getProfile(): UserProfileEntity?
    
    @Query("SELECT * FROM user_profiles LIMIT 1")
    fun observeProfile(): Flow<UserProfileEntity?>
    
    @Query("UPDATE user_profiles SET credits = credits + :amount, updatedAt = :timestamp WHERE id = :id")
    suspend fun addCredits(id: String, amount: Int, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE user_profiles SET credits = credits - :amount, updatedAt = :timestamp WHERE id = :id")
    suspend fun spendCredits(id: String, amount: Int, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE user_profiles SET currentStreak = :streak, longestStreak = CASE WHEN :streak > longestStreak THEN :streak ELSE longestStreak END, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateStreak(id: String, streak: Int, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE user_profiles SET totalTransactionsLogged = totalTransactionsLogged + 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun incrementTransactionCount(id: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE user_profiles SET activeTheme = :themeId, updatedAt = :timestamp WHERE id = :id")
    suspend fun setActiveTheme(id: String, themeId: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE user_profiles SET lastActiveDate = :timestamp, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateLastActive(id: String, timestamp: Long = System.currentTimeMillis())
}
