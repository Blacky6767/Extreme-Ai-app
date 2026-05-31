package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions ORDER BY timestamp DESC")
    fun getSessions(): Flow<List<ChatSession>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessages(sessionId: Long): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Delete
    suspend fun deleteSession(session: ChatSession)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: Long)
}

@Dao
interface CodeProjectDao {
    @Query("SELECT * FROM code_projects ORDER BY timestamp DESC")
    fun getProjects(): Flow<List<CodeProject>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: CodeProject): Long

    @Delete
    suspend fun deleteProject(project: CodeProject)
}

@Dao
interface DynamicAppDao {
    @Query("SELECT * FROM dynamic_apps ORDER BY creationTimestamp DESC")
    fun getApps(): Flow<List<DynamicApp>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: DynamicApp): Long

    @Delete
    suspend fun deleteApp(app: DynamicApp)
}
