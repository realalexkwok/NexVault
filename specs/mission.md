# NexVault — Mission

> Constitution files in this directory: `mission.md` (this file), `tech-stack.md`,
> `roadmap.md`, `dev-environment.md`. Together with the repository-root `AGENTS.md`
> they are the authoritative specification for the project. `doc/` and `doc/prompts/`
> are a **read-only archive** that these files supersede.
>
> Initialized 2026-09-20 from `doc/01-PROJECT-OVERVIEW.md`,
> `doc/02-ARCHITECTURE-AND-TECH-STACK.md` and `doc/03-UI-UX-DESIGN.md`, corrected
> against the actual state of the code (see `tech-stack.md` §Drift corrections).

## What this project is

NexVault is a **non-custodial, multi-chain crypto wallet for Android**, built as a
personal-use and portfolio project. It manages assets, sends and receives tokens,
shows transaction history, connects to DApps over WalletConnect, displays NFTs, and
keeps key material on-device behind AndroidKeyStore — the architecture a production
self-custody wallet needs, implemented end to end in Kotlin and Jetpack Compose.

- Package / applicationId: `com.nexvault.wallet`
- Platform: Android only, `minSdk` 26, `targetSdk` / `compileSdk` 36
- 100% Kotlin, Jetpack Compose + Material 3, single Activity
- 20 Gradle modules: `app`, 6 × `core:*`, `domain`, `data`, 11 × `feature:*`

The wallet is **non-custodial**: the mnemonic is generated on-device, encrypted with a
key wrapped by AndroidKeyStore (Tink AEAD), and never leaves the device. There is no
NexVault backend and no account system.

## What it is not

- **Not a custodial or cloud wallet.** No server stores keys, mnemonics, or balances on
  the user's behalf. There is no NexVault API; the app talks directly to public RPC
  endpoints and explorer APIs.
- **Not a real-funds product at this stage.** Sepolia testnet exists in the supported
  chain list precisely so the app can be demonstrated without risking money. Release
  distribution is not a V1 goal.
- **Not multi-platform.** No iOS, web, or desktop target. Kotlin/Android only.
- **Not a multi-wallet manager.** One active wallet at a time in the current scope;
  account/multi-wallet management is not planned before Phase 4.
- **Not a DEX or a fiat on-ramp.** The swap screen (Phase 3) is a UI over a public
  aggregator quote API; NexVault never holds funds or routes orders.
- **Not a portfolio tracker for arbitrary addresses.** It tracks the wallet it created.

## Supported chains (initial scope)

| Chain | Chain ID | RPC source | Token standards |
| --- | --- | --- | --- |
| Ethereum Mainnet | 1 | Infura / Alchemy | ERC-20 / ERC-721 |
| Ethereum Sepolia | 11155111 | Infura / Alchemy | ERC-20 / ERC-721 |
| BNB Smart Chain | 56 | Public RPC | BEP-20 / BEP-721 |
| Polygon | 137 | Public RPC | ERC-20 / ERC-721 |

Sepolia is the demo chain: it is the one that can be exercised end to end without funds.

## The experience (target)

1. **Onboarding** — create a wallet (show a 12-word BIP-39 mnemonic, verify it) or
   import one; then set a PIN.
2. **Unlock** — PIN or biometric on every cold start, with an auto-lock timeout after
   the app is backgrounded.
3. **Home dashboard** — total portfolio value, a value-over-time chart, the token list
   with balances and fiat values, add-custom-token by contract address, and a chain
   switcher.
4. **Token detail** — price chart, balance, recent transactions for that token, and
   entry points into send/receive.
5. **Send** — form → review (gas options: slow / normal / fast) → submit → result, for
   both the native coin and ERC-20 tokens.
6. **Receive** — QR code plus copyable/shareable address, with the active chain shown.
7. **History** — date-grouped transaction list with filters and pagination, plus a
   detail screen with a view-on-explorer action.
8. **DApp browser** — WalletConnect v2 pairing, session approval, sign-request handling,
   and an active-session list.
9. **NFT gallery** — ERC-721 / ERC-1155 display with metadata and IPFS images.
10. **Swap** — quote and approval flow against a public aggregator, testnet-first.
11. **Settings** — security, network management, address book, recovery-phrase export.

Dark theme is the default and currently the only theme (`NexVaultTheme(darkTheme = true)`).

## Audiences

- **The owner**, as a working demonstration of Android + Web3 engineering practice:
  Clean Architecture, multi-module Gradle, Compose, Hilt, Room, key management.
