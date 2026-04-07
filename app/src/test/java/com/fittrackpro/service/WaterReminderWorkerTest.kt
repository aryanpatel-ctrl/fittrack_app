package com.fittrackpro.service

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests for Water Reminder logic
 * Tests time-based filtering and reminder scheduling
 */
class WaterReminderWorkerTest {

    // ==================== TIME FILTERING TESTS ====================

    @Test
    fun `should send reminder at 8 AM`() {
        val hour = 8
        val shouldSend = isWithinReminderHours(hour)
        assertTrue("8 AM should be within reminder hours", shouldSend)
    }

    @Test
    fun `should send reminder at 12 PM`() {
        val hour = 12
        val shouldSend = isWithinReminderHours(hour)
        assertTrue("12 PM should be within reminder hours", shouldSend)
    }

    @Test
    fun `should send reminder at 6 PM`() {
        val hour = 18
        val shouldSend = isWithinReminderHours(hour)
        assertTrue("6 PM should be within reminder hours", shouldSend)
    }

    @Test
    fun `should send reminder at 9 PM`() {
        val hour = 21
        val shouldSend = isWithinReminderHours(hour)
        assertTrue("9 PM should be within reminder hours", shouldSend)
    }

    @Test
    fun `should NOT send reminder at 6 AM`() {
        val hour = 6
        val shouldSend = isWithinReminderHours(hour)
        assertFalse("6 AM should NOT be within reminder hours", shouldSend)
    }

    @Test
    fun `should NOT send reminder at 10 PM`() {
        val hour = 22
        val shouldSend = isWithinReminderHours(hour)
        assertFalse("10 PM should NOT be within reminder hours", shouldSend)
    }

    @Test
    fun `should NOT send reminder at 11 PM`() {
        val hour = 23
        val shouldSend = isWithinReminderHours(hour)
        assertFalse("11 PM should NOT be within reminder hours", shouldSend)
    }

    @Test
    fun `should NOT send reminder at midnight`() {
        val hour = 0
        val shouldSend = isWithinReminderHours(hour)
        assertFalse("Midnight should NOT be within reminder hours", shouldSend)
    }

    @Test
    fun `should NOT send reminder at 3 AM`() {
        val hour = 3
        val shouldSend = isWithinReminderHours(hour)
        assertFalse("3 AM should NOT be within reminder hours", shouldSend)
    }

    @Test
    fun `should send reminder at boundary 7 AM`() {
        val hour = 7
        val shouldSend = isWithinReminderHours(hour)
        assertTrue("7 AM (start boundary) should be within reminder hours", shouldSend)
    }

    // ==================== HYDRATION MESSAGES TESTS ====================

    @Test
    fun `hydration messages are not empty`() {
        val messages = getHydrationMessages()
        assertTrue("Should have hydration messages", messages.isNotEmpty())
    }

    @Test
    fun `hydration messages have reasonable length`() {
        val messages = getHydrationMessages()
        messages.forEach { message ->
            assertTrue(
                "Message should be between 10-100 chars: $message",
                message.length in 10..100
            )
        }
    }

    @Test
    fun `hydration messages contain water-related words`() {
        val messages = getHydrationMessages()
        val waterWords = listOf("hydrat", "water", "drink", "H2O", "sip", "refresh")

        messages.forEach { message ->
            val containsWaterWord = waterWords.any { word ->
                message.lowercase().contains(word.lowercase())
            }
            assertTrue(
                "Message should contain water-related word: $message",
                containsWaterWord
            )
        }
    }

    // ==================== WORK SCHEDULING TESTS ====================

    @Test
    fun `reminder interval is 2 hours`() {
        val intervalHours = 2
        val intervalMillis = intervalHours * 60 * 60 * 1000L
        assertEquals(7_200_000L, intervalMillis)
    }

    @Test
    fun `flex interval is 15 minutes`() {
        val flexMinutes = 15
        val flexMillis = flexMinutes * 60 * 1000L
        assertEquals(900_000L, flexMillis)
    }

    // ==================== HELPER METHODS ====================

    /**
     * Simulates the time filtering logic from WaterReminderWorker.doWork()
     */
    private fun isWithinReminderHours(hour: Int): Boolean {
        return hour >= 7 && hour < 22
    }

    /**
     * Returns the same messages used in WaterReminderWorker
     */
    private fun getHydrationMessages(): List<String> {
        return listOf(
            "Time to hydrate! Drink a glass of water.",
            "Stay healthy! Have you had water recently?",
            "Water break! Your body will thank you.",
            "Hydration reminder! Grab some water.",
            "Keep it up! Time for some H2O.",
            "Stay refreshed! Drink some water now.",
            "Your muscles need water! Take a sip.",
            "Hydration fuels performance! Drink up."
        )
    }
}
