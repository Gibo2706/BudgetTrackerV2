package site.giboworks.budgettracker.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import site.giboworks.budgettracker.domain.model.Currency
import site.giboworks.budgettracker.domain.model.RingColors
import site.giboworks.budgettracker.domain.model.RingState
import site.giboworks.budgettracker.domain.model.RingStatus
import site.giboworks.budgettracker.domain.model.RingType
import site.giboworks.budgettracker.domain.model.VitalityRingsState
import site.giboworks.budgettracker.ui.theme.BudgetTrackerTheme
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Premium Vitality Rings Component
 * 
 * A beautiful, animated concentric rings display inspired by Apple Watch fitness rings.
 * Features:
 * - Smooth spring animations
 * - Gradient shaders
 * - Glow effects
 * - Background tracks
 * - Rounded stroke caps
 */
@Composable
fun VitalityRings(
    state: VitalityRingsState,
    modifier: Modifier = Modifier,
    ringSpacing: Dp = 24.dp,
    strokeWidth: Dp = 22.dp,
    animationDurationMs: Int = 1200,
    showLabels: Boolean = true,
    showCenterContent: Boolean = true
) {
    // Animated progress values with spring physics
    val pulseProgress = remember { Animatable(0f) }
    val shieldProgress = remember { Animatable(0f) }
    val clarityProgress = remember { Animatable(0f) }
    
    // Animate to target values
    LaunchedEffect(state.pulse.progress) {
        pulseProgress.animateTo(
            targetValue = state.pulse.progress.coerceIn(0f, 1.5f), // Allow overfill
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }
    
    LaunchedEffect(state.shield.progress) {
        shieldProgress.animateTo(
            targetValue = state.shield.progress.coerceIn(0f, 1f),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }
    
    LaunchedEffect(state.clarity.progress) {
        clarityProgress.animateTo(
            targetValue = state.clarity.progress.coerceIn(0f, 1f),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }
    
    // Glow pulse animation
    val glowAlpha by animateFloatAsState(
        targetValue = if (state.overallScore >= 80) 0.6f else 0.3f,
        animationSpec = tween(1000),
        label = "glowAlpha"
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                val strokeWidthPx = strokeWidth.toPx()
                val spacingPx = ringSpacing.toPx()
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = min(size.width, size.height) / 2f - strokeWidthPx / 2f
                
                // Calculate radii for three rings (outer to inner)
                val outerRadius = maxRadius
                val middleRadius = maxRadius - strokeWidthPx - spacingPx
                val innerRadius = middleRadius - strokeWidthPx - spacingPx
                
                // Draw rings from outer to inner
                // Ring 1: PULSE (Daily Spend) - Outer ring
                drawVitalityRing(
                    center = center,
                    radius = outerRadius,
                    strokeWidth = strokeWidthPx,
                    progress = pulseProgress.value,
                    ringStatus = state.pulse,
                    glowAlpha = glowAlpha
                )
                
                // Ring 2: SHIELD (Savings) - Middle ring
                drawVitalityRing(
                    center = center,
                    radius = middleRadius,
                    strokeWidth = strokeWidthPx,
                    progress = shieldProgress.value,
                    ringStatus = state.shield,
                    glowAlpha = glowAlpha
                )
                
                // Ring 3: CLARITY (Bills) - Inner ring
                drawVitalityRing(
                    center = center,
                    radius = innerRadius,
                    strokeWidth = strokeWidthPx,
                    progress = clarityProgress.value,
                    ringStatus = state.clarity,
                    glowAlpha = glowAlpha
                )
            }
            
            // Center content
            if (showCenterContent) {
                CenterContent(
                    overallScore = state.overallScore,
                    streak = state.streak
                )
            }
        }
        
        // Ring labels below
        if (showLabels) {
            Spacer(modifier = Modifier.height(24.dp))
            RingLabels(state = state)
        }
    }
}

/**
 * Draw a single vitality ring with background track, gradient fill, and glow
 */
private fun DrawScope.drawVitalityRing(
    center: Offset,
    radius: Float,
    strokeWidth: Float,
    progress: Float,
    ringStatus: RingStatus,
    glowAlpha: Float
) {
    val startAngle = -90f // Start from top
    val sweepAngle = progress * 360f
    
    // Get colors based on ring type and state
    val (gradientColors, trackColor) = getColorsForRing(ringStatus)
    
    // Calculate arc bounds
    val topLeft = Offset(center.x - radius, center.y - radius)
    val arcSize = Size(radius * 2, radius * 2)
    
    // 1. Draw background track (subtle, darker)
    drawArc(
        color = trackColor,
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
    
    // 2. Draw glow effect (behind main arc)
    if (progress > 0.01f && ringStatus.state != RingState.INACTIVE) {
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    gradientColors.first.copy(alpha = glowAlpha * 0.5f),
                    gradientColors.second.copy(alpha = glowAlpha * 0.5f),
                    gradientColors.first.copy(alpha = glowAlpha * 0.3f)
                ),
                center = center
            ),
            startAngle = startAngle,
            sweepAngle = sweepAngle.coerceAtMost(360f),
            useCenter = false,
            topLeft = Offset(center.x - radius - 4, center.y - radius - 4),
            size = Size(radius * 2 + 8, radius * 2 + 8),
            style = Stroke(width = strokeWidth + 12, cap = StrokeCap.Round)
        )
    }
    
    // 3. Draw main progress arc with gradient
    if (progress > 0.001f) {
        // Use rotating gradient for smooth color transition along the arc
        rotate(degrees = startAngle, pivot = center) {
            drawArc(
                brush = Brush.sweepGradient(
                    colorStops = arrayOf(
                        0f to gradientColors.first,
                        (progress.coerceAtMost(1f)) to gradientColors.second,
                        1f to gradientColors.first.copy(alpha = 0.3f)
                    ),
                    center = center
                ),
                startAngle = 0f,
                sweepAngle = sweepAngle.coerceAtMost(360f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        
        // Draw end cap highlight (creates 3D effect)
        if (progress > 0.05f) {
            val endAngle = Math.toRadians((startAngle + sweepAngle.coerceAtMost(360f)).toDouble())
            val capX = center.x + radius * cos(endAngle).toFloat()
            val capY = center.y + radius * sin(endAngle).toFloat()
            
            // Highlight dot at end
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = strokeWidth / 4,
                center = Offset(capX, capY)
            )
        }
    }
    
    // 4. Handle overfill (> 100%) - draw second lap with calmer color
    if (progress > 1f) {
        val overfillProgress = progress - 1f
        val overfillSweep = overfillProgress * 360f
        
        rotate(degrees = startAngle, pivot = center) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFFF9800),  // Orange for overspending (de-escalated)
                        Color(0xFFFF5722),  // Deep Amber
                        Color(0xFFFF9800)
                    ),
                    center = center
                ),
                startAngle = 0f,
                sweepAngle = overfillSweep.coerceAtMost(360f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth * 0.7f, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Get gradient colors and track color based on ring type and state
 */
private fun getColorsForRing(ringStatus: RingStatus): Pair<Pair<Color, Color>, Color> {
    val trackAlpha = 0.15f
    
    return when (ringStatus.type) {
        RingType.PULSE -> {
            val colors = when (ringStatus.state) {
                RingState.EXCELLENT, RingState.GOOD -> RingColors.pulseGood
                RingState.WARNING -> RingColors.pulseWarning
                RingState.ADJUSTED, RingState.CRITICAL -> RingColors.pulseCritical
                RingState.COMPLETED -> RingColors.pulseExcellent
                RingState.INACTIVE -> Pair(0xFF757575, 0xFF616161)
            }
            Pair(
                Pair(Color(colors.first), Color(colors.second)),
                Color(colors.first).copy(alpha = trackAlpha)
            )
        }
        RingType.SHIELD -> {
            val colors = when {
                ringStatus.progress >= 1f -> RingColors.shieldComplete
                ringStatus.progress >= 0.5f -> RingColors.shieldProgress
                else -> RingColors.shieldEmpty
            }
            Pair(
                Pair(Color(colors.first), Color(colors.second)),
                Color(colors.first).copy(alpha = trackAlpha)
            )
        }
        RingType.CLARITY -> {
            val colors = when {
                ringStatus.progress >= 1f -> RingColors.clarityComplete
                ringStatus.progress >= 0.5f -> RingColors.clarityPartial
                else -> RingColors.clarityPending
            }
            Pair(
                Pair(Color(colors.first), Color(colors.second)),
                Color(colors.first).copy(alpha = trackAlpha)
            )
        }
    }
}

/**
 * Center content showing overall score (NO streak - moved to header)
 */
@Composable
private fun CenterContent(
    overallScore: Int,
    streak: Int // kept for API compatibility but not displayed here
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "$overallScore",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "VITALITY",
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        // Streak removed from here - now in header as pill badge
    }
}

