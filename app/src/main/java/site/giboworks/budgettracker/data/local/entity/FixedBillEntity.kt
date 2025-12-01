package site.giboworks.budgettracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import site.giboworks.budgettracker.domain.model.FixedBill

/**
 * Room Entity for Fixed Bills (recurring monthly expenses).
 * 
 * This replaces the simple `fixedExpensesTotal` double with a detailed list
 * of individual bills that users can track and mark as paid each month.
 * 
 * Key features:
 * - Track individual bills by name (Rent, Electricity, Internet, etc.)
 * - Set estimated amounts for budgeting
 * - Mark bills as paid with actual amount (for variable bills)
 * - Auto-reset on pay day
 */
@Entity(tableName = "fixed_bills")
data class FixedBillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /**
     * Bill name (e.g., "Rent", "Electricity", "Internet")
     */
    val name: String,
    
    /**
     * Estimated/budgeted amount for this bill.
     * User sets this as the maximum expected amount.
     */
    val estimatedAmount: Double,
    
    /**
     * Day of month when this bill is typically due (1-31).
     * Used for reminders and sorting.
     */
    val dayDue: Int = 1,
    
    /**
     * Whether this bill has been paid in the current budget cycle.
     * Resets to false on each pay day.
     */
    val isPaidThisMonth: Boolean = false,
    
    /**
     * Actual amount paid for this bill (nullable for variable bills).
     * If null and isPaid=true, estimated amount is used.
     * 
     * Variable Bill Logic:
     * - If actualAmount < estimatedAmount: difference goes to Savings
     * - If actualAmount > estimatedAmount: excess deducted from Daily Limit
     */
    val actualAmountPaid: Double? = null,
    
    /**
     * Timestamp when this bill was last marked as paid.
     */
    val lastPaidAt: Long? = null,
    
    /**
     * Optional icon/emoji for visual identification.
     */
    val icon: String = "📄",
    
    /**
     * Sort order for display.
     */
    val sortOrder: Int = 0,
    
    /**
     * Whether this bill recurs every month.
     * If false, the bill is a one-time expense and should be deleted after payment
     * or excluded from future cycles.
     */
    val isRecurring: Boolean = true,
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Convert to domain model
     */
    fun toDomain(): FixedBill {
        return FixedBill(
            id = id,
            name = name,
            estimatedAmount = estimatedAmount,
            dayDue = dayDue,
            isPaidThisMonth = isPaidThisMonth,
            actualAmountPaid = actualAmountPaid,
            lastPaidAt = lastPaidAt,
            icon = icon,
            isRecurring = isRecurring
        )
    }
    
    companion object {
        /**
         * Create entity from domain model
         */
        fun fromDomain(bill: FixedBill): FixedBillEntity {
            return FixedBillEntity(
                id = bill.id,
                name = bill.name,
                estimatedAmount = bill.estimatedAmount,
                dayDue = bill.dayDue,
                isPaidThisMonth = bill.isPaidThisMonth,
                actualAmountPaid = bill.actualAmountPaid,
                lastPaidAt = bill.lastPaidAt,
                icon = bill.icon,
                isRecurring = bill.isRecurring,
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}
