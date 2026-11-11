package com.driveup.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.driveup.data.repository.TimetableRepository
import com.driveup.utils.AlarmScheduler
import com.driveup.utils.ReminderPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!ReminderPreference.isEnabled(context)) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = TimetableRepository(context.applicationContext)
                val semester = repository.getSemesterOnce() ?: return@launch
                val courses = repository.getCoursesOnce(semester.id)
                AlarmScheduler.scheduleAll(
                    context.applicationContext, courses, semester,
                    ReminderPreference.getAdvanceMinutes(context)
                )
            } finally {
                pending.finish()
            }
        }
    }
}
