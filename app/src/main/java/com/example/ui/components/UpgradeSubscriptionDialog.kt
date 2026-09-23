package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PaymentMethodConfig
import com.example.data.model.SubscriptionInfo
import com.example.data.model.SubscriptionPaymentRequest
import com.example.ui.theme.LahoGreen
import com.example.ui.theme.TawthiqAmber
import com.example.ui.theme.TawthiqPrimary
import com.example.ui.viewmodel.TawthiqViewModel
import java.util.UUID

@Composable
fun UpgradeSubscriptionDialog(
    viewModel: TawthiqViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val subscriptionInfo by viewModel.subscriptionInfo.collectAsStateWithLifecycle()
    val paymentMethods by viewModel.paymentMethods.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
    val merchantName by viewModel.merchantName.collectAsStateWithLifecycle()
    val merchantPhone by viewModel.merchantPhone.collectAsStateWithLifecycle()

    var selectedPlan by remember { mutableStateOf("شهري") }
    var selectedPlanPrice by remember { mutableStateOf("5$") }

    val activeMethods = remember(paymentMethods) { paymentMethods.filter { it.isActive } }
    var selectedMethod by remember(activeMethods) {
        mutableStateOf<PaymentMethodConfig?>(activeMethods.firstOrNull())
    }

    var transferNumber by remember { mutableStateOf("") }
    var senderPhone by remember { mutableStateOf(merchantPhone) }
    var notes by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {},
            dismissButton = {},
            title = null,
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    // Header Banner
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
                                        )
                                    )
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "ترقية خطة الاشتراك 💎",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = if (subscriptionInfo.isPro) "حساب مفعل" else "خطة مجانية",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text(
                                        text = "اختر الباقة المناسبة، وحوّل المبلغ عبر المحفظة الإلكترونية المعتمدة، ثم أرسل إشعار التحويل ليتم التفعيل فوراً من قبل الإدارة.",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    if (isSubmitted) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = LahoGreen.copy(alpha = 0.1f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = LahoGreen,
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Text(
                                        text = "تم إرسال طلب التفعيل بنجاح! 🚀",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = LahoGreen,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "وصل إشعار بالدفعة إلى إدارة التطبيق، وسيتم التحقق من الحوالة وتفعيل باقتك خلال دقائق معدودة.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(
                                        onClick = onDismiss,
                                        colors = ButtonDefaults.buttonColors(containerColor = LahoGreen),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("حسناً، تم", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        // 1. Select Plan
                        item {
                            Text(
                                text = "1. اختر باقة الاشتراك:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PlanOptionCard(
                                    title = "أسبوعي",
                                    price = "2$",
                                    duration = "7 أيام",
                                    isSelected = selectedPlan == "أسبوعي",
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selectedPlan = "أسبوعي"
                                        selectedPlanPrice = "2$"
                                    }
                                )
                                PlanOptionCard(
                                    title = "شهري",
                                    price = "5$",
                                    duration = "30 يوماً",
                                    tag = "الأكثر طلباً",
                                    isSelected = selectedPlan == "شهري",
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selectedPlan = "شهري"
                                        selectedPlanPrice = "5$"
                                    }
                                )
                                PlanOptionCard(
                                    title = "سنوي",
                                    price = "45$",
                                    duration = "365 يوماً",
                                    tag = "توفير 25%",
                                    isSelected = selectedPlan == "سنوي",
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selectedPlan = "سنوي"
                                        selectedPlanPrice = "45$"
                                    }
                                )
                            }
                        }

                        // 2. Select Payment Method
                        item {
                            Text(
                                text = "2. اختر وسيلة الدفع للتحويل إليها:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                activeMethods.forEach { method ->
                                    val isSelected = selectedMethod?.id == method.id
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedMethod = method },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) TawthiqPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                                        ),
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, TawthiqPrimary) else null
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(CircleShape)
                                                        .background(TawthiqPrimary.copy(alpha = 0.12f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = getPaymentIcon(method.iconName),
                                                        contentDescription = null,
                                                        tint = TawthiqPrimary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = method.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = method.accountHolder,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = TawthiqPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Wallet Number & Details Card (with 1-click copy)
                        selectedMethod?.let { method ->
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "رقم المحفظة / الحساب للتحويل:",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(TawthiqPrimary.copy(alpha = 0.12f))
                                                    .clickable {
                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                        clipboard.setPrimaryClip(ClipData.newPlainText("Payment Number", method.accountNumber))
                                                        Toast.makeText(context, "تم نسخ الرقم إلى الحافظة بنجاح 📋", Toast.LENGTH_SHORT).show()
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "نسخ",
                                                    tint = TawthiqPrimary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "نسخ الرقم",
                                                    color = TawthiqPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        Text(
                                            text = method.accountNumber,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        if (method.instructions.isNotBlank()) {
                                            Text(
                                                text = "💡 تعليمات: ${method.instructions}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Enter Payment Receipt info
                        item {
                            Text(
                                text = "3. بيانات التحويل بعد إتمام العملية:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = transferNumber,
                                onValueChange = { transferNumber = it },
                                label = { Text("رقم العملية / الحوالة / السند *") },
                                placeholder = { Text("مثال: TRX-982312") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = senderPhone,
                                onValueChange = { senderPhone = it },
                                label = { Text("رقم هاتف المحوّل *") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("ملاحظات إضافية أو اسم المتجر") },
                                placeholder = { Text("أدخل أي تفاصيل مساعدة للإدارة...") },
                                maxLines = 2,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("إلغاء")
                                }

                                Button(
                                    onClick = {
                                        if (transferNumber.isBlank()) {
                                            Toast.makeText(context, "يرجى كتابة رقم العملية أو إشعار التحويل", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        val req = SubscriptionPaymentRequest(
                                            id = "req_${UUID.randomUUID().toString().take(8)}",
                                            userEmail = userEmail.ifBlank { "user@bayan.app" },
                                            userName = merchantName.ifBlank { "مشترك جديد" },
                                            userPhone = senderPhone.ifBlank { merchantPhone },
                                            planName = selectedPlan,
                                            planPrice = selectedPlanPrice,
                                            paymentMethodName = selectedMethod?.name ?: "تحويل محفظة",
                                            transferNumber = transferNumber.trim(),
                                            senderPhone = senderPhone.trim(),
                                            notes = notes.trim(),
                                            status = "PENDING",
                                            createdAt = System.currentTimeMillis()
                                        )
                                        viewModel.submitPaymentRequest(req, context)
                                        isSubmitted = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1.5f)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إرسال الإشعار للإدارة", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun PlanOptionCard(
    title: String,
    price: String,
    duration: String,
    tag: String? = null,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) TawthiqPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, TawthiqPrimary) else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (tag != null) {
                Box(
                    modifier = Modifier
                        .background(TawthiqAmber, RoundedCornerShape(10.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = tag, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Spacer(modifier = Modifier.height(13.dp))
            }

            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isSelected) TawthiqPrimary else MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = price,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = if (isSelected) TawthiqPrimary else MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = duration,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getPaymentIcon(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "wallet" -> Icons.Default.AccountBalanceWallet
        "phone" -> Icons.Default.PhoneAndroid
        "crypto" -> Icons.Default.CurrencyBitcoin
        "bank" -> Icons.Default.AccountBalance
        else -> Icons.Default.AccountBalanceWallet
    }
}
