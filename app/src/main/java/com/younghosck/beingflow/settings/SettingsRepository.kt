package com.younghosck.beingflow.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.younghosck.beingflow.domain.MeditationType
import com.younghosck.beingflow.domain.RoutineSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("being_flow_settings", Context.MODE_PRIVATE)
    private val securePrefs: SharedPreferences = runCatching {
        val key = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "being_flow_secure",
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrElse {
        context.getSharedPreferences("being_flow_secure_fallback", Context.MODE_PRIVATE)
    }

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<RoutineSettings> = _settings

    fun readSettings(): RoutineSettings = RoutineSettings(
        meditationSeconds = prefs.getInt("meditation_seconds", 5 * 60),
        workSeconds = prefs.getInt("work_seconds", 40 * 60),
        finalMeditationSeconds = prefs.getInt("final_meditation_seconds", 5 * 60),
        diaryHour = prefs.getInt("diary_hour", 22),
        diaryMinute = prefs.getInt("diary_minute", 30),
        defaultMeditationType = enumValueOf(
            prefs.getString("meditation_type", MeditationType.SITTING.name) ?: MeditationType.SITTING.name
        ),
        openAiDiaryEnabled = prefs.getBoolean("openai_diary_enabled", false),
        openAiTranscriptionEnabled = prefs.getBoolean("openai_transcription_enabled", false),
        diaryModel = prefs.getString("diary_model", "gpt-4o-mini") ?: "gpt-4o-mini",
        transcriptionModel = prefs.getString("transcription_model", "gpt-4o-mini-transcribe")
            ?: "gpt-4o-mini-transcribe"
    )

    fun save(settings: RoutineSettings) {
        prefs.edit {
            putInt("meditation_seconds", settings.meditationSeconds)
            putInt("work_seconds", settings.workSeconds)
            putInt("final_meditation_seconds", settings.finalMeditationSeconds)
            putInt("diary_hour", settings.diaryHour)
            putInt("diary_minute", settings.diaryMinute)
            putString("meditation_type", settings.defaultMeditationType.name)
            putBoolean("openai_diary_enabled", settings.openAiDiaryEnabled)
            putBoolean("openai_transcription_enabled", settings.openAiTranscriptionEnabled)
            putString("diary_model", settings.diaryModel)
            putString("transcription_model", settings.transcriptionModel)
        }
        _settings.value = settings
    }

    fun saveApiKey(value: String) {
        securePrefs.edit { putString("openai_api_key", value.trim()) }
    }

    fun apiKey(): String? = securePrefs.getString("openai_api_key", null)?.takeIf { it.isNotBlank() }
}

