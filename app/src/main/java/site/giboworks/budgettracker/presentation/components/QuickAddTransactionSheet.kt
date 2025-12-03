package site.giboworks.budgettracker.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import site.giboworks.budgettracker.domain.model.Currency
import site.giboworks.budgettracker.domain.model.TransactionCategory
import site.giboworks.budgettracker.ui.theme.BudgetTrackerTheme
import java.time.LocalTime

/**
 * Quick Add Transaction Bottom Sheet
 * 
 * Layout Structure:
 * - Section A (Top/Fixed): Amount Display + Emergency Toggle + Description
 * - Section B (Middle/Weight 1f): Category Selector (scrollable)
 * - Section C (Bottom/Fixed): Custom Numeric Keypad (3x4 grid with Save button)
 * 
 * Features:
 * - Custom numeric keypad (no system keyboard for amount)
 * - Emergency toggle (affects Savings instead of Daily budget)
 * - Optional description field (only field that uses system keyboard)
 * - Time-based category predictions
 * - Haptic feedback on all interactions
 * 
 * @param onDismiss Called when sheet is dismissed
 * @param onAddTransaction Called with (amount, category, description, isEmergency)
 * @param currency Currency for display
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickAddTransactionSheet(
    onDismiss: () -> Unit,
    onAddTransaction: (amount: Double, category: TransactionCategory, description: String, isEmergency: Boolean) -> Unit,
    currency: Currency = Currency.RSD,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current
    
    // State
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<TransactionCategory?>(null) }
    var description by remember { mutableStateOf("") }
    var isEmergency by remember { mutableStateOf(false) }
    
    // Predicted categories based on time of day
    val predictedCategories = remember { getPredictedCategories() }
    
    // Can save only if amount > 0 AND category is selected
    val canSave = amountText.isNotEmpty() && 
                  amountText.toDoubleOrNull()?.let { it > 0 } == true && 
                  selectedCategory != null
    
    // Colors based on emergency state
    val amountColor = when {
        amountText.isEmpty() -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        isEmergency -> Color(0xFFFFB300) // Amber for emergency
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .navigationBarsPadding()
                .imePadding()
        ) {
            // ═══════════════════════════════════════════════════════════════
            // SECTION A: Header + Amount + Emergency + Description (Fixed Top)
            // ═══════════════════════════════════════════════════════════════
            
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEmergency) "🚨 Emergency Expense" else "Add Expense",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isEmergency) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close, 
                        contentDescription = "Close", 
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // Amount Display (Large, prominent)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(
                        if (isEmergency) Color(0xFFFFB300).copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = formatDisplayAmount(if (amountText.isEmpty()) "0" else amountText),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 48.sp
                        ),
                        color = amountColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currency.symbol,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            
            // Emergency Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .background(
                        if (isEmergency) Color(0xFFFFB300).copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { 
                        isEmergency = !isEmergency
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🚨", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Unexpected / Emergency?",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = if (isEmergency) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isEmergency) "Deducted from Savings (Shield)" 
                                   else "Deducted from Daily budget",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                Switch(
                    checked = isEmergency,
                    onCheckedChange = { 
                        isEmergency = it
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFFFB300),
                        checkedTrackColor = Color(0xFFFFB300).copy(alpha = 0.5f)
                    )
                )
            }
            
            // Description Field (Optional - only field with system keyboard)
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isEmergency) Color(0xFFFFB300) else MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // ═══════════════════════════════════════════════════════════════
            // SECTION B: Category Selector (Scrollable Middle - Weight 1f)
            // ═══════════════════════════════════════════════════════════════
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                // Predicted Categories
                Text(
                    text = "Suggested",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    predictedCategories.forEach { category ->
                        CategoryChip(
                            category = category,
                            selected = selectedCategory == category,
                            isPredicted = true,
                            onClick = {
                                selectedCategory = category
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // All Categories
                Text(
                    text = "All Categories",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    getExpenseCategories()
                        .filterNot { it in predictedCategories }
                        .forEach { category ->
                            CategoryChip(
                                category = category,
                                selected = selectedCategory == category,
                                isPredicted = false,
                                onClick = {
                                    selectedCategory = category
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            )
                        }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Credits hint
            AnimatedVisibility(
                visible = canSave,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = if (isEmergency) "⭐ +5 credits (emergency)" else "✨ +10 credits",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isEmergency) Color(0xFFFFB300) else Color(0xFFFFD700),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    textAlign = TextAlign.Center
                )
            }
            
            // ═══════════════════════════════════════════════════════════════
            // SECTION C: Custom Numeric Keypad (Fixed Bottom)
            // Grid: 3x4 -> [1,2,3], [4,5,6], [7,8,9], [C,0,✔]
            // ═══════════════════════════════════════════════════════════════
            
            NumericKeypad(
                onNumberClick = { digit ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (amountText.length < 10) {
                        amountText += digit
                    }
                },
                onClearClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (amountText.isNotEmpty()) {
                        amountText = amountText.dropLast(1)
                    }
                },
                onSaveClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0 && selectedCategory != null) {
                        onAddTransaction(amount, selectedCategory!!, description, isEmergency)
                        onDismiss()
                    }
                },
                canSave = canSave,
                isEmergency = isEmergency,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// NUMERIC KEYPAD COMPONENT
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun NumericKeypad(
    onNumberClick: (String) -> Unit,
    onClearClick: () -> Unit,
    onSaveClick: () -> Unit,
    canSave: Boolean,
    isEmergency: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 1: 1, 2, 3
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeypadButton(text = "1", onClick = { onNumberClick("1") }, modifier = Modifier.weight(1f))
            KeypadButton(text = "2", onClick = { onNumberClick("2") }, modifier = Modifier.weight(1f))
            KeypadButton(text = "3", onClick = { onNumberClick("3") }, modifier = Modifier.weight(1f))
        }
        
        // Row 2: 4, 5, 6
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeypadButton(text = "4", onClick = { onNumberClick("4") }, modifier = Modifier.weight(1f))
            KeypadButton(text = "5", onClick = { onNumberClick("5") }, modifier = Modifier.weight(1f))
            KeypadButton(text = "6", onClick = { onNumberClick("6") }, modifier = Modifier.weight(1f))
        }
        
        // Row 3: 7, 8, 9
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeypadButton(text = "7", onClick = { onNumberClick("7") }, modifier = Modifier.weight(1f))
            KeypadButton(text = "8", onClick = { onNumberClick("8") }, modifier = Modifier.weight(1f))
            KeypadButton(text = "9", onClick = { onNumberClick("9") }, modifier = Modifier.weight(1f))
        }
        
        // Row 4: C (Clear), 0, ✔ (Save)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Clear button (C / Backspace)
            KeypadButton(
                text = "C",
                onClick = onClearClick,
                modifier = Modifier.weight(1f),
                backgroundColor = MaterialTheme.colorScheme.errorContainer,
                textColor = MaterialTheme.colorScheme.onErrorContainer
            )
            
            // 0 button
            KeypadButton(
                text = "0",
                onClick = { onNumberClick("0") },
                modifier = Modifier.weight(1f)
            )
            
            // Save button (Checkmark)
            SaveButton(
                onClick = onSaveClick,
                enabled = canSave,
                isEmergency = isEmergency,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 24.sp
            ),
            color = textColor
        )
    }
}

@Composable
private fun SaveButton(
    onClick: () -> Unit,
    enabled: Boolean,
    isEmergency: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant
        isEmergency -> Color(0xFFFFB300)
        else -> Color(0xFF4CAF50)
    }
    
    val iconColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        else -> Color.White
    }
    
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isEmergency && enabled) Icons.Default.Star else Icons.Default.Check,
            contentDescription = "Save",
            tint = iconColor,
            modifier = Modifier.size(28.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// CATEGORY CHIP COMPONENT
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun CategoryChip(
    category: TransactionCategory,
    selected: Boolean,
    isPredicted: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = category.emoji, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = if (isPredicted) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.height(36.dp)
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// HELPER FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

private fun formatDisplayAmount(amount: String): String {
    return try {
        val number = amount.toLongOrNull() ?: 0L
        "%,d".format(number).replace(",", ".")
    } catch (e: Exception) {
        amount
    }
}

private fun getPredictedCategories(): List<TransactionCategory> {
    val hour = LocalTime.now().hour
    
    return when {
        hour in 6..10 -> listOf(
            TransactionCategory.FOOD_COFFEE,
            TransactionCategory.TRANSPORT_PUBLIC,
            TransactionCategory.TRANSPORT_FUEL,
            TransactionCategory.FOOD_GROCERIES
        )
        hour in 11..14 -> listOf(
            TransactionCategory.FOOD_RESTAURANTS,
            TransactionCategory.FOOD_COFFEE,
            TransactionCategory.SHOPPING_OTHER
        )
        hour in 15..18 -> listOf(
            TransactionCategory.FOOD_GROCERIES,
            TransactionCategory.SHOPPING_OTHER,
            TransactionCategory.TRANSPORT_TAXI
        )
        hour in 19..22 -> listOf(
            TransactionCategory.FOOD_RESTAURANTS,
            TransactionCategory.ENTERTAINMENT_EVENTS,
            TransactionCategory.ENTERTAINMENT_STREAMING
        )
        else -> listOf(
            TransactionCategory.OTHER_EXPENSE,
            TransactionCategory.TRANSPORT_TAXI,
            TransactionCategory.FOOD_RESTAURANTS
        )
    }
}

private fun getExpenseCategories(): List<TransactionCategory> {
    return TransactionCategory.entries.filter { 
        it.isExpense && it != TransactionCategory.MICRO_SAVINGS 
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// PREVIEW
// ═══════════════════════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun QuickAddPreview() {
    BudgetTrackerTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Amount preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFFFFB300).copy(alpha = 0.1f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "1.500",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFFFFB300)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Keypad preview
                NumericKeypad(
                    onNumberClick = {},
                    onClearClick = {},
                    onSaveClick = {},
                    canSave = true,
                    isEmergency = true
                )
            }
        }
    }
}
