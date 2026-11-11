package com.driveup.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.content.Context
import java.time.LocalTime

object NotificationHelper {
    const val CHANNEL_ID = "driveup_reminders"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val channel = NotificationChannel(
                CHANNEL_ID, "课前提醒", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "在课程开始前提醒"
                setSound(soundUri, audioAttr)
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    fun showReminder(context: Context, courseName: String, advanceMinutes: Int) {
        val hour = LocalTime.now().hour
        val greeting = when {
            hour < 12 -> "早上好"
            hour < 18 -> "下午好"
            else      -> "晚上好"
        }
        val timeText = if (advanceMinutes < 60) "${advanceMinutes}分钟" else "${advanceMinutes / 60}小时"
        val body = "${greeting}！${timeText}后，${courseName}开始，请做好准备工作。"

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("课程提醒")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setAutoCancel(true)
            .build()

        @Suppress("MissingPermission")
        NotificationManagerCompat.from(context)
            .notify(System.currentTimeMillis().toInt(), notification)
    }
}
