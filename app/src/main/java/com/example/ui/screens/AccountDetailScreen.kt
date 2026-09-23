package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import com.example.ui.components.AddAccountBottomSheet
import com.example.ui.components.VoiceTransactionBottomSheet
import com.example.data.model.TransactionEntity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AddTransactionBottomSheet
import com.example.ui.components.AvatarColors
import com.example.ui.components.CustomerQrStatementDialog
import com.example.util.TawthiqNotificationManager
import com.example.ui.components.TransactionBubble
import com.example.ui.components.formatMoney
import com.example.ui.components.openDialer
import com.example.ui.components.shareWhatsAppStatement
import com.example.ui.theme.LahoGreen
import com.example.data.model.AppPermissions
import com.example.ui.theme.LahoGreenContainer
import com.example.ui.theme.LanaRed
import com.example.ui.theme.LanaRedContainer
import com.example.ui.theme.TawthiqAmber
import com.example.ui.theme.TawthiqAmberContainer
import com.example.ui.theme.TawthiqPrimary
import com.example.ui.theme.TawthiqPrimaryContainer
import com.example.ui.viewmodel.TawthiqViewModel
import com.example.util.ExcelExportHelper
import com.example.util.QrGeneratorHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    viewModel: TawthiqViewModel,
    accountId: Long,
    onBackClick: () -> Unit,
    isReadOnly: Boolean = false
) {
    val context = LocalContext.current

    LaunchedEffect(accountId) {
        if (accountId > 0) {
            viewModel.selectAccount(accountId)
        }
    }

    val account by viewModel.activeAccount.collectAsStateWithLifecycle()
    val transactions by viewModel.activeAccountTransactions.collectAsStateWithLifecycle()
    val currentAccount = account
    val canAddTransaction = viewModel.hasPermission(AppPermissions.ADD_TRANSACTION)
    val canViewTransactions = viewModel.hasPermission(AppPermissions.VIEW_TRANSACTIONS)
    val canDeleteTransaction = viewModel.hasPermission(AppPermissions.DELETE_TRANSACTION)
    val canExportReports = viewModel.hasPermission(AppPermissions.EXPORT_REPORTS)
    val canWrite by viewModel.canWrite.collectAsStateWithLifecycle()
    val isCustomerView = isReadOnly || !canWrite || !canAddTransaction || (currentAccount?.category == "حسابات متابعة") || (currentAccount?.notes?.contains("كشف حساب مرتبط") == true)
    val effectiveReadOnly = isCustomerView

    var filterType by remember { mutableStateOf("ALL") } // "ALL", "LANA", "LAHO"
    var searchQuery by remember { mutableStateOf("") }
    var showAddTransactionSheet by remember { mutableStateOf(false) }
    var transactionSheetDefaultType by remember { mutableStateOf("LANA") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showVoiceRecordDialog by remember { mutableStateOf(false) }
    var showDigitalStatementViewer by remember { mutableStateOf(false) }
    var showEditAccountSheet by remember { mutableStateOf(false) }
    var showCustomerNotificationDialog by remember { mutableStateOf(false) }
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val storeName by viewModel.storeName.collectAsStateWithLifecycle()

    val allAccounts by viewModel.accountsWithBalances.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentAccount?.id, isCustomerView) {
        if (currentAccount != null && currentAccount.id == accountId) {
            viewModel.deduplicateAccount(currentAccount.id)
            val prefs = context.getSharedPreferences("tawthiq_prefs", Context.MODE_PRIVATE)
            val isCustomerAccount = !viewModel.isLoggedIn.value && (isCustomerView || currentAccount.category == "حسابات متابعة" || currentAccount.notes.contains("كشف حساب مرتبط"))
            if (isCustomerAccount) {
                prefs.edit().putLong("saved_customer_account_id", currentAccount.id).apply()
                // Start Live Snapshot listener ONLY for linked customer statements to prevent duplicate merchant echoes
                com.example.util.FirebaseSyncManager.startLiveSyncForAccount(
                    context = context,
                    account = currentAccount,
                    coroutineScope = scope,
                    onUpdate = {
                        viewModel.selectAccount(currentAccount.id)
                    }
                )
            }

            // Start all persistent background sync mechanisms
            com.example.util.TawthiqBackgroundSyncManager.startAllBackgroundSync(context)
        }
    }

    if (currentAccount == null || currentAccount.id != accountId) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                CircularProgressIndicator(color = TawthiqPrimary)
                Text(
                    text = "جاري تحميل بيانات كشف الحساب...",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Button(
                    onClick = onBackClick,
                    colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary)
                ) {
                    Text("رجوع")
                }
            }
        }
        return
    }

    val availableCurrencies = remember(transactions, currentAccount.currency) {
        (transactions.map { it.currency }.filter { it.isNotBlank() } + currentAccount.currency).distinct()
    }
    var selectedCurrencyTab by remember(currentAccount.id, transactions) {
        val preferred = if (transactions.any { it.currency == currentAccount.currency }) {
            currentAccount.currency
        } else if (transactions.isNotEmpty()) {
            transactions.groupBy { it.currency }.maxByOrNull { it.value.size }?.key ?: currentAccount.currency
        } else {
            currentAccount.currency
        }
        mutableStateOf(preferred.ifBlank { "SAR" })
    }

    val currencyTransactions = remember(transactions, selectedCurrencyTab) {
        transactions.filter { it.currency == selectedCurrencyTab }
    }

    val lanaTotal = currencyTransactions.filter { it.type == "LANA" }.sumOf { it.amount }
    val lahoTotal = currencyTransactions.filter { it.type == "LAHO" }.sumOf { it.amount }
    val netBalance = lanaTotal - lahoTotal

    val filteredTransactions = currencyTransactions.filter { tx ->
        val matchesType = when (filterType) {
            "LANA" -> tx.type == "LANA"
            "LAHO" -> tx.type == "LAHO"
            else -> true
        }
        val matchesSearch = if (searchQuery.isBlank()) true else {
            tx.description.contains(searchQuery, ignoreCase = true) ||
            tx.amount.toString().contains(searchQuery) ||
            tx.receiptNumber.contains(searchQuery, ignoreCase = true)
        }
        matchesType && matchesSearch
    }

    val dueTransactionsCount = transactions.count { it.dueDate != null && !it.isSettled }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Circular avatar with photo or Person icon matching 4.jpeg
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentAccount.avatarUri.isNotBlank()) {
                                AsyncImage(
                                    model = currentAccount.avatarUri,
                                    contentDescription = currentAccount.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Account Name
                        Text(
                            text = currentAccount.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Clean single-line non-wrapping balance badge
                        val balanceBadgeColor = when {
                            netBalance > 0 -> Color(0xFFDC2626)
                            netBalance < 0 -> Color(0xFF16A34A)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val balanceBadgeBg = when {
                            netBalance > 0 -> Color(0xFFFEF2F2)
                            netBalance < 0 -> Color(0xFFF0FDF4)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        val balanceBadgeText = when {
                            netBalance > 0 -> "لنا: ${formatMoney(netBalance)}"
                            netBalance < 0 -> "له: ${formatMoney(-netBalance)}"
                            else -> "خالص 0"
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = balanceBadgeBg,
                            border = BorderStroke(1.dp, balanceBadgeColor.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "$balanceBadgeText $selectedCurrencyTab",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = balanceBadgeColor,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        if (!effectiveReadOnly) {
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { showEditAccountSheet = true },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "تعديل إعدادات الحساب وسقف الدين",
                                    tint = TawthiqPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("account_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                actions = {
                    // Quick Cloud Reconcile / Refresh button
                    IconButton(
                        onClick = {
                            com.example.util.FirebaseSyncManager.reconcileAccountFromFirestore(
                                context = context,
                                account = currentAccount,
                                coroutineScope = scope,
                                onUpdate = {
                                    viewModel.selectAccount(currentAccount.id)
                                }
                            )
                            android.widget.Toast.makeText(context, "جاري مزامنة وتحديث كشف الحساب...", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("account_detail_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "مزامنة وتحديث كشف الحساب",
                            tint = TawthiqPrimary
                        )
                    }

                    if (effectiveReadOnly) {
                        // Live Notification Bell for Customer Mode
                        Box(contentAlignment = Alignment.TopEnd) {
                            IconButton(
                                onClick = { showCustomerNotificationDialog = true },
                                modifier = Modifier.testTag("customer_notification_bell")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "الإشعارات والمزامنة اللحظية",
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            // Green Live Pulse Dot
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF22C55E),
                                modifier = Modifier
                                    .padding(top = 8.dp, end = 8.dp)
                                    .size(8.dp)
                            ) {}
                        }
                    } else {
                        // Direct Customer QR Button (Merchant Mode)
                        IconButton(
                            onClick = { showQrDialog = true },
                            modifier = Modifier.testTag("account_detail_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "عرض باركود الزبون (QR)",
                                tint = TawthiqPrimary
                            )
                        }
                    }

                    // Excel Download button
                    IconButton(
                        onClick = {
                            ExcelExportHelper.exportAccountToExcel(context, currentAccount, transactions)
                        },
                        modifier = Modifier.testTag("account_detail_download_excel")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "تحميل ملف إكسل",
                            tint = Color(0xFF107C41)
                        )
                    }

                    // 3-dots Menu button matching 4.jpeg
                    Box {
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "خيارات الحساب",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false },
                            containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF1E293B) else Color.White
                        ) {
                            // 0. مزامنة وتحديث فوري من السحابة
                            DropdownMenuItem(
                                text = { Text("مزامنة وتحديث فوري من السحابة", fontWeight = FontWeight.Bold, color = TawthiqPrimary) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = TawthiqPrimary
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    com.example.util.FirebaseSyncManager.reconcileAccountFromFirestore(
                                        context = context,
                                        account = currentAccount,
                                        coroutineScope = scope,
                                        onUpdate = {
                                            viewModel.selectAccount(currentAccount.id)
                                        }
                                    )
                                    android.widget.Toast.makeText(context, "جاري مزامنة وتحديث كشف الحساب...", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )

                            // 1. مشاركة الحساب والباركود (QR)
                            DropdownMenuItem(
                                text = { Text("مشاركة الحساب والباركود (QR)", fontWeight = FontWeight.Bold, color = TawthiqPrimary) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.QrCode,
                                        contentDescription = null,
                                        tint = TawthiqPrimary
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    showQrDialog = true
                                }
                            )

                            // 2. مشاركة الرصيد (orange share icon matching 4.jpeg)
                            DropdownMenuItem(
                                text = { Text("مشاركة الرصيد عبر واتساب", fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = null,
                                        tint = Color(0xFFF97316)
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    val text = "رصيد حساب ${currentAccount.name} في البيان: ${if (netBalance >= 0) "لنا " + formatMoney(netBalance) else "له " + formatMoney(-netBalance)} ${currentAccount.currency}"
                                    shareWhatsAppStatement(context, currentAccount.phone, text)
                                }
                            )

                            // 3. كشف حساب نصي (sheet/grid icon matching 4.jpeg)
                            DropdownMenuItem(
                                text = { Text("كشف حساب نصي (واتساب)", fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.TableChart,
                                        contentDescription = null,
                                        tint = Color(0xFF334155)
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    val report = ExcelExportHelper.createStatementSummaryWhatsAppMessage(
                                        account = currentAccount,
                                        transactions = transactions,
                                        netBalance = netBalance
                                    )
                                    shareWhatsAppStatement(context, currentAccount.phone, report)
                                }
                            )

                            // 4. تصدير وتحميل ملف إكسل (Excel)
                            DropdownMenuItem(
                                text = { Text("تحميل كشف الحساب (Excel)", fontWeight = FontWeight.Bold, color = Color(0xFF107C41)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Description,
                                        contentDescription = null,
                                        tint = Color(0xFF107C41)
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    ExcelExportHelper.exportAccountToExcel(context, currentAccount, transactions)
                                }
                            )

                            if (!effectiveReadOnly) {
                                androidx.compose.material3.HorizontalDivider(color = Color(0xFFE2E8F0))

                                DropdownMenuItem(
                                    text = { Text("تعديل بيانات الحساب وسقف الدين", fontWeight = FontWeight.Bold, color = TawthiqPrimary) },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = TawthiqPrimary) },
                                    onClick = {
                                        showOptionsMenu = false
                                        showEditAccountSheet = true
                                    }
                                )
                            }

                            if (!effectiveReadOnly && viewModel.hasPermission(AppPermissions.DELETE_ACCOUNT)) {
                                androidx.compose.material3.HorizontalDivider(color = Color(0xFFE2E8F0))

                                // 5. حذف الحساب
                                DropdownMenuItem(
                                    text = { Text("حذف الحساب بالكامل", color = LanaRed) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = LanaRed) },
                                    onClick = {
                                        showOptionsMenu = false
                                        showDeleteConfirmDialog = true
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (effectiveReadOnly) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                com.example.util.FirebaseSyncManager.reconcileAccountFromFirestore(
                                    context = context,
                                    account = currentAccount,
                                    coroutineScope = scope,
                                    onUpdate = {
                                        viewModel.selectAccount(currentAccount.id)
                                    }
                                )
                                android.widget.Toast.makeText(context, "جاري تحديث كشف الحساب من السحابة...", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تحديث",
                            tint = TawthiqPrimary,
                            modifier = Modifier.size(16.dp).padding(end = 4.dp)
                        )
                        Text(
                            text = if (!canWrite) "🔒 صلاحية قراءة فقط (اضغط للتحديث الفوري)" else "⚡ كشف حساب مباشر (المعاملات تُحدّث تلقائياً - اضغط للتحديث)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // Fixed Bottom Bar with green mic button and "+ معاملة جديدة" matching 4.jpeg
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Square green microphone button on the left matching 4.jpeg
                        Surface(
                            onClick = { showVoiceRecordDialog = true },
                            modifier = Modifier.size(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF00A86B)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "تسجيل صوتي",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        // Large card/pill button with "+ معاملة جديدة" matching 4.jpeg
                        Surface(
                            onClick = {
                                transactionSheetDefaultType = "LANA"
                                showAddTransactionSheet = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("new_transaction_button"),
                            shape = RoundedCornerShape(25.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "معاملة جديدة",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Read-Only mode banner
            if (effectiveReadOnly) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF93C5FD))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF22C55E),
                                    modifier = Modifier.size(10.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (!canWrite) "وضع الموظف (قراءة فقط) 🔒" else "وضع متابعة الزبون 👁️ (مربوط ومحدث سحابياً)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF1E40AF)
                                    )
                                    Text(
                                        text = if (!canWrite) "تم تسجيل الدخول بصلاحية قراءة فقط، لا يمكن إضافة أو تعديل أو حذف المعاملات." else "حسابك محفوظ تلقائياً حتى عند إغلاق التطبيق. الإشعارات الفورية مفعلة 🟢 وتصلك لحظياً بكل معاملة.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF2563EB),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    onClick = { showCustomerNotificationDialog = true },
                                    color = Color(0xFFDCFCE7),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(0.5.dp, Color(0xFF86EFAC))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Notifications,
                                            contentDescription = null,
                                            tint = Color(0xFF15803D),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "فحص الإشعارات 🔔",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF15803D)
                                        )
                                    }
                                }

                                Surface(
                                    onClick = onBackClick,
                                    color = Color(0xFFDBEAFE),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "🚪 مسح باركود آخر",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E40AF),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            // Credit Limit Exceeded Red Warning Banner (Requirement 8 - merchant only)
            if (!effectiveReadOnly && currentAccount.creditLimit > 0 && netBalance >= currentAccount.creditLimit) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🚨 تحذير: هذا الحساب تجاوز الحد الأقصى المستحق المسموح به (${formatMoney(currentAccount.creditLimit)} ${currentAccount.currency})!",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                onClick = { showEditAccountSheet = true },
                                color = Color(0xFFDC2626),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "زيادة الحد ✏️",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Multi-Currency Selection Tabs (Requirement 2)
            if (availableCurrencies.size > 1) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "تبويب العملات:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            availableCurrencies.forEach { cur ->
                                val isSelected = cur == selectedCurrencyTab
                                Surface(
                                    onClick = { selectedCurrencyTab = cur },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) TawthiqPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = cur,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Master Financial Balance Summary Card (Clear, beautiful, unambiguous)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Net Balance Hero Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "صافي الرصيد الحالي",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = when {
                                        netBalance > 0 -> "المطلوب لنا (مدين)"
                                        netBalance < 0 -> "المستحق له علينا (دائن)"
                                        else -> "الحساب متوازن (خالص)"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        netBalance > 0 -> Color(0xFFDC2626) // Red
                                        netBalance < 0 -> Color(0xFF16A34A) // Green
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when {
                                    netBalance > 0 -> Color(0xFFFEF2F2)
                                    netBalance < 0 -> Color(0xFFF0FDF4)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                border = BorderStroke(
                                    1.dp,
                                    when {
                                        netBalance > 0 -> Color(0xFFFCA5A5)
                                        netBalance < 0 -> Color(0xFF86EFAC)
                                        else -> MaterialTheme.colorScheme.outlineVariant
                                    }
                                )
                            ) {
                                Text(
                                    text = "${formatMoney(kotlin.math.abs(netBalance))} $selectedCurrencyTab",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = when {
                                        netBalance > 0 -> Color(0xFFDC2626)
                                        netBalance < 0 -> Color(0xFF16A34A)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Subtotals Row (Total Lana & Total Laho)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Lana Box
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFEF2F2).copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, Color(0xFFFECACA))
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("↗", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "إجمالي لنا (أعطيناه)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF991B1B)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${formatMoney(lanaTotal)} $selectedCurrencyTab",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                            }

                            // Laho Box
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF0FDF4).copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("↙", color = Color(0xFF16A34A), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "إجمالي له (أخذنا منه)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF166534)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${formatMoney(lahoTotal)} $selectedCurrencyTab",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF16A34A)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Search / Filter row below Top Bar matching 4.jpeg
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Start
                            ),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "ادخل نص البحث هنا...",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                innerTextField()
                            }
                        )

                        IconButton(
                            onClick = {
                                filterType = when (filterType) {
                                    "ALL" -> "LANA"
                                    "LANA" -> "LAHO"
                                    else -> "ALL"
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterAlt,
                                contentDescription = "تصفية",
                                tint = if (filterType != "ALL") TawthiqPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Customer QR Code Action Banner (Only for merchant, hidden for customer)
            if (!effectiveReadOnly) {
                item {
                    Surface(
                        onClick = { showQrDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = TawthiqPrimary.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, TawthiqPrimary.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = TawthiqPrimary,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.QrCode,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "📱 باركود الزبون (QR) لمشاهدة معاملاته",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TawthiqPrimary
                                    )
                                    Text(
                                        text = "اضغط هنا لعرض الباركود للزبون ليصوره بكاميرا هاتفه فوراً",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                tint = TawthiqPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Optional Filter Tabs & Excel Quick Action if transactions exist
            if (transactions.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = filterType == "ALL",
                            onClick = { filterType = "ALL" },
                            label = { Text("الكل (${transactions.size})") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        FilterChip(
                            selected = filterType == "LANA",
                            onClick = { filterType = "LANA" },
                            label = { Text("إرسال / لنا (${transactions.count { it.type == "LANA" }})") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        FilterChip(
                            selected = filterType == "LAHO",
                            onClick = { filterType = "LAHO" },
                            label = { Text("استلام / له (${transactions.count { it.type == "LAHO" }})") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // Quick Actions: Excel Download & Share Account
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Share Account & QR Card
                        Surface(
                            onClick = { showQrDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = TawthiqPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "مشاركة الحساب والـ QR",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TawthiqPrimary
                                )
                            }
                        }

                        // Excel Download Card
                        Surface(
                            onClick = {
                                ExcelExportHelper.exportAccountToExcel(context, currentAccount, transactions)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = Color(0xFF107C41),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "تحميل Excel",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534)
                                )
                            }
                        }
                    }
                }
            }

            // Empty state for transactions matching 4.jpeg
            if (!canViewTransactions) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 50.dp, bottom = 40.dp, start = 24.dp, end = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(LanaRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = LanaRed,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "عذراً، الصلاحية غير متوفرة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "حسابك لا يمتلك صلاحية [عرض القيود والمعاملات] لهذا الحساب.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (filteredTransactions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 70.dp, bottom = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(86.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "لا يوجد معاملات",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "لا يوجد معاملات حتى الآن",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredTransactions, key = { it.id }) { tx ->
                    TransactionBubble(
                        transaction = tx,
                        onDelete = if (!effectiveReadOnly) { { transactionToDelete = tx } } else null
                    )
                }
            }
        }
    }

    // Modal Sheet: Add Transaction (matching 3.jpeg)
    if (showAddTransactionSheet) {
        AddTransactionBottomSheet(
            account = currentAccount,
            defaultType = transactionSheetDefaultType,
            onDismiss = { showAddTransactionSheet = false },
            onSave = { type, amount, cur, desc, due, receipt ->
                val oldBal = netBalance
                val newBal = if (type == "LANA") oldBal + amount else oldBal - amount
                viewModel.addTransaction(
                    accountId = currentAccount.id,
                    type = type,
                    amount = amount,
                    currency = cur,
                    description = desc,
                    dueDate = due,
                    receiptNumber = receipt
                )
                showAddTransactionSheet = false

                if (due != null) {
                    TawthiqNotificationManager.sendDueReminderPushNotification(
                        context = context,
                        accountName = currentAccount.name,
                        amountText = "$amount $cur",
                        isLana = type == "LANA",
                        dueDateMillis = due
                    )
                }

                // Conditionally redirect to WhatsApp if autoSendWhatsApp is enabled and phone exists
                if (currentAccount.autoSendWhatsApp && currentAccount.phone.isNotBlank()) {
                    val updatedTxs = transactions.toMutableList().apply {
                        add(
                            0,
                            TransactionEntity(
                                userEmail = currentAccount.userEmail,
                                accountId = currentAccount.id,
                                type = type,
                                amount = amount,
                                currency = cur,
                                description = desc,
                                date = System.currentTimeMillis(),
                                receiptNumber = receipt
                            )
                        )
                    }
                    val waMsg = ExcelExportHelper.createInvoiceWhatsAppMessage(
                        account = currentAccount,
                        type = type,
                        invoiceAmount = amount,
                        oldBalance = oldBal,
                        newBalance = newBal,
                        description = desc,
                        dateMillis = System.currentTimeMillis(),
                        receiptNumber = receipt,
                        transactions = updatedTxs
                    )
                    ExcelExportHelper.sendWhatsAppInvoice(context, currentAccount.phone, waMsg)
                }
            },
            onSaveWithDate = { type, amount, cur, desc, date, due, receipt ->
                val oldBal = netBalance
                val newBal = if (type == "LANA") oldBal + amount else oldBal - amount
                viewModel.addTransaction(
                    accountId = currentAccount.id,
                    type = type,
                    amount = amount,
                    currency = cur,
                    description = desc,
                    date = date,
                    dueDate = due,
                    receiptNumber = receipt
                )
                showAddTransactionSheet = false

                if (due != null) {
                    TawthiqNotificationManager.sendDueReminderPushNotification(
                        context = context,
                        accountName = currentAccount.name,
                        amountText = "$amount $cur",
                        isLana = type == "LANA",
                        dueDateMillis = due
                    )
                }

                // Conditionally redirect to WhatsApp if autoSendWhatsApp is enabled and phone exists
                if (currentAccount.autoSendWhatsApp && currentAccount.phone.isNotBlank()) {
                    val updatedTxs = transactions.toMutableList().apply {
                        add(
                            0,
                            TransactionEntity(
                                userEmail = currentAccount.userEmail,
                                accountId = currentAccount.id,
                                type = type,
                                amount = amount,
                                currency = cur,
                                description = desc,
                                date = date,
                                receiptNumber = receipt
                            )
                        )
                    }
                    val waMsg = ExcelExportHelper.createInvoiceWhatsAppMessage(
                        account = currentAccount,
                        type = type,
                        invoiceAmount = amount,
                        oldBalance = oldBal,
                        newBalance = newBal,
                        description = desc,
                        dateMillis = date,
                        receiptNumber = receipt,
                        transactions = updatedTxs
                    )
                    ExcelExportHelper.sendWhatsAppInvoice(context, currentAccount.phone, waMsg)
                }
            }
        )
    }

    // Voice Note BottomSheet for Mic button (Matching Image 2)
    if (showVoiceRecordDialog) {
        VoiceTransactionBottomSheet(
            onDismiss = { showVoiceRecordDialog = false },
            currency = currentAccount.currency,
            onTransactionExtracted = { amount, type, desc ->
                showVoiceRecordDialog = false
                viewModel.addTransaction(
                    accountId = currentAccount.id,
                    type = type,
                    amount = amount,
                    currency = currentAccount.currency,
                    description = desc
                )
                Toast.makeText(context, "تم استخراج المعاملة وتسجيلها بنجاح ✓ (${amount} ${currentAccount.currency})", Toast.LENGTH_LONG).show()
            }
        )
    }

    // Delete Account Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("حذف الحساب", fontWeight = FontWeight.Bold) },
            text = {
                Text("هل أنت متأكد من رغبتك في حذف حساب '${currentAccount.name}' وكافة حركاته المالية؟ لا يمكن التراجع عن هذا الإجراء.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAccount(currentAccount.id)
                        showDeleteConfirmDialog = false
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LanaRed)
                ) {
                    Text("نعم، حذف", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Delete Single Transaction Confirmation Dialog
    val currentTxToDelete = transactionToDelete
    if (currentTxToDelete != null) {
        val tx = currentTxToDelete
        val typeArabic = if (tx.type == "LANA") "لنا" else "له"
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = LanaRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حذف المعاملة", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text("هل أنت متأكد من رغبتك في حذف هذه المعاملة؟")
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "$typeArabic: ${formatMoney(tx.amount)} ${tx.currency}",
                                fontWeight = FontWeight.Bold,
                                color = if (tx.type == "LANA") LanaRed else LahoGreen
                            )
                            if (tx.description.isNotBlank()) {
                                Text(
                                    text = tx.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "سيتم حذف المعاملة وتحديث صافي الرصيد تلقائياً.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(tx.id, currentAccount.id, tx.receiptNumber)
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LanaRed)
                ) {
                    Text("نعم، حذف", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Payment Reminder Dialog
    if (showReminderDialog) {
        val clipboardManager = LocalClipboardManager.current
        val reminderMessage = remember(currentAccount, netBalance) {
            "السلام عليكم ورحمة الله وبركاته، أخي الفاضل ${currentAccount.name}.\n" +
            "نود تذكيركم بلطف بأن رصيد حسابكم المتبقي لدى مؤسسة البيان التجارية هو:\n" +
            "💰 ${formatMoney(if (netBalance > 0) netBalance else 0.0)} ${currentAccount.currency}\n" +
            "يرجى التكرم بترتيب سداد المبلغ في أقرب فرصة ممكنة.\n" +
            "🔗 يمكنك معاينة كشف حسابك المحدث عبر الرابط:\n" +
            "https://bayan.app/statement/${currentAccount.id}\n" +
            "شاكرين ومقدرين حسن تعاونكم الدائم معنا."
        }

        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = LanaRed, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إرسال تذكير سداد للمستدين", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "نص رسالة التذكير التي سيتم إرسالها للعميل:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = reminderMessage,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        shareWhatsAppStatement(context, currentAccount.phone, reminderMessage)
                        showReminderDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إرسال عبر واتساب")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReminderDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // QR Code & Account Sharing Dialog
    if (showQrDialog) {
        CustomerQrStatementDialog(
            account = currentAccount,
            netBalance = netBalance,
            currency = currentAccount.currency,
            storeName = storeName,
            transactions = transactions,
            onDismiss = { showQrDialog = false }
        )
    }

    // Interactive Digital Web Statement Viewer Modal (Eliminates any 404 page)
    if (showDigitalStatementViewer) {
        val clipboardManager = LocalClipboardManager.current
        AlertDialog(
            onDismissRequest = { showDigitalStatementViewer = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = TawthiqPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("كشف الحساب الرقمي المعتمد", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = TawthiqPrimary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "موثّق سحابياً ✓",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TawthiqPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Merchant & Client Banner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "العميل: ${currentAccount.name}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "#TW-${currentAccount.id}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = TawthiqPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "الرصيد الصافي المطلوب: ${formatMoney(netBalance)} ${currentAccount.currency}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = if (netBalance > 0) LanaRed else TawthiqPrimary
                            )
                        }
                    }

                    Text(
                        text = "سجل العمليات المالية (${transactions.size} معاملة):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Transactions List
                    if (transactions.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Text("لا توجد عمليات مسجلة حتى الآن", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(transactions) { tx ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (tx.type == "LANA") LanaRed.copy(alpha = 0.1f) else LahoGreen.copy(alpha = 0.1f)
                                    ),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = if (tx.type == "LANA") "له دين (لنا)" else "سداد / دفعة (له)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (tx.type == "LANA") LanaRed else LahoGreen
                                            )
                                            Text(
                                                text = tx.description.ifBlank { "معاملة بدون تفاصيل" },
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            val dateFormatted = remember(tx.date) {
                                                java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault()).format(java.util.Date(tx.date))
                                            }
                                            Text(
                                                text = dateFormatted,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Text(
                                            text = "${tx.amount} ${tx.currency}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (tx.type == "LANA") LanaRed else LahoGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reportText = ExcelExportHelper.createStatementSummaryWhatsAppMessage(
                            account = currentAccount,
                            transactions = transactions,
                            netBalance = netBalance
                        )
                        shareWhatsAppStatement(context, currentAccount.phone, reportText)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إرسال التقرير كامل", fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDigitalStatementViewer = false }) {
                    Text("إغلاق", fontSize = 12.sp)
                }
            }
        )
    }

    if (showEditAccountSheet && currentAccount != null) {
        AddAccountBottomSheet(
            categories = categories,
            accountToEdit = currentAccount,
            existingAccounts = allAccounts.map { it.account },
            onDismiss = { showEditAccountSheet = false },
            onSave = { _, _, _, _, _, _, _, _, _, _, _ -> },
            onUpdate = { updatedAccount ->
                viewModel.updateAccount(updatedAccount)
                showEditAccountSheet = false
                android.widget.Toast.makeText(context, "تم حفظ تعديلات الحساب بنجاح", android.widget.Toast.LENGTH_SHORT).show()
            },
            onAddNewCategory = { newCat ->
                viewModel.addCategory(newCat)
            }
        )
    }

    if (showCustomerNotificationDialog && currentAccount != null) {
        AlertDialog(
            onDismissRequest = { showCustomerNotificationDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "الإشعارات والمزامنة اللحظية 🔔",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF22C55E),
                                modifier = Modifier.size(10.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "حالة الاتصال: متصل بالسحابة ولحظي 🟢",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF15803D)
                            )
                        }
                    }

                    Text(
                        text = "• حسابك مربوط بشكل آمن مع متجر (${storeName.ifBlank { "التاجر" }}).\n• ستصلك إشعارات فورية على شاشة هاتفك عند إضافة أو تعديل أي فاتورة أو دفعة جديدة.\n• يبقى كشف حسابك محفوظاً في التطبيق تلقائياً.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Button(
                        onClick = {
                            TawthiqNotificationManager.sendLiveTransactionPushNotification(
                                context = context,
                                storeName = storeName.ifBlank { "التاجر" },
                                accountName = currentAccount.name,
                                amount = 150.0,
                                currency = currentAccount.currency,
                                type = "LANA",
                                description = "إشعار تجريبي لاختبار التنبيهات الفورية",
                                accountId = currentAccount.id
                            )
                            android.widget.Toast.makeText(context, "تم إرسال إشعار تجريبي إلى هاتفك الآن!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إرسال إشعار تجريبي للتأكد 📲", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showCustomerNotificationDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("تم", fontSize = 12.sp)
                }
            }
        )
    }
}
