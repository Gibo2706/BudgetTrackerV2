package site.giboworks.budgettracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import site.giboworks.budgettracker.data.preferences.AppPreferences
import site.giboworks.budgettracker.navigation.BudgetTrackerNavGraph
import site.giboworks.budgettracker.navigation.Screen
import site.giboworks.budgettracker.service.BankNotificationListenerService
import site.giboworks.budgettracker.ui.theme.BudgetTrackerTheme
import javax.inject.Inject

/**
 * Main Activity - Entry point for the Budget Tracker app.
 * Uses Hilt for dependency injection.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var appPreferences: AppPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BudgetTrackerTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    var startDestination by remember { mutableStateOf<String?>(null) }
                    
                    // Check onboarding status on startup
                    LaunchedEffect(Unit) {
                        val onboardingCompleted = appPreferences.getOnboardingCompleted()
                        startDestination = if (onboardingCompleted) {
                            Screen.Dashboard.route
                        } else {
                            Screen.Onboarding.route
                        }
                    }
                    
                    // Show loading while determining start destination
                    if (startDestination == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        BudgetTrackerNavGraph(
                            navController = navController,
                            startDestination = startDestination!!,
                            onNavigateToNotificationSettings = {
                                BankNotificationListenerService.openNotificationAccessSettings(this)
                            }
                        )
                    }
                }
            }
        }
    }
}