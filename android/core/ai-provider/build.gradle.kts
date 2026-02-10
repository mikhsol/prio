plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.prio.core.aiprovider"
    compileSdk = 35

    defaultConfig {
        minSdk = 29
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // NDK configuration for llama.cpp JNI
        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++17", "-fexceptions", "-frtti")
                arguments += listOf(
                    "-DANDROID_ARM_NEON=ON",
                    "-DANDROID_STL=c++_shared"
                )
            }
        }
        
        ndk {
            // Target ARM64 for performance (per 0.2.1 findings)
            abiFilters += listOf("arm64-v8a")
        }
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
    
    // Optional: Enable native build when llama.cpp is integrated
    // externalNativeBuild {
    //     cmake {
    //         path = file("src/main/cpp/CMakeLists.txt")
    //         version = "3.22.1"
    //     }
    // }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
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
    implementation(project(":core:ai"))

    // Compose (for Gemini Nano UI components)
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.bundles.compose.debug)

    // ML Kit GenAI (Gemini Nano on-device AI — Milestone 3.6)
    implementation(libs.mlkit.genai.prompt)
    implementation(libs.mlkit.genai.summarization)
    
    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    
    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    
    // Coroutines
    implementation(libs.bundles.coroutines)
    
    // Logging
    implementation(libs.timber)
    
    // Testing
    testImplementation(libs.bundles.testing)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.jupiter.engine)
}
