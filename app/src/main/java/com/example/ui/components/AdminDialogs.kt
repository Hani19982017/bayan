package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminUserAccount
import com.example.data.model.PaymentMethodConfig
import com.example.data.model.SubscriptionPaymentRequest
import com.example.ui.theme.LahoGreen
import com.example.ui.theme.LanaRed
import com.example.ui.theme.TawthiqAmber
import com.example.ui.theme.TawthiqPrimary
import com.example.ui.viewmodel.TawthiqViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Requirement 6: Change password for account recovery assistance
 */
@Composable
fun ChangeUserPasswordDialog(
    userAccount: AdminUserAccount,
    viewModel: TawthiqViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = TawthiqPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "تغيير كلمة المرور للحساب", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "المستخدم: ${userAccount.merchantName.ifBlank { userAccount.storeName }} (${userAccount.email})",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "كلمة المرور الحالية المسجلة: ${userAccount.password.ifBlank { "غير محددة" }}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TawthiqPrimary
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("كلمة المرور الجديدة *") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("تأكيد كلمة المرور *") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPassword.isBlank()) {
                            Toast.makeText(context, "يرجى إدخال كلمة المرور الجديدة", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newPassword != confirmPassword) {
                            Toast.makeText(context, "كلمتا المرور غير متطابقتين", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.updateUserPassword(userAccount.email, newPassword.trim())
                        Toast.makeText(context, "تم تحديث كلمة المرور للحساب بنجاح 🔑", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("حفظ التغيير", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                    Text("إلغاء")
                }
            }
        )
    }
}

/**
 * Requirement 7: Manage Subscriptions (Free, Weekly, Monthly, Yearly) & Expiry Date
 */
@Composable
fun ManageUserSubscriptionDialog(
    userAccount: AdminUserAccount,
    viewModel: TawthiqViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedPlan by remember { mutableStateOf(userAccount.plan.ifBlank { "مجاني" }) }
    var durationDays by remember {
        mutableIntStateOf(
            when (userAccount.plan.trim()) {
                "أسبوعي" -> 7
                "شهري" -> 30
                "سنوي" -> 365
                else -> 4
            }
        )
    }

    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    val currentExpiryFormatted = if (userAccount.subscriptionExpiry > 0) sdf.format(Date(userAccount.subscriptionExpiry)) else "غير محدد"

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = TawthiqPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "إدارة اشتراك المستخدم", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "المستخدم: ${userAccount.merchantName.ifBlank { userAccount.storeName }}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "تاريخ الانتهاء الحالي: $currentExpiryFormatted (${if (userAccount.isExpired) "منتهي ⚠️" else "ساري ✓"})",
                        fontSize = 12.sp,
                        color = if (userAccount.isExpired) LanaRed else LahoGreen
                    )

                    Text(
                        text = "اختر الباقة لتفعيلها أو تمديدها:",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )

                    val plans = listOf(
                        Triple("مجاني", "4 أيام تجريبية", 4),
                        Triple("أسبوعي", "7 أيام كاملة", 7),
                        Triple("شهري", "30 يوماً غير محدود", 30),
                        Triple("سنوي", "365 يوماً كامل المزايا", 365)
                    )

                    plans.forEach { (planName, subtitle, days) ->
                        val isSelected = selectedPlan == planName
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedPlan = planName
                                    durationDays = days
                                },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) TawthiqPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                            ),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, TawthiqPrimary) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = planName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = TawthiqPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateUserSubscription(userAccount.email, selectedPlan, durationDays)
                        Toast.makeText(context, "تم تفعيل باقة $selectedPlan وتحديث الصلاحية بنجاح 📅", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("حفظ وتفعيل", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                    Text("إلغاء")
                }
            }
        )
    }
}

/**
 * Requirement 4: Add / Edit Payment Methods (Wallets, Bank Accounts, Crypto)
 */
