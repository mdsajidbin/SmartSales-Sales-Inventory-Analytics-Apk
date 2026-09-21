package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SmartSalesRepository
import com.example.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class AppTab(val title: String) {
    DASHBOARD("Dashboard"),
    PRODUCTS("Products"),
    INVENTORY("Inventory"),
    SALES("Sales"),
    CUSTOMERS("Customers"),
    PROFILE("Profile")
}

class SmartSalesViewModel(private val repository: SmartSalesRepository) : ViewModel() {

    val currentUser: StateFlow<User?> = repository.currentUser
    val products: StateFlow<List<Product>> = repository.products
    val categories: StateFlow<List<Category>> = repository.categories
    val customers: StateFlow<List<Customer>> = repository.customers
    val allSales: StateFlow<List<Sale>> = repository.sales
    val stockAdjustments: StateFlow<List<StockAdjustment>> = repository.stockAdjustments
    val isOnline: StateFlow<Boolean> = repository.isOnline

    private val _currentTab = MutableStateFlow(AppTab.DASHBOARD)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _selectedDateRange = MutableStateFlow(DateRangeFilter.ALL_TIME)
    val selectedDateRange: StateFlow<DateRangeFilter> = _selectedDateRange.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Sales Cart State
    private val _cartItems = MutableStateFlow<List<SaleItem>>(emptyList())
    val cartItems: StateFlow<List<SaleItem>> = _cartItems.asStateFlow()

    // Scoped sales according to user role: Staff only sees own sales; Admin sees all
    val scopedSales: StateFlow<List<Sale>> = combine(allSales, currentUser) { sales, user ->
        if (user != null && user.isStaff()) {
            sales.filter { it.salesPersonId == user.id || it.salesPersonId.contains("staff") }
        } else {
            sales
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Computed Analytics KPIs
    val analyticsKPIs: StateFlow<AnalyticsKPIs> = combine(
        allSales,
        products,
        categories,
        customers,
        selectedDateRange
    ) { sales, productList, categoryList, customerList, range ->
        calculateAnalytics(sales, productList, categoryList, customerList, range)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AnalyticsKPIs())

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun setDateRange(range: DateRangeFilter) {
        _selectedDateRange.value = range
    }

    fun clearMessages() {
        _feedbackMessage.value = null
        _errorMessage.value = null
    }

    // ==========================================
    // CART OPERATIONS
    // ==========================================

    fun addToCart(product: Product, quantity: Int = 1) {
        val current = _cartItems.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.productId == product.id }

        val newQuantity = if (existingIndex >= 0) {
            current[existingIndex].quantity + quantity
        } else {
            quantity
        }

        if (newQuantity > product.stock) {
            _errorMessage.value = "Cannot add more. Only ${product.stock} available in stock for ${product.name}."
            return
        }

        if (existingIndex >= 0) {
            val existing = current[existingIndex]
            current[existingIndex] = existing.copy(
                quantity = newQuantity,
                lineTotal = newQuantity * existing.unitPrice
            )
        } else {
            current.add(
                SaleItem(
                    productId = product.id,
                    productName = product.name,
                    quantity = quantity,
                    unitPrice = product.price,
                    lineTotal = quantity * product.price
                )
            )
        }
        _cartItems.value = current
        _feedbackMessage.value = "Added '${product.name}' to cart."
    }

    fun updateCartQuantity(productId: String, quantity: Int) {
        val prod = products.value.find { it.id == productId }
        if (prod != null && quantity > prod.stock) {
            _errorMessage.value = "Only ${prod.stock} units available for ${prod.name}."
            return
        }

        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.productId == productId }
        if (index >= 0) {
            if (quantity <= 0) {
                current.removeAt(index)
            } else {
                val item = current[index]
                current[index] = item.copy(
                    quantity = quantity,
                    lineTotal = quantity * item.unitPrice
                )
            }
            _cartItems.value = current
        }
    }

