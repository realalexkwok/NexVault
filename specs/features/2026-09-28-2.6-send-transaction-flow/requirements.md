# 2.6 — Send transaction flow

> Feature spec (requirements). Planning round **2026-09-28**: five ask-round questions answered by
> the owner (Q1–Q5 below). Branch: `feature/2.6-send-transaction-flow`. Parent roadmap item:
> Phase 2 / 2.6 (acceptance `AC-2.6` in `doc/08`). Sibling records: `plan.md`, `validation.md`.

## Goal

Implement the three unimplemented `TransactionRepository` write paths — `estimateGas`,
`sendNativeTransaction`, `sendTokenTransaction` (today `UnsupportedOperationException`) — and the
full send UI: form → review → PIN-confirmed submit → result with "View on Explorer". Native coin
and ERC-20. This closes the 2.0.3 disabled Send buttons and the 2.0.6-registered TC ids that have
their home in this item.

## Owner decisions (2026-09-28)

| Q | Question | Decision |
| --- | --- | --- |
| Q1 | AC-2.6 mentions "via QR scan" and "pick from address book" — scope them here? | **Defer both.** Manual entry satisfies the AC ("manually or via QR"); QR scan lands with **2.7** (the receive/QR item, TC-UI-006 replanned there) and the address-book picker with **3.4** (its UI owner). No camerax adoption now. |
| Q2 | How does "confirmation requires PIN or biometric" work? | **PIN re-entry on the review screen** via `VerifyPinUseCase` (its KDoc already names transaction confirmation). Biometric confirmation is registered to 3.4/4.7. |
| Q3 | How is the full send flow (TC-INT-002) exercised? | **Sepolia testnet with faucet ETH** (no real money): the manual half walks a funded Sepolia send; if the faucet is unavailable the row carries forward honestly. |
| Q4 | Singly or grouped? | **2.6 alone.** |
| Q5 | The milestone-scan baseline precedes 2.6 but the scanner was deferred — resolve now? | **SonarScanner for Gradle as task 0**: catalog + root build script + tech-stack entry, then the baseline scan against the owner's local server. Token: owner-generated (the server's default credentials are no longer admin/admin). |

## Handoff re-read — every 2.6-owned row, explicit verdicts

