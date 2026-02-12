# Prio ProGuard Rules

# Keep serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
}

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }

# Keep Room entities
-keep class com.prio.core.data.local.entity.** { *; }

# Keep AI model classes
-keep class com.prio.core.ai.model.** { *; }
-keep class com.prio.core.ai.provider.** { *; }

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Compose
-keep class androidx.compose.** { *; }

# Firebase Crashlytics
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# =========================================================================
# Milestone 4.3.7 — Enhanced R8 Configuration
# =========================================================================

# --- kotlinx.datetime ---
# Prevent stripping of date/time classes used in Room converters & business logic
-keep class kotlinx.datetime.** { *; }
-dontwarn kotlinx.datetime.**

# --- kotlinx.coroutines ---
# Keep coroutine internals for proper stack traces in Crashlytics
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.flow.**

# --- WorkManager ---
# Keep Worker classes that are instantiated via reflection
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class com.prio.core.data.worker.** { *; }

# --- DataStore ---
# Keep Preferences DataStore serializers
-keep class androidx.datastore.** { *; }
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# --- Navigation Compose ---
# Keep NavType and argument classes for safe args / deep links
-keep class * extends androidx.navigation.NavType { *; }
-keep class * implements java.io.Serializable { *; }

# --- Timber ---
# Remove Timber debug logs in release (Tree.d, Tree.v stripped)
-assumenosideeffects class timber.log.Timber {
    public static void d(...);
    public static void v(...);
}

# --- ML Kit / GenAI ---
# Keep GenAI inference classes used for Gemini Nano
-keep class com.google.ai.edge.** { *; }
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.ai.edge.**

# --- llama.cpp JNI ---
# Keep native method declarations for llama.cpp bindings
-keepclasseswithmembernames class * {
    native <methods>;
}

# --- Domain model classes ---
# Keep all domain model classes (used in serialization / Parcelable)
-keep class com.prio.core.common.model.** { *; }
-keep class com.prio.core.domain.model.** { *; }

# --- Strict mode: report removed classes for audit ---
-printusage build/outputs/r8-usage.txt
