package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountEntity
import com.example.data.model.AdminUserAccount
import com.example.data.model.TransactionEntity
import com.example.ui.theme.LahoGreen
import com.example.ui.theme.LanaRed
import com.example.ui.theme.TawthiqPrimary
import com.example.ui.viewmodel.TawthiqViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminCustomerLedgerDialog(
    userAccount: AdminUserAccount,
    viewModel: TawthiqViewModel,
    onDismiss: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var data by remember { mutableStateOf<List<Pair<AccountEntity, List<TransactionEntity>>>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var expandedAccountId by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()

    fun loadLedgerData() {
        isLoading = true
        // Set a safety timeout so spinner never gets stuck
        val timeoutJob = scope.launch {
            delay(5000L)
            if (isLoading) isLoading = false
        }
        viewModel.getCustomersAndTransactionsForUser(userAccount.email) { result ->
            data = result
            isLoading = false
            timeoutJob.cancel()
        }
    }

    LaunchedEffect(userAccount.email) {
        loadLedgerData()
    }

    val filteredData = remember(data, searchQuery) {
        if (searchQuery.isBlank()) data
        else data.filter { (account, _) ->
            account.name.contains(searchQuery, ignoreCase = true) ||
            account.phone.contains(searchQuery, ignoreCase = true)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("admin_ledger_close_btn")
                ) {
                    Text("إغلاق", fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "زبائن ومعاملات: ${userAccount.merchantName.ifBlank { userAccount.storeName }}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1
                        )
                        Text(
                            text = userAccount.email,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { loadLedgerData() },
                            modifier = Modifier.testTag("admin_ledger_refresh_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "تحديث من السحابة",
                                tint = TawthiqPrimary
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(480.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("بحث عن زبون أو رقم هاتف...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_ledger_search_field")
                    )

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = TawthiqPrimary, modifier = Modifier.size(36.dp))
                                Text(
                                    text = "جاري جلب الزبائن والمعاملات من السحابة...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (filteredData.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "لا توجد زبائن أو قيود سحابية مسجلة لهذا الحساب حالياً.",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "💡 تنبيه: إذا كان المستخدم مسجلاً للتو على جهازه، تأكد من فتح التطبيق على جهازه لتبدأ المزامنة التلقائية الفورية لكافة المعاملات.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp
                                )
                                Button(
                                    onClick = { loadLedgerData() },
                                    colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("admin_ledger_retry_btn")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إعادة الفحص والتحديث السحابي", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            item {
                                Text(
                                    text = "إجمالي الزبائن: ${filteredData.size} | إجمالي المعاملات: ${filteredData.sumOf { it.second.size }}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TawthiqPrimary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            items(filteredData, key = { it.first.id }) { (account, transactions) ->
                                val isExpanded = expandedAccountId == account.id
                                val totalLana = transactions.filter { it.type == "LANA" }.sumOf { it.amount }
                                val totalLaho = transactions.filter { it.type == "LAHO" }.sumOf { it.amount }
                                val netBalance = totalLana - totalLaho
                                val df = DecimalFormat("#,##0.00")

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    expandedAccountId = if (isExpanded) null else account.id
                                                }
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
                                                        imageVector = Icons.Default.AccountCircle,
                                                        contentDescription = null,
                                                        tint = TawthiqPrimary,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = account.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                    Text(
                                                        text = "${transactions.size} معاملة • ${account.phone.ifBlank { "بدون هاتف" }}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "${df.format(netBalance)} ${account.currency}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = if (netBalance >= 0) LanaRed else LahoGreen
                                                )
                                                Text(
                                                    text = if (netBalance >= 0) "لنا (مطلوب)" else "له (دائن)",
                                                    fontSize = 10.sp,
                                                    color = if (netBalance >= 0) LanaRed else LahoGreen
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        // Transactions Table when expanded
                                        AnimatedVisibility(visible = isExpanded) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                                    .padding(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (transactions.isEmpty()) {
                                                    Text(
                                                        text = "لا توجد حركات مسجلة داخل هذا الحساب.",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                } else {
                                                    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                                                    transactions.forEach { tx ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .background(
                                                                    MaterialTheme.colorScheme.surface,
                                                                    RoundedCornerShape(8.dp)
                                                                )
                                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    text = tx.description.ifBlank { if (tx.type == "LANA") "مبيعات آجل / دين" else "تسديد / استلام دفعة" },
                                                                    fontWeight = FontWeight.Medium,
                                                                    fontSize = 12.sp
                                                                )
                                                                Text(
                                                                    text = "${sdf.format(Date(tx.date))} • ${if (tx.receiptNumber.isNotBlank()) "سند: " + tx.receiptNumber else ""}",
                                                                    fontSize = 10.sp,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                            val isLana = tx.type == "LANA"
                                                            Text(
                                                                text = "${if (isLana) "+" else "-"}${df.format(tx.amount)} ${tx.currency}",
                                                                color = if (isLana) LanaRed else LahoGreen,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 13.sp
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
                }
            }
        )
    }
}
