package com.jishiyong.agent

import com.jishiyong.data.db.entity.ItemCategory

class CategoryInferencer {

    fun infer(name: String): ItemCategory {
        val normalizedName = InventoryTextNormalizer.compact(name)
        return when {
            normalizedName.containsAny(drinkKeywords) -> ItemCategory.DRINK
            normalizedName.containsAny(medicineKeywords) -> ItemCategory.MEDICINE
            normalizedName.containsAny(cosmeticsKeywords) -> ItemCategory.COSMETICS
            normalizedName.containsAny(dailyKeywords) -> ItemCategory.DAILY
            normalizedName.containsAny(electronicsKeywords) -> ItemCategory.ELECTRONICS
            normalizedName.containsAny(clothingKeywords) -> ItemCategory.CLOTHING
            normalizedName.containsAny(foodKeywords) -> ItemCategory.FOOD
            else -> ItemCategory.OTHER
        }
    }

    private fun String.containsAny(keywords: List<String>): Boolean {
        return keywords.any { contains(it) }
    }

    private companion object {
        private val drinkKeywords = listOf("奶", "牛奶", "酸奶", "饮料", "茶", "水", "咖啡", "果汁", "可乐", "啤酒", "东方树叶")
        private val foodKeywords = listOf("饭", "面", "肉", "菜", "蛋", "水果", "面包", "饼干", "零食", "米", "油", "盐", "糖")
        private val medicineKeywords = listOf("药", "片", "胶囊", "口服液", "感冒", "退烧", "消炎")
        private val cosmeticsKeywords = listOf("面霜", "口红", "粉底", "精华", "乳液", "防晒", "洗面奶", "化妆")
        private val dailyKeywords = listOf("纸", "洗衣", "牙膏", "牙刷", "沐浴", "洗发", "肥皂", "清洁")
        private val electronicsKeywords = listOf("电池", "充电", "耳机", "数据线", "手机")
        private val clothingKeywords = listOf("衣", "裤", "袜", "鞋", "帽")
    }
}
