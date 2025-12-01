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
}
