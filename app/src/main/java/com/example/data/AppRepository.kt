package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val db: AppDatabase) {

    private val chatDao = db.chatDao()
    private val codeProjectDao = db.codeProjectDao()
    private val dynamicAppDao = db.dynamicAppDao()

    // --- Chat ---
    val chatSessions: Flow<List<ChatSession>> = chatDao.getSessions()

    fun getChatMessages(sessionId: Long): Flow<List<ChatMessage>> {
        return chatDao.getMessages(sessionId)
    }

    suspend fun createChatSession(title: String, model: String, emotion: String, language: String): Long {
        return chatDao.insertSession(ChatSession(title = title, model = model, emotion = emotion, language = language))
    }

    suspend fun saveChatMessage(sessionId: Long, role: String, content: String, emotion: String, language: String): Long {
        return chatDao.insertMessage(ChatMessage(sessionId = sessionId, role = role, content = content, emotion = emotion, language = language))
    }

    suspend fun deleteChatSession(session: ChatSession) {
        chatDao.deleteMessagesForSession(session.id)
        chatDao.deleteSession(session)
    }

    // --- Coding ---
    val allProjects: Flow<List<CodeProject>> = codeProjectDao.getProjects()

    suspend fun saveProject(title: String, description: String, code: String): Long {
        return codeProjectDao.insertProject(CodeProject(title = title, description = description, code = code))
    }

    suspend fun deleteProject(project: CodeProject) {
        codeProjectDao.deleteProject(project)
    }

    // --- Dynamic Mini-Apps ---
    val allDynamicApps: Flow<List<DynamicApp>> = dynamicAppDao.getApps()

    suspend fun saveDynamicApp(title: String, description: String, widgetsConfigJson: String): Long {
        return dynamicAppDao.insertApp(DynamicApp(title = title, description = description, widgetsConfigJson = widgetsConfigJson))
    }

    suspend fun deleteDynamicApp(app: DynamicApp) {
        dynamicAppDao.deleteApp(app)
    }
}
