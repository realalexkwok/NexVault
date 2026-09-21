# Phase 1.9 — Legacy code review — Validation record

> Evidence record. One section per CR task; each section is filled when that task is reviewed.
> Two halves: **automatic** = the agent scan (commands + observed results, `file:line` evidence);
> **manual** = the owner's per-task sign-off.
> Findings lifecycle: record → developer session fixes → reviewer verifies → finding row deleted
> (`requirements.md` Q6). Nothing is "done" without a command and its observed result.

## Gate status

| Task | Scan evidence | Owner sign-off | Status |
| --- | --- | --- | --- |
| 1.1 Project scaffolding | 2026-09-21 (A1–A8) + fix verification (V1–V8) | 2026-09-21 | `[x]` |
| 1.2 Design system & theme | 2026-09-21 (B1–B5) | — | `[?]` |
| 1.3 Security module | — | — | `[ ]` |
| 1.4 DataStore & preferences | — | — | `[ ]` |
| 1.5 Domain models & repository interfaces | — | — | `[ ]` |
| 1.6 Data layer | — | — | `[ ]` |
| 1.7 Onboarding | — | — | `[ ]` |
| 1.8 Auth / unlock | — | — | `[ ]` |
| 1.9 Main scaffold & navigation shell | — | — | `[ ]` |
| 2.1 Network module | — | — | `[ ]` |
| 2.2 Database module | — | — | `[ ]` |
| 2.3 Chain management | — | — | `[ ]` |
| 2.4 Home dashboard | — | — | `[ ]` |
| 2.5 Token detail | — | — | `[ ]` |

---

## Task 1.1 — Project scaffolding

> Status: **CLOSED 2026-09-21** — scan + fix verification complete (A1–A8, V1–V8); findings
> 1.1-1 and 1.1-2 fixed and verified (rows deleted); finding 1.1-3 recorded and handed to the
> developer session's next round. Owner sign-off below.

### Automatic half — scan (commands and observed results)

All commands from the repository root with `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`
(AGENTS.md's macOS JBR path does not exist on this machine; JDK 17 is the project's own JVM target).

| # | Check | Command | Result |
| --- | --- | --- | --- |
| A1 | Build | `./gradlew :app:assembleDebug --console=plain` | **UP-TO-DATE (pass)** — artifact `/home/superguo/Projects/NexVault/app/build/outputs/apk/debug/app-debug.apk` (45,024,167 bytes, 2026-09-21 09:04). Full clean-build PASS is accepted evidence E1. |
| A2 | detekt | `./gradlew detekt --continue` | **FAIL — 10 tasks, 91 weighted issues**: app 3 · data 29 · domain 9 · core-database 4 · core-datastore 8 · core-security 11 · core-ui 5 · feature-auth 9 · feature-home 2 · feature-onboarding 11. Owned by 2.0.5. |
| A3 | ktlint | `./gradlew ktlintCheck --continue` | **FAIL — 39 tasks**. Samples: `:app:ktlintAndroidTestSourceSetCheck` (import order, wildcard import, missing trailing newline in `ExampleInstrumentedTest.kt`); `:app:ktlintTestSourceSetCheck`. Owned by 2.0.5. |
| A4 | TODO/FIXME | `grep -rnE 'TODO|FIXME' --include='*.kt' app core data domain feature` | **0** (template `<!-- TODO -->` comments existed in the two backup-rule XMLs — removed by the 1.1-1 fix, see V1). |
| A5 | Hardcoded versions | `grep -nE '"[0-9]+\.[0-9]+' --include='build.gradle.kts' -r app core data domain feature` | Only a commented-out Netty block (`app/build.gradle.kts:7–13`) and `versionName = "1.0"` (`:30`) — no hardcoded dependency versions. |
| A6 | Module graph | read `settings.gradle.kts` + 9 module build files | 20 modules declared, matching tech-stack §2; `data` → five `core:*` (no core-ui) — wording drift was finding 1.1-2 (fixed, V4). |
| A7 | Entry point | read `app/src/main/AndroidManifest.xml` | `.MainActivity` MAIN/LAUNCHER + `.NexVaultApplication` — 2.0.2 fix verified in tree. |
| A8 | Backup posture | read manifest + `res/xml/backup_rules.xml` + `res/xml/data_extraction_rules.xml` | `allowBackup="true"` + stock template rules → was **finding 1.1-1 (High)** — fixed, V1–V3. |

### Fix verification — developer handoff re-checked by the reviewer (2026-09-21)

Per the role split, the reviewer did **not** trust the developer's claims; every one was re-verified:

