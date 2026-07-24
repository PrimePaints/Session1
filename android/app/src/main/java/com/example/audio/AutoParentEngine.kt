package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.example.data.db.PadEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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

class AutoParentEngine(
    private val context: Context,
    private val audioPlayer: AudioPlayer
) {
    private val TAG = "AutoParentEngine"

    private val _state = MutableStateFlow(AutoParentState())
    val state: StateFlow<AutoParentState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)
    private var listeningJob: Job? = null
    private var mediaRecorder: MediaRecorder? = null
    private var listeningTempFile: File? = null
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

        stopListeningInternal()

        _state.value = _state.value.copy(
            isListening = true,
            statusMessage = "Listening for vocal inputs..."
        )

        listeningJob = scope.launch(Dispatchers.IO) {
            val recDir = File(context.cacheDir, "autoparent").apply { mkdirs() }

            while (isActive && _state.value.isListening) {
                try {
                    val tempChunk = File(recDir, "chunk_${System.currentTimeMillis()}.m4a")
                    listeningTempFile = tempChunk

                    val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        MediaRecorder(context)
                    } else {
                        @Suppress("DEPRECATION")
                        MediaRecorder()
                    }

                    recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                    recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    recorder.setAudioChannels(1)
                    recorder.setAudioSamplingRate(22050)
                    recorder.setOutputFile(tempChunk.absolutePath)

                    recorder.prepare()
                    recorder.start()
                    mediaRecorder = recorder

                    // Adaptive chunk sampling: Base duration 1200ms, early stop 500ms after a volume spike (> 0.30f)
                    var maxAmplitude = 0
                    val sampleStartTime = System.currentTimeMillis()
                    var spikeDetectedTimeMs = 0L

                    while (isActive && _state.value.isListening) {
                        val elapsed = System.currentTimeMillis() - sampleStartTime
                        try {
                            val amp = recorder.maxAmplitude
                            if (amp > maxAmplitude) maxAmplitude = amp

                            val normAmp = (amp / 32767f).coerceIn(0f, 1f)
                            _state.value = _state.value.copy(ambientAmplitude = normAmp)

                            // Detect sudden vocal burst / shouting spike
                            if (normAmp > 0.30f && spikeDetectedTimeMs == 0L) {
                                spikeDetectedTimeMs = System.currentTimeMillis()
                            }
                        } catch (_: Exception) {}

                        // If a loud vocal spike was detected and we captured at least 600ms of audio,
                        // trigger early (500ms after spike) to cut off shouting fast!
                        if (spikeDetectedTimeMs > 0L && (System.currentTimeMillis() - spikeDetectedTimeMs) >= 500L && elapsed >= 600L) {
                            Log.d(TAG, "Early trigger activated on vocal burst spike (elapsed: ${elapsed}ms)")
                            break
                        }

                        // Cap standard chunk duration at 1200ms (down from 2500ms)
                        if (elapsed >= 1200L) {
                            break
                        }

                        delay(50)
                    }

                    try {
                        recorder.stop()
                    } catch (_: Exception) {}
                    recorder.release()
                    mediaRecorder = null

                    val normMaxAmp = (maxAmplitude / 32767f).coerceIn(0f, 1f)

                    // Run local TFLite acoustic classifier
                    val acousticResult = TFLiteAcousticClassifier.analyzeAudioChunk(tempChunk, normMaxAmp, pads)

                    // If acoustic level triggers or noise is significant, run contextual Gemini classifier
                    if (acousticResult.category != AcousticCategory.QUIET_AMBIENT && tempChunk.exists() && tempChunk.length() > 500) {
                        _state.value = _state.value.copy(
                            isAnalyzing = true,
                            statusMessage = "Analyzing voice input (${acousticResult.category.name})..."
                        )

                        val availableLabels = pads.map { it.label }
                        val result = GeminiAudioClassifier.classifyAudio(
                            audioFile = tempChunk,
                            availablePads = availableLabels,
                            feedbackRules = getFeedbackRules(),
                            acousticHint = acousticResult.summary
                        )

                        _state.value = _state.value.copy(isAnalyzing = false)

                        if (result.matchedLabel != null && result.confidence >= _state.value.sensitivity) {
                            val now = System.currentTimeMillis()
                            val cooldownMs = _state.value.cooldownSeconds * 1000L

                            val shouldTrigger = (now - lastTriggerTimeMs) >= cooldownMs

                            val newLog = AutoParentLog(
                                detectedSituation = result.detectedSituation,
                                matchedPadLabel = result.matchedLabel,
                                confidence = result.confidence,
                                triggered = shouldTrigger
                            )

                            addLog(newLog)

                            if (shouldTrigger) {
                                lastTriggerTimeMs = now
                                _state.value = _state.value.copy(
                                    lastTriggeredLabel = result.matchedLabel,
                                    statusMessage = "Matched '${result.matchedLabel}' (${(result.confidence * 100).toInt()}%) -> Playing Clip!"
                                )

                                val targetPad = pads.firstOrNull { it.label.trim().equals(result.matchedLabel?.trim(), ignoreCase = true) }
                                    ?: pads.firstOrNull { pad ->
                                        val m = result.matchedLabel?.trim() ?: ""
                                        m.isNotBlank() && (pad.label.contains(m, ignoreCase = true) || m.contains(pad.label, ignoreCase = true))
                                    }
                                if (targetPad != null) {
                                    val audioFile = fileResolver(targetPad)
                                    if (audioFile != null && audioFile.exists()) {
                                        scope.launch(Dispatchers.Main) {
                                            audioPlayer.playPad(targetPad.id, audioFile)
                                        }
                                    }
                                }
                            } else {
                                _state.value = _state.value.copy(
                                    statusMessage = "Cooldown active (Skipped duplicate clip)"
                                )
                            }
                        } else {
                            _state.value = _state.value.copy(
                                statusMessage = "Listening for vocal inputs..."
                            )
                        }
                    } else {
                        _state.value = _state.value.copy(
                            statusMessage = "Listening for vocal inputs...",
                            ambientAmplitude = 0f
                        )
                    }

                    if (tempChunk.exists()) {
                        tempChunk.delete()
                    }

                    delay(150) // Seamless continuous monitoring cycle

                } catch (e: Exception) {
                    Log.e(TAG, "Error in listening loop", e)
                    stopListeningInternal()
                    _state.value = _state.value.copy(
                        isListening = false,
                        isAnalyzing = false,
                        statusMessage = "Listening paused (Mic busy or released)"
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

            if (result.matchedLabel != null) {
                val newLog = AutoParentLog(
                    detectedSituation = result.detectedSituation,
                    matchedPadLabel = result.matchedLabel,
                    confidence = result.confidence,
                    triggered = true
                )
                addLog(newLog)

                _state.value = _state.value.copy(
                    lastTriggeredLabel = result.matchedLabel,
                    statusMessage = "AI Matched: '${result.matchedLabel}' -> Auto-Playing Clip!"
                )

                val targetPad = pads.firstOrNull { it.label.trim().equals(result.matchedLabel?.trim(), ignoreCase = true) }
                    ?: pads.firstOrNull { pad ->
                        val m = result.matchedLabel?.trim() ?: ""
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
        listeningJob?.cancel()
        listeningJob = null

        try {
            mediaRecorder?.let { recorder ->
                try { recorder.stop() } catch (_: Exception) {}
                recorder.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing media recorder", e)
        } finally {
            mediaRecorder = null
        }

        listeningTempFile?.let {
            if (it.exists()) it.delete()
        }
        listeningTempFile = null
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
