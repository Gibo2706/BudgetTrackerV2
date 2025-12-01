package site.giboworks.budgettracker.presentation.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import site.giboworks.budgettracker.data.local.dao.TransactionDao
import site.giboworks.budgettracker.domain.model.CalendarDay
import site.giboworks.budgettracker.domain.model.CategoryBreakdown
import site.giboworks.budgettracker.domain.model.Currency
import site.giboworks.budgettracker.domain.model.DayStatus
import site.giboworks.budgettracker.domain.model.InsightsState
import site.giboworks.budgettracker.domain.model.SpendingVelocityPoint
import site.giboworks.budgettracker.domain.model.TransactionCategory
import site.giboworks.budgettracker.domain.model.VelocityTrend
import site.giboworks.budgettracker.domain.repository.UserBudgetRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

/**
 * ViewModel for the Insights/Analytics screen.
 * Prepares data for:
 * - Spending Velocity chart (current vs previous month)
 * - Top Leaks category breakdown
 * - Consistency Calendar heatmap
 */
@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val transactionDao: TransactionDao,
    private val userBudgetRepository: UserBudgetRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(InsightsState())
    val state: StateFlow<InsightsState> = _state.asStateFlow()
    
    init {
        loadAnalytics()
    }
    
    fun refresh() {
        loadAnalytics()
    }
    
    private fun loadAnalytics() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            try {
                // Get user budget configuration
                val budget = userBudgetRepository.getActive()
                val dailyLimit = calculateDailyLimit(budget?.monthlyIncome ?: 0.0, budget?.fixedExpensesTotal ?: 0.0, budget?.savingsTarget ?: 0.0)
                val currency = budget?.currency ?: Currency.RSD
                
                // Load all analytics data in parallel
                val velocityData = loadSpendingVelocity()
                val categoryData = loadTopCategories()
                val calendarData = loadConsistencyCalendar(dailyLimit)
                
                // Calculate velocity trend
                val trend = calculateVelocityTrend(velocityData.first, velocityData.second)
                
                // Calculate streak and stats
                val (underBudget, overBudget, streak) = calculateCalendarStats(calendarData)
                
                _state.value = InsightsState(
                    isLoading = false,
                    currentMonthVelocity = velocityData.first,
                    previousMonthVelocity = velocityData.second,
                    velocityTrend = trend,
                    topCategories = categoryData.first,
                    totalSpentThisMonth = categoryData.second,
                    calendarDays = calendarData,
                    daysUnderBudget = underBudget,
                    daysOverBudget = overBudget,
                    currentStreak = streak,
                    monthlyBudget = budget?.monthlyIncome ?: 0.0,
                    dailyLimit = dailyLimit,
                    currency = currency
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }
    
    /**
     * Load cumulative spending data for current and previous month.
     */
    private suspend fun loadSpendingVelocity(): Pair<List<SpendingVelocityPoint>, List<SpendingVelocityPoint>> {
        val today = LocalDate.now()
        val currentMonth = YearMonth.from(today)
        val previousMonth = currentMonth.minusMonths(1)
        
        // Current month: Day 1 to today
        val currentStart = currentMonth.atDay(1).atStartOfDay()
        val currentEnd = today.plusDays(1).atStartOfDay()
        
        // Previous month: Day 1 to same day of month (or end of month if shorter)
        val prevStart = previousMonth.atDay(1).atStartOfDay()
        val prevDayOfMonth = minOf(today.dayOfMonth, previousMonth.lengthOfMonth())
        val prevEnd = previousMonth.atDay(prevDayOfMonth).plusDays(1).atStartOfDay()
        
        val currentExpenses = transactionDao.getExpensesInRange(
            currentStart.toEpochMilli(),
            currentEnd.toEpochMilli()
        )
        
        val previousExpenses = transactionDao.getExpensesInRange(
            prevStart.toEpochMilli(),
            prevEnd.toEpochMilli()
        )
        
        // Build cumulative points for current month
        val currentPoints = buildCumulativePoints(currentExpenses, currentMonth, today.dayOfMonth)
        
        // Build cumulative points for previous month (up to same day)
        val previousPoints = buildCumulativePoints(previousExpenses, previousMonth, prevDayOfMonth)
        
        return Pair(currentPoints, previousPoints)
    }
    
    private fun buildCumulativePoints(
        expenses: List<site.giboworks.budgettracker.data.local.entity.TransactionEntity>,
        month: YearMonth,
        maxDay: Int
    ): List<SpendingVelocityPoint> {
        // Group expenses by day
        val dailyTotals = mutableMapOf<Int, Double>()
        expenses.forEach { expense ->
            val date = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(expense.timestamp),
                ZoneId.systemDefault()
            ).toLocalDate()
            val day = date.dayOfMonth
            dailyTotals[day] = (dailyTotals[day] ?: 0.0) + expense.amount
        }
        
        // Build cumulative points
        val points = mutableListOf<SpendingVelocityPoint>()
        var cumulative = 0.0
        
        for (day in 1..maxDay) {
            cumulative += dailyTotals[day] ?: 0.0
            points.add(SpendingVelocityPoint(day, cumulative))
        }
        
        return points
    }
    
    /**
     * Load top spending categories for current month.
     */
    private suspend fun loadTopCategories(): Pair<List<CategoryBreakdown>, Double> {
        val today = LocalDate.now()
        val monthStart = YearMonth.from(today).atDay(1).atStartOfDay()
        val monthEnd = today.plusDays(1).atStartOfDay()
        
        val categorySpending = transactionDao.getSpendingByCategory(
            monthStart.toEpochMilli(),
            monthEnd.toEpochMilli()
        )
        
        val totalSpent = categorySpending.sumOf { it.total }
        
        val breakdowns = categorySpending
            .take(5) // Top 5 categories
            .map { spending ->
                CategoryBreakdown(
                    category = TransactionCategory.entries.find { it.name == spending.category }
                        ?: TransactionCategory.OTHER_EXPENSE,
                    amount = spending.total,
                    percentage = if (totalSpent > 0) ((spending.total / totalSpent) * 100).toFloat() else 0f
                )
            }
        
        return Pair(breakdowns, totalSpent)
    }
    
    /**
     * Load consistency calendar data for current month.
     */
    private suspend fun loadConsistencyCalendar(dailyLimit: Double): List<CalendarDay> {
        val today = LocalDate.now()
        val currentMonth = YearMonth.from(today)
        val monthStart = currentMonth.atDay(1).atStartOfDay()
        val monthEnd = currentMonth.atEndOfMonth().plusDays(1).atStartOfDay()
        
        val dailySpending = transactionDao.getDailySpendingTotals(
            monthStart.toEpochMilli(),
            monthEnd.toEpochMilli()
        )
        
        // Convert to map for easy lookup
        val spendingByDay = dailySpending.associate { spending ->
            val date = LocalDate.ofEpochDay(spending.dayEpoch)
            date.dayOfMonth to spending.total
        }
        
        // Build calendar days
        val calendarDays = mutableListOf<CalendarDay>()
        
        for (day in 1..currentMonth.lengthOfMonth()) {
            val date = currentMonth.atDay(day)
            val spent = spendingByDay[day] ?: 0.0
            
            val status = when {
                date.isAfter(today) -> DayStatus.FUTURE
                date.isEqual(today) -> DayStatus.TODAY
                spent == 0.0 -> DayStatus.NO_SPEND
                spent <= dailyLimit -> DayStatus.UNDER_BUDGET
                spent <= dailyLimit * 1.25 -> DayStatus.SLIGHTLY_OVER
                else -> DayStatus.OVER_BUDGET
            }
            
            calendarDays.add(
                CalendarDay(
                    date = date,
                    dayOfMonth = day,
                    amountSpent = spent,
                    dailyLimit = dailyLimit,
                    status = status
                )
            )
        }
        
        return calendarDays
    }
    
    private fun calculateVelocityTrend(
        current: List<SpendingVelocityPoint>,
        previous: List<SpendingVelocityPoint>
    ): VelocityTrend {
        if (current.isEmpty() || previous.isEmpty()) return VelocityTrend.SAME
        
        val currentTotal = current.lastOrNull()?.cumulativeAmount ?: 0.0
        val previousTotal = previous.lastOrNull()?.cumulativeAmount ?: 0.0
        
        val difference = currentTotal - previousTotal
        val threshold = previousTotal * 0.1 // 10% threshold
        
        return when {
            difference < -threshold -> VelocityTrend.SLOWER
            difference > threshold -> VelocityTrend.FASTER
            else -> VelocityTrend.SAME
        }
    }
    
    private fun calculateCalendarStats(days: List<CalendarDay>): Triple<Int, Int, Int> {
        val pastDays = days.filter { 
            it.status != DayStatus.FUTURE && it.status != DayStatus.TODAY 
        }
        
        val underBudget = pastDays.count { 
            it.status == DayStatus.UNDER_BUDGET || it.status == DayStatus.NO_SPEND 
        }
        val overBudget = pastDays.count { 
            it.status == DayStatus.OVER_BUDGET || it.status == DayStatus.SLIGHTLY_OVER 
        }
        
        // Calculate current streak of under-budget days
        var streak = 0
        for (day in pastDays.reversed()) {
            if (day.status == DayStatus.UNDER_BUDGET || day.status == DayStatus.NO_SPEND) {
                streak++
            } else {
                break
            }
        }
        
        return Triple(underBudget, overBudget, streak)
    }
    
    private fun calculateDailyLimit(income: Double, fixedExpenses: Double, savings: Double): Double {
        val disposable = income - fixedExpenses - savings
        val daysInMonth = YearMonth.now().lengthOfMonth()
        return (disposable / daysInMonth).coerceAtLeast(0.0)
    }
    
    private fun LocalDateTime.toEpochMilli(): Long {
        return this.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