    fun removeFromCart(productId: String) {
        _cartItems.value = _cartItems.value.filterNot { it.productId == productId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    // ==========================================
    // SALE CHECKOUT (With Automatic Stock Reduction)
    // ==========================================

    fun recordSingleSale(
        product: Product,
        quantity: Int,
        customerId: String = "walk_in",
        customerName: String = "Walk-in Customer",
        onResult: (Boolean) -> Unit = {}
    ) {
        if (quantity <= 0) {
            _errorMessage.value = "Sales quantity is required and must be greater than 0."
            onResult(false)
            return
        }
        if (quantity > product.stock) {
            _errorMessage.value = "Sales quantity ($quantity) cannot exceed available stock (${product.stock} units)."
            onResult(false)
            return
        }

        val saleItem = SaleItem(
            productId = product.id,
            productName = product.name,
            quantity = quantity,
            unitPrice = product.price,
            lineTotal = quantity * product.price
        )

        _isLoading.value = true
        _errorMessage.value = null
        repository.createSale(customerId, customerName, listOf(saleItem)) { result ->
            _isLoading.value = false
            result.onSuccess { sale ->
                val remaining = (product.stock - quantity).coerceAtLeast(0)
                _feedbackMessage.value = "Sale recorded! Sold $quantity units of '${product.name}'. Remaining stock: $remaining."
                onResult(true)
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to process sale."
                onCompleteSaleFailed(error.message ?: "Failed to process sale.")
                onResult(false)
            }
        }
    }

    private fun onCompleteSaleFailed(msg: String) {
        _errorMessage.value = msg
    }

    fun completeSale(customerId: String, customerName: String, onComplete: (Boolean) -> Unit) {
        val items = _cartItems.value
        if (items.isEmpty()) {
            _errorMessage.value = "Cart is empty. Please add products before checking out."
            onComplete(false)
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        repository.createSale(customerId, customerName, items) { result ->
            _isLoading.value = false
            result.onSuccess { sale ->
                clearCart()
                _feedbackMessage.value = "Sale #${sale.id.takeLast(6)} completed successfully! Amount: $${String.format(Locale.US, "%.2f", sale.totalAmount)}"
                onComplete(true)
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to process sale."
                onComplete(false)
            }
        }
    }

    // ==========================================
    // PRODUCT CRUD
    // ==========================================

    fun addProduct(product: Product, onResult: (Boolean) -> Unit = {}) {
        _isLoading.value = true
        repository.addProduct(product) { result ->
            _isLoading.value = false
            result.onSuccess {
                _feedbackMessage.value = "Product '${it.name}' added successfully."
                onResult(true)
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to add product."
                onResult(false)
            }
        }
    }

    fun updateProduct(product: Product, onResult: (Boolean) -> Unit = {}) {
        _isLoading.value = true
        repository.updateProduct(product) { result ->
            _isLoading.value = false
            result.onSuccess {
                _feedbackMessage.value = "Product '${it.name}' updated."
                onResult(true)
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to update product."
                onResult(false)
            }
        }
    }

    fun deleteProduct(productId: String) {
        val name = products.value.find { it.id == productId }?.name ?: "Product"
        _isLoading.value = true
        repository.deleteProduct(productId) { result ->
            _isLoading.value = false
            result.onSuccess {
                _feedbackMessage.value = "'$name' deleted successfully."
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to delete product."
            }
        }
    }

    // ==========================================
    // CATEGORY CRUD
    // ==========================================

    fun addCategory(name: String, onResult: (Boolean) -> Unit = {}) {
        if (name.isBlank()) {
            _errorMessage.value = "Category name cannot be empty."
            return
        }
        _isLoading.value = true
        repository.addCategory(name.trim()) { result ->
            _isLoading.value = false
            result.onSuccess {
                _feedbackMessage.value = "Category '${it.name}' added."
                onResult(true)
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to add category."
                onResult(false)
            }
        }
    }

    fun updateCategory(category: Category, onResult: (Boolean) -> Unit = {}) {
        _isLoading.value = true
        repository.updateCategory(category) { result ->
            _isLoading.value = false
            result.onSuccess {
                _feedbackMessage.value = "Category updated."
                onResult(true)
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to update category."
                onResult(false)
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        _isLoading.value = true
        repository.deleteCategory(categoryId) { result ->
            _isLoading.value = false
            result.onSuccess {
                _feedbackMessage.value = "Category deleted."
            }.onFailure {
                _errorMessage.value = it.message ?: "Could not delete category."
            }
        }
    }

    // ==========================================
    // CUSTOMER CRUD
    // ==========================================

    fun addCustomer(customer: Customer, onResult: (Boolean) -> Unit = {}) {
        if (customer.name.isBlank()) {
            _errorMessage.value = "Customer name is required."
            return
        }
        _isLoading.value = true
        repository.addCustomer(customer) { result ->
            _isLoading.value = false
            result.onSuccess {
                _feedbackMessage.value = "Customer '${it.name}' added."
                onResult(true)
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to add customer."
                onResult(false)
            }
        }
    }

    fun updateCustomer(customer: Customer, onResult: (Boolean) -> Unit = {}) {
        _isLoading.value = true
        repository.updateCustomer(customer) { result ->
            _isLoading.value = false
            result.onSuccess {
                _feedbackMessage.value = "Customer details updated."
                onResult(true)
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to update customer."
                onResult(false)
            }
        }
    }

    // ==========================================
    // INVENTORY ADJUSTMENTS (Admin Only)
    // ==========================================

    fun addStock(productId: String, quantityToAdd: Int, reason: String = "Restock", onResult: (Boolean) -> Unit = {}) {
        val user = currentUser.value
        if (user?.isAdmin() != true) {
            _errorMessage.value = "Access Denied: Only Admin can add stock."
            onResult(false)
            return
        }
        if (quantityToAdd <= 0) {
            _errorMessage.value = "Quantity to add must be greater than 0."
            onResult(false)
            return
        }
        _isLoading.value = true
        _errorMessage.value = null
        repository.addStock(productId, quantityToAdd, reason.trim()) { result ->
            _isLoading.value = false
            result.onSuccess { adj ->
                _feedbackMessage.value = "Stock increased for '${adj.productName}'! Added $quantityToAdd units (New Stock: ${adj.newStock})."
                onResult(true)
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to add stock."
                onResult(false)
            }
        }
    }

    fun adjustStock(productId: String, newStock: Int, reason: String, onResult: (Boolean) -> Unit = {}) {
        if (newStock < 0) {
            _errorMessage.value = "Stock quantity cannot be negative."
            return
        }
        if (reason.isBlank()) {
            _errorMessage.value = "Please provide an adjustment reason (e.g., restock, damage, audit correction)."
            return
        }
        _isLoading.value = true
        repository.adjustStock(productId, newStock, reason.trim()) { result ->
            _isLoading.value = false
            result.onSuccess {
                _feedbackMessage.value = "Stock updated for '${it.productName}' to $newStock units."
                onResult(true)
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to adjust stock."
                onResult(false)
            }
        }
    }

    fun seedDemoData() {
        _isLoading.value = true
        repository.seedDemoDataToFirebase { result ->
            _isLoading.value = false
            _feedbackMessage.value = "Demo data loaded successfully with sample products, categories, customers, and sales history!"
        }
    }

    // ==========================================
    // ANALYTICS COMPUTATION ENGINE
    // ==========================================

    private fun calculateAnalytics(
        allSales: List<Sale>,
        products: List<Product>,
        categories: List<Category>,
        customers: List<Customer>,
        dateRange: DateRangeFilter
    ): AnalyticsKPIs {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        val filterStartTime = when (dateRange) {
            DateRangeFilter.TODAY -> {
                cal.timeInMillis = now
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            DateRangeFilter.LAST_7_DAYS -> now - (7L * 24 * 60 * 60 * 1000)
            DateRangeFilter.LAST_30_DAYS -> now - (30L * 24 * 60 * 60 * 1000)
            DateRangeFilter.THIS_MONTH -> {
                cal.timeInMillis = now
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            DateRangeFilter.ALL_TIME -> 0L
        }

        val filteredSales = allSales.filter { it.saleDate >= filterStartTime }

        val totalRevenue = filteredSales.sumOf { it.totalAmount }
        val totalSalesCount = filteredSales.size
        val averageOrderValue = if (totalSalesCount > 0) totalRevenue / totalSalesCount else 0.0

        val totalProducts = products.size
        val lowStockCount = products.count { it.isLowStock() }
        val outOfStockCount = products.count { it.isOutOfStock() }
        val inStockCount = totalProducts - (lowStockCount + outOfStockCount)

        // Top Selling Product
        val productSalesMap = mutableMapOf<String, Int>()
        for (sale in filteredSales) {
            for (item in sale.items) {
                productSalesMap[item.productId] = (productSalesMap[item.productId] ?: 0) + item.quantity
            }
        }
        val topProductEntry = productSalesMap.maxByOrNull { it.value }
        val topSellingProductName = topProductEntry?.let { entry ->
            products.find { it.id == entry.key }?.name ?: "Product"
        } ?: if (filteredSales.isNotEmpty()) "Top Product" else "—"
        val topSellingQuantity = topProductEntry?.value ?: 0

        // Top Category by Revenue
        val categoryRevenueMap = mutableMapOf<String, Double>()
        val categoryItemsMap = mutableMapOf<String, Int>()
        for (sale in filteredSales) {
            for (item in sale.items) {
                val prod = products.find { it.id == item.productId }
                val catId = prod?.categoryId ?: "unknown"
                categoryRevenueMap[catId] = (categoryRevenueMap[catId] ?: 0.0) + item.lineTotal
                categoryItemsMap[catId] = (categoryItemsMap[catId] ?: 0) + item.quantity
            }
        }
        val topCategoryEntry = categoryRevenueMap.maxByOrNull { it.value }
        val topCategoryName = topCategoryEntry?.let { entry ->
            categories.find { it.id == entry.key }?.name ?: "Category"
        } ?: if (filteredSales.isNotEmpty()) "Top Category" else "—"
        val topCategoryRevenue = topCategoryEntry?.value ?: 0.0

        // Category breakdown points
        val categorySales = categories.mapNotNull { cat ->
            val rev = categoryRevenueMap[cat.id] ?: 0.0
            val count = categoryItemsMap[cat.id] ?: 0
            val pct = if (totalRevenue > 0) (rev / totalRevenue).toFloat() * 100f else 0f
            if (rev > 0.0 || count > 0) {
                CategorySalesPoint(cat.id, cat.name, rev, count, pct)
            } else null
        }.sortedByDescending { it.revenue }

        // Monthly / Periodic sales points
        val monthlySales = computePeriodicSales(filteredSales, dateRange)

        return AnalyticsKPIs(
            totalRevenue = totalRevenue,
            totalSalesCount = totalSalesCount,
            totalProducts = totalProducts,
            lowStockCount = lowStockCount,
            outOfStockCount = outOfStockCount,
            totalCustomers = customers.size,
            averageOrderValue = averageOrderValue,
            topSellingProductName = topSellingProductName,
            topSellingProductQuantity = topSellingQuantity,
            topCategoryName = topCategoryName,
            topCategoryRevenue = topCategoryRevenue,
            monthlySales = monthlySales,
            categorySales = categorySales,
            stockStatusSummary = StockStatusSummary(
                inStockCount = inStockCount.coerceAtLeast(0),
                lowStockCount = lowStockCount,
                outOfStockCount = outOfStockCount,
                totalCount = totalProducts
            )
        )
    }

    private fun computePeriodicSales(sales: List<Sale>, range: DateRangeFilter): List<MonthlySalesPoint> {
        val sdf = when (range) {
            DateRangeFilter.TODAY -> SimpleDateFormat("HH:00", Locale.getDefault())
            DateRangeFilter.LAST_7_DAYS -> SimpleDateFormat("EEE", Locale.getDefault())
            DateRangeFilter.LAST_30_DAYS, DateRangeFilter.THIS_MONTH -> SimpleDateFormat("dd MMM", Locale.getDefault())
            DateRangeFilter.ALL_TIME -> SimpleDateFormat("MMM yyyy", Locale.getDefault())
        }

        val grouped = sales.groupBy { sdf.format(Date(it.saleDate)) }
        return grouped.map { (label, salesList) ->
            MonthlySalesPoint(
                label = label,
                revenue = salesList.sumOf { it.totalAmount },
                salesCount = salesList.size
            )
        }
    }
}
