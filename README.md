# Jeeves - Personal Assistant

> Your priorities, clarified. AI that stays on your phone.

![Status](https://img.shields.io/badge/status-MVP%20development-blue)
![Platform](https://img.shields.io/badge/platform-Android-green)
![License](https://img.shields.io/badge/license-proprietary-red)

## Overview

Jeeves is a **privacy-first, offline-capable** personal assistant powered by **on-device AI**. Built around the Eisenhower Matrix methodology, Jeeves helps you focus on what truly matters—without sending your data to the cloud.

### Why Jeeves?

- 🧠 **On-Device AI** - LLM runs locally on your phone. No internet required.
- 🎯 **Eisenhower Matrix** - Visual priority system: Do, Schedule, Delegate, Eliminate
- 📊 **Goal Tracking** - Connect daily tasks to long-term objectives  
- 📅 **Smart Calendar** - Daily briefings, meeting notes, action items
- 🔒 **Privacy-First** - Your data never leaves your device (sync is optional)
- 💰 **Cost-Efficient** - Minimal backend = fair pricing, lifetime option available

## MVP Features

### Core (100% Offline)
- ✅ Eisenhower Matrix task management with AI categorization
- ✅ Natural language task creation
- ✅ Goal setting and progress tracking
- ✅ Smart priority engine based on deadlines and importance
- ✅ Calendar integration with daily briefings
- ✅ Meeting notes with action item extraction
- ✅ Performance analytics and insights
- ✅ Smart reminders and nudges

### Post-MVP (Planned)
- 🤖 **AI Model Selection** - Choose GPT, Claude, Gemini, or Grok for premium features
- 🧩 **Custom AI Agents** - Build specialized agents for goals (fitness coach, career advisor, etc.)
- 📧 Email integration and summarization
- 💬 Messenger integration (WhatsApp, Telegram)
- ✈️ End-to-end trip planning
- 💪 Health goals integration
- 🍎 iOS app
- 🌐 Web companion

## Documentation

| Document | Description |
|----------|-------------|
| [Product Brief](docs/PRODUCT_BRIEF.md) | Product vision, features, MVP definition |
| [Action Plan](docs/ACTION_PLAN.md) | MVP development roadmap with SMART tasks |
| [Architecture](docs/ARCHITECTURE.md) | Technical architecture, on-device AI design |
| [UX Design System](docs/UX_DESIGN_SYSTEM.md) | Design guidelines, Eisenhower UX |
| [Marketing Strategy](docs/MARKETING_STRATEGY.md) | Go-to-market, competitive analysis |
| [Security Guidelines](docs/SECURITY_GUIDELINES.md) | Security and privacy standards |
| [DevOps Guide](docs/DEVOPS_GUIDE.md) | Infrastructure and deployment |
| [Team Agent](TEAM_AGENT.md) | AI team configuration and guidelines |

## Tech Stack

### Android (MVP)
- **Language**: Kotlin 2.0+
- **UI**: Jetpack Compose, Material Design 3
- **Architecture**: MVVM, Clean Architecture
- **Database**: Room (SQLCipher encrypted)
- **AI**: llama.cpp via JNI (Phi-3-mini / Gemma 2B)
- **Preferences**: DataStore

### Backend (Optional - Premium Features)
- **Auth & Sync**: Go 1.22+
- **Database**: PostgreSQL 16
- **Storage**: S3-compatible (encrypted backups)

### Future (Post-MVP)
- **iOS**: Swift 5.9+, SwiftUI
- **Shared Logic**: Kotlin Multiplatform

## Project Structure

```
jeeves/
├── README.md
├── TEAM_AGENT.md              # AI team config & action plan
├── docs/                      # Documentation
│   ├── PRODUCT_BRIEF.md       # Product vision & MVP
│   ├── ARCHITECTURE.md        # Technical architecture
│   ├── UX_DESIGN_SYSTEM.md    # Design system
│   ├── MARKETING_STRATEGY.md  # Go-to-market
│   ├── SECURITY_GUIDELINES.md # Security standards
│   └── DEVOPS_GUIDE.md        # DevOps guide
├── apps/
│   └── android/               # Android app (MVP)
│       ├── app/               # Main application
│       ├── core/              # Core modules
│       │   ├── common/
│       │   ├── ui/            # Design system
│       │   ├── data/          # Room, DataStore
│       │   ├── domain/        # Use cases
│       │   ├── ai/            # LLM engine
│       │   └── analytics/
│       ├── plugins/           # Feature plugins
│       │   ├── tasks/         # Eisenhower tasks
│       │   ├── goals/         # Goals & progress
│       │   └── calendar/      # Smart calendar
│       └── sync/              # Optional cloud sync
├── services/                  # Backend (optional)
│   ├── auth/                  # Authentication (Go)
│   └── sync/                  # Sync service (Rust)
├── models/                    # LLM models
│   └── README.md              # Model download instructions
└── scripts/                   # Development scripts
```

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17+
- Android SDK 34
- Device with 4GB+ RAM (for on-device LLM)

### Setup

```bash
# Clone repository
git clone https://github.com/your-org/jeeves.git
cd jeeves

# Open in Android Studio
studio apps/android

# Build debug APK
cd apps/android && ./gradlew assembleDebug

# Run tests
./gradlew test
```

### On-Device LLM Setup

The AI model (~1.5GB) downloads on first launch. For development:

```bash
# Download model manually (optional)
cd models
./download-phi3-mini.sh
```

### Running Tests

```bash
# Unit tests
cd apps/android && ./gradlew test

# UI tests
./gradlew connectedAndroidTest

# Lint checks
./gradlew lint
```

## Architecture Highlights

### Offline-First Design
- All core features work without internet
- Local Room database with SQLCipher encryption
- On-device LLM (llama.cpp) for AI features
- Optional encrypted cloud sync for premium users

### Pluggable Features
- Each feature is a self-contained module (plugin)
- Easy to enable/disable features
- Third-party plugin support planned

### Privacy Guarantees
- No analytics without consent
- No data leaves device by default
- E2E encryption for optional sync
- No third-party tracking SDKs

## Development Workflow

1. **Design** - Product requirements, UX specs
2. **TDD** - Write tests first, then implementation
3. **Build** - `./gradlew build -j 2`
4. **Test** - Unit, integration, UI tests
5. **Review** - Code review, design review
6. **Release** - Internal testing → Beta → Production

## Current Status

**Phase**: MVP Development  
**Platform**: Android  
**Target Launch**: Q2 2026

### Milestones
- [x] Product requirements complete
- [x] Architecture design complete
- [x] UX design system complete
- [ ] Core database and models
- [ ] AI engine integration
- [ ] Eisenhower task plugin
- [ ] Goals plugin
- [ ] Calendar plugin
- [ ] Beta testing
- [ ] Play Store launch

## Team

This project is developed by a cross-functional AI agent team:

- **Principal Product Manager** - Product vision, MVP definition, roadmap
- **Marketing Expert** - Go-to-market, competitive analysis, ASO
- **Principal UX Designer** - Eisenhower UX, Material Design 3
- **Principal Android Developer** - Kotlin, Compose, on-device AI
- **Principal Backend/Infrastructure Engineer** - Optional sync services
- **Security Expert** - Privacy-first design, encryption

## Contributing

See [TEAM_AGENT.md](TEAM_AGENT.md) for development guidelines and action plan.

## License

Proprietary - All rights reserved

---

*Built with ❤️ and on-device AI by the Jeeves Team*
