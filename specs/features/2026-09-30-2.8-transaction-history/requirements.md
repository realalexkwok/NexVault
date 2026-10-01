# 2.8 Transaction history — requirements (owner decision table)

Date: 2026-09-30. Branch: `feature/2.8-transaction-history` (from `main` bbe7f0e).

| Q | Decision |
| --- | --- |
| Q1 Filter chips | **AC-2.8's five** — All, Sent, Received, Swap, Failed. `CONTRACT_INTERACTION` and `APPROVAL` rows appear under All only. |
| Q2 Pagination | **Fetch-on-scroll from the explorer**, 20 rows per page, hard cap of 50 pages to protect the API key's rate limit. |
| Q3 Pending status | **On-demand receipt checks** — pull-to-refresh re-checks the loaded pending rows and the detail screen has a "Check status now" action. No background polling (3.5 Background sync owns that). |

Roadmap constraints honoured: the explorer is already on Etherscan V2 (2.0.4b), and the two shortcuts
the roadmap listed under 2.5/2.8 territory are closed here — `getTransactionHistory` ignoring
`page`/`pageSize`, and `updateTransactionStatus` returning the cached row without consulting the chain.

## Handoff from 2.7 (re-read at this item's planning)

1. Instrumented re-run on 2.7's closed revision — owed; the phone's adb session had dropped.
2. Second-phone QR scan of the receive QR (AC-2.7's reader-compatibility half) — owed.
   Both are executed in 2.8's device session and closed in 2.8's validation record.
