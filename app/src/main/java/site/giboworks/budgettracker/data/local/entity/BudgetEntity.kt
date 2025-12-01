package site.giboworks.budgettracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import site.giboworks.budgettracker.domain.model.Budget
import site.giboworks.budgettracker.domain.model.Currency
import site.giboworks.budgettracker.domain.model.FixedExpense
import site.giboworks.budgettracker.domain.model.TransactionCategory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.YearMonth
import java.util.UUID

/**
 * Room Entity for Budget configuration
 */
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    val monthlyIncome: Double,
    val fixedExpensesJson: String = "[]", // Serialized as JSON
    val savingsGoalPercentage: Float = 20f,
    val savingsGoalAmount: Double? = null,
    val currency: String = Currency.RSD.code,
    val yearMonth: String, // Format: "2025-12"
    val emergencyFundTarget: Double? = null,
    val categoryBudgetsJson: String = "{}", // Serialized as JSON
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Budget {
        val gson = Gson()
        
        val fixedExpenses: List<FixedExpenseDto> = gson.fromJson(
            fixedExpensesJson,
            object : TypeToken<List<FixedExpenseDto>>() {}.type
        ) ?: emptyList()
        
        val categoryBudgets: Map<String, Double> = gson.fromJson(
            categoryBudgetsJson,
            object : TypeToken<Map<String, Double>>() {}.type
        ) ?: emptyMap()
        
        return Budget(
            id = id,
            monthlyIncome = monthlyIncome,
            fixedExpenses = fixedExpenses.map { it.toDomain() },
            savingsGoalPercentage = savingsGoalPercentage,
            savingsGoalAmount = savingsGoalAmount,
            currency = Currency.entries.find { it.code == currency } ?: Currency.RSD,
            month = YearMonth.parse(yearMonth),
            emergencyFundTarget = emergencyFundTarget,
            categoryBudgets = categoryBudgets.mapKeys { (key, _) ->
                TransactionCategory.entries.find { it.name == key } ?: TransactionCategory.OTHER_EXPENSE
            }
        )
    }
    
    companion object {
        fun fromDomain(budget: Budget): BudgetEntity {
            val gson = Gson()
            
            return BudgetEntity(
                id = budget.id,
                monthlyIncome = budget.monthlyIncome,
                fixedExpensesJson = gson.toJson(budget.fixedExpenses.map { FixedExpenseDto.fromDomain(it) }),
                savingsGoalPercentage = budget.savingsGoalPercentage,
                savingsGoalAmount = budget.savingsGoalAmount,
                currency = budget.currency.code,
                yearMonth = budget.month.toString(),
                emergencyFundTarget = budget.emergencyFundTarget,
                categoryBudgetsJson = gson.toJson(budget.categoryBudgets.mapKeys { it.key.name })
            )
        }
    }
}

/**
 * DTO for serializing FixedExpense to JSON
 */
data class FixedExpenseDto(
    val id: String,
    val name: String,
    val amount: Double,
    val category: String,
    val dueDay: Int,
    val isPaid: Boolean,
    val autoPay: Boolean,
    val reminderDaysBefore: Int
) {
    fun toDomain(): FixedExpense {
        return FixedExpense(
            id = id,
            name = name,
            amount = amount,
            category = TransactionCategory.entries.find { it.name == category } 
                ?: TransactionCategory.OTHER_EXPENSE,
            dueDay = dueDay,
            isPaid = isPaid,
            autoPay = autoPay,
            reminderDaysBefore = reminderDaysBefore
        )
    }
    
    companion object {
        fun fromDomain(expense: FixedExpense): FixedExpenseDto {
            return FixedExpenseDto(
                id = expense.id,
                name = expense.name,
                amount = expense.amount,
                category = expense.category.name,
                dueDay = expense.dueDay,
                isPaid = expense.isPaid,
                autoPay = expense.autoPay,
                reminderDaysBefore = expense.reminderDaysBefore
            )
        }
    }
}
