package com.nammaspendai.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.nammaspendai.data.model.CategoryRule
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: CategoryRule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<CategoryRule>)

    @Update
    suspend fun update(rule: CategoryRule)

    @Delete
    suspend fun delete(rule: CategoryRule)

    @Query("DELETE FROM category_rules WHERE id = :ruleId")
    suspend fun deleteById(ruleId: String)

    @Query("SELECT * FROM category_rules ORDER BY priority DESC, keyword ASC")
    fun getAll(): Flow<List<CategoryRule>>

    @Query("SELECT * FROM category_rules WHERE isUserDefined = 1 ORDER BY priority DESC, keyword ASC")
    fun getUserDefinedRules(): Flow<List<CategoryRule>>

    @Query("SELECT * FROM category_rules WHERE isUserDefined = 0 ORDER BY priority DESC, keyword ASC")
    suspend fun getDefaultRules(): List<CategoryRule>

    @Query("SELECT * FROM category_rules WHERE keyword = :keyword LIMIT 1")
    suspend fun getByKeyword(keyword: String): CategoryRule?

    @Query("SELECT EXISTS(SELECT 1 FROM category_rules WHERE keyword = :keyword)")
    suspend fun exists(keyword: String): Boolean

    @Query("DELETE FROM category_rules WHERE isUserDefined = 1")
    suspend fun deleteAllUserRules()

    @Query("SELECT COUNT(*) FROM category_rules")
    suspend fun getCount(): Int
}
