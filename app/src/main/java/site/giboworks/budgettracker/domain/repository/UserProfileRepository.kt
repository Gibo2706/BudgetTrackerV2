package site.giboworks.budgettracker.domain.repository

import kotlinx.coroutines.flow.Flow
import site.giboworks.budgettracker.domain.model.UserProfile

/**
 * Repository interface for UserProfile operations
 */
interface UserProfileRepository {
    
    suspend fun insert(profile: UserProfile)
    suspend fun update(profile: UserProfile)
    suspend fun getProfile(): UserProfile?
    
    fun observeProfile(): Flow<UserProfile?>
    
    suspend fun addCredits(amount: Int)
    suspend fun spendCredits(amount: Int): Boolean
    suspend fun updateStreak(streak: Int)
    suspend fun incrementTransactionCount()
    suspend fun setActiveTheme(themeId: String)
    suspend fun unlockTheme(themeId: String)
}
