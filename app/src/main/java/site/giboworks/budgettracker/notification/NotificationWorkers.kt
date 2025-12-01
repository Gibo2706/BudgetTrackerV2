package site.giboworks.budgettracker.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import site.giboworks.budgettracker.MainActivity
import site.giboworks.budgettracker.R
import site.giboworks.budgettracker.data.local.dao.TransactionDao
import site.giboworks.budgettracker.domain.model.Currency
import site.giboworks.budgettracker.domain.repository.UserBudgetRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

/**
 * Smart Notification System - Your Financial Coach
 * 
 * Three scheduled notifications:
 * 1. Morning Briefing (08:00) - Daily budget reminder
 * 2. Danger Zone Check (14:00) - Mid-day spending alert (only if >80%)
 * 3. Victory Lap (21:00) - End of day celebration/summary
 */

// ==================== MORNING BRIEFING WORKER ====================

/**
 * Morning Briefing Worker - 08:00 AM
 * "Good morning! ☀️ Your mission: Keep it under [Amount] RSD today to fill your Shield."
 */
@HiltWorker
class MorningBriefingWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val userBudgetRepository: UserBudgetRepository
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            val budget = userBudgetRepository.getActive()
            
            if (budget == null) {
                return Result.success() // No budget configured, skip
            }
            
            val dailyLimit = calculateDailyLimit(
                income = budget.monthlyIncome,
                fixedExpenses = budget.fixedExpensesTotal,
                savings = budget.savingsTarget
            )
            
            val formattedAmount = formatCurrency(dailyLimit, budget.currency)
            
            val messages = listOf(
                "Good morning! ☀️ Your mission: Keep it under $formattedAmount today to fill your Shield.",
                "Rise and shine! 🌅 Today's target: Stay within $formattedAmount. You've got this!",
                "Morning champion! 💪 $formattedAmount is your daily superpower. Use it wisely!",
                "New day, new opportunity! 🎯 $formattedAmount available. Make every spend count!"
            )
            
            showNotification(
                context = context,
                title = "Your Daily Mission",
                message = messages.random(),
                channelId = CHANNEL_COACHING,
                notificationId = NOTIFICATION_MORNING
            )
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
    
    private fun calculateDailyLimit(income: Double, fixedExpenses: Double, savings: Double): Double {
        val disposable = income - fixedExpenses - savings
        val daysInMonth = YearMonth.now().lengthOfMonth()
        return (disposable / daysInMonth).coerceAtLeast(0.0)
    }
}

// ==================== DANGER ZONE WORKER ====================

/**
 * Danger Zone Check Worker - 14:00 PM
 * Only fires if CurrentDailySpend > 80% of DailyLimit
 * "Whoa, slow down! 🏎️ Heavy spending detected. Maybe cook dinner tonight?"
 */
@HiltWorker
class DangerZoneWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionDao: TransactionDao,
    private val userBudgetRepository: UserBudgetRepository
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            val budget = userBudgetRepository.getActive()
            
            if (budget == null) {
                return Result.success() // No budget configured, skip
            }
            
            val dailyLimit = calculateDailyLimit(
                income = budget.monthlyIncome,
                fixedExpenses = budget.fixedExpensesTotal,
                savings = budget.savingsTarget
            )
            
            // Get today's spending
            val today = LocalDate.now()
            val startOfDay = today.atStartOfDay().toEpochMilli()
            val endOfDay = today.plusDays(1).atStartOfDay().toEpochMilli()
            
            val currentSpending = transactionDao.getTotalNormalExpensesToday(startOfDay, endOfDay)
            
            val spendingPercentage = if (dailyLimit > 0) {
                (currentSpending / dailyLimit) * 100
            } else {
                0.0
            }
            
            // Only notify if spending > 80% of daily limit
            if (spendingPercentage >= 80) {
                val remaining = (dailyLimit - currentSpending).coerceAtLeast(0.0)
                val formattedRemaining = formatCurrency(remaining, budget.currency)
                
                val messages = listOf(
                    "Whoa, slow down! 🏎️ Heavy spending detected. Only $formattedRemaining left for today.",
                    "Heads up! ⚠️ You've used ${spendingPercentage.toInt()}% of today's budget. Maybe cook dinner tonight?",
                    "Budget alert! 📊 $formattedRemaining remaining. Time to tap into that willpower!",
                    "Quick check-in: 🎯 You're at ${spendingPercentage.toInt()}% of daily limit. Let's finish strong!"
                )
                
                showNotification(
                    context = context,
                    title = "Spending Check-In",
                    message = messages.random(),
                    channelId = CHANNEL_ALERTS,
                    notificationId = NOTIFICATION_DANGER
                )
            }
            // If under 80%, don't spam - just silently succeed
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
    
    private fun calculateDailyLimit(income: Double, fixedExpenses: Double, savings: Double): Double {
        val disposable = income - fixedExpenses - savings
        val daysInMonth = YearMonth.now().lengthOfMonth()
        return (disposable / daysInMonth).coerceAtLeast(0.0)
    }
}

