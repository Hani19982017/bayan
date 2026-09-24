package com.example

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.util.FirebaseSyncManager
import com.example.util.TawthiqBackgroundSyncManager
import com.example.util.TawthiqNotificationManager
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TawthiqApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        try {
            // 0. Ensure Firebase is initialized and configure App Check
            FirebaseApp.initializeApp(this)
            try {
                if (BuildConfig.DEBUG) {
                    val firebaseAppCheck = FirebaseAppCheck.getInstance()
                    firebaseAppCheck.installAppCheckProviderFactory(
                        DebugAppCheckProviderFactory.getInstance()
                    )
                    Log.d("TawthiqApplication", "Firebase App Check initialized with DebugAppCheckProviderFactory")
                }
            } catch (e: Exception) {
                Log.w("TawthiqApplication", "AppCheck setup skipped: ${e.message}")
            }

            // 1. Setup system notification channels
            TawthiqNotificationManager.createNotificationChannels(this)

            // 2. Schedule background sync workers and alarms safely
            TawthiqBackgroundSyncManager.scheduleWorkManager(this)
            TawthiqBackgroundSyncManager.schedulePeriodicSync(this)

            // Clean install check: reset any lingering test sessions on fresh install / app version update
            val prefs = getSharedPreferences("tawthiq_prefs", Context.MODE_PRIVATE)
            val isAuthV2Init = prefs.getBoolean("auth_v2_initialized", false)
            if (!isAuthV2Init) {
                val hasUser = !prefs.getString("user_email", "").isNullOrBlank()
                val editor = prefs.edit().putBoolean("auth_v2_initialized", true)
                if (!hasUser) {
                    editor.putBoolean("is_logged_in", false)
                        .putBoolean("is_staff_logged_in", false)
                        .remove("staff_merchant_email")
                        .remove("saved_customer_account_id")
                        .putBoolean("is_customer_mode", false)
                }
                editor.apply()
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
