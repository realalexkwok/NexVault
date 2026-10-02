# 2.10 — Milestone scan & Phase 2 close (validation record)

> Status: **`[~]` — automatic half, agent-executed device half, and the owner-approved
> independent QR decode all recorded; what remains is the reviewer verification result the
> owner will send, then the owner close.** Branch `feature/2.10-milestone-scan-phase2-close`
> (owner decisions: `requirements.md` Q1–Q8, 2026-10-02; QR substitute + reviewer gate,
> 2026-10-02). Nothing here is written as "done" without its command and observed result.

## T0 — Spec & branch (DONE 2026-10-02)

- Branch: `feature/2.10-milestone-scan-phase2-close`, cut from `main` @ `321751b`
  (verified before the cut: `git log -1 --format=%h` → `321751b`).
- Spec directory: `specs/features/2026-10-02-2.10-milestone-scan-phase2-close/`
  (`requirements.md` Q1–Q8, `plan.md`, this file).

## T1 — Fresh automatic evidence

### Unit suite + gates + build

| # | Command (host `superguo-SQM2270`, `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`, `ulimit -n 32768`) | Observed |
| --- | --- | --- |
| 1 | `./gradlew testDebugUnitTest :domain:test detekt ktlintCheck :app:assembleDebug --continue` | **BUILD SUCCESSFUL** — 798 actionable tasks, **798 up-to-date** (this is the cache-check run; the following runs force real execution) |
| 2 | `./gradlew testDebugUnitTest :domain:test --rerun-tasks --continue` | **BUILD SUCCESSFUL in 8m 27s** — 576 tasks **executed**; XML aggregation (81 `TEST-*.xml` files): **473 tests, 0 failures, 0 errors, 1 skipped** |
| 3 | `./gradlew detekt ktlintCheck --rerun-tasks --continue` | **BUILD SUCCESSFUL in 33s** — 144 tasks **executed**; exit 0, zero findings |
| 4 | `:app:assembleDebug` (same invocations) | APK **`/home/superguo/Projects/NexVault/app/build/outputs/apk/debug/app-debug.apk`** |
| 5 | `./gradlew testDebugUnitTest :domain:test detekt ktlintCheck :app:assembleDebug --continue` on the final (post-fix) tree | **BUILD SUCCESSFUL in 1m 39s** — 798 tasks, 1 executed / 797 up-to-date (the real execution evidence for the post-fix tree is the after-fix run below: 119 executed, suite **473/0/0/1**, gates exit 0) |

Test-result roots cited: `domain/build/test-results/test/TEST-*.xml` (e.g.
`TEST-com.nexvault.wallet.domain.model.TokenTest.xml`) and
`<module>/build/test-results/testDebugUnitTest/TEST-*.xml` for the Android modules (e.g.
`feature/feature-send/build/test-results/testDebugUnitTest/TEST-com.nexvault.wallet.feature.send.QrDecoderTest.xml`).

### Instrumented suite on the closed revision (`29f1d41` tree)

- Device prep: Pixel 6a (bluejay), awake, AC powered, first colon-free adb serial taken
  with the standing recipe. No `com.nexvault.wallet.debug` package was installed, so the
  connected run installed fresh (no `pm clear` needed).
- `ANDROID_SERIAL=<mDNS serial> ./gradlew :app:connectedDebugAndroidTest` →
  **BUILD SUCCESSFUL in 1m**.
- Result file: **`app/build/outputs/androidTest-results/connected/debug/TEST-Pixel 6a - 17.xml`** —
  **11 tests, 0 failures, 0 errors, 0 skipped**: BackupPolicyTest 3/3,
  ConfirmationDialogTest 3/3, CreationFlowE2ETest 1/1, SendFlowE2ETest 3/3,
  ExampleInstrumentedTest 1/1. **This closes 2.8 handoff item 1 (and re-closes 2.7's
  earlier instrumented re-run handoff).**

## T2 — Milestone static-analysis scan

### Pre-fix server state (the 2.8-close analysis, read back before our scan)

`/api/measures/component?component=NexVault&metricKeys=...`:
ncloc **15,340** · bugs **0** · vulnerabilities **0** · security hotspots **0** ·
blocker/critical **0** · code smells **63** · duplication **0.5%** (116 duplicated lines,
4 blocks, 2 files) · coverage **56.4%**.

### Triage (before fixing)

- New-code issues since the pre-2.6 baseline (`createdAfter=2026-09-28`): **9** — eight
  `kotlin:S6310` (hardcoded dispatchers; 5 in `Web3jChainRpcClient`, 3 in
  `TransactionRepositoryImpl`) and one `kotlin:S107` (8-parameter
  `ConfirmationDialog`).