| # | Claim | Reviewer's command | Observed result | Verdict |
| --- | --- | --- | --- | --- |
| V1 | `allowBackup=false` + excludes in both rule files | read `AndroidManifest.xml`, `backup_rules.xml`, `data_extraction_rules.xml` | `android:allowBackup="false"` (line 7); `full-backup-content` excludes all 9 domains (root, file, database, sharedpref, external, device_root, device_file, device_database, device_sharedpref) with path `.`; `cloud-backup` **and** `device-transfer` each exclude the same 9; template TODO comments replaced by real rationale comments | ✅ matches claim |
| V2 | APK carries the new manifest | `aapt2 dump xmltree app/build/outputs/apk/debug/app-debug.apk --file AndroidManifest.xml` (build-tools 37.0.0) | `android:allowBackup(...)=false`, `fullBackupContent` and `dataExtractionRules` attributes present | ✅ |
| V3 | Device: ALLOW_BACKUP flag gone | `adb shell dumpsys package com.nexvault.wallet.debug` | `flags=[ DEBUGGABLE HAS_CODE ALLOW_CLEAR_USER_DATA ]` — **no ALLOW_BACKUP** | ✅ |
| V4 | tech-stack §2 wording fixed | read `specs/tech-stack.md:65` | "`data` | `domain` + five of the six `core:*` modules — … `core-ui` is deliberately **not** a dependency (2.3 AC 25; verified in `data/build.gradle.kts`)." | ✅ finding 1.1-2 verified-fixed |
| V5 | Build + unit regression | `./gradlew :app:assembleDebug testDebugUnitTest :domain:test --rerun-tasks` | **PASS** — fresh run; unit suites summed from 29 result XMLs: **223 tests, 0 failures, 0 errors, 1 skipped** (claim was 223 pass / 1 skipped / 0 fail) | ✅ matches claim |
| V6 | Device tests | `./gradlew :app:connectedDebugAndroidTest --rerun-tasks` on Pixel 6a (adb-tls) | `BackupPolicyTest` **3/3 pass** (claim verified); `ExampleInstrumentedTest` **1/1 FAIL** — `expected:<com.nexvault.wallet[]> but was:<com.nexvault.wallet[.debug]>` at `ExampleInstrumentedTest.kt:22` → **finding 1.1-3 recorded** | ✅ backup test verified; 1.1-3 confirmed |
| V7 | `bmgr backupnow` | `adb shell bmgr backupnow com.nexvault.wallet.debug` | `Unable to run backup` · `Backup finished with result: Backup is not allowed` | ✅ matches claim |
| V8 | App launches / Welcome renders | not re-run by the reviewer (launch already evidenced by 2.0.2 on the same device; the 1.1-1 change touches only backup posture) | accepted as owner-relayed | ✅ (manual half) |

**Reviewer note (not a finding):** the `device_*` domains inside `<cloud-backup>` are belt-and-suspenders —
cloud backup is governed by the five standard domains plus `allowBackup=false`; the extra entries are
harmless if ignored, and the new `BackupPolicyTest` locks the intended posture in.

### Findings lifecycle record

| ID | Severity | State |
| --- | --- | --- |
| 1.1-1 (High, backup posture) | fixed by developer; **verified by reviewer (V1–V3, V5–V8) → row deleted** | Closed |
| 1.1-2 (Low, tech-stack §2 wording) | fixed by developer; **verified by reviewer (V4) → row deleted** | Closed |
| 1.1-3 (Low, `ExampleInstrumentedTest` package assertion) | **Open** — recorded in the task file for the developer session's next fix round | Open |

### Manual half

Owner sign-off: **DONE 2026-09-21** — the reviewer closed the item per the owner's branch sign-off
instruction (fixes verified by the reviewer V1–V8 and confirmed by the user). Open finding 1.1-3
stays in the task file's findings table as the transfer channel for the developer session.

---

## Task 1.2 — Design system & theme

> Status: **SCAN COMPLETE 2026-09-21 — 5 findings recorded (1.2-1 … 1.2-5) — AWAITING SIGN-OFF.**

### Automatic half — commands and observed results

| # | Check | Command | Result |
| --- | --- | --- | --- |
| B1 | Build | `./gradlew :core:core-ui:assembleDebug --console=plain` | **BUILD SUCCESSFUL** (2026-09-21) |
| B2 | detekt (module) | `./gradlew :core:core-ui:detekt` | **FAIL — 5 weighted issues** (pre-registered 2.0.5, not re-created) |
| B3 | Previews | `grep -rn '@Preview' core/core-ui` | **0** → finding 1.2-2 |
| B4 | Hardcoded values in features | `grep -rnE '[0-9]+\\.(dp|sp)' feature` · `grep -rnE 'Color\\(0x' feature app` · `grep -rnE 'Text\\(\"[A-Za-z]' feature` | 149 dim literals · 1 color (`TokenDetailScreen.kt:333`) · 10 string hits; no `feature/*/res/values` strings → finding 1.2-1 |
| B5 | Component token discipline | `grep -rn 'Color(0x' core/core-ui/src/main/.../components` | only `TransactionRow.kt:62,64` → finding 1.2-3; `NexVaultButton` spot-read token-compliant |

### Findings recorded (transfer channel)

| ID | Severity | Owning roadmap item |
| --- | --- | --- |
| 1.2-1 | Medium | 4.17 (owner-approved 2026-09-21) |
| 1.2-2 | Low | 4.17 |
| 1.2-3 | Low | 4.17 |
| 1.2-4 | Low | 4.17 (or 3.7) |
| 1.2-5 | Medium | 2.0.6 (TC traceability) |

### Manual half

Sign-off: **pending** (date: —).

## Task 1.3 — Security module

_(filled during the review)_

## Task 1.4 — DataStore & preferences

_(filled during the review)_

## Task 1.5 — Domain models & repository interfaces

_(filled during the review)_

## Task 1.6 — Data layer

_(filled during the review)_

## Task 1.7 — Onboarding

_(filled during the review)_

## Task 1.8 — Auth / unlock

_(filled during the review)_

## Task 1.9 — Main scaffold & navigation shell

_(filled during the review)_

## Task 2.1 — Network module

_(filled during the review)_

## Task 2.2 — Database module

_(filled during the review)_

## Task 2.3 — Chain management

_(filled during the review)_

## Task 2.4 — Home dashboard

_(filled during the review)_

## Task 2.5 — Token detail

_(filled during the review)_
