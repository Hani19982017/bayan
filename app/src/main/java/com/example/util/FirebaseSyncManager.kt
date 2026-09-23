package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.TransactionEntity
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

object FirebaseSyncManager {

    private const val TAG = "FirebaseSyncManager"
    private const val COLLECTION_STATEMENTS = "live_statements"
    private const val COLLECTION_TRANSACTIONS = "transactions"

    private val activeListeners = ConcurrentHashMap<String, ListenerRegistration>()
    private val activeParentListeners = ConcurrentHashMap<String, ListenerRegistration>()

    /**
     * Generates a stable unique sync key for an account.
     */
    fun generateSyncKey(account: AccountEntity, merchantEmail: String = ""): String {
        if (account.syncKey.isNotBlank()) return account.syncKey.trim()
        val email = if (account.userEmail.isNotBlank()) account.userEmail else merchantEmail
        val cleanEmail = email.trim().replace(".", "_").replace("@", "_").replace(" ", "_")
        return if (cleanEmail.isNotBlank()) {
            "sync_${cleanEmail}_acc_${account.id}_${account.createdAt}"
        } else {
            "sync_acc_${account.id}_${account.createdAt}_${kotlin.math.abs(account.name.trim().hashCode())}"
        }
    }

    /**
     * Extracts sync key from account notes or generates it.
     */
    fun extractSyncKey(account: AccountEntity, merchantEmail: String = ""): String {
        if (account.syncKey.isNotBlank()) {
            return account.syncKey.trim()
        }
        val match = Regex("\\[SYNC:(.+?)\\]").find(account.notes)
        if (match != null) {
            return match.groupValues[1].trim()
        }
        return generateSyncKey(account, merchantEmail)
    }

    /**
     * Pushes a transaction to Firestore in real-time (Merchant Side).
     */
    fun pushTransaction(
        account: AccountEntity,
        transaction: TransactionEntity,
        storeName: String,
        merchantEmail: String = ""
    ) {
        try {
            val db = FirebaseFirestore.getInstance()
            val syncKey = extractSyncKey(account, merchantEmail)

            val txMap = hashMapOf<String, Any>(
                "id" to transaction.id,
                "accountId" to transaction.accountId,
                "syncKey" to syncKey,
                "userEmail" to (if (transaction.userEmail.isNotBlank()) transaction.userEmail else merchantEmail),
                "accountName" to account.name,
                "storeName" to storeName,
                "type" to transaction.type,
                "amount" to transaction.amount,
                "currency" to (if (transaction.currency.isNotBlank()) transaction.currency else account.currency),
                "description" to transaction.description,
                "date" to transaction.date,
                "dueDate" to (transaction.dueDate ?: 0L),
                "receiptNumber" to transaction.receiptNumber,
                "isSettled" to transaction.isSettled,
                "updatedAt" to System.currentTimeMillis()
            )

            val statementDoc = hashMapOf<String, Any>(
                "syncKey" to syncKey,
                "accountName" to account.name,
                "storeName" to storeName,
                "merchantEmail" to (if (account.userEmail.isNotBlank()) account.userEmail else merchantEmail),
                "currency" to account.currency,
                "lastUpdated" to System.currentTimeMillis()
            )

            // 1. Save in statement subcollection
            val txDocId = if (transaction.id > 0) transaction.id.toString() else "tx_${transaction.date}_${kotlin.math.abs(transaction.amount.hashCode())}"
            db.collection(COLLECTION_STATEMENTS).document(syncKey).set(statementDoc, SetOptions.merge())
            db.collection(COLLECTION_STATEMENTS).document(syncKey)
                .collection(COLLECTION_TRANSACTIONS)
                .document(txDocId)
                .set(txMap, SetOptions.merge())

            // 2. Also save in root transactions collection for global dashboard visibility
            db.collection(COLLECTION_TRANSACTIONS).document("${syncKey}_$txDocId").set(txMap, SetOptions.merge())

            Log.d(TAG, "Transaction pushed to Firestore live successfully: $txDocId (syncKey: $syncKey)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push transaction to Firestore", e)
        }
    }

