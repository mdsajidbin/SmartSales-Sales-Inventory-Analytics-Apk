package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.model.Customer
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.*
import com.example.viewmodel.SmartSalesViewModel
import java.util.Locale

@Composable
fun CustomersScreen(
    viewModel: SmartSalesViewModel,
    modifier: Modifier = Modifier
) {
    val customers by viewModel.customers.collectAsState()
    val allSales by viewModel.allSales.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<Customer?>(null) }

    val filteredCustomers = customers.filter { cust ->
        cust.name.contains(searchQuery, ignoreCase = true) ||
                cust.phone.contains(searchQuery, ignoreCase = true) ||
                cust.email.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("customers_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingCustomer = null
                    showAddEditDialog = true
                },
                containerColor = SmartIndigo,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("add_customer_fab")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Customer")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by customer name, phone, email...") },
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
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("customer_search_input")
            )

            if (filteredCustomers.isEmpty()) {
                EmptyStateView(
                    title = "No Customers Found",
                    description = if (searchQuery.isNotEmpty()) {
                        "No customers match '$searchQuery'."
                    } else {
                        "Start by adding your first customer to personalize sales and track analytics."
                    },
                    icon = Icons.Default.People,
                    actionLabel = "Add Customer",
                    onActionClick = {
                        editingCustomer = null
                        showAddEditDialog = true
                    }
                )
            } else {
                val uniqueFilteredCustomers = remember(filteredCustomers) { filteredCustomers.distinctBy { it.id } }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uniqueFilteredCustomers, key = { "cust_${it.id}" }) { customer ->
                        val customerSales = allSales.filter { it.customerId == customer.id }
                        val customerSpend = customerSales.sumOf { it.totalAmount }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("customer_card_${customer.id}"),
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
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(SmartIndigoLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = customer.name.take(1).uppercase(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = SmartIndigo
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = customer.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = SmartTextPrimary
                                        )
                                        if (customer.phone.isNotBlank()) {
                                            Text(
                                                text = customer.phone,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SmartTextSecondary
                                            )
                                        }
                                        if (customer.email.isNotBlank()) {
                                            Text(
                                                text = customer.email,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SmartTextMuted
                                            )
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$${String.format(Locale.US, "%,.2f", customerSpend)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SmartIndigo
                                    )
                                    Text(
                                        text = "${customerSales.size} purchase(s)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SmartTextSecondary
                                    )

                                    IconButton(
                                        onClick = {
                                            editingCustomer = customer
                                            showAddEditDialog = true
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit Customer",
                                            tint = SmartTextSecondary,
                                            modifier = Modifier.size(16.dp)
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

    // Add / Edit Customer Dialog
    if (showAddEditDialog) {
        var name by remember { mutableStateOf(editingCustomer?.name ?: "") }
        var phone by remember { mutableStateOf(editingCustomer?.phone ?: "") }
        var email by remember { mutableStateOf(editingCustomer?.email ?: "") }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddEditDialog = false },
            title = {
                Text(
                    text = if (editingCustomer == null) "New Customer" else "Edit Customer",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (error != null) {
                        Text(text = error ?: "", color = StatusRed, style = MaterialTheme.typography.bodySmall)
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Customer Name *") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_dialog_name")
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_dialog_phone")
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_dialog_email")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            error = "Customer name is required"
                            return@Button
                        }
                        val cust = Customer(
                            id = editingCustomer?.id ?: "",
                            name = name.trim(),
                            phone = phone.trim(),
                            email = email.trim()
                        )
                        if (editingCustomer == null) {
                            viewModel.addCustomer(cust) { showAddEditDialog = false }
                        } else {
                            viewModel.updateCustomer(cust) { showAddEditDialog = false }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmartIndigo),
                    modifier = Modifier.testTag("customer_dialog_save_btn")
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
