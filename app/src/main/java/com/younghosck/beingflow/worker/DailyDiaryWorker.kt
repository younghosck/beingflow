package com.younghosck.beingflow.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.younghosck.beingflow.BeingFlowApp
import com.younghosck.beingflow.diary.DiaryPrompt
import com.younghosck.beingflow.diary.DiaryPromptBuilder
import com.younghosck.beingflow.diary.LocalDailyJournalBuilder
import com.younghosck.beingflow.domain.DiaryStatus
import com.younghosck.beingflow.domain.TranscriptionStatus
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class DailyDiaryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as BeingFlowApp
        val settings = app.container.settingsRepository.readSettings()
        val apiKey = app.container.settingsRepository.apiKey()
        if (apiKey.isNullOrBlank() || !settings.openAiDiaryEnabled) {
            val local = LocalDailyJournalBuilder.build(app.container.repository.getTodayNotes())
            app.container.repository.saveDiary(LocalDate.now(), local.content, local.sourceNoteIds, DiaryStatus.DRAFT)
            return Result.success()
        }

        if (settings.openAiTranscriptionEnabled) {
            app.container.repository.pendingAudioNotes().forEach { note ->
                val path = note.audioPath ?: return@forEach
                val result = app.container.openAiAudioTranscriptionProvider.transcribe(
                    File(path),
                    apiKey,
                    settings.transcriptionModel
                )
                if (result.status == TranscriptionStatus.COMPLETED) {
                    app.container.repository.updateNote(
                        note.copy(
                            transcript = result.text,
                            transcriptionSource = result.source,
                            transcriptionStatus = result.status
                        )
                    )
                }
            }
        }

        val notes = app.container.repository.getTodayNotes()
        return when (val prompt = DiaryPromptBuilder.build(notes)) {
            is DiaryPrompt.Empty -> {
                app.container.repository.saveDiary(LocalDate.now(), prompt.message, emptyList(), DiaryStatus.DRAFT)
                Result.success()
            }
            is DiaryPrompt.Ready -> {
                val generated = app.container.openAiDiaryGenerator.generate(apiKey, settings.diaryModel, prompt.prompt)
                generated.fold(
                    onSuccess = {
                        app.container.repository.saveDiary(LocalDate.now(), it, prompt.sourceNoteIds, DiaryStatus.GENERATED)
                        Result.success()
                    },
                    onFailure = {
                        app.container.repository.saveDiary(LocalDate.now(), "일기 생성 실패: ${it.message}", prompt.sourceNoteIds, DiaryStatus.FAILED)
                        Result.retry()
                    }
                )
            }
        }
    }

    companion object {
        fun schedule(context: Context, hour: Int, minute: Int) {
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(hour, minute)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val delay = Duration.between(now, next).toMillis()
            val request = PeriodicWorkRequestBuilder<DailyDiaryWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "daily-diary",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
