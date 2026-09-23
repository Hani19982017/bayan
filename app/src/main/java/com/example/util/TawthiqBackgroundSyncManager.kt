package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.local.AppDatabase
import com.example.data.model.TransactionEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Ensures robust 24/7 background synchronization and Push Notifications for customer accounts
 * even when the application is completely closed or removed from recent tasks.
 */
object TawthiqBackgroundSyncManager {

    private const val TAG = "TawthiqBackgroundSync"
    private const val PREFS_NAME = "tawthiq_bg_sync_prefs"
    private const val KEY_LAST_SYNC_TIME = "last_bg_sync_time"
    private const val WORK_TAG_PERIODIC = "tawthiq_periodic_sync"
    private const val SYNC_INTERVAL_MS = 2 * 60 * 1000L // 2 minutes

    private val bgScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Initializes all background mechanisms (Service + WorkManager + AlarmManager).
     */
    fun startAllBackgroundSync(context: Context) {
        try {
            // 1. Start Persistent Live Foreground Service (safe if in foreground)
            TawthiqLiveSyncService.startService(context)

            // 2. Schedule WorkManager periodic worker (Survives reboots & OS aggressive cleaning)
            scheduleWorkManager(context)

            // 3. Schedule AlarmManager exact repeating check
            schedulePeriodicSync(context)

            Log.d(TAG, "All background sync mechanisms initialized successfully")
        } catch (e: Exception) {
            Log.d(TAG, "Background sync setup: ${e.message}")
        }
    }

    fun scheduleWorkManager(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicSyncRequest = PeriodicWorkRequestBuilder<TawthiqSyncWorker>(
                15, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_TAG_PERIODIC,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicSyncRequest
            )
            Log.d(TAG, "WorkManager periodic sync enqueued")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enqueue WorkManager sync", e)
        }
    }

    fun schedulePeriodicSync(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, TawthiqBackgroundSyncReceiver::class.java).apply {
                action = "com.example.tawthiq.ACTION_BG_SYNC"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val triggerAt = System.currentTimeMillis() + SYNC_INTERVAL_MS
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                } catch (e: SecurityException) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                }
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
            Log.d(TAG, "Scheduled next background sync alarm in 2 minutes")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule background sync alarm", e)
        }
    }

    fun performBackgroundSync(context: Context, onComplete: () -> Unit = {}) {
        bgScope.launch {
            try {
                val prefs = context.getSharedPreferences("tawthiq_prefs", Context.MODE_PRIVATE)
                val bgPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val savedCustId = prefs.getLong("saved_customer_account_id", -1L)

                if (savedCustId <= 0) {
                    onComplete()
                    return@launch
                }

                val db = AppDatabase.getDatabase(context, bgScope)
                val account = db.accountDao().getAccountByIdOnce(savedCustId)
                if (account == null) {
                    onComplete()
                    return@launch
                }

                val syncKey = FirebaseSyncManager.extractSyncKey(account)
                val lastSyncTime = bgPrefs.getLong(KEY_LAST_SYNC_TIME, System.currentTimeMillis() - 86400000L)

                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("live_statements")
                    .document(syncKey)
                    .collection("transactions")
                    .whereGreaterThan("updatedAt", lastSyncTime)
                    .orderBy("updatedAt", Query.Direction.ASCENDING)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        bgScope.launch {
                            val repo = com.example.data.repository.TawthiqRepository(db.accountDao(), db.transactionDao())
                            var newestUpdated = lastSyncTime

                            for (doc in snapshot.documents) {
                                val amt = doc.getDouble("amount") ?: (doc.getString("amount")?.toDoubleOrNull() ?: 0.0)
                                val type = doc.getString("type") ?: "LANA"
                                val desc = doc.getString("description") ?: ""
                                val cur = doc.getString("currency") ?: account.currency
                                val date = doc.getLong("date") ?: System.currentTimeMillis()
                                val receipt = doc.getString("receiptNumber") ?: ""
                                val storeName = doc.getString("storeName") ?: "التاجر"
                                val updatedAt = doc.getLong("updatedAt") ?: date
                                val effectiveReceipt = if (receipt.isNotBlank()) receipt else doc.id

                                if (updatedAt > newestUpdated) {
                                    newestUpdated = updatedAt
                                }

                                val existingTxs = db.transactionDao().getTransactionsForAccountOnce(account.id)

                                val exists = existingTxs.any {
                                    it.receiptNumber == doc.id ||
                                    (receipt.isNotBlank() && it.receiptNumber == receipt) ||
                                    (it.date == date && kotlin.math.abs(it.amount - amt) < 0.0001 && it.type == type && it.description.trim() == desc.trim())
                                }

                                if (!exists && amt > 0.0) {
                                    db.transactionDao().insertTransaction(
                                        TransactionEntity(
                                            userEmail = account.userEmail,
                                            accountId = account.id,
                                            type = type,
                                            amount = amt,
                                            currency = cur,
                                            description = desc,
                                            date = date,
                                            receiptNumber = effectiveReceipt
                                        )
                                    )

                                    db.accountDao().updateAccount(account.copy(updatedAt = System.currentTimeMillis()))
                                    repo.deduplicateTransactionsForAccount(account.id)

                                    TawthiqNotificationManager.sendLiveTransactionPushNotification(
                                        context = context,
                                        storeName = storeName,
                                        accountName = account.name,
                                        amount = amt,
                                        currency = cur,
                                        type = type,
                                        description = desc,
                                        accountId = account.id
                                    )
                                }
                            }

                            repo.deduplicateTransactionsForAccount(account.id)
                            bgPrefs.edit().putLong(KEY_LAST_SYNC_TIME, maxOf(newestUpdated, System.currentTimeMillis())).apply()
                            onComplete()
                        }
                    }
                    .addOnFailureListener {
                        onComplete()
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error in background sync", e)
                onComplete()
            }
        }
    }

    /**
     * Checks if battery optimization is disabled for this app.
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } else {
            true
        }
    }

    /**
     * Opens battery optimization exemption settings dialog/screen.
     */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (ex: Exception) {
                    Log.e(TAG, "Failed to open battery optimization settings", ex)
                }
            }
        }
    }
}

class TawthiqBackgroundSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("TawthiqBgSyncReceiver", "onReceive triggered with action: ${intent?.action}")
        // Perform direct background query safely without starting foreground service in background
        TawthiqBackgroundSyncManager.performBackgroundSync(context) {
            // Re-schedule next check
            TawthiqBackgroundSyncManager.schedulePeriodicSync(context)
        }
    }
}
