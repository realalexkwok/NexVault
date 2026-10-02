# 2.10 — Milestone scan & Phase 2 close (requirements)

> One roadmap item = one spec directory = one branch. Branch:
> `feature/2.10-milestone-scan-phase2-close`, cut from `main` @ `321751b`
> (the 2.8 close merge) on 2026-10-02. This record is the owner-decision table
> for the item; `plan.md` is the technical plan, `validation.md` the evidence.

## Scope

Roadmap 2.10, exactly as specified:

1. Run the milestone static-analysis scan (local SonarQube Community Build,
   `http://localhost:9000`, project `NexVault`) over everything Phase 2 built —
   the current post-2.8 tree — and gate it against the pre-2.6 baseline recorded
   in `specs/features/2026-09-28-2.6-send-transaction-flow/validation.md` Task 0.
2. Triage every finding: fix or accept-with-named-owner, nothing silently ignored.
3. Consume the deferred-work handoffs of 2.6 / 2.7 / 2.8 and the funding-suspended
   wake-up register in `specs/roadmap.md`.
4. Verify the Phase 2 checklist (`doc/08-ACCEPTANCE-CRITERIA.md` §Phase 2,
   AC-2.1–AC-2.8) with recorded evidence.
5. Record everything in this item's `validation.md`, flip roadmap 2.10 to `[x]`,
   mark Phase 2 closed, and update `AGENTS.md` / `roadmap.md` in the same pass.

## Entry state (measured 2026-10-02)

- Git: `main` @ `321751b` "Merge 2.8 Transaction history (owner close 2026-10-02)",
  working tree clean. No 2.10 spec or branch existed.
- SonarQube server: **26.9.0.129388, status UP** at `http://localhost:9000`.
  Current server measures for `NexVault`: ncloc **15,340**, bugs **0**,
  vulnerabilities **0**, security hotspots **0**, blocker/critical **0**,
  code smells **63**, duplication **0.5%**, coverage **56.4%** (lines_to_cover 7,958).
- Baseline recorded pre-2.6 (2.6 validation Task 0): ncloc 12,121, bugs 3,
  vulnerabilities 5, code smells 70, security hotspots 0, duplication **0.3%**,
  coverage 0.0 (coverage not gated until 4.8).
- Device: Pixel 6a (`bluejay`) connected over adb mDNS, **awake, AC powered**;
  real API keys present in `local.properties` (lengths checked, values never printed).
- Unit-suite standing claim: **473 tests / 0 failures / 1 skipped** at 2.8's close.

## Deferred work 2.10 MUST re-read (planning obligation)

- **2.6 handoff:** (1) funded Sepolia send + 2.0.4b M1/M2/P4 — wake-up register;
  2.10 consumes. (2) `kotlin:S6310` dispatcher-injection refactor — owner **4.8**.
- **2.7 handoff:** (1) instrumented re-run on the closed revision — **closed by
  2.8's reviewer R2** at the pre-fix revision; the post-fix re-run is 2.8's handoff
  item 1. (2) second-phone QR scan — owner **2.10**. (3) EIP-681 URIs — later
  enhancement, not Phase 2.
- **2.8 handoff:** (1) device items — instrumented re-run on the closed revision
  `29f1d41` (phone kept awake / plugged in), interactive History walk, and 2.7's
  second-phone QR scan — owner **2.10**. (2) funded history rows — wake-up
  register, owner **2.10** on the funding signal. (3) background status polling —
  owner **3.5**.
- **Pending-manual register** (`specs/pending-manual-test-cases.md`, adopted 2026-10-02):
  2.0.4b M1 (mainnet history), M2 (Polygon history), P4 (known-active addresses),
  2.6 TC-INT-002 (funded Sepolia submit), and the second-phone QR scan. Funding signal:
  not received — see Q4.

## Owner decisions (ask round 2026-10-02)

