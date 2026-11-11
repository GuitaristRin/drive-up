pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    // 让 gradle-daemon-jvm.properties 的 toolchainVersion=21 能自动解析/复用本地
    // JDK 21(gradle user home 已缓存 adoptium 21,不会走网络)。
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "DriveUp"
include(":app")

// Kanesumi 通过组合构建接入 —— 与 Ncrust 相同的消费方式。
// Kanesumi 侧源码改动不需要 publish 就能被增量构建看到。
// 显式 dependencySubstitution 让 Kanesumi 无需配 maven-publish。
// 未来 Kanesumi 上 Maven Central 时,只需删掉整个 includeBuild 块,
// app/build.gradle.kts 里的坐标一字不改就能切。
includeBuild("../../projects/Kanesumi") {
    dependencySubstitution {
        substitute(module("io.github.takahashirinta:kanesumi-core"))
            .using(project(":kanesumi-core"))
        substitute(module("io.github.takahashirinta:kanesumi-anim"))
            .using(project(":kanesumi-anim"))
        substitute(module("io.github.takahashirinta:kanesumi-controls"))
            .using(project(":kanesumi-controls"))
        substitute(module("io.github.takahashirinta:kanesumi-structure"))
            .using(project(":kanesumi-structure"))
    }
}
