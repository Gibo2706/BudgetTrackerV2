package site.giboworks.budgettracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import site.giboworks.budgettracker.domain.model.Currency
import site.giboworks.budgettracker.domain.model.UserBudget

/**
 * Room Entity for User Budget Configuration.
 * 
 * This is the core configuration that drives the daily allowance calculation.
 * Only ONE record should exist (singleton pattern for user settings).
 * 
 * Key concept: The budget cycle resets on payDay, not necessarily the 1st of the month.
 * Example: If user gets paid on the 15th, their "budget month" runs from 15th to 14th.
 */
@Entity(tableName = "user_budget")
data class UserBudgetEntity(
    @PrimaryKey
    val id: String = "user_budget_singleton", // Single record
    
    val monthlyIncome: Double,
    val currency: String = Currency.RSD.code,
    
    /**
     * Day of month when salary arrives (1-31).
     * This is when the budget cycle resets.
     */
    val payDay: Int = 1,
    
    /**
     * Total fixed monthly expenses (rent, bills, subscriptions combined).
     * This is subtracted from income before calculating daily allowance.
     */
    val fixedExpensesTotal: Double = 0.0,
    
    /**
     * Monthly savings target amount.
     * This is "paid to yourself first" before calculating daily allowance.
     */
    val savingsTarget: Double = 0.0,
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Convert to domain model
     */
    fun toDomain(): UserBudget {
        return UserBudget(
            id = id,
            monthlyIncome = monthlyIncome,
            currency = Currency.entries.find { it.code == currency } ?: Currency.RSD,
            payDay = payDay,
            fixedExpensesTotal = fixedExpensesTotal,
            savingsTarget = savingsTarget
        )
    }
    
    companion object {
        const val SINGLETON_ID = "user_budget_singleton"
        
        /**
         * Create entity from domain model
         */
        fun fromDomain(userBudget: UserBudget): UserBudgetEntity {
            return UserBudgetEntity(
                id = SINGLETON_ID,
                monthlyIncome = userBudget.monthlyIncome,
                currency = userBudget.currency.code,
                payDay = userBudget.payDay,
                fixedExpensesTotal = userBudget.fixedExpensesTotal,
                savingsTarget = userBudget.savingsTarget,
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}
