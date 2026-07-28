package app.repeatless.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
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

data class PlaybackState(
    val activePadId: String? = null,
    val isPlaying: Boolean = false,
    val progress: Float = 0f, // 0.0 to 1.0
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val error: String? = null
)

class AudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun playPad(padId: String, audioFile: File): Boolean {
        // If already playing this pad, stop it
        if (_playbackState.value.activePadId == padId && _playbackState.value.isPlaying) {
            stop()
            return false
        }

        stopInternal()

        if (!audioFile.exists() || audioFile.length() == 0L) {
            _playbackState.value = PlaybackState(
                activePadId = padId,
                error = "Audio file missing or empty"
            )
            return false
        }

        try {
            requestAudioFocus()

            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(audioFile.absolutePath)
                prepare()
            }

            val totalDuration = player.duration.toLong().coerceAtLeast(1L)
            mediaPlayer = player

            player.setOnCompletionListener {
                stopInternal()
            }

            player.setOnErrorListener { _, what, extra ->
                Log.e("AudioPlayer", "MediaPlayer error: what=$what, extra=$extra")
                stopInternal()
                _playbackState.value = PlaybackState(
                    activePadId = padId,
                    error = "Failed to play audio clip"
                )
                true
            }

            player.start()

            _playbackState.value = PlaybackState(
                activePadId = padId,
                isPlaying = true,
                progress = 0f,
                currentPositionMs = 0L,
                totalDurationMs = totalDuration
            )

            startProgressTracker(padId, player, totalDuration)
            return true

        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error starting playback for $padId", e)
            stopInternal()
            _playbackState.value = PlaybackState(
                activePadId = padId,
                error = e.message ?: "Playback error"
            )
            return false
        }
    }

    fun playRange(padId: String, audioFile: File, startMs: Long, endMs: Long): Boolean {
        if (_playbackState.value.activePadId == padId && _playbackState.value.isPlaying) {
            stop()
            return false
        }

        stopInternal()

        if (!audioFile.exists() || audioFile.length() == 0L) {
            _playbackState.value = PlaybackState(
                activePadId = padId,
                error = "Audio file missing or empty"
            )
            return false
        }

        try {
            requestAudioFocus()

            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(audioFile.absolutePath)
                prepare()
            }

            val totalDuration = player.duration.toLong().coerceAtLeast(1L)
            val actualStart = startMs.coerceIn(0L, totalDuration)
            val actualEnd = if (endMs > 0L) endMs.coerceIn(actualStart + 100L, totalDuration) else totalDuration

            mediaPlayer = player

            player.setOnCompletionListener {
                stopInternal()
            }

            player.setOnErrorListener { _, what, extra ->
                Log.e("AudioPlayer", "MediaPlayer error: what=$what, extra=$extra")
                stopInternal()
                _playbackState.value = PlaybackState(
                    activePadId = padId,
                    error = "Failed to play audio clip"
                )
                true
            }

            if (actualStart > 0) {
                player.seekTo(actualStart.toInt())
            }

            player.start()

            _playbackState.value = PlaybackState(
                activePadId = padId,
                isPlaying = true,
                progress = 0f,
                currentPositionMs = actualStart,
                totalDurationMs = totalDuration
            )

            startProgressTrackerWithEnd(padId, player, actualStart, actualEnd)
            return true

        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error starting playback for $padId", e)
            stopInternal()
            _playbackState.value = PlaybackState(
                activePadId = padId,
                error = e.message ?: "Playback error"
            )
            return false
        }
    }

    private fun startProgressTrackerWithEnd(padId: String, player: MediaPlayer, startMs: Long, endMs: Long) {
        progressJob?.cancel()
        progressJob = scope.launch {
            val rangeDuration = (endMs - startMs).coerceAtLeast(1L).toFloat()
            while (isActive && _playbackState.value.isPlaying && _playbackState.value.activePadId == padId) {
                try {
                    if (player.isPlaying) {
                        val pos = player.currentPosition.toLong().coerceAtLeast(0L)
                        if (pos >= endMs) {
                            stopInternal()
                            break
                        }
                        val frac = ((pos - startMs).toFloat() / rangeDuration).coerceIn(0f, 1f)
                        _playbackState.value = _playbackState.value.copy(
                            progress = frac,
                            currentPositionMs = pos
                        )
                    }
                } catch (e: Exception) {
                    break
                }
                delay(30)
            }
        }
    }

    private fun startProgressTracker(padId: String, player: MediaPlayer, durationMs: Long) {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _playbackState.value.isPlaying && _playbackState.value.activePadId == padId) {
                try {
                    if (player.isPlaying) {
                        val pos = player.currentPosition.toLong().coerceAtLeast(0L)
                        val frac = (pos.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                        _playbackState.value = _playbackState.value.copy(
                            progress = frac,
                            currentPositionMs = pos
                        )
                    }
                } catch (e: Exception) {
                    break
                }
                delay(30)
            }
        }
    }

    fun stop() {
        stopInternal()
    }

    private fun stopInternal() {
        progressJob?.cancel()
        progressJob = null

        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error stopping player", e)
        } finally {
            mediaPlayer = null
        }

        abandonAudioFocus()
        _playbackState.value = PlaybackState()
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attr)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        stop()
                    }
                }
                .build()

            audioFocusRequest = focusRequest
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        stop()
                    }
                },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    fun release() {
        stopInternal()
    }
}
