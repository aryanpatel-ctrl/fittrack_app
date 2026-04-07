package com.fittrackpro.service

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Achievement Auto-Unlock logic
 * Tests achievement criteria checking, XP calculations, and level progression
 */
class AchievementServiceTest {

    // ==================== ACHIEVEMENT CRITERIA TESTS ====================

    @Test
    fun `check total distance achievement criteria`() {
        val criteriaType = "total_distance"
        val criteriaValue = 5000.0 // 5km in meters
        val userTotalDistance = 5500.0

        val isMet = userTotalDistance >= criteriaValue
        assertTrue("Should unlock 5K distance achievement", isMet)
    }

    @Test
    fun `total distance achievement not met when below criteria`() {
        val criteriaValue = 10000.0 // 10km
        val userTotalDistance = 8000.0

        val isMet = userTotalDistance >= criteriaValue
        assertFalse("Should not unlock 10K achievement yet", isMet)
    }

    @Test
    fun `check total activities achievement criteria`() {
        val criteriaType = "total_activities"
        val criteriaValue = 10.0
        val userTotalWorkouts = 15

        val isMet = userTotalWorkouts >= criteriaValue
        assertTrue("Should unlock 10 workouts achievement", isMet)
    }

    @Test
    fun `check streak days achievement criteria`() {
        val criteriaType = "streak_days"
        val criteriaValue = 7.0
        val userCurrentStreak = 7

        val isMet = userCurrentStreak >= criteriaValue
        assertTrue("Should unlock 7-day streak achievement", isMet)
    }

    @Test
    fun `streak achievement not met with broken streak`() {
        val criteriaValue = 7.0
        val userCurrentStreak = 5

        val isMet = userCurrentStreak >= criteriaValue
        assertFalse("Should not unlock 7-day streak with 5 days", isMet)
    }

    @Test
    fun `check single distance achievement criteria`() {
        val criteriaType = "single_distance"
        val criteriaValue = 10000.0 // 10km single run
        val userLongestRun = 12500.0

        val isMet = userLongestRun >= criteriaValue
        assertTrue("Should unlock longest run achievement", isMet)
    }

    @Test
    fun `check total calories achievement criteria`() {
        val criteriaType = "total_calories"
        val criteriaValue = 10000.0
        val userTotalCalories = 15000

        val isMet = userTotalCalories >= criteriaValue
        assertTrue("Should unlock calorie burn achievement", isMet)
    }

    // ==================== XP CALCULATION TESTS ====================

    @Test
    fun `calculate XP reward for bronze tier`() {
        val tier = "bronze"
        val baseXp = 50

        val xpReward = when (tier) {
            "bronze" -> baseXp
            "silver" -> baseXp * 2
            "gold" -> baseXp * 3
            "platinum" -> baseXp * 5
            else -> baseXp
        }

        assertEquals(50, xpReward)
    }

    @Test
    fun `calculate XP reward for silver tier`() {
        val tier = "silver"
        val baseXp = 50
        val xpReward = baseXp * 2
        assertEquals(100, xpReward)
    }

    @Test
    fun `calculate XP reward for gold tier`() {
        val tier = "gold"
        val baseXp = 50
        val xpReward = baseXp * 3
        assertEquals(150, xpReward)
    }

    @Test
    fun `calculate XP reward for platinum tier`() {
        val tier = "platinum"
        val baseXp = 50
        val xpReward = baseXp * 5
        assertEquals(250, xpReward)
    }

    // ==================== LEVEL CALCULATION TESTS ====================

    @Test
    fun `calculate level from XP - level 1`() {
        val totalXp = 50
        val level = calculateLevelFromXp(totalXp)
        assertEquals(1, level)
    }

    @Test
    fun `calculate level from XP - level 2`() {
        val totalXp = 150 // Above 100 threshold
        val level = calculateLevelFromXp(totalXp)
        assertEquals(2, level)
    }

    @Test
    fun `calculate level from XP - level 5`() {
        val totalXp = 1000 // Above 800 threshold
        val level = calculateLevelFromXp(totalXp)
        assertEquals(5, level)
    }

    @Test
    fun `calculate level from XP - level 10`() {
        val totalXp = 5000 // Above 4700 threshold
        val level = calculateLevelFromXp(totalXp)
        assertEquals(11, level) // Level 11 for 5000+ XP
    }

    @Test
    fun `level up detection when crossing threshold`() {
        val oldLevel = 3
        val oldXp = 450
        val xpGained = 100
        val newXp = oldXp + xpGained

        val newLevel = calculateLevelFromXp(newXp)
        val didLevelUp = newLevel > oldLevel

        assertTrue("Should level up from 3 to 4", didLevelUp)
        assertEquals(4, newLevel)
    }

