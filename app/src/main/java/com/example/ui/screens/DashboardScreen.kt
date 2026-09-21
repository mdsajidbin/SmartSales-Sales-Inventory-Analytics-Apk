package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.model.DateRangeFilter
import com.example.model.User
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.AppTab
import com.example.viewmodel.SmartSalesViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: SmartSalesViewModel,
    currentUser: User?,
    onNavigateToTab: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAdmin = currentUser?.isAdmin() ?: true
    val kpis by viewModel.analyticsKPIs.collectAsState()
    val selectedRange by viewModel.selectedDateRange.collectAsState()
    val scopedSales by viewModel.scopedSales.collectAsState()
    val products by viewModel.products.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("dashboard_welcome_banner"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAdmin) SmartIndigo else SmartCyanDark
                )
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (isAdmin) "Executive Analytics" else "Sales Associate",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = "Welcome back, ${currentUser?.name ?: "User"}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = if (isAdmin) {
                                "Real-time revenue, product performance, and stock health"
                            } else {
                                "Record new transactions and track your customer sales"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // Role: ADMIN FULL ANALYTICS
        if (isAdmin) {
            // Date Range Selector
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Timeframe Range",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SmartTextSecondary
                    )
                    DateRangeFilterChips(
                        selectedRange = selectedRange,
                        onRangeSelected = { viewModel.setDateRange(it) },
                        modifier = Modifier.padding(horizontal = 0.dp)
                    )
                }
            }

            // Primary KPI Row 1: Total Revenue & Total Sales
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "Total Revenue",
                        value = "$${String.format(Locale.US, "%,.2f", kpis.totalRevenue)}",
                        subtitle = "${kpis.totalSalesCount} transactions",
                        icon = Icons.Default.AttachMoney,
                        iconTint = SmartIndigo,
                        iconBgColor = SmartIndigoLight,
                        badgeText = selectedRange.label,
                        modifier = Modifier.weight(1f)
                    )

                    KpiCard(
                        title = "Avg Order Value",
                        value = "$${String.format(Locale.US, "%.2f", kpis.averageOrderValue)}",
                        subtitle = "Per transaction",
                        icon = Icons.Default.ShoppingCart,
                        iconTint = SmartCyan,
                        iconBgColor = SmartCyanLight,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Primary KPI Row 2: Inventory & Stock Health
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "Active Products",
                        value = "${kpis.totalProducts}",
                        subtitle = "${kpis.totalCustomers} customers",
                        icon = Icons.Default.Category,
                        iconTint = Color(0xFF8B5CF6),
                        iconBgColor = Color(0xFFF3E8FF),
                        modifier = Modifier.weight(1f)
                    )

                    val hasLowStock = kpis.lowStockCount > 0 || kpis.outOfStockCount > 0
                    KpiCard(
                        title = "Low / Out of Stock",
                        value = "${kpis.lowStockCount + kpis.outOfStockCount}",
                        subtitle = "${kpis.outOfStockCount} critical out of stock",
                        icon = Icons.Default.Warning,
                        iconTint = if (hasLowStock) StatusAmber else StatusGreen,
                        iconBgColor = if (hasLowStock) StatusAmberBg else StatusGreenBg,
                        badgeText = if (hasLowStock) "Alert" else "Healthy",
                        badgeColor = if (hasLowStock) StatusAmber else StatusGreen,
                        badgeBgColor = if (hasLowStock) StatusAmberBg else StatusGreenBg,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Primary KPI Row 3: Top Performer Cards
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "Top Product",
                        value = kpis.topSellingProductName.take(16),
                        subtitle = "${kpis.topSellingProductQuantity} units sold",
                        icon = Icons.Default.Star,
                        iconTint = StatusAmber,
                        iconBgColor = StatusAmberBg,
                        modifier = Modifier.weight(1f)
                    )

                    KpiCard(
                        title = "Top Category",
                        value = kpis.topCategoryName.take(16),
                        subtitle = "$${String.format(Locale.US, "%.1f", kpis.topCategoryRevenue)} revenue",
                        icon = Icons.Default.LocalOffer,
                        iconTint = SmartIndigo,
                        iconBgColor = SmartIndigoLight,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Chart 1: Monthly / Periodic Sales Bar & Line Chart
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    MonthlySalesChart(dataPoints = kpis.monthlySales)
                }
            }

            // Chart 2: Category Breakdown Donut Chart
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    CategoryDistributionChart(categoryPoints = kpis.categorySales)
                }
            }

            // Chart 3: Stock Status Breakdown Bar
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    StockStatusBarChart(stockSummary = kpis.stockStatusSummary)
                }
            }

            // Quick Actions Hub for Admin
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("admin_quick_actions"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Operational Shortcuts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SmartTextPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onNavigateToTab(AppTab.SALES) },
                                colors = ButtonDefaults.buttonColors(containerColor = SmartIndigo),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("shortcut_new_sale")
                            ) {
                                Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("New Sale", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { onNavigateToTab(AppTab.PRODUCTS) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("shortcut_products")
                            ) {
                                Icon(Icons.Default.Inventory2, contentDescription = null, tint = SmartIndigo, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Catalog", color = SmartIndigo, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { onNavigateToTab(AppTab.INVENTORY) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("shortcut_inventory")
                            ) {
                                Icon(Icons.Default.Warehouse, contentDescription = null, tint = SmartIndigo, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Stock", color = SmartIndigo, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            // Role: SALES STAFF SIMPLIFIED WORKSPACE
            item {
                // Primary CTA: Record Sale
                Button(
                    onClick = { onNavigateToTab(AppTab.SALES) },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SmartIndigo),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(56.dp)
                        .testTag("staff_new_sale_primary_button")
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Record New Sale", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Staff Personal Summary KPIs
            item {
                val mySales = scopedSales
                val myTotalRevenue = mySales.sumOf { it.totalAmount }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "My Sales Revenue",
                        value = "$${String.format(Locale.US, "%,.2f", myTotalRevenue)}",
                        subtitle = "Personal processed total",
                        icon = Icons.Default.AttachMoney,
                        iconTint = SmartCyan,
                        iconBgColor = SmartCyanLight,
                        modifier = Modifier.weight(1f)
                    )

                    KpiCard(
                        title = "Orders Created",
                        value = "${mySales.size}",
                        subtitle = "Completed receipts",
                        icon = Icons.Default.ReceiptLong,
                        iconTint = SmartIndigo,
                        iconBgColor = SmartIndigoLight,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Staff Recent Sales List
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("staff_recent_sales_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "My Recent Transactions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SmartTextPrimary
                            )
                            TextButton(onClick = { onNavigateToTab(AppTab.SALES) }) {
                                Text("See All", color = SmartIndigo, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (scopedSales.isEmpty()) {
                            Text(
                                text = "You haven't recorded any sales yet. Tap 'Record New Sale' to start.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SmartTextSecondary,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            val sdf = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())
                            scopedSales.take(5).forEach { sale ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = sale.customerName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = SmartTextPrimary
                                        )
                                        Text(
                                            text = "${sale.items.size} item(s) · ${sdf.format(Date(sale.saleDate))}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SmartTextSecondary
                                        )
                                    }

                                    Text(
                                        text = "$${String.format(Locale.US, "%.2f", sale.totalAmount)}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = SmartIndigo
                                    )
                                }
                                HorizontalDivider(color = SmartCardBorder.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}
