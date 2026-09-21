package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.SmartSalesRepository
import com.example.model.UserRole
import com.example.ui.components.SmartSalesBottomBar
import com.example.ui.components.SmartSalesTopBar
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StatusRed
import com.example.viewmodel.AppTab
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.SmartSalesViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                SmartSalesApp(this)
            }
        }
    }
}

@Composable
fun SmartSalesApp(activity: ComponentActivity) {
    val repository = remember { SmartSalesRepository(activity.applicationContext) }
    val authViewModel = remember { AuthViewModel(repository) }
    val salesViewModel = remember { SmartSalesViewModel(repository) }

    val currentUser by authViewModel.currentUser.collectAsState()
    val isOnline by salesViewModel.isOnline.collectAsState()
    val currentTab by salesViewModel.currentTab.collectAsState()
    val cartItems by salesViewModel.cartItems.collectAsState()

    val feedbackMsg by salesViewModel.feedbackMessage.collectAsState()
    val errorMsg by salesViewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Handle feedback & error Snackbars
    LaunchedEffect(feedbackMsg) {
        feedbackMsg?.let {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(it)
                salesViewModel.clearMessages()
            }
        }
    }

    LaunchedEffect(errorMsg) {
        errorMsg?.let {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(it)
                salesViewModel.clearMessages()
            }
        }
    }

    if (currentUser == null) {
        // Authentication Flow
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            AuthScreen(
                viewModel = authViewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    } else {
        val user = currentUser!!
        val isAdmin = user.isAdmin()

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("main_app_scaffold"),
            topBar = {
                SmartSalesTopBar(
                    currentUser = user,
                    isOnline = isOnline,
                    onSeedClick = { salesViewModel.seedDemoData() },
                    onProfileClick = { salesViewModel.setTab(AppTab.PROFILE) }
                )
            },
            bottomBar = {
                SmartSalesBottomBar(
                    currentTab = currentTab,
                    onTabSelected = { salesViewModel.setTab(it) },
                    isAdmin = isAdmin,
                    cartItemCount = cartItems.sumOf { it.quantity }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    AppTab.DASHBOARD -> {
                        DashboardScreen(
                            viewModel = salesViewModel,
                            currentUser = user,
                            onNavigateToTab = { salesViewModel.setTab(it) }
                        )
                    }
                    AppTab.PRODUCTS -> {
                        ProductsScreen(
                            viewModel = salesViewModel,
                            currentUser = user
                        )
                    }
                    AppTab.INVENTORY -> {
                        InventoryScreen(
                            viewModel = salesViewModel,
                            currentUser = user
                        )
                    }
                    AppTab.SALES -> {
                        // In Sales tab: Provide toggle between New Sale Entry and Sales History
                        var salesSubTab by remember { mutableStateOf(0) } // 0: New Sale POS, 1: Sales History

                        Column(modifier = Modifier.fillMaxSize()) {
                            TabRow(
                                selectedTabIndex = salesSubTab,
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                Tab(
                                    selected = salesSubTab == 0,
                                    onClick = { salesSubTab = 0 },
                                    text = { Text("New Sale (POS)", style = MaterialTheme.typography.titleSmall) },
                                    modifier = Modifier.testTag("sales_subtab_pos")
                                )
                                Tab(
                                    selected = salesSubTab == 1,
                                    onClick = { salesSubTab = 1 },
                                    text = { Text("Sales History", style = MaterialTheme.typography.titleSmall) },
                                    modifier = Modifier.testTag("sales_subtab_history")
                                )
                            }

                            if (salesSubTab == 0) {
                                SalesEntryScreen(viewModel = salesViewModel)
                            } else {
                                SalesHistoryScreen(viewModel = salesViewModel, currentUser = user)
                            }
                        }
                    }
                    AppTab.CUSTOMERS -> {
                        CustomersScreen(viewModel = salesViewModel)
                    }
                    AppTab.PROFILE -> {
                        ProfileScreen(
                            authViewModel = authViewModel,
                            salesViewModel = salesViewModel,
                            currentUser = user
                        )
                    }
                }
            }
        }
    }
}
