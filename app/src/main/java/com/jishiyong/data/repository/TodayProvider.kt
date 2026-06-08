package com.jishiyong.data.repository

import java.time.LocalDate

fun interface TodayProvider {
    fun today(): LocalDate
}

object SystemTodayProvider : TodayProvider {
    override fun today(): LocalDate = LocalDate.now()
}
