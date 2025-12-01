package site.giboworks.budgettracker.presentation.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import site.giboworks.budgettracker.domain.model.Currency
import site.giboworks.budgettracker.ui.theme.BudgetTrackerTheme

// ==================== GRADIENT COLORS ====================
private val GradientStart = Color(0xFF1A1A2E)
private val GradientMid = Color(0xFF16213E)
private val GradientEnd = Color(0xFF0F3460)

private val AccentCyan = Color(0xFF56CCF2)
private val AccentPurple = Color(0xFF9C27B0)
private val AccentGreen = Color(0xFF00E676)

/**
 * Main Onboarding Screen with multi-step wizard using HorizontalPager.
 * 
 * Steps:
 * 1. Welcome & Permission
 * 2. Income & Pay Day
 * 3. Expenses & Savings Goal (with Daily Allowance preview)
 * 4. Completion
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onOnboardingComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(
        initialPage = uiState.currentStep,
        pageCount = { uiState.totalSteps }
    )
    val coroutineScope = rememberCoroutineScope()
    
    // Sync pager with viewmodel state
    LaunchedEffect(uiState.currentStep) {
        if (pagerState.currentPage != uiState.currentStep) {
            pagerState.animateScrollToPage(uiState.currentStep)
        }
    }
    
    // Navigate to dashboard when complete
    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            onOnboardingComplete()
        }
    }
    
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
            modifier = Modifier.fillMaxSize()
        ) {
            // Progress Indicator
            OnboardingProgressIndicator(
                currentStep = pagerState.currentPage,
                totalSteps = uiState.totalSteps,
                modifier = Modifier.padding(top = 48.dp, start = 24.dp, end = 24.dp)
            )
            
            // Pager Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = false // Control navigation through buttons
            ) { page ->
                when (page) {
                    0 -> WelcomeStep(
                        isPermissionGranted = uiState.notificationPermissionGranted,
                        onEnablePermission = { viewModel.openNotificationListenerSettings() },
                        onCheckPermission = { viewModel.checkNotificationPermission() }
                    )
                    1 -> IncomeStep(
                        monthlyIncome = uiState.monthlyIncome,
                        selectedCurrency = uiState.selectedCurrency,
                        payDay = uiState.payDay,
                        incomeError = uiState.incomeError,
                        availableCurrencies = viewModel.availableCurrencies,
                        payDayOptions = viewModel.payDayOptions,
                        onIncomeChange = viewModel::updateMonthlyIncome,
                        onCurrencyChange = viewModel::updateCurrency,
                        onPayDayChange = viewModel::updatePayDay
                    )
                    2 -> ExpensesStep(
                        bills = uiState.bills,
                        savingsTarget = uiState.savingsTarget,
                        currency = uiState.selectedCurrency,
                        dailyAllowancePreview = uiState.dailyAllowancePreview,
                        totalExpenses = uiState.fixedExpensesTotal,
                        expensesError = uiState.expensesError,
                        onAddBill = viewModel::addBill,
                        onRemoveBill = viewModel::removeBill,
                        onBillNameChange = viewModel::updateBillName,
                        onBillAmountChange = viewModel::updateBillAmount,
                        onSavingsChange = viewModel::updateSavingsTarget
                    )
                    3 -> CompletionStep(
                        isSaving = uiState.isSaving,
                        dailyAllowance = uiState.dailyAllowancePreview,
                        currency = uiState.selectedCurrency
                    )
                }
            }
            
            // Navigation Buttons
            OnboardingNavigationButtons(
                currentStep = pagerState.currentPage,
                totalSteps = uiState.totalSteps,
                canProceed = when (pagerState.currentPage) {
                    1 -> uiState.monthlyIncome.isNotBlank()
                    else -> true
                },
                isSaving = uiState.isSaving,
                onBack = {
                    coroutineScope.launch {
                        viewModel.previousStep()
                    }
                },
                onNext = {
                    if (pagerState.currentPage == uiState.totalSteps - 1) {
                        viewModel.completeOnboarding()
                    } else {
                        viewModel.nextStep()
                    }
                },
                onSkip = {
                    if (pagerState.currentPage == 0) {
                        viewModel.nextStep()
                    }
                },
                modifier = Modifier.padding(24.dp)
            )
        }
    }
}

// ==================== PROGRESS INDICATOR ====================

@Composable
private fun OnboardingProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(totalSteps) { step ->
            val isActive = step <= currentStep
            val progress by animateFloatAsState(
                targetValue = if (isActive) 1f else 0f,
                animationSpec = tween(300),
                label = "progress"
            )
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(4.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(AccentCyan, AccentPurple)
                            )
                        )
                )
            }
        }
    }
}

// ==================== STEP 1: WELCOME ====================

@Composable
private fun WelcomeStep(
    isPermissionGranted: Boolean,
    onEnablePermission: () -> Unit,
    onCheckPermission: () -> Unit
) {
    val context = LocalContext.current
    
    // Check permission on resume
    LaunchedEffect(Unit) {
        onCheckPermission()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        // Welcome Icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(AccentCyan, AccentPurple)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPermissionGranted) Icons.Filled.Check else Icons.Filled.Notifications,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Welcome to Budget Tracker",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Let's set up automatic expense tracking from your bank notifications",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Permission Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalance,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Bank Notification Access",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "We'll automatically read your bank notifications to track expenses. Your data stays on your device - we never upload it anywhere.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isPermissionGranted) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Permission granted!",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = AccentGreen
                        )
                    }
                } else {
                    Button(
                        onClick = onEnablePermission,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentCyan
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Enable Notification Access",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "You can skip this step and enable it later in Settings",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

// ==================== STEP 2: INCOME ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncomeStep(
    monthlyIncome: String,
    selectedCurrency: Currency,
    payDay: Int,
    incomeError: String?,
    availableCurrencies: List<Currency>,
    payDayOptions: List<Int>,
    onIncomeChange: (String) -> Unit,
    onCurrencyChange: (Currency) -> Unit,
    onPayDayChange: (Int) -> Unit
) {
    var currencyExpanded by remember { mutableStateOf(false) }
    var payDayExpanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Icon(
            imageVector = Icons.Outlined.AttachMoney,
            contentDescription = null,
            tint = AccentCyan,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Your Income",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Tell us about your monthly income to calculate your daily budget",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Monthly Income Field
        OutlinedTextField(
            value = monthlyIncome,
            onValueChange = onIncomeChange,
            label = { Text("Monthly Income") },
            placeholder = { Text("e.g., 100000") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.TrendingUp,
                    contentDescription = null,
                    tint = AccentCyan
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = incomeError != null,
            supportingText = incomeError?.let { { Text(it, color = Color(0xFFFF5252)) } },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentCyan,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                focusedLabelColor = AccentCyan,
                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = AccentCyan
            ),
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Currency Selector
        ExposedDropdownMenuBox(
            expanded = currencyExpanded,
            onExpandedChange = { currencyExpanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = "${selectedCurrency.name} (${selectedCurrency.symbol})",
                onValueChange = {},
                readOnly = true,
                label = { Text("Currency") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.CurrencyExchange,
                        contentDescription = null,
                        tint = AccentCyan
                    )
                },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedLabelColor = AccentCyan,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )
            
            ExposedDropdownMenu(
                expanded = currencyExpanded,
                onDismissRequest = { currencyExpanded = false }
            ) {
                availableCurrencies.forEach { currency ->
                    DropdownMenuItem(
                        text = { Text("${currency.name} (${currency.symbol})") },
                        onClick = {
                            onCurrencyChange(currency)
                            currencyExpanded = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Pay Day Selector
        ExposedDropdownMenuBox(
            expanded = payDayExpanded,
            onExpandedChange = { payDayExpanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = "Day $payDay of each month",
                onValueChange = {},
                readOnly = true,
                label = { Text("Pay Day") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = AccentCyan
                    )
                },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = payDayExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedLabelColor = AccentCyan,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
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
}

// ==================== STEP 3: BILLS & SAVINGS (Dynamic List) ====================

@Composable
private fun ExpensesStep(
    bills: List<BillEntry>,
    savingsTarget: String,
    currency: Currency,
    dailyAllowancePreview: Double,
    totalExpenses: Double,
    expensesError: String?,
    onAddBill: () -> Unit,
    onRemoveBill: (Long) -> Unit,
    onBillNameChange: (Long, String) -> Unit,
    onBillAmountChange: (Long, String) -> Unit,
    onSavingsChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Icon(
            imageVector = Icons.Outlined.Savings,
            contentDescription = null,
            tint = AccentPurple,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Your Fixed Bills",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Add your recurring monthly expenses",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Bills List
        bills.forEachIndexed { index, bill ->
            BillEntryRow(
                bill = bill,
                currency = currency,
                showRemoveButton = bills.size > 1,
                onNameChange = { onBillNameChange(bill.id, it) },
                onAmountChange = { onBillAmountChange(bill.id, it) },
                onRemove = { onRemoveBill(bill.id) }
            )
            
            if (index < bills.size - 1) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Add Bill Button
        OutlinedButton(
            onClick = onAddBill,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = AccentCyan
            ),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                brush = Brush.horizontalGradient(listOf(AccentCyan.copy(alpha = 0.5f), AccentPurple.copy(alpha = 0.5f)))
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Another Bill")
        }
        
        // Total Fixed Expenses
        if (totalExpenses > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = AccentPurple.copy(alpha = 0.2f)
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
                    Text(
                        text = "Total Fixed Expenses",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "${currency.symbol}%.0f".format(totalExpenses),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AccentPurple
                    )
                }
            }
        }
        
        // Error message
        expensesError?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFF5252)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Savings Target Field
        OutlinedTextField(
            value = savingsTarget,
            onValueChange = onSavingsChange,
            label = { Text("Monthly Savings Goal") },
            placeholder = { Text("How much to save each month") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Savings,
                    contentDescription = null,
                    tint = AccentGreen
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentGreen,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                focusedLabelColor = AccentGreen,
                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = AccentGreen
            ),
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Daily Allowance Preview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Your Daily Allowance",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "${currency.symbol}%.0f".format(dailyAllowancePreview),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "per day",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

/**
 * Single bill entry row with name and amount fields
 */
