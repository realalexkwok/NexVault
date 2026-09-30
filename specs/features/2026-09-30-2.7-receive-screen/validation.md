# 2.7 Receive screen — validation record

Branch `feature/2.7-receive-screen` (from `main` e502a5d). Owner decisions in `requirements.md`,
technical plan in `plan.md`. Every claim below carries its command and observed result.

## Automatic half

### Unit + Robolectric suite

`JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew testDebugUnitTest :domain:test` →
BUILD SUCCESSFUL, **443 tests / 0 failures / 0 errors / 1 skipped** (2.6 closed at 417; 2.7 adds 26;
the one skip is the pre-existing `@Ignore`d keystore test, unchanged by 2.7):

- `core-ui`: `QrCodeImageTest` (4) — generated QR decodes back with zxing's own reader; quiet zone
  present; both content descriptions render.
- `feature-receive`: `ReceiveViewModelTest` (4) — active-account address, testnet flag, no wallet,
  wallet without accounts; `ReceiveScreenTest` (5) — badge/QR/address/warning render, testnet
  warning variant, no-wallet message, Copy puts the address on the clipboard, Share opens a chooser.
- `feature-send`: `QrDecoderTest` (4) — generated QR → luminance plane → decode, row-stride padded
  plane, blank frame, empty frame; `QrScannerViewModelTest` (2) — first payload wins, blank frames
  ignored and payload trimmed; `QrScannerScreenTest` (1) — permission-denied branch.

### Gates

`./gradlew detekt ktlintCheck --continue` → BUILD SUCCESSFUL (exit 0); `ktlintFormat` applied once
for the new files, no rule was disabled or relaxed.

### Instrumented (device)

`ANDROID_SERIAL=<pixel-6a> ./gradlew :app:connectedDebugAndroidTest` → BUILD SUCCESSFUL,
**11/11 tests on the Pixel 6a** (run on revision `27110c4`; the reviewer fixes below touched
`QrScannerScreen`, `ReceiveScreen` and `ReceiveViewModel`, so a device re-run on the final revision is
**owed at the close walk** — the adb wireless session dropped before it could be repeated): BackupPolicy 3, ConfirmationDialog 3, CreationFlow E2E 1,
Example 1, `SendFlowE2ETest` **3** — the 2.6 pair plus the new
`sendForm_qrButtonNavigatesToTheScanner` (TC-UI-006): the QR icon opens the scanner route and the
top-bar back returns to the form. CAMERA is granted to the test process through
`GrantPermissionRule` so the scanner binds a real camera during the run.

### Coverage (server, SonarQube `NexVault`)

`./gradlew testDebugUnitTest :domain:test koverXmlReport sonar` → BUILD SUCCESSFUL. Server measures
after the scan: **coverage 53.9%** (2.6 closed at 53.4), lines_to_cover 7,339, ncloc 14,234. New code
is measured: `ReceiveScreen.kt` 85.3%, `QrCodeImage.kt` 91.7%, `QrScannerScreen.kt` 42.1%.
Local Kover aggregate — method: sum the top-level `LINE` counter of every
`<module>/build/reports/kover/report.xml` that carries data (13 modules):
**covered 4,799 / total 9,535 = 50.3%**. (An earlier revision of this record quoted 4,775/9,498 from
the pre-reviewer-fix revision; summing *all* counter types in the same files yields ~23.9k/47.5k —
the percentage is the comparable figure, the absolute pair is only meaningful per counter type.)

### Reviewer findings (2.7) — resolution

Every finding is either fixed in this branch or refuted with evidence; nothing is deferred silently.

