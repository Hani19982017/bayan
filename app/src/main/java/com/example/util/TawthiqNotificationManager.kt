package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TawthiqNotificationManager {

    const val CHANNEL_WELCOME = "tawthiq_welcome_channel"
    const val CHANNEL_REMINDERS = "tawthiq_reminders_channel"
    const val CHANNEL_GENERAL = "tawthiq_general_channel"
    const val CHANNEL_LIVE_TRANSACTIONS = "tawthiq_live_transactions_channel"
    const val CHANNEL_BACKGROUND_SERVICE = "tawthiq_bg_service_channel"

    private const val PREFS_NAME = "tawthiq_notification_prefs"
    private const val KEY_WELCOME_SENT_PREFIX = "welcome_push_sent_"
    private const val KEY_NOTIFICATIONS_ENABLED = "push_notifications_enabled"

    /**
     * Initializes all notification channels for Android 8.0 (API 26) and above.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Welcome & General Channel (High Importance with Sound & Vibration)
            val welcomeChannel = NotificationChannel(
                CHANNEL_WELCOME,
                "إشعارات الترحيب والعامة - توثيق",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات بدء الفترة المجانية وتحديثات الحساب الترحيبية"
                enableLights(true)
                lightColor = Color.parseColor("#0D9488")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setShowBadge(true)
            }

            // 2. Financial & Debt Reminders Channel (Max/High Importance)
            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "تذكيرات السداد والمواعيد المالية",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات استحقاق مواعيد الديون وتذكيرات سداد الحسابات"
                enableLights(true)
                lightColor = Color.parseColor("#E11D48")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                setShowBadge(true)
            }

            // 3. General Updates Channel
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "تنبيهات النظام والنسخ الاحتياطي",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "إشعارات العمليات وإجراءات النسخ الاحتياطي السحابي"
                setShowBadge(true)
            }

            // 4. Live Customer Transaction Sync Channel (Max Importance with Sound, Lights & Vibration)
            val liveTxChannel = NotificationChannel(
                CHANNEL_LIVE_TRANSACTIONS,
                "إشعارات الفواتير والمعاملات المباشرة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات فورية لحظية عند تسجيل التاجر لأي فاتورة أو دفعة جديدة"
                enableLights(true)
                lightColor = Color.parseColor("#059669")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                setShowBadge(true)
            }

            // 5. Persistent Background Service Channel (Low importance, silent)
            val bgServiceChannel = NotificationChannel(
                CHANNEL_BACKGROUND_SERVICE,
                "خدمة المزامنة الحية في الخلفية",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "الحفاظ على اتصال الإشعارات الفورية حتى بعد إغلاق التطبيق"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannels(listOf(welcomeChannel, remindersChannel, generalChannel, liveTxChannel, bgServiceChannel))
        }
    }

    /**
     * Checks if notifications are permitted and enabled.
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val appPrefEnabled = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        if (!appPrefEnabled) return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    /**
     * Sends the Returning Merchant Welcome Push Notification for existing merchants logging in:
     * Title: 👋 أهلاً بك مجدداً في توثيق!
     * Body: سعداء برؤيتك مجدداً يا [اسم التاجر] ✨
     */
    fun sendReturningWelcomePushNotification(context: Context, merchantName: String = "", merchantEmail: String = "") {
        createNotificationChannels(context)

        val displayName = merchantName.ifBlank { merchantEmail.substringBefore("@") }
        val title = "👋 أهلاً بك مجدداً في البيان!"
        val shortBody = "سعداء برؤيتك مجدداً يا $displayName ✨"
        val fullBody = "سعداء برؤيتك مجدداً يا $displayName ✨\nنتمنى لك يوماً سعيداً وإدارة موفقة لحساباتك ومعاملاتك التجارية."

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "notifications")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_WELCOME)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(shortBody)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(fullBody)
                    .setBigContentTitle(title)
                    .setSummaryText("ترحيب بالتاجر")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setColor(Color.parseColor("#0D9488"))

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            ) {
                notificationManager.notify(1002, builder.build())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Sends the official Welcome Push Notification requested for new merchants:
     * Title: 📢 مرحباً بك في توثيق!
     * Body:
     * بدأت الآن فترة تجربتك المجانية لمدة 4 أيام ✨
     * استمتع بإدارة حساباتك، تسجيل معاملاتك، ومتابعة التقارير
     */
    fun sendWelcomePushNotification(context: Context, force: Boolean = false, merchantEmail: String = "") {
        createNotificationChannels(context)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = KEY_WELCOME_SENT_PREFIX + (merchantEmail.ifBlank { "default_merchant" })
        if (!force && prefs.getBoolean(key, false)) {
            // Already sent for this merchant unless forced
            return
        }

        val title = "📢 مرحباً بك في البيان!"
        val shortBody = "بدأت الآن فترة تجربتك المجانية لمدة 4 أيام ✨"
        val fullBody = "بدأت الآن فترة تجربتك المجانية لمدة 4 أيام ✨\nاستمتع بإدارة حساباتك، تسجيل معاملاتك، ومتابعة التقارير"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "notifications")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_WELCOME)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(shortBody)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(fullBody)
                    .setBigContentTitle(title)
                    .setSummaryText("فترة تجريبية مجانية 4 أيام")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setColor(Color.parseColor("#0D9488"))

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            ) {
                notificationManager.notify(1001, builder.build())
                prefs.edit().putBoolean(key, true).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Sends a reminder push notification for due debt or scheduled collection.
     */
    fun sendDueReminderPushNotification(
        context: Context,
        accountName: String,
        amountText: String,
        isLana: Boolean,
        dueDate: String = "",
        notificationId: Int = (System.currentTimeMillis() % 100000).toInt()
    ) {
        createNotificationChannels(context)

        val typeText = if (isLana) "لنا (مستحق القبض)" else "علينا (مستحق الدفع)"
        val title = "⏰ تذكير موعد سداد: $accountName"
        val body = "يحين موعد استحقاق مبلغ $amountText ($typeText) لحساب $accountName" +
                if (dueDate.isNotBlank()) " في تاريخ $dueDate." else " اليوم."

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "accounts")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body).setBigContentTitle(title))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setColor(if (isLana) Color.parseColor("#E11D48") else Color.parseColor("#0D9488"))

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            ) {
                notificationManager.notify(notificationId, builder.build())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendDueReminderPushNotification(
        context: Context,
        accountName: String,
        amountText: String,
        isLana: Boolean,
        dueDateMillis: Long?,
        notificationId: Int = (System.currentTimeMillis() % 100000).toInt()
    ) {
        val formattedDate = if (dueDateMillis != null && dueDateMillis > 0) {
            val sdf = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale("ar"))
            sdf.format(java.util.Date(dueDateMillis))
        } else ""
        sendDueReminderPushNotification(
            context = context,
            accountName = accountName,
            amountText = amountText,
            isLana = isLana,
            dueDate = formattedDate,
            notificationId = notificationId
        )
    }

    /**
     * Sends a generic test push notification so the user can verify on their device.
     */
    fun sendTestPushNotification(context: Context) {
        createNotificationChannels(context)

        val title = "🔔 إشعار تجريبي من البيان"
        val body = "نظام الإشعارات والتذكيرات يعمل بنجاح على جهازك! ستصلك تنبيهات مواعيد السداد والفترة المجانية."

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_GENERAL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(Color.parseColor("#0D9488"))

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            ) {
                notificationManager.notify(1002, builder.build())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Sends Instant Live Push Notification when merchant adds a new transaction for customer.
     */
     fun sendLiveTransactionPushNotification(
         context: Context,
         storeName: String,
         accountName: String,
         amount: Double,
         currency: String,
         type: String, // "LANA" or "LAHO"
         description: String,
         accountId: Long = 0L
     ) {
         createNotificationChannels(context)

         val formattedAmount = try {
             val df = java.text.DecimalFormat("#,##0.##")
             df.format(amount)
         } catch (_: Exception) { amount.toString() }

         val typeLabel = if (type == "LANA") "فاتورة جديدة عليك (لنا)" else "دفعة مسجلة لك (له)"
         val effectiveStore = storeName.ifBlank { "التاجر" }
         val title = "🔔 $effectiveStore: $typeLabel"
         val descText = if (description.isNotBlank()) " ($description)" else ""
         val shortBody = "تم تسجيل مبلغ $formattedAmount $currency$descText لحساب $accountName"
         val fullBody = "قام متجر $effectiveStore بتسجيل معاملة جديدة:\n• المبلغ: $formattedAmount $currency\n• النوع: $typeLabel\n• البيان: ${description.ifBlank { "معاملة جديدة" }}\n\nتم تحديث كشف حسابك تلقائياً وبشكل مباشر ✓"

         val intent = Intent(context, MainActivity::class.java).apply {
             flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
             putExtra("navigate_to", "account_detail")
             putExtra("account_id", accountId)
         }

         val notifId = (System.currentTimeMillis() % 100000).toInt() + 2000
         val pendingIntent = PendingIntent.getActivity(
             context,
             notifId,
             intent,
             PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
         )

         val builder = NotificationCompat.Builder(context, CHANNEL_LIVE_TRANSACTIONS)
             .setSmallIcon(R.mipmap.ic_launcher)
             .setContentTitle(title)
             .setContentText(shortBody)
             .setStyle(
                 NotificationCompat.BigTextStyle()
                     .bigText(fullBody)
                     .setBigContentTitle(title)
                     .setSummaryText(effectiveStore)
             )
             .setPriority(NotificationCompat.PRIORITY_MAX)
             .setAutoCancel(true)
             .setContentIntent(pendingIntent)
             .setColor(Color.parseColor("#059669"))
             .setDefaults(NotificationCompat.DEFAULT_ALL)

         try {
             val notificationManager = NotificationManagerCompat.from(context)
             if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                 ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
             ) {
                 notificationManager.notify(notifId, builder.build())
             }
         } catch (e: Exception) {
             e.printStackTrace()
         }
     }

    /**
     * Schedules a future reminder using AlarmManager.
     */
    fun scheduleReminder(
        context: Context,
        triggerAtMillis: Long,
        title: String,
        body: String,
        requestCode: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("body", body)
            putExtra("id", requestCode)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    /**
     * Sends immediate high-priority push notification to Admin when a user submits a manual payment
     */
    fun sendAdminPaymentNotification(
        context: Context,
        userEmail: String,
        planName: String,
        transferNumber: String
    ) {
        if (!areNotificationsEnabled(context)) return

        val title = "🔔 دفعة اشتراك جديدة واردة ($planName)"
        val content = "قام المستخدم $userEmail بإرسال إشعار تحويل (سند: ${transferNumber.ifBlank { "بدون رقم" }}). يرجى مراجعة وتفعيل الحساب."

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (System.currentTimeMillis() % 10000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setColor(Color.parseColor("#4F46E5"))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(context).notify(9911, builder.build())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Sends push notification to user when admin approves their subscription
     */
    fun sendUserSubscriptionApprovedNotification(
        context: Context,
        planName: String,
        expiryDateFormatted: String
    ) {
        if (!areNotificationsEnabled(context)) return

        val title = "🎉 تم تفعيل اشتراكك بنجاح ($planName)"
        val content = "تم تأكيد الدفعة وتفعيل اشتراكك الكامل في باقة $planName بنجاح. الصلاحية حتى: $expiryDateFormatted."

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (System.currentTimeMillis() % 10000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_WELCOME)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(Color.parseColor("#10B981"))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(context).notify(9912, builder.build())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Sends broadcast notification from admin to users
     */
    fun sendAdminBroadcastNotification(
        context: Context,
        title: String,
        message: String
    ) {
        if (!areNotificationsEnabled(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (System.currentTimeMillis() % 10000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_GENERAL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("📢 $title")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(Color.parseColor("#0D9488"))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(context).notify(9913, builder.build())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Sends direct personal message notification from admin to this specific user
     */
    fun sendAdminDirectNotification(
        context: Context,
        title: String,
        message: String
    ) {
        if (!areNotificationsEnabled(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("target_screen", "notifications")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (System.currentTimeMillis() % 10000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_GENERAL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("📩 $title")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setColor(Color.parseColor("#0D9488"))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % 10000).toInt() + 1000, builder.build())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Sends immediate alert when account status changes (e.g. suspended, banned, activated)
     */
    fun sendAccountStatusChangedNotification(
        context: Context,
        newStatus: String,
        reason: String = ""
    ) {
        if (!areNotificationsEnabled(context)) return

        val isSuspended = newStatus.equals("SUSPENDED", ignoreCase = true) || newStatus == "موقوف"
        val isBanned = newStatus.equals("BANNED", ignoreCase = true) || newStatus == "محظور"
        val isActive = newStatus.equals("ACTIVE", ignoreCase = true) || newStatus == "نشط"

        val title = when {
            isBanned -> "🚫 تنبيه عاجل: تم حظر الحساب"
            isSuspended -> "⏸ تنبيه: تم إيقاف الخدمة مؤقتاً"
            isActive -> "✅ تم تفعيل حسابك بنجاح"
            else -> "تحديث حالة الحساب: $newStatus"
        }

        val text = when {
            isBanned -> reason.ifBlank { "تم إيقاف وحظر حسابك من قِبل إدارة تطبيق البيان. يرجى التواصل مع الإدارة." }
            isSuspended -> reason.ifBlank { "تم إيقاف الخدمة مؤقتاً لحسابك من قِبل الإدارة. يرجى مراجعة الدعم الفني." }
            isActive -> "مرحباً بك! حسابك الآن نشط بالكامل ويمكنك استخدام كافة ميزات التطبيق."
            else -> reason.ifBlank { "تم تحديث حالة حسابك إلى $newStatus" }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            9914,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_GENERAL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setColor(if (isBanned) Color.parseColor("#E11D48") else if (isSuspended) Color.parseColor("#D97706") else Color.parseColor("#059669"))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(context).notify(9914, builder.build())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
