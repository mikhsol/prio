# Milestone 1.1: UX Screen Specifications

**Phase**: Phase 1 - Foundation (MVP)  
**Status**: ✅ Completed  
**Duration**: 2 days  
**Completed**: February 4, 2026  
**Owner**: UX Designer

---

## Overview

This milestone contains detailed text-based screen specifications for all Prio MVP screens. These specifications define the UI/UX requirements for developers to implement using Jetpack Compose and Material Design 3.

---

## Deliverables

| Doc ID | Document | Status | Description |
|--------|----------|--------|-------------|
| 1.1.1 | [Task List Screen Spec](1.1.1_task_list_screen_spec.md) | ✅ | Primary task list with Eisenhower Matrix grouping |
| 1.1.2 | [Task Detail Sheet Spec](1.1.2_task_detail_sheet_spec.md) | ✅ | Bottom sheet for viewing/editing tasks |
| 1.1.3 | [Quick Capture Flow Spec](1.1.3_quick_capture_flow_spec.md) | ✅ | FAB-to-input quick task capture (<5s flow) |
| 1.1.4 | [Goals Screens Spec](1.1.4_goals_screens_spec.md) | ✅ | Goals list, detail, and creation wizard |
| 1.1.5 | [Today Dashboard + Briefing Spec](1.1.5_today_dashboard_briefing_spec.md) | ✅ | Morning briefing, today view (highest retention driver) |
| 1.1.6 | [Calendar Day View Spec](1.1.6_calendar_day_view_spec.md) | ✅ | Timeline view with events and tasks |
| 1.1.7 | [Evening Summary Spec](1.1.7_evening_summary_spec.md) | ✅ | Day closure flow and reflection |
| 1.1.8 | [Onboarding Flow Spec](1.1.8_onboarding_flow_spec.md) | ✅ | 5-screen onboarding with privacy focus |
| 1.1.9 | [Settings Screens Spec](1.1.9_settings_screens_spec.md) | ✅ | All settings and configuration screens |
| 1.1.10 | [Error/Empty/Offline States Spec](1.1.10_error_empty_offline_states_spec.md) | ✅ | System states and error handling |
| 1.1.11 | [Accessibility Requirements Spec](1.1.11_accessibility_requirements_spec.md) | ✅ | WCAG 2.1 AA compliance requirements |
| 1.1.12 | [Low-Fidelity Wireframes](1.1.12_wireframes_spec.md) | ✅ | ASCII wireframes for 4 core flows |
| 1.1.13 | [Component Specifications](1.1.13_component_specifications.md) | ✅ | TaskCard, GoalCard, BriefingCard, QuadrantBadge |

**Note**: All 13 tasks completed.

---

## Key Design Decisions

### 1. Eisenhower Matrix Visual System
- **Q1 (DO FIRST)**: 🔴 Red (#DC2626)
- **Q2 (SCHEDULE)**: 🟡 Amber (#F59E0B)
- **Q3 (DELEGATE)**: 🟠 Orange (#F97316)
- **Q4 (MAYBE LATER)**: ⚪ Gray (#6B7280)

### 2. Privacy-First Messaging
Per Maya persona validation, privacy messaging is prominently displayed:
- Onboarding screen 2 dedicated to Privacy Promise
- "Data never leaves this device" emphasized throughout
- Settings clearly show AI runs locally

### 3. Habit Loop Design
Morning Briefing → Day Actions → Evening Summary creates a daily engagement loop with highest retention value (2.18 weighted score).

### 4. AI Trust Building
- Show AI classification with confidence indicator
- Provide brief explanation ("Because: deadline today")
- Allow manual override at all times
- Display "AI Insight" labels to build transparency

### 5. Offline-First UX
- Subtle offline banner (not disruptive)
- All core features work offline
- Sync indicator when back online

---

## Persona Alignment

| Persona | Key Features Addressed |
|---------|----------------------|
| **Alex** (Overwhelmed Professional) | Quick Capture <5s, Morning Briefing, AI classification reduces decision fatigue |
| **Maya** (Privacy-Conscious User) | Privacy Promise screen, on-device AI messaging, data export options |
| **Jordan** (Aspiring Achiever) | Goal tracking, progress visualization, milestone celebrations |

---

## Technical Specifications

- **Platform**: Android (API 29+)
- **UI Framework**: Jetpack Compose
- **Design System**: Material Design 3
- **Accessibility**: WCAG 2.1 AA
- **Touch Targets**: ≥48dp
- **Text Scaling**: Up to 200%
- **Dark Mode**: Full support

---

## Exit Criteria Status

| Criteria | Status |
|----------|--------|
| All 6 key screens specified | ✅ |
| Component specifications complete | ✅ |
| Accessibility requirements documented | ✅ |

---

## Dependencies for Next Milestone

Milestone 1.2 (Data Architecture) can use these specifications to:
- Define Task, Goal, Event data models
- Plan Room database schema
- Design AI classification input/output interfaces

---

## File Structure

```
docs/results/1.1/
├── README.md (this file)
├── 1.1.1_task_list_screen_spec.md
├── 1.1.2_task_detail_sheet_spec.md
├── 1.1.3_quick_capture_flow_spec.md
├── 1.1.4_goals_screens_spec.md
├── 1.1.5_today_dashboard_briefing_spec.md
├── 1.1.6_calendar_day_view_spec.md
├── 1.1.7_evening_summary_spec.md
├── 1.1.8_onboarding_flow_spec.md
├── 1.1.9_settings_screens_spec.md
├── 1.1.10_error_empty_offline_states_spec.md
├── 1.1.11_accessibility_requirements_spec.md
├── 1.1.12_wireframes_spec.md
└── 1.1.13_component_specifications.md
```

---

*Milestone Owner: UX Designer*  
*Last Updated: February 4, 2026*
