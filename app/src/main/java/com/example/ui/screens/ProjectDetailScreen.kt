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
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.ui.components.QuickAddBar
import com.example.ui.components.TaskCard
import com.example.ui.theme.BrandAccent
import com.example.ui.theme.BrandPrimary

@Composable
fun ProjectDetailScreen(
    project: Project,
    allProjects: List<Project>,
    allTasks: List<TaskItem>,
    onCompleteTask: (TaskItem) -> Unit,
    onOpenTaskEditor: (TaskItem?) -> Unit,
    onQuickAddTask: (String, Long, TaskPriority) -> Unit,
    onDuplicateTask: (TaskItem) -> Unit,
    onMoveTask: (TaskItem, Long) -> Unit,
    onArchiveTask: (TaskItem) -> Unit,
    onRestoreTask: (TaskItem) -> Unit,
    onDeleteTask: (TaskItem) -> Unit,
    onToggleSubtask: (TaskItem, String) -> Unit,
    onEditProject: (Project) -> Unit,
    onArchiveProjectToggle: (Project) -> Unit,
    onDeleteProject: (Project) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCompleted by remember { mutableStateOf(false) }

    val projectTasks = remember(allTasks, project.id) {
        allTasks.filter { it.projectId == project.id }
    }
    val activeTasks = remember(projectTasks) { projectTasks.filter { !it.isCompleted && !it.isArchived } }
    val completedTasks = remember(projectTasks) { projectTasks.filter { it.isCompleted } }

    val totalCount = activeTasks.size + completedTasks.size
    val progress = if (totalCount > 0) completedTasks.size.toFloat() / totalCount else 0f
    val percent = (progress * 100).toInt()

    val projectColor = remember(project.colorHex) {
        runCatching { Color(android.graphics.Color.parseColor(project.colorHex)) }.getOrDefault(BrandPrimary)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("project_detail_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Project Header Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(projectColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = projectColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = project.name,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                if (project.description.isNotBlank()) {
                                    Text(
                                        text = project.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        IconButton(onClick = { onEditProject(project) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Project")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress bar & counts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${activeTasks.size} active · ${completedTasks.size} completed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$percent% Complete",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (percent == 100 && totalCount > 0) BrandAccent else projectColor
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = if (percent == 100 && totalCount > 0) BrandAccent else projectColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // Quick Add for this specific Project
        item {
            QuickAddBar(
                projects = listOf(project),
                defaultProjectId = project.id,
                onAddTask = { title, _, priority ->
                    onQuickAddTask(title, project.id, priority)
                }
            )
        }

        // Active vs Completed Tab Switcher
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !showCompleted,
                        onClick = { showCompleted = false },
                        label = { Text("Active Tasks (${activeTasks.size})") }
                    )
                    FilterChip(
                        selected = showCompleted,
                        onClick = { showCompleted = true },
                        label = { Text("Completed (${completedTasks.size})") },
                        leadingIcon = {
                            Icon(Icons.Default.Check, contentDescription = null, tint = BrandAccent, modifier = Modifier.size(14.dp))
                        }
                    )
                }

                FilledTonalButton(
                    onClick = { onOpenTaskEditor(null) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Task", fontSize = 12.sp)
                }
            }
        }

        // Tasks list
        val displayedTasks = if (showCompleted) completedTasks else activeTasks
        if (displayedTasks.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (showCompleted) "No completed tasks yet" else "No active tasks in this project",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (showCompleted) "Tasks you finish with 1-click will appear here." else "Add tasks using the quick-add input above.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(displayedTasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    project = project,
                    allProjects = allProjects,
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
