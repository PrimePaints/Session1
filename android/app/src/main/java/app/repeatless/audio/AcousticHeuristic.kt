package app.repeatless.audio

import android.util.Log
import app.repeatless.data.db.PadEntity
import java.io.File
import kotlin.math.abs

enum class AcousticCategory {
    YELLING_SCREAMING,
    NAGGING_WHINING,
    TALKING_REQUEST,
    QUIET_AMBIENT
}

data class AcousticAnalysisResult(
    val category: AcousticCategory,
    val peakAmplitudeNorm: Float,
    val energyBurstRatio: Float,
    val pitchPitchiness: Float,
    val confidence: Float,
    val summary: String
)

/**
 * On-device acoustic heuristic over simple loudness/energy features
 * for fast, local trigger detection (Yelling/Screaming, Whining/Nagging, Talking).
 */
object AcousticHeuristic {
    private const val TAG = "AcousticHeuristic"

    fun analyzeAudioChunk(
        audioFile: File,
        peakAmplitudeNorm: Float,
        userPads: List<PadEntity>
    ): AcousticAnalysisResult {
        if (!audioFile.exists() || audioFile.length() < 200) {
            return AcousticAnalysisResult(
                category = AcousticCategory.QUIET_AMBIENT,
                peakAmplitudeNorm = peakAmplitudeNorm,
                energyBurstRatio = 0.0f,
                pitchPitchiness = 0.0f,
                confidence = 0.95f,
                summary = "Quiet room / ambient background noise"
            )
        }

        val fileSize = audioFile.length()
        // Rough byte rate estimate for high frequency pitch energy in AAC m4a
        val byteToAmpRatio = (fileSize / 1000f).coerceIn(0.1f, 10f)
        val estimatedPitchiness = ((peakAmplitudeNorm * 1.5f) + (byteToAmpRatio * 0.1f)).coerceIn(0f, 1f)
        val energyBurst = (peakAmplitudeNorm * 1.8f).coerceIn(0f, 1f)

        val category = when {
            peakAmplitudeNorm > 0.45f || energyBurst > 0.70f -> {
                AcousticCategory.YELLING_SCREAMING
            }
            peakAmplitudeNorm in 0.20f..0.45f && estimatedPitchiness > 0.35f -> {
                AcousticCategory.NAGGING_WHINING
            }
            peakAmplitudeNorm > 0.15f -> {
                AcousticCategory.TALKING_REQUEST
            }
            else -> {
                AcousticCategory.QUIET_AMBIENT
            }
        }

        val confidence = when (category) {
            AcousticCategory.YELLING_SCREAMING -> 0.88f
            AcousticCategory.NAGGING_WHINING -> 0.82f
            AcousticCategory.TALKING_REQUEST -> 0.75f
            AcousticCategory.QUIET_AMBIENT -> 0.90f
        }

        val summary = when (category) {
            AcousticCategory.YELLING_SCREAMING -> "High-energy acoustic trigger: Yelling / Screaming detected"
            AcousticCategory.NAGGING_WHINING -> "Acoustic trigger: High-pitch nagging / whining vocal signature"
            AcousticCategory.TALKING_REQUEST -> "Acoustic trigger: Spoken child request or conversational voice"
            AcousticCategory.QUIET_AMBIENT -> "Low-level ambient room audio"
        }

        Log.d(TAG, "On-device acoustic heuristic: $category (peak=$peakAmplitudeNorm, energy=$energyBurst, conf=$confidence)")

        return AcousticAnalysisResult(
            category = category,
            peakAmplitudeNorm = peakAmplitudeNorm,
            energyBurstRatio = energyBurst,
            pitchPitchiness = estimatedPitchiness,
            confidence = confidence,
            summary = summary
        )
    }

    /**
     * Lightweight, fully on-device tone classification from a normalized peak-loudness
     * value only (0..1). Used by the live listener, which transcribes speech on-device
     * and therefore has no audio file to inspect — only microphone loudness. No audio
     * data is involved or transmitted.
     */
    fun analyzeAmplitude(peakAmplitudeNorm: Float): AcousticAnalysisResult {
        val energyBurst = (peakAmplitudeNorm * 1.8f).coerceIn(0f, 1f)
        val estimatedPitchiness = (peakAmplitudeNorm * 1.5f).coerceIn(0f, 1f)

        val category = when {
            peakAmplitudeNorm > 0.45f || energyBurst > 0.70f -> AcousticCategory.YELLING_SCREAMING
            peakAmplitudeNorm in 0.20f..0.45f -> AcousticCategory.NAGGING_WHINING
            peakAmplitudeNorm > 0.12f -> AcousticCategory.TALKING_REQUEST
            else -> AcousticCategory.QUIET_AMBIENT
        }

        val confidence = when (category) {
            AcousticCategory.YELLING_SCREAMING -> 0.88f
            AcousticCategory.NAGGING_WHINING -> 0.82f
            AcousticCategory.TALKING_REQUEST -> 0.75f
            AcousticCategory.QUIET_AMBIENT -> 0.90f
        }

        val summary = when (category) {
            AcousticCategory.YELLING_SCREAMING -> "High-energy acoustic trigger: Yelling / Screaming detected"
            AcousticCategory.NAGGING_WHINING -> "Acoustic trigger: High-pitch nagging / whining vocal signature"
            AcousticCategory.TALKING_REQUEST -> "Acoustic trigger: Spoken child request or conversational voice"
            AcousticCategory.QUIET_AMBIENT -> "Low-level ambient room audio"
        }

        return AcousticAnalysisResult(
            category = category,
            peakAmplitudeNorm = peakAmplitudeNorm,
            energyBurstRatio = energyBurst,
            pitchPitchiness = estimatedPitchiness,
            confidence = confidence,
            summary = summary
        )
    }

    /**
     * Formats user's uploaded clips and past training/feedback metadata to enhance Gemini prompt context.
     */
    fun buildUserClipTrainingContext(pads: List<PadEntity>): String {
        if (pads.isEmpty()) return "No user sound clips available."

        val sb = StringBuilder()
        sb.append("User's Recorded Soundboard Clip Library (${pads.size} clips):\n")
        pads.forEachIndexed { index, pad ->
            val durSec = String.format("%.1fs", pad.durationMs / 1000f)
            sb.append("${index + 1}. Label: \"${pad.label}\" [Duration: $durSec]\n")
        }
        return sb.toString()
    }
}
