package com.example.audio

import android.util.Base64
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
import java.io.File
import java.util.concurrent.TimeUnit

data class AutoParentMatchResult(
    val matchedLabel: String?,
    val detectedSituation: String,
    val confidence: Float
)

object GeminiAudioClassifier {
    private const val TAG = "GeminiAudioClassifier"
    private const val MODEL_NAME = "gemini-3.1-flash-lite"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    suspend fun classifyAudio(
        audioFile: File,
        availablePads: List<String>,
        feedbackRules: List<String> = emptyList(),
        acousticHint: String? = null
    ): AutoParentMatchResult = withContext(Dispatchers.IO) {
        if (availablePads.isEmpty()) {
            return@withContext AutoParentMatchResult(null, "No sound clips available", 0f)
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key missing, falling back to heuristic matching")
            return@withContext fallbackHeuristicAudioMatch(audioFile, availablePads)
        }

        try {
            val base64Audio = Base64.encodeToString(audioFile.readBytes(), Base64.NO_WRAP)
            val padsFormatted = availablePads.joinToString(", ") { "\"$it\"" }
            val feedbackPromptStr = if (feedbackRules.isNotEmpty()) {
                "\nUser Training & Feedback Rules:\n" + feedbackRules.joinToString("\n") { "- $it" } + "\n"
            } else ""
            val acousticHintStr = if (!acousticHint.isNullOrBlank()) {
                "\nLocal TFLite Signal Analysis Trigger: $acousticHint\n"
            } else ""

            val prompt = """
                You are Auto-Parent, a fast low-latency AI assistant for parents. Analyze this audio clip of children.
                The parent has recorded voice clips on their soundboard with the following exact labels:
                [$padsFormatted]
                $feedbackPromptStr$acousticHintStr
                
                CRITICAL INSTRUCTIONS FOR CONTEXTUAL MATCHING:
                1. Listen carefully and transcribe any spoken words, nagging, whining, requests, or behavior in the audio.
                2. Analyze the SUBJECT and CONTEXT of what the children are saying/doing (e.g. complaining about food/veggies, refusing bedtime, nagging for screens/toys, arguing over items, asking for candy, crying, bad manners, or shouting).
                3. Map the context to the MOST APPROPRIATE parent clip label from [$padsFormatted].
                   - Match semantic meaning, even if wording differs! (e.g., child complains "I don't want to eat broccoli" -> match "Eat your veggies" or "Finish your plate"; child nags "Can I play on your phone?" -> match "No tablet" or "Go play outside"; child yells "Gimme that toy!" -> match "Stop fighting" or "Share your toys").
                4. DO NOT default to a generic "No Shouting" clip if a more specific contextual clip matches what the child is actually saying/nagging about. Tailor your selection specifically to the parent's available clips!
                5. If NO clip matches the context at all, return null for matchedLabel.

                Respond ONLY in JSON with this exact format:
                {
                  "matchedLabel": "<exact string from available labels list, or null>",
                  "detectedSituation": "<short description of what was heard/said in audio>",
                  "confidence": <float between 0.0 and 1.0>
                }
            """.trimIndent()

            val jsonPayload = """
                {
                  "contents": [
                    {
                      "parts": [
                        { "text": ${escapeJson(prompt)} },
                        {
                          "inlineData": {
                            "mimeType": "audio/mp4",
                            "data": "$base64Audio"
                          }
                        }
                      ]
                    }
                  ],
                  "generationConfig": {
                    "temperature": 0.1,
                    "maxOutputTokens": 60,
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
                return@withContext fallbackHeuristicAudioMatch(audioFile, availablePads)
            }

            return@withContext parseGeminiJsonResponse(responseString, availablePads)
        } catch (e: Exception) {
            Log.e(TAG, "Error in Gemini audio classification", e)
            return@withContext fallbackHeuristicAudioMatch(audioFile, availablePads)
        }
    }

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
                Respond strictly in JSON:
                {
                  "matchedLabel": "<exact label from list, or null if none fits>",
                  "detectedSituation": "$situationText",
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
                    "temperature": 0.2,
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
            return AutoParentMatchResult(null, "Unrecognized audio", 0f)
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

    private fun fallbackHeuristicAudioMatch(
        audioFile: File,
        availablePads: List<String>
    ): AutoParentMatchResult {
        if (!audioFile.exists() || audioFile.length() < 1000) {
            return AutoParentMatchResult(null, "Quiet ambient room", 0.1f)
        }

        val shoutPad = availablePads.firstOrNull {
            it.contains("shout", ignoreCase = true) ||
            it.contains("quiet", ignoreCase = true) ||
            it.contains("stop", ignoreCase = true) ||
            it.contains("no", ignoreCase = true)
        }

        return if (shoutPad != null) {
            AutoParentMatchResult(
                matchedLabel = shoutPad,
                detectedSituation = "Elevated sound level detected",
                confidence = 0.75f
            )
        } else {
            AutoParentMatchResult(
                matchedLabel = null,
                detectedSituation = "Ambient sound detected",
                confidence = 0.20f
            )
        }
    }

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
