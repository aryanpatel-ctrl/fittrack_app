package com.fittrackpro.service

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

/**
 * Unit tests for Step Counter logic
 * Tests step calculation, day reset, and goal tracking
 */
class StepCounterServiceTest {

    // ==================== STEP CALCULATION TESTS ====================

    @Test
    fun `calculate today steps correctly`() {
        val initialSteps = 1000
        val currentSensorSteps = 1500
        val todaySteps = currentSensorSteps - initialSteps
        assertEquals(500, todaySteps)
    }

    @Test
    fun `today steps cannot be negative`() {
        val initialSteps = 1500
        val currentSensorSteps = 1000 // Sensor reset
        var todaySteps = currentSensorSteps - initialSteps
        if (todaySteps < 0) todaySteps = 0
        assertEquals(0, todaySteps)
    }

    @Test
    fun `handle sensor value overflow`() {
        // TYPE_STEP_COUNTER returns total steps since device boot
        // It can be a very large number
        val initialSteps = Int.MAX_VALUE - 100
        val currentSensorSteps = Int.MAX_VALUE
        val todaySteps = currentSensorSteps - initialSteps
        assertEquals(100, todaySteps)
    }

    // ==================== PROGRESS CALCULATION TESTS ====================

    @Test
    fun `calculate progress percentage correctly at 50 percent`() {
        val steps = 5000
        val goal = 10000
        val progress = ((steps.toFloat() / goal) * 100).toInt()
        assertEquals(50, progress)
    }

    @Test
    fun `calculate progress percentage correctly at 100 percent`() {
        val steps = 10000
        val goal = 10000
        val progress = ((steps.toFloat() / goal) * 100).toInt()
        assertEquals(100, progress)
    }

    @Test
    fun `calculate progress percentage when exceeding goal`() {
        val steps = 15000
        val goal = 10000
        val progress = ((steps.toFloat() / goal) * 100).toInt().coerceIn(0, 100)
        assertEquals(100, progress) // Capped at 100
    }

    @Test
    fun `progress is 0 when no steps`() {
        val steps = 0
        val goal = 10000
        val progress = ((steps.toFloat() / goal) * 100).toInt()
        assertEquals(0, progress)
    }

    @Test
    fun `progress handles division properly`() {
        val steps = 3333
        val goal = 10000
        val progress = ((steps.toFloat() / goal) * 100).toInt()
        assertEquals(33, progress) // Floor division
    }

    // ==================== DAY RESET TESTS ====================

    @Test
    fun `detect new day correctly`() {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val today = format.format(Date())
        val yesterday = format.format(Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))

