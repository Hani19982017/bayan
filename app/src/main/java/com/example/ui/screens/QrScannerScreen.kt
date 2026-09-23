package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.model.AppPermissions
import com.example.ui.theme.TawthiqPrimary
import com.example.ui.viewmodel.TawthiqViewModel
import com.example.util.QrCodeAnalyzer
import java.util.concurrent.Executors

@Composable
fun QrScannerScreen(
    viewModel: TawthiqViewModel,
    onBack: () -> Unit,
    onAccountScanned: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    BackHandler { onBack() }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isTorchOn by remember { mutableStateOf(false) }
    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    var hasProcessedResult by remember { mutableStateOf(false) }
    var showManualInputDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "يلزم إذن الكاميرا لمسح رمز QR", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(100)
            }
        } catch (_: Exception) {}
    }

    fun handleQrScanned(rawContent: String) {
        if (hasProcessedResult) return
        hasProcessedResult = true
        triggerVibration()

        val trimmed = rawContent.trim()

        // 1. Check if this is a Staff User Login QR code (e.g. tawthiq://staff_login?email=...&userId=...)
        if (trimmed.contains("staff_login", ignoreCase = true)) {
            try {
                val uri = android.net.Uri.parse(trimmed)
                val email = uri.getQueryParameter("email") ?: ""
                val userId = uri.getQueryParameter("userId") ?: ""
                val rawName = uri.getQueryParameter("name") ?: "مستخدم"
                val name = try { java.net.URLDecoder.decode(rawName, "UTF-8") } catch (_: Exception) { rawName }
                val rawRole = uri.getQueryParameter("role") ?: "موظف"
                val role = try { java.net.URLDecoder.decode(rawRole, "UTF-8") } catch (_: Exception) { rawRole }
                val rawPermission = uri.getQueryParameter("permission") ?: "كتابة"
                val permission = try { java.net.URLDecoder.decode(rawPermission, "UTF-8") } catch (_: Exception) { rawPermission }
                val permsRaw = uri.getQueryParameter("perms") ?: ""
                val rawStore = uri.getQueryParameter("store") ?: ""
                val store = try { java.net.URLDecoder.decode(rawStore, "UTF-8") } catch (_: Exception) { rawStore }
                val avatarRaw = uri.getQueryParameter("avatar") ?: ""
                val avatarUri = if (avatarRaw.isNotBlank()) {
                    try { java.net.URLDecoder.decode(avatarRaw, "UTF-8") } catch (_: Exception) { "" }
                } else ""

                val permsList = when {
                    permsRaw == "ALL" || permission == "كامل الصلاحيات" || permission.startsWith("كامل") -> AppPermissions.ALL_PERMISSIONS
                    permsRaw == "READ_ONLY" || permission == "قراءة فقط" -> AppPermissions.READ_ONLY_PERMISSIONS
                    permsRaw.isNotBlank() -> {
                        try {
                            java.net.URLDecoder.decode(permsRaw, "UTF-8").split(",").filter { it.isNotBlank() }
                        } catch (_: Exception) {
                            AppPermissions.DEFAULT_STAFF_PERMISSIONS
                        }
                    }
                    else -> {
                        if (permission == "قراءة فقط") AppPermissions.READ_ONLY_PERMISSIONS
                        else AppPermissions.DEFAULT_STAFF_PERMISSIONS
                    }
                }

                if (email.isNotBlank()) {
                    viewModel.loginAsStaff(
                        merchantEmail = email,
                        userId = userId,
                        userName = name,
                        role = role,
                        permissionType = permission,
                        permissions = permsList,
                        storeName = store,
                        avatarUri = avatarUri
                    )
                    Toast.makeText(
                        context,
                        "أهلاً بك $name! تم تسجيل الدخول بنجاح (${permsList.size} صلاحيات) ✓",
                        Toast.LENGTH_LONG
                    ).show()
                    onBack()
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Check if this is a Customer Statement QR code (e.g. tawthiq://statement?... or https://tawthiq.app/statement?...)
        if (trimmed.contains("statement", ignoreCase = true)) {
            try {
                val uri = android.net.Uri.parse(trimmed)
                val rawName = uri.getQueryParameter("name") ?: ""
                val name = try { java.net.URLDecoder.decode(rawName, "UTF-8") } catch (_: Exception) { rawName }
                val rawStore = uri.getQueryParameter("store") ?: ""
                val store = try { java.net.URLDecoder.decode(rawStore, "UTF-8") } catch (_: Exception) { rawStore }
                val phone = uri.getQueryParameter("phone") ?: ""
                val cur = uri.getQueryParameter("cur")?.ifBlank { "USD" } ?: "USD"
                val balRaw = uri.getQueryParameter("bal")?.toDoubleOrNull() ?: 0.0
                val syncKey = uri.getQueryParameter("syncKey") ?: ""
                val rawTxs = uri.getQueryParameter("txs") ?: ""

                val txsList = mutableListOf<com.example.data.model.TransactionEntity>()
                if (rawTxs.isNotBlank()) {
                    val entries = rawTxs.split(";")
                    entries.forEach { entry ->
                        val parts = entry.split(",")
                        if (parts.size >= 3) {
                            val desc = try { java.net.URLDecoder.decode(parts[0], "UTF-8") } catch (_: Exception) { parts[0] }
                            val amt = parts[1].toDoubleOrNull() ?: 0.0
                            val type = parts[2]
                            val date = if (parts.size >= 4) parts[3].toLongOrNull() ?: System.currentTimeMillis() else System.currentTimeMillis()
                            val receipt = if (parts.size >= 5) {
                                try { java.net.URLDecoder.decode(parts[4], "UTF-8") } catch (_: Exception) { "" }
                            } else ""
                            if (amt > 0.0) {
                                txsList.add(
                                    com.example.data.model.TransactionEntity(
                                        userEmail = "",
                                        accountId = 0L,
                                        type = type,
                                        amount = amt,
                                        currency = cur,
                                        description = desc,
                                        date = date,
                                        receiptNumber = receipt
                                    )
                                )
                            }
                        }
                    }
                }

                // If no individual transactions were attached in QR, but there is a net balance, add an opening balance transaction!
                if (txsList.isEmpty() && kotlin.math.abs(balRaw) > 0.001) {
                    txsList.add(
                        com.example.data.model.TransactionEntity(
                            userEmail = "",
                            accountId = 0L,
                            type = if (balRaw > 0) "LANA" else "LAHO",
                            amount = kotlin.math.abs(balRaw),
                            currency = cur,
                            description = if (store.isNotBlank()) "رصيد كشف الحساب من $store" else "رصيد كشف الحساب السابق",
                            date = System.currentTimeMillis()
                        )
                    )
                }

                if (name.isNotBlank()) {
                    viewModel.importCustomerStatementFromQr(
                        accountName = name,
                        phone = phone,
                        storeName = store,
                        currency = cur,
                        syncKey = syncKey,
                        transactionsList = txsList
                    ) { targetId ->
                        val countMsg = if (txsList.isNotEmpty()) " (${txsList.size} معاملات)" else ""
                        Toast.makeText(context, "أهلاً بك $name! تم فتح كشف حسابك من $store بنجاح$countMsg ✓", Toast.LENGTH_LONG).show()
                        onAccountScanned(targetId)
                    }
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Extract account ID from URL (e.g., https://tawthiq.app/statement/3) or #TW-3 or direct number
        val accountId = when {
            trimmed.contains("/statement/") -> {
                trimmed.substringAfterLast("/statement/").filter { it.isDigit() }.toLongOrNull()
            }
            trimmed.startsWith("#TW-", ignoreCase = true) -> {
                trimmed.substring(4).filter { it.isDigit() }.toLongOrNull()
            }
            trimmed.startsWith("TW-", ignoreCase = true) -> {
                trimmed.substring(3).filter { it.isDigit() }.toLongOrNull()
            }
            else -> {
                trimmed.filter { it.isDigit() }.toLongOrNull()
            }
        }

        if (accountId != null && accountId > 0) {
            Toast.makeText(context, "تم قراءة الحساب بنجاح ✓ (وضع المتابعة)", Toast.LENGTH_SHORT).show()
            onAccountScanned(accountId)
        } else {
            // General link / text scanned
            Toast.makeText(context, "تم مسح الرمز بنجاح: $trimmed", Toast.LENGTH_LONG).show()
            // Default to first account or 1L for sample viewing
            onAccountScanned(1L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Live Camera Preview (when permission granted)
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    val cameraExecutor = Executors.newSingleThreadExecutor()

                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    it.setAnalyzer(cameraExecutor, QrCodeAnalyzer { qrText ->
                                        if (!hasProcessedResult) {
                                            previewView.post {
                                                handleQrScanned(qrText)
                                            }
                                        }
                                    })
                                }

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                            cameraInstance = camera
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Permission request placeholder view
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "يرجى منح إذن الكاميرا لمسح رمز QR",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary)
                ) {
                    Text("منح الإذن الآن")
                }
            }
        }

        // 2. Viewfinder Overlay matching Screenshot 2 precisely
        ExactScreenshotScannerOverlay(
            modifier = Modifier.fillMaxSize()
        )

        // 3. Top App Bar matching Screenshot 2:
        // Left: Flash toggle icon | Center: Title | Right: Back arrow
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Flashlight button on left
            IconButton(
                onClick = {
                    val nextTorch = !isTorchOn
                    isTorchOn = nextTorch
                    cameraInstance?.cameraControl?.enableTorch(nextTorch)
                },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = if (isTorchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                    contentDescription = "Flashlight",
                    tint = if (isTorchOn) Color(0xFFFACC15) else Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Title in center matching Screenshot 2: "مسح رمز QR للربط مع ح..."
            Text(
                text = "مسح رمز QR للربط مع حساب",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            // Back button on right (for RTL)
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // 4. Instructions below viewfinder matching Screenshot 2 exactly
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // First sentence
            Text(
                text = "مسح رمز QR يتيح لك عرض الحساب للمتابعة فقط، بدون إمكانية التعديل أو الحذف.",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Second sentence
            Text(
                text = "يتم توليد الرمز من خيار \"مشاركة الحساب\" في\nصفحة عرض الحساب بواسطة مستخدم آخر.",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Fallback manual entry button for convenience
            TextButton(
                onClick = { showManualInputDialog = true }
            ) {
                Icon(Icons.Default.Keyboard, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "إدخال كود الحساب يدوياً",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }
        }
    }

    // Manual Account Code Dialog
    if (showManualInputDialog) {
        var manualCode by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showManualInputDialog = false },
            title = {
                Text("إدخال كود الحساب", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أدخل كود الحساب أو الرابط المعطى لك للمتابعة:")
                    OutlinedTextField(
                        value = manualCode,
                        onValueChange = { manualCode = it },
                        placeholder = { Text("مثال: 1 أو #TW-1 أو رابط كشف الحساب") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = manualCode.trim()
                        if (trimmed.isNotBlank()) {
                            showManualInputDialog = false
                            handleQrScanned(trimmed)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary)
                ) {
                    Text("فتح الحساب")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualInputDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

/**
 * High-precision Viewfinder Overlay matching Screenshot 2:
 * Rounded square cutout with glowing green corner brackets and a smooth animated scanning laser
 */
@Composable
fun ExactScreenshotScannerOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser_anim")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Scanner box dimensions
        val boxSize = (canvasWidth * 0.68f).coerceIn(240.dp.toPx(), 320.dp.toPx())
        val left = (canvasWidth - boxSize) / 2f
        val top = canvasHeight * 0.30f
        val cornerRadius = 24.dp.toPx()
        val cornerLength = 22.dp.toPx()
        val cornerStroke = 4.5.dp.toPx()
        val greenColor = Color(0xFF00E676)

        // 1. Dark semi-transparent overlay outside the square
        val backgroundPath = Path().apply {
            addRect(Rect(0f, 0f, canvasWidth, canvasHeight))
        }
        val cutoutPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(left, top, left + boxSize, top + boxSize),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            )
        }

        clipPath(path = cutoutPath, clipOp = ClipOp.Difference) {
            drawRect(color = Color.Black.copy(alpha = 0.65f))
        }

        // 2. White subtle border around the rounded box
        drawRoundRect(
            color = Color.White.copy(alpha = 0.85f),
            topLeft = Offset(left, top),
            size = Size(boxSize, boxSize),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = 2.dp.toPx())
        )

        // 3. Green Corner Accent Marks (as seen in Screenshot 2)
        // Top-Left Corner
        drawPath(
            path = Path().apply {
                moveTo(left + cornerRadius + cornerLength, top + 10f)
                lineTo(left + cornerRadius, top + 10f)
                lineTo(left + cornerRadius, top + 10f + cornerLength)
            },
            color = greenColor,
            style = Stroke(width = cornerStroke, cap = StrokeCap.Round)
        )

        // Top-Right Corner
        drawPath(
            path = Path().apply {
                moveTo(left + boxSize - cornerRadius - cornerLength, top + 10f)
                lineTo(left + boxSize - cornerRadius, top + 10f)
                lineTo(left + boxSize - cornerRadius, top + 10f + cornerLength)
            },
            color = greenColor,
            style = Stroke(width = cornerStroke, cap = StrokeCap.Round)
        )

        // Bottom-Left Corner
        drawPath(
            path = Path().apply {
                moveTo(left + cornerRadius + cornerLength, top + boxSize - 10f)
                lineTo(left + cornerRadius, top + boxSize - 10f)
                lineTo(left + cornerRadius, top + boxSize - 10f - cornerLength)
            },
            color = greenColor,
            style = Stroke(width = cornerStroke, cap = StrokeCap.Round)
        )

        // Bottom-Right Corner
        drawPath(
            path = Path().apply {
                moveTo(left + boxSize - cornerRadius - cornerLength, top + boxSize - 10f)
                lineTo(left + boxSize - cornerRadius, top + boxSize - 10f)
                lineTo(left + boxSize - cornerRadius, top + boxSize - 10f - cornerLength)
            },
            color = greenColor,
            style = Stroke(width = cornerStroke, cap = StrokeCap.Round)
        )

        // 4. Smooth scanning laser
        val laserY = top + (boxSize * laserProgress)
        drawLine(
            color = greenColor.copy(alpha = 0.85f),
            start = Offset(left + 16.dp.toPx(), laserY),
            end = Offset(left + boxSize - 16.dp.toPx(), laserY),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}
