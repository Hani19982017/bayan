package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppPermissions
import com.example.ui.screens.AccountDetailScreen
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.AdminAppRoot
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.QrScannerScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SecuritySetupScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.UsersScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LahoGreen
import com.example.ui.theme.LanaRed
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TawthiqAmber
import com.example.ui.theme.TawthiqPrimary
import com.example.ui.viewmodel.TawthiqViewModel
import com.example.util.TawthiqNotificationManager

sealed class AppScreen {
    data object Accounts : AppScreen()
    data object Reports : AppScreen()
    data object Settings : AppScreen()
    data object Users : AppScreen()
    data object AdminDashboard : AppScreen()
    data object SecuritySetup : AppScreen()
    data object Notifications : AppScreen()
    data object QrScanner : AppScreen()
    data class AccountDetail(val accountId: Long, val isReadOnly: Boolean = false) : AppScreen()
}

class MainActivity : ComponentActivity() {

    private val viewModel: TawthiqViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create notification channels for reminders and welcome push notifications
        TawthiqNotificationManager.createNotificationChannels(this)

        val isFromAdminLauncher = intent?.component?.className?.contains("Admin") == true ||
                intent?.getStringExtra("target_screen") == "admin" ||
                intent?.action == "com.example.ACTION_OPEN_ADMIN" ||
                intent?.getStringExtra("navigate_to") == "admin"

        val navigateToExtra = if (isFromAdminLauncher) "admin" else intent?.getStringExtra("navigate_to")

        // Parse deep link URI if present (e.g. https://tawthiq.app/statement/2 or tawthiq://statement/2)
        val deepLinkUri = intent?.data
        val deepLinkAccountId = when {
            deepLinkUri != null -> {
                val path = deepLinkUri.path ?: ""
                if (path.contains("/statement/")) {
                    path.substringAfterLast("/statement/").filter { it.isDigit() }.toLongOrNull()
                } else if (deepLinkUri.scheme == "tawthiq") {
                    deepLinkUri.getQueryParameter("id")?.toLongOrNull()
                        ?: deepLinkUri.lastPathSegment?.filter { it.isDigit() }?.toLongOrNull()
                } else null
            }
            else -> null
        }

        if (deepLinkAccountId != null && deepLinkAccountId > 0) {
            viewModel.selectAccount(deepLinkAccountId)
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.SYSTEM -> systemDark
            }

