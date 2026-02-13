# Prio — Agent System Maintenance Guide

> How to keep the multi-agent development system effective as the project evolves.
> Read this when you need to update any agent configuration file.

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                      AGENT CONFIGURATION SYSTEM                      │
│                                                                      │
│  ┌──────────────────────┐     ┌──────────────────────────────────┐  │
│  │ .github/             │     │ Root Files                       │  │
│  │ copilot-instructions │────▶│ TEAM_AGENT.md  (roles, team)     │  │
│  │ .md                  │     │ CLAUDE.md      (orchestration)   │  │
│  │ (entry point)        │     │ AGENTS.md      (this file)       │  │
│  └──────────────────────┘     └──────────────┬───────────────────┘  │
│                                              │                       │
│                                              ▼                       │
│  ┌───────────────────────────────────────────────────────────────┐   │
│  │ docs/                                                          │   │
│  │ CONVENTIONS.md   — Code patterns extracted from real codebase  │   │
│  │ DECISIONS.md     — ADRs: resolved debates (don't repeat)       │   │
│  │ ARCHITECTURE.md  — System design (source of truth)             │   │
│  │ UX_DESIGN_SYSTEM.md — UI/UX standards                          │   │
│  │ SECURITY_GUIDELINES.md — Security rules                        │   │
│  │ ACTION_PLAN.md   — Sprint plan, milestones, task status        │   │
│  │ TODO.md          — Live bug list and backlog                   │   │
│  │ E2E_TEST_PLAN.md — Test matrix and results                    │   │
│  │ PRODUCT_BRIEF.md — Product vision and requirements             │   │
│  └───────────────────────────────────────────────────────────────┘   │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

### Information Flow

```
copilot-instructions.md  →  TEAM_AGENT.md  →  CLAUDE.md  →  docs/*
      (entry point)          (who you are)     (how to work)  (what to follow)
```

An agent reads `copilot-instructions.md` first (auto-loaded), which tells it to read `TEAM_AGENT.md` for role definition, which points to `CLAUDE.md` for working instructions, which points to `docs/*` for domain-specific details.

---

## When to Update Each File

### `TEAM_AGENT.md` — Update when:
- [ ] A new team role becomes active (move from Deferred to Active)
- [ ] Tech stack version changes (update §3 Tech Stack table)
- [ ] New feature module is created (update §3 Feature Package Structure)
- [ ] New reusable component is created (update §2 Component Library)
- [ ] Project phase changes (update §Current Project Status)
- [ ] Success metrics change (update §1 Supporting Metrics)
- [ ] Decision authority changes

### `CLAUDE.md` — Update when:
- [ ] Build commands change
- [ ] New code patterns are established
- [ ] New architectural rules are added
- [ ] New troubleshooting issues are discovered
- [ ] Module boundaries change
- [ ] Git conventions change

### `docs/CONVENTIONS.md` — Update when:
- [ ] New coding pattern is established (extract from real code, don't invent)
- [ ] Existing pattern changes (update the example to match)
- [ ] New testing pattern is used
- [ ] Naming conventions evolve
- [ ] New accessibility requirements added

### `docs/DECISIONS.md` — Update when:
- [ ] An architectural decision is made (add new ADR)
- [ ] An existing decision is revisited (add "Revisited" section)
- [ ] A decision is superseded (mark status, link to new ADR)

### `.github/copilot-instructions.md` — Update when:
- [ ] Team roles change (keep in sync with TEAM_AGENT.md)
- [ ] Tech stack headline changes
- [ ] Build commands change
- [ ] Critical rules change

### `docs/TODO.md` — Update continuously:
- [ ] When bugs are found → add to §BUGS §TO FIX
- [ ] When bugs are fixed → move to §BUGS §TO VERIFY with `[FIXED]` prefix
- [ ] When bugs are verified → remove from TODO
- [ ] When new features are needed → add to appropriate section

### `docs/ACTION_PLAN.md` — Update when:
- [ ] Task status changes (mark ✅, ⚠️, or update completion)
- [ ] New milestones are added
- [ ] Timeline changes
- [ ] New research findings emerge

---

## Principles for Effective Agent Configuration

### 1. Ground in Reality, Not Theory
Every pattern documented in `CONVENTIONS.md` must come from the actual codebase. Don't write aspirational patterns — extract them from working code.

```
❌ BAD:  "ViewModels should use Orbit MVI framework"  (not what we use)
✅ GOOD: "ViewModels use StateFlow + Channel<Effect> pattern" (extracted from TaskListViewModel.kt)
```

### 2. Specific Over Generic
Agents work better with concrete, project-specific guidance than generic best practices.

```
❌ BAD:  "Follow clean architecture principles"
✅ GOOD: ":core:data MUST NOT depend on :core:ui, :core:domain, or :app"
```

### 3. DRY Across Files
Don't duplicate information. Reference the source of truth.

```
❌ BAD:  Copying the full tech stack table into every file
✅ GOOD: "Tech stack versions — see TEAM_AGENT.md §3" (in CLAUDE.md)
```

### 4. Prevent Re-Debates
Every significant decision should be in `DECISIONS.md`. When an agent starts to question a resolved decision, it should find the ADR and stop.

### 5. Make the Happy Path Obvious
The most common task (fix a bug, add a feature) should have a clear step-by-step in `CLAUDE.md` §Common Tasks.

### 6. Keep Files Scannable
Use tables, headers, and code blocks. Agents parse structured content faster than prose paragraphs.

---

## File Size Guidelines

| File | Target Size | Rationale |
|------|-------------|-----------|
| `copilot-instructions.md` | < 80 lines | Entry point, must be fast to parse |
| `TEAM_AGENT.md` | 200–400 lines | Comprehensive but scannable roles |
| `CLAUDE.md` | 150–250 lines | Working reference, not a novel |
| `docs/CONVENTIONS.md` | 200–400 lines | Complete patterns with examples |
| `docs/DECISIONS.md` | Grows over time | Each ADR ~15 lines, no limit |

If a file grows beyond these targets, consider:
- Extracting sections into dedicated files
- Removing outdated content
- Consolidating redundant information

---

## Validation Checklist

After updating any agent file, verify:

- [ ] **Consistency**: Tech stack versions match across all files
- [ ] **Build commands**: Work when copy-pasted into terminal
- [ ] **Code patterns**: Match the actual codebase (grep to verify)
- [ ] **Cross-references**: File paths and section references are valid
- [ ] **No contradictions**: No file says the opposite of another
- [ ] **Roles aligned**: Role names match between `copilot-instructions.md` and `TEAM_AGENT.md`

---

## Multi-Tool Compatibility

The agent files are designed to work across multiple AI tools:

| Tool | Entry Point | Notes |
|------|-------------|-------|
| GitHub Copilot (VS Code) | `.github/copilot-instructions.md` | Auto-loaded per-workspace |
| Claude Code | `CLAUDE.md` | Auto-loaded at repo root |
| Cursor | `CLAUDE.md` + `.cursorrules` (if needed) | Respects CLAUDE.md |
| Windsurf | `CLAUDE.md` + `.windsurfrules` (if needed) | Respects CLAUDE.md |
| ChatGPT/Custom GPTs | Manual upload of `TEAM_AGENT.md` | Copy-paste context |
| Claude Web | Manual upload of `CLAUDE.md` | Copy-paste context |

### Creating Tool-Specific Overrides

If a specific tool needs different instructions:

```bash
# Cursor (if CLAUDE.md isn't enough)
cp CLAUDE.md .cursorrules
# Add tool-specific rules at the top of .cursorrules

# Windsurf
cp CLAUDE.md .windsurfrules
# Add tool-specific rules at the top of .windsurfrules
```

---

## Anti-Patterns to Avoid

### 1. Aspirational Documentation
Don't describe what you wish the code was. Describe what it is, then add TODOs for what it should become.

### 2. Copy-Paste Across Files
If information appears in multiple agent files, one is the source of truth and others reference it.

### 3. Stale Build Commands
Always verify build commands work before committing changes to agent files.

### 4. Role Confusion
Don't create roles for tech/features that don't exist yet. Defer them (see TEAM_AGENT.md §Deferred Roles).

### 5. Over-Documentation
If a convention is obvious from the code (e.g., Kotlin formatting enforced by IDE), don't document it. Focus on non-obvious decisions and project-specific patterns.

---

*This file is the meta-layer: documentation about the documentation system itself.
Update it when the system structure changes, not when project details change.*
