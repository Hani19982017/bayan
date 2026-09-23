package com.example.ui.components

import com.example.data.model.CurrencyBalance

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.data.model.AccountEntity
import com.example.data.model.AccountWithBalance
import com.example.data.model.OverallSummary
import com.example.data.model.TransactionEntity
import com.example.ui.theme.BackgroundLight
import com.example.ui.theme.BorderLight
import com.example.ui.theme.LahoGreen
import com.example.ui.theme.LahoGreenContainer
import com.example.ui.theme.LahoGreenText
import com.example.ui.theme.LanaRed
import com.example.ui.theme.LanaRedContainer
import com.example.ui.theme.LanaRedText
import com.example.ui.theme.TawthiqAmber
import com.example.ui.theme.TawthiqAmberContainer
import com.example.ui.theme.TawthiqPrimary
import com.example.ui.theme.TawthiqPrimaryContainer
import com.example.ui.theme.TawthiqSecondary
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val AvatarColors = listOf(
    Color(0xFF0D9488), // Teal
    Color(0xFF2563EB), // Blue
    Color(0xFFD97706), // Amber
    Color(0xFF7C3AED), // Purple
    Color(0xFFDC2626), // Red
    Color(0xFF059669)  // Green
)

fun formatMoney(amount: Double): String {
    if (amount.isNaN() || amount.isInfinite()) return "0"
    val symbols = java.text.DecimalFormatSymbols(java.util.Locale.US)
    val formatter = DecimalFormat("#,##0.##", symbols)
    return formatter.format(amount)
}

@Composable
fun tawthiqTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color.White else Color(0xFF0F172A),
    unfocusedTextColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color.White else Color(0xFF0F172A),
    disabledTextColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B),
    cursorColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color.White else TawthiqPrimary,
    unfocusedBorderColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF64748B) else Color(0xFFCBD5E1),
    focusedBorderColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF2DD4BF) else TawthiqPrimary,
    unfocusedContainerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF0F172A) else Color(0xFFF8FAFC),
    focusedContainerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF0F172A) else Color.White,
    unfocusedLabelColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFFCBD5E1) else Color(0xFF475569),
    focusedLabelColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF2DD4BF) else TawthiqPrimary,
    unfocusedPlaceholderColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF94A3B8) else Color(0xFF64748B),
    focusedPlaceholderColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF94A3B8) else Color(0xFF64748B)
)

fun formatRelativeArabicTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        diff < 0 -> "تاريخ قادم"
        minutes < 2 -> "الآن"
        minutes < 60 -> "منذ $minutes دقيقة"
        hours < 2 -> "منذ ساعة"
        hours == 2L -> "منذ ساعتين"
        hours < 24 -> "منذ $hours ساعة"
        days == 1L -> "منذ يوم"
        days == 2L -> "منذ يومين"
        days < 10 -> "منذ $days أيام"
        else -> {
            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

fun formatTransactionTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun formatTransactionDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun openDialer(context: Context, phone: String) {
    if (phone.isNotBlank()) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
        context.startActivity(intent)
    }
}

fun shareWhatsAppStatement(context: Context, phone: String, message: String) {
    try {
        val cleanPhone = phone.replace("+", "").replace(" ", "").replace("-", "")
        val uri = if (cleanPhone.isNotBlank()) {
            Uri.parse("https://wa.me/$cleanPhone?text=${Uri.encode(message)}")
        } else {
            Uri.parse("whatsapp://send?text=${Uri.encode(message)}")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (_: Exception) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(shareIntent, "مشاركة كشف الحساب"))
    }
}

/**
 * Al-Bayan Logo icon badge
 */
