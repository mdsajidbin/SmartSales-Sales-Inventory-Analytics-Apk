package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.User
import com.example.ui.theme.*
import com.example.viewmodel.AppTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartSalesTopBar(
    currentUser: User?,
    isOnline: Boolean,
    onSeedClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Logo Icon dot
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(SmartIndigo),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "SmartSales Logo",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = "SmartSales",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SmartTextPrimary
                    )
                    Text(
                        text = "Sales & Inventory Analytics",
                        style = MaterialTheme.typography.labelSmall,
                        color = SmartTextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        },
        actions = {
            // Live / Offline Status Pill
            Surface(
                shape = CircleShape,
                color = if (isOnline) StatusGreenBg else StatusAmberBg,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .testTag("network_status_indicator")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isOnline) StatusGreen else StatusAmber)
                    )
                    Text(
                        text = if (isOnline) "Firebase" else "Demo",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOnline) StatusGreen else StatusAmber
                    )
                }
            }

            if (currentUser != null) {
                RoleBadge(role = currentUser.role)
                Spacer(modifier = Modifier.width(4.dp))
            }

            IconButton(
                onClick = onProfileClick,
                modifier = Modifier.testTag("topbar_profile_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile",
                    tint = SmartIndigo
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = SmartTextPrimary
        ),
        modifier = modifier.testTag("main_top_bar")
    )
}

data class NavItem(
    val tab: AppTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun SmartSalesBottomBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    isAdmin: Boolean,
    cartItemCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val items = if (isAdmin) {
        listOf(
            NavItem(AppTab.DASHBOARD, "Analytics", Icons.Filled.Insights, Icons.Outlined.Insights),
            NavItem(AppTab.PRODUCTS, "Products", Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
            NavItem(AppTab.INVENTORY, "Inventory", Icons.Filled.Warehouse, Icons.Outlined.Warehouse),
            NavItem(AppTab.SALES, "Sales", Icons.Filled.PointOfSale, Icons.Outlined.PointOfSale),
            NavItem(AppTab.CUSTOMERS, "Customers", Icons.Filled.People, Icons.Outlined.People)
        )
    } else {
        listOf(
            NavItem(AppTab.DASHBOARD, "My Sales", Icons.Filled.Insights, Icons.Outlined.Insights),
            NavItem(AppTab.SALES, "New Sale", Icons.Filled.AddShoppingCart, Icons.Outlined.AddShoppingCart),
            NavItem(AppTab.PRODUCTS, "Catalog", Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
            NavItem(AppTab.CUSTOMERS, "Customers", Icons.Filled.People, Icons.Outlined.People),
            NavItem(AppTab.PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person)
        )
    }

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = modifier.testTag("main_bottom_nav_bar")
    ) {
        items.forEach { item ->
            val isSelected = currentTab == item.tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(item.tab) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (item.tab == AppTab.SALES && cartItemCount > 0) {
                                Badge(containerColor = SmartIndigo) {
                                    Text("$cartItemCount")
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label
                        )
                    }
                },
                label = {
                    Text(
                        text = item.label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = SmartIndigo,
                    selectedTextColor = SmartIndigo,
                    indicatorColor = SmartIndigoLight,
                    unselectedIconColor = SmartTextSecondary,
                    unselectedTextColor = SmartTextSecondary
                ),
                modifier = Modifier.testTag("nav_item_${item.tab.name.lowercase()}")
            )
        }
    }
}
