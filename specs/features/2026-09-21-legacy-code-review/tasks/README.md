# Legacy CR task list — index

> Phase 1.9 gate. One file per pre-SDD implemented feature (Tasks 1.1–2.5), mirroring
> `doc/prompts/01…15` one-to-one. Rules: `../requirements.md` · method: `../plan.md` ·
> evidence: `../validation.md`.

## Status legend

| Mark | Meaning |
| --- | --- |
| `[ ]` | Not started |
| `[~]` | Scanning (agent working the checklist) |
| `[?]` | Findings recorded — awaiting owner sign-off |
| `[x]` | Closed (signed off; verified-fix finding rows deleted) |

## Tasks

| Task | Feature | Modules | Tests (2026-09-21) | Prompt | Status |
| --- | --- | --- | --- | --- | --- |
| [1.1](1.1-project-scaffolding.md) | Project scaffolding | whole build (20 modules) | 1 (app template stub) | `01` | `[ ]` |
| [1.2](1.2-design-system-theme.md) | Design system & theme | `core:core-ui` | 0 | `02` | `[ ]` |
| [1.3](1.3-security-module.md) | Security module | `core:core-security` | 61 (1 skipped) | `03` | `[ ]` |
| [1.4](1.4-datastore-preferences.md) | DataStore & preferences | `core:core-datastore` | 27 | `04` | `[ ]` |
| [1.5](1.5-domain-models-repositories.md) | Domain models & repository interfaces | `domain` | 63 | `05` | `[ ]` |
| [1.6](1.6-data-layer.md) | Data layer | `data` | 27 | `06` | `[ ]` |
| [1.7](1.7-onboarding.md) | Onboarding | `feature:feature-onboarding` | 30 | `07`+`08` | `[ ]` |
| [1.8](1.8-auth-unlock.md) | Auth / unlock | `feature:feature-auth` | 14 | `09` | `[ ]` |
| [1.9](1.9-main-scaffold-navigation.md) | Main scaffold & navigation shell | `app` | 1 (template stub) | `10` | `[ ]` |
| [2.1](2.1-network-module.md) | Network module | `core:core-network` | **0** | `11` | `[ ]` |
| [2.2](2.2-database-module.md) | Database module | `core:core-database` | **0** | `12` | `[ ]` |
| [2.3](2.3-chain-management.md) | Chain management | `domain` + `data` + `core:core-ui` | **0** | `13` | `[ ]` |
| [2.4](2.4-home-dashboard.md) | Home dashboard | `feature:feature-home` (+ domain/data tokens) | **0** | `14` | `[ ]` |
| [2.5](2.5-token-detail.md) | Token detail | `feature:feature-tokens` (+ domain/data tx) | **0** | `15` | `[ ]` |

Order is strict 1.1 → 2.5; a later task does not start before the previous one is closed, per the
roadmap order rule. "Tests" counts are from the roadmap evidence appendix E2/E2b (2026-09-21) and
`specs/tech-stack.md` §5.
