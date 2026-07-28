package app.repeatless.audio

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * On-device speech-to-text.
 *
 * PRIVACY: audio is captured and transcribed to text locally on the phone and NEVER
 * leaves the device. Only the resulting text is ever passed onward (to Gemini Flash
 * Lite, via [GeminiAudioClassifier]). Prefers the platform's on-device recognizer and
 * always requests offline recognition.
 *
 * [SpeechRecognizer] must be created and driven on the main thread, so the public
 * entry point hops to [Dispatchers.Main] internally.
 */
class OnDeviceTranscriber(private val context: Context) {

    private val tag = "OnDeviceTranscriber"

    /** Whether any speech recognition service is present on this device. */
    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    /**
     * Listens once and returns the recognized text, or null if nothing usable was heard
     * (silence, error, or no recognizer). [onRms] streams a normalized 0..1 loudness
     * value for the ambient meter while listening.
     */
    suspend fun transcribeOnce(onRms: (Float) -> Unit): String? =
        withContext(Dispatchers.Main) {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.w(tag, "No speech recognition service available on this device")
                return@withContext null
            }

            suspendCancellableCoroutine { cont ->
                val recognizer = createRecognizer()
                var resumed = false

                fun finish(text: String?) {
                    if (resumed) return
                    resumed = true
                    try {
                        recognizer.destroy()
                    } catch (_: Exception) {
                    }
                    if (cont.isActive) cont.resume(text)
                }

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {
                        // rmsdB is roughly -2..12 dB; normalize to 0..1 for the meter.
                        val norm = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        onRms(norm)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        // Common non-fatal cases: no speech / timeout. Treat all as "nothing heard".
                        finish(null)
                    }

                    override fun onResults(results: Bundle?) {
                        val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        finish(list?.firstOrNull()?.takeIf { it.isNotBlank() })
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                cont.invokeOnCancellation {
                    try {
                        recognizer.cancel()
                        recognizer.destroy()
                    } catch (_: Exception) {
                    }
                }

                try {
                    recognizer.startListening(buildIntent())
                } catch (e: Exception) {
                    Log.e(tag, "startListening failed", e)
                    finish(null)
                }
            }
        }

    private fun createRecognizer(): SpeechRecognizer {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        ) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }
    }

    private fun buildIntent(): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            // Keep recognition on-device wherever the platform supports it.
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
}
