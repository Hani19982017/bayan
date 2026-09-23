package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.ui.theme.LahoGreen
import com.example.ui.theme.LanaRed
import com.example.ui.theme.LanaRedContainer
import com.example.ui.theme.TawthiqPrimary
import com.example.ui.viewmodel.TawthiqViewModel
import com.example.util.TawthiqNotificationManager

enum class NotificationTab {
    OTHER,
    FINANCIAL
}

data class OtherNotification(
    val id: String,
    val title: String,
    val body: String,
    val dateTime: String,
    var isUnread: Boolean = true
)

/**
 * Screen matching Screenshots 1 & 2: "الإشعارات"
 */
@Composable
fun NotificationsScreen(
    viewModel: TawthiqViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()
    val dueItems by viewModel.dueItems.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(NotificationTab.OTHER) }
    var welcomeNotificationRead by remember { mutableStateOf(false) }

    val effectiveUnread = if (welcomeNotificationRead) 0 else unreadCount

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Top Bar matching Screenshot 1 & 2:
            // Back Arrow > | "الإشعارات" | "تحديد الكل • 1"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Right side in RTL (Back button and Title)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("notifications_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "الإشعارات",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Left side in RTL (Pill "تحديد الكل • X")
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = TawthiqPrimary.copy(alpha = 0.12f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            viewModel.markAllNotificationsRead()
                            welcomeNotificationRead = true
                        }
                        .testTag("mark_all_read_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = TawthiqPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (effectiveUnread > 0) "تحديد الكل • $effectiveUnread" else "تحديد الكل",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TawthiqPrimary
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs Bar: "مالية" and "أخرى" matching Screenshots
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                // Tab "أخرى"
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = NotificationTab.OTHER }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "أُخرى",
                        fontSize = 15.sp,
                        fontWeight = if (selectedTab == NotificationTab.OTHER) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == NotificationTab.OTHER) TawthiqPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.35f)
                            .height(3.dp)
                            .background(
                                if (selectedTab == NotificationTab.OTHER) TawthiqPrimary else Color.Transparent,
                                RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                            )
                    )
                }

                // Tab "مالية"
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = NotificationTab.FINANCIAL }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "مالية",
                        fontSize = 15.sp,
                        fontWeight = if (selectedTab == NotificationTab.FINANCIAL) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == NotificationTab.FINANCIAL) TawthiqPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.35f)
                            .height(3.dp)
                            .background(
                                if (selectedTab == NotificationTab.FINANCIAL) TawthiqPrimary else Color.Transparent,
                                RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                            )
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

            // Content according to selected Tab
            when (selectedTab) {
                NotificationTab.OTHER -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Card matching conditional merchant status (New vs Returning)
                        item {
                            val isNewMerchant = viewModel.isNewAccount(userEmail)
                            val cardTitle = if (isNewMerchant) "📢 مرحباً بك في البيان!" else "👋 أهلاً بك مجدداً في البيان!"
                            val cardBody = if (isNewMerchant) {
                                "بدأت الآن فترة تجربتك المجانية لمدة 4 أيام ✨\nاستمتع بإدارة حساباتك، تسجيل معاملاتك، ومتابعة التقارير"
                            } else {
                                "سعداء برؤيتك مجدداً، أهلاً بك في تطبيق البيان ✨\nنتمنى لك يوماً سعيداً وإدارة موفقة لحساباتك ومعاملاتك التجارية"
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                    ) {
                                    // Header row with Icon, Title and Unread Dot
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Title
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = cardTitle,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        // Left side in RTL: Bell Badge & Unread Dot
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // Soft circular green bell badge
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(TawthiqPrimary.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.NotificationsNone,
                                                    contentDescription = null,
                                                    tint = TawthiqPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            if (!welcomeNotificationRead && effectiveUnread > 0) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                // Unread Green Dot
                                                Box(
                                                    modifier = Modifier
                                                        .size(9.dp)
                                                        .clip(CircleShape)
                                                        .background(TawthiqPrimary)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Body description:
                                    Text(
                                        text = cardBody,
                                        fontSize = 13.5.sp,
                                        lineHeight = 22.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Normal
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Date & Time
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Send push to mobile status bar button
                                        Surface(
                                            onClick = {
                                                if (isNewMerchant) {
                                                    TawthiqNotificationManager.sendWelcomePushNotification(
                                                        context,
                                                        force = true,
                                                        merchantEmail = userEmail
                                                    )
                                                } else {
                                                    TawthiqNotificationManager.sendReturningWelcomePushNotification(
                                                        context,
                                                        merchantEmail = userEmail
                                                    )
                                                }
                                                Toast.makeText(context, "تم إرسال إشعار الترحيب لشريط الهاتف 📲", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = TawthiqPrimary.copy(alpha = 0.12f),
                                            modifier = Modifier.testTag("send_welcome_push_btn")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.NotificationsActive,
                                                    contentDescription = null,
                                                    tint = TawthiqPrimary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "إرسال للهاتف 📲",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TawthiqPrimary
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "PM 2:02 20/09/2026",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Push Notification & Reminder Control Panel
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Alarm,
                                            contentDescription = null,
                                            tint = TawthiqPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "نظام التذكيرات والإشعارات المباشرة",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "تصلك تنبيهات فورية على شريط الهاتف بمواعيد استحقاق ديون العملاء والفترة المجانية.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                TawthiqNotificationManager.sendTestPushNotification(context)
                                                Toast.makeText(context, "تم إرسال إشعار تجريبي للجهاز ✓", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("اختبار إشعار عام 🔔", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }

                                        Button(
                                            onClick = {
                                                TawthiqNotificationManager.sendDueReminderPushNotification(
                                                    context = context,
                                                    accountName = "عميل تجريبي",
                                                    amountText = "500 $",
                                                    isLana = true,
                                                    dueDate = "اليوم"
                                                )
                                                Toast.makeText(context, "تم إرسال إشعار تذكير سداد للهاتف ⏰", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary)
                                        ) {
                                            Text("تذكير سداد ⏰", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                NotificationTab.FINANCIAL -> {
                    // If no financial due items, show exact Empty State matching Screenshot 2.jpeg!
                    if (dueItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Subtle outline container with bell icon
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsNone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    text = "لا توجد تنبيهات",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "ستظهر هنا الإشعارات الجديدة عند وصولها",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // Display actual financial reminders & due debts
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(dueItems) { item ->
                                val isOverdue = item.daysRemaining < 0
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isOverdue) LanaRed.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "استحقاق سداد: ${item.accountName}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (isOverdue) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = LanaRed.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = "متأخر",
                                                            fontSize = 10.sp,
                                                            color = LanaRed,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = item.transaction.description.ifBlank { "سداد دين مستحق" },
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "${item.transaction.amount} ${item.transaction.currency}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (isOverdue) LanaRed else TawthiqPrimary
                                            )

                                            Surface(
                                                onClick = {
                                                    TawthiqNotificationManager.sendDueReminderPushNotification(
                                                        context = context,
                                                        accountName = item.accountName,
                                                        amountText = "${item.transaction.amount} ${item.transaction.currency}",
                                                        isLana = item.transaction.type == "LANA",
                                                        dueDateMillis = item.transaction.dueDate
                                                    )
                                                    Toast.makeText(context, "تم إرسال تذكير السداد لشريط الهاتف ⏰", Toast.LENGTH_SHORT).show()
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isOverdue) LanaRedContainer.copy(alpha = 0.5f) else TawthiqPrimary.copy(alpha = 0.12f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Default.NotificationsActive,
                                                        contentDescription = null,
                                                        tint = if (isOverdue) LanaRed else TawthiqPrimary,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = "إشعار 📲",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isOverdue) LanaRed else TawthiqPrimary
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
}
