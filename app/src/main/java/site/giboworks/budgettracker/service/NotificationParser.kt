package site.giboworks.budgettracker.service

import android.util.Log
import site.giboworks.budgettracker.domain.model.Currency
import site.giboworks.budgettracker.domain.model.Transaction
import site.giboworks.budgettracker.domain.model.TransactionCategory
import site.giboworks.budgettracker.domain.model.TransactionSource
import site.giboworks.budgettracker.domain.model.TransactionType
import java.time.LocalDateTime

/**
 * Advanced "Battle-Tested" Notification Parser for Serbian Banks
 * 
 * This parser uses strict keyword-based categorization with bank-specific rules:
 * 
 * PRIORITY ORDER (higher priority wins):
 * 1. EXPENSE Keywords (RED LIST) - Always treated as expense
 * 2. INCOME Keywords (GREEN LIST) - Only if autoTrackIncome=true
 * 3. INFO Keywords (IGNORE LIST) - Always ignored
 * 
 * CONFLICT RESOLUTION:
 * - If message has BOTH expense AND income keywords, it's an EXPENSE
 * - "Korišćenje kartice" + "Raspoloživo stanje" = EXPENSE (not info)
 * 
 * Bank-Specific Rules:
 * - Raiffeisen: "Koriscenje kartice" is always EXPENSE
 * - Unicredit: Check for "terećenje" or "odliv" to distinguish expense from income
 */
class NotificationParser {
    
