package com.jishiyong.agent

import com.jishiyong.data.db.entity.Item

class InventoryItemMatcher(
    private val categoryInferencer: CategoryInferencer = CategoryInferencer()
) {

    fun matchItem(
        itemName: String,
        quantity: Int,
        activeItems: List<Item>,
        itemId: Long? = null
    ): InventoryMatchResult {
        if (quantity <= 0) {
            return InventoryMatchResult.NotFound("数量必须大于 0")
        }

        val itemById = itemId?.let { id -> activeItems.firstOrNull { it.id == id } }
        if (itemById != null) {
            val remainingQuantity = itemById.remainingQuantity()
            return if (remainingQuantity >= quantity) {
                InventoryMatchResult.Matched(itemById)
            } else {
                InventoryMatchResult.NotFound("“${itemById.name}”剩余 $remainingQuantity，不能操作 $quantity")
            }
        }

        val normalizedName = InventoryTextNormalizer.compact(itemName)
        if (normalizedName.isBlank()) {
            return InventoryMatchResult.NotFound("请确认物品名称")
        }

        val availableItems = activeItems
            .filter { it.remainingQuantity() >= quantity }
            .sortedWith(compareBy<Item> { it.expirationDate }.thenBy { it.name })

        val exactMatches = availableItems.filter {
            InventoryTextNormalizer.compact(it.name) == normalizedName
        }
        exactMatches.singleOrNull()?.let { return InventoryMatchResult.Matched(it) }
        if (exactMatches.size > 1) {
            return InventoryMatchResult.NeedsSelection(
                candidates = exactMatches,
                message = "找到多个“$itemName”，请选择要操作的库存"
            )
        }

        val containsMatches = availableItems.filter {
            val itemNameNormalized = InventoryTextNormalizer.compact(it.name)
            itemNameNormalized.contains(normalizedName) || normalizedName.contains(itemNameNormalized)
        }
        containsMatches.singleOrNull()?.let { return InventoryMatchResult.Matched(it) }
        if (containsMatches.size > 1) {
            return InventoryMatchResult.NeedsSelection(
                candidates = containsMatches,
                message = "找到多个相关物品，请选择要操作的库存"
            )
        }

        val inferredCategory = categoryInferencer.infer(itemName)
        val similarMatches = availableItems.filter {
            it.category == inferredCategory && isSimilarName(normalizedName, InventoryTextNormalizer.compact(it.name))
        }
        similarMatches.singleOrNull()?.let { return InventoryMatchResult.Matched(it) }
        if (similarMatches.size > 1) {
            return InventoryMatchResult.NeedsSelection(
                candidates = similarMatches,
                message = "找到多个相似物品，请选择要操作的库存"
            )
        }

        return InventoryMatchResult.NotFound("没有找到“$itemName”的可用库存")
    }

    private fun isSimilarName(query: String, candidate: String): Boolean {
        if (query.length < 2 || candidate.length < 2) return false
        val queryChars = query.toSet()
        val candidateChars = candidate.toSet()
        val overlap = queryChars.intersect(candidateChars).size
        return overlap >= 2 && overlap.toFloat() / queryChars.size >= 0.5f
    }
}
