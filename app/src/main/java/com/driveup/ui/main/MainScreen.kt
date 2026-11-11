package com.driveup.ui.main

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.driveup.data.model.Course
import com.driveup.data.repository.TimetableRepository
import com.driveup.ui.components.MetroDrawer
import com.driveup.ui.theme.LocalAppColors
import com.driveup.ui.theme.ThemeMode
import com.driveup.utils.AlarmScheduler
import com.driveup.utils.ReminderPreference
import com.driveup.utils.WeekCalculator
import io.github.takahashirinta.kanesumi.anim.sokuou.MetroCubic
import io.github.takahashirinta.kanesumi.anim.sokuou.rememberMetroFlingBehavior
import io.github.takahashirinta.kanesumi.controls.MetroButton
import io.github.takahashirinta.kanesumi.controls.MetroDivider
import io.github.takahashirinta.kanesumi.controls.MetroIconButton
import io.github.takahashirinta.kanesumi.controls.MetroListRow
import io.github.takahashirinta.kanesumi.controls.MetroSwitch
import io.github.takahashirinta.kanesumi.core.insets.metroNavigationBarsPadding
import io.github.takahashirinta.kanesumi.core.insets.metroStatusBarsPadding
import io.github.takahashirinta.kanesumi.core.theme.LocalMetroTypography
import io.github.takahashirinta.kanesumi.core.theme.MetroIcon
import io.github.takahashirinta.kanesumi.core.theme.MetroText
import kotlinx.coroutines.launch
import java.time.LocalDate

// 周切换滑动。原 MetroEasing CubicBezier(0.1, 0.9, 0.2, 1.0) 数学上近似 UWP Cubic
// EaseOut(t=0.5 时 0.84 vs 0.875),按 Sokuou 词汇表直接用 MetroCubic,280ms 不变。
private val WeekSlideSpec = tween<Float>(durationMillis = 280, easing = MetroCubic)

