package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.data.model.BillingCycle
import com.example.data.model.SubscriptionTier
import com.example.ui.theme.*

@Composable
fun UpgradePromptDialog(
    targetTier: SubscriptionTier,
    reason: String,
    onUpgradeClick: (SubscriptionTier) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(BrandPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = "Unlock ${targetTier.title} Features",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${targetTier.title} Plan includes:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        when (targetTier) {
                            SubscriptionTier.PRO -> {
                                BulletItem("Unlimited projects & active tasks")
                                BulletItem("Tags, labels & subtasks checklist")
                                BulletItem("Recurring schedules & reminders")
                                BulletItem("Calendar view & CSV data export")
                                BulletItem("Bulk actions & productivity insights")
                            }
                            SubscriptionTier.BUSINESS -> {
                                BulletItem("Everything in Pro")
                                BulletItem("Team workspaces (up to 10 members)")
                                BulletItem("Shared projects & task assignments")
                                BulletItem("Team activity audit logs")
                                BulletItem("Centralized billing & priority support")
                            }
                            SubscriptionTier.STARTER -> {}
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onUpgradeClick(targetTier) },
                modifier = Modifier.testTag("upgrade_dialog_confirm_button"),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Text("View Plans & Upgrade")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Maybe Later")
            }
        }
    )
}

@Composable
fun BulletItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = BrandAccent,
            modifier = Modifier.size(16.dp)
        )
        Text(text = text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun CheckoutDialog(
    plan: SubscriptionTier,
    billingCycle: BillingCycle,
    onConfirmPayment: (cardLast4: String) -> Unit,
    onDismiss: () -> Unit
) {
    var cardName by remember { mutableStateOf("Alex Vance") }
    var cardNumber by remember { mutableStateOf("4242 •••• •••• 8892") }
    var cardExpiry by remember { mutableStateOf("12/28") }
    var cardCvc by remember { mutableStateOf("912") }
    var isProcessing by remember { mutableStateOf(false) }

    val basePrice = if (billingCycle == BillingCycle.MONTHLY) plan.monthlyPrice else plan.annualMonthlyPrice * 12
    val tax = basePrice * 0.08
    val total = basePrice + tax

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Payment,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Complete Subscription", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Order Summary Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "TaskFlow ${plan.title} (${billingCycle.name.lowercase()})",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "$${String.format(java.util.Locale.US, "%.2f", basePrice)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Estimated Tax (8%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$${String.format(java.util.Locale.US, "%.2f", tax)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Due Today", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text(
                                text = "$${String.format(java.util.Locale.US, "%.2f", total)}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BrandPrimary
                                )
                            )
                        }
                    }
                }

                // Payment Fields (Simulated Secure Vault)
                OutlinedTextField(
                    value = cardName,
                    onValueChange = { cardName = it },
                    label = { Text("Name on Card") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { cardNumber = it },
                    label = { Text("Card Number") },
                    leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = cardExpiry,
                        onValueChange = { cardExpiry = it },
                        label = { Text("MM/YY") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = cardCvc,
                        onValueChange = { cardCvc = it },
                        label = { Text("CVC") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "🔒 256-bit SSL encrypted checkout. You can cancel anytime.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isProcessing = true
                    onConfirmPayment(cardNumber.takeLast(4).ifBlank { "4242" })
                },
                enabled = !isProcessing,
                modifier = Modifier.testTag("checkout_confirm_button"),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Pay $${String.format(java.util.Locale.US, "%.2f", total)}")
                }
            }
        },
        dismissButton = {
            if (!isProcessing) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
