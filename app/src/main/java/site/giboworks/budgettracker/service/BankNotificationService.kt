package site.giboworks.budgettracker.service

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import site.giboworks.budgettracker.domain.model.Currency
import site.giboworks.budgettracker.domain.model.Transaction
import site.giboworks.budgettracker.domain.model.TransactionCategory
import site.giboworks.budgettracker.domain.model.TransactionSource
import site.giboworks.budgettracker.domain.model.TransactionType
import site.giboworks.budgettracker.domain.repository.TransactionRepository
import site.giboworks.budgettracker.domain.util.CurrencyConverter
import site.giboworks.budgettracker.domain.util.ParsedAmount
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.math.abs

/**
 * NotificationListenerService for automatically parsing bank notifications.
 * 
 * This service intercepts push notifications from Serbian/Balkan bank apps and extracts
 * transaction amounts using regex patterns. Critical for the "automatic entry"
 * feature since Open Banking APIs are not available in Serbia.
 * 
 * KEY FEATURES:
 * - Multi-bank support (Serbian + regional Balkan banks)
 * - Currency conversion (EUR, BAM, USD → RSD)
 * - Deduplication logic (prevents duplicate entries from spammy notifications)
 * - SMS fallback support (for banks without push notifications)
 * 
 * SETUP REQUIRED:
 * 1. User must grant notification access in Settings > Apps > Special app access > Notification access
 * 2. Service declaration in AndroidManifest.xml with BIND_NOTIFICATION_LISTENER_SERVICE permission
 * 
 * Privacy Note: This service only reads notifications from whitelisted bank packages.
 */
@AndroidEntryPoint
class BankNotificationService : NotificationListenerService() {
    
