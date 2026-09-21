package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Customer
import com.example.model.Product
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSaleDialog(
    product: Product,
    customers: List<Customer> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (quantity: Int, customerId: String, customerName: String) -> Unit,
    onAddToCart: ((quantity: Int) -> Unit)? = null
) {
    val safePrice = if (product.price.isNaN() || product.price.isInfinite() || product.price < 0.0) 0.0 else product.price
    val safeStock = product.stock.coerceAtLeast(0)

    var quantityText by remember { mutableStateOf(if (safeStock > 0) "1" else "0") }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var customerDropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val quantity = quantityText.toIntOrNull() ?: 0
    val totalAmount = quantity * safePrice
    val remainingStock = safeStock - quantity
    val isValidQuantity = safeStock > 0 && quantity > 0 && quantity <= safeStock

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Add Sale",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = SmartTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${product.name.ifBlank { "Product" }} — $${String.format(Locale.US, "%.2f", safePrice)} each",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SmartIndigo,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Out of stock warning if applicable
                if (safeStock <= 0) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StatusRed.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusRed)
                            Text(
                                text = "This product is currently out of stock. Cannot record a sale.",
                                color = StatusRed,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Stock & Total Calculation Preview Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SmartCyanLight.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, SmartCyanDark.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Available Stock:", style = MaterialTheme.typography.bodyMedium, color = SmartTextSecondary)
                            Text(
                                text = "$safeStock units",
                                fontWeight = FontWeight.Bold,
                                color = if (safeStock > 0) StatusGreen else StatusRed
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Unit Price:", style = MaterialTheme.typography.bodyMedium, color = SmartTextSecondary)
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", safePrice)}",
                                fontWeight = FontWeight.SemiBold,
                                color = SmartTextPrimary
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Sale Amount:", style = MaterialTheme.typography.bodyMedium, color = SmartTextSecondary)
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", totalAmount)}",
                                fontWeight = FontWeight.Bold,
                                color = SmartIndigo,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        HorizontalDivider(color = SmartCyanDark.copy(alpha = 0.2f), thickness = 1.dp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Remaining After Sale:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = when {
                                    safeStock <= 0 -> "0 units (Out of Stock)"
                                    quantity <= 0 -> "$safeStock units"
                                    remainingStock >= 0 -> "$remainingStock units"
                                    else -> "Insufficient Stock!"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (safeStock > 0 && remainingStock >= 0) StatusGreen else StatusRed
                            )
                        }
                    }
                }

                // Quick Quantity Stepper
                if (safeStock > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Quantity:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val current = quantityText.toIntOrNull() ?: 1
                                    if (current > 1) {
                                        quantityText = (current - 1).toString()
                                        errorMessage = null
                                    }
                                },
                                enabled = (quantityText.toIntOrNull() ?: 1) > 1
                            ) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease", tint = SmartIndigo)
                            }
                            Text(
                                text = quantityText.ifEmpty { "0" },
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(
                                onClick = {
                                    val current = quantityText.toIntOrNull() ?: 0
                                    if (current < safeStock) {
                                        quantityText = (current + 1).toString()
                                        errorMessage = null
                                    }
                                },
                                enabled = (quantityText.toIntOrNull() ?: 0) < safeStock
                            ) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase", tint = SmartIndigo)
                            }
                        }
                    }

                    // Quantity Text Input
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            quantityText = digits
                            val qty = digits.toIntOrNull() ?: 0
                            errorMessage = when {
                                digits.isEmpty() -> "Quantity cannot be empty."
                                qty <= 0 -> "Sales quantity must be at least 1."
                                qty > safeStock -> "Cannot exceed available stock ($safeStock units)."
                                else -> null
                            }
                        },
                        label = { Text("Sales Quantity *") },
                        placeholder = { Text("e.g. 1") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = errorMessage != null,
                        supportingText = {
                            if (errorMessage != null) {
                                Text(errorMessage!!, color = StatusRed)
                            } else {
                                Text("Max available: $safeStock units")
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sales_quantity_input")
                    )
                }

                // Customer Selection
                ExposedDropdownMenuBox(
                    expanded = customerDropdownExpanded,
                    onExpandedChange = { customerDropdownExpanded = !customerDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCustomer?.name ?: "Walk-in Customer (Guest)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Customer") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = customerDropdownExpanded,
                        onDismissRequest = { customerDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Walk-in Customer (Guest)") },
                            onClick = {
                                selectedCustomer = null
                                customerDropdownExpanded = false
                            }
                        )
                        customers.distinctBy { it.id }.forEach { cust ->
                            DropdownMenuItem(
                                text = { Text("${cust.name} (${cust.phone})") },
                                onClick = {
                                    selectedCustomer = cust
                                    customerDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onAddToCart != null && safeStock > 0) {
                    OutlinedButton(
                        onClick = {
                            if (isValidQuantity) {
                                onAddToCart(quantity)
                            }
                        },
                        enabled = isValidQuantity,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("add_to_cart_btn")
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add to Cart", fontWeight = FontWeight.SemiBold)
                    }
                }

                Button(
                    onClick = {
                        val qty = quantityText.toIntOrNull()
                        if (qty == null || qty <= 0) {
                            errorMessage = "Quantity is required and must be greater than 0."
                        } else if (qty > safeStock) {
                            errorMessage = "Cannot exceed available stock ($safeStock units)."
                        } else {
                            val custId = selectedCustomer?.id ?: "walk_in"
                            val custName = selectedCustomer?.name ?: "Walk-in Customer"
                            onConfirm(qty, custId, custName)
                        }
                    },
                    enabled = isValidQuantity,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("save_sale_btn")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Sale", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
