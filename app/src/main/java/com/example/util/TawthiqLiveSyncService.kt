package com.example.util

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.TransactionEntity
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * High-reliability Foreground Service that keeps Firebase Firestore snapshot listeners
 * actively connected 24/7 so push notifications arrive instantly when a transaction
 * is registered, even when the user has swiped away or closed the app.
 */
class TawthiqLiveSyncService : Service() {

    companion object {
        private const val TAG = "TawthiqLiveSyncService"
        private const val NOTIFICATION_ID = 9981

        fun startService(context: Context) {
            try {
                val intent = Intent(context, TawthiqLiveSyncService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // ForegroundServiceStartNotAllowedException on Android 12+ if in background.
                // WorkManager handles background execution safely.
                Log.d(TAG, "Foreground service start deferred: ${e.message}")
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, TawthiqLiveSyncService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop TawthiqLiveSyncService", e)
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val firestoreListeners = ConcurrentHashMap<String, ListenerRegistration>()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "TawthiqLiveSyncService onCreate")
        TawthiqNotificationManager.createNotificationChannels(this)
        startAsForeground()
        attachFirestoreListeners()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "TawthiqLiveSyncService onStartCommand")
        startAsForeground()
        attachFirestoreListeners()
        return START_STICKY
    }

    private fun startAsForeground() {
        try {
            val notification = buildForegroundNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground", e)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, TawthiqNotificationManager.CHANNEL_BACKGROUND_SERVICE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("توثيق: المزامنة اللحظية نشطة")
            .setContentText("استقبال إشعارات المعاملات والفواتير في نفس اللحظة")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .setColor(Color.parseColor("#0D9488"))
            .build()
    }

    private fun attachFirestoreListeners() {
        serviceScope.launch {
            try {
                val prefs = getSharedPreferences("tawthiq_prefs", Context.MODE_PRIVATE)
                val savedCustId = prefs.getLong("saved_customer_account_id", -1L)
                val db = AppDatabase.getDatabase(this@TawthiqLiveSyncService, serviceScope)

                // Listen ONLY for the saved customer account or imported customer statements
                val accountsToSync = mutableListOf<com.example.data.model.AccountEntity>()
                if (savedCustId > 0) {
                    val custAccount = db.accountDao().getAccountByIdOnce(savedCustId)
                    if (custAccount != null) {
                        accountsToSync.add(custAccount)
                    }
                }
                
                // Only listen for accounts that are linked customer statements (imported via QR)
                val allAccounts = db.accountDao().getAllAccountsOnce()
                for (acc in allAccounts) {
                    val isCustomerAccount = acc.category == "حسابات متابعة" ||
                            acc.notes.contains("كشف حساب مرتبط") ||
                            (acc.userEmail.isBlank() && acc.notes.contains("[SYNC:"))
                    if (isCustomerAccount && accountsToSync.none { it.id == acc.id }) {
                        accountsToSync.add(acc)
                    }
                }

                val firestore = FirebaseFirestore.getInstance()

                for (account in accountsToSync) {
                    val syncKey = FirebaseSyncManager.extractSyncKey(account)
                    if (firestoreListeners.containsKey(syncKey)) continue

                    Log.d(TAG, "Attaching 24/7 background listener for syncKey: $syncKey (${account.name})")
                    var isInitial = true

                    // Listen to the statement root document to detect when the merchant deletes this account
                    val parentRegistration = firestore.collection("live_statements")
                        .document(syncKey)
                        .addSnapshotListener { snapshot, error ->
                            if (error == null && snapshot != null && snapshot.exists() && snapshot.getBoolean("isDeleted") == true) {
                                serviceScope.launch(Dispatchers.IO) {
                                    db.transactionDao().deleteTransactionsByAccountId(account.id)
                                    db.accountDao().deleteAccountById(account.id)
                                    firestoreListeners.remove(syncKey)?.remove()
                                    TawthiqNotificationManager.sendDueReminderPushNotification(
                                        context = applicationContext,
                                        accountName = account.name,
                                        amountText = "قام التاجر بحذف هذا الحساب نهائياً",
                                        isLana = false,
                                        dueDateMillis = System.currentTimeMillis()
                                    )
                                }
                            }
                        }

                    val registration = firestore.collection("live_statements")
                        .document(syncKey)
                        .collection("transactions")
                        .addSnapshotListener { snapshots, error ->
                            if (error != null) {
                                Log.w(TAG, "LiveSyncService listen failed: $syncKey", error)
                                return@addSnapshotListener
                            }
                            if (snapshots == null) return@addSnapshotListener

                            val wasInitial = isInitial
                            isInitial = false

                            serviceScope.launch(Dispatchers.IO) {
                                val repo = com.example.data.repository.TawthiqRepository(db.accountDao(), db.transactionDao())

                                for (dc in snapshots.documentChanges) {
                                    val doc = dc.document
                                    val amt = doc.getDouble("amount") ?: (doc.getString("amount")?.toDoubleOrNull() ?: 0.0)
                                    val type = doc.getString("type") ?: "LANA"
                                    val desc = doc.getString("description") ?: ""
                                    val cur = doc.getString("currency") ?: account.currency
                                    val date = doc.getLong("date") ?: System.currentTimeMillis()
                                    val receipt = doc.getString("receiptNumber") ?: ""
                                    val storeName = doc.getString("storeName") ?: "التاجر"
                                    val effectiveReceipt = if (receipt.isNotBlank()) receipt else doc.id

                                    val freshExistingTxs = db.transactionDao().getTransactionsForAccountOnce(account.id)

                                    when (dc.type) {
                                        DocumentChange.Type.ADDED -> {
                                            val exists = freshExistingTxs.any {
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

                                                // Fire push notification even when killed!
                                                if (!wasInitial || freshExistingTxs.isNotEmpty()) {
                                                    TawthiqNotificationManager.sendLiveTransactionPushNotification(
                                                        context = applicationContext,
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
                                        }
                                        DocumentChange.Type.MODIFIED -> {
                                            val matchedTx = freshExistingTxs.find {
                                                it.receiptNumber == doc.id || (receipt.isNotBlank() && it.receiptNumber == receipt) ||
                                                (it.date == date && kotlin.math.abs(it.amount - amt) < 0.0001 && it.type == type)
                                            }
                                            if (matchedTx != null) {
                                                db.transactionDao().updateTransaction(
                                                    matchedTx.copy(
                                                        amount = amt,
                                                        type = type,
                                                        description = desc,
                                                        currency = cur,
                                                        receiptNumber = effectiveReceipt
                                                    )
                                                )
                                                repo.deduplicateTransactionsForAccount(account.id)
                                            }
                                        }
                                        DocumentChange.Type.REMOVED -> {
                                            val matchedTx = freshExistingTxs.find {
                                                it.receiptNumber == doc.id || (receipt.isNotBlank() && it.receiptNumber == receipt) ||
                                                (it.date == date && kotlin.math.abs(it.amount - amt) < 0.0001 && it.type == type)
                                            }
                                            if (matchedTx != null) {
                                                db.transactionDao().deleteTransaction(matchedTx)
                                            }
                                        }
                                    }
                                }
                                repo.deduplicateTransactionsForAccount(account.id)
                            }
                        }

                    firestoreListeners[syncKey] = registration
                }

                // Attach 24/7 background listener for account status & direct messages
                val userEmail = prefs.getString("user_email", "")?.trim()?.lowercase() ?: ""
                if (userEmail.isNotBlank()) {
                    val cleanId = userEmail.replace(".", "_").replace("@", "_")
                    val cleanUser = userEmail.substringBefore("@")

                    // Status listener
                    val statusKey = "status_$cleanId"
                    if (!firestoreListeners.containsKey(statusKey)) {
                        val statusReg = firestore.collection("system_admin_users")
                            .document(cleanId)
                            .addSnapshotListener { snapshot, error ->
                                if (error == null && snapshot != null && snapshot.exists()) {
                                    val status = snapshot.getString("status") ?: "ACTIVE"
                                    val currentSaved = prefs.getString("account_status_$userEmail", "ACTIVE")
                                    if (!status.equals(currentSaved, ignoreCase = true)) {
                                        prefs.edit()
                                            .putString("account_status_$userEmail", status)
                                            .putString("account_status_$cleanUser", status)
                                            .apply()
                                        TawthiqNotificationManager.sendAccountStatusChangedNotification(
                                            context = applicationContext,
                                            newStatus = status
                                        )
                                    }
                                }
                            }
                        firestoreListeners[statusKey] = statusReg
                    }

                    // Direct message listener
                    val msgKey = "msgs_$cleanId"
                    if (!firestoreListeners.containsKey(msgKey)) {
                        var initialMsgs = true
                        val msgReg = firestore.collection("user_notifications")
                            .document(cleanId)
                            .collection("messages")
                            .addSnapshotListener { snapshot, error ->
                                if (error != null || snapshot == null) return@addSnapshotListener
                                val wasInit = initialMsgs
                                initialMsgs = false
                                if (!wasInit) {
                                    for (dc in snapshot.documentChanges) {
                                        if (dc.type == DocumentChange.Type.ADDED) {
                                            val doc = dc.document
                                            val title = doc.getString("title") ?: "رسالة خاصة جديدة"
                                            val message = doc.getString("message") ?: ""
                                            TawthiqNotificationManager.sendAdminDirectNotification(
                                                context = applicationContext,
                                                title = title,
                                                message = message
                                            )
                                        }
                                    }
                                }
                            }
                        firestoreListeners[msgKey] = msgReg
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error attaching listeners", e)
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "onTaskRemoved: Scheduling background sync worker and alarm")
        TawthiqBackgroundSyncManager.schedulePeriodicSync(applicationContext)
        TawthiqBackgroundSyncManager.scheduleWorkManager(applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "TawthiqLiveSyncService onDestroy")
        for (reg in firestoreListeners.values) {
            reg.remove()
        }
        firestoreListeners.clear()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
