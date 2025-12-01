package site.giboworks.budgettracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import site.giboworks.budgettracker.data.local.entity.FixedBillEntity

/**
 * Data Access Object for FixedBill operations.
 * 
 * Provides CRUD operations and queries for managing fixed monthly bills.
 */
@Dao
interface FixedBillDao {
    
    // ==================== INSERT / UPDATE / DELETE ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bill: FixedBillEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bills: List<FixedBillEntity>)
    
    @Update
    suspend fun update(bill: FixedBillEntity)
    
    @Delete
    suspend fun delete(bill: FixedBillEntity)
    
    @Query("DELETE FROM fixed_bills WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM fixed_bills")
    suspend fun deleteAll()
    
    // ==================== QUERIES ====================
    
    /**
     * Get all fixed bills ordered by due date then sort order.
     */
    @Query("SELECT * FROM fixed_bills ORDER BY dayDue ASC, sortOrder ASC")
    fun observeAll(): Flow<List<FixedBillEntity>>
    
    /**
     * Get all fixed bills (non-reactive).
     */
    @Query("SELECT * FROM fixed_bills ORDER BY dayDue ASC, sortOrder ASC")
    suspend fun getAll(): List<FixedBillEntity>
    
    /**
     * Get a single bill by ID.
     */
    @Query("SELECT * FROM fixed_bills WHERE id = :id")
    suspend fun getById(id: Long): FixedBillEntity?
    
    /**
     * Observe a single bill by ID.
     */
    @Query("SELECT * FROM fixed_bills WHERE id = :id")
    fun observeById(id: Long): Flow<FixedBillEntity?>
    
    /**
     * Get all unpaid bills for this month.
     */
    @Query("SELECT * FROM fixed_bills WHERE isPaidThisMonth = 0 ORDER BY dayDue ASC")
    fun observeUnpaid(): Flow<List<FixedBillEntity>>
    
    /**
     * Get all paid bills for this month.
     */
    @Query("SELECT * FROM fixed_bills WHERE isPaidThisMonth = 1 ORDER BY dayDue ASC")
    fun observePaid(): Flow<List<FixedBillEntity>>
    
    /**
     * Get count of all bills.
     */
    @Query("SELECT COUNT(*) FROM fixed_bills")
    fun observeTotalCount(): Flow<Int>
    
    /**
     * Get count of paid bills this month.
     */
    @Query("SELECT COUNT(*) FROM fixed_bills WHERE isPaidThisMonth = 1")
    fun observePaidCount(): Flow<Int>
    
    /**
     * Get total estimated amount of all bills.
     */
    @Query("SELECT COALESCE(SUM(estimatedAmount), 0.0) FROM fixed_bills")
    fun observeTotalEstimated(): Flow<Double>
    
    /**
     * Get total estimated amount (non-reactive).
     */
    @Query("SELECT COALESCE(SUM(estimatedAmount), 0.0) FROM fixed_bills")
    suspend fun getTotalEstimated(): Double
    
    /**
     * Get total actual amount paid this month.
     * For bills with null actualAmountPaid, uses estimatedAmount.
     */
    @Query("""
        SELECT COALESCE(SUM(
            CASE 
                WHEN isPaidThisMonth = 1 AND actualAmountPaid IS NOT NULL THEN actualAmountPaid
                WHEN isPaidThisMonth = 1 THEN estimatedAmount
                ELSE 0
            END
        ), 0.0) FROM fixed_bills
    """)
    fun observeTotalActualPaid(): Flow<Double>
    
    /**
     * Mark a bill as paid with actual amount.
     */
    @Query("""
        UPDATE fixed_bills 
        SET isPaidThisMonth = 1, 
            actualAmountPaid = :actualAmount,
            lastPaidAt = :timestamp,
            updatedAt = :timestamp
        WHERE id = :billId
    """)
    suspend fun markAsPaid(billId: Long, actualAmount: Double?, timestamp: Long = System.currentTimeMillis())
    
    /**
     * Mark a bill as unpaid (undo payment).
     */
    @Query("""
        UPDATE fixed_bills 
        SET isPaidThisMonth = 0, 
            actualAmountPaid = NULL,
            updatedAt = :timestamp
        WHERE id = :billId
    """)
    suspend fun markAsUnpaid(billId: Long, timestamp: Long = System.currentTimeMillis())
    
    /**
     * Reset all bills to unpaid state (called on pay day).
     */
    @Query("""
        UPDATE fixed_bills 
        SET isPaidThisMonth = 0, 
            actualAmountPaid = NULL,
            updatedAt = :timestamp
    """)
    suspend fun resetAllBillsForNewCycle(timestamp: Long = System.currentTimeMillis())
    
    /**
     * Get total savings from variable bills (where actual < estimated).
     */
    @Query("""
        SELECT COALESCE(SUM(estimatedAmount - actualAmountPaid), 0.0) 
        FROM fixed_bills 
        WHERE isPaidThisMonth = 1 
          AND actualAmountPaid IS NOT NULL 
          AND actualAmountPaid < estimatedAmount
    """)
    fun observeSavingsFromBills(): Flow<Double>
    
    /**
     * Get total overage from variable bills (where actual > estimated).
     */
    @Query("""
        SELECT COALESCE(SUM(actualAmountPaid - estimatedAmount), 0.0) 
        FROM fixed_bills 
        WHERE isPaidThisMonth = 1 
          AND actualAmountPaid IS NOT NULL 
          AND actualAmountPaid > estimatedAmount
    """)
    fun observeOverageFromBills(): Flow<Double>
}
