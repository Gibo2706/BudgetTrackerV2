package site.giboworks.budgettracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "budget_tracker_preferences")

/**
 * DataStore-based preferences for quick access to app state.
 * Used for checking onboarding completion without database query.
 */
@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private object PreferencesKeys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val NOTIFICATION_PERMISSION_ASKED = booleanPreferencesKey("notification_permission_asked")
        val GHOST_MODE_ENABLED = booleanPreferencesKey("ghost_mode_enabled")
        val AUTO_TRACK_INCOME = booleanPreferencesKey("auto_track_income")
        val POST_NOTIFICATIONS_ASKED = booleanPreferencesKey("post_notifications_asked")
    }
    
    // ========== ONBOARDING STATE ==========
    
    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
    }
    
    suspend fun getOnboardingCompleted(): Boolean {
        return context.dataStore.data.first()[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
    }
    
    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }
    
    // ========== PERMISSION STATE ==========
    
    val notificationPermissionAsked: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.NOTIFICATION_PERMISSION_ASKED] ?: false
    }
    
    suspend fun setNotificationPermissionAsked(asked: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_PERMISSION_ASKED] = asked
        }
    }
    
    // ========== POST_NOTIFICATIONS PERMISSION (Android 13+) ==========
    
    val postNotificationsAsked: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.POST_NOTIFICATIONS_ASKED] ?: false
    }
    
    suspend fun getPostNotificationsAsked(): Boolean {
        return context.dataStore.data.first()[PreferencesKeys.POST_NOTIFICATIONS_ASKED] ?: false
    }
    
    suspend fun setPostNotificationsAsked(asked: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.POST_NOTIFICATIONS_ASKED] = asked
        }
    }
    
    // ========== GHOST MODE (PAUSE TRACKING) ==========
    
    /**
     * Ghost Mode pauses all tracking for mental health breaks.
     * When enabled:
     * - Rings are hidden
     * - FAB is disabled
     * - Soothing message is shown
     */
    val isGhostModeEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.GHOST_MODE_ENABLED] ?: false
    }
    
    suspend fun getGhostModeEnabled(): Boolean {
        return context.dataStore.data.first()[PreferencesKeys.GHOST_MODE_ENABLED] ?: false
    }
    
    suspend fun setGhostModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GHOST_MODE_ENABLED] = enabled
        }
    }
    
    // ========== AUTO TRACK INCOME ==========
    
    /**
     * If true, automatically parse and save income transactions (salary, refunds).
     * Default: false - only expenses are auto-tracked.
     * 
     * This prevents false positives where bank balance notifications are 
     * mistakenly classified as income.
     */
    val autoTrackIncome: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AUTO_TRACK_INCOME] ?: false
    }
    
    suspend fun getAutoTrackIncome(): Boolean {
        return context.dataStore.data.first()[PreferencesKeys.AUTO_TRACK_INCOME] ?: false
    }
    
    suspend fun setAutoTrackIncome(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_TRACK_INCOME] = enabled
        }
    }
}
