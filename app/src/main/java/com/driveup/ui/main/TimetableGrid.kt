package com.driveup.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driveup.data.model.Course
import com.driveup.ui.theme.LocalAppColors
import com.driveup.utils.ColorUtils
import com.driveup.utils.TimeTable
import io.github.takahashirinta.kanesumi.core.theme.LocalMetroTypography
import io.github.takahashirinta.kanesumi.core.theme.MetroText
import io.github.takahashirinta.kanesumi.core.theme.MetroTypography

private val DAY_LABELS    = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
private val SECTION_HEIGHT = 72.dp
private val TIME_COL_WIDTH = 36.dp
private val HEADER_HEIGHT  = 40.dp
private val CELL_PADDING   = 1.5.dp

@Composable
fun TimetableGrid(
    courses: List<Course>,
    todayDayOfWeek: Int,
    onCourseClick: (List<Course>) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalMetroTypography.current
    val courseMap = remember(courses) {
        courses.groupBy { it.dayOfWeek to it.startSection }
    }

    Column(modifier = modifier) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(HEADER_HEIGHT)
        ) {
            Spacer(Modifier.width(TIME_COL_WIDTH))
            for (day in 1..7) {
                val isToday = day == todayDayOfWeek
                key(day) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (isToday) colors.todayAccent else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        MetroText(
                            text = DAY_LABELS[day - 1],
                            color = if (isToday) Color.White else colors.textPrimary,
                            style = typography.bodySmall.copy(
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            ),
                        )
                    }
                }
            }
        }

        // Grid body
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(SECTION_HEIGHT * 11)
        ) {
            val dayWidth = (maxWidth - TIME_COL_WIDTH) / 7

            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridColor = colors.gridLine
                val stroke = 0.5.dp.toPx()
                for (s in 0..11) {
                    val y = (SECTION_HEIGHT * s).toPx()
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), stroke)
                }
                for (d in 0..7) {
                    val x = TIME_COL_WIDTH.toPx() + dayWidth.toPx() * d
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), stroke)
                }
            }

            for (s in 1..11) {
                key(s) {
                    SectionLabel(
                        section = s,
                        typography = typography,
                        modifier = Modifier
                            .offset(x = 0.dp, y = SECTION_HEIGHT * (s - 1))
                            .width(TIME_COL_WIDTH)
                            .height(SECTION_HEIGHT)
                    )
                }
            }

            courseMap.forEach { (k, cellCourses) ->
                val (dayOfWeek, startSection) = k
                val span = cellCourses.first().endSection - cellCourses.first().startSection + 1
                key(dayOfWeek * 100 + startSection) {
                    CourseCard(
                        courses = cellCourses,
                        typography = typography,
                        onClick = { onCourseClick(cellCourses) },
                        modifier = Modifier
                            .offset(
                                x = TIME_COL_WIDTH + dayWidth * (dayOfWeek - 1) + CELL_PADDING,
                                y = SECTION_HEIGHT * (startSection - 1) + CELL_PADDING
                            )
                            .width(dayWidth - CELL_PADDING * 2)
                            .height(SECTION_HEIGHT * span - CELL_PADDING * 2)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(
    section: Int,
    typography: MetroTypography,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val time = TimeTable.getSection(section)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MetroText(
            text = section.toString(),
            style = typography.label.copy(fontWeight = FontWeight.Medium),
            color = colors.textSecondary,
        )
        if (time != null) {
            MetroText(
                text = time.startTime,
                style = typography.label.copy(fontSize = 8.sp),
                color = colors.textSecondary.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun CourseCard(
    courses: List<Course>,
    typography: MetroTypography,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = courses.first()
    val bgColor = ColorUtils.forIndex(primary.colorIndex)
    val textColor = ColorUtils.textColorFor(bgColor)
    val conflictCount = courses.size - 1

    Box(
        modifier = modifier
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 3.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MetroText(
                text = primary.name,
                style = typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 14.sp,
                ),
                color = textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            MetroText(
                text = primary.location,
                style = typography.label.copy(fontSize = 10.sp),
                color = textColor.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (conflictCount > 0) {
            MetroText(
                text = "+$conflictCount",
                style = typography.label.copy(fontSize = 9.sp),
                color = textColor.copy(alpha = 0.8f),
                modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp),
            )
        }
    }
}
