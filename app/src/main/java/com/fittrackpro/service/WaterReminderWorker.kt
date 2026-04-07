package com.fittrackpro.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fittrackpro.FitTrackApplication
import com.fittrackpro.R
import com.fittrackpro.data.local.preferences.UserPreferences
import com.fittrackpro.ui.main.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar

@HiltWorker
class WaterReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val userPreferences: UserPreferences
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "water_reminder_work"
        private const val NOTIFICATION_ID = 3001

        private val HYDRATION_MESSAGES = listOf(
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

    override suspend fun doWork(): Result {
        // Check if water reminders are still enabled
        if (!userPreferences.waterRemindersEnabled) {
            return Result.success()
        }

        // Only send reminders during reasonable hours (7 AM - 10 PM)
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (currentHour < 7 || currentHour >= 22) {
            return Result.success()
        }

        sendWaterReminderNotification()
        return Result.success()
    }

    private fun sendWaterReminderNotification() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = HYDRATION_MESSAGES.random()

        val notification = NotificationCompat.Builder(context, FitTrackApplication.CHANNEL_WATER_REMINDER)
            .setSmallIcon(R.drawable.ic_water)
            .setContentTitle("Stay Hydrated!")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Handle missing notification permission gracefully
        }
    }
}
