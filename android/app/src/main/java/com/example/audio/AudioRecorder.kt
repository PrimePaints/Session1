package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
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

enum class RecordingState {
    IDLE,
    RECORDING,
    REVIEWING
}

data class RecordingStatus(
    val state: RecordingState = RecordingState.IDLE,
    val durationMs: Long = 0L,
    val remainingSeconds: Int = 30,
    val amplitudeNormalized: Float = 0f,
    val amplitudes: List<Float> = emptyList(),
    val tempFile: File? = null,
    val error: String? = null
)

class AudioRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentTempFile: File? = null

    private val _status = MutableStateFlow(RecordingStatus())
    val status: StateFlow<RecordingStatus> = _status.asStateFlow()

    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun startRecording(): Boolean {
        try {
            stopRecordingInternal(discardFile = true)

            val outputDir = File(context.cacheDir, "recordings").apply { mkdirs() }
            val tempFile = File(outputDir, "temp_rec_${System.currentTimeMillis()}.m4a")
            currentTempFile = tempFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioChannels(1) // Mono
            recorder.setAudioSamplingRate(44100)
            recorder.setAudioEncodingBitRate(96000)
            recorder.setOutputFile(tempFile.absolutePath)

            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder

            _status.value = RecordingStatus(
                state = RecordingState.RECORDING,
                durationMs = 0L,
                remainingSeconds = 30,
                amplitudeNormalized = 0f,
                amplitudes = emptyList(),
                tempFile = tempFile
            )

            startTimerAndAmplitudePolling()
            return true
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error starting recording", e)
            _status.value = RecordingStatus(
                state = RecordingState.IDLE,
                error = e.message ?: "Failed to start recording"
            )
            cleanUpTempFile()
            return false
        }
    }

    private fun startTimerAndAmplitudePolling() {
        timerJob?.cancel()
        val startTime = System.currentTimeMillis()

        timerJob = scope.launch {
            while (isActive && _status.value.state == RecordingState.RECORDING) {
                val elapsed = System.currentTimeMillis() - startTime
                val remainingSec = ((30000L - elapsed) / 1000L).toInt().coerceAtLeast(0)

                var maxAmp = 0
                try {
                    maxAmp = mediaRecorder?.maxAmplitude ?: 0
                } catch (e: Exception) {
                    // Ignore transient amplitude read error
                }

                val normalizedAmp = (maxAmp / 32767f).coerceIn(0f, 1f)

                _status.value = _status.value.copy(
                    durationMs = elapsed,
                    remainingSeconds = remainingSec,
                    amplitudeNormalized = normalizedAmp,
                    amplitudes = _status.value.amplitudes + normalizedAmp
                )

                if (elapsed >= 30000L) {
                    stopRecordingForReview()
                    break
                }

                delay(80)
            }
        }
    }

    fun stopRecordingForReview(): File? {
        timerJob?.cancel()
        timerJob = null

        try {
            mediaRecorder?.let { recorder ->
                try {
                    recorder.stop()
                } catch (e: Exception) {
                    Log.e("AudioRecorder", "Error stopping recorder", e)
                }
                recorder.release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error releasing recorder", e)
        } finally {
            mediaRecorder = null
        }

        val file = currentTempFile
        if (file != null && file.exists() && file.length() > 0) {
            _status.value = _status.value.copy(
                state = RecordingState.REVIEWING,
                amplitudeNormalized = 0f
            )
            return file
        } else {
            _status.value = RecordingStatus(
                state = RecordingState.IDLE,
                error = "Recording was too short or empty"
            )
            cleanUpTempFile()
            return null
        }
    }

    fun cancelRecording() {
        stopRecordingInternal(discardFile = true)
        _status.value = RecordingStatus(state = RecordingState.IDLE)
    }

    fun resetState() {
        cancelRecording()
    }

    private fun stopRecordingInternal(discardFile: Boolean) {
        timerJob?.cancel()
        timerJob = null

        try {
            mediaRecorder?.let { recorder ->
                try {
                    recorder.stop()
                } catch (e: Exception) {
                    // Ignore if not recording
                }
                recorder.release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error releasing media recorder", e)
        } finally {
            mediaRecorder = null
        }

        if (discardFile) {
            cleanUpTempFile()
        }
    }

    private fun cleanUpTempFile() {
        currentTempFile?.let { file ->
            if (file.exists()) {
                file.delete()
            }
        }
        currentTempFile = null
    }
}
