package com.example.data.repository

import com.example.data.local.AccountDao
import com.example.data.local.TransactionDao
import com.example.data.model.AccountEntity
import com.example.data.model.AccountWithBalance
import com.example.data.model.CurrencyBalance
import com.example.data.model.DueItem
import com.example.data.model.OverallSummary
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.concurrent.TimeUnit

class TawthiqRepository(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao
) {
    fun getAccountsForUser(userEmail: String): Flow<List<AccountEntity>> =
        accountDao.getAccountsByUser(userEmail)

    fun getTransactionsForUser(userEmail: String): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByUser(userEmail)

    /**
     * Combines accounts with their transactions to calculate real-time net balances for this specific user
     */
    fun getAccountsWithBalances(userEmail: String): Flow<List<AccountWithBalance>> =
        combine(getAccountsForUser(userEmail), getTransactionsForUser(userEmail)) { accounts, transactions ->
            val txByAccount = transactions.groupBy { it.accountId }

            accounts.map { account ->
                val txs = txByAccount[account.id] ?: emptyList()
                val latestTx = txs.maxByOrNull { it.date }

                // Group transactions by currency
                val currencyMap = txs.groupBy { it.currency }.mapValues { (_, curTxs) ->
                    val cLana = curTxs.filter { it.type == "LANA" }.sumOf { it.amount }
                    val cLaho = curTxs.filter { it.type == "LAHO" }.sumOf { it.amount }
                    cLana - cLaho
                }

                // Determine primary active currency:
                // 1. If transactions exist in the account's selected currency, use it.
                // 2. Otherwise, if there are transactions in other currencies (e.g. SAR), use the active currency.
                // 3. Otherwise, use the account's selected currency.
                val activeCurrency = when {
                    txs.any { it.currency.equals(account.currency, ignoreCase = true) } -> account.currency
                    currencyMap.any { it.value != 0.0 } -> currencyMap.filter { it.value != 0.0 }.keys.first()
                    txs.isNotEmpty() -> txs.groupBy { it.currency }.maxByOrNull { it.value.size }?.key ?: account.currency
                    else -> account.currency
                }

                val primaryCurTxs = txs.filter { it.currency.equals(activeCurrency, ignoreCase = true) }
                val lanaTotal = primaryCurTxs.filter { it.type == "LANA" }.sumOf { it.amount }
                val lahoTotal = primaryCurTxs.filter { it.type == "LAHO" }.sumOf { it.amount }
                val net = lanaTotal - lahoTotal

                // Credit limit check: exceeds limit if creditLimit > 0 and net debt >= creditLimit
                val isExceeded = account.creditLimit > 0.0 && (net >= account.creditLimit || lanaTotal >= account.creditLimit)

                AccountWithBalance(
                    account = account.copy(currency = activeCurrency),
                    totalLana = lanaTotal,
                    totalLaho = lahoTotal,
                    netBalance = net,
                    transactionCount = txs.size,
                    lastTransactionDate = latestTx?.date ?: account.updatedAt,
                    lastTransactionDescription = latestTx?.description,
                    isCreditLimitExceeded = isExceeded,
                    currencyBalances = currencyMap
                )
            }
        }

    /**
     * Overall financial health metrics for top dashboard cards
     */
    fun getOverallSummary(userEmail: String): Flow<OverallSummary> =
        combine(getAccountsWithBalances(userEmail), getCurrencyBalances(userEmail), transactionDao.getPendingDueTransactions(userEmail)) { balances, currencies, dueTxs ->
            var totalLana = 0.0
            var totalLaho = 0.0

            balances.forEach { b ->
                if (b.netBalance > 0) {
                    totalLana += b.netBalance
                } else if (b.netBalance < 0) {
                    totalLaho += -b.netBalance
                }
            }

            OverallSummary(
                totalLana = totalLana,
                totalLaho = totalLaho,
                netBalance = totalLana - totalLaho,
                totalAccounts = balances.size,
                dueSoonCount = dueTxs.size,
                currencySummaries = currencies
            )
        }

    /**
     * Grouping by currency for "أرصدة العملات" report
     */
    fun getCurrencyBalances(userEmail: String): Flow<List<CurrencyBalance>> =
        combine(getAccountsForUser(userEmail), getTransactionsForUser(userEmail)) { accounts, transactions ->
            val txByCurrency = transactions.groupBy { it.currency }
            val currencies = (accounts.map { it.currency } + transactions.map { it.currency }).distinct()

            currencies.map { cur ->
                val txs = txByCurrency[cur] ?: emptyList()
                val lana = txs.filter { it.type == "LANA" }.sumOf { it.amount }
                val laho = txs.filter { it.type == "LAHO" }.sumOf { it.amount }
                val affectedAccounts = txs.map { it.accountId }.distinct().count()

                CurrencyBalance(
                    currency = cur,
                    totalLana = lana,
                    totalLaho = laho,
                    netBalance = lana - laho,
                    accountCount = affectedAccounts
                )
            }.sortedByDescending { it.totalLana + it.totalLaho }
        }

    /**
     * Due transactions for "تقرير المستحقات"
     */
    fun getDueItems(userEmail: String): Flow<List<DueItem>> =
        combine(transactionDao.getPendingDueTransactions(userEmail), getAccountsForUser(userEmail)) { dueTxs, accounts ->
            val accountMap = accounts.associateBy { it.id }
            val now = System.currentTimeMillis()

            dueTxs.mapNotNull { tx ->
                val acc = accountMap[tx.accountId] ?: return@mapNotNull null
                val due = tx.dueDate ?: return@mapNotNull null
                val diffDays = TimeUnit.MILLISECONDS.toDays(due - now).toInt()

                DueItem(
                    transaction = tx,
                    accountName = acc.name,
                    accountPhone = acc.phone,
                    daysRemaining = diffDays
                )
            }.sortedBy { it.daysRemaining }
        }

    fun getTransactionsForAccount(accountId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsForAccount(accountId)

    fun getAccountById(accountId: Long): Flow<AccountEntity?> =
        accountDao.getAccountById(accountId)

    suspend fun sanitizeAndEnsureUniqueIndexes(userEmail: String) {
        val accounts = accountDao.getAccountsForUserOnce(userEmail)
        if (accounts.isEmpty()) return

        val seen = mutableSetOf<Int>()
        val hasDuplicatesOrZero = accounts.any { it.displayIndex <= 0 || !seen.add(it.displayIndex) }

        if (hasDuplicatesOrZero) {
            val assigned = mutableSetOf<Int>()
            // Sort by id to preserve creation order
            val sorted = accounts.sortedBy { it.id }
            var nextIndex = 1
            sorted.forEach { acc ->
                if (acc.displayIndex > 0 && !assigned.contains(acc.displayIndex) && accounts.count { it.displayIndex == acc.displayIndex } == 1) {
                    assigned.add(acc.displayIndex)
                } else {
                    while (assigned.contains(nextIndex)) {
                        nextIndex++
                    }
                    assigned.add(nextIndex)
                    accountDao.updateAccount(acc.copy(displayIndex = nextIndex, updatedAt = System.currentTimeMillis()))
                    nextIndex++
                }
            }
        }
    }

    private suspend fun getNextAvailableUniqueIndex(userEmail: String, requestedIndex: Int = 0, excludeAccountId: Long = 0L): Int {
        val accounts = accountDao.getAccountsForUserOnce(userEmail)
        val used = accounts.filter { it.id != excludeAccountId && it.displayIndex > 0 }.map { it.displayIndex }.toSet()
        if (requestedIndex > 0 && !used.contains(requestedIndex)) {
            return requestedIndex
        }
        var candidate = if (requestedIndex > 0) requestedIndex + 1 else 1
        while (used.contains(candidate)) {
            candidate++
        }
        return candidate
    }

    suspend fun addAccount(account: AccountEntity, initialBalance: Double = 0.0, initialType: String = "LANA"): Long {
        val finalIndex = getNextAvailableUniqueIndex(account.userEmail, account.displayIndex)
        val uniqueSyncKey = if (account.syncKey.isNotBlank()) account.syncKey else "tw_" + java.util.UUID.randomUUID().toString().replace("-", "").take(16)
        val preparedAccount = account.copy(
            displayIndex = finalIndex,
            syncKey = uniqueSyncKey
        )
        val id = accountDao.insertAccount(preparedAccount)
        if (initialBalance > 0) {
            transactionDao.insertTransaction(
                TransactionEntity(
                    userEmail = preparedAccount.userEmail,
                    accountId = id,
                    type = initialType,
                    amount = initialBalance,
                    currency = preparedAccount.currency,
                    description = "رصيد افتتاحي سابق",
                    date = System.currentTimeMillis()
                )
            )
        }
        return id
    }

    suspend fun updateAccount(account: AccountEntity) {
        val finalIndex = getNextAvailableUniqueIndex(account.userEmail, account.displayIndex, account.id)
        accountDao.updateAccount(account.copy(displayIndex = finalIndex, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteAccount(accountId: Long) {
        transactionDao.deleteTransactionsByAccountId(accountId)
        accountDao.deleteAccountById(accountId)
    }

    suspend fun addTransaction(transaction: TransactionEntity): Long {
        val id = transactionDao.insertTransaction(transaction)
        val acc = accountDao.getAccountByIdOnce(transaction.accountId)
        if (acc != null) {
            accountDao.updateAccount(acc.copy(updatedAt = transaction.date))
        }
        return id
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transactionId: Long) {
        transactionDao.deleteTransactionById(transactionId)
    }

    suspend fun reseedDemoData(userEmail: String) {
        val existingAccounts = accountDao.getAccountsForUserOnce(userEmail)
        existingAccounts.forEach { acc ->
            transactionDao.deleteTransactionsByAccountId(acc.id)
            accountDao.deleteAccountById(acc.id)
        }
    }

    suspend fun getAllTransactionsSnapshot(userEmail: String): List<TransactionEntity> {
        return transactionDao.getAllTransactionsSnapshot(userEmail)
    }

    suspend fun getTransactionsSnapshotForAccount(accountId: Long): List<TransactionEntity> {
        return transactionDao.getTransactionsForAccountOnce(accountId)
    }

    suspend fun importOrSyncCustomerStatement(
        userEmail: String,
        accountName: String,
        phone: String,
        storeName: String,
        currency: String,
        syncKey: String = "",
        transactionsList: List<TransactionEntity> = emptyList()
    ): Long {
        val existing = accountDao.getAccountsForUserOnce(userEmail)
        val matched = existing.find { it.name.trim().equals(accountName.trim(), ignoreCase = true) }
        val targetAccountId: Long
        val cleanCur = currency.ifBlank { "USD" }
        val effectiveSyncKey = if (syncKey.isNotBlank()) syncKey else "tw_" + java.util.UUID.randomUUID().toString().replace("-", "").take(16)
        val effectiveNotes = when {
            effectiveSyncKey.isNotBlank() && storeName.isNotBlank() -> "كشف حساب مرتبط مع: $storeName [SYNC:$effectiveSyncKey]"
            effectiveSyncKey.isNotBlank() -> "كشف حساب مرتبط عبر QR [SYNC:$effectiveSyncKey]"
            storeName.isNotBlank() -> "كشف حساب مرتبط مع: $storeName"
            else -> "كشف حساب مرتبط عبر QR"
        }

        if (matched != null) {
            targetAccountId = matched.id
            accountDao.updateAccount(matched.copy(
                phone = if (phone.isNotBlank()) phone else matched.phone,
                currency = cleanCur,
                notes = effectiveNotes,
                syncKey = effectiveSyncKey,
                updatedAt = System.currentTimeMillis()
            ))
        } else {
            val nextIndex = getNextAvailableUniqueIndex(userEmail)
            val newAcc = AccountEntity(
                userEmail = userEmail,
                name = accountName.trim(),
                phone = phone.trim(),
                category = "حسابات متابعة",
                currency = cleanCur,
                notes = effectiveNotes,
                displayIndex = nextIndex,
                syncKey = effectiveSyncKey
            )
            targetAccountId = accountDao.insertAccount(newAcc)
        }

        val existingTxs = transactionDao.getTransactionsForAccountOnce(targetAccountId)
        transactionsList.forEach { tx ->
            val alreadyExists = existingTxs.any { 
                (tx.receiptNumber.isNotBlank() && it.receiptNumber.isNotBlank() && it.receiptNumber == tx.receiptNumber) ||
                (it.date == tx.date && kotlin.math.abs(it.amount - tx.amount) < 0.0001 && it.type == tx.type && it.description.trim() == tx.description.trim())
            }
            if (!alreadyExists && tx.amount > 0.0) {
                transactionDao.insertTransaction(
                    tx.copy(
                        id = 0L,
                        accountId = targetAccountId,
                        userEmail = userEmail,
                        currency = if (tx.currency.isNotBlank()) tx.currency else cleanCur
                    )
                )
            }
        }
        deduplicateTransactionsForAccount(targetAccountId)
        return targetAccountId
    }

    /**
     * Eliminates truly duplicated transaction rows (identical receipt number or identical exact millisecond timestamp).
     * Preserves genuine multiple payments/charges of the same amount!
     */
    suspend fun deduplicateTransactionsForAccount(accountId: Long): Int {
        val txs = transactionDao.getTransactionsForAccountOnce(accountId)
        if (txs.size <= 1) return 0

        val toDelete = mutableListOf<TransactionEntity>()
        val seen = mutableListOf<TransactionEntity>()

        // Prioritize keeping transactions with non-blank receipt number or older stable ID
        val sorted = txs.sortedWith(
            compareBy<TransactionEntity> { if (it.receiptNumber.isNotBlank()) 0 else 1 }
                .thenBy { it.id }
        )

        for (tx in sorted) {
            val duplicate = seen.find { s ->
                val sameReceipt = tx.receiptNumber.isNotBlank() && s.receiptNumber.isNotBlank() && tx.receiptNumber == s.receiptNumber
                val exactSameRow = tx.date == s.date &&
                        kotlin.math.abs(tx.amount - s.amount) < 0.0001 &&
                        tx.type == s.type &&
                        tx.currency.equals(s.currency, ignoreCase = true) &&
                        tx.description.trim() == s.description.trim()

                sameReceipt || exactSameRow
            }

            if (duplicate != null) {
                toDelete.add(tx)
            } else {
                seen.add(tx)
            }
        }

        if (toDelete.isNotEmpty()) {
            for (del in toDelete) {
                transactionDao.deleteTransactionById(del.id)
            }
        }
        return toDelete.size
    }

    suspend fun deduplicateAllAccounts(): Int {
        val allAccounts = accountDao.getAllAccountsOnce()
        var totalDeleted = 0
        for (acc in allAccounts) {
            totalDeleted += deduplicateTransactionsForAccount(acc.id)
        }
        return totalDeleted
    }

    suspend fun updateAllCurrencies(userEmail: String, currency: String) {
        val cleanCur = currency.trim().ifBlank { "USD" }
        accountDao.updateAllAccountsCurrency(userEmail, cleanCur)
        transactionDao.updateAllTransactionsCurrency(userEmail, cleanCur)
    }

    suspend fun getAccountsForUserSnapshot(userEmail: String): List<AccountEntity> {
        return accountDao.getAccountsForUserOnce(userEmail)
    }

    suspend fun getAllAccountsSnapshot(): List<AccountEntity> {
        return accountDao.getAllAccountsOnce()
    }
}
