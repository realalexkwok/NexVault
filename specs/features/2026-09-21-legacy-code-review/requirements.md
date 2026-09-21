# Phase 1.9 — Legacy code review (pre-SDD, Tasks 1.1–2.5) — Requirements

> Feature spec (requirements). Owner interview **2026-09-21**: eight questions answered (Q1–Q8 below).
> Parent: the new roadmap gate "Phase 1.9 — Legacy code review", inserted before the Phase 2.0 remainder.
> Sibling files: `plan.md` (scan method), `tasks/` (15 per-task CR files), `validation.md` (evidence record).

## Goal

Tasks 1.1–2.5 were implemented by feeding `doc/prompts/01…15` to an agent, one file per task, with
**no code review** and no verification step. Before any further roadmap work proceeds, every one of
those 15 implemented features gets a recorded code review. The reviewer produces findings only; a
separate developer session fixes them.

## Owner decisions (2026-09-21)

| Q | Question | Decision |
| --- | --- | --- |
| Q1 | Where does the legacy CR gate sit in roadmap order? | **Before resuming Phase 2.0 — new gate.** Legacy CR is the only open entry point now; the Phase 2.0 remainder (2.0.2b first) resumes after it closes. |
| Q2 | How are the CR task files split? | **One md per task — 15 files** (1.1–2.5), mirroring `doc/prompts/01…15` one-to-one. |
| Q3 | What happens to a finding? | **Record-only.** The reviewer never fixes inline. Every fix is the developer session's work. |
| Q4 | Who does the review? | **Agent scans + owner signs off.** The agent walks each checklist with evidence; the owner's per-task sign-off is the manual half. |
| Q5 | What may this session write? | **Record files (md/txt) and test files/test projects for testing only.** Production code, build scripts, config: read-only. |
| Q6 | How are findings transferred and cleared? | **Delete after verified fix.** Findings stay in the task files as the transfer channel; when the developer session reports a fix and the reviewer verifies it, that finding row is deleted. |
| Q7 | Implementation pause | **Pause after plan approval** for the owner's model switch; resume on the owner's go-ahead. |
| Q8 | Where do the files live? | **`specs/features/2026-09-21-legacy-code-review/`**. `doc/prompts/` stays a read-only archive. |

## Scope

**In:** roadmap Phase 0 items 1.1–2.5 exactly — the pre-SDD implementations. Review targets are the
files in the **current tree** (post-2.0.1, post-2.0.2, post-dependency-upgrade G1–G5), not the
historical state.

**Out of scope:**

- Post-SDD work (2.0.1–2.0.2b, toolchain upgrade, dependency groups G1–G5) — already has specs in
  `specs/features/2026-09-20-2.0.0-stabilization/`.
- All Phase 2.6+, Phase 3 and Phase 4 feature work.
- Fixing anything (Q3/Q5).
- Quality-gate remediation (2.0.5's decision) and TC traceability (2.0.6) — findings that belong
  there are handed over, not resolved here.
- Re-verifying the G1–G5 dependency upgrades: their regression numbers are accepted evidence.

## Definition of done (per CR task)

1. The agent scan is executed per `plan.md` — every checklist item gets a verdict:
   `✓ pass`, `✗ finding`, or `n/a` with a reason.
2. Findings are recorded in the task file's findings table with `file:line` evidence and a severity.
3. The owner signs the task off; the sign-off is recorded in `validation.md` with the date.
4. Nothing is fixed by the reviewer (Q3).

The gate (Phase 1.9) closes when all 15 tasks are signed off. After that, the next undone roadmap
item proceeds — Phase 2.0 remainder, 2.0.2b first — singly or as owner-approved grouped items
(G1–G5 grouping precedent).

## Severity scale

| Severity | Meaning |
| --- | --- |
| **Blocker** | Feature unusable or unsafe as shipped — blocks the gate (e.g. the 2.0.2b class). |
| **Critical** | Security or fund-safety defect. |
| **High** | Functional defect on a main path, or data-loss risk. |
| **Medium** | Architecture/convention violation, missing tests, degraded behaviour. |
| **Low** | Style, naming, documentation. |

## Findings lifecycle (Q6)

1. The reviewer records a finding in the task file's findings table with evidence + an owning
   roadmap item (an existing Phase 2.0 / Phase 4 item where one fits; otherwise a proposed new
   Phase 4 item for owner approval).
2. The developer session consumes the findings tables and fixes production code.
3. The developer reports each fix; the reviewer verifies it (re-runs the checklist item / evidence
   command).
4. The reviewer **deletes the verified finding row**. The table is the live transfer channel — what
   remains in it is what still needs fixing.

Known defects already owned by roadmap items (e.g. 2.0.2b, the `TransactionRepositoryImpl` write
paths) are listed in each task's **Known notes** as pre-registered findings so the developer session
sees them; they are not re-created in the findings table.

## Acceptance criteria for the CR program

- **AC-1** 15 task files exist under `tasks/`, all linked from `tasks/README.md`.
- **AC-2** Every checklist item carries a verify-by anchor (file path, spec section, or command).
- **AC-3** Every finding carries severity + evidence + owning roadmap item.
- **AC-4** All 15 tasks reach owner sign-off recorded in `validation.md`; no `✗` verdict without
  a finding row.
- **AC-5** The reviewer changed no production code (`git status` evidence: only md/txt record files
  and test files/test projects).

## Handoff (owned by this item, for the next planning round)

- Findings handed to the developer session remain open until the fix-and-delete loop (Q6) completes;
  the Phase 2.0 remainder does not start while any **Blocker/Critical** finding rows exist.
- New Phase 4 items proposed by findings need owner approval at the task's sign-off.
