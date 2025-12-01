package site.giboworks.budgettracker.domain.model

/**
 * Represents the current status of all three vitality rings.
 * This is the primary UI state for the dashboard.
 */
data class VitalityRingsState(
    val pulse: RingStatus,
    val shield: RingStatus,
    val clarity: RingStatus,
    val overallScore: Int = calculateOverallScore(pulse, shield, clarity),
    val vitalityScore: Int = overallScore, // New: Vitality algorithm score (60% daily + 40% savings)
    val streak: Int = 0,
    val creditsEarnedToday: Int = 0
) {
    companion object {
        fun calculateOverallScore(pulse: RingStatus, shield: RingStatus, clarity: RingStatus): Int {
            // Weighted average: Pulse (40%), Shield (35%), Clarity (25%)
            val pulseScore = when {
                pulse.progress <= 0.7f -> 100
                pulse.progress <= 0.9f -> 80
                pulse.progress <= 1.0f -> 60
                else -> 30 // Over budget
            }
            val shieldScore = (shield.progress * 100).toInt().coerceIn(0, 100)
            val clarityScore = (clarity.progress * 100).toInt().coerceIn(0, 100)
            
            return (pulseScore * 0.4 + shieldScore * 0.35 + clarityScore * 0.25).toInt()
        }
        
        fun empty() = VitalityRingsState(
            pulse = RingStatus.empty(RingType.PULSE),
            shield = RingStatus.empty(RingType.SHIELD),
            clarity = RingStatus.empty(RingType.CLARITY)
        )
    }
}

/**
 * Individual ring status with all display information
 */
data class RingStatus(
    val type: RingType,
    val progress: Float, // 0.0 to 1.0+ (can exceed 1.0 for overspending)
    val currentValue: Double,
    val targetValue: Double,
    val state: RingState,
    val label: String,
    val sublabel: String,
    val currency: Currency = Currency.RSD
) {
    companion object {
        fun empty(type: RingType) = RingStatus(
            type = type,
            progress = 0f,
            currentValue = 0.0,
            targetValue = 0.0,
            state = RingState.INACTIVE,
            label = type.displayName,
            sublabel = "No data"
        )
    }
    
    /**
     * Formatted display string for current value
     */
    val formattedCurrent: String
        get() = formatAmount(currentValue, currency)
    
    /**
     * Formatted display string for target value
     */
    val formattedTarget: String
        get() = formatAmount(targetValue, currency)
    
    /**
     * Percentage display (capped at 100% for display, but actual can exceed)
     */
    val displayPercentage: Int
        get() = (progress * 100).toInt().coerceIn(0, 999)
    
    private fun formatAmount(amount: Double, currency: Currency): String {
        return when (currency) {
            Currency.RSD -> "%,.0f %s".format(amount, currency.symbol)
            else -> "%s%,.2f".format(currency.symbol, amount)
        }
    }
}

/**
 * Types of rings in the vitality system
 */
enum class RingType(val displayName: String, val description: String) {
    PULSE(
        displayName = "Pulse",
        description = "Daily spending allowance"
    ),
    SHIELD(
        displayName = "Shield", 
        description = "Savings & investments progress"
    ),
    CLARITY(
        displayName = "Clarity",
        description = "Fixed bills status"
    )
}

/**
 * Visual state of a ring affecting colors and animations
 * 
 * NON-JUDGMENTAL DESIGN:
 * - ADJUSTED replaces CRITICAL for a gentler approach
 * - Colors are softer, messaging is encouraging
 */
enum class RingState {
    EXCELLENT,    // Under budget, ahead of goals
    GOOD,         // On track
    WARNING,      // Approaching limit
    ADJUSTED,     // Rolling budget adjusted (replaces harsh "Over!" messaging)
    CRITICAL,     // Legacy: Over budget (use ADJUSTED instead for better UX)
    COMPLETED,    // Ring is full (100%)
    INACTIVE      // No activity or disabled
}

/**
 * Extension to get appropriate colors for ring state
 */
fun RingState.getColorScheme(): RingColorScheme {
    return when (this) {
        RingState.EXCELLENT -> RingColorScheme(
            primary = 0xFF00E676,    // Bright green
            secondary = 0xFF00C853,
            glow = 0x4000E676
        )
        RingState.GOOD -> RingColorScheme(
            primary = 0xFF2196F3,    // Blue
            secondary = 0xFF1976D2,
            glow = 0x402196F3
        )
        RingState.WARNING -> RingColorScheme(
            primary = 0xFFFF9800,    // Orange
            secondary = 0xFFF57C00,
            glow = 0x40FF9800
        )
        RingState.ADJUSTED -> RingColorScheme(
            primary = 0xFFFFB74D,    // Soft orange - non-judgmental
            secondary = 0xFFFF9800,
            glow = 0x40FFB74D
        )
        RingState.CRITICAL -> RingColorScheme(
            primary = 0xFFFF5252,    // Red
            secondary = 0xFFD32F2F,
            glow = 0x40FF5252
        )
        RingState.COMPLETED -> RingColorScheme(
            primary = 0xFFFFD700,    // Gold
            secondary = 0xFFFFC107,
            glow = 0x40FFD700
        )
        RingState.INACTIVE -> RingColorScheme(
            primary = 0xFF757575,    // Gray
            secondary = 0xFF616161,
            glow = 0x00000000
        )
    }
}

data class RingColorScheme(
    val primary: Long,
    val secondary: Long,
    val glow: Long
)

/**
 * Ring-specific color themes
 */
object RingColors {
    // Pulse Ring (Daily Spend) - Inverted logic: filling up is spending
    val pulseExcellent = Pair(0xFF4ECDC4, 0xFF44A08D)  // Teal gradient - under budget
    val pulseGood = Pair(0xFF56CCF2, 0xFF2F80ED)       // Blue gradient - on track
    val pulseWarning = Pair(0xFFFF9966, 0xFFFF5E62)    // Orange-red gradient
    // UPDATED: Changed from aggressive red to calmer Orange-Amber "warning" state
    val pulseCritical = Pair(0xFFFF9800, 0xFFFF5722)   // Orange-Amber gradient - over budget (de-escalated)
    
    // Shield Ring (Savings) - Filling up is progress
    val shieldEmpty = Pair(0xFF667eea, 0xFF764ba2)     // Purple gradient - starting
    val shieldProgress = Pair(0xFF11998e, 0xFF38ef7d) // Green gradient - growing
    val shieldComplete = Pair(0xFFf7971e, 0xFFffd200) // Gold gradient - goal reached
    
    // Clarity Ring (Fixed Bills)
    val clarityPending = Pair(0xFF606c88, 0xFF3f4c6b)  // Dark blue-gray - unpaid
    val clarityPartial = Pair(0xFF4776E6, 0xFF8E54E9)  // Blue-purple - some paid
    val clarityComplete = Pair(0xFF00b09b, 0xFF96c93d) // Green - all paid
}
