package site.giboworks.budgettracker.domain.repository

import kotlinx.coroutines.flow.Flow
import site.giboworks.budgettracker.domain.model.BillsSummary
import site.giboworks.budgettracker.domain.model.FixedBill

/**
 * Repository interface for FixedBill operations.
 * 
 * Abstracts data access for fixed monthly bills management.
 */
interface FixedBillRepository {
    
    // ==================== OBSERVE ====================
    
    /**
     * Observe all fixed bills ordered by due date.
     */
    fun observeAll(): Flow<List<FixedBill>>
    
    /**
     * Observe unpaid bills for this month.
     */
    fun observeUnpaid(): Flow<List<FixedBill>>
    
    /**
     * Observe paid bills for this month.
     */
    fun observePaid(): Flow<List<FixedBill>>
    
    /**
     * Observe recurring bills only.
     */
    fun observeRecurring(): Flow<List<FixedBill>>
    
    /**
     * Observe one-time (non-recurring) bills only.
     */
    fun observeOneTime(): Flow<List<FixedBill>>
    
    /**
     * Observe a single bill by ID.
     */
    fun observeById(id: Long): Flow<FixedBill?>
    
    /**
     * Observe total count of bills.
     */
    fun observeTotalCount(): Flow<Int>
    
    /**
     * Observe count of paid bills.
     */
    fun observePaidCount(): Flow<Int>
    
    /**
     * Observe total estimated amount of all bills.
     */
    fun observeTotalEstimated(): Flow<Double>
    
    /**
     * Observe total actual amount paid this month.
     */
    fun observeTotalActualPaid(): Flow<Double>
    
    /**
     * Observe savings from variable bills (where actual < estimated).
     */
    fun observeSavingsFromBills(): Flow<Double>
    
    /**
     * Observe overage from variable bills (where actual > estimated).
     */
    fun observeOverageFromBills(): Flow<Double>
    
    // ==================== GET ====================
    
    /**
     * Get all fixed bills (non-reactive).
     */
    suspend fun getAll(): List<FixedBill>
    
    /**
     * Get a single bill by ID.
     */
    suspend fun getById(id: Long): FixedBill?
    
    /**
     * Get total estimated amount (non-reactive).
     */
    suspend fun getTotalEstimated(): Double
    
    /**
     * Get bills summary.
     */
    suspend fun getBillsSummary(): BillsSummary
    
    // ==================== MUTATE ====================
    
    /**
     * Insert a new bill.
     * @return The ID of the inserted bill.
     */
    suspend fun insert(bill: FixedBill): Long
    
    /**
     * Insert multiple bills at once.
     */
    suspend fun insertAll(bills: List<FixedBill>)
    
    /**
     * Update an existing bill.
     */
    suspend fun update(bill: FixedBill)
    
    /**
     * Delete a bill.
     */
    suspend fun delete(bill: FixedBill)
    
    /**
     * Delete a bill by ID.
     */
    suspend fun deleteById(id: Long)
    
    /**
     * Delete all bills.
     */
    suspend fun deleteAll()
    
    // ==================== PAYMENT OPERATIONS ====================
    
    /**
     * Mark a bill as paid with actual amount.
     * 
     * @param billId The ID of the bill to mark as paid.
     * @param actualAmount The actual amount paid. If null, uses estimated amount.
     */
    suspend fun markAsPaid(billId: Long, actualAmount: Double?)
    
    /**
     * Mark a bill as unpaid (undo payment).
     */
    suspend fun markAsUnpaid(billId: Long)
    
    /**
     * Reset all bills to unpaid state for new budget cycle.
     * Should be called on pay day. Only resets recurring bills.
     */
    suspend fun resetAllBillsForNewCycle()
    
    /**
     * Delete all paid one-time bills (cleanup after cycle).
     */
    suspend fun deletePaidOneTimeBills()
}
