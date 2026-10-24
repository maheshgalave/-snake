package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Project
import com.example.data.model.TaskPriority
import com.example.ui.theme.BrandPrimary

@Composable
fun QuickAddBar(
    projects: List<Project>,
    defaultProjectId: Long?,
    onAddTask: (title: String, projectId: Long, priority: TaskPriority) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var selectedProjectId by remember(projects, defaultProjectId) {
        mutableStateOf(defaultProjectId ?: projects.firstOrNull()?.id ?: 0L)
    }
    var selectedPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var showProjectDropdown by remember { mutableStateOf(false) }
    var showPriorityDropdown by remember { mutableStateOf(false) }

    val currentProject = projects.firstOrNull { it.id == selectedProjectId } ?: projects.firstOrNull()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("What needs to be done? Quick add...", fontSize = 14.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_add_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (title.isNotBlank() && selectedProjectId != 0L) {
                                onAddTask(title, selectedProjectId, selectedPriority)
                                title = ""
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                FilledIconButton(
                    onClick = {
                        if (title.isNotBlank() && selectedProjectId != 0L) {
                            onAddTask(title, selectedProjectId, selectedPriority)
                            title = ""
                        }
                    },
                    enabled = title.isNotBlank() && selectedProjectId != 0L,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("quick_add_submit_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = BrandPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Task",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Filters for Project & Priority
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Project picker chip
                Box {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showProjectDropdown = true }
                            .testTag("quick_add_project_picker")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            val pColor = currentProject?.let {
                                runCatching { Color(android.graphics.Color.parseColor(it.colorHex)) }.getOrDefault(BrandPrimary)
                            } ?: BrandPrimary
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(pColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentProject?.name ?: "Select Project",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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

                // Priority picker chip
                Box {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showPriorityDropdown = true }
                            .testTag("quick_add_priority_picker")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Priority: ${selectedPriority.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showPriorityDropdown,
                        onDismissRequest = { showPriorityDropdown = false }
                    ) {
                        TaskPriority.values().forEach { prio ->
                            DropdownMenuItem(
                                text = { Text(prio.label) },
                                leadingIcon = { PriorityBadge(priority = prio) },
                                onClick = {
                                    selectedPriority = prio
                                    showPriorityDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
