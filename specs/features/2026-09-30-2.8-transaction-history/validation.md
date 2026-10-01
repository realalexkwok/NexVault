# 2.8 Transaction history — validation record

Branch `feature/2.8-transaction-history` (from `main` bbe7f0e). Owner decisions in
`requirements.md`, technical plan in `plan.md`. Every claim carries its command and result.

## Automatic half

### Suite and gates

`JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew testDebugUnitTest :domain:test detekt ktlintCheck`
→ BUILD SUCCESSFUL, **473 tests / 0 failures / 0 errors / 1 skipped** (2.7 closed at 443; 2.8 adds 30:
11 repository paging/receipt tests, 7 history ViewModel tests + the grouping test, 4 history screen
tests, 4 detail ViewModel tests, 3 detail screen tests, and the token-detail actions test). The skip is the pre-existing `@Ignore`d
keystore test.

### The two roadmap shortcuts, closed with tests

| Shortcut | Fix | Tests |
| --- | --- | --- |
| `getTransactionHistory` ignored `page`/`pageSize` | maps onto the DAO's limit/offset query (`page 3, pageSize 20` → `offset 40`), clamps nonsense pages | `TransactionHistoryPagingTest` |
| `updateTransactionStatus` returned the cached row | asks the chain via the new V2 `transaction/gettxreceiptstatus`: receipt `1` → CONFIRMED, `0` → FAILED, no receipt → stays PENDING, rejection → `ExplorerApiException`, no key → `ApiKeyNotConfiguredException`, settled row → no network call | `TransactionHistoryPagingTest` |
| `refreshTransactionHistory` fetched one fixed 100-row page | forwards `page`/`offset` (default 20/page) so the screen can load more on scroll, capped at 50 pages | `TransactionHistoryPagingTest` |

### Reviewer findings (2.8) — resolution

| ID | Sev | Resolution |
| --- | --- | --- |
| F-2.8-1 | MAJOR | **Real bug, fixed.** The live no-receipt envelope is `{"status":"1","result":{"status":""}}` (re-probed 2026-09-30 against the V2 host), and the DTO mapped `"" == "1"` → `false` → the repository persisted **FAILED** for a transaction that has simply not been mined yet. `receiptSucceeded` now treats a blank inner status as **unknown** (`takeIf { it.isNotBlank() }`), so the row stays PENDING; the invented `result = null` fixture was replaced by the verbatim live shape and a second test keeps the null payload case. Covered by `TransactionHistoryPagingTest#a pending row stays pending on the live no-receipt envelope` (and asserts the DAO update never runs). |
| F-2.8-2 | MINOR | **Fixed, and the sibling dead affordance with it.** TokenDetail's "See All" was disabled with no route while the history destination existed; its **Receive** button was in the same state even though 2.7 shipped the receive screen. Both now navigate (`onSeeAllClicked` switches to the history tab, `onReceiveClicked` opens the receive route), and the stale caption "Receive and full transaction history arrive in Phase 2." is gone (string deleted). Pinned by `TokenDetailActionsTest#receiveAndSeeAll_bothNavigate`. |
| NIT (page overflow) | NIT | **Fixed** — the page is clamped (`MAX_PAGE_NUMBER = 10_000`) before the `Int` multiplication, so an absurd page can no longer produce a negative offset; `TransactionHistoryPagingTest#an absurd page cannot overflow the offset` pins it. |

The follow-up scan also surfaced four findings in the files this work touched, all fixed rather than
accepted: `kotlin:S1874` x2 in `TokenDetailScreen` (deprecated `hiltViewModel` import, non-mirrored
`ArrowForward` icon) and `kotlin:S108` x2 (the two swallowing catches in `TokenDetailViewModel` now
explain why a failed explorer refresh must not abort the local reload). Server after the fixes:
**coverage 56.4%**, 0 bugs, 0 vulnerabilities, 0 criticals, 63 code smells, **0 open issues in any 2.8
file**.

### SonarQube (server `NexVault`)

`./gradlew sonar` → BUILD SUCCESSFUL. Measures after the reviewer fixes: **coverage 56.4%**
(2.7 closed at 54.1; the first 2.8 scan read 55.3%),
lines_to_cover 7,951, **0 bugs, 0 vulnerabilities, 0 blocker/critical**, 67 code smells,
duplication **0.5%** (2.7: 0.3% — recorded, the 2.10 phase scan owns the duplication judgement),
**0 open issues in any 2.8 file**. Findings raised by the first scans were all fixed, none accepted:

| Rule | Where | Fix |
| --- | --- | --- |
| `kotlin:S1192` CRITICAL | `TransactionRepositoryImpl` — "Block explorer API is not configured" 3x | extracted `EXPLORER_NOT_CONFIGURED_MESSAGE` |
| `kotlin:S1172` | `HistoryViewModel.loadPage` unused `activeAddress` | parameter removed (the use case resolves the address) |
| `kotlin:S1481` x2 | `HistoryViewModel.refresh`/`loadMore` unused locals | guard rewritten as `if (address == null) return` |

### AC-2.8 matrix

| Criterion | Evidence |
| --- | --- |
| History shows the wallet's transactions on the selected chain | explorer page 1 on entry + chain switch reload (`HistoryViewModelTest`) |
| Date-grouped with sticky headers | `groupByLocalDate` Today/Yesterday/`MMM d, yyyy` + `LazyColumn.stickyHeader` (`HistoryViewModelTest`, `HistoryScreenTest`) |
| Filter chips (All, Sent, Received, Swap, Failed) work | `HistoryFilter.matches` + chip behaviour tests |
| Scrolling loads more (pagination) | `loadMore` fetches the next explorer page and appends; short page ends the list (`HistoryViewModelTest`) |
| Tapping a transaction opens a detail view with full info | `TransactionDetailScreenTest` (status, amount, date, from, to, block, hash) |
| "View on Explorer" opens the correct explorer page | intent assertion contains `etherscan.io/tx/<hash>` |

