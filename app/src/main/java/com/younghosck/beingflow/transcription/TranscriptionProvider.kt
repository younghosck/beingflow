package com.younghosck.beingflow.transcription

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.younghosck.beingflow.domain.TranscriptionSource
import com.younghosck.beingflow.domain.TranscriptionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.Locale

data class TranscriptionResult(
    val text: String?,
    val source: TranscriptionSource,
    val status: TranscriptionStatus,
    val errorMessage: String? = null
)

interface LiveTranscriptionProvider {
    fun start(): Flow<TranscriptionResult>
}

interface AudioTranscriptionProvider {
    suspend fun transcribe(file: File, apiKey: String, model: String): TranscriptionResult
}

class AndroidLiveSpeechTranscriptionProvider(private val context: Context) : LiveTranscriptionProvider {
    override fun start(): Flow<TranscriptionResult> = callbackFlow {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            trySend(TranscriptionResult(null, TranscriptionSource.ANDROID_SPEECH_RECOGNIZER, TranscriptionStatus.FAILED, "SpeechRecognizer unavailable"))
            close()
            return@callbackFlow
        }
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!text.isNullOrBlank()) {
                    trySend(TranscriptionResult(text, TranscriptionSource.ANDROID_SPEECH_RECOGNIZER, TranscriptionStatus.PENDING))
                }
            }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                trySend(
                    TranscriptionResult(
                        text,
                        TranscriptionSource.ANDROID_SPEECH_RECOGNIZER,
                        if (text.isNullOrBlank()) TranscriptionStatus.FAILED else TranscriptionStatus.COMPLETED
                    )
                )
                close()
            }
            override fun onError(error: Int) {
                trySend(TranscriptionResult(null, TranscriptionSource.ANDROID_SPEECH_RECOGNIZER, TranscriptionStatus.FAILED, "SpeechRecognizer error $error"))
                close()
            }
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREAN.toLanguageTag())
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        recognizer.startListening(intent)
        awaitClose {
            recognizer.stopListening()
            recognizer.destroy()
        }
    }
}

class OpenAiAudioTranscriptionProvider(
    private val client: OkHttpClient = OkHttpClient()
) : AudioTranscriptionProvider {
    override suspend fun transcribe(file: File, apiKey: String, model: String): TranscriptionResult = withContext(Dispatchers.IO) {
        try {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("model", model)
            .addFormDataPart("language", "ko")
            .addFormDataPart("file", file.name, file.asRequestBody("audio/mp4".toMediaType()))
            .build()
        val request = Request.Builder()
            .url("https://api.openai.com/v1/audio/transcriptions")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("OpenAI transcription API failed: HTTP ${response.code}")
            val text = JSONObject(raw).optString("text").trim()
            TranscriptionResult(
                text.ifBlank { null },
                TranscriptionSource.OPENAI_AUDIO,
                if (text.isBlank()) TranscriptionStatus.FAILED else TranscriptionStatus.COMPLETED
            )
        }
    } catch (e: Exception) {
        TranscriptionResult(null, TranscriptionSource.OPENAI_AUDIO, TranscriptionStatus.FAILED, e.message)
    }
    }
}
