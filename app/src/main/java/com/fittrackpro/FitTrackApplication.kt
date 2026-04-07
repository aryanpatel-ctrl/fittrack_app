package com.fittrackpro

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FitTrackApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Tracking Channel - High importance for active tracking
            val trackingChannel = NotificationChannel(
                CHANNEL_TRACKING,
                getString(R.string.channel_tracking),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for active activity tracking"
                setShowBadge(false)
            }

            // Achievements Channel
            val achievementsChannel = NotificationChannel(
                CHANNEL_ACHIEVEMENTS,
                getString(R.string.channel_achievements),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for achievements and badges"
            }

            // Challenges Channel
            val challengesChannel = NotificationChannel(
                CHANNEL_CHALLENGES,
                getString(R.string.channel_challenges),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for challenges and leaderboards"
            }

            // Water Reminders Channel
            val waterChannel = NotificationChannel(
                CHANNEL_WATER_REMINDER,
                "Water Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders to stay hydrated throughout the day"
            }

            notificationManager.createNotificationChannels(
                listOf(trackingChannel, achievementsChannel, challengesChannel, waterChannel)
            )
        }
    }

    companion object {
        const val CHANNEL_TRACKING = "tracking_channel"
        const val CHANNEL_ACHIEVEMENTS = "achievements_channel"
        const val CHANNEL_CHALLENGES = "challenges_channel"
        const val CHANNEL_WATER_REMINDER = "water_reminder_channel"
    }
}