    companion object {
        private const val TAG = "NotificationParser"
        
        // ═══════════════════════════════════════════════════════════════════
        // RED LIST - EXPENSE KEYWORDS (PRIORITY 1 - HIGHEST)
        // If ANY of these are present, it's an EXPENSE
        // ═══════════════════════════════════════════════════════════════════
        private val EXPENSE_KEYWORDS_PRIORITY_1 = listOf(
            // Raiffeisen-specific (CRITICAL)
            "koriscenje kartice",
            "korišćenje kartice",
            "korištenje kartice",
            
            // Payment/Purchase
            "kupovina",
            "placeno",
            "plaćeno",
            "placanje",
            "plaćanje",
            "iznos transakcije",
            
            // Outflow
            "odliv",
            "terećenje",
            "terecenje",
            "rashod",
            
            // Card transactions  
            "pos ",                    // POS terminal (space to avoid false positives)
            "pos terminal",
            "karticom",
            "platna kartica",
            "debitna kartica",
            
            // Cash
            "podizanje gotovine",
            "bankomat",
            "atm",
            "isplata",
            "withdrawal",
            
            // Fees
            "naplata",
            "provizija",
            "naknada",
            
            // English keywords
            "payment",
            "purchase",
            "charge"
        )
        
        // ═══════════════════════════════════════════════════════════════════
        // GREEN LIST - INCOME KEYWORDS (PRIORITY 2)
        // Only processed if autoTrackIncome == true
        // ═══════════════════════════════════════════════════════════════════
        private val INCOME_KEYWORDS = listOf(
            // Salary/Wage
            "priliv",
            "uplata zarade",
            "uplata po racunu",
            "uplata po računu",
            "zarada",
            "plata",
            "primljena uplata",
            "prihod",
            "primanje",
            "transfer primljen",
            "primljeno",
            "uplata na račun",
            "uplata na racun",
            
            // Refunds
            "storno",
            "refundacija",
            "povraćaj",
            "povracaj",
            "povrat sredstava",
            "reklamacija",
            
            // Credit/Deposit
            "odobrenje",
            "kredit na račun",
            "incoming",
            "deposit",
            "credit"
        )
        
        // ═══════════════════════════════════════════════════════════════════
        // IGNORE LIST - INFO/BALANCE KEYWORDS (PRIORITY 3 - LOWEST)
        // These are ignored ONLY if no expense/income keywords present
        // ═══════════════════════════════════════════════════════════════════
        private val INFO_KEYWORDS = listOf(
            "stanje na računu",
            "stanje na racunu",
            "raspoloživo stanje",
            "raspolozivo stanje",
            "upit stanja",
            "proverite stanje",
            "vaše stanje",
            "vase stanje",
            "trenutno stanje",
            "preostalo",
            "dostupno",
            "aktivirano",
            "istekla",
            "podsjetnik",
            "podsetnik",
            "obavještenje",
            "obavestenje",
            "otp kod",
            "verifikacioni kod",
            "aktivacija"
        )
        
        // ═══════════════════════════════════════════════════════════════════
        // AMOUNT EXTRACTION PATTERNS
        // ═══════════════════════════════════════════════════════════════════
        private val AMOUNT_PATTERNS = listOf(
            // Serbian format: 1.234,56 RSD (dot for thousands, comma for decimals)
            Regex("""(\d{1,3}(?:\.\d{3})*,\d{2})\s*(RSD|EUR|USD|BAM|дин\.?|din\.?)""", RegexOption.IGNORE_CASE),
            // Serbian format without thousands: 234,56 RSD
            Regex("""(\d+,\d{2})\s*(RSD|EUR|USD|BAM|дин\.?|din\.?)""", RegexOption.IGNORE_CASE),
            // International format: 1,234.56 EUR
            Regex("""(\d{1,3}(?:,\d{3})*\.\d{2})\s*(RSD|EUR|USD|BAM)""", RegexOption.IGNORE_CASE),
            // Simple format: 1234.56 EUR
            Regex("""(\d+\.\d{2})\s*(RSD|EUR|USD|BAM)""", RegexOption.IGNORE_CASE),
            // Whole number: 500 RSD, 1.200 RSD, 1,200 RSD
            Regex("""(\d{1,3}(?:[.,]\d{3})*)\s*(RSD|EUR|USD|BAM|дин\.?|din\.?|KM)""", RegexOption.IGNORE_CASE),
            // Currency prefix: €100, $50
            Regex("""([€\$])\s*(\d+[.,]?\d*)"""),
            // Currency suffix: 100€, 50$
            Regex("""(\d+[.,]?\d*)\s*([€\$])"""),
            // Bosnian format: 40.00 KM
            Regex("""(\d+[.,]\d{2})\s*KM""", RegexOption.IGNORE_CASE),
            // Iznos pattern: "Iznos: 1.234,56"
            Regex("""iznos[:\s]+(\d{1,3}(?:\.\d{3})*,\d{2})""", RegexOption.IGNORE_CASE),
            Regex("""iznos[:\s]+(\d+,\d{2})""", RegexOption.IGNORE_CASE)
        )
        
        // ═══════════════════════════════════════════════════════════════════
        // MERCHANT EXTRACTION PATTERNS
        // ═══════════════════════════════════════════════════════════════════
        private val MERCHANT_PATTERNS = listOf(
            // "na OMV PUMPA", "kod MAXI", "u LIDL"
            Regex("""(?:na|kod|u|at|@)\s+([A-Za-z0-9\s\-\.čćžšđČĆŽŠĐ]{2,30})(?:,|\.|$|\d)""", RegexOption.IGNORE_CASE),
            // "merchant:", "prodavac:", "trgovac:"
            Regex("""(?:merchant|prodavac|trgovac|prodajno mesto)[:\s]+([A-Za-z0-9\s\-\.čćžšđČĆŽŠĐ]+)""", RegexOption.IGNORE_CASE),
            // After amount: "1234.56 RSD, MAXI"
            Regex("""(?:RSD|EUR|BAM|дин\.?),\s*([A-Za-z][A-Za-z0-9\s\-\.čćžšđČĆŽŠĐ]+)""", RegexOption.IGNORE_CASE)
        )
    }
    
    /**
     * Result of notification parsing
     */
    sealed class ParseResult {
        data class Expense(val transaction: Transaction) : ParseResult()
        data class Income(val transaction: Transaction) : ParseResult()
        object Info : ParseResult()
        object Unknown : ParseResult()
    }
    
