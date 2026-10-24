package com.example.data.repository

import com.example.data.db.TaskFlowDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed class LimitCheckResult {
    object Allowed : LimitCheckResult()
    data class Exceeded(val message: String, val requiredTier: SubscriptionTier) : LimitCheckResult()
}

class TaskFlowRepository(private val dao: TaskFlowDao) {

    // --- User & Auth ---
    suspend fun getUserByEmail(email: String): UserAccount? = dao.getUserByEmail(email)
    suspend fun getUserById(id: Long): UserAccount? = dao.getUserById(id)
    fun observeUser(id: Long): Flow<UserAccount?> = dao.observeUserById(id)
    suspend fun updateUser(user: UserAccount) = dao.updateUser(user)

    suspend fun registerUser(
        email: String,
        password: String,
        name: String,
        tier: SubscriptionTier = SubscriptionTier.STARTER,
        billingCycle: BillingCycle = BillingCycle.MONTHLY
    ): UserAccount {
        val user = UserAccount(
            email = email.trim().lowercase(),
            passwordHash = password, // In production app this would use PBKDF2/BCrypt
            name = name.trim(),
            avatarColorHex = getRandomAvatarColor(),
            tier = tier,
            billingCycle = billingCycle,
            teamWorkspaceName = "${name.trim().take(12)} Workspace"
        )
        val userId = dao.insertUser(user)
        val createdUser = user.copy(id = userId)

        // Seed default projects: Personal, Work, Ideas
        seedDefaultProjectsAndTasks(createdUser)

        // Seed initial invoice
        val amount = if (billingCycle == BillingCycle.MONTHLY) tier.monthlyPrice else tier.annualMonthlyPrice * 12
        dao.insertInvoice(
            Invoice(
                userId = userId,
                invoiceNumber = "INV-" + UUID.randomUUID().toString().take(8).uppercase(),
                planName = tier.title,
                billingCycle = billingCycle.name,
                amount = amount,
                status = "Paid"
            )
        )

        dao.insertActivityLog(
            ActivityLog(
                userId = userId,
                action = "Account Created",
                taskTitle = "",
                projectName = "",
                details = "Welcome to TaskFlow! Started on ${tier.title} plan."
            )
        )

        return createdUser
    }

    suspend fun resetPassword(email: String, newPass: String): Boolean {
        val user = dao.getUserByEmail(email.trim().lowercase()) ?: return false
        dao.updateUser(user.copy(passwordHash = newPass))
        dao.insertActivityLog(
            ActivityLog(
                userId = user.id,
                action = "Password Reset",
                taskTitle = "",
                projectName = "",
                details = "Password was successfully reset."
            )
        )
        return true
    }

    // --- Projects ---
    fun getActiveProjects(userId: Long): Flow<List<Project>> = dao.getActiveProjects(userId)
    fun getArchivedProjects(userId: Long): Flow<List<Project>> = dao.getArchivedProjects(userId)
    fun getAllProjects(userId: Long): Flow<List<Project>> = dao.getAllProjects(userId)

    suspend fun canCreateProject(userId: Long): LimitCheckResult {
        val user = dao.getUserById(userId) ?: return LimitCheckResult.Exceeded("User not found", SubscriptionTier.STARTER)
        if (user.tier.maxProjects == -1) return LimitCheckResult.Allowed
        val count = dao.countActiveProjects(userId)
        return if (count < user.tier.maxProjects) {
            LimitCheckResult.Allowed
        } else {
            LimitCheckResult.Exceeded(
                "You have reached the limit of ${user.tier.maxProjects} active projects on the ${user.tier.title} plan. Upgrade to Pro for unlimited projects.",
                SubscriptionTier.PRO
            )
        }
    }

    suspend fun createProject(
        userId: Long,
        name: String,
        description: String,
        colorHex: String,
        iconName: String
    ): Project {
        val project = Project(
            userId = userId,
            name = name.trim(),
            description = description.trim(),
            colorHex = colorHex,
            iconName = iconName
        )
        val id = dao.insertProject(project)
        dao.insertActivityLog(
            ActivityLog(
                userId = userId,
                action = "Project Created",
                taskTitle = "",
                projectName = name.trim(),
                details = "Created project '$name'"
            )
        )
        return project.copy(id = id)
    }

    suspend fun updateProject(project: Project) {
        dao.updateProject(project)
    }

