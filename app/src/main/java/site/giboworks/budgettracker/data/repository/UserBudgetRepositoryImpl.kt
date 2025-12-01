package site.giboworks.budgettracker.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import site.giboworks.budgettracker.data.local.dao.UserBudgetDao
import site.giboworks.budgettracker.data.local.entity.UserBudgetEntity
import site.giboworks.budgettracker.domain.model.UserBudget
import site.giboworks.budgettracker.domain.repository.UserBudgetRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of UserBudgetRepository using Room database.
 */
@Singleton
class UserBudgetRepositoryImpl @Inject constructor(
    private val userBudgetDao: UserBudgetDao
) : UserBudgetRepository {
    
    // ========== WRITE OPERATIONS ==========
    
    override suspend fun save(userBudget: UserBudget) {
        userBudgetDao.insert(UserBudgetEntity.fromDomain(userBudget))
    }
    
    override suspend fun update(userBudget: UserBudget) {
        userBudgetDao.update(UserBudgetEntity.fromDomain(userBudget))
    }
    
    // ========== READ OPERATIONS ==========
    
    override suspend fun getActive(): UserBudget? {
        return userBudgetDao.get()?.toDomain()
    }
    
    override fun observeActive(): Flow<UserBudget?> {
        return userBudgetDao.observe().map { it?.toDomain() }
    }
    
    // ========== ONBOARDING STATE ==========
    
    override suspend fun hasCompletedOnboarding(): Boolean {
        return userBudgetDao.exists()
    }
    
    override suspend fun setOnboardingCompleted(completed: Boolean) {
        // Onboarding is considered complete when budget exists
        // If setting to false, we would delete the record (not implemented - use delete())
    }
}
