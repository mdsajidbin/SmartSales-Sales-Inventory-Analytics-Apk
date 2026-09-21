package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Category
import com.example.model.Customer
import com.example.model.Product
import com.example.model.StockStatus
import com.example.model.User
import com.example.ui.components.AddSaleDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LoadingView
import com.example.ui.components.StockStatusBadge
import com.example.ui.theme.*
import com.example.viewmodel.SmartSalesViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    viewModel: SmartSalesViewModel,
    currentUser: User?,
    modifier: Modifier = Modifier
) {
    val isAdmin = currentUser?.isAdmin() ?: true
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedStatusFilter by remember { mutableStateOf<StockStatus?>(null) }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var productForStockAddition by remember { mutableStateOf<Product?>(null) }
    var productForSale by remember { mutableStateOf<Product?>(null) }

    // Filter products
    val filteredProducts = products.filter { prod ->
        val matchesSearch = prod.name.contains(searchQuery, ignoreCase = true) ||
                prod.supplier.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategoryId == null || prod.categoryId == selectedCategoryId
        val matchesStatus = selectedStatusFilter == null || prod.getStockStatus() == selectedStatusFilter
        matchesSearch && matchesCategory && matchesStatus
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("products_screen"),
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = {
                        editingProduct = null
                        showAddEditDialog = true
                    },
                    containerColor = SmartIndigo,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("add_product_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Product")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Input Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("product_search_input"),
                placeholder = { Text("Search products by name or supplier...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = SmartIndigo)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SmartIndigo,
                    unfocusedBorderColor = SmartCardBorder
                )
            )

            // Category Filter Chips Row
            val uniqueCategories = remember(categories) { categories.distinctBy { it.id } }
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { selectedCategoryId = null },
                        label = { Text("All Categories") },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SmartIndigo,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                items(uniqueCategories, key = { "cat_filter_${it.id}" }) { cat ->
                    FilterChip(
                        selected = selectedCategoryId == cat.id,
                        onClick = { selectedCategoryId = if (selectedCategoryId == cat.id) null else cat.id },
                        label = { Text(cat.name) },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SmartIndigo,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Stock Status Filter Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedStatusFilter == null,
                        onClick = { selectedStatusFilter = null },
                        label = { Text("All Stock") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = selectedStatusFilter == StockStatus.IN_STOCK,
                        onClick = {
                            selectedStatusFilter = if (selectedStatusFilter == StockStatus.IN_STOCK) null else StockStatus.IN_STOCK
                        },
                        label = { Text("In Stock") },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StatusGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedStatusFilter == StockStatus.LOW_STOCK,
                        onClick = {
                            selectedStatusFilter = if (selectedStatusFilter == StockStatus.LOW_STOCK) null else StockStatus.LOW_STOCK
                        },
                        label = { Text("Low Stock") },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StatusAmber,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedStatusFilter == StockStatus.OUT_OF_STOCK,
                        onClick = {
                            selectedStatusFilter = if (selectedStatusFilter == StockStatus.OUT_OF_STOCK) null else StockStatus.OUT_OF_STOCK
                        },
                        label = { Text("Out of Stock") },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StatusRed,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Products List or Loading / Empty States
            if (isLoading && products.isEmpty()) {
                LoadingView(message = "Loading products...")
            } else if (filteredProducts.isEmpty()) {
                EmptyStateView(
                    title = "No Products Found",
                    description = if (searchQuery.isNotEmpty() || selectedCategoryId != null || selectedStatusFilter != null) {
                        "No products match your filter criteria. Clear filters to see all."
                    } else {
                        "No products added yet. Tap '+' to create your first inventory item."
                    },
                    icon = Icons.Default.Inventory2,
                    actionLabel = if (isAdmin) "Add First Product" else null,
                    onActionClick = if (isAdmin) {
                        {
                            editingProduct = null
                            showAddEditDialog = true
                        }
                    } else null
                )
            } else {
                val uniqueFilteredProducts = remember(filteredProducts) { filteredProducts.distinctBy { it.id } }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uniqueFilteredProducts, key = { "prod_${it.id}" }) { product ->
                        val categoryName = categories.find { it.id == product.categoryId }?.name ?: "Uncategorized"

                        ProductItemCard(
                            product = product,
                            categoryName = categoryName,
                            isAdmin = isAdmin,
                            onEdit = {
                                editingProduct = product
                                showAddEditDialog = true
                            },
                            onDelete = {
                                productToDelete = product
                            },
                            onAddStock = {
                                productForStockAddition = product
                            },
                            onAddSale = {
                                productForSale = product
                            },
                            onAddToCart = {
                                viewModel.addToCart(product, 1)
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Product Dialog (Admin Only)
    if (isAdmin && showAddEditDialog) {
        AddEditProductDialog(
            product = editingProduct,
            categories = categories,
            onDismiss = { showAddEditDialog = false },
            onSave = { product ->
                if (editingProduct == null) {
                    viewModel.addProduct(product) { showAddEditDialog = false }
                } else {
                    viewModel.updateProduct(product) { showAddEditDialog = false }
                }
            }
        )
    }

    // Delete Confirmation Dialog (Admin Only)
    if (isAdmin && productToDelete != null) {
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Delete Product", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete '${productToDelete?.name}'? Historical sales records will be preserved.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        productToDelete?.let { viewModel.deleteProduct(it.id) }
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Stock Dialog (Admin Only)
    if (isAdmin && productForStockAddition != null) {
        AddStockDialog(
            product = productForStockAddition!!,
            onDismiss = { productForStockAddition = null },
            onConfirm = { quantityToAdd ->
                viewModel.addStock(productForStockAddition!!.id, quantityToAdd)
                productForStockAddition = null
            }
        )
    }

    // Add Sale Dialog (Admin and Staff)
    if (productForSale != null) {
        AddSaleDialog(
            product = productForSale!!,
            customers = customers,
            onDismiss = { productForSale = null },
            onConfirm = { quantity, custId, custName ->
                viewModel.recordSingleSale(
                    product = productForSale!!,
                    quantity = quantity,
                    customerId = custId,
                    customerName = custName
                )
                productForSale = null
            }
        )
    }
}

@Composable
fun ProductItemCard(
    product: Product,
    categoryName: String,
    isAdmin: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddStock: () -> Unit,
    onAddSale: () -> Unit,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("product_item_${product.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Product Icon box
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SmartIndigoLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = SmartIndigo,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Info Column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SmartTextPrimary,
                        maxLines = 1
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.bodySmall,
                            color = SmartTextSecondary
                        )
                        if (product.supplier.isNotBlank()) {
                            Text(
                                text = "· ${product.supplier}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SmartTextMuted,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", product.price)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SmartIndigo
                        )

                        StockStatusBadge(
                            status = product.getStockStatus(),
                            stockCount = product.stock
                        )
                    }
                }

                // Admin Edit & Delete Quick Icons
                if (isAdmin) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit product",
                                tint = SmartTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete product",
                                tint = StatusRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Action Buttons
            // Admin: [ + Add Stock ]  [ + Add Sale ]
            // Staff: [ + Add Sale ] (Add Stock completely restricted and hidden!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isAdmin) {
                    Button(
                        onClick = onAddStock,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SmartIndigoLight,
                            contentColor = SmartIndigo
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_stock_btn_${product.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddBusiness,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Stock", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Button(
                    onClick = onAddSale,
                    enabled = product.stock > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (product.stock > 0) SmartCyanDark else SmartTextMuted,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_sale_btn_${product.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PointOfSale,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (product.stock > 0) "Add Sale" else "Out of Stock",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AddStockDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (quantityToAdd: Int) -> Unit
) {
    var quantityText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val quantityToAdd = quantityText.toIntOrNull() ?: 0
    val newStock = product.stock + quantityToAdd

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Add Stock", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SmartIndigo,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Stock Calculation Preview Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SmartIndigoLight.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, SmartIndigo.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Current Stock:", style = MaterialTheme.typography.bodyMedium, color = SmartTextSecondary)
                            Text("${product.stock} units", fontWeight = FontWeight.Bold, color = SmartTextPrimary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("+ Adding:", style = MaterialTheme.typography.bodyMedium, color = SmartIndigo)
                            Text(
                                if (quantityToAdd > 0) "+$quantityToAdd units" else "—",
                                fontWeight = FontWeight.Bold,
                                color = SmartIndigo
                            )
                        }
                        HorizontalDivider(color = SmartIndigo.copy(alpha = 0.3f), thickness = 1.dp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("= New Total Stock:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                if (quantityToAdd > 0) "$newStock units" else "${product.stock} units",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (quantityToAdd > 0) StatusGreen else SmartTextPrimary
                            )
                        }
                    }
                }

                // Quick Increment Chips
                Text("Quick Add:", style = MaterialTheme.typography.labelMedium, color = SmartTextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 10, 20, 50).forEach { amount ->
                        SuggestionChip(
                            onClick = {
                                val current = quantityText.toIntOrNull() ?: 0
                                quantityText = (current + amount).toString()
                                errorMessage = null
                            },
                            label = { Text("+$amount", fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Quantity Input Field
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        quantityText = digits
                        errorMessage = null
                    },
                    label = { Text("Quantity to Add *") },
                    placeholder = { Text("e.g. 20") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(errorMessage!!, color = StatusRed)
                        } else {
                            Text("Enter the quantity to increase stock.")
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_stock_quantity_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityText.toIntOrNull()
                    if (qty == null || qty <= 0) {
                        errorMessage = "Quantity is required and must be greater than 0."
                    } else {
                        onConfirm(qty)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SmartIndigo),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_stock_btn")
            ) {
                Text("Save Stock", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductDialog(
    product: Product?,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    val isEdit = product != null
    var name by remember { mutableStateOf(product?.name ?: "") }
    var selectedCategoryId by remember {
        mutableStateOf(product?.categoryId ?: (categories.firstOrNull()?.id ?: ""))
    }
    var priceText by remember { mutableStateOf(if (product != null) "${product.price}" else "") }
    var stockText by remember { mutableStateOf(if (product != null) "${product.stock}" else "10") }
    var lowStockThresholdText by remember {
        mutableStateOf(if (product != null) "${product.lowStockThreshold}" else "5")
    }
    var supplier by remember { mutableStateOf(product?.supplier ?: "") }
    var imageUrl by remember { mutableStateOf(product?.imageUrl ?: "") }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "Edit Product" else "Add New Product",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorText != null) {
                    Text(
                        text = errorText ?: "",
                        color = StatusRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name *") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("product_dialog_name")
                )

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    val currentCatName = categories.find { it.id == selectedCategoryId }?.name ?: "Select Category"
                    OutlinedTextField(
                        value = currentCatName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Price ($) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("product_dialog_price")
                    )

                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it },
                        label = { Text("Stock Qty *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("product_dialog_stock")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = lowStockThresholdText,
                        onValueChange = { lowStockThresholdText = it },
                        label = { Text("Alert Below") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = supplier,
                        onValueChange = { supplier = it },
                        label = { Text("Supplier") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorText = "Product name is required"
                        return@Button
                    }
                    val price = priceText.toDoubleOrNull() ?: -1.0
                    if (price < 0) {
                        errorText = "Please enter a valid price"
                        return@Button
                    }
                    val stock = stockText.toIntOrNull() ?: -1
                    if (stock < 0) {
                        errorText = "Please enter valid stock"
                        return@Button
                    }
                    val threshold = lowStockThresholdText.toIntOrNull() ?: 5

                    onSave(
                        Product(
                            id = product?.id ?: "",
                            name = name.trim(),
                            categoryId = selectedCategoryId,
                            price = price,
                            stock = stock,
                            lowStockThreshold = threshold,
                            imageUrl = imageUrl.trim(),
                            supplier = supplier.trim()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = SmartIndigo),
                modifier = Modifier.testTag("product_dialog_save_btn")
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
