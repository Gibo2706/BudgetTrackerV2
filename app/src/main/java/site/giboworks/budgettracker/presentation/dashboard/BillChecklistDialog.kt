package site.giboworks.budgettracker.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import site.giboworks.budgettracker.domain.model.BillsSummary
import site.giboworks.budgettracker.domain.model.Currency
import site.giboworks.budgettracker.domain.model.FixedBill

// Color constants for the dialog
private val AccentCyan = Color(0xFF56CCF2)
private val AccentPurple = Color(0xFF9C27B0)
private val AccentGreen = Color(0xFF00E676)
private val AccentOrange = Color(0xFFFF9800)

/**
 * Bill Checklist Dialog
 * 
 * Shows a list of fixed bills that users can check off as paid.
 * Handles variable bill logic:
 * - If paid amount < estimated: difference goes to Savings (Shield ring)
 * - If paid amount > estimated: excess is deducted from Daily Limit (Pulse ring)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillChecklistDialog(
    bills: List<FixedBill>,
    summary: BillsSummary,
    currency: Currency,
    onDismiss: () -> Unit,
    onBillPaid: (billId: Long, actualAmount: Double?) -> Unit,
    onBillUnpaid: (billId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // State for variable amount dialog
    var showAmountDialog by remember { mutableStateOf(false) }
    var selectedBill by remember { mutableStateOf<FixedBill?>(null) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Bill Manager",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "${summary.paidBills}/${summary.totalBills} bills paid",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress bar
            LinearProgressIndicator(
                progress = { summary.completionPercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = AccentGreen,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Summary Card (if there are savings/overages)
            if (summary.paidBills > 0 && summary.netVariance != 0.0) {
                BillsSummaryCard(
                    summary = summary,
                    currency = currency
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Bills List
            if (bills.isEmpty()) {
                EmptyBillsState()
            } else {
                bills.forEach { bill ->
                    BillChecklistItem(
                        bill = bill,
                        currency = currency,
                        onTogglePaid = { isPaid ->
                            if (isPaid) {
                                // Show amount dialog for variable bill entry
                                selectedBill = bill
                                showAmountDialog = true
                            } else {
                                onBillUnpaid(bill.id)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Info text
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pay less than estimated? The savings go to your Shield! Pay more? It adjusts your daily budget.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
    
    // Variable amount dialog
    if (showAmountDialog && selectedBill != null) {
        VariableAmountDialog(
            bill = selectedBill!!,
            currency = currency,
            onDismiss = {
                showAmountDialog = false
                selectedBill = null
            },
            onConfirm = { actualAmount ->
                onBillPaid(selectedBill!!.id, actualAmount)
                showAmountDialog = false
                selectedBill = null
            }
        )
    }
}

/**
 * Summary card showing savings/overage from variable bills
 */
