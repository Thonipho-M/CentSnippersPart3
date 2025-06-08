package com.centsnippers.models


data class CategoryItem(
    val id: Int,
    val userId: String,
    val title: String,
    val description: String = "",
    val amount: Double = 0.0,
    val iconResId: Int = 0,
    val totalSpent: Double = 0.0,
    val transactionCount: Int = 0
)
