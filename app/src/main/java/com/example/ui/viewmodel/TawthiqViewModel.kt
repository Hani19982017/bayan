package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.AccountWithBalance
import com.example.data.model.AppPermissions
import com.example.data.model.CurrencyBalance
import com.example.data.model.DueItem
import com.example.data.model.OverallSummary
import com.example.data.model.StaffSession
import com.example.data.model.StaffUser
import com.example.data.model.SubscriptionInfo
import com.example.data.model.SupportTicket
import com.example.data.model.TransactionEntity
import com.example.data.model.TopPurchaserItem
import com.example.data.model.OverdueAccountItem
import com.example.data.model.AdminUserAccount
import com.example.data.model.PaymentMethodConfig
import com.example.data.model.SubscriptionPaymentRequest
import com.example.data.model.SystemBroadcastMessage
import com.example.data.model.PlanPricing
import com.example.data.repository.TawthiqRepository
import com.example.ui.theme.AppThemeMode
import com.example.util.TawthiqNotificationManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import com.example.util.ExcelExportHelper

class TawthiqViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TawthiqRepository
    private val prefs = application.getSharedPreferences("tawthiq_settings_prefs", Context.MODE_PRIVATE)

    // App Theme (Light / Dark / System)
    private val _themeMode = MutableStateFlow(
        try {
            val saved = prefs.getString("app_theme_mode", AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
            AppThemeMode.valueOf(saved)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("app_theme_mode", mode.name).apply()
    }

    fun toggleDarkMode() {
        val nextMode = when (_themeMode.value) {
            AppThemeMode.LIGHT -> AppThemeMode.DARK
            AppThemeMode.DARK -> AppThemeMode.LIGHT
            AppThemeMode.SYSTEM -> AppThemeMode.DARK
        }
        setThemeMode(nextMode)
    }

    // Clean install check: reset any lingering test sessions on new version
    private val isFreshInstallSession = !prefs.getBoolean("auth_v2_initialized", false)

    // Authentication state
    private val _isLoggedIn = MutableStateFlow(if (isFreshInstallSession) false else prefs.getBoolean("is_logged_in", false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userEmail = MutableStateFlow(if (isFreshInstallSession) "" else (prefs.getString("user_email", "") ?: ""))
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    // Store & Merchant Profile State (persisted per user in prefs)
    private val _storeName = MutableStateFlow(getSavedStoreName(_userEmail.value))
    val storeName: StateFlow<String> = _storeName.asStateFlow()

    private val _merchantName = MutableStateFlow(getSavedMerchantName(_userEmail.value))
    val merchantName: StateFlow<String> = _merchantName.asStateFlow()

    private val _merchantPhone = MutableStateFlow(getSavedMerchantPhone(_userEmail.value))
    val merchantPhone: StateFlow<String> = _merchantPhone.asStateFlow()

    private val _userWhatsApp = MutableStateFlow(getSavedWhatsApp(_userEmail.value))
    val userWhatsApp: StateFlow<String> = _userWhatsApp.asStateFlow()

    private val _storeImageUri = MutableStateFlow(getSavedStoreImageUri(_userEmail.value))
    val storeImageUri: StateFlow<String> = _storeImageUri.asStateFlow()

    private val _defaultCurrency = MutableStateFlow(getSavedCurrency(_userEmail.value))
    val defaultCurrency: StateFlow<String> = _defaultCurrency.asStateFlow()

    // Subscription & Plan Status
    private val _subscriptionInfo = MutableStateFlow(getSavedSubscriptionInfo(_userEmail.value))
    val subscriptionInfo: StateFlow<SubscriptionInfo> = _subscriptionInfo.asStateFlow()

    // Current User Account Status (ACTIVE, SUSPENDED, BANNED)
    private val _currentUserAccountStatus = MutableStateFlow(getSavedAccountStatus(_userEmail.value))
    val currentUserAccountStatus: StateFlow<String> = _currentUserAccountStatus.asStateFlow()

    // Admin Dashboard: Registered User Accounts
    private val _adminUserAccounts = MutableStateFlow<List<AdminUserAccount>>(loadSavedAdminUsers())
    val adminUserAccounts: StateFlow<List<AdminUserAccount>> = _adminUserAccounts.asStateFlow()

    // Admin Dashboard: Payment Methods (Wallets, Syriatel Cash, Sham Cash, USDT, Bank)
    private val _paymentMethods = MutableStateFlow<List<PaymentMethodConfig>>(loadSavedPaymentMethods())
    val paymentMethods: StateFlow<List<PaymentMethodConfig>> = _paymentMethods.asStateFlow()

    // Admin Dashboard: Incoming Subscription Payment Requests (Manual Payment Verification)
    private val _subscriptionPaymentRequests = MutableStateFlow<List<SubscriptionPaymentRequest>>(loadSavedPaymentRequests())
    val subscriptionPaymentRequests: StateFlow<List<SubscriptionPaymentRequest>> = _subscriptionPaymentRequests.asStateFlow()

    // Admin Dashboard: System Broadcast Messages
    private val _systemBroadcasts = MutableStateFlow<List<SystemBroadcastMessage>>(loadSavedBroadcastMessages())
    val systemBroadcasts: StateFlow<List<SystemBroadcastMessage>> = _systemBroadcasts.asStateFlow()

    // Staff Users (المستخدمين - موظفين، محاسبين، كاشير...)
    private val _staffUsers = MutableStateFlow<List<StaffUser>>(loadSavedStaffUsers(_userEmail.value))
    val staffUsers: StateFlow<List<StaffUser>> = _staffUsers.asStateFlow()

    // Current Staff Session (if logged in via QR)
    private val _currentStaffSession = MutableStateFlow<StaffSession?>(getSavedStaffSession())
    val currentStaffSession: StateFlow<StaffSession?> = _currentStaffSession.asStateFlow()

    val isStaffUser: StateFlow<Boolean> = _currentStaffSession.map { it != null }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = _currentStaffSession.value != null
    )

    val canWrite: StateFlow<Boolean> = _currentStaffSession.map { session ->
        session == null || session.canWrite
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = _currentStaffSession.value?.canWrite ?: true
    )

    fun hasPermission(permission: String): Boolean {
        val session = _currentStaffSession.value ?: return true
        return session.hasPermission(permission)
    }

    // Support Tickets
    private val _supportTickets = MutableStateFlow<List<SupportTicket>>(listOf(
        SupportTicket(
            subject = "طلب تفعيل ميزة المزامنة السحابية",
            details = "أرغب في تفعيل النسخ الاحتياطي التلقائي لكافة الحسابات يومياً",
            status = "تم الرد"
        )
    ))
    val supportTickets: StateFlow<List<SupportTicket>> = _supportTickets.asStateFlow()

    private fun getSavedStoreName(email: String): String {
        return prefs.getString("store_name_$email", prefs.getString("store_name", "متجري") ?: "متجري") ?: "متجري"
    }

    private fun getSavedMerchantName(email: String): String {
        val defaultName = if (email.isNotBlank()) email.substringBefore("@") else "التاجر"
        return prefs.getString("merchant_name_$email", prefs.getString("merchant_name", defaultName) ?: defaultName) ?: defaultName
    }

    private fun getSavedMerchantPhone(email: String): String {
        return prefs.getString("merchant_phone_$email", prefs.getString("merchant_phone", "") ?: "") ?: ""
    }

    private fun getSavedWhatsApp(email: String): String {
        return prefs.getString("whatsapp_$email", "") ?: ""
    }

    private fun getSavedStoreImageUri(email: String): String {
        return prefs.getString("store_image_uri_$email", prefs.getString("store_image_uri", "") ?: "") ?: ""
    }

    private fun getSavedCurrency(email: String): String {
        return prefs.getString("default_currency_$email", prefs.getString("default_currency", "USD") ?: "USD") ?: "USD"
    }

    private fun getSavedAccountStatus(email: String): String {
        if (email.isBlank()) return "ACTIVE"
        return prefs.getString("account_status_$email", "ACTIVE") ?: "ACTIVE"
    }

    private fun getSavedSubscriptionInfo(email: String): SubscriptionInfo {
        val isPro = prefs.getBoolean("is_pro_$email", false)
        val trialDays = prefs.getInt("trial_days_$email", 4)
        val allowed = prefs.getInt("allowed_tx_$email", if (isPro) 99999 else 50)
        val used = prefs.getInt("used_tx_$email", 0)
        val planName = prefs.getString("plan_name_$email", if (isPro) "الباقة غير المحدودة (Pro)" else "فترة تجريبية $trialDays يوم") ?: "مجاني"
        val expiry = prefs.getLong("subscription_expiry_$email", System.currentTimeMillis() + trialDays.toLong() * 86400000L)
        
        // Auto-expiration: if paid/pro plan and current time is past expiry date, automatically revert to free
        val isPastExpiry = isPro && (System.currentTimeMillis() > expiry)
        val effectivePro = isPro && !isPastExpiry
        val effectivePlanName = if (isPastExpiry) "فترة تجريبية منتهية (مجاني)" else planName
        val remainingDays = if (effectivePro) (((expiry - System.currentTimeMillis()).coerceAtLeast(0)) / 86400000L).toInt() else trialDays

        if (isPastExpiry) {
            prefs.edit()
                .putBoolean("is_pro_$email", false)
                .putString("plan_name_$email", "مجاني")
                .apply()
        }

        return SubscriptionInfo(
            planName = effectivePlanName,
            isPro = effectivePro,
            trialDaysRemaining = remainingDays,
            allowedTransactionsMonthly = if (effectivePro) allowed else 50,
            usedTransactionsMonthly = used,
            isTrialActive = !effectivePro && trialDays > 0,
            expiryDate = expiry
        )
    }

    private fun getSavedStaffSession(): StaffSession? {
        if (!prefs.getBoolean("auth_v2_initialized", false)) return null
        val isStaff = prefs.getBoolean("is_staff_logged_in", false)
        if (!isStaff) return null
        val email = prefs.getString("staff_merchant_email", "") ?: ""
        val userId = prefs.getString("staff_user_id", "") ?: ""
        val name = prefs.getString("staff_user_name", "") ?: ""
        val role = prefs.getString("staff_user_role", "موظف") ?: "موظف"
        val permissionType = prefs.getString("staff_permission_type", "كتابة") ?: "كتابة"
        val permsRaw = prefs.getString("staff_permissions", "ALL") ?: "ALL"
        val store = prefs.getString("staff_store_name", "") ?: ""
        val avatar = prefs.getString("staff_avatar_uri", "") ?: ""
        if (email.isBlank()) return null

        val permList = when {
            permissionType == "كامل الصلاحيات" || permissionType.startsWith("كامل") || permsRaw == "ALL" -> AppPermissions.ALL_PERMISSIONS
            permissionType == "قراءة فقط" || permsRaw == "READ_ONLY" -> AppPermissions.READ_ONLY_PERMISSIONS
            permsRaw.isNotBlank() -> permsRaw.split(",").filter { it.isNotBlank() }
            else -> AppPermissions.DEFAULT_STAFF_PERMISSIONS
        }

        return StaffSession(
            merchantEmail = email,
            userId = userId,
            userName = name,
            role = role,
            permissionType = permissionType,
            permissions = permList,
            storeName = store,
            avatarUri = avatar
        )
    }

    private fun loadSavedStaffUsers(email: String): List<StaffUser> {
        val raw = prefs.getString("staff_users_$email", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return try {
            raw.split(";;;").filter { it.isNotBlank() }.map { part ->
                val fields = part.split("|||")
                StaffUser(
                    id = fields.getOrElse(0) { java.util.UUID.randomUUID().toString() },
                    name = fields.getOrElse(1) { "مستخدم" },
                    phone = fields.getOrElse(2) { "" },
                    role = fields.getOrElse(3) { "موظف" },
                    permissionType = fields.getOrElse(4) { "كتابة" },
                    permissions = fields.getOrElse(5) { "إضافة معاملات" }.split(",").filter { it.isNotBlank() },
                    isActive = fields.getOrElse(6) { "true" }.toBoolean(),
                    addedAt = fields.getOrElse(7) { "${System.currentTimeMillis()}" }.toLongOrNull() ?: System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveStaffUsers(email: String, users: List<StaffUser>) {
        val raw = users.joinToString(";;;") { u ->
            "${u.id}|||${u.name}|||${u.phone}|||${u.role}|||${u.permissionType}|||${u.permissions.joinToString(",")}|||${u.isActive}|||${u.addedAt}"
        }
        prefs.edit().putString("staff_users_$email", raw).apply()
    }

    private fun loadUserProfile(email: String) {
        _storeName.value = getSavedStoreName(email)
        _merchantName.value = getSavedMerchantName(email)
        _merchantPhone.value = getSavedMerchantPhone(email)
        _userWhatsApp.value = getSavedWhatsApp(email)
        _storeImageUri.value = getSavedStoreImageUri(email)
        _defaultCurrency.value = getSavedCurrency(email)
        _subscriptionInfo.value = getSavedSubscriptionInfo(email)
        _staffUsers.value = loadSavedStaffUsers(email)
        _merchantAvatarUri.value = getSavedMerchantAvatarUri(email)

        viewModelScope.launch {
            try {
                repository.sanitizeAndEnsureUniqueIndexes(email)
            } catch (_: Exception) {}
        }
    }

    private val _merchantAvatarUri = MutableStateFlow(getSavedMerchantAvatarUri(_userEmail.value))
    val merchantAvatarUri: StateFlow<String> = _merchantAvatarUri.asStateFlow()

    private fun getSavedMerchantAvatarUri(email: String): String {
        return prefs.getString("merchant_avatar_$email", prefs.getString("merchant_avatar", "") ?: "") ?: ""
    }

    fun updateMerchantAvatar(uri: String) {
        val email = _userEmail.value
        _merchantAvatarUri.value = uri
        prefs.edit().putString("merchant_avatar_$email", uri).apply()
    }

    fun setWhatsAppNumber(whatsapp: String) {
        val email = _userEmail.value
        _userWhatsApp.value = whatsapp.trim()
        prefs.edit().putString("whatsapp_$email", whatsapp.trim()).apply()
    }

    fun addStaffUser(user: StaffUser) {
        val email = _userEmail.value
        val updated = _staffUsers.value + user
        _staffUsers.value = updated
        saveStaffUsers(email, updated)
    }

    fun updateStaffUser(user: StaffUser) {
        val email = _userEmail.value
        val updated = _staffUsers.value.map { if (it.id == user.id) user else it }
        _staffUsers.value = updated
        saveStaffUsers(email, updated)

        val session = _currentStaffSession.value
        if (session != null && (session.userId == user.id || session.userName.trim() == user.name.trim())) {
            val updatedSession = session.copy(
                userName = user.name,
                role = user.role,
                permissionType = user.permissionType,
                permissions = user.permissions,
                avatarUri = user.avatarUri
            )
            _currentStaffSession.value = updatedSession
            prefs.edit().putString("staff_avatar_uri", user.avatarUri).apply()
        }
    }

    fun removeStaffUser(userId: String) {
        val email = _userEmail.value
        val updated = _staffUsers.value.filter { it.id != userId }
        _staffUsers.value = updated
        saveStaffUsers(email, updated)
    }

    // =========================================================================
    // Admin Dashboard & Subscription Management (100% Real Persistence & Sync)
    // =========================================================================

    private fun loadSavedAdminUsers(): List<AdminUserAccount> {
        val raw = prefs.getString("admin_user_accounts_json", "") ?: ""
        if (raw.isNotBlank()) {
            try {
                val array = JSONArray(raw)
                val list = mutableListOf<AdminUserAccount>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        AdminUserAccount(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                            email = obj.optString("email", ""),
                            storeName = obj.optString("storeName", "متجر البيان"),
                            merchantName = obj.optString("merchantName", ""),
                            phone = obj.optString("phone", ""),
                            password = obj.optString("password", "123456"),
                            status = obj.optString("status", "ACTIVE"),
                            plan = obj.optString("plan", "مجاني"),
                            registeredAt = obj.optLong("registeredAt", System.currentTimeMillis()),
                            subscriptionStart = obj.optLong("subscriptionStart", System.currentTimeMillis()),
                            subscriptionExpiry = obj.optLong("subscriptionExpiry", System.currentTimeMillis() + 4L * 86400000L),
                            totalAccountsCount = obj.optInt("totalAccountsCount", 0),
                            totalVolume = obj.optDouble("totalVolume", 0.0),
                            notes = obj.optString("notes", "")
                        )
                    )
                }
                if (list.isNotEmpty()) return list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Seed initial default accounts so admin has instant rich data to inspect
        val now = System.currentTimeMillis()
        val defaultList = listOf(
            AdminUserAccount(
                id = "usr_admin",
                email = if (_userEmail.value.isNotBlank()) _userEmail.value else "admin@bayan.app",
                storeName = _storeName.value.ifBlank { "الإدارة العامة للبيان" },
                merchantName = _merchantName.value.ifBlank { "مدير النظام" },
                phone = "+963933123456",
                password = "admin",
                status = "ACTIVE",
                plan = "سنوي",
                registeredAt = now - 30L * 86400000L,
                subscriptionStart = now - 30L * 86400000L,
                subscriptionExpiry = now + 335L * 86400000L,
                notes = "حساب إدارة النظام الرئيسي"
            ),
            AdminUserAccount(
                id = "usr_ahmed",
                email = "ahmed.trader@gmail.com",
                storeName = "مؤسسة أحمد التجارية",
                merchantName = "أحمد المحمد",
                phone = "+963944112233",
                password = "user123",
                status = "ACTIVE",
                plan = "شهري",
                registeredAt = now - 14L * 86400000L,
                subscriptionStart = now - 14L * 86400000L,
                subscriptionExpiry = now + 16L * 86400000L,
                notes = "مشترك نشط - محفظة شام كاش"
            ),
            AdminUserAccount(
                id = "usr_samer",
                email = "samer.store@gmail.com",
                storeName = "متجر سامر للأجهزة الكهربائية",
                merchantName = "سامر الخالد",
                phone = "+963933556677",
                password = "pass987",
                status = "ACTIVE",
                plan = "أسبوعي",
                registeredAt = now - 3L * 86400000L,
                subscriptionStart = now - 3L * 86400000L,
                subscriptionExpiry = now + 4L * 86400000L,
                notes = "دفعة أسبوعية عبر سيريتل كاش"
            ),
            AdminUserAccount(
                id = "usr_nour",
                email = "nour.fashion@gmail.com",
                storeName = "أزياء النور الراقية",
                merchantName = "نور الدين العلي",
                phone = "+963955998877",
                password = "nour456",
                status = "ACTIVE",
                plan = "مجاني",
                registeredAt = now - 20L * 86400000L,
                subscriptionStart = now - 20L * 86400000L,
                subscriptionExpiry = now - 2L * 86400000L,
                notes = "انتهت الفترة التجريبية - بانتظار التجديد"
            ),
            AdminUserAccount(
                id = "usr_khalid",
                email = "khalid.tech@gmail.com",
                storeName = "خالد لتقنية المعلومات",
                merchantName = "خالد النجار",
                phone = "+9647801234567",
                password = "khalid2026",
                status = "SUSPENDED",
                plan = "مجاني",
                registeredAt = now - 10L * 86400000L,
                subscriptionStart = now - 10L * 86400000L,
                subscriptionExpiry = now - 6L * 86400000L,
                notes = "تم إيقاف الخدمة مؤقتاً بانتظار تحديث البيانات"
            )
        )
        saveAdminUsersInternal(defaultList)
        return defaultList
    }

    private fun saveAdminUsersInternal(list: List<AdminUserAccount>) {
        try {
            val array = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("email", item.email)
                    put("storeName", item.storeName)
                    put("merchantName", item.merchantName)
                    put("phone", item.phone)
                    put("password", item.password)
                    put("status", item.status)
                    put("plan", item.plan)
                    put("registeredAt", item.registeredAt)
                    put("subscriptionStart", item.subscriptionStart)
                    put("subscriptionExpiry", item.subscriptionExpiry)
                    put("totalAccountsCount", item.totalAccountsCount)
                    put("totalVolume", item.totalVolume)
                    put("notes", item.notes)
                }
                array.put(obj)
            }
            prefs.edit().putString("admin_user_accounts_json", array.toString()).apply()

            // Real-time Firestore sync
            val db = FirebaseFirestore.getInstance()
            for (item in list) {
                val cleanId = item.email.replace(".", "_").replace("@", "_")
                val doc = hashMapOf(
                    "id" to item.id,
                    "email" to item.email,
                    "storeName" to item.storeName,
                    "merchantName" to item.merchantName,
                    "phone" to item.phone,
                    "password" to item.password,
                    "status" to item.status,
                    "plan" to item.plan,
                    "registeredAt" to item.registeredAt,
                    "subscriptionStart" to item.subscriptionStart,
                    "subscriptionExpiry" to item.subscriptionExpiry,
                    "updatedAt" to System.currentTimeMillis()
                )
                db.collection("system_admin_users").document(cleanId).set(doc, SetOptions.merge())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Requirement 2: Activate, Suspend, Ban, or Delete Account
     */
    fun updateUserStatus(userEmail: String, newStatus: String) {
        val currentList = _adminUserAccounts.value
        val updated = currentList.map {
            if (it.email.equals(userEmail.trim(), ignoreCase = true)) it.copy(status = newStatus) else it
        }
        _adminUserAccounts.value = updated
        saveAdminUsersInternal(updated)

        prefs.edit().putString("account_status_${userEmail.trim().lowercase()}", newStatus).apply()
        if (userEmail.trim().equals(_userEmail.value.trim(), ignoreCase = true)) {
            _currentUserAccountStatus.value = newStatus
        }
    }

    fun deleteAdminUser(userEmail: String) {
        val clean = userEmail.trim().lowercase()
        val currentList = _adminUserAccounts.value
        val updated = currentList.filterNot { it.email.equals(clean, ignoreCase = true) }
        _adminUserAccounts.value = updated
        saveAdminUsersInternal(updated)

        try {
            val cleanId = clean.replace(".", "_").replace("@", "_")
            FirebaseFirestore.getInstance().collection("system_admin_users").document(cleanId).delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Requirement 6: Change user password for account recovery assistance
     */
    fun updateUserPassword(userEmail: String, newPassword: String) {
        val clean = userEmail.trim().lowercase()
        val currentList = _adminUserAccounts.value
        val updated = currentList.map {
            if (it.email.equals(clean, ignoreCase = true)) it.copy(password = newPassword.trim()) else it
        }
        _adminUserAccounts.value = updated
        saveAdminUsersInternal(updated)
        prefs.edit().putString("user_pwd_$clean", newPassword.trim()).apply()
    }

    /**
     * Requirement 7: Manage Subscriptions (مجاني، أسبوعي، شهري، سنوي) & Expiry Date
     */
    fun updateUserSubscription(userEmail: String, newPlan: String, durationDays: Int) {
        val clean = userEmail.trim().lowercase()
        val now = System.currentTimeMillis()
        val expiryTime = if (newPlan == "مجاني") {
            now + durationDays.toLong() * 86400000L
        } else {
            now + durationDays.toLong() * 86400000L
        }
        val isPro = newPlan != "مجاني"

        val currentList = _adminUserAccounts.value
        val updated = currentList.map {
            if (it.email.equals(clean, ignoreCase = true)) {
                it.copy(
                    plan = newPlan,
                    subscriptionStart = now,
                    subscriptionExpiry = expiryTime
                )
            } else it
        }
        _adminUserAccounts.value = updated
        saveAdminUsersInternal(updated)

        prefs.edit()
            .putBoolean("is_pro_$clean", isPro)
            .putString("plan_name_$clean", newPlan)
            .putLong("subscription_expiry_$clean", expiryTime)
            .putInt("trial_days_$clean", if (!isPro) durationDays else 0)
            .putInt("allowed_tx_$clean", if (isPro) 99999 else 50)
            .apply()

        if (clean.equals(_userEmail.value.trim().lowercase(), ignoreCase = true)) {
            _subscriptionInfo.value = SubscriptionInfo(
                planName = newPlan,
                isPro = isPro,
                trialDaysRemaining = durationDays,
                allowedTransactionsMonthly = if (isPro) 99999 else 50,
                usedTransactionsMonthly = _subscriptionInfo.value.usedTransactionsMonthly,
                isTrialActive = !isPro && durationDays > 0,
                expiryDate = expiryTime
            )
        }
    }

    /**
     * Requirement 3: View Customer Accounts and Ledger Transactions for any Merchant
     */
    fun getCustomersAndTransactionsForUser(
        userEmail: String,
        onResult: (List<Pair<AccountEntity, List<TransactionEntity>>>) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val accounts = repository.getAccountsForUserSnapshot(userEmail)
                val effectiveAccounts = if (accounts.isEmpty()) {
                    repository.getAllAccountsSnapshot().take(10)
                } else accounts

                val result = mutableListOf<Pair<AccountEntity, List<TransactionEntity>>>()
                for (acc in effectiveAccounts) {
                    val txs = repository.getTransactionsSnapshotForAccount(acc.id)
                    result.add(Pair(acc, txs))
                }

                launch(Dispatchers.Main) {
                    onResult(result)
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    onResult(emptyList())
                }
            }
        }
    }

    // =========================================================================
    // Payment Methods Configuration (Requirement 4 & 7)
    // =========================================================================

    private fun loadSavedPaymentMethods(): List<PaymentMethodConfig> {
        val raw = prefs.getString("admin_payment_methods_json", "") ?: ""
        if (raw.isNotBlank()) {
            try {
                val array = JSONArray(raw)
                val list = mutableListOf<PaymentMethodConfig>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        PaymentMethodConfig(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString().take(8)),
                            name = obj.optString("name", ""),
                            accountNumber = obj.optString("accountNumber", ""),
                            accountHolder = obj.optString("accountHolder", ""),
                            instructions = obj.optString("instructions", ""),
                            iconName = obj.optString("iconName", "wallet"),
                            isActive = obj.optBoolean("isActive", true)
                        )
                    )
                }
                if (list.isNotEmpty()) return list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val defaultMethods = listOf(
            PaymentMethodConfig(
                id = "pm_sham",
                name = "شام كاش (Sham Cash)",
                accountNumber = "0987654321",
                accountHolder = "إدارة تطبيق البيان",
                instructions = "يرجى كتابة اسم المتجر ورقم الهاتف في خانة سبب التحويل، وتصوير إشعار العملية.",
                iconName = "wallet",
                isActive = true
            ),
            PaymentMethodConfig(
                id = "pm_syriatel",
                name = "سيريتل كاش (Syriatel Cash)",
                accountNumber = "0933123456",
                accountHolder = "شركة البيان للحلول البرمجية",
                instructions = "التحويل المباشر عبر تطبيق أقرب إليك أو *3030#، مع إرفاق رقم العملية.",
                iconName = "phone",
                isActive = true
            ),
            PaymentMethodConfig(
                id = "pm_usdt",
                name = "USDT (TRC-20 Network)",
                accountNumber = "TXyz987654321BayanCryptoAddressTRC20",
                accountHolder = "Bayan Tech TRC20 Wallet",
                instructions = "التحويل عبر شبكة TRON TRC20 حصراً. يرجى إرسال رقم TxID في إشعار الدفع.",
                iconName = "crypto",
                isActive = true
            ),
            PaymentMethodConfig(
                id = "pm_zain",
                name = "زين كاش / زين العراق",
                accountNumber = "07801234567",
                accountHolder = "مؤسسة البيان المالية",
                instructions = "التحويل عبر تطبيق زين كاش وتأكيد رقم الحوالة.",
                iconName = "wallet",
                isActive = true
            ),
            PaymentMethodConfig(
                id = "pm_bank",
                name = "حساب مصرفي / تحويل بنكي",
                accountNumber = "IBAN: SA0380000001234567890123",
                accountHolder = "مؤسسة البيان التجارية المحدودة",
                instructions = "التحويل البنكي متاح للحسابات السنوية والمؤسسات. إرفاق صورة السند إلزامي.",
                iconName = "bank",
                isActive = true
            )
        )
        savePaymentMethodsInternal(defaultMethods)
        return defaultMethods
    }

    private fun savePaymentMethodsInternal(list: List<PaymentMethodConfig>) {
        try {
            val array = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("accountNumber", item.accountNumber)
                    put("accountHolder", item.accountHolder)
                    put("instructions", item.instructions)
                    put("iconName", item.iconName)
                    put("isActive", item.isActive)
                }
                array.put(obj)
            }
            prefs.edit().putString("admin_payment_methods_json", array.toString()).apply()

            val db = FirebaseFirestore.getInstance()
            for (item in list) {
                val doc = hashMapOf(
                    "id" to item.id,
                    "name" to item.name,
                    "accountNumber" to item.accountNumber,
                    "accountHolder" to item.accountHolder,
                    "instructions" to item.instructions,
                    "isActive" to item.isActive
                )
                db.collection("system_payment_methods").document(item.id).set(doc, SetOptions.merge())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addPaymentMethod(method: PaymentMethodConfig) {
        val updated = _paymentMethods.value + method
        _paymentMethods.value = updated
        savePaymentMethodsInternal(updated)
    }

    fun updatePaymentMethod(method: PaymentMethodConfig) {
        val updated = _paymentMethods.value.map { if (it.id == method.id) method else it }
        _paymentMethods.value = updated
        savePaymentMethodsInternal(updated)
    }

    fun deletePaymentMethod(id: String) {
        val updated = _paymentMethods.value.filterNot { it.id == id }
        _paymentMethods.value = updated
        savePaymentMethodsInternal(updated)
        try {
            FirebaseFirestore.getInstance().collection("system_payment_methods").document(id).delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun togglePaymentMethod(id: String, active: Boolean) {
        val updated = _paymentMethods.value.map { if (it.id == id) it.copy(isActive = active) else it }
        _paymentMethods.value = updated
        savePaymentMethodsInternal(updated)
    }

    // =========================================================================
    // Incoming Subscription Payment Requests (Requirement 4 & Manual Payment Flow)
    // =========================================================================

    private fun loadSavedPaymentRequests(): List<SubscriptionPaymentRequest> {
        val raw = prefs.getString("admin_payment_requests_json", "") ?: ""
        if (raw.isNotBlank()) {
            try {
                val array = JSONArray(raw)
                val list = mutableListOf<SubscriptionPaymentRequest>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        SubscriptionPaymentRequest(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString().take(8)),
                            userEmail = obj.optString("userEmail", ""),
                            userName = obj.optString("userName", ""),
                            userPhone = obj.optString("userPhone", ""),
                            planName = obj.optString("planName", "شهري"),
                            planPrice = obj.optString("planPrice", ""),
                            paymentMethodName = obj.optString("paymentMethodName", ""),
                            transferNumber = obj.optString("transferNumber", ""),
                            senderPhone = obj.optString("senderPhone", ""),
                            proofImageUri = obj.optString("proofImageUri", ""),
                            notes = obj.optString("notes", ""),
                            status = obj.optString("status", "PENDING"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            reviewedAt = if (obj.has("reviewedAt")) obj.optLong("reviewedAt") else null,
                            adminNotes = obj.optString("adminNotes", "")
                        )
                    )
                }
                if (list.isNotEmpty()) return list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Default initial pending requests for immediate demonstration
        val now = System.currentTimeMillis()
        val sample = listOf(
            SubscriptionPaymentRequest(
                id = "req_101",
                userEmail = "ahmed.trader@gmail.com",
                userName = "أحمد المحمد",
                userPhone = "+963944112233",
                planName = "شهري",
                planPrice = "5$",
                paymentMethodName = "شام كاش (Sham Cash)",
                transferNumber = "TRX-998822",
                senderPhone = "0944112233",
                notes = "تم تحويل 5$ عبر شام كاش، يرجى تفعيل الباقة الشهرية.",
                status = "PENDING",
                createdAt = now - 25 * 60 * 1000L
            ),
            SubscriptionPaymentRequest(
                id = "req_102",
                userEmail = "nour.fashion@gmail.com",
                userName = "نور الدين العلي",
                userPhone = "+963955998877",
                planName = "سنوي",
                planPrice = "45$",
                paymentMethodName = "سيريتل كاش (Syriatel Cash)",
                transferNumber = "SYR-554411",
                senderPhone = "0955998877",
                notes = "تحويل باقة سنوية كاملة.",
                status = "PENDING",
                createdAt = now - 2 * 3600 * 1000L
            )
        )
        savePaymentRequestsInternal(sample)
        return sample
    }

    private fun savePaymentRequestsInternal(list: List<SubscriptionPaymentRequest>) {
        try {
            val array = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("userEmail", item.userEmail)
                    put("userName", item.userName)
                    put("userPhone", item.userPhone)
                    put("planName", item.planName)
                    put("planPrice", item.planPrice)
                    put("paymentMethodName", item.paymentMethodName)
                    put("transferNumber", item.transferNumber)
                    put("senderPhone", item.senderPhone)
                    put("proofImageUri", item.proofImageUri)
                    put("notes", item.notes)
                    put("status", item.status)
                    put("createdAt", item.createdAt)
                    item.reviewedAt?.let { put("reviewedAt", it) }
                    put("adminNotes", item.adminNotes)
                }
                array.put(obj)
            }
            prefs.edit().putString("admin_payment_requests_json", array.toString()).apply()

            val db = FirebaseFirestore.getInstance()
            for (item in list) {
                val doc = hashMapOf(
                    "id" to item.id,
                    "userEmail" to item.userEmail,
                    "userName" to item.userName,
                    "userPhone" to item.userPhone,
                    "planName" to item.planName,
                    "planPrice" to item.planPrice,
                    "paymentMethodName" to item.paymentMethodName,
                    "transferNumber" to item.transferNumber,
                    "senderPhone" to item.senderPhone,
                    "proofImageUri" to item.proofImageUri,
                    "notes" to item.notes,
                    "status" to item.status,
                    "createdAt" to item.createdAt,
                    "reviewedAt" to (item.reviewedAt ?: 0L),
                    "adminNotes" to item.adminNotes
                )
                db.collection("subscription_requests").document(item.id).set(doc, SetOptions.merge())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * User submits payment proof after transferring funds
     */
    fun submitPaymentRequest(request: SubscriptionPaymentRequest, context: Context) {
        val updated = listOf(request) + _subscriptionPaymentRequests.value.filterNot { it.id == request.id }
        _subscriptionPaymentRequests.value = updated
        savePaymentRequestsInternal(updated)

        // Instant Push notification to Admin
        TawthiqNotificationManager.sendAdminPaymentNotification(
            context = context,
            userEmail = request.userEmail,
            planName = request.planName,
            transferNumber = request.transferNumber
        )
    }

    /**
     * Admin reviews and activates the subscription
     */
    fun approvePaymentRequest(requestId: String, context: Context, customDurationDays: Int? = null) {
        val req = _subscriptionPaymentRequests.value.find { it.id == requestId } ?: return
        val days = customDurationDays ?: when (req.planName.trim()) {
            "أسبوعي" -> 7
            "شهري" -> 30
            "سنوي" -> 365
            else -> 30
        }

        updateUserSubscription(req.userEmail, req.planName, days)

        val updated = _subscriptionPaymentRequests.value.map {
            if (it.id == requestId) it.copy(status = "APPROVED", reviewedAt = System.currentTimeMillis()) else it
        }
        _subscriptionPaymentRequests.value = updated
        savePaymentRequestsInternal(updated)

        val sdf = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault())
        val expiryFormatted = sdf.format(java.util.Date(System.currentTimeMillis() + days.toLong() * 86400000L))
        TawthiqNotificationManager.sendUserSubscriptionApprovedNotification(
            context = context,
            planName = req.planName,
            expiryDateFormatted = expiryFormatted
        )
    }

    fun rejectPaymentRequest(requestId: String, reason: String = "بيانات التحويل غير مطابقة") {
        val updated = _subscriptionPaymentRequests.value.map {
            if (it.id == requestId) it.copy(status = "REJECTED", adminNotes = reason, reviewedAt = System.currentTimeMillis()) else it
        }
        _subscriptionPaymentRequests.value = updated
        savePaymentRequestsInternal(updated)
    }

    // =========================================================================
    // System Broadcast Messages (Requirement 5)
    // =========================================================================

    private fun loadSavedBroadcastMessages(): List<SystemBroadcastMessage> {
        val raw = prefs.getString("admin_broadcast_messages_json", "") ?: ""
        if (raw.isNotBlank()) {
            try {
                val array = JSONArray(raw)
                val list = mutableListOf<SystemBroadcastMessage>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        SystemBroadcastMessage(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString().take(8)),
                            title = obj.optString("title", ""),
                            message = obj.optString("message", ""),
                            sender = obj.optString("sender", "إدارة تطبيق البيان"),
                            sentAt = obj.optLong("sentAt", System.currentTimeMillis())
                        )
                    )
                }
                if (list.isNotEmpty()) return list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return listOf(
            SystemBroadcastMessage(
                id = "bc_1",
                title = "مرحباً بكم في تحديث البيان الجديد",
                message = "تم إطلاق ميزة المزامنة السحابية اللحظية وإدارة الاشتراكات.",
                sentAt = System.currentTimeMillis() - 24 * 3600 * 1000L
            )
        )
    }

    private fun saveBroadcastMessagesInternal(list: List<SystemBroadcastMessage>) {
        try {
            val array = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("message", item.message)
                    put("sender", item.sender)
                    put("sentAt", item.sentAt)
                }
                array.put(obj)
            }
            prefs.edit().putString("admin_broadcast_messages_json", array.toString()).apply()

            val db = FirebaseFirestore.getInstance()
            for (item in list) {
                val doc = hashMapOf(
                    "id" to item.id,
                    "title" to item.title,
                    "message" to item.message,
                    "sender" to item.sender,
                    "sentAt" to item.sentAt
                )
                db.collection("system_broadcasts").document(item.id).set(doc, SetOptions.merge())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendBroadcastMessage(title: String, message: String, context: Context) {
        val newMsg = SystemBroadcastMessage(
            title = title.trim(),
            message = message.trim(),
            sender = _storeName.value.ifBlank { "إدارة تطبيق البيان" },
            sentAt = System.currentTimeMillis()
        )
        val updated = listOf(newMsg) + _systemBroadcasts.value
        _systemBroadcasts.value = updated
        saveBroadcastMessagesInternal(updated)

        TawthiqNotificationManager.sendAdminBroadcastNotification(
            context = context,
            title = title.trim(),
            message = message.trim()
        )
    }

    // Pro Upgrade Helpers
    fun upgradeToPro(planName: String = "الباقة غير المحدودة (Pro)") {
        updateUserSubscription(_userEmail.value, planName, 30)
    }

    fun setTrialDays(days: Int) {
        updateUserSubscription(_userEmail.value, "فترة تجريبية $days يوم", days)
    }

    fun setMonthlyTransactionLimit(limit: Int) {
        val email = _userEmail.value
        val current = _subscriptionInfo.value
        val updated = current.copy(allowedTransactionsMonthly = limit)
        _subscriptionInfo.value = updated
        prefs.edit().putInt("allowed_tx_$email", limit).apply()
    }

    fun submitSupportTicket(subject: String, details: String) {
        val ticket = SupportTicket(subject = subject.trim(), details = details.trim())
        _supportTickets.value = listOf(ticket) + _supportTickets.value
    }

    fun login(phone: String, merchant: String = "", store: String = "") {
        _isLoggedIn.value = true
        prefs.edit().putBoolean("is_logged_in", true).apply()
        if (merchant.isNotBlank() || store.isNotBlank() || phone.isNotBlank()) {
            val updatedStore = if (store.isNotBlank()) store else _storeName.value
            val updatedMerchant = if (merchant.isNotBlank()) merchant else _merchantName.value
            val updatedPhone = if (phone.isNotBlank()) phone else _merchantPhone.value
            updateStoreProfile(updatedStore, updatedMerchant, updatedPhone, _defaultCurrency.value)
        }
    }

    fun isNewAccount(email: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        return prefs.getBoolean("is_new_account_$cleanEmail", false)
    }

    fun loginWithEmail(email: String, merchant: String = "", store: String = "", phone: String = "", password: String = "") {
        val cleanEmail = email.trim().lowercase()
        _isLoggedIn.value = true
        _userEmail.value = cleanEmail

        val existingStore = prefs.getString("store_name_$cleanEmail", null)
        val isRegisteredBefore = prefs.getBoolean("user_registered_$cleanEmail", false) || existingStore != null

        val editor = prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("user_email", cleanEmail)

        if (password.isNotBlank()) {
            editor.putString("user_pwd_$cleanEmail", password.trim())
        }

        if (!isRegisteredBefore) {
            editor.putBoolean("user_registered_$cleanEmail", true)
            editor.putBoolean("is_new_account_$cleanEmail", true)
        } else {
            editor.putBoolean("is_new_account_$cleanEmail", false)
        }
        editor.apply()

        val derivedMerchant = if (merchant.isNotBlank()) merchant else cleanEmail.substringBefore("@")
        val derivedStore = if (store.isNotBlank()) store else "متجر $derivedMerchant"
        val derivedPhone = if (phone.isNotBlank()) phone else "+966500000000"

        // Ensure user is present in Admin users list
        val currentUsers = _adminUserAccounts.value
        val existsInAdmin = currentUsers.any { it.email.equals(cleanEmail, ignoreCase = true) }
        if (!existsInAdmin) {
            val newUser = AdminUserAccount(
                id = "usr_${System.currentTimeMillis() % 100000}",
                email = cleanEmail,
                storeName = derivedStore,
                merchantName = derivedMerchant,
                phone = derivedPhone,
                password = password.ifBlank { "123456" },
                status = "ACTIVE",
                plan = "مجاني",
                registeredAt = System.currentTimeMillis(),
                subscriptionStart = System.currentTimeMillis(),
                subscriptionExpiry = System.currentTimeMillis() + 4L * 86400000L
            )
            val updated = currentUsers + newUser
            _adminUserAccounts.value = updated
            saveAdminUsersInternal(updated)
        }

        val currentStatus = getSavedAccountStatus(cleanEmail)
        _currentUserAccountStatus.value = currentStatus

        if (existingStore != null) {
            loadUserProfile(cleanEmail)
        } else {
            updateStoreProfile(derivedStore, derivedMerchant, derivedPhone, "USD")
        }
    }

    fun loginAsStaff(
        merchantEmail: String,
        userId: String,
        userName: String,
        role: String,
        permissionType: String,
        permissions: List<String> = emptyList(),
        storeName: String = "",
        avatarUri: String = ""
    ) {
        val cleanEmail = merchantEmail.trim().lowercase()

        val finalPermissions = when {
            permissionType == "كامل الصلاحيات" || permissionType.startsWith("كامل") || permissions.contains("ALL") -> AppPermissions.ALL_PERMISSIONS
            permissionType == "قراءة فقط" || permissions.contains("READ_ONLY") -> AppPermissions.READ_ONLY_PERMISSIONS
            permissions.isNotEmpty() -> permissions
            else -> AppPermissions.DEFAULT_STAFF_PERMISSIONS
        }

        val session = StaffSession(
            merchantEmail = cleanEmail,
            userId = userId,
            userName = userName,
            role = role,
            permissionType = permissionType,
            permissions = finalPermissions,
            storeName = storeName,
            avatarUri = avatarUri
        )
        _currentStaffSession.value = session
        _userEmail.value = cleanEmail
        _isLoggedIn.value = true

        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("user_email", cleanEmail)
            .putBoolean("is_staff_logged_in", true)
            .putString("staff_merchant_email", cleanEmail)
            .putString("staff_user_id", userId)
            .putString("staff_user_name", userName)
            .putString("staff_user_role", role)
            .putString("staff_permission_type", permissionType)
            .putString("staff_permissions", finalPermissions.joinToString(","))
            .putString("staff_store_name", storeName)
            .putString("staff_avatar_uri", avatarUri)
            .apply()

        loadUserProfile(cleanEmail)
    }

    fun logout() {
        _isLoggedIn.value = false
        _currentStaffSession.value = null
        _userEmail.value = ""
        _savedCustomerAccountId.value = null
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .putBoolean("is_staff_logged_in", false)
            .remove("user_email")
            .remove("staff_merchant_email")
            .remove("staff_user_id")
            .remove("staff_user_name")
            .remove("staff_user_role")
            .remove("staff_permission_type")
            .remove("staff_permissions")
            .remove("staff_store_name")
            .remove("staff_avatar_uri")
            .remove("saved_customer_account_id")
            .putBoolean("is_customer_mode", false)
            .apply()
    }

    fun updateStoreProfile(
        storeName: String,
        merchantName: String,
        merchantPhone: String,
        currency: String
    ) {
        val email = _userEmail.value
        val cleanCur = currency.trim().ifBlank { "USD" }
        _storeName.value = storeName
        _merchantName.value = merchantName
        _merchantPhone.value = merchantPhone
        _defaultCurrency.value = cleanCur
        prefs.edit()
            .putString("store_name_$email", storeName)
            .putString("merchant_name_$email", merchantName)
            .putString("merchant_phone_$email", merchantPhone)
            .putString("default_currency_$email", cleanCur)
            .apply()
        viewModelScope.launch {
            repository.updateAllCurrencies(email, cleanCur)
        }
    }

    fun setAppDefaultCurrency(currency: String) {
        val email = _userEmail.value
        val cleanCur = currency.trim().ifBlank { "USD" }
        _defaultCurrency.value = cleanCur
        prefs.edit()
            .putString("default_currency_$email", cleanCur)
            .apply()
        viewModelScope.launch {
            repository.updateAllCurrencies(email, cleanCur)
        }
    }

    fun setStoreImageUri(uri: String) {
        val email = _userEmail.value
        _storeImageUri.value = uri
        prefs.edit().putString("store_image_uri_$email", uri).apply()
    }

    // Active Selected Account for Ledger Detail Screen
    private val _selectedAccountId = MutableStateFlow<Long?>(null)
    val selectedAccountId = _selectedAccountId.asStateFlow()

    // Persistent Saved Customer Account Session (for customers who view statement without merchant login)
    private val _savedCustomerAccountId = MutableStateFlow<Long?>(
        if (isFreshInstallSession) null else prefs.getLong("saved_customer_account_id", -1L).takeIf { it > 0 }
    )
    val savedCustomerAccountId: StateFlow<Long?> = _savedCustomerAccountId.asStateFlow()

    fun setSavedCustomerAccount(accountId: Long) {
        _savedCustomerAccountId.value = accountId
        _selectedAccountId.value = accountId
        _isLoggedIn.value = false
        prefs.edit()
            .putLong("saved_customer_account_id", accountId)
            .putBoolean("is_customer_mode", true)
            .putBoolean("is_logged_in", false)
            .apply()

        // Start real-time sync for this customer account immediately
        viewModelScope.launch {
            val account = repository.getAccountById(accountId).firstOrNull()
            if (account != null) {
                com.example.util.FirebaseSyncManager.startLiveSyncForAccount(
                    context = getApplication(),
                    account = account,
                    coroutineScope = viewModelScope
                )
            }
        }
    }

    fun clearSavedCustomerAccount() {
        val savedId = _savedCustomerAccountId.value
        if (savedId != null) {
            viewModelScope.launch {
                val account = repository.getAccountById(savedId).firstOrNull()
                if (account != null) {
                    com.example.util.FirebaseSyncManager.stopLiveSyncForAccount(account)
                }
            }
        }
        _savedCustomerAccountId.value = null
        clearSelectedAccount()
        prefs.edit()
            .remove("saved_customer_account_id")
            .putBoolean("is_customer_mode", false)
            .apply()
    }

    init {
        if (isFreshInstallSession) {
            prefs.edit()
                .putBoolean("is_logged_in", false)
                .putBoolean("is_staff_logged_in", false)
                .remove("user_email")
                .remove("staff_merchant_email")
                .remove("staff_user_id")
                .remove("staff_user_name")
                .remove("staff_user_role")
                .remove("staff_permission_type")
                .remove("staff_permissions")
                .remove("staff_store_name")
                .remove("staff_avatar_uri")
                .remove("saved_customer_account_id")
                .putBoolean("is_customer_mode", false)
                .putBoolean("auth_v2_initialized", true)
                .apply()
        }

        val database = AppDatabase.getDatabase(getApplication(), viewModelScope)
        repository = TawthiqRepository(database.accountDao(), database.transactionDao())
        loadUserProfile(_userEmail.value)

        // Automatically clean and deduplicate all transactions across accounts on startup
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cleaned = repository.deduplicateAllAccounts()
                if (cleaned > 0) {
                    android.util.Log.d("TawthiqViewModel", "Auto-cleaned $cleaned duplicate transactions on startup")
                }
            } catch (e: Exception) {
                android.util.Log.e("TawthiqViewModel", "Error cleaning duplicate transactions", e)
            }
        }

        // If a customer was previously viewing an account, restore it & start live sync!
        val savedCustId = if (isFreshInstallSession) null else prefs.getLong("saved_customer_account_id", -1L).takeIf { it > 0 }
        if (savedCustId != null) {
            _savedCustomerAccountId.value = savedCustId
            _selectedAccountId.value = savedCustId
            _isLoggedIn.value = false
            viewModelScope.launch {
                val acc = repository.getAccountById(savedCustId).firstOrNull()
                if (acc != null) {
                    com.example.util.FirebaseSyncManager.startLiveSyncForAccount(
                        context = getApplication(),
                        account = acc,
                        coroutineScope = viewModelScope,
                        onUpdate = {
                            _selectedAccountId.value = savedCustId
                        }
                    )
                }
            }
        }
    }

    // Filters and Search
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("الحسابات")

    val categories = MutableStateFlow(listOf("الحسابات"))

    @OptIn(ExperimentalCoroutinesApi::class)
    val accountsWithBalances: StateFlow<List<AccountWithBalance>> = _userEmail.flatMapLatest { email ->
        repository.getAccountsWithBalances(email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredAccounts: StateFlow<List<AccountWithBalance>> =
        combine(accountsWithBalances, searchQuery, selectedCategory) { accounts, query, cat ->
            accounts.filter { item ->
                val matchesCategory = if (cat == "الحسابات") {
                    // Show all accounts under default or "الحسابات"
                    item.account.category.isBlank() || item.account.category == "الحسابات" || item.account.category == "عميل" || item.account.category == "عملاء" || item.account.category == "بدون"
                } else {
                    item.account.category == cat
                }
                val cleanQuery = query.trim()
                val matchesQuery = cleanQuery.isBlank() ||
                        item.account.name.contains(cleanQuery, ignoreCase = true) ||
                        item.account.phone.contains(cleanQuery) ||
                        item.account.notes.contains(cleanQuery, ignoreCase = true) ||
                        item.account.displayIndex.toString() == cleanQuery.removePrefix("#") ||
                        "#${item.account.displayIndex}".contains(cleanQuery)
                matchesCategory && matchesQuery
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val overallSummary: StateFlow<OverallSummary> = _userEmail.flatMapLatest { email ->
        repository.getOverallSummary(email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OverallSummary())

    @OptIn(ExperimentalCoroutinesApi::class)
    val currencyBalances: StateFlow<List<CurrencyBalance>> = _userEmail.flatMapLatest { email ->
        repository.getCurrencyBalances(email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val dueItems: StateFlow<List<DueItem>> = _userEmail.flatMapLatest { email ->
        repository.getDueItems(email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allTransactions: StateFlow<List<TransactionEntity>> = _userEmail.flatMapLatest { email ->
        repository.getTransactionsForUser(email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notifications state
    val unreadNotificationCount = MutableStateFlow(1)

    fun markAllNotificationsRead() {
        unreadNotificationCount.value = 0
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeAccount: StateFlow<AccountEntity?> = _selectedAccountId.flatMapLatest { id ->
        if (id != null) repository.getAccountById(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeAccountTransactions: StateFlow<List<TransactionEntity>> = _selectedAccountId.flatMapLatest { id ->
        if (id != null) repository.getTransactionsForAccount(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectAccount(id: Long) {
        _selectedAccountId.value = id
    }

    fun clearSelectedAccount() {
        _selectedAccountId.value = null
    }

    suspend fun getTransactionsSnapshotForAccount(accountId: Long): List<TransactionEntity> {
        return repository.getTransactionsSnapshotForAccount(accountId)
    }

    fun addCategory(newCategory: String) {
        if (newCategory.isNotBlank() && !categories.value.contains(newCategory.trim())) {
            categories.value = categories.value + newCategory.trim()
        }
    }

    fun addAccount(
        name: String,
        phone: String,
        category: String,
        currency: String,
        initialBalance: Double = 0.0,
        initialType: String = "LANA",
        notes: String = "",
        avatarUri: String = "",
        creditLimit: Double = 0.0,
        displayIndex: Int = 0,
        autoSendWhatsApp: Boolean = false
    ) {
        viewModelScope.launch {
            if (!hasPermission(AppPermissions.ADD_ACCOUNT)) return@launch
            val colorsCount = 6
            val colorIdx = (name.hashCode() % colorsCount + colorsCount) % colorsCount
            val effectiveCurrency = if (currency.isBlank()) _defaultCurrency.value else currency.trim()
            val account = AccountEntity(
                userEmail = _userEmail.value,
                name = name.trim(),
                phone = phone.trim(),
                category = category.trim(),
                currency = effectiveCurrency,
                notes = notes.trim(),
                avatarColorIndex = colorIdx,
                avatarUri = avatarUri,
                creditLimit = creditLimit,
                displayIndex = displayIndex,
                autoSendWhatsApp = autoSendWhatsApp
            )
            repository.addAccount(account, initialBalance, initialType)
        }
    }

    fun updateAccount(account: AccountEntity) {
        viewModelScope.launch {
            if (!hasPermission(AppPermissions.EDIT_ACCOUNT)) return@launch
            repository.updateAccount(account.copy(userEmail = if (account.userEmail.isBlank()) _userEmail.value else account.userEmail))
        }
    }

    fun deleteAccount(accountId: Long) {
        viewModelScope.launch {
            if (!hasPermission(AppPermissions.DELETE_ACCOUNT)) return@launch
            val acc = repository.getAccountById(accountId).firstOrNull()
            if (acc != null) {
                com.example.util.FirebaseSyncManager.deleteAccountEverywhere(acc, _userEmail.value)
            }
            repository.deleteAccount(accountId)
            if (_selectedAccountId.value == accountId) {
                _selectedAccountId.value = null
            }
        }
    }

    fun importCustomerStatementFromQr(
        accountName: String,
        phone: String,
        storeName: String,
        currency: String,
        syncKey: String = "",
        transactionsList: List<TransactionEntity>,
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val accountId = repository.importOrSyncCustomerStatement(
                userEmail = _userEmail.value,
                accountName = accountName,
                phone = phone,
                storeName = storeName,
                currency = currency,
                syncKey = syncKey,
                transactionsList = transactionsList
            )
            _selectedAccountId.value = accountId
            onSuccess(accountId)
        }
    }

    fun addTransaction(
        accountId: Long,
        type: String, // "LANA" or "LAHO"
        amount: Double,
        currency: String = "",
        description: String,
        date: Long = System.currentTimeMillis(),
        dueDate: Long? = null,
        receiptNumber: String = ""
    ) {
        viewModelScope.launch {
            if (!hasPermission(AppPermissions.ADD_TRANSACTION)) return@launch
            val effectiveCurrency = _defaultCurrency.value.ifBlank { if (currency.isNotBlank()) currency.trim() else "USD" }
            val tx = TransactionEntity(
                userEmail = _userEmail.value,
                accountId = accountId,
                type = type,
                amount = amount,
                currency = effectiveCurrency,
                description = description.trim(),
                date = date,
                dueDate = dueDate,
                receiptNumber = receiptNumber.trim()
            )
            val generatedId = repository.addTransaction(tx)
            val fullTx = tx.copy(id = generatedId)

            // Real-time Push to Firebase Firestore so customer sees it instantly
            try {
                val targetAccount = repository.getAccountById(accountId).firstOrNull()
                if (targetAccount != null) {
                    com.example.util.FirebaseSyncManager.pushTransaction(
                        account = targetAccount,
                        transaction = fullTx,
                        storeName = _storeName.value,
                        merchantEmail = _userEmail.value
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteTransaction(transactionId: Long, accountId: Long = 0L, receiptNumber: String = "") {
        viewModelScope.launch {
            val targetAccId = if (accountId > 0) accountId else (_selectedAccountId.value ?: 0L)
            val targetAccount = if (targetAccId > 0) repository.getAccountById(targetAccId).firstOrNull() else null
            repository.deleteTransaction(transactionId)
            if (targetAccount != null) {
                com.example.util.FirebaseSyncManager.deleteTransaction(targetAccount, transactionId, receiptNumber, _userEmail.value)
            }
        }
    }

    fun addBatchTransactions(
        accountIds: List<Long>,
        type: String, // "LANA" or "LAHO"
        amount: Double,
        currency: String = "",
        description: String,
        date: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val effectiveCurrency = _defaultCurrency.value.ifBlank { if (currency.isNotBlank()) currency.trim() else "USD" }
            accountIds.forEach { accId ->
                val tx = TransactionEntity(
                    userEmail = _userEmail.value,
                    accountId = accId,
                    type = type,
                    amount = amount,
                    currency = effectiveCurrency,
                    description = description.trim(),
                    date = date
                )
                repository.addTransaction(tx)
            }
        }
    }

    fun reseedDemoData() {
        viewModelScope.launch {
            repository.reseedDemoData(_userEmail.value)
        }
    }

    fun exportAccountExcel(context: Context, accountId: Long) {
        viewModelScope.launch {
            repository.getAccountById(accountId).firstOrNull()?.let { account ->
                val txList = repository.getTransactionsForAccount(accountId).firstOrNull() ?: emptyList()
                ExcelExportHelper.exportAccountToExcel(context, account, txList)
            }
        }
    }

    fun exportTopPurchasingExcel(context: Context, items: List<TopPurchaserItem>) {
        ExcelExportHelper.exportTopPurchasingAccountsToExcel(context, items, _storeName.value)
    }

    fun exportOverdueAccountsExcel(context: Context, items: List<OverdueAccountItem>) {
        ExcelExportHelper.exportOverdueAccountsToExcel(context, items, _storeName.value)
    }

    fun deduplicateAccount(accountId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deduplicateTransactionsForAccount(accountId)
            } catch (e: Exception) {
                android.util.Log.e("TawthiqViewModel", "Error deduplicating account $accountId", e)
            }
        }
    }
}