    suspend fun deleteProject(project: Project, deleteTasks: Boolean) {
        if (deleteTasks) {
            dao.deleteTasksByProjectId(project.id)
        }
        dao.deleteProject(project)
        dao.insertActivityLog(
            ActivityLog(
                userId = project.userId,
                action = "Project Deleted",
                taskTitle = "",
                projectName = project.name,
                details = "Project '${project.name}' deleted"
            )
        )
    }

    suspend fun setProjectArchived(project: Project, archive: Boolean) {
        dao.updateProject(project.copy(isArchived = archive))
        dao.insertActivityLog(
            ActivityLog(
                userId = project.userId,
                action = if (archive) "Project Archived" else "Project Restored",
                taskTitle = "",
                projectName = project.name,
                details = if (archive) "Archived project '${project.name}'" else "Restored project '${project.name}'"
            )
        )
    }

    // --- Tasks ---
    fun getActiveTasks(userId: Long): Flow<List<TaskItem>> = dao.getActiveTasks(userId)
    fun getActiveTasksForProject(userId: Long, projectId: Long): Flow<List<TaskItem>> =
        dao.getActiveTasksForProject(userId, projectId)
    fun getArchiveTasks(userId: Long): Flow<List<TaskItem>> = dao.getArchiveTasks(userId)
    fun getAllTasks(userId: Long): Flow<List<TaskItem>> = dao.getAllTasks(userId)

    suspend fun canCreateTask(userId: Long): LimitCheckResult {
        val user = dao.getUserById(userId) ?: return LimitCheckResult.Exceeded("User not found", SubscriptionTier.STARTER)
        if (user.tier.maxActiveTasks == -1) return LimitCheckResult.Allowed
        val count = dao.countActiveTasks(userId)
        return if (count < user.tier.maxActiveTasks) {
            LimitCheckResult.Allowed
        } else {
            LimitCheckResult.Exceeded(
                "You have reached ${user.tier.maxActiveTasks} active tasks on the ${user.tier.title} plan. Upgrade to Pro for unlimited tasks.",
                SubscriptionTier.PRO
            )
        }
    }

    suspend fun createTask(task: TaskItem): Long {
        val id = dao.insertTask(task)
        val project = dao.getProjectById(task.projectId)
        dao.insertActivityLog(
            ActivityLog(
                userId = task.userId,
                action = "Task Created",
                taskTitle = task.title,
                projectName = project?.name ?: "",
                details = "Created task in ${project?.name ?: "project"}"
            )
        )
        return id
    }

