package com.example.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AdminUserAccount
import com.example.data.model.PaymentMethodConfig
import com.example.data.model.SubscriptionPaymentRequest
import com.example.data.model.SystemBroadcastMessage
import com.example.ui.components.AddEditPaymentMethodDialog
import com.example.ui.components.AdminCustomerLedgerDialog
import com.example.ui.components.ChangeUserPasswordDialog
import com.example.ui.components.ManageUserSubscriptionDialog
import com.example.ui.components.ReviewPaymentRequestDialog
import com.example.ui.components.SendBroadcastMessageDialog
import com.example.ui.components.formatMoney
import com.example.ui.theme.LahoGreen
import com.example.ui.theme.LanaRed
import com.example.ui.theme.TawthiqAmber
import com.example.ui.theme.TawthiqPrimary
import com.example.ui.viewmodel.TawthiqViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: TawthiqViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val adminUserAccounts by viewModel.adminUserAccounts.collectAsStateWithLifecycle()
    val paymentMethods by viewModel.paymentMethods.collectAsStateWithLifecycle()
    val paymentRequests by viewModel.subscriptionPaymentRequests.collectAsStateWithLifecycle()
    val systemBroadcasts by viewModel.systemBroadcasts.collectAsStateWithLifecycle()
    val overallSummary by viewModel.overallSummary.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "المستخدمين (${adminUserAccounts.size})",
        "طلبات الدفع (${paymentRequests.count { it.status == "PENDING" }})",
        "طرق الدفع (${paymentMethods.size})",
        "خطط الاشتراكات",
        "الرسائل الجماعية (${systemBroadcasts.size})"
    )

    // Dialog States
    var selectedUserForPassword by remember { mutableStateOf<AdminUserAccount?>(null) }
    var selectedUserForSubscription by remember { mutableStateOf<AdminUserAccount?>(null) }
    var selectedUserForLedger by remember { mutableStateOf<AdminUserAccount?>(null) }
    var selectedRequestForReview by remember { mutableStateOf<SubscriptionPaymentRequest?>(null) }
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var selectedPaymentForEdit by remember { mutableStateOf<PaymentMethodConfig?>(null) }
    var showBroadcastDialog by remember { mutableStateOf(false) }
    var userToDelete by remember { mutableStateOf<AdminUserAccount?>(null) }

    LaunchedEffect(Unit) {
        viewModel.syncAdminDataFromCloud()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "لوحة تحكم إدارة البيان (Super Admin)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "رجوع",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            viewModel.syncAdminDataFromCloud()
                            Toast.makeText(context, "جاري مزامنة بيانات المشتركين والمعاملات من السحابة...", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "تحديث ومزامنة السحابة",
                                tint = Color(0xFF6366F1)
                            )
                        }
                        IconButton(onClick = { showBroadcastDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "بث رسالة عامة",
                                tint = Color(0xFF6366F1)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Header Mini Summary
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "مركز القيادة والاشتراكات",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "الحسابات: ${adminUserAccounts.size} • طلبات معلقة: ${paymentRequests.count { it.status == "PENDING" }}",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 12.sp
                                )
                            }

                            Button(
                                onClick = { showBroadcastDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("رسالة جماعية", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Scrollable Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = TawthiqPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = TawthiqPrimary
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = if (selectedTab == index) TawthiqPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }

                // Tab Content
                Box(modifier = Modifier.fillMaxSize()) {
                    when (selectedTab) {
                        0 -> AdminUsersTab(
                            users = adminUserAccounts,
                            viewModel = viewModel,
                            onPasswordClick = { selectedUserForPassword = it },
                            onSubscriptionClick = { selectedUserForSubscription = it },
                            onLedgerClick = { selectedUserForLedger = it },
                            onDeleteClick = { userToDelete = it }
                        )
                        1 -> AdminPaymentRequestsTab(
                            requests = paymentRequests,
                            onRequestClick = { selectedRequestForReview = it }
                        )
                        2 -> AdminPaymentMethodsTab(
                            methods = paymentMethods,
                            viewModel = viewModel,
                            onAddClick = { showAddPaymentDialog = true },
                            onEditClick = { selectedPaymentForEdit = it }
                        )
                        3 -> AdminSubscriptionPlansTab(
                            users = adminUserAccounts,
                            viewModel = viewModel
                        )
                        4 -> AdminBroadcastsTab(
                            broadcasts = systemBroadcasts,
                            onSendClick = { showBroadcastDialog = true }
                        )
                    }
                }
            }
        }

        // Dialogs
        selectedUserForPassword?.let { user ->
            ChangeUserPasswordDialog(
                userAccount = user,
                viewModel = viewModel,
                onDismiss = { selectedUserForPassword = null }
            )
        }

        selectedUserForSubscription?.let { user ->
            ManageUserSubscriptionDialog(
                userAccount = user,
                viewModel = viewModel,
                onDismiss = { selectedUserForSubscription = null }
            )
        }

        selectedUserForLedger?.let { user ->
            AdminCustomerLedgerDialog(
                userAccount = user,
                viewModel = viewModel,
                onDismiss = { selectedUserForLedger = null }
            )
        }

        selectedRequestForReview?.let { request ->
            ReviewPaymentRequestDialog(
                request = request,
                viewModel = viewModel,
                onDismiss = { selectedRequestForReview = null }
            )
        }

        if (showAddPaymentDialog) {
            AddEditPaymentMethodDialog(
                initialMethod = null,
                viewModel = viewModel,
                onDismiss = { showAddPaymentDialog = false }
            )
        }

        selectedPaymentForEdit?.let { method ->
            AddEditPaymentMethodDialog(
                initialMethod = method,
                viewModel = viewModel,
                onDismiss = { selectedPaymentForEdit = null }
            )
        }

        if (showBroadcastDialog) {
            SendBroadcastMessageDialog(
                viewModel = viewModel,
                onDismiss = { showBroadcastDialog = false }
            )
        }

        userToDelete?.let { user ->
            AlertDialog(
                onDismissRequest = { userToDelete = null },
                title = { Text("تأكيد حذف الحساب", fontWeight = FontWeight.Bold) },
                text = { Text("هل أنت متأكد من حذف حساب ${user.merchantName.ifBlank { user.storeName }} (${user.email}) نهائياً من النظام؟") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteAdminUser(user.email)
                            Toast.makeText(context, "تم حذف الحساب بنجاح", Toast.LENGTH_SHORT).show()
                            userToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LanaRed)
                    ) {
                        Text("نعم، احذف الحساب")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { userToDelete = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

/**
 * Tab 0: Requirement 1, 2, 3, 6, 7
 * View accounts, suspend, ban, delete, activate, change password, manage subscription, view transactions
 */
@Composable
private fun AdminUsersTab(
    users: List<AdminUserAccount>,
    viewModel: TawthiqViewModel,
    onPasswordClick: (AdminUserAccount) -> Unit,
    onSubscriptionClick: (AdminUserAccount) -> Unit,
    onLedgerClick: (AdminUserAccount) -> Unit,
    onDeleteClick: (AdminUserAccount) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("الكل") }

    val filteredUsers = remember(users, searchQuery, selectedFilter) {
        users.filter { user ->
            val matchesSearch = user.merchantName.contains(searchQuery, ignoreCase = true) ||
                    user.storeName.contains(searchQuery, ignoreCase = true) ||
                    user.email.contains(searchQuery, ignoreCase = true) ||
                    user.phone.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "نشط" -> user.status == "ACTIVE"
                "موقوف" -> user.status == "SUSPENDED"
                "محظور" -> user.status == "BANNED"
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search and Filter Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث بالاسم، المتجر، الإيميل أو الهاتف...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("الكل", "نشط", "موقوف", "محظور").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) TawthiqPrimary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (filteredUsers.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد حسابات مطابقة للبحث",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(filteredUsers, key = { it.email }) { user ->
                AdminUserCard(
                    user = user,
                    onActivate = {
                        viewModel.updateUserStatus(user.email, "ACTIVE")
                        Toast.makeText(context, "تم تفعيل خدمة الحساب بنجاح ✓", Toast.LENGTH_SHORT).show()
                    },
                    onSuspend = {
                        viewModel.updateUserStatus(user.email, "SUSPENDED")
                        Toast.makeText(context, "تم إيقاف الخدمة عن الحساب مؤقتاً ⏸", Toast.LENGTH_SHORT).show()
                    },
                    onBan = {
                        viewModel.updateUserStatus(user.email, "BANNED")
                        Toast.makeText(context, "تم حظر الحساب 🚫", Toast.LENGTH_SHORT).show()
                    },
                    onDelete = { onDeleteClick(user) },
                    onPasswordClick = { onPasswordClick(user) },
                    onSubscriptionClick = { onSubscriptionClick(user) },
                    onLedgerClick = { onLedgerClick(user) }
                )
            }
        }
    }
}

@Composable
private fun AdminUserCard(
    user: AdminUserAccount,
    onActivate: () -> Unit,
    onSuspend: () -> Unit,
    onBan: () -> Unit,
    onDelete: () -> Unit,
    onPasswordClick: () -> Unit,
    onSubscriptionClick: () -> Unit,
    onLedgerClick: () -> Unit
) {
    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    val expiryFormatted = if (user.subscriptionExpiry > 0) sdf.format(Date(user.subscriptionExpiry)) else "غير محدد"

    val isActive = user.status.equals("ACTIVE", ignoreCase = true) || user.status == "نشط"
    val isSuspended = user.status.equals("SUSPENDED", ignoreCase = true) || user.status == "موقوف"
    val isBanned = user.status.equals("BANNED", ignoreCase = true) || user.status == "محظور"

    val statusColor = when {
        isActive -> LahoGreen
        isSuspended -> TawthiqAmber
        isBanned -> LanaRed
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val statusText = when {
        isActive -> "نشط ✓"
        isSuspended -> "موقوف ⏸"
        isBanned -> "محظور 🚫"
        else -> user.status
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: User Info & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(TawthiqPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (user.merchantName.ifBlank { user.storeName }).take(1),
                            color = TawthiqPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = user.merchantName.ifBlank { user.storeName },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${user.storeName} • ${user.email}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            // Details info row: Phone, Plan, Expiry
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الهاتف: ${user.phone.ifBlank { "غير مسجل" }}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "الباقة: ${user.plan} • الانتهاء: $expiryFormatted",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (user.isExpired) LanaRed else TawthiqPrimary
                )
            }

            // Quick Actions: Row 1 (Service control & Customer transactions)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // View Customers & Transactions
                OutlinedButton(
                    onClick = onLedgerClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("الزبائن والمعاملات", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Change Password
                OutlinedButton(
                    onClick = onPasswordClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("كلمة المرور", fontSize = 11.sp)
                }

                // Manage Subscription
                OutlinedButton(
                    onClick = onSubscriptionClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Diamond, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("الاشتراك", fontSize = 11.sp)
                }
            }

            // Service Control: Row 2 (Activate, Suspend, Ban, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!isActive) {
                    Button(
                        onClick = onActivate,
                        colors = ButtonDefaults.buttonColors(containerColor = LahoGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("تفعيل الخدمة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (!isSuspended) {
                    OutlinedButton(
                        onClick = onSuspend,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null, tint = TawthiqAmber, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("إيقاف مؤقت", fontSize = 11.sp, color = TawthiqAmber)
                    }
                }

                if (!isBanned) {
                    OutlinedButton(
                        onClick = onBan,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, tint = LanaRed, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("حظر", fontSize = 11.sp, color = LanaRed)
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/**
 * Tab 1: Requirement 4 & Manual Payment Flow
 * Review incoming subscription payment proofs
 */
@Composable
private fun AdminPaymentRequestsTab(
    requests: List<SubscriptionPaymentRequest>,
    onRequestClick: (SubscriptionPaymentRequest) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = TawthiqPrimary.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null, tint = TawthiqPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "عندما يقوم المشترك بالتحويل لمحفظتك وإرسال إشعار الدفع، يظهر الطلب هنا فوراً لتأكيد التفعيل بضغطة واحدة.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        if (requests.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد طلبات دفع جديدة حالياً",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(requests, key = { it.id }) { req ->
                val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                val dateFormatted = sdf.format(Date(req.createdAt))

                val statusColor = when (req.status) {
                    "PENDING" -> TawthiqAmber
                    "APPROVED" -> LahoGreen
                    "REJECTED" -> LanaRed
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val statusText = when (req.status) {
                    "PENDING" -> "قيد المراجعة ⏳"
                    "APPROVED" -> "تم التفعيل ✓"
                    "REJECTED" -> "مرفوض ✕"
                    else -> req.status
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRequestClick(req) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = req.userName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(statusColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(text = statusText, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Text(
                                text = "باقة ${req.planName} (${req.planPrice}) • ${req.paymentMethodName}",
                                fontSize = 12.sp,
                                color = TawthiqPrimary,
                                fontWeight = FontWeight.Medium
                            )

                            Text(
                                text = "رقم العملية: ${req.transferNumber} • $dateFormatted",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { onRequestClick(req) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (req.status == "PENDING") TawthiqPrimary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (req.status == "PENDING") "مراجعة واعتماد" else "عرض",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (req.status == "PENDING") Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 2: Requirement 4
 * Add / Edit Payment Methods (Wallets, Syriatel Cash, Sham Cash, USDT, Bank)
 */
@Composable
private fun AdminPaymentMethodsTab(
    methods: List<PaymentMethodConfig>,
    viewModel: TawthiqViewModel,
    onAddClick: () -> Unit,
    onEditClick: (PaymentMethodConfig) -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("إضافة وسيلة دفع / محفظة جديدة", fontWeight = FontWeight.Bold)
            }
        }

        items(methods, key = { it.id }) { method ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(TawthiqPrimary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (method.iconName) {
                                        "phone" -> Icons.Default.PhoneAndroid
                                        "crypto" -> Icons.Default.CurrencyBitcoin
                                        "bank" -> Icons.Default.AccountBalance
                                        else -> Icons.Default.AccountBalanceWallet
                                    },
                                    contentDescription = null,
                                    tint = TawthiqPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = method.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(text = method.accountHolder, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Switch(
                            checked = method.isActive,
                            onCheckedChange = { active ->
                                viewModel.togglePaymentMethod(method.id, active)
                                Toast.makeText(context, if (active) "تم تفعيل وسيلة الدفع" else "تم إيقاف وسيلة الدفع", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    Text(
                        text = "الرقم: ${method.accountNumber}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TawthiqPrimary
                    )

                    if (method.instructions.isNotBlank()) {
                        Text(
                            text = method.instructions,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { onEditClick(method) }) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تعديل", fontSize = 12.sp)
                        }

                        TextButton(
                            onClick = {
                                viewModel.deletePaymentMethod(method.id)
                                Toast.makeText(context, "تم حذف وسيلة الدفع", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = LanaRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حذف", color = LanaRed, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 3: Requirement 7
 * Subscription Plans overview & expiry monitoring
 */
@Composable
private fun AdminSubscriptionPlansTab(
    users: List<AdminUserAccount>,
    viewModel: TawthiqViewModel
) {
    val freeCount = users.count { it.plan == "مجاني" }
    val weeklyCount = users.count { it.plan == "أسبوعي" }
    val monthlyCount = users.count { it.plan == "شهري" }
    val yearlyCount = users.count { it.plan == "سنوي" }
    val expiredCount = users.count { it.isExpired }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Expiry alert if any
        if (expiredCount > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = LanaRed.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️ يوجد $expiredCount مشترك انتهى تاريخ اشتراكهم وتم تحويلهم تلقائياً للخطة المجانية بانتظار التجديد.",
                            fontSize = 12.sp,
                            color = LanaRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            PlanSummaryCard(
                planName = "الباقة المجانية (Free)",
                price = "0$",
                duration = "4 أيام تجريبية أو حتى 50 معاملة",
                subscribersCount = freeCount,
                color = Color(0xFF6B7280)
            )
        }

        item {
            PlanSummaryCard(
                planName = "الباقة الأسبوعية (Weekly)",
                price = "2$",
                duration = "7 أيام كاملة غير محدودة",
                subscribersCount = weeklyCount,
                color = Color(0xFF3B82F6)
            )
        }

        item {
            PlanSummaryCard(
                planName = "الباقة الشهرية (Monthly)",
                price = "5$",
                duration = "30 يوماً كاملة غير محدودة",
                subscribersCount = monthlyCount,
                color = TawthiqPrimary
            )
        }

        item {
            PlanSummaryCard(
                planName = "الباقة السنوية (Yearly VIP)",
                price = "45$",
                duration = "365 يوماً كامل المزايا مع أولوية الدعم",
                subscribersCount = yearlyCount,
                color = TawthiqAmber
            )
        }
    }
}

@Composable
private fun PlanSummaryCard(
    planName: String,
    price: String,
    duration: String,
    subscribersCount: Int,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = planName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Text(text = duration, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "السعر المقترح: $price", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$subscribersCount مشترك",
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * Tab 4: Requirement 5
 * Send Broadcast Messages and view history
 */
@Composable
private fun AdminBroadcastsTab(
    broadcasts: List<SystemBroadcastMessage>,
    onSendClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(
                onClick = onSendClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("إرسال رسالة جماعية جديدة لجميع المستخدمين", fontWeight = FontWeight.Bold)
            }
        }

        if (broadcasts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد رسائل جماعية سابقة",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(broadcasts, key = { it.id }) { msg ->
                val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                val dateFormatted = sdf.format(Date(msg.sentAt))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = msg.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = dateFormatted, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Text(
                            text = msg.message,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 17.sp
                        )

                        Text(
                            text = "المرسل: ${msg.sender}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
