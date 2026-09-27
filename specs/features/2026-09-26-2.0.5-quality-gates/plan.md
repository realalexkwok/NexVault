# 2.0.5 — Turn both quality gates green — Plan

> Approved by the owner 2026-09-26 (planning round; decisions Q1–Q4 in `requirements.md`).
> Branch: `feature/2.0.5-quality-gates` from `main` (`aa539d6`). Evidence: `validation.md`.

## Method

The plan was fixed by measurement before any file was touched:

1. Both gates were run on `main` and their reports parsed per rule and per file
   (1556 ktlint findings / 184 files; 65 detekt issues / 78 weighted).
2. Both ktlint paths were **simulated in a scratch tree** (a copy of the 184 offending
   files under `/tmp`, never the repository): `ktlint -F` then `ktlint` to see what
   survives. Result: option A leaves 108 findings after touching 181 files (+6616/−5954);
   option B (11 layout rules off + the Compose naming property) leaves the same **15**
   findings after touching 124 files (+220/−254 by `diff`, +230/−317 by `git`).
3. The overlap between the tools was measured, not assumed: ktlint's `final-newline`
   covers **all 9** detekt `NewLineAtEndOfFile` files.
4. detekt's own default config (read out of the cached `detekt-core-1.23.8.jar`) confirmed
   the config keys: `thresholdInFiles/Classes/Interfaces/Objects/Enums`.
5. Setting `max_line_length = 120` was tested and **rejected**: it activates
   `function-literal` (8 more findings) and `binary-expression-wrapping` (7 more).

## Task groups

### TG1 — `.editorconfig` (new file, repo root)

`root = true`; section `[*.{kt,kts}]`; the 11 `ktlint_standard_*` rules from
`requirements.md` disabled with a header comment recording the decision, the reason
`max_line_length` is not set, and a pointer to the spec. No rule is disabled anywhere else.

### TG2 — Mechanical formatting (own commit)

1. Root `build.gradle.kts`: `apply(plugin = "org.jlleitschuh.gradle.ktlint")` after the
   `subprojects { … }` block (Q3). *Fallback if the plugin refuses a plugin-less root
   project: fix the root newline by hand and record the gap — not needed, it worked.*
2. `./gradlew ktlintFormat` (aggregate; 20 modules + root).
3. Expected and observed: 124 source files + the root build script, resolving 320 findings.
4. No hand edits in this commit; generated sources untouched.

### TG3 — The 15 findings ktlint cannot auto-correct (own commit)

11 × `import org.junit.Assert.*` → explicit symbols; 2 × 172-char mnemonic literals split
across concatenated literals; `Dimens.kt` → `NexVaultDimens.kt`; `_selectedChartDays` →
`selectedChartDaysFlow`. Detail and rationale: `validation.md` §TG3.

### TG4 — detekt (own commit)

`config/detekt/detekt.yml`: uniform `TooManyFunctions` ceiling 25,
`CommentOverPrivateFunction: active: false`; `maxIssues: 0`, `MaxLineLength: 120` and
`ComplexCondition: 3` unchanged. Code: wrap the 23 remaining >120 lines (8 files, plus one
androidTest line for uniformity), drop the two fully-qualified references that caused two of
them, and extract the two `UnlockViewModel` guards into a named `isInputBlocked` local.

### TG5 — Records (own commit)

This spec directory; `roadmap.md` (row, section, position paragraph, evidence appendix);
`tech-stack.md` §6/§9; `mission.md` debt list; `AGENTS.md` build/gates line;
`dev-environment.md` checklist. `doc/` is a read-only archive and is not touched.

## Commit structure (on the branch)

| # | Commit | Content |
| --- | --- | --- |
| 1 | `chore(2.0.5): codify the ktlint style and apply the mechanical fixes` | TG1 + TG2 |
| 2 | `fix(2.0.5): clear the ktlint findings that cannot be auto-corrected` | TG3 |
| 3 | `fix(2.0.5): clear detekt — ceiling, long lines, complex conditions` | TG4 |
| 4 | `docs(2.0.5): spec records, roadmap and constitution for green gates` | TG5 |

ktlint is green after commit 2; detekt only after commit 3 (commit 1 removes its 9
`NewLineAtEndOfFile`, commit 3 the remaining 56). Merge to `main`, push and branch deletion
are owner actions at close, as in 2.0.4/2.0.4b.

## Verification plan

| # | Check | Expected |
| --- | --- | --- |
| A1 | `./gradlew detekt ktlintCheck --continue; echo $?` | BUILD SUCCESSFUL, exit 0 |
| A2 | `./gradlew :app:assembleDebug testDebugUnitTest :domain:test --continue` | BUILD SUCCESSFUL, 274 / 0 / 1, APK under `app/build/outputs/apk/debug/` |
| A3 | per-rule before/after from the generated reports | 1556 → 0 and 65 → 0, every finding attributed to exactly one of: config, formatter, code |
| A4 | `git ls-files '*.kt' '*.kts'` swept for lines >120 | 0 |
| A5 | `git diff --ignore-all-space --ignore-blank-lines -U0` multiset comparison on commit 1 | only removed unused imports, added trailing commas and two cosmetic rewrites |
| A6 | root project reports under `build/reports/ktlint/` | root scripts inside the gate, report empty |
| A7 | per-file review of every hand-edited file against `main` | only the intended hunks |
| M1–M3 | owner: install the branch build, walk Home → chain switch → token detail, watch `logcat` for crashes, re-run both gates | no regression, no crash, exit 0 |

## Risks and how they were handled

| Risk | Handling |
| --- | --- |
| A 124-file formatter run silently changing behaviour | Output proven identical to the scratch-tree simulation file by file (`cmp`), then compiled and tested: 274/0/1 and a byte-identical APK |
| Loosening the gate instead of clearing it | Only 11 layout rules were disabled — each named with its count; all 15 non-auto-correctable findings and all 27 detekt code findings were fixed in code; no baseline file |
| The formatter touching generated sources | Task audit: `ktlintFormat` only writes files under `src/` plus `*.kts`; the changed-file list confirms it |
| The mnemonic literals being altered by the wrap | The wrap keeps the exact space-separated value; the tests assert on word counts and pass |
| Formatting invalidating 2.0.4b's pre-format verification | Recorded as the compensating note in `requirements.md`: content unchanged, automatic half re-run here, manual half still owed by 2.0.4b |
