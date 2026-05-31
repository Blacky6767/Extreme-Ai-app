package com.example.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.MainViewModel
import com.example.data.*
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkspaceScreen(viewModel: MainViewModel) {
    val activeTab by viewModel.activeTab.collectAsState()
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("main_workspace_scaffold"),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            CustomBottomNavigation(
                activeTab = activeTab,
                onTabSelected = { viewModel.activeTab.value = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "tab_navigation_anim"
            ) { tab ->
                when (tab) {
                    "dashboard" -> BentoDashboardScreen(viewModel)
                    "chat" -> GenerativeChatScreen(viewModel)
                    "workspace" -> ExtremeCodingScreen(viewModel)
                    "apps" -> AppsOfAppsScreen(viewModel)
                    "settings" -> TelemetrySettingsScreen(viewModel)
                }
            }
        }
    }
}

// --- Dynamic Visual Wave Generator for Premium Audio Speech Vibe ---
@Composable
fun SiriVoiceWave(modifier: Modifier = Modifier, isSpeaking: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice_wave_pulse")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Canvas(modifier = modifier.height(60.dp).fillMaxWidth().padding(horizontal = 8.dp)) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val maxAmplitude = if (isSpeaking) centerY * 0.7f * scale else centerY * 0.15f

        val path1 = Path()
        val path2 = Path()
        val path3 = Path()

        path1.moveTo(0f, centerY)
        path2.moveTo(0f, centerY)
        path3.moveTo(0f, centerY)

        for (x in 0..width.toInt() step 4) {
            val relativeX = x.toFloat() / width
            // Bell curve to taper ends beautifully
            val envelope = Math.sin(relativeX * Math.PI).toFloat()

            val y1 = centerY + Math.sin(relativeX * 3.5 * Math.PI + phase).toFloat() * maxAmplitude * envelope
            val y2 = centerY + Math.sin(relativeX * 2.2 * Math.PI - phase * 1.5f).toFloat() * (maxAmplitude * 0.7f) * envelope
            val y3 = centerY + Math.sin(relativeX * 5.0 * Math.PI + phase * 0.5f).toFloat() * (maxAmplitude * 0.4f) * envelope

            path1.lineTo(x.toFloat(), y1)
            path2.lineTo(x.toFloat(), y2)
            path3.lineTo(x.toFloat(), y3)
        }

        drawPath(path1, brush = Brush.horizontalGradient(listOf(Color(0xFFD0BCFF), Color(0xFF8B5CF6))), style = Stroke(width = 3.dp.toPx()))
        drawPath(path2, brush = Brush.horizontalGradient(listOf(Color(0xFFF472B6), Color(0xFFEC4899))), style = Stroke(width = 2.dp.toPx()))
        drawPath(path3, brush = Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7))), style = Stroke(width = 1.dp.toPx()))
    }
}

