plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
}

// ── App version name ────────────────────────────────────────────────────────
// The base version name is still written here in code. The git short commit
// hash is appended at build time, producing e.g. "4.2-1a2b3d4".
val baseVersionName = "4.2"
val gitCommitHash: String = try {
    val process = ProcessBuilder("git", "rev-parse", "--short=7", "HEAD")
        .directory(rootDir)
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
    process.waitFor()
    if (process.exitValue() == 0 && output.matches(Regex("[0-9a-f]{7,40}"))) output else ""
} catch (_: Exception) {
    ""
}

android {
    namespace = "moe.echo.variablefonttest_n"
    compileSdk = 35

    defaultConfig {
        applicationId = "moe.echo.variablefonttest_n"
        minSdk = 21
        targetSdk = 35
        versionCode = 23
        versionName = if (gitCommitHash.isEmpty()) baseVersionName else "$baseVersionName-$gitCommitHash"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // ========== 新增 ABI 拆分配置 ==========
    splits {
        abi {
            isEnable = true
            reset()
            // 包含需要拆分的所有 ABI（与 Universal 包涵盖的架构一致）
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            // 同时生成包含全部架构的 Universal 包
            isUniversalApk = true
        }
    }
    // =====================================

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_FILE") ?: "/dev/null")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: ""
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }

        // 带 R8 的产物：其余配置与 release 保持一致（同样的签名、混淆规则、Splits），
        // 仅额外开启代码压缩与资源压缩，以提供体积更小的安装包。
        // 产物：app-universal-r8.apk 以及对应的 ABI 拆分包。
        create("r8") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
//    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
//    implementation(libs.androidx.ui)
//    implementation(libs.androidx.ui.graphics)
//    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

//    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
    implementation(libs.androidx.preference)
}