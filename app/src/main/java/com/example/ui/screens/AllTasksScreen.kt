package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Project
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.data.model.UserAccount
import com.example.ui.components.QuickAddBar
import com.example.ui.components.TaskCard
import com.example.ui.viewmodel.TaskSort

enum class TaskViewFilter(val label: String) {
    ALL("All Tasks"),
    TODAY("Today"),
    UPCOMING("Upcoming (7d)"),
    OVERDUE("Overdue"),
    URGENT("Urgent & High")
}

@Composable
fun AllTasksScreen(
    user: UserAccount?,
    activeTasks: List<TaskItem>,
    projects: List<Project>,
    onCompleteTask: (TaskItem) -> Unit,
    onOpenTaskEditor: (TaskItem?) -> Unit,
    onQuickAddTask: (String, Long, TaskPriority) -> Unit,
    onDuplicateTask: (TaskItem) -> Unit,
    onMoveTask: (TaskItem, Long) -> Unit,
    onArchiveTask: (TaskItem) -> Unit,
    onRestoreTask: (TaskItem) -> Unit,
    onDeleteTask: (TaskItem) -> Unit,
    onToggleSubtask: (TaskItem, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(TaskViewFilter.ALL) }
    var selectedPriority by remember { mutableStateOf<TaskPriority?>(null) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var selectedSort by remember { mutableStateOf(TaskSort.DUE_DATE) }
    var searchQuery by remember { mutableStateOf("") }
    var showSortMenu by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val dayMs = 24 * 60 * 60 * 1000L

    // Extract all unique tags
    val allTags = remember(activeTasks) {
        activeTasks.flatMap { it.getTagsList() }.distinct()
    }

    // Filter tasks
    val filteredTasks = remember(activeTasks, selectedFilter, selectedPriority, selectedTag, selectedSort, searchQuery) {
        activeTasks
            .filter { task ->
                when (selectedFilter) {
                    TaskViewFilter.ALL -> true
                    TaskViewFilter.TODAY -> task.dueDate != null && task.dueDate in (now - dayMs / 2)..(now + dayMs)
                    TaskViewFilter.UPCOMING -> task.dueDate != null && task.dueDate > now
                    TaskViewFilter.OVERDUE -> task.dueDate != null && task.dueDate < now - dayMs / 2
                    TaskViewFilter.URGENT -> task.priority == TaskPriority.URGENT || task.priority == TaskPriority.HIGH
                }
            }
            .filter { selectedPriority == null || it.priority == selectedPriority }
            .filter { selectedTag == null || it.getTagsList().contains(selectedTag) }
            .filter {
                if (searchQuery.isBlank()) true
                else it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true)
            }
            .sortedWith { a, b ->
                when (selectedSort) {
                    TaskSort.DUE_DATE -> {
                        val aDue = a.dueDate ?: Long.MAX_VALUE
                        val bDue = b.dueDate ?: Long.MAX_VALUE
                        aDue.compareTo(bDue)
                    }
                    TaskSort.PRIORITY -> b.priority.level.compareTo(a.priority.level)
                    TaskSort.CREATED -> b.createdAt.compareTo(a.createdAt)
                    TaskSort.TITLE -> a.title.compareTo(b.title, ignoreCase = true)
                }
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("all_tasks_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Task Center",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${filteredTasks.size} active tasks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = { onOpenTaskEditor(null) },
                    modifier = Modifier.testTag("all_tasks_new_task_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Task")
                }
            }

            // Quick Add Bar
            QuickAddBar(
                projects = projects,
                defaultProjectId = projects.firstOrNull()?.id,
                onAddTask = onQuickAddTask
            )

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter tasks...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // View Filter Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(TaskViewFilter.values()) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label) }
                    )
                }

                // Sort Dropdown
                item {
                    Box {
                        AssistChip(
                            onClick = { showSortMenu = true },
                            label = { Text("Sort: ${selectedSort.label}") },
                            leadingIcon = { Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            TaskSort.values().forEach { sort ->
                                DropdownMenuItem(
                                    text = { Text(sort.label) },
                                    onClick = {
                                        selectedSort = sort
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Tags row (if any)
            if (allTags.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedTag == null,
                            onClick = { selectedTag = null },
                            label = { Text("All Tags") }
                        )
                    }
                    items(allTags) { tag ->
                        FilterChip(
                            selected = selectedTag == tag,
                            onClick = { selectedTag = if (selectedTag == tag) null else tag },
                            label = { Text("#$tag") }
                        )
                    }
                }
            }
        }

        // Tasks List
        if (filteredTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChecklistRtl,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(56.dp)
                    )
                    Text("No tasks found", fontWeight = FontWeight.Bold)
                    Text(
                        "Use the quick-add bar above or adjust your filters.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTasks, key = { it.id }) { task ->
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
