package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.model.*
import com.example.ui.theme.BrandPrimary
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TaskEditorDialog(
    initialTask: TaskItem?,
    projects: List<Project>,
    defaultProjectId: Long?,
    userTier: SubscriptionTier,
    teamMembers: List<TeamMember>,
    onSave: (
        id: Long,
        title: String,
        description: String,
        projectId: Long,
        priority: TaskPriority,
        dueDate: Long?,
        dueTime: String?,
        tagsCsv: String,
        subtasksRaw: String,
        isRecurring: Boolean,
        recurrenceRule: String?,
        hasReminder: Boolean,
        assignedTo: String?
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var description by remember { mutableStateOf(initialTask?.description ?: "") }
    var selectedProjectId by remember {
        mutableStateOf(initialTask?.projectId ?: defaultProjectId ?: projects.firstOrNull()?.id ?: 0L)
    }
    var priority by remember { mutableStateOf(initialTask?.priority ?: TaskPriority.MEDIUM) }
    var dueDate by remember { mutableStateOf(initialTask?.dueDate) }
    var tagsInput by remember { mutableStateOf(initialTask?.tagsCsv ?: "") }

    var subtasksList by remember {
        mutableStateOf(initialTask?.getSubtasksList() ?: emptyList())
    }
    var newSubtaskText by remember { mutableStateOf("") }

    var isRecurring by remember { mutableStateOf(initialTask?.isRecurring ?: false) }
    var recurrenceRule by remember { mutableStateOf(initialTask?.recurrenceRule ?: "WEEKLY") }
    var hasReminder by remember { mutableStateOf(initialTask?.hasReminder ?: false) }
    var assignedTo by remember { mutableStateOf(initialTask?.assignedTo) }

    var showProjectDropdown by remember { mutableStateOf(false) }

    val currentProject = projects.firstOrNull { it.id == selectedProjectId } ?: projects.firstOrNull()
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialTask == null) "New Task" else "Edit Task",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    placeholder = { Text("e.g., Finalize quarterly budget") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_editor_title_input")
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description & Notes (optional)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                // Project selector
                Column {
                    Text("Project", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showProjectDropdown = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val pColor = currentProject?.let {
                                        runCatching { Color(android.graphics.Color.parseColor(it.colorHex)) }.getOrDefault(BrandPrimary)
                                    } ?: BrandPrimary
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(pColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(currentProject?.name ?: "No Projects", style = MaterialTheme.typography.bodyMedium)
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }

                        DropdownMenu(
                            expanded = showProjectDropdown,
                            onDismissRequest = { showProjectDropdown = false }
                        ) {
                            projects.forEach { proj ->
                                DropdownMenuItem(
                                    text = { Text(proj.name) },
                                    leadingIcon = {
                                        val color = runCatching { Color(android.graphics.Color.parseColor(proj.colorHex)) }.getOrDefault(BrandPrimary)
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                        )
                                    },
                                    onClick = {
                                        selectedProjectId = proj.id
                                        showProjectDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Priority Row
                Column {
                    Text("Priority", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TaskPriority.values().forEach { prio ->
                            FilterChip(
                                selected = priority == prio,
                                onClick = { priority = prio },
                                label = { Text(prio.label, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Due Date Quick Selectors
                Column {
                    Text("Due Date", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val now = System.currentTimeMillis()
                        val dayMs = 24 * 60 * 60 * 1000L

                        FilterChip(
                            selected = dueDate != null && Math.abs(dueDate!! - now) < dayMs,
                            onClick = { dueDate = now },
                            label = { Text("Today", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = dueDate != null && Math.abs(dueDate!! - (now + dayMs)) < dayMs / 2,
                            onClick = { dueDate = now + dayMs },
                            label = { Text("Tomorrow", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = dueDate != null && Math.abs(dueDate!! - (now + 7 * dayMs)) < dayMs,
                            onClick = { dueDate = now + 7 * dayMs },
                            label = { Text("Next Week", fontSize = 11.sp) }
                        )
                        if (dueDate != null) {
                            FilterChip(
                                selected = false,
                                onClick = { dueDate = null },
                                label = { Text("Clear", fontSize = 11.sp) }
                            )
                        }
                    }
                    if (dueDate != null) {
                        val sdf = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) }
                        Text(
                            text = "Scheduled: ${sdf.format(Date(dueDate!!))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandPrimary
                        )
                    }
                }

                // Subtasks / Checklist (Pro/Business)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Subtasks & Checklist", style = MaterialTheme.typography.labelMedium)
                        if (!userTier.hasSubtasks) {
                            Surface(shape = RoundedCornerShape(4.dp), color = BrandPrimary.copy(alpha = 0.1f)) {
                                Text("Pro Feature", fontSize = 10.sp, color = BrandPrimary, modifier = Modifier.padding(horizontal = 4.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    subtasksList.forEach { sub ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (sub.isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(sub.title, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = { subtasksList = subtasksList.filter { it.id != sub.id } },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newSubtaskText,
                            onValueChange = { newSubtaskText = it },
                            placeholder = { Text("Add subtask...", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        FilledTonalIconButton(
                            onClick = {
                                if (newSubtaskText.isNotBlank()) {
                                    val newSub = Subtask(UUID.randomUUID().toString().take(6), newSubtaskText.trim(), false)
                                    subtasksList = subtasksList + newSub
                                    newSubtaskText = ""
                                }
                            },
                            enabled = newSubtaskText.isNotBlank()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Subtask")
                        }
                    }
                }

                // Tags (Pro/Business)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tags & Labels", style = MaterialTheme.typography.labelMedium)
                        if (!userTier.hasTags) {
                            Surface(shape = RoundedCornerShape(4.dp), color = BrandPrimary.copy(alpha = 0.1f)) {
                                Text("Pro Feature", fontSize = 10.sp, color = BrandPrimary, modifier = Modifier.padding(horizontal = 4.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = tagsInput,
                        onValueChange = { tagsInput = it },
                        placeholder = { Text("e.g. Design, Urgent, Q4 (comma separated)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Recurring Toggle (Pro/Business)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Recurring Task", style = MaterialTheme.typography.bodyMedium)
                        Text("Repeats automatically on completion", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
                }

                if (isRecurring) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("DAILY", "WEEKLY", "MONTHLY").forEach { rule ->
                            FilterChip(
                                selected = recurrenceRule == rule,
                                onClick = { recurrenceRule = rule },
                                label = { Text(rule.lowercase().capitalize(Locale.getDefault()), fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Team Assignee (Business)
                if (teamMembers.isNotEmpty()) {
                    Column {
                        Text("Assigned Member (Business)", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                FilterChip(
                                    selected = assignedTo == null,
                                    onClick = { assignedTo = null },
                                    label = { Text("Unassigned") }
                                )
                            }
                            items(teamMembers) { member ->
                                FilterChip(
                                    selected = assignedTo == member.name,
                                    onClick = { assignedTo = member.name },
                                    label = { Text(member.name) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && selectedProjectId != 0L) {
                        val subtasksRaw = subtasksList.joinToString("|") { "${it.id}::${it.title}::${it.isDone}" }
                        onSave(
                            initialTask?.id ?: 0L,
                            title,
                            description,
                            selectedProjectId,
                            priority,
                            dueDate,
                            null,
                            tagsInput,
                            subtasksRaw,
                            isRecurring,
                            if (isRecurring) recurrenceRule else null,
                            hasReminder,
                            assignedTo
                        )
                    }
                },
                enabled = title.isNotBlank() && selectedProjectId != 0L,
                modifier = Modifier.testTag("task_editor_save_button"),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Text("Save Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ProjectEditorDialog(
    initialProject: Project?,
    onSave: (name: String, description: String, colorHex: String, iconName: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialProject?.name ?: "") }
    var description by remember { mutableStateOf(initialProject?.description ?: "") }
    var selectedColor by remember { mutableStateOf(initialProject?.colorHex ?: "#4F46E5") }
    var selectedIcon by remember { mutableStateOf(initialProject?.iconName ?: "folder") }

    val colors = listOf(
        "#4F46E5", // Indigo
        "#0EA5E9", // Sky
        "#10B981", // Emerald
        "#F59E0B", // Amber
        "#EF4444", // Rose
        "#8B5CF6", // Purple
        "#06B6D4", // Cyan
        "#64748B"  // Slate
    )

    val icons = listOf(
        "folder" to Icons.Default.Folder,
        "work" to Icons.Default.Work,
        "person" to Icons.Default.Person,
        "lightbulb" to Icons.Default.Lightbulb,
        "star" to Icons.Default.Star,
        "code" to Icons.Default.Code
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialProject == null) "Create Project" else "Edit Project",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Name *") },
                    placeholder = { Text("e.g., Mobile App Launch") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_editor_name_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Color Selection
                Column {
                    Text("Project Color", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        colors.forEach { hex ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColor = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedColor == hex) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Icon Selection
                Column {
                    Text("Icon", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        icons.forEach { (key, icon) ->
                            val isSelected = selectedIcon == key
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) BrandPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedIcon = key }
                            ) {
                                Box(
                                    modifier = Modifier.size(36.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = key,
                                        tint = if (isSelected) BrandPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, description, selectedColor, selectedIcon)
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("project_editor_save_button"),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Text("Save Project")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
