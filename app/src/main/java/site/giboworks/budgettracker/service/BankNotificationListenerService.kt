package site.giboworks.budgettracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import site.giboworks.budgettracker.MainActivity
import site.giboworks.budgettracker.R
import site.giboworks.budgettracker.data.preferences.AppPreferences
import site.giboworks.budgettracker.domain.model.Transaction
import site.giboworks.budgettracker.domain.repository.TransactionRepository
import site.giboworks.budgettracker.domain.repository.UserBudgetRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * NotificationListenerService for automatically parsing bank notifications.
 * 
 * This service intercepts push notifications from Serbian bank apps and extracts
 * transaction amounts using the NotificationParser with keyword-based categorization.
 * 
 * Key Features:
 * - Ignores INCOME notifications unless autoTrackIncome=true
 * - Ignores INFO notifications (balance inquiries)
 * - Parses EXPENSE notifications (payments, purchases)
 * - Sends feedback notification when transaction is saved
 * 
 * SETUP REQUIRED:
 * 1. User must grant notification access in Settings > Apps > Notification access
 * 2. User must grant POST_NOTIFICATIONS permission (Android 13+)
 * 
 * Privacy Note: This service only reads notifications from whitelisted bank packages.
 */
@AndroidEntryPoint
class BankNotificationListenerService : NotificationListenerService() {
    
