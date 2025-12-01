package site.giboworks.budgettracker.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scheduler for Smart Notification Workers.
 * 
 * Schedules three daily notifications:
 * - Morning Briefing: 08:00 AM
 * - Danger Zone Check: 14:00 PM
 * - Victory Lap: 21:00 PM
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val workManager = WorkManager.getInstance(context)
    
    companion object {
        private const val WORK_MORNING_BRIEFING = "morning_briefing_work"
        private const val WORK_DANGER_ZONE = "danger_zone_work"
        private const val WORK_VICTORY_LAP = "victory_lap_work"
        
        private val MORNING_TIME = LocalTime.of(8, 0)   // 08:00 AM
        private val DANGER_TIME = LocalTime.of(14, 0)   // 02:00 PM
        private val VICTORY_TIME = LocalTime.of(21, 0)  // 09:00 PM
    }
    
    /**
     * Schedule all daily notification workers.
     * Call this during app initialization or after onboarding.
     */
    fun scheduleAllNotifications() {
        scheduleMorningBriefing()
        scheduleDangerZoneCheck()
        scheduleVictoryLap()
    }
    
    /**
     * Cancel all scheduled notifications.
     * Call this when user disables notifications in settings.
     */
    fun cancelAllNotifications() {
        workManager.cancelUniqueWork(WORK_MORNING_BRIEFING)
        workManager.cancelUniqueWork(WORK_DANGER_ZONE)
        workManager.cancelUniqueWork(WORK_VICTORY_LAP)
    }
    
    /**
     * Schedule Morning Briefing - 08:00 AM daily
     */
    private fun scheduleMorningBriefing() {
        val initialDelay = calculateInitialDelay(MORNING_TIME)
        
        val workRequest = PeriodicWorkRequestBuilder<MorningBriefingWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(defaultConstraints())
            .addTag("notification")
            .addTag("morning")
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            WORK_MORNING_BRIEFING,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
    
    /**
     * Schedule Danger Zone Check - 14:00 PM daily
     */
    private fun scheduleDangerZoneCheck() {
        val initialDelay = calculateInitialDelay(DANGER_TIME)
        
        val workRequest = PeriodicWorkRequestBuilder<DangerZoneWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(defaultConstraints())
            .addTag("notification")
            .addTag("danger")
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            WORK_DANGER_ZONE,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
    
    /**
     * Schedule Victory Lap - 21:00 PM daily
     */
    private fun scheduleVictoryLap() {
        val initialDelay = calculateInitialDelay(VICTORY_TIME)
        
        val workRequest = PeriodicWorkRequestBuilder<VictoryLapWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(defaultConstraints())
            .addTag("notification")
            .addTag("victory")
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            WORK_VICTORY_LAP,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
    
    /**
     * Calculate delay from now until the target time.
     * If target time has passed today, schedules for tomorrow.
     */
    private fun calculateInitialDelay(targetTime: LocalTime): Long {
        val now = LocalDateTime.now()
        val todayTarget = now.toLocalDate().atTime(targetTime)
        
        val targetDateTime = if (now.isBefore(todayTarget)) {
            todayTarget
        } else {
            todayTarget.plusDays(1)
        }
        
        return Duration.between(now, targetDateTime).toMillis()
    }
    
    /**
     * Default constraints for notification workers.
     * Lightweight - no network or charging required.
     */
    private fun defaultConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .setRequiresCharging(false)
            .build()
    }
}