// ==================== VICTORY LAP WORKER ====================

/**
 * Victory Lap Worker - 21:00 PM
 * IF CurrentDailySpend < DailyLimit:
 * "Circle Closed! 🟢 We're moving [SavedAmount] to virtual savings. Great job!"
 */
@HiltWorker
class VictoryLapWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionDao: TransactionDao,
    private val userBudgetRepository: UserBudgetRepository
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            val budget = userBudgetRepository.getActive()
            
            if (budget == null) {
                return Result.success() // No budget configured, skip
            }
            
            val dailyLimit = calculateDailyLimit(
                income = budget.monthlyIncome,
                fixedExpenses = budget.fixedExpensesTotal,
                savings = budget.savingsTarget
            )
            
            // Get today's spending
            val today = LocalDate.now()
            val startOfDay = today.atStartOfDay().toEpochMilli()
            val endOfDay = today.plusDays(1).atStartOfDay().toEpochMilli()
            
            val currentSpending = transactionDao.getTotalNormalExpensesToday(startOfDay, endOfDay)
            
            if (currentSpending < dailyLimit) {
                // Victory! Under budget
                val savedAmount = dailyLimit - currentSpending
                val formattedSaved = formatCurrency(savedAmount, budget.currency)
                
                val messages = listOf(
                    "Circle Closed! 🟢 You saved $formattedSaved today. Moving it to your Shield!",
                    "Victory Lap! 🏆 $formattedSaved under budget. Your future self thanks you!",
                    "You crushed it! 💪 $formattedSaved saved. That's the discipline of a champion!",
                    "Day complete! ✨ +$formattedSaved to savings. Keep this momentum going!"
                )
                
                showNotification(
                    context = context,
                    title = "Daily Victory! 🎉",
                    message = messages.random(),
                    channelId = CHANNEL_ACHIEVEMENTS,
                    notificationId = NOTIFICATION_VICTORY
                )
            } else {
                // Over budget - gentle, non-judgmental message
                val overAmount = currentSpending - dailyLimit
                val formattedOver = formatCurrency(overAmount, budget.currency)
                
                val messages = listOf(
                    "Day wrapped! 📊 $formattedOver over today, but tomorrow is a fresh start.",
                    "End of day check: You went $formattedOver over. No stress - it's already adjusting for tomorrow.",
                    "Today was a learning day. 📈 $formattedOver over, but your Rolling Budget has your back!"
                )
                
                showNotification(
                    context = context,
                    title = "Daily Summary",
                    message = messages.random(),
                    channelId = CHANNEL_COACHING,
                    notificationId = NOTIFICATION_VICTORY
                )
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
    
    private fun calculateDailyLimit(income: Double, fixedExpenses: Double, savings: Double): Double {
        val disposable = income - fixedExpenses - savings
        val daysInMonth = YearMonth.now().lengthOfMonth()
        return (disposable / daysInMonth).coerceAtLeast(0.0)
    }
}

// ==================== NOTIFICATION HELPERS ====================

private const val CHANNEL_COACHING = "coaching_channel"
private const val CHANNEL_ALERTS = "alerts_channel"
private const val CHANNEL_ACHIEVEMENTS = "achievements_channel"

private const val NOTIFICATION_MORNING = 1001
private const val NOTIFICATION_DANGER = 1002
private const val NOTIFICATION_VICTORY = 1003

private fun showNotification(
    context: Context,
    title: String,
    message: String,
    channelId: String,
    notificationId: Int
) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    // Create notification channel (required for Android 8+)
    createNotificationChannel(notificationManager, channelId)
    
    // Intent to open app when notification is tapped
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent = PendingIntent.getActivity(
        context, 
        0, 
        intent, 
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_launcher_foreground) // TODO: Use proper icon
        .setContentTitle(title)
        .setContentText(message)
        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()
    
    notificationManager.notify(notificationId, notification)
}

private fun createNotificationChannel(
    notificationManager: NotificationManager,
    channelId: String
) {
    val (name, description, importance) = when (channelId) {
        CHANNEL_COACHING -> Triple(
            "Financial Coaching",
            "Daily briefings and tips",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        CHANNEL_ALERTS -> Triple(
            "Spending Alerts",
            "Alerts when approaching daily limit",
            NotificationManager.IMPORTANCE_HIGH
        )
        CHANNEL_ACHIEVEMENTS -> Triple(
            "Achievements",
            "Celebrations for financial wins",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        else -> Triple(
            "Budget Tracker",
            "General notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        )
    }
    
    val channel = NotificationChannel(channelId, name, importance).apply {
        this.description = description
    }
    
    notificationManager.createNotificationChannel(channel)
}

private fun formatCurrency(amount: Double, currency: Currency): String {
    return when (currency) {
        Currency.RSD -> "%,.0f %s".format(amount, currency.symbol)
        else -> "%s%,.2f".format(currency.symbol, amount)
    }
}

private fun LocalDateTime.toEpochMilli(): Long {
    return this.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