    companion object {
        private const val TAG = "BankNotificationService"
        
        // Deduplication window: 5 minutes
        private const val DEDUP_WINDOW_MS = 5 * 60 * 1000L
        
        // Merchant similarity threshold (0.0 to 1.0)
        private const val MERCHANT_SIMILARITY_THRESHOLD = 0.7
        
        /**
         * Package names of Serbian and regional Balkan bank applications.
         * Also includes SMS app for banks that send transaction alerts via SMS.
         */
        val SUPPORTED_BANK_PACKAGES = setOf(
            // ========== Serbian Banks ==========
            "rs.raiffeisenbank.mobilebanking",      // Raiffeisen Bank Serbia
            "com.raiffeisen.mobile",                // Raiffeisen (alternative)
            "rs.unicreditbank.mobile",              // UniCredit Bank Serbia
            "rs.intesasanpaolo.mbanking",          // Banca Intesa Serbia
            "rs.kombank.mbanking",                  // Komercijalna Banka
            "rs.aikbanka.mbanking",                 // AIK Banka
            "rs.erstedigital.george",              // Erste Bank Serbia (George)
            "rs.otp.mbanking",                     // OTP Banka Serbia
            "rs.otp.bank",                         // OTP Bank (alternative)
            "rs.nlb.mbanking",                     // NLB Banka
            "rs.postanska.mbanking",              // Poštanska Štedionica
            "rs.procredit.mbanking",              // ProCredit Bank
            "rs.api.mbanking",                    // API Bank
            "rs.mts.banka",                       // MTS Banka
            
            // ========== Regional Banks (Bosnia, Montenegro, Croatia) ==========
            "hr.pbz.mbanking",                    // PBZ Croatia
            "hr.zaba.mbanking",                   // Zagrebačka Banka
            "ba.raiffeisen.mbanking",             // Raiffeisen BH
            "ba.unicredit.mbanking",              // UniCredit BH
            "me.ckb.mbanking",                    // CKB Montenegro
            "me.nlb.mbanking",                    // NLB Montenegro
            
            // ========== SMS Apps (fallback for transaction alerts) ==========
            "com.google.android.apps.messaging",  // Google Messages
            "com.samsung.android.messaging",      // Samsung Messages
            "com.android.mms",                    // Stock Android SMS
            
            // ========== Development/Testing ==========
            "site.giboworks.budgettracker.test"
        )
        
        /**
         * SMS sender IDs from banks (for SMS parsing)
         */
        val BANK_SMS_SENDERS = setOf(
            "RAIFFEISEN", "INTESA", "OTP", "UNICREDIT", "ERSTE",
            "KOMBANK", "AIK", "NLB", "PROCREDIT", "POSTANSKA",
            "381", "+381" // Serbian phone prefixes (some banks use phone numbers)
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
    
    // Repository for saving parsed transactions (injected via Hilt EntryPoint)
    @Inject
    lateinit var transactionRepository: TransactionRepository
    
    // Parser for extracting transaction data
    private val parser = BankNotificationParser()
    
    // Deduplication helper
    private val deduplicator = TransactionDeduplicator()
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "BankNotificationService created")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "BankNotificationService destroyed")
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }
    
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected - ready to capture bank transactions")
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
        
        // Only process notifications from whitelisted bank/SMS apps
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
            
            // For SMS apps, check if sender is a bank
            if (isSmsApp(packageName)) {
                if (!isBankSms(title)) {
                    Log.d(TAG, "SMS not from bank, ignoring: $title")
                    return
                }
            }
            
            Log.d(TAG, "Processing notification - Title: $title, Text: $bigText")
            
            // Parse the notification
            val source = if (isSmsApp(packageName)) TransactionSource.SMS else TransactionSource.NOTIFICATION
            val parsedTransaction = parser.parseNotification(
                packageName = packageName,
                title = title,
                text = bigText,
                timestamp = sbn.postTime,
                source = source
            )
            
            if (parsedTransaction != null) {
                Log.d(TAG, "Parsed transaction: ${parsedTransaction.amount} RSD " +
                    "(original: ${parsedTransaction.originalAmount} ${parsedTransaction.originalCurrency?.code})")
                
                // Save to database with deduplication
                serviceScope.launch {
                    try {
                        if (::transactionRepository.isInitialized) {
                            // Check for duplicates
                            val isDuplicate = deduplicator.isDuplicate(
                                newTransaction = parsedTransaction,
                                repository = transactionRepository
                            )
                            
                            if (isDuplicate) {
                                Log.d(TAG, "Transaction is a duplicate, skipping")
                                return@launch
                            }
                            
                            transactionRepository.insert(parsedTransaction)
                            Log.d(TAG, "Transaction saved successfully: ${parsedTransaction.id}")
                            
                            // Optionally notify user
                            notifyTransactionSaved(parsedTransaction)
                        } else {
                            Log.w(TAG, "Repository not initialized, cannot save transaction")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to save transaction", e)
                    }
                }
            } else {
                Log.d(TAG, "Could not parse transaction from notification")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing notification", e)
        }
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Optional: Track when notifications are dismissed
    }
    
    private fun isSmsApp(packageName: String): Boolean {
        return packageName in setOf(
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.android.mms"
        )
    }
    
    private fun isBankSms(title: String): Boolean {
        return BANK_SMS_SENDERS.any { sender ->
            title.contains(sender, ignoreCase = true)
        }
    }
    
    /**
     * Notify the user that a transaction was automatically captured
     */
    private fun notifyTransactionSaved(transaction: Transaction) {
        // TODO: Could show a small heads-up notification or update a persistent notification
        // showing the number of auto-captured transactions today
        Log.d(TAG, "Auto-captured: ${transaction.description} - ${transaction.amount} RSD")
    }
}

/**
 * Parser for extracting transaction data from bank notifications.
 * Supports Serbian and Balkan bank notification formats.
 */
class BankNotificationParser {
    
