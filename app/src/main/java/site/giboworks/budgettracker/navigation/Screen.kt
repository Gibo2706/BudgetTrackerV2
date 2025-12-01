package site.giboworks.budgettracker.navigation

/**
 * Navigation routes for the Budget Tracker app
 */
sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Dashboard : Screen("dashboard")
    data object TransactionHistory : Screen("history")
    data object Settings : Screen("settings")
    data object NotificationSettings : Screen("notification_settings")
    data object AddTransaction : Screen("add_transaction")
    data object TransactionDetail : Screen("transaction/{transactionId}") {
        fun createRoute(transactionId: String) = "transaction/$transactionId"
    }
    data object BudgetSettings : Screen("budget_settings")
    data object Insights : Screen("insights")
    data object ManageBills : Screen("manage_bills")
}
