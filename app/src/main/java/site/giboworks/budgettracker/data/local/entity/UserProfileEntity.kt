package site.giboworks.budgettracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import site.giboworks.budgettracker.domain.model.Achievement
import site.giboworks.budgettracker.domain.model.UserProfile
import java.time.LocalDate
import java.util.UUID
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room Entity for User Profile (Gamification data)
 */
@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    val displayName: String = "User",
    val credits: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalTransactionsLogged: Int = 0,
    val unlockedThemesJson: String = "[\"default\"]",
    val activeTheme: String = "default",
    val achievementsJson: String = "[]",
    val joinedDate: Long = System.currentTimeMillis(),
    
    val lastActiveDate: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): UserProfile {
        val gson = Gson()
        
        val unlockedThemes: List<String> = gson.fromJson(
            unlockedThemesJson,
            object : TypeToken<List<String>>() {}.type
        ) ?: listOf("default")
        
        val achievements: List<AchievementDto> = gson.fromJson(
            achievementsJson,
            object : TypeToken<List<AchievementDto>>() {}.type
        ) ?: emptyList()
        
        return UserProfile(
            id = id,
            displayName = displayName,
            credits = credits,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            totalTransactionsLogged = totalTransactionsLogged,
            unlockedThemes = unlockedThemes,
            activeTheme = activeTheme,
            achievements = achievements.map { it.toDomain() },
            joinedDate = LocalDate.ofEpochDay(joinedDate / (24 * 60 * 60 * 1000))
        )
    }
    
    companion object {
        fun fromDomain(profile: UserProfile): UserProfileEntity {
            val gson = Gson()
            
            return UserProfileEntity(
                id = profile.id,
                displayName = profile.displayName,
                credits = profile.credits,
                currentStreak = profile.currentStreak,
                longestStreak = profile.longestStreak,
                totalTransactionsLogged = profile.totalTransactionsLogged,
                unlockedThemesJson = gson.toJson(profile.unlockedThemes),
                activeTheme = profile.activeTheme,
                achievementsJson = gson.toJson(profile.achievements.map { AchievementDto.fromDomain(it) }),
                joinedDate = profile.joinedDate.toEpochDay() * 24 * 60 * 60 * 1000
            )
        }
    }
}

data class AchievementDto(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val unlockedAt: Long?,
    val creditsReward: Int
) {
    fun toDomain(): Achievement {
        return Achievement(
            id = id,
            name = name,
            description = description,
            icon = icon,
            unlockedAt = unlockedAt?.let { LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000)) },
            creditsReward = creditsReward
        )
    }
    
    companion object {
        fun fromDomain(achievement: Achievement): AchievementDto {
            return AchievementDto(
                id = achievement.id,
                name = achievement.name,
                description = achievement.description,
                icon = achievement.icon,
                unlockedAt = achievement.unlockedAt?.toEpochDay()?.times(24 * 60 * 60 * 1000),
                creditsReward = achievement.creditsReward
            )
        }
    }
}
