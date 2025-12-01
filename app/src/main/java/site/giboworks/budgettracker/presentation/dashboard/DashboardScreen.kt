package site.giboworks.budgettracker.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import site.giboworks.budgettracker.domain.model.Currency
import site.giboworks.budgettracker.domain.model.RingState
import site.giboworks.budgettracker.domain.model.RingStatus
import site.giboworks.budgettracker.domain.model.RingType
import site.giboworks.budgettracker.domain.model.Transaction
import site.giboworks.budgettracker.domain.model.TransactionCategory
import site.giboworks.budgettracker.domain.model.TransactionType
import site.giboworks.budgettracker.domain.model.VitalityRingsState
import site.giboworks.budgettracker.presentation.components.VitalityRings
import site.giboworks.budgettracker.ui.theme.BudgetTrackerTheme
import java.time.format.DateTimeFormatter

/**
 * Main Dashboard Screen with Vitality Rings
 * FIXED: Removed redundant labels, fixed FAB size, moved streak to header
 * WIRED: Didn't Buy dialog, Snackbar messages, See All navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToNotificationSettings: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToBudgetSettings: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {}
) {
    val vitalityState by viewModel.vitalityRingsState.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val showQuickAdd by viewModel.showQuickAddDialog.collectAsState()
    val showDidntBuy by viewModel.showDidntBuyDialog.collectAsState()
    val showBillChecklist by viewModel.showBillChecklistDialog.collectAsState()
    val creditsToday by viewModel.creditsToday.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val isGhostModeEnabled by viewModel.isGhostModeEnabled.collectAsState()
    val fixedBills by viewModel.fixedBills.collectAsState()
    val billsSummary by viewModel.billsSummary.collectAsState()
    val budgetConfig by viewModel.budgetConfig.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    var fabExpanded by remember { mutableStateOf(false) }
    
    // Show snackbar when message changes
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSnackbar()
        }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // Hide FAB when Ghost Mode is enabled
            if (!isGhostModeEnabled) {
                CompactFAB(
                    expanded = fabExpanded,
                    onExpandedChange = { fabExpanded = it },
                    onAddExpense = { 
                        viewModel.showQuickAdd()
                        fabExpanded = false
                    },
                    onAddMicroSavings = { 
                        viewModel.showDidntBuy()
                        fabExpanded = false
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Header with streak badge
            item {
                DashboardHeader(
                    creditsToday = creditsToday,
                    streak = vitalityState.streak,
                    onSettingsClick = onNavigateToSettings,
                    onNotificationSettingsClick = onNavigateToNotificationSettings
                )
            }
            
            // Ghost Mode - Soothing message when tracking is paused
            if (isGhostModeEnabled) {
                item {
                    GhostModeContent(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        onResumeClick = onNavigateToSettings
                    )
                }
            } else {
                // Vitality Rings - NO LABELS below (showLabels = false)
                item {
                    VitalityRings(
                        state = vitalityState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 8.dp),
                        showLabels = false,
                        showCenterContent = true,
                        ringSpacing = 20.dp,
                        strokeWidth = 20.dp
                    )
                }
            
                // Quick Stats Cards
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    QuickStatsRow(
                        vitalityState = vitalityState,
                        onBillsClick = { viewModel.showBillChecklist() }
                    )
                }
                
                // Insights Button
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    InsightsButton(onClick = onNavigateToInsights)
                }
            }
            
            // Recent Transactions Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = "See All",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onNavigateToHistory() }
                    )
                }
            }
            
            // Recent Transactions List
            if (recentTransactions.isEmpty()) {
                item {
                    EmptyTransactionsCard()
                }
            } else {
                items(recentTransactions) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
    
    // Add Transaction Bottom Sheet
    if (showQuickAdd) {
        AddTransactionBottomSheet(
            onDismiss = { viewModel.hideQuickAdd() },
            onSave = { amount, category, isEmergency ->
                viewModel.quickAddTransaction(amount, category, "", isEmergency)
            }
        )
    }
    
    // Didn't Buy Dialog (Anti-Spend Feature)
    if (showDidntBuy) {
        DidntBuyDialog(
            onDismiss = { viewModel.hideDidntBuy() },
            onSave = { itemName, amount ->
                viewModel.recordDidntBuy(itemName, amount)
            }
        )
    }
    
    // Bill Checklist Dialog
    if (showBillChecklist) {
        BillChecklistDialog(
            bills = fixedBills,
            summary = billsSummary,
            currency = budgetConfig.currency,
            onDismiss = { viewModel.hideBillChecklist() },
            onBillPaid = { billId, actualAmount ->
                viewModel.markBillAsPaid(billId, actualAmount)
            },
            onBillUnpaid = { billId ->
                viewModel.markBillAsUnpaid(billId)
            }
        )
    }
}

/**
 * Header with streak badge in top-right
 */