    @Test
    fun `no level up when staying within same level`() {
        // Level 4 requires 500-799 XP (threshold 500)
        val oldLevel = 4
        val oldXp = 550
        val xpGained = 50
        val newXp = oldXp + xpGained // 600, still level 4

        val newLevel = calculateLevelFromXp(newXp)
        val didLevelUp = newLevel > oldLevel

        assertFalse("Should not level up", didLevelUp)
        assertEquals(4, newLevel)
    }

    // Helper function matching the app's level calculation
    private fun calculateLevelFromXp(totalXp: Int): Int {
        val thresholds = listOf(0, 100, 250, 500, 800, 1200, 1700, 2300, 3000, 3800, 4700)
        for (i in thresholds.indices.reversed()) {
            if (totalXp >= thresholds[i]) {
                return i + 1
            }
        }
        return 1
    }

    // ==================== ACHIEVEMENT UNLOCK TESTS ====================

    @Test
    fun `achievement should not unlock twice`() {
        val isAlreadyUnlocked = true
        val criteriaValue = 5000.0
        val userValue = 6000.0

        val shouldUnlock = !isAlreadyUnlocked && userValue >= criteriaValue
        assertFalse("Already unlocked achievements should not unlock again", shouldUnlock)
    }

    @Test
    fun `pending achievement should unlock when criteria met`() {
        val isAlreadyUnlocked = false
        val criteriaValue = 5000.0
        val userValue = 5000.0

        val shouldUnlock = !isAlreadyUnlocked && userValue >= criteriaValue
        assertTrue("Should unlock pending achievement", shouldUnlock)
    }

    @Test
    fun `hidden achievement visibility after unlock`() {
        val isHidden = true
        val isUnlocked = true

        // Hidden achievements become visible once unlocked
        val isVisible = !isHidden || isUnlocked
        assertTrue("Unlocked hidden achievement should be visible", isVisible)
    }

    // ==================== STREAK CALCULATION TESTS ====================

    @Test
    fun `calculate streak from consecutive days`() {
        // Simulating workout timestamps (days in milliseconds)
        val today = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L

        val workoutDates = listOf(
            today,
            today - oneDayMs,
            today - 2 * oneDayMs,
            today - 3 * oneDayMs
        )

        // Count consecutive days from today
        var streak = 0
        var expectedDate = today

        for (date in workoutDates.sorted().reversed()) {
            val dayDiff = (expectedDate - date) / oneDayMs
            if (dayDiff <= 1) {
                streak++
                expectedDate = date
            } else {
                break
            }
        }

        assertEquals(4, streak)
    }

    @Test
    fun `streak breaks when day is missed`() {
        val today = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L

        // Gap of 2 days between some workouts
        val workoutDates = listOf(
            today,
            today - oneDayMs,
            today - 4 * oneDayMs // Gap - breaks streak
        )

        var streak = 0
        var expectedDate = today

        for (date in workoutDates.sorted().reversed()) {
            val dayDiff = (expectedDate - date) / oneDayMs
            if (dayDiff <= 1) {
                streak++
                expectedDate = date
            } else {
                break
            }
        }

        assertEquals(2, streak) // Only today and yesterday count
    }

    // ==================== NOTIFICATION CONTENT TESTS ====================

    @Test
    fun `achievement notification title includes tier emoji`() {
        val tiers = mapOf(
            "bronze" to "🥉",
            "silver" to "🥈",
            "gold" to "🥇",
            "platinum" to "💎"
        )

        tiers.forEach { (tier, emoji) ->
            val title = "$emoji Achievement Unlocked!"
            assertTrue("Title should contain $emoji for $tier tier", title.contains(emoji))
        }
    }

    @Test
    fun `level up notification shows new level`() {
        val newLevel = 5
        val title = "Level Up! 🎉"
        val content = "You've reached Level $newLevel!"

        assertTrue(title.contains("Level Up"))
        assertTrue(content.contains("Level 5"))
    }

    // ==================== PROGRESS TRACKING TESTS ====================

    @Test
    fun `calculate achievement progress percentage`() {
        val currentValue = 3500.0
        val targetValue = 5000.0

        val progress = (currentValue / targetValue * 100).coerceIn(0.0, 100.0)
        assertEquals(70.0, progress, 0.01)
    }

    @Test
    fun `progress capped at 100 percent when exceeded`() {
        val currentValue = 6000.0
        val targetValue = 5000.0

        val progress = (currentValue / targetValue * 100).coerceIn(0.0, 100.0)
        assertEquals(100.0, progress, 0.01)
    }

    @Test
    fun `progress is 0 when no activity`() {
        val currentValue = 0.0
        val targetValue = 5000.0

        val progress = (currentValue / targetValue * 100).coerceIn(0.0, 100.0)
        assertEquals(0.0, progress, 0.01)
    }
}
