package site.giboworks.budgettracker.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import site.giboworks.budgettracker.data.local.BudgetTrackerDatabase
import site.giboworks.budgettracker.data.local.dao.BudgetDao
import site.giboworks.budgettracker.data.local.dao.FixedBillDao
import site.giboworks.budgettracker.data.local.dao.TransactionDao
import site.giboworks.budgettracker.data.local.dao.UserBudgetDao
import site.giboworks.budgettracker.data.local.dao.UserProfileDao
import site.giboworks.budgettracker.data.repository.FixedBillRepositoryImpl
import site.giboworks.budgettracker.domain.repository.FixedBillRepository
import javax.inject.Singleton

/**
 * Hilt module for providing database dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): BudgetTrackerDatabase {
        return Room.databaseBuilder(
            context,
            BudgetTrackerDatabase::class.java,
            BudgetTrackerDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    @Singleton
    fun provideTransactionDao(database: BudgetTrackerDatabase): TransactionDao {
        return database.transactionDao()
    }
    
    @Provides
    @Singleton
    fun provideBudgetDao(database: BudgetTrackerDatabase): BudgetDao {
        return database.budgetDao()
    }
    
    @Provides
    @Singleton
    fun provideUserProfileDao(database: BudgetTrackerDatabase): UserProfileDao {
        return database.userProfileDao()
    }
    
    @Provides
    @Singleton
    fun provideUserBudgetDao(database: BudgetTrackerDatabase): UserBudgetDao {
        return database.userBudgetDao()
    }
    
    @Provides
    @Singleton
    fun provideFixedBillDao(database: BudgetTrackerDatabase): FixedBillDao {
        return database.fixedBillDao()
    }
    
    @Provides
    @Singleton
    fun provideFixedBillRepository(fixedBillDao: FixedBillDao): FixedBillRepository {
        return FixedBillRepositoryImpl(fixedBillDao)
    }
}
