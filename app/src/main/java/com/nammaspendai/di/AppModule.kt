package com.nammaspendai.di

import android.content.Context
import com.nammaspendai.data.local.NammaSpendDatabase
import com.nammaspendai.data.repository.CategoryRepository
import com.nammaspendai.data.repository.TransactionRepository
import com.nammaspendai.util.PredictionEngine
import com.nammaspendai.util.SecurityManager
import com.nammaspendai.util.SmsImporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NammaSpendDatabase {
        return NammaSpendDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideTransactionDao(database: NammaSpendDatabase) = database.transactionDao()

    @Provides
    @Singleton
    fun provideCategoryRuleDao(database: NammaSpendDatabase) = database.categoryRuleDao()

    @Provides
    @Singleton
    fun provideUsageRecordDao(database: NammaSpendDatabase) = database.usageRecordDao()

    @Provides
    @Singleton
    fun providePredictionEngine(): PredictionEngine = PredictionEngine()

    @Provides
    @Singleton
    fun provideSecurityManager(@ApplicationContext context: Context): SecurityManager =
        SecurityManager(context)

    @Provides
    @Singleton
    fun provideSmsImporter(
        @ApplicationContext context: Context,
        transactionRepository: TransactionRepository,
        categoryRepository: CategoryRepository
    ): SmsImporter = SmsImporter(context, transactionRepository, categoryRepository)
}
