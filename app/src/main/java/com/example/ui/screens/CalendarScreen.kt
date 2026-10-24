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
import com.example.data.model.Project
import com.example.data.model.SubscriptionTier
import com.example.data.model.TaskItem
import com.example.data.model.UserAccount
import com.example.ui.components.TaskCard
import com.example.ui.theme.BrandAccent
import com.example.ui.theme.BrandPrimary
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarScreen(
    user: UserAccount?,
    activeTasks: List<TaskItem>,
    projects: List<Project>,
    onCompleteTask: (TaskItem) -> Unit,
    onOpenTaskEditor: (TaskItem?) -> Unit,
    onDuplicateTask: (TaskItem) -> Unit,
    onMoveTask: (TaskItem, Long) -> Unit,
    onArchiveTask: (TaskItem) -> Unit,
    onRestoreTask: (TaskItem) -> Unit,
    onDeleteTask: (TaskItem) -> Unit,
    onToggleSubtask: (TaskItem, String) -> Unit,
    onNavigateToPricing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isProOrBusiness = user?.tier?.hasCalendarView == true

    if (!isProOrBusiness) {
        // Upgrade Gate for Starter
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
                            .background(BrandPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(28.dp))
                    }
                    Text(
                        text = "Visual Calendar & Timeline View",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Track your deadlines on a chronological calendar timeline. Plan recurring sprints, manage workload, and never miss a due date.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onNavigateToPricing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calendar_upgrade_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                    ) {
                        Text("Upgrade to Pro to Unlock Calendar ($20/mo)")
                    }
                }
            }
        }
        return
    }

    // Pro / Business Calendar Agenda View
    val sdfDay = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }
    val groupedTasks = remember(activeTasks) {
        val withDate = activeTasks.filter { it.dueDate != null }.sortedBy { it.dueDate }
        val withoutDate = activeTasks.filter { it.dueDate == null }
        val map = LinkedHashMap<String, MutableList<TaskItem>>()
        withDate.forEach { task ->
            val key = sdfDay.format(Date(task.dueDate!!))
            map.getOrPut(key) { mutableListOf() }.add(task)
        }
        if (withoutDate.isNotEmpty()) {
            map["No Due Date (Someday)"] = withoutDate.toMutableList()
        }
        map
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("calendar_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Calendar Agenda",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Chronological timeline of upcoming tasks & recurring events",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (groupedTasks.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No scheduled tasks", fontWeight = FontWeight.Bold)
                        Text("Add due dates to your tasks to see them plotted here.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            groupedTasks.forEach { (dateLabel, tasks) ->
                item(key = "header_$dateLabel") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(BrandPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = BrandPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(${tasks.size})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(tasks, key = { it.id }) { task ->
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
}
