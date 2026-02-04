# Jeeves Android App

Your Private Productivity AI - An offline-first Android app with on-device AI for task prioritization using the Eisenhower Matrix.

## Project Structure

```
android/
├── app/                     # Main application module
├── core/
│   ├── common/              # Shared models and utilities
│   ├── ui/                  # Compose theme and components
│   ├── data/                # Room database and repositories
│   ├── domain/              # Use cases and domain models
│   ├── ai/                  # AI types and interfaces
│   └── ai-provider/         # AI provider implementations (llama.cpp)
├── gradle/
│   └── libs.versions.toml   # Dependency version catalog
└── build.gradle.kts         # Root build configuration
```

## Build Variants

- **debug**: Development build with logging
- **release**: Production build with ProGuard
- **benchmark**: Performance testing build

## Getting Started

1. Open in Android Studio Hedgehog (2023.1.1) or later
2. Sync Gradle files
3. Run on device or emulator (API 29+)

## Key Technologies

- **UI**: Jetpack Compose with Material 3
- **DI**: Hilt
- **Database**: Room with SQLite
- **Preferences**: DataStore
- **AI**: llama.cpp via JNI (Phi-3-mini)
- **Navigation**: Compose Navigation with type-safe routes

## Architecture

MVVM with Clean Architecture:
- Presentation → ViewModels → Use Cases → Repositories → Data Sources

## Testing

```bash
./gradlew testDebugUnitTest      # Unit tests
./gradlew lint                   # Lint checks
./gradlew assembleDebug          # Build debug APK
```
