package com.younghosck.beingflow

import android.app.Application
import com.younghosck.beingflow.audio.AudioRecorder
import com.younghosck.beingflow.data.BeingFlowDatabase
import com.younghosck.beingflow.data.RoutineRepository
import com.younghosck.beingflow.diary.OpenAiDiaryGenerator
import com.younghosck.beingflow.settings.SettingsRepository
import com.younghosck.beingflow.timer.TimerNotifier
import com.younghosck.beingflow.transcription.AndroidLiveSpeechTranscriptionProvider
import com.younghosck.beingflow.transcription.OpenAiAudioTranscriptionProvider
import com.younghosck.beingflow.worker.DailyDiaryWorker

class BeingFlowApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        val settings = container.settingsRepository.readSettings()
        DailyDiaryWorker.schedule(this, settings.diaryHour, settings.diaryMinute)
    }
}

class AppContainer(application: Application) {
    private val database = BeingFlowDatabase.create(application)
    val repository = RoutineRepository(database.routineDao(), database.voiceNoteDao(), database.dailyDiaryDao())
    val settingsRepository = SettingsRepository(application)
    val audioRecorder = AudioRecorder(application)
    val liveTranscriptionProvider = AndroidLiveSpeechTranscriptionProvider(application)
    val openAiAudioTranscriptionProvider = OpenAiAudioTranscriptionProvider()
    val openAiDiaryGenerator = OpenAiDiaryGenerator()
    val timerNotifier = TimerNotifier(application)
}