            MyApplicationTheme(darkTheme = if (com.example.BuildConfig.IS_ADMIN_APP) true else isDark) {
                // Ensure RTL layout for authentic Arabic app experience
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    if (com.example.BuildConfig.IS_ADMIN_APP) {
                        AdminAppRoot(viewModel = viewModel)
                    } else {
                        TawthiqApp(
                            viewModel = viewModel,
                            initialTarget = navigateToExtra,
                            initialDeepLinkAccountId = deepLinkAccountId
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // When activity comes to foreground, initiate live sync service safely
        com.example.util.TawthiqLiveSyncService.startService(this)
    }
}

@Composable
fun TawthiqApp(
    viewModel: TawthiqViewModel,
    initialTarget: String? = null,
    initialDeepLinkAccountId: Long? = null
) {
    val context = LocalContext.current
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
    val savedCustomerAccountId by viewModel.savedCustomerAccountId.collectAsStateWithLifecycle()

    var currentScreen by remember(savedCustomerAccountId, initialDeepLinkAccountId, initialTarget) {
        mutableStateOf<AppScreen>(
            if (initialTarget == "admin") {
                AppScreen.AdminDashboard
            } else if (initialDeepLinkAccountId != null && initialDeepLinkAccountId > 0) {
                AppScreen.AccountDetail(initialDeepLinkAccountId, isReadOnly = true)
            } else if (savedCustomerAccountId != null && !isLoggedIn) {
                AppScreen.AccountDetail(savedCustomerAccountId!!, isReadOnly = true)
            } else if (initialTarget == "notifications") {
                AppScreen.Notifications
            } else {
                AppScreen.Accounts
            }
        )
    }
    var isScanningQr by remember { mutableStateOf(false) }
    val selectedAccountId by viewModel.selectedAccountId.collectAsStateWithLifecycle()

    // Request notification permission for Android 13+ (POST_NOTIFICATIONS)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && isLoggedIn) {
            TawthiqNotificationManager.sendWelcomePushNotification(context, force = false, merchantEmail = userEmail)
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        viewModel.listenToSystemBroadcasts()
        viewModel.listenToPaymentMethods()
    }
    LaunchedEffect(userEmail) {
        if (userEmail.isNotBlank()) {
            viewModel.listenToUserAccountStatus(userEmail)
        }
    }

    // Trigger welcome push notification when merchant is logged in
    LaunchedEffect(isLoggedIn, userEmail) {
        if (isLoggedIn) {
            TawthiqNotificationManager.sendWelcomePushNotification(context, force = false, merchantEmail = userEmail)
        }
    }

    if (!isLoggedIn) {
        // Customer Mode has priority when saved and user is not logged in
        if (savedCustomerAccountId != null && savedCustomerAccountId!! > 0) {
            val targetId = savedCustomerAccountId!!
            AccountDetailScreen(
                viewModel = viewModel,
                accountId = targetId,
                onBackClick = {
                    viewModel.clearSavedCustomerAccount()
                    currentScreen = AppScreen.Accounts
                },
                isReadOnly = true
            )
            return
        }

        if (isScanningQr) {
            QrScannerScreen(
                viewModel = viewModel,
                onBack = { isScanningQr = false },
                onAccountScanned = { accId ->
                    isScanningQr = false
                    viewModel.setSavedCustomerAccount(accId)
                }
            )
        } else if (currentScreen is AppScreen.AccountDetail) {
            val targetDetailId = (currentScreen as AppScreen.AccountDetail).accountId
            AccountDetailScreen(
                viewModel = viewModel,
                accountId = targetDetailId,
                onBackClick = {
                    viewModel.clearSavedCustomerAccount()
                    currentScreen = AppScreen.Accounts
                },
                isReadOnly = true
            )
        } else {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    currentScreen = AppScreen.Accounts
                },
                onOpenQrScanner = {
                    isScanningQr = true
                }
            )
        }
        return
    }

    // Synchronize selection
    if (selectedAccountId != null && currentScreen !is AppScreen.AccountDetail) {
        currentScreen = AppScreen.AccountDetail(selectedAccountId!!, isReadOnly = false)
    }

    BackHandler(enabled = currentScreen is AppScreen.AccountDetail) {
        viewModel.clearSelectedAccount()
        currentScreen = AppScreen.Accounts
    }

    BackHandler(enabled = currentScreen is AppScreen.QrScanner) {
        currentScreen = AppScreen.Accounts
    }

    BackHandler(enabled = currentScreen is AppScreen.SecuritySetup) {
        currentScreen = AppScreen.Settings
    }

    BackHandler(enabled = currentScreen is AppScreen.Users) {
        currentScreen = AppScreen.Settings
    }

    BackHandler(enabled = currentScreen is AppScreen.AdminDashboard) {
        currentScreen = AppScreen.Settings
    }

    BackHandler(enabled = currentScreen is AppScreen.Notifications) {
        currentScreen = AppScreen.Accounts
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Show bottom navigation bar only when on main top-level tabs
            if (currentScreen is AppScreen.Accounts || currentScreen is AppScreen.Reports || currentScreen is AppScreen.Settings) {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_navigation")
                ) {
                    NavigationBarItem(
                        selected = currentScreen is AppScreen.Accounts,
                        onClick = {
                            viewModel.clearSelectedAccount()
                            currentScreen = AppScreen.Accounts
                        },
                        icon = { Icon(Icons.Default.People, contentDescription = "الحسابات") },
                        label = {
                            Text(
                                text = "الحسابات",
                                fontSize = 12.sp,
                                fontWeight = if (currentScreen is AppScreen.Accounts) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TawthiqPrimary,
                            selectedTextColor = TawthiqPrimary,
                            indicatorColor = TawthiqPrimary.copy(alpha = 0.15f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen is AppScreen.Reports,
                        onClick = {
                            viewModel.clearSelectedAccount()
                            currentScreen = AppScreen.Reports
                        },
                        icon = { Icon(Icons.Default.DateRange, contentDescription = "التقارير") },
                        label = {
                            Text(
                                text = "التقارير",
                                fontSize = 12.sp,
                                fontWeight = if (currentScreen is AppScreen.Reports) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TawthiqPrimary,
                            selectedTextColor = TawthiqPrimary,
                            indicatorColor = TawthiqPrimary.copy(alpha = 0.15f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen is AppScreen.Settings,
                        onClick = {
                            viewModel.clearSelectedAccount()
                            currentScreen = AppScreen.Settings
                        },
                        icon = { Icon(Icons.Default.AccountCircle, contentDescription = "حسابي") },
                        label = {
                            Text(
                                text = "حسابي",
                                fontSize = 12.sp,
                                fontWeight = if (currentScreen is AppScreen.Settings) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TawthiqPrimary,
                            selectedTextColor = TawthiqPrimary,
                            indicatorColor = TawthiqPrimary.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                is AppScreen.Accounts -> {
                    AccountsScreen(
                        viewModel = viewModel,
                        onAccountClick = { id ->
                            viewModel.selectAccount(id)
                            currentScreen = AppScreen.AccountDetail(id, isReadOnly = false)
                        },
                        onNotificationClick = {
                            currentScreen = AppScreen.Notifications
                        },
                        onOpenQrScanner = {
                            currentScreen = AppScreen.QrScanner
                        }
                    )
                }
                is AppScreen.QrScanner -> {
                    QrScannerScreen(
                        viewModel = viewModel,
                        onBack = {
                            currentScreen = AppScreen.Accounts
                        },
                        onAccountScanned = { id ->
                            viewModel.selectAccount(id)
                            currentScreen = AppScreen.AccountDetail(id, isReadOnly = true)
                        }
                    )
                }
                is AppScreen.Notifications -> {
                    NotificationsScreen(
                        viewModel = viewModel,
                        onBackClick = {
                            currentScreen = AppScreen.Accounts
                        }
                    )
                }
                is AppScreen.Reports -> {
                    ReportsScreen(
                        viewModel = viewModel,
                        onAccountClick = { id ->
                            viewModel.selectAccount(id)
                            currentScreen = AppScreen.AccountDetail(id, isReadOnly = false)
                        }
                    )
                }
                is AppScreen.Settings -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onLogout = {
                            viewModel.clearSelectedAccount()
                            currentScreen = AppScreen.Accounts
                        },
                        onNavigateToUsers = {
                            currentScreen = AppScreen.Users
                        },
                        onNavigateToAdmin = {
                            currentScreen = AppScreen.AdminDashboard
                        }
                    )
                }
                is AppScreen.Users -> {
                    UsersScreen(
                        viewModel = viewModel,
                        onBackClick = {
                            currentScreen = AppScreen.Settings
                        }
                    )
                }
                is AppScreen.AdminDashboard -> {
                    AdminDashboardScreen(
                        viewModel = viewModel,
                        onBackClick = {
                            currentScreen = AppScreen.Settings
                        }
                    )
                }
                is AppScreen.SecuritySetup -> {
                    SecuritySetupScreen(
                        onContinue = { currentScreen = AppScreen.Settings },
                        onSkip = { currentScreen = AppScreen.Settings }
                    )
                }
                is AppScreen.AccountDetail -> {
                    AccountDetailScreen(
                        viewModel = viewModel,
                        accountId = screen.accountId,
                        onBackClick = {
                            viewModel.clearSelectedAccount()
                            currentScreen = AppScreen.Accounts
                        },
                        isReadOnly = screen.isReadOnly
                    )
                }
            }

            // Real-time Service Lock Overlay for Suspended or Banned Accounts
            val accountStatus by viewModel.currentUserAccountStatus.collectAsStateWithLifecycle()
            val isSuspended = accountStatus.equals("SUSPENDED", ignoreCase = true) || accountStatus == "موقوف"
            val isBanned = accountStatus.equals("BANNED", ignoreCase = true) || accountStatus == "محظور"

            if ((isLoggedIn || userEmail.isNotBlank()) && (isSuspended || isBanned)) {
                AccountStatusBlockedOverlay(
                    isBanned = isBanned,
                    userEmail = userEmail,
                    onRefresh = { viewModel.syncUserAccountStatusNow() }
                )
            }
        }
    }
}

@Composable
fun AccountStatusBlockedOverlay(
    isBanned: Boolean,
    userEmail: String,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(enabled = false) {}, // Intercept touch events
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(if (isBanned) LanaRed.copy(alpha = 0.15f) else TawthiqAmber.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isBanned) Icons.Default.Block else Icons.Default.PauseCircle,
                        contentDescription = null,
                        tint = if (isBanned) LanaRed else TawthiqAmber,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Text(
                    text = if (isBanned) "تم حظر هذا الحساب" else "تم إيقاف الخدمة مؤقتاً",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isBanned) LanaRed else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isBanned) {
                        "تم إيقاف وحظر حسابك ($userEmail) من قِبل إدارة تطبيق البيان.\nيرجى التواصل مع الإدارة للاستفسار أو حل المشكلة."
                    } else {
                        "تم تعليق خدمة التطبيق مؤقتاً لحسابك ($userEmail) من قِبل إدارة البيان.\nيرجى التواصل مع الإدارة لتفعيل وإعادة فتح الحساب."
                    },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        val whatsapp = "963955998877"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$whatsapp?text=${Uri.encode("مرحباً إدارة البيان، حسابي ($userEmail) متوقف وأرغب في مراجعة التفعيل")}"))
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = LahoGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تواصل مع إدارة البيان عبر واتساب", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("التحقق من حالة التفعيل الآن")
                }
            }
        }
    }
}
