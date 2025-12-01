package site.giboworks.budgettracker.presentation.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import site.giboworks.budgettracker.service.BankNotificationService
import site.giboworks.budgettracker.ui.theme.BudgetTrackerTheme

/**
 * Screen for setting up notification access permission.
 * Guides users through enabling automatic transaction capture.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSetupScreen(
    onBackClick: () -> Unit = {},
    onSetupComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var isNotificationAccessEnabled by remember {
        mutableStateOf(BankNotificationService.isNotificationAccessEnabled(context))
    }
    
    // Refresh permission status when screen becomes visible
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isNotificationAccessEnabled = BankNotificationService.isNotificationAccessEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Automatic Tracking") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            item {
                NotificationStatusCard(
                    isEnabled = isNotificationAccessEnabled,
                    onEnableClick = {
                        BankNotificationService.openNotificationAccessSettings(context)
                    }
                )
            }
            
            // How it works section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "How it works",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
            
            item {
                HowItWorksCard()
            }
            
            // Supported banks
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Supported Banks",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
            
            items(supportedBanks) { bank ->
                BankCard(bank = bank)
            }
            
            // Privacy note
            item {
                Spacer(modifier = Modifier.height(8.dp))
                PrivacyNoteCard()
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun NotificationStatusCard(
    isEnabled: Boolean,
    onEnableClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) 
                Color(0xFF4CAF50).copy(alpha = 0.15f) 
            else 
                Color(0xFFFF9800).copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        if (isEnabled) Color(0xFF4CAF50).copy(alpha = 0.2f)
                        else Color(0xFFFF9800).copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isEnabled) Icons.Default.Check else Icons.Default.NotificationsActive,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = if (isEnabled) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (isEnabled) "Auto-tracking Active" else "Enable Auto-tracking",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (isEnabled) 
                    "Bank notifications will be automatically converted to transactions"
                else 
                    "Allow notification access to automatically capture transactions from your bank apps",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            if (!isEnabled) {
                Spacer(modifier = Modifier.height(20.dp))
                
                Button(
                    onClick = onEnableClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Enable Notification Access")
                }
            }
        }
    }
}

@Composable
private fun HowItWorksCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HowItWorksStep(
                number = "1",
                title = "Receive notification",
                description = "Your bank sends a payment notification"
            )
            HowItWorksStep(
                number = "2",
                title = "Smart parsing",
                description = "We extract the amount and merchant automatically"
            )
            HowItWorksStep(
                number = "3",
                title = "Auto-categorize",
                description = "Transaction is saved with predicted category"
            )
            HowItWorksStep(
                number = "4",
                title = "Earn credits",
                description = "+5 credits for each auto-captured transaction!"
            )
        }
    }
}

@Composable
private fun HowItWorksStep(
    number: String,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun BankCard(bank: BankInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bank.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = bank.emoji,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bank.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = bank.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            if (bank.isSupported) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Supported",
                    tint = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
private fun PrivacyNoteCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = "Your Privacy",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "We only read notifications from bank apps you've approved. " +
                            "All data stays on your device. We never share your financial information.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// Data classes

data class BankInfo(
    val name: String,
    val description: String,
    val emoji: String,
    val color: Color,
    val isSupported: Boolean = true
)

private val supportedBanks = listOf(
    BankInfo("Raiffeisen Bank", "Full support", "🟡", Color(0xFFFFEB3B)),
    BankInfo("Banca Intesa", "Full support", "🟢", Color(0xFF4CAF50)),
    BankInfo("UniCredit Bank", "Full support", "🔴", Color(0xFFF44336)),
    BankInfo("Komercijalna Banka", "Full support", "🔵", Color(0xFF2196F3)),
    BankInfo("AIK Banka", "Full support", "🟣", Color(0xFF9C27B0)),
    BankInfo("Erste Bank", "George app supported", "🔵", Color(0xFF1976D2)),
    BankInfo("OTP Banka", "Full support", "🟢", Color(0xFF689F38)),
    BankInfo("NLB Banka", "Full support", "🟠", Color(0xFFFF9800)),
    BankInfo("Poštanska Štedionica", "Basic support", "🟡", Color(0xFFFFC107)),
    BankInfo("ProCredit Bank", "Basic support", "🔵", Color(0xFF03A9F4))
)

// ==================== PREVIEW ====================

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun NotificationSetupScreenPreview() {
    BudgetTrackerTheme(darkTheme = true) {
        NotificationSetupScreen()
    }
}
