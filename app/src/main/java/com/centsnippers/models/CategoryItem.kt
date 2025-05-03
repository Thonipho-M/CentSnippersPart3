package com.centsnippers.models

data class CategoryItem(
    val id: Int,
    val userId: Int,
    val title: String,
    val description: String,
    val amount: Double,
    var totalSpent: Double = 0.0,
    var transactionCount: Int = 0
)
