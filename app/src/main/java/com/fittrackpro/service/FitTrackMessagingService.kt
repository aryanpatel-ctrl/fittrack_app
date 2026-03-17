package com.fittrackpro.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.fittrackpro.FitTrackApplication
import com.fittrackpro.R
import com.fittrackpro.ui.main.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FitTrackMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "FitTrack Pro"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: ""
        val type = remoteMessage.data["type"] ?: "general"
        showNotification(title, body, type)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    private fun showNotification(title: String, body: String, type: String) {
        val channelId = when (type) {
            "challenge" -> FitTrackApplication.CHANNEL_CHALLENGES
            "achievement" -> FitTrackApplication.CHANNEL_ACHIEVEMENTS
            else -> FitTrackApplication.CHANNEL_TRACKING
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("notification_type", type)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title).setContentText(body)
            .setSmallIcon(R.drawable.ic_activities)
            .setAutoCancel(true).setContentIntent(pendingIntent).build()

        getSystemService(NotificationManager::class.java)
            .notify(System.currentTimeMillis().toInt(), notification)
    }
}
