# AGENTS.md

## Part A — System Prompt Context

You are an expert Android/Kotlin developer agent operating in the NexVault repository.

ACTIVE PROJECT: **NexVault — non-custodial multi-chain crypto wallet for Android**
(package `com.nexvault.wallet`, single Gradle build, 20 modules).

Before writing any code, read the project constitution files in `specs/` first.
Before every write to disk, an **ask-user-question round** covering requirements
(scope/decisions/context), plan (task groups), and validation (success criteria)
must be answered by the user.

**Standing gate:** the Phase 1.9 legacy code review gate is **closed** (2026-09-25,
15/15 signed off). **Phase 2.0 is CLOSED `[x]` (2026-09-28, item 2.0.7 — record and close)**:
2.0.1–2.0.6 verified, the Phase 0 rows re-graded (15 → `[x]`, 1.2/1.4 `[~]` for open 4.17/4.18
findings), both gates green, the app device-walked and instrumented-tested, and every `doc/07` TC id
mapped or registered. **2.6 (Send transaction flow) is CLOSED `[x]` (2026-09-30, owner close)**:
29 commits, 417 unit tests / 0 failures, instrumented 10/10 on the Pixel 6a, gates green, SonarQube
coverage 53.4% with 0 criticals. **2.7 (Receive screen) is CLOSED `[x]` (2026-09-30, owner close
after reviewer verification)**: 443 unit tests / 0 failures / 1 skipped, gates green, SonarQube
coverage 54.1% with 0 bugs/vulnerabilities/criticals, TC-UI-006 mapped to the send-flow QR scanner.
The open entry point is now **Phase 2.8 — Transaction history**. Remaining verification debt: 2.0.4b's M1/M2
(suspended pending funding — see the wake-up register), M5 crash watch owed, and the funded
TC-INT-002 submit deferred to the 2.10 walk.

## Part B — Project Constitution (authoritative)

- **Core Mission:** `specs/mission.md` — what NexVault is, what it is not, principles.
- **Current Goals:** `specs/roadmap.md` — strict item order; status distinguishes
  **implemented** from **verified**.
- **Tech Boundaries:** `specs/tech-stack.md` — approved modules, libraries and
  versions, conventions, quality gates.
- **Dev Environment:** `specs/dev-environment.md` — JDK, Android SDK, emulator,
  API keys, command reference.
- **Read-only archive:** `doc/` (design docs 01–09) and `doc/prompts/` (historical
  prompt record, prompts 01–15). The specs supersede both. **Never start work from a
  `doc/prompts/` file again** — the prompt-driven workflow is retired; use it only to
  look up historical intent.

## Part C — Operational Instructions

### Spec-driven workflow (supersedes prompt-md-first)
- One roadmap item = one feature spec directory:
  `specs/features/YYYY-MM-DD-<item>-<slug>/` with `requirements.md`, `plan.md`,
  `validation.md`.
- `requirements.md` records the owner-decision table (Q/A) from the ask round.
  `plan.md` is the technical plan. `validation.md` is the evidence record.
- Roadmap order is authoritative; never skip or reorder items without owner approval.

### Legacy code review (reviewer / developer split)
- **Reviewer session** may modify **record files (md/txt) and test files/test projects
  only**. Production code, build scripts, config: read-only.
- Findings live in the CR task files under
  `specs/features/2026-09-21-legacy-code-review/tasks/`; the **developer session**
  consumes them and fixes; the reviewer **deletes a finding row only after verifying the
  fix**.
- **Branch flow:** legacy CR fixes run on a **temporary branch created by the code
  reviewer** (`cr/<task>-<slug>`). Phase 2+ feature items already have their
  `feature/<item>-<slug>` branch, so the reviewer does not create one for them.
- **Close / sign-off:** when the fixes are verified by the reviewer and **confirmed by
  the user**, either the code reviewer or the developer closes the item: **mark the item
  done** in the records, **commit**, **merge to main**, **delete the local branch**, and
  **push main**.

### Branch rules
- `main` is the trunk (there is no `develop` branch, despite `doc/01`).
- One branch per roadmap item: `feature/<item>-<slug>`; never stack a feature branch
  on an unfinished feature branch.
- Agents must NOT `git commit` / `git push`. Leave changes in the working tree for
  owner review. **Exception:** the close / sign-off procedure for legacy CR items and
  feature items (section above) is owner-directed and may end in a pushed `main`.

