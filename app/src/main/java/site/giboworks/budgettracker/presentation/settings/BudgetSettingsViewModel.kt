package site.giboworks.budgettracker.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import site.giboworks.budgettracker.domain.model.Currency
import site.giboworks.budgettracker.domain.model.UserBudget
import site.giboworks.budgettracker.domain.repository.UserBudgetRepository
import javax.inject.Inject

/**
 * UI State for Budget Settings Screen
 */
data class BudgetSettingsUiState(
    val monthlyIncome: String = "",
    val selectedCurrency: Currency = Currency.RSD,
    val payDay: Int = 1,
    val fixedExpenses: String = "",
    val savingsTarget: String = "",
    val dailyAllowancePreview: Double = 0.0,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val existingBudgetId: String? = null
)

/**
 * ViewModel for Budget Settings Screen
 */
@HiltViewModel
class BudgetSettingsViewModel @Inject constructor(
    private val userBudgetRepository: UserBudgetRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BudgetSettingsUiState())
    val uiState: StateFlow<BudgetSettingsUiState> = _uiState.asStateFlow()
    
    val availableCurrencies = listOf(
        Currency.RSD,
        Currency.EUR,
        Currency.USD,
        Currency.BAM
    )
    
    val payDayOptions = (1..28).toList()
    
    init {
        loadExistingBudget()
    }
    
    private fun loadExistingBudget() {
        viewModelScope.launch {
            val existing = userBudgetRepository.getActive()
            if (existing != null) {
                _uiState.update {
                    it.copy(
                        monthlyIncome = existing.monthlyIncome.toLong().toString(),
                        selectedCurrency = existing.currency,
                        payDay = existing.payDay,
                        fixedExpenses = existing.fixedExpensesTotal.toLong().toString(),
                        savingsTarget = existing.savingsTarget.toLong().toString(),
                        dailyAllowancePreview = existing.calculateDailyAllowance(),
                        isLoading = false,
                        existingBudgetId = existing.id
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    fun updateMonthlyIncome(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(monthlyIncome = filtered) }
        calculateDailyAllowancePreview()
    }
    
    fun updateCurrency(currency: Currency) {
        _uiState.update { it.copy(selectedCurrency = currency) }
    }
    
    fun updatePayDay(day: Int) {
        _uiState.update { it.copy(payDay = day.coerceIn(1, 31)) }
        calculateDailyAllowancePreview()
    }
    
    fun updateFixedExpenses(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(fixedExpenses = filtered) }
        calculateDailyAllowancePreview()
    }
    
    fun updateSavingsTarget(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(savingsTarget = filtered) }
        calculateDailyAllowancePreview()
    }
    
    private fun calculateDailyAllowancePreview() {
        val state = _uiState.value
        val income = state.monthlyIncome.toDoubleOrNull() ?: 0.0
        val expenses = state.fixedExpenses.toDoubleOrNull() ?: 0.0
        val savings = state.savingsTarget.toDoubleOrNull() ?: 0.0
        
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
    
    fun save() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            
            try {
                val state = _uiState.value
                
                val userBudget = UserBudget(
                    id = state.existingBudgetId ?: java.util.UUID.randomUUID().toString(),
                    monthlyIncome = state.monthlyIncome.toDoubleOrNull() ?: 0.0,
                    currency = state.selectedCurrency,
                    payDay = state.payDay,
                    fixedExpensesTotal = state.fixedExpenses.toDoubleOrNull() ?: 0.0,
                    savingsTarget = state.savingsTarget.toDoubleOrNull() ?: 0.0
                )
                
                if (state.existingBudgetId != null) {
                    userBudgetRepository.update(userBudget)
                } else {
                    userBudgetRepository.save(userBudget)
                }
                
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isSaved = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
