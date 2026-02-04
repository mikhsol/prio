# Benchmark ProGuard Rules
# Additional rules for benchmark builds

# Keep benchmark classes
-keep class androidx.benchmark.** { *; }

# Keep all for accurate benchmarks
-dontobfuscate
