package com.nammaspendai.data.repository

import com.nammaspendai.data.dao.CategoryRuleDao
import com.nammaspendai.data.model.CategoryRule
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryRuleDao: CategoryRuleDao
) {
    val allRules: Flow<List<CategoryRule>> = categoryRuleDao.getAll()
    val userRules: Flow<List<CategoryRule>> = categoryRuleDao.getUserDefinedRules()

    suspend fun insert(rule: CategoryRule) {
        categoryRuleDao.insert(rule)
    }

    suspend fun insertAll(rules: List<CategoryRule>) {
        categoryRuleDao.insertAll(rules)
    }

    suspend fun update(rule: CategoryRule) {
        categoryRuleDao.update(rule)
    }

    suspend fun delete(rule: CategoryRule) {
        categoryRuleDao.delete(rule)
    }

    suspend fun deleteById(ruleId: String) {
        categoryRuleDao.deleteById(ruleId)
    }

    suspend fun getDefaultRules(): List<CategoryRule> {
        return categoryRuleDao.getDefaultRules()
    }

    suspend fun getByKeyword(keyword: String): CategoryRule? {
        return categoryRuleDao.getByKeyword(keyword)
    }

    suspend fun exists(keyword: String): Boolean {
        return categoryRuleDao.exists(keyword)
    }

    suspend fun deleteAllUserRules() {
        categoryRuleDao.deleteAllUserRules()
    }

    suspend fun getCount(): Int {
        return categoryRuleDao.getCount()
    }

    /**
     * Categorize text based on rules
     * Returns the category name or null if no match
     */
    fun categorize(text: String, rules: List<CategoryRule>): String {
        val lowercaseText = text.lowercase()

        // Sort by priority (highest first), then check each rule
        return rules
            .sortedByDescending { it.priority }
            .firstOrNull { rule ->
                lowercaseText.contains(rule.keyword.lowercase())
            }
            ?.category
            ?: "Other"
    }

    /**
     * Initialize default rules if not already present
     */
    suspend fun initializeDefaultRules() {
        if (categoryRuleDao.getCount() == 0) {
            categoryRuleDao.insertAll(CategoryRule.getDefaultRules())
        }
    }
}
