package com.driveup.utils

data class SectionTime(
    val number: Int,
    val startTime: String,   // "08:30"
    val endTime: String      // "09:15"
)

object TimeTable {
    val sections: List<SectionTime> = listOf(
        SectionTime(1, "08:30", "09:15"),
        SectionTime(2, "09:15", "10:00"),
        SectionTime(3, "10:30", "11:15"),
        SectionTime(4, "11:15", "12:00"),
        SectionTime(5, "13:30", "14:15"),
        SectionTime(6, "14:15", "15:00"),
        SectionTime(7, "15:30", "16:15"),
        SectionTime(8, "16:15", "17:00"),
        SectionTime(9, "18:30", "19:10"),
        SectionTime(10, "19:10", "19:50"),
        SectionTime(11, "19:50", "20:30")
    )

    fun getSection(number: Int): SectionTime? = sections.getOrNull(number - 1)

    fun timeRangeFor(startSection: Int, endSection: Int): String {
        val start = getSection(startSection)?.startTime ?: return ""
        val end = getSection(endSection)?.endTime ?: return ""
        return "$start-$end"
    }
}
