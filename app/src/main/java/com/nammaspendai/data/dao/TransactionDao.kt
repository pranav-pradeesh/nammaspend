package com.nammaspendai.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.nammaspendai.data.model.Transaction
import com.nammaspendai.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<Transaction>): List<Long>

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteById(transactionId: String)

    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    suspend fun getById(transactionId: String): Transaction?

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    fun getBetweenDates(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY timestamp DESC")
    fun getByCategory(category: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY timestamp DESC")
    fun getByType(type: TransactionType): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE smsHash = :hash LIMIT 1")
    suspend fun getBySmsHash(hash: String): Transaction?

    @Query("SELECT * FROM transactions WHERE merchant LIKE '%' || :searchTerm || '%' OR description LIKE '%' || :searchTerm || '%' ORDER BY timestamp DESC")
    fun search(searchTerm: String): Flow<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type AND timestamp BETWEEN :startDate AND :endDate")
    suspend fun getSumByTypeAndDateRange(type: TransactionType, startDate: LocalDateTime, endDate: LocalDateTime): Double?

    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE type = 'DEBIT' AND timestamp BETWEEN :startDate AND :endDate GROUP BY category ORDER BY total DESC")
    suspend fun getCategoryTotals(startDate: LocalDateTime, endDate: LocalDateTime): List<CategoryTotal>

    @Query("SELECT strftime('%Y-%m', timestamp) as month, SUM(CASE WHEN type = 'DEBIT' THEN amount ELSE 0 END) as expenses, SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE 0 END) as income FROM transactions GROUP BY month ORDER BY month")
    suspend fun getMonthlyTotals(): List<MonthlyTotal>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE source = 'SMS'")
    suspend fun getSmsTransactionCount(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE smsHash = :hash)")
    suspend fun existsWithHash(hash: Boolean): Boolean

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

data class CategoryTotal(
    val category: String,
    val total: Double
)

data class MonthlyTotal(
    val month: String,
    val expenses: Double,
    val income: Double
)
