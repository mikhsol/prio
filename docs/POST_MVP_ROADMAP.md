# Prio - Post-MVP Roadmap

## Overview

This document contains features and phases deferred from the MVP to accelerate initial launch. These features are planned for v1.1+ releases after the Play Store launch.

---

## Deferred Features (v1.1)

The following features were removed from MVP scope to accelerate delivery:

| Feature | Original Effort | Reason for Deferral | Priority |
|---------|----------------|---------------------|----------|
| **Gemma-2B benchmarking** | 3h | Phi-3-mini is sufficient; add Gemma only if issues arise | Low |
| **SQLCipher encryption** | 1h extra | Standard Room + Android Keystore is secure enough for MVP | Medium |
| **Streaming LLM inference** | 3h | Sync inference meets UX needs; streaming is polish | Medium |
| **Matrix View (2x2 grid)** | 3h | List View with quadrant badges delivers same value | Medium |
| **Voice input** | 2h+ | Requires audio permissions, speech API - adds complexity | High |
| **Rich text meeting notes** | 1h extra | Plain text is sufficient for MVP | Low |
| **Full RRULE recurring tasks** | 1h extra | Daily/weekly/monthly presets cover 95% of use cases | Low |
| **80/20 Insights analytics** | 6h | Complex ML analysis - basic stats sufficient for MVP | High |
| **Home screen widget** | 4h | Glance widgets are buggy; high effort, low impact | High |
| **Quick Settings tile** | 2h | Nice-to-have, not core value | Low |
| **Battery optimization task** | 3h | Standard practices sufficient; optimize based on feedback | Medium |

**Total saved from MVP: ~30 hours (~4 dev days)**

### v1.1 Release Plan (Weeks 16-18)

| Priority | Feature | Effort | Owner |
|----------|---------|--------|-------|
| High | Voice input for Quick Capture | 3h | Android Developer |
| High | Home screen widget (Glance) | 4h | Android Developer |
| High | 80/20 Insights analytics | 6h | Android Developer |
| Medium | Matrix View toggle | 3h | Android Developer |
| Medium | Streaming LLM inference | 3h | Android Developer |
| Medium | SQLCipher database encryption | 2h | Android Developer |

---

## Phase 7: Cloud AI Integration (Weeks 18-22)

### Overview
Build secure AI gateway backend to enable Pro tier users to access cloud LLM providers (OpenAI, Anthropic, Google, xAI) while maintaining on-device fallback. The mobile app already has the `CloudGatewayProvider` stub and API contract from MVP—this phase implements the backend.

### Milestone 7.1: AI Gateway Backend (Rust)
**Goal**: Build production-ready AI gateway to route requests to cloud providers  
**Owner**: Backend Engineer

*Note: API contract already defined in MVP (Milestone 2.2.14). Backend implements the AiRequest/AiResponse types.*

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 7.1.1 | Set up Axum-based AI gateway service | Backend Engineer | 3h | Rust project with Axum, proper error handling, health checks |
| 7.1.2 | Implement authentication middleware (JWT) | Backend Engineer | 3h | JWT validation, user extraction, Pro tier check |
| 7.1.3 | Implement rate limiting middleware | Backend Engineer | 2h | Per-user limits: 60 req/min free, 300 req/min pro |
| 7.1.4 | Define LlmProvider trait and adapter pattern | Backend Engineer | 2h | Rust trait matching mobile AiProvider interface |
| 7.1.5 | Create OpenAI adapter (GPT-4o, GPT-4o-mini) | Backend Engineer | 4h | Working adapter with streaming SSE support |
| 7.1.6 | Create Anthropic adapter (Claude family) | Backend Engineer | 4h | Sonnet, Haiku support with streaming |
| 7.1.7 | Create Google adapter (Gemini 1.5) | Backend Engineer | 4h | Pro and Flash variants working |
| 7.1.8 | Create xAI adapter (Grok-2) | Backend Engineer | 3h | Grok-2 and Grok-2-mini support |
| 7.1.9 | Implement model router with failover | Backend Engineer | 3h | Automatic failover when provider down, cost-based routing |
| 7.1.10 | Implement usage tracking (PostgreSQL + Redis) | Backend Engineer | 4h | Per-request token tracking, cost calculation, monthly aggregation |
| 7.1.11 | Implement quota management | Backend Engineer | 3h | Soft/hard monthly limits, overage alerts, quota reset |
| 7.1.12 | Add self-hosted LLM option (vLLM/Ollama) | Backend Engineer | 3h | Route to self-hosted for cost reduction |
| 7.1.13 | Load testing and optimization | Backend Engineer | 3h | Gateway handles 100 req/s, p99 latency <500ms |
| 7.1.14 | Deploy to staging with Kubernetes | Backend Engineer | 2h | Deployed with HPA, ready for testing |