    companion object {
        /**
         * Regex patterns for extracting amount and currency from various notification formats.
         * 
         * Serbian formats:
         * - "Plaćeno karticom: 40.00 BAM na OMV PUMPA..."
         * - "Odliv: 1,200 RSD, MAXI..."
         * - "Uspešna transakcija 1.234,56 дин."
         * - "Kupovina: 500,00 EUR"
         */
        private val AMOUNT_PATTERNS = listOf(
            // Serbian format: 1.234,56 RSD (dot for thousands, comma for decimals)
            Regex("""(\d{1,3}(?:\.\d{3})*,\d{2})\s*(RSD|EUR|USD|BAM|дин\.?|din\.?|KM|€|\$)""", RegexOption.IGNORE_CASE),
            // Alternative Serbian: 1234,56 RSD (no thousand separator)
            Regex("""(\d+,\d{2})\s*(RSD|EUR|USD|BAM|дин\.?|din\.?|KM|€|\$)""", RegexOption.IGNORE_CASE),
            // International format: 1,234.56 EUR (comma for thousands, dot for decimals)
            Regex("""(\d{1,3}(?:,\d{3})*\.\d{2})\s*(RSD|EUR|USD|BAM)""", RegexOption.IGNORE_CASE),
            // Simple format: 1234.56 EUR
            Regex("""(\d+\.\d{2})\s*(RSD|EUR|USD|BAM)""", RegexOption.IGNORE_CASE),
            // Whole number with currency: 500 RSD, 1,200 RSD
            Regex("""(\d{1,3}(?:[.,]\d{3})*)\s*(RSD|EUR|USD|BAM|дин\.?|din\.?|KM)""", RegexOption.IGNORE_CASE),
            // Currency symbol prefix: €100.00, $50.00
            Regex("""([€\$])\s*(\d+[.,]?\d*)"""),
            // Currency symbol suffix: 100.00€, 50.00$
            Regex("""(\d+[.,]?\d*)\s*([€\$])"""),
            // Bosnian format: 40.00 KM
            Regex("""(\d+[.,]\d{2})\s*KM""", RegexOption.IGNORE_CASE)
        )
        
        /**
         * Patterns to extract merchant/location from notifications
         */
        private val MERCHANT_PATTERNS = listOf(
            // "na OMV PUMPA", "kod MAXI", "at McDonalds"
            Regex("""(?:na|kod|u|at|@)\s+([A-Za-z0-9\s\-\.čćžšđČĆŽŠĐ]+?)(?:,|\.|$|\d)""", RegexOption.IGNORE_CASE),
            // "MAXI, Beograd" or "OMV PUMPA"
            Regex("""(?:merchant|prodavac|trgovac|prodajno mesto):\s*([A-Za-z0-9\s\-\.čćžšđČĆŽŠĐ]+)""", RegexOption.IGNORE_CASE),
            // After amount: "1234.56 RSD, MAXI"
            Regex("""(?:RSD|EUR|BAM|дин\.?),\s*([A-Za-z][A-Za-z0-9\s\-\.čćžšđČĆŽŠĐ]+)""", RegexOption.IGNORE_CASE)
        )
        
        // Expense keywords (Serbian and English)
        private val EXPENSE_KEYWORDS = listOf(
            "placanje", "plaćanje", "kupovina", "uplata", "payment", "purchase",
            "pos", "withdrawal", "povlačenje", "isplata", "terećenje", "charge",
            "odliv", "transakcija", "platna kartica", "karticom", "rashod"
        )
        
        // Income keywords (Serbian and English)
        private val INCOME_KEYWORDS = listOf(
            "primljeno", "uplata na račun", "deposit", "prihod", "plata", "salary",
            "credit", "incoming", "primanje", "priliv", "uplate", "transfer primljen"
        )
    }
    
