package site.giboworks.budgettracker

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import site.giboworks.budgettracker.notification.NotificationScheduler
import javax.inject.Inject

/**
 * Application class for Budget Tracker.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 * Implements Configuration.Provider for WorkManager with Hilt.
 */
@HiltAndroidApp
class BudgetTrackerApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var notificationScheduler: NotificationScheduler
    
    override fun onCreate() {
        super.onCreate()
        
        // Schedule daily notification workers
        notificationScheduler.scheduleAllNotifications()
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
