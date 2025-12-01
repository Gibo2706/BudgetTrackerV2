package site.giboworks.budgettracker.domain.model

import java.time.LocalDate

/**
 * Analytics data models for the Insights screen.
 */

/**
 * Represents a data point for the Spending Velocity chart.
 * Shows cumulative spending for a specific day.
 */
data class SpendingVelocityPoint(
    val dayOfMonth: Int,
    val cumulativeAmount: Double
)

/**
 * Category spending data for "Top Leaks" breakdown.
 */
data class CategoryBreakdown(
    val category: TransactionCategory,
    val amount: Double,
    val percentage: Float, // 0-100
    val transactionCount: Int = 0
)

/**
 * Day status for "Consistency Calendar" heatmap.
 */
enum class DayStatus {
    /** Spent less than daily limit - great job! */
    UNDER_BUDGET,
    /** Spent more than limit but within safety margin */
    SLIGHTLY_OVER,
    /** Major overspending */
    OVER_BUDGET,
    /** No spending (Ghost Mode or $0 days) */
    NO_SPEND,
    /** Future day - not yet occurred */
    FUTURE,
    /** Today - in progress */
    TODAY
}

/**
 * Represents a single day in the Consistency Calendar.
 */
data class CalendarDay(
    val date: LocalDate,
    val dayOfMonth: Int,
    val amountSpent: Double,
    val dailyLimit: Double,
    val status: DayStatus
)

/**
 * Complete analytics state for InsightsScreen.
 */
data class InsightsState(
    val isLoading: Boolean = true,
    
    // Spending Velocity
    val currentMonthVelocity: List<SpendingVelocityPoint> = emptyList(),
    val previousMonthVelocity: List<SpendingVelocityPoint> = emptyList(),
    val velocityTrend: VelocityTrend = VelocityTrend.SAME,
    
    // Top Leaks
    val topCategories: List<CategoryBreakdown> = emptyList(),
    val totalSpentThisMonth: Double = 0.0,
    
    // Consistency Calendar
    val calendarDays: List<CalendarDay> = emptyList(),
    val daysUnderBudget: Int = 0,
    val daysOverBudget: Int = 0,
    val currentStreak: Int = 0,
    
    // Summary Stats
    val monthlyBudget: Double = 0.0,
    val dailyLimit: Double = 0.0,
    val currency: Currency = Currency.RSD
)

/**
 * Trend comparison between current and previous month spending pace.
 */
enum class VelocityTrend {
    /** Spending slower than last month - good! */
    SLOWER,
    /** Spending at same pace */
    SAME,
    /** Spending faster than last month - warning! */
    FASTER
}
