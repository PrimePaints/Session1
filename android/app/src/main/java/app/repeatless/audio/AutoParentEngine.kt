package app.repeatless.audio

import android.content.Context
import android.util.Log
import app.repeatless.data.db.PadEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class AutoParentLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val detectedSituation: String,
    val matchedPadLabel: String,
    val confidence: Float,
    val triggered: Boolean,
    val userFeedback: Boolean? = null // true = thumbs up, false = thumbs down
)

data class AutoParentState(
    val isListening: Boolean = false,
    val isAnalyzing: Boolean = false,
    val ambientAmplitude: Float = 0f,
    val sensitivity: Float = 0.60f,
    val cooldownSeconds: Int = 8,
    val autoPlayEnabled: Boolean = true,
    val lastTriggeredLabel: String? = null,
    val logs: List<AutoParentLog> = emptyList(),
    val statusMessage: String = "Auto-Parent Standing By"
)

/**
 * Continuously listens for children's speech and auto-plays the best matching clip.
 *
 * PRIVACY MODEL: microphone audio is transcribed to text ON-DEVICE via
 * [OnDeviceTranscriber]; the raw audio never leaves the phone. Only the resulting
 * transcript (plus a short, locally-derived loudness/tone hint) is sent to Gemini
 * Flash Lite to choose which recorded clip to play. The user's own recordings are
 * only ever read from local storage and played locally.
 */
