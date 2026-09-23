package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Account/Contact entity (عميل / تاجر / جار / موظف)
 */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userEmail: String = "", // Associated merchant / user email
    val name: String,
    val phone: String = "",
    val category: String = "عميل", // عملاء، تجار، جيران، موظف...
    val currency: String = "USD", // USD, SAR, EGP, AED, etc.
    val notes: String = "",
    val avatarColorIndex: Int = 0,
    val avatarUri: String = "",
    val displayIndex: Int = 0, // Fixed display order/number (e.g. 1: احمد, 2: محمد)
    val creditLimit: Double = 0.0, // Max debt limit / المبلغ المستحق المسموح
    val autoSendWhatsApp: Boolean = false, // إرسال الفواتير تلقائياً لواتساب
    val syncKey: String = "", // Unique persistent cloud sync key for real-time customer QR sync
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Financial Transaction Movement (لنا أو له)
 * - LANA: لنا (دين عليه / مبيعات / دفعنا له) - Debtor owe us
 * - LAHO: له (دفعة منه / سداد / استلمنا منه / دين له علينا) - Creditor we owe
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userEmail: String = "", // Associated merchant / user email
    val accountId: Long,
    val type: String, // "LANA" or "LAHO"
    val amount: Double,
    val currency: String = "USD",
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    val dueDate: Long? = null, // Optional due date for collection reminder
    val isSettled: Boolean = false,
    val receiptNumber: String = ""
)

/**
 * Account with aggregate balance data for UI lists
 */
data class AccountWithBalance(
    val account: AccountEntity,
    val totalLana: Double = 0.0,
    val totalLaho: Double = 0.0,
    val netBalance: Double = 0.0, // totalLana - totalLaho (positive = لنا / debtor, negative = له / creditor)
    val transactionCount: Int = 0,
    val lastTransactionDate: Long? = null,
    val lastTransactionDescription: String? = null,
    val isCreditLimitExceeded: Boolean = false,
    val currencyBalances: Map<String, Double> = emptyMap() // currency -> net balance
) {
    /**
     * Returns the effective balance for a given currency filter.
     * When filter is "ALL", returns the primary non-zero net balance.
     */
    fun getEffectiveBalance(currencyFilter: String = "ALL"): Double {
        return if (currencyFilter == "ALL") {
            if (netBalance != 0.0) netBalance
            else currencyBalances.values.firstOrNull { it != 0.0 } ?: 0.0
        } else {
            currencyBalances[currencyFilter] ?: if (account.currency.equals(currencyFilter, ignoreCase = true)) netBalance else 0.0
        }
    }
}

/**
 * Currency aggregate for "أرصدة العملات" report
 */
data class CurrencyBalance(
    val currency: String,
    val totalLana: Double,
    val totalLaho: Double,
    val netBalance: Double,
    val accountCount: Int
)

/**
 * Due transaction item for "تقرير المستحقات"
 */
data class DueItem(
    val transaction: TransactionEntity,
    val accountName: String,
    val accountPhone: String,
    val daysRemaining: Int // negative = overdue
)

/**
 * Overall summary for top dashboard cards
 */
data class OverallSummary(
    val totalLana: Double = 0.0,
    val totalLaho: Double = 0.0,
    val netBalance: Double = 0.0,
    val totalAccounts: Int = 0,
    val dueSoonCount: Int = 0,
    val currencySummaries: List<CurrencyBalance> = emptyList()
)

/**
 * Staff user entity for "المستخدمين" (إدارة المستخدمين)
 */
