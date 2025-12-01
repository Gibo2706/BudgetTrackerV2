package site.giboworks.budgettracker.presentation.onboarding

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import site.giboworks.budgettracker.data.preferences.AppPreferences
import site.giboworks.budgettracker.domain.model.Currency
import site.giboworks.budgettracker.domain.model.FixedBill
import site.giboworks.budgettracker.domain.model.UserBudget
import site.giboworks.budgettracker.domain.repository.FixedBillRepository
import site.giboworks.budgettracker.domain.repository.UserBudgetRepository
import java.util.UUID
import javax.inject.Inject

/**
 * UI State for a single bill entry in onboarding
 */
data class BillEntry(
    val id: Long = System.currentTimeMillis(),
    val name: String = "",
    val amount: String = "",
    val icon: String = "📄"
)

/**
 * UI State for the Onboarding wizard
 */
data class OnboardingUiState(
    val currentStep: Int = 0,
    val totalSteps: Int = 4,
    
    // Step 1: Welcome / Permission
    val notificationPermissionGranted: Boolean = false,
    
    // Step 2: Income & Pay Day
    val monthlyIncome: String = "",
    val selectedCurrency: Currency = Currency.RSD,
    val payDay: Int = 1,
    
    // Step 3: Bills & Savings (Updated to use dynamic bill list)
    val bills: List<BillEntry> = listOf(BillEntry()), // Start with one empty bill
    val savingsTarget: String = "",
    
    // Computed from bills list
    val fixedExpensesTotal: Double = 0.0,
    
    // Calculated preview (Step 3)
    val dailyAllowancePreview: Double = 0.0,
    
    // Validation
    val incomeError: String? = null,
    val expensesError: String? = null,
    val savingsError: String? = null,
    
    // Loading state
    val isSaving: Boolean = false,
    val isComplete: Boolean = false
)