## Device half — OWED

`adb devices` is empty (the phone's wireless-debugging session has been down since the 2.7 close), so
the instrumented run on this revision **and the two 2.7 handoff items** (instrumented re-run, the
second-phone QR scan) are executed in the next device session and recorded here.

## Handoff

1. Funded history rows — the funding-suspended register in `specs/roadmap.md` (2.0.4b M1/M2). When the
   owner funds the wallet, the history tab is the screen that proves them; owner: **2.10**.
2. Background status polling — deliberately out of scope; owner: **3.5** (WorkManager sync).
3. Device half of this item plus the 2.7 handoff items — next device session (see above).

---

## Reviewer verification — fcf039a + 97da158 + 89f1762 (2026-09-30)

> Code-reviewer session. Every claim re-derived; two findings filed with live-probe proof.

### Fresh evidence (reviewer's own runs)

| # | Check | Command | Result |
| --- | --- | --- | --- |
| R1 | Suite + gates + build | `./gradlew testDebugUnitTest :domain:test detekt ktlintCheck :app:assembleDebug --continue --rerun-tasks` | **BUILD SUCCESSFUL, exit 0 — 798 tasks executed, 0 failed tasks; 470 tests / 0 failures / 0 errors / 1 skipped** (matches; per-file `@Test` counts sum to +27: repo 9, VM 7 incl. grouping, screen 4, detail screen 3, detail VM 4) |
| R2 | Instrumented | Pixel 6a (connected during the review): wake + `pm clear` + pinned serial → `:app:connectedDebugAndroidTest` | **11/11 pass** on this revision — also closes the 2.7 handoff item #1 (instrumented re-run) |
| R3 | AC-2.8 code checks | reads | paging maps page→offset with clamps, 50-page cap (`MAX_PAGES = 50`), fetch-on-scroll `loadMore`, chain-switch reload via `combine`, 5 chips (`HistoryFilter.matches`; CONTRACT_INTERACTION/APPROVAL match only All), `stickyHeader` date groups, detail screen with status/amount/date/from/to/block/hash, explorer link `$explorerUrl/tx/{hash}` with chain-correct frontends (etherscan.io / sepolia.etherscan.io / bscscan.com / polygonscan.com) |
| R4 | Sonar code-fixes | reads | S1192 → `EXPLORER_NOT_CONFIGURED_MESSAGE` (3 sites, 1 literal); S1172 → `loadPage` param removed; S1481 ×2 → guard rewrites — all present in the tree. Server-side measures (55.3%, 0 open issues) remain token-gated/developer-recorded |
| R5 | Masked live probe (receipt wire shapes) | `gettxreceiptstatus` with a confirmed-successful tx, a reverted tx, and a no-receipt hash (key length 34, never printed) | success → `result.status "1"`; revert → `"0"`; **no receipt → `{"status": ""}` (an object with an EMPTY string)** — see F-2.8-1 |

### Findings

| ID | Severity | Location | Finding | Proof |
| --- | --- | --- | --- | --- |
| F-2.8-1 | **MAJOR** | `EtherscanTxReceiptStatusResponse.receiptSucceeded` + `TransactionRepositoryImpl.updateTransactionStatus` | **Pending transactions get labelled FAILED and persisted as FAILED.** The real V2 no-receipt response is `{"status":"","message":"OK","result":{"status":""}}` — the DTO maps the empty string to `false` (`status == "1"`), the repository then writes FAILED, and the "no receipt → stays PENDING" branch requires `result == null`, which the real API never returns. Triggered by pull-to-refresh (`checkPendingReceipts`) and the detail screen's "Check status now". | masked live probe (R5); test fixture `TransactionHistoryPagingTest` seeds `result = null` for the pending case — the real wire shape is not seeded. Fix direction: `result?.status?.takeIf { it.isNotBlank() }?.let { it == "1" }` |
| F-2.8-2 | MINOR | `TokenDetailScreen` See All + caption; `MainScreen` | **The consolidated-handoff row "See All" (owner 2.8) is only half-closed.** The history tab and its nested graph landed, but TokenDetail's See All is still `enabled = false` with `onClick = { }` and no wiring to the new `history` route — a dead affordance next to a live destination. The token-detail caption (`token_detail_actions_coming_soon`) still claims full transaction history "arrives in Phase 2", which is now false. | file reads: `TokenDetailScreen.kt:236` (`enabled = false`), `strings.xml:7`; `97da158` wires only the tab graph |

### NIT

`TransactionRepositoryImpl.getTransactionHistory`: `offset = (page - 1) * pageSize` can overflow `Int` for absurd page values (e.g. `Int.MAX_VALUE` → negative offset). SQLite treats a negative OFFSET as 0, so the effect is benign; a `coerceIn` would make the clamp explicit.

### Device half status

Instrumented 11/11 **passed on-device during this review** (the phone reconnected). The interactive
walk items (History tab rendered on-device with real explorer responses, the BSC plan-gate notice on
the history screen, and the second-phone QR scan of the receive QR) could not be run: the owner was
actively using the phone (another app held the foreground) — they stay owed for the close walk and
remain in the record.

### Verdict

**PASS with findings — F-2.8-1 (MAJOR) should be fixed before the item closes.** The receipt
mislabel corrupts persisted transaction state from the two most user-visible actions; the rest of the
item (paging, chips, groups, detail, explorer links, gates, suite, device suite) verifies cleanly.
