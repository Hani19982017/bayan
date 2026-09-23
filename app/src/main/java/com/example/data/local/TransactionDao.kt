package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userEmail = :userEmail OR (:userEmail = '' AND userEmail = '') ORDER BY date DESC")
    fun getTransactionsByUser(userEmail: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC, id DESC")
    fun getTransactionsForAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE (userEmail = :userEmail OR (:userEmail = '' AND userEmail = '')) AND dueDate IS NOT NULL AND isSettled = 0 ORDER BY dueDate ASC")
    fun getPendingDueTransactions(userEmail: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions WHERE accountId = :accountId")
    suspend fun deleteTransactionsByAccountId(accountId: Long)

    @Query("SELECT * FROM transactions WHERE accountId = :accountId")
    suspend fun getTransactionsForAccountOnce(accountId: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE userEmail = :userEmail")
    suspend fun getAllTransactionsSnapshot(userEmail: String): List<TransactionEntity>

    @Query("UPDATE transactions SET currency = :currency WHERE userEmail = :userEmail OR :userEmail = ''")
    suspend fun updateAllTransactionsCurrency(userEmail: String, currency: String)
}
