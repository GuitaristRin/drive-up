package com.driveup.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object WeekCalculator {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun parseDate(dateStr: String): LocalDate = LocalDate.parse(dateStr, formatter)

    fun formatDate(date: LocalDate): String = date.format(formatter)

    fun currentWeek(semesterStartDate: LocalDate, today: LocalDate = LocalDate.now()): Int {
        val daysBetween = ChronoUnit.DAYS.between(semesterStartDate, today)
        if (daysBetween < 0) return 1
        return (daysBetween / 7 + 1).toInt()
    }

    fun weekStartDate(semesterStartDate: LocalDate, weekNumber: Int): LocalDate =
        semesterStartDate.plusWeeks((weekNumber - 1).toLong())

    fun weekEndDate(semesterStartDate: LocalDate, weekNumber: Int): LocalDate =
        weekStartDate(semesterStartDate, weekNumber).plusDays(6)

    fun todayDayOfWeek(): Int {
        // java.time DayOfWeek: MONDAY=1 … SUNDAY=7
        return LocalDate.now().dayOfWeek.value
    }

    fun formatWeekLabel(weekNumber: Int, startDate: LocalDate): String {
        val end = startDate.plusDays(6)
        val startFmt = "${startDate.monthValue}/${startDate.dayOfMonth}"
        val endFmt = "${end.monthValue}/${end.dayOfMonth}"
        return "第${weekNumber}周（${startDate.year}/$startFmt-$endFmt）"
    }

    // Returns true if today falls in the given week
    fun isCurrentWeek(semesterStartDate: LocalDate, weekNumber: Int): Boolean {
        val current = currentWeek(semesterStartDate)
        return current == weekNumber
    }
}
