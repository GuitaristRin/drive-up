## 文档信息

| 项目 | 内容 |
|------|------|
| 软件名称 | Drive Up! |
| 设计风格 | Metro Design（现代化磁贴风格） |
| 目标平台 | Android（原生，Kotlin） |
| 架构模式 | MVVM |
| 最低支持版本 | Android 8.0（API 26） |
| 目标版本 | Android 14（API 34） |

---

## 一、整体架构设计

### 1.1 技术选型

| 层级 | 技术方案 |
|------|----------|
| UI 框架 | Jetpack Compose（声明式UI，符合Metro设计理念） |
| 本地存储 | Room Database |
| 文件解析 | Apache POI for Android（或 kotlinx.serialization + 自定义解析） |
| 通知调度 | WorkManager + AlarmManager |
| 图片/图标加载 | Coil |
| 依赖注入 | Dagger Hilt（可选，便于测试） |
| 日期处理 | java.time（ThreeTenABP兼容旧版本） |

### 1.2 模块分层

```
app/
├── ui/                    # 界面层
│   ├── main/              # 主课表页面
│   ├── menu/              # 汉堡菜单（侧边栏）
│   ├── settings/          # 设置页面
│   ├── import/            # 导入页面
│   └── components/        # 可复用组件（色块、课程卡片）
├── data/                  # 数据层
│   ├── local/             # Room数据库相关
│   ├── model/             # 数据实体
│   ├── repository/        # 数据仓库
│   └── parser/            # Excel解析器
├── notification/          # 通知模块
│   ├── scheduler/         # 提醒调度器
│   └── receiver/          # 广播接收器
├── utils/                 # 工具类
│   ├── DateUtils.kt
│   ├── ColorUtils.kt
│   └── WeekCalculator.kt
└── di/                    # 依赖注入模块
```

---

## 二、数据模型设计

### 2.1 核心实体

```kotlin
@Entity(tableName = "semesters")
data class Semester(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,              // "2025-2026年第2学期"
    val startDate: String,         // "2026-03-02"（周一）
    val totalWeeks: Int = 20       // 总周数
)

@Entity(
    tableName = "courses",
    indices = [Index(value = ["semesterId"])]
)
data class Course(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val semesterId: Long,
    val name: String,               // "微观经济学"
    val location: String,           // "历城1号419"
    val teacher: String? = null,    // "刘美芬"
    val dayOfWeek: Int,             // 1=周一, 7=周日
    val startSection: Int,          // 1~11
    val endSection: Int,            // 1~11，连堂时 > startSection
    val weeks: String,              // 存储为 "1,2,3,5-10,12" 字符串
    val weekPattern: Int,           // 0=全部, 1=单周, 2=双周
    val classGroup: String? = null, // 教学班信息（备用）
    val colorIndex: Int = 0         // 预计算的颜色索引
)

@Entity(tableName = "user_settings")
data class UserSetting(
    @PrimaryKey
    val key: String,
    val value: String
)
```

### 2.2 视图模型（UI专用）

```kotlin
data class DisplayCourse(
    val courseId: Long,
    val name: String,
    val location: String,
    val isConflict: Boolean = false,     // 是否冲突（多门课）
    val conflictCount: Int = 0
)

data class WeekInfo(
    val weekNumber: Int,
    val startDate: String,               // 周一日期
    val endDate: String                  // 周日日期
)

data class CellData(
    val section: Int,                    // 节次1-11
    val dayOfWeek: Int,                  // 1-7
    val courses: List<DisplayCourse>
)
```

### 2.3 固定时间配置（待您提供后填入）

```kotlin
object TimeTable {
    // 示例格式，待替换为实际数据
    val sections: List<Section> = listOf(
        Section(1, "8:00", "8:45"),
        Section(2, "8:55", "9:40"),
        Section(3, "10:00", "10:45"),
        Section(4, "10:55", "11:40"),
        Section(5, "13:30", "14:15"),
        Section(6, "14:25", "15:10"),
        Section(7, "15:30", "16:15"),
        Section(8, "16:25", "17:10"),
        Section(9, "18:30", "19:15"),
        Section(10, "19:25", "20:10"),
        Section(11, "20:20", "21:05")
    )
}

data class Section(
    val number: Int,
    val startTime: String,
    val endTime: String
)
```

---

## 三、UI设计（Metro Design）

### 3.1 配色方案

| 用途 | 色值 | 说明 |
|------|------|------|
| 主背景 | `#F3F3F3` | 浅灰，不干扰色块 |
| 卡片背景 | `#FFFFFF` | 白色卡片 |
| 今天标识 | `#0078D7` | 表头高亮（Windows 蓝） |
| 课程文字 | `#1A1A1A` | 深灰，保证对比度 |
| 地点文字 | `#6B6B6B` | 中灰色 |
| 网格线 | `#E0E0E0` | 极细分割线 |
| 周次切换栏 | `#2D2D2D` | 深色背景 |

