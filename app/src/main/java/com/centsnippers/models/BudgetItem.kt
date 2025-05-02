package com.centsnippers.models

data class BudgetItem(
    val id: Int,
    val userId: Int,
    val category: String,
    val amount: Double,
    val description : String,
    val imageUrl: String? = null
)