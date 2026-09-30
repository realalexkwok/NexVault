# 2.7 Receive screen — plan

Approved 2026-09-30 (owner accepted all three recommended decisions; see requirements.md).

## Deliverables

| # | Deliverable | Where |
| --- | --- | --- |
| 1 | `zxing-core` 3.5.3 + CameraX (already catalogued) wiring, verification metadata | catalog, `core-ui`, `feature-send`, `gradle/verification-metadata.xml` |
| 2 | `QrCodeImage` — zxing QRCodeWriter -> Bitmap (512px, EC level M, quiet zone), always black-on-white | `core/core-ui/.../components/QrCodeImage.kt` |
| 3 | `ReceiveViewModel` — active wallet address + selected chain (`toChainUi()`), no-wallet message | `feature/feature-receive/.../ReceiveViewModel.kt` |
| 4 | `ReceiveScreen` — chain badge, QR, full address, Copy (clipboard + snackbar), Share (ACTION_SEND chooser), network-compatibility warning (testnet variant) | `feature/feature-receive/.../ReceiveScreen.kt` |
| 5 | `QrScannerScreen` + `QrScannerViewModel` — CameraX Preview + ImageAnalysis (KEEP_ONLY_LATEST), zxing decode from the Y plane, first-payload-only, CAMERA permission flow with retry | `feature/feature-send/.../QrScanner*.kt` |
| 6 | Send form QR button (To field trailing icon) + returned address fills the field once | `feature-send/SendScreen.kt` |
| 7 | Routes `receive` + `scan_qr`, result via `previousBackStackEntry.savedStateHandle`, Home Receive quick action enabled, `android.permission.CAMERA` | `app/.../MainScreen.kt`, `HomeScreen.kt`, `app/src/main/AndroidManifest.xml` |

## Verification

| What | How | Status |
| --- | --- | --- |
| QR encodes + scans with any reader | `QrCodeImageTest` round-trips the bitmap through zxing's `QRCodeReader`; quiet-zone assertion; owner scans with a second phone | see validation.md |
| Receive screen behaviour | `ReceiveViewModelTest` (address/testnet/no-wallet/no-account) + `ReceiveScreenTest` (render, clipboard, share chooser, warning) | see validation.md |
| Scanner decode | `QrDecoderTest` (generated QR -> Y plane -> decode, row-stride padding, blank frame) + `QrScannerViewModelTest` (first payload wins, trim/blank) + `QrScannerScreenTest` (permission-denied branch) | see validation.md |
| TC-UI-006 | `SendFlowE2ETest#sendForm_qrButtonNavigatesToTheScanner` on the Pixel 6a (QR icon -> scanner screen -> back to the form), CAMERA granted via `GrantPermissionRule` | see validation.md |
| Gates | `detekt`, `ktlintCheck` | see validation.md |
| Coverage | `koverXmlReport` + sonar scan, must not drop below 53.4% | see validation.md |

## Notes and deferred work

- The scanner lives in `feature-send` because only the send flow scans in this phase; if 3.x needs a
  scanner elsewhere it moves to a shared module then.
- The QR payload is the raw address; an EIP-681 payment URI (`ethereum:0x…@chainId`) is a later
  enhancement, not part of AC-2.7.
- CameraX binding is never exercised under Robolectric: the screen tests cover the permission
  branch, the device test covers the real camera path.
