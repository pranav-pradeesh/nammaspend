package com.nammaspendai.data.repository

import com.nammaspendai.data.dao.TransactionDao
import com.nammaspendai.data.model.Transaction
import com.nammaspendai.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAll()

    fun getRecent(limit: Int): Flow<List<Transaction>> =
        transactionDao.getRecent(limit)

    fun getByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<Transaction>> =
        transactionDao.getBetweenDates(startDate, endDate)

    fun getByCategory(category: String): Flow<List<Transaction>> =
        transactionDao.getByCategory(category)

    fun getByType(type: TransactionType): Flow<List<Transaction>> =
        transactionDao.getByType(type)

    fun search(query: String): Flow<List<Transaction>> =
        transactionDao.search(query)

    suspend fun insert(transaction: Transaction): Boolean {
        val id = transactionDao.insert(transaction)
        return id != -1L
    }

    suspend fun insertAll(transactions: List<Transaction>): List<Long> {
        return transactionDao.insertAll(transactions)
    }

    suspend fun update(transaction: Transaction) {
        transactionDao.update(transaction)
    }

    suspend fun delete(transaction: Transaction) {
        transactionDao.delete(transaction)
    }

    suspend fun deleteById(transactionId: String) {
        transactionDao.deleteById(transactionId)
    }

    suspend fun getById(transactionId: String): Transaction? {
        return transactionDao.getById(transactionId)
    }

    suspend fun existsWithSmsHash(hash: String): Boolean {
        return transactionDao.getBySmsHash(hash) != null
    }

    suspend fun getTotalExpenses(startDate: LocalDateTime, endDate: LocalDateTime): Double {
        return transactionDao.getSumByTypeAndDateRange(TransactionType.DEBIT, startDate, endDate) ?: 0.0
    }

    suspend fun getTotalIncome(startDate: LocalDateTime, endDate: LocalDateTime): Double {
        return transactionDao.getSumByTypeAndDateRange(TransactionType.CREDIT, startDate, endDate) ?: 0.0
    }

    suspend fun getCategoryTotals(startDate: LocalDateTime, endDate: LocalDateTime) =
        transactionDao.getCategoryTotals(startDate, endDate)

    suspend fun getMonthlyTotals() =
        transactionDao.getMonthlyTotals()

    suspend fun getTransactionCount(): Int {
        return transactionDao.getCount()
    }

    suspend fun getSmsTransactionCount(): Int {
        return transactionDao.getSmsTransactionCount()
    }

    suspend fun deleteAll() {
        transactionDao.deleteAll()
    }
}
