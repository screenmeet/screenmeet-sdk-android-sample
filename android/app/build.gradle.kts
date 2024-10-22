import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.application)
    alias(libs.plugins.dagger.hilt.plugin)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.screenmeet.live"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.screenmeet.live"
        minSdk = 23
        targetSdk = 33
        versionCode = 39
        versionName = "3.0.11"

        ndk {
            abiFilters += listOf("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
        }

        val apiKey = System.getenv("SM_API_KEY")
            ?: gradleLocalProperties(rootDir, providers).getProperty("SM_API_KEY")
        buildConfigField("String", "SM_API_KEY", "\"$apiKey\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("debug") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

detekt {
    config = files("$rootDir/.detekt/config.yml")
}

ktlint {
    verbose.set(true)
    android.set(true)
    outputToConsole.set(true)
    outputColorName.set("RED")
    ignoreFailures.set(false)
    enableExperimentalRules.set(true)
    disabledRules.set(listOf("no-wildcard-imports", "import-ordering"))
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}

dependencies {
    // ScreenMeet SDK
    implementation(libs.screenmeet.sdk)

    // Conditional dependencies
    val includeFlutter = project.findProperty("includeFlutter") as String? ?: "false"
    val includeReactNative = project.findProperty("includeReactNative") as String? ?: "false"

    if (includeFlutter.toBoolean()) {
        implementation(project(":flutter_demo"))
    }
    if (includeReactNative.toBoolean()) {
        implementation(project(":react_demo"))
    }

    // Kotlin
    implementation(libs.kotlin.reflect)

    // AndroidX Core and UI Components
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.material)

    // Firebase
    implementation(libs.firebase.crashlytics.ktx)
    implementation(libs.firebase.analytics.ktx)

    // Dagger Hilt
    kapt(libs.dagger.hilt.compiler)
    implementation(libs.dagger.hilt.android)
    implementation(libs.androidx.hilt.navigation)

    // Other Libraries
    implementation(libs.zoomlayout)
    implementation(libs.otpview.pinview)
    implementation(libs.insetter)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
