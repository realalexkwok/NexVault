# 2.8 Transaction history — validation record

Branch `feature/2.8-transaction-history` (from `main` bbe7f0e). Owner decisions in
`requirements.md`, technical plan in `plan.md`. Every claim carries its command and result.

## Automatic half

### Suite and gates

`JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew testDebugUnitTest :domain:test detekt ktlintCheck`
→ BUILD SUCCESSFUL, **470 tests / 0 failures / 0 errors / 1 skipped** (2.7 closed at 443; 2.8 adds 27:
9 repository paging/receipt tests, 7 history ViewModel tests + the grouping test, 4 history screen
tests, 4 detail ViewModel tests, 3 detail screen tests). The skip is the pre-existing `@Ignore`d
keystore test.

### The two roadmap shortcuts, closed with tests

| Shortcut | Fix | Tests |
| --- | --- | --- |
| `getTransactionHistory` ignored `page`/`pageSize` | maps onto the DAO's limit/offset query (`page 3, pageSize 20` → `offset 40`), clamps nonsense pages | `TransactionHistoryPagingTest` |
| `updateTransactionStatus` returned the cached row | asks the chain via the new V2 `transaction/gettxreceiptstatus`: receipt `1` → CONFIRMED, `0` → FAILED, no receipt → stays PENDING, rejection → `ExplorerApiException`, no key → `ApiKeyNotConfiguredException`, settled row → no network call | `TransactionHistoryPagingTest` |
| `refreshTransactionHistory` fetched one fixed 100-row page | forwards `page`/`offset` (default 20/page) so the screen can load more on scroll, capped at 50 pages | `TransactionHistoryPagingTest` |

### SonarQube (server `NexVault`)

`./gradlew sonar` → BUILD SUCCESSFUL. Measures: **coverage 55.3%** (2.7 closed at 54.1),
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
