plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.prio.core.ai"
    compileSdk = 35

    defaultConfig {
        minSdk = 29
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    @Suppress("UnstableApiUsage")
    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    
    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    
    // DataStore for preferences
    implementation(libs.datastore.preferences)
    
    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    
    // Coroutines
    implementation(libs.bundles.coroutines)
    
    // Logging
    implementation(libs.timber)
    
    // Testing
    testImplementation(libs.bundles.testing)
    testRuntimeOnly(libs.junit.jupiter.engine)
}