@Composable
private fun BillsSummaryCard(
    summary: BillsSummary,
    currency: Currency
) {
    val isPositive = summary.netVariance > 0
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPositive) 
                AccentGreen.copy(alpha = 0.15f) 
            else 
                AccentOrange.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isPositive) Icons.Default.Savings else Icons.Default.TrendingDown,
                contentDescription = null,
                tint = if (isPositive) AccentGreen else AccentOrange,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (isPositive) "Bills Savings!" else "Budget Adjustment",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPositive) AccentGreen else AccentOrange
                )
                Text(
                    text = if (isPositive) {
                        "${formatCurrency(summary.totalSavingsFromBills, currency)} added to savings"
                    } else {
                        "${formatCurrency(summary.totalOverageFromBills, currency)} over budget"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Single bill checklist item
 */
@Composable
private fun BillChecklistItem(
    bill: FixedBill,
    currency: Currency,
    onTogglePaid: (isPaid: Boolean) -> Unit
) {
    val isPaid = bill.isPaidThisMonth
    val variance = bill.getVariance()
    val hasSavings = variance != null && variance < 0
    val hasOverage = variance != null && variance > 0
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTogglePaid(!isPaid) },
        colors = CardDefaults.cardColors(
            containerColor = if (isPaid) 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else 
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox icon
            Icon(
                imageVector = if (isPaid) Icons.Outlined.CheckCircle else Icons.Outlined.Circle,
                contentDescription = if (isPaid) "Paid" else "Not paid",
                tint = if (isPaid) AccentGreen else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(28.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Icon and name
            Text(
                text = bill.icon,
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bill.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        textDecoration = if (isPaid) TextDecoration.LineThrough else null
                    ),
                    color = if (isPaid) 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                
                // Show actual vs estimated if different
                if (isPaid && bill.actualAmountPaid != null && bill.actualAmountPaid != bill.estimatedAmount) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Paid: ${formatCurrency(bill.actualAmountPaid, currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasSavings) AccentGreen else AccentOrange
                        )
                        if (hasSavings) {
                            Icon(
                                imageVector = Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier
                                    .size(14.dp)
                                    .padding(start = 4.dp)
                            )
                        } else if (hasOverage) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = AccentOrange,
                                modifier = Modifier
                                    .size(14.dp)
                                    .padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
            
            // Amount
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(bill.estimatedAmount, currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPaid)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                
                // Show variance badge
                if (hasSavings) {
                    Text(
                        text = "-${formatCurrency(-variance!!, currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentGreen
                    )
                } else if (hasOverage) {
                    Text(
                        text = "+${formatCurrency(variance!!, currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentOrange
                    )
                }
            }
        }
    }
}

/**
 * Dialog to enter actual amount paid for a bill
 */
@Composable
private fun VariableAmountDialog(
    bill: FixedBill,
    currency: Currency,
    onDismiss: () -> Unit,
    onConfirm: (actualAmount: Double?) -> Unit
) {
    var amountText by remember { mutableStateOf(bill.estimatedAmount.toInt().toString()) }
    var useEstimated by remember { mutableStateOf(true) }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = bill.icon)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Pay ${bill.name}")
                }
            }
        },
        text = {
            Column {
                Text(
                    text = "Estimated: ${formatCurrency(bill.estimatedAmount, currency)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Enter actual amount paid:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            amountText = newValue
                            useEstimated = false
                        }
                    },
                    label = { Text(currency.symbol) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { keyboardController?.hide() }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Quick action: Use estimated amount
                OutlinedButton(
                    onClick = {
                        amountText = bill.estimatedAmount.toInt().toString()
                        useEstimated = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use estimated amount")
                }
                
                // Show preview of impact
                val actualAmount = amountText.toDoubleOrNull() ?: bill.estimatedAmount
                val difference = actualAmount - bill.estimatedAmount
                
                if (difference != 0.0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (difference < 0) 
                                AccentGreen.copy(alpha = 0.15f) 
                            else 
                                AccentOrange.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (difference < 0) Icons.Default.Savings else Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = if (difference < 0) AccentGreen else AccentOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (difference < 0) {
                                    "${formatCurrency(-difference, currency)} goes to savings!"
                                } else {
                                    "${formatCurrency(difference, currency)} over budget"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (difference < 0) AccentGreen else AccentOrange
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val actual = amountText.toDoubleOrNull()
                    onConfirm(if (useEstimated || actual == bill.estimatedAmount) null else actual)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentCyan
                )
            ) {
                Text("Mark as Paid")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Empty state when no bills are configured
 */
@Composable
private fun EmptyBillsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "📄",
            style = MaterialTheme.typography.displayMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No bills configured",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Add your fixed bills in Settings to track them here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Format currency amount
 */
private fun formatCurrency(amount: Double, currency: Currency): String {
    return when (currency) {
        Currency.RSD -> "%,.0f %s".format(amount, currency.symbol)
        else -> "%s%,.2f".format(currency.symbol, amount)
    }
}
