package com.nammaspendai.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.nammaspendai.data.model.Transaction
import com.opencsv.CSVWriter
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.CreationHelper
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ExportManager(private val context: Context) {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val fileDateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

    /**
     * Export transactions to CSV file
     */
    fun exportToCsv(
        transactions: List<Transaction>,
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null
    ): Result<Uri> {
        return try {
            val filteredTransactions = filterByDateRange(transactions, startDate, endDate)
            if (filteredTransactions.isEmpty()) {
                return Result.failure(Exception("No transactions to export"))
            }

            val fileName = "expenses_${fileDateFormatter.format(LocalDateTime.now())}.csv"
            val file = File(context.cacheDir, fileName)

            CSVWriter(FileWriter(file)).use { writer ->
                // Header
                writer.writeNext(arrayOf(
                    "Date", "Type", "Category", "Amount", "Merchant", "Description", "Source"
                ))

                // Data rows
                filteredTransactions.forEach { transaction ->
                    writer.writeNext(arrayOf(
                        dateFormatter.format(transaction.timestamp),
                        transaction.type.name,
                        transaction.category,
                        transaction.amount.toString(),
                        transaction.merchant ?: "",
                        transaction.description ?: "",
                        transaction.source.name
                    ))
                }
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Export transactions to Excel (.xlsx) file
     */
    fun exportToExcel(
        transactions: List<Transaction>,
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null
    ): Result<Uri> {
        return try {
            val filteredTransactions = filterByDateRange(transactions, startDate, endDate)
            if (filteredTransactions.isEmpty()) {
                return Result.failure(Exception("No transactions to export"))
            }

            val fileName = "expenses_${fileDateFormatter.format(LocalDateTime.now())}.xlsx"
            val file = File(context.cacheDir, fileName)

            XSSFWorkbook().use { workbook ->
                val sheet = workbook.createSheet("Expenses")

                // Create date cell style
                val dateStyle: CellStyle = workbook.createCellStyle().apply {
                    val creationHelper: CreationHelper = workbook.creationHelper
                    dataFormat = creationHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss")
                }

                // Header row
                val headerRow = sheet.createRow(0)
                val headers = arrayOf(
                    "Date", "Type", "Category", "Amount", "Merchant", "Description", "Source"
                )
                headers.forEachIndexed { index, header ->
                    headerRow.createCell(index).setCellValue(header)
                }

                // Data rows
                filteredTransactions.forEachIndexed { rowIndex, transaction ->
                    val row = sheet.createRow(rowIndex + 1)

                    // Date cell with formatting
                    row.createCell(0).apply {
                        setCellValue(transaction.timestamp.toString())
                        cellStyle = dateStyle
                    }

                    row.createCell(1).setCellValue(transaction.type.name)
                    row.createCell(2).setCellValue(transaction.category)
                    row.createCell(3).setCellValue(transaction.amount)
                    row.createCell(4).setCellValue(transaction.merchant ?: "")
                    row.createCell(5).setCellValue(transaction.description ?: "")
                    row.createCell(6).setCellValue(transaction.source.name)
                }

                // Auto-size columns
                for (i in headers.indices) {
                    sheet.autoSizeColumn(i)
                }

                // Write to file
                file.outputStream().use { outputStream ->
                    workbook.write(outputStream)
                }
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Share exported file
     */
    fun shareFile(uri: Uri, mimeType: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Expenses")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private fun filterByDateRange(
        transactions: List<Transaction>,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?
    ): List<Transaction> {
        return transactions.filter { transaction ->
            val afterStart = startDate?.let { !transaction.timestamp.isBefore(it) } ?: true
            val beforeEnd = endDate?.let { !transaction.timestamp.isAfter(it) } ?: true
            afterStart && beforeEnd
        }
    }
}
