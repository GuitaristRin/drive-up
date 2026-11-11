package com.driveup.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.driveup.utils.NotificationHelper

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val courseName = intent.getStringExtra("course_name") ?: return
        val advanceMinutes = intent.getIntExtra("advance_minutes", 10)
        NotificationHelper.showReminder(context, courseName, advanceMinutes)
    }
}
