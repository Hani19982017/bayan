package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AccountWithBalance
import com.example.data.model.CurrencyBalance
import com.example.data.model.DueItem
import com.example.data.model.OverallSummary
import com.example.data.model.TopPurchaserItem
import com.example.data.model.OverdueAccountItem
import com.example.data.model.OverdueSeverity
import com.example.data.model.TransactionEntity
import com.example.ui.components.formatMoney
import com.example.ui.components.formatTransactionDate
import com.example.ui.components.shareWhatsAppStatement
import com.example.ui.theme.LahoGreen
import com.example.ui.theme.LahoGreenContainer
import com.example.ui.theme.LahoGreenText
import com.example.ui.theme.LanaRed
import com.example.ui.theme.LanaRedContainer
import com.example.ui.theme.LanaRedText
import com.example.ui.theme.ReportBlue
import com.example.ui.theme.ReportBlueBg
import com.example.ui.theme.ReportGreen
import com.example.ui.theme.ReportGreenBg
import com.example.ui.theme.ReportOrange
import com.example.ui.theme.ReportOrangeBg
import com.example.ui.theme.ReportPurple
import com.example.ui.theme.ReportPurpleBg
import com.example.ui.theme.ReportSkyBlue
import com.example.ui.theme.ReportSkyBlueBg
import com.example.ui.theme.ReportTeal
import com.example.ui.theme.ReportTealBg
import com.example.ui.theme.TawthiqAmber
import com.example.ui.theme.TawthiqAmberContainer
import com.example.ui.theme.TawthiqPrimary
import com.example.ui.theme.TawthiqPrimaryContainer
import com.example.ui.theme.TawthiqSecondary
import com.example.ui.theme.TawthiqSecondaryContainer
import com.example.data.model.AppPermissions
import com.example.ui.viewmodel.TawthiqViewModel

enum class ReportView {
    MENU,
    TOP_PURCHASING,
    TOP_OVERDUE,
    ACCOUNTS_SUMMARY,
    ACCOUNTS_BALANCES,
    CURRENCIES_BALANCES,
    CATEGORIES_BALANCES,
    DUE_DATES,
    BALANCE_CHART
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: TawthiqViewModel,
    onAccountClick: (Long) -> Unit
) {
    var currentView by remember { mutableStateOf(ReportView.MENU) }

    val hasViewReports = viewModel.hasPermission(AppPermissions.VIEW_REPORTS)

    if (!hasViewReports) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = LanaRed,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "عذراً، الصلاحية غير متوفرة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "حسابك الحالي لا يمتلك صلاحية [عرض التقارير والبيانات المالية]. يرجى التواصل مع مالك الحساب لتعديل صلاحياتك.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }
        }
        return
    }
    val accountsWithBalances by viewModel.accountsWithBalances.collectAsStateWithLifecycle()
    val currencyBalances by viewModel.currencyBalances.collectAsStateWithLifecycle()
    val defaultCurrency by viewModel.defaultCurrency.collectAsStateWithLifecycle()
    val dueItems by viewModel.dueItems.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val overallSummary by viewModel.overallSummary.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentView) {
                            ReportView.MENU -> "التقارير"
                            ReportView.TOP_PURCHASING -> "أكثر الحسابات شراءً"
                            ReportView.TOP_OVERDUE -> "أكثر الحسابات تأخراً بالدفع"
                            ReportView.ACCOUNTS_SUMMARY -> "ملخص الحسابات"
                            ReportView.ACCOUNTS_BALANCES -> "أرصدة الحسابات"
                            ReportView.CURRENCIES_BALANCES -> "أرصدة العملات"
                            ReportView.CATEGORIES_BALANCES -> "الأرصدة حسب التصانيف"
                            ReportView.DUE_DATES -> "تقرير المستحقات"
                            ReportView.BALANCE_CHART -> "مخطط الأرصدة"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = if (currentView == ReportView.MENU) Modifier.fillMaxWidth() else Modifier,
                        textAlign = if (currentView == ReportView.MENU) TextAlign.Center else TextAlign.Start
                    )
                },
                navigationIcon = {
                    if (currentView != ReportView.MENU) {
                        IconButton(onClick = { currentView = ReportView.MENU }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentView) {
                ReportView.MENU -> {
                    ReportsMenu(
                        onSelectReport = { currentView = it }
                    )
                }
                ReportView.TOP_PURCHASING -> {
                    TopPurchasingReport(
                        accounts = accountsWithBalances,
                        transactions = allTransactions,
                        onAccountClick = onAccountClick,
                        onExportExcel = { items ->
                            viewModel.exportTopPurchasingExcel(context, items)
                        }
                    )
                }
                ReportView.TOP_OVERDUE -> {
                    TopOverdueReport(
                        accounts = accountsWithBalances,
                        transactions = allTransactions,
                        dueItems = dueItems,
                        onAccountClick = onAccountClick,
                        onExportExcel = { items ->
                            viewModel.exportOverdueAccountsExcel(context, items)
                        }
                    )
                }
                ReportView.ACCOUNTS_SUMMARY -> {
                    AccountsSummaryReport(
                        summary = overallSummary,
                        accounts = accountsWithBalances,
                        defaultCurrency = defaultCurrency,
                        onAccountClick = onAccountClick
                    )
                }
                ReportView.ACCOUNTS_BALANCES -> {
                    AccountsBalancesReport(
                        accounts = accountsWithBalances,
                        onAccountClick = onAccountClick
                    )
                }
                ReportView.CURRENCIES_BALANCES -> {
                    CurrenciesBalancesReport(
                        currencies = currencyBalances
                    )
                }
                ReportView.CATEGORIES_BALANCES -> {
                    CategoriesBalancesReport(
                        accounts = accountsWithBalances,
                        onAccountClick = onAccountClick
                    )
                }
                ReportView.DUE_DATES -> {
                    DueDatesReport(
                        dueItems = dueItems
                    )
                }
                ReportView.BALANCE_CHART -> {
                    BalanceChartReport(
                        accounts = accountsWithBalances,
                        currencies = currencyBalances
                    )
                }
            }
        }
    }
}

