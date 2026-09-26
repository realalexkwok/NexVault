# 2.0.5 — Turn both quality gates green — Requirements

> Roadmap item: **2.0.5** (Phase 2.0), the item the roadmap has been pointing at since the
> 2026-09-20 baseline. Plan: `plan.md`. Evidence: `validation.md`.
> Branch: `feature/2.0.5-quality-gates`, cut from `main` (`aa539d6`) after 2.0.4b closed.
> Owner decisions recorded 2026-09-26 (planning round Q1–Q4).

## Problem

`./gradlew detekt` and `./gradlew ktlintCheck` have failed since Phase 0. `AGENTS.md`'s
definition of done and `doc/08-ACCEPTANCE-CRITERIA.md` (AC-1.1: *"`./gradlew detekt` and
`./gradlew ktlintCheck` pass with zero issues"*) both require them green, and Phase 2.0 —
whose whole purpose is "make the existing code true" — cannot close while they are red.
Phase 2 feature work is explicitly gated behind this item.

Measured on this host at `main` (`aa539d6`), Linux recipe of `specs/dev-environment.md`:

| Gate | Result | Breakdown |
| --- | --- | --- |
| `./gradlew ktlintCheck` | **BUILD FAILED (exit 1)** | **1556** findings in **184** files across the 20 modules |
| `./gradlew detekt` | **BUILD FAILED (exit 1)** | **65** issues (78 weighted) against `maxIssues: 0` |

The ktlint number is a style-vocabulary conflict, not 1556 independent mistakes: the code
follows Android Studio/IntelliJ defaults while ktlint 1.x's `ktlint_official` layout rules
disagree. Its dominant rules are `multiline-expression-wrapping` 481 and
`function-signature` 321; the hygiene rules (unused imports, import order, missing final
newlines, sloppy indentation) are a small minority but are real defects.

detekt's 65 issues: `TooManyFunctions` 20, `MaxLineLength` (>120) 25,
`CommentOverPrivateFunction` 9, `NewLineAtEndOfFile` 9, `ComplexCondition` 2.

## Compensating note for 2.0.4b (`[~]`, merged but not verified)

The roadmap forbids building on an implemented-unverified item without recording it. Two
facts make this item safe to proceed:

1. 2.0.5 does not build on 2.0.4b's behaviour — it reformats and re-tunes quality config.
   The one place they meet is the `data`/`core-network`/`feature-tokens` files that the
   formatter rewrites. Their content is unchanged (verified: whitespace, import order and
   trailing commas only, plus the two cosmetic rewrites recorded in `validation.md` §A5).
2. The automatic half of 2.0.4b is **re-run here on the post-format tree**: 274 tests /
   0 failures / 1 skipped, including all 19 explorer tests. The manual half owed by 2.0.4b
   (M1/M2 suspended with the funding decision, M3–M5 owner-pending) is untouched by this
   item and is not claimed here.

## Owner decisions (Q/A, 2026-09-26)

| # | Question | Answer |
| --- | --- | --- |
| Q1 | Which ktlint path? | **B — codify the existing style.** `.editorconfig` disables ktlint's 11 layout-opinion rules and keeps every hygiene rule on; `ktlintFormat` then auto-fixes the hygiene findings only. Measured alternatives: (A) reformat to `ktlint_official` = 181 files, +6616/−5954 lines, and Android Studio's *Reformat Code* would undo it unless the IDE is re-pinned too; (C) disable everything and fix nothing = leaves 37 unused imports, 11 wildcard imports and 19 missing final newlines. |
| Q2 | How does detekt reach zero? | **B — deliberate config plus real fixes.** One uniform `TooManyFunctions` ceiling of 25 (detekt's default is 11), `CommentOverPrivateFunction` off (comments already carry weight 0, and the KDocs it fires on explain behaviour the private name cannot), `MaxLineLength` stays **120** and the long lines are wrapped, the 2 `ComplexCondition`s are extracted. (A) refactor all 20 `TooManyFunctions` offenders was rejected: 4 are repository/DAO interfaces, 3 impls, 2 ViewModels, 2 DataStores and 9 test classes, and those splits ripple through DI/use cases into 2.6/2.8. (C) a baseline file hides 56 issues — the roadmap's weakest option. |
| Q3 | Gate coverage beyond subprojects? | **Yes.** The root project is not a subproject, so ktlint never saw `build.gradle.kts` / `settings.gradle.kts` (1 finding: missing final newline). Apply the plugin to the root project too; detekt and spotless stay subproject-only. |
| Q4 | How does the work land? | **Branch + isolated commits.** `feature/2.0.5-quality-gates` from `aa539d6`; four commits, formatting separate from every logic edit (roadmap requirement). Merge/push/branch-delete remain owner actions at close. |

## What the ktlint decision means, rule by rule

Disabled in `.editorconfig` (1128 of the 1556 findings):

| Rule | Findings | Why disabled |
| --- | --- | --- |
| `multiline-expression-wrapping` | 481 | ktlint wants `val x =\n    when …`; the code (and Android Studio) writes `val x = when …` |
| `function-signature` | 321 | ktlint collapses/re-wraps parameter lists differently from the IDE |
| `function-expression-body` | 104 | ktlint wants `fun f() = expr` where the code uses a block body |
| `annotation` | 92 | annotation placement opinion |
| `argument-list-wrapping` | 58 | argument wrapping opinion |
| `chain-method-continuation` | 43 | call-chain wrapping opinion |
| `class-signature` | 19 | class header wrapping opinion |
| `if-else-wrapping` | 4 | single-line `if` opinion |
| `multiline-if-else` | 3 | ditto |
| `parameter-list-wrapping` | 2 | ditto |
| `condition-wrapping` | 1 | ditto |

`ktlint_function_naming_ignore_when_annotated_with = Composable` covers the 93
`function-naming` findings — every one is a PascalCase composable (`@Composable fun
HomeScreen()`), which is the Compose convention, not a naming mistake.

Deliberately **not** set: `max_line_length`. Measured: setting it to 120 additionally
activates `function-literal` (8) and `binary-expression-wrapping` (7). detekt's
`MaxLineLength: 120` remains the single, stricter ceiling, and every line above it is
wrapped in this item.

Still enforced (and therefore still meaningful after the change): `no-unused-imports`,
`import-ordering`, `final-newline`, `indent`, `no-wildcard-imports`, `max-line-length`,
the trailing-comma rules, `comment-spacing`, `no-multi-spaces`, `if-else-bracing`,
`blank-line-before-declaration`, `filename`, `backing-property-naming` and the rest of the
1.x standard set.

## What the detekt decision means

- `TooManyFunctions`: `thresholdInFiles/Classes/Interfaces/Objects/Enums = 25`. Offenders at
  the decision point: `WalletRepositoryImplTest` 22, `TokenRepositoryImpl` 19,
  `WalletRepositoryImpl` 19, `ExceptionsTest` 16, `SecurityUtilsTest` 16,
  `PasswordValidatorTest` 16, `WalletRepository` 14, `UserPreferencesDataStore` 14, plus 12
  more at 11–14. A ceiling of 25 still fails a god class.
- `CommentOverPrivateFunction`: off. The 9 findings are KDocs on private `@Composable`
  helpers and private helpers such as `UiState.withError` (whose KDoc documents the
  precedence rule) and `validateMnemonicInput` (whose KDoc documents checksum timing).
- Unchanged and still enforced: `build.maxIssues: 0`, `MaxLineLength.maxLineLength: 120`,
  `ComplexCondition.threshold: 3`, the whole `empty-blocks` and `potential-bugs` sections,
  and `NewLineAtEndOfFile`.

## Gate scope (recorded so "green" is not over-claimed)

| Instrument | Covers | Does **not** cover |
| --- | --- | --- |
| ktlint | every module's `src/main`, `src/test`, `src/androidTest` Kotlin, every module `build.gradle.kts`, and (after this item) the two root Kotlin scripts | nothing in the source tree; generated `build/` output is correctly excluded |
| detekt | every module's `src/main` and `src/test` Kotlin | `src/androidTest` (the Android plugin's default source sets) |
| spotless | **nothing** — applied with no configuration block; a no-op that must never be cited as evidence | — |

There is still **no CI**: green means green when someone runs the gates (roadmap 4.3).

## Acceptance criteria

| AC | Criterion |
| --- | --- |
| AC-1 | `./gradlew ktlintCheck --continue` exits **0** for the whole repository, root scripts included |
| AC-2 | `./gradlew detekt --continue` exits **0** with `build.maxIssues: 0`, and every loosened threshold is named and justified in `config/detekt/detekt.yml` |
| AC-3 | The style decision lives in one place (`.editorconfig`) and every disabled rule is listed with its finding count in `requirements.md`/`validation.md` |
| AC-4 | The 15 findings ktlint cannot auto-correct are fixed **in code**, not disabled: 11 wildcard imports, 2 over-long literals, 1 `filename`, 1 `backing-property-naming` |
| AC-5 | Build and suite are unchanged: `:app:assembleDebug testDebugUnitTest :domain:test` → BUILD SUCCESSFUL, **274 tests / 0 failures / 0 errors / 1 skipped**, APK at `/home/superguo/Projects/NexVault/app/build/outputs/apk/debug/app-debug.apk` |
| AC-6 | No tracked Kotlin/KotlinScript line exceeds 120 characters, and the formatting commit contains no logic change (whitespace, imports and trailing commas only) |
| AC-7 | `roadmap.md`, `tech-stack.md`, `mission.md`, `AGENTS.md` and `dev-environment.md` are updated with the measured numbers — no constitution file still claims the gates are red or that `.editorconfig` does not exist |
| AC-8 | Both verification halves: the automatic evidence above, plus the owner's device walk (M1–M3 in `validation.md`) |

## Non-goals

No CI (4.3), no spotless configuration (4.6 — it stays a no-op), no detekt baseline file, no
refactor of the 20 `TooManyFunctions` offenders, no coverage/KDoc work (4.8), no dependency
or version changes, no new production behaviour, and no work on 2.0.4b's owed device walk.
