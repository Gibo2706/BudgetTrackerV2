package site.giboworks.budgettracker.presentation.onboarding

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import site.giboworks.budgettracker.service.BankNotificationService
import site.giboworks.budgettracker.ui.theme.BudgetTrackerTheme

/**
 * Onboarding Permission Screen for Notification Access.
 * 
 * This screen explains to users why we need notification access permission
 * and guides them through enabling it in system settings.
 * 
 * Key features:
 * - Clear explanation of why the permission is needed
 * - Privacy-focused messaging
 * - Direct link to system notification listener settings
 * - Visual feedback when permission is granted
 */
@Composable
fun PermissionScreen(
    onPermissionGranted: () -> Unit = {},
    onSkip: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var isPermissionGranted by remember {
        mutableStateOf(BankNotificationService.isNotificationAccessEnabled(context))
    }
    
    var showContent by remember { mutableStateOf(false) }
    
    // Animate content appearance
    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }
    
    // Refresh permission status when returning from settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val wasGranted = isPermissionGranted
                isPermissionGranted = BankNotificationService.isNotificationAccessEnabled(context)
                
                // Auto-proceed if permission was just granted
                if (!wasGranted && isPermissionGranted) {
                    onPermissionGranted()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                        Color(0xFF0F3460)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Animated Header
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { -50 }
            ) {
                HeaderSection(isPermissionGranted = isPermissionGranted)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Benefits Section
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(600, delayMillis = 200)) + slideInVertically(tween(600, delayMillis = 200)) { 50 }
            ) {
                BenefitsSection()
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Privacy Assurance
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(600, delayMillis = 400)) + slideInVertically(tween(600, delayMillis = 400)) { 50 }
            ) {
                PrivacySection()
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Action Buttons
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(600, delayMillis = 600))
            ) {
                ActionButtons(
                    isPermissionGranted = isPermissionGranted,
                    onEnableClick = {
                        openNotificationListenerSettings(context)
                    },
                    onContinueClick = onPermissionGranted,
                    onSkipClick = onSkip
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun HeaderSection(isPermissionGranted: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon with animated state
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    if (isPermissionGranted) 
                        Color(0xFF4CAF50).copy(alpha = 0.2f)
                    else 
                        Color(0xFF00D9FF).copy(alpha = 0.2f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPermissionGranted) Icons.Default.Check else Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(50.dp),
                tint = if (isPermissionGranted) Color(0xFF4CAF50) else Color(0xFF00D9FF)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = if (isPermissionGranted) "You're All Set!" else "Automatic Tracking",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = if (isPermissionGranted)
                "Bank notifications will be automatically converted to transactions"
            else
                "Let BudgetTracker read your bank notifications to automatically log transactions",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun BenefitsSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Why enable this?",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )
            
            BenefitItem(
                icon = Icons.Outlined.AutoMode,
                title = "Zero manual entry",
                description = "Transactions are captured automatically when you pay"
            )
            
            BenefitItem(
                icon = Icons.Outlined.AccountBalance,
                title = "Multi-bank support",
                description = "Works with all major Serbian and regional banks"
            )
            
            BenefitItem(
                icon = Icons.Default.Sync,
                title = "Currency conversion",
                description = "Automatically converts EUR, BAM, USD to RSD"
            )
        }
    }
}

@Composable
private fun BenefitItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF00D9FF).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = Color(0xFF00D9FF)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.6f)
                )
            )
        }
    }
}

@Composable
private fun PrivacySection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = Color(0xFF4CAF50)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your privacy is protected",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
                Text(
                    text = "We only read notifications from bank apps. All data stays on your device.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.7f)
                    )
                )
            }
        }
    }
}

@Composable
private fun ActionButtons(
    isPermissionGranted: Boolean,
    onEnableClick: () -> Unit,
    onContinueClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isPermissionGranted) {
            Button(
                onClick = onContinueClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        } else {
            Button(
                onClick = onEnableClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00D9FF)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF1A1A2E)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Enable Notification Access",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A1A2E)
                    )
                )
            }
            
            OutlinedButton(
                onClick = onSkipClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Skip for now",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Text(
                text = "You can enable this later in Settings",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.5f)
                )
            )
        }
    }
}

/**
 * Opens the system Notification Listener Settings screen.
 * This is where users grant notification access to apps.
 */
private fun openNotificationListenerSettings(context: Context) {
    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A2E)
@Composable
private fun PermissionScreenPreview() {
    BudgetTrackerTheme {
        PermissionScreen()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A2E)
@Composable
private fun PermissionScreenGrantedPreview() {
    BudgetTrackerTheme {
        // This would show the "granted" state if we could mock it
        PermissionScreen()
    }
}
