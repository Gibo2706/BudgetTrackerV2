package site.giboworks.budgettracker.presentation.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import site.giboworks.budgettracker.data.preferences.AppPreferences
import site.giboworks.budgettracker.domain.model.Currency
import site.giboworks.budgettracker.domain.model.UserBudget
import site.giboworks.budgettracker.domain.repository.UserBudgetRepository
import javax.inject.Inject

/**
 * UI State for Settings Screen
 */
data class SettingsUiState(
    // Blueprint section
    val monthlyIncome: String = "",
    val selectedCurrency: Currency = Currency.RSD,
    val payDay: Int = 1,
    val fixedExpenses: String = "",
    val savingsGoal: String = "",
    val dailyAllowancePreview: Double = 0.0,
    
    // Well-being section
    val isGhostModeEnabled: Boolean = false,
    
    // State
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val existingBudgetId: String? = null
)

/**
 * ViewModel for the Settings Screen.
 * Manages budget configuration and app preferences.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userBudgetRepository: UserBudgetRepository,
    private val appPreferences: AppPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    // Observe ghost mode from DataStore
    val isGhostModeEnabled = appPreferences.isGhostModeEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    val availableCurrencies = listOf(
        Currency.RSD,
        Currency.EUR,
        Currency.USD,
        Currency.BAM
    )
    
    val payDayOptions = (1..28).toList()
    
    init {
        loadSettings()
        observeGhostMode()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            val existing = userBudgetRepository.getActive()
            if (existing != null) {
                _uiState.update {
                    it.copy(
                        monthlyIncome = existing.monthlyIncome.toLong().toString(),
                        selectedCurrency = existing.currency,
                        payDay = existing.payDay,
                        fixedExpenses = existing.fixedExpensesTotal.toLong().toString(),
                        savingsGoal = existing.savingsTarget.toLong().toString(),
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
    
    private fun observeGhostMode() {
        viewModelScope.launch {
            appPreferences.isGhostModeEnabled.collect { enabled ->
                _uiState.update { it.copy(isGhostModeEnabled = enabled) }
            }
        }
    }
    
    // ========== BLUEPRINT UPDATES ==========
    
    fun updateMonthlyIncome(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(monthlyIncome = filtered) }
        calculateDailyAllowancePreview()
    }
    
    fun updateCurrency(currency: Currency) {
        _uiState.update { it.copy(selectedCurrency = currency) }
    }
    
    fun updatePayDay(day: Int) {
        _uiState.update { it.copy(payDay = day.coerceIn(1, 28)) }
        calculateDailyAllowancePreview()
    }
    
    fun updateFixedExpenses(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(fixedExpenses = filtered) }
        calculateDailyAllowancePreview()
    }
    
    fun updateSavingsGoal(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(savingsGoal = filtered) }
        calculateDailyAllowancePreview()
    }
    
    private fun calculateDailyAllowancePreview() {
        val state = _uiState.value
        val income = state.monthlyIncome.toDoubleOrNull() ?: 0.0
        val expenses = state.fixedExpenses.toDoubleOrNull() ?: 0.0
        val savings = state.savingsGoal.toDoubleOrNull() ?: 0.0
        
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
    
    fun saveBlueprint() {
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
                    savingsTarget = state.savingsGoal.toDoubleOrNull() ?: 0.0
                )
                
                if (state.existingBudgetId != null) {
                    userBudgetRepository.update(userBudget)
                } else {
                    userBudgetRepository.save(userBudget)
                }
                
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveSuccess = true,
                        existingBudgetId = userBudget.id
                    )
                }
                
                // Reset success flag after delay
                kotlinx.coroutines.delay(2000)
                _uiState.update { it.copy(saveSuccess = false) }
                
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
    
    // ========== WELL-BEING (GHOST MODE) ==========
    
    fun toggleGhostMode(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setGhostModeEnabled(enabled)
        }
    }
    
    // ========== NOTIFICATION SETTINGS ==========
    
    fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    fun openAppNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        context.startActivity(intent)
    }
}