    /**
     * Parse a bank notification and return appropriate result.
     * 
     * @param title Notification title
     * @param text Notification body
     * @param packageName Bank package name for specific rules
     * @param timestamp Notification timestamp
     * @param autoTrackIncome If true, income transactions are also parsed
     * @return ParseResult indicating expense, income (if enabled), info, or unknown
     */
    fun parseNotification(
        title: String,
        text: String,
        packageName: String? = null,
        timestamp: Long = System.currentTimeMillis(),
        autoTrackIncome: Boolean = false
    ): ParseResult {
        val fullText = "$title $text"
        val normalizedText = fullText.lowercase()
        
        Log.d(TAG, "═══════════════════════════════════════════════════════════")
        Log.d(TAG, "Parsing: $fullText")
        Log.d(TAG, "Package: $packageName, AutoTrackIncome: $autoTrackIncome")
        
        // ═══════════════════════════════════════════════════════════════════
        // STEP 1: Check for EXPENSE keywords (PRIORITY 1 - HIGHEST)
        // If ANY expense keyword is present, it's an EXPENSE regardless of other keywords
        // ═══════════════════════════════════════════════════════════════════
        val hasExpenseKeyword = EXPENSE_KEYWORDS_PRIORITY_1.any { keyword ->
            normalizedText.contains(keyword)
        }
        
        if (hasExpenseKeyword) {
            Log.d(TAG, "✓ EXPENSE keyword detected")
            return parseAsExpense(fullText, normalizedText, packageName, timestamp)
        }
        
        // ═══════════════════════════════════════════════════════════════════
        // STEP 2: Check for INCOME keywords (PRIORITY 2)
        // Only process if autoTrackIncome is enabled
        // ═══════════════════════════════════════════════════════════════════
        val hasIncomeKeyword = INCOME_KEYWORDS.any { keyword ->
            normalizedText.contains(keyword)
        }
        
        if (hasIncomeKeyword) {
            if (autoTrackIncome) {
                Log.d(TAG, "✓ INCOME keyword detected, autoTrackIncome=true, parsing...")
                return parseAsIncome(fullText, normalizedText, packageName, timestamp)
            } else {
                Log.d(TAG, "✓ INCOME keyword detected, but autoTrackIncome=false, IGNORING")
                return ParseResult.Info // Treat as info/ignored
            }
        }
        
        // ═══════════════════════════════════════════════════════════════════
        // STEP 3: Check for INFO keywords (PRIORITY 3 - LOWEST)
        // ═══════════════════════════════════════════════════════════════════
        val hasInfoKeyword = INFO_KEYWORDS.any { keyword ->
            normalizedText.contains(keyword)
        }
        
        if (hasInfoKeyword) {
            Log.d(TAG, "✓ INFO keyword detected - ignoring")
            return ParseResult.Info
        }
        
        // ═══════════════════════════════════════════════════════════════════
        // STEP 4: Unknown - no recognized keywords
        // ═══════════════════════════════════════════════════════════════════
        Log.d(TAG, "✗ No recognized keywords - unknown notification")
        return ParseResult.Unknown
    }
    