/**
 * Main Reports Hub Menu (Including new Top Purchasing & Overdue reports)
 */
@Composable
fun ReportsMenu(
    onSelectReport: (ReportView) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. أكثر الحسابات شراءً وتعاملاً (مطلوب من المستخدم)
        item {
            TawthiqReportCard(
                title = "أكثر الحسابات شراءً",
                subtitle = "كشف ترتيب العملاء الأكثر شراءً وطلباً وفواتير",
                icon = Icons.Default.ShoppingCart,
                color = Color(0xFF0284C7),
                bgColor = Color(0xFFE0F2FE),
                onClick = { onSelectReport(ReportView.TOP_PURCHASING) }
            )
        }

        // 2. أكثر الحسابات تأخراً بالدفع (مطلوب من المستخدم)
        item {
            TawthiqReportCard(
                title = "أكثر الحسابات تأخراً بالدفع",
                subtitle = "كشف بالعملاء المتأخرين في السداد والديون المتعثرة",
                icon = Icons.Default.HistoryToggleOff,
                color = Color(0xFFDC2626),
                bgColor = Color(0xFFFEE2E2),
                onClick = { onSelectReport(ReportView.TOP_OVERDUE) }
            )
        }

        // 3. ملخص الحسابات
        item {
            TawthiqReportCard(
                title = "ملخص الحسابات",
                subtitle = "عرض ملخص الحسابات",
                icon = Icons.Default.BarChart,
                color = ReportTeal,
                bgColor = ReportTealBg,
                onClick = { onSelectReport(ReportView.ACCOUNTS_SUMMARY) }
            )
        }

        // 4. أرصدة الحسابات
        item {
            TawthiqReportCard(
                title = "أرصدة الحسابات",
                subtitle = "عرض تفصيل أرصدة الحسابات",
                icon = Icons.Default.AccountBalanceWallet,
                color = ReportGreen,
                bgColor = ReportGreenBg,
                onClick = { onSelectReport(ReportView.ACCOUNTS_BALANCES) }
            )
        }

        // 5. أرصدة العملات
        item {
            TawthiqReportCard(
                title = "أرصدة العملات",
                subtitle = "عرض تفصيلي لأرصدة جميع العملات",
                icon = Icons.Default.Work,
                color = ReportBlue,
                bgColor = ReportBlueBg,
                onClick = { onSelectReport(ReportView.CURRENCIES_BALANCES) }
            )
        }

        // 6. الأرصدة حسب التصانيف
        item {
            TawthiqReportCard(
                title = "الأرصدة حسب التصانيف",
                subtitle = "عرض الأرصدة مصنفة حسب التصانيف",
                icon = Icons.Default.LocalOffer,
                color = ReportPurple,
                bgColor = ReportPurpleBg,
                onClick = { onSelectReport(ReportView.CATEGORIES_BALANCES) }
            )
        }

        // 7. تقرير المستحقات
        item {
            TawthiqReportCard(
                title = "تقرير المستحقات",
                subtitle = "عرض المعاملات المستحقة على الحسابات",
                icon = Icons.Default.NotificationsActive,
                color = ReportOrange,
                bgColor = ReportOrangeBg,
                onClick = { onSelectReport(ReportView.DUE_DATES) }
            )
        }

        // 8. مخطط الأرصدة
        item {
            TawthiqReportCard(
                title = "مخطط الأرصدة",
                subtitle = "عرض الأرصدة ضمن فترة زمنية",
                icon = Icons.Default.TrendingUp,
                color = ReportSkyBlue,
                bgColor = ReportSkyBlueBg,
                onClick = { onSelectReport(ReportView.BALANCE_CHART) }
            )
        }
    }
}

/**
 * Report Card matching Screenshot 3 & 4 precisely:
 * Left: Circular button with left arrow ←
 * Middle: Title & Subtitle
 * Right: Rounded pastel square with colored icon
 */
