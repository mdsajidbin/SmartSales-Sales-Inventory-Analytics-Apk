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
import com.example.model.Category
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.*
import com.example.viewmodel.SmartSalesViewModel

@Composable
fun CategoriesScreen(
    viewModel: SmartSalesViewModel,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.categories.collectAsState()
    val products by viewModel.products.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }
    var categoryNameInput by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("categories_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingCategory = null
                    categoryNameInput = ""
                    validationError = null
                    showAddEditDialog = true
                },
                containerColor = SmartIndigo,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("add_category_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Product Categories",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SmartTextPrimary
            )

            Text(
                text = "Group items for structured sales analytics and organized catalog browsing",
                style = MaterialTheme.typography.bodySmall,
                color = SmartTextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (categories.isEmpty()) {
                EmptyStateView(
                    title = "No Categories Yet",
                    description = "Categories help you organize inventory and track category-wise sales revenue.",
                    icon = Icons.Default.Category,
                    actionLabel = "Create Category",
                    onActionClick = {
                        editingCategory = null
                        categoryNameInput = ""
                        showAddEditDialog = true
                    }
                )
            } else {
                val uniqueCategories = remember(categories) { categories.distinctBy { it.id } }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uniqueCategories, key = { "cat_${it.id}" }) { cat ->
                        val productCount = products.count { it.categoryId == cat.id }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("category_card_${cat.id}"),
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
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(SmartIndigoLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = SmartIndigo,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = SmartTextPrimary
                                        )
                                        Text(
                                            text = "$productCount product(s)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SmartTextSecondary
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            editingCategory = cat
                                            categoryNameInput = cat.name
                                            validationError = null
                                            showAddEditDialog = true
                                        }
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = SmartTextSecondary)
                                    }

                                    IconButton(
                                        onClick = { categoryToDelete = cat }
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StatusRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddEditDialog) {
        AlertDialog(
            onDismissRequest = { showAddEditDialog = false },
            title = {
                Text(
                    text = if (editingCategory == null) "New Category" else "Edit Category",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (validationError != null) {
                        Text(
                            text = validationError ?: "",
                            color = StatusRed,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    OutlinedTextField(
                        value = categoryNameInput,
                        onValueChange = { categoryNameInput = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("category_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (categoryNameInput.isBlank()) {
                            validationError = "Category name cannot be empty"
                            return@Button
                        }
                        if (editingCategory == null) {
                            viewModel.addCategory(categoryNameInput) { showAddEditDialog = false }
                        } else {
                            viewModel.updateCategory(editingCategory!!.copy(name = categoryNameInput.trim())) {
                                showAddEditDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmartIndigo)
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

    // Delete Confirmation with Guard Dialog
    if (categoryToDelete != null) {
        val assignedCount = products.count { it.categoryId == categoryToDelete!!.id }
        val canDelete = assignedCount == 0

        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Delete Category", fontWeight = FontWeight.Bold) },
            text = {
                if (canDelete) {
                    Text("Are you sure you want to delete '${categoryToDelete?.name}'? This action cannot be undone.")
                } else {
                    Text(
                        "Cannot delete '${categoryToDelete?.name}' because $assignedCount product(s) are currently assigned to it. Please reassign or delete those products first.",
                        color = StatusRed
                    )
                }
            },
            confirmButton = {
                if (canDelete) {
                    Button(
                        onClick = {
                            categoryToDelete?.let { viewModel.deleteCategory(it.id) }
                            categoryToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text(if (canDelete) "Cancel" else "OK")
                }
            }
        )
    }
}