| ID | Sev | Resolution |
| --- | --- | --- |
| F-2.7-1 | MINOR | **Reviewer error — refuted by git evidence.** The caption string was already "Swap arrives in Phase 3." at the reviewed revision `27110c4` (changed by `226d257` itself: "Receive arrives in Phase 2…" → "Swap arrives in Phase 3."). The finding was filed from the 2.0.3-era value without re-reading the string — verified by the reviewer 2026-09-30: `git show 27110c4:…strings.xml`. The only remnant is the stale code comment above the caption (`HomeScreen.kt` "Roadmap 2.0.3: Receive and Swap have no destination yet") — NIT for the next touch. The developer's pin-test is a good lock regardless. |
| F-2.7-2 | MINOR | **Fixed** — `ReceiveViewModel` now `.catch`es the upstream flow and emits `ReceiveError.LOAD_FAILED` with `isLoading = false`; `ReceiveScreen` renders "Could not load the wallet address." Tests: `ReceiveViewModelTest#a failing upstream flow ends up as a load error instead of loading forever`, `ReceiveScreenTest#receiveScreen_whenTheWalletFlowFails_reportsItInsteadOfLoadingForever`. |
| F-2.7-3 | NIT | **Fixed** — the hardcoded `NO_ACTIVE_WALLET_MESSAGE` constant is gone; the ViewModel exposes a `ReceiveError` enum and the copy lives in `strings.xml` (`receive_no_wallet`, `receive_load_failed`), so no user-facing text sits in Kotlin. |
| F-2.7-4 | NIT | **Fixed** — `specs/tech-stack.md` updated: the ZXing row now documents the wired `zxing-core` 3.5.4 (bumped from the previously pinned 3.5.3, matching the version the tech-stack already listed as current), the CameraX row records that 2.7 uses the individual aliases, and the §4d "`zxing` is an orphan version alias" debt row is marked **RESOLVED 2.7**. |
| F-2.7-5 | MINOR | **Fixed (record corrected)** — the aggregate now states its method and the re-derived pair; see the coverage section. The reviewer's percentage (50.3%) matches the LINE-counter derivation exactly. |
| F-2.7-6 | NIT | **Fixed (record corrected)** — the counts now read 443/0/0/**1**, naming the pre-existing keystore skip. |
| F-2.7-7 | MINOR | **Fixed** — the camera bind result is no longer swallowed: `CameraPreview` reports failure to `QrScannerScreen`, which renders "The camera could not be started on this device." with a **Try again** action that re-binds via a keyed attempt counter. This matters because the manifest marks the camera optional. |
| F-2.7-8 | MINOR | **Refuted with evidence** — rotation is not consulted, but QR decoding does not need it: `QrDecoderTest#decodes a frame whose content is rotated a quarter turn` renders the code rotated 90° into the luminance plane and decodes it successfully (zxing locates and orients the symbol itself). A comment in the decoder documents the decision. |
| F-2.7-9 | NIT | **Fixed** — `QrCodeImage` renders nothing for blank content and `generateQrBitmap` `require`s non-blank, so the reusable core-ui component cannot crash a caller; covered by `blankContent_rendersNothingInsteadOfCrashing` and `generateQrBitmap_rejectsBlankContent`. |
| F-2.7-10 | NIT | **Partly fixed, partly deferred to the walk** — the quiet zone is now the spec's **4 modules** (was 1). The "scans with any QR reader" claim is still only proven zxing→zxing in CI; the second-phone scan is the AC-2.7 manual half at the close walk and is recorded as such. |

### SonarQube triage of the new code

The first 2.7 scan raised 7 issues in the new/changed files; **all were fixed rather than accepted**,
and the re-scan shows **0 open issues in any 2.7 file**, with the project at **0 bugs, 0
vulnerabilities, 0 blocker/critical, 66 code smells (from 73), duplication 0.3%**:

| Rule | Where | Fix |
| --- | --- | --- |
| `kotlin:S3776` CRITICAL | `SendScreen` complexity 16 > 15 | extracted the scan-result handling into `ScannedAddressEffect` |
| `kotlin:S6619` | `ReceiveViewModel` useless `?.` | `chain.toChainUi()` (the combine lambda gives a non-null chain) |
| `kotlin:S6518` | `QrScannerScreen` `ByteBuffer.get(array)` | indexed accessor loop |
| `kotlin:S1874` x4 | `hiltViewModel()` (deprecated import) in `ReceiveScreen`, `QrScannerScreen`, `SendScreen`, `HomeScreen` | moved to `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel` |

## Deferred work / handoff

- Owner scan of the displayed QR with a second phone (AC-2.7 "scans correctly with any QR reader")
  is part of the close walk; the zxing round-trip test is the automatic half.
- EIP-681 payment URIs (`ethereum:0x…@chainId`) are **not** in 2.7; noted in `plan.md` as a later
  enhancement.
- The scanner lives in `feature-send`; if a later item needs scanning elsewhere, it moves to a
  shared module (3.x).

---

## Reviewer verification — commits 226d257 + 773f0fc + 27110c4 (2026-09-30)

> Code-reviewer session (read-only on production code; record files only). Re-verified from the
> tree, by fresh runs, and by a device rerun; nothing taken on trust.

### Diff review — verdicts

| # | Area | Check | Verdict |
| --- | --- | --- | --- |
| R1 | Receive screen (AC-2.7) | `ReceiveScreen`: chain badge, `QrCodeImage` (240dp, black-on-white), full address with test tag, Copy → `ClipboardManager` + snackbar, Share → `ACTION_SEND` chooser, no-wallet fallback, testnet-aware network warning; `ReceiveViewModel` combines `GetActiveWalletUseCase` + `GetSelectedChainUseCase` (active account address, `toChainUi()`) | ✅ |
| R2 | QR image | `QrCodeImage`: zxing `QRCodeWriter`, quiet zone, error correction M, forced black-on-white (scanner reliability), semantics contentDescription, round-trip decode test; `core-ui` depends on `libs.zxing.core` with verification metadata for 3.5.3 | ✅ |
| R3 | Scanner (TC-UI-006) | `QrScannerScreen`: CameraX `ImageAnalysis` + zxing decode from the luminance plane (rowStride/width/height correct, `image.close()` in finally), permission auto-request + denial branch with retry, camera bound while composed and unbound on dispose; `QrScannerViewModel` keeps only the first non-blank payload (single navigation); validation stays in the send form | ✅ |
| R4 | Nav wiring | `MainScreen`: `receive` + `scan_qr` routes; scanner payload → `previousBackStackEntry.savedStateHandle` → `SendScreen` fills once via `ScannedAddressEffect` and clears; Home Receive quick action enabled and routed | ✅ |
| R5 | Manifest | `CAMERA` permission + `uses-feature camera required=false` — installable without a camera; scanner degrades to a notice | ✅ |
| R6 | Sonar fixes present in code | S3776 → `ScannedAddressEffect` extraction ✓; S6619 → `chain.toChainUi()` (no useless `?.`) ✓; S6518 → indexed accessor loop in `decodeQr` ✓; S1874 ×4 → `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel` in ReceiveScreen, QrScannerScreen, SendScreen, HomeScreen ✓ (all four verified by grep) | ✅ |
| R7 | Traceability | TC-UI-006 row moved **registered → mapped** to `SendFlowE2ETest#sendForm_qrButtonNavigatesToTheScanner` | ✅ |

### Fresh evidence (reviewer's own runs)

| # | Check | Command | Result |
| --- | --- | --- | --- |
| R8 | Suite + gates + build | `./gradlew testDebugUnitTest :domain:test detekt ktlintCheck :app:assembleDebug --continue --rerun-tasks` | **BUILD SUCCESSFUL, exit 0 — 776 tasks executed, 0 failed tasks**; **437 tests / 0 failures / 0 errors / 1 skipped** (417 → 437, +20 — matches claim) |
| R9 | Instrumented | Pixel 6a, wake + `pm clear` + pinned serial → `:app:connectedDebugAndroidTest` | **11/11 pass** (BackupPolicy 3, ConfirmationDialog 3, CreationFlow 1, Example 1, SendFlowE2E **3** incl. the scanner test) — first attempt hit a Gradle-daemon crash (environment, no test ran); the clean rerun passed |
| R10 | Coverage input | `./gradlew koverXmlReport` + my own parse of the Kover XML | new-file line coverage confirms the direction (my local basis: ReceiveScreen 97%, QrCodeImage 100%, QrScannerScreen 45% — the scanner's camera-binding block is Robolectric-hostile, which is why it trails); the server-side 53.9% and the "0 open issues" re-scan numbers are **token-gated** (SonarQube API returns 401 anonymously) and remain developer-recorded | ⚠️ server numbers developer-recorded |

### Findings (reviewer, adversarial re-pass 2026-09-30 — scale BLOCKER/MAJOR/MINOR/NIT)

| ID | Severity | Location | Finding | Proof | Owner |
| --- | --- | --- | --- | --- | --- |
| F-2.7-1 | **MINOR** | `HomeScreen.kt:394-401` + `home_actions_coming_soon` | The caption **"Send and receive arrive in Phase 2; swap in Phase 3."** renders unconditionally directly under the now-**live** Send and Receive buttons (enabled since 2.6/2.7) — stale, self-contradictory user-facing copy; the 2.0.3 comment above it is stale too | read the file: buttons `enabled = true` at :376/:382, caption unconditional at :395 | next feature round (copy fix) |
| F-2.7-2 | **MINOR** | `ReceiveViewModel.observeWalletAndChain` | No error handling around the combined flow: an upstream failure leaves `isLoading = true` forever → blank content area, no error state, no retry | code trace: `isLoading` is only cleared inside the `combine` lambda; no `catch` anywhere in the VM | next receive-screen touch |
| F-2.7-5 | **MINOR** | `validation.md` §Automatic ("Local Kover aggregate: 4,775/9,498") | **Recorded number does not reproduce.** Re-derived by summing the 19 per-module `report.xml` LINE counters: **23,905 / 47,525 (50.3%)**. The percentage direction matches; the absolute pair does not match any subset of the current reports. | own re-derivation (scripted, this review) | next planning round (correct the record) |
| F-2.7-7 | **MINOR** | `QrScannerScreen.kt:151` `CameraPreview` | Bind failure is swallowed by `runCatching`: on a device with **no back camera** (the manifest declares camera `required=false`) or any `bindToLifecycle` failure, the user gets a **silent black preview** — no error message, unlike the permission-denied branch which has one. | code trace: `runCatching { … bindToLifecycle } ` has no `onFailure` and there is no error state in `CameraPreview` | next scanner touch |
| F-2.7-8 | **MINOR** | `QrScannerScreen.kt` decode path | `imageInfo.rotationDegrees` is **never consulted** — the luminance plane is fed to zxing unrotated. On sensors that deliver rotated frames the QR will not decode. Evidence coverage does not catch it: `QrDecoderTest` feeds synthetic unrotated frames and the E2E only asserts **navigation**, never a real decode. | `grep -rn rotation` in the module → 0 hits; test inventory read | next scanner touch (consult rotation, add a rotated-frame test) |
| F-2.7-3 | NIT | `ReceiveViewModel` `NO_ACTIVE_WALLET_MESSAGE` | Hardcoded user-facing string; the module's other copy lives in `values/strings.xml` | code read | **4.17** |
| F-2.7-4 | NIT | `specs/tech-stack.md` §4d | Two rows stale: "zxing is an orphan version alias" (resolved by this item's `zxing-core` entry) and "4 unused bundles" (no longer includes `camerax` — feature-send references it) | file read | next planning round |
| F-2.7-6 | NIT | `validation.md` §Automatic | "437 tests / 0 failures / 0 errors" omits the **1 pre-existing skip** (the `@Ignore`d AndroidKeyStore test); the reviewer's fresh sum is 437 / 0 / 0 / **1** | fresh XML sums (776-task run) | next planning round |
| F-2.7-9 | NIT | `QrCodeImage` (core-ui reusable component) | No guard on blank `content`: `QRCodeWriter.encode("")` throws `IllegalArgumentException` inside `remember` → crash. The only current caller guards (`hasAddress`), but the reusable component's contract is unguarded and untested for the empty input | code read; `QrCodeImageTest` has no blank-content case | core-ui touch (guard or document) |
| F-2.7-10 | NIT | `QrCodeImage` quiet zone | `EncodeHintType.MARGIN` is explicitly **1 module**; the QR spec recommends 4 for real-world reader compatibility. AC-2.7's "scans correctly with any QR reader" is currently proven only zxing→zxing (round-trip test); the owner's second-phone scan is deferred to the close walk. Consider MARGIN 4 before that walk. | `hints` map read | this item's close walk |

### Reviewer nit (not a finding)

`QrCodeImage` computes `sizePx` only as a `remember` key while the bitmap is always 512px —
harmless, but the key could just be `content`.

### Verdict

**PASS (automatic + diff + device).** The receive screen and the send-flow scanner implement the
owner decisions (Q1–Q3) and AC-2.7 faithfully: QR/copy/share/warning verified in code and by the
Robolectric tests, the scanner is a CameraX+zxing implementation with a sound permission flow, the
TC-UI-006 test is mapped and green on the device (11/11), the suite is 437/0/0/1 from a fresh
776-task run, both gates exit 0, and all seven SonarQube new-code findings are fixed in the tree.
The SonarQube server-side percentages (53.9%, 0 open issues) are developer-recorded — the API is
token-gated. **Findings: 4×MINOR + 6×NIT** (F-2.7-1 … F-2.7-10, table above) — none blocking, each
with an owner and proof. Remaining for closure: the owner's close walk (including the second-phone
QR scan) and the owner merge of `feature/2.7-receive-screen` to `main`.

---

## Reviewer verification of the fixes — 69c3796 + fba7234 (2026-09-30)

> The same discipline as the findings pass: every resolution was re-derived, not trusted. Fresh
> evidence: `./gradlew testDebugUnitTest :domain:test detekt ktlintCheck --continue --rerun-tasks`
> → **BUILD SUCCESSFUL, exit 0 — 697 tasks executed, 0 failed tasks; 443 tests / 0 failures / 0
> errors / 1 skipped** (matches the corrected record exactly; all 6 new tests included).

| ID | Resolution | Reviewer verification |
| --- | --- | --- |
| F-2.7-1 | Refuted (reviewer error) | confirmed by git: the string was correct at `27110c4`; the finding was filed from stale knowledge. Retracted. |
| F-2.7-2 | Fixed | verified: `.catch` → `ReceiveError.LOAD_FAILED` + `isLoading = false`; screen maps the error to `receive_load_failed`; both new tests present and green |
| F-2.7-3 | Fixed | verified: `NO_ACTIVE_WALLET_MESSAGE` deleted; `ReceiveError` enum; copy in `strings.xml` only |
| F-2.7-4 | Fixed | verified: tech-stack ZXing/CameraX rows updated; §4d orphan-alias row marked RESOLVED 2.7; zxing pinned 3.5.4 with verification metadata |
| F-2.7-5 | Fixed (record corrected) | **reproduced exactly** by the reviewer: summing the top-level `LINE` counter of each data-carrying `report.xml` gives **4,799 / 9,535 = 50.3%** — matches the corrected record |
| F-2.7-6 | Fixed (record corrected) | fresh XML sums: **443 / 0 / 0 / 1** — matches |
| F-2.7-7 | Fixed | verified: `bound.isFailure → onCameraFailed` → `ScannerMessage` with "Try again" that re-binds via the keyed attempt counter; two new strings |
| F-2.7-8 | Refuted with evidence | accepted: `QrDecoderTest#decodes a frame whose content is rotated a quarter turn` rotates a real matrix and decodes; `fba7234` documents the rotation-invariance rationale. The reviewer's original MINOR was over-claimed (QR finder patterns are orientation-agnostic) |
| F-2.7-9 | Fixed | verified: `QrCodeImage` returns nothing for blank content; `generateQrBitmap` `require`s non-blank; both tests present |
| F-2.7-10 | Partly fixed | verified: quiet zone now **4 modules**; the second-phone "any QR reader" scan remains the AC-2.7 manual half at the close walk — recorded |

### What stays open

1. The **device 11/11 re-run on the fixed revision** (the fixes touched `QrScannerScreen`,
   `ReceiveScreen`, `ReceiveViewModel`): owed at the close walk — the reviewer's attempt also
   found no adb device attached (the wireless session is down), consistent with the record.
2. The **stale code comment** above the Home caption (NIT, next touch).
3. The **second-phone QR scan** (AC-2.7 manual half).

### Verdict on the fix commit

**PASS.** All ten findings are resolved honestly: six fixed (F-2.7-2/3/4/7/9 + records F-2.7-5/6),
two refuted with evidence (F-2.7-8 by test, F-2.7-1 by git history — the latter being the
reviewer's own error, now on the record), one partly fixed with the remainder correctly deferred to
the close walk (F-2.7-10). Nothing was deferred silently.

## Close record — 2026-09-30, owner-directed close ("Close 2.7", after reviewer verification `d75b328`)

Branch `feature/2.7-receive-screen`, 10 commits ahead of `main` (e502a5d..HEAD), clean tree.

- Suite **443 tests / 0 failures / 0 errors / 1 skipped**; `detekt` + `ktlintCheck` exit 0.
- SonarQube (`NexVault`): **coverage 54.1%** (2.6 closed at 53.4%), **0 bugs, 0 vulnerabilities,
  0 blocker/critical**, 66 code smells, duplication 0.3%; **0 open issues in any 2.7 file**.
- Reviewer pass: findings F-2.7-1..10 filed across `9640fb3`/`17cfc87`/`6d978e6`, all resolved in
  `69c3796`/`fba7234`, re-verified by the reviewer in `d75b328`.

AC-2.7 matrix: QR encodes the wallet address ✓ (round-trip decode test, 4-module quiet zone);
address displayed in full, copyable and shareable ✓ (screen tests + clipboard/chooser assertions);
network-compatibility warning ✓ (mainnet and testnet variants, testnet-pinned); "scans correctly with
any QR reader" — proven zxing→zxing in CI, the **second-phone scan is owed** (see Handoff).

## Handoff (owed at the next device session)

1. **Device re-run on the closed revision** — the recorded 11/11 instrumented run is from `27110c4`;
   the reviewer fixes changed `QrScannerScreen`, `ReceiveScreen`, `ReceiveViewModel`. `adb devices` is
   empty (the phone's wireless-debugging session dropped), so re-run
   `:app:connectedDebugAndroidTest` when it is back. Owner: **2.8** (or the next device session).
2. **Second-phone QR scan** — the manual half of AC-2.7's reader-compatibility row: display the
   receive QR and scan it with another phone; same session as (1). Owner: **2.8**.
3. EIP-681 payment URIs (`ethereum:0x…@chainId`) remain out of scope; noted in `plan.md` as a later
   enhancement.
