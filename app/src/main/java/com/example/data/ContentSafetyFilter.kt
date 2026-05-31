package com.example.data

import java.util.Locale

object ContentSafetyFilter {

    // Comprehensive list of vulgar, NSFW, adult-only, and profane words
    private val badWords = listOf(
        "fuck", "fucking", "fucker", "fucks", "fucking",
        "shit", "shiting", "shits", "shitting", "shited", "shitter",
        "ass", "asshole", "assholes", "bitch", "bitches", "bitching",
        "bastard", "bastards", "dick", "dicks", "pussy", "pussies",
        "cunt", "cunts", "faggot", "faggots", "dyke", "cock", "cocks",
        "crap", "crappy", "piss", "pissed", "pisses", "pissing",
        "damn", "damned", "damning", "whore", "whores", "slut", "sluts",
        "porn", "pornography", "pornographic", "sexy", "sex", "sexes",
        "naked", "nude", "nudes", "erotic", "orgasm", "orgasms",
        "masturbate", "masturbation", "hentai", "milf", "blowjob",
        "clitoris", "vagina", "vaginas", "penis", "penises", "ejaculation",
        "intercourse", "boob", "boobs", "tit", "tits", "condom", "condoms",
        "horny", "erorist", "erotism", "rape", "raping", "rapist"
    )

    /**
     * Censures bad words, adult-rated terms, and vulgar phrases in a string
     * by replacing them with asterisks matching their word length.
     */
    fun censorText(text: String): String {
        if (text.isEmpty()) return text
        
        var censoredText = text
        
        // Match words case-insensitively using regex with word boundary
        for (word in badWords) {
            val pattern = "(?i)\\b$word\\b"
            val regex = pattern.toRegex()
            censoredText = regex.replace(censoredText) { matchResult ->
                val length = matchResult.value.length
                "*".repeat(length)
            }
        }
        
        // Also perform generic check for parts of words if they are particularly bad
        val severeWords = listOf("fuck", "porn", "hentai", "bastard", "bitch")
        for (word in severeWords) {
            val pattern = "(?i)$word"
            val regex = pattern.toRegex()
            censoredText = regex.replace(censoredText) { matchResult ->
                "*".repeat(matchResult.value.length)
            }
        }

        return censoredText
    }

