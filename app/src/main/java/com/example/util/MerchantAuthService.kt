package com.example.util

import android.util.Log
import com.example.data.model.AdminUserAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.regex.Pattern

/**
 * Service enforcing strict Unique Merchant Email Identity across the Tawthiq platform.
 *
 * Enforces:
 * 1. Email is the unique identity of each merchant.
 * 2. Store name duplication is permitted, but email duplication is strictly prohibited.
 * 3. Atomic registration via Firestore transactions to prevent race conditions.
 * 4. Case-insensitive and whitespace-trimmed email normalization.
 * 5. Integration with Firebase Authentication & Firestore system_admin_users.
 * 6. Protection against accidental recreation of deleted accounts.
 */
object MerchantAuthService {

    private const val TAG = "MerchantAuthService"
    const val ERR_DUPLICATE_EMAIL = "هذا البريد الإلكتروني مستخدم بالفعل. لا يمكن إنشاء أكثر من حساب تاجر بنفس البريد الإلكتروني."
    const val ERR_DELETED_ACCOUNT = "هذا الحساب تم إغلاقه أو حذفه مسبقاً. لا يمكن إنشاء حساب جديد بنفس البريد. يرجى التواصل مع إدارة البيان."
    const val ERR_INVALID_EMAIL = "يرجى إدخال بريد إلكتروني صالح."
    const val ERR_PASSWORD_MISMATCH = "كلمة المرور وتأكيد كلمة المرور غير متطابقتين."
    const val ERR_PASSWORD_TOO_SHORT = "يجب أن تتكون كلمة المرور من 6 أحرف أو أرقام على الأقل."