### 3.2 课程颜色生成规则

```kotlin
fun getColorForCourse(courseName: String): Color {
    val hue = (courseName.hashCode() and 0x7FFFFFFF) % 360
    // 保持饱和度和亮度在Metro舒适区
    return Color.hsv(hue.toFloat(), 0.3f, 0.88f)
}
```

### 3.3 主课表布局规格

**布局参数（竖屏固定）**：

| 元素 | 高度/宽度 | 说明 |
|------|-----------|------|
| 状态栏占位 | 24dp | 沉浸式 |
| 标题栏 | 56dp | 学期名称 + 学生姓名 |
| 周次切换栏 | 48dp | 左右箭头 + 周次显示 + 本周按钮 |
| 表头行 | 40dp | 周一 ~ 周日 |
| 每行（节次） | 72dp | 共11行 |
| 网格宽度 | 等分7列 | 权重分配 |

**总高度计算**：24 + 56 + 48 + 40 + (72 × 11) = **960dp**

注：主流手机屏幕高度 1920px ~ 2400px，按 2.5x 屏幕密度计算 960dp 约 2400px，可完整展示无需滚动。极端小屏（如 1280×720）下允许轻微垂直滚动，但水平方向固定不滚动。

### 3.4 格子内文字样式

| 元素 | 字体大小 | 最大行数 | 截断方式 |
|------|----------|----------|----------|
| 课程名 | 14sp，Medium | 1行 | 超出显示… |
| 地点 | 11sp，Regular | 1行 | 超出显示… |
| 冲突标记 | 10sp，置右下 | - | 显示 "+2" |

### 3.5 汉堡菜单（侧边栏）结构

```
┌─────────────────────────────┐
│  👤 金雨阳                   │
│     jinyuyang@example.com   │
├─────────────────────────────┤
│  📅 我的课表         ★ 当前  │
│  📊 学期统计                │
│  🔔 通知设置                │
│  🎨 外观主题                │
│  📤 导出课表                │
│  ❓ 帮助与反馈              │
│  ℹ️ 关于                    │
└─────────────────────────────┘
```

- 点击左上角菜单图标（☰）打开/关闭
- 当前页面菜单项高亮（Metro 左侧色条）

---

## 四、功能模块详细设计

### 4.1 导入课表功能

**用户流程**：
1. 首次启动 → 自动跳转导入页面
2. 点击“选择文件” → 调用系统文件选择器（仅显示 .xls/.xlsx）
3. 解析进度条 → 解析完成后预览前3条课程
4. 用户确认 → 保存到数据库

**解析规则**（针对您的文件格式）：

| 文件位置 | 解析目标 |
|----------|----------|
| A1~H2 | 学期名称、学生姓名、学号（仅展示，不存储） |
| 第3行表头 | 星期几（从C3列开始） |
| 第4行及以后 | 上午1-2节、3-4节，下午5-6节、7-8节，晚上9-11节 |

**单元格文本解析正则**：
```kotlin
// 示例：◇1-13周,15-16周(1-2节)◇历城1号419◇刘美芬
val regex = Regex("◇(\\d{1,2}(?:-\\d{1,2})?(?:周)?(?:,\\d{1,2}(?:-\\d{1,2})?周?)*(?:\\([单双]\\）)?)?(.*?)◇(.*?)◇(.*?)(?:◇|$)")
```

### 4.2 周次切换逻辑

**状态管理**：
```kotlin
data class WeekState(
    val currentWeek: Int,      // 当前显示的第几周
    val isCurrentWeek: Boolean // 是否为本学期真实当前周
)

// 学期起始周一: 2026-03-02 为第1周
// 第14周: 2026-06-08 ~ 2026-06-14
// 系统日期 2026-06-10 属于第14周
```

**底部控件交互**：
- `<` 按钮 → currentWeek - 1（不小于1）
- `>` 按钮 → currentWeek + 1（不大于总周数）
- `本周` 按钮 → 跳回系统日期所在周
- 周次文本显示格式：`第14周（2026/6/8-6/14）`

### 4.3 课程通知系统

**通知数据结构**：
```kotlin
data class CourseReminder(
    val courseId: Long,
    val courseName: String,
    val location: String,
    val startTime: LocalDateTime,  // 具体上课时间
    val reminderMinutes: Int = 10   // 提前分钟数
)
```

**调度策略**：

