package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AccountWithBalance
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.launch
import com.example.ui.components.AccountCard
import com.example.ui.components.AddAccountBottomSheet
import com.example.ui.components.BalanceSummaryCards
import com.example.ui.components.BatchActionsBottomSheet
import com.example.ui.components.BatchAddTransactionDialog
import com.example.ui.components.BatchReminderDialog
import com.example.ui.components.CategoryFilterRow
import com.example.ui.components.CustomerQrStatementDialog
import com.example.ui.components.TawthiqEmptyAccountsView
import com.example.ui.components.TawthiqTopBar
import com.example.ui.theme.LanaRed
import com.example.ui.components.openDialer
import com.example.ui.components.shareWhatsAppStatement
import androidx.compose.material.icons.filled.Lock
import com.example.data.model.AppPermissions
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.TawthiqPrimary
import com.example.ui.viewmodel.TawthiqViewModel
import com.example.util.ExcelExportHelper

@Composable
fun AccountsScreen(
    viewModel: TawthiqViewModel,
    onAccountClick: (Long) -> Unit,
    onNotificationClick: () -> Unit = {},
    onOpenQrScanner: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val canViewAccounts = viewModel.hasPermission(AppPermissions.VIEW_ACCOUNTS)
    val canAddAccount = viewModel.hasPermission(AppPermissions.ADD_ACCOUNT)
    val canWrite by viewModel.canWrite.collectAsStateWithLifecycle()
    val accountsWithBalances by viewModel.accountsWithBalances.collectAsStateWithLifecycle()
    val filteredAccounts by viewModel.filteredAccounts.collectAsStateWithLifecycle()
    val overallSummary by viewModel.overallSummary.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val unreadNotificationCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()
    val storeName by viewModel.storeName.collectAsStateWithLifecycle()
    val defaultCurrency by viewModel.defaultCurrency.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> isSystemDark
    }

    var showAddAccountDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<com.example.data.model.AccountEntity?>(null) }
    var accountToDelete by remember { mutableStateOf<com.example.data.model.AccountEntity?>(null) }
    var accountForQr by remember { mutableStateOf<AccountWithBalance?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var showBatchActionsSheet by remember { mutableStateOf(false) }
    var showBatchAddTxDialog by remember { mutableStateOf(false) }
    var showBatchReminderDialog by remember { mutableStateOf(false) }
    var selectedAccountForNewTx by remember { mutableStateOf<com.example.data.model.AccountEntity?>(null) }
    var newCategoryText by remember { mutableStateOf("") }
    var showExcelExportDialog by remember { mutableStateOf(false) }

    var accountStatusFilter by remember { mutableStateOf("ALL") } // "ALL", "LANA", "LAHO", "SETTLED"
    var selectedCurrencyFilter by remember { mutableStateOf("ALL") } // "ALL", "USD", "SYP", etc.
    var sortOption by remember { mutableStateOf("INDEX") } // "INDEX", "MAX_DEBT", "EXCEEDED_LIMIT", "RECENT", "NAME"
    var showSortMenu by remember { mutableStateOf(false) }

    val displayAccounts = remember(filteredAccounts, accountStatusFilter, sortOption) {
        val filtered = when (accountStatusFilter) {
            "LANA" -> filteredAccounts.filter { it.netBalance > 0 }
            "LAHO" -> filteredAccounts.filter { it.netBalance < 0 }
            "SETTLED" -> filteredAccounts.filter { it.netBalance == 0.0 }
            else -> filteredAccounts
        }
        when (sortOption) {
            "INDEX" -> filtered.sortedWith(
                compareBy(
                    { if (it.account.displayIndex > 0) it.account.displayIndex else Int.MAX_VALUE },
                    { it.account.id }
                )
            )
            "MAX_DEBT" -> filtered.sortedByDescending { kotlin.math.abs(it.netBalance) }
            "EXCEEDED_LIMIT" -> filtered.sortedWith(
                compareByDescending<AccountWithBalance> { it.isCreditLimitExceeded }
                    .thenByDescending { it.netBalance }
            )
            "RECENT" -> filtered.sortedByDescending { it.lastTransactionDate ?: it.account.updatedAt }
            "NAME" -> filtered.sortedBy { it.account.name }
            else -> filtered.sortedWith(
                compareBy(
                    { if (it.account.displayIndex > 0) it.account.displayIndex else Int.MAX_VALUE },
                    { it.account.id }
                )
            )
        }
    }

    val lanaCount = remember(filteredAccounts) { filteredAccounts.count { it.netBalance > 0 } }
    val lahoCount = remember(filteredAccounts) { filteredAccounts.count { it.netBalance < 0 } }
    val settledCount = remember(filteredAccounts) { filteredAccounts.count { it.netBalance == 0.0 } }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Top Bar matching Screenshot 2 (Theme Switch, Bell, Search, QR, Tawthiq logo)
            item {
                TawthiqTopBar(
                    onSearchClick = {
                        showSearchBar = !showSearchBar
                        if (!showSearchBar) {
                            viewModel.searchQuery.value = ""
                        }
                    },
                    onQrClick = onOpenQrScanner,
                    onNotificationClick = onNotificationClick,
                    onBatchActionsClick = { showBatchActionsSheet = true },
                    unreadNotificationCount = unreadNotificationCount,
                    isDarkTheme = isDark,
                    onToggleTheme = { viewModel.toggleDarkMode() }
                )
            }

            // Category Filter Row matching Screenshot 2 (Circular + button, then category chips)
            item {
                CategoryFilterRow(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onSelectCategory = { viewModel.selectedCategory.value = it },
                    onAddCategoryClick = { showAddCategoryDialog = true }
                )
            }

            // Expandable Search Bar if active
            if (showSearchBar || searchQuery.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "بحث",
                                tint = TawthiqPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.searchQuery.value = it },
                                placeholder = {
                                    Text(
                                        "ابحث بالاسم أو رقم الهاتف أو الملاحظات...",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("search_accounts_input"),
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                )
                            )

                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "مسح النص",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            IconButton(onClick = {
                                viewModel.searchQuery.value = ""
                                showSearchBar = false
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق البحث",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // If accounts exist, show balance summary card and organized filters
            if (!canViewAccounts) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "عذراً، الصلاحية غير متوفرة",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "حسابك لا يمتلك صلاحية [عرض الحسابات والعملاء]. يرجى التواصل مع مالك الحساب لتعديل صلاحياتك.",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            } else if (filteredAccounts.isNotEmpty()) {
                item {
                    BalanceSummaryCards(
                        summary = overallSummary,
                        defaultCurrency = defaultCurrency,
                        selectedCurrency = selectedCurrencyFilter,
                        onCurrencySelected = { selectedCurrencyFilter = it },
                        showOnlyNet = true
                    )
                }

                // Status Filter Chips (الكل، لنا، علينا، خالصة)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = accountStatusFilter == "ALL",
                            onClick = { accountStatusFilter = "ALL" },
                            label = { Text("الكل (${filteredAccounts.size})", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        FilterChip(
                            selected = accountStatusFilter == "LANA",
                            onClick = { accountStatusFilter = "LANA" },
                            label = { Text("لنا ($lanaCount)", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = com.example.ui.theme.LanaRedContainer,
                                selectedLabelColor = com.example.ui.theme.LanaRed
                            )
                        )
                        FilterChip(
                            selected = accountStatusFilter == "LAHO",
                            onClick = { accountStatusFilter = "LAHO" },
                            label = { Text("علينا ($lahoCount)", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = com.example.ui.theme.LahoGreenContainer,
                                selectedLabelColor = com.example.ui.theme.LahoGreen
                            )
                        )
                        FilterChip(
                            selected = accountStatusFilter == "SETTLED",
                            onClick = { accountStatusFilter = "SETTLED" },
                            label = { Text("خالص ($settledCount)", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // Excel Export & Sort Action Bar
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick Excel Button
                        Surface(
                            onClick = {
                                showExcelExportDialog = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF0FDF4),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = Color(0xFF107C41),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "تصدير كشف Excel",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534)
                                )
                            }
                        }

                        // Sort Dropdown
                        Box {
                            Surface(
                                onClick = { showSortMenu = true },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sort,
                                        contentDescription = "ترتيب",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val sortLabel = when (sortOption) {
                                        "INDEX" -> "الترتيب الثابت (#1 أولاً)"
                                        "MAX_DEBT" -> "الأعلى رصيداً"
                                        "EXCEEDED_LIMIT" -> "تجاوز الحد الأقصى 🚨"
                                        "RECENT" -> "الأحدث نشاطاً"
                                        "NAME" -> "أبجدياً (أ - ي)"
                                        else -> "الترتيب الثابت (#1 أولاً)"
                                    }
                                    Text(
                                        text = sortLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF1E293B) else Color.White
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.FormatListNumbered,
                                            contentDescription = null,
                                            tint = if (sortOption == "INDEX") TawthiqPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    text = { 
                                        Text(
                                            "الترتيب الثابت بالرقم (#1 أولاً - عادي)",
                                            fontWeight = if (sortOption == "INDEX") FontWeight.Bold else FontWeight.Normal,
                                            color = if (sortOption == "INDEX") TawthiqPrimary else MaterialTheme.colorScheme.onSurface
                                        ) 
                                    },
                                    onClick = {
                                        sortOption = "INDEX"
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.TrendingUp,
                                            contentDescription = null,
                                            tint = if (sortOption == "MAX_DEBT") TawthiqPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    text = { 
                                        Text(
                                            "الأعلى رصيداً",
                                            fontWeight = if (sortOption == "MAX_DEBT") FontWeight.Bold else FontWeight.Normal,
                                            color = if (sortOption == "MAX_DEBT") TawthiqPrimary else MaterialTheme.colorScheme.onSurface
                                        ) 
                                    },
                                    onClick = {
                                        sortOption = "MAX_DEBT"
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (sortOption == "EXCEEDED_LIMIT") Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    text = { 
                                        Text(
                                            "تجاوز السقف الائتماني (تجاوز الحد 🚨)",
                                            fontWeight = if (sortOption == "EXCEEDED_LIMIT") FontWeight.Bold else FontWeight.Normal,
                                            color = if (sortOption == "EXCEEDED_LIMIT") Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                                        ) 
                                    },
                                    onClick = {
                                        sortOption = "EXCEEDED_LIMIT"
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = if (sortOption == "RECENT") TawthiqPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    text = { 
                                        Text(
                                            "الأحدث نشاطاً",
                                            fontWeight = if (sortOption == "RECENT") FontWeight.Bold else FontWeight.Normal,
                                            color = if (sortOption == "RECENT") TawthiqPrimary else MaterialTheme.colorScheme.onSurface
                                        ) 
                                    },
                                    onClick = {
                                        sortOption = "RECENT"
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.SortByAlpha,
                                            contentDescription = null,
                                            tint = if (sortOption == "NAME") TawthiqPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    text = { 
                                        Text(
                                            "أبجدياً (أ - ي)",
                                            fontWeight = if (sortOption == "NAME") FontWeight.Bold else FontWeight.Normal,
                                            color = if (sortOption == "NAME") TawthiqPrimary else MaterialTheme.colorScheme.onSurface
                                        ) 
                                    },
                                    onClick = {
                                        sortOption = "NAME"
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Header with current counts
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "قائمة الحسابات (${displayAccounts.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (displayAccounts.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 20.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "لا توجد حسابات مطابقة للتصفية المحددة",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(displayAccounts, key = { it.account.id }) { item ->
                        AccountCard(
                            item = item,
                            onClick = { selectedAccountForNewTx = item.account },
                            onOpenDetailClick = { onAccountClick(item.account.id) },
                            onAddTransactionClick = { selectedAccountForNewTx = item.account },
                            onCallClick = { openDialer(context, item.account.phone) },
                            onShareClick = {
                                scope.launch {
                                    val txs = viewModel.getTransactionsSnapshotForAccount(item.account.id)
                                    val msg = ExcelExportHelper.createStatementSummaryWhatsAppMessage(
                                        account = item.account,
                                        transactions = txs,
                                        netBalance = item.netBalance
                                    )
                                    shareWhatsAppStatement(context, item.account.phone, msg)
                                }
                            },
                            onExcelClick = {
                                viewModel.exportAccountExcel(context, item.account.id)
                            },
                            onEditClick = {
                                accountToEdit = item.account
                            },
                            onQrClick = {
                                accountForQr = item
                            },
                            onDeleteClick = {
                                accountToDelete = item.account
                            },
                            selectedCurrency = selectedCurrencyFilter
                        )
                    }
                }
            } else if (searchQuery.isNotEmpty()) {
                // Search Empty State
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp, bottom = 20.dp, start = 24.dp, end = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "لا توجد نتائج مطابقة لـ \"$searchQuery\"",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "تأكد من كتابة الاسم أو رقم الهاتف أو التصنيف بشكل صحيح",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = { viewModel.searchQuery.value = "" },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("إلغاء البحث وعرض كافة الحسابات", fontSize = 13.sp)
                        }
                    }
                }
            } else {
                // Empty state matching Screenshot 2 precisely
                item {
                    TawthiqEmptyAccountsView(
                        onAddAccountClick = { showAddAccountDialog = true }
                    )
                }
            }
        }

        // Floating Action Button matching Screenshot 2
        if (canAddAccount && canWrite) {
            FloatingActionButton(
                onClick = { showAddAccountDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("fab_add_account"),
                containerColor = TawthiqPrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة حساب", modifier = Modifier.size(28.dp))
            }
        }
    }

    // Modal Bottom Sheet: Add Account matching Screenshot 5
    if (showAddAccountDialog) {
        AddAccountBottomSheet(
            categories = categories,
            defaultCurrency = defaultCurrency,
            existingAccounts = accountsWithBalances.map { it.account },
            onDismiss = { showAddAccountDialog = false },
            onSave = { name, phone, cat, cur, bal, type, notes, avatarUri, creditLimit, displayIndex, autoSendWhatsApp ->
                viewModel.addAccount(
                    name = name,
                    phone = phone,
                    category = cat,
                    currency = cur,
                    initialBalance = bal,
                    initialType = type,
                    notes = notes,
                    avatarUri = avatarUri,
                    creditLimit = creditLimit,
                    displayIndex = displayIndex,
                    autoSendWhatsApp = autoSendWhatsApp
                )
                showAddAccountDialog = false
            },
            onAddNewCategory = { newCat ->
                viewModel.addCategory(newCat)
            }
        )
    }

    // Modal Bottom Sheet: Edit Account
    if (accountToEdit != null) {
        AddAccountBottomSheet(
            categories = categories,
            defaultCurrency = defaultCurrency,
            accountToEdit = accountToEdit,
            existingAccounts = accountsWithBalances.map { it.account },
            onDismiss = { accountToEdit = null },
            onSave = { _, _, _, _, _, _, _, _, _, _, _ -> },
            onUpdate = { updatedAccount ->
                viewModel.updateAccount(updatedAccount)
                accountToEdit = null
            },
            onAddNewCategory = { newCat ->
                viewModel.addCategory(newCat)
            }
        )
    }

    // Dialog: Add Category
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("إضافة تصنيف جديد", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newCategoryText,
                    onValueChange = { newCategoryText = it },
                    label = { Text("اسم التصنيف (مثال: موردين، شركاء)") },
                    singleLine = true,
                    colors = com.example.ui.components.tawthiqTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryText.isNotBlank()) {
                            viewModel.addCategory(newCategoryText)
                            newCategoryText = ""
                        }
                        showAddCategoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary)
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Modal Bottom Sheet: إضافة معاملة سريعة وحفظ ثم تحويل لواتساب مباشرة
    selectedAccountForNewTx?.let { targetAcc ->
        com.example.ui.components.AddTransactionBottomSheet(
            account = targetAcc,
            defaultType = "LANA",
            onDismiss = { selectedAccountForNewTx = null },
            onSave = { type, amount, currency, desc, dueDate, receipt ->
                viewModel.addTransaction(
                    accountId = targetAcc.id,
                    type = type,
                    amount = amount,
                    currency = currency,
                    description = desc,
                    dueDate = dueDate,
                    receiptNumber = receipt
                )
                selectedAccountForNewTx = null
                val accWithBal = accountsWithBalances.find { it.account.id == targetAcc.id }
                val oldBal = accWithBal?.netBalance ?: 0.0
                val newNet = oldBal + (if (type == "LANA") amount else -amount)
                if (targetAcc.autoSendWhatsApp && targetAcc.phone.isNotBlank()) {
                    scope.launch {
                        val allTxs = viewModel.getTransactionsSnapshotForAccount(targetAcc.id)
                        val updatedTxs = allTxs.toMutableList().apply {
                            add(
                                0,
                                TransactionEntity(
                                    userEmail = targetAcc.userEmail,
                                    accountId = targetAcc.id,
                                    type = type,
                                    amount = amount,
                                    currency = currency,
                                    description = desc,
                                    date = System.currentTimeMillis(),
                                    receiptNumber = receipt
                                )
                            )
                        }
                        val waMsg = ExcelExportHelper.createInvoiceWhatsAppMessage(
                            account = targetAcc,
                            type = type,
                            invoiceAmount = amount,
                            oldBalance = oldBal,
                            newBalance = newNet,
                            description = desc,
                            dateMillis = System.currentTimeMillis(),
                            receiptNumber = receipt,
                            transactions = updatedTxs
                        )
                        ExcelExportHelper.sendWhatsAppInvoice(context, targetAcc.phone, waMsg)
                    }
                }
            }
        )
    }

    // Modal Bottom Sheet: إجراءات جماعية (matching uploaded screenshot)
    if (showBatchActionsSheet) {
        BatchActionsBottomSheet(
            onDismiss = { showBatchActionsSheet = false },
            onAddBatchTransactionClick = { showBatchAddTxDialog = true },
            onSendBatchReminderClick = { showBatchReminderDialog = true }
        )
    }

    // Dialog: إضافة معاملة جماعية
    if (showBatchAddTxDialog) {
        BatchAddTransactionDialog(
            accounts = accountsWithBalances,
            onDismiss = { showBatchAddTxDialog = false },
            onConfirm = { selectedIds, type, amount, description ->
                viewModel.addBatchTransactions(
                    accountIds = selectedIds,
                    type = type,
                    amount = amount,
                    currency = "ل.س",
                    description = description
                )
                showBatchAddTxDialog = false
            }
        )
    }

    // Dialog: إرسال تذكير جماعي
    if (showBatchReminderDialog) {
        val debtors = accountsWithBalances.filter { it.netBalance > 0 }
        BatchReminderDialog(
            debtors = debtors,
            onDismiss = { showBatchReminderDialog = false }
        )
    }

    // Dialog: باركود الزبون (QR) ليصوره بكاميرا هاتفه
    if (accountForQr != null) {
        val targetAcc = accountForQr!!
        val qrTransactions by androidx.compose.runtime.produceState<List<com.example.data.model.TransactionEntity>>(
            initialValue = emptyList(),
            key1 = targetAcc.account.id
        ) {
            value = viewModel.getTransactionsSnapshotForAccount(targetAcc.account.id)
        }
        CustomerQrStatementDialog(
            account = targetAcc.account,
            netBalance = targetAcc.netBalance,
            currency = targetAcc.account.currency,
            storeName = storeName,
            transactions = qrTransactions,
            onDismiss = { accountForQr = null }
        )
    }

    // Dialog: تأكيد حذف الحساب نهائياً للتاجر ومزامنته للزبون
    if (accountToDelete != null) {
        val targetAcc = accountToDelete!!
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = LanaRed
                    )
                    Text(
                        text = "حذف الحساب نهائياً",
                        fontWeight = FontWeight.Bold,
                        color = LanaRed
                    )
                }
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف حساب (${targetAcc.name}) وجميع معاملاته المالية؟\n\n⚠️ سيتم حذف الحساب نهائياً، كما سيتم حذفه تلقائياً من تطبيق أي زبون مرتبط بهذا الحساب عبر الباركود (QR).",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAccount(targetAcc.id)
                        accountToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LanaRed)
                ) {
                    Text("نعم، احذف الحساب", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Dialog: خيارات تصدير كشف Excel الملون
    if (showExcelExportDialog) {
        ExcelExportOptionsDialog(
            accounts = displayAccounts,
            storeName = storeName,
            onExportExcel = {
                ExcelExportHelper.exportAllAccountsToExcel(context, displayAccounts, storeName)
                showExcelExportDialog = false
            },
            onExportCsv = {
                ExcelExportHelper.exportAllAccountsToCsv(context, displayAccounts)
                showExcelExportDialog = false
            },
            onDismiss = { showExcelExportDialog = false }
        )
    }
}

@Composable
fun ExcelExportOptionsDialog(
    accounts: List<AccountWithBalance>,
    storeName: String,
    onExportExcel: () -> Unit,
    onExportCsv: () -> Unit,
    onDismiss: () -> Unit
) {
    val totalLana = remember(accounts) { accounts.sumOf { it.totalLana } }
    val totalLaho = remember(accounts) { accounts.sumOf { it.totalLaho } }
    val netBalance = totalLana - totalLaho

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("excel_export_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF107C41).copy(alpha = 0.12f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Color(0xFF107C41),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "تصدير كشف الحسابات (Excel)",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "قالب مالي ملون للأرصدة والمبالغ الصافية",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Summary Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "عدد الحسابات المشمولة:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${accounts.size} حساب",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "إجمالي لنا (مدين):",
                                fontSize = 12.sp,
                                color = Color(0xFFDC2626)
                            )
                            Text(
                                text = com.example.ui.components.formatMoney(totalLana),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "إجمالي له (دائن):",
                                fontSize = 12.sp,
                                color = Color(0xFF16A34A)
                            )
                            Text(
                                text = com.example.ui.components.formatMoney(totalLaho),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A34A)
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "المبلغ الصافي العام:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (netBalance > 0) Color(0xFFFEE2E2) else if (netBalance < 0) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = "${com.example.ui.components.formatMoney(kotlin.math.abs(netBalance))} (${if (netBalance > 0) "لنا" else if (netBalance < 0) "له" else "مطابق"})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (netBalance > 0) Color(0xFF991B1B) else if (netBalance < 0) Color(0xFF166534) else Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                // Features highlight banner
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F766E).copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0F766E).copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "✨ ميزات القالب المالي المحاسبي:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F766E)
                        )
                        Text(
                            text = "• إبراز اسم الحساب والمبلغ الصافي بألوان وأحجام واضحة ومميزة\n• بطاقات إحصائية ملخصة بأعلى الشيت لأرصدة (لنا / له / الصافي)\n• متوافق تماماً مع Microsoft Excel و Google Sheets",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Primary Action: Colored Excel (.xls)
                Button(
                    onClick = onExportExcel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_excel_xls_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF107C41),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تصدير كشف Excel الملون (.xls)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Secondary Action: CSV
                OutlinedButton(
                    onClick = onExportCsv,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_excel_csv_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "تصدير كملف جدول بيانات (.csv)",
                        fontSize = 12.5.sp
                    )
                }

                // Dismiss
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "إلغاء",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}

