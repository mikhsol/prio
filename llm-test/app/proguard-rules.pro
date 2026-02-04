# Keep JNI methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep LlamaEngine
-keep class app.prio.llmtest.engine.LlamaEngine { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class app.prio.llmtest.**$$serializer { *; }
-keepclassmembers class app.prio.llmtest.** {
    *** Companion;
}
-keepclasseswithmembers class app.prio.llmtest.** {
    kotlinx.serialization.KSerializer serializer(...);
}
