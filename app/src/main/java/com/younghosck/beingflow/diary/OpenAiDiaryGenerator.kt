package com.younghosck.beingflow.diary

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

interface DiaryGenerator {
    suspend fun generate(apiKey: String, model: String, prompt: String): Result<String>
}

class OpenAiDiaryGenerator(
    private val client: OkHttpClient = OkHttpClient()
) : DiaryGenerator {
    override suspend fun generate(apiKey: String, model: String, prompt: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
        val json = JSONObject()
            .put("model", model)
            .put("input", prompt)
            .put("instructions", "한국어로만 답하세요. 제공된 기록 밖의 사실은 만들지 마세요.")
            .put("max_output_tokens", 900)
        val request = Request.Builder()
            .url("https://api.openai.com/v1/responses")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("OpenAI diary API failed: HTTP ${response.code}${extractErrorMessage(raw)}")
            }
            extractText(JSONObject(raw)).ifBlank { throw IOException("OpenAI diary API returned empty text") }
        }
        }
    }

    private fun extractText(json: JSONObject): String {
        json.optString("output_text").takeIf { it.isNotBlank() }?.let { return it }
        val output = json.optJSONArray("output") ?: JSONArray()
        val parts = mutableListOf<String>()
        for (i in 0 until output.length()) {
            val item = output.optJSONObject(i) ?: continue
            val content = item.optJSONArray("content") ?: continue
            for (j in 0 until content.length()) {
                val contentItem = content.optJSONObject(j) ?: continue
                contentItem.optString("text").takeIf { it.isNotBlank() }?.let(parts::add)
            }
        }
        return parts.joinToString("\n")
    }

    private fun extractErrorMessage(raw: String): String {
        if (raw.isBlank()) return ""
        return runCatching {
            val message = JSONObject(raw)
                .optJSONObject("error")
                ?.optString("message")
                ?.takeIf { it.isNotBlank() }
            if (message == null) "" else " - $message"
        }.getOrDefault("")
    }
}
