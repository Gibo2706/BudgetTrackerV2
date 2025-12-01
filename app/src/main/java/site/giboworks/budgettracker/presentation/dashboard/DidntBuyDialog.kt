package site.giboworks.budgettracker.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import site.giboworks.budgettracker.ui.theme.BudgetTrackerTheme

/**
 * "Didn't Buy" Dialog - The Anti-Spend Feature
 * 
 * Celebrates the user's willpower when they resist an impulse purchase.
 * This is positive reinforcement - making NOT spending feel rewarding.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DidntBuyDialog(
    onDismiss: () -> Unit,
    onSave: (itemName: String, amountSaved: Double) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    
    var itemName by remember { mutableStateOf("") }
    var amountString by remember { mutableStateOf("0") }
    
    // Quick preset amounts for common temptations
    val quickAmounts = listOf(
        "☕" to 350.0,    // Coffee
        "🍔" to 700.0,   // Fast food
        "🛒" to 1500.0,  // Impulse shopping
        "🎮" to 3000.0   // Entertainment
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
    
    fun setQuickAmount(amount: Double) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        amountString = amount.toInt().toString()
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 8.dp)
                .navigationBarsPadding()
        ) {
            // Header with celebration theme
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = null,
                        tint = Color(0xFF9C27B0),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "You Didn't Buy It!",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Pay yourself instead 💜",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Item Name Input
            Text(
                text = "What did you resist?",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = itemName,
                onValueChange = { itemName = it },
                placeholder = { 
                    Text(
                        text = "e.g., \"Third coffee today\" or \"Impulse buy on Aliexpress\"",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF9C27B0),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Amount Display
            Text(
                text = "Amount Saved",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFF9C27B0).copy(alpha = 0.1f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "+",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF9C27B0)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatDisplayAmount(amountString),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp
                        ),
                        color = Color(0xFF9C27B0)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RSD",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF9C27B0).copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Quick Amount Buttons
            Text(
                text = "Quick Presets",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickAmounts.forEach { (emoji, amount) ->
                    FilledTonalButton(
                        onClick = { setQuickAmount(amount) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = emoji, fontSize = 18.sp)
                            Text(
                                text = "${amount.toInt()}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Numeric Keypad
            MiniNumericKeypad(
                onNumberClick = ::onNumberClick,
                onBackspace = ::onBackspace,
                onClear = ::onClear
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Save Button - Celebratory Style
            Button(
                onClick = {
                    val amount = amountString.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        val name = itemName.ifBlank { "Resisted temptation" }
                        onSave(name, amount)
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
                    containerColor = Color(0xFF9C27B0)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(text = "🎉", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pay Yourself!",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
            
            // Motivational message
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "+15 credits for your willpower! 💪",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFFD700),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MiniNumericKeypad(
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
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { label ->
                    MiniKeypadButton(
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
private fun MiniKeypadButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isSpecial = label == "C" || label == "⌫"
    
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(10.dp),
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
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.error
            )
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
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

private fun formatDisplayAmount(amount: String): String {
    return try {
        val number = amount.toDoubleOrNull() ?: 0.0
        if (amount.contains(".")) {
            amount
        } else {
            "%,.0f".format(number)
        }
    } catch (e: Exception) {
        amount
    }
}

@Preview(showBackground = true)
@Composable
private fun DidntBuyDialogPreview() {
    BudgetTrackerTheme(darkTheme = true) {
        // Preview only - won't show bottom sheet properly
    }
}
