# 2.0.6 — Build the TC traceability

> Feature spec (requirements). Planning round **2026-09-27**: five ask-round questions answered by
> the owner (Q1–Q5 below). Branch: `feature/2.0.6-tc-traceability`. Parent roadmap item: Phase 2.0,
> sub-item 2.0.6. Sibling records: `plan.md`, `traceability.md`, `validation.md`.

## Goal

`doc/07-TEST-CASES.md` specifies 52 cases (TC-SEC 8, TC-SECTEST 5, TC-UC 4, TC-REPO 5, TC-NET 3,
TC-DB 3, TC-VM 10, TC-UI 10, TC-INT 4), and no test method referenced a TC id — nobody could tell
which specified behaviours were actually covered. This item makes every TC id land in exactly one
auditable bucket:

1. **Mapped** — a concrete test method carrying a `// TC-XXX-NNN` marker directly above its `@Test`.
2. **Registered** — declared unimplemented in `traceability.md` with a named owning roadmap item.

## Owner decisions (2026-09-27)

| Q | Question | Decision |
| --- | --- | --- |
| Q1 | How much new test writing beyond the mapping? | **Unit gaps only** — TC-NET ×3, TC-DB ×3 (Robolectric), HomeViewModel ×2, lockout, CreateWalletUseCase, TokenDetailViewModel tests. Instrumented/UI/E2E gaps (except TC-INT-001) are registered to owning items. |
| Q2 | How does a test method reference its TC id? | **One-line comment marker** `// TC-XXX-NNN` above each mapped `@Test`. Existing `TC-UNLOCK-*` names stay; they are aliased in the matrix. |
| Q3 | Where does the authoritative mapping live? | **New spec dir** `specs/features/2026-09-27-2.0.6-tc-traceability/` (`traceability.md` is the 52-row register). `doc/07` stays frozen as the definition source. |
| Q4 | How do the 3 Room DAO cases execute? | **Robolectric JVM tests** in `core/core-database/src/test` — first use of the catalog's declared-but-unused Robolectric entry. |
| Q5 | Add the TC-INT-001 creation-flow E2E now? | **Yes** — instrumented Compose E2E in `app/src/androidTest`, run on the owner's Pixel. |

## Handoff re-read — every entry that names 2.0.6 (explicit verdicts)

| Source | Deferred entry | Verdict (implement / replan / cancel) |
| --- | --- | --- |
| Legacy CR finding 1.2-5 (Medium, core-ui untested) | Component-level coverage of `core-ui` | **Split.** The VM/unit residue is covered by this item's new tests where a JVM home exists; the composable residue (TC-UI-003 `PinInputField` + the other four screen-level TC-UI cases whose screens exist) is **replanned to 4.18** (new roadmap row, below). |
| Legacy CR finding 2.1-1 (Medium, no tests, TC-NET) | Adapter tests for `core-network` | **Implemented.** `BigDecimalAdapterTest` + `AddressValidatorTest` (TC-NET-001…003). |
| Legacy CR finding 2.2-1 (Medium, no tests, TC-DB) | DAO tests for `core-database` | **Implemented.** `NexVaultDaoTest` under Robolectric (TC-DB-001…003). |
| Legacy CR finding 2.4-1 (Medium, no tests, TC-VM/TC-UI) | `feature-home` VM + UI tests | **Split.** `HomeViewModelTest` (TC-VM-001/002) implemented; the TC-UI-004/005 screen tests replanned to **4.18**. |
| Legacy CR finding 2.5-1 (Medium, no tests, TC-VM/TC-UI) | `feature-tokens` VM + UI tests | **Split.** `TokenDetailViewModelTest` implemented (no TC id exists in doc/07 — matrix appendix); screen tests replanned to **4.18**-style coverage owned by 2.6/2.8 as their screens land. |
| 2.0.2b handoff "Instrumented UI tests for the onboarding screens (incl. the grid layout that D1 crashed on)" | TC-UI has no home | **Replanned to 4.18** — the new roadmap row owns instrumented UI tests for every screen that already exists (Welcome, CreateWallet, PinInput, Home, …), the onboarding grid included. |