@Composable
fun BayanLogo(
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    showText: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.bayan_logo),
            contentDescription = "شعار البيان",
            modifier = Modifier
                .height(size)
                .width(size * 1.85f),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
        if (showText) {
            Text(
                text = "البيان",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Top App Bar with Bayan Logo, Title & Cloud Sync Indicator
 */
@Composable
fun TawthiqHeader(
    title: String = "البيان للحسابات المالية",
    onAddAccountClick: () -> Unit = {},
    showCloudStatus: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Mini Logo Emblem
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(TawthiqPrimary, Color(0xFF0F766E))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "Tawthiq",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "حول دفتر ديونك لتطبيق ذكي",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showCloudStatus) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = TawthiqPrimaryContainer,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = "Sync",
                                tint = TawthiqPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "مُزامن",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TawthiqPrimary
                            )
                        }
                    }
                }

                Button(
                    onClick = onAddAccountClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary),
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("add_account_header_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "حساب جديد",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "حساب جديد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Overall Summary Banner Cards (ما لنا / ما علينا / صافي الرصيد)
 */
@Composable
fun BalanceSummaryCards(
    summary: OverallSummary,
    defaultCurrency: String = "USD",
    selectedCurrency: String = "ALL",
    onCurrencySelected: (String) -> Unit = {},
    showOnlyNet: Boolean = false
) {
    val activeCurrencies = summary.currencySummaries.map { it.currency }.distinct()
    var internalSelectedCurrency by remember(selectedCurrency, activeCurrencies, defaultCurrency) {
        mutableStateOf(
            if (selectedCurrency != "ALL" && activeCurrencies.contains(selectedCurrency)) selectedCurrency
            else activeCurrencies.find { it.equals(defaultCurrency, ignoreCase = true) } ?: activeCurrencies.firstOrNull() ?: defaultCurrency.ifBlank { "USD" }
        )
    }

    val currentCurrency = if (selectedCurrency != "ALL" && activeCurrencies.contains(selectedCurrency)) selectedCurrency else internalSelectedCurrency

    val activeSummary = summary.currencySummaries.find { it.currency == currentCurrency }
        ?: summary.currencySummaries.firstOrNull()
        ?: CurrencyBalance(
            currency = currentCurrency,
            totalLana = summary.totalLana,
            totalLaho = summary.totalLaho,
            netBalance = summary.netBalance,
            accountCount = summary.totalAccounts
        )

    Column(modifier = Modifier.fillMaxWidth()) {
        // Currency Selector Pills Row if there are multiple currencies
        if (activeCurrencies.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Label "العملة:"
                Text(
                    text = "العملة:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (selectedCurrency == "ALL") TawthiqPrimary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable {
                        onCurrencySelected("ALL")
                    }
                ) {
                    Text(
                        text = "جميع العملات (${activeCurrencies.size})",
                        fontSize = 11.5.sp,
                        fontWeight = if (selectedCurrency == "ALL") FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedCurrency == "ALL") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                activeCurrencies.forEach { cur ->
                    val isSelected = (selectedCurrency == cur) || (selectedCurrency == "ALL" && cur == currentCurrency)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) TawthiqPrimary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable {
                            onCurrencySelected(cur)
                            internalSelectedCurrency = cur
                        }
                    ) {
                        Text(
                            text = cur,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        if (showOnlyNet) {
            // Screen 9: Single, clean Net Balance card (صافي الرصيد فقط)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (activeSummary.netBalance > 0) LanaRedContainer.copy(alpha = 0.65f)
                    else if (activeSummary.netBalance < 0) LahoGreenContainer.copy(alpha = 0.65f)
                    else TawthiqPrimaryContainer.copy(alpha = 0.65f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "صافي الرصيد العام",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (activeSummary.netBalance > 0) "إجمالي ديون لنا (مطلوب)" else if (activeSummary.netBalance < 0) "إجمالي مستحقات علينا (دائن)" else "الحسابات متوازنة تماماً",
                            fontSize = 11.sp,
                            color = if (activeSummary.netBalance > 0) LanaRedText else if (activeSummary.netBalance < 0) LahoGreenText else TawthiqPrimary
                        )
                    }

                    Text(
                        text = "${if (activeSummary.netBalance > 0) "↗ +" else if (activeSummary.netBalance < 0) "↙ -" else ""}${formatMoney(kotlin.math.abs(activeSummary.netBalance))} ${activeSummary.currency}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeSummary.netBalance > 0) LanaRed else if (activeSummary.netBalance < 0) LahoGreenText else TawthiqPrimary
                    )
                }
            }
        } else {
            // Full 3 cards breakdown (for Reports screen)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Card 1: إجمالي ما لنا
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = LanaRedContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "إجمالي ما لنا",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = LanaRedText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${formatMoney(activeSummary.totalLana)} ${activeSummary.currency}",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = LanaRed,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "ديون مستحقة",
                            fontSize = 10.sp,
                            color = LanaRedText.copy(alpha = 0.8f)
                        )
                    }
                }

                // Card 2: إجمالي ما علينا
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = LahoGreenContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "إجمالي ما علينا",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = LahoGreenText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${formatMoney(activeSummary.totalLaho)} ${activeSummary.currency}",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = LahoGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "مستحقات علينا",
                            fontSize = 10.sp,
                            color = LahoGreenText.copy(alpha = 0.8f)
                        )
                    }
                }

                // Card 3: صافي الرصيد
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = TawthiqPrimaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "صافي الرصيد",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TawthiqPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${formatMoney(activeSummary.netBalance)} ${activeSummary.currency}",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeSummary.netBalance >= 0) TawthiqPrimary else LanaRed,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${activeSummary.accountCount} حسابات",
                            fontSize = 10.sp,
                            color = TawthiqPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Category Filter Horizontal Bar matching Screenshot 3 (+ button on left, pills for categories)
 */