- Duplication attribution (per-file measures queried for all 176 tracked main-source
  Kotlin files): the only duplicated files are
  `core/core-network/.../BlockExplorerApi.kt` (**20** duplicated lines; the two list
  endpoints, pre-existing since the 2.0.4b V2 migration) and
  `data/.../TransactionRepositoryImpl.kt` (**96** duplicated lines). Git evidence: at the
  pre-2.8 revision those two status methods shared a 19-line identical block; at HEAD they
  share a 48-line block — the 0.3 → 0.5 rise is **2.8 code**, so per owner decision Q3 it
  must be refactored back.

### Fixes applied

| # | Finding | Fix | Verification |
| --- | --- | --- | --- |
| 1 | `kotlin:S107` new in `ConfirmationDialog` (8 params) | Bundled `title`/`confirmText`/`dismissText` into `ConfirmationDialogTexts`; updated `SendScreen`, `CoreUiComponentsTest`, `ConfirmationDialogTest` | re-scan closes the issue; suite below |
| 2 | Duplication in `TransactionRepositoryImpl` (2.8 code) | Extracted the duplicated detail/refresh body into one private `resolveTransactionStatus(...)`; both public methods are now one-line delegates | re-scan: file duplication 96 → **0** |
| 3 | Six pre-existing `kotlin:S1874` deprecated `hiltViewModel` imports (onboarding ×5, UnlockScreen) | Migrated to `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel` — the same fix 2.7 already applied to the other screens | re-scan closes 6 of 11 S1874 issues |

### Post-fix scan (the milestone scan)

Command (token passed on the command line, read from `local.properties`, never printed):

```bash
./gradlew sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.projectKey=NexVault -Dsonar.token=<local.properties>
```

Observed: **BUILD SUCCESSFUL in 1m 32s** (835 tasks, 69 executed). Measures read back
from `/api/measures/component?component=NexVault&metricKeys=...`:

| Metric | Pre-2.6 baseline | Pre-fix (2.8 close) | **Post-fix (2.10)** | Gate |
| --- | --- | --- | --- | --- |
| ncloc | 12,121 | 15,340 | **15,312** | — |
| bugs | 3 | 0 | **0** | ✅ |
| vulnerabilities | 5 | 0 | **0** | ✅ |
| security hotspots | 0 | 0 | **0** | ✅ no new |
| blocker violations | 0 | 0 | **0** | ✅ no new |
| critical violations | 0 | 0 | **0** | ✅ no new |
| duplicated lines density | 0.3 | 0.5 | **0.1** | ✅ not increasing (116 → 20 duplicated lines) |
| new duplicated lines (period) | — | 68 | **0** | ✅ |
| coverage | 0.0 | 56.4 | **56.4** (7,935 lines_to_cover) | measured only; 4.8 sets the bar |

### Findings disposition (nothing silently ignored)

- **Fixed:** the new `kotlin:S107` and the 2.8 duplication (table above).
- **Accepted, new-code (8 × `kotlin:S6310`):** dispatcher-injection refactor across
  `Web3jChainRpcClient` and `TransactionRepositoryImpl`. Already accepted in 2.6's close
  record (Task 0e/close) as **owner 4.8** debt; 2.10 carries that acceptance forward —
  the refactor is repo-wide, not a 2.10 change.
- **Accepted, pre-existing lower severity (owner decision Q2):** 47 remaining legacy
  smells — 17 × `kotlin:S6310` (owner **4.8**, same pattern), and 30 others
  (S107 ×6, S1874 ×5, S1172 ×6, S6524 ×4, S6532 ×4, S108 ×2, S6526 ×1, S1481 ×1,
  S6519 ×1) recorded as debt owned by **4.19** (repo close-out scan).
- Scan log warnings recorded, both expected and non-gating: missing Android Lint report
  files (lint reports are not part of this recipe) and missing SCM blame for the
  uncommitted 2.10 edits.

## T3 — Device walk

Pixel 6a (`bluejay`), awake, AC powered, first colon-free adb serial; orientation locked
portrait for the walk; the unrelated reading app's overlay was force-stopped and its
`SYSTEM_ALERT_WINDOW` denied because it kept stealing taps during the session.

- **Home:** cold start → Unlock (PIN 123456) → Home rendered live data: Total Balance,
  1D/7D/1M/3M/ALL chart ranges, Send/Receive/Swap quick actions (Swap still captioned
  "arrives in Phase 3"), token rows ETH/USDC/USDT with live 24h changes (+0.8%, +0.0%).
- **History walk (Ethereum):** History tab rendered the five filter chips
  (All/Sent/Received/Swap/Failed) and the empty state "No transactions yet" with its
  explanation — a real explorer-backed empty result for the walk wallet.
