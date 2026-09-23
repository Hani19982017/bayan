package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE userEmail = :userEmail OR (:userEmail = '' AND userEmail = '') ORDER BY displayIndex ASC, id ASC")
    fun getAccountsByUser(userEmail: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY displayIndex ASC, id ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY displayIndex ASC, id ASC")
    suspend fun getAllAccountsOnce(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun getAccountById(id: Long): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountByIdOnce(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE userEmail = :userEmail AND category = :category ORDER BY displayIndex ASC, id ASC")
    fun getAccountsByCategory(userEmail: String, category: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE userEmail = :userEmail OR (:userEmail = '' AND userEmail = '') ORDER BY displayIndex ASC, id ASC")
    suspend fun getAccountsForUserOnce(userEmail: String): List<AccountEntity>

    @Query("SELECT MAX(displayIndex) FROM accounts WHERE userEmail = :userEmail")
    suspend fun getMaxDisplayIndex(userEmail: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccountById(id: Long)

    @Query("SELECT COUNT(*) FROM accounts WHERE userEmail = :userEmail")
    suspend fun getAccountCount(userEmail: String): Int

    @Query("UPDATE accounts SET currency = :currency WHERE userEmail = :userEmail OR :userEmail = ''")
    suspend fun updateAllAccountsCurrency(userEmail: String, currency: String)
}
