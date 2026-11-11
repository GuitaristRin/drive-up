plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.driveup"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.driveup"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            storeFile = file("keystore.jks")
            storePassword = "driveup123"
            keyAlias = "driveup"
            keyPassword = "driveup123"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions { jvmTarget = "11" }

    buildFeatures { compose = true }

    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/NOTICE.md"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/*.txt"
            excludes += "module-info.class"
        }
    }
}

dependencies {
    // Kanesumi —— Metro 组件库,通过 settings.gradle.kts 的 includeBuild 从
    // ../../projects/Kanesumi 接入。core/anim 由 controls/structure 的 api
    // 依赖自动带出,这里只列上游两件。
    implementation("io.github.takahashirinta:kanesumi-controls:0.1.0-SNAPSHOT")
    implementation("io.github.takahashirinta:kanesumi-structure:0.1.0-SNAPSHOT")

    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    // foundation 不随 ui / Kanesumi 的 implementation 依赖暴露,本 app 直接使用
    // LazyColumn / Canvas / gesture 等,必须显式声明。
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Apache POI 3.17 for .xls parsing — more Android-compatible than 5.x
    implementation("org.apache.poi:poi:3.17") {
        exclude(group = "log4j")
        exclude(group = "org.slf4j")
        exclude(module = "commons-logging")
    }
    implementation("commons-codec:commons-codec:1.10")
    implementation("commons-logging:commons-logging:1.2")
}
