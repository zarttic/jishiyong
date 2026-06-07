package com.jishiyong.data.db.converter

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateConverterTest {

    private val converter = DateConverter()

    @Test
    fun toLocalDateParsesIsoDate() {
        assertEquals(LocalDate.of(2026, 6, 7), converter.toLocalDate("2026-06-07"))
    }

    @Test
    fun toLocalDateUsesStableFallbackForBlankValue() {
        assertEquals(LocalDate.of(1970, 1, 1), converter.toLocalDate(""))
    }

    @Test
    fun toLocalDateUsesStableFallbackForInvalidValue() {
        assertEquals(LocalDate.of(1970, 1, 1), converter.toLocalDate("not-a-date"))
    }
}
