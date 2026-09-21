package com.example.model

enum class DateRangeFilter(val label: String) {
    TODAY("Today"),
    LAST_7_DAYS("7 Days"),
    LAST_30_DAYS("30 Days"),
    THIS_MONTH("This Month"),
    ALL_TIME("All Time")
}

data class MonthlySalesPoint(
    val label: String, // e.g., "Jan", "Feb" or "Mon", "Tue"
    val revenue: Double,
    val salesCount: Int
)

data class CategorySalesPoint(
    val categoryId: String,
    val categoryName: String,
    val revenue: Double,
    val itemsSold: Int,
    val percentage: Float
)

data class StockStatusSummary(
    val inStockCount: Int = 0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val totalCount: Int = 0
)

data class AnalyticsKPIs(
    val totalRevenue: Double = 0.0,
    val totalSalesCount: Int = 0,
    val totalProducts: Int = 0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val totalCustomers: Int = 0,
    val averageOrderValue: Double = 0.0,
    val topSellingProductName: String = "—",
    val topSellingProductQuantity: Int = 0,
    val topCategoryName: String = "—",
    val topCategoryRevenue: Double = 0.0,
    val monthlySales: List<MonthlySalesPoint> = emptyList(),
    val categorySales: List<CategorySalesPoint> = emptyList(),
    val stockStatusSummary: StockStatusSummary = StockStatusSummary()
)