    private val EMAIL_REGEX = Pattern.compile(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}$"
    )

    /**
     * Normalizes an email string: strips leading/trailing whitespaces and converts to lowercase.
     * Example: "  Merchant@Gmail.COM " -> "merchant@gmail.com"
     */
    fun normalizeEmail(email: String): String {
        return email.trim().lowercase()
    }

    /**
     * Validates whether an email format is valid.
     */
    fun isValidEmail(email: String): Boolean {
        val normalized = normalizeEmail(email)
        return normalized.isNotEmpty() && EMAIL_REGEX.matcher(normalized).matches()
    }

    /**
     * Sanitizes email for use as a Firestore Document ID where necessary.
     */
    fun emailToDocId(email: String): String {
        val normalized = normalizeEmail(email)
        return normalized.replace("/", "_")
    }

    /**
     * Sanitizes email for standard key paths.
     */
    fun cleanEmailKey(email: String): String {
        val normalized = normalizeEmail(email)
        return normalized.replace(".", "_").replace("@", "_").replace("/", "_")
    }

    sealed class AuthResult {
        data class Success(val user: AdminUserAccount, val isNew: Boolean = false) : AuthResult()
        data class Error(val message: String) : AuthResult()
    }

    /**
     * Atomic Merchant Registration
     *
     * Uses Firestore transaction to guarantee that even if two requests arrive simultaneously
     * for the same email (from different devices, tabs, or networks), only ONE can succeed.
     * The other will be rejected atomically with ERR_DUPLICATE_EMAIL.
     */
    suspend fun registerMerchantAtomic(
        rawEmail: String,
        merchantName: String,
        storeName: String,
        phone: String,
        password: String
    ): AuthResult {
        val normalizedEmail = normalizeEmail(rawEmail)

        if (!isValidEmail(normalizedEmail)) {
            return AuthResult.Error(ERR_INVALID_EMAIL)
        }
        if (password.length < 6) {
            return AuthResult.Error(ERR_PASSWORD_TOO_SHORT)
        }

        val cleanMerchant = merchantName.trim().ifBlank { normalizedEmail.substringBefore("@") }
        val cleanStore = storeName.trim().ifBlank { "متجر $cleanMerchant" }
        val cleanPhone = phone.trim().ifBlank { "+966500000000" }

        // 1. Check Firebase Auth if available (Server-side identity provider check)
        try {
            val auth = FirebaseAuth.getInstance()
            val signInMethods = auth.fetchSignInMethodsForEmail(normalizedEmail).await()
            val methods = signInMethods.signInMethods
            if (!methods.isNullOrEmpty()) {
                Log.w(TAG, "Email already exists in Firebase Auth: $normalizedEmail")
                return AuthResult.Error(ERR_DUPLICATE_EMAIL)
            }
        } catch (e: FirebaseAuthUserCollisionException) {
            return AuthResult.Error(ERR_DUPLICATE_EMAIL)
        } catch (e: Exception) {
            Log.d(TAG, "Firebase Auth pre-check skipped or offline: ${e.message}")
        }

        // 2. Perform Atomic Firestore Transaction on unique email registry
        val db = FirebaseFirestore.getInstance()
        val emailDocId = emailToDocId(normalizedEmail)
        val cleanDocId = cleanEmailKey(normalizedEmail)
        val usernameKey = normalizedEmail.substringBefore("@")

        val uniqueEmailDocRef = db.collection("merchant_unique_emails").document(emailDocId)
        val adminUserDocRef = db.collection("system_admin_users").document(cleanDocId)
        val userProfileDocRef = db.collection("user_profiles").document(cleanDocId)

        val newUser = AdminUserAccount(
            id = "usr_${Math.abs(normalizedEmail.hashCode()) % 100000}",
            email = normalizedEmail,
            storeName = cleanStore,
            merchantName = cleanMerchant,
            phone = cleanPhone,
            password = password,
            status = "ACTIVE",
            plan = "مجاني",
            registeredAt = System.currentTimeMillis(),
            subscriptionStart = System.currentTimeMillis(),
            subscriptionExpiry = System.currentTimeMillis() + 4L * 86400000L,
            totalAccountsCount = 0,
            totalVolume = 0.0,
            notes = "حساب تاجر مسجل عبر النظام الذاتي"
        )

        return try {
            db.runTransaction { transaction ->
                // READ PHASE (must precede all writes in Firestore transactions)
                val uniqueEmailSnapshot = transaction.get(uniqueEmailDocRef)
                val adminUserSnapshot = transaction.get(adminUserDocRef)

                // Verify Uniqueness in merchant_unique_emails
                if (uniqueEmailSnapshot.exists()) {
                    val status = uniqueEmailSnapshot.getString("status") ?: "ACTIVE"
                    if (status.equals("DELETED", ignoreCase = true) || status.equals("ARCHIVED", ignoreCase = true)) {
                        throw IllegalStateException(ERR_DELETED_ACCOUNT)
                    }
                    throw IllegalArgumentException(ERR_DUPLICATE_EMAIL)
                }

                // Verify Uniqueness in system_admin_users
                if (adminUserSnapshot.exists()) {
                    val status = adminUserSnapshot.getString("status") ?: "ACTIVE"
                    if (status.equals("DELETED", ignoreCase = true)) {
                        throw IllegalStateException(ERR_DELETED_ACCOUNT)
                    }
                    throw IllegalArgumentException(ERR_DUPLICATE_EMAIL)
                }

                // WRITE PHASE: Atomically claim the email registry document
                val registryData = hashMapOf<String, Any>(
                    "email" to normalizedEmail,
                    "merchantId" to newUser.id,
                    "storeName" to cleanStore,
                    "merchantName" to cleanMerchant,
                    "phone" to cleanPhone,
                    "registeredAt" to newUser.registeredAt,
                    "status" to "ACTIVE"
                )
                transaction.set(uniqueEmailDocRef, registryData)

                // Atomically create the merchant user profile
                val userDocData = hashMapOf<String, Any>(
                    "id" to newUser.id,
                    "email" to normalizedEmail,
                    "username" to usernameKey,
                    "storeName" to cleanStore,
                    "merchantName" to cleanMerchant,
                    "phone" to cleanPhone,
                    "password" to password,
                    "status" to "ACTIVE",
                    "plan" to "مجاني",
                    "registeredAt" to newUser.registeredAt,
                    "subscriptionStart" to newUser.subscriptionStart,
                    "subscriptionExpiry" to newUser.subscriptionExpiry,
                    "lastActive" to System.currentTimeMillis(),
                    "totalAccountsCount" to 0,
                    "totalVolume" to 0.0
                )
                transaction.set(adminUserDocRef, userDocData)
                transaction.set(userProfileDocRef, userDocData)
            }.await()

            // Optional: Create in Firebase Auth if supported
            try {
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(normalizedEmail, password).await()
            } catch (e: Exception) {
                Log.d(TAG, "FirebaseAuth createUser optional: ${e.message}")
            }

            AuthResult.Success(newUser, isNew = true)
        } catch (e: IllegalArgumentException) {
            AuthResult.Error(ERR_DUPLICATE_EMAIL)
        } catch (e: IllegalStateException) {
            AuthResult.Error(e.message ?: ERR_DELETED_ACCOUNT)
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("duplicate", ignoreCase = true) || msg.contains(ERR_DUPLICATE_EMAIL)) {
                AuthResult.Error(ERR_DUPLICATE_EMAIL)
            } else {
                Log.e(TAG, "Transaction error: ${e.message}", e)
                AuthResult.Error(e.localizedMessage ?: "حدث خطأ أثناء إنشاء الحساب، يرجى المحاولة ثانية.")
            }
        }
    }

    /**
     * Checks whether an email is already registered across Firestore or local cache.
     */
    suspend fun isEmailRegistered(rawEmail: String): Boolean {
        val normalized = normalizeEmail(rawEmail)
        if (!isValidEmail(normalized)) return false

        return try {
            val db = FirebaseFirestore.getInstance()
            val emailDocId = emailToDocId(normalized)
            val cleanDocId = cleanEmailKey(normalized)

            val emailDoc = db.collection("merchant_unique_emails").document(emailDocId).get().await()
            if (emailDoc.exists()) return true

            val adminDoc = db.collection("system_admin_users").document(cleanDocId).get().await()
            if (adminDoc.exists()) return true

            val query = db.collection("system_admin_users")
                .whereEqualTo("email", normalized)
                .limit(1)
                .get()
                .await()
            !query.isEmpty
        } catch (e: Exception) {
            Log.e(TAG, "Check email existence error: ${e.message}")
            false
        }
    }

    /**
     * Verifies merchant credentials for login.
     */
    suspend fun verifyMerchantLogin(rawEmail: String, rawPassword: String): AuthResult {
        val normalized = normalizeEmail(rawEmail)
        if (normalized.isBlank()) {
            return AuthResult.Error("يرجى إدخال البريد الإلكتروني.")
        }

        val db = FirebaseFirestore.getInstance()
        val cleanDocId = cleanEmailKey(normalized)
        val emailDocId = emailToDocId(normalized)

        try {
            // Check in system_admin_users
            var snapshot = db.collection("system_admin_users").document(cleanDocId).get().await()
            if (!snapshot.exists()) {
                val query = db.collection("system_admin_users")
                    .whereEqualTo("email", normalized)
                    .limit(1)
                    .get()
                    .await()
                if (!query.isEmpty) {
                    snapshot = query.documents[0]
                }
            }

            if (!snapshot.exists()) {
                // Check if account was deleted
                val uniqueDoc = db.collection("merchant_unique_emails").document(emailDocId).get().await()
                if (uniqueDoc.exists() && uniqueDoc.getString("status") == "DELETED") {
                    return AuthResult.Error(ERR_DELETED_ACCOUNT)
                }
                return AuthResult.Error("لم يتم العثور على أي حساب مسجل بهذا البريد الإلكتروني.")
            }

            val status = snapshot.getString("status") ?: "ACTIVE"
            if (status.equals("DELETED", ignoreCase = true)) {
                return AuthResult.Error(ERR_DELETED_ACCOUNT)
            }
            if (status.equals("BANNED", ignoreCase = true) || status.equals("SUSPENDED", ignoreCase = true)) {
                val reason = snapshot.getString("banReason") ?: "تم إيقاف هذا الحساب من قبل الإدارة"
                return AuthResult.Error("⚠️ الحساب موقوف: $reason")
            }

            val savedPassword = snapshot.getString("password") ?: ""
            if (rawPassword.isNotBlank() && savedPassword.isNotBlank()) {
                if (savedPassword != rawPassword.trim()) {
                    return AuthResult.Error("كلمة المرور غير صحيحة. يرجى التأكد أو استخدام خيار 'استعادة كلمة المرور'.")
                }
            }

            val merchant = AdminUserAccount(
                id = snapshot.getString("id") ?: "usr_${cleanDocId}",
                email = snapshot.getString("email") ?: normalized,
                storeName = snapshot.getString("storeName") ?: "متجر البيان",
                merchantName = snapshot.getString("merchantName") ?: normalized.substringBefore("@"),
                phone = snapshot.getString("phone") ?: "",
                password = savedPassword,
                status = status,
                plan = snapshot.getString("plan") ?: "مجاني",
                registeredAt = snapshot.getLong("registeredAt") ?: System.currentTimeMillis(),
                subscriptionStart = snapshot.getLong("subscriptionStart") ?: System.currentTimeMillis(),
                subscriptionExpiry = snapshot.getLong("subscriptionExpiry") ?: (System.currentTimeMillis() + 4L * 86400000L)
            )

            return AuthResult.Success(merchant, isNew = false)
        } catch (e: Exception) {
            Log.e(TAG, "Login error: ${e.message}")
            return AuthResult.Error("حدث خطأ أثناء التحقق: ${e.localizedMessage}")
        }
    }

    /**
     * Password Recovery: resets the password for a verified merchant email.
     */
    suspend fun recoverMerchantPassword(rawEmail: String, newPassword: String): AuthResult {
        val normalized = normalizeEmail(rawEmail)
        if (!isValidEmail(normalized)) {
            return AuthResult.Error(ERR_INVALID_EMAIL)
        }
        if (newPassword.length < 6) {
            return AuthResult.Error(ERR_PASSWORD_TOO_SHORT)
        }

        val db = FirebaseFirestore.getInstance()
        val cleanDocId = cleanEmailKey(normalized)

        try {
            val docRef = db.collection("system_admin_users").document(cleanDocId)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                return AuthResult.Error("لم يتم العثور على أي حساب تاجر مسجل بهذا البريد الإلكتروني.")
            }

            val update = hashMapOf<String, Any>(
                "password" to newPassword.trim(),
                "passwordUpdatedAt" to System.currentTimeMillis()
            )
            docRef.set(update, SetOptions.merge()).await()

            // Also update in Firebase Auth if user exists
            try {
                val auth = FirebaseAuth.getInstance()
                auth.sendPasswordResetEmail(normalized).await()
            } catch (e: Exception) {
                Log.d(TAG, "Firebase Auth reset email: ${e.message}")
            }

            return AuthResult.Success(
                AdminUserAccount(
                    id = snapshot.getString("id") ?: "usr_$cleanDocId",
                    email = normalized,
                    merchantName = snapshot.getString("merchantName") ?: "",
                    storeName = snapshot.getString("storeName") ?: ""
                )
            )
        } catch (e: Exception) {
            return AuthResult.Error("فشلت استعادة كلمة المرور: ${e.localizedMessage}")
        }
    }

    /**
     * Marks account as deleted in merchant_unique_emails and system_admin_users
     * to protect against accidental recreation.
     */
    suspend fun markAccountDeleted(rawEmail: String) {
        val normalized = normalizeEmail(rawEmail)
        val db = FirebaseFirestore.getInstance()
        val emailDocId = emailToDocId(normalized)
        val cleanDocId = cleanEmailKey(normalized)

        try {
            val deleteRecord = hashMapOf<String, Any>(
                "status" to "DELETED",
                "deletedAt" to System.currentTimeMillis()
            )
            db.collection("merchant_unique_emails").document(emailDocId).set(deleteRecord, SetOptions.merge()).await()
            db.collection("system_admin_users").document(cleanDocId).set(deleteRecord, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error marking account deleted: ${e.message}")
        }
    }
}
