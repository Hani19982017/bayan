package com.example.ui.screens

import android.app.Activity
import android.accounts.AccountManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.LahoGreen
import com.example.ui.theme.TawthiqPrimary
import com.example.ui.viewmodel.TawthiqViewModel
import com.example.util.GoogleAuthHelper
import com.example.util.TawthiqNotificationManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: TawthiqViewModel,
    onLoginSuccess: () -> Unit = {},
    onOpenQrScanner: () -> Unit = {}
) {
    val context = LocalContext.current

    var showHelpDialog by remember { mutableStateOf(false) }
    var showVideoDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showDirectEmailDialog by remember { mutableStateOf(false) }
    var isAuthenticating by remember { mutableStateOf(false) }

    fun authenticateWithEmail(email: String, name: String = "") {
        val cleanEmail = email.trim().lowercase()
        val derivedName = if (name.isNotBlank()) name else cleanEmail.substringBefore("@")
        val isNew = viewModel.isNewAccount(cleanEmail)

        viewModel.loginWithEmail(
            email = cleanEmail,
            merchant = derivedName,
            store = "متجر $derivedName"
        )
        isAuthenticating = false

        if (isNew) {
            TawthiqNotificationManager.sendWelcomePushNotification(context, force = true, merchantEmail = cleanEmail)
            Toast.makeText(context, "مرحباً بك في البيان! بدأت الآن فترة تجربتك المجانية لمدة 4 أيام ✨", Toast.LENGTH_LONG).show()
        } else {
            TawthiqNotificationManager.sendReturningWelcomePushNotification(context, merchantName = derivedName, merchantEmail = cleanEmail)
            Toast.makeText(context, "أهلاً بك مجدداً في البيان يا $derivedName 👋", Toast.LENGTH_LONG).show()
        }
        onLoginSuccess()
    }

    // Result launcher for Google Account picker
    val googleAccountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isAuthenticating = false
        val account = GoogleAuthHelper.getAccountFromIntent(result.data)
        if (account != null && account.email.isNotBlank()) {
            authenticateWithEmail(account.email, account.displayName)
        } else {
            val selectedEmail = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                ?: result.data?.getStringExtra("accountName")
                ?: result.data?.getStringExtra("authAccount")
                ?: result.data?.getStringExtra("email")
            if (!selectedEmail.isNullOrBlank() && selectedEmail.contains("@")) {
                authenticateWithEmail(selectedEmail)
            }
        }
    }

    fun launchGoogleLogin() {
        isAuthenticating = true
        try {
            // Android OS Native Account Chooser: always reliable on all devices
            val intent = GoogleAuthHelper.createAccountChooserIntent()
            googleAccountPickerLauncher.launch(intent)
        } catch (e: Exception) {
            try {
                val client = GoogleAuthHelper.getGoogleSignInClient(context)
                client.signOut().addOnCompleteListener {
                    googleAccountPickerLauncher.launch(client.signInIntent)
                }
            } catch (ex: Exception) {
                isAuthenticating = false
                showDirectEmailDialog = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "تسجيل الدخول",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            // 1. Central Illustration: Al-Bayan Brand Logo with beautiful glow
            Surface(
                modifier = Modifier
                    .width(220.dp)
                    .height(138.dp)
                    .testTag("login_bayan_logo"),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF0284C7).copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.bayan_logo),
                        contentDescription = "شعار البيان",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // 2. Main Title
            Text(
                text = "أهلاً بك في البيان",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Subtitle
            Text(
                text = "سجّل الدخول لمتابعة إدارة حساباتك بسهولة وأمان",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 4. Primary Button: "متابعة باستخدام Google"
            Surface(
                onClick = { launchGoogleLogin() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = Color.Black.copy(alpha = 0.08f),
                        spotColor = Color.Black.copy(alpha = 0.08f)
                    )
                    .testTag("google_login_button"),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isAuthenticating) "جارٍ الاتصال بـ Google..." else "متابعة باستخدام Google",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    GoogleLogo(modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 5. Divider: "أو"
            Text(
                text = "أو",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 6. Secondary Button: "مسح باركود الزبون / متابعة كشف الحساب عبر QR"
            Surface(
                onClick = onOpenQrScanner,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("qr_login_button"),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.2.dp, TawthiqPrimary.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "QR",
                        tint = TawthiqPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "مسح باركود الزبون / متابعة الحساب (QR)",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TawthiqPrimary
                        )
                        Text(
                            text = "للزبائن: اضغط هنا لتصوير باركود التاجر ومتابعة معاملاتك فوراً",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6b. Email / Username quick entry link
            TextButton(
                onClick = { showDirectEmailDialog = true },
                modifier = Modifier.testTag("direct_email_login_btn")
            ) {
                Icon(Icons.Default.Email, contentDescription = null, tint = TawthiqPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "تسجيل الدخول باسم المستخدم أو البريد الإلكتروني",
                    color = TawthiqPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 7. Pills Row: "كيف اسجل الدخول؟" & "الفيديو التعريفي"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pill 1: كيف اسجل الدخول؟
                HelpPill(
                    text = "كيف اسجل الدخول؟",
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .border(1.5.dp, TawthiqPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "!",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TawthiqPrimary
                            )
                        }
                    },
                    onClick = { showHelpDialog = true },
                    modifier = Modifier.weight(1f)
                )

                // Pill 2: الفيديو التعريفي
                HelpPill(
                    text = "الفيديو التعريفي",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.OndemandVideo,
                            contentDescription = null,
                            tint = TawthiqPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    },
                    onClick = { showVideoDialog = true },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // 8. Footer: Security Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "بياناتك محفوظة بأمان عبر التخزين السحابي",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = LahoGreen,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 9. Footer Terms & Privacy
            Text(
                text = "عند المتابعة، أنت موافق على",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "شروط الإستخدام و سياسة الخصوصية",
                fontSize = 13.sp,
                color = TawthiqPrimary,
                fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clickable { showTermsDialog = true }
                    .padding(4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Direct Email Login Dialog
    if (showDirectEmailDialog) {
        var directEmail by remember { mutableStateOf("") }
        var directName by remember { mutableStateOf("") }
        var emailError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showDirectEmailDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = TawthiqPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("الدخول باسم المستخدم أو البريد", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("أدخل اسم المستخدم (مثال: menesy) أو البريد الإلكتروني لإدارة حساباتك المالية:")
                    OutlinedTextField(
                        value = directEmail,
                        onValueChange = { 
                            directEmail = it
                            emailError = null
                        },
                        label = { Text("اسم المستخدم أو البريد (مثال: menesy أو name@gmail.com)") },
                        isError = emailError != null,
                        supportingText = {
                            if (emailError != null) {
                                Text(emailError!!, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = directName,
                        onValueChange = { directName = it },
                        label = { Text("اسم التاجر / المتجر (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = directEmail.trim().lowercase()
                        if (trimmed.isBlank()) {
                            emailError = "يرجى كتابة اسم المستخدم أو البريد الإلكتروني"
                            return@Button
                        }
                        val effectiveEmail = if (trimmed.contains("@")) trimmed else "$trimmed@tawthiq.app"
                        val name = if (directName.isNotBlank()) directName.trim() else trimmed.substringBefore("@")
                        viewModel.loginWithEmail(
                            email = effectiveEmail,
                            merchant = name,
                            store = if (directName.isNotBlank()) "متجر ${directName.trim()}" else "متجر $name"
                        )
                        TawthiqNotificationManager.sendWelcomePushNotification(context, force = true, merchantEmail = effectiveEmail)
                        showDirectEmailDialog = false
                        Toast.makeText(context, "تم تسجيل الدخول بنجاح ✓", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary)
                ) {
                    Text("تسجيل الدخول")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDirectEmailDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Help Dialog: كيف اسجل الدخول؟
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = TawthiqPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("كيف تسجل الدخول في البيان؟", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text("1. الدخول عبر Google: اضغط على 'متابعة باستخدام Google' واختر حسابك لتسجيل الدخول الفوري ومزامنة الحسابات بأمان.", fontSize = 13.sp)
                    Text("2. الدخول عبر رمز QR: اضغط على 'متابعة كمستخدم عبر QR' وافتح الكاميرا لمسح رمز الحساب الذي أنشأه المسؤول مسبقاً.", fontSize = 13.sp)
                    Text("3. الدخول بالبريد المباشر: يمكنك أيضاً إدخال بريدك الإلكتروني الشخصي ومتابعة إدارة سجلاتك.", fontSize = 13.sp)
                    Text("4. الأمان: جميع بيانات دفاتر الديون مشفرة ومحفوظة لحسابك فقط.", fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHelpDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary)
                ) {
                    Text("حسناً، فهمت")
                }
            }
        )
    }

    // Video Guide Dialog: الفيديو التعريفي
    if (showVideoDialog) {
        var currentStep by remember { mutableIntStateOf(0) }
        val steps = listOf(
            "1. تسجيل الدخول واختيار حساب Google لمزامنة البيانات السحابية بأمان.",
            "2. مسح رمز QR الخاص بالمسؤول أو التاجر للربط المباشر مع الحساب.",
            "3. إضافة حسابات العملاء وتسجيل فواتير (لنا) و (له).",
            "4. إرسال الفاتورة تلقائياً بضغطة زر واحدة عبر الواتساب فور الحفظ وتصدير ملف Excel."
        )

        AlertDialog(
            onDismissRequest = { showVideoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.OndemandVideo, contentDescription = null, tint = TawthiqPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("دليل استخدام تطبيق البيان", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                            .clickable {
                                currentStep = (currentStep + 1) % steps.size
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFF22C55E),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "خطوة ${currentStep + 1} من ${steps.size} (اضغط للمتابعة)",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = steps[currentStep],
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = { if (currentStep > 0) currentStep-- },
                            enabled = currentStep > 0
                        ) {
                            Text("السابق")
                        }
                        TextButton(
                            onClick = { if (currentStep < steps.size - 1) currentStep++ },
                            enabled = currentStep < steps.size - 1
                        ) {
                            Text("التالي")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showVideoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary)
                ) {
                    Text("إغلاق")
                }
            }
        )
    }

    // Terms & Privacy Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("شروط الاستخدام وسياسة الخصوصية", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        """
                        • حماية البيانات: نلتزم بأعلى معايير التشفير للحفاظ على سرية سجلاتك المالية وبيانات عملائك.
                        • الملكية التامة: جميع السجلات والمعاملات المسجلة في دفتر الديون ملك للتاجر والمستخدم فقط.
                        • النسخ الاحتياطي: يتم تفعيل الحفظ السحابي والمحلي لضمان عدم ضياع الديون عند تغيير الجهاز.
                        • تصدير البيانات: يحق للمستخدم تصدير كشوف حساباته إلى Excel في أي وقت بدون أي قيود.
                        """.trimIndent(),
                        fontSize = 13.sp,
                        lineHeight = 22.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showTermsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary)
                ) {
                    Text("موافق")
                }
            }
        )
    }
}

/**
 * Custom Compose Canvas reproducing the exact visual from screenshot 1:
 * A fluffy gradient cyan/blue cloud behind a warm beige ledger notebook with spiral rings and a glowing cyan key link.
 */
@Composable
fun CloudLedgerIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Draw Fluffy Cyan/Blue Cloud in the background
        val cloudBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF38BDF8),
                Color(0xFF0284C7)
            ),
            startY = h * 0.1f,
            endY = h * 0.95f
        )

        // Overlapping cloud bubbles
        drawCircle(
            brush = cloudBrush,
            radius = w * 0.30f,
            center = Offset(w * 0.50f, h * 0.45f)
        )
        drawCircle(
            brush = cloudBrush,
            radius = w * 0.22f,
            center = Offset(w * 0.30f, h * 0.55f)
        )
        drawCircle(
            brush = cloudBrush,
            radius = w * 0.23f,
            center = Offset(w * 0.70f, h * 0.55f)
        )
        drawCircle(
            brush = cloudBrush,
            radius = w * 0.18f,
            center = Offset(w * 0.24f, h * 0.68f)
        )
        drawCircle(
            brush = cloudBrush,
            radius = w * 0.18f,
            center = Offset(w * 0.76f, h * 0.68f)
        )

        // Cloud base pill
        drawRoundRect(
            brush = cloudBrush,
            topLeft = Offset(w * 0.20f, h * 0.60f),
            size = Size(w * 0.60f, h * 0.28f),
            cornerRadius = CornerRadius(w * 0.14f, w * 0.14f)
        )

        // 2. Beige / Golden Orange Ledger Notebook in Foreground
        val bookLeft = w * 0.48f
        val bookTop = h * 0.25f
        val bookWidth = w * 0.38f
        val bookHeight = h * 0.60f
        val bookCorner = 18.dp.toPx()

        val notebookBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFCD34D),
                Color(0xFFF59E0B)
            ),
            startY = bookTop,
            endY = bookTop + bookHeight
        )

        drawRoundRect(
            brush = notebookBrush,
            topLeft = Offset(bookLeft, bookTop),
            size = Size(bookWidth, bookHeight),
            cornerRadius = CornerRadius(bookCorner, bookCorner)
        )

        // Notebook Lines
        val lineSpacing = bookHeight * 0.11f
        val lineStartX = bookLeft + bookWidth * 0.28f
        val lineEndX = bookLeft + bookWidth * 0.85f

        for (i in 2..5) {
            val lineY = bookTop + i * lineSpacing
            drawLine(
                color = Color(0xFFD97706).copy(alpha = 0.6f),
                start = Offset(lineStartX, lineY),
                end = Offset(lineEndX, lineY),
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // 3. Notebook Spiral Rings on left edge
        val ringCount = 6
        val ringSpacing = bookHeight / (ringCount + 1)
        val ringRadius = 5.dp.toPx()

        for (i in 1..ringCount) {
            val ringCenterY = bookTop + i * ringSpacing
            val ringCenterX = bookLeft + bookWidth * 0.12f

            drawCircle(
                color = Color(0xFFB45309),
                radius = ringRadius,
                center = Offset(ringCenterX, ringCenterY)
            )

            val loopPath = Path().apply {
                moveTo(ringCenterX - ringRadius * 1.5f, ringCenterY - ringRadius)
                cubicTo(
                    ringCenterX - ringRadius * 3f, ringCenterY - ringRadius,
                    ringCenterX - ringRadius * 3f, ringCenterY + ringRadius,
                    ringCenterX - ringRadius * 1.5f, ringCenterY + ringRadius
                )
            }
            drawPath(
                path = loopPath,
                color = Color(0xFFD97706),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // 4. Glowing Cyan Key / Bridge Connector Node in Center
        val keyCenterY = h * 0.58f
        val keyLeftX = w * 0.42f
        val keyRightX = w * 0.60f
        val keyBarWidth = keyRightX - keyLeftX

        drawRoundRect(
            color = Color(0xFF06B6D4),
            topLeft = Offset(keyLeftX, keyCenterY - 7.dp.toPx()),
            size = Size(keyBarWidth, 14.dp.toPx()),
            cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
        )

        drawCircle(
            color = Color(0xFF06B6D4),
            radius = 16.dp.toPx(),
            center = Offset(keyLeftX, keyCenterY)
        )
        drawCircle(
            color = Color.White,
            radius = 7.dp.toPx(),
            center = Offset(keyLeftX, keyCenterY)
        )

        drawCircle(
            color = Color(0xFF06B6D4),
            radius = 14.dp.toPx(),
            center = Offset(keyRightX, keyCenterY)
        )
        drawCircle(
            color = Color.White,
            radius = 6.dp.toPx(),
            center = Offset(keyRightX, keyCenterY)
        )
    }
}

/**
 * Clean Google "G" 4-color vector logo
 */
@Composable
fun GoogleLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = w * 0.46f

        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = Offset(cx - r, cy - r),
            size = Size(r * 2, r * 2)
        )

        drawArc(
            color = Color(0xFF34A853),
            startAngle = 45f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = Offset(cx - r, cy - r),
            size = Size(r * 2, r * 2)
        )

        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 135f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = Offset(cx - r, cy - r),
            size = Size(r * 2, r * 2)
        )

        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = Offset(cx - r, cy - r),
            size = Size(r * 2, r * 2)
        )

        drawCircle(
            color = Color.White,
            radius = r * 0.58f,
            center = Offset(cx, cy)
        )

        drawRect(
            color = Color(0xFF4285F4),
            topLeft = Offset(cx, cy - r * 0.22f),
            size = Size(r * 0.95f, r * 0.44f)
        )
    }
}

/**
 * Reusable Help Pill Button for bottom links
 */
@Composable
fun HelpPill(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, TawthiqPrimary.copy(alpha = 0.35f)),
        modifier = modifier.height(44.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(6.dp))
            icon()
        }
    }
}