**Milestone Exit Criteria**:
- [ ] All 4 cloud providers integrated with streaming support
- [ ] Failover works automatically (tested with provider unavailability)
- [ ] Usage tracking accurate to $0.01
- [ ] Self-hosted option available for cost optimization
- [ ] API matches mobile CloudGatewayProvider contract

### Milestone 7.2: Android Cloud AI Integration
**Goal**: Complete CloudGatewayProvider implementation and add model selection UI  
**Owner**: Android Developer

*Note: CloudGatewayProvider stub already exists from MVP. This phase adds full implementation and user-facing features.*

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 7.2.1 | Implement CloudGatewayProvider (full) | Android Developer | 3h | HTTP client calling backend AI gateway, handles streaming |
| 7.2.2 | Implement SSE streaming parser | Android Developer | 2h | Parses Server-Sent Events for real-time response display |
| 7.2.3 | Update AiProviderRouter for cloud integration | Android Developer | 2h | Routes to cloud when user enables and has quota |
| 7.2.4 | Implement model selection UI in Settings | Android Developer | 3h | Settings screen showing available cloud models with pricing |
| 7.2.5 | Implement per-feature model defaults | Android Developer | 2h | Different models per feature (cheap for classification, powerful for briefings) |
| 7.2.6 | Implement usage dashboard | Android Developer | 4h | Show tokens used, cost this month, remaining quota |
| 7.2.7 | Implement cost estimation before request | Android Developer | 2h | Show estimated cost before expensive cloud operations |
| 7.2.8 | Implement smart offline fallback | Android Developer | 2h | Seamless switch to on-device when offline, queue if preferred |
| 7.2.9 | Write integration tests for cloud provider | Android Developer | 2h | E2E tests with mocked backend responses |

**Milestone Exit Criteria**:
- [ ] CloudGatewayProvider fully functional with streaming
- [ ] Users can select preferred cloud model per feature
- [ ] Usage displayed accurately in real-time
- [ ] Offline fallback seamless and transparent
- [ ] Integration tests passing

---

## Phase 8: Custom AI Agents (Weeks 22-26)

### Overview
Enable users to create and customize AI agents specialized for specific goals (fitness coach, career advisor, etc.). Premium feature for Pro tier.

### Milestone 8.1: Agent System Backend
**Goal**: Build agent storage, execution, and marketplace infrastructure  
**Owner**: Backend Engineer

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 8.1.1 | Design agent data model schema | Backend Engineer | 2h | PostgreSQL schema for agents |
| 8.1.2 | Implement agent CRUD API | Backend Engineer | 3h | REST endpoints for agent management |
| 8.1.3 | Implement agent sync service | Backend Engineer | 3h | Sync agents across devices |
| 8.1.4 | Create built-in agent templates (7 templates) | Backend Engineer | 4h | PM, Career, Fitness, Learning, Finance, Writing, Home |
| 8.1.5 | Implement agent execution service | Backend Engineer | 4h | Build prompts, execute via AI gateway |
| 8.1.6 | Implement agent context injection | Backend Engineer | 3h | Inject user data respecting permissions |
| 8.1.7 | Design marketplace API | Backend Engineer | 2h | API for browsing/downloading community agents |

