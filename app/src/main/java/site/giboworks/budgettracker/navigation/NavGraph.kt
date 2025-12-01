package site.giboworks.budgettracker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import site.giboworks.budgettracker.presentation.dashboard.DashboardScreen
import site.giboworks.budgettracker.presentation.history.TransactionHistoryScreen
import site.giboworks.budgettracker.presentation.insights.InsightsScreen
import site.giboworks.budgettracker.presentation.onboarding.OnboardingScreen
import site.giboworks.budgettracker.presentation.settings.BudgetSettingsScreen
import site.giboworks.budgettracker.presentation.settings.ManageBillsScreen
import site.giboworks.budgettracker.presentation.settings.SettingsScreen

/**
 * Main Navigation Graph for the Budget Tracker app
 */
@Composable
fun BudgetTrackerNavGraph(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    onNavigateToNotificationSettings: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Onboarding - First time setup wizard
        composable(route = Screen.Onboarding.route) {
            OnboardingScreen(
                onOnboardingComplete = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Dashboard - Main screen with Vitality Rings
        composable(route = Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToNotificationSettings = onNavigateToNotificationSettings,
                onNavigateToHistory = {
                    navController.navigate(Screen.TransactionHistory.route)
                },
                onNavigateToBudgetSettings = {
                    navController.navigate(Screen.BudgetSettings.route)
                },
                onNavigateToInsights = {
                    navController.navigate(Screen.Insights.route)
                }
            )
        }
        
        // Transaction History - All transactions grouped by date
        composable(route = Screen.TransactionHistory.route) {
            TransactionHistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Budget Settings - Edit budget configuration
        composable(route = Screen.BudgetSettings.route) {
            BudgetSettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Settings - Full settings screen with budget, well-being, and app options
        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToNotificationSettings = onNavigateToNotificationSettings,
                onNavigateToManageBills = {
                    navController.navigate(Screen.ManageBills.route)
                }
            )
        }
        
        // Manage Bills - Add/Edit/Delete fixed bills
        composable(route = Screen.ManageBills.route) {
            ManageBillsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Insights - Analytics and spending patterns
        composable(route = Screen.Insights.route) {
            InsightsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
