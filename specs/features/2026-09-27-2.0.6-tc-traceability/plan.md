# 2.0.6 — TC traceability (plan)

> Approved by the owner 2026-09-27 (plan-mode review; Q1–Q5 answered in `requirements.md`).
> Deviations from the approved plan are recorded in `validation.md`.

## Order of work

1. Spec scaffold + matrix draft from the test-file inventory (no code).
2. New unit tests: TC-NET ×3, TC-DB ×3 (Robolectric), HomeViewModel ×2, TokenDetailViewModel,
   lockout, CreateWalletUseCase, PIN-hash ×2, true-checksum case — each mapped method marked.
3. Marker pass on existing mapped tests (3 recorded strengthenings + 1 rename).
4. `traceability.md` finalise + registrations + roadmap 4.18 row + CR task rows + 2.0.2b replan.
5. TC-INT-001 instrumented E2E (last — device-dependent; everything else green first).
6. Gates + full suite + TC audit; fix anything red.
7. `validation.md` evidence → owner review → owner-directed close.

## Verification

- **Automatic:** `./gradlew testDebugUnitTest :domain:test --continue` → BUILD SUCCESSFUL, totals
  reconciled; `./gradlew detekt ktlintCheck --continue` → exit 0; `:app:assembleDebug` with
  absolute APK path; `connectedDebugAndroidTest` → BackupPolicyTest 3 + CreationFlowE2ETest 1 on
  the first connected device; the three-way TC audit (doc/07 ids == markers ∪ registered).
- **Manual:** production code is untouched except the one FR-named fix, so the manual half is the
  owner confirming the device E2E run / a no-regression walk — recorded honestly.

## Edge cases

- Room's KSP-generated impl not resolvable under Robolectric → fall back to instrumented DAO tests
  or explicit re-plan (recorded, not silent). *(Did not occur — Robolectric path works.)*
- Robolectric on JDK 17 needed `--add-opens=java.base/jdk.internal.access=ALL-UNNAMED` for its
  FileDescriptor interceptor — added to `core/core-database` `testOptions` with a comment.
- E2E clears app data at start (documented consequence for the device); asserts rendering, never
  live prices; adb may list the phone twice → first-connected-device rule.
- `@Ignore`d keystore tests get no marker — registered (4.13), never claimed.
- `doc/07` drift is recorded in the matrix; `doc/07` is not edited.
