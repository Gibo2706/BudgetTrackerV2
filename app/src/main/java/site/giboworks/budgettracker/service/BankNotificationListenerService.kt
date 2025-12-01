package site.giboworks.budgettracker.service

import android.app.Notification
import android.content.Intent
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import site.giboworks.budgettracker.domain.model.Transaction
import site.giboworks.budgettracker.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * NotificationListenerService for automatically parsing bank notifications.
 * 
 * @deprecated Use BankNotificationService instead, which includes deduplication
 * and currency conversion features.
 * 
 * This service intercepts push notifications from Serbian bank apps and extracts
 * transaction amounts using regex patterns. Critical for the "automatic entry"
 * feature since Open Banking APIs are not available in Serbia.
 * 
 * SETUP REQUIRED:
 * 1. User must grant notification access in Settings > Apps > Special app access > Notification access
 * 2. Add bank packages to SUPPORTED_BANK_PACKAGES
 * 3. Configure regex patterns for each bank in BankNotificationParser
 * 
 * Privacy Note: This service only reads notifications from whitelisted bank packages.
 */
@AndroidEntryPoint
class BankNotificationListenerService : NotificationListenerService() {
    
    companion object {
        private const val TAG = "BankNotificationService"
        
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
        fun isNotificationAccessEnabled(context: android.content.Context): Boolean {
            val enabledListeners = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            return enabledListeners?.contains(context.packageName) == true
        }
        
        /**
         * Open notification access settings
         */
        fun openNotificationAccessSettings(context: android.content.Context) {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
    
    // Coroutine scope for background processing
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Repository for saving parsed transactions
    // Will be injected when Hilt is fully set up
    @Inject
    lateinit var transactionRepository: TransactionRepository
    
    private val parser = BankNotificationParser()
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "BankNotificationListenerService created")
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
        
        try {
            val notification = sbn.notification
            val extras = notification.extras
            
            // Extract notification content
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: text
            
            Log.d(TAG, "Notification - Title: $title, Text: $bigText")
            
            // Parse the notification
            val parsedTransaction = parser.parseNotification(
                packageName = packageName,
                title = title,
                text = bigText,
                timestamp = sbn.postTime
            )
            
            if (parsedTransaction != null) {
                Log.d(TAG, "Parsed transaction: ${parsedTransaction.amount} ${parsedTransaction.currency}")
                
                // Save to database
                serviceScope.launch {
                    try {
                        if (::transactionRepository.isInitialized) {
                            transactionRepository.insert(parsedTransaction)
                            Log.d(TAG, "Transaction saved successfully")
                            
                            // Notify user (could show a small toast or update a notification)
                            notifyTransactionSaved(parsedTransaction)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to save transaction", e)
                    }
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
     * Notify the user that a transaction was automatically captured
     */
    private fun notifyTransactionSaved(transaction: Transaction) {
        // TODO: Could show a small heads-up notification or update a persistent notification
        // showing the number of auto-captured transactions today
    }
}

// BankNotificationParser has been moved to BankNotificationService.kt
// This file is kept for backward compatibility but should use the new service.
