package site.giboworks.budgettracker.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import site.giboworks.budgettracker.domain.model.Transaction
import site.giboworks.budgettracker.domain.repository.TransactionRepository
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for Transaction History Screen
 */
@HiltViewModel
class TransactionHistoryViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    
    // Delete confirmation state
    private val _transactionToDelete = MutableStateFlow<Transaction?>(null)
    val transactionToDelete: StateFlow<Transaction?> = _transactionToDelete.asStateFlow()
    
    // All transactions (sorted by date desc in repository)
    val allTransactions: StateFlow<List<Transaction>> = transactionRepository.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Monthly statistics
    val monthlyStats: StateFlow<MonthlyStats> = combine(
        transactionRepository.observeTotalExpensesThisMonth(),
        transactionRepository.observeTotalSavingsThisMonth(),
        transactionRepository.observeCreditsEarnedThisMonth()
    ) { expenses, savings, credits ->
        val currentMonth = YearMonth.now()
        val monthName = currentMonth.format(
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
        )
        MonthlyStats(
            monthName = monthName,
            totalSpent = expenses,
            totalSaved = savings,
            creditsEarned = credits
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MonthlyStats()
    )
    
    /**
     * Show delete confirmation for a transaction
     */
    fun showDeleteConfirmation(transaction: Transaction) {
        _transactionToDelete.value = transaction
    }
    
    /**
     * Dismiss delete confirmation
     */
    fun dismissDeleteConfirmation() {
        _transactionToDelete.value = null
    }
    
    /**
     * Delete a transaction
     */
    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.delete(transaction)
            _transactionToDelete.value = null
        }
    }
    
    /**
     * Confirm and delete the pending transaction
     */
    fun confirmDelete() {
        _transactionToDelete.value?.let { transaction ->
            deleteTransaction(transaction)
        }
    }
}
