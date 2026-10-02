# 2.10 — Milestone scan & Phase 2 close (plan)

> Owner decisions: `requirements.md` (Q1–Q8, 2026-10-02). This plan implements the
> approved task-group order **T0 → T1 → T2 → T3 → T4**.

## Host conventions

Linux build host (`superguo-SQM2270`), per `dev-environment.md` §8:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ulimit -n 32768
export PATH="$HOME/Android/Sdk/platform-tools:$PATH"
```

Device: first connected mDNS device (colon-free serial), the standing recipe from
`dev-environment.md` §5.

## T0 — Spec & branch (DONE)

- Branch `feature/2.10-milestone-scan-phase2-close` cut from `main` @ `321751b`.
- Spec directory `specs/features/2026-10-02-2.10-milestone-scan-phase2-close/`
  with `requirements.md`, `plan.md`, `validation.md`.

## T1 — Fresh automatic evidence

Commands, in order, on the closed revision:

1. `./gradlew testDebugUnitTest :domain:test --continue` — expect ~473 tests,
   0 failures, 1 skipped; record the XML result paths.
2. `./gradlew detekt ktlintCheck --continue` — record exit codes and report paths.
3. `./gradlew :app:assembleDebug` — record APK absolute path + size.
4. Device: `adb shell pm clear com.nexvault.wallet.debug` (required before the
   creation-flow E2E; no funded wallet exists, so nothing of value is wiped), wake
   the device, then
   `ANDROID_SERIAL=<mDNS serial> ./gradlew :app:connectedDebugAndroidTest` —
   expect 11/11 (BackupPolicyTest 3, ConfirmationDialogTest 3, CreationFlowE2ETest 1,
   SendFlowE2ETest 2, ExampleInstrumentedTest 1). This is the owed 2.8-handoff
   instrumented re-run on the closed revision.

## T2 — Milestone static-analysis scan

1. `./gradlew sonar -Dsonar.host.url=http://localhost:9000
   -Dsonar.projectKey=NexVault -Dsonar.token=$(grep '^SONAR_TOKEN=' local.properties | cut -d= -f2-)`
   (token passed on the command line, never echoed). The `sonar` task runs the Kover
   XML reports; delete stale `build/reports/jacoco` XMLs if the imported numbers look
   wrong (2.6 Task 0e trap).
2. Read back `/api/measures/component?component=NexVault&metricKeys=...` and compare
   with the pre-2.6 baseline: bugs 3, vulnerabilities 5, hotspots 0, duplication 0.3.
   Gate: **no new blocker/critical, no new security hotspots, duplication not
   increasing**; coverage measured (56.4% current) but not gated.
3. Findings triage per Q2:
   - Fix every blocker / critical / security hotspot.
   - Fix every issue whose file belongs to 2.6–2.8 work (send / receive / history /
     touched shared files).
   - Accept pre-existing lower-severity smells only with a named owner in
     `validation.md`.
4. Duplication triage per Q3: pull the duplicated-line density per file from the
   server API, separate files introduced/touched by 2.6–2.8 from legacy files,
   and — if the 0.3 → 0.5 rise comes from the new code — refactor until density is
   ≤ 0.3; if it is legacy, present the evidence for the owner's call before closing.
5. Re-scan after fixes; record both scans.

## T3 — Device walk (agent-executed items)

On the fresh debug build (real keys from `local.properties`), with the phone kept
awake / AC powered:

- **Instrumented re-run** — already produced in T1; recorded here as the 2.8 handoff
  item 1 closure.
- **Interactive History walk** — launch, unlock with the E2E wallet (PIN 123456),
  open the History tab, observe the rendered states (empty state on a chain where
  the explorer answers, chain switcher, detail screen if a row exists), and the
  BSC plan-gate notice where applicable. Capture `uiautomator dump` / screenshots
  and record what the UI actually rendered.
- **M5 crash watch** — clear `logcat` first, perform the walk, then
  `adb logcat -d | grep -E 'FATAL EXCEPTION|AndroidRuntime'`; record the result.
- **Second-phone QR scan** — the OWNER's step (Q7): display the Receive QR on the
  device and scan it with a second phone; the owner reports the result and it is
  recorded verbatim in `validation.md`.
- **Funded rows** — none funded (Q4): re-read the wake-up register and record all
  rows as still suspended; do not fail or drop them.

## T4 — Phase 2 checklist, records, close

1. Build the AC-2.1–AC-2.8 matrix with one evidence row each: fresh evidence from
   T1/T2/T3 or the named close record of the owning item. Replanned rows are marked
   replanned with their owner, not re-opened.
2. Write `validation.md`: commands, observed results, absolute artifact paths, scan
   counts, triage table, duplication disposition, device evidence, AC matrix, and a
   **Handoff** section (funded rows suspended; `kotlin:S6310` → 4.8; background
   polling → 3.5; biometric device proof recorded-as-unavailable).
3. Update `specs/roadmap.md`: 2.10 → `[x]`, Phase 2 header marked closed, current
   position moves to **3.1**, wake-up register unchanged (rows still suspended).
4. Update `AGENTS.md`: standing gate → Phase 2 closed, next entry 3.1, fresh counts
   from T1/T2, owed list consumed.
5. Update `specs/mission.md` current-reality lines with the new counts only if the
   numbers move (they are the same record source as the roadmap close).
6. Leave all changes in the working tree for owner review — no commit/push unless
   the owner directs the close procedure.

## Risks

- **Device dozing** (2.8's blocker): phone is AC powered now; keep `svc power
  stayon true` and re-check wakefulness before each instrumented run.
- **Long builds** on 4 cores: run suites in sequence, never two Gradle invocations
  on the same tree at once.
- **Sonar token/project visibility**: token is the standing project-analysis token
  in `local.properties`; the server is already UP and the project exists.
- **Duplication gate ambiguity**: resolved by the Q3 triage-then-decide rule above.