- **History walk (BSC plan gate):** the debug app's `selected_network` DataStore
  preference was set to `BSC` (original value backed up first), the app relaunched, and
  the History tab rendered the plan-gate state verbatim:
  **"Your explorer plan does not cover this network, so its history cannot be loaded."**
  plus **"Free API access is not supported for this chain. Please upgrade your api plan
  for full chain coverage. https://etherscan.io/apis"** — the owed 2.8 handoff item.
  Home also reloaded BSC tokens (BNB/BUSD/CAKE), proving chain persistence. The original
  preference was restored and Home verified back on Ethereum (ETH/USDC/USDT).
- **M5 crash watch:** logcat cleared before the walk; after Home/History/Receive walking,
  `adb logcat -d` contains **0** `FATAL EXCEPTION` lines and no
  `Process: com.nexvault.wallet.debug` crash records.
- **Instrumented re-runs:** 11/11 on the closed revision (T1) and **11/11 again after the
  2.10 fixes** (`:app:connectedDebugAndroidTest` → BUILD SUCCESSFUL; same result file,
  `app/build/outputs/androidTest-results/connected/debug/TEST-Pixel 6a - 17.xml`).
- **Receive screen / independent QR decode (MAN-001) — CLOSED 2026-10-02 by owner-approved
  substitute.** The owner has no second phone, so (owner decision) the QR was captured from
  the Pixel and decoded by an independent reader on the Linux host:
  `adb exec-out screencap -p > /tmp/nexvault-receive-sepolia-qr.png` (1080×2400 PNG,
  108,691 bytes) while the Receive screen showed "Sepolia Testnet (Testnet)", the full
  address `0xF5b235366Bf511437B633DcEFC2aC15f40000EFd`, Copy/Share, the testnet warning,
  and the QR node `content-desc="Wallet address QR code"`. Decode with **zbar 0.23.93**
  (`/tmp/qrdec/root/usr/bin/zbarimg`, extracted from the Ubuntu `zbar-tools` package, a
  QR implementation independent of ZXing):
  `zbarimg --quiet --raw /tmp/nexvault-receive-sepolia-qr.png` →
  **`0xF5b235366Bf511437B633DcEFC2aC15f40000EFd`** — exact match with the displayed
  address. Raw evidence: `/tmp/nv-qr-decoded.txt`. MAN-001 is `[x]` in
  `specs/pending-manual-test-cases.md`.
- **Funded row MAN-005 (2.6 TC-INT-002) — CLOSED 2026-10-02** on the owner's funding
  signal ("Transaction complete with tx hash …"). Evidence:
  - Faucet verification: Etherscan V2 `account/balance` for the current walk wallet
    `0xF5b235366Bf511437B633DcEFC2aC15f40000EFd` on chainid 11155111 returned
    `{"status":"1","message":"OK","result":"50000000000000000"}` (0.05 ETH), and the
    owner's faucet tx `0x0d78d7c222841bac9194ef789faab5196c185306b9519447d698f5a88a218b76`
    returned `gettxreceiptstatus` → `{"status":"1","result":{"status":"1"}}`.
  - Test fix decision (recorded per the test-vs-implementation rule): the implementation
    is right — AC-2.6 requires the review screen before the PIN dialog. The instrumented
    test was wrong: its funded branch clicked the form's "Confirm and send" and then
    waited for the PIN dialog, skipping the review screen's "Confirm" button. This branch
    had never executed until now because it self-gated on funds. Fixed in
    `app/src/androidTest/.../SendFlowE2ETest.kt` by clicking "Confirm" on the review
    screen before waiting for "Enter your PIN to sign and submit."; production code
    unchanged. Gates re-run after the test fix: `./gradlew detekt ktlintCheck --continue`
    → **BUILD SUCCESSFUL** (exit 0).
  - Funded run:
    `adb shell am instrument -w -r -e class 'com.nexvault.wallet.SendFlowE2ETest#sendFlow_submitsAndShowsTheHash' com.nexvault.wallet.debug.test/androidx.test.runner.AndroidJUnitRunner`
    → **OK (1 test)** in 9.128s; raw output
    `/tmp/nv-funded-send-run3.txt`.
  - On-chain verification: Etherscan V2 `account/txlist` (chainid 11155111, sort desc)
    returned the send row — hash
    **`0xc4db490381b8e92e277a8d6a86d9cdc3efddbd6213289f126972f5ed16298f83`**,
    from `0xF5b235…EFd` to `0x0000…0001`, value `100000000000000` (0.0001 ETH),
    `isError=0`, `txreceipt_status=1`; independent `gettxreceiptstatus` →
    `{"status":"1","result":{"status":"1"}}`.
  - Post-send balance re-check: `account/balance` →
    `{"status":"1","result":"49873221369448000"}` (0.049873 ETH after value + gas).
  - **MAN-005 is now `[x]` in `specs/pending-manual-test-cases.md`.** The remaining
    funded rows are MAN-002…MAN-004 (mainnet/Polygon/P4), still suspended.

