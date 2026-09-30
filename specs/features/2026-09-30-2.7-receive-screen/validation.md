# 2.7 Receive screen — validation record

Branch `feature/2.7-receive-screen` (from `main` e502a5d). Owner decisions in `requirements.md`,
technical plan in `plan.md`. Every claim below carries its command and observed result.

## Automatic half

### Unit + Robolectric suite

`JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew testDebugUnitTest :domain:test` →
BUILD SUCCESSFUL, **437 tests / 0 failures / 0 errors** (2.6 closed at 417; 2.7 adds 20):

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
**11/11 tests on the Pixel 6a**: BackupPolicy 3, ConfirmationDialog 3, CreationFlow E2E 1,
Example 1, `SendFlowE2ETest` **3** — the 2.6 pair plus the new
`sendForm_qrButtonNavigatesToTheScanner` (TC-UI-006): the QR icon opens the scanner route and the
top-bar back returns to the form. CAMERA is granted to the test process through
`GrantPermissionRule` so the scanner binds a real camera during the run.

### Coverage (server, SonarQube `NexVault`)

`./gradlew testDebugUnitTest :domain:test koverXmlReport sonar` → BUILD SUCCESSFUL. Server measures
after the scan: **coverage 53.9%** (2.6 closed at 53.4), lines_to_cover 7,339, ncloc 14,234. New code
is measured: `ReceiveScreen.kt` 85.3%, `QrCodeImage.kt` 91.7%, `QrScannerScreen.kt` 42.1%.
Local Kover aggregate: 4,775/9,498 (50%).

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
