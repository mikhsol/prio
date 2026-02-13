# Prio — Architectural Decision Records (ADRs)

> Resolved architectural and product decisions. Agents must not re-debate these.
> If you disagree with a decision, propose a revision with evidence — don't silently ignore it.
> Last Updated: February 13, 2026

---

## How to Use This File

1. **Before making an architectural choice**: Check if it's already decided here
2. **When adding a new decision**: Use the template at the bottom
3. **When revisiting a decision**: Add a "Revisited" section with date and new context

---

## ADR-001: Android-First, Offline-First Architecture

**Date**: January 2026  
**Status**: ✅ Accepted  
**Context**: Need to choose initial platform and connectivity model.  
**Decision**: Build for Android first (API 29+). All MVP features work without internet. iOS is architecture-ready (kotlinx.datetime, clean module boundaries) but not in MVP scope.  
**Consequences**: No web app or iOS in MVP. Backend services are optional. All data stored locally on device.  
**Evidence**: `docs/PRODUCT_BRIEF.md` §Offline-First, `docs/ARCHITECTURE.md` §Core Design Philosophy

---

## ADR-002: Rule-Based AI as Primary Classifier

**Date**: February 2026  
**Status**: ✅ Accepted  
**Context**: Tested Phi-3-mini (40% accuracy), Mistral 7B (80% accuracy but 45-60s latency), and rule-based classifier (75% accuracy, <50ms latency) for Eisenhower task classification.  
**Decision**: Rule-based classifier is the primary classification engine. LLM (Gemini Nano where available, llama.cpp as fallback) is used for enhancement/edge cases only.  
**Consequences**: Accuracy ceiling at ~75% without LLM. Fast and reliable for users. Can improve with more rules and training data.  
**Evidence**: `docs/ACTION_PLAN.md` §Verified Performance Baselines, `docs/results/0.2/`

---

## ADR-003: Gemini Nano via ML Kit as Preferred LLM

**Date**: February 2026  
**Status**: ✅ Accepted  
**Context**: Need on-device LLM that doesn't require shipping a 2-4GB model with the app. Google's Gemini Nano via Android AI Core runs on supported Pixel/Samsung devices with zero app size impact.  
**Decision**: Gemini Nano (ML Kit GenAI 1.0.0-beta1) is the preferred LLM provider. llama.cpp with Phi-3-mini is the fallback for devices that don't support Gemini Nano.  
**Consequences**: AI quality depends on device support. Need graceful degradation to rule-based on unsupported devices.  
**Evidence**: `core/ai-provider/`, `docs/results/0.2.6/`

---

## ADR-004: AiProvider Abstraction Layer

**Date**: February 2026  
**Status**: ✅ Accepted  
**Context**: Multiple AI backends (Gemini Nano, llama.cpp, cloud APIs, rule-based) need a unified interface.  
**Decision**: All AI access goes through `AiProvider` interface → `AiProviderRouter` selects the best available provider at runtime.  
**Consequences**: Easy to add/swap providers. Slight abstraction overhead. Router logic needs testing.  
**Evidence**: `core/ai/provider/AiProvider.kt`, `core/ai-provider/router/AiProviderRouter.kt`

---

## ADR-005: MVVM + MVI Hybrid with StateFlow

**Date**: January 2026  
**Status**: ✅ Accepted  
**Context**: Need UI architecture pattern for Compose screens.  
**Decision**: MVVM structure with MVI-style event/effect channels. StateFlow for persistent state, Channel<Effect> for one-time events (navigation, snackbar, confetti).  
**Alternatives Rejected**: 
- Pure MVI (Orbit/Redux): Over-engineered for this app size
- LiveData: Not Compose-idiomatic, lifecycle-bound
- Compose State only: Hard to test, leaks concerns  
**Evidence**: `feature/tasks/TaskListViewModel.kt`, `docs/CONVENTIONS.md`

---

## ADR-006: Room + kotlinx.datetime for Data Layer

**Date**: January 2026  
**Status**: ✅ Accepted  
**Context**: Need local database with type-safe queries and multiplatform-ready time types.  
**Decision**: Room 2.7 with KSP for database. kotlinx.datetime.Instant for all timestamps. Custom type converters in `PrioTypeConverters.kt`.  
**Alternatives Rejected**:
- SQLDelight: Better multiplatform, but less ecosystem support on Android
- java.time: Not multiplatform-ready
- Long timestamps: No type safety  
**Evidence**: `core/data/`, `android/gradle/libs.versions.toml`

---

## ADR-007: Hilt for Dependency Injection

**Date**: January 2026  
**Status**: ✅ Accepted  
**Context**: Need DI framework that works with Compose, ViewModel, WorkManager, and testing.  
**Decision**: Hilt (Dagger 2.56) with KSP. Standard patterns: `@HiltViewModel`, `@Singleton`, `@Module @InstallIn`.  
**Alternatives Rejected**:
- Koin: Simpler but no compile-time verification
- Manual DI: Doesn't scale, error-prone  
**Evidence**: `core/data/di/`, `app/build.gradle.kts`

