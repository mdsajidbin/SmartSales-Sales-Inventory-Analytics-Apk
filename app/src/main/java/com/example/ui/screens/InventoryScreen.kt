package com.example.ui.screens

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
import com.example.model.Product
import com.example.model.StockAdjustment
import com.example.model.StockStatus
import com.example.model.User
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StockStatusBadge
import com.example.ui.components.StockStatusBarChart
import com.example.ui.theme.*
import com.example.viewmodel.SmartSalesViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InventoryScreen(
    viewModel: SmartSalesViewModel,
    currentUser: User?,
    modifier: Modifier = Modifier
) {
    val isAdmin = currentUser?.isAdmin() ?: true
    val products by viewModel.products.collectAsState()
    val adjustments by viewModel.stockAdjustments.collectAsState()
    val kpis by viewModel.analyticsKPIs.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Stock Levels, 1: Audit Log
    var statusFilter by remember { mutableStateOf<StockStatus?>(null) }
    var adjustingProduct by remember { mutableStateOf<Product?>(null) }
    var adjustmentStockInput by remember { mutableStateOf("") }
    var adjustmentReasonInput by remember { mutableStateOf("") }
    var adjustmentError by remember { mutableStateOf<String?>(null) }

    val filteredProducts = products.filter { prod ->
        statusFilter == null || prod.getStockStatus() == statusFilter
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("inventory_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Stock Health Overview Banner
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                StockStatusBarChart(stockSummary = kpis.stockStatusSummary)
            }

            // Tab Selector: Live Stock vs Audit History
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = SmartIndigo,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Current Stock (${products.size})", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("inventory_tab_levels")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Adjustment Log (${adjustments.size})", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("inventory_tab_audit")
                )
            }

            if (selectedTab == 0) {
                // Stock Filter Chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = statusFilter == null,
                            onClick = { statusFilter = null },
                            label = { Text("All (${products.size})") },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                    item {
                        FilterChip(
                            selected = statusFilter == StockStatus.LOW_STOCK,
                            onClick = { statusFilter = if (statusFilter == StockStatus.LOW_STOCK) null else StockStatus.LOW_STOCK },
                            label = { Text("Low Stock (${kpis.stockStatusSummary.lowStockCount})") },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StatusAmber,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = statusFilter == StockStatus.OUT_OF_STOCK,
                            onClick = { statusFilter = if (statusFilter == StockStatus.OUT_OF_STOCK) null else StockStatus.OUT_OF_STOCK },
                            label = { Text("Out of Stock (${kpis.stockStatusSummary.outOfStockCount})") },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StatusRed,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = statusFilter == StockStatus.IN_STOCK,
                            onClick = { statusFilter = if (statusFilter == StockStatus.IN_STOCK) null else StockStatus.IN_STOCK },
                            label = { Text("In Stock (${kpis.stockStatusSummary.inStockCount})") },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StatusGreen,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                if (filteredProducts.isEmpty()) {
                    EmptyStateView(
                        title = "No Inventory Items",
                        description = "No products found matching the selected stock status.",
                        icon = Icons.Default.Warehouse
                    )
                } else {
                    val uniqueFilteredProducts = remember(filteredProducts) { filteredProducts.distinctBy { it.id } }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uniqueFilteredProducts, key = { "inv_prod_${it.id}" }) { product ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("inventory_item_${product.id}"),
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = product.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = SmartTextPrimary
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            StockStatusBadge(
                                                status = product.getStockStatus(),
                                                stockCount = product.stock
                                            )
                                            Text(
                                                text = "Threshold: ${product.lowStockThreshold}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SmartTextMuted
                                            )
                                        }
                                    }

                                    // Stock Count & Adjustment Button
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "${product.stock}",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = when (product.getStockStatus()) {
                                                    StockStatus.IN_STOCK -> StatusGreen
                                                    StockStatus.LOW_STOCK -> StatusAmber
                                                    StockStatus.OUT_OF_STOCK -> StatusRed
                                                }
                                            )
                                            Text(
                                                text = "units",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SmartTextSecondary
                                            )
                                        }

                                        if (isAdmin) {
                                            Button(
                                                onClick = {
                                                    adjustingProduct = product
                                                    adjustmentStockInput = ""
                                                    adjustmentReasonInput = "Restock"
                                                    adjustmentError = null
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = SmartIndigoLight,
                                                    contentColor = SmartIndigo
                                                ),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.testTag("add_stock_btn_${product.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AddBusiness,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Add Stock", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Tab 1: Audit Log of Stock Adjustments
                if (adjustments.isEmpty()) {
                    EmptyStateView(
                        title = "No Adjustments Recorded",
                        description = "When stock levels are updated manually or restocked, the audit trail will appear here.",
                        icon = Icons.Default.History
                    )
                } else {
                    val sdf = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())
                    val uniqueAdjustments = remember(adjustments) { adjustments.distinctBy { it.id } }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uniqueAdjustments, key = { "adj_${it.id}" }) { adj ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("adjustment_log_${adj.id}"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = adj.productName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SmartTextPrimary
                                        )

                                        val sign = if (adj.changeAmount >= 0) "+" else ""
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (adj.changeAmount >= 0) StatusGreenBg else StatusRedBg
                                        ) {
                                            Text(
                                                text = "$sign${adj.changeAmount} units",
                                                color = if (adj.changeAmount >= 0) StatusGreen else StatusRed,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Reason: ${adj.reason}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SmartTextSecondary
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "By ${adj.adjustedBy} · ${sdf.format(Date(adj.timestamp))}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SmartTextMuted
                                        )
                                        Text(
                                            text = "${adj.previousStock} → ${adj.newStock}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SmartTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Stock Dialog (Admin Only)
    if (isAdmin && adjustingProduct != null) {
        val prod = adjustingProduct!!
        val addQty = adjustmentStockInput.toIntOrNull() ?: 0
        val calculatedNewStock = prod.stock + addQty

        AlertDialog(
            onDismissRequest = { adjustingProduct = null },
            title = {
                Text("Add Stock (Admin Only)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = prod.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SmartIndigo
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SmartIndigoLight.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Current Stock:", style = MaterialTheme.typography.bodySmall, color = SmartTextSecondary)
                                Text("${prod.stock} units", fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("+ Quantity to Add:", style = MaterialTheme.typography.bodySmall, color = SmartIndigo)
                                Text(if (addQty > 0) "+$addQty units" else "—", fontWeight = FontWeight.Bold, color = SmartIndigo)
                            }
                            HorizontalDivider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("= New Stock:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    if (addQty > 0) "$calculatedNewStock units" else "${prod.stock} units",
                                    fontWeight = FontWeight.Bold,
                                    color = if (addQty > 0) StatusGreen else SmartTextPrimary
                                )
                            }
                        }
                    }

                    if (adjustmentError != null) {
                        Text(
                            text = adjustmentError ?: "",
                            color = StatusRed,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    OutlinedTextField(
                        value = adjustmentStockInput,
                        onValueChange = {
                            adjustmentStockInput = it.filter { ch -> ch.isDigit() }
                            adjustmentError = null
                        },
                        label = { Text("Quantity to Add *") },
                        placeholder = { Text("e.g. 20") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("adjustment_stock_input")
                    )

                    OutlinedTextField(
                        value = adjustmentReasonInput,
                        onValueChange = { adjustmentReasonInput = it },
                        label = { Text("Reason / Notes") },
                        placeholder = { Text("e.g. Restock shipment") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("adjustment_reason_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = adjustmentStockInput.toIntOrNull()
                        if (qty == null || qty <= 0) {
                            adjustmentError = "Please enter a quantity greater than 0"
                            return@Button
                        }

                        val reason = if (adjustmentReasonInput.isBlank()) "Restock" else adjustmentReasonInput.trim()
                        viewModel.addStock(prod.id, qty, reason) { success ->
                            if (success) {
                                adjustingProduct = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmartIndigo),
                    modifier = Modifier.testTag("adjustment_submit_btn")
                ) {
                    Text("Save Stock", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { adjustingProduct = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
