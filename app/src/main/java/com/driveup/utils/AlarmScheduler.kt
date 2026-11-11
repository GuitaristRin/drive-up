package com.driveup.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.driveup.data.model.Course
import com.driveup.data.model.Semester
import com.driveup.data.parser.ExcelParser
import com.driveup.receiver.ReminderReceiver

object AlarmScheduler {
    private const val ACTION = "com.driveup.COURSE_REMINDER"

    fun scheduleAll(
        context: Context,
        courses: List<Course>,
        semester: Semester,
        advanceMinutes: Int
    ) {
        cancelAll(context, courses)
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val semesterStart = WeekCalculator.parseDate(semester.startDate)
        val now = System.currentTimeMillis()
        val parser = ExcelParser()

        courses.forEach { course ->
            val weeks = parser.expandWeeks(course.weeks, course.weekPattern)
            weeks.forEach { weekNum ->
                val courseDate = semesterStart
                    .plusWeeks((weekNum - 1).toLong())
                    .plusDays((course.dayOfWeek - 1).toLong())
                val section = TimeTable.getSection(course.startSection) ?: return@forEach
                val (h, m) = section.startTime.split(":").map { it.toInt() }
                val triggerMillis = courseDate.atTime(h, m)
                    .minusMinutes(advanceMinutes.toLong())
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
                if (triggerMillis <= now) return@forEach

                val pi = buildPendingIntent(context, course, weekNum, advanceMinutes)
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
            }
        }
    }

    fun cancelAll(context: Context, courses: List<Course>) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val parser = ExcelParser()
        courses.forEach { course ->
            parser.expandWeeks(course.weeks, course.weekPattern).forEach { weekNum ->
                alarmManager.cancel(buildPendingIntent(context, course, weekNum, 0))
            }
        }
    }

    private fun buildPendingIntent(
        context: Context,
        course: Course,
        weekNum: Int,
        advanceMinutes: Int
    ): PendingIntent {
        val requestCode = (course.id.toInt() * 100 + weekNum)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION
            putExtra("course_name", course.name)
            putExtra("advance_minutes", advanceMinutes)
        }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
