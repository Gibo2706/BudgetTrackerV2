package site.giboworks.budgettracker.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Permission state for notification permission
 */
data class NotificationPermissionState(
    val isGranted: Boolean,
    val shouldShowRationale: Boolean
)

/**
 * Composable to handle notification permission for Android 13+.
 * 
 * Usage:
 * ```kotlin
 * val (permissionState, requestPermission) = rememberNotificationPermission()
 * 
 * if (!permissionState.isGranted) {
 *     Button(onClick = { requestPermission() }) {
 *         Text("Enable Notifications")
 *     }
 * }
 * ```
 * 
 * @param onPermissionResult Callback when permission result is received
 * @return Pair of (state, request lambda)
 */
@Composable
fun rememberNotificationPermission(
    onPermissionResult: (Boolean) -> Unit = {}
): Pair<NotificationPermissionState, () -> Unit> {
    val context = LocalContext.current
    
    // For Android < 13, notifications are always allowed
    val isPermissionNeeded = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    
    var permissionState by remember {
        mutableStateOf(
            NotificationPermissionState(
                isGranted = !isPermissionNeeded || ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED,
                shouldShowRationale = false
            )
        )
    }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionState = permissionState.copy(isGranted = isGranted)
        onPermissionResult(isGranted)
    }
    
    val requestPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // Permission automatically granted on older versions
            onPermissionResult(true)
        }
    }
    
    return permissionState to requestPermission
}

/**
 * Check if notification permission is granted.
 * On Android < 13, this always returns true.
 */
fun isNotificationPermissionGranted(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

/**
 * Composable that automatically requests notification permission on first composition.
 * Useful for onboarding flows.
 * 
 * @param onResult Callback with permission result
 */
@Composable
fun RequestNotificationPermissionEffect(onResult: (Boolean) -> Unit) {
    val context = LocalContext.current
    val (state, requestPermission) = rememberNotificationPermission(onResult)
    
    LaunchedEffect(Unit) {
        if (!state.isGranted) {
            requestPermission()
        } else {
            onResult(true)
        }
    }
}
