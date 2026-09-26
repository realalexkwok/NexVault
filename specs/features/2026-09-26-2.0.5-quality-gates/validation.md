# 2.0.5 — Turn both quality gates green — Validation record

> **Status: IMPLEMENTED on `feature/2.0.5-quality-gates`, AUTOMATIC HALF GREEN — manual half
> (device walk) still owed, so the roadmap row stays `[~]` until M1–M3 pass.**
> `./gradlew detekt ktlintCheck --continue` is BUILD SUCCESSFUL (exit 0) for the first time
> in the project's history. Requirements: `requirements.md` (Q1–Q4, AC-1…AC-8). Plan: `plan.md`.

**Host used for every command below:** Linux (`superguo-SQM2270`),
`JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64` (the Linux recipe of `specs/dev-environment.md`;
the macOS JBR path in the same doc does not exist on this host). All commands from the
repository root. Branch cut from `main` = `aa539d6`.

## Baseline at `main` (`aa539d6`) — the number this item had to move

| Gate | Command | Observed |
| --- | --- | --- |
| ktlint | `./gradlew ktlintCheck --continue` | **BUILD FAILED, exit 1** — **1556** findings in **184** files (41 task reports over 20 modules) |
| detekt | `./gradlew detekt --continue` | **BUILD FAILED, exit 1** — **65** issues / **78** weighted: `TooManyFunctions` 20, `MaxLineLength` 25, `CommentOverPrivateFunction` 9, `NewLineAtEndOfFile` 9, `ComplexCondition` 2 |

Cross-tool overlap measured before deciding: all **9** detekt `NewLineAtEndOfFile` files are
also ktlint `final-newline` findings, so commit 1 clears those for free.

## Automatic half — commands and observed results

| # | Check (AC) | Command | Observed |
| --- | --- | --- | --- |
| A1 | Both gates (AC-1, AC-2) | `./gradlew detekt ktlintCheck --continue; echo $?` | **BUILD SUCCESSFUL in 16s, exit 0** (`GRADLE_EXIT=0`), `119 actionable tasks: 18 executed, 101 up-to-date`; zero findings in every module's `build/reports/detekt/detekt.xml` (12 modules, 0 each) and zero lines in every `build/reports/ktlint/**/*.txt` |
| A2 | Build + suite (AC-5) | `./gradlew :app:assembleDebug testDebugUnitTest :domain:test --continue` | **BUILD SUCCESSFUL**; **274 tests / 0 failures / 0 errors / 1 skipped** (unchanged from the 2.0.4b close); artifact `/home/superguo/Projects/NexVault/app/build/outputs/apk/debug/app-debug.apk`, **47,986,669 bytes** — byte-identical in size to the baseline |
| A3 | Attribution (AC-3) | per-rule parse of the generated reports before/after (baseline vs commit 1 vs final) | **ktlint 1556 → 0** and **detekt 65 → 0**; every finding attributed to exactly one of config / formatter / code — tables below |
| A4 | Line length (AC-6) | `git ls-files '*.kt' '*.kts'` swept with `awk 'length($0)>120'` | **0 lines above 120** (23 lines wrapped in commit 3, plus the androidTest line, plus the 2 literals in commit 2) |
| A5 | Commit 1 is not a logic change (AC-6) | `git diff --ignore-all-space --ignore-blank-lines -U0` on commit 1, multiset-compared side by side | delta is exactly: removed unused imports, added trailing commas, and **two cosmetic rewrites** — `NEVER(-1);` → `NEVER(-1),` + `;` (`AutoLockTimeout`), and `} else null,` → `} else {\n null\n },` (`ChainSelectorDropdown`). No identifier, literal, operator or control flow changed; corroborated by A2 |
| A6 | Root scripts inside the gate (AC-1) | `./gradlew tasks --all` + `build/reports/ktlint/ktlintKotlinScriptCheck/ktlintKotlinScriptCheck.txt` | root tasks `:ktlintKotlinScriptCheck` / `:ktlintCheck` execute; the root report exists and is **0 bytes** (the root `build.gradle.kts` final newline was fixed by the formatter in the same commit) |
| A7 | Hand edits are the only hand edits | `git diff --stat main..HEAD` + per-file review | **130 files, +371/−353** total: 126 files in commit 1 (124 formatter output + `.editorconfig` + root build script), 14 in commit 2, 11 in commit 3; every hand hunk is one of the intended fixes listed under §TG3/§TG4 |

