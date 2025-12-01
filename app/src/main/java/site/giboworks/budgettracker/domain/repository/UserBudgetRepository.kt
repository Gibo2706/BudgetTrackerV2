package site.giboworks.budgettracker.domain.repository

import kotlinx.coroutines.flow.Flow
import site.giboworks.budgettracker.domain.model.UserBudget

/**
 * Repository interface for UserBudget operations.
 * Manages the user's budget configuration and onboarding state.
 */
interface UserBudgetRepository {
    
    // ========== WRITE OPERATIONS ==========
    
    suspend fun save(userBudget: UserBudget)
    suspend fun update(userBudget: UserBudget)
    
    // ========== READ OPERATIONS ==========
    
    suspend fun getActive(): UserBudget?
    fun observeActive(): Flow<UserBudget?>
    
    // ========== ONBOARDING STATE ==========
    
    suspend fun hasCompletedOnboarding(): Boolean
    suspend fun setOnboardingCompleted(completed: Boolean)
}