class AutoParentEngine(
    private val context: Context,
    private val audioPlayer: AudioPlayer
) {
    private val TAG = "AutoParentEngine"

    private val _state = MutableStateFlow(AutoParentState())
    val state: StateFlow<AutoParentState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)
    private var listeningJob: Job? = null

    // On-device speech-to-text. Audio is transcribed locally; only text is sent onward.
    private val transcriber = OnDeviceTranscriber(context)

    private var lastTriggerTimeMs: Long = 0L

    fun toggleListening(
        pads: List<PadEntity>,
        getAudioFile: (PadEntity) -> File?
    ) {
        if (_state.value.isListening) {
            stopListening()
        } else {
            startListening(pads, getAudioFile)
        }
    }

    fun startListening(
        pads: List<PadEntity>,
        getAudioFile: ((PadEntity) -> File?)? = null
    ) {
        val fileResolver: (PadEntity) -> File? = getAudioFile ?: { pad ->
            val file = File(context.filesDir, pad.audioFileName)
            if (file.exists()) file else null
        }

        if (pads.isEmpty()) {
            _state.value = _state.value.copy(statusMessage = "No sound clips recorded to monitor")
            return
        }

        if (!transcriber.isAvailable()) {
            _state.value = _state.value.copy(
                statusMessage = "Speech recognition isn't available on this device"
            )
            return
        }

        stopListeningInternal()

        _state.value = _state.value.copy(
            isListening = true,
            statusMessage = "Listening for vocal inputs..."
        )

        // SpeechRecognizer must be driven on the main thread; the transcriber handles
        // that internally, so we run the loop on the main dispatcher.
        listeningJob = scope.launch(Dispatchers.Main) {
            while (isActive && _state.value.isListening) {
                try {
                    // 1) Capture speech and transcribe it locally. peakRms tracks the
                    //    loudest moment so we can derive an on-device tone hint.
                    var peakRms = 0f
                    val transcript = transcriber.transcribeOnce { norm ->
                        if (norm > peakRms) peakRms = norm
                        _state.value = _state.value.copy(ambientAmplitude = norm)
                    }

                    if (!isActive || !_state.value.isListening) break
                    _state.value = _state.value.copy(ambientAmplitude = 0f)

                    if (transcript.isNullOrBlank()) {
                        _state.value = _state.value.copy(statusMessage = "Listening for vocal inputs...")
                        delay(120)
                        continue
                    }

                    // 2) Local, on-device tone classification from loudness only (no audio leaves).
                    val acoustic = AcousticHeuristic.analyzeAmplitude(peakRms)
                    val situation = if (acoustic.category != AcousticCategory.QUIET_AMBIENT) {
                        "$transcript [tone: ${acoustic.category.name}]"
                    } else {
                        transcript
                    }

                    _state.value = _state.value.copy(
                        isAnalyzing = true,
                        statusMessage = "Analyzing: \"$transcript\"..."
                    )

                    // 3) Text-only match via Gemini Flash Lite (falls back to on-device heuristics).
                    val result = withContext(Dispatchers.IO) {
                        GeminiAudioClassifier.classifySituationText(
                            situationText = situation,
                            availablePads = pads.map { it.label },
                            feedbackRules = getFeedbackRules()
                        )
                    }

                    _state.value = _state.value.copy(isAnalyzing = false)

                    val matchedLabel = result.matchedLabel
                    if (matchedLabel != null && result.confidence >= _state.value.sensitivity) {
                        val now = System.currentTimeMillis()
                        val cooldownMs = _state.value.cooldownSeconds * 1000L
                        val shouldTrigger = _state.value.autoPlayEnabled && (now - lastTriggerTimeMs) >= cooldownMs

                        addLog(
                            AutoParentLog(
                                detectedSituation = transcript,
                                matchedPadLabel = matchedLabel,
                                confidence = result.confidence,
                                triggered = shouldTrigger
                            )
                        )

                        if (shouldTrigger) {
                            lastTriggerTimeMs = now
                            _state.value = _state.value.copy(
                                lastTriggeredLabel = matchedLabel,
                                statusMessage = "Matched '$matchedLabel' (${(result.confidence * 100).toInt()}%) -> Playing Clip!"
                            )

                            val targetPad = pads.firstOrNull {
                                it.label.trim().equals(matchedLabel.trim(), ignoreCase = true)
                            } ?: pads.firstOrNull { pad ->
                                val m = matchedLabel.trim()
                                m.isNotBlank() && (pad.label.contains(m, ignoreCase = true) || m.contains(pad.label, ignoreCase = true))
                            }
                            if (targetPad != null) {
                                val audioFile = fileResolver(targetPad)
                                if (audioFile != null && audioFile.exists()) {
                                    audioPlayer.playPad(targetPad.id, audioFile)
                                }
                            }
                        } else {
                            _state.value = _state.value.copy(
                                statusMessage = if (_state.value.autoPlayEnabled)
                                    "Cooldown active (Skipped duplicate clip)"
                                else
                                    "Match found (auto-play off)"
                            )
                        }
                    } else {
                        _state.value = _state.value.copy(statusMessage = "Listening for vocal inputs...")
                    }

                    delay(120) // brief pause before the next listen cycle
                } catch (e: Exception) {
                    Log.e(TAG, "Error in listening loop", e)
                    _state.value = _state.value.copy(
                        isListening = false,
                        isAnalyzing = false,
                        statusMessage = "Listening paused (Mic busy or unavailable)"
                    )
                    break
                }
            }
        }
    }

    fun simulateTestSituation(
        situationText: String,
        pads: List<PadEntity>,
        getAudioFile: (PadEntity) -> File?
    ) {
        if (pads.isEmpty()) {
            _state.value = _state.value.copy(statusMessage = "No sound clips recorded to test against")
            return
        }

        scope.launch {
            _state.value = _state.value.copy(
                isAnalyzing = true,
                statusMessage = "Simulating & analyzing '$situationText'..."
            )

            val availableLabels = pads.map { it.label }
            val result = GeminiAudioClassifier.classifySituationText(situationText, availableLabels, getFeedbackRules())

            _state.value = _state.value.copy(isAnalyzing = false)

            val matchedLabel = result.matchedLabel
            if (matchedLabel != null) {
                addLog(
                    AutoParentLog(
                        detectedSituation = result.detectedSituation,
                        matchedPadLabel = matchedLabel,
                        confidence = result.confidence,
                        triggered = true
                    )
                )

                _state.value = _state.value.copy(
                    lastTriggeredLabel = matchedLabel,
                    statusMessage = "AI Matched: '$matchedLabel' -> Auto-Playing Clip!"
                )

                val targetPad = pads.firstOrNull { it.label.trim().equals(matchedLabel.trim(), ignoreCase = true) }
                    ?: pads.firstOrNull { pad ->
                        val m = matchedLabel.trim()
                        m.isNotBlank() && (pad.label.contains(m, ignoreCase = true) || m.contains(pad.label, ignoreCase = true))
                    }
                if (targetPad != null) {
                    val audioFile = getAudioFile(targetPad)
                    if (audioFile != null && audioFile.exists()) {
                        audioPlayer.playPad(targetPad.id, audioFile)
                    }
                }
            } else {
                _state.value = _state.value.copy(
                    statusMessage = "No matching voice clip found for that situation"
                )
            }
        }
    }

    fun setAutoPlayEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(autoPlayEnabled = enabled)
    }

    fun setSensitivity(sensitivity: Float) {
        _state.value = _state.value.copy(sensitivity = sensitivity.coerceIn(0.3f, 0.95f))
    }

    fun setCooldownSeconds(seconds: Int) {
        _state.value = _state.value.copy(cooldownSeconds = seconds.coerceIn(3, 60))
    }

    fun stopListening() {
        stopListeningInternal()
        _state.value = _state.value.copy(
            isListening = false,
            isAnalyzing = false,
            ambientAmplitude = 0f,
            statusMessage = "Auto-Parent Standing By"
        )
    }

    private fun addLog(log: AutoParentLog) {
        val currentLogs = _state.value.logs.toMutableList()
        currentLogs.add(0, log) // prepend newest
        if (currentLogs.size > 20) {
            currentLogs.removeAt(currentLogs.lastIndex)
        }
        _state.value = _state.value.copy(logs = currentLogs)
    }

    private fun stopListeningInternal() {
        // Cancelling the job triggers the transcriber's cancellation handler, which
        // cancels and releases the underlying SpeechRecognizer.
        listeningJob?.cancel()
        listeningJob = null
    }

    fun rateLog(logId: String, isPositive: Boolean) {
        val updatedLogs = _state.value.logs.map { log ->
            if (log.id == logId) {
                log.copy(userFeedback = isPositive)
            } else log
        }
        val feedbackType = if (isPositive) "thumbs-up 👍" else "thumbs-down 👎"
        _state.value = _state.value.copy(
            logs = updatedLogs,
            statusMessage = "Feedback saved ($feedbackType) - Training AI!"
        )
    }

    private fun getFeedbackRules(): List<String> {
        return _state.value.logs.mapNotNull { log ->
            when (log.userFeedback) {
                true -> "When situation matches '${log.detectedSituation}', PREFER matching '${log.matchedPadLabel}'."
                false -> "When situation matches '${log.detectedSituation}', DO NOT match '${log.matchedPadLabel}'."
                null -> null
            }
        }.take(10)
    }

    fun clearLogs() {
        _state.value = _state.value.copy(logs = emptyList())
    }
}
