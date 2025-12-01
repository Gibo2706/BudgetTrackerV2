package site.giboworks.budgettracker.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import site.giboworks.budgettracker.domain.model.Budget
import site.giboworks.budgettracker.domain.model.Currency
import site.giboworks.budgettracker.domain.model.RingState
import site.giboworks.budgettracker.domain.model.RingStatus
import site.giboworks.budgettracker.domain.model.RingType
import site.giboworks.budgettracker.domain.model.VitalityRingsState
import site.giboworks.budgettracker.domain.repository.BudgetRepository
import site.giboworks.budgettracker.domain.repository.TransactionRepository
import site.giboworks.budgettracker.domain.repository.UserProfileRepository
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Use case for calculating the current Vitality Rings state.
 * This encapsulates all the business logic for ring calculations.
 */
class CalculateVitalityRingsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val userProfileRepository: UserProfileRepository
) {
    /**
     * Returns a Flow of VitalityRingsState that updates in real-time
     */
    operator fun invoke(): Flow<VitalityRingsState> {
        return combine(
            transactionRepository.observeTotalExpensesToday(),
            transactionRepository.observeTotalSavingsThisMonth(),
            transactionRepository.observeTotalExpensesThisMonth(),
            budgetRepository.observeCurrentMonth(),
            transactionRepository.observeCreditsEarnedToday()
        ) { dailyExpenses, monthlySavings, monthlyExpenses, budget, creditsToday ->
            calculateState(
                dailyExpenses = dailyExpenses,
                monthlySavings = monthlySavings,
                monthlyExpenses = monthlyExpenses,
                budget = budget,
                creditsToday = creditsToday
            )
        }
    }
    
    private fun calculateState(
        dailyExpenses: Double,
        monthlySavings: Double,
        monthlyExpenses: Double,
        budget: Budget?,
        creditsToday: Int
    ): VitalityRingsState {
        // Use default values if no budget is configured
        val effectiveBudget = budget ?: createDefaultBudget()
        
        val today = LocalDate.now()
        val dailyAllowance = effectiveBudget.calculateDailyAllowance(today)
        
        // PULSE Ring - Daily spending (inverse: filling up = spending)
        val pulseProgress = calculatePulseProgress(dailyExpenses, dailyAllowance)
        val pulseState = determinePulseState(pulseProgress)
        
        // SHIELD Ring - Savings progress
        val shieldProgress = calculateShieldProgress(monthlySavings, effectiveBudget.targetSavingsAmount)
        val shieldState = determineShieldState(shieldProgress)
        
        // CLARITY Ring - Bills paid
        val (billsPaid, totalBills) = calculateBillsStatus(effectiveBudget)
        val clarityProgress = if (totalBills > 0) billsPaid.toFloat() / totalBills else 0f
        val clarityState = determineClarityState(clarityProgress)
        
        return VitalityRingsState(
            pulse = RingStatus(
                type = RingType.PULSE,
                progress = pulseProgress,
                currentValue = dailyExpenses,
                targetValue = dailyAllowance,
                state = pulseState,
                label = "Pulse",
                sublabel = formatPulseSublabel(dailyExpenses, dailyAllowance, effectiveBudget.currency),
                currency = effectiveBudget.currency
            ),
            shield = RingStatus(
                type = RingType.SHIELD,
                progress = shieldProgress.coerceIn(0f, 1f),
                currentValue = monthlySavings,
                targetValue = effectiveBudget.targetSavingsAmount,
                state = shieldState,
                label = "Shield",
                sublabel = formatShieldSublabel(monthlySavings, effectiveBudget.targetSavingsAmount, effectiveBudget.currency),
                currency = effectiveBudget.currency
            ),
            clarity = RingStatus(
                type = RingType.CLARITY,
                progress = clarityProgress,
                currentValue = billsPaid.toDouble(),
                targetValue = totalBills.toDouble(),
                state = clarityState,
                label = "Clarity",
                sublabel = "$billsPaid/$totalBills bills paid",
                currency = effectiveBudget.currency
            ),
            streak = 0, // Would come from UserProfile
            creditsEarnedToday = creditsToday
        )
    }
    
    private fun createDefaultBudget(): Budget {
        return Budget(
            monthlyIncome = 100000.0, // 100k RSD default
            savingsGoalPercentage = 20f,
            currency = Currency.RSD,
            month = YearMonth.now()
        )
    }
    
    private fun calculatePulseProgress(spent: Double, allowance: Double): Float {
        if (allowance <= 0) return 0f
        return (spent / allowance).toFloat()
    }
    
    private fun determinePulseState(progress: Float): RingState {
        return when {
            progress <= 0.5f -> RingState.EXCELLENT
            progress <= 0.7f -> RingState.GOOD
            progress <= 0.9f -> RingState.WARNING
            progress > 1f -> RingState.CRITICAL
            else -> RingState.GOOD
        }
    }
    
    private fun calculateShieldProgress(saved: Double, target: Double): Float {
        if (target <= 0) return 0f
        return (saved / target).toFloat()
    }
    
    private fun determineShieldState(progress: Float): RingState {
        return when {
            progress >= 1f -> RingState.COMPLETED
            progress >= 0.7f -> RingState.EXCELLENT
            progress >= 0.4f -> RingState.GOOD
            progress > 0f -> RingState.WARNING
            else -> RingState.INACTIVE
        }
    }
    
    private fun calculateBillsStatus(budget: Budget): Pair<Int, Int> {
        val paidCount = budget.fixedExpenses.count { it.isPaid }
        val totalCount = budget.fixedExpenses.size
        return Pair(paidCount, totalCount)
    }
    
    private fun determineClarityState(progress: Float): RingState {
        return when {
            progress >= 1f -> RingState.COMPLETED
            progress >= 0.7f -> RingState.GOOD
            progress >= 0.3f -> RingState.WARNING
            progress > 0f -> RingState.WARNING
            else -> RingState.INACTIVE
        }
    }
    
    private fun formatPulseSublabel(current: Double, target: Double, currency: Currency): String {
        val remaining = target - current
        return if (remaining > 0) {
            "${formatCurrency(remaining, currency)} left today"
        } else {
            "Over by ${formatCurrency(-remaining, currency)}"
        }
    }
    
    private fun formatShieldSublabel(current: Double, target: Double, currency: Currency): String {
        return "${formatCurrency(current, currency)} / ${formatCurrency(target, currency)}"
    }
    
    private fun formatCurrency(amount: Double, currency: Currency): String {
        return when (currency) {
            Currency.RSD -> "%,.0f %s".format(amount, currency.symbol)
            else -> "%s%,.2f".format(currency.symbol, amount)
        }
    }
}
