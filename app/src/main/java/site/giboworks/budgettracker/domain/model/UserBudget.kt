package site.giboworks.budgettracker.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Domain model for User Budget Configuration.
 * 
 * This drives the "Daily Allowance" calculation which is the core
 * feature of the app's budget tracking.
 */
data class UserBudget(
    val id: String = "user_budget_singleton",
    val monthlyIncome: Double,
    val currency: Currency = Currency.RSD,
    val payDay: Int = 1, // 1-31, day when salary arrives
    val fixedExpensesTotal: Double = 0.0,
    val savingsTarget: Double = 0.0
) {
    /**
     * Calculate the daily spending allowance.
     * 
     * Formula: (Income - Fixed Expenses - Savings Target) / Days until next payday
     * 
     * This gives users a "safe to spend" amount per day that ensures
     * they'll have enough for bills and savings.
     */
    fun calculateDailyAllowance(): Double {
        val spendableMoney = monthlyIncome - fixedExpensesTotal - savingsTarget
        if (spendableMoney <= 0) return 0.0
        
        val daysRemaining = getDaysUntilNextPayDay()
        if (daysRemaining <= 0) return spendableMoney
        
        return spendableMoney / daysRemaining
    }
    
    /**
     * Get the total amount available for discretionary spending this cycle.
     */
    fun getSpendableBudget(): Double {
        return (monthlyIncome - fixedExpensesTotal - savingsTarget).coerceAtLeast(0.0)
    }
    
    /**
     * Calculate days remaining until next pay day.
     * 
     * If today is pay day, returns days until next month's pay day.
     * Handles edge cases like pay day being 31 in a 30-day month.
     */
    fun getDaysUntilNextPayDay(): Int {
        val today = LocalDate.now()
        val currentDay = today.dayOfMonth
        val currentMonth = today.monthValue
        val currentYear = today.year
        
        // Adjust payDay for months with fewer days
        val effectivePayDay = minOf(payDay, today.lengthOfMonth())
        
        return if (currentDay < effectivePayDay) {
            // Pay day is still coming this month
            effectivePayDay - currentDay
        } else {
            // Pay day has passed, calculate days until next month's pay day
            val nextMonth = today.plusMonths(1)
            val nextPayDay = minOf(payDay, nextMonth.lengthOfMonth())
            val nextPayDate = LocalDate.of(nextMonth.year, nextMonth.month, nextPayDay)
            ChronoUnit.DAYS.between(today, nextPayDate).toInt()
        }
    }
    
    /**
     * Get the start date of the current budget cycle.
     */
    fun getCurrentCycleStartDate(): LocalDate {
        val today = LocalDate.now()
        val currentDay = today.dayOfMonth
        val effectivePayDay = minOf(payDay, today.lengthOfMonth())
        
        return if (currentDay >= effectivePayDay) {
            // We're in a cycle that started this month
            LocalDate.of(today.year, today.month, effectivePayDay)
        } else {
            // We're in a cycle that started last month
            val lastMonth = today.minusMonths(1)
            val lastMonthPayDay = minOf(payDay, lastMonth.lengthOfMonth())
            LocalDate.of(lastMonth.year, lastMonth.month, lastMonthPayDay)
        }
    }
    
    /**
     * Get days elapsed in current budget cycle.
     */
    fun getDaysElapsedInCycle(): Int {
        val today = LocalDate.now()
        val cycleStart = getCurrentCycleStartDate()
        return ChronoUnit.DAYS.between(cycleStart, today).toInt() + 1
    }
    
    /**
     * Get total days in current budget cycle.
     */
    fun getTotalDaysInCycle(): Int {
        return getDaysElapsedInCycle() + getDaysUntilNextPayDay()
    }
    
    companion object {
        /**
         * Create a default/empty UserBudget for new users.
         */
        fun createDefault(): UserBudget {
            return UserBudget(
                monthlyIncome = 0.0,
                currency = Currency.RSD,
                payDay = 1,
                fixedExpensesTotal = 0.0,
                savingsTarget = 0.0
            )
        }
    }
}