@Composable
fun AddEditPaymentMethodDialog(
    initialMethod: PaymentMethodConfig? = null,
    viewModel: TawthiqViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialMethod?.name ?: "") }
    var accountNumber by remember { mutableStateOf(initialMethod?.accountNumber ?: "") }
    var accountHolder by remember { mutableStateOf(initialMethod?.accountHolder ?: "") }
    var instructions by remember { mutableStateOf(initialMethod?.instructions ?: "") }
    var selectedIcon by remember { mutableStateOf(initialMethod?.iconName ?: "wallet") }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = null,
                        tint = TawthiqPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (initialMethod == null) "إضافة طريقة دفع / محفظة" else "تعديل طريقة الدفع",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم وسيلة الدفع / المحفظة *") },
                        placeholder = { Text("مثال: شام كاش، سيريتل كاش، USDT...") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it },
                        label = { Text("رقم المحفظة / الحساب / العنوان *") },
                        placeholder = { Text("مثال: 0987654321 أو عنوان المحفظة") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = accountHolder,
                        onValueChange = { accountHolder = it },
                        label = { Text("اسم صاحب الحساب / المحفظة") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        label = { Text("تعليمات التحويل للمستخدمين") },
                        placeholder = { Text("مثال: يرجى إرسال رقم العملية بعد التحويل...") },
                        maxLines = 2,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(text = "نوع الأيقونة:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Pair("wallet", "محفظة"),
                            Pair("phone", "هاتف"),
                            Pair("crypto", "كريبتو"),
                            Pair("bank", "بنك")
                        ).forEach { (iconKey, label) ->
                            val isSelected = selectedIcon == iconKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) TawthiqPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedIcon = iconKey }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank() || accountNumber.isBlank()) {
                            Toast.makeText(context, "يرجى تعبئة اسم الوسيلة ورقم الحساب", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val config = PaymentMethodConfig(
                            id = initialMethod?.id ?: "pm_${UUID.randomUUID().toString().take(8)}",
                            name = name.trim(),
                            accountNumber = accountNumber.trim(),
                            accountHolder = accountHolder.trim(),
                            instructions = instructions.trim(),
                            iconName = selectedIcon,
                            isActive = initialMethod?.isActive ?: true
                        )
                        if (initialMethod == null) {
                            viewModel.addPaymentMethod(config)
                            Toast.makeText(context, "تمت إضافة وسيلة الدفع بنجاح 💳", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.updatePaymentMethod(config)
                            Toast.makeText(context, "تم حفظ تعديلات وسيلة الدفع 💾", Toast.LENGTH_SHORT).show()
                        }
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("حفظ", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                    Text("إلغاء")
                }
            }
        )
    }
}

/**
 * Requirement 4 & Manual Payment Flow: Review and Approve/Reject Payment Request
 */
