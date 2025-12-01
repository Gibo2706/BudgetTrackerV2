package site.giboworks.budgettracker.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

// Theme Colors
private val GradientStart = Color(0xFF1A1A2E)
private val GradientMid = Color(0xFF16213E)
private val GradientEnd = Color(0xFF0F3460)
private val AccentCyan = Color(0xFF56CCF2)
private val AccentPurple = Color(0xFF9C27B0)
private val AccentGreen = Color(0xFF00E676)
private val AccentAmber = Color(0xFFFFB74D)
private val SurfaceCard = Color(0xFF21262D)

/**
 * Full Settings Screen with Blueprint, Well-being, and App sections.
 * 
 * Sections:
 * 1. My Blueprint - Edit budget configuration
 * 2. Well-being - Ghost Mode toggle
 * 3. App - Notification settings
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit = {}
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            SettingsHeader(onNavigateBack = onNavigateBack)
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Section 1: My Blueprint
                BlueprintSection(
                    uiState = uiState,
                    availableCurrencies = viewModel.availableCurrencies,
                    payDayOptions = viewModel.payDayOptions,
                    onIncomeChange = viewModel::updateMonthlyIncome,
                    onCurrencyChange = viewModel::updateCurrency,
                    onPayDayChange = viewModel::updatePayDay,
                    onExpensesChange = viewModel::updateFixedExpenses,
                    onSavingsChange = viewModel::updateSavingsGoal,
                    onSave = viewModel::saveBlueprint
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Section 2: Well-being
                WellbeingSection(
                    isGhostModeEnabled = uiState.isGhostModeEnabled,
                    onGhostModeToggle = viewModel::toggleGhostMode
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Section 3: App Settings
                AppSettingsSection(
                    onNotificationSettingsClick = onNavigateToNotificationSettings
                )
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SettingsHeader(onNavigateBack: () -> Unit) {
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
        
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

// ==================== SECTION 1: BLUEPRINT ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlueprintSection(
    uiState: SettingsUiState,
    availableCurrencies: List<site.giboworks.budgettracker.domain.model.Currency>,
    payDayOptions: List<Int>,
    onIncomeChange: (String) -> Unit,
    onCurrencyChange: (site.giboworks.budgettracker.domain.model.Currency) -> Unit,
    onPayDayChange: (Int) -> Unit,
    onExpensesChange: (String) -> Unit,
    onSavingsChange: (String) -> Unit,
    onSave: () -> Unit
) {
    var currencyExpanded by remember { mutableStateOf(false) }
    var payDayExpanded by remember { mutableStateOf(false) }
    
    SectionCard(
        title = "My Blueprint",
        subtitle = "Your financial foundation",
        icon = Icons.Filled.TrendingUp,
        iconTint = AccentCyan
    ) {
        // Monthly Income
        OutlinedTextField(
            value = uiState.monthlyIncome,
            onValueChange = onIncomeChange,
            label = { Text("Monthly Income") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.TrendingUp,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(20.dp)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = settingsTextFieldColors(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Currency and Pay Day Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Currency
            ExposedDropdownMenuBox(
                expanded = currencyExpanded,
                onExpandedChange = { currencyExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = uiState.selectedCurrency.code,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Currency") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.CurrencyExchange,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                    modifier = Modifier.menuAnchor(),
                    colors = settingsTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                ExposedDropdownMenu(
                    expanded = currencyExpanded,
                    onDismissRequest = { currencyExpanded = false }
                ) {
                    availableCurrencies.forEach { currency ->
                        DropdownMenuItem(
                            text = { Text("${currency.code} (${currency.symbol})") },
                            onClick = {
                                onCurrencyChange(currency)
                                currencyExpanded = false
                            }
                        )
                    }
                }
            }
            
            // Pay Day
            ExposedDropdownMenuBox(
                expanded = payDayExpanded,
                onExpandedChange = { payDayExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = "Day ${uiState.payDay}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Pay Day") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = payDayExpanded) },
                    modifier = Modifier.menuAnchor(),
                    colors = settingsTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                ExposedDropdownMenu(
                    expanded = payDayExpanded,
                    onDismissRequest = { payDayExpanded = false }
                ) {
                    payDayOptions.forEach { day ->
                        DropdownMenuItem(
                            text = { Text("Day $day") },
                            onClick = {
                                onPayDayChange(day)
                                payDayExpanded = false
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Fixed Expenses
        OutlinedTextField(
            value = uiState.fixedExpenses,
            onValueChange = onExpensesChange,
            label = { Text("Fixed Monthly Expenses") },
            placeholder = { Text("Rent, bills, subscriptions...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Home,
                    contentDescription = null,
                    tint = AccentPurple,
                    modifier = Modifier.size(20.dp)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = settingsTextFieldColors(focusedColor = AccentPurple),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Savings Goal
        OutlinedTextField(
            value = uiState.savingsGoal,
            onValueChange = onSavingsChange,
            label = { Text("Monthly Savings Goal") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Savings,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(20.dp)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = settingsTextFieldColors(focusedColor = AccentGreen),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Daily Allowance Preview
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.05f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Daily Safe-to-Spend",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "${uiState.selectedCurrency.symbol}%.0f".format(uiState.dailyAllowancePreview),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan
                    )
                }
                
                Text(
                    text = "per day",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Save Button
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSaving,
            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            AnimatedVisibility(
                visible = uiState.saveSuccess,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = if (uiState.saveSuccess) "Saved!" else "Save Changes",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ==================== SECTION 2: WELL-BEING ====================

@Composable
private fun WellbeingSection(
    isGhostModeEnabled: Boolean,
    onGhostModeToggle: (Boolean) -> Unit
) {
    SectionCard(
        title = "Well-being",
        subtitle = "Your mental health matters",
        icon = Icons.Outlined.SelfImprovement,
        iconTint = AccentAmber
    ) {
        // Ghost Mode Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Nightlight,
                    contentDescription = null,
                    tint = AccentAmber,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Ghost Mode",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Text(
                        text = "Pause tracking for a mental break",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
            
            Switch(
                checked = isGhostModeEnabled,
                onCheckedChange = onGhostModeToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AccentAmber,
                    checkedTrackColor = AccentAmber.copy(alpha = 0.5f),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                )
            )
        }
        
        // Helper Text
        AnimatedVisibility(visible = isGhostModeEnabled) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentAmber.copy(alpha = 0.1f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tracking is paused. Take care of yourself - we'll be here when you're ready. 💛",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentAmber
                    )
                }
            }
        }
    }
}

// ==================== SECTION 3: APP SETTINGS ====================

@Composable
private fun AppSettingsSection(
    onNotificationSettingsClick: () -> Unit
) {
    SectionCard(
        title = "App",
        subtitle = "Notification & permissions",
        icon = Icons.Filled.Notifications,
        iconTint = AccentPurple
    ) {
        // Notification Settings
        SettingsListItem(
            title = "Notification Access",
            subtitle = "Configure bank notification reading",
            icon = Icons.Filled.Notifications,
            iconTint = AccentPurple,
            onClick = onNotificationSettingsClick
        )
    }
}

// ==================== REUSABLE COMPONENTS ====================

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))
            
            content()
        }
    }
}

@Composable
private fun SettingsListItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
        
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun settingsTextFieldColors(focusedColor: Color = AccentCyan) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = focusedColor,
    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
    focusedLabelColor = focusedColor,
    unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = focusedColor,
    focusedLeadingIconColor = focusedColor,
    unfocusedLeadingIconColor = Color.White.copy(alpha = 0.5f),
    focusedTrailingIconColor = focusedColor,
    unfocusedTrailingIconColor = Color.White.copy(alpha = 0.5f),
    focusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
    unfocusedPlaceholderColor = Color.White.copy(alpha = 0.3f)
)
