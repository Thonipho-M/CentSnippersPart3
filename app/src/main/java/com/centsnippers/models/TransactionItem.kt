package com.centsnippers.models

data class TransactionItem(
    val id: Int = 0,
    val userId: Int,
    val categoryId: Int,
    val title: String,
    val description: String,
    val amount: Double,
    val startDate: String,
    val endDate: String,
    val imageUrl: String? = null
)