@Composable
fun CategoryFilterRow(
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    onAddCategoryClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circular '+' Button on the left
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8F8F0))
                .clickable { onAddCategoryClick() }
                .testTag("add_category_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "إضافة تصنيف",
                tint = Color(0xFF10B981),
                modifier = Modifier.size(20.dp)
            )
        }

        // Categories list
        categories.forEach { cat ->
            val isSelected = cat == selectedCategory
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                modifier = Modifier.clickable { onSelectCategory(cat) }
            ) {
                Text(
                    text = cat,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp,
                    color = if (isSelected) Color(0xFF16A34A) else Color(0xFF475569),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * Top App Bar matching the exact screenshot layout:
 * - Left: Tawthiq Cloud + Notebook Logo
 * - Right: QR Scanner, Search, Batch Actions (Checklist), Notification Bell with Red Badge
 */
@Composable
fun TawthiqTopBar(
    onSearchClick: () -> Unit,
    onQrClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onBatchActionsClick: () -> Unit = {},
    unreadNotificationCount: Int = 1,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Bayan Logo + Theme Toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BayanLogo(
                size = 38.dp,
                showText = false
            )

            // Brightness / Theme Toggle (شكل السطوع وتغيير الإضاءة)
            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("top_bar_theme_toggle_button")
            ) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "السطوع وتغيير الإضاءة",
                    tint = if (isDarkTheme) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Right side: QR Scanner, Search, Batch Actions (Checklist), Notification Bell with Red Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 1. QR scanner icon (in blue)
            IconButton(
                onClick = onQrClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("top_bar_qr_button")
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "مسح باركود",
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(24.dp)
                )
            }

            // 2. Search Icon
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("top_bar_search_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "بحث",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }

            // 3. Batch Actions (Checklist / إجراءات جماعية)
            IconButton(
                onClick = onBatchActionsClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("top_bar_batch_actions_button")
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Outlined.Checklist,
                    contentDescription = "إجراءات جماعية",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(25.dp)
                )
            }

            // 4. Notification Bell with Red Badge
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onNotificationClick,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("top_bar_notifications_button")
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Outlined.Notifications,
                        contentDescription = "التنبيهات",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Red solid circular badge on top-right of bell
                if (unreadNotificationCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-2).dp, y = 2.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadNotificationCount > 9) "+9" else unreadNotificationCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bottom Sheet for "إجراءات جماعية" matching the uploaded screenshot exactly in Arabic RTL:
 * - Header with Title and Green Checklist Icon on the right
 * - Option 1: إضافة معاملة جماعية (Green)
 * - Option 2: إرسال تذكير جماعي (Blue)
 * - Left side chevron (<) for navigation
 * - Option 3: إلغاء (Cancel)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchActionsBottomSheet(
    onDismiss: () -> Unit,
    onAddBatchTransactionClick: () -> Unit,
    onSendBatchReminderClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Green Checklist icon on Right, and title "إجراءات جماعية"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Right: Green rounded icon container
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFE8F8F0))
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Outlined.Checklist,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "إجراءات جماعية",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                // Option 1: إضافة معاملة جماعية (Green)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable {
                            onDismiss()
                            onAddBatchTransactionClick()
                        }
                        .testTag("batch_add_transaction_option"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Right: Mint Box with note/receipt icon
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE8F8F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Middle: Text Details (aligned right in RTL)
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "إضافة معاملة جماعية",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF10B981)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "إضافة نفس المعاملة لعدة حسابات",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        // Left: Chevron pointing left (<) in RTL
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Option 2: إرسال تذكير جماعي (Blue)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable {
                            onDismiss()
                            onSendBatchReminderClick()
                        }
                        .testTag("batch_send_reminder_option"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Right: Light Blue Box with send/plane icon
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE0F2FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Middle: Text Details (aligned right in RTL)
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "إرسال تذكير جماعي",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF0284C7)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "إرسال تذكير لعدة حسابات مرتبطة",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        // Left: Chevron pointing left (<) in RTL
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cancel button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "إلغاء",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Dialog for Batch Adding a Transaction to Multiple Selected Accounts
 */
@Composable
fun BatchAddTransactionDialog(
    accounts: List<AccountWithBalance>,
    onDismiss: () -> Unit,
    onConfirm: (selectedAccountIds: List<Long>, type: String, amount: Double, description: String) -> Unit
) {
    var selectedIds by remember { mutableStateOf(accounts.map { it.account.id }.toSet()) }
    var type by remember { mutableStateOf("LANA") } // "LANA" (لنا) or "LAHO" (له)
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    val filteredList = remember(accounts, searchQuery) {
        if (searchQuery.isBlank()) accounts
        else accounts.filter { it.account.name.contains(searchQuery, ignoreCase = true) }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "إضافة معاملة جماعية",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE8F8F0)
                    ) {
                        Text(
                            text = "المحدد: ${selectedIds.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                ) {
                    // Type Switcher: لنا (مدين) vs له (دائن)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { type = "LANA" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == "LANA") LanaRed else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (type == "LANA") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("لنا (سحب/دين)", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { type = "LAHO" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == "LAHO") LahoGreenText else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (type == "LAHO") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("له (دفعة/إيداع)", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Amount Field
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("المبلغ المالي") },
                        placeholder = { Text("مثال: 5000") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true
                    )

                    // Description Field
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("البيان / تفاصيل المعاملة") },
                        placeholder = { Text("مثال: اشتراك شهري، صيانة...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Select All / Deselect All Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("اختر الحسابات:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        TextButton(onClick = {
                            selectedIds = if (selectedIds.size == accounts.size) emptySet() else accounts.map { it.account.id }.toSet()
                        }) {
                            Text(if (selectedIds.size == accounts.size) "إلغاء تحديد الكل" else "تحديد الكل")
                        }
                    }

                    // Accounts Selection List
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .padding(4.dp)
                    ) {
                        items(filteredList.size) { idx ->
                            val item = filteredList[idx]
                            val isChecked = selectedIds.contains(item.account.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedIds = if (isChecked) selectedIds - item.account.id else selectedIds + item.account.id
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            selectedIds = if (checked) selectedIds + item.account.id else selectedIds - item.account.id
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF10B981))
                                    )
                                    Column(horizontalAlignment = Alignment.Start) {
                                        Text(item.account.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("${formatMoney(item.netBalance)} ${item.account.currency}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0 && selectedIds.isNotEmpty()) {
                            onConfirm(selectedIds.toList(), type, amt, description)
                        }
                    },
                    enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0 && selectedIds.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("حفظ المعاملة للجميع (${selectedIds.size})", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("إلغاء")
                }
            }
        )
    }
}

/**
 * Dialog for Batch Sending Debt/Payment Reminders to Multiple Accounts
 */
