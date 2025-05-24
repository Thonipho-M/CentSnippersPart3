// ===========================================
// IncomeItem.kt — MODEL
// Purpose: Represents an individual income entry for a user.
// Used for: Storage in database, UI listing, filtering by cycle/month
// ===========================================

package com.centsnippers.models

import java.time.LocalDate

// =============================================================
// IncomeItem data class

// =============================================================
data class IncomeItem(
    val id: Int = 0,                     // Auto-generated ID from SQLite
    val userId: Int,                     // FK to associate income with a specific user
    val description: String,             // Example: "Job", "Freelance", "Grant"
    val amount: Double,                  // Default income amount per period (e.g., R10,000)
    val cycleType: CycleType,           // Monthly or Yearly recurrence
    val cycleStartDay: Int,             // e.g., 1st or 25th of the month
    val startDate: LocalDate,           // When this income began
    val endDate: LocalDate? = null,     // When this income ended (null = ongoing)
    val isActive: Boolean = true        // Controls if this income contributes to future months
)

