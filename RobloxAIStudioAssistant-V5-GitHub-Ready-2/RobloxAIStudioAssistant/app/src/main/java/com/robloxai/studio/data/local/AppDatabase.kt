package com.robloxai.studio.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val genre: String,
    val instructions: String,
    val progressNotes: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "scripts",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class ScriptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val scriptType: String, // SCRIPT, LOCAL_SCRIPT, MODULE_SCRIPT
    val studioLocation: String,
    val code: String,
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val role: String,
    val content: String,
    val providerName: String,
    val modelId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "concepts_3d",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class Concept3DEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val category: String,
    val generatorPrompt: String,
    val conceptDetails: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface StudioDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Insert
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("SELECT * FROM scripts WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun observeScripts(projectId: Long): Flow<List<ScriptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScript(script: ScriptEntity): Long

    @Delete
    suspend fun deleteScript(script: ScriptEntity)

    @Query("SELECT * FROM chat_messages WHERE projectId = :projectId ORDER BY createdAt ASC")
    fun observeChat(projectId: Long): Flow<List<ChatMessageEntity>>

    @Insert
    suspend fun insertChat(msg: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE projectId = :projectId")
    suspend fun clearChat(projectId: Long)

    @Query("SELECT * FROM concepts_3d WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun observeConcepts3D(projectId: Long): Flow<List<Concept3DEntity>>

    @Insert
    suspend fun insertConcept3D(concept: Concept3DEntity): Long

    @Delete
    suspend fun deleteConcept3D(concept: Concept3DEntity)
}

@Database(
    entities = [ProjectEntity::class, ScriptEntity::class, ChatMessageEntity::class, Concept3DEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studioDao(): StudioDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "roblox_ai_studio.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
