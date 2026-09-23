package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import com.example.ui.components.BayanLogo
import com.example.ui.components.UpgradeSubscriptionDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.example.ui.components.tawthiqTextFieldColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.StaffSession
import com.example.ui.theme.LahoGreen
import com.example.ui.theme.LanaRed
import com.example.ui.theme.TawthiqAmber
import com.example.ui.theme.TawthiqPrimary
import com.example.data.model.AppPermissions
import com.example.ui.theme.AppThemeMode
import com.example.ui.viewmodel.TawthiqViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TawthiqViewModel,
    onLogout: () -> Unit,
    onNavigateToUsers: () -> Unit = {},
    onNavigateToAdmin: () -> Unit = {}
) {
    val context = LocalContext.current
    val merchantName by viewModel.merchantName.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
    val userWhatsApp by viewModel.userWhatsApp.collectAsStateWithLifecycle()
    val subscriptionInfo by viewModel.subscriptionInfo.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val storeName by viewModel.storeName.collectAsStateWithLifecycle()
    val defaultCurrency by viewModel.defaultCurrency.collectAsStateWithLifecycle()
    val currentStaffSession by viewModel.currentStaffSession.collectAsStateWithLifecycle()
    val isStaffUser by viewModel.isStaffUser.collectAsStateWithLifecycle()
    val staffUsers by viewModel.staffUsers.collectAsStateWithLifecycle()
    val merchantAvatarUri by viewModel.merchantAvatarUri.collectAsStateWithLifecycle()

    val staff = currentStaffSession

    val matchingStaffUser = remember(staff, staffUsers) {
        if (staff != null) staffUsers.find { it.id == staff.userId || it.name.trim() == staff.userName.trim() } else null
    }

    val activeAvatarUri = matchingStaffUser?.avatarUri?.ifBlank { null }
        ?: staff?.avatarUri?.ifBlank { null }
        ?: merchantAvatarUri.ifBlank { null }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
            try { context.contentResolver.takePersistableUriPermission(it, flag) } catch (_: Exception) {}
            if (staff != null) {
                val targetUser = matchingStaffUser ?: staffUsers.find { u -> u.name.trim() == staff.userName.trim() }
                if (targetUser != null) {
                    viewModel.updateStaffUser(targetUser.copy(avatarUri = it.toString()))
                }
            } else {
                viewModel.updateMerchantAvatar(it.toString())
            }
            Toast.makeText(context, "تم تحديث الصورة الشخصية بنجاح ✓", Toast.LENGTH_SHORT).show()
        }
    }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showWhatsAppDialog by remember { mutableStateOf(false) }
    var showUpgradeDialog by remember { mutableStateOf(false) }
    var showAppSettingsDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showRateDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showSwitchAccountDialog by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "حسابي",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Profile Header Card (Matching Screenshot 2)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Top Row: Avatar on right (RTL), Name, and Settings Icon on left
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // User Avatar Circle on right
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable {
                                                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!activeAvatarUri.isNullOrBlank()) {
                                            AsyncImage(
                                                model = activeAvatarUri,
                                                contentDescription = "الملف الشخصي",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = "الملف الشخصي",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(34.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    val staff = currentStaffSession
                                    Column {
                                        Text(
                                            text = if (staff != null) staff.userName else merchantName.ifBlank { if (userEmail.isNotBlank()) userEmail.substringBefore("@") else "التاجر" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        if (staff != null) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "موظف (${staff.role})",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            if (staff.canWrite) Color(0xFF22C55E) else Color(0xFF0284C7),
                                                            RoundedCornerShape(6.dp)
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = staff.permissionType,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        } else {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Email,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = userEmail.ifBlank { "غير مسجل" },
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                // Settings gear button on top-left of card
                                IconButton(
                                    onClick = { showAppSettingsDialog = true },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "الإعدادات",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // WhatsApp Completion Banner
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showWhatsAppDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, LahoGreen.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // WhatsApp green icon
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(LahoGreen),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Chat,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text(
                                            text = if (userWhatsApp.isNotBlank()) "رقم الواتساب: $userWhatsApp ✓" else "استكمل بيانات حسابك بإضافة رقم واتساب",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Text(
                                        text = if (userWhatsApp.isNotBlank()) "تعديل <" else "إكمال <",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LahoGreen
                                    )
                                }
                            }

                            // Divider
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            )

                            // "تبديل الحساب" (Switch account) button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showSwitchAccountDialog = true }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SyncAlt,
                                    contentDescription = null,
                                    tint = TawthiqPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تبديل الحساب",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TawthiqPrimary
                                )
                            }
                        }
                    }
                }

                // 2. Trial & Subscription Card (Matching Screenshot 2)
                item {
                    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFF0F9FF)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFBAE6FD)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Header of trial card
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = null,
                                        tint = TawthiqPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = subscriptionInfo.planName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF0369A1)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isDark) TawthiqPrimary.copy(alpha = 0.2f) else Color(0xFFE0F2FE),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (subscriptionInfo.isPro) "Pro نشط ★" else "تجريبي",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TawthiqPrimary
                                    )
                                }
                            }

                            Text(
                                text = if (subscriptionInfo.isPro) "اشتراكك الاحترافي نشط بدون أي قيود على المعاملات." else "الفترة التجريبية نشطة.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Inner Box for Allowed Transactions
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "المعاملات المسموحة",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    MaterialTheme.colorScheme.background,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = if (subscriptionInfo.isPro) "غير محدود" else "${subscriptionInfo.allowedTransactionsMonthly} معاملة / شهرياً",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "المستخدم: ${subscriptionInfo.usedTransactionsMonthly}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (subscriptionInfo.isPro) "المتبقي: ∞" else "المتبقي: ${subscriptionInfo.allowedTransactionsMonthly - subscriptionInfo.usedTransactionsMonthly}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Text(
                                        text = "عند الاشتراك يمكنك إضافة عدد غير محدود من المعاملات",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            // Green Upgrade Subscription Button
                            Button(
                                onClick = { showUpgradeDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("upgrade_subscription_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Diamond,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ترقية الاشتراك",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }

                            // Available Payment Methods
                            Text(
                                text = "طرق الدفع المتاحة:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF0369A1)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val methods = listOf("USDT", "Visa", "Sham Cash", "Syriatel", "Zain Cash")
                                methods.forEach { method ->
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                            .border(
                                                1.dp,
                                                if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFBAE6FD),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = method,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF0369A1)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Settings Menu Items (Screenshots 1 & 4)

                // Item 0: Admin Dashboard (لوحة تحكم الإدارة - فقط في تطبيق الإدارة المستقل)
                if (com.example.BuildConfig.IS_ADMIN_APP && viewModel.hasPermission(AppPermissions.ACCESS_ADMIN)) {
                    item {
                        SettingsMenuCard(
                            title = "لوحة تحكم الإدارة (Admin)",
                            subtitle = "التحكم في التطبيق، الاشتراكات، المستخدمين والتقارير العامة",
                            icon = Icons.Default.AdminPanelSettings,
                            iconBgColor = Color(0xFF6366F1),
                            onClick = onNavigateToAdmin
                        )
                    }
                }

                // Item 1: App Settings (الإعدادات)
                if (viewModel.hasPermission(AppPermissions.ACCESS_SETTINGS)) {
                    item {
                        SettingsMenuCard(
                            title = "الإعدادات",
                            subtitle = "إعدادات التطبيق والعملات",
                            icon = Icons.Default.Settings,
                            iconBgColor = Color(0xFF0284C7),
                            onClick = { showAppSettingsDialog = true }
                        )
                    }
                }

                // Item: Background Sync & Notifications Status Card
                item {
                    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFF0FDF4)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFBBF7D0)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.SyncAlt,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "المزامنة والإشعارات بالخلفية",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF065F46)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF059669).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "نشط 24/7 ✓",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF059669)
                                    )
                                }
                            }

                            Text(
                                text = "تضمن خدمة توثيق وصول إشعارات المعاملات للزبائن في نفس اللحظة حتى عند إغلاق التطبيق تماماً.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        com.example.util.TawthiqBackgroundSyncManager.requestIgnoreBatteryOptimizations(context)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("السماح بالخلفية", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }

                                Button(
                                    onClick = {
                                        com.example.util.TawthiqNotificationManager.sendTestPushNotification(context)
                                        Toast.makeText(context, "تم إرسال إشعار تجريبي لهاتفك الآن 🔔", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("اختبار الإشعار 🔔", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // Item 2: Users Management (المستخدمين)
                if (viewModel.hasPermission(AppPermissions.MANAGE_STAFF)) {
                    item {
                        SettingsMenuCard(
                            title = "المستخدمين والموظفين",
                            subtitle = "إدارة المستخدمين والصلاحيات المسموحة",
                            icon = Icons.Default.Group,
                            iconBgColor = Color(0xFF10B981),
                            onClick = onNavigateToUsers
                        )
                    }
                }

                // Item 3: Help & Support (الدعم والمساعدة)
                item {
                    SettingsMenuCard(
                        title = "الدعم والمساعدة",
                        subtitle = "تواصل معنا عبر التذاكر",
                        icon = Icons.Default.Headphones,
                        iconBgColor = Color(0xFF8B5CF6),
                        onClick = { showSupportDialog = true }
                    )
                }

                // Item 4: Share App (مشاركة التطبيق)
                item {
                    SettingsMenuCard(
                        title = "مشاركة التطبيق",
                        subtitle = "رمز استجابة سريعة أو مشاركة رابط المتجر",
                        icon = Icons.Default.Share,
                        iconBgColor = Color(0xFFF97316),
                        onClick = { showShareDialog = true }
                    )
                }

                // Item 5: Rate App (تقييم التطبيق)
                item {
                    SettingsMenuCard(
                        title = "تقييم التطبيق",
                        subtitle = "قيّم التطبيق على متجر Play",
                        icon = Icons.Default.Star,
                        iconBgColor = Color(0xFFEAB308),
                        onClick = { showRateDialog = true }
                    )
                }

                // Item 6: Privacy Policy (سياسة الخصوصية)
                item {
                    SettingsMenuCard(
                        title = "سياسة الخصوصية",
                        subtitle = "اطلع على سياسة الخصوصية",
                        icon = Icons.Outlined.Security,
                        iconBgColor = Color(0xFF06B6D4),
                        onClick = { showPrivacyDialog = true }
                    )
                }

                // Item 7: About Us (من نحن)
                item {
                    SettingsMenuCard(
                        title = "من نحن",
                        subtitle = "تعرف أكثر عنا وعن فكرة التطبيق",
                        icon = Icons.Default.Info,
                        iconBgColor = Color(0xFF3B82F6),
                        onClick = { showAboutDialog = true }
                    )
                }

                // Item 8: Logout Button
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLogoutDialog = true }
                            .testTag("logout_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = LanaRed.copy(alpha = 0.08f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LanaRed.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "تسجيل الخروج",
                                tint = LanaRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "تسجيل الخروج",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = LanaRed
                            )
                        }
                    }
                }

                // Item 9: Version Footer
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إصدار التطبيق 2.9.4",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // WhatsApp Dialog
    if (showWhatsAppDialog) {
        var whatsappInput by remember { mutableStateOf(userWhatsApp) }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AlertDialog(
                onDismissRequest = { showWhatsAppDialog = false },
                title = { Text("إضافة رقم واتساب للحساب", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("أدخل رقم الواتساب الخاص بك للتواصل وتلقي النسخ الاحتياطية وإشعارات الحساب:", fontSize = 13.sp)
                        OutlinedTextField(
                            value = whatsappInput,
                            onValueChange = { whatsappInput = it },
                            label = { Text("رقم الواتساب مع مفتاح الدولة") },
                            placeholder = { Text("+9665...") },
                            singleLine = true,
                            colors = tawthiqTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.setWhatsAppNumber(whatsappInput)
                            Toast.makeText(context, "تم حفظ رقم الواتساب بنجاح ✓", Toast.LENGTH_SHORT).show()
                            showWhatsAppDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary)
                    ) {
                        Text("حفظ")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWhatsAppDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }

    // App Settings & Theme Dialog
    if (showAppSettingsDialog) {
        var tempStoreName by remember { mutableStateOf(storeName) }
        var tempMerchantName by remember { mutableStateOf(merchantName) }
        var tempCurrency by remember { mutableStateOf(defaultCurrency) }
        var showCurrencyMenu by remember { mutableStateOf(false) }

        val availableCurrencies = listOf(
            "USD" to "دولار أمريكي ($)",
            "SYP" to "ليرة سورية (ل.س)",
            "SAR" to "ريال سعودي (ر.س)",
            "EGP" to "جنيه مصري (ج.م)",
            "AED" to "درهم إماراتي (د.إ)",
            "KWD" to "دينار كويتي (د.ك)",
            "EUR" to "يورو (€)",
            "YER" to "ريال يمني (ر.ي)",
            "IQD" to "دينار عراقي (د.ع)",
            "TRY" to "ليرة تركية (₺)",
            "JOD" to "دينار أردني (د.أ)",
            "QAR" to "ريال قطري (ر.ق)",
            "OMR" to "ريال عماني (ر.ع)",
            "BHD" to "دينار بحريني (د.ب)",
            "GBP" to "جنيه إسترليني (£)"
        )

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AlertDialog(
                onDismissRequest = { showAppSettingsDialog = false },
                title = { Text("إعدادات التطبيق والمظهر", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("مظهر التطبيق والإضاءة:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeSelectorOption(
                                title = "فاتح ☀️",
                                icon = Icons.Default.LightMode,
                                isSelected = themeMode == AppThemeMode.LIGHT,
                                onClick = { viewModel.setThemeMode(AppThemeMode.LIGHT) },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeSelectorOption(
                                title = "داكن 🌙",
                                icon = Icons.Default.DarkMode,
                                isSelected = themeMode == AppThemeMode.DARK,
                                onClick = { viewModel.setThemeMode(AppThemeMode.DARK) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = tempStoreName,
                            onValueChange = { tempStoreName = it },
                            label = { Text("اسم المتجر / النشاط") },
                            singleLine = true,
                            colors = tawthiqTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = tempMerchantName,
                            onValueChange = { tempMerchantName = it },
                            label = { Text("اسم التاجر / المالك") },
                            singleLine = true,
                            colors = tawthiqTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Currency Selector Box with Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = tempCurrency,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("عملة التطبيق الموحدة *") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "اختيار العملة",
                                        modifier = Modifier.clickable { showCurrencyMenu = true }
                                    )
                                },
                                singleLine = true,
                                colors = tawthiqTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showCurrencyMenu = true }
                            )

                            DropdownMenu(
                                expanded = showCurrencyMenu,
                                onDismissRequest = { showCurrencyMenu = false },
                                containerColor = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                            ) {
                                availableCurrencies.forEach { (code, label) ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "($code) - $label",
                                                fontWeight = if (tempCurrency == code) FontWeight.Bold else FontWeight.Normal,
                                                color = if (tempCurrency == code) TawthiqPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            tempCurrency = code
                                            showCurrencyMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TawthiqPrimary.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "💡 يتم توحيد جميع الحسابات والمعاملات على هذه العملة تلقائياً لضمان دقة وصحة جمع الأرصدة.",
                                fontSize = 11.sp,
                                color = TawthiqPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateStoreProfile(
                                storeName = tempStoreName.trim(),
                                merchantName = tempMerchantName.trim(),
                                merchantPhone = userWhatsApp,
                                currency = tempCurrency.trim().ifBlank { "USD" }
                            )
                            showAppSettingsDialog = false
                            Toast.makeText(context, "تم حفظ الإعدادات بنجاح ✓", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary)
                    ) {
                        Text("حفظ")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAppSettingsDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }

    // Upgrade Subscription Dialog
    if (showUpgradeDialog) {
        UpgradeSubscriptionDialog(
            viewModel = viewModel,
            onDismiss = { showUpgradeDialog = false }
        )
    }

    // Support Tickets Dialog
    if (showSupportDialog) {
        var supportSubject by remember { mutableStateOf("") }
        var supportDetails by remember { mutableStateOf("") }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AlertDialog(
                onDismissRequest = { showSupportDialog = false },
                title = { Text("الدعم والمساعدة - فتح تذكرة 🎧", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("أرسل استفسارك أو مشكلتك وسيقوم فريق الدعم بالرد فوراً:", fontSize = 12.sp)
                        OutlinedTextField(
                            value = supportSubject,
                            onValueChange = { supportSubject = it },
                            label = { Text("موضوع التذكرة") },
                            singleLine = true,
                            colors = tawthiqTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = supportDetails,
                            onValueChange = { supportDetails = it },
                            label = { Text("تفاصيل الاستفسار") },
                            minLines = 3,
                            colors = tawthiqTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (supportSubject.isNotBlank()) {
                                viewModel.submitSupportTicket(supportSubject, supportDetails)
                                Toast.makeText(context, "تم إرسال تذكرة الدعم بنجاح وسيتم الرد خلال دقائق ✓", Toast.LENGTH_LONG).show()
                                showSupportDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary)
                    ) {
                        Text("إرسال التذكرة")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSupportDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }

    // Share App Dialog
    if (showShareDialog) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AlertDialog(
                onDismissRequest = { showShareDialog = false },
                title = { Text("مشاركة تطبيق البيان 📲", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .width(120.dp)
                                .height(78.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            shadowElevation = 4.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, TawthiqPrimary.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp),
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
                        Text(
                            text = "شارك تطبيق البيان مع أصدقائك والتجار لتوثيق ديونهم وحساباتهم بسهولة وأمان.",
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "تطبيق البيان للحسابات والديون")
                                putExtra(Intent.EXTRA_TEXT, "أنصحك بتحميل تطبيق البيان لإدارة دفاتر الديون والعمليات المالية بدقة وسهولة: https://bayan.app")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة تطبيق البيان عبر:"))
                            showShareDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مشاركة الرابط")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showShareDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }

    // Rate App Dialog
    if (showRateDialog) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AlertDialog(
                onDismissRequest = { showRateDialog = false },
                title = { Text("تقييم التطبيق ⭐", fontWeight = FontWeight.Bold) },
                text = {
                    Text("يسعدنا تقييمك لتطبيق البيان على متجر التطبيقات بخمس نجوم لدعم استمرار التطوير وإضافة مزايا جديدة!", fontSize = 13.sp)
                },
                confirmButton = {
                    Button(
                        onClick = {
                            Toast.makeText(context, "شكراً لدعمك وتقييمك الرائع! ⭐⭐⭐⭐⭐", Toast.LENGTH_LONG).show()
                            showRateDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB308))
                    ) {
                        Text("تقييم 5 نجوم ★", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRateDialog = false }) {
                        Text("لاحقاً")
                    }
                }
            )
        }
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AlertDialog(
                onDismissRequest = { showPrivacyDialog = false },
                title = { Text("سياسة الخصوصية 🛡️", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("نحن في تطبيق البيان نلتزم بأعلى معايير حماية وخصوصية بياناتك المالية:", fontSize = 12.sp)
                        Text("• يتم تشفير كافة السجلات والعمليات محلياً على جهازك بأعلى درجات الأمان.", fontSize = 11.sp)
                        Text("• لا نقوم بمشاركة أو بيع أي بيانات مالية أو أرقام هواتف لأي جهة خارجية.", fontSize = 11.sp)
                        Text("• لك الحق الكامل في تصدير أو حذف جميع بياناتك في أي وقت.", fontSize = 11.sp)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showPrivacyDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary)
                    ) {
                        Text("فهمت ذلك")
                    }
                }
            )
        }
    }

    // About Us Dialog
    if (showAboutDialog) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BayanLogo(size = 32.dp)
                        Text("عن تطبيق البيان 📖", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("تطبيق البيان هو رفيق التاجر وصاحب العمل الذكي:", fontSize = 12.sp)
                        Text("• تحويل الدفاتر الورقية المعقدة إلى سجل رقمي مبسط ومرتب.", fontSize = 11.sp)
                        Text("• كشوف حسابات فورية وسندات ديون بصيغة PDF و Excel.", fontSize = 11.sp)
                        Text("• رسائل تذكير تلقائية للعملاء عبر واتساب والرسائل القصيرة.", fontSize = 11.sp)
                        Text("• إصدار 3.0.0 • جميع الحقوق محفوظة © تطبيق البيان.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showAboutDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary)
                    ) {
                        Text("إغلاق")
                    }
                }
            )
        }
    }

    // Switch Account Dialog
    if (showSwitchAccountDialog) {
        var newEmail by remember { mutableStateOf("") }
        var newMerchant by remember { mutableStateOf("") }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AlertDialog(
                onDismissRequest = { showSwitchAccountDialog = false },
                title = { Text("تبديل الحساب / تسجيل الدخول بحساب آخر", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("أدخل البريد الإلكتروني للحساب الآخر لتبديل النشاط:", fontSize = 12.sp)
                        OutlinedTextField(
                            value = newEmail,
                            onValueChange = { newEmail = it },
                            label = { Text("البريد الإلكتروني") },
                            placeholder = { Text("example@gmail.com") },
                            singleLine = true,
                            colors = tawthiqTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newMerchant,
                            onValueChange = { newMerchant = it },
                            label = { Text("اسم التاجر / الحساب (اختياري)") },
                            singleLine = true,
                            colors = tawthiqTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newEmail.isNotBlank()) {
                                viewModel.loginWithEmail(newEmail, merchant = newMerchant)
                                Toast.makeText(context, "تم تبديل الحساب بنجاح ✓", Toast.LENGTH_SHORT).show()
                                showSwitchAccountDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary)
                    ) {
                        Text("تبديل")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSwitchAccountDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }

    // Logout Dialog
    if (showLogoutDialog) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("تسجيل الخروج", fontWeight = FontWeight.Bold) },
                text = { Text("هل أنت متأكد من رغبتك في تسجيل الخروج من حساب المتجر؟ بياناتك المحلية ستبقى محفوظة ومحمية.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutDialog = false
                            viewModel.logout()
                            Toast.makeText(context, "تم تسجيل الخروج بنجاح", Toast.LENGTH_SHORT).show()
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LanaRed)
                    ) {
                        Text("نعم، تسجيل الخروج")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBgColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Colored rounded box for icon on right (in RTL)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBgColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconBgColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Left Chevron Arrow `<` in RTL
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ThemeSelectorOption(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) TawthiqPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) TawthiqPrimary else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) TawthiqPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) TawthiqPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
