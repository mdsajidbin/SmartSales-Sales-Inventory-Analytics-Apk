package com.example.model

data class StockAdjustment(
    val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val previousStock: Int = 0,
    val newStock: Int = 0,
    val changeAmount: Int = 0,
    val reason: String = "",
    val adjustedBy: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
