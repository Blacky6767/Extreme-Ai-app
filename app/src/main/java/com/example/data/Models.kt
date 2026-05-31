package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val model: String,
    val emotion: String,
    val language: String,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val role: String, // "user" or "model" or "system"
    val content: String,
    val emotion: String, // Emotion tags (e.g., "Analytical", "Empathetic", "Encouraging")
    val language: String,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "code_projects")
data class CodeProject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val code: String,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "dynamic_apps")
data class DynamicApp(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val widgetsConfigJson: String, // Stores serialized config containing dynamic controls
    val creationTimestamp: Long = System.currentTimeMillis()
) : Serializable