| Source | Deferred entry | Verdict |
| --- | --- | --- |
| 2.0.4b Handoff | `TransactionRepositoryImpl` write paths (`estimateGas`, `sendNativeTransaction`, `sendTokenTransaction`) | **Implemented** (this item's core). |
| 2.0.3 Handoff | Re-enable Home + TokenDetail **Send** callbacks and restore the `MainScreen` wiring | **Implemented** (Receive/Swap stay disabled until 2.7/3.3). |
| 2.0.4 Handoff | Alchemy key still unused (failover/RPC switching) | **Replanned** — RPC failover is not needed for a single-provider send path; carried to **3.4** (network management). |
| 2.0.4 Handoff | Key-absent states on the send screen | **Implemented** — sending on chains 1/11155111 without `INFURA_API_KEY` fails with the explicit "Not configured" state, mirroring 2.0.4. |
| 2.0.6 traceability | TC-REPO-003/004, TC-VM-003/004/005, TC-UI-006, TC-UI-010, TC-INT-002 | **Implemented except TC-UI-006**, which is replanned to **2.7** (its QR button only exists once 2.7's scanner exists; Q1). TC-INT-002 is satisfied by the funded Sepolia walk (Q3). |

## Functional requirements

- **FR1** `estimateGas` returns slow/normal/fast `GasOption`s for native and ERC-20 sends: gas limit
  from `eth_estimateGas` (ERC-20 via an encoded `transfer(address,uint256)`), gas price tiers from
  `eth_gasPrice` multipliers (slow ×0.8, normal ×1.0, fast ×1.3), heuristic time names, and
  `fiatCost` from the stored native-token price when available (null otherwise).
- **FR2** `sendNativeTransaction` and `sendTokenTransaction` sign with the account's key
  (mnemonic-derived for HD wallets, stored key for imported wallets), submit via
  `eth_sendRawTransaction`, persist the transaction as **pending** in the Room DAO, and return the
  hash. Private-key bytes are wiped immediately after signing (verification stays 4.7's
  TC-SECTEST-002).
- **FR3** A thin RPC seam in `core:core-network` (`ChainRpcClient` + web3j implementation) is what
  `data` talks to, so the send paths are unit-testable without mocking the final web3j classes —
  this also unblocks the TC-REPO-003/004 tests that 2.0.6 registered to 4.8.
- **FR4** Repository-level validation: `InvalidAddressException` on a malformed recipient,
  `InsufficientBalanceException` when the balance cannot cover value + gas (TC-REPO-004), and
  `ApiKeyNotConfiguredException` for keyed chains without an RPC key (TC-REPO-003's guard twin).
- **FR5** `feature:feature-send`: form (recipient, amount with MAX, token picker from the selected
  chain's list), review, PIN-confirmed submit, result with hash + "View on Explorer"
  (`chain.explorerUrl + /tx/<hash>`). MAX fills balance minus estimated gas cost (TC-VM-005).
- **FR6** `core-ui` gains a generic `ConfirmationDialog` (title, body slot, confirm/dismiss —
  TC-UI-010) used by the send review for the PIN step; `PinInputField` is reused for the PIN entry.
- **FR7** Navigation: a send route in the app graph; Home's Send button and TokenDetail's Send
  button re-enabled (TokenDetail prefills the token); Receive/Swap stay disabled with their captions.
- **FR8** Task 0: SonarScanner for Gradle adopted (catalog + root build + tech-stack entry) and the
  **baseline scan** run against the local server, with the report recorded in `validation.md`.

## AC-2.6 mapping (in scope / deferred)

| AC-2.6 item | 2.6 disposition |
| --- | --- |
| Enter recipient manually or via QR scan | manual entry now; QR → **2.7** |
| Pick from address book | → **3.4** |
| Amount validates against available balance | FR4/FR5 |
| MAX fills maximum minus estimated gas | FR5/TC-VM-005 |
| Gas options with estimated time and fiat cost | FR1 |
| Review screen shows all details | FR5 |
| Confirmation requires PIN or biometric | PIN via `VerifyPinUseCase` (Q2) |
| Hash shown with "View on Explorer" | FR5 |
| Appears in history as pending, updates to confirmed | pending row persisted now (FR2); the history **screen** and status polling are 2.8/3.5 |
| Sending ERC-20 works | FR1/FR2 |

## Out of scope

QR scanning (2.7), address-book UI (3.4), biometric transaction confirmation (3.4/4.7), key-zeroing
verification (4.7), history UI and status polling (2.8/3.5), RPC failover/Alchemy (replanned to 3.4).

## Acceptance criteria

- **AC-1** `estimateGas` + both send paths are implemented behind the seam and unit-tested:
  TC-REPO-003 (success + pending row), TC-REPO-004 (insufficient balance), plus estimate-gas tests.
- **AC-2** SendViewModel passes TC-VM-003 (invalid address), TC-VM-004 (amount > balance),
  TC-VM-005 (MAX = balance − gas), and the key-absent state shows explicitly.
- **AC-3** The whole flow works on a device on **Sepolia with faucet ETH** (walk, manual half):
  form → review → PIN → submitted hash → "View on Explorer" → pending on the explorer.
- **AC-4** Both gates green; full unit suite green with the new totals reconciled; APK path
  recorded.
- **AC-5** The baseline Sonar scan is recorded (task 0), and no baseline regression is claimed
  without re-scanning.
- **AC-6** `validation.md` carries commands, observed results, absolute artifact paths, and the
  Handoff for anything left (Receive button, TC-UI-006 → 2.7, address book/QR notes).