/**
 * ViewModel for the Onboarding wizard flow.
 * Manages multi-step form state and validates user input.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userBudgetRepository: UserBudgetRepository,
    private val fixedBillRepository: FixedBillRepository,
    private val appPreferences: AppPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()
    
    init {
        checkNotificationPermission()
    }
    
    // ==================== NAVIGATION ====================
    
    fun nextStep() {
        val currentState = _uiState.value
        
        // Validate current step before proceeding
        when (currentState.currentStep) {
            1 -> if (!validateIncomeStep()) return
            2 -> if (!validateExpensesStep()) return
        }
        
        if (currentState.currentStep < currentState.totalSteps - 1) {
            _uiState.update { it.copy(currentStep = it.currentStep + 1) }
            
            // Calculate preview when entering step 3
            if (_uiState.value.currentStep == 2) {
                calculateDailyAllowancePreview()
            }
        }
    }
    
    fun previousStep() {
        if (_uiState.value.currentStep > 0) {
            _uiState.update { it.copy(currentStep = it.currentStep - 1) }
        }
    }
    
    fun goToStep(step: Int) {
        if (step in 0 until _uiState.value.totalSteps) {
            _uiState.update { it.copy(currentStep = step) }
        }
    }
    
    // ==================== STEP 1: PERMISSIONS ====================
    
    fun checkNotificationPermission() {
        val hasPermission = isNotificationListenerEnabled()
        _uiState.update { it.copy(notificationPermissionGranted = hasPermission) }
    }
    
    private fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return flat?.contains(context.packageName) == true
    }
    
    fun openNotificationListenerSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    // ==================== STEP 2: INCOME ====================
    
    fun updateMonthlyIncome(value: String) {
        // Filter to allow only numbers and one decimal point
        val filtered = value.filter { it.isDigit() || it == '.' }
        _uiState.update { 
            it.copy(
                monthlyIncome = filtered,
                incomeError = null
            ) 
        }
    }
    
    fun updateCurrency(currency: Currency) {
        _uiState.update { it.copy(selectedCurrency = currency) }
    }
    
    fun updatePayDay(day: Int) {
        _uiState.update { it.copy(payDay = day.coerceIn(1, 31)) }
    }
    
    private fun validateIncomeStep(): Boolean {
        val income = _uiState.value.monthlyIncome.toDoubleOrNull()
        
        if (income == null || income <= 0) {
            _uiState.update { it.copy(incomeError = "Please enter a valid income amount") }
            return false
        }
        
        return true
    }
    
    // ==================== STEP 3: BILLS & SAVINGS ====================
    
    /**
     * Add a new empty bill entry to the list
     */
    fun addBill() {
        _uiState.update { state ->
            state.copy(
                bills = state.bills + BillEntry(id = System.currentTimeMillis()),
                expensesError = null
            )
        }
    }
    
    /**
     * Remove a bill entry by ID
     */
    fun removeBill(billId: Long) {
        _uiState.update { state ->
            val updatedBills = state.bills.filter { it.id != billId }
            // Ensure at least one bill entry remains
            val finalBills = if (updatedBills.isEmpty()) listOf(BillEntry()) else updatedBills
            state.copy(
                bills = finalBills,
                fixedExpensesTotal = calculateBillsTotal(finalBills),
                expensesError = null
            )
        }
        calculateDailyAllowancePreview()
    }
    
    /**
     * Update a bill's name
     */
    fun updateBillName(billId: Long, name: String) {
        _uiState.update { state ->
            val icon = FixedBill.suggestIcon(name)
            state.copy(
                bills = state.bills.map { bill ->
                    if (bill.id == billId) bill.copy(name = name, icon = icon) else bill
                },
                expensesError = null
            )
        }
    }
    
    /**
     * Update a bill's amount
     */
    fun updateBillAmount(billId: Long, amount: String) {
        val filtered = amount.filter { it.isDigit() || it == '.' }
        _uiState.update { state ->
            val updatedBills = state.bills.map { bill ->
                if (bill.id == billId) bill.copy(amount = filtered) else bill
            }
            state.copy(
                bills = updatedBills,
                fixedExpensesTotal = calculateBillsTotal(updatedBills),
                expensesError = null
            )
        }
        calculateDailyAllowancePreview()
    }
    
    /**
     * Calculate total from all bills
     */
    private fun calculateBillsTotal(bills: List<BillEntry>): Double {
        return bills.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    }
    
    /**
     * Legacy method for backwards compatibility
     */
    fun updateFixedExpenses(value: String) {
        // No longer used - bills are managed individually
        calculateDailyAllowancePreview()
    }
    
    fun updateSavingsTarget(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
        _uiState.update { 
            it.copy(
                savingsTarget = filtered,
                savingsError = null
            ) 
        }
        calculateDailyAllowancePreview()
    }
    
    private fun validateExpensesStep(): Boolean {
        val state = _uiState.value
        val income = state.monthlyIncome.toDoubleOrNull() ?: 0.0
        val expenses = state.fixedExpensesTotal
        val savings = state.savingsTarget.toDoubleOrNull() ?: 0.0
        
        if (expenses + savings >= income) {
            _uiState.update { 
                it.copy(expensesError = "Fixed expenses + savings cannot exceed income") 
            }
            return false
        }
        
        return true
    }
    
    private fun calculateDailyAllowancePreview() {
        val state = _uiState.value
        val income = state.monthlyIncome.toDoubleOrNull() ?: 0.0
        val expenses = state.fixedExpensesTotal
        val savings = state.savingsTarget.toDoubleOrNull() ?: 0.0
        
        // Create temporary UserBudget to calculate
        val tempBudget = UserBudget(
            id = "temp",
            monthlyIncome = income,
            currency = state.selectedCurrency,
            payDay = state.payDay,
            fixedExpensesTotal = expenses,
            savingsTarget = savings
        )
        
        val dailyAllowance = tempBudget.calculateDailyAllowance()
        _uiState.update { it.copy(dailyAllowancePreview = dailyAllowance) }
    }
    
    // ==================== STEP 4: COMPLETE ====================
    
    fun completeOnboarding() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            
            try {
                val state = _uiState.value
                
                val userBudget = UserBudget(
                    id = UUID.randomUUID().toString(),
                    monthlyIncome = state.monthlyIncome.toDoubleOrNull() ?: 0.0,
                    currency = state.selectedCurrency,
                    payDay = state.payDay,
                    fixedExpensesTotal = state.fixedExpensesTotal,
                    savingsTarget = state.savingsTarget.toDoubleOrNull() ?: 0.0
                )
                
                // Save to Room database
                userBudgetRepository.save(userBudget)
                
                // Save individual bills to Room database
                val validBills = state.bills
                    .filter { it.name.isNotBlank() && (it.amount.toDoubleOrNull() ?: 0.0) > 0 }
                    .map { entry ->
                        FixedBill(
                            name = entry.name,
                            estimatedAmount = entry.amount.toDoubleOrNull() ?: 0.0,
                            dayDue = state.payDay, // Default to pay day
                            icon = entry.icon
                        )
                    }
                
                if (validBills.isNotEmpty()) {
                    fixedBillRepository.insertAll(validBills)
                }
                
                // Mark onboarding as complete in preferences (fast check on startup)
                appPreferences.setOnboardingCompleted(true)
                
                _uiState.update { 
                    it.copy(
                        isSaving = false,
                        isComplete = true
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                // Handle error - could add error state here
            }
        }
    }
    
    // ==================== CURRENCY HELPERS ====================
    
    val availableCurrencies = listOf(
        Currency.RSD,
        Currency.EUR,
        Currency.USD,
        Currency.BAM
    )
    
    val payDayOptions = (1..28).toList() // 1-28 to avoid month-end complications
}
