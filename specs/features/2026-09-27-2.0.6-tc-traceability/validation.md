# 2.0.6 — TC traceability (validation record)

> Status: **implemented on `feature/2.0.6-tc-traceability`; automatic half GREEN; the TC-INT-001
> E2E passed on the owner's Pixel 6a; the manual half (owner confirmation) is pending**, so the
> roadmap row is `[~]` until the owner-directed close. Every command below is reproduced with its
> observed result; nothing is claimed from memory.

## Automatic half — commands and observed results

### A1 — Full unit suite

`JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew testDebugUnitTest :domain:test --continue`
→ **BUILD SUCCESSFUL, exit 0** (2026-09-27; fresh run at 10:42, re-confirmed green in the final
combined run below).

Summed from the XML reports (`*/build/test-results/testDebugUnitTest/TEST-*.xml` +
`domain/build/test-results/test/TEST-*.xml`): **299 tests, 0 failures, 0 errors, 1 skipped**
(was 274 at 2.0.5's close — **+25**, attributed below).

| Module | Tests | Delta | What changed |
| --- | --- | --- | --- |
| `core-network` | 20 | +6 | `BigDecimalAdapterTest` (3) + `AddressValidatorTest` (3) — TC-NET-001…003 |
| `core-database` | 3 | +3 | `NexVaultDaoTest` (Robolectric) — TC-DB-001…003 |
| `feature-home` | 2 | +2 | `HomeViewModelTest` — TC-VM-001/002 |
| `feature-tokens` | 6 | +6 | `TokenDetailViewModelTest` — finding 2.5-1 |
| `domain` | 71 | +4 | `CreateWalletUseCaseTest` (TC-UC-001), `RefreshTransactionHistoryUseCaseTest` (3, the fix lock) |
| `feature-auth` | 15 | +1 | lockout test (TC-VM-009) |
| `core-security` | 64 | +3 | PIN-hash tests (TC-SEC-007/008) + the true-checksum case (TC-SEC-003); still 1 skipped (`@Ignore`d keystore test → 4.13) |
| others | unchanged | 0 | onboarding 35 (strengthenings, no new tests), data 53, datastore 29, app 1 |

**+25 = 6+3+2+6+4+1+3 exactly.** No test count moved without an explanation.

### A2 — Quality gates

`./gradlew detekt ktlintCheck --continue` → **BUILD SUCCESSFUL, exit 0** (final combined run
2026-09-27, 0 failed tasks). First attempt was not clean — the four findings and their fixes are
recorded, not hidden: ktlint `import-ordering` on the three new test files (the repo's convention
puts `java.*` imports **after** `org.junit.*`), ktlint `no-consecutive-blank-lines` and detekt
`MaxLineLength` on `RefreshTransactionHistoryUseCase.kt` (wrapped). Reports at
`<module>/build/reports/ktlint/` and `<module>/build/reports/detekt/`.

### A3 — APK

`:app:assembleDebug` → `/home/superguo/Projects/NexVault/app/build/outputs/apk/debug/app-debug.apk`,
**46,008,209 bytes** (2026-09-27 10:42; includes the one production fix in §A6). The unit-suite
and gates run is also the build proof: `testDebugUnitTest` compiled every module including `app`.

### A4 — The TC audit (AC-1/AC-2 proof)

Three sets, extracted by grep and compared with `comm`:

- **doc/07 ids:** 52 (`grep -oE 'TC-[A-Z]+-[0-9]{3}' doc/07-TEST-CASES.md | sort -u`).
- **Markers:** 25 (`// TC-XXX-NNN` in `src/test` + `src/androidTest`), **0 orphan markers** (none
  outside doc/07), **all 25 directly above their `@Test`** (verified line-by-line adjacency).
- **Registered:** the remaining 27 — exactly the `traceability.md` registered table
  (INT 3, REPO 5, SECTEST 5, UI 10, VM 4).

52 = 25 ∪ 27, disjoint, nothing dropped, nothing invented.

### A5 — Instrumented tests on the device (AC-4 proof)

Run: `adb shell pm clear com.nexvault.wallet.debug` (the debug variant's id — wipes any wallet,
**documented consequence**), device woken (`input keyevent KEYCODE_WAKEUP` + `wm dismiss-keyguard`),
then `ANDROID_SERIAL=<mDNS transport> ./gradlew :app:connectedDebugAndroidTest` →
**BUILD SUCCESSFUL, exit 0, 5/5 pass**: `BackupPolicyTest` 3/3, `CreationFlowE2ETest` 1/1
(create → read the 12 words off the screen → verify in order → PIN 123456 → **Home rendered**),
`ExampleInstrumentedTest` 1/1. XML:
`app/build/outputs/androidTest-results/connected/debug/TEST-Pixel 6a - 17.xml`.

Three real environment findings this run exposed, all recorded in `dev-environment.md` §5:

1. **Screen must be awake.** With the screen off, `MainActivity` is paused the instant it
   resumes and the Compose hierarchy never attaches → "No compose hierarchies found in the app".
2. **A serial containing `:` breaks the task verdict.** With the Wi-Fi TCP endpoint
   (`10.42.0.139:33001`) every test passed in the XML/pb yet the task failed: AGP 9.4.1's utp
   result listener wrote an **empty device id** into `test-result.pb`, so the run could not be
   attributed (confirmed by decoding the protobuf: every testcase `status=3` = `PASSED` in
   `TestStatusProto`). The colon-free mDNS transport (`adb-…-tls-connect._tcp`, same physical
   phone) works. The recipe prefers it.
3. **The device listing twice also races the run** — an unpinned run executed the suite on both
   transports concurrently and collided (install/uninstall conflicts). `ANDROID_SERIAL` pinning
   is mandatory.

### A6 — The production fix found by this item's tests (AC-7 exception, owner-visible)

`RefreshTransactionHistoryUseCase.invoke` returned `Unit` and swallowed the repository's
`DataResult`. In `TokenDetailViewModel`, `result is DataResult.Error` was therefore **dead code**:
`isHistoryConfigured` and `isHistoryPlanGated` could never leave their defaults — the 2.0.4
"Not configured" and 2.0.4b plan-gate notices on the token detail screen were unreachable states
(the same states 2.0.4b's owner-pending M3/M4 checks were meant to observe). The fix returns
`DataResult<Unit>` (plus `WalletNotFoundException` for the no-wallet cases), matching the
contract the VM was already written against. Locked in by `RefreshTransactionHistoryUseCaseTest`
(3 tests) and the two notice cases in `TokenDetailViewModelTest`. **Diff scope: exactly one
production file** (`git diff --name-only` vs main: everything else is tests, three build scripts,
and the spec/records).

### A7 — Diff shape (AC-7)

```
domain/src/main/.../RefreshTransactionHistoryUseCase.kt   (the §A6 fix)
3 build scripts (core-database, feature-home, feature-tokens — test deps/Robolectric opts)
13 test files modified (markers, 3 strengthenings, 1 rename), 8 test files/classes added
app/src/androidTest: CreationFlowE2ETest.kt added, ExampleInstrumentedTest.kt fixed
specs/: new 2026-09-27-2.0.6-tc-traceability/{requirements,plan,traceability,validation}.md,
        roadmap (2.0.6 row + 4.18), tech-stack §5, dev-environment §5, 5 CR task rows, 2.0.2b handoff
```

## Manual half — M1 (pending)

The creation flow the owner normally walks by hand is now automated **on the device** by
`CreationFlowE2ETest` (A5) — the manual half of this tests-only item is the owner reviewing the
branch and confirming the E2E result, then directing the close. The roadmap row stays `[~]` until
then. **Honesty note:** the device currently holds a wallet created by the E2E run (PIN `123456`,
mnemonic unknown to any record — the test generated it on screen and never logged it).

## Decisions and deviations (recorded, not silent)

| # | Decision | Why |
| --- | --- | --- |
| D1 | AC-7 exception: the use-case fix | The alternative was registering the dead-notice defect as a finding and leaving 2.0.4/2.0.4b behaviour broken; the fix restores behaviour the owner already approved in those items and is regression-locked |
| D2 | `ExampleInstrumentedTest` fixed (finding 1.1-3) | The template asserted the release package id and had been failing every connected run since 2.0.2; now asserts `BuildConfig.APPLICATION_ID` — a test-file edit that closes the open "developer follow-up" finding |
| D3 | 3 test strengthenings + 1 rename | SEC-001 (generated mnemonic re-validated), VM-006 (words non-empty), VM-008 (post-error reset asserted); `testInvalidMnemonicWrongChecksum` was actually a word-count case → renamed, and the real checksum case added as TC-SEC-003 |
| D4 | TC-REPO-001/002 registered → 4.8, not implemented | The deep form needs a mocked `Web3j` response and web3j is a final class unmockable with the current stack; partial evidence cited in the matrix instead of a hollow test |
| D5 | Robolectric adopted for core-database | First use of the catalog's declared-but-unused entry (tech-stack §5 flipped); needed `--add-opens=java.base/jdk.internal.access=ALL-UNNAMED` on JDK 17, added with a comment |
| D6 | doc/07 left frozen; API drift recorded in the matrix | `AddressAdapter`→`AddressValidator`, `WalletSecureStorage`→`AuthRepositoryImpl`+`SecurityUtils`, no-arg `CreateWalletUseCase()`→`(walletName, draft)` — each deviation notes the real home instead of editing the archive |

## Handoff (the registered 27 TCs — transfer channel for their owning items)

| Owner | Registered TCs |
| --- | --- |
| **2.6** | TC-REPO-003/004, TC-VM-003/004/005, TC-UI-006, TC-UI-010, TC-INT-002 |
| **2.8** | TC-REPO-005, TC-UI-008 |
| **3.1** | TC-INT-003 |
| **3.2** | TC-UI-007 |
| **3.3** | TC-VM-010 |
| **3.4** | TC-UI-009, TC-SECTEST-005, TC-INT-004 |
| **4.7** | TC-SECTEST-001/002/003 |
| **4.8** | TC-REPO-001/002 |
| **4.13** | TC-SECTEST-004 (+ the `@Ignore`d keystore half of TC-SEC-006) |
| **4.18** (new roadmap row) | TC-UI-001…005 (+ the 2.0.2b onboarding-grid handoff and the 1.2-5 composable residue) |

Each owning item's planning round must re-read this list (repo handoff rule). `traceability.md`
is the authoritative register with per-row notes.

## Reviewer handoff — requested 2026-09-27 (developer note, not a verdict)

The owner opened a code-reviewer session over the branch (commits `c7631f9` → `67eb1a8` → this
docs commit, the branch head). What it is asked to verify, in the 2.0.5 V1–V10 style — nothing
below is claimed as verified by the reviewer:

| # | Claim to check | Where the evidence is |
| --- | --- | --- |
| V1 | All 52 `doc/07` ids land in exactly one bucket: 25 markers ∪ 27 registered, zero orphans | §A4 audit commands + `traceability.md` |
| V2 | Every marker is a `// TC-XXX-NNN` comment directly above its `@Test` (25/25 adjacency) | §A4 line-by-line check |
| V3 | The production fix (`c7631f9`) is real: before it, `result is DataResult.Error` in `TokenDetailViewModel` was dead code because the use case returned `Unit`; after it the notices can actually appear | `git show c7631f9` + read `TokenDetailViewModel.loadRecentTransactionsSync` |
| V4 | The full unit suite is green with **299 / 0 / 0 / 1** from a fresh run | `./gradlew testDebugUnitTest :domain:test --continue; echo $?` |
| V5 | Both gates exit 0 from a fresh run | `./gradlew detekt ktlintCheck --continue; echo $?` |
| V6 | The instrumented suite passes 5/5 using the colon-free-serial recipe (screen awake, `pm clear` first) | §A5 + `specs/dev-environment.md` §5 |
| V7 | Diff scope: exactly one production file changed (`RefreshTransactionHistoryUseCase.kt`); everything else is tests, three build scripts, specs | `git diff --name-only main...HEAD` |
| V8 | Records match the tree: roadmap 2.0.6 row + 4.18, tech-stack §5 counts, the 5 CR task rows, the 2.0.2b handoff replan | read each file cited in §A7 |

## Close / sign-off — pending the owner

Automatic half: done and green (A1–A7). Manual half: owner confirmation pending. On the owner's
word this item closes as `[x]`: roadmap row flip, AGENTS.md standing gate → 2.0.7, mission.md
reality counters, `--no-ff` merge, push, branch delete — the 2.0.5 procedure.

---

## Reviewer verification — commits c7631f9 + 67eb1a8 + f4af934 (2026-09-27)

> Code-reviewer session (read-only on production code; record files only). Re-verified from the
> tree and by a fully fresh `--rerun-tasks` execution; nothing taken on trust. The developer's
> V1–V8 checklist is answered point by point.

### Verdicts

| # | Claim | Reviewer's check | Verdict |
| --- | --- | --- | --- |
| V1 | 52 ids / 25 markers / 0 orphans | re-extracted: `doc/07` ids **52**; `// TC-XXX-NNN` markers in `src/test`+`src/androidTest` **25**; `comm` against doc/07 → **0 orphan markers** | ✅ |
| V2 | Marker adjacency | `grep -rn -B1 '@Test'` … `grep 'TC-'` → **25/25** markers sit directly above their `@Test` | ✅ |
| V3 | The one production fix | `RefreshTransactionHistoryUseCase.invoke` returns `DataResult<Unit>` (was `Unit`); no-wallet → `DataResult.Error(WalletNotFoundException())`; the `TokenDetailViewModel` branches (`is DataResult.Error` → `isHistoryConfigured`/`isHistoryPlanGated`) were dead before the fix and are now exercised by `TokenDetailViewModelTest`'s two notice cases + `RefreshTransactionHistoryUseCaseTest` (3) | ✅ (the pre-fix tree compiled, so the constant-false branch was silently dead — exactly as claimed) |
| V4 | Suite 299 / 0 / 0 / 1 | fresh `--rerun-tasks` (660 tasks executed): **299 tests, 0 failures, 0 errors, 1 skipped** — matches the claim and the +25 attribution sums exactly (6+3+2+6+4+1+3) | ✅ |
| V5 | Both gates exit 0 | same fresh run included `detekt ktlintCheck` (13 detekt + 276 ktlint task lines): **exit 0, no failed tasks** | ✅ |
| V6 | Instrumented 5/5 | **Re-driven by the reviewer 2026-09-27** with the device reconnected: stale duplicate adb entry disconnected, screen woken + keyguard dismissed (`mWakefulness=Awake`), `pm clear`, `ANDROID_SERIAL=<mDNS transport> ./gradlew :app:connectedDebugAndroidTest` → **BUILD SUCCESSFUL, exit 0**. Fresh XML `TEST-Pixel 6a - 17.xml`: **5 tests, 0 failures** — `BackupPolicyTest` 3/3, `CreationFlowE2ETest` 1/1, `ExampleInstrumentedTest` 1/1 (the latter proving legacy CR finding **1.1-3** fixed — `BuildConfig.APPLICATION_ID` — row closed in the legacy records). | ✅ verified by reviewer |
| V7 | Diff scope | `git diff --name-only main...HEAD` filtered to `src/main` (excluding `res`): **exactly one production file** — `RefreshTransactionHistoryUseCase.kt`; everything else is tests, three build scripts and specs | ✅ |
| V8 | Records match the tree | roadmap 2.0.6 `[~]` + new **4.18** row + next-entry 2.0.7; tech-stack §5 counts (core-network 20, core-database 3, feature-home 2, feature-tokens 6); legacy CR rows 2.1-1/2.2-1/2.4-1/2.5-1 closed-by-2.0.6 with replans, 1.2-5 split verdict recorded | ✅ |

### Reviewer note (not a finding)

The pre-fix `TokenDetailViewModel` compiled with a `Unit`-typed `result` checked against
`DataResult.Error` — a constant-false branch the compiler accepted, which is why the 2.0.4b
notices never appeared and no build failure surfaced it. The fix and its regression tests are the
right remedy; this class of silently-dead branch is exactly what 2.0.6's mapping discipline
guards against going forward.

### Verdict

**PASS (automatic + diff + device).** The matrix is complete and disjoint (52 = 25 ∪ 27), every
marker is adjacent to its `@Test`, the single production change is justified and locked by tests,
the suite is green at 299/0/0/1 from a fresh run, both gates exit 0, the records match the tree,
and the reviewer re-ran the instrumented suite **5/5** on the Pixel 6a. Remaining for closure: the
owner-directed close (row flip, AGENTS.md → 2.0.7, mission counters, `--no-ff` merge, push,
branch delete).