@Composable
private fun DashboardHeader(
    creditsToday: Int,
    streak: Int,
    onSettingsClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Financial Vitality",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "✨ $creditsToday credits today",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFFD700)
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Streak Badge - pill shaped
            if (streak > 0) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFF9800).copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🔥", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$streak",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFFFF9800)
                        )
                    }
                }
            }
            
            IconButton(onClick = onNotificationSettingsClick) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickStatsRow(
    vitalityState: VitalityRingsState,
    onBillsClick: () -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            val remaining = vitalityState.pulse.targetValue - vitalityState.pulse.currentValue
            val isOverBudget = remaining < 0
            DailyLeftStatCard(
                title = "Daily Left",
                value = "%,.0f".format(remaining),
                subtitle = vitalityState.pulse.currency.symbol,
                // De-escalated colors: Orange/Amber instead of red
                color = if (!isOverBudget) Color(0xFF4CAF50) else Color(0xFFFF9800),
                isAdjusting = isOverBudget
            )
        }
        item {
            QuickStatCard(
                title = "Saved",
                value = "%,.0f".format(vitalityState.shield.currentValue),
                subtitle = vitalityState.shield.currency.symbol,
                color = Color(0xFF9C27B0)
            )
        }
        item {
            // Bills card is now clickable to open bill checklist
            QuickStatCard(
                title = "Bills",
                value = "${vitalityState.clarity.currentValue.toInt()}/${vitalityState.clarity.targetValue.toInt()}",
                subtitle = "paid",
                color = Color(0xFF2196F3),
                onClick = onBillsClick
            )
        }
    }
}

@Composable
private fun QuickStatCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = color
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Insights navigation button - access analytics and spending patterns
 */
@Composable
private fun InsightsButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊",
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "View Insights",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Spending patterns & analytics",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = "Insights",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Special card for Daily Left that shows adjusting icon when over budget
 * De-escalated UI: Shows negative balance with subtle "adjusting" indicator
 * instead of aggressive "Over!" text
 */