    /**
     * Deletes an account from Firestore and marks it as deleted so all linked customer apps delete it in real-time.
     */
    fun deleteAccountEverywhere(account: AccountEntity, merchantEmail: String = "") {
        try {
            val db = FirebaseFirestore.getInstance()
            val syncKey = extractSyncKey(account, merchantEmail)
            val statementRef = db.collection(COLLECTION_STATEMENTS).document(syncKey)

            val deletePayload = hashMapOf<String, Any>(
                "isDeleted" to true,
                "status" to "DELETED",
                "accountName" to account.name,
                "deletedAt" to System.currentTimeMillis()
            )
            statementRef.set(deletePayload, SetOptions.merge())

            statementRef.collection(COLLECTION_TRANSACTIONS).get()
                .addOnSuccessListener { snapshot ->
                    for (doc in snapshot.documents) {
                        doc.reference.delete()
                        db.collection(COLLECTION_TRANSACTIONS).document("${syncKey}_${doc.id}").delete()
                    }
                }

            stopLiveSyncForAccount(account)
            Log.d(TAG, "Account marked as deleted in Firestore: $syncKey (${account.name})")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete account in Firestore", e)
        }
    }

    /**
     * Deletes a transaction from Firestore (Merchant Side).
     */
    fun deleteTransaction(account: AccountEntity, transactionId: Long, receiptNumber: String = "", merchantEmail: String = "") {
        try {
            val db = FirebaseFirestore.getInstance()
            val syncKey = extractSyncKey(account, merchantEmail)
            val txDocId = transactionId.toString()

            val statementTxsRef = db.collection(COLLECTION_STATEMENTS).document(syncKey).collection(COLLECTION_TRANSACTIONS)
            statementTxsRef.document(txDocId).delete()
            db.collection(COLLECTION_TRANSACTIONS).document("${syncKey}_$txDocId").delete()

            if (receiptNumber.isNotBlank() && receiptNumber != txDocId) {
                statementTxsRef.document(receiptNumber).delete()
                db.collection(COLLECTION_TRANSACTIONS).document("${syncKey}_$receiptNumber").delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete transaction from Firestore", e)
        }
    }

    /**
     * Starts Real-time Listening for Customer Account (Customer Side).
     * Automatically inserts new transactions into Room DB and fires instant push notification!
     */
    fun startLiveSyncForAccount(
        context: Context,
        account: AccountEntity,
        coroutineScope: CoroutineScope,
        onUpdate: () -> Unit = {}
    ) {
        val syncKey = extractSyncKey(account)
        if (activeListeners.containsKey(syncKey)) {
            Log.d(TAG, "Listener already active for syncKey: $syncKey - triggering reconciliation")
            reconcileAccountFromFirestore(context, account, coroutineScope, onUpdate)
            return
        }

        try {
            val db = FirebaseFirestore.getInstance()
            val query = db.collection(COLLECTION_STATEMENTS)
                .document(syncKey)
                .collection(COLLECTION_TRANSACTIONS)

            var isInitialLoad = true

            val registration = query.addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w(TAG, "Listen failed for syncKey: $syncKey", error)
                    return@addSnapshotListener
                }

                if (snapshots == null) {
                    return@addSnapshotListener
                }

                val wasInitial = isInitialLoad
                isInitialLoad = false

                coroutineScope.launch(Dispatchers.IO) {
                    val appDb = AppDatabase.getDatabase(context, coroutineScope)
                    val repo = com.example.data.repository.TawthiqRepository(appDb.accountDao(), appDb.transactionDao())

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

                        val freshExistingTxs = appDb.transactionDao().getTransactionsForAccountOnce(account.id)

                        when (dc.type) {
                            DocumentChange.Type.ADDED -> {
                                val alreadyExists = freshExistingTxs.any {
                                    it.receiptNumber == doc.id ||
                                    (receipt.isNotBlank() && it.receiptNumber == receipt) ||
                                    (it.date == date && kotlin.math.abs(it.amount - amt) < 0.0001 && it.type == type && it.description.trim() == desc.trim())
                                }

                                if (!alreadyExists && amt > 0.0) {
                                    appDb.transactionDao().insertTransaction(
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

                                    // Update account timestamp so list re-orders
                                    appDb.accountDao().updateAccount(account.copy(updatedAt = System.currentTimeMillis()))

                                    // Clean any duplicate records safely
                                    repo.deduplicateTransactionsForAccount(account.id)

                                    // Send push notification if live transaction arrived or after initial load
                                    if (!wasInitial || freshExistingTxs.isNotEmpty()) {
                                        launch(Dispatchers.Main) {
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
                                }
                            }
                            DocumentChange.Type.MODIFIED -> {
                                val matchedTx = freshExistingTxs.find { 
                                    it.receiptNumber == doc.id || (receipt.isNotBlank() && it.receiptNumber == receipt) ||
                                    (it.date == date && kotlin.math.abs(it.amount - amt) < 0.0001 && it.type == type)
                                }
                                if (matchedTx != null) {
                                    appDb.transactionDao().updateTransaction(
                                        matchedTx.copy(
                                            amount = amt,
                                            type = type,
                                            description = desc,
                                            currency = cur,
                                            receiptNumber = effectiveReceipt
                                        )
                                    )
                                }
                            }
                            DocumentChange.Type.REMOVED -> {
                                val matchedTx = freshExistingTxs.find { 
                                    it.receiptNumber == doc.id || (receipt.isNotBlank() && it.receiptNumber == receipt) ||
                                    (it.date == date && kotlin.math.abs(it.amount - amt) < 0.0001 && it.type == type)
                                }
                                if (matchedTx != null) {
                                    appDb.transactionDao().deleteTransaction(matchedTx)
                                }
                            }
                        }
                    }

                    // Reconcile deleted transactions if Firestore has fewer items
                    val currentLocalTxs = appDb.transactionDao().getTransactionsForAccountOnce(account.id)
                    val remoteDocIds = snapshots.documents.map { it.id }.toSet()
                    val remoteReceipts = snapshots.documents.mapNotNull { it.getString("receiptNumber") }.filter { it.isNotBlank() }.toSet()

                    if (snapshots.documents.isNotEmpty()) {
                        for (localTx in currentLocalTxs) {
                            if (localTx.receiptNumber.isNotBlank() &&
                                localTx.receiptNumber !in remoteDocIds &&
                                localTx.receiptNumber !in remoteReceipts
                            ) {
                                appDb.transactionDao().deleteTransaction(localTx)
                            }
                        }
                    }

                    repo.deduplicateTransactionsForAccount(account.id)

                    launch(Dispatchers.Main) {
                        onUpdate()
                    }
                }
            }

            activeListeners[syncKey] = registration

            // Also run a one-time pull to guarantee full sync immediately
            reconcileAccountFromFirestore(context, account, coroutineScope, onUpdate)

            // Also listen to the statement root document to detect when the merchant deletes this account
            val parentRegistration = db.collection(COLLECTION_STATEMENTS).document(syncKey)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists() && snapshot.getBoolean("isDeleted") == true) {
                        coroutineScope.launch(Dispatchers.IO) {
                            val appDb = AppDatabase.getDatabase(context, coroutineScope)
                            appDb.transactionDao().deleteTransactionsByAccountId(account.id)
                            appDb.accountDao().deleteAccountById(account.id)
                            val prefs = context.getSharedPreferences("tawthiq_prefs", Context.MODE_PRIVATE)
                            if (prefs.getLong("saved_customer_account_id", -1L) == account.id) {
                                prefs.edit().remove("saved_customer_account_id").putBoolean("is_customer_mode", false).apply()
                            }
                            stopLiveSyncForAccount(account)
                            launch(Dispatchers.Main) {
                                try {
                                    android.widget.Toast.makeText(
                                        context,
                                        "قام التاجر بحذف هذا الحساب (${account.name})",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                } catch (_: Exception) {}
                                onUpdate()
                            }
                        }
                    }
                }
            activeParentListeners[syncKey] = parentRegistration

            Log.d(TAG, "Started live Firestore sync listener for $syncKey")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting live sync", e)
        }
    }

