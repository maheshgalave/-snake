package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SubscriptionTier(
    val title: String,
    val monthlyPrice: Double,
    val annualMonthlyPrice: Double,
    val maxProjects: Int, // -1 means unlimited
    val maxActiveTasks: Int, // -1 means unlimited
    val hasAdvancedFilters: Boolean,
    val hasTags: Boolean,
    val hasSubtasks: Boolean,
    val hasRecurringTasks: Boolean,
    val hasCalendarView: Boolean,
    val hasBulkActions: Boolean,
    val hasCsvExport: Boolean,
    val hasTeamWorkspaces: Boolean,
    val maxTeamMembers: Int
) {
    STARTER(
        title = "Starter",
        monthlyPrice = 10.0,
        annualMonthlyPrice = 8.0, // 20% discount on annual
        maxProjects = 5,
        maxActiveTasks = 250,
        hasAdvancedFilters = false,
        hasTags = false,
        hasSubtasks = false,
        hasRecurringTasks = false,
        hasCalendarView = false,
        hasBulkActions = false,
        hasCsvExport = false,
        hasTeamWorkspaces = false,
        maxTeamMembers = 1
    ),
    PRO(
        title = "Pro",
        monthlyPrice = 20.0,
        annualMonthlyPrice = 16.0,
        maxProjects = -1,
        maxActiveTasks = -1,
        hasAdvancedFilters = true,
        hasTags = true,
        hasSubtasks = true,
        hasRecurringTasks = true,
        hasCalendarView = true,
        hasBulkActions = true,
        hasCsvExport = true,
        hasTeamWorkspaces = false,
        maxTeamMembers = 1
    ),
    BUSINESS(
        title = "Business",
        monthlyPrice = 80.0,
        annualMonthlyPrice = 64.0,
        maxProjects = -1,
        maxActiveTasks = -1,
        hasAdvancedFilters = true,
        hasTags = true,
        hasSubtasks = true,
        hasRecurringTasks = true,
        hasCalendarView = true,
        hasBulkActions = true,
        hasCsvExport = true,
        hasTeamWorkspaces = true,
        maxTeamMembers = 10
    )
}

enum class BillingCycle {
    MONTHLY,
    ANNUAL
}

enum class TaskPriority(val label: String, val level: Int) {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3),
    URGENT("Urgent", 4)
}

@Entity(tableName = "users")
data class UserAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val passwordHash: String,
    val name: String,
    val avatarColorHex: String = "#4F46E5",
    val tier: SubscriptionTier = SubscriptionTier.STARTER,
    val billingCycle: BillingCycle = BillingCycle.MONTHLY,
    val teamWorkspaceName: String = "Personal Workspace",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val name: String,
    val description: String = "",
    val colorHex: String = "#4F46E5",
    val iconName: String = "folder",
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class Subtask(
    val id: String,
    val title: String,
    val isDone: Boolean = false
)

@Entity(tableName = "tasks")
data class TaskItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val projectId: Long,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val isArchived: Boolean = false,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueDate: Long? = null,
    val dueTime: String? = null, // e.g. "14:00"
    val tagsCsv: String = "", // comma-separated tags
    val subtasksRaw: String = "", // serialized subtasks "id::title::isDone|id::title::isDone"
    val isRecurring: Boolean = false,
    val recurrenceRule: String? = null, // "DAILY", "WEEKLY", "MONTHLY"
    val hasReminder: Boolean = false,
    val sortOrder: Int = 0,
    val assignedTo: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getSubtasksList(): List<Subtask> {
        if (subtasksRaw.isBlank()) return emptyList()
        return subtasksRaw.split("|").mapNotNull { part ->
            val tokens = part.split("::")
            if (tokens.size >= 3) {
                Subtask(tokens[0], tokens[1], tokens[2] == "true")
            } else null
        }
    }

    fun getTagsList(): List<String> {
        if (tagsCsv.isBlank()) return emptyList()
        return tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val action: String, // e.g. "Completed", "Restored", "Created", "Upgraded"
    val taskTitle: String,
    val projectName: String = "",
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "team_members")
data class TeamMember(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val name: String,
    val email: String,
    val role: String = "Member", // "Owner", "Admin", "Member"
    val avatarColorHex: String = "#0EA5E9"
)

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val invoiceNumber: String,
    val planName: String,
    val billingCycle: String,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val status: String = "Paid",
    val paymentMethodEnding: String = "4242"
)
