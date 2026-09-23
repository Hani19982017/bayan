package com.example.ui.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.data.model.AccountEntity
import com.example.ui.theme.LahoGreenText
import com.example.data.model.TransactionEntity
import com.example.ui.theme.LanaRed
import com.example.ui.theme.TawthiqPrimary
import com.example.util.QrGeneratorHelper
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder

@Composable
fun CustomerQrStatementDialog(
    account: AccountEntity,
    netBalance: Double,
    currency: String,
    storeName: String,
    transactions: List<TransactionEntity> = emptyList(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val statementPayload = remember(account, storeName, netBalance, currency, transactions) {
        val encodedName = try { URLEncoder.encode(account.name.trim(), "UTF-8") } catch (_: Exception) { account.name.trim() }
        val encodedStore = try { URLEncoder.encode(storeName.trim(), "UTF-8") } catch (_: Exception) { storeName.trim() }
        val cleanCur = currency.ifBlank { "USD" }
        
        // Encode transactions: desc,amount,type,date,receipt
        val txsEncoded = transactions.take(60).joinToString(";") { tx ->
            val cleanDesc = try {
                URLEncoder.encode(tx.description.ifBlank { if (tx.type == "LANA") "إرسال (لنا)" else "استلام (له)" }.trim(), "UTF-8")
            } catch (_: Exception) { "معاملة" }
            val cleanReceipt = try { URLEncoder.encode(tx.receiptNumber.trim(), "UTF-8") } catch (_: Exception) { "" }
            "$cleanDesc,${tx.amount},${tx.type},${tx.date},$cleanReceipt"
        }
        val syncKey = com.example.util.FirebaseSyncManager.extractSyncKey(account)
        val syncParam = "&syncKey=$syncKey"
        val txsParam = if (txsEncoded.isNotBlank()) "&txs=$txsEncoded" else ""
        "https://tawthiq.app/statement?id=${account.id}&name=$encodedName&store=$encodedStore&phone=${account.phone}&cur=$cleanCur&bal=$netBalance$syncParam$txsParam"
    }

    val qrBitmap = remember(statementPayload) {
        QrGeneratorHelper.generateQrBitmap(
            content = statementPayload,
            width = 600,
            height = 600
        )
    }

    androidx.compose.runtime.LaunchedEffect(account.id, transactions) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            transactions.forEach { tx ->
                com.example.util.FirebaseSyncManager.pushTransaction(
                    account = account,
                    transaction = tx,
                    storeName = storeName,
                    merchantEmail = account.userEmail
                )
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "📱 باركود الزبون (QR)",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TawthiqPrimary
                        )
                        Text(
                            text = "كشف حساب: ${account.name} (#${if (account.displayIndex > 0) account.displayIndex else account.id})",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(modifier = Modifier.size(40.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // QR Code Container
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(2.dp, TawthiqPrimary.copy(alpha = 0.3f)),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .size(230.dp)
                        .padding(4.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "باركود كشف الحساب",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            )
                        } else {
                            CircularProgressIndicator(color = TawthiqPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Balance summary badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (netBalance > 0) LanaRed.copy(alpha = 0.08f) else if (netBalance < 0) LahoGreenText.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = when {
                            netBalance > 0 -> "المبلغ المستحق (لك): ${formatMoney(netBalance)} $currency"
                            netBalance < 0 -> "المبلغ المستحق (له): ${formatMoney(-netBalance)} $currency"
                            else -> "الحساب مسدد بالكامل (0.00 $currency)"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (netBalance > 0) LanaRed else if (netBalance < 0) LahoGreenText else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // How it works instructions
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = TawthiqPrimary.copy(alpha = 0.06f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TawthiqPrimary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = TawthiqPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "كيف يشاهد الزبون معاملاته على هاتفه؟",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TawthiqPrimary
                            )
                        }
                        Text(
                            text = "1️⃣ افتح تطبيق البيان على هاتف الزبون.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "2️⃣ اضغط على زر 'مسح باركود الزبون عبر QR' (في شاشة الدخول أو من علامة الكاميرا في الأعلى).",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "3️⃣ صوّر هذا الباركود بكاميرا الزبون ليفتح كشف حسابه مباشرة بكافة معاملاته دون تعديل.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Button: Send ONLY the QR image via WhatsApp (NO links!)
                Button(
                    onClick = {
                        if (qrBitmap != null) {
                            shareQrImageViaWhatsApp(context, qrBitmap)
                        } else {
                            Toast.makeText(context, "جاري تجهيز الباركود...", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "مشاركة عبر واتساب",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "مشاركة صورة الباركود فقط عبر واتساب",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary Button: Save image to gallery
                OutlinedButton(
                    onClick = {
                        if (qrBitmap != null) {
                            saveQrImageToGallery(context, qrBitmap)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "حفظ",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "حفظ صورة الباركود في المعرض",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Shares ONLY the QR image file to WhatsApp without any text or URL links.
 */
private fun shareQrImageViaWhatsApp(context: Context, qrBitmap: Bitmap) {
    try {
        val cacheDir = File(context.cacheDir, "exports")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val imageFile = File(cacheDir, "customer_qr_${System.currentTimeMillis()}.png")
        val stream = FileOutputStream(imageFile)
        qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.flush()
        stream.close()

        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )

        val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            // Explicitly DO NOT include Intent.EXTRA_TEXT with any URLs or links
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.whatsapp")
        }

        val pm = context.packageManager
        if (whatsappIntent.resolveActivity(pm) != null) {
            context.startActivity(whatsappIntent)
            return
        }

        val waBusinessIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.whatsapp.w4b")
        }
        if (waBusinessIntent.resolveActivity(pm) != null) {
            context.startActivity(waBusinessIntent)
            return
        }

        // Fallback: standard share chooser (pure image stream, no text/link)
        val chooser = Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            "إرسال صورة الباركود"
        ).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "تعذر مشاركة صورة الباركود: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Saves the QR bitmap directly to the device's Pictures/Tawthiq gallery folder.
 */
private fun saveQrImageToGallery(context: Context, qrBitmap: Bitmap) {
    try {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "tawthiq_qr_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Tawthiq")
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            resolver.openOutputStream(uri)?.use { out ->
                qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            Toast.makeText(context, "تم حفظ صورة الباركود في المعرض بنجاح ✓", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "تعذر حفظ الصورة: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