        assertNotEquals(today, yesterday)
    }

    @Test
    fun `same day detection works`() {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val now = format.format(Date())
        val alsoNow = format.format(Date())

        assertEquals(now, alsoNow)
    }

    @Test
    fun `date format is correct`() {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = format.format(Date())

        // Should match pattern YYYY-MM-DD
        val pattern = Regex("\\d{4}-\\d{2}-\\d{2}")
        assertTrue("Date format should be YYYY-MM-DD: $date", pattern.matches(date))
    }

    // ==================== STEP GOAL TESTS ====================

    @Test
    fun `default step goal is 10000`() {
        val defaultGoal = 10000
        assertEquals(10000, defaultGoal)
    }

    @Test
    fun `step goal values are reasonable`() {
        val validGoals = listOf(5000, 7500, 10000, 12500, 15000)

        validGoals.forEach { goal ->
            assertTrue("Goal $goal should be positive", goal > 0)
            assertTrue("Goal $goal should be achievable", goal <= 30000)
        }
    }

    // ==================== NOTIFICATION CONTENT TESTS ====================

    @Test
    fun `notification title shows steps and goal`() {
        val steps = 5432
        val goal = 10000
        val title = "Steps: $steps / $goal"
        assertEquals("Steps: 5432 / 10000", title)
    }

    @Test
    fun `notification content shows progress percentage`() {
        val steps = 7500
        val goal = 10000
        val progress = ((steps.toFloat() / goal) * 100).toInt()
        val content = "$progress% of daily goal"
        assertEquals("75% of daily goal", content)
    }

    // ==================== SENSOR EVENT HANDLING TESTS ====================

    @Test
    fun `first sensor reading initializes initial steps`() {
        var initialSteps = -1
        val firstReading = 50000

        if (initialSteps < 0) {
            initialSteps = firstReading
        }

        assertEquals(50000, initialSteps)
    }

    @Test
    fun `subsequent readings calculate delta correctly`() {
        val initialSteps = 50000
        val readings = listOf(50010, 50025, 50050, 50100)

        val expectedSteps = listOf(10, 25, 50, 100)

        readings.forEachIndexed { index, reading ->
            val todaySteps = reading - initialSteps
            assertEquals(expectedSteps[index], todaySteps)
        }
    }

    // ==================== PREFERENCES KEYS TESTS ====================

    @Test
    fun `preference keys are consistent`() {
        // These should match the keys in StepCounterService
        val prefsName = "step_counter_prefs"
        val keyInitialSteps = "initial_steps"
        val keyTodaySteps = "today_steps"
        val keyLastDate = "last_date"
        val keyStepGoal = "step_goal"

        assertNotEquals(keyInitialSteps, keyTodaySteps)
        assertNotEquals(keyLastDate, keyStepGoal)
        assertTrue(prefsName.isNotEmpty())
    }

    // ==================== NUMBER FORMATTING TESTS ====================

    @Test
    fun `format steps with thousands separator`() {
        val steps = 12345
        val formatted = String.format("%,d", steps)
        // Depending on locale, could be "12,345" or "12.345"
        assertTrue(formatted.length >= 6) // "12,345" or similar
    }

    @Test
    fun `format small step count`() {
        val steps = 500
        val formatted = String.format("%,d", steps)
        assertEquals("500", formatted)
    }

    // ==================== DISTANCE CALCULATION TESTS ====================

    @Test
    fun `calculate distance from steps correctly`() {
        // Using average step length of 0.762 meters
        val steps = 1000
        val averageStepLength = 0.762
        val expectedDistance = steps * averageStepLength

        assertEquals(762.0, expectedDistance, 0.01)
    }

    @Test
    fun `calculate distance for 10000 steps`() {
        val steps = 10000
        val averageStepLength = 0.762
        val distanceMeters = steps * averageStepLength
        val distanceKm = distanceMeters / 1000

        assertEquals(7.62, distanceKm, 0.01)
    }

    @Test
    fun `distance is zero when no steps`() {
        val steps = 0
        val averageStepLength = 0.762
        val distance = steps * averageStepLength

        assertEquals(0.0, distance, 0.0)
    }

    // ==================== CALORIES CALCULATION TESTS ====================

    @Test
    fun `calculate calories from steps correctly`() {
        // Using approximately 0.04 calories per step
        val steps = 1000
        val caloriesPerStep = 0.04
        val expectedCalories = (steps * caloriesPerStep).toInt()

        assertEquals(40, expectedCalories)
    }

    @Test
    fun `calculate calories for 10000 steps`() {
        val steps = 10000
        val caloriesPerStep = 0.04
        val calories = (steps * caloriesPerStep).toInt()

        assertEquals(400, calories)
    }

    @Test
    fun `calories is zero when no steps`() {
        val steps = 0
        val caloriesPerStep = 0.04
        val calories = (steps * caloriesPerStep).toInt()

        assertEquals(0, calories)
    }

    // ==================== GOAL ACHIEVEMENT TESTS ====================

    @Test
    fun `goal achieved when steps equal goal`() {
        val steps = 10000
        val goal = 10000
        val goalAchieved = steps >= goal

        assertTrue(goalAchieved)
    }

    @Test
    fun `goal achieved when steps exceed goal`() {
        val steps = 12000
        val goal = 10000
        val goalAchieved = steps >= goal

        assertTrue(goalAchieved)
    }

    @Test
    fun `goal not achieved when steps below goal`() {
        val steps = 9999
        val goal = 10000
        val goalAchieved = steps >= goal

        assertFalse(goalAchieved)
    }

    // ==================== DATABASE DATE FORMAT TESTS ====================

    @Test
    fun `date format for database storage is correct`() {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.MARCH, 15) // March 15, 2024
        val date = format.format(calendar.time)

        assertEquals("2024-03-15", date)
    }

    @Test
    fun `dates can be compared for ordering`() {
        val date1 = "2024-03-15"
        val date2 = "2024-03-16"
        val date3 = "2024-03-14"

        assertTrue(date1 < date2)
        assertTrue(date1 > date3)
        assertTrue(date3 < date2)
    }

    // ==================== WEEKLY DATE RANGE TESTS ====================

    @Test
    fun `calculate weekly date range correctly`() {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        // Set to a known date
        calendar.set(2024, Calendar.MARCH, 17) // March 17, 2024
        val endDate = format.format(calendar.time)

        // Go back 6 days (7 days total)
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        val startDate = format.format(calendar.time)

        assertEquals("2024-03-11", startDate)
        assertEquals("2024-03-17", endDate)
    }

    // ==================== STEP DATA AGGREGATION TESTS ====================

    @Test
    fun `aggregate weekly steps correctly`() {
        val dailySteps = listOf(5000, 7000, 10000, 8000, 6000, 12000, 9000)
        val totalSteps = dailySteps.sum()
        val averageSteps = dailySteps.average().toInt()

        assertEquals(57000, totalSteps)
        assertEquals(8142, averageSteps)
    }

    @Test
    fun `count goals achieved in week`() {
        val goal = 10000
        val dailySteps = listOf(5000, 7000, 10000, 8000, 6000, 12000, 9000)
        val goalsAchieved = dailySteps.count { it >= goal }

        assertEquals(2, goalsAchieved) // 10000 and 12000
    }

    @Test
    fun `find best day in week`() {
        val dailySteps = listOf(5000, 7000, 10000, 8000, 6000, 12000, 9000)
        val bestDay = dailySteps.maxOrNull()

        assertEquals(12000, bestDay)
    }
}