    suspend fun updateTask(task: TaskItem) {
        dao.updateTask(task.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteTask(task: TaskItem) {
        dao.deleteTask(task)
        dao.insertActivityLog(
            ActivityLog(
                userId = task.userId,
                action = "Task Permanently Deleted",
                taskTitle = task.title,
                projectName = "",
                details = "Removed from archive"
            )
        )
    }

    suspend fun completeTask(task: TaskItem): TaskItem {
        val completedTask = task.copy(
            isCompleted = true,
            completedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        dao.updateTask(completedTask)
        val project = dao.getProjectById(task.projectId)
        dao.insertActivityLog(
            ActivityLog(
                userId = task.userId,
                action = "Completed",
                taskTitle = task.title,
                projectName = project?.name ?: "",
                details = "One-click completion"
            )
        )

        // Check if recurring task should schedule next occurrence
        if (task.isRecurring && task.dueDate != null) {
            scheduleNextOccurrence(task)
        }

        return completedTask
    }

    suspend fun undoCompleteTask(task: TaskItem) {
        val restoredTask = task.copy(
            isCompleted = false,
            completedAt = null,
            updatedAt = System.currentTimeMillis()
        )
        dao.updateTask(restoredTask)
        val project = dao.getProjectById(task.projectId)
        dao.insertActivityLog(
            ActivityLog(
                userId = task.userId,
                action = "Restored",
                taskTitle = task.title,
                projectName = project?.name ?: "",
                details = "Undid completion"
            )
        )
    }

    suspend fun archiveTask(task: TaskItem) {
        val archivedTask = task.copy(
            isArchived = true,
            updatedAt = System.currentTimeMillis()
        )
        dao.updateTask(archivedTask)
        val project = dao.getProjectById(task.projectId)
        dao.insertActivityLog(
            ActivityLog(
                userId = task.userId,
                action = "Archived",
                taskTitle = task.title,
                projectName = project?.name ?: "",
                details = "Manually archived"
            )
        )
    }

    suspend fun restoreArchivedTask(task: TaskItem, newProjectId: Long? = null) {
        val restoredTask = task.copy(
            isCompleted = false,
            isArchived = false,
            completedAt = null,
            projectId = newProjectId ?: task.projectId,
            updatedAt = System.currentTimeMillis()
        )
        dao.updateTask(restoredTask)
        val project = dao.getProjectById(restoredTask.projectId)
        dao.insertActivityLog(
            ActivityLog(
                userId = task.userId,
                action = "Restored to Active",
                taskTitle = task.title,
                projectName = project?.name ?: "",
                details = "Restored from archive"
            )
        )
    }

    suspend fun duplicateTask(task: TaskItem): Long {
        val copy = task.copy(
            id = 0,
            title = "${task.title} (Copy)",
            isCompleted = false,
            isArchived = false,
            completedAt = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val newId = dao.insertTask(copy)
        val project = dao.getProjectById(task.projectId)
        dao.insertActivityLog(
            ActivityLog(
                userId = task.userId,
                action = "Duplicated",
                taskTitle = copy.title,
                projectName = project?.name ?: "",
                details = "Duplicated from task #${task.id}"
            )
        )
        return newId
    }

    private suspend fun scheduleNextOccurrence(task: TaskItem) {
        val currentDue = task.dueDate ?: return
        val oneDayMs = 24 * 60 * 60 * 1000L
        val nextDue = when (task.recurrenceRule) {
            "DAILY" -> currentDue + oneDayMs
            "WEEKLY" -> currentDue + 7 * oneDayMs
            "MONTHLY" -> currentDue + 30 * oneDayMs
            else -> currentDue + 7 * oneDayMs
        }
        val nextTask = task.copy(
            id = 0,
            isCompleted = false,
            isArchived = false,
            dueDate = nextDue,
            completedAt = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        dao.insertTask(nextTask)
    }

    // --- Bulk Actions ---
    suspend fun bulkCompleteTasks(tasks: List<TaskItem>) {
        val now = System.currentTimeMillis()
        tasks.forEach { task ->
            dao.updateTask(task.copy(isCompleted = true, completedAt = now, updatedAt = now))
        }
    }

    suspend fun bulkRestoreTasks(tasks: List<TaskItem>, targetProjectId: Long? = null) {
        val now = System.currentTimeMillis()
        tasks.forEach { task ->
            dao.updateTask(
                task.copy(
                    isCompleted = false,
                    isArchived = false,
                    completedAt = null,
                    projectId = targetProjectId ?: task.projectId,
                    updatedAt = now
                )
            )
        }
    }

    suspend fun bulkDeleteTasks(tasks: List<TaskItem>) {
        tasks.forEach { dao.deleteTask(it) }
    }

    suspend fun bulkMoveTasks(tasks: List<TaskItem>, targetProjectId: Long) {
        val now = System.currentTimeMillis()
        tasks.forEach { task ->
            dao.updateTask(task.copy(projectId = targetProjectId, updatedAt = now))
        }
    }

    // --- Subscriptions & Billing ---
    suspend fun upgradeSubscription(
        userId: Long,
        newTier: SubscriptionTier,
        billingCycle: BillingCycle,
        paymentCardLast4: String = "4242"
    ): Invoice {
        val user = dao.getUserById(userId) ?: error("User not found")
        val updatedUser = user.copy(tier = newTier, billingCycle = billingCycle)
        dao.updateUser(updatedUser)

        val amount = if (billingCycle == BillingCycle.MONTHLY) newTier.monthlyPrice else newTier.annualMonthlyPrice * 12
        val invoice = Invoice(
            userId = userId,
            invoiceNumber = "INV-" + UUID.randomUUID().toString().take(8).uppercase(),
            planName = newTier.title,
            billingCycle = billingCycle.name,
            amount = amount,
            status = "Paid",
            paymentMethodEnding = paymentCardLast4
        )
        val invoiceId = dao.insertInvoice(invoice)

        dao.insertActivityLog(
            ActivityLog(
                userId = userId,
                action = "Plan Upgraded",
                taskTitle = "",
                projectName = "",
                details = "Subscribed to ${newTier.title} plan (${billingCycle.name.lowercase()})"
            )
        )

        return invoice.copy(id = invoiceId)
    }

    suspend fun cancelSubscription(userId: Long) {
        val user = dao.getUserById(userId) ?: return
        dao.updateUser(user.copy(tier = SubscriptionTier.STARTER))
        dao.insertActivityLog(
            ActivityLog(
                userId = userId,
                action = "Subscription Modified",
                taskTitle = "",
                projectName = "",
                details = "Plan switched back to Starter"
            )
        )
    }

    // --- Team Members ---
    fun getTeamMembers(userId: Long): Flow<List<TeamMember>> = dao.getTeamMembers(userId)

    suspend fun canAddTeamMember(userId: Long): LimitCheckResult {
        val user = dao.getUserById(userId) ?: return LimitCheckResult.Exceeded("User not found", SubscriptionTier.STARTER)
        if (!user.tier.hasTeamWorkspaces) {
            return LimitCheckResult.Exceeded("Team workspaces are exclusively available on the Business plan ($80/mo).", SubscriptionTier.BUSINESS)
        }
        val count = dao.countTeamMembers(userId)
        return if (count < user.tier.maxTeamMembers) {
            LimitCheckResult.Allowed
        } else {
            LimitCheckResult.Exceeded("Business plan includes up to ${user.tier.maxTeamMembers} team members.", SubscriptionTier.BUSINESS)
        }
    }

    suspend fun addTeamMember(userId: Long, name: String, email: String, role: String): TeamMember {
        val member = TeamMember(
            userId = userId,
            name = name.trim(),
            email = email.trim(),
            role = role,
            avatarColorHex = getRandomAvatarColor()
        )
        val id = dao.insertTeamMember(member)
        dao.insertActivityLog(
            ActivityLog(
                userId = userId,
                action = "Member Invited",
                taskTitle = "",
                projectName = "",
                details = "Invited $name ($email) as $role"
            )
        )
        return member.copy(id = id)
    }

    suspend fun removeTeamMember(userId: Long, memberId: Long, memberName: String) {
        dao.deleteTeamMember(memberId)
        dao.insertActivityLog(
            ActivityLog(
                userId = userId,
                action = "Member Removed",
                taskTitle = "",
                projectName = "",
                details = "Removed $memberName from workspace"
            )
        )
    }

    // --- Activity Logs & Invoices ---
    fun getActivityLogs(userId: Long): Flow<List<ActivityLog>> = dao.getActivityLogs(userId)
    fun getInvoices(userId: Long): Flow<List<Invoice>> = dao.getInvoices(userId)

    // --- CSV Export (Pro/Business Feature) ---
    suspend fun exportTasksToCsv(userId: Long, tasks: List<TaskItem>, projects: List<Project>): String {
        val projectMap = projects.associateBy { it.id }
        val sb = StringBuilder()
        sb.append("Task ID,Title,Project,Priority,Status,Due Date,Tags,Created At,Completed At\n")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        tasks.forEach { task ->
            val pName = projectMap[task.projectId]?.name?.replace("\"", "\"\"") ?: "No Project"
            val title = task.title.replace("\"", "\"\"")
            val status = if (task.isCompleted) "Completed" else if (task.isArchived) "Archived" else "Active"
            val due = task.dueDate?.let { dateFormat.format(Date(it)) } ?: ""
            val created = dateFormat.format(Date(task.createdAt))
            val completed = task.completedAt?.let { dateFormat.format(Date(it)) } ?: ""
            sb.append("\"${task.id}\",\"$title\",\"$pName\",\"${task.priority.name}\",\"$status\",\"$due\",\"${task.tagsCsv}\",\"$created\",\"$completed\"\n")
        }
        return sb.toString()
    }

    // --- Helpers & Seeding ---
    suspend fun ensureDefaultUserExists(): UserAccount {
        val count = dao.getUserCount()
        if (count == 0) {
            return registerUser(
                email = "alex.founder@taskflow.io",
                password = "password123",
                name = "Alex Vance",
                tier = SubscriptionTier.PRO,
                billingCycle = BillingCycle.MONTHLY
            )
        }
        return dao.getUserByEmail("alex.founder@taskflow.io")
            ?: dao.getUserById(1)
            ?: registerUser("alex.founder@taskflow.io", "password123", "Alex Vance", SubscriptionTier.PRO)
    }

    private suspend fun seedDefaultProjectsAndTasks(user: UserAccount) {
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L

        // 1. Work Project
        val workProjectId = dao.insertProject(
            Project(
                userId = user.id,
                name = "Work & Product",
                description = "Sprint priorities, product launch items, and team deliverables.",
                colorHex = "#4F46E5",
                iconName = "work",
                sortOrder = 1
            )
        )

        // 2. Personal Project
        val personalProjectId = dao.insertProject(
            Project(
                userId = user.id,
                name = "Personal",
                description = "Daily habits, health, finances, and personal errands.",
                colorHex = "#10B981",
                iconName = "person",
                sortOrder = 2
            )
        )

        // 3. Ideas & Roadmap Project
        val ideasProjectId = dao.insertProject(
            Project(
                userId = user.id,
                name = "Ideas & Roadmap",
                description = "Feature suggestions, design brainstorms, and quarterly goals.",
                colorHex = "#F59E0B",
                iconName = "lightbulb",
                sortOrder = 3
            )
        )

        // Seed Tasks in Work
        dao.insertTask(
            TaskItem(
                userId = user.id,
                projectId = workProjectId,
                title = "Review Q4 SaaS roadmap with leadership team",
                description = "Prepare metrics deck, churn forecast, and feature delivery timeline.",
                isCompleted = false,
                priority = TaskPriority.URGENT,
                dueDate = now + dayMs / 2, // due today
                tagsCsv = "Strategy,Leadership,Q4",
                subtasksRaw = "1::Update MRR numbers::true|2::Draft pricing slide::true|3::Email deck link::false",
                hasReminder = true,
                sortOrder = 1
            )
        )
        dao.insertTask(
            TaskItem(
                userId = user.id,
                projectId = workProjectId,
                title = "Refine pricing tier limits & feature comparison matrix",
                description = "Ensure Starter, Pro, and Business plan perks match enterprise customer demand.",
                isCompleted = false,
                priority = TaskPriority.HIGH,
                dueDate = now + dayMs * 2,
                tagsCsv = "Pricing,Product",
                subtasksRaw = "1::Check team workspace limits::false|2::Verify CSV export access::false",
                sortOrder = 2
            )
        )
        dao.insertTask(
            TaskItem(
                userId = user.id,
                projectId = workProjectId,
                title = "Deploy task completion undo animation polish",
                description = "Provide 1-click completion feedback and snackbar reversal.",
                isCompleted = true,
                completedAt = now - 2 * 60 * 60 * 1000L,
                priority = TaskPriority.MEDIUM,
                dueDate = now - dayMs,
                tagsCsv = "UX,Engineering",
                sortOrder = 3
            )
        )

        // Seed Tasks in Personal
        dao.insertTask(
            TaskItem(
                userId = user.id,
                projectId = personalProjectId,
                title = "Morning 30-minute cardio & stretch workout",
                description = "Consistent aerobic exercise routine.",
                isCompleted = true,
                completedAt = now - 6 * 60 * 60 * 1000L,
                priority = TaskPriority.MEDIUM,
                dueDate = now,
                tagsCsv = "Fitness,Health",
                isRecurring = true,
                recurrenceRule = "DAILY",
                sortOrder = 1
            )
        )
        dao.insertTask(
            TaskItem(
                userId = user.id,
                projectId = personalProjectId,
                title = "Schedule yearly dental checkup and clean",
                description = "Call Dr. Peterson's clinic for next Tuesday slot.",
                isCompleted = false,
                priority = TaskPriority.LOW,
                dueDate = now + dayMs * 5,
                tagsCsv = "Health",
                sortOrder = 2
            )
        )

        // Seed Tasks in Ideas
        dao.insertTask(
            TaskItem(
                userId = user.id,
                projectId = ideasProjectId,
                title = "Explore AI auto-categorization for inbox tasks",
                description = "Assess zero-latency local classification based on task titles and due dates.",
                isCompleted = false,
                priority = TaskPriority.MEDIUM,
                dueDate = now + dayMs * 10,
                tagsCsv = "AI,Innovation",
                sortOrder = 1
            )
        )

        // Seed team members for Business preview
        dao.insertTeamMember(
            TeamMember(
                userId = user.id,
                name = "Elena Rostova",
                email = "elena.r@taskflow.io",
                role = "Admin",
                avatarColorHex = "#8B5CF6"
            )
        )
        dao.insertTeamMember(
            TeamMember(
                userId = user.id,
                name = "David Chen",
                email = "david.c@taskflow.io",
                role = "Member",
                avatarColorHex = "#0EA5E9"
            )
        )
    }

    private fun getRandomAvatarColor(): String {
        val colors = listOf("#4F46E5", "#0EA5E9", "#10B981", "#8B5CF6", "#F59E0B", "#EC4899")
        return colors.random()
    }
}
