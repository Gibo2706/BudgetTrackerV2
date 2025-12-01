package site.giboworks.budgettracker.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import site.giboworks.budgettracker.data.repository.TransactionRepositoryImpl
import site.giboworks.budgettracker.data.repository.UserBudgetRepositoryImpl
import site.giboworks.budgettracker.domain.repository.TransactionRepository
import site.giboworks.budgettracker.domain.repository.UserBudgetRepository
import javax.inject.Singleton

/**
 * Hilt module for providing repository dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl
    ): TransactionRepository
    
    @Binds
    @Singleton
    abstract fun bindUserBudgetRepository(
        impl: UserBudgetRepositoryImpl
    ): UserBudgetRepository
}
