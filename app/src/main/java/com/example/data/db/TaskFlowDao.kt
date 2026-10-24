package com.example.data.db

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskFlowDao {

    // --- Users ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccount): Long

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserAccount?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserAccount?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun observeUserById(id: Long): Flow<UserAccount?>

    @Update
    suspend fun updateUser(user: UserAccount)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    // --- Projects ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long

    @Update
    suspend fun updateProject(project: Project)

    @Delete
    suspend fun deleteProject(project: Project)

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Long): Project?

    @Query("SELECT * FROM projects WHERE userId = :userId AND isArchived = 0 ORDER BY sortOrder ASC, createdAt DESC")
    fun getActiveProjects(userId: Long): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE userId = :userId AND isArchived = 1 ORDER BY createdAt DESC")
    fun getArchivedProjects(userId: Long): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE userId = :userId ORDER BY sortOrder ASC, createdAt DESC")
    fun getAllProjects(userId: Long): Flow<List<Project>>

    @Query("SELECT COUNT(*) FROM projects WHERE userId = :userId AND isArchived = 0")
    suspend fun countActiveProjects(userId: Long): Int

    // --- Tasks ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskItem): Long

    @Update
    suspend fun updateTask(task: TaskItem)

    @Delete
    suspend fun deleteTask(task: TaskItem)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("DELETE FROM tasks WHERE projectId = :projectId")
    suspend fun deleteTasksByProjectId(projectId: Long)

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): TaskItem?

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isCompleted = 0 AND isArchived = 0 ORDER BY sortOrder ASC, dueDate ASC, priority DESC")
    fun getActiveTasks(userId: Long): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND projectId = :projectId AND isCompleted = 0 AND isArchived = 0 ORDER BY sortOrder ASC, dueDate ASC, priority DESC")
    fun getActiveTasksForProject(userId: Long, projectId: Long): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND (isCompleted = 1 OR isArchived = 1) ORDER BY completedAt DESC, updatedAt DESC")
    fun getArchiveTasks(userId: Long): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllTasks(userId: Long): Flow<List<TaskItem>>

    @Query("SELECT COUNT(*) FROM tasks WHERE userId = :userId AND isCompleted = 0 AND isArchived = 0")
    suspend fun countActiveTasks(userId: Long): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE userId = :userId AND isCompleted = 1")
    suspend fun countCompletedTasks(userId: Long): Int

    // --- Activity Logs ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLog)

    @Query("SELECT * FROM activity_logs WHERE userId = :userId ORDER BY timestamp DESC LIMIT 100")
    fun getActivityLogs(userId: Long): Flow<List<ActivityLog>>

    // --- Team Members ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeamMember(member: TeamMember): Long

    @Query("SELECT * FROM team_members WHERE userId = :userId")
    fun getTeamMembers(userId: Long): Flow<List<TeamMember>>

    @Query("DELETE FROM team_members WHERE id = :id")
    suspend fun deleteTeamMember(id: Long)

    @Query("SELECT COUNT(*) FROM team_members WHERE userId = :userId")
    suspend fun countTeamMembers(userId: Long): Int

    // --- Invoices ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice): Long

    @Query("SELECT * FROM invoices WHERE userId = :userId ORDER BY date DESC")
    fun getInvoices(userId: Long): Flow<List<Invoice>>
}
