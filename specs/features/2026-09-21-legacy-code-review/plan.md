# Phase 1.9 — Legacy code review — Plan

> Requirements: `requirements.md` (Q1–Q8 answered 2026-09-21).
> Evidence record: `validation.md`. Task files: `tasks/`.
> Role: this session is **reviewer-only** — production code, build scripts and config are read-only
> (Q5). Fixes belong to a separate developer session (Q3, Q6).

## §0 Inputs per task

Each CR task file was built from four measured inputs:

1. **Live file inventory** — `find <module> -name '*.kt' -not -path '*/build/*'`, measured 2026-09-21.
2. **Historical intent** — the matching `doc/prompts/0N…` file (scope + acceptance criteria), read-only.
3. **Constitution anchors** — `doc/08` AC rows, `specs/tech-stack.md` §2 binding rules + §3
   conventions, `specs/mission.md` security principles.
4. **Known-defect ledger** — `specs/roadmap.md` Phase 0/2.0/4 notes and
   `specs/features/2026-09-20-2.0.0-stabilization/validation.md` handoffs.

## §1 Scan method (per task)

1. Enumerate the task's files from the measured inventory (never from roadmap numbers).
2. Walk the checklist: for each item, read the named file(s) and record a verdict.
   - `✓ pass` — behaviour matches intent; note the evidence.
   - `✗ finding` — open a finding row: ID (`<task>-<n>`), severity, location `file:line`,
     finding, evidence, owning roadmap item.
   - `n/a` — with a one-line reason (e.g. "intent superseded by the 2.0.1 fix").
3. Findings that duplicate an already-owned roadmap item are **referenced, not re-created**
   (requirements: pre-registered findings in Known notes).
4. Verifying a developer fix: re-run the checklist item and its evidence; only then delete the
   finding row (Q6).

Order is strict: 1.1 → 2.5, one task at a time. Statuses live in `tasks/README.md`.

## §2 Checklist construction

Checklist items come from, in priority order:

1. The prompt's own acceptance criteria (translated to code-level checks).
2. The matching `doc/08` AC rows.
3. Binding conventions: feature modules never depend on `data`/other features; `DataResult` with
   no `Loading` variant; use-case pattern (`operator fun invoke`, `@Inject constructor`); Hilt
   rules; no hardcoded keys; KDoc.
4. Security principles: no logging of mnemonic/keys/PIN; secrets never plaintext on disk; HTTPS
   only; FLAG_SECURE on sensitive screens; byte arrays zeroed after use.
5. Recorded defects (roadmap/validation) — listed under each task's **Known notes** as pre-registered
   findings.

## §3 Evidence rules

- Every verdict and finding cites `file:line` or a command with its observed result. No
  evidence-free claims ("nothing to see here") — per the verification-debt rule.
- Test/build numbers quoted in task files come from the roadmap evidence appendix (E1–E8, measured
  2026-09-21) unless a checklist item requires a fresh run; any fresh run records the command and
  result in `validation.md`.
- The G1–G5 dependency-upgrade regression numbers are accepted as-is; the CR reviews logic and
  security, not the upgrades.

## §4 Risks

| Risk | Mitigation |
| --- | --- |
| Inventory drift (roadmap numbers stale) | Inventories measured 2026-09-21 with the exact `find` command recorded per task. |
| Overlap with 2.0.1 handoffs (4.11–4.13) | Referenced, not re-owned. |
| 2.0.2b defect duplicated by the 1.7/1.8 reviews | Pre-registered as a Blocker with owner 2.0.2b; not re-created. |
| Reviewer tempted to fix | Q3/Q5 forbid it; AC-5 checks `git status`. |
| Findings with no owning item | Proposed as new Phase 4 items; owner approves at sign-off. |
| Roadmap-vs-code mismatch (e.g. 2.9 seeding) | Recorded as a reconciliation finding; owner decides at sign-off. |

## §5 Order of work

1. Skeleton (requirements/plan/README/validation) — this pass.
2. Author the 15 task files — this pass.
3. Update `specs/roadmap.md` + `AGENTS.md` — this pass.
4. Execute reviews 1.1 → 2.5 — subsequent passes (agent scans).
5. Owner sign-offs; developer session fixes; reviewer deletes verified findings (Q6 loop).
6. Gate closes; Phase 2.0 remainder (2.0.2b first) proceeds.
