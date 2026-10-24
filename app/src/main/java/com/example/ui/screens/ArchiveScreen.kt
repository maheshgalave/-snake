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
import com.example.data.model.UserAccount
import com.example.ui.components.TaskCard
import com.example.ui.theme.BrandAccent
import com.example.ui.theme.BrandPrimary
import com.example.ui.viewmodel.ArchiveFilter
import com.example.ui.viewmodel.TaskSort
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ArchiveScreen(
    user: UserAccount?,
    archiveTasks: List<TaskItem>,
    projects: List<Project>,
    selectedTaskIds: Set<Long>,
    onToggleSelectTaskId: (Long) -> Unit,
    onSelectAll: (List<Long>) -> Unit,
    onClearSelection: () -> Unit,
    onRestoreTask: (TaskItem, Long?) -> Unit,
    onDeletePermanently: (TaskItem) -> Unit,
    onBulkRestore: (List<TaskItem>, Long?) -> Unit,
    onBulkDelete: (List<TaskItem>) -> Unit,
    onBulkMove: (List<TaskItem>, Long) -> Unit,
    onEditTask: (TaskItem) -> Unit,
    onDuplicateTask: (TaskItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ArchiveFilter.ALL) }
    var selectedProjectId by remember { mutableStateOf<Long?>(null) }
    var selectedSort by remember { mutableStateOf(TaskSort.DUE_DATE) }
    var showProjectDropdown by remember { mutableStateOf(false) }
    var showSortDropdown by remember { mutableStateOf(false) }
    var showBulkMoveDialog by remember { mutableStateOf(false) }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }

    // Filter & Search processing
    val filteredTasks = remember(archiveTasks, searchQuery, selectedFilter, selectedProjectId, selectedSort) {
        archiveTasks
            .filter { task ->
                when (selectedFilter) {
                    ArchiveFilter.ALL -> true
                    ArchiveFilter.COMPLETED_ONLY -> task.isCompleted
                    ArchiveFilter.MANUALLY_ARCHIVED -> task.isArchived && !task.isCompleted
                }
            }
            .filter { task ->
                selectedProjectId == null || task.projectId == selectedProjectId
            }
            .filter { task ->
                if (searchQuery.isBlank()) true
                else {
                    task.title.contains(searchQuery, ignoreCase = true) ||
                    task.description.contains(searchQuery, ignoreCase = true) ||
                    task.tagsCsv.contains(searchQuery, ignoreCase = true)
                }
            }
            .sortedWith { a, b ->
                when (selectedSort) {
                    TaskSort.DUE_DATE -> (b.completedAt ?: b.updatedAt).compareTo(a.completedAt ?: a.updatedAt)
                    TaskSort.PRIORITY -> b.priority.level.compareTo(a.priority.level)
                    TaskSort.CREATED -> b.createdAt.compareTo(a.createdAt)
                    TaskSort.TITLE -> a.title.compareTo(b.title, ignoreCase = true)
                }
            }
    }

    val hasSelection = selectedTaskIds.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("archive_screen")
    ) {
        // Top Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Archive & Completed",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${filteredTasks.size} archived tasks · Restore or purge anytime",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (user?.tier?.hasBulkActions == true && filteredTasks.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            if (hasSelection) onClearSelection()
                            else onSelectAll(filteredTasks.map { it.id })
                        },
                        modifier = Modifier.testTag("archive_select_all_button")
                    ) {
                        Text(if (hasSelection) "Clear (${selectedTaskIds.size})" else "Select All")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search archived tasks, projects, tags...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("archive_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips Row: ALL vs COMPLETED vs MANUALLY ARCHIVED
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == ArchiveFilter.ALL,
                        onClick = { selectedFilter = ArchiveFilter.ALL },
                        label = { Text("All (${archiveTasks.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == ArchiveFilter.COMPLETED_ONLY,
                        onClick = { selectedFilter = ArchiveFilter.COMPLETED_ONLY },
                        label = { Text("Completed (${archiveTasks.count { it.isCompleted }})") },
                        leadingIcon = {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrandAccent, modifier = Modifier.size(16.dp))
                        }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == ArchiveFilter.MANUALLY_ARCHIVED,
                        onClick = { selectedFilter = ArchiveFilter.MANUALLY_ARCHIVED },
                        label = { Text("Archived (${archiveTasks.count { it.isArchived && !it.isCompleted }})") },
                        leadingIcon = {
                            Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }

                // Project Filter dropdown chip
                item {
                    Box {
                        AssistChip(
                            onClick = { showProjectDropdown = true },
                            label = {
                                val projName = projects.firstOrNull { it.id == selectedProjectId }?.name ?: "All Projects"
                                Text(projName)
                            },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
                        )

                        DropdownMenu(
                            expanded = showProjectDropdown,
                            onDismissRequest = { showProjectDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Projects") },
                                onClick = {
                                    selectedProjectId = null
                                    showProjectDropdown = false
                                }
                            )
                            projects.forEach { proj ->
                                DropdownMenuItem(
                                    text = { Text(proj.name) },
                                    onClick = {
                                        selectedProjectId = proj.id
                                        showProjectDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Sort Dropdown Chip
                item {
                    Box {
                        AssistChip(
                            onClick = { showSortDropdown = true },
                            label = { Text("Sort: ${selectedSort.label}") },
                            leadingIcon = { Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )

                        DropdownMenu(
                            expanded = showSortDropdown,
                            onDismissRequest = { showSortDropdown = false }
                        ) {
                            TaskSort.values().forEach { sort ->
                                DropdownMenuItem(
                                    text = { Text(sort.label) },
                                    onClick = {
                                        selectedSort = sort
                                        showSortDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bulk Action Bar (when items selected)
        if (hasSelection) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${selectedTaskIds.size} selected",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(
                            onClick = { onBulkRestore(filteredTasks, null) },
                            modifier = Modifier.testTag("bulk_restore_button")
                        ) {
                            Icon(Icons.Default.Unarchive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restore")
                        }
                        TextButton(
                            onClick = { showBulkMoveDialog = true }
                        ) {
                            Icon(Icons.Default.DriveFileMove, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Move")
                        }
                        TextButton(
                            onClick = { showBulkDeleteConfirm = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete")
                        }
                    }
                }
            }
        }

        // Task List / Empty State
        if (filteredTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Archive is empty",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Completed tasks and manually archived items will be stored here.",
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
                    val isSelected = selectedTaskIds.contains(task.id)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (user?.tier?.hasBulkActions == true) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { onToggleSelectTaskId(task.id) },
                                modifier = Modifier.testTag("archive_checkbox_${task.id}")
                            )
                        }

                        TaskCard(
                            task = task,
                            project = project,
                            allProjects = projects,
                            onCompleteToggle = { onRestoreTask(task, null) }, // toggle in archive restores to active
                            onEdit = onEditTask,
                            onDuplicate = onDuplicateTask,
                            onMove = { _, targetProj -> onRestoreTask(task, targetProj) },
                            onArchive = { /* already archived */ },
                            onRestore = { onRestoreTask(task, null) },
                            onDelete = onDeletePermanently,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    // Bulk Move Dialog
    if (showBulkMoveDialog) {
        AlertDialog(
            onDismissRequest = { showBulkMoveDialog = false },
            title = { Text("Move Selected Tasks to Project") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    projects.forEach { p ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onBulkMove(filteredTasks, p.id)
                                    showBulkMoveDialog = false
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(p.name, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBulkMoveDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Bulk Delete Confirmation
    if (showBulkDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            title = { Text("Permanently Delete ${selectedTaskIds.size} Tasks?") },
            text = {
                Text("This action cannot be undone. These tasks will be permanently removed from your account.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onBulkDelete(filteredTasks)
                        showBulkDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