| 时机 | 行为 |
|------|------|
| 应用启动 | 删除过期通知，重新注册未来7天的课程通知 |
| 系统日期变更（跨天） | WorkManager 每日凌晨扫描当天及次日课程 |
| 用户修改通知设置 | 重新调度所有通知 |
| 课表导入/修改 | 通知系统重置 |

**通知内容**：
```
标题：[课程名] 即将开始
内容：📖 地点：[位置]  |  ⏰ [开始时间]-[结束时间]
点击：打开应用并跳转到对应周次/当天/该课程
```

**通知渠道设置**：
- 渠道ID: `course_reminder`
- 渠道名称: 课程提醒
- 重要性: `IMPORTANCE_HIGH`（可弹出横幅）
- 震动: 默认开启，用户可关闭
- 声音: 默认通知音

### 4.4 设置页面

**可配置项**：

| 设置项 | 默认值 | 说明 |
|--------|--------|------|
| 上课提醒总开关 | 开 | 全局控制 |
| 提前提醒时间 | 10分钟 | 5/10/15/30可选 |
| 震动提醒 | 开 | 通知时震动 |
| 声音提醒 | 开 | 通知时播放声音 |
| 免打扰模式 | 关 | 开启后22:00-7:00静默 |
| 暗色模式 | 跟随系统 | 跟随/浅色/深色 |
| 课程颜色模式 | 按课程名 | 按课程名/随机/统一 |
| 周次显示格式 | 第X周 | 第X周/Week X |

---

## 五、核心交互流程图

### 5.1 应用启动流程

```
启动 → 检查是否已导入课表
    ├─ 否 → 引导导入页面
    └─ 是 → 读取学期信息 → 计算当前周 → 展示主课表
         └─ 后台：注册本周及下周课程通知
```

### 5.2 课程点击流程

```
点击格子 → 获取该格子所有课程
    ├─ 单课程 → 弹出详情BottomSheet
    └─ 多课程 → 弹出列表（含节次区分）
         └─ 点击某课程 → 详情BottomSheet
```

### 5.3 通知触发流程

```
WorkManager触发 → 检查当前时间与课程时间差值
    ├─ 差值 = 预设提前时间 → 发出通知
    ├─ 差值 < 预设时间（已过）→ 跳过
    └─ 差值 > 预设时间 → 重新调度
```

---

## 六、性能设计要点

| 场景 | 优化方案 |
|------|----------|
| 课表网格渲染 | 使用 `LazyVerticalGrid` 按需组合，固定 `cellsCount = 7*11 = 77` |
| 颜色计算 | 预计算并缓存课程色值，不重复调用 `hashCode()` |
| 日期计算 | 使用预计算的周起始日期映射表，避免反复调用日历API |
| 通知调度 | 批量注册，使用 `setExact()` 精确触发（Android 12+ 需申请 `SCHEDULE_EXACT_ALARM` 权限） |
| 文件解析 | 流式读取，大文件分块解析，不一次性加载到内存 |

---

## 七、错误处理与边界情况

| 异常情况 | 处理方式 |
|----------|----------|
| 文件格式不支持 | 提示“仅支持 .xls 或 .xlsx 文件” |
| 解析失败 | 显示失败行号，建议用户检查格式 |
| 学期未配置 | 导入时要求用户确认学期开始日期 |
| 课程时间跨度过长（>3节） | 警告但继续导入，视图中合并展示 |
| 同一格超过3门冲突课程 | 显示主课程 + “+N”，列表展示全部 |
| 通知权限被拒绝 | 引导用户到系统设置开启 |
| 存储权限拒绝 | 使用 SAF（Storage Access Framework）无需存储权限 |

---

## 八、版本计划

| 版本 | 功能范围 |
|------|----------|
| v1.0（MVP） | 导入Excel、展示课表、周次切换、课程详情弹窗 |
| v1.1 | 通知提醒、基础设置页面 |
| v1.2 | 汉堡菜单、学期统计（周数统计/课程统计） |
| v1.3 | 暗色模式、导出图片、桌面小部件 |
| v2.0 | 多课表管理、手动调课、云备份 |

---

## 九、待确认项（需您补充）

1. **上课节次的具体时间**：您提到会稍后提供，收到后填入 `TimeTable` 对象。
2. **学生姓名与学号**：是否需要在侧边栏顶部展示？需要的话，需从Excel中解析。(不显示这些信息)
3. **实践课程和其他课程**（如“新生研讨课”“黄河文化（智慧树）”等）是否需要在主课表中展示当前周有实践课程的横幅提示？（不需要）
4. **学期总周数**：一般设置为20周还是根据实际校历配置？是否需要支持手动设置？（不需要，自动计算一直持续到课程结束）

---

文档版本：v1.0  
最后更新：2026-06-10  
