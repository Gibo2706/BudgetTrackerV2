package site.giboworks.budgettracker.domain.usecase

import site.giboworks.budgettracker.domain.model.Transaction
import site.giboworks.budgettracker.domain.model.TransactionCategory
import site.giboworks.budgettracker.domain.model.TransactionSource
import site.giboworks.budgettracker.domain.model.TransactionType
import site.giboworks.budgettracker.domain.repository.TransactionRepository
import site.giboworks.budgettracker.domain.repository.UserProfileRepository
import javax.inject.Inject

/**
 * Use case for adding a new transaction with gamification rewards.
 */
class AddTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val userProfileRepository: UserProfileRepository
) {
    /**
     * Add a transaction and calculate credits earned
     */
    suspend operator fun invoke(
        amount: Double,
        category: TransactionCategory,
        description: String,
        type: TransactionType = if (category.isExpense) TransactionType.EXPENSE else TransactionType.INCOME,
        source: TransactionSource = TransactionSource.MANUAL,
        merchantName: String? = null
    ): Result<Transaction> {
        return try {
            // Calculate credits based on action
            val creditsEarned = calculateCredits(source, type, category)
            
            val transaction = Transaction(
                amount = amount,
                category = category,
                description = description,
                type = type,
                source = source,
                merchantName = merchantName,
                creditsEarned = creditsEarned
            )
            
            // Save transaction
            transactionRepository.insert(transaction)
            
            // Update user stats
            userProfileRepository.addCredits(creditsEarned)
            userProfileRepository.incrementTransactionCount()
            
            Result.success(transaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Calculate credits earned based on transaction properties
     */
    private fun calculateCredits(
        source: TransactionSource,
        type: TransactionType,
        category: TransactionCategory
    ): Int {
        var credits = 0
        
        // Base credits for logging
        credits += when (source) {
            TransactionSource.MANUAL -> 10      // Reward manual entry
            TransactionSource.NOTIFICATION -> 5  // Small bonus for auto-capture
            TransactionSource.SMS -> 5
            TransactionSource.RECURRING -> 0     // No bonus for auto-generated
            TransactionSource.IMPORT -> 2
        }
        
        // Bonus for savings-related transactions
        credits += when (type) {
            TransactionType.SAVINGS -> 15
            TransactionType.INVESTMENT -> 20
            TransactionType.MICRO_SAVINGS -> 25  // Big reward for willpower!
            else -> 0
        }
        
        // Category-specific bonuses (encourage tracking certain categories)
        if (category == TransactionCategory.FOOD_GROCERIES ||
            category == TransactionCategory.UTILITIES_ELECTRICITY ||
            category == TransactionCategory.UTILITIES_WATER) {
            credits += 5 // Bonus for essential expense tracking
        }
        
        return credits
    }
}

/**
 * Use case for recording a micro-savings (deciding not to buy something)
 */
class RecordMicroSavingsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val userProfileRepository: UserProfileRepository
) {
    suspend operator fun invoke(
        amount: Double,
        description: String
    ): Result<Transaction> {
        return try {
            val transaction = Transaction(
                amount = amount,
                category = TransactionCategory.MICRO_SAVINGS,
                description = description,
                type = TransactionType.MICRO_SAVINGS,
                source = TransactionSource.MANUAL,
                creditsEarned = 25 // Generous reward for self-control!
            )
            
            transactionRepository.insert(transaction)
            userProfileRepository.addCredits(25)
            
            Result.success(transaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
