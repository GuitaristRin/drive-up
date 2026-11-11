// 工具链与 Kanesumi / Ncrust 对齐:AGP 8.5.0 + Kotlin 1.9.24 + Gradle 9.3.1。
// 组合构建里 Kanesumi 模块以 jvmTarget 11 编译,消费端保持同一套 Kotlin 版本避免
// 元数据冲突。KSP 必须跟随 Kotlin 版本号。
plugins {
    id("com.android.application") version "8.5.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
}