- **A future contributor or reviewer** who needs to understand what is built, what is
  verified, and what is not — without reading 29 historical prompt files.
- **Not** a consumer audience: there is no store listing, no support channel, and no
  commitment to fund safety for third parties.

## Principles

- **Spec-driven.** This directory is the constitution. Behaviour changes start as a
  feature spec, not as a code edit and not as a prompt file. `roadmap.md` order is
  authoritative, and a roadmap item closes only when its `validation.md` records
  evidence.
- **Honest status.** "Implemented" and "verified" are different states, and the roadmap
  records them separately. The project's defining failure mode so far has been declaring
  tasks complete on the strength of written prompts — the Phase 2.0 gate in
  `roadmap.md` exists to pay that debt down before any new feature work.
- **Security first, and security is testable.** Mnemonics and private keys are never
  logged, never written unencrypted, and never leave the device. The security module's
  test suite must compile and pass before any security-adjacent item can close.
- **Offline-first.** Room is the read model; the network refreshes it. Screens render
  from cache and degrade visibly, never blankly, when RPC keys or connectivity are
  absent.
- **One direction of dependency.** Features depend on `domain` and `core:*`; only `data`
  implements repositories. No feature reaches into another feature or into `data`.
- **Human-in-the-loop.** Before any write to disk, an ask-user-question round covering
  requirements, plan, and validation must be answered by the owner.
- **Two-sided verification.** Every item needs an automatic half (Gradle tests, build,
  gates — with absolute artifact paths) and a manual half (the owner runs the app).

## Current reality (baseline 2026-09-20, kept honest on 2026-09-27)

Recorded here so no reader mistakes intent for achievement. The plain lines are the
2026-09-20 measurement; every `→` note is what has moved since, with its owning item.

- The app **compiles and packages** (`app/build/outputs/apk/debug/app-debug.apk`), and
  162 unit tests across 6 modules pass.
  → **299 tests / 0 failures / 0 errors / 1 skipped** as of 2026-09-27 (2.0.6; 274 at
    2.0.5's close) plus **5 instrumented device tests** — BackupPolicy 3, the TC-INT-001
    creation-flow E2E, and the app-context smoke test — green on the Pixel 6a.
  → **Phase 2.0 is closed `[x]`** (2.0.7, 2026-09-27): 2.0.1–2.0.6 verified, the Phase 0 rows
    re-graded, and every deferred item handed to a named Phase 2.6+ / Phase 4 owner in the
    consolidated Handoff.
  → **Phase 2 (core features) is closed `[x]`** (2.10, 2026-10-02): 2.6–2.8 and the 2.10
    milestone scan all verified, 473 unit tests green, instrumented 11/11, the funded Sepolia
    send verified on-chain, and the QR reader row closed; 2.0.4b's mainnet/Polygon funded
    rows stay suspended in `specs/pending-manual-test-cases.md`.
- The app has **never been run on a device or emulator** — no emulator is installed and
  no AVD exists. Every "the screen works" claim in the archive is therefore unverified.
  → **refuted 2026-09-21**: a physical Pixel 6a is attached, and the app has been built,
    installed, launched and walked through onboarding, unlock and the network-backed screens
    (2.0.2, 2.0.2b, 2.0.3, 2.0.4). 2.0.4b's funded history rows are still owed.
- The `core-security` test suite (64 tests) **does not compile** and has never run.
  → **fixed 2026-09-21** by 2.0.1: the suite compiles, 61 tests run, 1 is skipped.
- Both quality gates fail: 1656 ktlint violations across 178 files, 75 detekt issues
  against `maxIssues: 0`.
  → **fixed 2026-09-26** by 2.0.5: `./gradlew detekt ktlintCheck` → BUILD SUCCESSFUL
    (exit 0), detekt 65 → 0 and ktlint 1556 → 0 at that date (the 2026-09-20 numbers were
    75 / 1656). The item's device walk is still owed, and nothing runs the gates
    automatically until 4.3 adds CI.
- 4 of the 5 main tabs are placeholders; send / receive / swap / history callbacks in
  the main scaffold are empty functions.
  → still true; 2.0.3 disabled the dead controls with visible captions, and Phase 2.6–2.8
    implement the destinations.
- All five API keys in `local.properties` are placeholders, so every network-backed
  screen is expected to degrade until real keys are supplied.
  → **superseded 2026-09-25** by 2.0.4: real keys are supplied and are the supported path;
    a key-absent surface shows an explicit "Not configured" state instead of degrading
    silently.

See `roadmap.md` → **Phase 2.0** for the plan that closes these gaps.