// --- 1. Bento Dashboard Screen (From Premium "Elegant Dark" mockup!) ---
@Composable
fun BentoDashboardScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentEmotion by viewModel.currentEmotion.collectAsState()
    val activeLanguage by viewModel.currentLanguage.collectAsState()
    val modelMode by viewModel.modelMode.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState()
    val allDynamicApps by viewModel.allDynamicApps.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("bento_dashboard_root"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Aesthetic Top App Bar Mockup Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ω",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(
                        text = "OmniAI Workspace",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "OMNIAI COGNITIVE CORE v5.0",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.alpha(0.8f)
                    )
                }
            }

            // Connection Badge Pulsing
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2B2930))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Text(
                    text = "ACTIVE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            }
        }

        // Card 1: Primary Reasoning Engine (Span 2 Row 2 in Bento Grid)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "COGNITIVE CORE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                    
                    // Simulated active nodes
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(4) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Deep Reasoning Active",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Inductive core logic resolving mathematical patterns, code synthesis protocols, and hyper-precise semantic networks inside $activeLanguage.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.activeTab.value = "chat" },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Bolt, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Trigger Core", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.modelMode.value = if (modelMode.contains("pro")) "gemini-3.5-flash" else "gemini-3.1-pro-preview"
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (modelMode.contains("pro")) "Use Fast Flash" else "Use Deep Pro", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Grid split Row: Extreme Coding Module (Left) & Empathy EQ Core (Right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Extreme Coding Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp)
                    .clickable { viewModel.activeTab.value = "workspace" },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1D192B))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF49454F)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("</>", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Column {
                        Text(
                            text = "Extreme Coding",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$allProjects modules stored in neural archives.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Empathy EQ Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp)
                    .clickable { viewModel.activeTab.value = "chat" },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF332D41))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFB8C8)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (currentEmotion) {
                                "Analytical" -> "🔬"
                                "Empathetic" -> "❤️"
                                "Witty" -> "🍿"
                                "Creative" -> "🌌"
                                "Encouraging" -> "🔥"
                                else -> "🤖"
                            },
                            fontSize = 18.sp
                        )
                    }

                    Column {
                        Text(
                            text = "Empathy Face",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEFB8C8)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "AI Sentiment: $currentEmotion mode active.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // Language & Voice Space card (Row 3 Bento Grid)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "GLOBAL PRESENCE & VOICE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Zero-Latency Speech Engine",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Quick Language Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("EN", "ES", "HI", "FR", "ZH").forEach { lang ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF49454F))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(lang, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Voice audio synthesize trigger
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            if (isSpeaking) {
                                viewModel.stopSpeaking()
                            } else {
                                viewModel.speakText("Cognitive speech transmission online. Say anything inside the Generative chat terminal.")
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Filled.VolumeUp else Icons.Filled.PlayArrow,
                        contentDescription = "Speaker Trigger",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Siri Style wave when speaking
        if (isSpeaking) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F))
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SYNTHESIZING TELEMETRY AUDIO...", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    SiriVoiceWave(isSpeaking = true, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        // Mini Apps Module (Row 4 Bento Grid)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.activeTab.value = "apps" },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1D192B))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "DYNAMIC APP MATRIX",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Apps-of-Apps Vault",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Generate and deploy $allDynamicApps sandboxed micro-tools.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = "Navigate to Apps",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// --- 2. Advanced Generative Chat Screen (Claude Pro / Gemini Advanced style Chat) ---
@Composable
fun GenerativeChatScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentEmotion by viewModel.currentEmotion.collectAsState()
    val activeLanguage by viewModel.currentLanguage.collectAsState()
    val modelMode by viewModel.modelMode.collectAsState()
    val uiPreset by viewModel.uiPreset.collectAsState() // Dynamic ChatGPT/Gemini/Claude Hybrid Switcher
    val isGenerating by viewModel.isGenerating.collectAsState()
    val chatInput by viewModel.chatInput.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val chatSessions by viewModel.chatSessions.collectAsState()
    val selectedSession by viewModel.selectedSession.collectAsState()
    val activeThoughts by viewModel.activeThoughtProcess.collectAsState()
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Dynamic Visual Presets Colors & Accents
    val presetAccentColor = when (uiPreset) {
        "chatgpt" -> Color(0xFF10A37F) // ChatGPT Emerald Green
        "claude" -> Color(0xFFE28A63)  // Claude Sunset Amber
        else -> Color(0xFF7F9CF5)      // Gemini Star Indigo
    }
    
    val presetContainerBg = when (uiPreset) {
        "chatgpt" -> Color(0xFF1F2023) // ChatGPT Obsidian Black
        "claude" -> Color(0xFF1E1A17)  // Claude Deep Cocoa
        else -> Color(0xFF131316)      // Gemini Space Navy
    }

    val presetCardBg = when (uiPreset) {
        "chatgpt" -> Color(0xFF2B2C2F) // Slate Glass
        "claude" -> Color(0xFF26211E)  // Clay Dark Card
        else -> Color(0xFF232329)      // Space Grey Card
    }

    val presetHeaderBg = when (uiPreset) {
        "chatgpt" -> Color(0xFF1B1B1E)
        "claude" -> Color(0xFF1A1614)
        else -> Color(0xFF18181D)
    }

    val presetNameSuffix = when (uiPreset) {
        "chatgpt" -> "ChatGPT Core"
        "claude" -> "Claude Core"
        else -> "Gemini Core"
    }

    // Speech recognizer setup for Voice actions
    var sttRequestActive by remember { mutableStateOf(false) }
    var voiceErrorOccured by remember { mutableStateOf(false) }

    val speechRecognizer = remember {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                SpeechRecognizer.createSpeechRecognizer(context)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    val sttIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
    }

    val speechListener = remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {
                viewModel.isListening.value = true
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                viewModel.isListening.value = false
                sttRequestActive = false
            }
            override fun onError(error: Int) {
                viewModel.isListening.value = false
                sttRequestActive = false
                voiceErrorOccured = true
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0]
                    viewModel.chatInput.value = spokenText
                    viewModel.sendMessage()
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    DisposableEffect(Unit) {
        speechRecognizer?.setRecognitionListener(speechListener)
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    // Auto scroll down when messages scale
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(chatMessages.size - 1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(presetContainerBg)
            .testTag("generative_chat_root")
    ) {
        // Chat workspace header with config toggles
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = presetHeaderBg)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Session selector triggered via pop dropdown
                    var showSessionMenu by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1C1B1F))
                            .clickable { showSessionMenu = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.ChatBubbleOutline, null, tint = presetAccentColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = selectedSession?.title ?: "No session selected",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 140.dp)
                        )
                        Icon(Icons.Filled.ArrowDropDown, null, tint = Color.White)

                        DropdownMenu(
                            expanded = showSessionMenu,
                            onDismissRequest = { showSessionMenu = false },
                            modifier = Modifier.background(presetCardBg)
                        ) {
                            chatSessions.forEach { sess ->
                                DropdownMenuItem(
                                    text = { Text(sess.title, fontSize = 12.sp, color = Color.White) },
                                    onClick = {
                                        viewModel.selectSession(sess)
                                        showSessionMenu = false
                                    }
                                )
                            }
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                            DropdownMenuItem(
                                text = { Text("+ New Workspace", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = presetAccentColor) },
                                onClick = {
                                    viewModel.createNewSession("Workspace Session ${chatSessions.size + 1}")
                                    showSessionMenu = false
                                }
                            )
                        }
                    }

                    // Preset Title badge (ChatGPT + Gemini + Claude Hybrid)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(presetAccentColor)
                        )
                        Text(
                            text = presetNameSuffix,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    // Quick deletion of active session
                    IconButton(
                        onClick = {
                            selectedSession?.let { viewModel.deleteSession(it) }
                        }
                    ) {
                        Icon(Icons.Filled.Delete, "Delete Session", tint = Color.Red.copy(alpha = 0.7f))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Modern 3-Way Selector representing ChatGPT + Gemini + Claude Hybrid AI
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF121214))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val presets = listOf(
                        Triple("chatgpt", "ChatGPT", Color(0xFF10A37F)),
                        Triple("gemini", "Gemini AI", Color(0xFF7F9CF5)),
                        Triple("claude", "Claude.ai", Color(0xFFE28A63))
                    )
                    presets.forEach { (id, name, color) ->
                        val active = uiPreset == id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) color.copy(alpha = 0.15f) else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (active) color.copy(alpha = 0.8f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.uiPreset.value = id }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (active) color else Color.Gray.copy(alpha = 0.5f))
                                )
                                Text(
                                    text = name,
                                    fontSize = 11.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                    color = if (active) Color.White else Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Config bars inside Header tab
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Emotion Config
                    var emotionMenuOpen by remember { mutableStateOf(false) }
                    FilterChip(
                        selected = true,
                        onClick = { emotionMenuOpen = true },
                        label = { Text("EQ: $currentEmotion", fontSize = 10.sp) },
                        leadingIcon = { Icon(Icons.Filled.Favorite, null, modifier = Modifier.size(12.dp)) }
                    )
                    DropdownMenu(expanded = emotionMenuOpen, onDismissRequest = { emotionMenuOpen = false }) {
                        listOf("Analytical", "Empathetic", "Witty", "Creative", "Encouraging").forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    viewModel.currentEmotion.value = item
                                    emotionMenuOpen = false
                                }
                            )
                        }
                    }

                    // 2. Language Context Config
                    var langMenuOpen by remember { mutableStateOf(false) }
                    FilterChip(
                        selected = true,
                        onClick = { langMenuOpen = true },
                        label = { Text("Trans: $activeLanguage", fontSize = 10.sp) },
                        leadingIcon = { Icon(Icons.Filled.Translate, null, modifier = Modifier.size(12.dp)) }
                    )
                    DropdownMenu(expanded = langMenuOpen, onDismissRequest = { langMenuOpen = false }) {
                        listOf("English", "Spanish", "Hindi", "French", "Simplified Chinese", "Japanese", "German", "Arabic").forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    viewModel.currentLanguage.value = item
                                    langMenuOpen = false
                                }
                            )
                        }
                    }

                    // 3. Model Level
                    var modelMenuOpen by remember { mutableStateOf(false) }
                    FilterChip(
                        selected = true,
                        onClick = { modelMenuOpen = true },
                        label = { Text(if (modelMode.contains("pro")) "Pro Engine" else "Flash Engine", fontSize = 10.sp) },
                        leadingIcon = { Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(12.dp)) }
                    )
                    DropdownMenu(expanded = modelMenuOpen, onDismissRequest = { modelMenuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("v3.5 Flash (Instant)") },
                            onClick = {
                                viewModel.modelMode.value = "gemini-3.5-flash"
                                modelMenuOpen = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("v3.1 Pro (Heavy Logic)") },
                            onClick = {
                                viewModel.modelMode.value = "gemini-3.1-pro-preview"
                                modelMenuOpen = false
                            }
                        )
                    }
                }
            }
        }

        // Chat logs
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (chatMessages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .border(BorderStroke(1.dp, presetAccentColor.copy(alpha = 0.3f)), RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = presetCardBg)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Display the premium generated hybrid workspace visual image!
                            Image(
                                painter = painterResource(id = com.example.R.drawable.img_hybrid_workspace),
                                contentDescription = "OmniAI Unified Workspace Concept",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "Welcome to OmniAI Workspace",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Text(
                                text = "A supreme, secure intelligence uniting the strengths of ChatGPT, Gemini, and Claude into a unified local workspace. No setup hassle, absolute privacy.",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Visual indication of current hybrid setting
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(presetAccentColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(presetAccentColor)
                                )
                                Text(
                                    text = "Ready: $presetNameSuffix",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = presetAccentColor
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(chatMessages) { msg ->
                        ChatBubble(msg, uiPreset)
                    }

                    // Thinking State Drawer
                    if (isGenerating && activeThoughts.isNotEmpty()) {
                        item {
                            ThinkingProcessWidget(activeThoughts, uiPreset)
                        }
                    }
                }
            }
        }

        // Dialog for Speech Listening visualizer
        if (sttRequestActive) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("SPEECH INTELLIGENCE ACTIVE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = presetAccentColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    SiriVoiceWave(isSpeaking = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Speak now. Translating to input instantly...", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = {
                        try {
                            speechRecognizer?.stopListening()
                        } catch (e: Exception) {}
                        sttRequestActive = false
                    }) {
                        Text("Cancel", color = Color.Red)
                    }
                }
            }
        }

        // Input chat tray
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(presetContainerBg.copy(alpha = 0.95f))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(presetCardBg)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Voice microphone setup
                IconButton(
                    onClick = {
                        val activity = context as? Activity
                        if (activity != null && speechRecognizer != null) {
                            try {
                                speechRecognizer.startListening(sttIntent)
                                sttRequestActive = true
                            } catch (e: Exception) {
                                sttRequestActive = false
                                Toast.makeText(context, "Microphone service unavailable", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Voice input not supported in sandbox emulator", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(Icons.Filled.Mic, "Voice actions", tint = presetAccentColor)
                }

                TextField(
                    value = chatInput,
                    onValueChange = { viewModel.chatInput.value = it },
                    placeholder = { Text("Ask Omni-AI anything...", fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_textfield"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { viewModel.sendMessage() })
                )

                // TTS Active indicator trigger speaker toggle
                val isSpeakingNow by viewModel.isSpeaking.collectAsState()
                IconButton(onClick = {
                    if (isSpeakingNow) viewModel.stopSpeaking() else {
                        if (chatMessages.isNotEmpty()) {
                            viewModel.speakText(chatMessages.last().content)
                        } else {
                            viewModel.speakText("Transmit connection ready.")
                        }
                    }
                }) {
                    Icon(
                        imageVector = if (isSpeakingNow) Icons.Filled.VolumeUp else Icons.Filled.VolumeMute,
                        contentDescription = "TTS Active",
                        tint = if (isSpeakingNow) presetAccentColor else Color.Gray
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isGenerating) Color.Gray else presetAccentColor)
                        .clickable(enabled = !isGenerating) { viewModel.sendMessage() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowUpward,
                        contentDescription = "Send",
                        tint = if (uiPreset == "claude" && !isGenerating) Color.Black else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// --- Chat Bubble UI ---
@Composable
fun ChatBubble(message: ChatMessage, uiPreset: String) {
    val isUser = message.role == "user"
    val align = if (isUser) Alignment.End else Alignment.Start
    val containerBg = if (isUser) {
        when (uiPreset) {
            "chatgpt" -> Color(0xFF10A37F)
            "claude" -> Color(0xFFE28A63)
            else -> MaterialTheme.colorScheme.primary
        }
    } else {
        when (uiPreset) {
            "chatgpt" -> Color(0xFF2B2C2F)
            "claude" -> Color(0xFF26211E)
            else -> Color(0xFF2B2930)
        }
    }
    val textColors = if (isUser) {
        if (uiPreset == "claude") Color.Black else Color.White
    } else {
        Color.White
    }
    val borderStroke = if (isUser) null else {
        when (uiPreset) {
            "chatgpt" -> BorderStroke(1.dp, Color(0xFF494A54))
            "claude" -> BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.2f))
            else -> BorderStroke(1.dp, Color(0xFF49454F))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_bubble_${message.id}"),
        horizontalAlignment = align
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!isUser) {
                Text(
                    text = when (message.emotion) {
                        "Analytical" -> "🔬 OmniAI Logic"
                        "Empathetic" -> "❤️ OmniAI Serene"
                        "Witty" -> "🍿 OmniAI Witty"
                        "Creative" -> "🌌 OmniAI Creative"
                        "Encouraging" -> "🔥 OmniAI Catalyst"
                        else -> "🤖 OmniAI Core"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (uiPreset) {
                        "chatgpt" -> Color(0xFF10A37F)
                        "claude" -> Color(0xFFE28A63)
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            } else {
                Text("User Command", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Surface(
            color = containerBg,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            border = borderStroke,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = message.content,
                    color = textColors,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

// --- Thinking Process Widget (Claude Pro Thinking block) ---
@Composable
fun ThinkingProcessWidget(steps: List<String>, uiPreset: String) {
    val cardBg = when (uiPreset) {
        "chatgpt" -> Color(0xFF2B2C2F)
        "claude" -> Color(0xFF26211E)
        else -> Color(0xFF1D192B)
    }
    val accentColor = when (uiPreset) {
        "chatgpt" -> Color(0xFF10A37F)
        "claude" -> Color(0xFFE28A63)
        else -> MaterialTheme.colorScheme.primary
    }
    val titleText = when (uiPreset) {
        "chatgpt" -> "Thinking..."
        "claude" -> "Thinking Process..."
        else -> "Formulating answer..."
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = accentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = titleText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            steps.forEach { step ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Check, null, tint = Color(0xFF10B981), modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(step, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
    }
}

// --- 3. Extreme Coding Screen ("IDE Workspace") ---
@Composable
fun ExtremeCodingScreen(viewModel: MainViewModel) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val isGenerating by viewModel.isGenerating.collectAsState()
    val projectTitle by viewModel.codeProjectTitle.collectAsState()
    val projectDesc by viewModel.codeProjectDesc.collectAsState()
    val codePrompt by viewModel.codeProjectPrompt.collectAsState()
    val generatedCode by viewModel.generatedCode.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("extreme_coding_root"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Workspace Header Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1D192B)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "EXTREME CODING PROTOCOL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Claude Pro AI Architect",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Optimized algorithms, modular system scripts, and clean architectural paradigms directly compiled into localized code vault matrices.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    lineHeight = 18.sp
                )
            }
        }

        // Design Specs Fields Inputs
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("SPEC PROJECT METADATA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                OutlinedTextField(
                    value = projectTitle,
                    onValueChange = { viewModel.codeProjectTitle.value = it },
                    label = { Text("Code Project Title") },
                    modifier = Modifier.fillMaxWidth().testTag("code_title_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFF49454F)
                    )
                )

                OutlinedTextField(
                    value = projectDesc,
                    onValueChange = { viewModel.codeProjectDesc.value = it },
                    label = { Text("Short Description") },
                    modifier = Modifier.fillMaxWidth().testTag("code_desc_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFF49454F)
                    )
                )

                OutlinedTextField(
                    value = codePrompt,
                    onValueChange = { viewModel.codeProjectPrompt.value = it },
                    label = { Text("Enter algorithm or script prompt...") },
                    modifier = Modifier.fillMaxWidth().height(100.dp).testTag("code_prompt_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFF49454F)
                    ),
                    maxLines = 5
                )

                // Prompt examples presets
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(
                        "Dijksthra visual flow chart in Kotlin",
                        "Clean Architecture Retrofit Interface Client",
                        "Responsive Canvas Sine Wave Drawer",
                        "Custom Binary Tree Depth Solver"
                    )
                    presets.forEach { preset ->
                        AssistChip(
                            onClick = { viewModel.codeProjectPrompt.value = preset },
                            label = { Text(preset, fontSize = 10.sp) }
                        )
                    }
                }

                Button(
                    onClick = { viewModel.generateSolution() },
                    modifier = Modifier.fillMaxWidth().testTag("compile_code_button"),
                    enabled = !isGenerating,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Synthesizing Code...")
                    } else {
                        Icon(Icons.Filled.IntegrationInstructions, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compile Code Architecture", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Output Code Sandbox Layout
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F)),
            border = BorderStroke(1.dp, Color(0xFF49454F)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column {
                // Code view titlebar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2B2930))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "terminal - output.kt",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            modifier = Modifier.size(24.dp),
                            onClick = {
                                clipboardManager.setText(AnnotatedString(generatedCode))
                                Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Filled.ContentCopy, "Copy Code", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        }

                        IconButton(
                            modifier = Modifier.size(24.dp),
                            onClick = {
                                viewModel.saveCodeToRepository()
                                Toast.makeText(context, "Project deployed to Local Vault", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Filled.Save, "Save Code", tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Main code viewport
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .verticalScroll(rememberScrollState(), enabled = false)
                        .padding(16.dp)
                ) {
                    Text(
                        text = generatedCode,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFFD0BCFF),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Coding Vault list
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "NEURAL ARCHIVE VAULT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            if (allProjects.isEmpty()) {
                Text(
                    "No saved algorithm models inside Local Session Vault yet.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                allProjects.forEach { proj ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(proj.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(proj.description, fontSize = 11.sp, color = Color.LightGray)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(onClick = { viewModel.generatedCode.value = proj.code }) {
                                    Icon(Icons.Filled.Launch, "Load Project", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { viewModel.deleteProject(proj) }) {
                                    Icon(Icons.Filled.Delete, "Delete Project", tint = Color.Red.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// --- 4. Apps Selector & Compiler Platform ("Apps of Apps" Sandbox Workspace) ---
@Composable
fun AppsOfAppsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val isGenerating by viewModel.isGenerating.collectAsState()
    val appPrompt by viewModel.appPrompt.collectAsState()
    val statusText by viewModel.appCreationStatus.collectAsState()
    val compiledConfig by viewModel.generatedAppConfig.collectAsState()
    val widgetStates by viewModel.appWidgetStates.collectAsState()
    val allDynamicApps by viewModel.allDynamicApps.collectAsState()
    val activeApp by viewModel.activeDynamicApp.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("apps_matrix_root"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Builder Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1D192B)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "APPS-OF-APPS MULTIVERSE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Dynamic Tool Synthesis",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Unshackle from static models. Describe any utility or interaction card, and OmniAI will synthesize it instantly into reactive UI grids.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    lineHeight = 18.sp
                )
            }
        }

        // Compiler Generator Form input
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("COGNITIVE DESIGN INPUT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                OutlinedTextField(
                    value = appPrompt,
                    onValueChange = { viewModel.appPrompt.value = it },
                    label = { Text("Describe details of the app to build...") },
                    placeholder = { Text("E.g., A dynamic calorie ledger containing increments, water gauges, and workout soreness scale.") },
                    modifier = Modifier.fillMaxWidth().height(100.dp).testTag("app_prompt_textfield"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFF49454F)
                    ),
                    maxLines = 5
                )

                // Presets
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val appPresets = listOf(
                        "Water Tracker Counter with simple reset button",
                        "Countdown rest stopwatch timer widget",
                        "Daily rating tracker scale for mental calmness",
                        "Chore checklist widget with priority state"
                    )
                    appPresets.forEach { preset ->
                        AssistChip(
                            onClick = { viewModel.appPrompt.value = preset },
                            label = { Text(preset, fontSize = 10.sp) }
                        )
                    }
                }

                Button(
                    onClick = { viewModel.generateDynamicApp() },
                    modifier = Modifier.fillMaxWidth().testTag("app_compile_submit_button"),
                    enabled = !isGenerating,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compiling UI...")
                    } else {
                        Icon(Icons.Filled.BuildCircle, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Assemble Dynamic App Work", fontWeight = FontWeight.Bold)
                    }
                }

                statusText?.let {
                    Text(
                        text = it,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Live App Sandbox Canvas rendering
        compiledConfig?.let { cfg ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .testTag("dynamic_app_sandbox"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F))
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Titlebar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(cfg.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(cfg.description, fontSize = 11.sp, color = Color.LightGray)
                        }

                        IconButton(
                            onClick = {
                                viewModel.saveDynamicAppToDb()
                            }
                        ) {
                            Icon(Icons.Filled.CloudUpload, "Deploy workspace", tint = Color(0xFF10B981))
                        }
                    }

                    Divider(color = Color(0xFF49454F))

                    // Dynamically build widgets based on config parsed loop
                    cfg.widgets.forEach { wdg ->
                        DynamicSandboxWidget(
                            widget = wdg,
                            currentState = widgetStates[wdg.id] ?: "",
                            onUpdate = { key, newVal -> viewModel.updateWidgetState(key, newVal) }
                        )
                    }
                }
            }
        }

        // Apps Repository Workspace Storage
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "SANDBOX MICRO-APP VAULT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            if (allDynamicApps.isEmpty()) {
                Text(
                    "No custom compiled apps inside sandbox storage.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                allDynamicApps.forEach { dApp ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectDynamicApp(dApp) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (activeApp?.id == dApp.id) Color(0xFF332D41) else Color(0xFF2B2930)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(dApp.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(dApp.description, fontSize = 11.sp, color = Color.LightGray)
                            }
                            IconButton(onClick = { viewModel.deleteDynamicApp(dApp) }) {
                                Icon(Icons.Filled.Delete, "Delete Workspace App", tint = Color.Red.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// --- Dynamic Parser Sandbox Widget renderer inside compiled state ---
@Composable
fun DynamicSandboxWidget(
    widget: DynamicWidget,
    currentState: String,
    onUpdate: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
        border = BorderStroke(1.dp, Color(0xFF49454F)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(widget.label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            widget.hint?.let {
                Text(it, fontSize = 9.sp, color = Color.LightGray, modifier = Modifier.padding(vertical = 2.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (widget.type) {
                "status_info" -> {
                    Text(
                        text = currentState.ifEmpty { widget.initialValue ?: "N/A" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                "button_counter" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val limitCount = currentState.ifEmpty { widget.initialValue ?: "0" }.toIntOrNull() ?: 0
                        Text(
                            text = "Counter: $limitCount",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilledTonalButton(
                                onClick = { onUpdate(widget.id, (limitCount + 1).toString()) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("+1")
                            }
                            OutlinedButton(
                                onClick = { onUpdate(widget.id, "0") },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                            ) {
                                Text("Reset")
                            }
                        }
                    }
                }

                "text_field" -> {
                    OutlinedTextField(
                        value = currentState,
                        onValueChange = { onUpdate(widget.id, it) },
                        placeholder = { Text(widget.placeholder ?: "Enter value...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF49454F)
                        )
                    )
                }

                "checklist" -> {
                    val listOptions = widget.options ?: listOf("Completed Tasks")
                    listOptions.forEach { opt ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val checked = currentState.contains(opt)
                                    val updated = if (checked) {
                                        currentState.replace(opt, "").replace(",,", ",").trim()
                                    } else {
                                        "$currentState,$opt"
                                    }
                                    onUpdate(widget.id, updated)
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            val isChecked = currentState.contains(opt)
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    val updated = if (isChecked) {
                                        currentState.replace(opt, "").replace(",,", ",").trim()
                                    } else {
                                        "$currentState,$opt"
                                    }
                                    onUpdate(widget.id, updated)
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(opt, fontSize = 12.sp, color = if (isChecked) Color.Gray else Color.White)
                        }
                    }
                }

                "quick_timer" -> {
                    var timerValue by remember { mutableStateOf(currentState.ifEmpty { widget.initialValue ?: "60" }.toIntOrNull() ?: 60) }
                    var timerRunning by remember { mutableStateOf(false) }

                    LaunchedEffect(timerRunning) {
                        if (timerRunning) {
                            while (timerValue > 0 && timerRunning) {
                                kotlinx.coroutines.delay(1000)
                                timerValue -= 1
                                onUpdate(widget.id, timerValue.toString())
                            }
                            if (timerValue == 0) {
                                timerRunning = false
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Remaining: $timerValue secs",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { timerRunning = !timerRunning },
                                colors = ButtonDefaults.buttonColors(containerColor = if (timerRunning) Color.Red else Color(0xFF10B981))
                            ) {
                                Text(if (timerRunning) "Pause" else "Start")
                            }
                            OutlinedButton(
                                onClick = {
                                    timerRunning = false
                                    timerValue = widget.initialValue?.toIntOrNull() ?: 60
                                    onUpdate(widget.id, timerValue.toString())
                                }
                            ) {
                                Text("Reset")
                            }
                        }
                    }
                }

                "rating_scale" -> {
                    val rateLimit = 5
                    val currentRate = currentState.ifEmpty { widget.initialValue ?: "3" }.toIntOrNull() ?: 3
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (i in 1..rateLimit) {
                            val active = i <= currentRate
                            Icon(
                                imageVector = if (active) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = "Rate $i",
                                tint = if (active) Color(0xFFF59E0B) else Color.Gray,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable {
                                        onUpdate(widget.id, i.toString())
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- 5. Telemetry & Settings Screen ---
@Composable
fun TelemetrySettingsScreen(viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("NEURAL MATRIX TELEMETRY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Version Configuration", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Text("OmniAI Engine: V4.0.2 Stable Build", fontSize = 12.sp, color = Color.LightGray)
                Text("Workspace Application ID: com.aistudio.omniai.workspace", fontSize = 10.sp, color = Color.Gray)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1D192B)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("OMNI COGNITIVE BOUNDARIES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Microphone Sync", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Voice dictation input logic", fontSize = 11.sp, color = Color.LightGray)
                    }
                    Switch(checked = true, onCheckedChange = {})
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Ambient Voice Audio", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Text-To-Speech responsive trigger option", fontSize = 11.sp, color = Color.LightGray)
                    }
                    Switch(checked = true, onCheckedChange = {})
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Local Vault Backup", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Room Database transaction sync rules", fontSize = 11.sp, color = Color.LightGray)
                    }
                    Switch(checked = true, onCheckedChange = {})
                }
            }
        }

        // Warning message required by the Secret Management Skill
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF3C1F1F)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Decompilation warning", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Security Warning: I have included your API keys in the generated APK file for this prototype. Please be aware that Android APKs can be easily decompiled, and these keys can be extracted by anyone who has access to the file. Do not share this APK file publicly or with unauthorized individuals to prevent potential misuse.",
                    fontSize = 10.sp,
                    color = Color.LightGray,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

// --- Custom bottom navigation ---
@Composable
fun CustomBottomNavigation(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars) // Fix system navigation bar safe area
            .testTag("custom_bottom_navigation_bar"),
        containerColor = Color(0xFF1C1B1F),
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = activeTab == "dashboard",
            onClick = { onTabSelected("dashboard") },
            label = { Text("Dashboard", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
            icon = {
                Icon(
                    imageVector = if (activeTab == "dashboard") Icons.Filled.GridView else Icons.Outlined.GridView,
                    contentDescription = "Dashboard"
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF381E72),
                selectedTextColor = Color.White,
                indicatorColor = Color(0xFFD0BCFF),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            ),
            modifier = Modifier.testTag("nav_item_dashboard")
        )

        NavigationBarItem(
            selected = activeTab == "chat",
            onClick = { onTabSelected("chat") },
            label = { Text("Chat", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
            icon = {
                Icon(
                    imageVector = if (activeTab == "chat") Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Chat"
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF381E72),
                selectedTextColor = Color.White,
                indicatorColor = Color(0xFFD0BCFF),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            ),
            modifier = Modifier.testTag("nav_item_chat")
        )

        NavigationBarItem(
            selected = activeTab == "workspace",
            onClick = { onTabSelected("workspace") },
            label = { Text("Vault", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
            icon = {
                Icon(
                    imageVector = if (activeTab == "workspace") Icons.Filled.Code else Icons.Outlined.Code,
                    contentDescription = "Workspace"
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF381E72),
                selectedTextColor = Color.White,
                indicatorColor = Color(0xFFD0BCFF),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            ),
            modifier = Modifier.testTag("nav_item_workspace")
        )

        NavigationBarItem(
            selected = activeTab == "apps",
            onClick = { onTabSelected("apps") },
            label = { Text("Apps", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
            icon = {
                Icon(
                    imageVector = if (activeTab == "apps") Icons.Filled.AppSettingsAlt else Icons.Outlined.AppSettingsAlt,
                    contentDescription = "Apps"
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF381E72),
                selectedTextColor = Color.White,
                indicatorColor = Color(0xFFD0BCFF),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            ),
            modifier = Modifier.testTag("nav_item_apps")
        )

        NavigationBarItem(
            selected = activeTab == "settings",
            onClick = { onTabSelected("settings") },
            label = { Text("Config", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
            icon = {
                Icon(
                    imageVector = if (activeTab == "settings") Icons.Filled.Settings else Icons.Outlined.Settings,
                    contentDescription = "Settings"
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF381E72),
                selectedTextColor = Color.White,
                indicatorColor = Color(0xFFD0BCFF),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            ),
            modifier = Modifier.testTag("nav_item_settings")
        )
    }
}
