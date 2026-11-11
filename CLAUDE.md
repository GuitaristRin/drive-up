# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Lint
./gradlew lint

# Signed release build (requires JDK 21 — Java 26+ breaks AGP's jlink)
JAVA_HOME=/tmp/jdk-21.0.11+10 ./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.driveup.ExampleTest"

# Clean build
./gradlew clean assembleDebug
```

The `gradle.properties` sets `-Xmx2g` for the JVM — this was necessary to fix D8 OOM errors during dex merging. Do not reduce it.

## Architecture

Single-module Android app (`minSdk 26`, `targetSdk 34`). MVVM with Jetpack Compose, Room, and Kotlin Coroutines/Flow.

```
data/
  local/        — Room DAOs (SemesterDao, CourseDao)
  model/        — Entities: Semester, Course
  parser/       — ExcelParser (HSSF only, reads .xls)
  repository/   — TimetableRepository (single source of truth)
ui/
  import_/      — ImportScreen + ImportViewModel (file picker + XLS import)
  main/         — MainScreen + MainViewModel + TimetableGrid + CourseDetailSheet
  theme/        — AppColors, LocalAppColors, DriveUpTheme, ThemeMode
utils/
  WeekCalculator, TimeTable, ColorUtils, ThemePreference, ReminderPreference
  AlarmScheduler, NotificationHelper
receiver/
  ReminderReceiver (fires notification on alarm)
  BootReceiver    (reschedules alarms after reboot)
```

### Navigation

There is no Jetpack Navigation component. `AppRoot` in `MainActivity.kt` switches between `ImportScreen` and `MainScreen` based on a `hasSemester: Flow<Boolean>` and a `forceImport: Boolean` state variable. Re-import is triggered by setting `forceImport = true`.

### Theming

All colors flow through `LocalAppColors` (a `staticCompositionLocalOf<AppColors>`), **not** `MaterialTheme.colorScheme`. Every composable reads `val colors = LocalAppColors.current`. `DriveUpTheme` in `Theme.kt` provides both the `CompositionLocalProvider` for `LocalAppColors` and the `MaterialTheme` wrapper. The `ThemeMode` enum (SYSTEM/LIGHT/DARK) is persisted via `ThemePreference` (SharedPreferences).

**Metro Design constraint**: zero `RoundedCornerShape` anywhere. All `Button`, `ModalBottomSheet`, `ModalDrawerSheet`, and card shapes must use `RectangleShape`.

### Timetable Grid

`TimetableGrid` uses `BoxWithConstraints` + absolute `Modifier.offset` positioning for course cards — **not** `LazyVerticalGrid`. This is intentional: it enables multi-section course spans (a course spanning sections 1–2 gets a card twice as tall). `courseMap` is `remember(courses)`-keyed. Course cards are wrapped in `key(dayOfWeek * 100 + startSection)` composable keys.

Week transition animation in `MainScreen` uses a single `Animatable<Float>` (`slideOffset`) driven by `LaunchedEffect(state.displayWeek)`. Easing: `CubicBezierEasing(0.1f, 0.9f, 0.2f, 1.0f)` (Metro expo-out, 280 ms). The timetable column sits inside a `Box(Modifier.clipToBounds())` with `Modifier.graphicsLayer { translationX = slideOffset.value * size.width }`.

### Excel Parsing

Only `.xls` (HSSF) is supported. POI is pinned to **3.17** — POI 5.x ships a `module-info.class` that D8 cannot process on Android. Cell format in the real timetable file:

```
CourseName\r\n◇weekInfo(X-Y节)◇location◇teacher◇…
```

Week info examples: `2-14周(双)(3-4节)`, `1-13周(单)(5-6节)`, `1-16周(1-2节)`. `ExcelParser.expandWeeks()` expands compact ranges ("1-13,15-16") into `Set<Int>` respecting odd/even patterns.

### Section Times (fixed)

Sections 1–2: 08:30–10:00 (45 min each). Sections 3–4: 10:30–12:00. Sections 5–6: 13:30–15:00. Sections 7–8: 15:30–17:00. Sections 9–11: 18:30–20:30 (40 min each). Defined in `utils/TimeTable.kt`.

### Week Calculation

`WeekCalculator.currentWeek(semesterStart, today)` = `(DAYS.between(start, today) / 7 + 1).toInt()`. Semester start is always a Monday stored as `"yyyy-MM-dd"` in `Semester.startDate`.

### Alarm Scheduling

`AlarmScheduler` uses `AlarmManager.setAndAllowWhileIdle()` — **not** `setExactAndAllowWhileIdle()`. Using the exact variant requires `SCHEDULE_EXACT_ALARM` (a special permission the user must grant in Settings); calling it without the grant throws `SecurityException`. `setAndAllowWhileIdle` requires no special permission and is sufficient for class reminders where ±9 min accuracy is acceptable.

### Key Dependencies

| Dependency | Version | Note |
|---|---|---|
| Compose BOM | 2024.02.00 | |
| Room | 2.6.1 | KSP processor |
| KSP | 1.9.22-1.0.17 | Must match Kotlin version |
| Apache POI | **3.17** | HSSF only; do not upgrade to 5.x |
| commons-codec | 1.10 | POI 3.17 runtime dep |

`packaging.resources.excludes` in `app/build.gradle.kts` must include `module-info.class` or the POI build will fail.

## Repository & Local Setup

| 项 | 值 |
|---|---|
| 上游仓 | GitHub `GuitaristRin/drive-up`（**private**） |
| 工作副本 | `C:\Users\mc158\Documents\Projects\drive-up`（tianxuan2） |
| 源出处 | 2026-11-11 从 t14p（Arch，`RAIN-THINKPAD`）`~/projects/drive-up` 拷回：仅源码 70 个文件，丢弃 2.27 GB 的 `java_pid*.hprof` 与 `.gradle/`、`build/`、`app/build/` |

远端：`github` → 上游仓；`origin` → gitee `takahashirinta/drive-up`（只有 `first commit` 的占位历史）。默认 `git push github master`。

### 不入库的内容（`.gitignore` 已锁死，勿用 `git add -f` 绕过）

| 路径 | 为什么 |
|---|---|
| `app/keystore.jks`、`*.jks`、`*.keystore` | 发布签名，泄露即可伪造更新 |
| `local.properties` | 本机 SDK 路径（含家目录结构） |
| `202513253075*.xls` | 课表原表：文件名含学号，内容含真实姓名。只留本机做导入样本 |
| `*.hprof` | JVM 堆转储，可能含内存中的个人信息 |
| `.claude/settings.local.json`、`.idea/` | 本机工具状态 |

### 本机暂不构建

- `settings.gradle.kts` 用 `includeBuild("../../projects/Kanesumi")` 组合构建消费 `kanesumi-core` / `-anim` / `-controls` / `-structure`。在 Windows 上该相对路径解析为 `C:\Users\mc158\Documents\Projects\Kanesumi`，本机没有这个仓，Gradle 配置阶段就会失败。
- `local.properties` 的 `sdk.dir=/home/rain/Android/Sdk` 是 t14p 的路径，需改指本机 Android SDK。
- 要在本机构建，先补上 Kanesumi 仓 + 修 `sdk.dir`；`gradle.properties` 的 `-Xmx2g` 是修 D8 OOM 的，别降。