    /**
     * Parse a bank notification and extract transaction data
     */
    fun parseNotification(
        packageName: String,
        title: String,
        text: String,
        timestamp: Long,
        source: TransactionSource = TransactionSource.NOTIFICATION
    ): Transaction? {
        val fullText = "$title $text"
        
        // Try to extract amount and currency
        val parsedAmount = extractAmount(fullText) ?: return null
        
        // Determine transaction type
        val type = determineTransactionType(fullText)
        
        // Try to extract merchant name
        val merchant = extractMerchant(fullText)
        
        // Infer category based on merchant or keywords
        val category = inferCategory(fullText, merchant)
        
        // Build description
        val description = buildDescription(text, merchant, parsedAmount)
        
        return Transaction(
            amount = parsedAmount.amountInRSD,
            currency = Currency.RSD, // Always store in RSD
            category = category,
            description = description,
            timestamp = LocalDateTime.now(),
            type = type,
            source = source,
            merchantName = merchant,
            creditsEarned = 5, // Reward for automatic capture
            originalAmount = if (parsedAmount.originalCurrency != Currency.RSD) parsedAmount.originalAmount else null,
            originalCurrency = if (parsedAmount.originalCurrency != Currency.RSD) parsedAmount.originalCurrency else null
        )
    }
    
    /**
     * Extract amount and currency from notification text
     */
    private fun extractAmount(text: String): ParsedAmount? {
        for (pattern in AMOUNT_PATTERNS) {
            val match = pattern.find(text)
            if (match != null) {
                try {
                    val groups = match.groupValues
                    
                    val (amountStr, currencyStr) = when {
                        groups.size >= 3 -> {
                            // Check if first group is currency symbol
                            if (groups[1].matches(Regex("""[€\$]"""))) {
                                Pair(groups[2], groups[1])
                            } else {
                                Pair(groups[1], groups[2])
                            }
                        }
                        groups.size == 2 && groups[0].contains("KM", ignoreCase = true) -> {
                            Pair(groups[1], "KM")
                        }
                        else -> continue
                    }
                    
                    val amount = parseAmount(amountStr)
                    val currency = CurrencyConverter.parseCurrency(currencyStr)
                    
                    if (amount > 0) {
                        return ParsedAmount.create(amount, currency)
                    }
                } catch (e: Exception) {
                    Log.w("BankNotificationParser", "Failed to parse amount: ${e.message}")
                    continue
                }
            }
        }
        return null
    }
    
    /**
     * Parse amount string to Double, handling Serbian and international formats
     */
    private fun parseAmount(amountStr: String): Double {
        val cleanStr = amountStr.trim()
        
        return when {
            // Serbian: 1.234,56 (dot for thousands, comma for decimals)
            cleanStr.contains(",") && cleanStr.lastIndexOf(",") > cleanStr.lastIndexOf(".") -> {
                cleanStr.replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0
            }
            // International: 1,234.56 (comma for thousands, dot for decimals)
            cleanStr.contains(".") && cleanStr.lastIndexOf(".") > cleanStr.lastIndexOf(",") -> {
                cleanStr.replace(",", "").toDoubleOrNull() ?: 0.0
            }
            // Just comma (Serbian no thousands): 1234,56
            cleanStr.contains(",") -> {
                cleanStr.replace(",", ".").toDoubleOrNull() ?: 0.0
            }
            // Whole number with dot thousands: 1.200
            cleanStr.matches(Regex("""\d{1,3}(\.\d{3})+""")) -> {
                cleanStr.replace(".", "").toDoubleOrNull() ?: 0.0
            }
            else -> {
                cleanStr.toDoubleOrNull() ?: 0.0
            }
        }
    }
    
    /**
     * Determine if this is an expense or income
     */
    private fun determineTransactionType(text: String): TransactionType {
        val lowerText = text.lowercase()
        
        return when {
            INCOME_KEYWORDS.any { lowerText.contains(it) } -> TransactionType.INCOME
            EXPENSE_KEYWORDS.any { lowerText.contains(it) } -> TransactionType.EXPENSE
            else -> TransactionType.EXPENSE // Default to expense (most common)
        }
    }
    
