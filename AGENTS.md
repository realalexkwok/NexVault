# AGENTS.md

## Part A — System Prompt Context

You are an expert Android/Kotlin developer agent operating in the NexVault repository.

ACTIVE PROJECT: **NexVault — non-custodial multi-chain crypto wallet for Android**
(package `com.nexvault.wallet`, single Gradle build, 20 modules).

Before writing any code, read the project constitution files in `specs/` first.
Before every write to disk, an **ask-user-question round** covering requirements
(scope/decisions/context), plan (task groups), and validation (success criteria)
must be answered by the user.

**Standing gate:** Phase 2.0 (stabilization + verification debt) is the only open
entry point in the roadmap. Do not start Phase 2.6+ feature work while it is open.

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

### Branch rules
- `main` is the trunk (there is no `develop` branch, despite `doc/01`).
- One branch per roadmap item: `feature/<item>-<slug>`; never stack a feature branch
  on an unfinished feature branch.
- Agents must NOT `git commit` / `git push`. Leave changes in the working tree for
  owner review.

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
- Gates: `detekt` (config `config/detekt/detekt.yml`, `maxIssues: 0`) and `ktlintCheck`
  (ktlint 1.x defaults; there is **no** `.editorconfig`).
- `spotless` is applied but **has no configuration block** — it is a no-op and must not
  be cited as evidence of quality.
- Verification reports MUST include absolute paths of built artifacts.

### Verification debt rule (this repo's core discipline)
- Roadmap status: `[x]` verified · `[~]` implemented but unverified · `[ ]` not started.
- An item may only be marked `[x]` when a `validation.md` exists for it AND both halves
  of verification have passed.
- Never write "done", "complete", or "works" in a validation record without a command
  and its observed result. Baseline re-measured 2026-09-20: 162 unit tests green, but the
  `core-security` suite does not compile, 4 of 5 main tabs are placeholders, and both
  quality gates are red.

### Two-sided verification (BOTH halves required)
1. **Automatic** — the agent runs the relevant Gradle test/build/gate tasks and pastes
   results.
2. **Manual** — the owner exercises the app on a device/emulator and reports back.
   The app has never been run on a device; installing the emulator is a prerequisite
   (see `specs/dev-environment.md`).

### Secrets
- `local.properties` (git-ignored) holds `INFURA_API_KEY`, `ALCHEMY_API_KEY`,
  `COINGECKO_API_KEY`, `ETHERSCAN_API_KEY`, `WALLETCONNECT_PROJECT_ID`; they are exposed
  via BuildConfig. Never commit a real key; never log key material or mnemonics.
- All five keys are currently placeholders (`your_key_here`). Network-backed screens
  degrading at runtime is **expected**, not a bug to "fix" by faking data.

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