@Composable
fun MainScreen(
    repository: TimetableRepository,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onRequestReimport: () -> Unit = {}
) {
    val vm: MainViewModel = viewModel(
        factory = remember(repository) {
            object : androidx.lifecycle.ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return MainViewModel(repository) as T
                }
            }
        }
    )
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val typography = LocalMetroTypography.current
    val scope = rememberCoroutineScope()

    var drawerOpen by remember { mutableStateOf(false) }
    var selectedCourses by remember { mutableStateOf<List<Course>?>(null) }
    var reminderEnabled by remember { mutableStateOf(ReminderPreference.isEnabled(context)) }
    var advanceMinutes by remember { mutableStateOf(ReminderPreference.getAdvanceMinutes(context)) }

    // 抽屉打开时系统返回键先关抽屉,不直接退出 app。
    BackHandler(enabled = drawerOpen) { drawerOpen = false }

    // Week slide animation — progress-based, interruptible. 视觉只在 graphicsLayer 读取,零重组。
    val slideOffset = remember { Animatable(0f) }
    val prevWeek = remember { mutableIntStateOf(0) }

    LaunchedEffect(state.displayWeek) {
        if (prevWeek.intValue == 0) {
            prevWeek.intValue = state.displayWeek
            return@LaunchedEffect
        }
        val direction = if (state.displayWeek > prevWeek.intValue) 1 else -1
        prevWeek.intValue = state.displayWeek
        slideOffset.snapTo(direction.toFloat())
        slideOffset.animateTo(0f, WeekSlideSpec)
    }

    fun applyReminderSettings(enabled: Boolean, minutes: Int) {
        scope.launch {
            val semester = state.semester ?: return@launch
            val courses = repository.getCoursesOnce(semester.id)
            if (enabled) AlarmScheduler.scheduleAll(context, courses, semester, minutes)
            else AlarmScheduler.cancelAll(context, courses)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .metroStatusBarsPadding()
        ) {
            // Title bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(colors.surface),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetroIconButton(onClick = { drawerOpen = true }) {
                    MetroIcon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "菜单",
                        tint = colors.textPrimary,
                    )
                }
                MetroText(
                    text = state.semester?.name ?: "",
                    color = colors.textPrimary,
                    style = typography.titleMedium.copy(textAlign = TextAlign.Center),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(48.dp))
            }

            // Week navigation bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(colors.weekBarBg)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetroIconButton(
                    onClick = vm::previousWeek,
                    enabled = state.displayWeek > 1,
                ) {
                    MetroIcon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "上一周",
                        tint = if (state.displayWeek > 1) colors.weekBarText
                               else colors.weekBarText.copy(alpha = 0.3f),
                    )
                }
                MetroText(
                    text = state.weekLabel,
                    color = colors.weekBarText,
                    style = typography.caption.copy(textAlign = TextAlign.Center),
                    modifier = Modifier.weight(1f),
                )
                if (!state.isCurrentWeek) {
                    MetroButton(
                        text = "本周",
                        onClick = vm::jumpToCurrentWeek,
                        containerColor = Color.Transparent,
                        contentColor = colors.todayAccent,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        textStyle = typography.caption,
                    )
                }
                MetroIconButton(
                    onClick = vm::nextWeek,
                    enabled = state.displayWeek < state.totalWeeks,
                ) {
                    MetroIcon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "下一周",
                        tint = if (state.displayWeek < state.totalWeeks) colors.weekBarText
                               else colors.weekBarText.copy(alpha = 0.3f),
                    )
                }
            }

            // Timetable with Metro slide transition
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(
                            rememberScrollState(),
                            flingBehavior = rememberMetroFlingBehavior(),
                        )
                        .graphicsLayer { translationX = slideOffset.value * size.width }
                ) {
                    TimetableGrid(
                        courses        = state.coursesThisWeek,
                        todayDayOfWeek = state.todayDayOfWeek,
                        onCourseClick  = { courses -> selectedCourses = courses },
                        modifier       = Modifier.fillMaxWidth()
                    )
                    // 底部留白避开系统导航栏,最后一行课可滚到导航栏上方。
                    Spacer(Modifier.metroNavigationBarsPadding())
                }
            }
        }

        MetroDrawer(
            drawerOpen = drawerOpen,
            onDismiss = { drawerOpen = false },
            drawerColor = colors.surface,
        ) {
            DrawerContent(
                semesterName     = state.semester?.name ?: "",
                startDate        = state.semester?.startDate ?: "",
                themeMode        = themeMode,
                reminderEnabled  = reminderEnabled,
                advanceMinutes   = advanceMinutes,
                onThemeChange    = onThemeChange,
                onStartDateChange = { newDate ->
                    vm.updateStartDate(newDate)
                    drawerOpen = false
                },
                onReimport = {
                    drawerOpen = false
                    onRequestReimport()
                },
                onReminderToggle = { enabled ->
                    reminderEnabled = enabled
                    ReminderPreference.setEnabled(context, enabled)
                    applyReminderSettings(enabled, advanceMinutes)
                },
                onAdvanceChange = { minutes ->
                    advanceMinutes = minutes
                    ReminderPreference.setAdvanceMinutes(context, minutes)
                    if (reminderEnabled) applyReminderSettings(true, minutes)
                },
            )
        }
    }

    selectedCourses?.let { courses ->
        CourseDetailSheet(courses = courses, onDismiss = { selectedCourses = null })
    }
}

