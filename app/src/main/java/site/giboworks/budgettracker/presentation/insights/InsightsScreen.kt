package site.giboworks.budgettracker.presentation.insights

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import site.giboworks.budgettracker.domain.model.CalendarDay
import site.giboworks.budgettracker.domain.model.CategoryBreakdown
import site.giboworks.budgettracker.domain.model.Currency
import site.giboworks.budgettracker.domain.model.DayStatus
import site.giboworks.budgettracker.domain.model.InsightsState
import site.giboworks.budgettracker.domain.model.SpendingVelocityPoint
import site.giboworks.budgettracker.domain.model.VelocityTrend
import java.time.format.TextStyle
import java.util.Locale

// Cyberpunk/Fitness color palette
private val NeonGreen = Color(0xFF00FF88)
private val NeonBlue = Color(0xFF00D4FF)
private val NeonPurple = Color(0xFFAA00FF)
private val NeonOrange = Color(0xFFFF9500)
private val NeonRed = Color(0xFFFF3B30)
private val DarkSurface = Color(0xFF1A1A2E)
private val DarkCard = Color(0xFF16213E)
private val GrayLine = Color(0xFF4A4A6A)

/**
 * Insights Screen - Visual Analytics Dashboard
 * 
 * Features:
 * - Spending Velocity Chart (Canvas line chart)
 * - Top Leaks Category Breakdown
 * - Consistency Calendar Heatmap
 */
@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkSurface,
                        DarkCard,
                        DarkSurface
                    )
                )
            )
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = NeonGreen
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Header
                item {
                    InsightsHeader(
                        onBackClick = onNavigateBack,
                        onRefresh = viewModel::refresh
                    )
                }
                
                // Spending Velocity Chart
                item {
                    SpendingVelocityCard(
                        currentMonth = state.currentMonthVelocity,
                        previousMonth = state.previousMonthVelocity,
                        trend = state.velocityTrend,
                        currency = state.currency
                    )
                }
                
                // Top Leaks
                item {
                    TopLeaksCard(
                        categories = state.topCategories,
                        totalSpent = state.totalSpentThisMonth,
                        currency = state.currency
                    )
                }
                
                // Consistency Calendar
                item {
                    ConsistencyCalendarCard(
                        days = state.calendarDays,
                        daysUnderBudget = state.daysUnderBudget,
                        daysOverBudget = state.daysOverBudget,
                        streak = state.currentStreak
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightsHeader(
    onBackClick: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Insights",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
                Text(
                    text = "Your spending patterns",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
        
        IconButton(onClick = onRefresh) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh",
                tint = NeonGreen
            )
        }
    }
}

// ==================== SPENDING VELOCITY CHART ====================

@Composable
private fun SpendingVelocityCard(
    currentMonth: List<SpendingVelocityPoint>,
    previousMonth: List<SpendingVelocityPoint>,
    trend: VelocityTrend,
    currency: Currency
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header with trend indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Spending Velocity",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "Current vs Last Month",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                
                TrendBadge(trend = trend)
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Canvas Line Chart
            SpendingVelocityChart(
                currentMonth = currentMonth,
                previousMonth = previousMonth,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                LegendItem(color = NeonGreen, label = "This Month")
                Spacer(modifier = Modifier.width(24.dp))
                LegendItem(color = GrayLine, label = "Last Month", isDashed = true)
            }
        }
    }
}

@Composable
private fun SpendingVelocityChart(
    currentMonth: List<SpendingVelocityPoint>,
    previousMonth: List<SpendingVelocityPoint>,
    modifier: Modifier = Modifier
) {
    if (currentMonth.isEmpty() && previousMonth.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No spending data yet",
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val padding = 40f
        
        val chartWidth = width - padding * 2
        val chartHeight = height - padding * 2
        
        // Find max value for scaling
        val allValues = currentMonth.map { it.cumulativeAmount } + previousMonth.map { it.cumulativeAmount }
        val maxValue = allValues.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
        val maxDay = maxOf(
            currentMonth.maxOfOrNull { it.dayOfMonth } ?: 1,
            previousMonth.maxOfOrNull { it.dayOfMonth } ?: 1
        )
        
        // Draw grid lines
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = padding + (chartHeight * i / gridLines)
            drawLine(
                color = Color.White.copy(alpha = 0.1f),
                start = Offset(padding, y),
                end = Offset(width - padding, y),
                strokeWidth = 1f
            )
        }
        
        // Draw previous month line (dashed, gray)
        if (previousMonth.isNotEmpty()) {
            val prevPath = Path()
            previousMonth.forEachIndexed { index, point ->
                val x = padding + (point.dayOfMonth - 1) * chartWidth / (maxDay - 1).coerceAtLeast(1)
                val y = padding + chartHeight - (point.cumulativeAmount / maxValue * chartHeight).toFloat()
                
                if (index == 0) {
                    prevPath.moveTo(x, y)
                } else {
                    prevPath.lineTo(x, y)
                }
            }
            
            drawPath(
                path = prevPath,
                color = GrayLine,
                style = Stroke(
                    width = 3f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
            )
        }
        
        // Draw current month line (solid, neon green)
        if (currentMonth.isNotEmpty()) {
            val currentPath = Path()
            currentMonth.forEachIndexed { index, point ->
                val x = padding + (point.dayOfMonth - 1) * chartWidth / (maxDay - 1).coerceAtLeast(1)
                val y = padding + chartHeight - (point.cumulativeAmount / maxValue * chartHeight).toFloat()
                
                if (index == 0) {
                    currentPath.moveTo(x, y)
                } else {
                    currentPath.lineTo(x, y)
                }
            }
            
            drawPath(
                path = currentPath,
                color = NeonGreen,
                style = Stroke(
                    width = 4f,
                    cap = StrokeCap.Round
                )
            )
            
            // Draw dots on current month line
            currentMonth.forEach { point ->
                val x = padding + (point.dayOfMonth - 1) * chartWidth / (maxDay - 1).coerceAtLeast(1)
                val y = padding + chartHeight - (point.cumulativeAmount / maxValue * chartHeight).toFloat()
                
                drawCircle(
                    color = NeonGreen,
                    radius = 6f,
                    center = Offset(x, y)
                )
                drawCircle(
                    color = DarkCard,
                    radius = 3f,
                    center = Offset(x, y)
                )
            }
        }
    }
}

