package com.homevault.app.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homevault.app.data.AssetEntity
import com.homevault.app.data.AssetStatus
import com.homevault.app.scanner.ReceiptOcrHelper
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeVaultApp(viewModel: HomeVaultViewModel) {
    val assets by viewModel.assets.collectAsStateWithLifecycle()
    val feedback by viewModel.scanFeedback.collectAsStateWithLifecycle()
    var showScanner by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(feedback) {
        feedback?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("HomeVault (AES-256 Encrypted)", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = {
                        viewModel.addAssetFromOcrText(
                            "LG Frost Free Refrigerator Model GL-I292RPZX Date: 06/11/2026 Total: ₹28,490 2 Year Warranty"
                        )
                    }) {
                        Text("+ Sample Bill")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showScanner = !showScanner },
                icon = { Icon(if (showScanner) Icons.Default.Close else Icons.Default.CameraAlt, null) },
                text = { Text(if (showScanner) "Close Scanner" else "Scan Invoice") }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (showScanner) {
                ReceiptScannerScreen(
                    onOcrCompleted = { extractedText ->
                        viewModel.addAssetFromOcrText(extractedText)
                        showScanner = false
                    },
                    onError = { errorMsg ->
                        scope.launch { snackbarHostState.showSnackbar(errorMsg) }
                    }
                )
            } else {
                AssetDashboardScreen(
                    assets = assets,
                    onDelete = viewModel::deleteAsset,
                    onLaunchError = { errorMsg ->
                        scope.launch { snackbarHostState.showSnackbar(errorMsg) }
                    }
                )
            }
        }
    }
}

@Composable
fun AssetDashboardScreen(
    assets: List<AssetEntity>,
    onDelete: (AssetEntity) -> Unit,
    onLaunchError: (String) -> Unit
) {
    val context = LocalContext.current

    if (assets.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Vault is empty and encrypted.\nTap 'Scan Invoice' or '+ Sample Bill' to begin.")
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(assets, key = { it.id }) { asset ->
            val status = asset.status()
            val badgeColor = when (status) {
                AssetStatus.ACTIVE -> Color(0xFF2E7D32)
                AssetStatus.EXPIRING_SOON -> Color(0xFFEF6C00)
                AssetStatus.EXPIRED -> Color(0xFFC62828)
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = asset.productName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(color = badgeColor.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small) {
                            Text(
                                text = "${asset.daysRemaining()}d left",
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Model: ${asset.modelNumber} • Category: ${asset.category}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Purchased: ${LocalDate.ofEpochDay(asset.purchaseDateEpochDay)} • Price: ₹${asset.priceInr.toInt()}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    val safeDigits = asset.customerCarePhone.filter { it.isDigit() || it == '+' }
                                    val callIntent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", safeDigits, null))
                                    context.startActivity(callIntent)
                                }.onFailure {
                                    onLaunchError("No phone dialer app available.")
                                }
                            }
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Support")
                        }

                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    val searchUri = Uri.Builder()
                                        .scheme("https")
                                        .authority("www.google.com")
                                        .path("search")
                                        .appendQueryParameter("q", "${asset.brand} ${asset.modelNumber} user manual pdf")
                                        .build()
                                    context.startActivity(Intent(Intent.ACTION_VIEW, searchUri))
                                }.onFailure {
                                    onLaunchError("No web browser installed.")
                                }
                            }
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Manual")
                        }

                        IconButton(onClick = { onDelete(asset) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Asset")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptScannerScreen(
    onOcrCompleted: (String) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewView = remember { PreviewView(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    DisposableEffect(lifecycleOwner, hasCameraPermission) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            if (!hasCameraPermission) return@Runnable
            runCatching {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            }.onFailure {
                onError("Camera initialization failed: ${it.localizedMessage}")
            }
        }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching { cameraProviderFuture.get().unbindAll() }
        }
    }

    if (!hasCameraPermission) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Camera permission is required to scan GST invoices.")
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { previewView }
        )

        Button(
            onClick = {
                isScanning = true
                ReceiptOcrHelper.captureAndRecognize(
                    imageCapture = imageCapture,
                    onSuccess = { text ->
                        isScanning = false
                        onOcrCompleted(text)
                    },
                    onError = { err ->
                        isScanning = false
                        onError(err)
                    }
                )
            },
            enabled = !isScanning,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
        ) {
            Text(if (isScanning) "Extracting Offline..." else "Capture & Extract Bill")
        }
    }
}