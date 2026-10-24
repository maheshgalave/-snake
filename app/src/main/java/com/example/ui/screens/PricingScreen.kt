package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
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
import com.example.data.model.UserAccount
import com.example.ui.components.CheckoutDialog
import com.example.ui.theme.*

@Composable
fun PricingScreen(
    user: UserAccount?,
    onUpgradePlan: (SubscriptionTier, BillingCycle, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var billingCycle by remember { mutableStateOf(user?.billingCycle ?: BillingCycle.MONTHLY) }
    var planToCheckout by remember { mutableStateOf<SubscriptionTier?>(null) }

    val currentTier = user?.tier ?: SubscriptionTier.STARTER

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("pricing_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = BrandPrimary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "FLEXIBLE SAAS PRICING",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = BrandPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Supercharge Your Productivity",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Pick the plan tailored for your personal workflows, freelance business, or high-performing team.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Monthly vs Annual Toggle
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (billingCycle == BillingCycle.MONTHLY) MaterialTheme.colorScheme.surface else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { billingCycle = BillingCycle.MONTHLY }
                        ) {
                            Text(
                                text = "Monthly Billing",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = if (billingCycle == BillingCycle.MONTHLY) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (billingCycle == BillingCycle.ANNUAL) MaterialTheme.colorScheme.surface else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { billingCycle = BillingCycle.ANNUAL }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Annual Billing",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (billingCycle == BillingCycle.ANNUAL) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = BrandAccent.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "Save 20%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                        color = BrandAccent,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Plan 1: Starter ($10/mo)
        item {
            PlanCard(
                tier = SubscriptionTier.STARTER,
                isCurrent = currentTier == SubscriptionTier.STARTER,
                billingCycle = billingCycle,
                features = listOf(
                    "Up to 5 active projects",
                    "Up to 250 active tasks",
                    "Unlimited 1-click completion & archive",
                    "Basic task priorities & due dates",
                    "Project progress tracking",
                    "List, Today & Upcoming views",
                    "Fast search & filters",
                    "Standard account support"
                ),
                onSelectPlan = { planToCheckout = SubscriptionTier.STARTER }
            )
        }

        // Plan 2: Pro ($20/mo) - Highlighted as Most Popular
        item {
            PlanCard(
                tier = SubscriptionTier.PRO,
                isCurrent = currentTier == SubscriptionTier.PRO,
                isPopular = true,
                billingCycle = billingCycle,
                features = listOf(
                    "Everything in Starter",
                    "Unlimited projects & tasks",
                    "Tags, labels & categories",
                    "Subtasks & checklist trees",
                    "Recurring tasks (daily, weekly, monthly)",
                    "Task reminders & due time notifications",
                    "Visual Calendar & timeline agenda",
                    "Bulk archive, restore & move actions",
                    "CSV data export & backup",
                    "Priority productivity insights"
                ),
                onSelectPlan = { planToCheckout = SubscriptionTier.PRO }
            )
        }

        // Plan 3: Business ($80/mo) - Team Workspaces
        item {
            PlanCard(
                tier = SubscriptionTier.BUSINESS,
                isCurrent = currentTier == SubscriptionTier.BUSINESS,
                billingCycle = billingCycle,
                features = listOf(
                    "Everything in Pro",
                    "Team workspaces included",
                    "Up to 10 team members included",
                    "Shared projects & assigned tasks",
                    "Team roles (Owner, Admin, Member)",
                    "Team activity & audit logs",
                    "Centralized billing management",
                    "Priority 24/7 dedicated support"
                ),
                onSelectPlan = { planToCheckout = SubscriptionTier.BUSINESS }
            )
        }

        // Enterprise Notice / FAQ
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Transparent SaaS Pricing", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "• All prices in USD. Billed automatically based on your selected cycle.\n" +
                        "• Switch plans or cancel anytime without cancellation fees.\n" +
                        "• Instant activation with immediate invoice receipt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Checkout Modal
    planToCheckout?.let { plan ->
        CheckoutDialog(
            plan = plan,
            billingCycle = billingCycle,
            onConfirmPayment = { cardLast4 ->
                onUpgradePlan(plan, billingCycle, cardLast4)
                planToCheckout = null
            },
            onDismiss = { planToCheckout = null }
        )
    }
}

@Composable
fun PlanCard(
    tier: SubscriptionTier,
    isCurrent: Boolean,
    isPopular: Boolean = false,
    billingCycle: BillingCycle,
    features: List<String>,
    onSelectPlan: () -> Unit
) {
    val price = if (billingCycle == BillingCycle.MONTHLY) tier.monthlyPrice else tier.annualMonthlyPrice

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isPopular) Modifier.border(2.dp, BrandPrimary, RoundedCornerShape(16.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPopular) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPopular) 4.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tier.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    if (isPopular) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BrandPrimary
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "MOST POPULAR",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                if (isCurrent) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BrandAccent.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Current Plan",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = BrandAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Price Row
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$${price.toInt()}",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    text = "/month",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                )
                if (billingCycle == BillingCycle.ANNUAL) {
                    Text(
                        text = " (billed annually $${(price * 12).toInt()}/yr)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button
            Button(
                onClick = onSelectPlan,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pricing_select_${tier.name.lowercase()}_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCurrent) MaterialTheme.colorScheme.surfaceVariant
                    else if (isPopular) BrandPrimary
                    else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (isCurrent) MaterialTheme.colorScheme.onSurfaceVariant
                    else if (isPopular) Color.White
                    else MaterialTheme.colorScheme.onPrimaryContainer
                ),
                enabled = !isCurrent
            ) {
                Text(
                    text = if (isCurrent) "Current Active Plan" else "Select ${tier.title}",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(14.dp))

            // Features Checklist
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                features.forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(BrandAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = BrandAccent,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
