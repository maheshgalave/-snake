package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.data.model.Invoice
import com.example.data.model.SubscriptionTier
import com.example.data.model.UserAccount
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(
    user: UserAccount?,
    invoices: List<Invoice>,
    onUpdateProfile: (name: String, teamName: String) -> Unit,
    onNavigateToPricing: () -> Unit,
    onCancelSubscription: () -> Unit,
    onExportCsv: ((String) -> Unit) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember(user?.name) { mutableStateOf(user?.name ?: "") }
    var teamName by remember(user?.teamWorkspaceName) { mutableStateOf(user?.teamWorkspaceName ?: "") }
    var showExportResultDialog by remember { mutableStateOf(false) }
    var exportedCsvContent by remember { mutableStateOf("") }
    var showCancelConfirm by remember { mutableStateOf(false) }

    val sdf = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Account Profile", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val color = runCatching { Color(android.graphics.Color.parseColor(user?.avatarColorHex ?: "#4F46E5")) }.getOrDefault(BrandPrimary)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(color),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (user?.name?.take(1) ?: "U").uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(user?.name ?: "User", fontWeight = FontWeight.Bold)
                            Text(user?.email ?: "user@taskflow.io", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = teamName,
                        onValueChange = { teamName = it },
                        label = { Text("Workspace Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { onUpdateProfile(name, teamName) },
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("settings_save_profile_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                    ) {
                        Text("Save Profile")
                    }
                }
            }
        }

        // Active Subscription & Billing Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Subscription & Billing", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when (user?.tier) {
                                SubscriptionTier.BUSINESS -> PlanBusiness.copy(alpha = 0.15f)
                                SubscriptionTier.PRO -> PlanPro.copy(alpha = 0.15f)
                                else -> PlanStarter.copy(alpha = 0.15f)
                            }
                        ) {
                            Text(
                                text = "${user?.tier?.title ?: "Starter"} Active",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = when (user?.tier) {
                                    SubscriptionTier.BUSINESS -> PlanBusiness
                                    SubscriptionTier.PRO -> PlanPro
                                    else -> PlanStarter
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Current Plan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("TaskFlow ${user?.tier?.title}", fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Billing Frequency", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(user?.billingCycle?.name?.lowercase()?.capitalize(Locale.getDefault()) ?: "Monthly", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onNavigateToPricing,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_change_plan_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                        ) {
                            Text("Change Plan")
                        }

                        if (user?.tier != SubscriptionTier.STARTER) {
                            OutlinedButton(
                                onClick = { showCancelConfirm = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Downgrade")
                            }
                        }
                    }
                }
            }
        }

        // Data & CSV Export Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Data Portability", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        "Export all your tasks, notes, completion timestamps, and tags to a standard CSV file for backup or external reporting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = {
                            onExportCsv { csv ->
                                exportedCsvContent = csv
                                showExportResultDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export All Tasks as CSV")
                    }
                }
            }
        }

        // Invoices History
        item {
            Text("Billing Invoices & Receipts", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }

        if (invoices.isEmpty()) {
            item {
                Text("No previous invoices found.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(invoices, key = { it.id }) { invoice ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(invoice.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = "${invoice.planName} · ${sdf.format(Date(invoice.date))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("$${String.format(Locale.US, "%.2f", invoice.amount)}", fontWeight = FontWeight.Bold)
                            Surface(shape = RoundedCornerShape(8.dp), color = BrandAccent.copy(alpha = 0.15f)) {
                                Text(
                                    text = invoice.status,
                                    fontSize = 11.sp,
                                    color = BrandAccent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Account Session / Logout
        item {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_logout_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out of Account")
            }
        }
    }

    // CSV Export Modal
    if (showExportResultDialog) {
        AlertDialog(
            onDismissRequest = { showExportResultDialog = false },
            title = { Text("CSV Export Ready") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your TaskFlow CSV export has been generated:")
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
                    ) {
                        Text(
                            text = exportedCsvContent,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showExportResultDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Cancel Subscription Confirmation
    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text("Downgrade to Starter?") },
            text = {
                Text("Your account will switch to the Starter plan ($10/mo). Advanced features such as unlimited projects and subtasks will be capped.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelSubscription()
                        showCancelConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Downgrade")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) { Text("Keep Plan") }
            }
        )
    }
}