/**
 * Ring labels showing individual ring details
 */
@Composable
private fun RingLabels(
    state: VitalityRingsState
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        RingLabel(
            ringStatus = state.pulse,
            color = Color(RingColors.pulseGood.first)
        )
        RingLabel(
            ringStatus = state.shield,
            color = Color(RingColors.shieldProgress.first)
        )
        RingLabel(
            ringStatus = state.clarity,
            color = Color(RingColors.clarityPartial.first)
        )
    }
}

@Composable
private fun RingLabel(
    ringStatus: RingStatus,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Color indicator
        Canvas(modifier = Modifier.size(12.dp)) {
            drawCircle(color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        
        // Ring name
        Text(
            text = ringStatus.type.displayName,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        
        // Progress percentage
        Text(
            text = "${ringStatus.displayPercentage}%",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = color
        )
        
        // Current / Target
        Text(
            text = ringStatus.sublabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

// ==================== PREVIEW ====================

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun VitalityRingsPreview() {
    BudgetTrackerTheme(darkTheme = true) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            VitalityRings(
                state = VitalityRingsState(
                    pulse = RingStatus(
                        type = RingType.PULSE,
                        progress = 0.65f,
                        currentValue = 3250.0,
                        targetValue = 5000.0,
                        state = RingState.GOOD,
                        label = "Pulse",
                        sublabel = "3,250 / 5,000 дин."
                    ),
                    shield = RingStatus(
                        type = RingType.SHIELD,
                        progress = 0.45f,
                        currentValue = 9000.0,
                        targetValue = 20000.0,
                        state = RingState.GOOD,
                        label = "Shield",
                        sublabel = "9,000 / 20,000 дин."
                    ),
                    clarity = RingStatus(
                        type = RingType.CLARITY,
                        progress = 0.8f,
                        currentValue = 4.0,
                        targetValue = 5.0,
                        state = RingState.GOOD,
                        label = "Clarity",
                        sublabel = "4/5 bills paid"
                    ),
                    streak = 7
                ),
                modifier = Modifier.padding(24.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun VitalityRingsOverspendPreview() {
    BudgetTrackerTheme(darkTheme = true) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            VitalityRings(
                state = VitalityRingsState(
                    pulse = RingStatus(
                        type = RingType.PULSE,
                        progress = 1.3f, // Over budget!
                        currentValue = 6500.0,
                        targetValue = 5000.0,
                        state = RingState.CRITICAL,
                        label = "Pulse",
                        sublabel = "Over budget!"
                    ),
                    shield = RingStatus(
                        type = RingType.SHIELD,
                        progress = 1.0f,
                        currentValue = 20000.0,
                        targetValue = 20000.0,
                        state = RingState.COMPLETED,
                        label = "Shield",
                        sublabel = "Goal reached! 🎉"
                    ),
                    clarity = RingStatus(
                        type = RingType.CLARITY,
                        progress = 1.0f,
                        currentValue = 5.0,
                        targetValue = 5.0,
                        state = RingState.COMPLETED,
                        label = "Clarity",
                        sublabel = "All bills paid ✓"
                    ),
                    streak = 14
                ),
                modifier = Modifier.padding(24.dp)
            )
        }
    }
}
