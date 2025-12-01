package site.giboworks.budgettracker.domain.model

import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/**
 * Budget configuration for a user.
 * Contains all the settings needed to calculate ring statuses.
 */
data class Budget(
    val id: String = UUID.randomUUID().toString(),
    val monthlyIncome: Double,
    val fixedExpenses: List<FixedExpense> = emptyList(),
    val savingsGoalPercentage: Float = 20f, // Target % of income to save
    val savingsGoalAmount: Double? = null,  // Or fixed amount
    val currency: Currency = Currency.RSD,
    val month: YearMonth = YearMonth.now(),
    val emergencyFundTarget: Double? = null,
    val categoryBudgets: Map<TransactionCategory, Double> = emptyMap()
) {
    /**
     * Calculate disposable income (after fixed expenses)
     */
    val disposableIncome: Double
        get() = monthlyIncome - fixedExpenses.sumOf { it.amount }
    
    /**
     * Calculate daily allowance based on remaining days in month
     */
    fun calculateDailyAllowance(currentDate: LocalDate = LocalDate.now()): Double {
        val daysInMonth = month.lengthOfMonth()
        val remainingDays = daysInMonth - currentDate.dayOfMonth + 1
        return if (remainingDays > 0) disposableIncome / remainingDays else 0.0
    }
    
    /**
     * Calculate target savings amount for the month
     */
    val targetSavingsAmount: Double
        get() = savingsGoalAmount ?: (monthlyIncome * savingsGoalPercentage / 100)
    
    /**
     * Total fixed expenses for the month
     */
    val totalFixedExpenses: Double
        get() = fixedExpenses.sumOf { it.amount }
}

/**
 * Represents a fixed recurring expense (bill)
 */
data class FixedExpense(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: Double,
    val category: TransactionCategory,
    val dueDay: Int, // Day of month when due
    val isPaid: Boolean = false,
    val autoPay: Boolean = false,
    val reminderDaysBefore: Int = 3
)

/**
 * User's gamification profile
 */
data class UserProfile(
    val id: String = UUID.randomUUID().toString(),
    val displayName: String = "User",
    val credits: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalTransactionsLogged: Int = 0,
    val unlockedThemes: List<String> = listOf("default"),
    val activeTheme: String = "default",
    val achievements: List<Achievement> = emptyList(),
    val joinedDate: LocalDate = LocalDate.now()
)

/**
 * Achievement for gamification
 */
data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val unlockedAt: LocalDate? = null,
    val creditsReward: Int = 0
)

/**
 * Theme that can be unlocked with credits
 */
data class RingTheme(
    val id: String,
    val name: String,
    val description: String,
    val creditsCost: Int,
    val pulseColors: Pair<Long, Long>,    // Start and end gradient colors
    val shieldColors: Pair<Long, Long>,
    val clarityColors: Pair<Long, Long>,
    val isPremium: Boolean = false
)
