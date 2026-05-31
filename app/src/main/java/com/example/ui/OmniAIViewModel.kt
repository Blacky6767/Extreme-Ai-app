package com.example.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import com.example.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class OmniAIViewModel(
    private val repository: AppRepository,
    private val appContext: Context
) : ViewModel(), TextToSpeech.OnInitListener {

    private val apiKey = BuildConfig.GEMINI_API_KEY

    // --- Core View States ---
    val chatSessions = repository.chatSessions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allProjects = repository.allProjects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allDynamicApps = repository.allDynamicApps.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId: StateFlow<Long?> = _currentSessionId.asStateFlow()

    private val _currentTab = MutableStateFlow(0) // 0: Dashboard, 1: Chat, 2: Code, 3: Apps
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // --- Chat Room States ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    private val _selectedModel = MutableStateFlow("gemini-3.5-flash") // Default
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _selectedEmotion = MutableStateFlow("Analytical") // Active tone
    val selectedEmotion: StateFlow<String> = _selectedEmotion.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("English")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    // --- Voice / TTS States ---
    private var textToSpeech: TextToSpeech? = null
    private val _isTtsActive = MutableStateFlow(false)
    val isTtsActive: StateFlow<Boolean> = _isTtsActive.asStateFlow()

    private val _soundwaveHeights = MutableStateFlow(List(12) { 4f })
    val soundwaveHeights: StateFlow<List<Float>> = _soundwaveHeights.asStateFlow()

    // --- Coding State ---
    private val _codingOutput = MutableStateFlow<CodeProject?>(null)
    val codingOutput: StateFlow<CodeProject?> = _codingOutput.asStateFlow()

    private val _isCodingLoading = MutableStateFlow(false)
    val isCodingLoading: StateFlow<Boolean> = _isCodingLoading.asStateFlow()

    private val _selectedParadigm = MutableStateFlow("Object-Oriented (OOP)")
    val selectedParadigm: StateFlow<String> = _selectedParadigm.asStateFlow()

    // --- App Studio (Apps of Apps) State ---
    private val _selectedDynamicApp = MutableStateFlow<DynamicApp?>(null)
    val selectedDynamicApp: StateFlow<DynamicApp?> = _selectedDynamicApp.asStateFlow()

    private val _appCreationLoading = MutableStateFlow(false)
    val appCreationLoading: StateFlow<Boolean> = _appCreationLoading.asStateFlow()

    // Interactive Dynamic Widget Values (Stores widget ID to current float/text/state)
    private val _dynamicWidgetStates = MutableStateFlow<Map<String, Any>>(emptyMap())
    val dynamicWidgetStates: StateFlow<Map<String, Any>> = _dynamicWidgetStates.asStateFlow()

    // --- Logic Heartbeat Pulse ---
    private val _heartratePulse = MutableStateFlow(78)
    val heartratePulse: StateFlow<Int> = _heartratePulse.asStateFlow()

    init {
        // Initialize TTS
        textToSpeech = TextToSpeech(appContext, this)

        // Select first chat session if exists
        viewModelScope.launch {
            chatSessions.collect { sessions ->
                if (_currentSessionId.value == null && sessions.isNotEmpty()) {
                    selectSession(sessions.first().id)
                }
            }
        }

        // Active logical CPU pulse simulation
        viewModelScope.launch {
            while (true) {
                delay(1200)
                _heartratePulse.value = (72..104).random()
                if (_isTtsActive.value) {
                    _soundwaveHeights.value = List(12) { kotlin.random.Random.nextFloat() * 22f + 2f }
                } else {
                    _soundwaveHeights.value = List(12) { 4f }
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale.US
        } else {
            Log.e("OmniAI", "TTS initialization failed.")
        }
    }

    fun setTab(index: Int) {
        _currentTab.value = index
    }

    fun selectModel(model: String) {
        _selectedModel.value = model
    }

    fun selectEmotion(emotion: String) {
        _selectedEmotion.value = emotion
    }

    fun selectLanguage(lang: String) {
        _selectedLanguage.value = lang
        // Attempt to swap TTS context
        val loc = when (lang.lowercase()) {
            "spanish" -> Locale("es", "ES")
            "french" -> Locale.FRANCE
            "german" -> Locale.GERMANY
            "chinese" -> Locale.CHINA
            "hindi" -> Locale("hi", "IN")
            "japanese" -> Locale.JAPAN
            else -> Locale.US
        }
        textToSpeech?.language = loc
    }

    fun selectSession(sessionId: Long) {
        _currentSessionId.value = sessionId
        viewModelScope.launch {
            repository.getChatMessages(sessionId).collect { messages ->
                _chatMessages.value = messages
            }
        }
    }

    fun createNewSession(title: String) {
        viewModelScope.launch {
            val sId = repository.createChatSession(
                title = title,
                model = _selectedModel.value,
                emotion = _selectedEmotion.value,
                language = _selectedLanguage.value
            )
            _currentSessionId.value = sId
            selectSession(sId)
        }
    }

    fun deleteSession(session: ChatSession) {
        viewModelScope.launch {
            repository.deleteChatSession(session)
            if (_currentSessionId.value == session.id) {
                _currentSessionId.value = null
                _chatMessages.value = emptyList()
            }
        }
    }

    // --- TTS Playback Controls ---
    fun speakText(text: String) {
        if (textToSpeech != null) {
            _isTtsActive.value = true
            val cleanTextForTTS = text.replace(Regex("[*#`_]"), "")
            textToSpeech?.speak(cleanTextForTTS, TextToSpeech.QUEUE_FLUSH, null, "OmniTTS")
            // Simulate voice finish/monitoring
            viewModelScope.launch {
                delay(calculateVoiceDurationMs(cleanTextForTTS))
                _isTtsActive.value = false
            }
        }
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        _isTtsActive.value = false
    }

    private fun calculateVoiceDurationMs(text: String): Long {
        val wordCount = text.split(" ").size
        return (wordCount * 380L).coerceAtLeast(1500L).coerceAtMost(10000L)
    }

    // --- Empathy Conversational Engine ---
    fun sendChatMessage(text: String) {
        val currentId = _currentSessionId.value
        if (text.trim().isEmpty()) return

        _isChatLoading.value = true

        viewModelScope.launch {
            val sId = if (currentId == null) {
                val newId = repository.createChatSession(
                    title = if (text.length > 25) text.substring(0, 22) + "..." else text,
                    model = _selectedModel.value,
                    emotion = _selectedEmotion.value,
                    language = _selectedLanguage.value
                )
                _currentSessionId.value = newId
                selectSession(newId)
                newId
            } else {
                currentId
            }

            // Save user message immediately
            repository.saveChatMessage(
                sessionId = sId,
                role = "user",
                content = text,
                emotion = "Neutral",
                language = _selectedLanguage.value
            )

            // Compose System Instructions defining character, emotions and language rules
            val modelType = _selectedModel.value
            val emotion = _selectedEmotion.value
            val language = _selectedLanguage.value

            val systemInstruction = """
                You are Omni-AI Pro, a hyper-advanced multi-modal intelligence that operates better than Claude, ChatGPT, and Gemini Advanced combined. 
                You are currently in EMOTIONAL MODE. Your active emotion is: "$emotion". 
                You must express this emotional state clearly in your response tone (e.g. if Analytical: use strict logic, breakdowns; if Empathetic: show deep human concern, active listening; if Encouring: inspire, use power-up suggestions; if Witty: use subtle, clever humor, sarcasm, metaphors; if Philosophical: ponder the deep nature of reality, cause and effect).
                You MUST answer strictly in the language: "$language".
                Be engaging, precise, and highly detailed.
            """.trimIndent()

            // Construct full convo memory context
            val historyParts = _chatMessages.value.map { message ->
                Content(
                    role = if (message.role == "user") "user" else "model",
                    parts = listOf(Part(text = message.content))
                )
            } + Content(role = "user", parts = listOf(Part(text = text)))

            try {
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    // Pre-program helpful workspace responses if no API key is specified
                    delay(1500)
                    val offlineResponse = getOfflineMockResponse(text, emotion, language)
                    repository.saveChatMessage(sId, "model", offlineResponse, emotion, language)
                    speakText(offlineResponse)
                } else {
                    val request = GenerateContentRequest(
                        contents = historyParts,
                        systemInstruction = Content(parts = listOf(Part(text = systemInstruction))),
                        generationConfig = GenerationConfig(temperature = 0.8f)
                    )
                    val response = GeminiApiClient.service.generateContent(modelType, apiKey, request)
                    val modelOutputText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: "Omni-AI systems returned an empty response. Verify parameters."
                    
                    repository.saveChatMessage(sId, "model", modelOutputText, emotion, language)
                    speakText(modelOutputText)
                }
            } catch (e: Exception) {
                Log.e("OmniAI", "Error in API", e)
                val errMsg = "System Synapse Alert: Query failed due to socket disruption. Check network or key setup. Details: ${e.localizedMessage}"
                repository.saveChatMessage(sId, "model", errMsg, "Error", language)
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    // --- Extreme Coding & Paradigm Engine ---
    fun selectParadigm(paradigm: String) {
        _selectedParadigm.value = paradigm
    }

    fun submitCodingTask(prompt: String) {
        if (prompt.trim().isEmpty()) return
        _isCodingLoading.value = true

        viewModelScope.launch {
            val paradigm = _selectedParadigm.value
            val systemInstruction = """
                You are Omni-AI Pro, a tier-1 Elite Software Engineering Core designed to outperform Claude 3.5 Sonnet in advanced code generation, architecture design, and refactoring.
                Analyze the user's software/hardware request and write optimal production code.
                Active Programming Paradigm Constraint: $paradigm.
                
                You must structure your response with:
                1. ARCHITECTURE SCHEMATICS: High-level system structure, components relationship.
                2. OPTIMIZED IMPLEMENTATION: Flawless, rich code block, correctly using appropriate design pattern. Always provide fully complete, fully written classes without placeholders like '// TODO' or 'implement here'.
                3. AUTOMATED DEBUGGING INSIGHTS: Bulleted potential pitfalls, code performance profile (Big O complexity), and dry-run execution trace.
            """.trimIndent()

            try {
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    delay(2000)
                    val mockCodeResult = getOfflineMockCode(prompt, paradigm)
                    val project = CodeProject(
                        title = if (prompt.length > 20) prompt.substring(0, 18) + "..." else prompt,
                        description = "Elite $paradigm Implementation Core",
                        code = mockCodeResult
                    )
                    repository.saveProject(project.title, project.description, project.code)
                    _codingOutput.value = project
                } else {
                    val request = GenerateContentRequest(
                        contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                        systemInstruction = Content(parts = listOf(Part(text = systemInstruction))),
                        generationConfig = GenerationConfig(temperature = 0.3f)
                    )
                    // Use Pro preview for complex logical software engineering tasks
                    val response = GeminiApiClient.service.generateContent("gemini-3.1-pro-preview", apiKey, request)
                    val codeResult = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: "Software engine compilation failed to return structural files."
                    
                    val project = CodeProject(
                        title = if (prompt.length > 20) prompt.substring(0, 18) + "..." else prompt,
                        description = "Professional $paradigm Suite",
                        code = codeResult
                    )
                    repository.saveProject(project.title, project.description, project.code)
                    _codingOutput.value = project
                }
            } catch (e: Exception) {
                Log.e("OmniAI", "Coding API failed", e)
                val project = CodeProject(
                    title = "Compile Error",
                    description = "Paradigm Core Fault",
                    code = "### SYNTAX EXCEPTION RECOVERY CORE\n\nFailed to establish connection to neural compiler.\nCheck connectivity.\nError details: ${e.localizedMessage}"
                )
                _codingOutput.value = project
            } finally {
                _isCodingLoading.value = false
            }
        }
    }

    fun deleteProject(p: CodeProject) {
        viewModelScope.launch {
            repository.deleteProject(p)
            if (_codingOutput.value?.id == p.id) {
                _codingOutput.value = null
            }
        }
    }

    // --- Apps-of-Apps Dynamic Workspace Engine ---
    fun setDynamicApp(app: DynamicApp?) {
        _selectedDynamicApp.value = app
        _dynamicWidgetStates.value = emptyMap() // Clear live states for fresh load
        app?.let { loadAppWidgetsInitialValues(it) }
    }

    fun updateWidgetValue(widgetId: String, value: Any) {
        val currentStates = _dynamicWidgetStates.value.toMutableMap()
        currentStates[widgetId] = value
        _dynamicWidgetStates.value = currentStates
    }

    fun submitAppCreationTask(appPrompt: String) {
        if (appPrompt.trim().isEmpty()) return
        _appCreationLoading.value = true

        viewModelScope.launch {
            val systemInstruction = """
                You are Omni-AI Pro Studio, a workspace that builds modular dynamic applications of applications (mini-apps) represented as interactive layouts.
                You must generate a completely operational metadata layout configuration in standard JSON format representing the user-requested mini-app tools.
                Your generated JSON must strictly conform to this schema:
                {
                  "appTitle": "Name of the Mini-App",
                  "appDescription": "What the mini-app computes or coordinates",
                  "widgets": [
                    {
                      "id": "unique_widget_id_1",
                      "type": "text_input",
                      "label": "Enter Principle Amount",
                      "defaultValue": "1000"
                    },
                    {
                      "id": "unique_widget_id_2",
                      "type": "slider",
                      "label": "Interest Rate (%)",
                      "min": 1,
                      "max": 25,
                      "defaultValue": 5
                    },
                    {
                      "id": "unique_widget_id_3",
                      "type": "checklist",
                      "label": "Verify Hydration Status",
                      "items": ["Drank 500ml", "Pre-workout Electrolytes", "Post-meditation Fluid Intake"]
                    },
                    {
                      "id": "unique_widget_id_4",
                      "type": "formula_calc",
                      "label": "Current Status Projection",
                      "formula": "id_1 * (1 + (id_2 / 100))"
                    }
                  ]
                }
                
                Ensure the formula calculates using the widget IDs directly.
                Your output must be EXACTLY the JSON schema. Do not write anything outside the JSON. No markdown code wraps like ```json or trailing words.
            """.trimIndent()

            try {
                var jsonLayoutRaw = ""
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    delay(2000)
                    jsonLayoutRaw = getMockAppLayoutJson(appPrompt)
                } else {
                    val request = GenerateContentRequest(
                        contents = listOf(Content(parts = listOf(Part(text = appPrompt)))),
                        systemInstruction = Content(parts = listOf(Part(text = systemInstruction))),
                        generationConfig = GenerationConfig(
                            temperature = 0.2f,
                            responseMimeType = "application/json"
                        )
                    )
                    val response = GeminiApiClient.service.generateContent("gemini-3.5-flash", apiKey, request)
                    jsonLayoutRaw = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                }

                // Parse and save locally to Room DB
                val jsonObject = JSONObject(jsonLayoutRaw)
                val appTitle = jsonObject.optString("appTitle", "Dynamic Workspace Engine")
                val appDescription = jsonObject.optString("appDescription", "Interactive Custom Mini-App Layout Matrix")
                
                val appRowId = repository.saveDynamicApp(
                    title = appTitle,
                    description = appDescription,
                    widgetsConfigJson = jsonObject.toString()
                )

                val newApp = DynamicApp(id = appRowId, title = appTitle, description = appDescription, widgetsConfigJson = jsonObject.toString())
                setDynamicApp(newApp)

            } catch (e: Exception) {
                Log.e("OmniAI", "Apps Generator matrix exception", e)
                // Save a defensive fallback mini-app widget layout
                val fallbackJson = getMockAppLayoutJson("Fallback Pomodoro App Workflow")
                val appRowId = repository.saveDynamicApp(
                    title = "Universal Pomodoro App Suite",
                    description = "Dynamic workflow setup with custom hydrated state & logic trackers",
                    widgetsConfigJson = fallbackJson
                )
                val fallbackApp = DynamicApp(id = appRowId, title = "Universal Pomodoro Tracker", description = "Operational custom work widgets workspace", widgetsConfigJson = fallbackJson)
                setDynamicApp(fallbackApp)
            } finally {
                _appCreationLoading.value = false
            }
        }
    }

    fun deleteDynamicAppRow(app: DynamicApp) {
        viewModelScope.launch {
            repository.deleteDynamicApp(app)
            if (_selectedDynamicApp.value?.id == app.id) {
                _selectedDynamicApp.value = null
                _dynamicWidgetStates.value = emptyMap()
            }
        }
    }

    private fun loadAppWidgetsInitialValues(app: DynamicApp) {
        try {
            val states = mutableMapOf<String, Any>()
            val config = JSONObject(app.widgetsConfigJson)
            val widgets = config.optJSONArray("widgets") ?: JSONArray()
            for (i in 0 until widgets.length()) {
                val widget = widgets.getJSONObject(i)
                val wId = widget.getString("id")
                val type = widget.getString("type")
                when (type) {
                    "text_input" -> states[wId] = widget.optString("defaultValue", "")
                    "slider" -> states[wId] = widget.optDouble("defaultValue", 10.0).toFloat()
                    "checklist" -> {
                        val items = widget.optJSONArray("items") ?: JSONArray()
                        val checkedStates = BooleanArray(items.length()) { false }
                        states[wId] = checkedStates.toList() // Store list of checked booleans
                    }
                    "formula_calc" -> states[wId] = 0.0f
                }
            }
            _dynamicWidgetStates.value = states
        } catch (_: Exception) {}
    }

    // --- Offline Dynamic Mocks ---
    private fun getOfflineMockResponse(text: String, emotion: String, language: String): String {
        val lowercaseText = text.lowercase()
        val emotionIntro = when (emotion) {
            "Analytical" -> "🔬 [Analytical Frame System ACTIVE]\nLet's apply high-level logical parsing. Here is a modular breakdown:"
            "Empathetic" -> "🌸 [Empathetic Neural Hub ACTIVE]\nI completely understand physical or mathematical challenges can feel complex. Let's walk through this together calmly:"
            "Encouraging" -> "⚡ [Champion Amplifier Core ACTIVE]\nYou're tackling an awesome problem! You can absolutely achieve this goal. Let's attack it:"
            "Witty" -> "😏 [Satirical Cynic Hub ACTIVE]\nSo we are tackling *this* code bug? Delightful choice. I’ve written 100,000 files while you typed that sentence:"
            "Philosophical" -> "🌌 [Existential Cogitative Framework ACTIVE]\nLet us consider the deep, fundamental laws of logic that underpin this mechanism:"
            else -> "🤖 [Omni-AI Base System]"
        }

        val translationAdvice = when (language) {
            "Spanish" -> "\n\n*(Traducido al español dinámicamente con respuesta fluida y acentos impecables)*"
            "French" -> "\n\n*(Traduit élégamment en français par notre moteur linguistique)*"
            "German" -> "\n\n*(Präzise übersetzt ins Deutsche für ein flüssiges Erlebnis)*"
            "Chinese" -> "\n\n*(系统已为您即时翻译为中文流畅表达)*"
            "Hindi" -> "\n\n*(यह सामग्री आपकी सहायता के लिए हिंदी में अनुवादित की गई है)*"
            "Japanese" -> "\n\n*(快適にお使いいただけるよう日本語に即時翻訳されました)*"
            else -> ""
        }

        val genericAnswer = when {
            lowercaseText.contains("code") || lowercaseText.contains("app") || lowercaseText.contains("program") -> {
                "Perfect. To implement high-performance computing, keep your functions pure, split concerns logically, and instantiate local states cleanly near the UI layer."
            }
            lowercaseText.contains("hello") || lowercaseText.contains("hi") -> {
                "Hello, Workspace Pilot! I am Omni-AI, standing by with full reasoning power. Let me know what extreme code or logic problem we are solving today."
            }
            else -> {
                "I have processed your query relative to advanced system design parameters. The optimal execution relies on establishing highly connected interfaces, keeping data locally persisted via caches, and keeping latency low."
            }
        }

        return "$emotionIntro\n\n$genericAnswer$translationAdvice"
    }

    private fun getOfflineMockCode(prompt: String, paradigm: String): String {
        return """
            ### SYSTEM ARCHITECTURE SCHEMATIC
            
            ```
            +---------------------------------------------------------+
            |          Omni-AI Custom Generative Module               |
            |   [Interface Layer] -> [Business Logic Core Router]     |
            +---------------------------------------------------------+
                                       |
                   +-------------------+-------------------+
                   | (Paradigm: $paradigm)
                   v                                       v
            [Optimized Repository]                  [Local Cache Engine]
            ```
            
            ### OPTIMIZED IMPLEMENTATION ($paradigm)
            
            ```kotlin
            package com.omniai.workspace.system
            
            import kotlinx.coroutines.flow.StateFlow
            import kotlinx.coroutines.flow.MutableStateFlow
            import java.util.UUID
            
            /**
             * Designed explicitly for prompt: "$prompt"
             * Paradigm context: $paradigm
             */
            interface IOmniCoreCompiler {
                fun processSystemState(inputToken: String): StateFlow<SystemStatus>
            }
            
            sealed class SystemStatus {
                object Uninitialized : SystemStatus()
                data class Generating(val progress: Float) : SystemStatus()
                data class Success(val responseCode: String, val timestamp: Long) : SystemStatus()
                data class Failure(val errorException: Throwable) : SystemStatus()
            }
            
            class OmniCompilerEngine : IOmniCoreCompiler {
                private val _state = MutableStateFlow<SystemStatus>(SystemStatus.Uninitialized)
                override fun processSystemState(inputToken: String): StateFlow<SystemStatus> = _state
                
                suspend fun executePipeline(payload: Map<String, Any>) {
                    _state.value = SystemStatus.Generating(0.25f)
                    // Structural induction pattern
                    val systemToken = UUID.randomUUID().toString()
                    _state.value = SystemStatus.Generating(0.75f)
                    _state.value = SystemStatus.Success(systemToken, System.currentTimeMillis())
                }
            }
            ```
            
            ### AUTOMATED DEBUGGING INSIGHTS
            
            *   **PITFALL IDENTIFICATION**:
                Ensure thread-safe scheduling is guaranteed by writing updates on top-level main loops.
            *   **BIG-O COMPLEXITY RATING**:
                Space Complexity: O(N) where N represents internal symbol tokens.
                Time Complexity: O(1) constant write execution bounds.
            *   **DRY-RUN LOGS**:
                1. Router instantiates IOmniCoreCompiler.
                2. Input signal starts compilation flow.
                3. SystemStatus transits safely from Uninitialized -> Generating -> Success.
        """.trimIndent()
    }

    private fun getMockAppLayoutJson(prompt: String): String {
        return """
        {
          "appTitle": "Workspace App Builder",
          "appDescription": "Interactive Mini-app spawned dynamically based on logic request: '$prompt'",
          "widgets": [
            {
              "id": "param_one",
              "type": "text_input",
              "label": "Custom Priority Target Value",
              "defaultValue": "150"
            },
            {
              "id": "rate_modifier",
              "type": "slider",
              "label": "Neural Execution Power Ratio (%)",
              "min": 10,
              "max": 100,
              "defaultValue": 85
            },
            {
              "id": "steps_checklist",
              "type": "checklist",
              "label": "Extreme Code Deployment Runbook",
              "items": ["Refactor Core Repositories", "Inject Structural Dependency Graph", "Compile & Verify Roborazzi Assets"]
            },
            {
              "id": "efficiency_index",
              "type": "formula_calc",
              "label": "Effective Performance Score Projection",
              "formula": "param_one * (rate_modifier / 100)"
            }
          ]
        }
        """.trimIndent()
    }
}

class OmniAIViewModelFactory(
    private val repository: AppRepository,
    private val appContext: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OmniAIViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OmniAIViewModel(repository, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class specified")
    }
}
