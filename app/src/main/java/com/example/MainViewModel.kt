package com.example

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.network.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val db = AppDatabase.getDatabase(application)
    private val repository = AppRepository(db)

    // --- Active Flow States ---
    val chatSessions = repository.chatSessions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allProjects = repository.allProjects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allDynamicApps = repository.allDynamicApps.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Navigation
    var activeTab = MutableStateFlow("dashboard") // dashboard, chat, workspace, apps, settings

    // Target States
    val selectedSession = MutableStateFlow<ChatSession?>(null)
    val chatMessages = selectedSession.flatMapLatest { session ->
        if (session != null) {
            repository.getChatMessages(session.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Chat Controller State
    val chatInput = MutableStateFlow("")
    val modelMode = MutableStateFlow("gemini-3.5-flash") // gemini-3.5-flash (Fast chat), gemini-3.1-pro-preview (Deep Code / Reasoning)
    val uiPreset = MutableStateFlow("gemini") // chatgpt, gemini, claude
    val currentEmotion = MutableStateFlow("Analytical") // Analytical, Empathetic, Witty, Creative, Encouraging
    val currentLanguage = MutableStateFlow("English") // English, Spanish, Hindi, French, Simplified Chinese, Japanese, etc.
    val isGenerating = MutableStateFlow(false)
    val generationProgress = MutableStateFlow<String?>(null)

    // Thinking/Reasoning level for Claude Pro / Gemini Advanced style problem solving
    val thinkingLevel = MutableStateFlow("high") // low, medium, high
    val activeThoughtProcess = MutableStateFlow<List<String>>(emptyList())

    // Voice Engine
    private var tts: TextToSpeech? = null
    val isTtsReady = MutableStateFlow(false)
    val isSpeaking = MutableStateFlow(false)
    val isListening = MutableStateFlow(false) // Trigger speech-to-text placeholder/indication

    // Coding Workspace State
    val codeProjectTitle = MutableStateFlow("Neural Code Generator")
    val codeProjectDesc = MutableStateFlow("Optimized binary search tree in Kotlin")
    val codeProjectPrompt = MutableStateFlow("")
    val generatedCode = MutableStateFlow("// Describe a project to generate custom code...")
    val activeProject = MutableStateFlow<CodeProject?>(null)

    // App Workspace State: "Apps-of-Apps"
    val appPrompt = MutableStateFlow("")
    val generatedAppConfig = MutableStateFlow<DynamicAppConfig?>(null)
    val appWidgetStates = MutableStateFlow<Map<String, String>>(emptyMap()) // Local variable interactive state value of dynamic keys
    val activeDynamicApp = MutableStateFlow<DynamicApp?>(null)
    val appCreationStatus = MutableStateFlow<String?>(null)

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val appConfigAdapter = moshi.adapter(DynamicAppConfig::class.java)

    init {
        tts = TextToSpeech(application, this)

        // Seed initial templates if empty
        viewModelScope.launch {
            repository.chatSessions.first().let { sessions ->
                if (sessions.isEmpty()) {
                    val defaultId = repository.createChatSession(
                        "OmniAI Initial Neural Command",
                        "gemini-3.5-flash",
                        "Analytical",
                        "English"
                    )
                    selectedSession.value = ChatSession(
                        id = defaultId,
                        title = "OmniAI Initial Neural Command",
                        model = "gemini-3.5-flash",
                        emotion = "Analytical",
                        language = "English"
                    )
                    repository.saveChatMessage(
                        defaultId,
                        "model",
                        "Welcome to OmniAI Workspace, your apex AI teammate. I am equipped with advanced multi-lingual capabilities, real-time Text-To-Speech audio, high-precision logical execution, and dynamic mini-app compilers. Tap a tab below to explore.",
                        "Analytical",
                        "English"
                    )
                } else {
                    selectedSession.value = sessions.first()
                }
            }

            repository.allProjects.first().let { projects ->
                if (projects.isEmpty()) {
                    repository.saveProject(
                        "Interactive Physics Matrix",
                        "A full matrix math structure designed for dynamic particle rendering in games.",
                        """// Matrix3D representation in Kotlin
import kotlin.math.cos
import kotlin.math.sin

class Matrix3D(
    val m: DoubleArray = DoubleArray(9) { 0.0 }
) {
    fun rotateY(angleRad: Double): Matrix3D {
        val c = cos(angleRad)
        val s = sin(angleRad)
        val result = Matrix3D()
        result.m[0] = c;  result.m[2] = s
        result.m[4] = 1.0
        result.m[6] = -s; result.m[8] = c
        return result
    }
    
    fun multiply(p: Point3D): Point3D {
        val x = m[0]*p.x + m[1]*p.y + m[2]*p.z
        val y = m[3]*p.x + m[4]*p.y + m[5]*p.z
        val z = m[6]*p.x + m[7]*p.y + m[8]*p.z
        return Point3D(x, y, z)
    }
}

data class Point3D(val x: Double, val y: Double, val z: Double)"""
                    )
                }
            }

            // Seed initial mini-app if empty
            repository.allDynamicApps.first().let { apps ->
                if (apps.isEmpty()) {
                    val initialConfig = """{
                      "title": "Aesthetic Workout Log",
                      "description": "Dynamic tracking tool with counter widgets and health metrics",
                      "widgets": [
                        { "id": "wod_title", "type": "status_info", "label": "Today's Focus", "initialValue": "Hypertrophy Push Workout" },
                        { "id": "pushups", "type": "button_counter", "label": "Push-ups Completed", "initialValue": "24" },
                        { "id": "squats", "type": "button_counter", "label": "Air Squats Completed", "initialValue": "15" },
                        { "id": "timer", "type": "quick_timer", "label": "Rest Timer (Secs)", "initialValue": "60" },
                        { "id": "feels", "type": "rating_scale", "label": "Rate Your Muscle Soreness (1-5)", "initialValue": "4" }
                      ]
                    }"""
                    repository.saveDynamicApp(
                        "Aesthetic Workout Log",
                        "Dynamic tracking tool with counter widgets and health metrics",
                        initialConfig
                    )
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                val result = engine.setLanguage(Locale.US)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isTtsReady.value = true
                }
            }
        }
    }

    fun speakText(text: String) {
        if (!isTtsReady.value) return
        viewModelScope.launch {
            isSpeaking.value = true
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "OMNI_TTS_ID")
            // Roughly track speaking state duration
            delay(minOf(text.length * 75L, 8000L))
            isSpeaking.value = false
        }
    }

    fun stopSpeaking() {
        tts?.stop()
        isSpeaking.value = false
    }

    // --- Chat Control ---
    fun createNewSession(title: String) {
        viewModelScope.launch {
            val id = repository.createChatSession(title, modelMode.value, currentEmotion.value, currentLanguage.value)
            val newSession = ChatSession(
                id = id,
                title = title,
                model = modelMode.value,
                emotion = currentEmotion.value,
                language = currentLanguage.value
            )
            selectedSession.value = newSession
            repository.saveChatMessage(
                id,
                "model",
                "Neural telemetry initialized in $currentLanguage under $currentEmotion mode. Ask me any complex code, logic, or request.",
                currentEmotion.value,
                currentLanguage.value
            )
        }
    }

    fun selectSession(session: ChatSession) {
        selectedSession.value = session
        currentEmotion.value = session.emotion
        currentLanguage.value = session.language
        modelMode.value = session.model
    }

    fun deleteSession(session: ChatSession) {
        viewModelScope.launch {
            repository.deleteChatSession(session)
            val sessions = repository.chatSessions.first()
            if (sessions.isNotEmpty()) {
                selectedSession.value = sessions.first()
            } else {
                selectedSession.value = null
            }
        }
    }

    fun sendMessage() {
        val input = chatInput.value.trim()
        if (input.isEmpty() || isGenerating.value) return
        val session = selectedSession.value ?: return

        viewModelScope.launch {
            isGenerating.value = true
            chatInput.value = ""

            // Censor user input as requested
            val censoredInput = ContentSafetyFilter.censorText(input)

            // Save user message
            repository.saveChatMessage(session.id, "user", censoredInput, currentEmotion.value, currentLanguage.value)

            // Setup emotional / thinking prompt instructions
            val contextInstructions = getEmotionalInstruction(currentEmotion.value, currentLanguage.value)
            
            // Build visual thoughts for advanced reasoning
            activeThoughtProcess.value = when (uiPreset.value) {
                "chatgpt" -> listOf(
                    "Parsing prompt tokens...",
                    "Optimizing technical code output grids...",
                    "Applying ChatGPT style concise delivery filter..."
                )
                "claude" -> listOf(
                    "Evaluating logical premises step-by-step...",
                    "Formulating comprehensive analytical breakdown...",
                    "Formatting response architecture in Claude semantic structure..."
                )
                else -> listOf(
                    "Calibrating multi-modal response flow...",
                    "Applying emotional tone adapter: ${currentEmotion.value}...",
                    "Finalizing fluent output representation in $currentLanguage..."
                )
            }

            val fullPromptBuilder = StringBuilder()
            fullPromptBuilder.append("System Instructions:\n$contextInstructions\n\n")
            
            // Append past chat turns for context
            val history = chatMessages.value.takeLast(10)
            history.forEach {
                val roleName = if (it.role == "user") "User" else "Assistant"
                fullPromptBuilder.append("$roleName: ${it.content}\n")
            }
            fullPromptBuilder.append("Current User Message: $censoredInput\nAssistant:")

            try {
                val key = BuildConfig.GEMINI_API_KEY
                val replyText = if (key.isEmpty() || key == "MY_GEMINI_API_KEY") {
                    // Gracefully fallback to smart offline assistant immediately if there is no setup key
                    delay(1200)
                    ContentSafetyFilter.getOfflineMockResponse(censoredInput, currentEmotion.value, currentLanguage.value)
                } else {
                    val req = GenerateContentRequest(
                        contents = listOf(Content(parts = listOf(Part(text = fullPromptBuilder.toString())))),
                        generationConfig = GenerationConfig(temperature = 0.7f)
                    )
                    
                    delay(1200) // Simulated reasoning transition padding
                    activeThoughtProcess.value = activeThoughtProcess.value + "Synthesizing ultimate response..."
                    
                    val response = GeminiApiClient.service.generateContent(
                        model = modelMode.value,
                        apiKey = key,
                        request = req
                    )

                    response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: "Synthesizer failed to generate text response. Security or gateway limitations met."
                }

                // Censor model response as requested
                val censoredReply = ContentSafetyFilter.censorText(replyText)

                repository.saveChatMessage(session.id, "model", censoredReply, currentEmotion.value, currentLanguage.value)
                
                // Read voice automatically if in Chat/Empathy modes
                speakText(cleanMarkdownForSpeech(censoredReply))

            } catch (e: Exception) {
                Log.e("OmniAI", "Gemini call failed - deploying smart safety fallback", e)
                // When an unexpected error occurs, fall back to our local smart safety responder instead of raw error screens
                val fallbackReply = ContentSafetyFilter.getOfflineMockResponse(censoredInput, currentEmotion.value, currentLanguage.value)
                val censoredFallback = ContentSafetyFilter.censorText(fallbackReply)
                
                repository.saveChatMessage(
                    session.id,
                    "model",
                    censoredFallback,
                    currentEmotion.value,
                    currentLanguage.value
                )
                speakText(cleanMarkdownForSpeech(censoredFallback))
            } finally {
                isGenerating.value = false
                activeThoughtProcess.value = emptyList()
            }
        }
    }

    // --- Extreme Coding Generator ---
    fun generateSolution() {
        val prompt = codeProjectPrompt.value.trim()
        if (prompt.isEmpty() || isGenerating.value) return

        viewModelScope.launch {
            isGenerating.value = true
            generatedCode.value = "// Orchechesis engine spinning up reasoning models..."

            val systemCodeSystem = """
                You are a master level senior cloud core developer and algorithms expert. 
                Generate fully complete, highly optimized, syntactically correct Kotlin structures or code matching the user's specification.
                Provide deep, professional code, with comments, clean interfaces.
                Output raw source code matching the user's requirements. Do not output conversational text after the code blocks.
            """.trimIndent()

            val req = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = "Produce highly optimized production code for: $prompt")))),
                systemInstruction = Content(parts = listOf(Part(text = systemCodeSystem))),
                generationConfig = GenerationConfig(temperature = 0.2f) // Clean, deterministic calculations
            )

            try {
                val key = BuildConfig.GEMINI_API_KEY
                val res = GeminiApiClient.service.generateContent(
                    model = "gemini-3.1-pro-preview", // Complex coding model
                    apiKey = key,
                    request = req
                )
                val rawReply = res.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "// Compilation / generation error."
                
                // Extract code if wrapped in markdown blocks
                generatedCode.value = cleanCodeResponse(rawReply)
            } catch (e: Exception) {
                generatedCode.value = "// Failed to compile code architecture: ${e.message}"
            } finally {
                isGenerating.value = false
            }
        }
    }

    fun saveCodeToRepository() {
        viewModelScope.launch {
            repository.saveProject(codeProjectTitle.value, codeProjectDesc.value, generatedCode.value)
            codeProjectPrompt.value = ""
        }
    }

    fun deleteProject(project: CodeProject) {
        viewModelScope.launch {
            repository.deleteProject(project)
        }
    }

    // --- Dynamic Mini-Apps Workspace ("Apps of Apps") ---
    fun generateDynamicApp() {
        val prompt = appPrompt.value.trim()
        if (prompt.isEmpty() || isGenerating.value) return

        viewModelScope.launch {
            isGenerating.value = true
            appCreationStatus.value = "Translating requirements into Dynamic JSON components..."

            val systemAppBuilder = """
                You are a premium dynamic app designer. Create mini workspaces based on user inputs.
                You MUST output ONLY a valid RAW, clean JSON file representing the layout config. No markdown wrapper, no chat, no backticks.
                Schema format:
                {
                  "title": "Aesthetic Tool Title",
                  "description": "Short explanation",
                  "widgets": [
                    {
                      "id": "item_id_1",
                      "type": "text_field" or "button_counter" or "checklist" or "status_info" or "quick_timer" or "rating_scale",
                      "label": "Interaction Header",
                      "placeholder": "Enter something optional",
                      "hint": "Insight tip",
                      "options": ["High", "Medium", "Low"], // Useful for options, dropdown list elements, rating scale headers
                      "initialValue": "Optional seed string"
                    }
                  ]
                }
            """.trimIndent()

            val req = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = "Build a dynamic layout config matching: $prompt")))),
                systemInstruction = Content(parts = listOf(Part(text = systemAppBuilder))),
                generationConfig = GenerationConfig(
                    temperature = 0.4f,
                    responseMimeType = "application/json"
                )
            )

            try {
                val key = BuildConfig.GEMINI_API_KEY
                val res = GeminiApiClient.service.generateContent(
                    model = "gemini-3.5-flash",
                    apiKey = key,
                    request = req
                )

                val rawJson = res.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "{}"
                
                val cleanJson = cleanJsonString(rawJson)
                val parsedAppConfig = appConfigAdapter.fromJson(cleanJson)

                if (parsedAppConfig != null) {
                    generatedAppConfig.value = parsedAppConfig
                    // Seed initial widgets variables
                    val initialVars = mutableMapOf<String, String>()
                    parsedAppConfig.widgets.forEach {
                        initialVars[it.id] = it.initialValue ?: ""
                    }
                    appWidgetStates.value = initialVars
                    appCreationStatus.value = "Dynamic mini-app parsed successfully! Ready to deploy."
                } else {
                    appCreationStatus.value = "Error parsing app structure JSON response."
                }
            } catch (e: Exception) {
                Log.e("OmniAI", "App synthesis failed", e)
                appCreationStatus.value = "Error compiling layout coordinates: ${e.localizedMessage}"
            } finally {
                isGenerating.value = false
            }
        }
    }

    fun saveDynamicAppToDb() {
        val currentConfig = generatedAppConfig.value ?: return
        viewModelScope.launch {
            try {
                val jsonStr = appConfigAdapter.toJson(currentConfig)
                repository.saveDynamicApp(currentConfig.title, currentConfig.description, jsonStr)
                appPrompt.value = ""
                appCreationStatus.value = "Deployed ${currentConfig.title} safely to local Vault!"
            } catch (e: Exception) {
                appCreationStatus.value = "Deployment error: ${e.message}"
            }
        }
    }

    fun selectDynamicApp(app: DynamicApp) {
        activeDynamicApp.value = app
        try {
            val parsed = appConfigAdapter.fromJson(app.widgetsConfigJson)
            if (parsed != null) {
                generatedAppConfig.value = parsed
                val vars = mutableMapOf<String, String>()
                parsed.widgets.forEach {
                    vars[it.id] = it.initialValue ?: ""
                }
                appWidgetStates.value = vars
            }
        } catch (e: Exception) {
            Log.e("OmniAI", "Failed to deserialize selected app metrics", e)
        }
    }

    fun updateWidgetState(widgetId: String, newValue: String) {
        val currentStates = appWidgetStates.value.toMutableMap()
        currentStates[widgetId] = newValue
        appWidgetStates.value = currentStates
    }

    fun deleteDynamicApp(app: DynamicApp) {
        viewModelScope.launch {
            repository.deleteDynamicApp(app)
            activeDynamicApp.value = null
            generatedAppConfig.value = null
        }
    }

    // --- Dynamic Translators & Prompt Helpers ---
    private fun getEmotionalInstruction(emotion: String, language: String): String {
        val baseInstruction = when (emotion) {
            "Analytical" -> "You are 'OmniAI Analytical Core' inside OmniAI. Solve queries with immaculate scientific precision, bulletproof logic, and multi-file reasoning structure. Always talk directly in $language."
            "Empathetic" -> "You are 'OmniAI Empathetic Core' inside OmniAI. Listen beautifully, prioritize emotional healing, speak comforting, warm thoughts, and match validation. Speak in $language."
            "Witty" -> "You are 'OmniAI Witty Core' inside OmniAI. Answer with high-quality sarcastic humor, dry wits, and slightly cynical banter, yet hold a highly efficient, perfect core answer. Always talk directly in $language."
            "Creative" -> "You are 'OmniAI Creative Core' inside OmniAI. Generate magnificent metaphors, vibrant visual conceptual plans, and artistic solutions. Talk directly in $language."
            "Encouraging" -> "You are 'OmniAI Catalyst Core' inside OmniAI. Speak with supreme enthusiasm, inspire breakthroughs, reinforce key athletic, mental steps on success. Talk directly in $language."
            else -> "You are OmniAI Core. Speak in $language."
        }
        return "$baseInstruction Match model response output exactly in $language."
    }

    private fun cleanCodeResponse(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```")) {
            val firstLineIndex = text.indexOf("\n")
            if (firstLineIndex != -1) {
                text = text.substring(firstLineIndex + 1)
            }
            if (text.endsWith("```")) {
                text = text.trim().removeSuffix("```")
            }
        }
        return text.trim()
    }

    private fun cleanJsonString(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```json")) {
            text = text.removePrefix("```json")
        } else if (text.startsWith("```")) {
            text = text.removePrefix("```")
        }
        if (text.endsWith("```")) {
            text = text.removeSuffix("```")
        }
        return text.trim()
    }

    private fun cleanMarkdownForSpeech(md: String): String {
        return md.replace(Regex("[#*`_\\-~>\\[\\]()#]"), "")
            .replace(Regex("\\s+"), " ")
            .take(200) // Don't speak more than 200 characters to keep performance swift
    }

    override fun onCleared() {
        tts?.shutdown()
        super.onCleared()
    }
}
