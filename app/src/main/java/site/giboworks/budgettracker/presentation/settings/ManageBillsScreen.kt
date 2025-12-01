package site.giboworks.budgettracker.presentation.settings

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import site.giboworks.budgettracker.domain.model.FixedBill

// Theme Colors
private val GradientStart = Color(0xFF1A1A2E)
private val GradientMid = Color(0xFF16213E)
private val GradientEnd = Color(0xFF0F3460)
private val AccentCyan = Color(0xFF56CCF2)
private val AccentRed = Color(0xFFFF5252)
private val AccentAmber = Color(0xFFFFB74D)
private val SurfaceCard = Color(0xFF21262D)

/**
 * Manage Bills Screen - Full CRUD for fixed bills.
 * 
 * Features:
 * - List all bills
 * - Add new bills via FAB
 * - Edit existing bills by tapping
 * - Delete with confirmation
 * - Shows recurring vs one-time indicator
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageBillsScreen(
    viewModel: ManageBillsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(GradientStart, GradientMid, GradientEnd)
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { viewModel.showAddDialog() },
                    containerColor = AccentCyan,
                    contentColor = Color.Black
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Bill")
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(innerPadding)
            ) {
                // Header
                ManageBillsHeader(
                    onNavigateBack = onNavigateBack,
                    totalBills = uiState.bills.size,
                    totalEstimated = uiState.totalEstimated,
                    currencySymbol = uiState.currencySymbol
                )
                
                // Summary Cards
                BillsSummaryCards(
                    recurringCount = uiState.recurringBillsCount,
                    oneTimeCount = uiState.oneTimeBillsCount
                )
                
                // Bills List
                if (uiState.bills.isEmpty()) {
                    EmptyBillsState()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.bills,
                            key = { it.id }
                        ) { bill ->
                            BillItemCard(
                                bill = bill,
                                currencySymbol = uiState.currencySymbol,
                                onEdit = { viewModel.showEditDialog(bill) },
                                onDelete = { viewModel.showDeleteConfirmation(bill) }
                            )
                        }
                        
                        // Bottom spacing for FAB
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
        
        // Add/Edit Dialog
        if (uiState.showAddEditDialog) {
            AddEditBillDialog(
                bill = uiState.editingBill,
                currencySymbol = uiState.currencySymbol,
                onDismiss = { viewModel.dismissAddEditDialog() },
                onSave = { name, amount, dayDue, icon, isRecurring ->
                    val existingBill = uiState.editingBill
                    if (existingBill != null) {
                        viewModel.updateBill(existingBill, name, amount, dayDue, icon, isRecurring)
                    } else {
                        viewModel.addBill(name, amount, dayDue, icon, isRecurring)
                    }
                }
            )
        }
        
        // Delete Confirmation Dialog
        if (uiState.showDeleteConfirmation && uiState.billToDelete != null) {
            DeleteConfirmationDialog(
                billName = uiState.billToDelete!!.name,
                onConfirm = { viewModel.confirmDelete() },
                onDismiss = { viewModel.dismissDeleteConfirmation() }
            )
        }
    }
}

@Composable
private fun ManageBillsHeader(
    onNavigateBack: () -> Unit,
    totalBills: Int,
    totalEstimated: Double,
    currencySymbol: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Manage Bills",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "$totalBills bills • $currencySymbol${"%.0f".format(totalEstimated)}/month",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun BillsSummaryCards(
    recurringCount: Int,
    oneTimeCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Recurring Card
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = AccentCyan.copy(alpha = 0.15f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "$recurringCount",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan
                    )
                    Text(
                        text = "Recurring",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
        
        // One-Time Card
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = AccentAmber.copy(alpha = 0.15f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.RepeatOne,
                    contentDescription = null,
                    tint = AccentAmber,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "$oneTimeCount",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentAmber
                    )
                    Text(
                        text = "One-Time",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyBillsState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Receipt,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Bills Yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap + to add your first bill",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BillItemCard(
    bill: FixedBill,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = bill.icon,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Bill Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = bill.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Recurring indicator
                    if (bill.isRecurring) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Recurring",
                            tint = AccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.RepeatOne,
                            contentDescription = "One-Time",
                            tint = AccentAmber,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Due day ${bill.dayDue}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
            
            // Amount
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "$currencySymbol${"%.0f".format(bill.estimatedAmount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan
                )
                
                Text(
                    text = if (bill.isRecurring) "/month" else "one-time",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            
            // Delete Button
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = AccentRed.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AddEditBillDialog(
    bill: FixedBill?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (name: String, amount: Double, dayDue: Int, icon: String, isRecurring: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(bill?.name ?: "") }
    var amountText by remember { mutableStateOf(bill?.estimatedAmount?.let { "%.0f".format(it) } ?: "") }
    var dayDue by remember { mutableIntStateOf(bill?.dayDue ?: 1) }
    var selectedIcon by remember { mutableStateOf(bill?.icon ?: "📄") }
    var isRecurring by remember { mutableStateOf(bill?.isRecurring ?: true) }
    
    val isEditing = bill != null
    val canSave = name.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0
    
    // Icon options
    val iconOptions = listOf("🏠", "⚡", "🔥", "💧", "📶", "📱", "🛡️", "📺", "💪", "🚗", "🏦", "📄")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = {
            Text(
                text = if (isEditing) "Edit Bill" else "Add Bill",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        // Auto-suggest icon based on name
                        if (!isEditing) {
                            selectedIcon = FixedBill.suggestIcon(it)
                        }
                    },
                    label = { Text("Bill Name") },
                    placeholder = { Text("e.g., Rent, Electricity") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = dialogTextFieldColors()
                )
                
                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount ($currencySymbol)") },
                    placeholder = { Text("0") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = dialogTextFieldColors()
                )
                
                // Day Due
                OutlinedTextField(
                    value = dayDue.toString(),
                    onValueChange = { 
                        val newDay = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 1
                        dayDue = newDay.coerceIn(1, 31)
                    },
                    label = { Text("Due Day (1-31)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = dialogTextFieldColors()
                )
                
                // Icon Selector
                Text(
                    text = "Icon",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    iconOptions.take(6).forEach { icon ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selectedIcon == icon) AccentCyan.copy(alpha = 0.3f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .clickable { selectedIcon = icon },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = icon, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    iconOptions.drop(6).forEach { icon ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selectedIcon == icon) AccentCyan.copy(alpha = 0.3f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .clickable { selectedIcon = icon },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = icon, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                
                // Recurring Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isRecurring = !isRecurring }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isRecurring,
                        onCheckedChange = { isRecurring = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = AccentCyan,
                            uncheckedColor = Color.White.copy(alpha = 0.5f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Recurring Monthly",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Text(
                            text = if (isRecurring) "Bill will reset each month" else "One-time expense",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    onSave(name, amount, dayDue, selectedIcon, isRecurring)
                },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
            ) {
                Text(
                    text = if (isEditing) "Update" else "Add",
                    color = Color.Black
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
private fun DeleteConfirmationDialog(
    billName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = {
            Text(
                text = "Delete Bill?",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete \"$billName\"? This action cannot be undone.",
                color = Color.White.copy(alpha = 0.7f)
            )
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
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
private fun dialogTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentCyan,
    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
    focusedLabelColor = AccentCyan,
    unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = AccentCyan,
    focusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
    unfocusedPlaceholderColor = Color.White.copy(alpha = 0.3f)
)
