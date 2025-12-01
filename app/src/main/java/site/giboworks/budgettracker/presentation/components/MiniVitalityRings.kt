package site.giboworks.budgettracker.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import site.giboworks.budgettracker.domain.model.RingColors
import site.giboworks.budgettracker.domain.model.RingState
import site.giboworks.budgettracker.domain.model.RingStatus
import site.giboworks.budgettracker.domain.model.RingType
import site.giboworks.budgettracker.domain.model.VitalityRingsState
import site.giboworks.budgettracker.ui.theme.BudgetTrackerTheme
import kotlin.math.min

/**
 * Mini Vitality Rings - Compact version for list items and widgets
 */
@Composable
fun MiniVitalityRings(
    state: VitalityRingsState,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    strokeWidth: Dp = 6.dp
) {
    val pulseProgress = remember { Animatable(0f) }
    val shieldProgress = remember { Animatable(0f) }
    val clarityProgress = remember { Animatable(0f) }
    
    LaunchedEffect(state.pulse.progress) {
        pulseProgress.animateTo(
            targetValue = state.pulse.progress.coerceIn(0f, 1.5f),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }
    
    LaunchedEffect(state.shield.progress) {
        shieldProgress.animateTo(
            targetValue = state.shield.progress.coerceIn(0f, 1f),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }
    
    LaunchedEffect(state.clarity.progress) {
        clarityProgress.animateTo(
            targetValue = state.clarity.progress.coerceIn(0f, 1f),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidthPx = strokeWidth.toPx()
            val spacing = strokeWidthPx * 1.2f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val maxRadius = min(this.size.width, this.size.height) / 2f - strokeWidthPx / 2f
            
            val outerRadius = maxRadius
            val middleRadius = maxRadius - strokeWidthPx - spacing
            val innerRadius = middleRadius - strokeWidthPx - spacing
            
            // Pulse (outer)
            drawMiniRing(
                center = center,
                radius = outerRadius,
                strokeWidth = strokeWidthPx,
                progress = pulseProgress.value,
                colors = getPulseColors(state.pulse.state)
            )
            
            // Shield (middle)
            drawMiniRing(
                center = center,
                radius = middleRadius,
                strokeWidth = strokeWidthPx,
                progress = shieldProgress.value,
                colors = getShieldColors(state.shield.progress)
            )
            
            // Clarity (inner)
            drawMiniRing(
                center = center,
                radius = innerRadius,
                strokeWidth = strokeWidthPx,
                progress = clarityProgress.value,
                colors = getClarityColors(state.clarity.progress)
            )
        }
        
        // Center score
        Text(
            text = "${state.overallScore}",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMiniRing(
    center: Offset,
    radius: Float,
    strokeWidth: Float,
    progress: Float,
    colors: Pair<Color, Color>
) {
    val startAngle = -90f
    val sweepAngle = progress.coerceAtMost(1f) * 360f
    val topLeft = Offset(center.x - radius, center.y - radius)
    val arcSize = Size(radius * 2, radius * 2)
    
    // Background track
    drawArc(
        color = colors.first.copy(alpha = 0.15f),
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
    
    // Progress arc
    if (progress > 0.001f) {
        rotate(degrees = startAngle, pivot = center) {
            drawArc(
                brush = Brush.sweepGradient(
                    colorStops = arrayOf(
                        0f to colors.first,
                        (progress.coerceAtMost(1f)) to colors.second,
                        1f to colors.first.copy(alpha = 0.3f)
                    ),
                    center = center
                ),
                startAngle = 0f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

private fun getPulseColors(state: RingState): Pair<Color, Color> {
    val colors = when (state) {
        RingState.EXCELLENT, RingState.GOOD -> RingColors.pulseGood
        RingState.WARNING -> RingColors.pulseWarning
        RingState.CRITICAL -> RingColors.pulseCritical
        else -> RingColors.pulseExcellent
    }
    return Pair(Color(colors.first), Color(colors.second))
}

private fun getShieldColors(progress: Float): Pair<Color, Color> {
    val colors = when {
        progress >= 1f -> RingColors.shieldComplete
        progress >= 0.5f -> RingColors.shieldProgress
        else -> RingColors.shieldEmpty
    }
    return Pair(Color(colors.first), Color(colors.second))
}

private fun getClarityColors(progress: Float): Pair<Color, Color> {
    val colors = when {
        progress >= 1f -> RingColors.clarityComplete
        progress >= 0.5f -> RingColors.clarityPartial
        else -> RingColors.clarityPending
    }
    return Pair(Color(colors.first), Color(colors.second))
}

/**
 * Single Ring Progress Indicator - For individual metric display
 */
@Composable
fun SingleRingIndicator(
    progress: Float,
    ringType: RingType,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    strokeWidth: Dp = 8.dp,
    showPercentage: Boolean = true
) {
    val animatedProgress = remember { Animatable(0f) }
    
    LaunchedEffect(progress) {
        animatedProgress.animateTo(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }
    
    val colors = when (ringType) {
        RingType.PULSE -> Pair(Color(RingColors.pulseGood.first), Color(RingColors.pulseGood.second))
        RingType.SHIELD -> Pair(Color(RingColors.shieldProgress.first), Color(RingColors.shieldProgress.second))
        RingType.CLARITY -> Pair(Color(RingColors.clarityPartial.first), Color(RingColors.clarityPartial.second))
    }
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidthPx = strokeWidth.toPx()
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = min(this.size.width, this.size.height) / 2f - strokeWidthPx / 2f
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2, radius * 2)
            
            // Background
            drawArc(
                color = colors.first.copy(alpha = 0.15f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
            
            // Progress
            if (animatedProgress.value > 0.001f) {
                rotate(degrees = -90f, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(colors.first, colors.second, colors.first.copy(alpha = 0.3f)),
                            center = center
                        ),
                        startAngle = 0f,
                        sweepAngle = animatedProgress.value * 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )
                }
            }
        }
        
        if (showPercentage) {
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = colors.first
            )
        }
    }
}

/**
 * Compact Rings Card for Dashboard widgets
 */
@Composable
fun CompactRingsCard(
    state: VitalityRingsState,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MiniVitalityRings(
                state = state,
                size = 72.dp,
                strokeWidth = 5.dp
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Financial Vitality",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RingMiniStat(
                        label = "Pulse",
                        value = "${state.pulse.displayPercentage}%",
                        color = Color(RingColors.pulseGood.first)
                    )
                    RingMiniStat(
                        label = "Shield",
                        value = "${state.shield.displayPercentage}%",
                        color = Color(RingColors.shieldProgress.first)
                    )
                    RingMiniStat(
                        label = "Clarity",
                        value = "${state.clarity.displayPercentage}%",
                        color = Color(RingColors.clarityPartial.first)
                    )
                }
                
                if (state.streak > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🔥 ${state.streak} day streak",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF9800)
                    )
                }
            }
        }
    }
}

@Composable
private fun RingMiniStat(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
private fun MiniRingsPreview() {
    BudgetTrackerTheme(darkTheme = true) {
        MiniVitalityRings(
            state = VitalityRingsState(
                pulse = RingStatus(
                    type = RingType.PULSE,
                    progress = 0.65f,
                    currentValue = 3250.0,
                    targetValue = 5000.0,
                    state = RingState.GOOD,
                    label = "Pulse",
                    sublabel = "65%"
                ),
                shield = RingStatus(
                    type = RingType.SHIELD,
                    progress = 0.45f,
                    currentValue = 9000.0,
                    targetValue = 20000.0,
                    state = RingState.GOOD,
                    label = "Shield",
                    sublabel = "45%"
                ),
                clarity = RingStatus(
                    type = RingType.CLARITY,
                    progress = 0.8f,
                    currentValue = 4.0,
                    targetValue = 5.0,
                    state = RingState.GOOD,
                    label = "Clarity",
                    sublabel = "80%"
                ),
                streak = 7
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
private fun CompactRingsCardPreview() {
    BudgetTrackerTheme(darkTheme = true) {
        CompactRingsCard(
            state = VitalityRingsState(
                pulse = RingStatus(
                    type = RingType.PULSE,
                    progress = 0.65f,
                    currentValue = 3250.0,
                    targetValue = 5000.0,
                    state = RingState.GOOD,
                    label = "Pulse",
                    sublabel = "65%"
                ),
                shield = RingStatus(
                    type = RingType.SHIELD,
                    progress = 0.45f,
                    currentValue = 9000.0,
                    targetValue = 20000.0,
                    state = RingState.GOOD,
                    label = "Shield",
                    sublabel = "45%"
                ),
                clarity = RingStatus(
                    type = RingType.CLARITY,
                    progress = 0.8f,
                    currentValue = 4.0,
                    targetValue = 5.0,
                    state = RingState.GOOD,
                    label = "Clarity",
                    sublabel = "80%"
                ),
                streak = 12
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
