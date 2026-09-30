package com.nexvault.wallet.feature.send

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BinaryBitmap
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.nexvault.wallet.core.ui.components.NexVaultButton
import com.nexvault.wallet.core.ui.components.NexVaultTopBar
import com.nexvault.wallet.core.ui.theme.NexVaultDimens
import java.util.concurrent.Executors

/**
 * QR scanner for the send flow (roadmap 2.7, TC-UI-006): the first decoded payload is returned to
 * the caller, which fills the recipient field.
 *
 * CameraX + zxing core only — no third-party scanner activity. The camera is bound while the
 * screen is composed and unbound on dispose; a denied CAMERA permission shows an explanation with
 * a retry button instead of an empty preview.
 */
@Composable
fun QrScannerScreen(
    onQrDetected: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: QrScannerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var hasPermission by
        remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED,
            )
        }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasPermission = granted
        }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(uiState.decodedPayload) {
        uiState.decodedPayload?.let(onQrDetected)
    }

    Scaffold(
        topBar = {
            NexVaultTopBar(
                title = stringResource(R.string.send_scan_title),
                showBackButton = true,
                onBackClick = onNavigateBack,
            )
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            if (hasPermission) {
                CameraPreview(onPayloadDecoded = viewModel::onPayloadDecoded)
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize().padding(NexVaultDimens.spacingLg),
                ) {
                    Text(
                        text = stringResource(R.string.send_scan_permission_denied),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                    NexVaultButton(
                        text = stringResource(R.string.send_scan_grant_permission),
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.padding(top = NexVaultDimens.spacingLg),
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(onPayloadDecoded: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context) }
    val analyzer =
        remember {
            QrFrameAnalyzer { payload -> onPayloadDecoded(payload) }
        }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize(),
    )

    DisposableEffect(lifecycleOwner) {
        val analysis =
            ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply {
                    setAnalyzer(executor, analyzer)
                }
        var providerRef: ProcessCameraProvider? = null
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            {
                runCatching {
                    val provider = future.get()
                    providerRef = provider
                    val preview =
                        Preview.Builder().build().apply {
                            surfaceProvider = previewView.surfaceProvider
                        }
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                    )
                }
            },
            ContextCompat.getMainExecutor(context),
        )

        onDispose {
            providerRef?.unbindAll()
            executor.shutdown()
        }
    }
}

/**
 * Decodes QR codes from camera frames with zxing, using the frame's luminance plane directly
 * (no RGB conversion needed for a monochrome code).
 */
internal class QrFrameAnalyzer(
    private val onDecoded: (String) -> Unit,
) : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        try {
            val payload = decodeQr(image)
            if (payload != null) onDecoded(payload)
        } finally {
            image.close()
        }
    }
}

/** Decodes the QR payload of a single camera frame, or null when the frame holds no QR code. */
internal fun decodeQr(image: ImageProxy): String? {
    val plane = image.planes.firstOrNull() ?: return null
    val buffer = plane.buffer
    val data = ByteArray(buffer.remaining())
    for (index in data.indices) {
        data[index] = buffer[index]
    }
    return decodeQrLuminance(
        luminance = data,
        dataWidth = plane.rowStride,
        dataHeight = image.height,
        width = image.width,
        height = image.height,
    )
}

/**
 * Pure decoder used by [decodeQr] and by the unit tests: [luminance] is a Y plane whose rows are
 * [dataWidth] bytes wide while only the left [width]x[height] window carries the image.
 */
internal fun decodeQrLuminance(
    luminance: ByteArray,
    dataWidth: Int,
    dataHeight: Int,
    width: Int,
    height: Int,
): String? {
    if (width <= 0 || height <= 0) return null
    val source = PlanarYUVLuminanceSource(luminance, dataWidth, dataHeight, 0, 0, width, height, false)
    return runCatching {
        val result: Result = QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source)))
        result.text
    }.getOrNull()
}