### Build, test and quality gates
Run Gradle with the Android Studio JBR — it is the JDK Android Studio itself uses and
what `.vscode/settings.json` pins, so CLI and editor builds stay reproducible:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew <task>
```

(The override is a reproducibility choice, not a workaround: both the JBR and Homebrew's
JDK 25 ship `jlink`, and `:data:assembleDebug` was verified to succeed without it.
See `specs/dev-environment.md` §7.)

- Unit tests (all modules): `testDebugUnitTest` — plus `:domain:test` (pure JVM module).
- Build the app: `:app:assembleDebug` → artifact `app/build/outputs/apk/debug/app-debug.apk`.
- Install on a device: `:app:installDebug`.
- Gates: `detekt` (config `config/detekt/detekt.yml`, `maxIssues: 0`, uniform
  `TooManyFunctions` ceiling 25, `CommentOverPrivateFunction` off) and `ktlintCheck`
  (`.editorconfig` codifies the project style — the 11 `ktlint_official` layout rules are
  disabled by the 2.0.5 owner decision, the Compose naming exception uses ktlint's own
  property, everything else is on; `max_line_length` is deliberately unset so detekt's 120
  stays the single ceiling). **Both gates are green as of 2.0.5 (2026-09-26) — keep them
  that way: run them before claiming an item is done, and never loosen them silently.**
  Scope: ktlint covers `src/main`, `src/test`, `src/androidTest` and every `*.kts` build
  script (root scripts included); detekt covers `src/main` and `src/test` only.
- `spotless` is applied but **has no configuration block** — it is a no-op and must not
  be cited as evidence of quality.
- **Milestone static-analysis scan (owner decision 2026-09-28).** Each remaining phase ends with a
  scan-and-close item — **2.10** (Phase 2), **3.8** (Phase 3), **4.19** (repo close-out) — that runs
  a SonarQube scan over what the phase built, triages the findings, and only then records and closes
  the phase. Server: the owner's local SonarQube Community Build at `http://localhost:9000`
  (`sudo docker start sonarqube`; verified 26.9.0.129388 on 2026-09-28) — never SonarCloud. A
  **baseline scan is recorded before 2.6 starts**, so the gate is **new code only**: no new
  blocker/critical issues, no new security hotspots, duplication not increasing; coverage enters the
  gate only at 4.8 (**Kover 0.9.11** since 2026-09-30 — the owner-approved migration off JaCoCo,
  because Kover measures Robolectric-sandboxed classes; the 2.6-era baseline is **53.4%**
  server-wide). **Scanner (resolved 2026-09-28, 2.6 task 0):** SonarScanner for Gradle
  `org.sonarqube` **7.4.0.8496**, root-applied; the server token is a **project analysis token scoped
  to `NexVault`** (never global); the npm `@sonar/scan` is JS/TS-only and unusable here. Findings are
  fixed or accepted as named-owner debt in the checkpoint's
  `validation.md`; the scan never replaces `detekt`/`ktlintCheck`. Recipe:
  `specs/dev-environment.md` §"Milestone static-analysis scan".
- Verification reports MUST include absolute paths of built artifacts.

### Verification debt rule (this repo's core discipline)
- Roadmap status: `[x]` verified · `[~]` implemented but unverified · `[ ]` not started.
- An item may only be marked `[x]` when a `validation.md` exists for it AND both halves
  of verification have passed.
- Never write "done", "complete", or "works" in a validation record without a command
  and its observed result. Baseline 2026-09-21 (kept for contrast): 223 unit tests green
  (1 skipped), the app never run on a device, 4 of 5 main tabs placeholders, both quality
  gates red. Current 2026-09-30: **443** unit tests green (plus **11 instrumented device tests**),
  the app walked on a Pixel 6a, both gates green, coverage **54.1%** (Kover), **Phase 2.0 closed
  `[x]`**, **2.6 and 2.7 closed `[x]`**; 2.0.4b `[~]` for M1/M2/M5; owed: a device re-run on 2.7's
  closed revision and the second-phone QR scan (2.8); 2 of 5 tabs still placeholders until 2.8.
- **Funding-suspended verification (owner's rule 2026-09-30):** a test case that needs a
  funded wallet is **suspended**, never failed and never silently dropped. The single register
  of suspended cases is `specs/roadmap.md` §"Funding-suspended verification (wake-up
  register)" — every row states what funding it needs and what to run. When the owner signals
  funding in any wording ("funded", "funds ready", ...), re-read that section and resume the
  funded rows in one pass; the 2.10 Phase 2 walk consumes whatever is funded by then.

### Two-sided verification (BOTH halves required)
1. **Automatic** — the agent runs the relevant Gradle test/build/gate tasks and pastes
   results.
2. **Manual** — the owner exercises the app on a device and reports back.
   The owner's device of choice is a **physical Android device over USB**
   (decision 2026-09-21), not the emulator — installing one is not required. The app has
   been installed and walked on a Pixel 6a since 2026-09-25 (2.0.2b → 2.0.4), so the manual
   half is a real, repeatable step; 2.0.4b's funded history rows are still owed (2.0.5's walk
   was completed 2026-09-27).
   See `specs/dev-environment.md`.

### Secrets
- `local.properties` (git-ignored) holds `INFURA_API_KEY`, `ALCHEMY_API_KEY`,
  `COINGECKO_API_KEY`, `ETHERSCAN_API_KEY`, `WALLETCONNECT_PROJECT_ID`; they are exposed
  via BuildConfig. Never commit a real key; never log key material or mnemonics.
- Real keys were supplied 2026-09-25 and are the supported path (2.0.4); setting one to an
  **empty** value remains the way to exercise the "Not configured" states — the placeholder string
  `your_key_here` does **not** trip them (`ChainConfigProvider.isExplorerConfigured` checks
  `isNotBlank()`, corrected 2026-09-28 by the 2.0.4b M4 walk).
  Network-backed screens degrading without a key is **expected**, not a bug to "fix" by
  faking data.

### Spec compliance
- Never add a dependency outside `specs/tech-stack.md`.
- Never change behavior without updating the constitution or the feature spec first.
- Do not "fix" a failing test by weakening the production code it tests; decide whether
  the test or the implementation is wrong and record the decision in `validation.md`.

### Deferred-work handoff
- When an item deliberately leaves work behind, its `validation.md` MUST end with a
  **"Handoff"** list: what was deferred, why, and which roadmap item owns it.
- The owning item's planning round MUST re-read that list and explicitly implement,
  replan, or cancel each entry — never drop it silently.

### Definition of done
Build passes · relevant tests pass · both gates green · every acceptance criterion in
`doc/08-ACCEPTANCE-CRITERIA.md` for the item is checked · `validation.md` written ·
roadmap status updated in the same pass.
