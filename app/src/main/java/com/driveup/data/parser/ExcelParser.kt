package com.driveup.data.parser

import com.driveup.data.model.Course
import com.driveup.utils.ColorUtils
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Cell
import java.io.InputStream

data class ParsedCourseRaw(
    val name: String,
    val location: String,
    val teacher: String,
    val dayOfWeek: Int,
    val startSection: Int,
    val endSection: Int,
    val weeks: String,         // compact: "1-13,15-16"
    val weekPattern: Int,      // 0=mixed/all, 1=odd, 2=even
    val maxWeek: Int
) {
    fun toCourse(semesterId: Long) = Course(
        semesterId = semesterId,
        name = name,
        location = location,
        teacher = teacher,
        dayOfWeek = dayOfWeek,
        startSection = startSection,
        endSection = endSection,
        weeks = weeks,
        weekPattern = weekPattern,
        colorIndex = ColorUtils.indexForName(name)
    )
}

data class ParseResult(
    val semesterName: String,
    val courses: List<ParsedCourseRaw>
)

class ExcelParser {

    fun parse(input: InputStream): ParseResult {
        val wb = HSSFWorkbook(input)
        val sheet = wb.getSheetAt(0)

        val semesterName = sheet.getRow(0)?.getCell(0)?.stringCellValue?.trim() ?: "未知学期"

        val courses = mutableListOf<ParsedCourseRaw>()

        // Rows 2–12 (0-indexed) contain timetable data; cols 2–8 = Mon–Sun
        for (rowIdx in 2..12) {
            val row = sheet.getRow(rowIdx) ?: continue
            for (colIdx in 2..8) {
                val cell = row.getCell(colIdx) ?: continue
                val text = cell.safeStringValue().trim()
                if (text.isBlank()) continue
                val dayOfWeek = colIdx - 1  // col2=1(Mon)…col8=7(Sun)
                courses.addAll(parseCell(text, dayOfWeek))
            }
        }

        wb.close()
        return ParseResult(semesterName, courses)
    }

    private fun parseCell(cellText: String, dayOfWeek: Int): List<ParsedCourseRaw> {
        val results = mutableListOf<ParsedCourseRaw>()
        val lines = cellText.split("\r\n", "\n").map { it.trim() }.filter { it.isNotEmpty() }

        var i = 0
        while (i < lines.size) {
            val nameLine = lines[i]
            if (nameLine.startsWith("◇")) { i++; continue }  // skip orphan data lines
            i++
            if (i >= lines.size || !lines[i].startsWith("◇")) continue

            val dataLine = lines[i]
            i++

            val courseName = nameLine.trimStart()
            val parts = dataLine.split("◇").filter { it.isNotEmpty() }
            if (parts.isEmpty()) continue

            val weekSectionStr = parts[0]
            val location = parts.getOrNull(1)?.trim() ?: ""
            val teacher = parts.getOrNull(2)?.trim() ?: ""

            val parsed = parseWeekSection(weekSectionStr) ?: continue
            val maxWeek = expandWeeks(parsed.weeks, parsed.weekPattern).maxOrNull() ?: continue

            results.add(
                ParsedCourseRaw(
                    name = courseName,
                    location = location,
                    teacher = teacher,
                    dayOfWeek = dayOfWeek,
                    startSection = parsed.startSection,
                    endSection = parsed.endSection,
                    weeks = parsed.weeks,
                    weekPattern = parsed.weekPattern,
                    maxWeek = maxWeek
                )
            )
        }
        return results
    }

    private data class WeekSectionInfo(
        val weeks: String,
        val weekPattern: Int,
        val startSection: Int,
        val endSection: Int
    )

    private fun parseWeekSection(str: String): WeekSectionInfo? {
        // Extract trailing "(X-Y节)" or "(X节)"
        val sectionRegex = Regex("""\((\d+)(?:-(\d+))?节\)\s*$""")
        val sectionMatch = sectionRegex.find(str) ?: return null
        val startSection = sectionMatch.groupValues[1].toIntOrNull() ?: return null
        val endSection = sectionMatch.groupValues[2].toIntOrNull() ?: startSection
        val withoutSection = str.substring(0, sectionMatch.range.first).trim()

        // Determine week pattern and clean weeks string
        val weekPattern: Int
        val cleanWeeks: String
        when {
            withoutSection.contains("(单)") -> {
                weekPattern = 1
                cleanWeeks = withoutSection.replace("(单)", "").replace("周", "").trim().trimEnd(',')
            }
            withoutSection.contains("(双)") -> {
                // Check if there are additional weeks after the (双) segment that break the pattern
                val hasExtra = withoutSection.contains("(双),") ||
                    withoutSection.substringAfter("(双)").contains(Regex("\\d"))
                weekPattern = if (hasExtra) 0 else 2
                cleanWeeks = withoutSection.replace("(双)", "").replace("周", "").trim().trimEnd(',')
            }
            else -> {
                weekPattern = 0
                cleanWeeks = withoutSection.replace("周", "").trim().trimEnd(',')
            }
        }

        return WeekSectionInfo(cleanWeeks, weekPattern, startSection, endSection)
    }

    private fun Cell.safeStringValue(): String = try {
        stringCellValue ?: ""
    } catch (_: Exception) { "" }

    fun expandWeeks(weeks: String, weekPattern: Int): Set<Int> {
        val result = mutableSetOf<Int>()
        if (weeks.isBlank()) return result

        for (segment in weeks.split(",")) {
            val seg = segment.trim()
            if (seg.isBlank()) continue
            val rangeParts = seg.split("-")
            if (rangeParts.size == 2) {
                val start = rangeParts[0].toIntOrNull() ?: continue
                val end = rangeParts[1].toIntOrNull() ?: continue
                for (w in start..end) {
                    when (weekPattern) {
                        1 -> if (w % 2 != 0) result.add(w)
                        2 -> if (w % 2 == 0) result.add(w)
                        else -> result.add(w)
                    }
                }
            } else {
                val w = seg.toIntOrNull() ?: continue
                result.add(w)
            }
        }
        return result
    }
}
