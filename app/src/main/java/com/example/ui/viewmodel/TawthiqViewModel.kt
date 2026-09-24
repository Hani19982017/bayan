package com.example.ui.viewmodel
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.AccountWithBalance
import com.example.data.model.AdminUserAccount
import com.example.data.model.AppPermissions
import com.example.data.model.CurrencyBalance
import com.example.data.model.DueItem
import com.example.data.model.OverallSummary
import com.example.data.model.OverdueAccountItem
import com.example.data.model.PaymentMethodConfig
import com.example.data.model.PlanPricing
import com.example.data.model.StaffSession
import com.example.data.model.StaffUser
import com.example.data.model.SubscriptionInfo
import com.example.data.model.SubscriptionPaymentRequest
import com.example.data.model.SupportTicket
import com.example.data.model.SystemBroadcastMessage
import com.example.data.model.TopPurchaserItem
import com.example.data.model.TransactionEntity
import com.example.data.repository.TawthiqRepository
import com.example.ui.theme.AppThemeMode
import com.example.util.ExcelExportHelper
import com.example.util.TawthiqNotificationManager
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.DocumentChange
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject



suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener { exception ->
        if (cont.isActive) cont.resumeWithException(exception)
    }
    addOnCanceledListener {
        if (cont.isActive) cont.cancel()
    }
}


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
    private val isFreshInstallSession = !prefs.getBoolean("auth_v2_initialized", false) && prefs.getString("user_email", "").isNullOrBlank()

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

        // Sync staff user to Firestore database immediately
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = FirebaseFirestore.getInstance()
                val cleanMerchant = email.trim().lowercase().replace(".", "_").replace("@", "_")
                val cleanName = user.name.trim().lowercase().replace(" ", "_")
                val staffEmail = if (user.phone.isNotBlank()) "${user.phone}@staff.tawthiq.app" else "$cleanName@tawthiq.app"

                val staffDoc = hashMapOf<String, Any>(
                    "id" to user.id,
                    "name" to user.name,
                    "phone" to user.phone,
                    "role" to user.role,
                    "permissionType" to user.permissionType,
                    "permissions" to user.permissions,
                    "avatarUri" to user.avatarUri,
                    "merchantEmail" to email,
                    "storeName" to _storeName.value,
                    "createdAt" to System.currentTimeMillis()
                )
                db.collection("store_staff").document("staff_${cleanMerchant}_${user.id}").set(staffDoc, SetOptions.merge())
                db.collection("store_staff").document(cleanName).set(staffDoc, SetOptions.merge())

                // Also register as AdminUserAccount in system_admin_users so Admin sees them live
                val adminUserDoc = hashMapOf<String, Any>(
                    "id" to user.id,
                    "email" to staffEmail,
                    "username" to cleanName,
                    "merchantName" to user.name,
                    "storeName" to _storeName.value,
                    "phone" to user.phone,
                    "password" to "123456",
                    "status" to "ACTIVE",
                    "plan" to "موظف (${user.role})",
                    "registeredAt" to System.currentTimeMillis(),
                    "subscriptionStart" to System.currentTimeMillis(),
                    "subscriptionExpiry" to (System.currentTimeMillis() + 365L * 86400000L),
                    "lastActive" to System.currentTimeMillis(),
                    "notes" to "موظف مضاف من متجر ${_storeName.value}"
                )
                db.collection("system_admin_users").document("staff_${cleanMerchant}_${cleanName}").set(adminUserDoc, SetOptions.merge())
                db.collection("system_admin_users").document(cleanName).set(adminUserDoc, SetOptions.merge())
                db.collection("system_admin_users").document(staffEmail.replace(".", "_").replace("@", "_")).set(adminUserDoc, SetOptions.merge())

                mergeAndPublishAdminUsers()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = FirebaseFirestore.getInstance()
                val cleanMerchant = email.trim().lowercase().replace(".", "_").replace("@", "_")
                val cleanName = user.name.trim().lowercase().replace(" ", "_")
                val staffDoc = hashMapOf<String, Any>(
                    "id" to user.id,
                    "name" to user.name,
                    "phone" to user.phone,
                    "role" to user.role,
                    "permissionType" to user.permissionType,
                    "permissions" to user.permissions,
                    "avatarUri" to user.avatarUri,
                    "updatedAt" to System.currentTimeMillis()
                )
                db.collection("store_staff").document("staff_${cleanMerchant}_${user.id}").set(staffDoc, SetOptions.merge())
                db.collection("store_staff").document(cleanName).set(staffDoc, SetOptions.merge())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun removeStaffUser(userId: String) {
        val email = _userEmail.value
        val updated = _staffUsers.value.filter { it.id != userId }
        _staffUsers.value = updated
        saveStaffUsers(email, updated)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = FirebaseFirestore.getInstance()
                val cleanMerchant = email.trim().lowercase().replace(".", "_").replace("@", "_")
                db.collection("store_staff").document("staff_${cleanMerchant}_${userId}").delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // =========================================================================
    // Admin Dashboard & Subscription Management (100% Real Persistence & Sync)
    // =========================================================================

    private val fakeEmails = setOf(
        "admin@bayan.app",
        "ahmed.trader@gmail.com",
        "samer.store@gmail.com",
        "nour.fashion@gmail.com",
        "khalid.tech@gmail.com"
    )

    private fun loadSavedAdminUsers(): List<AdminUserAccount> {
        val raw = prefs.getString("admin_user_accounts_json", "") ?: ""
        if (raw.isNotBlank()) {
            try {
                val array = JSONArray(raw)
                val list = mutableListOf<AdminUserAccount>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val email = obj.optString("email", "").trim().lowercase()
                    if (email.isNotBlank() && email !in fakeEmails) {
                        list.add(
                            AdminUserAccount(
                                id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                                email = email,
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
                }
                return list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return emptyList()
    }

    private fun saveAdminUsersInternal(list: List<AdminUserAccount>) {
        try {
            val cleanList = list.filterNot { it.email.trim().lowercase() in fakeEmails }
            val array = JSONArray()
            for (item in cleanList) {
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

            // Asynchronous Firestore sync (never block caller thread)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val db = FirebaseFirestore.getInstance()
                    for (item in cleanList) {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Requirement 2: Activate, Suspend, Ban, or Delete Account
     */
    fun updateUserStatus(userEmail: String, newStatus: String) {
        val clean = userEmail.trim().lowercase()
        val cleanId = clean.replace(".", "_").replace("@", "_")
        val cleanUser = clean.substringBefore("@")
        val currentList = _adminUserAccounts.value
        val updated = currentList.map {
            if (it.email.equals(clean, ignoreCase = true) || it.email.substringBefore("@").equals(cleanUser, ignoreCase = true)) {
                it.copy(status = newStatus)
            } else it
        }
        _adminUserAccounts.value = updated
        saveAdminUsersInternal(updated)
        prefs.edit().putString("account_status_$clean", newStatus).apply()
        prefs.edit().putString("account_status_$cleanUser", newStatus).apply()
        if (clean.equals(_userEmail.value.trim().lowercase(), ignoreCase = true) ||
            cleanUser.equals(_userEmail.value.trim().lowercase().substringBefore("@"), ignoreCase = true)) {
            _currentUserAccountStatus.value = newStatus
        }

        // Direct real-time write to Firestore collections under cleanId, clean, and cleanUser
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = FirebaseFirestore.getInstance()
                val data = hashMapOf<String, Any>(
                    "status" to newStatus,
                    "updatedAt" to System.currentTimeMillis()
                )
                db.collection("system_admin_users").document(cleanId).set(data, SetOptions.merge())
                db.collection("system_admin_users").document(clean).set(data, SetOptions.merge())
                db.collection("system_admin_users").document(cleanUser).set(data, SetOptions.merge())
                db.collection("user_profiles").document(cleanId).set(data, SetOptions.merge())
                db.collection("user_profiles").document(clean).set(data, SetOptions.merge())
                db.collection("user_profiles").document(cleanUser).set(data, SetOptions.merge())
                db.collection("account_status_updates").document(cleanId).set(data, SetOptions.merge())
                db.collection("account_status_updates").document(clean).set(data, SetOptions.merge())
                db.collection("account_status_updates").document(cleanUser).set(data, SetOptions.merge())

                // Direct notification push to user
                val statusTitle = when (newStatus) {
                    "ACTIVE" -> "تم تفعيل حسابك بنجاح ✓"
                    "SUSPENDED" -> "تنبيه: تم إيقاف حسابك مؤقتاً ⏸"
                    "BANNED" -> "تنبيه: تم حظر هذا الحساب 🚫"
                    else -> "تحديث حالة الحساب: $newStatus"
                }
                val statusMsg = when (newStatus) {
                    "ACTIVE" -> "مرحباً بك، حسابك الآن نشط وتعمل كافة ميزاته بكفاءة."
                    "SUSPENDED" -> "تم إيقاف الخدمة مؤقتاً من قِبل الإدارة، يرجى التواصل مع الدعم."
                    "BANNED" -> "تم حظر الحساب لمخالفة الشروط أو انتهاء صلاحية الوصول."
                    else -> "تم تعديل حالة حسابك إلى: $newStatus"
                }
                val msgDoc = hashMapOf<String, Any>(
                    "id" to "status_${System.currentTimeMillis()}",
                    "title" to statusTitle,
                    "message" to statusMsg,
                    "sender" to "إدارة تطبيق البيان",
                    "targetEmail" to clean,
                    "sentAt" to System.currentTimeMillis()
                )
                db.collection("user_notifications").document(cleanId)
                    .collection("messages").document(msgDoc["id"] as String).set(msgDoc, SetOptions.merge())
                db.collection("user_notifications").document(clean)
                    .collection("messages").document(msgDoc["id"] as String).set(msgDoc, SetOptions.merge())
                db.collection("user_notifications").document(cleanUser)
                    .collection("messages").document(msgDoc["id"] as String).set(msgDoc, SetOptions.merge())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Requirement: Admin adds a new user directly with Strict Unique Email validation
     */
    fun createAdminUser(
        emailOrUsername: String,
        merchantName: String,
        storeName: String,
        phone: String,
        password: String = "123456",
        status: String = "ACTIVE",
        plan: String = "مجاني",
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val clean = com.example.util.MerchantAuthService.normalizeEmail(emailOrUsername)
        val effectiveEmail = if (clean.contains("@")) clean else "$clean@tawthiq.app"
        
        // Strict uniqueness check on current in-memory admin list
        val currentList = _adminUserAccounts.value
        val existsLocally = currentList.any { it.email.equals(effectiveEmail, ignoreCase = true) }
        if (existsLocally) {
            onResult(false, com.example.util.MerchantAuthService.ERR_DUPLICATE_EMAIL)
            return
        }

        val cleanId = effectiveEmail.replace(".", "_").replace("@", "_")
        val cleanUser = clean.substringBefore("@")
        val derivedMerchant = merchantName.ifBlank { cleanUser }
        val derivedStore = storeName.ifBlank { "متجر $derivedMerchant" }

        val newUser = AdminUserAccount(
            id = "usr_${Math.abs(clean.hashCode()) % 100000}",
            email = effectiveEmail,
            storeName = derivedStore,
            merchantName = derivedMerchant,
            phone = phone,
            password = password.ifBlank { "123456" },
            status = status,
            plan = plan,
            registeredAt = System.currentTimeMillis(),
            subscriptionStart = System.currentTimeMillis(),
            subscriptionExpiry = System.currentTimeMillis() + 30L * 86400000L,
            totalAccountsCount = 0,
            totalVolume = 0.0,
            notes = "تم إنشاء الحساب بواسطة الإدارة"
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = FirebaseFirestore.getInstance()
                val emailDocId = com.example.util.MerchantAuthService.emailToDocId(effectiveEmail)
                val uniqueEmailRef = db.collection("merchant_unique_emails").document(emailDocId)
                val userDocRef = db.collection("system_admin_users").document(cleanId)

                // Atomic transaction to enforce server-side uniqueness
                val txTask = db.runTransaction { tx ->
                    val emailSnap = tx.get(uniqueEmailRef)
                    val userSnap = tx.get(userDocRef)

                    if (emailSnap.exists()) {
                        val snapStatus = emailSnap.getString("status") ?: "ACTIVE"
                        if (snapStatus.equals("DELETED", ignoreCase = true)) {
                            throw IllegalStateException(com.example.util.MerchantAuthService.ERR_DELETED_ACCOUNT)
                        }
                        throw IllegalArgumentException(com.example.util.MerchantAuthService.ERR_DUPLICATE_EMAIL)
                    }
                    if (userSnap.exists()) {
                        throw IllegalArgumentException(com.example.util.MerchantAuthService.ERR_DUPLICATE_EMAIL)
                    }

                    val regData = hashMapOf<String, Any>(
                        "email" to effectiveEmail,
                        "merchantId" to newUser.id,
                        "storeName" to derivedStore,
                        "merchantName" to derivedMerchant,
                        "phone" to phone,
                        "registeredAt" to newUser.registeredAt,
                        "status" to status
                    )
                    tx.set(uniqueEmailRef, regData)

                    val docData = hashMapOf<String, Any>(
                        "id" to newUser.id,
                        "email" to effectiveEmail,
                        "username" to cleanUser,
                        "storeName" to derivedStore,
                        "merchantName" to derivedMerchant,
                        "phone" to phone,
                        "password" to newUser.password,
                        "status" to status,
                        "plan" to plan,
                        "registeredAt" to newUser.registeredAt,
                        "subscriptionStart" to newUser.subscriptionStart,
                        "subscriptionExpiry" to newUser.subscriptionExpiry,
                        "lastActive" to System.currentTimeMillis(),
                        "notes" to newUser.notes
                    )
                    tx.set(userDocRef, docData)
                }
                Tasks.await(txTask)

                withContext(Dispatchers.Main) {
                    val updated = listOf(newUser) + currentList
                    _adminUserAccounts.value = updated
                    saveAdminUsersInternal(updated)
                    onResult(true, "تم إنشاء الحساب بنجاح ✓")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val msg = when {
                        e is IllegalArgumentException || e.message?.contains("duplicate", true) == true -> 
                            com.example.util.MerchantAuthService.ERR_DUPLICATE_EMAIL
                        e is IllegalStateException -> 
                            com.example.util.MerchantAuthService.ERR_DELETED_ACCOUNT
                        else -> 
                            e.localizedMessage ?: "حدث خطأ أثناء إضافة الحساب"
                    }
                    onResult(false, msg)
                }
            }
        }
    }

    fun deleteAdminUser(userEmail: String) {
        val clean = com.example.util.MerchantAuthService.normalizeEmail(userEmail)
        val cleanId = clean.replace(".", "_").replace("@", "_")
        val currentList = _adminUserAccounts.value
        val updated = currentList.filterNot { it.email.equals(clean, ignoreCase = true) }
        _adminUserAccounts.value = updated
        saveAdminUsersInternal(updated)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Mark deleted in unique registry so it cannot be accidentally recreated
                com.example.util.MerchantAuthService.markAccountDeleted(clean)
                val db = FirebaseFirestore.getInstance()
                db.collection("system_admin_users").document(cleanId).delete()
                db.collection("system_admin_users").document(clean).delete()
                db.collection("user_profiles").document(cleanId).delete()
                db.collection("user_profiles").document(clean).delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Requirement 6: Change user password for account recovery assistance
     */
    fun updateUserPassword(userEmail: String, newPassword: String) {
        val clean = userEmail.trim().lowercase()
        val cleanId = clean.replace(".", "_").replace("@", "_")
        val currentList = _adminUserAccounts.value
        val updated = currentList.map {
            if (it.email.equals(clean, ignoreCase = true)) it.copy(password = newPassword.trim()) else it
        }
        _adminUserAccounts.value = updated
        saveAdminUsersInternal(updated)
        prefs.edit().putString("user_pwd_$clean", newPassword.trim()).apply()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = FirebaseFirestore.getInstance()
                val data = hashMapOf<String, Any>(
                    "password" to newPassword.trim(),
                    "updatedAt" to System.currentTimeMillis()
                )
                db.collection("system_admin_users").document(cleanId).set(data, SetOptions.merge())
                db.collection("system_admin_users").document(clean).set(data, SetOptions.merge())
                db.collection("user_profiles").document(cleanId).set(data, SetOptions.merge())
                db.collection("user_profiles").document(clean).set(data, SetOptions.merge())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Requirement 7: Manage Subscriptions (مجاني، أسبوعي، شهري، سنوي) & Expiry Date
     */
    fun updateUserSubscription(userEmail: String, newPlan: String, durationDays: Int) {
        val clean = userEmail.trim().lowercase()
        val cleanId = clean.replace(".", "_").replace("@", "_")
        val now = System.currentTimeMillis()
        val expiryTime = now + durationDays.toLong() * 86400000L
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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = FirebaseFirestore.getInstance()
                val data = hashMapOf<String, Any>(
                    "plan" to newPlan,
                    "subscriptionStart" to now,
                    "subscriptionExpiry" to expiryTime,
                    "isPro" to isPro,
                    "updatedAt" to System.currentTimeMillis()
                )
                db.collection("system_admin_users").document(cleanId).set(data, SetOptions.merge())
                db.collection("system_admin_users").document(clean).set(data, SetOptions.merge())
                db.collection("user_profiles").document(cleanId).set(data, SetOptions.merge())
                db.collection("user_profiles").document(clean).set(data, SetOptions.merge())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Send direct private notification/message to a specific user
     */
    fun sendDirectMessageToUser(targetEmail: String, title: String, message: String, context: Context) {
        val clean = targetEmail.trim().lowercase()
        val cleanId = clean.replace(".", "_").replace("@", "_")
        val msgId = "msg_${System.currentTimeMillis()}_${(1000..9999).random()}"
        val newMsg = SystemBroadcastMessage(
            id = msgId,
            title = title.trim(),
            message = message.trim(),
            sender = "إدارة تطبيق البيان (رسالة خاصة)",
            sentAt = System.currentTimeMillis()
        )
        val updated = listOf(newMsg) + _systemBroadcasts.value
        _systemBroadcasts.value = updated
        saveBroadcastMessagesInternal(updated)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = FirebaseFirestore.getInstance()
                val doc = hashMapOf<String, Any>(
                    "id" to newMsg.id,
                    "title" to newMsg.title,
                    "message" to newMsg.message,
                    "sender" to newMsg.sender,
                    "targetEmail" to clean,
                    "sentAt" to newMsg.sentAt
                )
                db.collection("system_broadcasts").document(newMsg.id).set(doc, SetOptions.merge())
                db.collection("user_notifications").document(cleanId)
                    .collection("messages").document(newMsg.id).set(doc, SetOptions.merge())
                db.collection("user_notifications").document(clean)
                    .collection("messages").document(newMsg.id).set(doc, SetOptions.merge())
                val cleanUser = clean.substringBefore("@")
                if (cleanUser.isNotBlank()) {
                    db.collection("user_notifications").document(cleanUser)
                        .collection("messages").document(newMsg.id).set(doc, SetOptions.merge())
                    db.collection("user_direct_messages").document(cleanUser)
                        .collection("inbox").document(newMsg.id).set(doc, SetOptions.merge())
                }
                db.collection("user_direct_messages").document(cleanId)
                    .collection("inbox").document(newMsg.id).set(doc, SetOptions.merge())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        TawthiqNotificationManager.sendAdminBroadcastNotification(
            context = context,
            title = title.trim(),
            message = message.trim()
        )
    }

    /**
     * Automatic Complete Cloud Synchronization:
     * Backs up and syncs ALL user accounts and their transactions to Firestore.
     */
    fun syncAllUserAccountsAndTransactionsToCloud() {
        val email = _userEmail.value.trim().lowercase()
        if (email.isBlank()) return
        val cleanId = email.replace(".", "_").replace("@", "_")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = FirebaseFirestore.getInstance()
                val allAccounts = repository.getAllAccountsSnapshot()
                var totalVolume = 0.0

                for (acc in allAccounts) {
                    val effectiveSyncKey = if (acc.syncKey.isNotBlank()) acc.syncKey else "tw_${cleanId}_acc_${acc.id}"
                    if (acc.userEmail.isBlank() || acc.syncKey.isBlank()) {
                        repository.updateAccount(acc.copy(userEmail = email, syncKey = effectiveSyncKey))
                    }

                    val txs = repository.getTransactionsSnapshotForAccount(acc.id)
                    totalVolume += txs.sumOf { it.amount }

                    // 1. live_statements
                    val statementDoc = hashMapOf<String, Any>(
                        "syncKey" to effectiveSyncKey,
                        "accountName" to acc.name,
                        "storeName" to _storeName.value,
                        "merchantEmail" to email,
                        "userEmail" to email,
                        "phone" to acc.phone,
                        "currency" to acc.currency,
                        "transactionCount" to txs.size,
                        "lastUpdated" to System.currentTimeMillis()
                    )
                    db.collection("live_statements").document(effectiveSyncKey).set(statementDoc, SetOptions.merge())

                    for (tx in txs) {
                        val txDocId = if (tx.id > 0) tx.id.toString() else "tx_${tx.date}_${Math.abs(tx.amount.hashCode())}"
                        val txMap = hashMapOf<String, Any>(
                            "id" to tx.id,
                            "accountId" to acc.id,
                            "syncKey" to effectiveSyncKey,
                            "userEmail" to email,
                            "merchantEmail" to email,
                            "accountName" to acc.name,
                            "storeName" to _storeName.value,
                            "type" to tx.type,
                            "amount" to tx.amount,
                            "currency" to (if (tx.currency.isNotBlank()) tx.currency else acc.currency),
                            "description" to tx.description,
                            "date" to tx.date,
                            "dueDate" to (tx.dueDate ?: 0L),
                            "receiptNumber" to tx.receiptNumber,
                            "isSettled" to tx.isSettled,
                            "updatedAt" to System.currentTimeMillis()
                        )
                        db.collection("live_statements").document(effectiveSyncKey)
                            .collection("transactions").document(txDocId).set(txMap, SetOptions.merge())
                        db.collection("transactions").document("${cleanId}_${acc.id}_${txDocId}").set(txMap, SetOptions.merge())
                    }

                    // 2. user_merchant_ledgers
                    val ledgerAcc = hashMapOf<String, Any>(
                        "accountId" to acc.id,
                        "accountName" to acc.name,
                        "phone" to acc.phone,
                        "currency" to acc.currency,
                        "userEmail" to email,
                        "syncKey" to effectiveSyncKey,
                        "transactionCount" to txs.size,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    db.collection("user_merchant_ledgers").document(cleanId)
                        .collection("accounts").document(acc.id.toString()).set(ledgerAcc, SetOptions.merge())
                    db.collection("user_merchant_ledgers").document(email)
                        .collection("accounts").document(acc.id.toString()).set(ledgerAcc, SetOptions.merge())

                    for (tx in txs) {
                        val txDocId = if (tx.id > 0) tx.id.toString() else "tx_${tx.date}"
                        val txMap = hashMapOf<String, Any>(
                            "id" to tx.id,
                            "accountId" to acc.id,
                            "userEmail" to email,
                            "merchantEmail" to email,
                            "accountName" to acc.name,
                            "type" to tx.type,
                            "amount" to tx.amount,
                            "currency" to (if (tx.currency.isNotBlank()) tx.currency else acc.currency),
                            "description" to tx.description,
                            "date" to tx.date,
                            "receiptNumber" to tx.receiptNumber,
                            "isSettled" to tx.isSettled
                        )
                        db.collection("user_merchant_ledgers").document(cleanId)
                            .collection("accounts").document(acc.id.toString())
                            .collection("transactions").document(txDocId).set(txMap, SetOptions.merge())
                        db.collection("user_merchant_ledgers").document(email)
                            .collection("accounts").document(acc.id.toString())
                            .collection("transactions").document(txDocId).set(txMap, SetOptions.merge())
                    }
                }

                // Update summary on system_admin_users
                val userSummary = hashMapOf<String, Any>(
                    "totalAccountsCount" to allAccounts.size,
                    "totalVolume" to totalVolume,
                    "lastActive" to System.currentTimeMillis()
                )
                db.collection("system_admin_users").document(cleanId).set(userSummary, SetOptions.merge())
                db.collection("system_admin_users").document(email).set(userSummary, SetOptions.merge())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Requirement 3: View Customer Accounts and Ledger Transactions for any Merchant
     * Synchronizes live from Firestore collections and local Room database.
     */
    fun getCustomersAndTransactionsForUser(
        userEmail: String,
        onResult: (List<Pair<AccountEntity, List<TransactionEntity>>>) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanEmail = userEmail.trim().lowercase()
            val cleanEmailKey = cleanEmail.replace(".", "_").replace("@", "_")
            val resultList = mutableListOf<Pair<AccountEntity, List<TransactionEntity>>>()

            // 1. Check local Room DB first
            try {
                val localAccounts = repository.getAccountsForUserSnapshot(cleanEmail)
                val allAccounts = repository.getAllAccountsSnapshot()
                val targetAccounts = (localAccounts + allAccounts.filter {
                    it.userEmail.equals(cleanEmail, ignoreCase = true) ||
                    cleanEmail.contains(it.userEmail) ||
                    (it.userEmail.isBlank() && _userEmail.value.trim().lowercase() == cleanEmail)
                }).distinctBy { it.id }

                for (acc in targetAccounts) {
                    val txs = repository.getTransactionsSnapshotForAccount(acc.id)
                    resultList.add(Pair(acc, txs))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // If we found local accounts, deliver them immediately to UI so user sees them fast
            if (resultList.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    onResult(resultList.toList())
                }
            }

            // 2. Fetch all cloud accounts and transactions from Firestore
            try {
                val db = FirebaseFirestore.getInstance()

                // Source A: user_merchant_ledgers
                val ledgerDocIds = listOf(cleanEmailKey, cleanEmail)
                for (docId in ledgerDocIds) {
                    try {
                        val accSnap = db.collection("user_merchant_ledgers").document(docId)
                            .collection("accounts").get().awaitTask()
                        for (doc in accSnap.documents) {
                            val accId = doc.getLong("accountId") ?: Math.abs(doc.id.hashCode()).toLong()
                            val accName = doc.getString("accountName") ?: "زبون"
                            val phone = doc.getString("phone") ?: ""
                            val currency = doc.getString("currency") ?: "USD"
                            val syncKey = doc.getString("syncKey") ?: ""
                            val txList = mutableListOf<TransactionEntity>()

                            try {
                                val txSnap = db.collection("user_merchant_ledgers").document(docId)
                                    .collection("accounts").document(doc.id)
                                    .collection("transactions").get().awaitTask()
                                for (txDoc in txSnap.documents) {
                                    txList.add(
                                        TransactionEntity(
                                            id = txDoc.getLong("id") ?: Math.abs(txDoc.id.hashCode()).toLong(),
                                            userEmail = cleanEmail,
                                            accountId = accId,
                                            type = txDoc.getString("type") ?: "LANA",
                                            amount = txDoc.getDouble("amount") ?: 0.0,
                                            currency = txDoc.getString("currency") ?: currency,
                                            description = txDoc.getString("description") ?: "",
                                            date = txDoc.getLong("date") ?: System.currentTimeMillis(),
                                            dueDate = txDoc.getLong("dueDate"),
                                            receiptNumber = txDoc.getString("receiptNumber") ?: "",
                                            isSettled = txDoc.getBoolean("isSettled") ?: false
                                        )
                                    )
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            val virtualAccount = AccountEntity(
                                id = accId,
                                userEmail = cleanEmail,
                                name = accName,
                                phone = phone,
                                category = "زبون",
                                currency = currency,
                                notes = "سجل سحابي موثق",
                                syncKey = syncKey
                            )
                            val existingIdx = resultList.indexOfFirst { it.first.name.equals(accName, ignoreCase = true) || it.first.id == accId }
                            if (existingIdx >= 0) {
                                val mergedTxs = (resultList[existingIdx].second + txList).distinctBy { it.id }
                                resultList[existingIdx] = Pair(resultList[existingIdx].first, mergedTxs.sortedByDescending { it.date })
                            } else {
                                resultList.add(Pair(virtualAccount, txList.sortedByDescending { it.date }))
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Source B: live_statements
                try {
                    val statementsSnap = db.collection("live_statements").get().awaitTask()
                    for (doc in statementsSnap.documents) {
                        val mEmail = doc.getString("merchantEmail")?.trim()?.lowercase()
                            ?: doc.getString("userEmail")?.trim()?.lowercase() ?: ""
                        val syncKey = doc.getString("syncKey") ?: doc.id
                        val isMatch = (mEmail.isNotBlank() && (
                            mEmail == cleanEmail ||
                            cleanEmail.contains(mEmail) ||
                            mEmail.contains(cleanEmail) ||
                            syncKey.contains(cleanEmailKey)
                        )) || syncKey.contains(cleanEmailKey) || syncKey.contains(cleanEmail)

                        if (isMatch) {
                            val accName = doc.getString("accountName") ?: "عميل"
                            val currency = doc.getString("currency") ?: "USD"
                            val storeName = doc.getString("storeName") ?: ""
                            val phone = doc.getString("phone") ?: ""
                            val txList = mutableListOf<TransactionEntity>()

                            try {
                                val subSnap = db.collection("live_statements").document(syncKey)
                                    .collection("transactions").get().awaitTask()
                                for (txDoc in subSnap.documents) {
                                    txList.add(
                                        TransactionEntity(
                                            id = txDoc.getLong("id") ?: Math.abs(txDoc.id.hashCode()).toLong(),
                                            userEmail = cleanEmail,
                                            accountId = Math.abs(syncKey.hashCode()).toLong(),
                                            type = txDoc.getString("type") ?: "LANA",
                                            amount = txDoc.getDouble("amount") ?: 0.0,
                                            currency = txDoc.getString("currency") ?: currency,
                                            description = txDoc.getString("description") ?: "",
                                            date = txDoc.getLong("date") ?: System.currentTimeMillis(),
                                            dueDate = txDoc.getLong("dueDate"),
                                            receiptNumber = txDoc.getString("receiptNumber") ?: "",
                                            isSettled = txDoc.getBoolean("isSettled") ?: false
                                        )
                                    )
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            val virtualAccount = AccountEntity(
                                id = Math.abs(syncKey.hashCode()).toLong(),
                                userEmail = cleanEmail,
                                name = accName,
                                phone = phone,
                                category = "زبون",
                                currency = currency,
                                notes = "متجر: $storeName",
                                syncKey = syncKey
                            )
                            val idx = resultList.indexOfFirst { it.first.name.equals(accName, ignoreCase = true) }
                            if (idx >= 0) {
                                val merged = (resultList[idx].second + txList).distinctBy { it.id }
                                resultList[idx] = Pair(resultList[idx].first, merged.sortedByDescending { it.date })
                            } else {
                                resultList.add(Pair(virtualAccount, txList.sortedByDescending { it.date }))
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Source C: root transactions collection
                try {
                    val rootSnap = db.collection("transactions").get().awaitTask()
                    for (doc in rootSnap.documents) {
                        val uEmail = doc.getString("userEmail")?.trim()?.lowercase()
                            ?: doc.getString("merchantEmail")?.trim()?.lowercase() ?: ""
                        val syncKey = doc.getString("syncKey") ?: ""
                        val isMatch = (uEmail.isNotBlank() && (uEmail == cleanEmail || uEmail.contains(cleanEmail) || cleanEmail.contains(uEmail))) ||
                                      (syncKey.isNotBlank() && (syncKey.contains(cleanEmailKey) || syncKey.contains(cleanEmail)))

                        if (isMatch) {
                            val accName = doc.getString("accountName") ?: "عميل"
                            val tx = TransactionEntity(
                                id = doc.getLong("id") ?: Math.abs(doc.id.hashCode()).toLong(),
                                userEmail = cleanEmail,
                                accountId = Math.abs(syncKey.hashCode()).toLong(),
                                type = doc.getString("type") ?: "LANA",
                                amount = doc.getDouble("amount") ?: 0.0,
                                currency = doc.getString("currency") ?: "USD",
                                description = doc.getString("description") ?: "",
                                date = doc.getLong("date") ?: System.currentTimeMillis(),
                                dueDate = doc.getLong("dueDate"),
                                receiptNumber = doc.getString("receiptNumber") ?: "",
                                isSettled = doc.getBoolean("isSettled") ?: false
                            )
                            val existingPair = resultList.find { it.first.name.equals(accName, ignoreCase = true) }
                            if (existingPair != null) {
                                if (existingPair.second.none { it.id == tx.id || (it.date == tx.date && it.amount == tx.amount) }) {
                                    val updatedList = (existingPair.second + tx).sortedByDescending { it.date }
                                    val idx = resultList.indexOf(existingPair)
                                    resultList[idx] = Pair(existingPair.first, updatedList)
                                }
                            } else {
                                val newAcc = AccountEntity(
                                    id = Math.abs(accName.hashCode()).toLong(),
                                    userEmail = cleanEmail,
                                    name = accName,
                                    phone = "",
                                    category = "زبون",
                                    currency = tx.currency,
                                    notes = "سجل سحابي",
                                    syncKey = syncKey
                                )
                                resultList.add(Pair(newAcc, listOf(tx)))
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Final delivery on Main thread
                withContext(Dispatchers.Main) {
                    onResult(resultList.toList())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(resultList.toList())
                }
            }
        }
    }

    private val cloudAdminUsers = mutableMapOf<String, AdminUserAccount>()
    private val cloudUniqueMerchants = mutableMapOf<String, AdminUserAccount>()
    private val cloudStatementUsers = mutableMapOf<String, AdminUserAccount>()
    private var adminUsersListenerReg: ListenerRegistration? = null
    private var uniqueMerchantsListenerReg: ListenerRegistration? = null
    private var statementsListenerReg: ListenerRegistration? = null
    private var subRequestsListenerReg: ListenerRegistration? = null

    fun syncAdminDataFromCloud() {
        // Seed from saved real users first
        val saved = loadSavedAdminUsers()
        if (saved.isNotEmpty()) {
            _adminUserAccounts.value = saved
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = FirebaseFirestore.getInstance()

                // 1. Real-time listener on system_admin_users
                adminUsersListenerReg?.remove()
                adminUsersListenerReg = db.collection("system_admin_users").addSnapshotListener { snapshot, e ->
                    if (e != null || snapshot == null) return@addSnapshotListener
                    synchronized(cloudAdminUsers) {
                        cloudAdminUsers.clear()
                        for (doc in snapshot.documents) {
                            val rawEmail = doc.getString("email")?.trim()?.lowercase()
                            val rawUsername = doc.getString("username")?.trim()?.lowercase()
                            val email = when {
                                !rawEmail.isNullOrBlank() -> rawEmail
                                !rawUsername.isNullOrBlank() -> if (rawUsername.contains("@")) rawUsername else "$rawUsername@tawthiq.app"
                                doc.id.isNotBlank() && !doc.id.startsWith("staff_") -> if (doc.id.contains("@")) doc.id else "${doc.id}@tawthiq.app"
                                else -> rawEmail ?: ""
                            }
                            if (email.isNotBlank() && email !in fakeEmails) {
                                val merchantName = doc.getString("merchantName") ?: doc.getString("name") ?: rawUsername ?: email.substringBefore("@")
                                val storeName = doc.getString("storeName") ?: "متجر $merchantName"
                                val phone = doc.getString("phone") ?: ""
                                val password = doc.getString("password") ?: "123456"
                                val status = doc.getString("status") ?: "ACTIVE"
                                val plan = doc.getString("plan") ?: "مجاني"
                                cloudAdminUsers[email] = AdminUserAccount(
                                    id = doc.getString("id") ?: doc.id,
                                    email = email,
                                    storeName = storeName,
                                    merchantName = merchantName,
                                    phone = phone,
                                    password = password,
                                    status = status,
                                    plan = plan,
                                    registeredAt = doc.getLong("registeredAt") ?: System.currentTimeMillis(),
                                    subscriptionStart = doc.getLong("subscriptionStart") ?: System.currentTimeMillis(),
                                    subscriptionExpiry = doc.getLong("subscriptionExpiry") ?: (System.currentTimeMillis() + 30L * 86400000L),
                                    totalAccountsCount = doc.getLong("totalAccountsCount")?.toInt() ?: 0,
                                    totalVolume = doc.getDouble("totalVolume") ?: 0.0,
                                    notes = doc.getString("notes") ?: ""
                                )
                            }
                        }
                    }
                    mergeAndPublishAdminUsers()
                }

                // 2. Real-time listener on merchant_unique_emails
                uniqueMerchantsListenerReg?.remove()
                uniqueMerchantsListenerReg = db.collection("merchant_unique_emails").addSnapshotListener { snapshot, e ->
                    if (e != null || snapshot == null) return@addSnapshotListener
                    synchronized(cloudUniqueMerchants) {
                        cloudUniqueMerchants.clear()
                        for (doc in snapshot.documents) {
                            val email = doc.getString("email")?.trim()?.lowercase() ?: doc.id.trim().lowercase()
                            val status = doc.getString("status") ?: "ACTIVE"
                            if (email.isNotBlank() && email !in fakeEmails && !status.equals("DELETED", ignoreCase = true)) {
                                val merchantName = doc.getString("merchantName") ?: email.substringBefore("@")
                                val storeName = doc.getString("storeName") ?: "متجر $merchantName"
                                val phone = doc.getString("phone") ?: ""
                                cloudUniqueMerchants[email] = AdminUserAccount(
                                    id = doc.getString("merchantId") ?: "usr_${Math.abs(email.hashCode()) % 100000}",
                                    email = email,
                                    storeName = storeName,
                                    merchantName = merchantName,
                                    phone = phone,
                                    password = "••••••••",
                                    status = status,
                                    plan = "مجاني",
                                    registeredAt = doc.getLong("registeredAt") ?: System.currentTimeMillis(),
                                    subscriptionStart = System.currentTimeMillis(),
                                    subscriptionExpiry = System.currentTimeMillis() + 30L * 86400000L,
                                    totalAccountsCount = 0,
                                    totalVolume = 0.0,
                                    notes = "حساب تاجر مسجل في النظام"
                                )
                            }
                        }
                    }
                    mergeAndPublishAdminUsers()
                }

                // 3. Real-time listener on live_statements
                statementsListenerReg?.remove()
                statementsListenerReg = db.collection("live_statements").addSnapshotListener { snapshot, e ->
                    if (e != null || snapshot == null) return@addSnapshotListener
                    synchronized(cloudStatementUsers) {
                        cloudStatementUsers.clear()
                        val statementCounts = mutableMapOf<String, Int>()
                        val merchantStores = mutableMapOf<String, String>()
                        val merchantPhones = mutableMapOf<String, String>()
                        for (doc in snapshot.documents) {
                            val email = doc.getString("merchantEmail")?.trim()?.lowercase()
                                ?: doc.getString("userEmail")?.trim()?.lowercase() ?: ""
                            val stName = doc.getString("storeName") ?: ""
                            val ph = doc.getString("phone") ?: ""
                            if (email.isNotBlank() && email !in fakeEmails) {
                                statementCounts[email] = (statementCounts[email] ?: 0) + 1
                                if (stName.isNotBlank()) merchantStores[email] = stName
                                if (ph.isNotBlank()) merchantPhones[email] = ph
                            }
                        }
                        for ((email, count) in statementCounts) {
                            val stName = merchantStores[email] ?: "متجر ${email.substringBefore("@")}"
                            val phone = merchantPhones[email] ?: ""
                            cloudStatementUsers[email] = AdminUserAccount(
                                id = "usr_${Math.abs(email.hashCode()) % 100000}",
                                email = email,
                                storeName = stName,
                                merchantName = email.substringBefore("@"),
                                phone = phone,
                                password = "••••••••",
                                status = "ACTIVE",
                                plan = "مجاني",
                                registeredAt = System.currentTimeMillis(),
                                subscriptionStart = System.currentTimeMillis(),
                                subscriptionExpiry = System.currentTimeMillis() + 30L * 86400000L,
                                totalAccountsCount = count,
                                totalVolume = 0.0,
                                notes = "حساب نشط مسجل من كشوفات الحسابات السحابية"
                            )
                        }
                    }
                    mergeAndPublishAdminUsers()
                }

                // 4. Real-time listener on subscription_requests
                subRequestsListenerReg?.remove()
                subRequestsListenerReg = db.collection("subscription_requests").addSnapshotListener { snapshot, e ->
                    if (e != null || snapshot == null) return@addSnapshotListener
                    val reqList = mutableListOf<SubscriptionPaymentRequest>()
                    for (doc in snapshot.documents) {
                        reqList.add(
                            SubscriptionPaymentRequest(
                                id = doc.getString("id") ?: doc.id,
                                userEmail = doc.getString("userEmail") ?: "",
                                userName = doc.getString("userName") ?: "",
                                userPhone = doc.getString("userPhone") ?: "",
                                planName = doc.getString("planName") ?: "شهري",
                                planPrice = doc.getString("planPrice") ?: "",
                                paymentMethodName = doc.getString("paymentMethodName") ?: "",
                                transferNumber = doc.getString("transferNumber") ?: "",
                                senderPhone = doc.getString("senderPhone") ?: "",
                                proofImageUri = doc.getString("proofImageUri") ?: "",
                                notes = doc.getString("notes") ?: "",
                                status = doc.getString("status") ?: "PENDING",
                                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                                reviewedAt = doc.getLong("reviewedAt"),
                                adminNotes = doc.getString("adminNotes") ?: ""
                            )
                        )
                    }
                    val sorted = reqList.sortedByDescending { it.createdAt }
                    _subscriptionPaymentRequests.value = sorted
                }

                // Initial merge with local Room database
                mergeAndPublishAdminUsers()
            } catch (e: Exception) {
                android.util.Log.e("TawthiqViewModel", "Error in syncAdminDataFromCloud", e)
            }
        }
    }

    private fun mergeAndPublishAdminUsers() {
        viewModelScope.launch(Dispatchers.IO) {
            val usersMap = mutableMapOf<String, AdminUserAccount>()

            // A. Cached saved users
            for (u in loadSavedAdminUsers()) {
                val key = u.email.trim().lowercase()
                if (key.isNotBlank() && key !in fakeEmails) usersMap[key] = u
            }

            // B. Cloud unique merchants
            synchronized(cloudUniqueMerchants) {
                for ((email, acc) in cloudUniqueMerchants) {
                    val existing = usersMap[email]
                    usersMap[email] = if (existing != null) {
                        existing.copy(
                            phone = if (existing.phone.isBlank()) acc.phone else existing.phone,
                            storeName = if (existing.storeName.isBlank() || existing.storeName.contains("متجر البيان")) acc.storeName else existing.storeName
                        )
                    } else acc
                }
            }

            // C. Cloud admin users (has priority for status, plan, passwords)
            synchronized(cloudAdminUsers) {
                for ((email, acc) in cloudAdminUsers) {
                    val existing = usersMap[email]
                    usersMap[email] = if (existing != null) {
                        existing.copy(
                            status = acc.status,
                            plan = acc.plan,
                            password = if (acc.password.isNotBlank() && acc.password != "123456") acc.password else existing.password,
                            phone = if (acc.phone.isNotBlank()) acc.phone else existing.phone,
                            storeName = if (acc.storeName.isNotBlank() && !acc.storeName.contains("متجر البيان")) acc.storeName else existing.storeName,
                            merchantName = if (acc.merchantName.isNotBlank()) acc.merchantName else existing.merchantName,
                            subscriptionStart = acc.subscriptionStart,
                            subscriptionExpiry = acc.subscriptionExpiry
                        )
                    } else acc
                }
            }

            // D. Statement counts
            synchronized(cloudStatementUsers) {
                for ((email, acc) in cloudStatementUsers) {
                    val existing = usersMap[email]
                    if (existing != null) {
                        usersMap[email] = existing.copy(
                            totalAccountsCount = maxOf(existing.totalAccountsCount, acc.totalAccountsCount)
                        )
                    } else {
                        usersMap[email] = acc
                    }
                }
            }

            // E. Local Room DB
            try {
                val localAccounts = repository.getAllAccountsSnapshot()
                val localGrouped = localAccounts.groupBy { it.userEmail.trim().lowercase() }
                for ((email, accList) in localGrouped) {
                    if (email.isNotBlank() && email !in fakeEmails) {
                        val existing = usersMap[email]
                        val localTxs = repository.getAllTransactionsSnapshot(email)
                        val vol = localTxs.sumOf { it.amount }
                        if (existing != null) {
                            usersMap[email] = existing.copy(
                                totalAccountsCount = maxOf(existing.totalAccountsCount, accList.size),
                                totalVolume = maxOf(existing.totalVolume, vol)
                            )
                        } else {
                            usersMap[email] = AdminUserAccount(
                                id = "usr_${Math.abs(email.hashCode()) % 100000}",
                                email = email,
                                storeName = "متجر ${email.substringBefore("@")}",
                                merchantName = email.substringBefore("@"),
                                phone = "",
                                password = "user123",
                                status = "ACTIVE",
                                plan = "مجاني",
                                registeredAt = System.currentTimeMillis(),
                                subscriptionStart = System.currentTimeMillis(),
                                subscriptionExpiry = System.currentTimeMillis() + 30L * 86400000L,
                                totalAccountsCount = accList.size,
                                totalVolume = vol,
                                notes = "مستخدم مسجل محلياً على هذا الجهاز"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val sortedList = usersMap.values
                .filterNot { it.email in fakeEmails || it.status.equals("DELETED", ignoreCase = true) }
                .sortedWith(
                    compareByDescending<AdminUserAccount> { it.email.contains("family") || it.email.contains("admin") }
                        .thenByDescending { it.registeredAt }
                )

            withContext(Dispatchers.Main) {
                _adminUserAccounts.value = sortedList
            }
            saveAdminUsersInternal(sortedList)
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

        private var paymentMethodsListenerRegistration: ListenerRegistration? = null

    fun listenToPaymentMethods() {
        paymentMethodsListenerRegistration?.remove()
        try {
            val db = FirebaseFirestore.getInstance()
            paymentMethodsListenerRegistration = db.collection("system_payment_methods").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = mutableListOf<PaymentMethodConfig>()
                for (doc in snapshot.documents) {
                    val id = doc.getString("id") ?: doc.id
                    val name = doc.getString("name") ?: ""
                    val accountNumber = doc.getString("accountNumber") ?: ""
                    val accountHolder = doc.getString("accountHolder") ?: ""
                    val instructions = doc.getString("instructions") ?: ""
                    val iconName = doc.getString("iconName") ?: "wallet"
                    val isActive = doc.getBoolean("isActive") ?: true
                    if (name.isNotBlank()) {
                        list.add(PaymentMethodConfig(id, name, accountNumber, accountHolder, instructions, iconName, isActive))
                    }
                }
                if (list.isNotEmpty()) {
                    _paymentMethods.value = list
                    savePaymentMethodsInternal(list)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

        return emptyList()
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
                    val id = obj.optString("id", "")
                    if (id.isNotBlank() && id != "bc_1") {
                        list.add(
                            SystemBroadcastMessage(
                                id = id,
                                title = obj.optString("title", ""),
                                message = obj.optString("message", ""),
                                sender = obj.optString("sender", "إدارة تطبيق البيان"),
                                sentAt = obj.optLong("sentAt", System.currentTimeMillis())
                            )
                        )
                    }
                }
                return list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return emptyList()
    }

    private fun saveBroadcastMessagesInternal(list: List<SystemBroadcastMessage>) {
        try {
            val cleanList = list.filterNot { it.id == "bc_1" }
            val array = JSONArray()
            for (item in cleanList) {
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

            // Asynchronous Firestore sync
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val db = FirebaseFirestore.getInstance()
                    for (item in cleanList) {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendBroadcastMessage(title: String, message: String, context: Context) {
        val newId = "bc_${System.currentTimeMillis()}_${(1000..9999).random()}"
        val newMsg = SystemBroadcastMessage(
            id = newId,
            title = title.trim(),
            message = message.trim(),
            sender = "إدارة تطبيق البيان",
            sentAt = System.currentTimeMillis()
        )
        val updated = (listOf(newMsg) + _systemBroadcasts.value).distinctBy { it.id }
        _systemBroadcasts.value = updated
        saveBroadcastMessagesInternal(updated)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = FirebaseFirestore.getInstance()
                val doc = hashMapOf<String, Any>(
                    "id" to newMsg.id,
                    "title" to newMsg.title,
                    "message" to newMsg.message,
                    "sender" to newMsg.sender,
                    "sentAt" to newMsg.sentAt,
                    "targetEmail" to "all"
                )
                db.collection("system_broadcasts").document(newMsg.id).set(doc, SetOptions.merge())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        TawthiqNotificationManager.sendAdminBroadcastNotification(
            context = context,
            title = title.trim(),
            message = message.trim()
        )
    }

    fun deleteBroadcastMessage(messageId: String) {
        val updated = _systemBroadcasts.value.filterNot { it.id == messageId }
        _systemBroadcasts.value = updated
        saveBroadcastMessagesInternal(updated)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = FirebaseFirestore.getInstance()
                db.collection("system_broadcasts").document(messageId).delete()
                db.collection("user_direct_messages").document(messageId).delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private var broadcastListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    fun listenToSystemBroadcasts() {
        val localSaved = loadSavedBroadcastMessages()
        if (localSaved.isNotEmpty()) {
            _systemBroadcasts.value = localSaved
        }
        broadcastListenerRegistration?.remove()

        try {
            val db = FirebaseFirestore.getInstance()
            broadcastListenerRegistration = db.collection("system_broadcasts").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val currentEmail = _userEmail.value.trim().lowercase()
                val isAdmin = com.example.BuildConfig.IS_ADMIN_APP
                val list = mutableListOf<SystemBroadcastMessage>()
                for (doc in snapshot.documents) {
                    val id = doc.getString("id") ?: doc.id
                    val title = doc.getString("title") ?: ""
                    val msg = doc.getString("message") ?: ""
                    val sender = doc.getString("sender") ?: "إدارة تطبيق البيان"
                    val sentAt = doc.getLong("sentAt") ?: System.currentTimeMillis()
                    val target = doc.getString("targetEmail")?.trim()?.lowercase()

                    val isVisible = isAdmin || target.isNullOrBlank() || target == "all" || (currentEmail.isNotBlank() && target == currentEmail)
                    if (isVisible && (title.isNotBlank() || msg.isNotBlank())) {
                        list.add(SystemBroadcastMessage(id, title, msg, sender, sentAt))
                    }
                }
                val sorted = list.distinctBy { it.id }.sortedByDescending { it.sentAt }
                _systemBroadcasts.value = sorted

                // Trigger notification on user device if there's a new message
                if (!isAdmin && sorted.isNotEmpty()) {
                    val lastSeenTime = prefs.getLong("last_seen_broadcast_timestamp", 0L)
                    val newest = sorted.firstOrNull()
                    if (newest != null && newest.sentAt > lastSeenTime) {
                        prefs.edit().putLong("last_seen_broadcast_timestamp", newest.sentAt).apply()
                        unreadNotificationCount.value = unreadNotificationCount.value + 1
                        TawthiqNotificationManager.sendAdminBroadcastNotification(
                            context = getApplication(),
                            title = newest.title,
                            message = newest.message
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val userStatusListeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()
    private val directMessagesListeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()

    fun listenToUserAccountStatus(email: String) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) return
        val cleanId = cleanEmail.replace(".", "_").replace("@", "_")
        val cleanUser = cleanEmail.substringBefore("@")

        for (reg in userStatusListeners) {
            reg.remove()
        }
        userStatusListeners.clear()

        try {
            val db = FirebaseFirestore.getInstance()
            val docPaths = mutableListOf(
                db.collection("system_admin_users").document(cleanId),
                db.collection("system_admin_users").document(cleanEmail),
                db.collection("account_status_updates").document(cleanId)
            )
            if (cleanUser.isNotBlank() && cleanUser != cleanId) {
                docPaths.add(db.collection("system_admin_users").document(cleanUser))
                docPaths.add(db.collection("account_status_updates").document(cleanUser))
            }

            for (docRef in docPaths) {
                val reg = docRef.addSnapshotListener { snapshot, e ->
                    if (e != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    val status = snapshot.getString("status")
                    if (!status.isNullOrBlank()) {
                        val prevStatus = _currentUserAccountStatus.value
                        _currentUserAccountStatus.value = status
                        prefs.edit()
                            .putString("account_status_$cleanEmail", status)
                            .putString("account_status_$cleanUser", status)
                            .apply()

                        // If status changed from active to suspended/banned or vice-versa, alert user immediately!
                        if (!status.equals(prevStatus, ignoreCase = true)) {
                            TawthiqNotificationManager.sendAccountStatusChangedNotification(
                                context = getApplication(),
                                newStatus = status
                            )
                        }
                    }
                    val plan = snapshot.getString("plan")
                    val expiry = snapshot.getLong("subscriptionExpiry")
                    if (!plan.isNullOrBlank() && expiry != null && expiry > 0) {
                        val isPro = plan != "مجاني"
                        _subscriptionInfo.value = _subscriptionInfo.value.copy(
                            planName = plan,
                            isPro = isPro,
                            expiryDate = expiry
                        )
                        prefs.edit()
                            .putBoolean("is_pro_$cleanEmail", isPro)
                            .putString("plan_name_$cleanEmail", plan)
                            .putLong("subscription_expiry_$cleanEmail", expiry)
                            .apply()
                    }
                }
                userStatusListeners.add(reg)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Real-time listener for direct personal messages from Admin to this specific User
     */
    fun listenToUserDirectMessages(email: String) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) return
        val cleanId = cleanEmail.replace(".", "_").replace("@", "_")
        val cleanUser = cleanEmail.substringBefore("@")

        for (reg in directMessagesListeners) {
            reg.remove()
        }
        directMessagesListeners.clear()

        try {
            val db = FirebaseFirestore.getInstance()
            val collectionPaths = mutableListOf(
                db.collection("user_notifications").document(cleanId).collection("messages"),
                db.collection("user_notifications").document(cleanEmail).collection("messages"),
                db.collection("user_direct_messages").document(cleanId).collection("inbox")
            )
            if (cleanUser.isNotBlank() && cleanUser != cleanId) {
                collectionPaths.add(db.collection("user_notifications").document(cleanUser).collection("messages"))
                collectionPaths.add(db.collection("user_direct_messages").document(cleanUser).collection("inbox"))
            }

            for (colRef in collectionPaths) {
                var isFirst = true
                val reg = colRef.addSnapshotListener { snapshot, e ->
                    if (e != null || snapshot == null) return@addSnapshotListener
                    val wasInitial = isFirst
                    isFirst = false

                    val newDirectMsgs = mutableListOf<SystemBroadcastMessage>()
                    for (doc in snapshot.documents) {
                        val id = doc.getString("id") ?: doc.id
                        val title = doc.getString("title") ?: ""
                        val msg = doc.getString("message") ?: ""
                        val sender = doc.getString("sender") ?: "إدارة تطبيق البيان (رسالة خاصة)"
                        val sentAt = doc.getLong("sentAt") ?: System.currentTimeMillis()
                        if (title.isNotBlank() || msg.isNotBlank()) {
                            newDirectMsgs.add(SystemBroadcastMessage(id, title, msg, sender, sentAt))
                        }
                    }

                    if (newDirectMsgs.isNotEmpty()) {
                        val combined = (newDirectMsgs + _systemBroadcasts.value).distinctBy { it.id }.sortedByDescending { it.sentAt }
                        _systemBroadcasts.value = combined
                        saveBroadcastMessagesInternal(combined)

                        if (!wasInitial) {
                            for (dc in snapshot.documentChanges) {
                                if (dc.type == DocumentChange.Type.ADDED) {
                                    val newDoc = dc.document
                                    val title = newDoc.getString("title") ?: "رسالة خاصة جديدة"
                                    val msg = newDoc.getString("message") ?: ""
                                    unreadNotificationCount.value = unreadNotificationCount.value + 1
                                    TawthiqNotificationManager.sendAdminDirectNotification(
                                        context = getApplication(),
                                        title = title,
                                        message = msg
                                    )
                                }
                            }
                        }
                    }
                }
                directMessagesListeners.add(reg)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun syncUserAccountStatusNow() {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanEmail = _userEmail.value.trim().lowercase()
            if (cleanEmail.isBlank()) return@launch
            val cleanId = cleanEmail.replace(".", "_").replace("@", "_")
            val cleanUser = cleanEmail.substringBefore("@")
            try {
                val db = FirebaseFirestore.getInstance()
                var snapshot = db.collection("system_admin_users").document(cleanId).get().awaitTask()
                if (!snapshot.exists()) {
                    snapshot = db.collection("system_admin_users").document(cleanEmail).get().awaitTask()
                }
                if (!snapshot.exists() && cleanUser.isNotBlank()) {
                    snapshot = db.collection("system_admin_users").document(cleanUser).get().awaitTask()
                }

                if (snapshot.exists()) {
                    val status = snapshot.getString("status") ?: "ACTIVE"
                    _currentUserAccountStatus.value = status
                    prefs.edit()
                        .putString("account_status_$cleanEmail", status)
                        .putString("account_status_$cleanUser", status)
                        .apply()
                    val plan = snapshot.getString("plan")
                    val expiry = snapshot.getLong("subscriptionExpiry")
                    if (!plan.isNullOrBlank() && expiry != null && expiry > 0) {
                        val isPro = plan != "مجاني"
                        _subscriptionInfo.value = _subscriptionInfo.value.copy(
                            planName = plan,
                            isPro = isPro,
                            expiryDate = expiry
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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

        // Real-time Push & Sync with Firestore system_admin_users so Admin sees them live immediately
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cleanId = cleanEmail.replace(".", "_").replace("@", "_")
                val cleanUser = cleanEmail.substringBefore("@")
                val db = FirebaseFirestore.getInstance()
                val docRef = db.collection("system_admin_users").document(cleanId)
                val snapshot = docRef.get().awaitTask()

                if (snapshot.exists()) {
                    val cloudStatus = snapshot.getString("status") ?: currentStatus
                    val cloudPlan = snapshot.getString("plan") ?: "مجاني"
                    val cloudExpiry = snapshot.getLong("subscriptionExpiry") ?: (System.currentTimeMillis() + 4L * 86400000L)

                    _currentUserAccountStatus.value = cloudStatus
                    prefs.edit()
                        .putString("account_status_$cleanEmail", cloudStatus)
                        .putString("account_status_$cleanUser", cloudStatus)
                        .apply()

                    val updateFields = hashMapOf<String, Any>(
                        "lastActive" to System.currentTimeMillis()
                    )
                    if (merchant.isNotBlank()) updateFields["merchantName"] = derivedMerchant
                    if (store.isNotBlank()) updateFields["storeName"] = derivedStore
                    if (phone.isNotBlank()) updateFields["phone"] = derivedPhone
                    if (password.isNotBlank()) updateFields["password"] = password.trim()

                    docRef.set(updateFields, SetOptions.merge())
                    db.collection("system_admin_users").document(cleanEmail).set(updateFields, SetOptions.merge())
                    if (cleanUser.isNotBlank()) {
                        db.collection("system_admin_users").document(cleanUser).set(updateFields, SetOptions.merge())
                    }
                } else {
                    val userDoc = hashMapOf<String, Any>(
                        "id" to "usr_${Math.abs(cleanEmail.hashCode()) % 100000}",
                        "email" to cleanEmail,
                        "username" to cleanUser,
                        "storeName" to derivedStore,
                        "merchantName" to derivedMerchant,
                        "phone" to derivedPhone,
                        "password" to password.ifBlank { "123456" },
                        "status" to currentStatus,
                        "plan" to "مجاني",
                        "registeredAt" to System.currentTimeMillis(),
                        "subscriptionStart" to System.currentTimeMillis(),
                        "subscriptionExpiry" to (System.currentTimeMillis() + 4L * 86400000L),
                        "lastActive" to System.currentTimeMillis()
                    )
                    docRef.set(userDoc, SetOptions.merge())
                    db.collection("system_admin_users").document(cleanEmail).set(userDoc, SetOptions.merge())
                    if (cleanUser.isNotBlank()) {
                        db.collection("system_admin_users").document(cleanUser).set(userDoc, SetOptions.merge())
                        db.collection("user_profiles").document(cleanUser).set(userDoc, SetOptions.merge())
                    }
                    db.collection("user_profiles").document(cleanId).set(userDoc, SetOptions.merge())
                }

                listenToUserAccountStatus(cleanEmail)
                listenToUserDirectMessages(cleanEmail)
                mergeAndPublishAdminUsers()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (existingStore != null) {
            loadUserProfile(cleanEmail)
        } else {
            updateStoreProfile(derivedStore, derivedMerchant, derivedPhone, "USD")
        }
        syncAllUserAccountsAndTransactionsToCloud()
    }

    /**
     * Requirement: Strict Unique Merchant Registration
     * Atomically registers a merchant using normalized email.
     * Rejects with ERR_DUPLICATE_EMAIL if email is already taken.
     */
    fun registerMerchant(
        email: String,
        merchantName: String,
        storeName: String,
        phone: String,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val cleanEmail = com.example.util.MerchantAuthService.normalizeEmail(email)
        if (!com.example.util.MerchantAuthService.isValidEmail(cleanEmail)) {
            onResult(false, com.example.util.MerchantAuthService.ERR_INVALID_EMAIL)
            return
        }

        // Quick local check across known admin accounts
        val currentUsers = _adminUserAccounts.value
        if (currentUsers.any { it.email.equals(cleanEmail, ignoreCase = true) }) {
            onResult(false, com.example.util.MerchantAuthService.ERR_DUPLICATE_EMAIL)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val result = com.example.util.MerchantAuthService.registerMerchantAtomic(
                rawEmail = cleanEmail,
                merchantName = merchantName,
                storeName = storeName,
                phone = phone,
                password = password
            )

            withContext(Dispatchers.Main) {
                when (result) {
                    is com.example.util.MerchantAuthService.AuthResult.Success -> {
                        val user = result.user
                        val updated = _adminUserAccounts.value + user
                        _adminUserAccounts.value = updated
                        saveAdminUsersInternal(updated)

                        loginWithEmail(
                            email = user.email,
                            merchant = user.merchantName,
                            store = user.storeName,
                            phone = user.phone,
                            password = user.password
                        )
                        onResult(true, "تم إنشاء حساب التاجر بنجاح ✓")
                    }
                    is com.example.util.MerchantAuthService.AuthResult.Error -> {
                        onResult(false, result.message)
                    }
                }
            }
        }
    }

    /**
     * Requirement: Strict Unique Merchant Login Check
     */
    fun verifyAndLoginMerchant(
        email: String,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val cleanEmail = com.example.util.MerchantAuthService.normalizeEmail(email)
        if (cleanEmail.isBlank()) {
            onResult(false, "يرجى إدخال البريد الإلكتروني")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val result = com.example.util.MerchantAuthService.verifyMerchantLogin(cleanEmail, password)
            withContext(Dispatchers.Main) {
                when (result) {
                    is com.example.util.MerchantAuthService.AuthResult.Success -> {
                        val user = result.user
                        loginWithEmail(
                            email = user.email,
                            merchant = user.merchantName,
                            store = user.storeName,
                            phone = user.phone,
                            password = user.password
                        )
                        onResult(true, "تم تسجيل الدخول بنجاح ✓")
                    }
                    is com.example.util.MerchantAuthService.AuthResult.Error -> {
                        onResult(false, result.message)
                    }
                }
            }
        }
    }

    /**
     * Requirement: Password Recovery for Unique Merchant Email
     */
    fun recoverMerchantPassword(
        email: String,
        newPassword: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val cleanEmail = com.example.util.MerchantAuthService.normalizeEmail(email)
        viewModelScope.launch(Dispatchers.IO) {
            val result = com.example.util.MerchantAuthService.recoverMerchantPassword(cleanEmail, newPassword)
            withContext(Dispatchers.Main) {
                when (result) {
                    is com.example.util.MerchantAuthService.AuthResult.Success -> {
                        onResult(true, "تم تحديث كلمة المرور لحسابك بنجاح! يمكنك الآن تسجيل الدخول.")
                    }
                    is com.example.util.MerchantAuthService.AuthResult.Error -> {
                        onResult(false, result.message)
                    }
                }
            }
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
        syncAdminDataFromCloud()
        listenToSystemBroadcasts()
        listenToPaymentMethods()
        listenToUserAccountStatus(_userEmail.value)
        listenToUserDirectMessages(_userEmail.value)
        syncAllUserAccountsAndTransactionsToCloud()

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
            val generatedId = repository.addAccount(account, initialBalance, initialType)
            syncAllUserAccountsAndTransactionsToCloud()
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
            syncAllUserAccountsAndTransactionsToCloud()
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
            syncAllUserAccountsAndTransactionsToCloud()
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