| Q | Decision | Choice |
| --- | --- | --- |
| Q1 scope | Full scope as specified | Scan → triage → checklist → device items → record → close Phase 2 |
| Q2 findings disposition | Fix all blocker/critical/security-hotspot findings plus **any issue in files introduced or touched by 2.6–2.8**; pre-existing lower-severity code smells may be accepted as named-owner debt | fix-new, accept-legacy |
| Q3 duplication | Triage first, then decide: locate the duplicated blocks; if the 0.3 → 0.5 rise comes from 2.6–2.8 code, refactor it back to ≤ 0.3%; otherwise present evidence for the owner's call | triage then decide |
| Q4 funding | **None funded.** All rows stay suspended in the wake-up register; 2.10 records them as still-suspended debt, never failed, never dropped | keep suspended |
| Q5 biometric path | Record the limitation as before: the walk device reports no enrolled biometrics; the crypto-bound path stays implemented and PIN-walked | record limitation |
| Q6 plan | Approve task groups and order **T0 → T1 → T2 → T3 → T4** | approved |
| Q7 device session | Agent executes the adb items (instrumented re-run, History walk, M5 crash watch); the **owner performs the second-phone QR scan** and reports back | split execution |
| Q8 validation | Fresh normal-run evidence: unit suite + `:domain:test`, both gates exit 0, `:app:assembleDebug` with absolute APK path, instrumented 11/11, scan gate green vs baseline with counts, full AC-2.1–2.8 matrix, owner's device report — only then close | accepted |

## Post-approval decision updates (2026-10-02)

- **Q7 update — QR substitute:** the owner has no second phone, so the manual half of
  AC-2.7 is satisfied by an **independent decoder on the Linux host** instead of a
  second phone: the Pixel's Receive QR is captured via `screencap` and decoded with
  zbar (not ZXing). Owner-approved 2026-10-02.
- **Q4 update — Sepolia funded:** the owner funded the current walk wallet
  `0xF5b235366Bf511437B633DcEFC2aC15f40000EFd` with 0.05 Sepolia ETH (faucet tx
  `0x0d78d7c2…8b76`) and the funded TC-INT-002 send was executed and closed (MAN-005
  `[x]`); mainnet/Polygon rows stay suspended.
- **Reviewer gate:** 2.10 will not be closed until the reviewer result is received from
  the owner, who will send it.

## Acceptance criteria

- **AC-2.10-scan** — a fresh `./gradlew sonar` runs against the local server with the
  project token passed on the command line; counts are recorded; the new-code gate
  holds: no new blocker/critical, no new security hotspots, and duplication not
  increasing (or an owner-recorded decision on the duplication triage).
- **AC-2.10-triage** — every finding is fixed in the tree and re-scanned, or accepted
  in `validation.md` with a named owner; no silent ignore.
- **AC-2.10-checklist** — every row of `doc/08` AC-2.1–AC-2.8 is checked against
  evidence (fresh runs or the named close record), including the replanned rows
  (address-book pick → 3.4, QR scan → 2.7, BSC plan gate → 2.0.4b behaviour).
- **AC-2.10-device** — instrumented suite re-runs 11/11 on the closed revision; the
  History walk and M5 crash watch are executed and recorded; the owner's second-phone
  QR scan report is recorded; suspended funded rows stay in the wake-up register.
- **AC-2.10-close** — `validation.md` exists with commands, observed results and
  absolute artifact paths; roadmap 2.10 → `[x]`, Phase 2 marked closed, AGENTS.md and
  the handoff/wake-up records updated in the same pass.

## Constraints (binding)

- No dependency or behaviour change outside the constitution; a needed change goes in
  this spec first.
- Agents do not `git commit` / `git push`; changes stay in the working tree for owner
  review. Close is owner-directed per AGENTS.md.
- `detekt` / `ktlintCheck` stay the standing gates; the scan is additional, never a
  replacement. Coverage is measured, not gated (4.8 sets the bar).
- Never print the Sonar token, API keys, mnemonics, PINs, or private keys.
