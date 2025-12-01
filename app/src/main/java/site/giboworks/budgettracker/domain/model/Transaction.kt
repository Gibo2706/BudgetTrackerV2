package site.giboworks.budgettracker.domain.model

import java.time.LocalDateTime
import java.util.UUID

/**
 * Core domain model representing a financial transaction.
 * This is the single source of truth for transaction data across the app.
 */
data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double, // Amount in base currency (RSD) for consistent reporting
    val currency: Currency = Currency.RSD,
    val category: TransactionCategory,
    val description: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val type: TransactionType,
    val source: TransactionSource = TransactionSource.MANUAL,
    val isRecurring: Boolean = false,
    val recurringFrequency: RecurringFrequency? = null,
    val merchantName: String? = null,
    val location: String? = null,
    val tags: List<String> = emptyList(),
    val creditsEarned: Int = 0, // Gamification: credits earned for this transaction
    // Original currency tracking for multi-currency support (Balkan focus)
    val originalAmount: Double? = null, // Original amount before conversion (null if RSD)
    val originalCurrency: Currency? = null, // Original currency code (null if RSD)
    /**
     * Emergency transactions don't affect Daily Safe-to-Spend limit.
     * Instead, they are deducted from Savings (Shield Ring).
     * Examples: Car repairs, medical emergencies, unexpected bills.
     */
    val isEmergency: Boolean = false
)

/**
 * Supported currencies with Serbian Dinar as default
 */
enum class Currency(val symbol: String, val code: String) {
    RSD("дин.", "RSD"),
    EUR("€", "EUR"),
    USD("$", "USD"),
    BAM("KM", "BAM"),  // Bosnia
    MKD("ден.", "MKD"), // North Macedonia
    HRK("kn", "HRK")   // Croatia (legacy)
}

/**
 * Transaction categories optimized for budget tracking
 */
enum class TransactionCategory(
    val displayName: String,
    val emoji: String,
    val isExpense: Boolean = true
) {
    // Expenses
    FOOD_GROCERIES("Groceries", "🛒"),
    FOOD_RESTAURANTS("Restaurants", "🍽️"),
    FOOD_COFFEE("Coffee & Drinks", "☕"),
    TRANSPORT_FUEL("Fuel", "⛽"),
    TRANSPORT_PUBLIC("Public Transport", "🚌"),
    TRANSPORT_TAXI("Taxi/Ride", "🚕"),
    UTILITIES_ELECTRICITY("Electricity", "⚡"),
    UTILITIES_WATER("Water", "💧"),
    UTILITIES_GAS("Gas/Heating", "🔥"),
    UTILITIES_INTERNET("Internet", "📡"),
    UTILITIES_PHONE("Phone", "📱"),
    HOUSING_RENT("Rent", "🏠"),
    HOUSING_MORTGAGE("Mortgage", "🏦"),
    HOUSING_MAINTENANCE("Home Maintenance", "🔧"),
    SHOPPING_CLOTHES("Clothing", "👕"),
    SHOPPING_ELECTRONICS("Electronics", "📱"),
    SHOPPING_OTHER("Shopping", "🛍️"),
    ENTERTAINMENT_STREAMING("Streaming", "📺"),
    ENTERTAINMENT_GAMES("Games", "🎮"),
    ENTERTAINMENT_EVENTS("Events", "🎉"),
    HEALTH_PHARMACY("Pharmacy", "💊"),
    HEALTH_DOCTOR("Medical", "🏥"),
    HEALTH_GYM("Fitness", "💪"),
    EDUCATION("Education", "📚"),
    SUBSCRIPTIONS("Subscriptions", "🔄"),
    PERSONAL_CARE("Personal Care", "💅"),
    GIFTS("Gifts", "🎁"),
    CHARITY("Charity", "❤️"),
    OTHER_EXPENSE("Other", "📝"),

    // Income
    SALARY("Salary", "💰", isExpense = false),
    FREELANCE("Freelance", "💼", isExpense = false),
    INVESTMENT_RETURN("Investment Return", "📈", isExpense = false),
    GIFT_RECEIVED("Gift Received", "🎁", isExpense = false),
    OTHER_INCOME("Other Income", "💵", isExpense = false),

    // Savings (treated specially for Shield ring)
    SAVINGS_DEPOSIT("Savings", "🏦", isExpense = true),
    MICRO_SAVINGS("Micro-Savings", "✨", isExpense = true), // "Decided not to buy"
    INVESTMENT("Investment", "📊", isExpense = true)
}

enum class TransactionType {
    EXPENSE,
    INCOME,
    SAVINGS,      // Money moved to savings
    INVESTMENT,   // Money invested
    MICRO_SAVINGS // Virtual savings (didn't buy something)
}

enum class TransactionSource {
    MANUAL,           // User entered manually
    NOTIFICATION,     // Parsed from bank notification
    SMS,             // Parsed from SMS
    RECURRING,       // Auto-generated recurring transaction
    IMPORT           // Imported from file
}

enum class RecurringFrequency {
    DAILY,
    WEEKLY,
    BIWEEKLY,
    MONTHLY,
    QUARTERLY,
    YEARLY
}
