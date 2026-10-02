# NexVault — Pending Manual Test Cases (single register)

> **Authoritative, global register of every currently pending or suspended manual/device
> test case.** Adopted 2026-10-02 (owner decision). It supersedes any other manual-case
> list: the funding wake-up register that lived in `specs/roadmap.md` moved here, and the
> second-phone QR scan is registered here too.
>
> One row = one manual test case. A row is **never failed and never silently dropped**;
> "suspended" means the precondition (funding, a second phone, the owner's time) is
> missing, not that the case is optional. When a row is executed, record the command and
> observed result (plus the owner's report for owner-executed rows) in the owning item's
> `validation.md`, then mark the row `[x]` here with the date and evidence pointer — never
> delete it without that evidence.

## Status legend

| Mark | Meaning |
| --- | --- |
| `[ ]` | Not run — the row is pending its precondition or its owner session. |
| `[~]` | Partially run — some evidence exists, the row is not complete. |
| `[x]` | Complete — evidence recorded in the owning `validation.md`. |

## Binding rules

1. Every future manual/device test case is added to this file at planning time, in the
   owning roadmap item's `requirements.md` or `plan.md` round. No second manual-case list
   may drift out of sync.
2. **Funding signal:** when the owner signals funding in any wording ("funded", "funds
   ready", ...), resume every funded row below in one pass, in order, and re-read this
   file first.
3. **Resume evidence:** a funded row closes only when the specified chain actually returns
   the rows the case requires (balance alone is not enough), and the walk observation is
   recorded with a command/output or screenshot reference.
4. Completed rows stay listed with `[x]` and a pointer to their evidence until the end of
   the phase that owns them; the owner may then archive them to the owning
   `validation.md`.

## Pending rows

| ID | Case | Precondition | What to run | Owning record | Status |
| --- | --- | --- | --- | --- | --- |
| MAN-001 | Second-phone QR scan (AC-2.7 "scans correctly with any QR reader") | A second phone with any QR reader; the Pixel 6a shows the Receive screen | Scan the QR on the Receive screen (walk-wallet address `0xF5b235366Bf511437B633DcEFC2aC15f40000EFd`; the address is the same on every supported chain) and report the decoded text | `specs/features/2026-10-02-2.10-milestone-scan-phase2-close/validation.md` (Receive screen staged 2026-10-02) | `[x]` — **done 2026-10-02 by owner-approved substitute**: no second phone was available, so the owner approved decoding the Pixel's Receive QR on the Linux host with **zbar 0.23.93** (independent of ZXing); decoded text `0xF5b235366Bf511437B633DcEFC2aC15f40000EFd` matches the displayed address |
| MAN-002 | 2.0.4b M1 — mainnet history walk | Walk wallet's **Ethereum mainnet** balance > 0 with `txlist`/`tokentx` rows | TokenDetail/History history walk on a known-active mainnet address, per 2.0.4b `validation.md` §Suspended | `specs/features/2026-09-26-2.0.4b-etherscan-v2-migration/validation.md` | `[ ]` — suspended pending funding |
| MAN-003 | 2.0.4b M2 — Polygon history walk | Walk wallet's **Polygon** balance > 0 with `txlist`/`tokentx` rows | The same walk on Polygon (chainid 137) | same 2.0.4b `validation.md` | `[ ]` — suspended pending funding |
| MAN-004 | 2.0.4b P4 — known-active addresses | Rides on MAN-002/MAN-003's wallet | Record the active mainnet/Polygon addresses used and walk them | same 2.0.4b `validation.md` | `[ ]` — suspended with MAN-002/MAN-003 |
| MAN-005 | 2.6 TC-INT-002 — funded on-chain send | **Sepolia** faucet funds to the current walk wallet `0xF5b235366Bf511437B633DcEFC2aC15f40000EFd` (the 2.6-era `0x97B6…` wallet was replaced by the 2.10 instrumented re-run; PIN as recorded in the 2.6 validation Task 0f) | Re-run `SendFlowE2ETest#sendFlow_submitsAndShowsTheHash` on the device, then verify the transaction via the Etherscan V2 API | `specs/features/2026-09-28-2.6-send-transaction-flow/validation.md` Task 0f / §Handoff; closure evidence in the 2.10 `validation.md` §T3 | `[x]` — **done 2026-10-02**: faucet `0x0d78d7…8b76` funded 0.05 Sepolia ETH; send tx `0xc4db4903…298f83` (0.0001 ETH → `0x0000…0001`), Etherscan V2 receipt status `"1"`; instrumented test OK (after fixing the test's missing review-Confirm step) |

## Ownership of the next pass

MAN-001 and MAN-005 are closed. The remaining funded rows (MAN-002…MAN-004) are resumed in
one pass on the owner's funding signal, whichever roadmap item is current at that time.
The 2.10 close now waits only on the reviewer result the owner will send, then the owner
close.
