# 2.8 Transaction history — plan

Owner decisions in `requirements.md` (approved 2026-09-30).

## Deliverables

| # | Deliverable | Where |
| --- | --- | --- |
| 1 | `getTxReceiptStatus` V2 endpoint + `EtherscanTxReceiptStatusResponse` + a lenient object-envelope Moshi adapter | `core/core-network` |
| 2 | Paged `getTransactionHistory` (DAO limit/offset), paged `refreshTransactionHistory(page, pageSize)`, chain-consulting `updateTransactionStatus` | `data` + `domain` (interface, `GetTransactionHistoryUseCase`, `GetTransactionDetailUseCase`, `UpdateTransactionStatusUseCase`) |
| 3 | `HistoryViewModel` — wallet/chain streams, page-1 refresh, fetch-on-scroll (cap 50), five-chip filter, date grouping, pending receipt re-checks, explorer notices | `feature/feature-history` |
| 4 | `HistoryScreen` — sticky date headers, filter chips, `TransactionRow`, pull-to-refresh, load-more footer, empty/error/notice states | `feature/feature-history` |
| 5 | `TransactionDetailScreen` + VM — full fields, "Check status now" for pending, "View on Explorer" | `feature/feature-history` |
| 6 | History tab becomes a nested list→detail graph (placeholder removed) | `app/.../MainScreen.kt` |

No new dependencies.

## Verification

| What | How | Status |
| --- | --- | --- |
| Paging + receipt semantics | `TransactionHistoryPagingTest` (9 tests: page→limit/offset, page forwarded to the explorer, receipt 1/0/absent, settled row skips the chain, rejection, no key) | see validation.md |
| Grouping + filter + paging state | `HistoryViewModelTest` (7) incl. the pure `groupByLocalDate` Today/Yesterday/date labels | see validation.md |
| Screens | `HistoryScreenTest` (4), `TransactionDetailViewModelTest` (4), `TransactionDetailScreenTest` (3) | see validation.md |
| Suite + gates | `testDebugUnitTest :domain:test` + `detekt`/`ktlintCheck` | see validation.md |
| Explorer page size | 20 rows/page, capped at 50 pages (`MAX_PAGES`) per the owner decision | in code |
| Device | instrumented suite + the 2.7 handoff items (re-run, second-phone QR scan) | see validation.md |

## Deferred / handoff

- Funded history rows (2.0.4b M1/M2) stay suspended in the roadmap's funding register; when the owner
  funds the wallet, the history tab is the screen that proves them.
- Background status polling is **3.5**'s (WorkManager); 2.8 checks receipts on demand only.
