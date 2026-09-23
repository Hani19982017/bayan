package com.example

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.util.FirebaseSyncManager
import com.example.util.TawthiqBackgroundSyncManager
import com.example.util.TawthiqNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TawthiqApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        try {
            // 1. Setup system notification channels
            TawthiqNotificationManager.createNotificationChannels(this)

            // 2. Schedule background sync workers and alarms safely
            TawthiqBackgroundSyncManager.scheduleWorkManager(this)
            TawthiqBackgroundSyncManager.schedulePeriodicSync(this)

            // Clean install check: reset any lingering test sessions on fresh install / app version update
            val prefs = getSharedPreferences("tawthiq_prefs", Context.MODE_PRIVATE)
            val isAuthV2Init = prefs.getBoolean("auth_v2_initialized", false)
            if (!isAuthV2Init) {
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

            // 3. Start Firestore Live Sync immediately if customer session is saved
            val savedCustId = prefs.getLong("saved_customer_account_id", -1L)
            if (savedCustId > 0) {
                applicationScope.launch {
                    val db = AppDatabase.getDatabase(this@TawthiqApplication, applicationScope)
                    val account = db.accountDao().getAccountByIdOnce(savedCustId)
                    if (account != null) {
                        FirebaseSyncManager.startLiveSyncForAccount(
                            context = this@TawthiqApplication,
                            account = account,
                            coroutineScope = applicationScope
                        )
                        Log.d("TawthiqApplication", "Initialized live sync for saved customer account: ${account.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("TawthiqApplication", "Error during app init", e)
        }
    }
}