@Composable
fun ReviewPaymentRequestDialog(
    request: SubscriptionPaymentRequest,
    viewModel: TawthiqViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var rejectReason by remember { mutableStateOf("") }
    var isRejecting by remember { mutableStateOf(false) }

    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    val dateFormatted = sdf.format(Date(request.createdAt))

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تفاصيل طلب التحويل والاشتراك",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "المشترك: ${request.userName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "البريد الإلكتروني: ${request.userEmail}", fontSize = 12.sp)
                            Text(text = "رقم هاتف المشترك: ${request.userPhone}", fontSize = 12.sp)
                            Text(text = "تاريخ الإرسال: $dateFormatted", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = TawthiqPrimary.copy(alpha = 0.08f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "الباقة المطلوبة: باقة ${request.planName} (${request.planPrice})", fontWeight = FontWeight.Bold, color = TawthiqPrimary)
                            Text(text = "طريقة الدفع: ${request.paymentMethodName}", fontSize = 13.sp)
                            Text(text = "رقم العملية / الحوالة: ${request.transferNumber}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (request.senderPhone.isNotBlank()) {
                                Text(text = "رقم المحوّل: ${request.senderPhone}", fontSize = 12.sp)
                            }
                            if (request.notes.isNotBlank()) {
                                Text(text = "ملاحظات: ${request.notes}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    if (isRejecting) {
                        OutlinedTextField(
                            value = rejectReason,
                            onValueChange = { rejectReason = it },
                            label = { Text("سبب الرفض *") },
                            placeholder = { Text("مثال: رقم العملية غير صحيح، لم يصل التحويل...") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                if (!isRejecting) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.approvePaymentRequest(request.id, context)
                                Toast.makeText(context, "تم قبول الطلب وتفعيل باقة ${request.planName} فوراً بنجاح! 🚀", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LahoGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("قبول وتفعيل", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { isRejecting = true },
                            colors = ButtonDefaults.buttonColors(containerColor = LanaRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("رفض الطلب")
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            viewModel.rejectPaymentRequest(request.id, rejectReason.ifBlank { "البيانات غير مطابقة" })
                            Toast.makeText(context, "تم رفض الطلب", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LanaRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("تأكيد الرفض", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                    Text("إغلاق")
                }
            }
        )
    }
}

/**
 * Requirement 5: Send Broadcast Messages to all users
 */
@Composable
fun SendBroadcastMessageDialog(
    viewModel: TawthiqViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "إرسال رسالة جماعية للمستخدمين 📢", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "سيصل هذا الإشعار فوريّاً لكافة مستخدمي التطبيق على أجهزتهم مصحوباً بنغمة تنبيه وشعار البيان.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("عنوان الرسالة *") },
                        placeholder = { Text("مثال: تحديث أمني جديد، عرض باقات الاشتراك...") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("نص الرسالة والتفاصيل *") },
                        placeholder = { Text("اكتب محتوى الرسالة الجماعية هنا...") },
                        maxLines = 4,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isBlank() || message.isBlank()) {
                            Toast.makeText(context, "يرجى كتابة العنوان ونص الرسالة", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.sendBroadcastMessage(title.trim(), message.trim(), context)
                        Toast.makeText(context, "تم إرسال الرسالة الجماعية وبث الإشعار بنجاح! 🚀", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("بث الرسالة فوراً", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun SendUserDirectMessageDialog(
    userAccount: AdminUserAccount,
    viewModel: TawthiqViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "مراسلة: ${userAccount.merchantName.ifBlank { userAccount.storeName }}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "سيصل هذا الإشعار والرسالة مباشرة لجهاز المستخدم (${userAccount.email}) فوراً وبشكل خاص.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("عنوان الرسالة *") },
                        placeholder = { Text("مثال: تنبيه هام، تحديث بياناتك...") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("نص الرسالة *") },
                        placeholder = { Text("اكتب محتوى الرسالة الموجهة لهذا المستخدم...") },
                        maxLines = 4,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isBlank() || message.isBlank()) {
                            Toast.makeText(context, "يرجى كتابة العنوان ونص الرسالة", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.sendDirectMessageToUser(userAccount.email, title.trim(), message.trim(), context)
                        Toast.makeText(context, "تم إرسال الرسالة إلى المستخدم بنجاح! 🚀", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إرسال للمستخدم فوراً", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun AddAdminUserDialog(
    viewModel: TawthiqViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var storeName by remember { mutableStateOf("") }
    var emailOrUser by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("123456") }
    var plan by remember { mutableStateOf("مجاني") }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = TawthiqPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إضافة مستخدم جديد للنظام", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "يمكنك إضافة أي تاجر أو مستخدم جديد (مثل: menesy) وسيتصل حسابه بالسحابة فوراً.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم التاجر / المستخدم *") },
                        placeholder = { Text("مثال: menesy أو محمد أحمد") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = emailOrUser,
                        onValueChange = { emailOrUser = it },
                        label = { Text("اسم الدخول أو البريد *") },
                        placeholder = { Text("مثال: menesy أو menesy@gmail.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("اسم المتجر (اختياري)") },
                        placeholder = { Text("مثال: متجر البيان") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الهاتف (اختياري)") },
                        placeholder = { Text("+9665...") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("كلمة المرور") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("مجاني", "شهري", "سنوي", "شامل").forEach { p ->
                            val isSel = plan == p
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) TawthiqPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { plan = p }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = p,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val inputUser = emailOrUser.ifBlank { name }.trim()
                        if (inputUser.isBlank()) {
                            Toast.makeText(context, "يرجى إدخال اسم المستخدم أو البريد", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val finalName = name.ifBlank { inputUser }
                        val finalStore = storeName.ifBlank { "متجر $finalName" }

                        viewModel.createAdminUser(
                            emailOrUsername = inputUser,
                            merchantName = finalName,
                            storeName = finalStore,
                            phone = phone.trim(),
                            password = password.ifBlank { "123456" },
                            status = "نشط",
                            plan = plan
                        ) { success, message ->
                            if (success) {
                                Toast.makeText(context, "تمت إضافة المستخدم ($finalName) بنجاح ✓", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            } else {
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("إضافة وحفظ الحساب", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                    Text("إلغاء")
                }
            }
        )
    }
}
