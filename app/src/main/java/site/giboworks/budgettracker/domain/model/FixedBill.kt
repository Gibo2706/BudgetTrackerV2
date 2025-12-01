package site.giboworks.budgettracker.domain.model

/**
 * Domain model for Fixed Bills (recurring monthly expenses).
 * 
 * Represents individual bills like Rent, Electricity, Internet, etc.
 * that users can track and mark as paid each month.
 */
data class FixedBill(
    val id: Long = 0,
    val name: String,
    val estimatedAmount: Double,
    val dayDue: Int = 1,
    val isPaidThisMonth: Boolean = false,
    val actualAmountPaid: Double? = null,
    val lastPaidAt: Long? = null,
    val icon: String = "📄"
) {
    /**
     * Get the effective paid amount.
     * Returns actual amount if set, otherwise estimated amount.
     */
    fun getEffectivePaidAmount(): Double {
        return if (isPaidThisMonth) {
            actualAmountPaid ?: estimatedAmount
        } else {
            0.0
        }
    }
    
    /**
     * Calculate variance from estimated amount.
     * Positive = over budget, Negative = under budget (savings!)
     * Returns null if not paid yet.
     */
    fun getVariance(): Double? {
        if (!isPaidThisMonth) return null
        val actual = actualAmountPaid ?: return 0.0
        return actual - estimatedAmount
    }
    
    /**
     * Check if this is a variable bill (actual amount differs from estimated).
     */
    fun isVariableBill(): Boolean {
        return isPaidThisMonth && actualAmountPaid != null && actualAmountPaid != estimatedAmount
    }
    
    /**
     * Get savings from this bill (if actual < estimated).
     */
    fun getSavingsFromBill(): Double {
        val variance = getVariance() ?: return 0.0
        return if (variance < 0) -variance else 0.0
    }
    
    /**
     * Get overage from this bill (if actual > estimated).
     */
    fun getOverageFromBill(): Double {
        val variance = getVariance() ?: return 0.0
        return if (variance > 0) variance else 0.0
    }
    
    companion object {
        /**
         * Common bill presets with suggested icons
         */
        val PRESETS = listOf(
            FixedBill(name = "Rent", estimatedAmount = 0.0, icon = "🏠"),
            FixedBill(name = "Electricity", estimatedAmount = 0.0, icon = "⚡"),
            FixedBill(name = "Gas", estimatedAmount = 0.0, icon = "🔥"),
            FixedBill(name = "Water", estimatedAmount = 0.0, icon = "💧"),
            FixedBill(name = "Internet", estimatedAmount = 0.0, icon = "📶"),
            FixedBill(name = "Phone", estimatedAmount = 0.0, icon = "📱"),
            FixedBill(name = "Insurance", estimatedAmount = 0.0, icon = "🛡️"),
            FixedBill(name = "Subscription", estimatedAmount = 0.0, icon = "📺"),
            FixedBill(name = "Gym", estimatedAmount = 0.0, icon = "💪"),
            FixedBill(name = "Other", estimatedAmount = 0.0, icon = "📄")
        )
        
        /**
         * Get icon suggestion based on bill name
         */
        fun suggestIcon(name: String): String {
            val lowerName = name.lowercase()
            return when {
                lowerName.contains("rent") || lowerName.contains("mortgage") -> "🏠"
                lowerName.contains("electric") -> "⚡"
                lowerName.contains("gas") || lowerName.contains("heating") -> "🔥"
                lowerName.contains("water") -> "💧"
                lowerName.contains("internet") || lowerName.contains("wifi") -> "📶"
                lowerName.contains("phone") || lowerName.contains("mobile") -> "📱"
                lowerName.contains("insurance") -> "🛡️"
                lowerName.contains("netflix") || lowerName.contains("spotify") || 
                    lowerName.contains("subscription") -> "📺"
                lowerName.contains("gym") || lowerName.contains("fitness") -> "💪"
                lowerName.contains("car") || lowerName.contains("transport") -> "🚗"
                lowerName.contains("loan") || lowerName.contains("credit") -> "🏦"
                else -> "📄"
            }
        }
    }
}

/**
 * Summary of all fixed bills for display purposes.
 */
data class BillsSummary(
    val totalBills: Int,
    val paidBills: Int,
    val totalEstimated: Double,
    val totalActualPaid: Double,
    val totalSavingsFromBills: Double,
    val totalOverageFromBills: Double
) {
    val unpaidBills: Int get() = totalBills - paidBills
    val completionPercentage: Float get() = if (totalBills > 0) paidBills.toFloat() / totalBills else 0f
    val netVariance: Double get() = totalSavingsFromBills - totalOverageFromBills
    
    companion object {
        fun fromBills(bills: List<FixedBill>): BillsSummary {
            return BillsSummary(
                totalBills = bills.size,
                paidBills = bills.count { it.isPaidThisMonth },
                totalEstimated = bills.sumOf { it.estimatedAmount },
                totalActualPaid = bills.sumOf { it.getEffectivePaidAmount() },
                totalSavingsFromBills = bills.sumOf { it.getSavingsFromBill() },
                totalOverageFromBills = bills.sumOf { it.getOverageFromBill() }
            )
        }
        
        fun empty() = BillsSummary(0, 0, 0.0, 0.0, 0.0, 0.0)
    }
}
