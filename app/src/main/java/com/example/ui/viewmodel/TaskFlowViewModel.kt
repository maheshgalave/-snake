package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.TaskFlowDatabase
import com.example.data.model.*
import com.example.data.repository.LimitCheckResult
import com.example.data.repository.TaskFlowRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppDestination(val title: String) {
    DASHBOARD("Dashboard"),
    INBOX("Inbox"),
    TODAY("Today"),
    UPCOMING("Upcoming"),
    ALL_TASKS("All Tasks"),
    PROJECT_DETAIL("Project"),
    ARCHIVE("Archive & Completed"),
    CALENDAR("Calendar View"),
    TEAM_WORKSPACE("Team Workspace"),
    PRICING_PLANS("Subscription & Plans"),
    SETTINGS("Settings & Profile")
}

enum class TaskSort(val label: String) {
    DUE_DATE("Due Date"),
    PRIORITY("Priority"),
    CREATED("Creation Date"),
    TITLE("Alphabetical")
}

enum class ArchiveFilter(val label: String) {
    ALL("All Archive"),
    COMPLETED_ONLY("Completed Tasks"),
    MANUALLY_ARCHIVED("Manually Archived")
}

enum class AuthMode {
    LOGGED_IN,
    LOGIN,
    REGISTER,
    FORGOT_PASSWORD
}

class TaskFlowViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskFlowRepository

    init {
        val db = TaskFlowDatabase.getDatabase(application)
        repository = TaskFlowRepository(db.taskFlowDao())
    }

    // --- Current User State ---
    private val _currentUserId = MutableStateFlow<Long?>(null)
    val currentUserId: StateFlow<Long?> = _currentUserId.asStateFlow()

    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    private val _authMode = MutableStateFlow<AuthMode>(AuthMode.LOGGED_IN)
    val authMode: StateFlow<AuthMode> = _authMode.asStateFlow()

    // --- Navigation & Views ---
    private val _currentDestination = MutableStateFlow(AppDestination.DASHBOARD)
    val currentDestination: StateFlow<AppDestination> = _currentDestination.asStateFlow()

    private val _selectedProjectId = MutableStateFlow<Long?>(null)
    val selectedProjectId: StateFlow<Long?> = _selectedProjectId.asStateFlow()

    // --- Search & Filters ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _priorityFilter = MutableStateFlow<TaskPriority?>(null)
    val priorityFilter: StateFlow<TaskPriority?> = _priorityFilter.asStateFlow()

    private val _tagFilter = MutableStateFlow<String?>(null)
    val tagFilter: StateFlow<String?> = _tagFilter.asStateFlow()

    private val _taskSort = MutableStateFlow(TaskSort.DUE_DATE)
    val taskSort: StateFlow<TaskSort> = _taskSort.asStateFlow()

    private val _archiveFilter = MutableStateFlow(ArchiveFilter.ALL)
    val archiveFilter: StateFlow<ArchiveFilter> = _archiveFilter.asStateFlow()

    // --- Bulk Selection in Archive ---
    private val _selectedArchiveTaskIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedArchiveTaskIds: StateFlow<Set<Long>> = _selectedArchiveTaskIds.asStateFlow()

    // --- Modals & Dialogs ---
    private val _upgradeModalTier = MutableStateFlow<SubscriptionTier?>(null)
    val upgradeModalTier: StateFlow<SubscriptionTier?> = _upgradeModalTier.asStateFlow()

    private val _upgradeReason = MutableStateFlow<String?>(null)
    val upgradeReason: StateFlow<String?> = _upgradeReason.asStateFlow()

    private val _showTaskEditor = MutableStateFlow(false)
    val showTaskEditor: StateFlow<Boolean> = _showTaskEditor.asStateFlow()

    private val _taskToEdit = MutableStateFlow<TaskItem?>(null)
    val taskToEdit: StateFlow<TaskItem?> = _taskToEdit.asStateFlow()

    private val _showProjectEditor = MutableStateFlow(false)
    val showProjectEditor: StateFlow<Boolean> = _showProjectEditor.asStateFlow()

    private val _projectToEdit = MutableStateFlow<Project?>(null)
    val projectToEdit: StateFlow<Project?> = _projectToEdit.asStateFlow()

    // --- Undo Notification ---
    private val _lastCompletedTask = MutableStateFlow<TaskItem?>(null)
    val lastCompletedTask: StateFlow<TaskItem?> = _lastCompletedTask.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // --- Reactive Data Streams from DB ---
    val activeProjects: StateFlow<List<Project>> = _currentUserId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getActiveProjects(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedProjects: StateFlow<List<Project>> = _currentUserId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getArchivedProjects(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTasks: StateFlow<List<TaskItem>> = _currentUserId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getActiveTasks(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archiveTasks: StateFlow<List<TaskItem>> = _currentUserId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getArchiveTasks(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activityLogs: StateFlow<List<ActivityLog>> = _currentUserId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getActivityLogs(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val teamMembers: StateFlow<List<TeamMember>> = _currentUserId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getTeamMembers(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val invoices: StateFlow<List<Invoice>> = _currentUserId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getInvoices(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Auto-initialize demo / default user
        viewModelScope.launch {
            val user = repository.ensureDefaultUserExists()
            _currentUserId.value = user.id
            _currentUser.value = user

            // Observe user updates (e.g. tier changes)
            repository.observeUser(user.id).collect { updated ->
                if (updated != null) {
                    _currentUser.value = updated
                }
            }
        }
    }

    // --- Navigation Actions ---
    fun navigateTo(dest: AppDestination, projectId: Long? = null) {
        _currentDestination.value = dest
        _selectedProjectId.value = projectId
        _selectedArchiveTaskIds.value = emptySet()
    }

    // --- Filters & Search ---
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setPriorityFilter(priority: TaskPriority?) { _priorityFilter.value = priority }
    fun setTagFilter(tag: String?) { _tagFilter.value = tag }
    fun setTaskSort(sort: TaskSort) { _taskSort.value = sort }
    fun setArchiveFilter(filter: ArchiveFilter) { _archiveFilter.value = filter }

    // --- One-Click Task Completion (CRITICAL) ---
    fun completeTaskOneClick(task: TaskItem) {
        viewModelScope.launch {
            val completed = repository.completeTask(task)
            _lastCompletedTask.value = completed
            _snackbarMessage.value = "Task completed: '${task.title}'"
        }
    }

    fun undoLastCompletion() {
        val task = _lastCompletedTask.value ?: return
        viewModelScope.launch {
            repository.undoCompleteTask(task)
            _lastCompletedTask.value = null
            _snackbarMessage.value = "Restored: '${task.title}'"
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    // --- Fast Quick Add Task ---
    fun quickAddTask(title: String, projectId: Long, priority: TaskPriority = TaskPriority.MEDIUM, dueDate: Long? = null) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            when (val check = repository.canCreateTask(user.id)) {
                is LimitCheckResult.Exceeded -> {
                    triggerUpgradePrompt(check.requiredTier, check.message)
                }
                is LimitCheckResult.Allowed -> {
                    val task = TaskItem(
                        userId = user.id,
                        projectId = projectId,
                        title = title.trim(),
                        priority = priority,
                        dueDate = dueDate,
                        createdAt = System.currentTimeMillis()
                    )
                    repository.createTask(task)
                    _snackbarMessage.value = "Task added to project"
                }
            }
        }
    }

    // --- Task Full Save (New or Edit) ---
    fun saveTask(
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
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            if (id == 0L) {
                // Check task limit
                when (val check = repository.canCreateTask(user.id)) {
                    is LimitCheckResult.Exceeded -> {
                        triggerUpgradePrompt(check.requiredTier, check.message)
                        return@launch
                    }
                    is LimitCheckResult.Allowed -> {
                        // Check subtasks / tags / recurring features if on starter
                        if ((tagsCsv.isNotBlank() || subtasksRaw.isNotBlank() || isRecurring) && !user.tier.hasSubtasks) {
                            triggerUpgradePrompt(SubscriptionTier.PRO, "Tags, subtasks, and recurring tasks are Pro features.")
                            return@launch
                        }
                        val task = TaskItem(
                            userId = user.id,
                            projectId = projectId,
                            title = title.trim(),
                            description = description.trim(),
                            priority = priority,
                            dueDate = dueDate,
                            dueTime = dueTime,
                            tagsCsv = tagsCsv,
                            subtasksRaw = subtasksRaw,
                            isRecurring = isRecurring,
                            recurrenceRule = recurrenceRule,
                            hasReminder = hasReminder,
                            assignedTo = assignedTo
                        )
                        repository.createTask(task)
                        _snackbarMessage.value = "Task created"
                    }
                }
            } else {
                val existing = _taskToEdit.value ?: return@launch
                val updated = existing.copy(
                    title = title.trim(),
                    description = description.trim(),
                    projectId = projectId,
                    priority = priority,
                    dueDate = dueDate,
                    dueTime = dueTime,
                    tagsCsv = tagsCsv,
                    subtasksRaw = subtasksRaw,
                    isRecurring = isRecurring,
                    recurrenceRule = recurrenceRule,
                    hasReminder = hasReminder,
                    assignedTo = assignedTo
                )
                repository.updateTask(updated)
                _snackbarMessage.value = "Task updated"
            }
            _showTaskEditor.value = false
            _taskToEdit.value = null
        }
    }

    fun openTaskEditor(task: TaskItem? = null) {
        _taskToEdit.value = task
        _showTaskEditor.value = true
    }

    fun closeTaskEditor() {
        _showTaskEditor.value = false
        _taskToEdit.value = null
    }

    fun toggleSubtask(task: TaskItem, subtaskId: String) {
        val currentSubtasks = task.getSubtasksList()
        val updated = currentSubtasks.map {
            if (it.id == subtaskId) it.copy(isDone = !it.isDone) else it
        }
        val raw = updated.joinToString("|") { "${it.id}::${it.title}::${it.isDone}" }
        viewModelScope.launch {
            repository.updateTask(task.copy(subtasksRaw = raw))
        }
    }

    fun duplicateTask(task: TaskItem) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            when (val check = repository.canCreateTask(user.id)) {
                is LimitCheckResult.Exceeded -> triggerUpgradePrompt(check.requiredTier, check.message)
                is LimitCheckResult.Allowed -> {
                    repository.duplicateTask(task)
                    _snackbarMessage.value = "Duplicated '${task.title}'"
                }
            }
        }
    }

    fun moveTask(task: TaskItem, targetProjectId: Long) {
        viewModelScope.launch {
            repository.updateTask(task.copy(projectId = targetProjectId))
            _snackbarMessage.value = "Moved task to another project"
        }
    }

    fun archiveTask(task: TaskItem) {
        viewModelScope.launch {
            repository.archiveTask(task)
            _snackbarMessage.value = "Moved to Archive"
        }
    }

    fun restoreArchivedTask(task: TaskItem, targetProjectId: Long? = null) {
        viewModelScope.launch {
            repository.restoreArchivedTask(task, targetProjectId)
            _snackbarMessage.value = "Restored task to active tasks"
        }
    }

    fun permanentlyDeleteTask(task: TaskItem) {
        viewModelScope.launch {
            repository.deleteTask(task)
            _snackbarMessage.value = "Task permanently deleted"
        }
    }

    // --- Bulk Selection & Actions in Archive ---
    fun toggleSelectArchiveTask(taskId: Long) {
        val user = _currentUser.value ?: return
        if (!user.tier.hasBulkActions) {
            triggerUpgradePrompt(SubscriptionTier.PRO, "Bulk actions are exclusive to Pro & Business plans.")
            return
        }
        val current = _selectedArchiveTaskIds.value.toMutableSet()
        if (current.contains(taskId)) current.remove(taskId) else current.add(taskId)
        _selectedArchiveTaskIds.value = current
    }

    fun selectAllArchiveTasks(taskIds: List<Long>) {
        val user = _currentUser.value ?: return
        if (!user.tier.hasBulkActions) {
            triggerUpgradePrompt(SubscriptionTier.PRO, "Bulk actions are exclusive to Pro & Business plans.")
            return
        }
        _selectedArchiveTaskIds.value = taskIds.toSet()
    }

    fun clearArchiveSelection() {
        _selectedArchiveTaskIds.value = emptySet()
    }

    fun bulkRestoreSelectedArchive(tasks: List<TaskItem>, targetProjectId: Long? = null) {
        val ids = _selectedArchiveTaskIds.value
        val toRestore = tasks.filter { ids.contains(it.id) }
        viewModelScope.launch {
            repository.bulkRestoreTasks(toRestore, targetProjectId)
            _selectedArchiveTaskIds.value = emptySet()
            _snackbarMessage.value = "Restored ${toRestore.size} tasks"
        }
    }

    fun bulkDeleteSelectedArchive(tasks: List<TaskItem>) {
        val ids = _selectedArchiveTaskIds.value
        val toDelete = tasks.filter { ids.contains(it.id) }
        viewModelScope.launch {
            repository.bulkDeleteTasks(toDelete)
            _selectedArchiveTaskIds.value = emptySet()
            _snackbarMessage.value = "Permanently deleted ${toDelete.size} tasks"
        }
    }

    fun bulkMoveTasks(tasks: List<TaskItem>, targetProjectId: Long) {
        val ids = _selectedArchiveTaskIds.value
        val toMove = tasks.filter { ids.contains(it.id) }
        viewModelScope.launch {
            repository.bulkMoveTasks(toMove, targetProjectId)
            _selectedArchiveTaskIds.value = emptySet()
            _snackbarMessage.value = "Moved ${toMove.size} tasks"
        }
    }

    // --- Projects Management ---
    fun openProjectEditor(project: Project? = null) {
        val user = _currentUser.value ?: return
        if (project == null) {
            viewModelScope.launch {
                when (val check = repository.canCreateProject(user.id)) {
                    is LimitCheckResult.Exceeded -> triggerUpgradePrompt(check.requiredTier, check.message)
                    is LimitCheckResult.Allowed -> {
                        _projectToEdit.value = null
                        _showProjectEditor.value = true
                    }
                }
            }
        } else {
            _projectToEdit.value = project
            _showProjectEditor.value = true
        }
    }

    fun closeProjectEditor() {
        _showProjectEditor.value = false
        _projectToEdit.value = null
    }

    fun saveProject(name: String, description: String, colorHex: String, iconName: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val existing = _projectToEdit.value
            if (existing == null) {
                val proj = repository.createProject(user.id, name, description, colorHex, iconName)
                _snackbarMessage.value = "Project '${proj.name}' created"
            } else {
                repository.updateProject(
                    existing.copy(
                        name = name.trim(),
                        description = description.trim(),
                        colorHex = colorHex,
                        iconName = iconName
                    )
                )
                _snackbarMessage.value = "Project updated"
            }
            _showProjectEditor.value = false
            _projectToEdit.value = null
        }
    }

    fun deleteProject(project: Project, deleteTasks: Boolean) {
        viewModelScope.launch {
            repository.deleteProject(project, deleteTasks)
            if (_selectedProjectId.value == project.id) {
                _currentDestination.value = AppDestination.DASHBOARD
                _selectedProjectId.value = null
            }
            _snackbarMessage.value = "Project '${project.name}' deleted"
        }
    }

    fun archiveProject(project: Project, archive: Boolean) {
        viewModelScope.launch {
            repository.setProjectArchived(project, archive)
            _snackbarMessage.value = if (archive) "Project archived" else "Project restored"
        }
    }

    // --- Subscription & Upgrade Prompt ---
    fun triggerUpgradePrompt(tier: SubscriptionTier, reason: String) {
        _upgradeModalTier.value = tier
        _upgradeReason.value = reason
    }

    fun dismissUpgradePrompt() {
        _upgradeModalTier.value = null
        _upgradeReason.value = null
    }

    fun upgradePlan(tier: SubscriptionTier, billingCycle: BillingCycle, cardLast4: String = "4242") {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val invoice = repository.upgradeSubscription(user.id, tier, billingCycle, cardLast4)
            _upgradeModalTier.value = null
            _upgradeReason.value = null
            _snackbarMessage.value = "Successfully upgraded to ${tier.title}! Invoice #${invoice.invoiceNumber} paid."
        }
    }

    fun cancelSubscription() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.cancelSubscription(user.id)
            _snackbarMessage.value = "Switched to Starter plan"
        }
    }

    // --- Team Workspace (Business Tier) ---
    fun inviteTeamMember(name: String, email: String, role: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            when (val check = repository.canAddTeamMember(user.id)) {
                is LimitCheckResult.Exceeded -> triggerUpgradePrompt(check.requiredTier, check.message)
                is LimitCheckResult.Allowed -> {
                    repository.addTeamMember(user.id, name, email, role)
                    _snackbarMessage.value = "Invitation sent to $email"
                }
            }
        }
    }

    fun removeTeamMember(member: TeamMember) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.removeTeamMember(user.id, member.id, member.name)
            _snackbarMessage.value = "Removed ${member.name} from workspace"
        }
    }

    // --- CSV Export (Pro/Business) ---
    fun exportCsv(onExportReady: (String) -> Unit) {
        val user = _currentUser.value ?: return
        if (!user.tier.hasCsvExport) {
            triggerUpgradePrompt(SubscriptionTier.PRO, "CSV data export is a Pro & Business feature.")
            return
        }
        viewModelScope.launch {
            val csv = repository.exportTasksToCsv(user.id, activeTasks.value + archiveTasks.value, activeProjects.value)
            onExportReady(csv)
            _snackbarMessage.value = "TaskFlow CSV exported successfully"
        }
    }

    // --- User Auth & Accounts ---
    fun setAuthMode(mode: AuthMode) { _authMode.value = mode }

    fun login(email: String, pass: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            val user = repository.getUserByEmail(email.trim().lowercase())
            if (user == null || user.passwordHash != pass) {
                onError("Invalid email or password.")
            } else {
                _currentUserId.value = user.id
                _currentUser.value = user
                _authMode.value = AuthMode.LOGGED_IN
                _snackbarMessage.value = "Welcome back, ${user.name}!"
            }
        }
    }

    fun register(email: String, pass: String, name: String, plan: SubscriptionTier, onError: (String) -> Unit) {
        viewModelScope.launch {
            val existing = repository.getUserByEmail(email.trim().lowercase())
            if (existing != null) {
                onError("An account with this email already exists.")
                return@launch
            }
            val user = repository.registerUser(email, pass, name, plan)
            _currentUserId.value = user.id
            _currentUser.value = user
            _authMode.value = AuthMode.LOGGED_IN
            _snackbarMessage.value = "Account created! Welcome to TaskFlow."
        }
    }

    fun resetPassword(email: String, newPass: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.resetPassword(email, newPass)
            onComplete(success)
            if (success) {
                _authMode.value = AuthMode.LOGIN
                _snackbarMessage.value = "Password reset! You can now log in."
            }
        }
    }

    fun logout() {
        _authMode.value = AuthMode.LOGIN
    }

    fun updateProfile(name: String, teamName: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updated = user.copy(name = name.trim(), teamWorkspaceName = teamName.trim())
            repository.updateUser(updated)
            _snackbarMessage.value = "Profile updated"
        }
    }
}
