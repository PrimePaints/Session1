package com.example.audio

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class AutoParentMatchResult(
    val matchedLabel: String?,
    val detectedSituation: String,
    val confidence: Float
)

/**
 * Sends ONLY TEXT to Gemini Flash Lite to pick the best matching soundboard clip.
 *
 * PRIVACY: This class never transmits audio. Ambient speech is transcribed to text
 * ON-DEVICE (see [OnDeviceTranscriber]); only that resulting text — plus a short
 * locally-derived tone hint — is ever sent off the device. Users' recordings and any
 * captured microphone audio always remain on the phone.
 */
object GeminiAudioClassifier {
    private const val TAG = "GeminiAudioClassifier"
    // Gemini Flash Lite: chosen for the fastest, lowest-latency responses.
    private const val MODEL_NAME = "gemini-3.1-flash-lite"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    /**
     * Matches a text description of the situation (e.g. an on-device transcript of what
     * a child said, optionally with a tone hint) to the best available clip label.
     * Text only — no audio is sent.
     */
    suspend fun classifySituationText(
        situationText: String,
        availablePads: List<String>,
        feedbackRules: List<String> = emptyList()
    ): AutoParentMatchResult = withContext(Dispatchers.IO) {
        if (availablePads.isEmpty()) {
            return@withContext AutoParentMatchResult(null, "No sound clips available", 0f)
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext fallbackTextMatch(situationText, availablePads)
        }

        try {
            val padsFormatted = availablePads.joinToString(", ") { "\"$it\"" }
            val feedbackPromptStr = if (feedbackRules.isNotEmpty()) {
                "\nUser Training & Feedback Rules:\n" + feedbackRules.joinToString("\n") { "- $it" } + "\n"
            } else ""

            val prompt = """
                You are Auto-Parent, a fast low-latency AI assistant for parents.
                The parent describes the ambient situation: "$situationText".
                The parent has recorded voice clips with the following exact labels:
                [$padsFormatted]
                $feedbackPromptStr
                Match the situation to the BEST matching voice clip label from the list.
                Match semantic meaning, even if wording differs (e.g. a child complaining
                about broccoli -> "Eat your veggies"; nagging for a tablet -> "No screens").
                If nothing fits, return null for matchedLabel.
                Respond strictly in JSON:
                {
                  "matchedLabel": "<exact label from list, or null if none fits>",
                  "detectedSituation": "<short description of the situation>",
                  "confidence": <float 0.0 to 1.0>
                }
            """.trimIndent()

            val jsonPayload = """
                {
                  "contents": [
                    {
                      "parts": [
                        { "text": ${escapeJson(prompt)} }
                      ]
                    }
                  ],
                  "generationConfig": {
                    "temperature": 0.1,
                    "maxOutputTokens": 150,
                    "responseMimeType": "application/json"
                  }
                }
            """.trimIndent()

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(jsonPayload.toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful || responseString.isBlank()) {
                Log.e(TAG, "API call failed: ${response.code} $responseString")
                return@withContext fallbackTextMatch(situationText, availablePads)
            }

            return@withContext parseGeminiJsonResponse(responseString, availablePads)
        } catch (e: Exception) {
            Log.e(TAG, "Error in Gemini text classification", e)
            return@withContext fallbackTextMatch(situationText, availablePads)
        }
    }

