package site.giboworks.budgettracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import site.giboworks.budgettracker.domain.model.Currency
import site.giboworks.budgettracker.domain.model.RecurringFrequency
import site.giboworks.budgettracker.domain.model.Transaction
import site.giboworks.budgettracker.domain.model.TransactionCategory
import site.giboworks.budgettracker.domain.model.TransactionSource
import site.giboworks.budgettracker.domain.model.TransactionType
import java.time.LocalDateTime
import java.util.UUID

/**
 * Room Entity for Transaction.
 * This is the database representation - separate from domain model for flexibility.
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["category"]),
        Index(value = ["type"]),
        Index(value = ["source"]),
        Index(value = ["isEmergency"])
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    val amount: Double, // Amount in base currency (RSD) for consistent reporting
    val currency: String = Currency.RSD.code,
    val category: String,
    val description: String,
    val timestamp: Long, // Stored as epoch millis for Room compatibility
    val type: String,
    val source: String = TransactionSource.MANUAL.name,
    val isRecurring: Boolean = false,
    val recurringFrequency: String? = null,
    val merchantName: String? = null,
    val location: String? = null,
    val tags: String = "", // Comma-separated for simplicity
    val creditsEarned: Int = 0,
    
    // Original currency tracking for multi-currency support (Balkan focus)
    val originalAmount: Double? = null, // Original amount before conversion (null if RSD)
    val originalCurrency: String? = null, // Original currency code (null if RSD)
    
    /**
     * Emergency transactions don't affect Daily Safe-to-Spend.
     * They are deducted from Savings instead.
     */
    val isEmergency: Boolean = false,
    
    // Audit fields
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null,
    val isDeleted: Boolean = false // Soft delete for sync
) {
    /**
     * Convert to domain model
     */
    fun toDomain(): Transaction {
        return Transaction(
            id = id,
            amount = amount,
            currency = Currency.entries.find { it.code == currency } ?: Currency.RSD,
            category = TransactionCategory.entries.find { it.name == category } 
                ?: TransactionCategory.OTHER_EXPENSE,
            description = description,
            timestamp = LocalDateTime.ofEpochSecond(
                timestamp / 1000, 
                ((timestamp % 1000) * 1_000_000).toInt(),
                java.time.ZoneOffset.systemDefault().rules.getOffset(java.time.Instant.ofEpochMilli(timestamp))
            ),
            type = TransactionType.entries.find { it.name == type } ?: TransactionType.EXPENSE,
            source = TransactionSource.entries.find { it.name == source } ?: TransactionSource.MANUAL,
            isRecurring = isRecurring,
            recurringFrequency = recurringFrequency?.let { 
                RecurringFrequency.entries.find { freq -> freq.name == it } 
            },
            merchantName = merchantName,
            location = location,
            tags = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() },
            creditsEarned = creditsEarned,
            originalAmount = originalAmount,
            originalCurrency = originalCurrency?.let { code ->
                Currency.entries.find { it.code == code }
            },
            isEmergency = isEmergency
        )
    }
    
    companion object {
        /**
         * Create entity from domain model
         */
        fun fromDomain(transaction: Transaction): TransactionEntity {
            val epochMilli = transaction.timestamp
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            
            return TransactionEntity(
                id = transaction.id,
                amount = transaction.amount,
                currency = transaction.currency.code,
                category = transaction.category.name,
                description = transaction.description,
                timestamp = epochMilli,
                type = transaction.type.name,
                source = transaction.source.name,
                isRecurring = transaction.isRecurring,
                recurringFrequency = transaction.recurringFrequency?.name,
                merchantName = transaction.merchantName,
                location = transaction.location,
                tags = transaction.tags.joinToString(","),
                creditsEarned = transaction.creditsEarned,
                originalAmount = transaction.originalAmount,
                originalCurrency = transaction.originalCurrency?.code,
                isEmergency = transaction.isEmergency
            )
        }
    }
}
