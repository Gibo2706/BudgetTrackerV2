package site.giboworks.budgettracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import site.giboworks.budgettracker.data.local.dao.BudgetDao
import site.giboworks.budgettracker.data.local.dao.FixedBillDao
import site.giboworks.budgettracker.data.local.dao.TransactionDao
import site.giboworks.budgettracker.data.local.dao.UserBudgetDao
import site.giboworks.budgettracker.data.local.dao.UserProfileDao
import site.giboworks.budgettracker.data.local.entity.BudgetEntity
import site.giboworks.budgettracker.data.local.entity.FixedBillEntity
import site.giboworks.budgettracker.data.local.entity.TransactionEntity
import site.giboworks.budgettracker.data.local.entity.UserBudgetEntity
import site.giboworks.budgettracker.data.local.entity.UserProfileEntity

/**
 * Main Room Database for Budget Tracker.
 * Single source of truth for all local data.
 * 
 * Version History:
 * v1 - Initial schema
 * v2 - Added UserBudgetEntity for onboarding
 * v3 - Added isEmergency field to TransactionEntity
 * v4 - Added FixedBillEntity for detailed bill tracking
 */
@Database(
    entities = [
        TransactionEntity::class,
        BudgetEntity::class,
        UserProfileEntity::class,
        UserBudgetEntity::class,
        FixedBillEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class BudgetTrackerDatabase : RoomDatabase() {
    
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun userBudgetDao(): UserBudgetDao
    abstract fun fixedBillDao(): FixedBillDao
    
    companion object {
        const val DATABASE_NAME = "budget_tracker_db"
    }
}
