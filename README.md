# Drive Up!

Android 课表应用。Kotlin + Jetpack Compose + Room，MVVM，Metro 风格磁贴界面（全直角，无圆角）。
导入 `.xls` 课表原表后展示周课表、周次切换、课程详情与上课提醒。

| 项 | 值 |
|---|---|
| 平台 | Android 8.0+（minSdk 26），targetSdk 34 |
| 模块 | 单模块 `:app` |
| 关键依赖 | Compose BOM 2024.02.00 / Room 2.6.1 / Apache POI 3.17（HSSF，勿升 5.x） |

## 构建

```bash
./gradlew assembleDebug        # 调试包
./gradlew installDebug         # 装到已连接设备
./gradlew assembleRelease      # 需 JDK 21（Java 26+ 会打断 AGP 的 jlink）
```

两点前置，缺一个 Gradle 配置阶段就失败：

1. `settings.gradle.kts` 用 `includeBuild("../../projects/Kanesumi")` 组合构建消费
   `kanesumi-core` / `-anim` / `-controls` / `-structure` —— 需先把这个仓放到同级。
2. `local.properties` 的 `sdk.dir` 指向本机 Android SDK（文件不入库，各机器自备）。

架构、数据模型、界面规格见 [BUILD.md](BUILD.md)；给编码代理的约束见 [CLAUDE.md](CLAUDE.md)。

## 隐私与密钥

以下内容**永不入库**（`.gitignore` 已锁死，别用 `git add -f` 绕过）：

- `app/keystore.jks`、`*.jks`、`*.keystore` —— 发布签名
- `local.properties` —— 本机 SDK 路径
- `202513253075*.xls` —— 课表原表，文件名含学号、内容含真实姓名，仅作本机导入样本
- `*.hprof` —— JVM 堆转储，可能含内存中的个人信息

## 历史

源码原在 t14p（Arch）`~/projects/drive-up`，2026-11-11 迁到本仓。迁移时丢弃了
2.27 GB 的 `java_pid*.hprof` 与 `.gradle/`、`build/`、`app/build/`，只保留 70 个源文件。