**Evidence commit.** A1–A7 were run against commit 3 (`dc6a0ad`), the last commit that touches
code or config. Commit 4 adds Markdown only; a re-run of both commands at that HEAD reports
`119 actionable tasks: 119 up-to-date` / `BUILD SUCCESSFUL` with exit 0 for each, which is the
correct result for a docs-only change.

### ktlint — where all 1556 findings went

| Fate | Rules | Findings |
| --- | --- | --- |
| Disabled in `.editorconfig` (Q1) | `multiline-expression-wrapping` 481, `function-signature` 321, `function-expression-body` 104, `annotation` 92, `argument-list-wrapping` 58, `chain-method-continuation` 43, `class-signature` 19, `if-else-wrapping` 4, `multiline-if-else` 3, `parameter-list-wrapping` 2, `condition-wrapping` 1 | **1128** |
| `ktlint_function_naming_ignore_when_annotated_with = Composable` | `function-naming` 93 — all PascalCase composables | **93** |
| Auto-corrected by `ktlintFormat` (commit 1) | `trailing-comma-on-call-site` 100, `no-empty-first-line-in-class-body` 59, `indent` 42, `no-unused-imports` 37, `trailing-comma-on-declaration-site` 23, `import-ordering` 22, `final-newline` 19, `blank-line-before-declaration` 10, `no-multi-spaces` 4, `comment-spacing` 2, `no-blank-line-before-rbrace` 1, `if-else-bracing` 1 | **320** |
| Fixed in code (commit 2) | `no-wildcard-imports` 11, `max-line-length` 2, `filename` 1, `backing-property-naming` 1 | **15** |
| | | **1556** |

Progression: **1556** (main) → **15** after commit 1 (`ktlintCheck` still exit 1) → **0** after
commit 2 (`BUILD SUCCESSFUL`, exit 0).

### detekt — where all 65 issues went

| Fate | Rules | Issues |
| --- | --- | --- |
| Cleared by the formatter (commit 1) | `NewLineAtEndOfFile` 9 (the `final-newline` fixes) | **9** |
| Cleared by config (commit 3, Q2) | `TooManyFunctions` 20 (uniform ceiling 25), `CommentOverPrivateFunction` 9 (off) | **29** |
| Fixed in code (commit 3) | `MaxLineLength` 25 (wrapped; `AuthRepositoryImpl`/`SecurityPreferencesDataStore` also lost their fully-qualified references), `ComplexCondition` 2 (named `isInputBlocked`) | **27** |
| | | **65** |

Progression: **65** (main) → **56** after commit 1 → **27** after the config edit → **0** after
the code fixes.

## TG3 — the 15 findings ktlint cannot auto-correct (commit 2, 14 files, +31/−16)

| Item | Files | Fix and why it is not a disable |
| --- | --- | --- |
| 11 × `no-wildcard-imports` | 9 `domain/src/test` files, `app/src/test/.../ExampleUnitTest.kt`, `app/src/androidTest/.../ExampleInstrumentedTest.kt` | `import org.junit.Assert.*` → the explicit symbols each file uses (`assertEquals`, `assertFalse`, `assertNotNull`, `assertNull`, `assertTrue`, `fail`), inserted in ktlint's lexicographic order |
| 2 × `max-line-length` (172 chars) | `feature-onboarding/src/test/.../ImportWalletViewModelTest.kt` | the 24-word `validMnemonic24` and the 26-word `tooMany` literals are split across two concatenated literals; the resulting strings are byte-identical, and the tests still assert on word counts |
| 1 × `filename` | `core-ui/.../theme/Dimens.kt` → `NexVaultDimens.kt` | the file contains a single top-level `object NexVaultDimens`; no code change, no reference update needed. Historical `doc/` and legacy-CR records keep the old name (they are archives) |
| 1 × `backing-property-naming` | `feature-tokens/.../TokenDetailViewModel.kt` | `_selectedChartDays` → `selectedChartDaysFlow`. It is **not** a backing property for a public member (the public state is `uiState.selectedChartDays`), so it simply loses the underscore prefix rather than gaining a fake counterpart |

## TG4 — detekt code fixes (commit 3, 11 files, +90/−30)

