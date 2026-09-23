package com.example.util

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task

data class GoogleAccountInfo(
    val email: String,
    val displayName: String = "",
    val photoUrl: String = ""
)

object GoogleAuthHelper {

    /**
     * Builds standard GoogleSignInClient
     */
    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    /**
     * Extracts GoogleAccountInfo from ANY Intent result (GoogleSignIn or AccountManager)
     */
    fun getAccountFromIntent(data: Intent?): GoogleAccountInfo? {
        if (data == null) return null

        // 1. Check AccountManager standard extras
        val accName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            ?: data.getStringExtra("accountName")
            ?: data.getStringExtra("authAccount")
            ?: data.getStringExtra("email")

        if (!accName.isNullOrBlank() && accName.contains("@")) {
            val cleanEmail = accName.trim().lowercase()
            return GoogleAccountInfo(
                email = cleanEmail,
                displayName = cleanEmail.substringBefore("@")
            )
        }

        // 2. Try GoogleSignIn task
        try {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            if (task.isSuccessful) {
                val account = task.result
                if (account != null && !account.email.isNullOrBlank()) {
                    return GoogleAccountInfo(
                        email = account.email!!.trim().lowercase(),
                        displayName = account.displayName ?: account.email!!.substringBefore("@"),
                        photoUrl = account.photoUrl?.toString() ?: ""
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("GoogleAuthHelper", "GoogleSignIn task parsing: ${e.message}")
        }

        // 3. Check GoogleSignIn parcelable extra
        try {
            val account = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data.getParcelableExtra("googleSignInAccount", GoogleSignInAccount::class.java)
            } else {
                @Suppress("DEPRECATION")
                data.getParcelableExtra("googleSignInAccount")
            }
            if (account != null && !account.email.isNullOrBlank()) {
                return GoogleAccountInfo(
                    email = account.email!!.trim().lowercase(),
                    displayName = account.displayName ?: account.email!!.substringBefore("@"),
                    photoUrl = account.photoUrl?.toString() ?: ""
                )
            }
        } catch (e: Exception) {
            Log.w("GoogleAuthHelper", "Parcelable error: ${e.message}")
        }

        // 4. Exhaustive search across all extras for any email string
        try {
            data.extras?.let { bundle ->
                for (key in bundle.keySet()) {
                    val value = bundle.get(key)
                    if (value is String && value.contains("@") && value.contains(".")) {
                        val clean = value.trim().lowercase()
                        if (android.util.Patterns.EMAIL_ADDRESS.matcher(clean).matches()) {
                            return GoogleAccountInfo(
                                email = clean,
                                displayName = clean.substringBefore("@")
                            )
                        }
                    } else if (value is Account && value.name.contains("@")) {
                        val clean = value.name.trim().lowercase()
                        return GoogleAccountInfo(
                            email = clean,
                            displayName = clean.substringBefore("@")
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("GoogleAuthHelper", "Extras scan error: ${e.message}")
        }

        return null
    }

    /**
     * Native Android OS account picker intent (AccountManager)
     * This opens the official native system account selection window on Android
     */
    fun createAccountChooserIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AccountManager.newChooseAccountIntent(
                null,
                null,
                arrayOf("com.google"),
                null,
                null,
                null,
                null
            )
        } else {
            @Suppress("DEPRECATION")
            AccountManager.newChooseAccountIntent(
                null,
                null,
                arrayOf("com.google"),
                false,
                null,
                null,
                null,
                null
            )
        }
    }
}