    /**
     * Extract merchant name from notification
     */
    private fun extractMerchant(text: String): String? {
        for (pattern in MERCHANT_PATTERNS) {
            val match = pattern.find(text)
            if (match != null && match.groupValues.size >= 2) {
                val merchant = match.groupValues[1].trim()
                // Filter out common non-merchant matches
                if (merchant.length >= 2 && !merchant.matches(Regex("""^\d+$"""))) {
                    return merchant.take(50) // Limit length
                }
            }
        }
        return null
    }
    
    /**
     * Infer transaction category based on text content
     */
    private fun inferCategory(text: String, merchant: String?): TransactionCategory {
        val lowerText = (text + " " + (merchant ?: "")).lowercase()
        
        return when {
            // Groceries
            lowerText.containsAny("maxi", "idea", "lidl", "univerexport", "mercator", 
                "roda", "tempo", "market", "grocery", "namirnice", "dis", "aman") -> 
                TransactionCategory.FOOD_GROCERIES
            
            // Restaurants
            lowerText.containsAny("restaurant", "restoran", "kfc", "mcdonald", "burger",
                "pizza", "cafe", "kafana", "bistro", "grill", "ćevapi", "fast food") -> 
                TransactionCategory.FOOD_RESTAURANTS
            
            // Coffee
            lowerText.containsAny("coffee", "kafa", "starbucks", "costa", "kafić", "caffé") -> 
                TransactionCategory.FOOD_COFFEE
            
            // Fuel
            lowerText.containsAny("nis petrol", "mol", "gazprom", "lukoil", "omv",
                "benzin", "gorivo", "fuel", "petrol", "pumpa", "eko") -> 
                TransactionCategory.TRANSPORT_FUEL
            
            // Public transport
            lowerText.containsAny("gsp", "bus plus", "busplus", "metro", "parking", "jgsp") -> 
                TransactionCategory.TRANSPORT_PUBLIC
            
            // Taxi
            lowerText.containsAny("car:go", "cargo", "taxi", "uber", "bolt", "yandex", "pink") -> 
                TransactionCategory.TRANSPORT_TAXI
            
            // Utilities
            lowerText.containsAny("eps", "elektro", "electric", "struja") -> 
                TransactionCategory.UTILITIES_ELECTRICITY
            lowerText.containsAny("vodovod", "water", "jkp", "voda") -> 
                TransactionCategory.UTILITIES_WATER
            lowerText.containsAny("srbijagas", "gas", "toplana", "heating", "grejanje") -> 
                TransactionCategory.UTILITIES_GAS
            lowerText.containsAny("mts", "telenor", "a1", "yettel", "sbb", "internet", "orion") -> 
                TransactionCategory.UTILITIES_INTERNET
            
            // Shopping
            lowerText.containsAny("zara", "h&m", "c&a", "fashion", "mode", "shoes", "obuća") -> 
                TransactionCategory.SHOPPING_CLOTHES
            lowerText.containsAny("tehnomanija", "gigatron", "winwin", "computer",
                "electronic", "apple", "samsung", "emmi", "comtrade") -> 
                TransactionCategory.SHOPPING_ELECTRONICS
            
            // Entertainment
            lowerText.containsAny("netflix", "spotify", "youtube", "hbo", "disney") -> 
                TransactionCategory.ENTERTAINMENT_STREAMING
            lowerText.containsAny("steam", "playstation", "xbox", "game", "epic") -> 
                TransactionCategory.ENTERTAINMENT_GAMES
            lowerText.containsAny("bioskop", "cinema", "arena", "cineplexx", "ticket", "karte") -> 
                TransactionCategory.ENTERTAINMENT_EVENTS
            
            // Health
            lowerText.containsAny("apoteka", "pharmacy", "lek", "drug", "benu", "lilly") -> 
                TransactionCategory.HEALTH_PHARMACY
            lowerText.containsAny("doctor", "doktor", "klinika", "hospital", "medical", "dom zdravlja") -> 
                TransactionCategory.HEALTH_DOCTOR
            lowerText.containsAny("gym", "fitnes", "teretana", "sport", "fitness") -> 
                TransactionCategory.HEALTH_GYM
            
            // Default
            else -> TransactionCategory.OTHER_EXPENSE
        }
    }
    
