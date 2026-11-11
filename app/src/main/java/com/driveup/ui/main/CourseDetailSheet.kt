package com.driveup.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driveup.data.model.Course
import com.driveup.ui.theme.LocalAppColors
import com.driveup.utils.ColorUtils
import com.driveup.utils.TimeTable
import io.github.takahashirinta.kanesumi.controls.MetroBottomSheet
import io.github.takahashirinta.kanesumi.controls.MetroDivider
import io.github.takahashirinta.kanesumi.core.theme.LocalMetroTypography
import io.github.takahashirinta.kanesumi.core.theme.MetroIcon
import io.github.takahashirinta.kanesumi.core.theme.MetroText
import io.github.takahashirinta.kanesumi.core.theme.MetroTypography

private val DAY_NAMES = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

@Composable
fun CourseDetailSheet(courses: List<Course>, onDismiss: () -> Unit) {
    val colors = LocalAppColors.current
    val typography = LocalMetroTypography.current

    MetroBottomSheet(
        onDismiss = onDismiss,
        applyNavigationBarsPadding = true,
        dragHandle = {
            // 直角 grabber —— 取代 M3 ModalBottomSheet 的圆角丸 handle。
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .background(colors.gridLine)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            courses.forEachIndexed { index, course ->
                if (index > 0) MetroDivider(color = colors.gridLine)
                CourseDetailCard(course, typography)
            }
        }
    }
}

@Composable
private fun CourseDetailCard(
    course: Course,
    typography: MetroTypography,
) {
    val colors = LocalAppColors.current
    val color = ColorUtils.forIndex(course.colorIndex)
    val dayName = DAY_NAMES.getOrNull(course.dayOfWeek - 1) ?: ""
    val timeRange = TimeTable.timeRangeFor(course.startSection, course.endSection)
    val sectionLabel = "${course.startSection}-${course.endSection}节"

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(24.dp).background(color))
            Spacer(Modifier.width(10.dp))
            MetroText(
                text = course.name,
                style = typography.titleMedium.copy(fontSize = 18.sp),
                color = colors.textPrimary,
            )
        }
        if (course.location.isNotBlank()) InfoRow(Icons.Default.LocationOn, course.location)
        if (course.teacher.isNotBlank())  InfoRow(Icons.Default.Person, course.teacher)
        InfoRow(Icons.Default.AccessTime, "$dayName  $timeRange  $sectionLabel")

        val patternText = when (course.weekPattern) { 1 -> "单周"; 2 -> "双周"; else -> null }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (patternText != null) WeekBadge(patternText, colors.todayAccent.copy(alpha = 0.15f), colors.todayAccent)
            WeekBadge("${course.weeks.replace("-", "–")}周", colors.gridLine, colors.textSecondary)
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String) {
    val colors = LocalAppColors.current
    val typography = LocalMetroTypography.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetroIcon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textSecondary,
            sizeDp = 16.dp,
        )
        MetroText(text = text, style = typography.bodyMedium, color = colors.textPrimary)
    }
}

@Composable
private fun WeekBadge(label: String, bg: Color, fg: Color) {
    val typography = LocalMetroTypography.current
    Box(modifier = Modifier.background(bg).padding(horizontal = 8.dp, vertical = 3.dp)) {
        MetroText(text = label, style = typography.label, color = fg)
    }
}
