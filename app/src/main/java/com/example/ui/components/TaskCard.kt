package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Project
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TaskCard(
    task: TaskItem,
    project: Project?,
    allProjects: List<Project>,
    onCompleteToggle: (TaskItem) -> Unit,
    onEdit: (TaskItem) -> Unit,
    onDuplicate: (TaskItem) -> Unit,
    onMove: (TaskItem, Long) -> Unit,
    onArchive: (TaskItem) -> Unit,
    onRestore: (TaskItem) -> Unit,
    onDelete: (TaskItem) -> Unit,
    onToggleSubtask: ((TaskItem, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var expandedSubtasks by remember { mutableStateOf(false) }

    val subtasks = remember(task.subtasksRaw) { task.getSubtasksList() }
    val tags = remember(task.tagsCsv) { task.getTagsList() }
    val isOverdue = remember(task.dueDate, task.isCompleted) {
        task.dueDate != null && !task.isCompleted && task.dueDate < System.currentTimeMillis() - 24 * 60 * 60 * 1000L
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}")
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.isCompleted) 0.dp else 1.5.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // One-Click Completion Checkbox Control
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { onCompleteToggle(task) }
                        .testTag("task_checkbox_${task.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isCompleted) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(BrandAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .then(
                                    Modifier.padding(2.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(
                                        when (task.priority) {
                                            TaskPriority.URGENT -> BrandUrgent.copy(alpha = 0.2f)
                                            TaskPriority.HIGH -> PriorityHigh.copy(alpha = 0.2f)
                                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        }
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Title & Details
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onEdit(task) }
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (task.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Badges Row: Project, Priority, Due Date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Project pill
                        if (project != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.height(24.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    val projectColor = runCatching { Color(android.graphics.Color.parseColor(project.colorHex)) }.getOrDefault(BrandPrimary)
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(projectColor)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = project.name,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Priority badge
                        PriorityBadge(priority = task.priority)

                        // Due Date Badge
                        if (task.dueDate != null) {
                            val sdf = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
                            val dateStr = sdf.format(Date(task.dueDate))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isOverdue) BrandUrgent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.height(24.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isOverdue) Icons.Default.Warning else Icons.Outlined.CalendarToday,
                                        contentDescription = null,
                                        tint = if (isOverdue) BrandUrgent else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = dateStr,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isOverdue) BrandUrgent else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Recurring indicator
                        if (task.isRecurring) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Recurring",
                                tint = BrandPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // Tags
                    if (tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            tags.take(3).forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = BrandPrimary.copy(alpha = 0.08f),
                                    modifier = Modifier.height(20.dp)
                                ) {
                                    Text(
                                        text = "#$tag",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = BrandPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Subtasks toggle button if any exist
                    if (subtasks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val completedSubtasks = subtasks.count { it.isDone }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { expandedSubtasks = !expandedSubtasks }
                                .padding(vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (expandedSubtasks) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Subtasks ($completedSubtasks/${subtasks.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Inline Subtasks
                        AnimatedVisibility(
                            visible = expandedSubtasks,
                            enter = expandVertically(spring()) + fadeIn(),
                            exit = shrinkVertically(spring()) + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, start = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                subtasks.forEach { subtask ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { onToggleSubtask?.invoke(task, subtask.id) }
                                            .padding(vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (subtask.isDone) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (subtask.isDone) BrandAccent else MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = subtask.title,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 12.sp,
                                                textDecoration = if (subtask.isDone) TextDecoration.LineThrough else TextDecoration.None
                                            ),
                                            color = if (subtask.isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Menu Button
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("task_menu_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Task options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Task") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onEdit(task)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onDuplicate(task)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Move to Project...") },
                            leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showMoveDialog = true
                            }
                        )
                        if (!task.isArchived) {
                            DropdownMenuItem(
                                text = { Text("Archive Task") },
                                leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onArchive(task)
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Restore to Active") },
                                leadingIcon = { Icon(Icons.Default.Unarchive, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onRestore(task)
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete Permanently", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete(task)
                            }
                        )
                    }
                }
            }
        }
    }

    // Move to Project Dialog
    if (showMoveDialog) {
        AlertDialog(
            onDismissRequest = { showMoveDialog = false },
            title = { Text("Move Task to Project") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select destination project:")
                    allProjects.forEach { p ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (p.id == task.projectId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onMove(task, p.id)
                                    showMoveDialog = false
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val pColor = runCatching { Color(android.graphics.Color.parseColor(p.colorHex)) }.getOrDefault(BrandPrimary)
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(pColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(p.name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMoveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PriorityBadge(priority: TaskPriority) {
    val (bgColor, textColor, label) = when (priority) {
        TaskPriority.URGENT -> Triple(BrandUrgent.copy(alpha = 0.15f), BrandUrgent, "Urgent")
        TaskPriority.HIGH -> Triple(PriorityHigh.copy(alpha = 0.15f), PriorityHigh, "High")
        TaskPriority.MEDIUM -> Triple(PriorityMedium.copy(alpha = 0.15f), PriorityMedium, "Medium")
        TaskPriority.LOW -> Triple(PriorityLow.copy(alpha = 0.15f), PriorityLow, "Low")
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = Modifier.height(24.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            ),
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
