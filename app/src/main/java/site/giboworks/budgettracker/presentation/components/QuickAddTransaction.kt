package site.giboworks.budgettracker.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import site.giboworks.budgettracker.domain.model.Currency
import site.giboworks.budgettracker.domain.model.TransactionCategory
import site.giboworks.budgettracker.ui.theme.BudgetTrackerTheme
import java.time.LocalTime

/**
 * Quick Add Transaction Bottom Sheet
 * Optimized for speed with predictive category selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddTransactionSheet(
    onDismiss: () -> Unit,
    onAddTransaction: (amount: Double, category: TransactionCategory, description: String) -> Unit,
    currency: Currency = Currency.RSD,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<TransactionCategory?>(null) }
    var description by remember { mutableStateOf("") }
    
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Predict category based on time of day
    val predictedCategories = remember { getPredictedCategories() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
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
                Text(
                    text = "Quick Add",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Amount Input
            AmountInputField(
                value = amountText,
                onValueChange = { amountText = it },
                currency = currency,
                focusRequester = focusRequester,
                onDone = { keyboardController?.hide() }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Suggested Categories (Time-based prediction)
            Text(
                text = "Suggested",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            CategoryGrid(
                categories = predictedCategories,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // All Categories
            Text(
                text = "All Categories",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            CategoryGrid(
                categories = getExpenseCategories().filterNot { it in predictedCategories },
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Optional Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Add Button
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0 && selectedCategory != null) {
                        onAddTransaction(amount, selectedCategory!!, description)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = amountText.toDoubleOrNull()?.let { it > 0 } == true && selectedCategory != null,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add Expense",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
            
            // Credits hint
            AnimatedVisibility(
                visible = amountText.isNotEmpty() && selectedCategory != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "✨ +10 credits for logging",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFD700)
                    )
                }
            }
        }
    }
}

@Composable
private fun AmountInputField(
    value: String,
    onValueChange: (String) -> Unit,
    currency: Currency,
    focusRequester: FocusRequester,
    onDone: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                // Only allow numbers and one decimal point
                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                    onValueChange(newValue)
                }
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            textStyle = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp
            ),
            placeholder = {
                Text(
                    text = "0",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onDone() }
            ),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = currency.symbol,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryGrid(
    categories: List<TransactionCategory>,
    selectedCategory: TransactionCategory?,
    onCategorySelected: (TransactionCategory) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            CategoryChip(
                category = category,
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) }
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
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = category.emoji)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

/**
 * Get predicted categories based on time of day
 */
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

/**
 * Get all expense categories
 */
private fun getExpenseCategories(): List<TransactionCategory> {
    return TransactionCategory.entries.filter { it.isExpense && it != TransactionCategory.MICRO_SAVINGS }
}

// ==================== PREVIEW ====================

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun QuickAddPreview() {
    BudgetTrackerTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                AmountInputField(
                    value = "1500",
                    onValueChange = {},
                    currency = Currency.RSD,
                    focusRequester = remember { FocusRequester() },
                    onDone = {}
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Suggested",
                    style = MaterialTheme.typography.labelLarge
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                CategoryGrid(
                    categories = listOf(
                        TransactionCategory.FOOD_COFFEE,
                        TransactionCategory.TRANSPORT_PUBLIC,
                        TransactionCategory.FOOD_GROCERIES
                    ),
                    selectedCategory = TransactionCategory.FOOD_COFFEE,
                    onCategorySelected = {}
                )
            }
        }
    }
}
