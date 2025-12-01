package site.giboworks.budgettracker.domain.util

import site.giboworks.budgettracker.domain.model.Currency

/**
 * Currency conversion helper for Balkan markets.
 * 
 * Base currency: RSD (Serbian Dinar)
 * All amounts are converted to RSD for consistent budgeting and reporting.
 * 
 * Note: Exchange rates are approximate and should be updated periodically.
 * For a production app, consider fetching live rates from an API.
 */
object CurrencyConverter {
    
    /**
     * Exchange rates to RSD (Serbian Dinar) as of December 2025
     * 1 unit of foreign currency = X RSD
     */
    private val exchangeRatesToRSD: Map<Currency, Double> = mapOf(
        Currency.RSD to 1.0,
        Currency.EUR to 117.5,   // 1 EUR ≈ 117.5 RSD
        Currency.USD to 108.0,   // 1 USD ≈ 108 RSD
        Currency.BAM to 60.0,    // 1 BAM (KM) ≈ 60 RSD (pegged to EUR at ~1.95583)
        Currency.MKD to 1.9,     // 1 MKD ≈ 1.9 RSD
        Currency.HRK to 15.6     // 1 HRK ≈ 15.6 RSD (legacy, Croatia uses EUR now)
    )
    
    /**
     * Convert amount from source currency to RSD.
     * 
     * @param amount The amount in source currency
     * @param from The source currency
     * @return The equivalent amount in RSD
     */
    fun toRSD(amount: Double, from: Currency): Double {
        val rate = exchangeRatesToRSD[from] ?: 1.0
        return amount * rate
    }
    
    /**
     * Convert amount from RSD to target currency.
     * 
     * @param amountRSD The amount in RSD
     * @param to The target currency
     * @return The equivalent amount in target currency
     */
    fun fromRSD(amountRSD: Double, to: Currency): Double {
        val rate = exchangeRatesToRSD[to] ?: 1.0
        return amountRSD / rate
    }
    
    /**
     * Convert amount between any two currencies.
     * 
     * @param amount The amount to convert
     * @param from Source currency
     * @param to Target currency
     * @return The converted amount
     */
    fun convert(amount: Double, from: Currency, to: Currency): Double {
        if (from == to) return amount
        val amountInRSD = toRSD(amount, from)
        return fromRSD(amountInRSD, to)
    }
    
    /**
     * Get the exchange rate for a currency to RSD.
     * 
     * @param currency The currency to get rate for
     * @return Exchange rate (1 unit of currency = X RSD)
     */
    fun getRate(currency: Currency): Double {
        return exchangeRatesToRSD[currency] ?: 1.0
    }
    
    /**
     * Format an amount with currency symbol.
     * Uses Serbian locale conventions (comma for decimals).
     * 
     * @param amount The amount to format
     * @param currency The currency
     * @param includeOriginal If true and original values provided, shows both
     * @param originalAmount Original amount before conversion
     * @param originalCurrency Original currency code
     * @return Formatted string like "11,750.00 дин." or "100.00 € (11,750.00 дин.)"
     */
    fun formatAmount(
        amount: Double,
        currency: Currency = Currency.RSD,
        includeOriginal: Boolean = false,
        originalAmount: Double? = null,
        originalCurrency: Currency? = null
    ): String {
        val formattedAmount = formatNumber(amount)
        val formatted = "$formattedAmount ${currency.symbol}"
        
        return if (includeOriginal && originalAmount != null && originalCurrency != null && originalCurrency != Currency.RSD) {
            val originalFormatted = formatNumber(originalAmount)
            "$originalFormatted ${originalCurrency.symbol} ($formatted)"
        } else {
            formatted
        }
    }
    
    /**
     * Format number with Serbian conventions (dot for thousands, comma for decimals).
     */
    private fun formatNumber(amount: Double): String {
        val wholePart = amount.toLong()
        val decimalPart = ((amount - wholePart) * 100).toLong()
        
        // Format with thousand separators (Serbian style: dots)
        val wholeFormatted = wholePart.toString()
            .reversed()
            .chunked(3)
            .joinToString(".")
            .reversed()
        
        return "$wholeFormatted,${decimalPart.toString().padStart(2, '0')}"
    }
    
    /**
     * Parse currency from string (flexible parsing for notification extraction).
     * Handles various formats: "EUR", "€", "RSD", "дин.", "din.", "BAM", "KM", etc.
     * 
     * @param currencyStr The currency string to parse
     * @return Parsed Currency enum, defaults to RSD if unknown
     */
    fun parseCurrency(currencyStr: String): Currency {
        val normalized = currencyStr.uppercase().trim()
        return when {
            normalized in listOf("RSD", "ДИН.", "ДИН", "DIN.", "DIN", "DINAR") -> Currency.RSD
            normalized in listOf("EUR", "€", "EURO", "EVRO") -> Currency.EUR
            normalized in listOf("USD", "$", "DOLLAR", "DOLAR") -> Currency.USD
            normalized in listOf("BAM", "KM", "MARKA", "KONVERTIBILNA MARKA") -> Currency.BAM
            normalized in listOf("MKD", "ДЕН.", "ДЕН", "DENAR") -> Currency.MKD
            normalized in listOf("HRK", "KN", "KUNA") -> Currency.HRK
            else -> Currency.RSD // Default for Serbian market
        }
    }
}

/**
 * Data class representing a parsed amount with currency information.
 * Used for notification parsing to preserve original currency data.
 */
data class ParsedAmount(
    val originalAmount: Double,
    val originalCurrency: Currency,
    val amountInRSD: Double
) {
    companion object {
        /**
         * Create ParsedAmount from raw values, automatically converting to RSD.
         */
        fun create(amount: Double, currency: Currency): ParsedAmount {
            return ParsedAmount(
                originalAmount = amount,
                originalCurrency = currency,
                amountInRSD = CurrencyConverter.toRSD(amount, currency)
            )
        }
    }
}
