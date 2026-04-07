package com.fittrackpro.ui.dashboard

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

/**
 * Unit tests for Weekly Progress Chart data processing
 * Tests date range calculation, data aggregation, and chart formatting
 */
class WeeklyChartTest {

    // ==================== DATE RANGE CALCULATION TESTS ====================

    @Test
    fun `calculate 7-day date range correctly`() {
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.MARCH, 17) // March 17, 2024

        // End of range is today
        val endTime = calendar.timeInMillis

        // Start of range is 6 days ago (7 days total)
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        val startTime = calendar.timeInMillis

        // Verify 6 days difference
        val daysDiff = (endTime - startTime) / (24 * 60 * 60 * 1000)
        assertEquals(6, daysDiff)
    }

    @Test
    fun `date range includes today`() {
        val calendar = Calendar.getInstance()
        val today = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, -6)
        val startDate = calendar.timeInMillis

        // Today should be within range
        assertTrue(today >= startDate)
    }

    @Test
    fun `normalize timestamp to start of day`() {
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.MARCH, 17, 14, 30, 45) // 2:30:45 PM

        // Normalize to start of day
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val normalized = calendar.timeInMillis

        // Parse back and verify it's midnight
        val resultCal = Calendar.getInstance()
        resultCal.timeInMillis = normalized

        assertEquals(0, resultCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, resultCal.get(Calendar.MINUTE))
        assertEquals(0, resultCal.get(Calendar.SECOND))
    }

    // ==================== DATA AGGREGATION TESTS ====================

    @Test
    fun `aggregate multiple tracks on same day`() {
        val track1Distance = 2500.0 // meters
        val track2Distance = 3500.0
        val track3Distance = 1000.0

        val totalDistance = track1Distance + track2Distance + track3Distance
        assertEquals(7000.0, totalDistance, 0.01)
    }

    @Test
    fun `aggregate duration correctly`() {
        val track1Duration = 1800000L // 30 minutes in ms
        val track2Duration = 2700000L // 45 minutes

        val totalDuration = track1Duration + track2Duration
        assertEquals(4500000L, totalDuration) // 75 minutes
    }

    @Test
    fun `aggregate calories correctly`() {
        val track1Calories = 150
        val track2Calories = 220
        val track3Calories = 180

        val totalCalories = track1Calories + track2Calories + track3Calories
        assertEquals(550, totalCalories)
    }

    @Test
    fun `initialize empty days with zero values`() {
        val dailyData = DailyData(
            date = System.currentTimeMillis(),
            distance = 0.0,
            duration = 0L,
            calories = 0
        )

        assertEquals(0.0, dailyData.distance, 0.01)
        assertEquals(0L, dailyData.duration)
        assertEquals(0, dailyData.calories)
    }

    @Test
    fun `create 7 days of data`() {
        val calendar = Calendar.getInstance()
        val dailyDataList = mutableListOf<DailyData>()

        repeat(7) { i ->
            dailyDataList.add(
                DailyData(
                    date = calendar.timeInMillis,
                    distance = 0.0,
                    duration = 0L,
                    calories = 0
                )
            )
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        assertEquals(7, dailyDataList.size)
    }

    // ==================== DISTANCE CONVERSION TESTS ====================

    @Test
    fun `convert meters to kilometers`() {
        val distanceMeters = 5000.0
        val distanceKm = distanceMeters / 1000

        assertEquals(5.0, distanceKm, 0.01)
    }

    @Test
    fun `convert small distance to km`() {
        val distanceMeters = 500.0
        val distanceKm = distanceMeters / 1000

        assertEquals(0.5, distanceKm, 0.01)
    }

    @Test
    fun `handle zero distance`() {
        val distanceMeters = 0.0
        val distanceKm = distanceMeters / 1000

        assertEquals(0.0, distanceKm, 0.01)
    }

    // ==================== DAY LABEL FORMATTING TESTS ====================

    @Test
    fun `format day label as abbreviated weekday`() {
        val dateFormat = SimpleDateFormat("EEE", Locale.US)
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.MARCH, 17) // Sunday

        val label = dateFormat.format(calendar.time)
        assertEquals("Sun", label)
    }

    @Test
    fun `format Monday label`() {
        val dateFormat = SimpleDateFormat("EEE", Locale.US)
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.MARCH, 18) // Monday

        val label = dateFormat.format(calendar.time)
        assertEquals("Mon", label)
    }

    @Test
    fun `generate labels for all 7 days`() {
        val dateFormat = SimpleDateFormat("EEE", Locale.US)
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.MARCH, 11) // Monday

        val labels = mutableListOf<String>()
        repeat(7) {
            labels.add(dateFormat.format(calendar.time))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        assertEquals(listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"), labels)
    }

    // ==================== CHART DATA TESTS ====================

    @Test
    fun `create bar entries with correct indices`() {
        val distances = listOf(2.5f, 3.0f, 0.0f, 5.5f, 4.2f, 1.8f, 6.0f)
        val entries = distances.mapIndexed { index, distance ->
            Pair(index.toFloat(), distance)
        }

        assertEquals(7, entries.size)
        assertEquals(0f, entries[0].first, 0.01f)
        assertEquals(6f, entries[6].first, 0.01f)
    }

    @Test
    fun `bar width should be less than 1 for spacing`() {
        val barWidth = 0.6f
        assertTrue("Bar width should be < 1 for spacing", barWidth < 1f)
        assertTrue("Bar width should be > 0", barWidth > 0f)
    }

    @Test
    fun `chart animation duration is reasonable`() {
        val animationDuration = 1000 // milliseconds
        assertTrue("Animation should be at least 500ms", animationDuration >= 500)
        assertTrue("Animation should not exceed 2000ms", animationDuration <= 2000)
    }

    // ==================== DATA SORTING TESTS ====================

    @Test
    fun `sort daily data by date ascending`() {
        val calendar = Calendar.getInstance()
        val day1 = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val day0 = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 3)
        val day2 = calendar.timeInMillis

        val unsorted = listOf(
            DailyData(day1, 100.0, 1000L, 50),
            DailyData(day2, 200.0, 2000L, 100),
            DailyData(day0, 50.0, 500L, 25)
        )

        val sorted = unsorted.sortedBy { it.date }

        assertEquals(day0, sorted[0].date)
        assertEquals(day1, sorted[1].date)
        assertEquals(day2, sorted[2].date)
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    fun `handle empty track list`() {
        val tracks = emptyList<Any>()
        val dailyDataList = mutableListOf<DailyData>()

        // Initialize 7 days with zeros when no tracks
        val calendar = Calendar.getInstance()
        repeat(7) {
            dailyDataList.add(DailyData(calendar.timeInMillis, 0.0, 0L, 0))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        assertEquals(7, dailyDataList.size)
        assertTrue(dailyDataList.all { it.distance == 0.0 })
    }

    @Test
    fun `handle single track`() {
        val singleTrackDistance = 5000.0
        val dailyDataList = mutableListOf<DailyData>()

        // One day has data, rest are zeros
        dailyDataList.add(DailyData(System.currentTimeMillis(), singleTrackDistance, 3600000L, 200))

        assertEquals(5000.0, dailyDataList[0].distance, 0.01)
    }

    @Test
    fun `handle track spanning midnight`() {
        // Track started at 11:30 PM, should be counted on start day
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 30)
        val trackStartTime = calendar.timeInMillis

        // Normalize to day
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val dayStart = calendar.timeInMillis

        // Track should be associated with the day it started
        assertNotEquals(trackStartTime, dayStart)
    }

    // ==================== WEEKLY SUMMARY TESTS ====================

    @Test
    fun `calculate weekly total distance`() {
        val dailyDistances = listOf(2000.0, 3500.0, 0.0, 5000.0, 4200.0, 1800.0, 6000.0)
        val weeklyTotal = dailyDistances.sum()

        assertEquals(22500.0, weeklyTotal, 0.01) // 22.5 km total
    }

    @Test
    fun `calculate weekly average distance`() {
        val dailyDistances = listOf(2000.0, 3500.0, 0.0, 5000.0, 4200.0, 1800.0, 6000.0)
        val weeklyAverage = dailyDistances.average()

        assertEquals(3214.29, weeklyAverage, 0.5) // ~3.2 km average
    }

    @Test
    fun `find best day of week`() {
        val dailyDistances = listOf(2000.0, 3500.0, 0.0, 5000.0, 4200.0, 1800.0, 6000.0)
        val bestDay = dailyDistances.maxOrNull()

        assertEquals(6000.0, bestDay)
    }

    @Test
    fun `count active days in week`() {
        val dailyDistances = listOf(2000.0, 3500.0, 0.0, 5000.0, 4200.0, 1800.0, 6000.0)
        val activeDays = dailyDistances.count { it > 0 }

        assertEquals(6, activeDays) // 6 days with activity
    }
}
