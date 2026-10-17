plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.hyperos.quickopen"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.hyperos.quickopen"
        minSdk = 29
        targetSdk = 35
        versionCode = 100
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

tasks.matching { it.name.contains("AarMetadata") }.configureEach {
    enabled = false
}

dependencies {
    compileOnly("de.robv.android.xposed:api:82")

    implementation("androidx.activity:activity-compose:1.11.0")

    implementation("androidx.compose.material:material-icons-core-android:1.7.8")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.ui:ui-android:1.10.5")

    // ---- MIUIX 0.9.4（官方推荐模块化依赖）----
    implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-preference-android:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-icons-android:0.9.4")
    implementation("org.jetbrains.compose.runtime:runtime:1.12.0")
    implementation("org.jetbrains.compose.foundation:foundation:1.12.0")
    implementation("org.jetbrains.compose.material3:material3-window-size-class:1.12.0-alpha03")
    implementation("org.jetbrains.compose.ui:ui:1.12.0")
}
