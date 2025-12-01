package site.giboworks.budgettracker.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import site.giboworks.budgettracker.data.preferences.AppPreferences
import site.giboworks.budgettracker.domain.model.BillsSummary
import site.giboworks.budgettracker.domain.model.Currency
import site.giboworks.budgettracker.domain.model.FixedBill
import site.giboworks.budgettracker.domain.model.RingState
import site.giboworks.budgettracker.domain.model.RingStatus
import site.giboworks.budgettracker.domain.model.RingType
import site.giboworks.budgettracker.domain.model.Transaction
import site.giboworks.budgettracker.domain.model.TransactionCategory
import site.giboworks.budgettracker.domain.model.TransactionSource
import site.giboworks.budgettracker.domain.model.TransactionType
import site.giboworks.budgettracker.domain.model.UserBudget
import site.giboworks.budgettracker.domain.model.VitalityRingsState
import site.giboworks.budgettracker.domain.repository.FixedBillRepository
import site.giboworks.budgettracker.domain.repository.TransactionRepository
import site.giboworks.budgettracker.domain.repository.UserBudgetRepository
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlin.math.max

/**
 * ViewModel for the main Dashboard screen.
 * 
 * Implements the "Non-Judgmental" Vitality Algorithm:
 * - No harsh "Over!" messages - uses "Adjusted" instead
 * - Rolling Budget: Overspent amounts spread across remaining days
 * - Vitality Score: 60% daily adherence + 40% savings progress
 * 
 * Emergency Expense Logic:
 * - Normal expenses affect Daily Safe-to-Spend (Pulse ring)
 * - Emergency expenses are deducted from Savings (Shield ring) instead
 * 
 * Ghost Mode:
 * - When enabled, rings are hidden and tracking is paused
 * - Shows a soothing message for mental health breaks
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val userBudgetRepository: UserBudgetRepository,
    private val fixedBillRepository: FixedBillRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {
    
    // Budget configuration - now from database
    private val _budgetConfig = MutableStateFlow(BudgetConfig())
    val budgetConfig: StateFlow<BudgetConfig> = _budgetConfig.asStateFlow()
    
    // Bills data
    val fixedBills: StateFlow<List<FixedBill>> = fixedBillRepository.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val billsSummary: StateFlow<BillsSummary> = fixedBillRepository.observeAll()
        .map { bills -> BillsSummary.fromBills(bills) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BillsSummary.empty()
        )
    
    // Observe user budget from database combined with bills data
    // Using nested combines since standard combine only supports up to 5 flows
    private val billStatsFlow = combine(
        fixedBillRepository.observeTotalCount(),
        fixedBillRepository.observePaidCount(),
        fixedBillRepository.observeTotalEstimated(),
        fixedBillRepository.observeSavingsFromBills(),
        fixedBillRepository.observeOverageFromBills()
    ) { totalBills, paidBills, totalEstimated, billSavings, billOverage ->
        BillStats(totalBills, paidBills, totalEstimated, billSavings, billOverage)
    }
    
    private val userBudgetFlow = combine(
        userBudgetRepository.observeActive(),
        billStatsFlow
    ) { userBudget, billStats ->
        userBudget?.toBudgetConfig(
            billsPaidCount = billStats.paidBills,
            totalBillsCount = billStats.totalBills,
            fixedExpensesFromBills = billStats.totalEstimated,
            savingsFromBills = billStats.savingsFromBills,
            overageFromBills = billStats.overageFromBills
        ) ?: BudgetConfig()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetConfig()
    )
    
    init {
        // Sync user budget to _budgetConfig
        viewModelScope.launch {
            userBudgetFlow.collect { config ->
                _budgetConfig.value = config
            }
        }
    }
    
    // Ghost Mode state - hide rings, pause tracking
    val isGhostModeEnabled = appPreferences.isGhostModeEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    // Recent transactions
    val recentTransactions = transactionRepository.observeRecent(10)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Calculate Vitality Rings state from live data with Rolling Budget
    // Uses NORMAL expenses for Pulse (emergency expenses don't affect daily limit)
    val vitalityRingsState: StateFlow<VitalityRingsState> = combine(
        transactionRepository.observeNormalExpensesToday(),  // Only normal expenses affect daily budget
        transactionRepository.observeTotalSavingsThisMonth(),
        transactionRepository.observeNormalExpensesThisMonth(),  // Only normal for monthly pace
        transactionRepository.observeEmergencyExpensesThisMonth(),  // Track emergency separately
        _budgetConfig
    ) { dailyNormalExpenses, monthlySavings, monthlyNormalExpenses, emergencyExpenses, config ->
        calculateVitalityState(
            dailyExpenses = dailyNormalExpenses,
            monthlySavings = monthlySavings,
            monthlyExpenses = monthlyNormalExpenses,
            emergencyExpenses = emergencyExpenses,
            config = config
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VitalityRingsState.empty()
    )
    
    // UI state for dialogs
    private val _showQuickAddDialog = MutableStateFlow(false)
    val showQuickAddDialog: StateFlow<Boolean> = _showQuickAddDialog.asStateFlow()
    
    private val _showDidntBuyDialog = MutableStateFlow(false)
    val showDidntBuyDialog: StateFlow<Boolean> = _showDidntBuyDialog.asStateFlow()
    
    private val _showBillChecklistDialog = MutableStateFlow(false)
    val showBillChecklistDialog: StateFlow<Boolean> = _showBillChecklistDialog.asStateFlow()
    
    // Snackbar message
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()
    
    // Credits earned today
    val creditsToday = transactionRepository.observeCreditsEarnedToday()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // ==================== VITALITY ALGORITHM ====================
    
    /**
     * Calculate the Vitality Rings state with Rolling Budget logic.
     * 
     * VITALITY SCORE FORMULA (0-100):
     * - 60% weight: Daily Limit Adherence (how well staying within daily budget)
     * - 40% weight: Savings Progress (how full the savings ring is)
     * 
     * ROLLING BUDGET:
     * If daily spending exceeds limit, instead of showing "Over!":
     * - Show "Adjusted" in orange (non-judgmental)
     * - Spread overspent amount across remaining days of month
     * - Recalculate future daily limits
     * 
     * EMERGENCY EXPENSE LOGIC:
     * - Emergency expenses are deducted from Savings (Shield ring)
     * - They do NOT affect the daily Pulse calculation
     * - This protects the user's daily budget for unexpected expenses
     */
    private fun calculateVitalityState(
        dailyExpenses: Double,
        monthlySavings: Double,
        monthlyExpenses: Double,
        emergencyExpenses: Double,
        config: BudgetConfig
    ): VitalityRingsState {
        val today = LocalDate.now()
        val yearMonth = YearMonth.from(today)
        val daysInMonth = yearMonth.lengthOfMonth()
        val dayOfMonth = today.dayOfMonth
        val remainingDaysIncludingToday = daysInMonth - dayOfMonth + 1
        
        // Calculate disposable income for the month (after fixed expenses and savings)
        val monthlyDisposable = config.monthlyIncome - config.totalFixedExpenses - config.savingsGoal
        
        // Calculate what SHOULD have been spent by now (ideal pace)
        val idealMonthlySpendByNow = (monthlyDisposable / daysInMonth) * dayOfMonth
        
        // Calculate overspend/underspend for the month so far
        val monthlyVariance = monthlyExpenses - idealMonthlySpendByNow
        
        // ROLLING BUDGET: Adjust today's allowance based on month's performance
        val baseDaily = monthlyDisposable / daysInMonth
        val adjustedDailyAllowance = if (monthlyVariance > 0) {
            // Over budget: spread deficit across remaining days (softer adjustment)
            val adjustmentPerDay = monthlyVariance / remainingDaysIncludingToday
            max(baseDaily * 0.5, baseDaily - adjustmentPerDay) // Never go below 50% of base
        } else {
            // Under budget: slightly boost daily allowance (reward!)
            val bonusPerDay = (-monthlyVariance) / remainingDaysIncludingToday * 0.5 // Only give 50% as bonus
            baseDaily + bonusPerDay
        }
        
        // PULSE: Daily spending progress
        val pulseProgress = if (adjustedDailyAllowance > 0) {
            (dailyExpenses / adjustedDailyAllowance).toFloat()
        } else 0f
        
        // Determine pulse state - NON-JUDGMENTAL approach
        val isAdjusted = monthlyVariance > baseDaily * 0.2 // More than 20% over monthly pace
        val pulseState = when {
            pulseProgress <= 0.5f -> RingState.EXCELLENT
            pulseProgress <= 0.75f -> RingState.GOOD
            pulseProgress <= 1f -> RingState.WARNING
            isAdjusted -> RingState.ADJUSTED // "Adjusted" not "Over!"
            else -> RingState.WARNING
        }
        
        // Calculate daily adherence score (0-100) for Vitality calculation
        val dailyAdherenceScore = when {
            pulseProgress <= 0.5f -> 100.0
            pulseProgress <= 0.75f -> 90.0
            pulseProgress <= 1f -> 75.0
            pulseProgress <= 1.25f -> 50.0
            else -> max(0.0, 30.0 - (pulseProgress - 1.25) * 20)
        }
        
        // SHIELD: Savings progress
        // Emergency expenses are deducted from available savings
        val effectiveSavings = (monthlySavings - emergencyExpenses).coerceAtLeast(0.0)
        val shieldProgress = if (config.savingsGoal > 0) {
            (effectiveSavings / config.savingsGoal).toFloat()
        } else 0f
        
        val shieldState = when {
            shieldProgress >= 1f -> RingState.COMPLETED
            shieldProgress >= 0.75f -> RingState.EXCELLENT
            shieldProgress >= 0.5f -> RingState.GOOD
            shieldProgress >= 0.25f -> RingState.WARNING
            else -> RingState.INACTIVE
        }
        
        // Calculate savings score (0-100) for Vitality calculation
        val savingsScore = (shieldProgress * 100).toDouble().coerceIn(0.0, 100.0)
        
        // CLARITY: Bills paid progress
        val clarityProgress = config.billsPaidCount.toFloat() / config.totalBillsCount.coerceAtLeast(1)
        val clarityState = when {
            clarityProgress >= 1f -> RingState.COMPLETED
            clarityProgress >= 0.7f -> RingState.GOOD
            clarityProgress >= 0.3f -> RingState.WARNING
            else -> RingState.INACTIVE
        }
        
        // VITALITY SCORE: 60% daily adherence + 40% savings
        val vitalityScore = (dailyAdherenceScore * 0.6 + savingsScore * 0.4).toInt()
        
        // Format sublabels based on state
        val pulseSublabel = if (isAdjusted) {
            "Adjusted: ${formatCurrency(adjustedDailyAllowance, config.currency)}/day"
        } else {
            "${formatCurrency(dailyExpenses, config.currency)} / ${formatCurrency(adjustedDailyAllowance, config.currency)}"
        }
        
        return VitalityRingsState(
            pulse = RingStatus(
                type = RingType.PULSE,
                progress = pulseProgress.coerceIn(0f, 1.5f),
                currentValue = dailyExpenses,
                targetValue = adjustedDailyAllowance,
                state = pulseState,
                label = if (isAdjusted) "Adjusted" else "Pulse",
                sublabel = pulseSublabel,
                currency = config.currency
            ),
            shield = RingStatus(
                type = RingType.SHIELD,
                progress = shieldProgress.coerceIn(0f, 1f),
                currentValue = effectiveSavings,
                targetValue = config.savingsGoal,
                state = shieldState,
                label = if (emergencyExpenses > 0) "Shield (Protected)" else "Shield",
                sublabel = if (emergencyExpenses > 0) {
                    "${formatCurrency(effectiveSavings, config.currency)} / ${formatCurrency(config.savingsGoal, config.currency)} (−${formatCurrency(emergencyExpenses, config.currency)} emergency)"
                } else {
                    "${formatCurrency(effectiveSavings, config.currency)} / ${formatCurrency(config.savingsGoal, config.currency)}"
                },
                currency = config.currency
            ),
            clarity = RingStatus(
                type = RingType.CLARITY,
                progress = clarityProgress,
                currentValue = config.billsPaidCount.toDouble(),
                targetValue = config.totalBillsCount.toDouble(),
                state = clarityState,
                label = "Clarity",
                sublabel = "${config.billsPaidCount}/${config.totalBillsCount} bills paid",
                currency = config.currency
            ),
            streak = config.currentStreak,
            vitalityScore = vitalityScore,
            creditsEarnedToday = 0
        )
    }
    
    private fun formatCurrency(amount: Double, currency: Currency): String {
        return when (currency) {
            Currency.RSD -> "%,.0f %s".format(amount, currency.symbol)
            else -> "%s%,.2f".format(currency.symbol, amount)
        }
    }
    
    // ==================== DIALOG CONTROLS ====================
    
    fun showQuickAdd() {
        _showQuickAddDialog.value = true
    }
    
    fun hideQuickAdd() {
        _showQuickAddDialog.value = false
    }
    
    fun showDidntBuy() {
        _showDidntBuyDialog.value = true
    }
    
    fun hideDidntBuy() {
        _showDidntBuyDialog.value = false
    }
    
    fun showBillChecklist() {
        _showBillChecklistDialog.value = true
    }
    
    fun hideBillChecklist() {
        _showBillChecklistDialog.value = false
    }
    
    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
    
    // ==================== BILL PAYMENT ACTIONS ====================
    
    /**
     * Mark a bill as paid with optional actual amount.
     * 
     * Variable Bill Logic:
     * - If actualAmount < estimatedAmount: difference is a "bill savings"
     * - If actualAmount > estimatedAmount: difference is an "overage" affecting daily budget
     */
    fun markBillAsPaid(billId: Long, actualAmount: Double?) {
        viewModelScope.launch {
            fixedBillRepository.markAsPaid(billId, actualAmount)
            
            // Show feedback message
            val bill = fixedBillRepository.getById(billId)
            bill?.let {
                val estimated = it.estimatedAmount
                val actual = actualAmount ?: estimated
                val difference = actual - estimated
                
                _snackbarMessage.value = when {
                    difference < 0 -> "💰 You saved ${formatCurrency(-difference, _budgetConfig.value.currency)} on ${it.name}!"
                    difference > 0 -> "📊 ${it.name} was ${formatCurrency(difference, _budgetConfig.value.currency)} over budget"
                    else -> "✅ ${it.name} marked as paid"
                }
            }
        }
    }
    
    /**
     * Mark a bill as unpaid (undo payment).
     */
    fun markBillAsUnpaid(billId: Long) {
        viewModelScope.launch {
            fixedBillRepository.markAsUnpaid(billId)
            _snackbarMessage.value = "Bill payment undone"
        }
    }
    
    // ==================== TRANSACTION ACTIONS ====================
    
    /**
     * Quick add a transaction (from FAB - Add Expense)
     * 
     * @param isEmergency If true, expense is deducted from Savings instead of Daily budget.
     *                    This protects the daily Safe-to-Spend limit for unexpected expenses.
     */
    fun quickAddTransaction(
        amount: Double,
        category: TransactionCategory,
        description: String = "",
        isEmergency: Boolean = false
    ) {
        viewModelScope.launch {
            val transaction = Transaction(
                amount = amount,
                category = category,
                description = description.ifBlank { category.displayName },
                type = if (category.isExpense) TransactionType.EXPENSE else TransactionType.INCOME,
                source = TransactionSource.MANUAL,
                creditsEarned = if (isEmergency) 5 else 10, // Less credits for emergency (still rewarded though)
                isEmergency = isEmergency
            )
            transactionRepository.insert(transaction)
            hideQuickAdd()
            _snackbarMessage.value = if (isEmergency) {
                "Emergency expense tracked. Your daily budget is protected 🛡️"
            } else {
                "Expense tracked! +10 credits ✨"
            }
        }
    }
    
    /**
     * Record a "Didn't Buy" micro-savings.
     * This is the anti-spend feature - celebrates NOT buying something.
     */
    fun recordDidntBuy(itemName: String, amount: Double) {
        viewModelScope.launch {
            val transaction = Transaction(
                amount = amount,
                category = TransactionCategory.MICRO_SAVINGS,
                description = "Didn't buy: $itemName",
                type = TransactionType.MICRO_SAVINGS,
                source = TransactionSource.MANUAL,
                creditsEarned = 15 // Extra reward for willpower!
            )
            transactionRepository.insert(transaction)
            hideDidntBuy()
            
            // Celebratory message
            _snackbarMessage.value = "You just paid yourself ${formatCurrency(amount, _budgetConfig.value.currency)}! 🎉"
        }
    }
    
    /**
     * Record a micro-savings (legacy method - kept for compatibility)
     */
    fun recordMicroSavings(amount: Double, description: String) {
        recordDidntBuy(description, amount)
    }
    
    /**
     * Update budget configuration
     */
    fun updateBudgetConfig(config: BudgetConfig) {
        _budgetConfig.value = config
    }
}

