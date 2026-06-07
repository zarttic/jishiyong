package com.jishiyong.data.repository

import com.jishiyong.data.db.entity.Item
import com.jishiyong.data.db.entity.ItemCategory
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ReminderLogicTest {

    private val today = LocalDate.of(2026, 6, 5)

    @Test
    fun triggeredReminderLevelsReturnsMatchingDay() {
        val item = item(expirationDate = today.plusDays(7), reminderDays = listOf(7, 3, 1))

        val levels = getTriggeredReminderLevels(item, today)

        assertEquals(listOf(7), levels)
    }

    @Test
    fun triggeredReminderLevelsReturnsMissedLevelInsideReminderWindow() {
        val item = item(expirationDate = today.plusDays(5), reminderDays = listOf(7, 3, 1))

        val levels = getTriggeredReminderLevels(item, today)

        assertEquals(listOf(7), levels)
    }

    @Test
    fun triggeredReminderLevelsReturnsEmptyForExpiredItem() {
        val item = item(expirationDate = today.minusDays(1), reminderDays = listOf(7, 3, 1))

        val levels = getTriggeredReminderLevels(item, today)

        assertEquals(emptyList<Int>(), levels)
    }

    @Test
    fun triggeredReminderLevelsReturnsDistinctSortedEnteredWindows() {
        val item = item(expirationDate = today.plusDays(3), reminderDays = listOf(3, 7, 3, 1))

        val levels = getTriggeredReminderLevels(item, today)

        assertEquals(listOf(3, 7), levels)
    }

    @Test
    fun triggeredReminderLevelsSupportsCustomLongReminderWindow() {
        val item = item(expirationDate = today.plusDays(45), reminderDays = listOf(60, 30, 7))

        val levels = getTriggeredReminderLevels(item, today)

        assertEquals(listOf(60), levels)
    }

    private fun item(
        expirationDate: LocalDate,
        reminderDays: List<Int>
    ): Item {
        return Item(
            id = 1,
            name = "牛奶",
            category = ItemCategory.DRINK,
            purchaseDate = today.minusDays(1),
            expirationDate = expirationDate,
            reminderDays = reminderDays
        )
    }
}