    private fun parseGeminiJsonResponse(
        rawJsonResponse: String,
        availablePads: List<String>
    ): AutoParentMatchResult {
        try {
            // Extract json content from candidate text
            val textContent = extractTextFromCandidates(rawJsonResponse) ?: rawJsonResponse
            val cleanJson = textContent.substringAfter("{").substringBeforeLast("}")
            val fullJson = "{$cleanJson}"

            val map = moshi.adapter(Map::class.java).fromJson(fullJson) as? Map<*, *>
            val matchedLabel = map?.get("matchedLabel") as? String
            val detectedSituation = map?.get("detectedSituation") as? String ?: "Ambient noise detected"
            val confidence = (map?.get("confidence") as? Number)?.toFloat() ?: 0.5f

            val validLabel = if (matchedLabel.isNullOrBlank()) {
                null
            } else {
                val cleanM = matchedLabel.trim()
                availablePads.firstOrNull { it.trim().equals(cleanM, ignoreCase = true) }
                    ?: availablePads.firstOrNull { pad ->
                        val cleanPad = pad.trim()
                        cleanPad.contains(cleanM, ignoreCase = true) || cleanM.contains(cleanPad, ignoreCase = true)
                    }
            }

            return AutoParentMatchResult(
                matchedLabel = validLabel,
                detectedSituation = detectedSituation,
                confidence = if (validLabel != null) confidence else 0f
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Gemini response: $rawJsonResponse", e)
            return AutoParentMatchResult(null, "Unrecognized situation", 0f)
        }
    }

    private fun extractTextFromCandidates(responseJson: String): String? {
        try {
            val root = moshi.adapter(Map::class.java).fromJson(responseJson) as? Map<*, *>
            val candidates = root?.get("candidates") as? List<*>
            val firstCand = candidates?.firstOrNull() as? Map<*, *>
            val content = firstCand?.get("content") as? Map<*, *>
            val parts = content?.get("parts") as? List<*>
            val firstPart = parts?.firstOrNull() as? Map<*, *>
            return firstPart?.get("text") as? String
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Offline fallback when no API key is set or the network call fails. Runs entirely
     * on-device using simple keyword/topic heuristics.
     */
    private fun fallbackTextMatch(
        text: String,
        availablePads: List<String>
    ): AutoParentMatchResult {
        val lowerText = text.lowercase()

        var highestScore = 0
        val matched = availablePads.maxByOrNull { padLabel ->
            val padLower = padLabel.lowercase()
            var score = 0

            // Direct word overlap
            val textWords = lowerText.split(Regex("\\W+")).filter { it.length > 2 }
            val padWords = padLower.split(Regex("\\W+")).filter { it.length > 2 }
            for (w in textWords) {
                if (padWords.contains(w)) score += 3
            }

            // Topic: Shouting / Loud Noise / Tantrums
            if (lowerText.contains("shout") || lowerText.contains("scream") || lowerText.contains("noise") || lowerText.contains("yell") || lowerText.contains("crying") || lowerText.contains("tantrum")) {
                if (padLower.contains("shout") || padLower.contains("quiet") || padLower.contains("stop") || padLower.contains("calm") || padLower.contains("cry") || padLower.contains("listen")) score += 5
            }
            // Topic: Food / Dinner / Veggies / Eating
            if (lowerText.contains("eat") || lowerText.contains("food") || lowerText.contains("dinner") || lowerText.contains("hungry") || lowerText.contains("broccoli") || lowerText.contains("veggie") || lowerText.contains("vegetable") || lowerText.contains("lunch") || lowerText.contains("breakfast")) {
                if (padLower.contains("eat") || padLower.contains("food") || padLower.contains("dinner") || padLower.contains("veggie") || padLower.contains("plate") || padLower.contains("chew") || padLower.contains("table")) score += 5
            }
            // Topic: Screen Time / Tablet / Phone / TV / Games
            if (lowerText.contains("tablet") || lowerText.contains("ipad") || lowerText.contains("phone") || lowerText.contains("tv") || lowerText.contains("game") || lowerText.contains("screen") || lowerText.contains("youtube")) {
                if (padLower.contains("tablet") || padLower.contains("screen") || padLower.contains("phone") || padLower.contains("tv") || padLower.contains("outside") || padLower.contains("off") || padLower.contains("homework")) score += 5
            }
            // Topic: Safety / Danger / Touching things
            if (lowerText.contains("touch") || lowerText.contains("danger") || lowerText.contains("plug") || lowerText.contains("hot") || lowerText.contains("sharp") || lowerText.contains("stove") || lowerText.contains("electrical")) {
                if (padLower.contains("touch") || padLower.contains("don't") || padLower.contains("stop") || padLower.contains("danger") || padLower.contains("hot") || padLower.contains("careful")) score += 5
            }
            // Topic: Bedtime / Sleep
            if (lowerText.contains("bed") || lowerText.contains("sleep") || lowerText.contains("tired") || lowerText.contains("bedtime") || lowerText.contains("pajamas") || lowerText.contains("teeth")) {
                if (padLower.contains("bed") || padLower.contains("sleep") || padLower.contains("night") || padLower.contains("pajama") || padLower.contains("brush")) score += 5
            }
            // Topic: Toys / Sharing / Fighting
            if (lowerText.contains("toy") || lowerText.contains("mine") || lowerText.contains("share") || lowerText.contains("fight") || lowerText.contains("took") || lowerText.contains("hit")) {
                if (padLower.contains("share") || padLower.contains("toy") || padLower.contains("nice") || padLower.contains("fight") || padLower.contains("kind")) score += 5
            }
            // Topic: Cleaning / Mess
            if (lowerText.contains("clean") || lowerText.contains("mess") || lowerText.contains("room") || lowerText.contains("pick up") || lowerText.contains("tidy")) {
                if (padLower.contains("clean") || padLower.contains("tidy") || padLower.contains("pick up") || padLower.contains("room") || padLower.contains("toy")) score += 5
            }

            if (score > highestScore) highestScore = score
            score
        }

        return if (matched != null && highestScore > 0) {
            AutoParentMatchResult(matched, text, 0.85f)
        } else {
            AutoParentMatchResult(null, text, 0.20f)
        }
    }

    private fun escapeJson(string: String): String {
        return moshi.adapter(String::class.java).toJson(string)
    }
}
