package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.model.User
import com.example.model.UserRole
import com.example.ui.components.RoleBadge
import com.example.ui.theme.*
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.SmartSalesViewModel

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    salesViewModel: SmartSalesViewModel,
    currentUser: User?,
    modifier: Modifier = Modifier
) {
    val isOnline by salesViewModel.isOnline.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Profile Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_header_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(SmartIndigo),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentUser?.name?.take(1)?.uppercase() ?: "U",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = currentUser?.name ?: "Sarah Manager",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SmartTextPrimary
                )

                Text(
                    text = currentUser?.email ?: "admin@smartsales.com",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SmartTextSecondary
                )

                RoleBadge(role = currentUser?.role ?: "admin")
            }
        }

        // Role Switcher for Evaluation
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("role_switcher_card"),
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
                    text = "Switch Active Role (Evaluation Mode)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SmartTextPrimary
                )
                Text(
                    text = "Toggle between Admin (full executive access) and Sales Staff (POS and personal sales scoping):",
                    style = MaterialTheme.typography.bodySmall,
                    color = SmartTextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { authViewModel.switchRole(UserRole.ADMIN) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentUser?.isAdmin() == true) SmartIndigo else SmartIndigoLight,
                            contentColor = if (currentUser?.isAdmin() == true) Color.White else SmartIndigo
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("switch_to_admin_btn")
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Admin View", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { authViewModel.switchRole(UserRole.STAFF) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentUser?.isStaff() == true) SmartCyanDark else SmartCyanLight,
                            contentColor = if (currentUser?.isStaff() == true) Color.White else SmartCyanDark
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("switch_to_staff_btn")
                    ) {
                        Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Staff View", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // System & Data Tools
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("data_tools_card"),
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
                    text = "Data Management",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SmartTextPrimary
                )

                OutlinedButton(
                    onClick = { salesViewModel.seedDemoData() },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("seed_demo_data_btn")
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = SmartIndigo)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reload Realistic Demo Dataset", color = SmartIndigo, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Firebase Backend Status",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SmartTextSecondary
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isOnline) StatusGreenBg else StatusAmberBg
                    ) {
                        Text(
                            text = if (isOnline) "Connected" else "Local Standalone",
                            color = if (isOnline) StatusGreen else StatusAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // App Information
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "App Architecture",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SmartTextPrimary
                )
                Text(
                    text = "• Platform: Android Native (Kotlin + Jetpack Compose M3)\n" +
                            "• Database: Firebase Realtime Database with transactional stock decrement\n" +
                            "• Authentication: Firebase Auth with Role-based Access Control\n" +
                            "• Architecture: MVVM with StateFlow & Clean Repository Pattern",
                    style = MaterialTheme.typography.bodySmall,
                    color = SmartTextSecondary,
                    lineHeight = 20.sp
                )
            }
        }

        // Logout Button
        Button(
            onClick = { authViewModel.logout() },
            colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("logout_button")
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out", fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