## Functional requirements

- **FR1** `traceability.md` carries the authoritative 52-row matrix: id → doc/07 section → mapped
  method (`file:Class.method`) or "unimplemented" → owning roadmap item. Zero orphan ids.
- **FR2** Every mapped test method carries a `// TC-XXX-NNN` marker directly above its `@Test`;
  no test-logic edits beyond the three recorded strengthenings (SEC-001 validate, VM-006
  non-empty words, VM-008 reset) and one renamed misleading test (SEC-004).
- **FR3** The unit gaps are written: TC-NET-001…003, TC-DB-001…003, TC-VM-001/002, TC-VM-009,
  TC-UC-001, TC-SEC-003 (true checksum case), TC-SEC-007/008 (PIN hash), `TokenDetailViewModelTest`,
  `RefreshTransactionHistoryUseCaseTest`, `CreateWalletUseCaseTest`.
- **FR4** TC-INT-001 is an instrumented Compose E2E in `app/src/androidTest` that clears app data
  at start, drives create → verify mnemonic → set PIN → Home, and asserts rendering — never live
  prices, so the test is network-independent.
- **FR5** Every registered gap names a roadmap item as owner; the registration is the transfer
  channel (see `traceability.md` §Registered and `validation.md` Handoff).
- **FR6** Both quality gates stay green and the full unit suite stays green with the new totals
  reconciled; `doc/07` is not modified.

## The one production change (deviation from AC-7, owner-visible)

`RefreshTransactionHistoryUseCase.invoke` returned `Unit` and swallowed the repository's
`DataResult`, which made `TokenDetailViewModel`'s `result is DataResult.Error` branch dead code —
the 2.0.4 "Not configured" and 2.0.4b plan-gate notices on the token detail screen could never
appear (exactly the states 2.0.4b's owner-pending M3/M4 checks would have exercised). The fix
returns the repository result (`DataResult<Unit>`), plus `WalletNotFoundException` for the no-wallet
cases; `TokenDetailViewModel` was already written against that contract. Regression-locked by
`RefreshTransactionHistoryUseCaseTest` and the two new `TokenDetailViewModelTest` notice cases.

## Out of scope

- Instrumented UI tests beyond TC-INT-001 (Q1; the rest are registered, see FR5).
- Deep Web3j-mocked balance/metadata tests for TC-REPO-001/002 — web3j is a final class and cannot
  be mocked with the current stack; registered to 4.8 (the data-layer coverage bar).
- Editing `doc/07` — it stays the frozen definition source; where its API names drifted
  (`AddressAdapter`, `WalletSecureStorage`, `CreateWalletUseCase()`), the matrix records the real
  home and the deviation instead.
- The 2.0.4b funded-history and owner-pending manual checks — untouched, still 2.0.4b's debt.

## Acceptance criteria

- **AC-1** The 52-row matrix is complete and audit-proven: ids extracted from `doc/07` == ids in
  `// TC-` markers ∪ ids in the registered table — three-way set equality, proof pasted.
- **AC-2** Every mapped method carries its marker; markers sit directly above `@Test`.
- **AC-3** `./gradlew testDebugUnitTest :domain:test --continue` → BUILD SUCCESSFUL with the new
  totals (274 + new) reconciled in the record.
- **AC-4** TC-INT-001 passes on the owner's device via `connectedDebugAndroidTest`.
- **AC-5** `./gradlew detekt ktlintCheck --continue` → exit 0.
- **AC-6** Every 2.0.6-owned handoff is explicitly implemented / replanned / cancelled above, and
  the owning items are named for every registered TC.
- **AC-7** No production behaviour change **except** the one FR-named use-case fix; `git diff` is
  tests + docs + that one file.
- **AC-8** `validation.md` written with commands, observed results, and absolute artifact paths.
