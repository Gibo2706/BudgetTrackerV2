package site.giboworks.budgettracker.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import site.giboworks.budgettracker.domain.model.Currency
import site.giboworks.budgettracker.domain.model.Transaction
import site.giboworks.budgettracker.domain.model.TransactionCategory
import site.giboworks.budgettracker.domain.model.TransactionType
import site.giboworks.budgettracker.ui.theme.BudgetTrackerTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val AccentRed = Color(0xFFFF5252)

/**
 * Transaction History Screen
 * 
 * Displays all transactions grouped by date with summary statistics.
 * Non-judgmental design - no negative colors or language.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    viewModel: TransactionHistoryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val transactions by viewModel.allTransactions.collectAsState()
    val monthlyStats by viewModel.monthlyStats.collectAsState()
    val transactionToDelete by viewModel.transactionToDelete.collectAsState()
    
    // Group transactions by date
    val groupedTransactions = remember(transactions) {
        transactions.groupBy { it.timestamp.toLocalDate() }
            .toSortedMap(compareByDescending { it })
    }
    
    // Delete confirmation dialog
    transactionToDelete?.let { transaction ->
        DeleteTransactionDialog(
            transaction = transaction,
            onConfirm = { viewModel.confirmDelete() },
            onDismiss = { viewModel.dismissDeleteConfirmation() }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Transaction History",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                // Actions removed: Search and Calendar icons were non-functional placeholders
                // Will be re-introduced when search functionality is implemented
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Monthly Summary Card
            item {
                MonthlySummaryCard(stats = monthlyStats)
            }
            
            // Empty state
            if (groupedTransactions.isEmpty()) {
                item {
                    EmptyHistoryCard()
                }
            }
            
            // Transactions grouped by date
            groupedTransactions.forEach { (date, dayTransactions) ->
                // Date Header
                item(key = "header_$date") {
                    DateHeader(
                        date = date,
                        totalSpent = dayTransactions
                            .filter { it.type == TransactionType.EXPENSE }
                            .sumOf { it.amount },
                        totalSaved = dayTransactions
                            .filter { it.type == TransactionType.MICRO_SAVINGS || it.type == TransactionType.SAVINGS }
                            .sumOf { it.amount }
                    )
                }
                
                // Transactions for this date
                items(
                    items = dayTransactions,
                    key = { it.id }
                ) { transaction ->
                    HistoryTransactionItem(
                        transaction = transaction,
                        onDelete = { viewModel.showDeleteConfirmation(transaction) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                
                // Spacer between date groups
                item(key = "spacer_$date") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun MonthlySummaryCard(stats: MonthlyStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = stats.monthName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(
                    label = "Spent",
                    value = formatCurrency(stats.totalSpent),
                    color = MaterialTheme.colorScheme.onSurface
                )
                StatColumn(
                    label = "Saved",
                    value = formatCurrency(stats.totalSaved),
                    color = Color(0xFF9C27B0)
                )
                StatColumn(
                    label = "Credits",
                    value = "+${stats.creditsEarned}",
                    color = Color(0xFFFFD700)
                )
            }
        }
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = color
        )
    }
}

@Composable
private fun DateHeader(
    date: LocalDate,
    totalSpent: Double,
    totalSaved: Double
) {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    
    val dateText = when (date) {
        today -> "Today"
        yesterday -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()))
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateText,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (totalSpent > 0) {
                Text(
                    text = "-${formatCurrency(totalSpent)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            if (totalSaved > 0) {
                Text(
                    text = "+${formatCurrency(totalSaved)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF9C27B0)
                )
            }
        }
    }
}

@Composable
private fun HistoryTransactionItem(
    transaction: Transaction,
    onDelete: () -> Unit,
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
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category emoji with colored background
            val bgColor = when (transaction.type) {
                TransactionType.MICRO_SAVINGS, TransactionType.SAVINGS -> Color(0xFF9C27B0).copy(alpha = 0.15f)
                TransactionType.INCOME -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            }
            
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = transaction.category.emoji,
                    fontSize = 18.sp
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description.ifBlank { transaction.category.displayName },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transaction.category.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Text(
                        text = transaction.timestamp.format(DateTimeFormatter.ofPattern("HH:mm")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    if (transaction.creditsEarned > 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFD700).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "+${transaction.creditsEarned}✨",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFD700),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            
            // Amount with type-appropriate styling
            val (prefix, color) = when (transaction.type) {
                TransactionType.EXPENSE -> "-" to MaterialTheme.colorScheme.onSurface
                TransactionType.MICRO_SAVINGS, TransactionType.SAVINGS -> "+" to Color(0xFF9C27B0)
                TransactionType.INCOME -> "+" to Color(0xFF4CAF50)
                else -> "" to MaterialTheme.colorScheme.onSurface
            }
            
            Text(
                text = "$prefix${formatCurrency(transaction.amount)}",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = color
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = AccentRed.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun DeleteTransactionDialog(
    transaction: Transaction,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete Transaction?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to delete this transaction?"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = transaction.category.emoji,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = transaction.description.ifBlank { transaction.category.displayName },
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = formatCurrency(transaction.amount),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
            ) {
                Text("Delete", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EmptyHistoryCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "📋", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No transactions yet",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Your financial journey starts here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

private fun formatCurrency(amount: Double): String {
    return "%,.0f дин.".format(amount)
}

// ==================== DATA CLASSES ====================

data class MonthlyStats(
    val monthName: String = "",
    val totalSpent: Double = 0.0,
    val totalSaved: Double = 0.0,
    val creditsEarned: Int = 0
)

// ==================== PREVIEW ====================

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun TransactionHistoryScreenPreview() {
    BudgetTrackerTheme(darkTheme = true) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            // Preview content
        }
    }
}