    /**
     * One-time pull and reconcile to ensure no missed or previously dropped transactions.
     */
    fun reconcileAccountFromFirestore(
        context: Context,
        account: AccountEntity,
        coroutineScope: CoroutineScope,
        onUpdate: () -> Unit = {}
    ) {
        val syncKey = extractSyncKey(account)
        try {
            val db = FirebaseFirestore.getInstance()
            db.collection(COLLECTION_STATEMENTS).document(syncKey).collection(COLLECTION_TRANSACTIONS)
                .get()
                .addOnSuccessListener { snapshots ->
                    if (snapshots == null) return@addOnSuccessListener
                    coroutineScope.launch(Dispatchers.IO) {
                        val appDb = AppDatabase.getDatabase(context, coroutineScope)
                        val repo = com.example.data.repository.TawthiqRepository(appDb.accountDao(), appDb.transactionDao())
                        val freshExistingTxs = appDb.transactionDao().getTransactionsForAccountOnce(account.id)

                        for (doc in snapshots.documents) {
                            val amt = doc.getDouble("amount") ?: (doc.getString("amount")?.toDoubleOrNull() ?: 0.0)
                            val type = doc.getString("type") ?: "LANA"
                            val desc = doc.getString("description") ?: ""
                            val cur = doc.getString("currency") ?: account.currency
                            val date = doc.getLong("date") ?: System.currentTimeMillis()
                            val receipt = doc.getString("receiptNumber") ?: ""
                            val effectiveReceipt = if (receipt.isNotBlank()) receipt else doc.id

                            val alreadyExists = freshExistingTxs.any {
                                it.receiptNumber == doc.id ||
                                (receipt.isNotBlank() && it.receiptNumber == receipt) ||
                                (it.date == date && kotlin.math.abs(it.amount - amt) < 0.0001 && it.type == type && it.description.trim() == desc.trim())
                            }

                            if (!alreadyExists && amt > 0.0) {
                                appDb.transactionDao().insertTransaction(
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
                            }
                        }

                        // Reconcile deleted transactions
                        val currentLocalTxs = appDb.transactionDao().getTransactionsForAccountOnce(account.id)
                        val remoteDocIds = snapshots.documents.map { it.id }.toSet()
                        val remoteReceipts = snapshots.documents.mapNotNull { it.getString("receiptNumber") }.filter { it.isNotBlank() }.toSet()

                        if (snapshots.documents.isNotEmpty()) {
                            for (localTx in currentLocalTxs) {
                                if (localTx.receiptNumber.isNotBlank() &&
                                    localTx.receiptNumber !in remoteDocIds &&
                                    localTx.receiptNumber !in remoteReceipts
                                ) {
                                    appDb.transactionDao().deleteTransaction(localTx)
                                }
                            }
                        }

                        repo.deduplicateTransactionsForAccount(account.id)

                        launch(Dispatchers.Main) {
                            onUpdate()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Reconcile failed for $syncKey", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reconcile account from Firestore", e)
        }
    }

    /**
     * Stops listening when leaving screen or resetting.
     */
    fun stopLiveSyncForAccount(account: AccountEntity) {
        val syncKey = extractSyncKey(account)
        activeListeners.remove(syncKey)?.remove()
        activeParentListeners.remove(syncKey)?.remove()
    }
}
