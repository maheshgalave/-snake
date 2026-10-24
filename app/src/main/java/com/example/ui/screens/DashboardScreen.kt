package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.data.model.Project
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.data.model.UserAccount
import com.example.ui.components.ProjectCard
import com.example.ui.components.QuickAddBar
import com.example.ui.components.TaskCard
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    user: UserAccount?,
    projects: List<Project>,
    activeTasks: List<TaskItem>,
    archiveTasks: List<TaskItem>,
    onNavigateToProject: (Long) -> Unit,
    onNavigateToAllTasks: () -> Unit,
    onNavigateToArchive: () -> Unit,
    onNavigateToPricing: () -> Unit,
    onOpenProjectEditor: (Project?) -> Unit,
    onOpenTaskEditor: (TaskItem?) -> Unit,
    onCompleteTask: (TaskItem) -> Unit,
    onQuickAddTask: (String, Long, TaskPriority) -> Unit,
    onDuplicateTask: (TaskItem) -> Unit,
    onMoveTask: (TaskItem, Long) -> Unit,
    onArchiveTask: (TaskItem) -> Unit,
    onRestoreTask: (TaskItem) -> Unit,
    onDeleteTask: (TaskItem) -> Unit,
    onToggleSubtask: (TaskItem, String) -> Unit,
    onArchiveProjectToggle: (Project) -> Unit,
    onDeleteProject: (Project) -> Unit,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val dayMs = 24 * 60 * 60 * 1000L

    // Metrics
    val dueTodayTasks = remember(activeTasks) {
        activeTasks.filter { it.dueDate != null && it.dueDate in (now - dayMs / 2)..(now + dayMs) }
    }
    val overdueTasks = remember(activeTasks) {
        activeTasks.filter { it.dueDate != null && it.dueDate < now - dayMs / 2 }
    }
    val urgentTasks = remember(activeTasks) {
        activeTasks.filter { it.priority == TaskPriority.URGENT || it.priority == TaskPriority.HIGH }
    }
    val completedRecently = remember(archiveTasks) {
        archiveTasks.filter { it.isCompleted }.take(5)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Welcome Header & Plan Chip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hello, ${user?.name ?: "Productive User"} 👋",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Here is your task breakdown and project progress for today.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Tier badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when (user?.tier) {
                        com.example.data.model.SubscriptionTier.BUSINESS -> PlanBusiness.copy(alpha = 0.15f)
                        com.example.data.model.SubscriptionTier.PRO -> PlanPro.copy(alpha = 0.15f)
                        else -> PlanStarter.copy(alpha = 0.15f)
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onNavigateToPricing() }
                        .testTag("dashboard_plan_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when (user?.tier) {
                                        com.example.data.model.SubscriptionTier.BUSINESS -> PlanBusiness
                                        com.example.data.model.SubscriptionTier.PRO -> PlanPro
                                        else -> PlanStarter
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${user?.tier?.title ?: "Starter"} Plan",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = when (user?.tier) {
                                com.example.data.model.SubscriptionTier.BUSINESS -> PlanBusiness
                                com.example.data.model.SubscriptionTier.PRO -> PlanPro
                                else -> PlanStarter
                            }
                        )
                    }
                }
            }
        }

        // Quick Add Task Bar
        item {
            QuickAddBar(
                projects = projects,
                defaultProjectId = projects.firstOrNull()?.id,
                onAddTask = { title, projId, prio ->
                    onQuickAddTask(title, projId, prio)
                }
            )
        }

        // Summary Statistics Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Today Stat
                StatCard(
                    title = "Due Today",
                    count = dueTodayTasks.size.toString(),
                    icon = Icons.Outlined.Today,
                    color = BrandPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToAllTasks
                )
                // Overdue Stat
                StatCard(
                    title = "Overdue",
                    count = overdueTasks.size.toString(),
                    icon = Icons.Default.Warning,
                    color = if (overdueTasks.isNotEmpty()) BrandUrgent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToAllTasks
                )
                // Total Active
                StatCard(
                    title = "Active",
                    count = activeTasks.size.toString(),
                    icon = Icons.Outlined.Assignment,
                    color = BrandSecondary,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToAllTasks
                )
                // Completed
                StatCard(
                    title = "Completed",
                    count = archiveTasks.count { it.isCompleted }.toString(),
                    icon = Icons.Default.CheckCircle,
                    color = BrandAccent,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToArchive
                )
            }
        }

        // Projects Carousel Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Projects (${projects.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(
                    onClick = { onOpenProjectEditor(null) },
                    modifier = Modifier.testTag("dashboard_new_project_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Project")
                }
            }

            if (projects.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(40.dp), tint = BrandPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No projects yet", fontWeight = FontWeight.Bold)
                        Text("Create your first project to start organizing tasks.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onOpenProjectEditor(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                        ) {
                            Text("Create Project")
                        }
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(projects, key = { it.id }) { proj ->
                        ProjectCard(
                            project = proj,
                            allTasks = activeTasks + archiveTasks,
                            onClick = { onNavigateToProject(proj.id) },
                            onEdit = { onOpenProjectEditor(proj) },
                            onArchiveToggle = { onArchiveProjectToggle(proj) },
                            onDelete = { onDeleteProject(proj) },
                            modifier = Modifier.width(260.dp)
                        )
                    }
                }
            }
        }

        // Urgent / Due Today Focus Tasks
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (dueTodayTasks.isNotEmpty()) "Focus: Due Today (${dueTodayTasks.size})" else "Priority Tasks (${urgentTasks.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = onNavigateToAllTasks) {
                    Text("View All")
                }
            }
        }

        val displayFocusTasks = if (dueTodayTasks.isNotEmpty()) dueTodayTasks else urgentTasks
        if (displayFocusTasks.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Celebration, contentDescription = null, tint = BrandAccent, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("All caught up!", fontWeight = FontWeight.SemiBold)
                            Text("No urgent or overdue tasks at the moment.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        } else {
            items(displayFocusTasks.take(5), key = { it.id }) { task ->
                val project = projects.firstOrNull { it.id == task.projectId }
                TaskCard(
                    task = task,
                    project = project,
                    allProjects = projects,
                    onCompleteToggle = onCompleteTask,
                    onEdit = onOpenTaskEditor,
                    onDuplicate = onDuplicateTask,
                    onMove = onMoveTask,
                    onArchive = onArchiveTask,
                    onRestore = onRestoreTask,
                    onDelete = onDeleteTask,
                    onToggleSubtask = onToggleSubtask
                )
            }
        }

        // Recently Completed Section
        if (completedRecently.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recently Completed",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(onClick = onNavigateToArchive) {
                        Text("Open Archive")
                    }
                }
            }

            items(completedRecently, key = { it.id }) { task ->
                val project = projects.firstOrNull { it.id == task.projectId }
                TaskCard(
                    task = task,
                    project = project,
                    allProjects = projects,
                    onCompleteToggle = onCompleteTask,
                    onEdit = onOpenTaskEditor,
                    onDuplicate = onDuplicateTask,
                    onMove = onMoveTask,
                    onArchive = onArchiveTask,
                    onRestore = onRestoreTask,
                    onDelete = onDeleteTask,
                    onToggleSubtask = onToggleSubtask
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    count: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = count,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