data class StaffUser(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val phone: String = "",
    val role: String = "محاسب", // موظف، محاسب، كاشير، مندوب مبيعات، مشرف
    val permissionType: String = "كتابة", // كتابة، قراءة فقط
    val permissions: List<String> = listOf("إضافة معاملات", "عرض التقارير"),
    val avatarUri: String = "",
    val isActive: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * Staff active session when logged in via QR
 */
data class StaffSession(
    val merchantEmail: String,
    val userId: String,
    val userName: String,
    val role: String,
    val permissionType: String = "كتابة", // كتابة، قراءة فقط، مخصص
    val permissions: List<String> = AppPermissions.DEFAULT_STAFF_PERMISSIONS,
    val storeName: String = "",
    val avatarUri: String = "",
    val loginTimestamp: Long = System.currentTimeMillis()
) {
    fun hasPermission(permission: String): Boolean {
        if (permissionType == "كامل الصلاحيات" || permissionType.startsWith("كامل")) return true
        if (permissions.contains(permission) || permissions.contains("كامل الصلاحيات") || permissions.contains("ALL")) return true

        val req = permission.trim()

        // Flexible matching for reports & views
        if (req == AppPermissions.VIEW_REPORTS) {
            if (permissions.any { it.contains("تقارير") || it.contains("التقارير") || it == "عرض التقارير" }) return true
        }
        if (req == AppPermissions.VIEW_ACCOUNTS) {
            if (permissions.any { it.contains("حسابات") || it.contains("الحسابات") || it == "عرض الحسابات" }) return true
        }
        if (req == AppPermissions.VIEW_TRANSACTIONS) {
            if (permissions.any { it.contains("قيود") || it.contains("معاملات") || it.contains("القيود") }) return true
        }
        if (req == AppPermissions.VIEW_DUES) {
            if (permissions.any { it.contains("مستحقات") || it.contains("ديون") || it.contains("المستحقات") }) return true
        }
        if (req == AppPermissions.EDIT_ACCOUNT) {
            if (permissions.any { it.contains("تعديل") || it.contains("حساب") || it.contains("إدارة") || it == AppPermissions.EDIT_ACCOUNT }) return true
        }
        if (req == AppPermissions.ADD_ACCOUNT) {
            if (permissions.any { it.contains("إضافة") || it.contains("حساب") || it == AppPermissions.ADD_ACCOUNT }) return true
        }

        // Legacy fallback checks
        if (permissionType == "كتابة" && req in listOf(
            AppPermissions.VIEW_ACCOUNTS, AppPermissions.ADD_ACCOUNT, AppPermissions.EDIT_ACCOUNT,
            AppPermissions.VIEW_TRANSACTIONS, AppPermissions.ADD_TRANSACTION, AppPermissions.EDIT_TRANSACTION,
            AppPermissions.VIEW_REPORTS, AppPermissions.VIEW_DUES
        )) return true
        if (permissionType == "قراءة فقط" && req in listOf(
            AppPermissions.VIEW_ACCOUNTS, AppPermissions.VIEW_TRANSACTIONS, AppPermissions.VIEW_REPORTS, AppPermissions.VIEW_DUES
        )) return true
        return false
    }

    val canWrite: Boolean
        get() = hasPermission(AppPermissions.ADD_TRANSACTION) || hasPermission(AppPermissions.ADD_ACCOUNT)
}

/**
 * Subscription info for trial & Pro plans
 */
data class SubscriptionInfo(
    val planName: String = "الفترة التجريبية",
    val isPro: Boolean = false,
    val trialDaysRemaining: Int = 4,
    val allowedTransactionsMonthly: Int = 50,
    val usedTransactionsMonthly: Int = 0,
    val isTrialActive: Boolean = true,
    val expiryDate: Long = System.currentTimeMillis() + 4L * 24 * 60 * 60 * 1000
)

/**
 * Support Ticket model
 */
data class SupportTicket(
    val id: String = java.util.UUID.randomUUID().toString().take(6).uppercase(),
    val subject: String,
    val details: String,
    val status: String = "قيد المراجعة", // قيد المراجعة، تم الرد، مغلقة
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Top Purchaser report item model
 */
data class TopPurchaserItem(
    val accountWithBalance: AccountWithBalance,
    val totalPurchases: Double,
    val purchaseCount: Int,
    val averagePurchase: Double,
    val purchaseSharePercent: Float,
    val rank: Int
)

/**
 * Overdue severity levels for delayed payments report
 */
enum class OverdueSeverity {
    CRITICAL, // > 30 days overdue
    WARNING,  // 15 - 30 days overdue
    DUE,      // 1 - 14 days overdue
    UPCOMING  // Due soon or pending
}

/**
 * Overdue / Delayed payment report item model
 */
data class OverdueAccountItem(
    val accountWithBalance: AccountWithBalance,
    val overdueAmount: Double,
    val daysOverdue: Int,
    val oldestUnpaidDate: Long?,
    val dueDate: Long?,
    val lastPaymentDate: Long?,
    val lastPaymentAmount: Double?,
    val severity: OverdueSeverity
)