@Composable
private fun TrendBadge(trend: VelocityTrend) {
    val (icon, color, text) = when (trend) {
        VelocityTrend.SLOWER -> Triple(Icons.Default.TrendingDown, NeonGreen, "Slower")
        VelocityTrend.SAME -> Triple(Icons.Default.TrendingFlat, NeonBlue, "Same")
        VelocityTrend.FASTER -> Triple(Icons.Default.TrendingUp, NeonOrange, "Faster")
    }
    
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = color
        )
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    isDashed: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(3.dp)
                .background(
                    color = color,
                    shape = RoundedCornerShape(2.dp)
                )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

// ==================== TOP LEAKS BREAKDOWN ====================

@Composable
private fun TopLeaksCard(
    categories: List<CategoryBreakdown>,
    totalSpent: Double,
    currency: Currency
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Top Leaks",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "Where your money goes",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                
                Text(
                    text = formatCurrency(totalSpent, currency),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = NeonPurple
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            if (categories.isEmpty()) {
                Text(
                    text = "No spending data yet",
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            } else {
                categories.forEachIndexed { index, category ->
                    CategoryProgressBar(
                        category = category,
                        currency = currency,
                        colorIndex = index
                    )
                    if (index < categories.lastIndex) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryProgressBar(
    category: CategoryBreakdown,
    currency: Currency,
    colorIndex: Int
) {
    val colors = listOf(NeonPurple, NeonBlue, NeonGreen, NeonOrange, NeonRed)
    val color = colors[colorIndex % colors.size]
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category.category.emoji,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = category.category.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(120.dp)
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatCurrency(category.amount, currency),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = color
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${category.percentage.toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Progress bar (capsule)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(category.percentage / 100f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(color, color.copy(alpha = 0.6f))
                        )
                    )
                    .animateContentSize()
            )
        }
    }
}

// ==================== CONSISTENCY CALENDAR ====================

@Composable
private fun ConsistencyCalendarCard(
    days: List<CalendarDay>,
    daysUnderBudget: Int,
    daysOverBudget: Int,
    streak: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Consistency Calendar",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "Your daily discipline",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                
                if (streak > 0) {
                    Row(
                        modifier = Modifier
                            .background(NeonGreen.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔥",
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$streak day streak",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = NeonGreen
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CalendarStat(
                    value = daysUnderBudget,
                    label = "Under Budget",
                    color = NeonGreen
                )
                CalendarStat(
                    value = daysOverBudget,
                    label = "Over Budget",
                    color = NeonOrange
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Weekday headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Calendar grid
            CalendarGrid(days = days)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CalendarLegendItem(color = NeonGreen, label = "Under")
                CalendarLegendItem(color = NeonOrange, label = "Slightly Over")
                CalendarLegendItem(color = NeonRed, label = "Over")
                CalendarLegendItem(color = GrayLine, label = "No Spend")
            }
        }
    }
}

@Composable
private fun CalendarGrid(days: List<CalendarDay>) {
    if (days.isEmpty()) return
    
    // Get the first day's day of week (1 = Monday, 7 = Sunday)
    val firstDayOfWeek = days.first().date.dayOfWeek.value
    
    // Calculate empty cells before first day
    val emptyCells = firstDayOfWeek - 1
    
    // Build list with empty cells
    val gridItems = buildList {
        repeat(emptyCells) { add(null) }
        addAll(days)
    }
    
    // Grid with 7 columns
    Column {
        gridItems.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                week.forEach { day ->
                    if (day != null) {
                        CalendarDayDot(day = day)
                    } else {
                        Spacer(modifier = Modifier.size(36.dp))
                    }
                }
                // Fill remaining cells in the row
                repeat(7 - week.size) {
                    Spacer(modifier = Modifier.size(36.dp))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun CalendarDayDot(day: CalendarDay) {
    val color = when (day.status) {
        DayStatus.UNDER_BUDGET -> NeonGreen
        DayStatus.SLIGHTLY_OVER -> NeonOrange
        DayStatus.OVER_BUDGET -> NeonRed
        DayStatus.NO_SPEND -> GrayLine
        DayStatus.FUTURE -> Color.Transparent
        DayStatus.TODAY -> NeonBlue
    }
    
    val borderColor = if (day.status == DayStatus.TODAY) NeonBlue else Color.Transparent
    
    Box(
        modifier = Modifier
            .size(36.dp)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Day number
        Text(
            text = day.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = if (day.status == DayStatus.FUTURE) 
                Color.White.copy(alpha = 0.3f) 
            else 
                Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp
        )
        
        // Status dot
        if (day.status != DayStatus.FUTURE) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun CalendarStat(
    value: Int,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun CalendarLegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp
        )
    }
}

// ==================== UTILITIES ====================

private fun formatCurrency(amount: Double, currency: Currency): String {
    return when (currency) {
        Currency.RSD -> "%,.0f %s".format(amount, currency.symbol)
        else -> "%s%,.2f".format(currency.symbol, amount)
    }
}
