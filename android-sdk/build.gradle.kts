plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.neteasedecryptor.android"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // 依赖同项目的 core 纯 Kotlin 模块
    implementation(project(":core"))
    
    // Android Support/Jetpack Core & DocumentFile
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
}
