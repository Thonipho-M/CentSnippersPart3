package com.centsnippers.models

data class TransactionItem(
    val id: String = "",
    val userId: String = "",
    val type: String = "expense",
    val categoryId: Int,         // Firestore category document ID
    val title: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val Date: String = "",
    val imageUrl: String? = null
)