| Item | Files | Fix |
| --- | --- | --- |
| 23 × `MaxLineLength` + 1 androidTest line | `TokenRepositoryImpl` (11 `makeDefaultToken` calls), `AuthRepositoryImpl` (1), `AuthRepositoryImplTest` (4), `TransactionMapper` (1 KDoc), `HDKeyManagerTest` (1), `MnemonicManagerTest` (1), `SecurityPreferencesDataStore` (2), `ImportWalletViewModelTest` (2), `BackupPolicyTest` (1, not detekt-analyzed but swept for A4) | wrapped without changing any value; the `CAKE` call additionally splits its argument list because one argument line would still have exceeded 120 |
| 2 referenced classes replaced by imports | `AuthRepositoryImpl`, `SecurityPreferencesDataStore` | `com.nexvault.wallet.core.security.biometric.BiometricStatus` and `android.util.Base64` are imported instead of written out in full (all four `Base64` call sites, for coherence); this is what made the lines long |
| 2 × `ComplexCondition` | `feature-auth/.../UnlockViewModel.kt` | `onDigitPressed` / `onBackspacePressed` guards extracted to a named `isInputBlocked` local. Behaviour unchanged; covered by existing `TC-UNLOCK-010` (6-digit cap) and `TC-UNLOCK-014` (input ignored while verifying), and the suite is green (A2) |

## Manual half — device (Pixel 6a) — NOT RUN

| # | Step | Observed | Verdict |
| --- | --- | --- | --- |
| M1 | `:app:installDebug` on the branch build; cold start → Unlock (PIN) → Home renders (live price + chart) → chain switch → token detail → back. No visual regression | ☐ not run | ☐ |
| M2 | Crash watch across the walk: `adb -s 10.42.0.139:40171 logcat -d \| grep -E "FATAL EXCEPTION\|AndroidRuntime"` → empty | ☐ not run | ☐ |
| M3 | Owner re-runs `./gradlew detekt ktlintCheck` on the same tree and reports the exit codes (independent confirmation of A1) | ☐ not run | ☐ |

Screenshots: *(none yet)*. Note the manual half is deliberately kept: this item rewrites 130
files including production sources, so "it compiles and the unit suite passes" is not the same
as "the app still renders".

## Decisions taken at implementation (2026-09-26)

| Decision | Choice / reason |
| --- | --- |
| Root project plugin (Q3) | **Applied and verified** — the plugin registers `ktlintKotlinScriptCheck`/`ktlintCheck` on the plugin-less root project and the report is empty, so the fallback (hand-fix + documented gap) was not needed |
| Name for the renamed backing property | `selectedChartDaysFlow` rather than `selectedChartDays`: `TokenDetailUiState.selectedChartDays` already exists and is used as a named argument in the same class, so the flow keeps a distinct, self-describing name |
| `max_line_length` in `.editorconfig` | **Not set** (measured: `= 120` activates `function-literal` +8 and `binary-expression-wrapping` +7). detekt's `MaxLineLength: 120` governs, and A4 proves the tree is compliant |
| `CAKE` line wrap | Its argument list is split over two lines because a single wrapped argument line would still be 126 characters |
| AndroidTest line (`BackupPolicyTest`) | Wrapped although detekt does not analyze `androidTest`, so A4 can state "0 tracked lines over 120" without an exception |
| Root `build.gradle.kts` | Landed in commit 1 (config/formatting commit) rather than its own: it is build configuration plus the formatter's own fix to that file |

## Handoff (deferred work and its owner)

| Deferred | Why | Owner |
| --- | --- | --- |
| Re-adopting ktlint's layout dialect (option A) | Owner chose to codify the existing style; reversing it means a future reformat item plus IDE re-pinning | owner decision |
| Refactoring the 20 `TooManyFunctions` offenders | Ceiling raised by decision (25); interface/impl splits would ripple through DI/use cases and collide with 2.6/2.8 | future item if the ceiling is hit |
| detekt does not analyze `src/androidTest` | Plugin-level source-set configuration, not a config edit | 4.3 (CI) or a new item |
| Nothing runs the gates automatically | Green today only means green when someone runs them | 4.3 |
| `spotless` is still a no-op | Applied with no configuration block; must never be cited as evidence | 4.6 |
| 2.0.4b M1/M2 (funding) and M3–M5 (owner-pending) | Untouched by this item; its automatic half is re-run here, its manual half is still owed | 2.0.4b |

## Close / sign-off — pending

| # | Step | State |
| --- | --- | --- |
| C1 | Automatic half | ✅ **GREEN** — A1–A7 above; both gates exit 0 |
| C2 | Manual half (M1–M3) | ☐ owed — the owner walks the app on the Pixel 6a and reports |
| C3 | Roadmap row | **`[~]` implemented, not verified** — becomes `[x]` only after C2 |
| C4 | Merge to `main`, push, delete the branch | ☐ owner action (2.0.4b precedent) |
