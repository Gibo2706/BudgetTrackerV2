package site.giboworks.budgettracker.domain.model

/**
 * Available themes that can be unlocked with credits.
 * Each theme changes the ring colors and overall app appearance.
 */
object UnlockableThemes {
    
    val defaultTheme = RingTheme(
        id = "default",
        name = "Ocean Blue",
        description = "The default calming blue theme",
        creditsCost = 0,
        pulseColors = Pair(0xFF56CCF2, 0xFF2F80ED),
        shieldColors = Pair(0xFF667eea, 0xFF764ba2),
        clarityColors = Pair(0xFF4776E6, 0xFF8E54E9),
        isPremium = false
    )
    
    val sunriseTheme = RingTheme(
        id = "sunrise",
        name = "Sunrise",
        description = "Warm orange and pink gradients",
        creditsCost = 500,
        pulseColors = Pair(0xFFFF9966, 0xFFFF5E62),
        shieldColors = Pair(0xFFf7971e, 0xFFffd200),
        clarityColors = Pair(0xFFfc4a1a, 0xFFf7b733),
        isPremium = false
    )
    
    val forestTheme = RingTheme(
        id = "forest",
        name = "Forest",
        description = "Deep greens for nature lovers",
        creditsCost = 500,
        pulseColors = Pair(0xFF11998e, 0xFF38ef7d),
        shieldColors = Pair(0xFF56ab2f, 0xFFa8e063),
        clarityColors = Pair(0xFF134E5E, 0xFF71B280),
        isPremium = false
    )
    
    val neonTheme = RingTheme(
        id = "neon",
        name = "Neon Nights",
        description = "Vibrant cyberpunk colors",
        creditsCost = 1000,
        pulseColors = Pair(0xFFf953c6, 0xFFb91d73),
        shieldColors = Pair(0xFF00F5A0, 0xFF00D9F5),
        clarityColors = Pair(0xFFf5af19, 0xFFf12711),
        isPremium = false
    )
    
    val galaxyTheme = RingTheme(
        id = "galaxy",
        name = "Galaxy",
        description = "Deep space purple and blue",
        creditsCost = 1500,
        pulseColors = Pair(0xFF654ea3, 0xFFeaafc8),
        shieldColors = Pair(0xFF0f0c29, 0xFF302b63),
        clarityColors = Pair(0xFF141E30, 0xFF243B55),
        isPremium = true
    )
    
    val goldPremiumTheme = RingTheme(
        id = "gold_premium",
        name = "Golden Hour",
        description = "Luxurious gold and bronze",
        creditsCost = 2500,
        pulseColors = Pair(0xFFBF953F, 0xFFFCF6BA),
        shieldColors = Pair(0xFFaa9714, 0xFFce8b08),
        clarityColors = Pair(0xFFD4A655, 0xFFE8D5A3),
        isPremium = true
    )
    
    /**
     * All available themes
     */
    val allThemes = listOf(
        defaultTheme,
        sunriseTheme,
        forestTheme,
        neonTheme,
        galaxyTheme,
        goldPremiumTheme
    )
    
    /**
     * Get theme by ID
     */
    fun getTheme(id: String): RingTheme {
        return allThemes.find { it.id == id } ?: defaultTheme
    }
}

/**
 * Predefined achievements for gamification
 */
object Achievements {
    
    val firstTransaction = Achievement(
        id = "first_transaction",
        name = "First Steps",
        description = "Log your first transaction",
        icon = "🎯",
        creditsReward = 50
    )
    
    val weekStreak = Achievement(
        id = "week_streak",
        name = "Week Warrior",
        description = "Maintain a 7-day logging streak",
        icon = "🔥",
        creditsReward = 100
    )
    
    val monthStreak = Achievement(
        id = "month_streak",
        name = "Monthly Master",
        description = "Maintain a 30-day logging streak",
        icon = "⭐",
        creditsReward = 500
    )
    
    val savingsGoalReached = Achievement(
        id = "savings_goal",
        name = "Shield Bearer",
        description = "Reach your monthly savings goal",
        icon = "🛡️",
        creditsReward = 200
    )
    
    val allBillsPaid = Achievement(
        id = "all_bills_paid",
        name = "Crystal Clear",
        description = "Pay all bills before their due date",
        icon = "💎",
        creditsReward = 150
    )
    
    val underBudgetWeek = Achievement(
        id = "under_budget_week",
        name = "Budget Boss",
        description = "Stay under daily budget for 7 days straight",
        icon = "👑",
        creditsReward = 200
    )
    
    val microSavingsPro = Achievement(
        id = "micro_savings_10",
        name = "Willpower Warrior",
        description = "Record 10 micro-savings (things you decided not to buy)",
        icon = "💪",
        creditsReward = 250
    )
    
    val hundredTransactions = Achievement(
        id = "hundred_transactions",
        name = "Data Driven",
        description = "Log 100 transactions",
        icon = "📊",
        creditsReward = 300
    )
    
    val notificationHunter = Achievement(
        id = "auto_capture_50",
        name = "Automation Expert",
        description = "Have 50 transactions auto-captured from notifications",
        icon = "🤖",
        creditsReward = 200
    )
    
    val themeCollector = Achievement(
        id = "theme_collector",
        name = "Theme Collector",
        description = "Unlock 3 different themes",
        icon = "🎨",
        creditsReward = 100
    )
    
    /**
     * All achievements
     */
    val allAchievements = listOf(
        firstTransaction,
        weekStreak,
        monthStreak,
        savingsGoalReached,
        allBillsPaid,
        underBudgetWeek,
        microSavingsPro,
        hundredTransactions,
        notificationHunter,
        themeCollector
    )
}