@Composable
private fun BillEntryRow(
    bill: BillEntry,
    currency: Currency,
    showRemoveButton: Boolean,
    onNameChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onRemove: () -> Unit
) {
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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Text(
                text = bill.icon,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(end = 8.dp)
            )
            
            // Name field
            OutlinedTextField(
                value = bill.name,
                onValueChange = onNameChange,
                label = { Text("Bill Name") },
                placeholder = { Text("e.g., Rent") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPurple,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedLabelColor = AccentPurple,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = AccentPurple
                ),
                shape = RoundedCornerShape(8.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Amount field
            OutlinedTextField(
                value = bill.amount,
                onValueChange = onAmountChange,
                label = { Text(currency.symbol) },
                placeholder = { Text("0") },
                modifier = Modifier.width(100.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPurple,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedLabelColor = AccentPurple,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = AccentPurple
                ),
                shape = RoundedCornerShape(8.dp)
            )
            
            // Remove button
            if (showRemoveButton) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remove bill",
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

// ==================== STEP 4: COMPLETION ====================

@Composable
private fun CompletionStep(
    isSaving: Boolean,
    dailyAllowance: Double,
    currency: Currency
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                color = AccentCyan,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Setting up your budget...",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Rocket,
                contentDescription = null,
                tint = AccentGreen,
                modifier = Modifier.size(80.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "You're All Set!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Your daily budget is",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${currency.symbol}%.0f".format(dailyAllowance),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = AccentCyan
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Start spending mindfully!\nWe'll track your bank notifications automatically.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==================== NAVIGATION BUTTONS ====================

@Composable
private fun OnboardingNavigationButtons(
    currentStep: Int,
    totalSteps: Int,
    canProceed: Boolean,
    isSaving: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button or Skip
        if (currentStep > 0) {
            OutlinedButton(
                onClick = onBack,
                enabled = !isSaving,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.5f)))
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Back")
            }
        } else {
            TextButton(
                onClick = onSkip,
                enabled = !isSaving
            ) {
                Text(
                    text = "Skip",
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
        
        // Next/Finish Button
        Button(
            onClick = onNext,
            enabled = canProceed && !isSaving,
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentCyan,
                disabledContainerColor = AccentCyan.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (currentStep == totalSteps - 1) "Let's Go!" else "Continue",
                    fontWeight = FontWeight.SemiBold
                )
                if (currentStep < totalSteps - 1) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomeStepPreview() {
    BudgetTrackerTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(GradientStart, GradientMid, GradientEnd)
                    )
                )
        ) {
            WelcomeStep(
                isPermissionGranted = false,
                onEnablePermission = {},
                onCheckPermission = {}
            )
        }
    }
}
