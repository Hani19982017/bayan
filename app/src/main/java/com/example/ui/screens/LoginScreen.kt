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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.util.MerchantAuthService
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
    var showPasswordRecoveryDialog by remember { mutableStateOf(false) }
    var isAuthenticating by remember { mutableStateOf(false) }

    // Tab state: 0 = تسجيل الدخول, 1 = إنشاء حساب تاجر جديد
    var selectedAuthTab by remember { mutableIntStateOf(0) }

    // Registration states
    var regEmail by remember { mutableStateOf("") }
    var regMerchantName by remember { mutableStateOf("") }
    var regStoreName by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var regPasswordVisible by remember { mutableStateOf(false) }
    var regErrorMessage by remember { mutableStateOf<String?>(null) }
    var isRegistering by remember { mutableStateOf(false) }

    // Login states
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }
    var loginErrorMessage by remember { mutableStateOf<String?>(null) }
    var isLoggingIn by remember { mutableStateOf(false) }

    // Password Recovery states
    var recoveryEmail by remember { mutableStateOf("") }
    var recoveryNewPassword by remember { mutableStateOf("") }
    var recoveryConfirmPassword by remember { mutableStateOf("") }
    var recoveryErrorMessage by remember { mutableStateOf<String?>(null) }
    var isRecovering by remember { mutableStateOf(false) }

    fun handleRegistrationSubmit() {
        val cleanEmail = MerchantAuthService.normalizeEmail(regEmail)
        if (!MerchantAuthService.isValidEmail(cleanEmail)) {
            regErrorMessage = MerchantAuthService.ERR_INVALID_EMAIL
            return
        }
        if (regMerchantName.trim().isBlank()) {
            regErrorMessage = "يرجى إدخال اسم التاجر / المالك"
            return
        }
        if (regPassword.length < 6) {
            regErrorMessage = "يجب أن تكون كلمة المرور 6 أحرف أو أرقام على الأقل"
            return
        }
        if (regPassword != regConfirmPassword) {
            regErrorMessage = "كلمة المرور وتأكيد كلمة المرور غير متطابقتين"
            return
        }

        regErrorMessage = null
        isRegistering = true

        val store = regStoreName.trim().ifBlank { "متجر ${regMerchantName.trim()}" }
        val phone = regPhone.trim().ifBlank { "+966500000000" }

        viewModel.registerMerchant(
            email = cleanEmail,
            merchantName = regMerchantName.trim(),
            storeName = store,
            phone = phone,
            password = regPassword.trim()
        ) { success, message ->
            isRegistering = false
            if (success) {
                TawthiqNotificationManager.sendWelcomePushNotification(context, force = true, merchantEmail = cleanEmail)
                Toast.makeText(context, "مرحباً بك في البيان! بدأت الآن تجربتك المجانية لمدة 4 أيام ✨", Toast.LENGTH_LONG).show()
                onLoginSuccess()
            } else {
                regErrorMessage = message
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun handleLoginSubmit() {
        val cleanEmail = MerchantAuthService.normalizeEmail(loginEmail)
        if (cleanEmail.isBlank()) {
            loginErrorMessage = "يرجى إدخال البريد الإلكتروني أو اسم المستخدم"
            return
        }

        val effectiveEmail = if (cleanEmail.contains("@")) cleanEmail else "$cleanEmail@tawthiq.app"
        loginErrorMessage = null
        isLoggingIn = true

        viewModel.verifyAndLoginMerchant(
            email = effectiveEmail,
            password = loginPassword.trim()
        ) { success, message ->
            isLoggingIn = false
            if (success) {
                val derivedName = effectiveEmail.substringBefore("@")
                TawthiqNotificationManager.sendReturningWelcomePushNotification(context, merchantName = derivedName, merchantEmail = effectiveEmail)
                Toast.makeText(context, "أهلاً بك مجدداً في البيان 👋", Toast.LENGTH_SHORT).show()
                onLoginSuccess()
            } else {
                loginErrorMessage = message
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Result launcher for Google Account picker
    val googleAccountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isAuthenticating = false
        val account = GoogleAuthHelper.getAccountFromIntent(result.data)
        val selectedEmail = if (account != null && account.email.isNotBlank()) {
            account.email
        } else {
            result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                ?: result.data?.getStringExtra("accountName")
                ?: result.data?.getStringExtra("authAccount")
                ?: result.data?.getStringExtra("email")
        }

        if (!selectedEmail.isNullOrBlank() && selectedEmail.contains("@")) {
            val cleanEmail = MerchantAuthService.normalizeEmail(selectedEmail)
            val displayName = account?.displayName ?: cleanEmail.substringBefore("@")

            if (selectedAuthTab == 1) {
                // User is in "إنشاء حساب تاجر جديد" mode -> Strictly register atomically
                isRegistering = true
                viewModel.registerMerchant(
                    email = cleanEmail,
                    merchantName = displayName,
                    storeName = "متجر $displayName",
                    phone = "+966500000000",
                    password = "GoogleOAuth_${cleanEmail.hashCode()}"
                ) { success, message ->
                    isRegistering = false
                    if (success) {
                        TawthiqNotificationManager.sendWelcomePushNotification(context, force = true, merchantEmail = cleanEmail)
                        Toast.makeText(context, "تم تسجيل حساب التاجر بنجاح وبدأت فترة التجربة المجانية ✨", Toast.LENGTH_LONG).show()
                        onLoginSuccess()
                    } else {
                        regErrorMessage = message
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                // User is in Login mode -> Login to existing merchant
                viewModel.loginWithEmail(
                    email = cleanEmail,
                    merchant = displayName,
                    store = "متجر $displayName"
                )
                TawthiqNotificationManager.sendReturningWelcomePushNotification(context, merchantName = displayName, merchantEmail = cleanEmail)
                Toast.makeText(context, "أهلاً بك مجدداً في البيان يا $displayName 👋", Toast.LENGTH_SHORT).show()
                onLoginSuccess()
            }
        }
    }

    fun launchGoogleLogin() {
        isAuthenticating = true
        try {
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
                selectedAuthTab = 0
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
                            text = if (selectedAuthTab == 0) "تسجيل الدخول" else "إنشاء حساب تاجر جديد",
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
            Spacer(modifier = Modifier.height(14.dp))

            // 1. Central Brand Logo
            Surface(
                modifier = Modifier
                    .width(180.dp)
                    .height(110.dp)
                    .testTag("login_bayan_logo"),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF0284C7).copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
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

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Main Title
            Text(
                text = "نظام البيان المحاسبي",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 3. Subtitle
            Text(
                text = if (selectedAuthTab == 0) "سجّل الدخول إلى دفتر حسابات متجرك" else "أنشئ حساب تاجر جديد بهوية رقمية فريدة",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Primary Mode Tab Row (Login vs Register)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                TabRow(
                    selectedTabIndex = selectedAuthTab,
                    containerColor = Color.Transparent,
                    contentColor = TawthiqPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedAuthTab]),
                            color = TawthiqPrimary,
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = selectedAuthTab == 0,
                        onClick = { 
                            selectedAuthTab = 0 
                            regErrorMessage = null
                        },
                        text = {
                            Text(
                                "تسجيل الدخول",
                                fontWeight = if (selectedAuthTab == 0) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 15.sp
                            )
                        },
                        modifier = Modifier.testTag("tab_login")
                    )
                    Tab(
                        selected = selectedAuthTab == 1,
                        onClick = { 
                            selectedAuthTab = 1 
                            loginErrorMessage = null
                        },
                        text = {
                            Text(
                                "حساب تاجر جديد",
                                fontWeight = if (selectedAuthTab == 1) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 15.sp
                            )
                        },
                        modifier = Modifier.testTag("tab_register")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Content based on selected tab
            if (selectedAuthTab == 1) {
                // ==================== TAB 1: NEW MERCHANT REGISTRATION ====================
                // Unique Merchant Identity Info Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = TawthiqPrimary.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = TawthiqPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "الهوية الفريدة للتاجر (Unique Merchant Identity)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TawthiqPrimary
                            )
                            Text(
                                text = "البريد الإلكتروني فريد لكل تاجر ولا يمكن تكراره مطلقاً، بينما اسم المتجر يمكن تكراره بحرية.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Error message banner if registration failed
                if (regErrorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("registration_error_banner")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = regErrorMessage!!,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 18.sp
                                )
                            }

                            // If duplicate email, suggest switching to Login
                            if (regErrorMessage!!.contains("مستخدم بالفعل")) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        loginEmail = regEmail.trim()
                                        selectedAuthTab = 0
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("switch_to_login_btn")
                                ) {
                                    Text("الانتقال لتسجيل الدخول بهذا البريد", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Field 1: Email (Unique Merchant Identifier)
                OutlinedTextField(
                    value = regEmail,
                    onValueChange = { 
                        regEmail = it
                        regErrorMessage = null
                    },
                    label = { Text("البريد الإلكتروني للتاجر * (هويتك الفريدة)") },
                    placeholder = { Text("merchant@gmail.com") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = TawthiqPrimary)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_email_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Field 2: Merchant Name
                OutlinedTextField(
                    value = regMerchantName,
                    onValueChange = { 
                        regMerchantName = it
                        regErrorMessage = null
                    },
                    label = { Text("اسم التاجر / المالك *") },
                    placeholder = { Text("أحمد المنصور") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_merchant_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Field 3: Store Name (Can be non-unique)
                OutlinedTextField(
                    value = regStoreName,
                    onValueChange = { regStoreName = it },
                    label = { Text("اسم المتجر أو النشاط (اختياري)") },
                    placeholder = { Text("متجر النور والبركة") },
                    leadingIcon = {
                        Icon(Icons.Default.Storefront, contentDescription = null)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_store_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Field 4: Phone
                OutlinedTextField(
                    value = regPhone,
                    onValueChange = { regPhone = it },
                    label = { Text("رقم الهاتف (اختياري)") },
                    placeholder = { Text("+966501234567") },
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_phone_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Field 5: Password
                OutlinedTextField(
                    value = regPassword,
                    onValueChange = { 
                        regPassword = it
                        regErrorMessage = null
                    },
                    label = { Text("كلمة المرور * (6 خانات على الأقل)") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { regPasswordVisible = !regPasswordVisible }) {
                            Icon(
                                imageVector = if (regPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (regPasswordVisible) "إخفاء كلمة المرور" else "إظهار كلمة المرور"
                            )
                        }
                    },
                    visualTransformation = if (regPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_password_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Field 6: Confirm Password
                OutlinedTextField(
                    value = regConfirmPassword,
                    onValueChange = { 
                        regConfirmPassword = it
                        regErrorMessage = null
                    },
                    label = { Text("تأكيد كلمة المرور *") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    },
                    visualTransformation = if (regPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_confirm_password_input")
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Submit Button: Register Merchant Account
                Button(
                    onClick = { handleRegistrationSubmit() },
                    enabled = !isRegistering,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("register_submit_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isRegistering) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("جارٍ التحقق الذري وإنشاء الحساب...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إنشاء حساب تاجر جديد", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Fast Google Sign up
                OutlinedButton(
                    onClick = { launchGoogleLogin() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("google_register_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    GoogleLogo(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("التسجيل السريع عبر Google", fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
                }

            } else {
                // ==================== TAB 0: EXISTING MERCHANT LOGIN ====================
                // Error banner if login failed
                if (loginErrorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_error_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = loginErrorMessage!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Field 1: Email or Username
                OutlinedTextField(
                    value = loginEmail,
                    onValueChange = { 
                        loginEmail = it
                        loginErrorMessage = null
                    },
                    label = { Text("البريد الإلكتروني للتاجر أو اسم المستخدم") },
                    placeholder = { Text("name@gmail.com أو menesy") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = TawthiqPrimary)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_email_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Field 2: Password
                OutlinedTextField(
                    value = loginPassword,
                    onValueChange = { 
                        loginPassword = it
                        loginErrorMessage = null
                    },
                    label = { Text("كلمة المرور") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { loginPasswordVisible = !loginPasswordVisible }) {
                            Icon(
                                imageVector = if (loginPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (loginPasswordVisible) "إخفاء كلمة المرور" else "إظهار كلمة المرور"
                            )
                        }
                    },
                    visualTransformation = if (loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_password_input")
                )

                // Forgot password button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { 
                            recoveryEmail = loginEmail.trim()
                            recoveryErrorMessage = null
                            showPasswordRecoveryDialog = true 
                        },
                        modifier = Modifier.testTag("forgot_password_btn")
                    ) {
                        Text(
                            "نسيت كلمة المرور؟",
                            color = TawthiqPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Submit Login Button
                Button(
                    onClick = { handleLoginSubmit() },
                    enabled = !isLoggingIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("login_submit_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isLoggingIn) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("جارٍ تسجيل الدخول...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تسجيل الدخول", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Divider: "أو"
                Text(
                    text = "أو",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Primary Google Login Button
                Surface(
                    onClick = { launchGoogleLogin() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
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
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        GoogleLogo(modifier = Modifier.size(24.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Secondary Button: "مسح باركود الزبون / متابعة كشف الحساب عبر QR"
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
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pills Row: "كيف اسجل الدخول؟" & "الفيديو التعريفي"
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

            Spacer(modifier = Modifier.height(28.dp))

            // Footer: Security Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "بياناتك محفوظة ومحمية بأمان عبر السحابة",
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

            // Footer Terms & Privacy
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

    // ==================== PASSWORD RECOVERY DIALOG ====================
    if (showPasswordRecoveryDialog) {
        AlertDialog(
            onDismissRequest = { 
                if (!isRecovering) showPasswordRecoveryDialog = false 
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = TawthiqPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("استعادة كلمة مرور التاجر", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "أدخل بريدك الإلكتروني المسجل وكلمة المرور الجديدة لاستعادة حسابك:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (recoveryErrorMessage != null) {
                        Text(
                            text = recoveryErrorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedTextField(
                        value = recoveryEmail,
                        onValueChange = { 
                            recoveryEmail = it
                            recoveryErrorMessage = null
                        },
                        label = { Text("البريد الإلكتروني المسجل") },
                        placeholder = { Text("merchant@gmail.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = recoveryNewPassword,
                        onValueChange = { 
                            recoveryNewPassword = it
                            recoveryErrorMessage = null
                        },
                        label = { Text("كلمة المرور الجديدة (6 خانات على الأقل)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = recoveryConfirmPassword,
                        onValueChange = { 
                            recoveryConfirmPassword = it
                            recoveryErrorMessage = null
                        },
                        label = { Text("تأكيد كلمة المرور الجديدة") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = MerchantAuthService.normalizeEmail(recoveryEmail)
                        if (!MerchantAuthService.isValidEmail(clean)) {
                            recoveryErrorMessage = MerchantAuthService.ERR_INVALID_EMAIL
                            return@Button
                        }
                        if (recoveryNewPassword.length < 6) {
                            recoveryErrorMessage = "يجب أن تتكون كلمة المرور من 6 أحرف على الأقل"
                            return@Button
                        }
                        if (recoveryNewPassword != recoveryConfirmPassword) {
                            recoveryErrorMessage = "كلمة المرور وتأكيد كلمة المرور غير متطابقتين"
                            return@Button
                        }

                        isRecovering = true
                        recoveryErrorMessage = null

                        viewModel.recoverMerchantPassword(clean, recoveryNewPassword) { success, msg ->
                            isRecovering = false
                            if (success) {
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                showPasswordRecoveryDialog = false
                                loginEmail = clean
                                loginPassword = recoveryNewPassword
                                selectedAuthTab = 0
                            } else {
                                recoveryErrorMessage = msg
                            }
                        }
                    },
                    enabled = !isRecovering,
                    colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isRecovering) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("جارٍ التحديث...")
                    } else {
                        Text("تحديث واستعادة كلمة المرور", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showPasswordRecoveryDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
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
