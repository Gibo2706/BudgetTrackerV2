package site.giboworks.budgettracker.presentation.settings

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
import site.giboworks.budgettracker.domain.model.FixedBill
import site.giboworks.budgettracker.domain.repository.FixedBillRepository
import site.giboworks.budgettracker.domain.repository.UserBudgetRepository
import javax.inject.Inject

/**
 * UI State for Manage Bills Screen
 */
data class ManageBillsUiState(
    val bills: List<FixedBill> = emptyList(),
    val recurringBillsCount: Int = 0,
    val oneTimeBillsCount: Int = 0,
    val totalEstimated: Double = 0.0,
    val currencySymbol: String = "$",
    val isLoading: Boolean = false,
    val showAddEditDialog: Boolean = false,
    val editingBill: FixedBill? = null,
    val showDeleteConfirmation: Boolean = false,
    val billToDelete: FixedBill? = null
)

/**
 * ViewModel for managing fixed bills (CRUD operations).
 */
@HiltViewModel
class ManageBillsViewModel @Inject constructor(
    private val fixedBillRepository: FixedBillRepository,
    private val userBudgetRepository: UserBudgetRepository
) : ViewModel() {
    
    private val _showAddEditDialog = MutableStateFlow(false)
    private val _editingBill = MutableStateFlow<FixedBill?>(null)
    private val _showDeleteConfirmation = MutableStateFlow(false)
    private val _billToDelete = MutableStateFlow<FixedBill?>(null)
    
    val uiState: StateFlow<ManageBillsUiState> = combine(
        fixedBillRepository.observeAll(),
        userBudgetRepository.observeActive(),
        _showAddEditDialog,
        _editingBill,
        _showDeleteConfirmation
    ) { bills, userBudget, showDialog, editingBill, showDeleteConfirm ->
        ManageBillsUiState(
            bills = bills,
            recurringBillsCount = bills.count { it.isRecurring },
            oneTimeBillsCount = bills.count { !it.isRecurring },
            totalEstimated = bills.sumOf { it.estimatedAmount },
            currencySymbol = userBudget?.currency?.symbol ?: "$",
            isLoading = false,
            showAddEditDialog = showDialog,
            editingBill = editingBill,
            showDeleteConfirmation = showDeleteConfirm,
            billToDelete = _billToDelete.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ManageBillsUiState(isLoading = true)
    )
    
    // ==================== DIALOG MANAGEMENT ====================
    
    /**
     * Show the add bill dialog (new bill)
     */
    fun showAddDialog() {
        _editingBill.value = null
        _showAddEditDialog.value = true
    }
    
    /**
     * Show the edit bill dialog with existing bill
     */
    fun showEditDialog(bill: FixedBill) {
        _editingBill.value = bill
        _showAddEditDialog.value = true
    }
    
    /**
     * Dismiss the add/edit dialog
     */
    fun dismissAddEditDialog() {
        _showAddEditDialog.value = false
        _editingBill.value = null
    }
    
    /**
     * Show delete confirmation dialog
     */
    fun showDeleteConfirmation(bill: FixedBill) {
        _billToDelete.value = bill
        _showDeleteConfirmation.value = true
    }
    
    /**
     * Dismiss delete confirmation dialog
     */
    fun dismissDeleteConfirmation() {
        _showDeleteConfirmation.value = false
        _billToDelete.value = null
    }
    
    // ==================== CRUD OPERATIONS ====================
    
    /**
     * Add a new bill
     */
    fun addBill(
        name: String,
        estimatedAmount: Double,
        dayDue: Int,
        icon: String,
        isRecurring: Boolean
    ) {
        viewModelScope.launch {
            val newBill = FixedBill(
                name = name.trim(),
                estimatedAmount = estimatedAmount,
                dayDue = dayDue,
                icon = icon,
                isRecurring = isRecurring
            )
            fixedBillRepository.insert(newBill)
            dismissAddEditDialog()
        }
    }
    
    /**
     * Update an existing bill
     */
    fun updateBill(
        bill: FixedBill,
        name: String,
        estimatedAmount: Double,
        dayDue: Int,
        icon: String,
        isRecurring: Boolean
    ) {
        viewModelScope.launch {
            val updatedBill = bill.copy(
                name = name.trim(),
                estimatedAmount = estimatedAmount,
                dayDue = dayDue,
                icon = icon,
                isRecurring = isRecurring
            )
            fixedBillRepository.update(updatedBill)
            dismissAddEditDialog()
        }
    }
    
    /**
     * Delete a bill
     */
    fun deleteBill(bill: FixedBill) {
        viewModelScope.launch {
            fixedBillRepository.delete(bill)
            dismissDeleteConfirmation()
        }
    }
    
    /**
     * Confirm deletion of the pending bill
     */
    fun confirmDelete() {
        _billToDelete.value?.let { bill ->
            deleteBill(bill)
        }
    }
}