---

## ADR-008: Type-Safe Navigation with kotlinx.serialization

**Date**: January 2026  
**Status**: ✅ Accepted  
**Context**: Compose Navigation 2.8+ supports type-safe routes via serialization.  
**Decision**: `sealed interface PrioRoute` with `@Serializable` routes. No string-based route definitions.  
**Consequences**: Compile-time route verification. Slightly more boilerplate than string routes.  
**Evidence**: `app/.../navigation/PrioNavigation.kt`

---

## ADR-009: JUnit 5 + MockK + Turbine for Testing

**Date**: January 2026  
**Status**: ✅ Accepted  
**Context**: Need testing stack that works with coroutines, Flow, and Compose.  
**Decision**: JUnit 5 (with `@Nested`, `@DisplayName`), MockK for mocking, Turbine for Flow assertions, `StandardTestDispatcher` for coroutines.  
**Alternatives Rejected**:
- JUnit 4: No nested tests, worse error messages
- Mockito: Worse Kotlin support
- Truth assertions: Personal preference, both fine  
**Evidence**: `app/src/test/`, `docs/CONVENTIONS.md` §Testing

---

## ADR-010: Robot Pattern for E2E Tests

**Date**: February 2026  
**Status**: ✅ Accepted  
**Context**: E2E tests need stable selectors and readable scenarios.  
**Decision**: Page Object Model adapted as "Robots" for Compose. Each screen has a Robot class. Scenarios compose Robots — never use raw selectors.  
**Consequences**: More initial setup, but tests are stable and readable. Selector changes affect only one Robot.  
**Evidence**: `app/src/androidTest/robots/`, `docs/E2E_TEST_PLAN.md`

---

## ADR-011: Monetization Model — Freemium + Lifetime

**Date**: February 2026  
**Status**: ✅ Accepted  
**Context**: Need revenue model that respects privacy-conscious users.  
**Decision**: Free ($0, basic tasks, 5 AI/day) → Pro ($6.99/mo, unlimited AI, full features) → Lifetime ($99.99, all Pro forever).  
**Rationale**: Maya persona has subscription aversion (WTP $5/mo). Lifetime option captures her. Alex persona has higher WTP ($10/mo). Jordan is freemium hook.  
**Evidence**: `docs/ACTION_PLAN.md` §Pricing Strategy, `docs/results/0.1/0.1.5_user_personas.md`

---

## ADR-012: Firebase for Analytics + Crashlytics Only

**Date**: February 2026  
**Status**: ✅ Accepted  
**Context**: Need crash reporting and basic analytics without compromising privacy.  
**Decision**: Firebase with anonymized user IDs only. No PII in events or crash reports. Analytics limited to feature usage, performance, and crash-free rates.  
**Constraints**: No user-identifiable data in Firebase. No task content, goal titles, or personal information.  
**Evidence**: `app/build.gradle.kts`, `docs/SECURITY_GUIDELINES.md`

---

## ADR-013: No Account Required for Core Features

**Date**: January 2026  
**Status**: ✅ Accepted  
**Context**: Privacy-first positioning requires minimal friction.  
**Decision**: Full app functionality without account creation (Things 3 model). Account only needed for optional cloud sync/backup (post-MVP).  
**Consequences**: No server-side user management in MVP. Simpler launch. Risk: harder to re-engage unidentified users.  
**Evidence**: `docs/PRODUCT_BRIEF.md` §Local-Only Mode

---

## ADR-014: Multi-Module Clean Architecture

**Date**: January 2026  
**Status**: ✅ Accepted  
**Context**: Need code organization that enforces layer boundaries.  
**Decision**: 7 Gradle modules: `:app`, `:core:common`, `:core:ui`, `:core:data`, `:core:domain`, `:core:ai`, `:core:ai-provider`. Feature modules within `:app` package for MVP simplicity.  
**Consequences**: Enforced boundaries. Build parallelism. Feature modules can be extracted later.  
**Future**: Extract feature modules (`:feature:tasks`, `:feature:goals`) when codebase grows past ~50K lines.  
**Evidence**: `android/settings.gradle.kts`, `docs/ARCHITECTURE.md`

---

## Template for New Decisions

```markdown
## ADR-XXX: [Title]

**Date**: [Month Year]
**Status**: ✅ Accepted | ⚠️ Proposed | ❌ Rejected | 🔄 Superseded by ADR-XXX
**Context**: [What prompted this decision?]
**Decision**: [What did we decide?]
**Alternatives Rejected**: [What else was considered and why rejected?]
**Consequences**: [What are the trade-offs?]
**Evidence**: [Links to relevant files, docs, or benchmarks]
```
