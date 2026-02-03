# Keep JNI methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep LlamaEngine
-keep class app.jeeves.llmtest.engine.LlamaEngine { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class app.jeeves.llmtest.**$$serializer { *; }
-keepclassmembers class app.jeeves.llmtest.** {
    *** Companion;
}
-keepclasseswithmembers class app.jeeves.llmtest.** {
    kotlinx.serialization.KSerializer serializer(...);
}