    companion object {
        private const val TAG = "BankNotificationService"
        private const val CHANNEL_ID = "transaction_feedback"
        private const val CHANNEL_NAME = "Transaction Alerts"
        private const val NOTIFICATION_ID_BASE = 10000
        
        /**
         * Package names of Serbian bank applications.
         * Add more banks as needed.
         */
        val SUPPORTED_BANK_PACKAGES = setOf(
            // Serbian Banks
            "rs.raiffeisenbank.mobilebanking",      // Raiffeisen Bank Serbia
            "rs.unicreditbank.mobile",              // UniCredit Bank Serbia
            "rs.intesasanpaolo.mbanking",          // Banca Intesa Serbia
            "rs.kombank.mbanking",                  // Komercijalna Banka
            "rs.aikbanka.mbanking",                 // AIK Banka
            "rs.erstedigital.george",              // Erste Bank Serbia (George)
            "rs.otp.mbanking",                     // OTP Banka Serbia
            "rs.nlb.mbanking",                     // NLB Banka
            "rs.postanska.mbanking",              // Poštanska Štedionica
            "rs.procredit.mbanking",              // ProCredit Bank
            
            // Regional Banks (Balkan)
            "hr.pbz.mbanking",                    // PBZ Croatia
            "hr.zaba.mbanking",                   // Zagrebačka Banka
            "ba.raiffeisen.mbanking",             // Raiffeisen BH
            "me.ckb.mbanking",                    // CKB Montenegro
            
            // Test package for development
            "site.giboworks.budgettracker.test"
        )
        
        /**
         * Check if notification listening is enabled
         */
        fun isNotificationAccessEnabled(context: Context): Boolean {
            val enabledListeners = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            return enabledListeners?.contains(context.packageName) == true
        }
        
        /**
         * Open notification access settings
         */
        fun openNotificationAccessSettings(context: Context) {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
    
    // Coroutine scope for background processing
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    @Inject
    lateinit var transactionRepository: TransactionRepository
    
    @Inject
    lateinit var userBudgetRepository: UserBudgetRepository
    
    @Inject
    lateinit var appPreferences: AppPreferences
    
    private val parser = NotificationParser()
    private var notificationCounter = 0
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "BankNotificationListenerService created")
        createNotificationChannel()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "BankNotificationListenerService destroyed")
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }
    
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
    }
    
    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Notification listener disconnected")
    }
    
    /**
     * Called when a new notification is posted.
     * Filter for bank apps and extract transaction data.
     */
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        
        val packageName = sbn.packageName
        
        // Only process notifications from whitelisted bank apps
        if (packageName !in SUPPORTED_BANK_PACKAGES) {
            return
        }
        
        Log.d(TAG, "Bank notification received from: $packageName")
        
        // Check if Ghost Mode is enabled - if so, ignore all notifications
        val isGhostMode = runBlocking { appPreferences.getGhostModeEnabled() }
        if (isGhostMode) {
            Log.d(TAG, "Ghost Mode enabled - ignoring notification")
            return
        }
        
        try {
            val notification = sbn.notification
            val extras = notification.extras
            
            // Extract notification content
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: text
            
            Log.d(TAG, "Notification - Title: $title, Text: $bigText")
            
            // Get autoTrackIncome setting
            val autoTrackIncome = runBlocking { appPreferences.getAutoTrackIncome() }
            
            // Parse the notification
            val parseResult = parser.parseNotification(
                title = title,
                text = bigText,
                packageName = packageName,
                timestamp = sbn.postTime,
                autoTrackIncome = autoTrackIncome
            )
            
            when (parseResult) {
                is NotificationParser.ParseResult.Expense -> {
                    Log.d(TAG, "Parsed EXPENSE: ${parseResult.transaction.amount} ${parseResult.transaction.currency}")
                    saveAndNotify(parseResult.transaction, isExpense = true)
                }
                is NotificationParser.ParseResult.Income -> {
                    Log.d(TAG, "Parsed INCOME: ${parseResult.transaction.amount} ${parseResult.transaction.currency}")
                    saveAndNotify(parseResult.transaction, isExpense = false)
                }
                is NotificationParser.ParseResult.Info -> {
                    Log.d(TAG, "Notification ignored (info/balance)")
                }
                is NotificationParser.ParseResult.Unknown -> {
                    Log.d(TAG, "Notification ignored (unrecognized)")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing notification", e)
        }
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Optional: Track when notifications are dismissed
    }
    
    /**
     * Save transaction to database and send feedback notification
     */
    private fun saveAndNotify(transaction: Transaction, isExpense: Boolean) {
        serviceScope.launch {
            try {
                if (::transactionRepository.isInitialized) {
                    transactionRepository.insert(transaction)
                    Log.d(TAG, "Transaction saved successfully")
                    
                    // Get current daily budget remaining for feedback
                    val dailyLeft = calculateDailyLeft()
                    
                    // Send feedback notification
                    sendFeedbackNotification(transaction, dailyLeft, isExpense)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save transaction", e)
            }
        }
    }
    
    /**
     * Calculate remaining daily budget
     */
    private suspend fun calculateDailyLeft(): Double {
        return try {
            val budget = userBudgetRepository.observeActive().first()
            val today = LocalDate.now()
            val todayExpenses = transactionRepository.observeDailyExpenses(today).first()
            
            val dailyBudget = budget?.let {
                val daysInMonth = today.lengthOfMonth()
                val disposableIncome = it.monthlyIncome - it.fixedExpensesTotal - it.savingsTarget
                (disposableIncome / daysInMonth).coerceAtLeast(0.0)
            } ?: 0.0
            
            dailyBudget - todayExpenses
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate daily left", e)
            0.0
        }
    }
    
    /**
     * Create notification channel for Android 8.0+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for automatically tracked transactions"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 100, 50, 100)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Send feedback notification to user
     * 
     * Title: "Expense Logged: [Merchant]" or "Income Logged"
     * Body: "-1,234 RSD. Daily Safe-to-Spend: 3,456 RSD"
     */
    private fun sendFeedbackNotification(
        transaction: Transaction,
        dailyLeft: Double,
        isExpense: Boolean
    ) {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Create intent to open app when notification is clicked
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Build notification content
            val title = if (isExpense) {
                val merchant = transaction.merchantName ?: transaction.category.displayName
                "💸 Expense Logged: $merchant"
            } else {
                "💰 Income Logged"
            }
            
            val amountFormatted = "%,.0f".format(transaction.amount)
            val dailyLeftFormatted = "%,.0f".format(dailyLeft)
            
            val body = if (isExpense) {
                "-$amountFormatted ${transaction.currency.symbol}. Daily Safe-to-Spend: $dailyLeftFormatted ${transaction.currency.symbol}"
            } else {
                "+$amountFormatted ${transaction.currency.symbol}"
            }
            
            // Build notification
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .build()
            
            // Use incrementing ID to avoid overwriting
            notificationCounter++
            val notificationId = NOTIFICATION_ID_BASE + notificationCounter
            
            notificationManager.notify(notificationId, notification)
            Log.d(TAG, "Feedback notification sent: $title")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send feedback notification", e)
        }
    }
}