    /**
     * Legacy method - returns Transaction only for expenses
     */
    fun parseExpenseOnly(
        title: String,
        text: String,
        packageName: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ): Transaction? {
        val result = parseNotification(title, text, packageName, timestamp, autoTrackIncome = false)
        return when (result) {
            is ParseResult.Expense -> result.transaction
            else -> null
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE PARSING METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private fun parseAsExpense(
        fullText: String,
        normalizedText: String,
        packageName: String?,
        timestamp: Long
    ): ParseResult {
        val amountData = extractAmount(fullText)
        if (amountData == null) {
            Log.d(TAG, "✗ Could not extract amount from expense notification")
            return ParseResult.Unknown
        }
        
        val (amount, currency) = amountData
        val merchant = extractMerchant(fullText)
        val category = inferCategory(normalizedText, merchant)
        val amountInRsd = convertToRsd(amount, currency)
        val description = buildDescription(fullText, merchant, amount, currency)
        
        Log.d(TAG, "✓ Parsed EXPENSE: $amountInRsd RSD at ${merchant ?: "unknown"}")
        
        val transaction = Transaction(
            amount = amountInRsd,
            currency = Currency.RSD,
            category = category,
            description = description,
            timestamp = LocalDateTime.now(),
            type = TransactionType.EXPENSE,
            source = TransactionSource.NOTIFICATION,
            merchantName = merchant,
            creditsEarned = 5,
            originalAmount = if (currency != Currency.RSD) amount else null,
            originalCurrency = if (currency != Currency.RSD) currency else null
        )
        
        return ParseResult.Expense(transaction)
    }
    
    private fun parseAsIncome(
        fullText: String,
        normalizedText: String,
        packageName: String?,
        timestamp: Long
    ): ParseResult {
        val amountData = extractAmount(fullText)
        if (amountData == null) {
            Log.d(TAG, "✗ Could not extract amount from income notification")
            return ParseResult.Unknown
        }
        
        val (amount, currency) = amountData
        val amountInRsd = convertToRsd(amount, currency)
        val description = inferIncomeDescription(normalizedText)
        
        Log.d(TAG, "✓ Parsed INCOME: $amountInRsd RSD - $description")
        
        val transaction = Transaction(
            amount = amountInRsd,
            currency = Currency.RSD,
            category = TransactionCategory.SALARY,
            description = description,
            timestamp = LocalDateTime.now(),
            type = TransactionType.INCOME,
            source = TransactionSource.NOTIFICATION,
            creditsEarned = 0,
            originalAmount = if (currency != Currency.RSD) amount else null,
            originalCurrency = if (currency != Currency.RSD) currency else null
        )
        
        return ParseResult.Income(transaction)
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // AMOUNT EXTRACTION
    // ═══════════════════════════════════════════════════════════════════════════
    
    private fun extractAmount(text: String): Pair<Double, Currency>? {
        for (pattern in AMOUNT_PATTERNS) {
            val match = pattern.find(text)
            if (match != null) {
                try {
                    val groups = match.groupValues
                    
                    val (amountStr, currencyStr) = when {
                        groups.size >= 3 -> {
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
                    
                    val amount = parseAmountString(amountStr)
                    val currency = parseCurrencyString(currencyStr)
                    
                    if (amount > 0) {
                        return Pair(amount, currency)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse amount: ${e.message}")
                    continue
                }
            }
        }
        return null
    }
    
    private fun parseAmountString(amountStr: String): Double {
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
    
    private fun parseCurrencyString(currencyStr: String): Currency {
        return when (currencyStr.uppercase().trim()) {
            "RSD", "ДИН", "ДИН.", "DIN", "DIN." -> Currency.RSD
            "EUR", "€" -> Currency.EUR
            "USD", "$" -> Currency.USD
            "BAM", "KM" -> Currency.BAM
            else -> Currency.RSD
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MERCHANT EXTRACTION
    // ═══════════════════════════════════════════════════════════════════════════
    
    private fun extractMerchant(text: String): String? {
        for (pattern in MERCHANT_PATTERNS) {
            val match = pattern.find(text)
            if (match != null) {
                val merchant = match.groupValues.getOrNull(1)?.trim()
                if (!merchant.isNullOrBlank() && merchant.length >= 2) {
                    return merchant.take(50)
                }
            }
        }
        return null
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CATEGORY INFERENCE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private fun inferCategory(text: String, merchant: String?): TransactionCategory {
        val searchText = "$text ${merchant ?: ""}".lowercase()
        
        return when {
            // Food & Coffee
            searchText.containsAny("kafa", "coffee", "caffe", "kafić", "kafic", "starbucks", "costa") -> 
                TransactionCategory.FOOD_COFFEE
            searchText.containsAny("restoran", "restaurant", "mcdonalds", "mcdonald", "kfc", "burger", "pizza", "sushi") -> 
                TransactionCategory.FOOD_RESTAURANTS
            searchText.containsAny("maxi", "idea", "lidl", "aman", "univerexport", "merkator", "market", "prodavnica", "grocery", "namirnice") -> 
                TransactionCategory.FOOD_GROCERIES
            searchText.containsAny("pekara", "bakery", "hleb", "pecivo") -> 
                TransactionCategory.FOOD_GROCERIES
            searchText.containsAny("dostava", "delivery", "wolt", "glovo", "mr d", "donesi") -> 
                TransactionCategory.FOOD_RESTAURANTS
            
            // Transport
            searchText.containsAny("gorivo", "benzin", "nafta", "omv", "nis petrol", "lukoil", "mol", "gas station", "pumpa") -> 
                TransactionCategory.TRANSPORT_FUEL
            searchText.containsAny("taxi", "taksi", "car:go", "cargo", "uber", "bolt") -> 
                TransactionCategory.TRANSPORT_TAXI
            searchText.containsAny("bus plus", "gsp", "beograd", "busplus", "javni prevoz", "metro", "autobus") -> 
                TransactionCategory.TRANSPORT_PUBLIC
            searchText.containsAny("parking", "garaža", "garaza") -> 
                TransactionCategory.OTHER_EXPENSE
            
            // Shopping
            searchText.containsAny("zara", "hm", "h&m", "c&a", "reserved", "bershka", "pull&bear", "oděća", "odeca", "fashion") -> 
                TransactionCategory.SHOPPING_CLOTHES
            searchText.containsAny("gigatron", "tehnomanija", "winwin", "comtrade", "ct shop", "laptop", "telefon", "phone", "computer", "tech") -> 
                TransactionCategory.SHOPPING_ELECTRONICS
            searchText.containsAny("apoteka", "pharmacy", "lek", "medicine", "benu", "lilly") -> 
                TransactionCategory.HEALTH_PHARMACY
            
            // Entertainment
            searchText.containsAny("netflix", "hbo", "disney", "spotify", "youtube", "apple music", "deezer") -> 
                TransactionCategory.ENTERTAINMENT_STREAMING
            searchText.containsAny("bioskop", "cinema", "cineplexx", "arena cinemas", "movie") -> 
                TransactionCategory.ENTERTAINMENT_EVENTS
            
            // Utilities & Bills
            searchText.containsAny("eps", "elektro", "struja", "electricity", "infostan", "komunalije") -> 
                TransactionCategory.UTILITIES_ELECTRICITY
            searchText.containsAny("mts", "telenor", "a1", "yettel", "sbb", "orion", "internet", "mobilni") -> 
                TransactionCategory.UTILITIES_INTERNET
            
            // Cash
            searchText.containsAny("bankomat", "atm", "podizanje", "withdrawal", "gotovina") -> 
                TransactionCategory.OTHER_EXPENSE
            
            // Default
            else -> TransactionCategory.OTHER_EXPENSE
        }
    }
    
    private fun inferIncomeDescription(text: String): String {
        return when {
            text.containsAny("zarada", "plata", "salary") -> "Zarada"
            text.containsAny("storno", "refund") -> "Refundacija"
            text.containsAny("transfer", "uplata") -> "Uplata"
            else -> "Priliv"
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CURRENCY CONVERSION
    // ═══════════════════════════════════════════════════════════════════════════
    
    private fun convertToRsd(amount: Double, currency: Currency): Double {
        return when (currency) {
            Currency.RSD -> amount
            Currency.EUR -> amount * 117.0
            Currency.USD -> amount * 108.0
            Currency.BAM -> amount * 60.0
            else -> amount
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DESCRIPTION BUILDER
    // ═══════════════════════════════════════════════════════════════════════════
    
    private fun buildDescription(
        originalText: String,
        merchant: String?,
        amount: Double,
        currency: Currency
    ): String {
        return when {
            merchant != null -> merchant
            currency != Currency.RSD -> "%.2f %s".format(amount, currency.name)
            else -> "Bank notification"
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STRING EXTENSIONS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it, ignoreCase = true) }
    }
}