@Composable
private fun DrawerContent(
    semesterName: String,
    startDate: String,
    themeMode: ThemeMode,
    reminderEnabled: Boolean,
    advanceMinutes: Int,
    onThemeChange: (ThemeMode) -> Unit,
    onStartDateChange: (String) -> Unit,
    onReimport: () -> Unit,
    onReminderToggle: (Boolean) -> Unit,
    onAdvanceChange: (Int) -> Unit,
) {
    val colors = LocalAppColors.current
    val typography = LocalMetroTypography.current
    val context = LocalContext.current

    // Header
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.weekBarBg)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Column {
            MetroText(
                text = "Drive Up!",
                style = typography.title.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
            if (semesterName.isNotBlank()) {
                MetroText(
                    text = semesterName,
                    style = typography.caption,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    // 我的课表
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.todayAccent.copy(alpha = 0.12f))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        MetroText(
            text = "我的课表",
            style = typography.body.copy(fontWeight = FontWeight.Medium),
            color = colors.todayAccent,
        )
        MetroText(
            text = "● 当前",
            style = typography.label,
            color = colors.todayAccent.copy(alpha = 0.7f),
        )
    }

    MetroDivider(color = colors.gridLine, modifier = Modifier.padding(vertical = 8.dp))

    // 修改开学日期
    MetroListRow(
        title = "修改开学日期",
        subtitle = startDate.ifBlank { null },
        leading = {
            MetroIcon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                tint = colors.textSecondary,
                sizeDp = 20.dp,
            )
        },
        onClick = {
            val parsed = if (startDate.isNotBlank()) WeekCalculator.parseDate(startDate)
                         else LocalDate.now()
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    onStartDateChange(WeekCalculator.formatDate(LocalDate.of(year, month + 1, day)))
                },
                parsed.year, parsed.monthValue - 1, parsed.dayOfMonth
            ).show()
        },
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    )

    // 重新导入课表
    MetroListRow(
        title = "重新导入课表",
        leading = {
            MetroIcon(
                imageVector = Icons.Default.FileUpload,
                contentDescription = null,
                tint = colors.textSecondary,
                sizeDp = 20.dp,
            )
        },
        onClick = onReimport,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    )

    MetroDivider(color = colors.gridLine, modifier = Modifier.padding(vertical = 8.dp))

    // 外观主题
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        MetroText(
            text = "外观主题",
            style = typography.bodySmall,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                val label = when (mode) {
                    ThemeMode.SYSTEM -> "跟随系统"
                    ThemeMode.LIGHT  -> "浅色"
                    ThemeMode.DARK   -> "深色"
                }
                val selected = mode == themeMode
                Box(
                    modifier = Modifier
                        .background(if (selected) colors.todayAccent else colors.surfaceVariant)
                        .clickable { onThemeChange(mode) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MetroText(
                        text = label,
                        style = typography.bodySmall.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        ),
                        color = if (selected) Color.White else colors.textPrimary,
                    )
                }
            }
        }
    }

    MetroDivider(color = colors.gridLine, modifier = Modifier.padding(vertical = 8.dp))

    // 课前提醒
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            MetroIcon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = if (reminderEnabled) colors.todayAccent else colors.textSecondary,
                sizeDp = 20.dp,
            )
            Spacer(Modifier.width(8.dp))
            MetroText(
                text = "课前提醒",
                style = typography.body,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            MetroSwitch(
                checked = reminderEnabled,
                onCheckedChange = onReminderToggle,
                // MetroSwitch 本身不挂 toggle semantics,这里补上 role,让 TalkBack
                // 能把它读成开关(Kanesumi 库层语义补齐前的 app 侧兜底)。
                modifier = Modifier.semantics {
                    role = Role.Switch
                    contentDescription = "课前提醒"
                },
            )
        }

        if (reminderEnabled) {
            Spacer(Modifier.height(10.dp))
            MetroText(
                text = "提前时间",
                style = typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 15, 30).forEach { min ->
                    val selected = min == advanceMinutes
                    Box(
                        modifier = Modifier
                            .background(if (selected) colors.todayAccent else colors.surfaceVariant)
                            .clickable { onAdvanceChange(min) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        MetroText(
                            text = "${min}分钟",
                            style = typography.bodySmall.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            ),
                            color = if (selected) Color.White else colors.textPrimary,
                        )
                    }
                }
            }
        }
    }
}
