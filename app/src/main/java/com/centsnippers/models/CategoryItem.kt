package com.centsnippers.models

data class CategoryItem(
    val id: Int,
    val userId: Int,
    val title: String,
    val description: String,
    val goalAmount: Double,             // New: target category budget
    val minSpend: Double? = null,       // Optional: budget lower bound
    val maxSpend: Double? = null,       // Optional: budget upper bound
    var totalSpent: Double = 0.0,
    var transactionCount: Int = 0,
    val colorHex: String = "#2196F3" // default blue

)
