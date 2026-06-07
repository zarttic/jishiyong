package com.jishiyong.agent

import com.jishiyong.data.db.entity.ItemCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryInferencerTest {

    private val inferencer = CategoryInferencer()

    @Test
    fun inferDrinkCategoryFromItemName() {
        assertEquals(ItemCategory.DRINK, inferencer.infer("低温酸奶"))
    }

    @Test
    fun inferDailyCategoryFromItemName() {
        assertEquals(ItemCategory.DAILY, inferencer.infer("洗衣凝珠"))
    }

    @Test
    fun unknownNameFallsBackToOther() {
        assertEquals(ItemCategory.OTHER, inferencer.infer("备用收纳盒"))
    }
}
