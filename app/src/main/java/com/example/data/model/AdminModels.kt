package com.example.data.model

/**
 * Registered merchant / user account for Admin Dashboard management
 */
data class AdminUserAccount(
    val id: String,
    val email: String,
    val storeName: String = "متجر البيان",
    val merchantName: String = "",
    val phone: String = "",
    val password: String = "123456", // Password for login and recovery assistance
    val status: String = "ACTIVE", // ACTIVE (نشط), SUSPENDED (متوقف مؤقتاً), BANNED (محظور)
    val plan: String = "مجاني", // مجاني، أسبوعي، شهري، سنوي
    val registeredAt: Long = System.currentTimeMillis(),
    val subscriptionStart: Long = System.currentTimeMillis(),
    val subscriptionExpiry: Long = System.currentTimeMillis() + 4L * 24 * 60 * 60 * 1000, // Default 4 days trial
    val totalAccountsCount: Int = 0,
    val totalVolume: Double = 0.0,
    val notes: String = ""
) {
    val isExpired: Boolean
        get() = plan != "مجاني" && System.currentTimeMillis() > subscriptionExpiry

    val daysRemaining: Int
        get() {
            val diff = subscriptionExpiry - System.currentTimeMillis()
            return if (diff > 0) (diff / (1000 * 60 * 60 * 24)).toInt() else 0
        }
}

/**
 * Payment method configuration set by Admin (e.g. Wallets, Bank accounts, Crypto)
 */
data class PaymentMethodConfig(
    val id: String = java.util.UUID.randomUUID().toString().take(8),
    val name: String, // e.g. شام كاش (Sham Cash), سيريتل كاش, زين كاش, فودافون كاش, USDT (TRC20), حساب بنكي
    val accountNumber: String, // رقم المحفظة أو الحساب
    val accountHolder: String = "", // اسم صاحب الحساب / المستلم
    val instructions: String = "", // تعليمات التحويل
    val iconName: String = "wallet", // wallet, bank, crypto, phone
    val isActive: Boolean = true
)

/**
 * Subscription payment request sent by a user after transferring funds
 */
data class SubscriptionPaymentRequest(
    val id: String = java.util.UUID.randomUUID().toString().take(8),
    val userEmail: String,
    val userName: String = "",
    val userPhone: String = "",
    val planName: String, // أسبوعي، شهري، سنوي
    val planPrice: String = "",
    val paymentMethodName: String, // e.g. Sham Cash, USDT
    val transferNumber: String = "", // رقم الحوالة / السند
    val senderPhone: String = "", // رقم هاتف المحول
    val proofImageUri: String = "", // صورة الإشعار
    val notes: String = "",
    val status: String = "PENDING", // PENDING (بانتظار المراجعة), APPROVED (تم التفعيل), REJECTED (مرفوض)
    val createdAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null,
    val adminNotes: String = ""
)

/**
 * System-wide broadcast announcement sent by Admin
 */
data class SystemBroadcastMessage(
    val id: String = java.util.UUID.randomUUID().toString().take(8),
    val title: String,
    val message: String,
    val sender: String = "إدارة تطبيق البيان",
    val sentAt: Long = System.currentTimeMillis()
)

/**
 * Pricing plans configuration
 */
data class PlanPricing(
    val id: String,
    val name: String, // مجاني، أسبوعي، شهري، سنوي
    val durationDays: Int,
    val priceText: String,
    val features: List<String>
)
