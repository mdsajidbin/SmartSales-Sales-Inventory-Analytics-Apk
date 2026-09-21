package com.example.model

data class SaleItem(
    val productId: String = "",
    val productName: String = "",
    val quantity: Int = 0,
    val unitPrice: Double = 0.0,
    val lineTotal: Double = 0.0
)

data class Sale(
    val id: String = "",
    val items: List<SaleItem> = emptyList(),
    val customerId: String = "",
    val customerName: String = "",
    val totalAmount: Double = 0.0,
    val saleDate: Long = System.currentTimeMillis(),
    val salesPersonId: String = "",
    val salesPersonName: String = ""
)
