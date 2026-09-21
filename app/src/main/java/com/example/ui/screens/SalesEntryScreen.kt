package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Customer
import com.example.model.Product
import com.example.model.SaleItem
import com.example.ui.components.AddSaleDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StockStatusBadge
import com.example.ui.theme.*
import com.example.viewmodel.SmartSalesViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesEntryScreen(
    viewModel: SmartSalesViewModel,
    modifier: Modifier = Modifier
) {
    val products by viewModel.products.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var productSearchQuery by remember { mutableStateOf("") }
    var productForSale by remember { mutableStateOf<Product?>(null) }
    var selectedCustomer by remember { mutableStateOf<Customer?>(customers.firstOrNull()) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var showQuickAddCustomerDialog by remember { mutableStateOf(false) }
    var showCheckoutSummaryDialog by remember { mutableStateOf(false) }
    var saleSuccessDialog by remember { mutableStateOf(false) }
    var lastCompletedTotal by remember { mutableStateOf(0.0) }

    val uniqueAvailableProducts = remember(products, productSearchQuery) {
        val q = productSearchQuery.trim()
        products.filter { prod ->
            val matchesQuery = q.isBlank() ||
                prod.name.contains(q, ignoreCase = true) ||
                (q.contains("arabian", ignoreCase = true) && prod.name.contains("arabica", ignoreCase = true)) ||
                (q.contains("arabica", ignoreCase = true) && prod.name.contains("arabian", ignoreCase = true)) ||
                (q.contains("coffee", ignoreCase = true) && prod.name.contains("coffee", ignoreCase = true))
            matchesQuery
        }.distinctBy { it.id }
    }
    val uniqueCartItems = remember(cartItems) { cartItems.distinctBy { it.productId } }

    val totalAmount = cartItems.sumOf { it.lineTotal }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("sales_entry_screen"),
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${cartItems.sumOf { it.quantity }} items in cart",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SmartTextSecondary
                                )
                                Text(
                                    text = "Total: $${String.format(Locale.US, "%.2f", totalAmount)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = SmartIndigo
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.clearCart() },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Clear", color = StatusRed)
                                }

                                Button(
                                    onClick = { showCheckoutSummaryDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = SmartIndigo),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("checkout_button")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Complete Sale", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header / Customer Selector Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sales_customer_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(SmartIndigoLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = SmartIndigo,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Customer",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SmartTextSecondary
                                )
                                Text(
                                    text = selectedCustomer?.name ?: "Walk-in Customer",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SmartTextPrimary
                                )
                                if (selectedCustomer != null && selectedCustomer!!.phone.isNotBlank()) {
                                    Text(
                                        text = selectedCustomer!!.phone,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SmartTextMuted
                                    )
                                }
                            }
                        }

                        Row {
                            TextButton(onClick = { showCustomerPicker = true }) {
                                Text("Change", color = SmartIndigo, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { showQuickAddCustomerDialog = true }) {
                                Icon(Icons.Default.PersonAdd, contentDescription = "Add Customer", tint = SmartIndigo)
                            }
                        }
                    }
                }
            }

            // Current Cart Items List
            if (uniqueCartItems.isNotEmpty()) {
                item {
                    Text(
                        text = "Sale Cart (${uniqueCartItems.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SmartTextPrimary
                    )
                }

                items(uniqueCartItems, key = { "cart_${it.productId}" }) { item ->
                    val prod = products.find { it.id == item.productId }
                    val maxStock = prod?.stock ?: 99

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cart_item_${item.productId}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.productName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SmartTextPrimary
                                )
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", item.unitPrice)} each · Stock: $maxStock",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SmartTextSecondary
                                )
                            }

                            // Stepper Controls
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        viewModel.updateCartQuantity(item.productId, item.quantity - 1)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease", tint = StatusRed)
                                }

                                Text(
                                    text = "${item.quantity}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SmartTextPrimary,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )

                                IconButton(
                                    onClick = {
                                        viewModel.updateCartQuantity(item.productId, item.quantity + 1)
                                    },
                                    enabled = item.quantity < maxStock,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AddCircleOutline,
                                        contentDescription = "Increase",
                                        tint = if (item.quantity < maxStock) SmartIndigo else SmartTextMuted
                                    )
                                }

                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", item.lineTotal)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SmartIndigo,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Products Catalog for Adding Items
            item {
                Text(
                    text = "Add Products to Cart",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SmartTextPrimary
                )
            }

            item {
                OutlinedTextField(
                    value = productSearchQuery,
                    onValueChange = { productSearchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pos_product_search"),
                    placeholder = { Text("Search catalog to add items...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SmartIndigo) },
                    trailingIcon = {
                        if (productSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { productSearchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (uniqueAvailableProducts.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "No In-Stock Products",
                        description = "All products matching this query are currently out of stock or not found.",
                        icon = Icons.Default.ProductionQuantityLimits
                    )
                }
            } else {
                items(uniqueAvailableProducts, key = { "pos_prod_${it.id}" }) { product ->
                    val inCart = cartItems.find { it.productId == product.id }

                    Card(
                        onClick = { productForSale = product },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pos_item_${product.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = SmartTextPrimary
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "$${String.format(Locale.US, "%.2f", product.price)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SmartIndigo
                                    )
                                    StockStatusBadge(
                                        status = product.getStockStatus(),
                                        stockCount = product.stock
                                    )
                                }
                            }

                            Button(
                                onClick = { productForSale = product },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (product.stock <= 0) Color.Gray else if (inCart != null) SmartCyanDark else SmartIndigo
                                ),
                                enabled = product.stock > 0,
                                modifier = Modifier.testTag("pos_add_btn_${product.id}")
                            ) {
                                Icon(
                                    imageVector = if (product.stock <= 0) Icons.Default.Block else if (inCart != null) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (product.stock <= 0) "Out of Stock" else if (inCart != null) "${inCart.quantity} in cart" else "Add",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Customer Selection Dialog
    if (showCustomerPicker) {
        AlertDialog(
            onDismissRequest = { showCustomerPicker = false },
            title = { Text("Select Customer", fontWeight = FontWeight.Bold) },
            text = {
                val uniqueCustomers = remember(customers) { customers.distinctBy { it.id } }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Card(
                            onClick = {
                                selectedCustomer = null
                                showCustomerPicker = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = SmartBgLight)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Walk-in Customer (Guest)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    items(uniqueCustomers, key = { "picker_cust_${it.id}" }) { cust ->
                        Card(
                            onClick = {
                                selectedCustomer = cust
                                showCustomerPicker = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedCustomer?.id == cust.id) SmartIndigoLight else SmartBgLight
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Text(cust.name, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${cust.phone} · ${cust.email}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SmartTextSecondary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCustomerPicker = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Quick Add Customer Dialog
    if (showQuickAddCustomerDialog) {
        var custName by remember { mutableStateOf("") }
        var custPhone by remember { mutableStateOf("") }
        var custEmail by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showQuickAddCustomerDialog = false },
            title = { Text("Quick Add Customer", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = custName,
                        onValueChange = { custName = it },
                        label = { Text("Customer Name *") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = custPhone,
                        onValueChange = { custPhone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = custEmail,
                        onValueChange = { custEmail = it },
                        label = { Text("Email") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (custName.isNotBlank()) {
                            val newCust = Customer(
                                id = "cust_${System.currentTimeMillis()}",
                                name = custName.trim(),
                                phone = custPhone.trim(),
                                email = custEmail.trim()
                            )
                            viewModel.addCustomer(newCust) {
                                selectedCustomer = newCust
                                showQuickAddCustomerDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmartIndigo)
                ) {
                    Text("Save & Select", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickAddCustomerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Checkout Confirmation Dialog
    if (showCheckoutSummaryDialog) {
        AlertDialog(
            onDismissRequest = { if (!isLoading) showCheckoutSummaryDialog = false },
            title = { Text("Confirm Sale & Decrement Stock", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Customer: ${selectedCustomer?.name ?: "Walk-in Guest"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    HorizontalDivider()
                    cartItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.quantity}x ${item.productName}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", item.lineTotal)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Amount:", fontWeight = FontWeight.Bold)
                        Text(
                            "$${String.format(Locale.US, "%.2f", totalAmount)}",
                            fontWeight = FontWeight.Bold,
                            color = SmartIndigo,
                            fontSize = 18.sp
                        )
                    }
                    Text(
                        text = "Stock will be safely decremented in real-time.",
                        style = MaterialTheme.typography.labelSmall,
                        color = SmartTextMuted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val custId = selectedCustomer?.id ?: "guest"
                        val custName = selectedCustomer?.name ?: "Walk-in Customer"
                        lastCompletedTotal = totalAmount
                        viewModel.completeSale(custId, custName) { success ->
                            showCheckoutSummaryDialog = false
                            if (success) {
                                saleSuccessDialog = true
                            }
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = SmartIndigo),
                    modifier = Modifier.testTag("confirm_checkout_btn")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Text("Submit Transaction", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCheckoutSummaryDialog = false },
                    enabled = !isLoading
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Success Dialog
    if (saleSuccessDialog) {
        AlertDialog(
            onDismissRequest = { saleSuccessDialog = false },
            icon = {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(48.dp))
            },
            title = { Text("Sale Processed!", fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Total: $${String.format(Locale.US, "%.2f", lastCompletedTotal)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SmartIndigo
                    )
                    Text(
                        text = "Stock levels were updated immediately and the receipt has been recorded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SmartTextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { saleSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = SmartIndigo)
                ) {
                    Text("Done")
                }
            }
        )
    }

    // Add Sale Dialog (Direct Sale or Add to Cart with quantity & stock decrease)
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
            },
            onAddToCart = { quantity ->
                viewModel.addToCart(productForSale!!, quantity)
                productForSale = null
            }
        )
    }
}
