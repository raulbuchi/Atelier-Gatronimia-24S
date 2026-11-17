package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.core.media.MediaCompressor
import com.example.ui.theme.AtelierCharcoal
import com.example.ui.theme.OnSwissRedContainer
import com.example.ui.theme.PccDanger
import com.example.ui.theme.PccDangerBg
import com.example.ui.theme.PureSurface
import com.example.ui.theme.SwissRed
import com.example.ui.theme.SwissRedContainer

@Composable
fun CameraXCaptureDialog(
    recipeId: String,
    sheetTitle: String,
    onPhotoCaptured: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDeniedOnce by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            permissionDeniedOnce = true
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        BackHandler { onDismiss() }
        KeepScreenOnEffect(enabled = true)

        Surface(
            color = AtelierCharcoal,
            modifier = Modifier
                .fillMaxSize()
                .testTag("camerax_capture_dialog")
        ) {
            if (!hasCameraPermission) {
                CameraPermissionRationaleContent(
                    sheetTitle = sheetTitle,
                    wasDenied = permissionDeniedOnce,
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onDismiss = onDismiss
                )
            } else {
                ActiveCameraXViewfinder(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    recipeId = recipeId,
                    sheetTitle = sheetTitle,
                    onPhotoCaptured = onPhotoCaptured,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun CameraPermissionRationaleContent(
    sheetTitle: String,
    wasDenied: Boolean,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = PureSurface),
            border = BorderStroke(2.dp, SwissRed),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                TagPill("CAMERAX • ESTÚDIO DE BANCADA", SwissRedContainer, OnSwissRedContainer)
                Text(
                    text = "Fotografar Empratamento: $sheetTitle",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.camerax_permission_rationale),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (wasDenied) {
                    Card(colors = CardDefaults.cardColors(containerColor = PccDangerBg)) {
                        Text(
                            text = "A permissão da câmera ainda não foi concedida. Toque abaixo para autorizar ou verifique as permissões do aplicativo nas configurações do Android.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PccDanger,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = SwissRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("request_camerax_permission_button")
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Permitir Acesso à Câmera", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Voltar à Ficha Técnica")
                }
            }
        }
    }
}

@Composable
private fun ActiveCameraXViewfinder(
    context: android.content.Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    recipeId: String,
    sheetTitle: String,
    onPhotoCaptured: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val previewView = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_AUTO) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner, previewView, lensFacing) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var boundProvider: ProcessCameraProvider? = null

        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                boundProvider = provider
                val desiredSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()
                val activeSelector = when {
                    provider.hasCamera(desiredSelector) -> desiredSelector
                    provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                    provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                    else -> null
                }

                if (activeSelector == null) {
                    cameraError = "Nenhum sensor de câmera disponível neste dispositivo."
                    return@addListener
                }

                val previewUseCase = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val captureUseCase = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setFlashMode(flashMode)
                    .build()

                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    activeSelector,
                    previewUseCase,
                    captureUseCase
                )
                imageCapture = captureUseCase
                cameraError = null
            } catch (e: Exception) {
                cameraError = "Falha ao inicializar CameraX: ${e.localizedMessage ?: "erro de hardware"}"
            }
        }, mainExecutor)

        onDispose {
            try {
                boundProvider?.unbindAll()
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(flashMode, imageCapture) {
        imageCapture?.flashMode = flashMode
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
                .testTag("camerax_preview_view")
        )

        // Culinary Rule-of-Thirds Empratamento Guide Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 1.dp.toPx()
            val guideColor = Color.White.copy(alpha = 0.28f)
            val thirdW = size.width / 3f
            val thirdH = size.height / 3f
            drawLine(guideColor, Offset(thirdW, 0f), Offset(thirdW, size.height), strokePx)
            drawLine(guideColor, Offset(thirdW * 2f, 0f), Offset(thirdW * 2f, size.height), strokePx)
            drawLine(guideColor, Offset(0f, thirdH), Offset(size.width, thirdH), strokePx)
            drawLine(guideColor, Offset(0f, thirdH * 2f), Offset(size.width, thirdH * 2f), strokePx)
        }

        // Top Control Bar (Low-Touch >= 48dp targets)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(AtelierCharcoal.copy(alpha = 0.82f))
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .align(Alignment.TopCenter)
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("camerax_close_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_close_dialog),
                    tint = PureSurface
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "ESTÚDIO DE EMPRATAMENTO",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFDAD6),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = sheetTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = PureSurface,
                    maxLines = 1
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = {
                        flashMode = when (flashMode) {
                            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
                            else -> ImageCapture.FLASH_MODE_AUTO
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("camerax_flash_button")
                ) {
                    val flashIcon = when (flashMode) {
                        ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                        ImageCapture.FLASH_MODE_OFF -> Icons.Default.FlashOff
                        else -> Icons.Default.FlashAuto
                    }
                    Icon(
                        imageVector = flashIcon,
                        contentDescription = stringResource(R.string.cd_camerax_flash),
                        tint = PureSurface
                    )
                }

                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("camerax_switch_lens_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = stringResource(R.string.cd_camerax_switch_lens),
                        tint = PureSurface
                    )
                }
            }
        }

        // Bottom Low-Touch Kitchen Shutter Deck
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(AtelierCharcoal.copy(alpha = 0.86f))
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .align(Alignment.BottomCenter)
        ) {
            cameraError?.let { err ->
                Card(colors = CardDefaults.cardColors(containerColor = PccDangerBg)) {
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = PccDanger,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Text(
                text = "Enquadre o prato ou mise en place na grade • EXIF removido automaticamente",
                style = MaterialTheme.typography.labelMedium,
                color = PureSurface.copy(alpha = 0.88f)
            )

            Button(
                onClick = {
                    val capture = imageCapture
                    if (capture == null || isCapturing) return@Button
                    isCapturing = true
                    cameraError = null
                    try {
                        val (photoFile, contentUri) = MediaCompressor.createMepCreationPhotoFile(
                            context = context,
                            recipeId = recipeId
                        )
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                        capture.takePicture(
                            outputOptions,
                            mainExecutor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    isCapturing = false
                                    onPhotoCaptured(contentUri)
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    isCapturing = false
                                    cameraError = "Falha na captura CameraX: ${exception.localizedMessage ?: "tente novamente"}"
                                }
                            }
                        )
                    } catch (e: Exception) {
                        isCapturing = false
                        cameraError = "Erro ao preparar arquivo de foto: ${e.localizedMessage}"
                    }
                },
                enabled = imageCapture != null && !isCapturing,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SwissRed,
                    contentColor = PureSurface
                ),
                border = BorderStroke(4.dp, PureSurface),
                modifier = Modifier
                    .size(76.dp)
                    .testTag("camerax_shutter_button")
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(
                        color = PureSurface,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = stringResource(R.string.cd_camerax_shutter),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
