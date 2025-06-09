package com.centsnippers

import org.junit.Assert.*
import org.junit.Test

class IncomeLogicTest {
    @Test
    fun testMonthlyIncomeCalculation() {
        val baseIncome = 1000.0
        val months = 12
        val total = baseIncome * months
        assertEquals(12000.0, total, 0.001)
    }

    @Test
    fun testYearlyBonusAdded() {
        val monthly = 1000.0 * 12
        val bonus = 500.0
        val total = monthly + bonus
        assertEquals(12500.0, total, 0.001)
    }
}