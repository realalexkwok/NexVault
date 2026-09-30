# 2.7 Receive screen — requirements (owner decision table)

Date: 2026-09-30. Branch: `feature/2.7-receive-screen` (from `main` e502a5d).

| Q | Decision |
| --- | --- |
| Q1 Scope | **Receive + send-flow QR scanner (TC-UI-006)** — the receive screen (AC-2.7) AND the in-app scanner wired to the send form's QR button. |
| Q2 Scanner stack | **CameraX 1.5.3 + zxing core 3.5.3** (no third-party scanner library; camera aliases already in the catalog). |
| Q3 Receive presentation | **Full-screen route** in the Home tab's nested graph, pushed from the Home Receive quick action (same pattern as 2.6's Send). |

Plan summary (approved 2026-09-30): core-ui `QrCodeImage` (zxing QRCodeWriter -> Bitmap, round-trip decode
test); `feature-receive` gets `ReceiveViewModel` (GetActiveWalletUseCase + GetSelectedChainUseCase,
existing `toChainUi()` badge) + `ReceiveScreen` (badge, full address, QR, Copy, Share, network
warning); `feature-send` gets `QrScannerScreen` (CameraX ImageAnalysis + zxing decode, permission
flow, dedupe) + the To-field QR icon + nav result via `previousBackStackEntry.savedStateHandle`;
app gains the `receive` + `scan_qr` routes, `HomeScreen.onReceiveClicked`, the enabled Receive
quick action, and `android.permission.CAMERA`. AC-2.7 all four rows; TC-UI-006 mapped.
