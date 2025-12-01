package site.giboworks.budgettracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Dark color scheme optimized for the Vitality Ring aesthetics
 */
private val VitalityDarkColorScheme = darkColorScheme(
    primary = VitalityPrimary,
    onPrimary = Color.White,
    primaryContainer = VitalityPrimary.copy(alpha = 0.2f),
    onPrimaryContainer = VitalityPrimary,
    
    secondary = VitalitySecondary,
    onSecondary = Color.White,
    secondaryContainer = VitalitySecondary.copy(alpha = 0.2f),
    onSecondaryContainer = VitalitySecondary,
    
    tertiary = VitalityTertiary,
    onTertiary = Color.Black,
    tertiaryContainer = VitalityTertiary.copy(alpha = 0.2f),
    onTertiaryContainer = VitalityTertiary,
    
    background = VitalityBackground,
    onBackground = VitalityOnBackground,
    
    surface = VitalitySurface,
    onSurface = VitalityOnSurface,
    
    surfaceVariant = VitalitySurfaceVariant,
    onSurfaceVariant = VitalityOnSurfaceVariant,
    
    error = ErrorRed,
    onError = Color.White,
    
    outline = Color(0xFF30363D)
)

/**
 * Light color scheme (can be customized later)
 */
private val VitalityLightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFAFAFA),
    surface = Color.White,
    surfaceVariant = Color(0xFFF5F5F5)
)

@Composable
fun BudgetTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    // Disabled by default to maintain brand consistency
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> VitalityDarkColorScheme
        else -> VitalityLightColorScheme
    }
    
    // Set status bar color
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}