## T4 — Phase 2 checklist (doc/08 AC-2.1–AC-2.8)

| AC | Row | Evidence for 2.10 |
| --- | --- | --- |
| AC-2.1 Multi-chain | Chain selector shows all chains; switching refreshes balances/tokens/history; selection persists | Legacy row 2.3 `[x]`; this session persisted `BSC` across force-stop/relaunch and Home + History both reloaded chain-scoped data; restored to Ethereum and verified |
| AC-2.2 Token balances | Native + ≥3 ERC-20 with fiat, 24h change, total, refresh | Home walk above (ETH/USDC/USDT live rows, fiat values, 24h changes); prior device walks 2.0.4/2.0.5 and 2.4 tests |
| AC-2.3 Portfolio chart | Line chart + range buttons | Home walk rendered 1D/7D/1M/3M/ALL buttons; prior 2.0.5 walk saw a drawn chart |
| AC-2.4 Add custom token | Address → metadata confirmation → added row | 2.0.7 close evidence + Home VM tests (`HomeViewModelCoverageTest`) |
| AC-2.5 Token detail | Balance/fiat/chart, Send/Receive, recent tx | Phase 0 row 2.5 `[x]`; 2.8 wired See All → History and Receive (reviewer-verified `97da158`/`29f1d41`) |
| AC-2.6 Send | See the 2.6 AC matrix in its close record; **replans:** address book → 3.4 (Q1), QR scan → 2.7 (done), funded on-chain submit → **done 2026-10-02** (MAN-005 `[x]`, send tx `0xc4db4903…298f83` on Sepolia) | 2.6 close `[x]`; 417 tests; 10/10 instrumented at close; this session re-ran the now-11-test suite 11/11 twice and then the funded TC-INT-002 test 1/1 with on-chain verification |
| AC-2.7 Receive | QR/copy/share/warning; "scans with any QR reader" | Receive screen rendered and staged (above); zxing round-trip test + screen tests; **independent-reader proof closed 2026-10-02**: zbar (non-ZXing) decoded the captured Pixel QR to the exact displayed address (owner-approved substitute for a second phone, MAN-001 `[x]`) |
| AC-2.8 History | Chain-scoped list, date groups, 5 chips, pagination, detail, explorer link | 2.8 close `[x]` (473 tests incl. paging/receipt); this session: 11/11 on the closed revision, interactive History walk, and the BSC plan-gate notice all recorded |

## Handoff (what 2.10 leaves behind)

1. **Reviewer verification (owner will send the result)** — this branch has not had a
   reviewer pass yet. On the owner's reviewer result: record it in a "Reviewer
   verification" section here, then — after owner confirmation — flip 2.10 → `[x]`,
   mark Phase 2 closed, and update the roadmap current-position and AGENTS standing gate
   in the same pass.
2. **Funding-suspended rows (MAN-002…MAN-004)** — the single global register is now
   `specs/pending-manual-test-cases.md`: 2.0.4b M1 (mainnet history), M2 (Polygon
   history), P4 addresses. MAN-005 (funded Sepolia send) is closed; resume the remaining
   rows in one pass on the owner's funding signal.
3. **`kotlin:S6310` dispatcher-injection refactor** — accepted debt, owner **4.8**
   (2.6 close + this record).
4. **30 pre-existing lower-severity smells** (S107/S1874/S1172/S6524/S6532/S108/S6526/
   S1481/S6519) — accepted debt, owner **4.19**.
5. **Background status polling** — owner **3.5** (2.8 handoff, unchanged).
6. **Biometric-path device proof** — recorded as unavailable (no enrolled biometrics on
   the walk device); the crypto-bound path remains implemented and PIN-walked (2.6 close +
   owner Q5).

## Reviewer verification

**Pending — the owner will send the reviewer's result.** This branch has not had a
reviewer pass yet. On receipt, the result (checks, verdict, any findings and their
resolution) is recorded here before the close.

## Close status

**Not closed — waiting for the reviewer result the owner will send.** Automatic half:
recorded above (suite 473/0/0/1, both gates fresh exit 0, build green, instrumented 11/11
on both the closed and fixed revisions, scan gate green). Manual/device half: all items
closed — the History/BSC walks, M5 crash watch, MAN-005 (funded Sepolia send, on-chain
evidence), and MAN-001 (independent zbar decode, owner-approved substitute). On the
owner's reviewer result and confirmation: record it here, flip 2.10 → `[x]`, and mark
Phase 2 closed in the same pass.