@Composable
fun BatchReminderDialog(
    debtors: List<AccountWithBalance>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedIds by remember { mutableStateOf(debtors.map { it.account.id }.toSet()) }
    var customNote by remember { mutableStateOf("يرجى التكرم بتسديد المبلغ المستحق لتطبيق البيان.") }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "إرسال تذكير جماعي",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE0F2FE)
                    ) {
                        Text(
                            text = "المدينون: ${selectedIds.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    Text(
                        text = "رسالة التذكير:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    OutlinedTextField(
                        value = customNote,
                        onValueChange = { customNote = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("قائمة المدينين:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        TextButton(onClick = {
                            selectedIds = if (selectedIds.size == debtors.size) emptySet() else debtors.map { it.account.id }.toSet()
                        }) {
                            Text(if (selectedIds.size == debtors.size) "إلغاء تحديد الكل" else "تحديد الكل")
                        }
                    }

                    if (debtors.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("لا يوجد حسابات عليها ديون حالياً", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                .padding(4.dp)
                        ) {
                            items(debtors.size) { idx ->
                                val item = debtors[idx]
                                val isChecked = selectedIds.contains(item.account.id)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedIds = if (isChecked) selectedIds - item.account.id else selectedIds + item.account.id
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                selectedIds = if (checked) selectedIds + item.account.id else selectedIds - item.account.id
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0284C7))
                                        )
                                        Column(horizontalAlignment = Alignment.Start) {
                                            Text(item.account.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(
                                                text = "مستحق: ${formatMoney(item.netBalance)} ${item.account.currency}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = LanaRed
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val selectedDebtors = debtors.filter { selectedIds.contains(it.account.id) }
                            if (selectedDebtors.isNotEmpty()) {
                                val combinedText = buildString {
                                    appendLine("📋 *كشف تذكيرات الديون - البيان*")
                                    appendLine("-----------------------------")
                                    selectedDebtors.forEach { d ->
                                        appendLine("👤 ${d.account.name}: ${formatMoney(d.netBalance)} ${d.account.currency}")
                                    }
                                    appendLine("-----------------------------")
                                    appendLine(customNote)
                                }
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, combinedText)
                                }
                                context.startActivity(Intent.createChooser(intent, "إرسال التذكيرات عبر"))
                                onDismiss()
                            }
                        },
                        enabled = selectedIds.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مشاركة التذكيرات (${selectedIds.size})", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("إلغاء")
                }
            }
        )
    }
}

/**
 * Empty Accounts View matching Screenshot 2 with exact avatar, button and pointing tooltip
 */
@Composable
fun TawthiqEmptyAccountsView(
    onAddAccountClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 45.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Subtle grey circular user outline icon
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(50.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Title matching Screenshot 2
        Text(
            text = "ابدأ بإضافة أول حساب",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle matching Screenshot 2
        Text(
            text = "أضف زبائنك أو مورديك لتتبع الديون والمدفوعات بسهولة",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(22.dp))

        // Button: "+ إضافة حساب"
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFE8F8F0),
            border = androidx.compose.foundation.BorderStroke(1.dp, TawthiqPrimary),
            modifier = Modifier
                .clickable { onAddAccountClick() }
                .testTag("empty_state_add_account_btn")
        ) {
            Text(
                text = "+ إضافة حساب",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TawthiqPrimary,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Upward pointing triangle pointer
        androidx.compose.foundation.Canvas(modifier = Modifier.size(width = 16.dp, height = 8.dp)) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(size.width / 2f, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(path, color = Color(0xFF0084C7))
        }

        // Blue Speech Bubble / Tooltip matching Screenshot 2
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF0084C7),
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = "من هنا قم بإضافة الحساب المالي الأول مثلاً: الزبون أحمد",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
            )
        }
    }
}

/**
 * Account Card in the Main List (Matching Tawthiq original clean layout with large typography)
 */