@Composable
private fun DailyLeftStatCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    isAdjusting: Boolean = false
) {
    Card(
        modifier = Modifier.width(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = color
                )
                if (isAdjusting) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Rounded.Autorenew,
                        contentDescription = "Adjusting",
                        modifier = Modifier.size(16.dp),
                        tint = color.copy(alpha = 0.7f)
                    )
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Ghost Mode Content - Shown when tracking is paused.
 * Provides a calming, non-judgmental message to users taking a break.
 */
@Composable
private fun GhostModeContent(
    modifier: Modifier = Modifier,
    onResumeClick: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Calming icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🌙",
                style = MaterialTheme.typography.displayLarge
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Taking a Break",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = "That's okay. Tracking is paused.\nYour financial journey will be here when you're ready.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        FilledTonalButton(
            onClick = onResumeClick,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Go to Settings to Resume")
        }
        
        // Subtle reassurance
        Text(
            text = "💜 Self-care is part of being financially healthy",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun EmptyTransactionsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📊",
                style = MaterialTheme.typography.displayMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No transactions yet",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap + to add your first expense",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun TransactionItem(
    transaction: Transaction,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category emoji
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = transaction.category.emoji,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description.ifBlank { transaction.category.displayName },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = transaction.category.displayName + " • " + 
                           transaction.timestamp.format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            // Amount
            val isExpense = transaction.type == TransactionType.EXPENSE
            Text(
                text = (if (isExpense) "-" else "+") + 
                       "%,.0f".format(transaction.amount) + " " + transaction.currency.symbol,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = if (isExpense) Color(0xFFFF5252) else Color(0xFF4CAF50)
            )
        }
    }
}

/**
 * Compact FAB - Standard size, not LargeFAB, with subdued accent color
 */
@Composable
private fun CompactFAB(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAddExpense: () -> Unit,
    onAddMicroSavings: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier.navigationBarsPadding()
    ) {
        // Mini action buttons
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
            exit = fadeOut() + scaleOut()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                // Micro-savings button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = "Didn't buy",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    SmallFloatingActionButton(
                        onClick = onAddMicroSavings,
                        containerColor = Color(0xFF9C27B0).copy(alpha = 0.9f),
                        elevation = FloatingActionButtonDefaults.elevation(4.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Savings, 
                            contentDescription = "Micro-savings",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                // Add expense button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = "Add expense",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    SmallFloatingActionButton(
                        onClick = onAddExpense,
                        containerColor = Color(0xFFEF5350).copy(alpha = 0.9f),
                        elevation = FloatingActionButtonDefaults.elevation(4.dp)
                    ) {
                        Icon(
                            Icons.Default.TrendingDown, 
                            contentDescription = "Add expense",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        
        // Main FAB - Standard size (not Large), softer accent color
        FloatingActionButton(
            onClick = { onExpandedChange(!expanded) },
            containerColor = Color(0xFF6366F1), // Softer indigo accent
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp
            )
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.Close else Icons.Default.Add,
                contentDescription = "Add transaction",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ==================== ADD TRANSACTION BOTTOM SHEET ====================

/**
 * Full-featured Add Transaction Bottom Sheet with numeric keypad
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionBottomSheet(
    onDismiss: () -> Unit,
    onSave: (amount: Double, category: TransactionCategory, isEmergency: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    
    var amountString by remember { mutableStateOf("0") }
    var selectedCategory by remember { mutableStateOf(TransactionCategory.OTHER_EXPENSE) }
    var isEmergency by remember { mutableStateOf(false) }
    
    val quickCategories = listOf(
        TransactionCategory.FOOD_GROCERIES,
        TransactionCategory.FOOD_RESTAURANTS,
        TransactionCategory.FOOD_COFFEE,
        TransactionCategory.TRANSPORT_FUEL,
        TransactionCategory.TRANSPORT_TAXI,
        TransactionCategory.TRANSPORT_PUBLIC,
        TransactionCategory.SHOPPING_OTHER,
        TransactionCategory.ENTERTAINMENT_EVENTS,
        TransactionCategory.HEALTH_PHARMACY,
        TransactionCategory.OTHER_EXPENSE
    )
    
    fun onNumberClick(digit: String) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        amountString = when {
            amountString == "0" && digit != "." -> digit
            digit == "." && amountString.contains(".") -> amountString
            amountString.length >= 10 -> amountString
            else -> amountString + digit
        }
    }
    
    fun onBackspace() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        amountString = if (amountString.length > 1) {
            amountString.dropLast(1)
        } else {
            "0"
        }
    }
    
    fun onClear() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        amountString = "0"
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { /* No drag handle for cleaner look */ }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 8.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add Expense",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Amount Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = formatDisplayAmount(amountString),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 42.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RSD",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Category Selection
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickCategories.forEach { category ->
                    CategoryChip(
                        category = category,
                        selected = selectedCategory == category,
                        onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedCategory = category 
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Emergency Toggle - Empathetic design for unexpected expenses
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isEmergency) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        },
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isEmergency = !isEmergency 
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isEmergency,
                    onCheckedChange = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isEmergency = it 
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "This is an emergency expense",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Won't affect your daily budget • Deducted from savings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                if (isEmergency) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Numeric Keypad
            NumericKeypad(
                onNumberClick = ::onNumberClick,
                onBackspace = ::onBackspace,
                onClear = ::onClear
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Save Button
            Button(
                onClick = {
                    val amount = amountString.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        onSave(amount, selectedCategory, isEmergency)
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = amountString != "0" && amountString.toDoubleOrNull() != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEmergency) MaterialTheme.colorScheme.primary else Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    if (isEmergency) Icons.Default.Star else Icons.Default.Check, 
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEmergency) "Save Emergency Expense" else "Save Expense",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun NumericKeypad(
    onNumberClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit
) {
    val buttons = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("C", "0", "⌫")
    )
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { label ->
                    KeypadButton(
                        label = label,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            when (label) {
                                "⌫" -> onBackspace()
                                "C" -> onClear()
                                else -> onNumberClick(label)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isSpecial = label == "C" || label == "⌫"
    
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (isSpecial) 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        if (label == "⌫") {
            Icon(
                Icons.Default.Backspace,
                contentDescription = "Backspace",
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.error
            )
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = if (isSpecial) FontWeight.Medium else FontWeight.SemiBold
                ),
                color = if (label == "C") 
                    MaterialTheme.colorScheme.error 
                else 
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CategoryChip(
    category: TransactionCategory,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) 
            MaterialTheme.colorScheme.primary 
        else 
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        tonalElevation = if (selected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = category.emoji, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) 
                    MaterialTheme.colorScheme.onPrimary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDisplayAmount(amount: String): String {
    return try {
        val number = amount.toDoubleOrNull() ?: 0.0
        if (amount.contains(".")) {
            amount // Keep decimal point visible
        } else {
            "%,.0f".format(number)
        }
    } catch (e: Exception) {
        amount
    }
}

// ==================== PREVIEW ====================

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun DashboardScreenPreview() {
    BudgetTrackerTheme(darkTheme = true) {
        // Preview with mock data
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                item {
                    DashboardHeader(
                        creditsToday = 45,
                        streak = 7,
                        onSettingsClick = {},
                        onNotificationSettingsClick = {}
                    )
                }
                
                item {
                    VitalityRings(
                        state = VitalityRingsState(
                            pulse = RingStatus(
                                type = RingType.PULSE,
                                progress = 0.65f,
                                currentValue = 3250.0,
                                targetValue = 5000.0,
                                state = RingState.GOOD,
                                label = "Pulse",
                                sublabel = "3,250 / 5,000 дин."
                            ),
                            shield = RingStatus(
                                type = RingType.SHIELD,
                                progress = 0.45f,
                                currentValue = 9000.0,
                                targetValue = 20000.0,
                                state = RingState.GOOD,
                                label = "Shield",
                                sublabel = "9,000 / 20,000 дин."
                            ),
                            clarity = RingStatus(
                                type = RingType.CLARITY,
                                progress = 0.8f,
                                currentValue = 4.0,
                                targetValue = 5.0,
                                state = RingState.GOOD,
                                label = "Clarity",
                                sublabel = "4/5 bills paid"
                            ),
                            streak = 7
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 8.dp),
                        showLabels = false
                    )
                }
            }
        }
    }
}
