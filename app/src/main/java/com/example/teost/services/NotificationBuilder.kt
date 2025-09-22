package com.example.teost.services

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.teost.R
import com.example.teost.presentation.MainActivity

object NotificationBuilder {
    const val CHANNEL_ID = "edgeone_security_tests"
    const val NOTIFICATION_ID = 1001

    fun create(context: Context, contentText: String = context.getString(com.example.teost.core.ui.R.string.security_tests)): Notification {
        val intent = Intent(context, MainActivity::class.java)
        val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, piFlags)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.teoc)
            .setContentTitle(context.getString(com.example.teost.core.ui.R.string.app_name))
            .setContentText(contentText)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }
}