@Composable
fun AccountCard(
    item: AccountWithBalance,
    onClick: () -> Unit,
    onOpenDetailClick: () -> Unit,
    onAddTransactionClick: () -> Unit,
    onCallClick: () -> Unit,
    onShareClick: () -> Unit,
    onExcelClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    onQrClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    selectedCurrency: String = "ALL"
) {
    val context = LocalContext.current
    val avatarColor = AvatarColors[item.account.avatarColorIndex % AvatarColors.size]

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable { onClick() }
            .testTag("account_card_${item.account.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Optional Red Credit Limit Exceeded Warning Signal Banner
            if (item.isCreditLimitExceeded) {
                Surface(
                    color = Color(0xFFFEF2F2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditClick?.invoke() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🚨 تحذير: تجاوز الحد الأقصى المستحق (${formatMoney(item.account.creditLimit)} ${item.account.currency})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                        Text(
                            text = "زيادة الحد ✏️",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TawthiqPrimary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Right side (in RTL): User Photo / Avatar
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(avatarColor.copy(alpha = 0.15f))
                        .border(1.5.dp, avatarColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.account.avatarUri.isNotBlank()) {
                        AsyncImage(
                            model = item.account.avatarUri,
                            contentDescription = item.account.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val initial = item.account.name.trim().firstOrNull()?.toString() ?: "ح"
                        Text(
                            text = initial,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = avatarColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Middle (in RTL): Account Name on the right with fixed index number and transaction summary
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Fixed Index Badge (Requirement 1: 1: احمد, 2: محمد)
                        Surface(
                            color = TawthiqPrimary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "#${if (item.account.displayIndex > 0) item.account.displayIndex else item.account.id}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TawthiqPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Account Name
                        Text(
                            text = item.account.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Subtitle: Net balance. Accurately respect selected filter or active currency
                    val effectiveCur = if (selectedCurrency != "ALL") {
                        selectedCurrency
                    } else if (item.currencyBalances[item.account.currency] == 0.0 && item.currencyBalances.any { it.value != 0.0 }) {
                        item.currencyBalances.entries.first { it.value != 0.0 }.key
                    } else {
                        item.account.currency
                    }
                    val effectiveBal = item.currencyBalances[effectiveCur] ?: if (effectiveCur.equals(item.account.currency, ignoreCase = true)) item.netBalance else 0.0

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (effectiveBal > 0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = LanaRed.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "الصافي: لنا ${formatMoney(effectiveBal)} $effectiveCur",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LanaRed,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else if (effectiveBal < 0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = LahoGreen.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "الصافي: له ${formatMoney(-effectiveBal)} $effectiveCur",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LahoGreenText,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "الصافي: متوازن (خالص)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Side-by-side currencies display if other currencies exist
                    val otherCurrencies = item.currencyBalances.filter { !it.key.equals(effectiveCur, ignoreCase = true) && it.value != 0.0 }
                    if (otherCurrencies.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            otherCurrencies.forEach { (cur, bal) ->
                                Surface(
                                    color = if (bal > 0) LanaRedContainer.copy(alpha = 0.5f) else LahoGreenContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "$cur: ${if (bal > 0) "+" else ""}${formatMoney(bal)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (bal > 0) LanaRedText else LahoGreenText,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Left side (in RTL): Action buttons (QR, Edit, and Delete)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (onDeleteClick != null) {
                        Surface(
                            onClick = { onDeleteClick.invoke() },
                            shape = RoundedCornerShape(10.dp),
                            color = LanaRedContainer.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, LanaRed.copy(alpha = 0.25f)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "حذف الحساب نهائياً",
                                    tint = LanaRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (onQrClick != null) {
                        Surface(
                            onClick = { onQrClick.invoke() },
                            shape = RoundedCornerShape(10.dp),
                            color = TawthiqPrimary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, TawthiqPrimary.copy(alpha = 0.25f)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = "عرض باركود الحساب للزبون",
                                    tint = TawthiqPrimary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = { onEditClick?.invoke() },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "تعديل إعدادات الحساب وسقف الدين",
                                tint = TawthiqPrimary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }

            // Quick Actions Footer Bar on the Card
            androidx.compose.material3.HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                thickness = 0.8.dp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick Call
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onCallClick() }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "اتصال",
                        tint = TawthiqPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("اتصال", fontSize = 11.sp, color = TawthiqPrimary)
                }

                // Quick WhatsApp
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onShareClick() }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "واتساب",
                        tint = Color(0xFF25D366),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("واتساب", fontSize = 11.sp, color = Color(0xFF16A34A))
                }

                // Quick Account Details Button (Requirement 7: زر صغير لدخول صفحة الحساب)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenDetailClick() }
                        .background(TawthiqPrimary.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "كشف الحساب",
                        tint = TawthiqPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("كشف الحساب 👁️", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TawthiqPrimary)
                }

                // Quick Add Transaction
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onAddTransactionClick() }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "معاملة",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("فاتورة جديدة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/**
 * Helper to launch WhatsApp with preformatted Arabic invoice text and complete account inventory (جرد كامل) directly after saving transaction
 */
fun launchWhatsAppForTransaction(
    context: android.content.Context,
    accountName: String,
    phone: String,
    type: String, // "LANA" or "LAHO"
    amount: Double,
    currency: String,
    description: String,
    date: Long,
    netBalance: Double,
    oldBalance: Double = 0.0,
    receiptNumber: String = "",
    transactions: List<com.example.data.model.TransactionEntity> = emptyList()
) {
    val dateStr = java.text.SimpleDateFormat("yyyy/MM/dd - hh:mm a", java.util.Locale("ar")).format(java.util.Date(date))
    val isLana = type == "LANA"
    val typeTitle = if (isLana) "فاتورة مشتريات / دين لنا (+)" else "سداد دفعة نقدية / دفعة له (-)"
    val balanceText = if (netBalance > 0) "المطلوب سداده (لنا): ${formatMoney(netBalance)} $currency 🔴" else if (netBalance < 0) "رصيد دائن لكم (له): ${formatMoney(-netBalance)} $currency 🟢" else "الحساب خالص ومطابق تماماً (0 $currency) ⚪"
    val oldBalText = if (oldBalance > 0) "لنا ${formatMoney(oldBalance)} $currency" else if (oldBalance < 0) "له ${formatMoney(-oldBalance)} $currency" else "0 خالص"

    val message = buildString {
        appendLine("🧾 *إشعار فاتورة ومعاملة جديدة - تطبيق البيان*")
        appendLine("👤 *العميل / الحساب:* $accountName")
        appendLine("📅 *التاريخ:* $dateStr")
        if (receiptNumber.isNotBlank()) {
            appendLine("🔢 *رقم الفاتورة/السند:* $receiptNumber")
        }
        appendLine("━━━━━━━━━━━━━━━━━━━━━")
        appendLine("📄 *تفاصيل الحركة الجديدة:*")
        appendLine("• نوع العملية: $typeTitle")
        appendLine("• المبلغ: ${formatMoney(amount)} $currency")
        appendLine("• البيان: ${if (description.isBlank()) "بدون بيان" else description.trim()}")
        appendLine("━━━━━━━━━━━━━━━━━━━━━")
        appendLine("📊 *الموقف المالي والرصيد الصافي:*")
        appendLine("• الرصيد السابق: $oldBalText")
        appendLine("• الحركة الحالية: ${if (isLana) "+" else "-"}${formatMoney(amount)} $currency")
        appendLine("👉 *الرصيد الصافي الجديد:* $balanceText")

        if (transactions.isNotEmpty()) {
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📋 *جرد العمليات والحركات بالكامل:*")
            val dateOnlySdf = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale("ar"))
            val sortedTx = transactions.sortedBy { it.date }
            sortedTx.forEachIndexed { index, tx ->
                val typeLabel = if (tx.type == "LANA") "لنا (+)" else "له (-)"
                val dateOnly = dateOnlySdf.format(java.util.Date(tx.date))
                val desc = if (tx.description.isNotBlank()) " | ${tx.description.trim()}" else ""
                val rec = if (tx.receiptNumber.isNotBlank()) " [سند #${tx.receiptNumber.trim()}]" else ""
                appendLine("${index + 1}. $dateOnly : ${formatMoney(tx.amount)} ${tx.currency} ($typeLabel)$desc$rec")
            }
        }
        appendLine("━━━━━━━━━━━━━━━━━━━━━")
        appendLine("✨ تم التوثيق وتحديث الرصيد آلياً عبر تطبيق البيان.")
    }

    val cleanPhone = phone.replace(Regex("[^0-9+]"), "").replace("+", "")
    val encodedMsg = java.net.URLEncoder.encode(message, "UTF-8")
    val url = if (cleanPhone.isNotBlank()) {
        "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg"
    } else {
        "https://api.whatsapp.com/send?text=$encodedMsg"
    }

    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "تم حفظ الفاتورة بنجاح. تعذر فتح تطبيق واتساب تلقائياً.", android.widget.Toast.LENGTH_SHORT).show()
    }
}

/**
 * Transaction Bubble (Matching Tawthiq Screen 2 - حركات بتصميم بسيط وسهل)
 */
@Composable
fun TransactionBubble(
    transaction: TransactionEntity,
    onDelete: (() -> Unit)? = null
) {
    val isLana = transaction.type == "LANA" // Red / debt owed to us
    var showMenu by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0F172A)

    val bgColor = if (isDark) {
        if (isLana) Color(0xFF3B1219) else Color(0xFF063726)
    } else {
        if (isLana) Color(0xFFFFF1F2) else Color(0xFFECFDF5)
    }

    val borderColor = if (isDark) {
        if (isLana) Color(0xFF7F1D1D) else Color(0xFF065F46)
    } else {
        if (isLana) Color(0xFFFECDD3) else Color(0xFFA7F3D0)
    }

    val badgeBg = if (isDark) {
        if (isLana) Color(0xFF7F1D1D) else Color(0xFF065F46)
    } else {
        if (isLana) LanaRedContainer else LahoGreenContainer
    }

    val textColor = if (isDark) {
        if (isLana) Color(0xFFFCA5A5) else Color(0xFF6EE7B7)
    } else {
        if (isLana) LanaRed else LahoGreen
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isLana) 16.dp else 48.dp,
                end = if (isLana) 48.dp else 16.dp,
                top = 4.dp,
                bottom = 4.dp
            ),
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = if (isLana) 4.dp else 16.dp,
            bottomEnd = if (isLana) 16.dp else 4.dp
        ),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Header badge (e.g. "لنا 300 USD" or "له 50 USD")
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = if (isLana) "لنا ${formatMoney(transaction.amount)} ${transaction.currency}"
                        else "له ${formatMoney(transaction.amount)} ${transaction.currency}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (onDelete != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = { onDelete() },
                            shape = RoundedCornerShape(8.dp),
                            color = LanaRed.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LanaRed.copy(alpha = 0.3f)),
                            modifier = Modifier.heightIn(min = 32.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "حذف المعاملة",
                                    tint = LanaRed,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "حذف",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LanaRed
                                )
                            }
                        }

                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "خيارات",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF1E293B) else Color.White
                            ) {
                                DropdownMenuItem(
                                    text = { Text("حذف المعاملة", color = LanaRed, fontWeight = FontWeight.Bold) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = LanaRed
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onDelete()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (transaction.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = transaction.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp
                )
            }

            if (transaction.receiptNumber.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "رقم الفاتورة/السند: ${transaction.receiptNumber}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Footer time & status checkmark
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatTransactionDate(transaction.date)}  •  ${formatTransactionTime(transaction.date)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (transaction.dueDate != null) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "استحقاق",
                            tint = TawthiqAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                    Text(
                        text = "مؤكد",
                        fontSize = 11.sp,
                        color = textColor.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "مؤكد",
                        tint = textColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Add / Edit Account Dialog
 */
@Composable
fun AddAccountDialog(
    categories: List<String>,
    defaultCurrency: String = "USD",
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, category: String, currency: String, initialBalance: Double, initialType: String, notes: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull { it != "الكل" } ?: "عميل") }
    var currency by remember(defaultCurrency) { mutableStateOf(defaultCurrency.ifBlank { "USD" }) }
    var initialBalance by remember { mutableStateOf("") }
    var initialType by remember { mutableStateOf("LANA") } // LANA or LAHO
    var notes by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    val currencies = listOf("USD", "SYP", "SAR", "EGP", "AED", "KWD", "EUR", "YER", "QAR", "OMR", "BHD", "JOD", "IQD", "TRY", "GBP")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "إضافة حساب جديد",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TawthiqPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        hasError = false
                    },
                    label = { Text("اسم العميل / التاجر *") },
                    isError = hasError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_name_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف / الواتساب") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Category Selector Chips
                Text("التصنيف:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.filter { it != "الكل" }.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // Currency Selector
                Text("العملة:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    currencies.forEach { cur ->
                        FilterChip(
                            selected = currency == cur,
                            onClick = { currency = cur },
                            label = { Text(cur, fontSize = 12.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Initial Balance
                OutlinedTextField(
                    value = initialBalance,
                    onValueChange = { initialBalance = it },
                    label = { Text("رصيد افتتاحي سابق (اختياري)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                if (initialBalance.toDoubleOrNull() ?: 0.0 > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { initialType = "LANA" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (initialType == "LANA") LanaRed else LanaRedContainer
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "لنا (دين عليه)",
                                color = if (initialType == "LANA") Color.White else LanaRedText,
                                fontSize = 12.sp
                            )
                        }
                        Button(
                            onClick = { initialType = "LAHO" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (initialType == "LAHO") LahoGreen else LahoGreenContainer
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "له (دين له)",
                                color = if (initialType == "LAHO") Color.White else LahoGreenText,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إضافية") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        hasError = true
                    } else {
                        val bal = initialBalance.toDoubleOrNull() ?: 0.0
                        onSave(name, phone, selectedCategory, currency, bal, initialType, notes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_account_button")
            ) {
                Text("حفظ الحساب", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

/**
 * Add Transaction Bottom Sheet matching screenshot 3.jpeg ("معاملة جديدة")
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionBottomSheet(
    account: AccountEntity,
    defaultType: String = "LANA", // "LANA" (إرسال) or "LAHO" (استلام)
    onDismiss: () -> Unit,
    onSave: (type: String, amount: Double, currency: String, description: String, dueDate: Long?, receipt: String) -> Unit,
    onSaveWithDate: ((type: String, amount: Double, currency: String, description: String, date: Long, dueDate: Long?, receipt: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isTxDark = androidx.compose.foundation.isSystemInDarkTheme()
    val txBoxBg = if (isTxDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val txBoxBorder = if (isTxDark) Color(0xFF475569) else Color(0xFFCBD5E1)
    val txTextColor = if (isTxDark) Color.White else Color(0xFF0F172A)
    val txPlaceholderColor = if (isTxDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    var type by remember { mutableStateOf(defaultType) } // "LANA" (إرسال) or "LAHO" (استلام)
    var amountText by remember { mutableStateOf("") }
    var selectedCurrency by remember(account.currency) { mutableStateOf(account.currency) }
    var showCurrencyDropdown by remember { mutableStateOf(false) }

    val availableCurrencies = remember {
        listOf(
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
    }
    var description by remember { mutableStateOf("") }
    var receiptUri by remember { mutableStateOf("") }
    var hasDueDate by remember { mutableStateOf(false) }
    var dueDaysOffset by remember { mutableStateOf(7) }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var hasError by remember { mutableStateOf(false) }
    var quickNotes by remember { mutableStateOf(mutableListOf<String>()) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var newNoteText by remember { mutableStateOf("") }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            receiptUri = uri.toString()
            Toast.makeText(context, "تم إرفاق صورة السند/الفاتورة بنجاح", Toast.LENGTH_SHORT).show()
        }
    }

    val dateFormatter = remember { java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.ENGLISH) }
    val formattedDate = remember(selectedDate) { dateFormatter.format(java.util.Date(selectedDate)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF1E293B) else Color.White
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
            // Header title centered matching 3.jpeg
            Text(
                text = "معاملة جديدة",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // Two large toggle pills matching 3.jpeg ("إرسال ↗" and "استلام ↙")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // إرسال ↗ (LANA / لنا)
                val isSend = type == "LANA"
                Surface(
                    onClick = { type = "LANA" },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = if (isSend) Color(0xFFFFE2E2) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isSend) Color(0xFFFCA5A5) else Color(0xFFE2E8F0))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "إرسال ↗",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isSend) Color(0xFFDC2626) else Color(0xFF64748B)
                        )
                    }
                }

                // استلام ↙ (LAHO / له)
                val isReceive = type == "LAHO"
                Surface(
                    onClick = { type = "LAHO" },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = if (isReceive) Color(0xFFDCFCE7) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isReceive) Color(0xFF86EFAC) else Color(0xFFE2E8F0))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "استلام ↙",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isReceive) Color(0xFF16A34A) else Color(0xFF64748B)
                        )
                    }
                }
            }

            // Label: "المبلغ *"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "المبلغ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = " *",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444)
                )
            }

            // Big spacious Amount Box with Currency inside
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(txBoxBg)
                    .border(
                        1.dp,
                        if (hasError) Color(0xFFEF4444) else txBoxBorder,
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BasicTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it
                            hasError = false
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = txTextColor,
                            textAlign = TextAlign.Start
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("transaction_amount_input"),
                        decorationBox = { innerTextField ->
                            if (amountText.isEmpty()) {
                                Text(
                                    text = "0.00",
                                    fontSize = 18.sp,
                                    color = txPlaceholderColor
                                )
                            }
                            innerTextField()
                        }
                    )

                    // Fixed Single Currency Badge (Read-only from Settings)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TawthiqPrimary.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, TawthiqPrimary.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedCurrency,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TawthiqPrimary
                            )
                        }
                    }
                }
            }

            // Label: "البيان *" and "أسطر مرقّمة ☰"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Right: "البيان *"
                Row {
                    Text(
                        text = "البيان",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = " *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                }

                // Left: Numbered lines toggle button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val lines = description.lines().toMutableList()
                            if (lines.isEmpty() || description.isBlank()) {
                                description = "1. "
                            } else {
                                val numbered = lines.mapIndexed { idx, line ->
                                    val cleaned = line.replace(Regex("^\\d+\\.\\s*"), "")
                                    "${idx + 1}. $cleaned"
                                }.joinToString("\n")
                                description = numbered
                            }
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatListNumbered,
                        contentDescription = "أسطر مرقمة",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "أسطر مرقّمة",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF475569)
                    )
                }
            }

            // Description Input Box with Camera button on the left
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(txBoxBg)
                    .border(1.dp, txBoxBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    BasicTextField(
                        value = description,
                        onValueChange = {
                            if (it.length <= 500) description = it
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = txTextColor,
                            textAlign = TextAlign.Start
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 8.dp),
                        decorationBox = { innerTextField ->
                            if (description.isEmpty()) {
                                Text(
                                    text = "البيان",
                                    fontSize = 14.sp,
                                    color = txPlaceholderColor
                                )
                            }
                            innerTextField()
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Camera / Attachment button on the left
                    IconButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "إرفاق صورة",
                            tint = if (receiptUri.isNotBlank()) Color(0xFF10B981) else Color(0xFF64748B),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Character counter "0/500" at the bottom-left
                Text(
                    text = "${description.length}/500",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }

            // Quick Notes Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Right: "لا يوجد ملاحظات جاهزة" or horizontal list of notes
                if (quickNotes.isEmpty()) {
                    Text(
                        text = "لا يوجد ملاحظات جاهزة",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        quickNotes.forEach { note ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clickable { description = note }
                            ) {
                                Text(
                                    text = note,
                                    fontSize = 11.sp,
                                    color = Color(0xFF334155),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Left: "+ إضافة ملاحظة" (green clickable text)
                Text(
                    text = "+ إضافة ملاحظة",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981),
                    modifier = Modifier
                        .clickable { showAddNoteDialog = true }
                        .padding(vertical = 4.dp)
                )
            }

            // Due Date Row with Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Label on right
                Text(
                    text = "الإستحقاق",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Switch on left
                androidx.compose.material3.Switch(
                    checked = hasDueDate,
                    onCheckedChange = { hasDueDate = it }
                )
            }

            if (hasDueDate) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(3 to "3 أيام", 7 to "أسبوع", 15 to "15 يوم", 30 to "شهر").forEach { (days, label) ->
                        FilterChip(
                            selected = dueDaysOffset == days,
                            onClick = { dueDaysOffset = days },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Transaction Date Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Right: "تاريخ المعاملة DD/MM/YYYY"
                Text(
                    text = "تاريخ المعاملة  $formattedDate",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Left: "تعديل؟" clickable in green
                Text(
                    text = "تعديل؟",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981),
                    modifier = Modifier
                        .clickable {
                            val cal = java.util.Calendar.getInstance().apply { timeInMillis = selectedDate }
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val newCal = java.util.Calendar.getInstance()
                                    newCal.set(year, month, dayOfMonth)
                                    selectedDate = newCal.timeInMillis
                                },
                                cal.get(java.util.Calendar.YEAR),
                                cal.get(java.util.Calendar.MONTH),
                                cal.get(java.util.Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Save Transaction Button (matching 3.jpeg: Big Green Button "إضافة المعاملة")
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        hasError = true
                    } else {
                        val dueTimestamp = if (hasDueDate) {
                            System.currentTimeMillis() + dueDaysOffset * 24 * 3600 * 1000L
                        } else null

                        val finalDesc = description.ifBlank { if (type == "LANA") "إرسال (لنا)" else "استلام (له)" }

                        if (onSaveWithDate != null) {
                            onSaveWithDate(
                                type,
                                amount,
                                selectedCurrency,
                                finalDesc,
                                selectedDate,
                                dueTimestamp,
                                receiptUri
                            )
                        } else {
                            onSave(
                                type,
                                amount,
                                selectedCurrency,
                                finalDesc,
                                dueTimestamp,
                                receiptUri
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_transaction_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00A86B)
                )
            ) {
                Text(
                    text = "إضافة المعاملة",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        }
    }

    // Quick Add Note Dialog
    if (showAddNoteDialog) {
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("إضافة ملاحظة سريعة", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newNoteText,
                        onValueChange = { newNoteText = it },
                        label = { Text("نص الملاحظة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("اقتراحات سريعة:", fontSize = 12.sp, color = Color(0xFF64748B))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("دفعة نقدية", "بضاعة جديدة", "سداد حوالة", "فاتورة مبيعات", "سلفة").forEach { preset ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.clickable { newNoteText = preset }
                            ) {
                                Text(
                                    preset,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newNoteText.isNotBlank()) {
                            quickNotes.add(newNoteText.trim())
                            description = newNoteText.trim()
                            newNoteText = ""
                            showAddNoteDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A86B))
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
