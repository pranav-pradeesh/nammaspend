package com.nammaspendai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.util.UUID

@Entity(tableName = "category_rules")
data class CategoryRule(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val keyword: String,
    val category: String,
    val priority: Int = 0, // Higher priority rules match first
    val isUserDefined: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        // Default rules for common merchants
        fun getDefaultRules(): List<CategoryRule> = listOf(
            CategoryRule(keyword = "swiggy", category = "Food", isUserDefined = false, priority = 10),
            CategoryRule(keyword = "zomato", category = "Food", isUserDefined = false, priority = 10),
            CategoryRule(keyword = "uber eats", category = "Food", isUserDefined = false, priority = 10),
            CategoryRule(keyword = "amazon", category = "Shopping", isUserDefined = false, priority = 10),
            CategoryRule(keyword = "flipkart", category = "Shopping", isUserDefined = false, priority = 10),
            CategoryRule(keyword = "myntra", category = "Shopping", isUserDefined = false, priority = 10),
            CategoryRule(keyword = "uber", category = "Transport", isUserDefined = false, priority = 10),
            CategoryRule(keyword = "ola", category = "Transport", isUserDefined = false, priority = 10),
            CategoryRule(keyword = "rapido", category = "Transport", isUserDefined = false, priority = 10),
            CategoryRule(keyword = "bookmyshow", category = "Entertainment", isUserDefined = false, priority = 10),
            CategoryRule(keyword = "netflix", category = "Entertainment", isUserDefined = false, priority = 10),
            CategoryRule(keyword = "prime", category = "Entertainment", isUserDefined = false, priority = 10),
            CategoryRule(keyword = "disney", category = "Entertainment", isUserDefined = false, priority = 10),
            CategoryRule(keyword = "reliance", category = "Utilities", isUserDefined = false, priority = 10),
            CategoryRule(keyword = "electricity", category = "Utilities", isUserDefined = false, priority = 10),
            CategoryRule(keyword = "water", category = "Utilities", isUserDefined = false, priority = 10),
            CategoryRule(keyword = "lpg", category = "Utilities", isUserDefined = false, priority = 10),
            CategoryRule(keyword = "gas", category = "Utilities", isUserDefined = false, priority = 10),
            CategoryRule(keyword = "mobile", category = "Recharge", isUserDefined = false, priority = 10),
            CategoryRule(keyword = "jio", category = "Recharge", isUserDefined = false, priority = 10),
            CategoryRule(keyword = "airtel", category = "Recharge", isUserDefined = false, priority = 10),
            CategoryRule(keyword = "hospital", category = "Medical", isUserDefined = false, priority = 10),
            CategoryRule(keyword = "pharmacy", category = "Medical", isUserDefined = false, priority = 10),
            CategoryRule(keyword = "medical", category = "Medical", isUserDefined = false, priority = 10),
            CategoryRule(keyword = "fuel", category = "Fuel", isUserDefined = false, priority = 10),
            CategoryRule(keyword = "petrol", category = "Fuel", isUserDefined = false, priority = 10),
            CategoryRule(keyword = "diesel", category = "Fuel", isUserDefined = false, priority = 10),
            CategoryRule(keyword = "emi", category = "Finance", isUserDefined = false, priority = 10),
            CategoryRule(keyword = "loan", category = "Finance", isUserDefined = false, priority = 10),
            CategoryRule(keyword = "insurance", category = "Finance", isUserDefined = false, priority = 10),
            CategoryRule(keyword = "salary", category = "Income", isUserDefined = false, priority = 10),
            CategoryRule(keyword = "refund", category = "Income", isUserDefined = false, priority = 10),
            CategoryRule(keyword = "cashback", category = "Income", isUserDefined = false, priority = 10)
        )

        val DEFAULT_CATEGORIES = listOf(
            "Food",
            "Shopping",
            "Transport",
            "Entertainment",
            "Utilities",
            "Recharge",
            "Medical",
            "Fuel",
            "Finance",
            "Education",
            "Travel",
            "Gifts",
            "Income",
            "Other"
        )
    }
}
