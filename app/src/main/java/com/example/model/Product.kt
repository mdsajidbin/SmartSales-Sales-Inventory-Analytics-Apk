package com.example.model

enum class StockStatus(val label: String) {
    IN_STOCK("In Stock"),
    LOW_STOCK("Low Stock"),
    OUT_OF_STOCK("Out of Stock")
}

data class Product(
    val id: String = "",
    val name: String = "",
    val categoryId: String = "",
    val price: Double = 0.0,
    val stock: Int = 0,
    val lowStockThreshold: Int = 5,
    val imageUrl: String = "",
    val supplier: String = ""
) {
    fun getStockStatus(): StockStatus {
        return when {
            stock <= 0 -> StockStatus.OUT_OF_STOCK
            stock <= lowStockThreshold -> StockStatus.LOW_STOCK
            else -> StockStatus.IN_STOCK
        }
    }

    fun isLowStock(): Boolean = stock in 1..lowStockThreshold
    fun isOutOfStock(): Boolean = stock <= 0
}
