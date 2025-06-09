package com.centsnippers.models

data class TransactionItem(
    val id: Int = 0,
    val userId: Int,
    val type: String = "expense",
    val categoryId: Int,
    val title: String,
    val description: String,
    val amount: Double,
    val Date: String,
    val imageUrl: String? = null
)
