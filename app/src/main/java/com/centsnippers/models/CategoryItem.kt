package com.centsnippers.models

data class CategoryItem(
    var id: String = "", // Firestore document ID
    var userId: String = "", // Firebase UID
    var title: String = "",
    var description: String = "",
    var amount: Double = 0.0,
    var totalSpent: Double = 0.0,
    var transactionCount: Int = 0
)
