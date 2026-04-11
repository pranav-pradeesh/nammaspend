package com.nammaspendai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nammaspendai.data.dao.CategoryRuleDao
import com.nammaspendai.data.dao.TransactionDao
import com.nammaspendai.data.dao.UsageRecordDao
import com.nammaspendai.data.model.CategoryRule
import com.nammaspendai.data.model.Transaction
import com.nammaspendai.data.model.UsageRecord
import com.nammaspendai.util.LocalDateTimeConverter
import com.nammaspendai.util.LocalDateConverter
import com.nammaspendai.util.MapConverter

@Database(
    entities = [
        Transaction::class,
        CategoryRule::class,
        UsageRecord::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(
    LocalDateTimeConverter::class,
    LocalDateConverter::class,
    MapConverter::class
)
abstract class NammaSpendDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryRuleDao(): CategoryRuleDao
    abstract fun usageRecordDao(): UsageRecordDao

    companion object {
        @Volatile
        private var INSTANCE: NammaSpendDatabase? = null

        fun getDatabase(context: Context): NammaSpendDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NammaSpendDatabase::class.java,
                    "nammaspend.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
