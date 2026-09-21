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
import com.example.model.Sale
import com.example.model.User
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.*
import com.example.viewmodel.SmartSalesViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SalesHistoryScreen(
    viewModel: SmartSalesViewModel,
    currentUser: User?,
    modifier: Modifier = Modifier
) {
    val isAdmin = currentUser?.isAdmin() ?: true
    val sales by viewModel.scopedSales.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedSaleForReceipt by remember { mutableStateOf<Sale?>(null) }

    val filteredSales = sales.filter { sale ->
        sale.customerName.contains(searchQuery, ignoreCase = true) ||
                sale.id.contains(searchQuery, ignoreCase = true) ||
                sale.salesPersonName.contains(searchQuery, ignoreCase = true)
    }

    val totalRevenue = filteredSales.sumOf { it.totalAmount }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("sales_history_screen")
    ) {
        // Top Summary Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isAdmin) "All Recorded Sales" else "My Sales History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SmartTextPrimary
                    )
                    Text(
                        text = "${filteredSales.size} completed transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = SmartTextSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$${String.format(Locale.US, "%,.2f", totalRevenue)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SmartIndigo
                    )
                    Text(
                        text = "Total Volume",
                        style = MaterialTheme.typography.labelSmall,
                        color = SmartTextSecondary
                    )
                }
            }
        }

        // Search Box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by customer, sale ID, or salesperson...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SmartIndigo) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("sales_history_search")
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredSales.isEmpty()) {
            EmptyStateView(
                title = "No Sales Found",
                description = if (searchQuery.isNotEmpty()) {
                    "No transactions match '$searchQuery'."
                } else {
                    "No sales have been recorded yet."
                },
                icon = Icons.Default.ReceiptLong
            )
        } else {
            val sdf = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())

            val uniqueFilteredSales = remember(filteredSales) { filteredSales.distinctBy { it.id } }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uniqueFilteredSales, key = { "sale_${it.id}" }) { sale ->
                    Card(
                        onClick = { selectedSaleForReceipt = sale },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sale_card_${sale.id}"),
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
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(SmartIndigoLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Receipt,
                                        contentDescription = null,
                                        tint = SmartIndigo,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = sale.customerName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SmartTextPrimary
                                    )
                                    Text(
                                        text = "${sale.items.sumOf { it.quantity }} items · ${sdf.format(Date(sale.saleDate))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SmartTextSecondary
                                    )
                                    if (isAdmin) {
                                        Text(
                                            text = "Sold by: ${sale.salesPersonName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SmartTextMuted
                                        )
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", sale.totalAmount)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SmartIndigo
                                )
                                Text(
                                    text = "Tap for receipt",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SmartTextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Receipt Dialog
    if (selectedSaleForReceipt != null) {
        val sale = selectedSaleForReceipt!!
        val sdf = SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault())

        AlertDialog(
            onDismissRequest = { selectedSaleForReceipt = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = SmartIndigo)
                    Text("Sale Receipt #${sale.id.takeLast(6)}", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Date & Time:", style = MaterialTheme.typography.bodySmall, color = SmartTextSecondary)
                        Text(sdf.format(Date(sale.saleDate)), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Customer:", style = MaterialTheme.typography.bodySmall, color = SmartTextSecondary)
                        Text(sale.customerName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Salesperson:", style = MaterialTheme.typography.bodySmall, color = SmartTextSecondary)
                        Text(sale.salesPersonName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("Purchased Items:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)

                    sale.items.forEach { item ->
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

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Paid:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "$${String.format(Locale.US, "%.2f", sale.totalAmount)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = SmartIndigo
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedSaleForReceipt = null },
                    colors = ButtonDefaults.buttonColors(containerColor = SmartIndigo)
                ) {
                    Text("Close")
                }
            }
        )
    }
}