@Composable
fun TawthiqReportCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    bgColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("report_card_${title}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Left Arrow Button (start side in RTL)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack, // In RTL layout, ArrowBack points left
                    contentDescription = "فتح التقرير",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Center Column with Title and Subtitle
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Rounded Square Icon Box on the Right (end side in RTL)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * 1. ملخص الحسابات
 */
@Composable
fun AccountsSummaryReport(
    summary: OverallSummary,
    accounts: List<AccountWithBalance>,
    defaultCurrency: String = "USD",
    onAccountClick: (Long) -> Unit
) {
    val activeCurrencies = summary.currencySummaries.map { it.currency }.distinct()
    var selectedReportCurrency by remember(activeCurrencies, defaultCurrency) {
        mutableStateOf(
            activeCurrencies.find { it.equals(defaultCurrency, ignoreCase = true) }
                ?: activeCurrencies.firstOrNull()
                ?: defaultCurrency.ifBlank { "USD" }
        )
    }

    val activeSummary = summary.currencySummaries.find { it.currency == selectedReportCurrency }
        ?: summary.currencySummaries.firstOrNull()
        ?: CurrencyBalance(
            currency = selectedReportCurrency,
            totalLana = summary.totalLana,
            totalLaho = summary.totalLaho,
            netBalance = summary.netBalance,
            accountCount = summary.totalAccounts
        )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الملخص المالي العام",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        if (activeCurrencies.size > 1) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = TawthiqPrimaryContainer
                            ) {
                                Text(
                                    text = "العملة: $selectedReportCurrency",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TawthiqPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (activeCurrencies.size > 1) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            activeCurrencies.forEach { cur ->
                                val isSelected = cur == selectedReportCurrency
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) TawthiqPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable { selectedReportCurrency = cur }
                                ) {
                                    Text(
                                        text = cur,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("إجمالي ما لنا (ديون)", fontSize = 12.sp, color = LanaRed)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${formatMoney(activeSummary.totalLana)} ${activeSummary.currency}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = LanaRed
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("إجمالي ما علينا (مستحقات)", fontSize = 12.sp, color = LahoGreen)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${formatMoney(activeSummary.totalLaho)} ${activeSummary.currency}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = LahoGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("صافي الرصيد:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = "${formatMoney(activeSummary.netBalance)} ${activeSummary.currency}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (activeSummary.netBalance >= 0) LanaRed else LahoGreen
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "قائمة الحسابات (${accounts.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        items(accounts, key = { it.account.id }) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAccountClick(item.account.id) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(item.account.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = "${item.account.category} • ${item.account.currency}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (item.netBalance > 0) LanaRedContainer else if (item.netBalance < 0) LahoGreenContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (item.netBalance > 0) "لنا ${formatMoney(item.netBalance)} ${item.account.currency}"
                            else if (item.netBalance < 0) "له ${formatMoney(-item.netBalance)} ${item.account.currency}"
                            else "خالص 0 ${item.account.currency}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.netBalance > 0) LanaRed else if (item.netBalance < 0) LahoGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 4. الأرصدة حسب التصانيف
 */
@Composable
fun CategoriesBalancesReport(
    accounts: List<AccountWithBalance>,
    onAccountClick: (Long) -> Unit
) {
    val grouped = remember(accounts) {
        accounts.groupBy { it.account.category }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (grouped.isEmpty()) {
            item {
                Text(
                    text = "لا توجد تصانيف مسجلة",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            grouped.forEach { (category, categoryAccounts) ->
                val categoryCurrencies = run {
                    val map = mutableMapOf<String, Triple<Double, Double, Double>>() // currency -> (lana, laho, net)
                    categoryAccounts.forEach { acc ->
                        if (acc.currencyBalances.isNotEmpty()) {
                            acc.currencyBalances.forEach { (cur, net) ->
                                val (c2Lana, c2Laho, _) = map.getOrDefault(cur, Triple(0.0, 0.0, 0.0))
                                if (net > 0) map[cur] = Triple(c2Lana + net, c2Laho, (c2Lana + net) - c2Laho)
                                else if (net < 0) map[cur] = Triple(c2Lana, c2Laho - net, c2Lana - (c2Laho - net))
                            }
                        } else {
                            val cur = acc.account.currency.ifBlank { "USD" }
                            val (c2Lana, c2Laho, _) = map.getOrDefault(cur, Triple(0.0, 0.0, 0.0))
                            val net = acc.netBalance
                            val newLana = if (net > 0) c2Lana + net else c2Lana
                            val newLaho = if (net < 0) c2Laho - net else c2Laho
                            map[cur] = Triple(newLana, newLaho, newLana - newLaho)
                        }
                    }
                    map
                }

                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(ReportPurpleBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocalOffer,
                                            contentDescription = null,
                                            tint = ReportPurple,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = category,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ReportPurpleBg
                                ) {
                                    Text(
                                        text = "${categoryAccounts.size} حسابات",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ReportPurple,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            categoryCurrencies.forEach { (cur, triple) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("لنا ($cur)", fontSize = 11.sp, color = LanaRed)
                                        Text("${formatMoney(triple.first)} $cur", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LanaRed)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("له ($cur)", fontSize = 11.sp, color = LahoGreen)
                                        Text("${formatMoney(triple.second)} $cur", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LahoGreen)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("الصافي ($cur)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            "${formatMoney(triple.third)} $cur",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (triple.third >= 0) LanaRed else LahoGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 2. أرصدة الحسابات التفصيلي
 */
@Composable
fun AccountsBalancesReport(
    accounts: List<AccountWithBalance>,
    onAccountClick: (Long) -> Unit
) {
    val currencyTotals = remember(accounts) {
        val map = mutableMapOf<String, Pair<Double, Double>>() // currency -> (lana, laho)
        accounts.forEach { item ->
            if (item.currencyBalances.isNotEmpty()) {
                item.currencyBalances.forEach { (cur, net) ->
                    val (c2Lana, c2Laho) = map.getOrDefault(cur, 0.0 to 0.0)
                    if (net > 0) map[cur] = (c2Lana + net) to c2Laho
                    else if (net < 0) map[cur] = c2Lana to (c2Laho - net)
                }
            } else {
                val cur = item.account.currency.ifBlank { "USD" }
                val (c2Lana, c2Laho) = map.getOrDefault(cur, 0.0 to 0.0)
                if (item.netBalance > 0) map[cur] = (c2Lana + item.netBalance) to c2Laho
                else if (item.netBalance < 0) map[cur] = c2Lana to (c2Laho - item.netBalance)
            }
        }
        map
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (currencyTotals.isEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("إجمالي الديون لنا", fontSize = 11.sp, color = LanaRed)
                                Text("0 USD", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LanaRed)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("إجمالي الديون علينا", fontSize = 11.sp, color = LahoGreen)
                                Text("0 USD", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LahoGreen)
                            }
                        }
                    } else {
                        currencyTotals.forEach { (cur, pair) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("إجمالي الديون لنا ($cur)", fontSize = 11.sp, color = LanaRed)
                                    Text("${formatMoney(pair.first)} $cur", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LanaRed)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("إجمالي الديون علينا ($cur)", fontSize = 11.sp, color = LahoGreen)
                                    Text("${formatMoney(pair.second)} $cur", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LahoGreen)
                                }
                            }
                        }
                    }
                }
            }
        }

        items(accounts.sortedByDescending { kotlin.math.abs(it.netBalance) }, key = { it.account.id }) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAccountClick(item.account.id) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(item.account.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${item.account.category} • ${item.transactionCount} حركات", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    val curBals = item.currencyBalances.filter { it.value != 0.0 }
                    if (curBals.size > 1) {
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            curBals.forEach { (cur, bal) ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (bal > 0) LanaRedContainer else if (bal < 0) LahoGreenContainer else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = if (bal > 0) "لنا ${formatMoney(bal)} $cur"
                                        else if (bal < 0) "له ${formatMoney(-bal)} $cur"
                                        else "خالص 0 $cur",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (bal > 0) LanaRed else if (bal < 0) LahoGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (item.netBalance > 0) LanaRedContainer else if (item.netBalance < 0) LahoGreenContainer else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (item.netBalance > 0) "لنا ${formatMoney(item.netBalance)} ${item.account.currency}"
                                else if (item.netBalance < 0) "له ${formatMoney(-item.netBalance)} ${item.account.currency}"
                                else "خالص 0 ${item.account.currency}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (item.netBalance > 0) LanaRed else if (item.netBalance < 0) LahoGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 2. أرصدة العملات التفصيلي
 */
@Composable
fun CurrenciesBalancesReport(
    currencies: List<CurrencyBalance>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (currencies.isEmpty()) {
            item {
                Text(
                    text = "لا توجد عملات مسجلة حالياً",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            items(currencies, key = { it.currency }) { cur ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = TawthiqSecondaryContainer
                                ) {
                                    Text(
                                        text = cur.currency,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TawthiqSecondary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "${cur.accountCount} حسابات تستخدم هذه العملة",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Net
                            Text(
                                text = "الصافي: ${formatMoney(cur.netBalance)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (cur.netBalance >= 0) TawthiqPrimary else LanaRed
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = LanaRedContainer,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("لنا (مدين)", fontSize = 10.sp, color = LanaRedText)
                                    Text(formatMoney(cur.totalLana), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LanaRed)
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = LahoGreenContainer,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("له (دائن)", fontSize = 10.sp, color = LahoGreenText)
                                    Text(formatMoney(cur.totalLaho), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LahoGreen)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. تقرير المستحقات والتذكيرات
 */
@Composable
fun DueDatesReport(
    dueItems: List<DueItem>
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (dueItems.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = TawthiqPrimary,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "لا توجد مبالغ مستحقة حالياً",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "جميع الالتزامات المالية في مسارها المحدد",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(dueItems, key = { it.transaction.id }) { item ->
                val isOverdue = item.daysRemaining < 0
                val isToday = item.daysRemaining == 0

                val statusColor = if (isOverdue) LanaRed else if (isToday) TawthiqAmber else TawthiqPrimary
                val statusBg = if (isOverdue) LanaRedContainer else if (isToday) TawthiqAmberContainer else TawthiqPrimaryContainer

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = item.accountName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = item.transaction.description.ifBlank { "مبلغ مستحق" },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = statusBg
                            ) {
                                Text(
                                    text = when {
                                        isOverdue -> "متأخر (${-item.daysRemaining} يوم)"
                                        isToday -> "يستحق اليوم"
                                        else -> "متبقي ${item.daysRemaining} يوم"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${if (item.transaction.type == "LANA") "لنا" else "له"}: ${formatMoney(item.transaction.amount)} ${item.transaction.currency}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (item.transaction.type == "LANA") LanaRed else LahoGreen
                            )

                            Button(
                                onClick = {
                                    val reminderMsg = "السلام عليكم ورحمة الله،\nأخي الكريم ${item.accountName}، نود تذكيركم بموعد استحقاق مبلغ ${item.transaction.amount} ${item.transaction.currency} بخصوص (${item.transaction.description}).\nشاكرين ومقدرين تعاونكم."
                                    shareWhatsAppStatement(context, item.accountPhone, reminderMsg)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إرسال تذكير واتساب", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 4. مخطط الأرصدة والتحليلات
 */
@Composable
fun BalanceChartReport(
    accounts: List<AccountWithBalance>,
    currencies: List<CurrencyBalance>
) {
    val totalDebtors = accounts.count { it.netBalance > 0 }
    val totalCreditors = accounts.count { it.netBalance < 0 }
    val totalSettled = accounts.count { it.netBalance == 0.0 }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Analytics Summary Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "توزيع الحسابات المالية",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$totalDebtors",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = LanaRed
                            )
                            Text("مدينون (لنا عندهم)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$totalCreditors",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = LahoGreen
                            )
                            Text("دائنون (لهم عندنا)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$totalSettled",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = TawthiqPrimary
                            )
                            Text("مسددون بالكامل", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Proportional Visual Bar
                    val total = (totalDebtors + totalCreditors + totalSettled).coerceAtLeast(1)
                    val debtorsRatio = totalDebtors.toFloat() / total
                    val creditorsRatio = totalCreditors.toFloat() / total
                    val settledRatio = totalSettled.toFloat() / total

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                    ) {
                        if (debtorsRatio > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(debtorsRatio)
                                    .background(LanaRed)
                            )
                        }
                        if (creditorsRatio > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(creditorsRatio)
                                    .background(LahoGreen)
                            )
                        }
                        if (settledRatio > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(settledRatio)
                                    .background(TawthiqPrimary)
                            )
                        }
                    }
                }
            }
        }

        // Top Debtors
        item {
            Text(
                text = "أعلى المدينين (مبالغ مطلوب تحصيلها)",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        val topDebtors = accounts.filter { it.netBalance > 0 }.sortedByDescending { it.netBalance }.take(5)
        if (topDebtors.isEmpty()) {
            item {
                Text("لا توجد مبالغ ديون مسجلة حالياً", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(topDebtors, key = { it.account.id }) { d ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(d.account.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            text = "${formatMoney(d.netBalance)} ${d.account.currency}",
                            color = LanaRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * تقرير أكثر الحسابات شراءً وتعاملاً
 */
@Composable
fun TopPurchasingReport(
    accounts: List<AccountWithBalance>,
    transactions: List<TransactionEntity>,
    onAccountClick: (Long) -> Unit,
    onExportExcel: (List<TopPurchaserItem>) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf("الكل") }
    var selectedSortBy by remember { mutableStateOf("PURCHASES") } // PURCHASES, COUNT, BALANCE

    // Group transactions by account
    val txByAccount = remember(transactions) {
        transactions.groupBy { it.accountId }
    }

    val availableCurrencies = remember(accounts) {
        listOf("الكل") + accounts.map { it.account.currency }.distinct()
    }

    // Build TopPurchaser items
    val allPurchasers = remember(accounts, txByAccount, selectedCurrency) {
        val filteredAccounts = if (selectedCurrency == "الكل") {
            accounts
        } else {
            accounts.filter { it.account.currency == selectedCurrency }
        }

        val totalPurchasesAll = filteredAccounts.sumOf { it.totalLana }.coerceAtLeast(1.0)

        filteredAccounts.map { accWithBal ->
            val accTxs = txByAccount[accWithBal.account.id] ?: emptyList()
            val lanaTxs = accTxs.filter { it.type == "LANA" }
            val purchaseCount = if (lanaTxs.isNotEmpty()) lanaTxs.size else if (accWithBal.totalLana > 0) 1 else 0
            val totalPurchases = accWithBal.totalLana
            val avg = if (purchaseCount > 0) totalPurchases / purchaseCount else 0.0
            val share = ((totalPurchases / totalPurchasesAll) * 100).toFloat()

            TopPurchaserItem(
                accountWithBalance = accWithBal,
                totalPurchases = totalPurchases,
                purchaseCount = purchaseCount,
                averagePurchase = avg,
                purchaseSharePercent = share,
                rank = 0
            )
        }
    }

    // Filter & Sort
    val sortedAndRanked = remember(allPurchasers, searchQuery, selectedSortBy) {
        val filtered = allPurchasers.filter { item ->
            val clean = searchQuery.trim()
            clean.isBlank() ||
                    item.accountWithBalance.account.name.contains(clean, ignoreCase = true) ||
                    item.accountWithBalance.account.phone.contains(clean) ||
                    "#${item.accountWithBalance.account.displayIndex}".contains(clean)
        }

        val sorted = when (selectedSortBy) {
            "COUNT" -> filtered.sortedWith(compareByDescending<TopPurchaserItem> { it.purchaseCount }.thenByDescending { it.totalPurchases })
            "BALANCE" -> filtered.sortedWith(compareByDescending<TopPurchaserItem> { it.accountWithBalance.netBalance }.thenByDescending { it.totalPurchases })
            else -> filtered.sortedWith(compareByDescending<TopPurchaserItem> { it.totalPurchases }.thenByDescending { it.purchaseCount })
        }

        sorted.mapIndexed { index, item -> item.copy(rank = index + 1) }
    }

    // KPI Metrics
    val totalPurchasesSum = sortedAndRanked.sumOf { it.totalPurchases }
    val totalInvoicesSum = sortedAndRanked.sumOf { it.purchaseCount }
    val topBuyer = sortedAndRanked.firstOrNull()
    val averageTicket = if (totalInvoicesSum > 0) totalPurchasesSum / totalInvoicesSum else 0.0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // KPI Summary Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFE0F2FE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "مؤشرات المبيعات والمشتريات",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "تحليل العملاء الأكثر طلباً وتعبيراً عن النشاط",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { onExportExcel(sortedAndRanked) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Excel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Total Purchases
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("إجمالي المشتريات", fontSize = 11.sp, color = Color(0xFF1E40AF), fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatMoney(totalPurchasesSum),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D4ED8)
                                )
                            }
                        }

                        // Top Buyer
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("العميل المتصدر 🥇", fontSize = 11.sp, color = Color(0xFF92400E), fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = topBuyer?.accountWithBalance?.account?.name ?: "لا يوجد",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Total Invoices
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("إجمالي الفواتير", fontSize = 11.sp, color = Color(0xFF166534), fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$totalInvoicesSum فاتورة",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16A34A)
                                )
                            }
                        }

                        // Average Ticket
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("متوسط الفاتورة", fontSize = 11.sp, color = Color(0xFF5B21B6), fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatMoney(averageTicket),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6D28D9)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("top_purchasing_search"),
                placeholder = { Text("بحث عن عميل بالاسم، الرقم، أو الكود...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF0284C7),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }

        // Filters: Currency & Sorting Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Sorting row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ترتيب حسب:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    FilterChip(
                        selected = selectedSortBy == "PURCHASES",
                        onClick = { selectedSortBy = "PURCHASES" },
                        label = { Text("الأعلى مشتريات 💰", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White
                        )
                    )

                    FilterChip(
                        selected = selectedSortBy == "COUNT",
                        onClick = { selectedSortBy = "COUNT" },
                        label = { Text("الأكثر فواتير 📑", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White
                        )
                    )

                    FilterChip(
                        selected = selectedSortBy == "BALANCE",
                        onClick = { selectedSortBy = "BALANCE" },
                        label = { Text("الأعلى رصيداً صافياً ⚖️", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White
                        )
                    )
                }

                // Currency chips
                if (availableCurrencies.size > 2) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("العملة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        availableCurrencies.forEach { cur ->
                            FilterChip(
                                selected = selectedCurrency == cur,
                                onClick = { selectedCurrency = cur },
                                label = { Text(cur, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Results header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ترتيب العملاء (${sortedAndRanked.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "النسبة من إجمالي النشاط",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Empty state
        if (sortedAndRanked.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("لا توجد حسابات أو مشتريات مطابقة للبحث", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        } else {
            items(sortedAndRanked, key = { it.accountWithBalance.account.id }) { item ->
                TopPurchaserCard(
                    item = item,
                    onAccountClick = { onAccountClick(item.accountWithBalance.account.id) },
                    onWhatsAppClick = {
                        val acc = item.accountWithBalance.account
                        val msg = "السلام عليكم ورحمة الله،\nأخي الكريم ${acc.name}، نشكركم على تعاملكم المستمر معنا.\nإجمالي قيمة مشترياتكم المسجلة: ${formatMoney(item.totalPurchases)} ${acc.currency}.\nالرصيد الصافي الحالي: ${if (item.accountWithBalance.netBalance > 0) "لنا: ${formatMoney(item.accountWithBalance.netBalance)}" else "خالص"}.\nنتشرف بخدمتكم دائماً."
                        shareWhatsAppStatement(context, acc.phone, msg)
                    },
                    onCallClick = {
                        val phone = item.accountWithBalance.account.phone.trim()
                        if (phone.isNotBlank()) {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    }
                )
            }
        }
    }
}

/**
 * بطاقة العميل الأكثر شراءً
 */
@Composable
fun TopPurchaserCard(
    item: TopPurchaserItem,
    onAccountClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onCallClick: () -> Unit
) {
    val acc = item.accountWithBalance.account

    val rankBgColor = when (item.rank) {
        1 -> Color(0xFFFEF3C7)
        2 -> Color(0xFFF1F5F9)
        3 -> Color(0xFFFFEDD5)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val rankTextColor = when (item.rank) {
        1 -> Color(0xFFB45309)
        2 -> Color(0xFF475569)
        3 -> Color(0xFFC2410C)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val rankMedal = when (item.rank) {
        1 -> "🥇 المركز 1"
        2 -> "🥈 المركز 2"
        3 -> "🥉 المركز 3"
        else -> "#${item.rank}"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAccountClick() }
            .testTag("top_purchaser_card_${acc.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (item.rank <= 3) rankTextColor.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Rank badge, Name, Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(rankBgColor)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = rankMedal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = rankTextColor
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = acc.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (acc.phone.isNotBlank()) {
                            Text(
                                text = acc.phone,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = acc.category.ifBlank { "عميل" },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Metrics: Purchases & Invoices
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("إجمالي المشتريات", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${formatMoney(item.totalPurchases)} ${acc.currency}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("عدد الفواتير", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${item.purchaseCount} فاتورة",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("الرصيد الصافي", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val net = item.accountWithBalance.netBalance
                    Text(
                        text = if (net > 0) "لنا: ${formatMoney(net)}" else if (net < 0) "له: ${formatMoney(-net)}" else "خالص ✓",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (net > 0) LanaRed else if (net < 0) LahoGreen else TawthiqPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar of Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = (item.purchaseSharePercent / 100f).coerceIn(0.02f, 1f))
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0284C7))
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${String.format(Locale.ENGLISH, "%.1f", item.purchaseSharePercent)}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0284C7)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onWhatsAppClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("واتساب", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }

                if (acc.phone.isNotBlank()) {
                    Button(
                        onClick = onCallClick,
                        modifier = Modifier.weight(0.9f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("اتصال", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Button(
                    onClick = onAccountClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("كشف الحساب", fontSize = 11.sp, color = Color.White)
                }
            }
        }
    }
}

/**
 * تقرير أكثر الحسابات تأخراً بالدفع والديون المتعثرة
 */
@Composable
fun TopOverdueReport(
    accounts: List<AccountWithBalance>,
    transactions: List<TransactionEntity>,
    dueItems: List<DueItem>,
    onAccountClick: (Long) -> Unit,
    onExportExcel: (List<OverdueAccountItem>) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedSeverityFilter by remember { mutableStateOf("ALL") } // ALL, CRITICAL, WARNING, DUE
    var selectedSortBy by remember { mutableStateOf("DELAY") } // DELAY, AMOUNT

    val now = System.currentTimeMillis()

    // Group transactions by account
    val txByAccount = remember(transactions) {
        transactions.groupBy { it.accountId }
    }

    // Build Overdue items
    val overdueItems = remember(accounts, txByAccount, dueItems) {
        accounts.filter { it.netBalance > 0 }.mapNotNull { accWithBal ->
            val accTxs = txByAccount[accWithBal.account.id] ?: emptyList()
            val dueTx = accTxs.filter { it.dueDate != null && it.type == "LANA" }.minByOrNull { it.dueDate ?: Long.MAX_VALUE }
            val oldestLanaTx = accTxs.filter { it.type == "LANA" }.minByOrNull { it.date }
            val lastPayment = accTxs.filter { it.type == "LAHO" }.maxByOrNull { it.date }

            // Calculate overdue days
            val delayDays = if (dueTx?.dueDate != null) {
                val diff = now - dueTx.dueDate
                if (diff > 0) java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff).toInt() else 0
            } else if (oldestLanaTx != null) {
                val diff = now - oldestLanaTx.date
                java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff).toInt()
            } else {
                val diff = now - accWithBal.account.createdAt
                java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff).toInt()
            }

            val severity = when {
                delayDays > 30 -> OverdueSeverity.CRITICAL
                delayDays > 14 -> OverdueSeverity.WARNING
                delayDays > 0 -> OverdueSeverity.DUE
                else -> OverdueSeverity.UPCOMING
            }

            OverdueAccountItem(
                accountWithBalance = accWithBal,
                overdueAmount = accWithBal.netBalance,
                daysOverdue = delayDays,
                oldestUnpaidDate = oldestLanaTx?.date,
                dueDate = dueTx?.dueDate,
                lastPaymentDate = lastPayment?.date,
                lastPaymentAmount = lastPayment?.amount,
                severity = severity
            )
        }
    }

    // Filter & Sort
    val filteredAndSorted = remember(overdueItems, searchQuery, selectedSeverityFilter, selectedSortBy) {
        val filtered = overdueItems.filter { item ->
            val matchesSeverity = when (selectedSeverityFilter) {
                "CRITICAL" -> item.severity == OverdueSeverity.CRITICAL
                "WARNING" -> item.severity == OverdueSeverity.WARNING
                "DUE" -> item.severity == OverdueSeverity.DUE || item.severity == OverdueSeverity.CRITICAL || item.severity == OverdueSeverity.WARNING
                else -> true
            }

            val clean = searchQuery.trim()
            val matchesQuery = clean.isBlank() ||
                    item.accountWithBalance.account.name.contains(clean, ignoreCase = true) ||
                    item.accountWithBalance.account.phone.contains(clean) ||
                    "#${item.accountWithBalance.account.displayIndex}".contains(clean)

            matchesSeverity && matchesQuery
        }

        when (selectedSortBy) {
            "AMOUNT" -> filtered.sortedWith(compareByDescending<OverdueAccountItem> { it.overdueAmount }.thenByDescending { it.daysOverdue })
            else -> filtered.sortedWith(compareByDescending<OverdueAccountItem> { it.daysOverdue }.thenByDescending { it.overdueAmount })
        }
    }

    // KPI Metrics
    val totalOverdueDebt = filteredAndSorted.sumOf { it.overdueAmount }
    val maxDelay = filteredAndSorted.maxOfOrNull { it.daysOverdue } ?: 0
    val criticalCount = filteredAndSorted.count { it.severity == OverdueSeverity.CRITICAL }
    val largestDebt = filteredAndSorted.maxOfOrNull { it.overdueAmount } ?: 0.0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // KPI Summary Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFEE2E2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HistoryToggleOff,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "مؤشرات الديون المتأخرة والتحصيل",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "متابعة الحسابات المتعثرة لضمان سرعة السداد",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { onExportExcel(filteredAndSorted) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Excel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Total Overdue
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("إجمالي الديون المتأخرة", fontSize = 11.sp, color = Color(0xFF991B1B), fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatMoney(totalOverdueDebt),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626)
                                )
                            }
                        }

                        // Overdue Accounts Count
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("الحسابات المتأخرة", fontSize = 11.sp, color = Color(0xFF92400E), fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${filteredAndSorted.size} عميل",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Longest Delay
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("أطول مدة تأخير", fontSize = 11.sp, color = Color(0xFF7F1D1D), fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$maxDelay يوم",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB91C1C)
                                )
                            }
                        }

                        // Critical Overdue Count
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("تأخر حرج (+30 يوم)", fontSize = 11.sp, color = Color(0xFF1E40AF), fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$criticalCount عملاء",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D4ED8)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Search Field
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("top_overdue_search"),
                placeholder = { Text("بحث عن عميل متأخر بالاسم، الرقم، أو الكود...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFDC2626)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFDC2626),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }

        // Filter chips: Severity & Sort
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Severity row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("درجة التأخير:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    FilterChip(
                        selected = selectedSeverityFilter == "ALL",
                        onClick = { selectedSeverityFilter = "ALL" },
                        label = { Text("الكل", fontSize = 11.sp) }
                    )

                    FilterChip(
                        selected = selectedSeverityFilter == "CRITICAL",
                        onClick = { selectedSeverityFilter = "CRITICAL" },
                        label = { Text("🚨 تأخر حرج (+30 يوم)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFDC2626),
                            selectedLabelColor = Color.White
                        )
                    )

                    FilterChip(
                        selected = selectedSeverityFilter == "WARNING",
                        onClick = { selectedSeverityFilter = "WARNING" },
                        label = { Text("⚠️ متأخر (+15 يوم)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFEA580C),
                            selectedLabelColor = Color.White
                        )
                    )
                }

                // Sort row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ترتيب حسب:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    FilterChip(
                        selected = selectedSortBy == "DELAY",
                        onClick = { selectedSortBy = "DELAY" },
                        label = { Text("الأطول فترة تأخير ⏳", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFDC2626),
                            selectedLabelColor = Color.White
                        )
                    )

                    FilterChip(
                        selected = selectedSortBy == "AMOUNT",
                        onClick = { selectedSortBy = "AMOUNT" },
                        label = { Text("الأعلى مبلغ دين 💰", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFDC2626),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "قائمة الحسابات المتأخرة بالسداد (${filteredAndSorted.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "مرتبة حسب الأولوية",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Empty state
        if (filteredAndSorted.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LahoGreen, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("رائع! لا توجد حسابات متأخرة في السداد حالياً", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("جميع العملاء مسددون أو لا توجد ديون متعثرة", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(filteredAndSorted, key = { it.accountWithBalance.account.id }) { item ->
                OverdueAccountCard(
                    item = item,
                    onAccountClick = { onAccountClick(item.accountWithBalance.account.id) },
                    onWhatsAppReminder = {
                        val acc = item.accountWithBalance.account
                        val reminderMsg = "السلام عليكم ورحمة الله،\nأخي الكريم ${acc.name}، نود تذكيركم بلطف بوجود رصيد مستحق السداد بمبلغ ${formatMoney(item.overdueAmount)} ${acc.currency} (متأخر منذ ${item.daysOverdue} يوم).\nنرجو منكم التكرم بالمبادرة بالسداد أو التنسيق معنا.\nشاكرين ومقدرين حسن تعاونكم الدائم."
                        shareWhatsAppStatement(context, acc.phone, reminderMsg)
                    },
                    onCallClick = {
                        val phone = item.accountWithBalance.account.phone.trim()
                        if (phone.isNotBlank()) {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    }
                )
            }
        }
    }
}

/**
 * بطاقة الحساب المتأخر في السداد
 */
@Composable
fun OverdueAccountCard(
    item: OverdueAccountItem,
    onAccountClick: () -> Unit,
    onWhatsAppReminder: () -> Unit,
    onCallClick: () -> Unit
) {
    val acc = item.accountWithBalance.account

    val badgeBgColor = when (item.severity) {
        OverdueSeverity.CRITICAL -> Color(0xFFFEE2E2)
        OverdueSeverity.WARNING -> Color(0xFFFEF3C7)
        else -> Color(0xFFF1F5F9)
    }

    val badgeTextColor = when (item.severity) {
        OverdueSeverity.CRITICAL -> Color(0xFF991B1B)
        OverdueSeverity.WARNING -> Color(0xFF92400E)
        else -> Color(0xFF334155)
    }

    val delayLabel = when (item.severity) {
        OverdueSeverity.CRITICAL -> "🚨 متأخر منذ ${item.daysOverdue} يوم (حرج)"
        OverdueSeverity.WARNING -> "⚠️ متأخر منذ ${item.daysOverdue} يوم"
        OverdueSeverity.DUE -> "⏰ مستحق منذ ${item.daysOverdue} يوم"
        OverdueSeverity.UPCOMING -> "⏳ يحل قريباً"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAccountClick() }
            .testTag("overdue_card_${acc.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (item.severity == OverdueSeverity.CRITICAL) Color(0xFFEF4444).copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Delay badge + Account Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = acc.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (acc.phone.isNotBlank()) {
                        Text(
                            text = acc.phone,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBgColor)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = delayLabel,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Overdue Debt Amount & Payment status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("مبلغ الدين المتأخر", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${formatMoney(item.overdueAmount)} ${acc.currency}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = LanaRed
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("آخر دفعة مسددة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (item.lastPaymentAmount != null && item.lastPaymentDate != null) {
                        Text(
                            text = "${formatMoney(item.lastPaymentAmount)} ${acc.currency}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = LahoGreen
                        )
                        Text(
                            text = formatTransactionDate(item.lastPaymentDate),
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "لم تسجل دفعات",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Send WhatsApp Payment Reminder, Call, Open Ledger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onWhatsAppReminder,
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تذكير سداد واتساب", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }

                if (acc.phone.isNotBlank()) {
                    Button(
                        onClick = onCallClick,
                        modifier = Modifier.weight(0.8f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("اتصال", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Button(
                    onClick = onAccountClick,
                    modifier = Modifier.weight(0.9f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("كشف الحساب", fontSize = 11.sp, color = Color.White)
                }
            }
        }
    }
}