/**
 * Budget configuration data class
 * 
 * Now includes data from both UserBudget and FixedBills tables.
 */
data class BudgetConfig(
    val monthlyIncome: Double = 150000.0, // 150k RSD default
    val totalFixedExpenses: Double = 50000.0,
    val savingsGoal: Double = 30000.0,
    val currency: Currency = Currency.RSD,
    val billsPaidCount: Int = 0,
    val totalBillsCount: Int = 0,
    val currentStreak: Int = 0,
    // New fields from FixedBills
    val savingsFromBills: Double = 0.0, // Extra savings from variable bills
    val overageFromBills: Double = 0.0  // Overage from variable bills
)

/**
 * Extension function to convert UserBudget to BudgetConfig
 * Now accepts bill tracking data from FixedBillRepository
 */
fun UserBudget.toBudgetConfig(
    billsPaidCount: Int = 0,
    totalBillsCount: Int = 0,
    fixedExpensesFromBills: Double = 0.0,
    savingsFromBills: Double = 0.0,
    overageFromBills: Double = 0.0
): BudgetConfig {
    // Use bills total if available, otherwise fall back to UserBudget's fixedExpensesTotal
    val effectiveFixedExpenses = if (totalBillsCount > 0) fixedExpensesFromBills else this.fixedExpensesTotal
    
    return BudgetConfig(
        monthlyIncome = this.monthlyIncome,
        totalFixedExpenses = effectiveFixedExpenses,
        savingsGoal = this.savingsTarget,
        currency = this.currency,
        billsPaidCount = billsPaidCount,
        totalBillsCount = totalBillsCount,
        currentStreak = 0, // TODO: Calculate from transaction history
        savingsFromBills = savingsFromBills,
        overageFromBills = overageFromBills
    )
}

/**
 * Helper data class for combining bill statistics flows
 */
private data class BillStats(
    val totalBills: Int,
    val paidBills: Int,
    val totalEstimated: Double,
    val savingsFromBills: Double,
    val overageFromBills: Double
)
