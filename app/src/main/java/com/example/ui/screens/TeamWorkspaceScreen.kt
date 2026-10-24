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
import com.example.data.model.ActivityLog
import com.example.data.model.SubscriptionTier
import com.example.data.model.TeamMember
import com.example.data.model.UserAccount
import com.example.ui.theme.BrandAccent
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.PlanBusiness
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TeamWorkspaceScreen(
    user: UserAccount?,
    teamMembers: List<TeamMember>,
    activityLogs: List<ActivityLog>,
    onInviteMember: (name: String, email: String, role: String) -> Unit,
    onRemoveMember: (TeamMember) -> Unit,
    onNavigateToPricing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBusiness = user?.tier == SubscriptionTier.BUSINESS
    var showInviteDialog by remember { mutableStateOf(false) }

    if (!isBusiness) {
        // Business Tier Gate
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(PlanBusiness.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = PlanBusiness, modifier = Modifier.size(28.dp))
                    }
                    Text(
                        text = "Team Workspace & Collaboration",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Collaborate with up to 10 team members. Share projects, assign tasks, manage permissions (Admin, Member, Viewer), and review real-time audit activity feeds.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onNavigateToPricing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("workspace_upgrade_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PlanBusiness)
                    ) {
                        Text("Upgrade to Business Plan ($80/mo)")
                    }
                }
            }
        }
        return
    }

    // Active Business Workspace
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("team_workspace_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Workspace Header Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = user.teamWorkspaceName.ifBlank { "Team Workspace" },
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${teamMembers.size + 1} of 10 seats used · Business Plan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { showInviteDialog = true },
                            modifier = Modifier.testTag("invite_member_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Invite")
                        }
                    }
                }
            }
        }

        // Team Members List
        item {
            Text(
                text = "Workspace Members",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        // Owner (Current user)
        item {
            MemberCard(
                name = "${user.name} (You)",
                email = user.email,
                role = "Owner",
                avatarColor = user.avatarColorHex,
                isOwner = true,
                onRemove = {}
            )
        }

        items(teamMembers, key = { it.id }) { member ->
            MemberCard(
                name = member.name,
                email = member.email,
                role = member.role,
                avatarColor = member.avatarColorHex,
                isOwner = false,
                onRemove = { onRemoveMember(member) }
            )
        }

        // Workspace Activity Feed
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Team Activity Feed & Audit Log",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (activityLogs.isEmpty()) {
            item {
                Text(
                    text = "No team activity recorded yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
            items(activityLogs.take(15), key = { it.id }) { log ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(BrandPrimary)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${log.action}: ${log.taskTitle.ifBlank { log.projectName }}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            if (log.details.isNotBlank()) {
                                Text(
                                    text = log.details,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = sdf.format(Date(log.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Invite Member Dialog
    if (showInviteDialog) {
        var inviteName by remember { mutableStateOf("") }
        var inviteEmail by remember { mutableStateOf("") }
        var selectedRole by remember { mutableStateOf("Member") }

        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("Invite Team Member") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = inviteName,
                        onValueChange = { inviteName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inviteEmail,
                        onValueChange = { inviteEmail = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Role", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Member", "Admin").forEach { role ->
                            FilterChip(
                                selected = selectedRole == role,
                                onClick = { selectedRole = role },
                                label = { Text(role) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inviteName.isNotBlank() && inviteEmail.isNotBlank()) {
                            onInviteMember(inviteName, inviteEmail, selectedRole)
                            showInviteDialog = false
                        }
                    },
                    enabled = inviteName.isNotBlank() && inviteEmail.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Text("Send Invitation")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun MemberCard(
    name: String,
    email: String,
    role: String,
    avatarColor: String,
    isOwner: Boolean,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val color = runCatching { Color(android.graphics.Color.parseColor(avatarColor)) }.getOrDefault(BrandPrimary)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isOwner) BrandAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = role,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isOwner) BrandAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (!isOwner) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