    /**
     * Build a user-friendly description
     */
    private fun buildDescription(text: String, merchant: String?, parsedAmount: ParsedAmount): String {
        val amountInfo = if (parsedAmount.originalCurrency != Currency.RSD) {
            "${parsedAmount.originalAmount} ${parsedAmount.originalCurrency.code}"
        } else {
            ""
        }
        
        return when {
            merchant != null && amountInfo.isNotEmpty() -> "Payment at $merchant ($amountInfo)"
            merchant != null -> "Payment at $merchant"
            amountInfo.isNotEmpty() -> "Transaction ($amountInfo)"
            text.length <= 100 -> text
            else -> text.take(97) + "..."
        }
    }
    
    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it, ignoreCase = true) }
    }
}

/**
 * Deduplication helper for preventing duplicate transaction entries.
 * Banks often send multiple notifications for the same transaction.
 */
class TransactionDeduplicator {
    
    companion object {
        private const val TAG = "TransactionDeduplicator"
        
        // Time window for deduplication (5 minutes)
        private const val DEDUP_WINDOW_MS = 5 * 60 * 1000L
        
        // Amount tolerance (allows for small rounding differences)
        private const val AMOUNT_TOLERANCE = 0.01
    }
    
    /**
     * Check if a transaction is a duplicate of a recent transaction.
     * 
     * A transaction is considered duplicate if within the last 5 minutes:
     * - Same amount (within tolerance)
     * - Similar merchant name (if available)
     * 
     * @param newTransaction The new transaction to check
     * @param repository Transaction repository for querying recent transactions
     * @return true if duplicate, false if unique
     */
    suspend fun isDuplicate(
        newTransaction: Transaction,
        repository: TransactionRepository
    ): Boolean {
        val cutoffTime = System.currentTimeMillis() - DEDUP_WINDOW_MS
        
        // Get recent transactions from notification/SMS sources
        val recentTransactions = repository.getRecentBySource(
            sinceTimestamp = cutoffTime,
            source = newTransaction.source
        )
        
        // Also check the other source (SMS might duplicate notification)
        val otherSource = if (newTransaction.source == TransactionSource.NOTIFICATION) {
            TransactionSource.SMS
        } else {
            TransactionSource.NOTIFICATION
        }
        val recentFromOtherSource = repository.getRecentBySource(cutoffTime, otherSource)
        
        val allRecent = recentTransactions + recentFromOtherSource
        
        for (existing in allRecent) {
            if (isSameTransaction(newTransaction, existing)) {
                Log.d(TAG, "Duplicate detected: ${newTransaction.amount} matches ${existing.amount}")
                return true
            }
        }
        
        return false
    }
    
    /**
     * Check if two transactions represent the same real-world transaction
     */
    private fun isSameTransaction(new: Transaction, existing: Transaction): Boolean {
        // Check amount match (with small tolerance for rounding)
        val amountMatch = abs(new.amount - existing.amount) <= AMOUNT_TOLERANCE
        
        if (!amountMatch) return false
        
        // If both have merchants, check similarity
        if (new.merchantName != null && existing.merchantName != null) {
            val similarity = calculateSimilarity(
                new.merchantName.lowercase(),
                existing.merchantName.lowercase()
            )
            return similarity >= 0.7
        }
        
        // If only amounts match and one/both don't have merchants, consider it duplicate
        // This is conservative but prevents duplicate entries from spammy bank apps
        return true
    }
    
    /**
     * Calculate string similarity using Levenshtein distance
     */
    private fun calculateSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0
        
        val maxLen = maxOf(s1.length, s2.length)
        val distance = levenshteinDistance(s1, s2)
        
        return 1.0 - (distance.toDouble() / maxLen)
    }
    
    /**
     * Calculate Levenshtein distance between two strings
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return dp[s1.length][s2.length]
    }
}