**Milestone Exit Criteria**:
- [ ] Agent CRUD working
- [ ] 7 built-in templates available
- [ ] Context injection respects permissions

### Milestone 8.2: Android Agent Builder
**Goal**: Create intuitive agent builder wizard in Android app  
**Owner**: Android Developer + UX Designer

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 8.2.1 | Write UX spec for Agent Builder wizard | UX Designer | 3h | 5-step wizard spec with all states |
| 8.2.2 | Implement Step 1: Purpose selection | Android Developer | 3h | Category/custom purpose picker |
| 8.2.3 | Implement Step 2: Personality sliders | Android Developer | 3h | 3 sliders with preview text |
| 8.2.4 | Implement Step 3: Expertise & instructions | Android Developer | 3h | Domain tags, custom text input |
| 8.2.5 | Implement Step 4: Permissions | Android Developer | 2h | Checkbox permissions UI |
| 8.2.6 | Implement Step 5: Test & refine | Android Developer | 4h | Interactive chat test |
| 8.2.7 | Implement agent list and management | Android Developer | 3h | CRUD operations for agents |
| 8.2.8 | Implement goal-to-agent linking | Android Developer | 2h | Link agent when creating/editing goal |
| 8.2.9 | Implement agent chat UI | Android Developer | 4h | Dedicated chat screen per agent |
| 8.2.10 | Write tests for agent builder | Android Developer | 3h | UI and integration tests |

**Milestone Exit Criteria**:
- [ ] Agent builder wizard complete
- [ ] Agents linkable to goals
- [ ] Chat UI functional

### Milestone 8.3: Built-in Templates & UX
**Goal**: Polish built-in agents and template system  
**Owner**: Android Developer + UX Designer

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 8.3.1 | Design agent card and list UI | UX Designer | 2h | Agent card spec, list layout |
| 8.3.2 | Design agent chat UI | UX Designer | 3h | Chat bubble styles, action buttons |
| 8.3.3 | Implement template browsing | Android Developer | 3h | Browse/preview/create from template |
| 8.3.4 | Refine system prompts for all 7 templates | Backend Engineer | 4h | Quality prompts with testing |
| 8.3.5 | Implement proactive agent suggestions | Android Developer | 3h | Agents suggest tasks/reminders |
| 8.3.6 | End-to-end agent testing | Android Developer | 4h | Full workflow tests |

**Milestone Exit Criteria**:
- [ ] All 7 templates polished
- [ ] Proactive suggestions working
- [ ] E2E tests passing

---

## Post-MVP Timeline Overview

| Phase | Weeks | Key Deliverables |
|-------|-------|------------------|
| v1.1 Features | 16-18 | Voice input, widget, advanced analytics |
| 7: Cloud AI | 18-22 | AI Gateway, model selection, hybrid routing |
| 8: Custom Agents | 22-26 | Agent builder, templates, marketplace |

---

## Post-MVP Resource Allocation

| Role | v1.1 | Phase 7 | Phase 8 |
|------|------|---------|---------|
| Product Manager | 40% | 50% | 50% |
| UX Designer | 20% | 20% | 40% |
| Android Developer | 100% | 60% | 80% |
| Backend Engineer | 10% | 100% | 100% |
| Marketing | 30% | 30% | 30% |
| Security Expert | 10% | 20% | 20% |

---

## Post-MVP Task Count

| Phase | Tasks | Estimated Hours |
|-------|-------|-----------------|
| v1.1 Deferred Features | 6 | ~21h |
| Phase 7: Cloud AI | 16 | ~51h |
| Phase 8: Custom Agents | 17 | ~58h |
| **Total Post-MVP** | **39** | **~130h** |

---

*Document Owner: Product Manager*  
*Last Updated: February 2026*  
*Status: Planned for Post-MVP*