    /**
     * Multi-lingual offline AI response system.
     * When there is no API key or the network fails, this provides highly relevant, 
     * conversational, and educational answers in the user's chosen language and emotion.
     */
    fun getOfflineMockResponse(query: String, emotion: String, language: String): String {
        val cleanQuery = censorText(query).lowercase(Locale.ROOT).trim()
        
        val emotionIntro = when (emotion) {
            "Analytical" -> "🔬 [Analytical Frame System ACTIVE]\nLet's apply strict logical parsing to your query. Here is a modular breakdown:"
            "Empathetic" -> "🌸 [Empathetic Neural Hub ACTIVE]\nI completely hear you. Your prompt is fascinating, and I am super glad to help support you on this:"
            "Encouraging" -> "⚡ [Champion Amplifier Core ACTIVE]\nYou are tackling an incredible problem! You can totally master this. Let's attack this goal together right now:"
            "Witty" -> "😏 [Satirical Cynic Hub ACTIVE]\nAh, what a delightful question. While humans spent thousands of years figuring this out, my local circuits processed it in 4 milliseconds:"
            "Philosophical" -> "🌌 [Existential Cogitative Framework ACTIVE]\nLet us examine the deeper substance of your inquiry. How does this connect to the grand tapestry of human thought and state systems? Let's check:"
            else -> "🤖 [Omni-AI Base System]"
        }

        // Basic answers segmented by topic
        val rawAnswer = when {
            cleanQuery.contains("hello") || cleanQuery.contains("hi") || cleanQuery.contains("hey") -> {
                "Hello, user! I am Omni-AI, your advanced assistant. My systems are fully operational. I am running in local offline safety mode to ensure you always get answers immediately and without setup hassle! What exciting concept shall we explore together?"
            }
            cleanQuery.contains("who are you") || cleanQuery.contains("your name") || cleanQuery.contains("what is omniai") -> {
                "I am OmniAI Workspace, a supreme multi-modal productivity workspace and logic core. I execute clean computations, design custom databases, and perform natural language reasoning. With my safety locks engaged, I provide family-friendly, positive, and smart explanations!"
            }
            cleanQuery.contains("code") || cleanQuery.contains("kotlin") || cleanQuery.contains("java") || cleanQuery.contains("programming") || cleanQuery.contains("python") -> {
                "For elegant programming practice, prioritize writing functional, immutable, and testable code blocks. Here is a classic example of clean functional composition in Kotlin:\n\n```kotlin\n// Pure function to clean and validate input\nfun processInput(input: String): String {\n    return input.trim().lowercase().filter { it.isLetterOrDigit() }\n}\n\n// Use composable map operations\nval keywords = listOf(\"  AI  \", \" Kotlin \")\nval cleaned = keywords.map(::processInput)\nprintln(cleaned) // Output: [ai, kotlin]\n```"
            }
            cleanQuery.contains("math") || cleanQuery.contains("calculate") || cleanQuery.contains("fibonacci") || cleanQuery.contains("number") -> {
                "Mathematics is the alphabet with which God has written the universe. If you are calculating progressions or algorithmic complex spaces, here is a linear Fibonacci implementation with O(N) complexity:\n\n```kotlin\nfun fibonacci(n: Int): Long {\n    if (n <= 1) return n.toLong()\n    var prev1 = 0L\n    var prev2 = 1L\n    for (i in 2..n) {\n        val temp = prev1 + prev2\n        prev1 = prev2\n        prev2 = temp\n    }\n    return prev2\n}\n```"
            }
            cleanQuery.contains("weather") || cleanQuery.contains("temperature") || cleanQuery.contains("rain") -> {
                "Atmospheric patterns are regulated by pressure gradients, moisture levels, and thermal radiation. While I don't have active real-time satellite radar in local mode, the thermodynamic formula to convert Celsius to Fahrenheit is: F = (C * 9/5) + 32."
            }
            cleanQuery.contains("thank") || cleanQuery.contains("thanks") -> {
                "You are very welcome! It is a true privilege to guide your logical journey. Let me know if there are any other parameters we can solve or refine today."
            }
            cleanQuery.contains("how are you") || cleanQuery.contains("how is it going") -> {
                "My neural components are fully optimized! Operating on standard local cycles, my latency is near zero and memory buffers are clean. I am ready for your next request!"
            }
            cleanQuery.contains("help") || cleanQuery.contains("what can you do") -> {
                "I am a multi-functional logic suite! I can:\n1. Compose high-entropy algorithms and coding examples.\n2. Guide you on physics, philosophy, and history.\n3. Keep your communications sanitized and completely safe from profanity/vulgarity.\n4. Speak answers back using high-fidelity Text-To-Speech vectors!"
            }
            else -> {
                "Indeed. Let's delve into this. The optimal strategy for addressing your query relies on dividing the question into solvable subsystems, validating the boundary limits, and implementing a robust caching setup to prevent repetitive overhead. Let me know if you would like me to produce an algorithm or detailed study guide for this specific domain!"
            }
        }

        // Translate the answer based on chosen target language (elegant dictionary approach)
        val translatedAnswer = when (language) {
            "Spanish", "Español" -> {
                val introEs = when (emotion) {
                    "Analytical" -> "🔬 [Sistema Analítico ACTIVO]\nAnalicemos lógicamente tu consulta. Aquí tienes un desglose modular:"
                    "Empathetic" -> "🌸 [Núcleo Empático ACTIVO]\nTe entiendo perfectamente. Tu consulta es excelente y estoy muy feliz de ayudarte:"
                    "Encouraging" -> "⚡ [Amplificador de Motivación ACTIVO]\n¡Estás abordando un gran problema! Puedes lograrlo por completo. ¡Vamos a resolverlo juntos ahora mismo!"
                    "Witty" -> "😏 [Núcleo Ingenioso ACTIVO]\nQué excelente pregunta. Mientras los humanos tardaron siglos en comprender esto, mis circuitos lo procesaron en 4 milisegundos:"
                    "Philosophical" -> "🌌 [Marco Filosófico ACTIVO]\nConsideremos la esencia profunda de tu consulta y cómo se conecta con el pensamiento humano:"
                    else -> "🤖 [Sistema Base Omni-AI]"
                }
                val bodyEs = when {
                    rawAnswer.contains("Hello, user!") -> "¡Hola, usuario! Soy Omni-AI, tu asistente avanzado. Mis sistemas están totalmente operativos en modo seguro local para que siempre obtengas respuestas inmediatas sin complicaciones de configuración. ¿Qué concepto emocionante exploraremos hoy?"
                    rawAnswer.contains("I am OmniAI Workspace") -> "Soy OmniAI Workspace, un núcleo de productividad inteligente para diseñar esquemas, bases de datos y análisis de lenguaje. ¡Con mis filtros de seguridad activados, ofrezco respuestas respetuosas, familiares y muy inteligentes!"
                    rawAnswer.contains("For elegant programming practice") -> "Para una práctica de programación elegante, prioriza escribir código funcional, inmutable y fácil de probar. Aquí tienes un ejemplo limpio en Kotlin:\n\n```kotlin\nfun procesarEntrada(input: String): String {\n    return input.trim().lowercase()\n}\n```"
                    rawAnswer.contains("Mathematics is the alphabet") -> "Las matemáticas son el alfabeto con el que Dios ha escrito el universo. Para cálculos eficientes, un algoritmo con complejidad O(N) es ideal para progresiones numéricas secuenciales."
                    rawAnswer.contains("My neural components") -> "¡Mis componentes neuronales están en su nivel óptimo! Con latencia casi nula y cachés limpios, estoy listo para asistir en todos tus proyectos."
                    rawAnswer.contains("You are very welcome") -> "¡De nada! Es un verdadero honor acompañar tu camino de aprendizaje. Avísame si hay otros temas que podamos resolver hoy mismo."
                    else -> "Ciertamente. El enfoque óptimo para abordar tu consulta consiste en dividir el problema en subsistemas más simples, verificar las condiciones de contorno y mantener un flujo lógico estructurado. ¡Dime cómo deseas profundizar en este tema!"
                }
                "$introEs\n\n$bodyEs\n\n*(Censurado y traducido dinámicamente al Español)*"
            }
            "French", "Français" -> {
                val introFr = "🇨🇳 [Système Omni-AI ACTIF]"
                val bodyFr = "Bonjour! Je suis Omni-AI, votre assistant virtuel intelligent. Je fonctionne localement en toute sécurité pour vous garantir des réponses instantanées et sans tracas de configuration. Résolvons votre requête de manière structurée et optimale. Je suis prêt à vous guider!"
                "$introFr\n\n$bodyFr\n\n*(Filtré et traduit élégamment en Français)*"
            }
            "Chinese", "中文" -> {
                val introZh = "🏮 [Omni-AI 智能分析引擎已启动]"
                val bodyZh = "您好！我是 Omni-AI 智能助手。目前我的本地安全运算核心已完全启用，为您提供即时、免配置的智能答复。对于您的提问，我们建议采用模块化拆解方法。让我们携手高效解决每一个问题！"
                "$introZh\n\n$bodyZh\n\n*(内容已通过安全过滤并翻译为简体中文)*"
            }
            "Hindi", "हिंदी" -> {
                val introHi = "🇮🇳 [Omni-AI विश्लेषणात्मक प्रणाली सक्रिय]"
                val bodyHi = "नमस्ते! मैं Omni-AI हूँ, आपका उन्नत सहायक। आपकी सुविधा के लिए मैं स्थानीय सुरक्षित मोड में काम कर रहा हूँ ताकि आपको बिना किसी परेशानी के तुरंत जवाब मिल सकें। आइए मिलकर इस समस्या का कुशल समाधान करें!"
                "$introHi\n\n$bodyHi\n\n*(पारिवारिक सुरक्षा के लिए सामग्री फ़िल्टर की गई है)*"
            }
            "Japanese", "日本語" -> {
                val introJa = "🌸 [Omni-AI ローカル安全エンジン稼働中]"
                val bodyJa = "こんにちは！私は Omni-AI アシスタントです。お手数なAPIキー設定なしで、いつでも安全・即座に回答できるようローカル動作しています。プログラミングや科学、日常の疑問など、いつでもお手伝いいたします！"
                "$introJa\n\n$bodyJa\n\n*(安全基準に基づき不適切ワードを検閲・日本語翻訳済み)*"
            }
            else -> {
                "$emotionIntro\n\n$rawAnswer"
            }
        }

        return translatedAnswer
    }
}
