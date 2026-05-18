package com.younghosck.beingflow.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.younghosck.beingflow.AppContainer
import com.younghosck.beingflow.data.DailyDiaryEntity
import com.younghosck.beingflow.data.RoutineRepository
import com.younghosck.beingflow.data.RoutineSegmentEntity
import com.younghosck.beingflow.data.RoutineSessionEntity
import com.younghosck.beingflow.data.VoiceNoteEntity
import com.younghosck.beingflow.diary.DiaryPrompt
import com.younghosck.beingflow.diary.DiaryPromptBuilder
import com.younghosck.beingflow.diary.LocalDailyJournalBuilder
import com.younghosck.beingflow.domain.DiaryStatus
import com.younghosck.beingflow.domain.MeditationType
import com.younghosck.beingflow.domain.RoutineSettings
import com.younghosck.beingflow.domain.SegmentStatus
import com.younghosck.beingflow.domain.TranscriptionSource
import com.younghosck.beingflow.domain.TranscriptionStatus
import com.younghosck.beingflow.settings.SettingsRepository
import com.younghosck.beingflow.worker.DailyDiaryWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

data class MainUiState(
    val sessions: List<RoutineSessionEntity> = emptyList(),
    val activeSession: RoutineSessionEntity? = null,
    val segments: List<RoutineSegmentEntity> = emptyList(),
    val activeSegment: RoutineSegmentEntity? = null,
    val todayNotes: List<VoiceNoteEntity> = emptyList(),
    val diary: DailyDiaryEntity? = null,
    val settings: RoutineSettings = RoutineSettings(),
    val message: String? = null,
    val isGeneratingDiary: Boolean = false
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val container: AppContainer,
    private val repository: RoutineRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val message = MutableStateFlow<String?>(null)
    private val generating = MutableStateFlow(false)
    private val activeSession = MutableStateFlow<RoutineSessionEntity?>(null)

    val uiState: StateFlow<MainUiState> = combine(
        repository.observeSessions(),
        activeSession.flatMapLatest { session -> session?.let { repository.observeSegments(it.id) } ?: flowOf(emptyList()) },
        repository.observeTodayNotes(),
        repository.observeDiary(LocalDate.now()),
        settingsRepository.settings,
        message,
        generating
    ) { values ->
        val sessions = values[0] as List<RoutineSessionEntity>
        val segments = values[1] as List<RoutineSegmentEntity>
        val notes = values[2] as List<VoiceNoteEntity>
        val diary = values[3] as DailyDiaryEntity?
        val settings = values[4] as RoutineSettings
        val msg = values[5] as String?
        val isGenerating = values[6] as Boolean
        val active = activeSession.value ?: sessions.firstOrNull { it.status.name == "IN_PROGRESS" }?.also { activeSession.value = it }
        MainUiState(
            sessions = sessions,
            activeSession = active,
            segments = segments,
            activeSegment = segments.firstOrNull { it.status == SegmentStatus.RUNNING || it.status == SegmentStatus.NOTE_REQUIRED },
            todayNotes = notes,
            diary = diary,
            settings = settings,
            message = msg,
            isGeneratingDiary = isGenerating
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    init {
        refreshActive()
    }

    fun startSession() = viewModelScope.launch {
        val id = repository.startSession(settingsRepository.readSettings())
        activeSession.value = repository.currentRunningOrNoteRequired()?.first
        message.value = "새 루틴을 시작했습니다."
    }

    fun refreshActive() = viewModelScope.launch {
        activeSession.value = repository.currentRunningOrNoteRequired()?.first
    }

    fun configureSegment(segmentId: Long, meditationType: MeditationType?, taskLabel: String?) = viewModelScope.launch {
        repository.configureCurrentSegment(segmentId, meditationType, taskLabel)
    }

    fun timerFinished(segment: RoutineSegmentEntity) = viewModelScope.launch {
        if (segment.status == SegmentStatus.RUNNING) {
            repository.markTimerEnded(segment.id)
            container.timerNotifier.notifyTimerComplete(segmentLabel(segment))
            message.value = "타이머가 끝났습니다. 음성 기록을 남겨주세요."
        }
    }

    fun saveNote(
        segment: RoutineSegmentEntity,
        audioPath: String?,
        transcript: String?,
        edited: Boolean,
        source: TranscriptionSource,
        status: TranscriptionStatus
    ) = viewModelScope.launch {
        repository.saveNote(segment, audioPath, transcript, edited, source, status, skipped = false)
        refreshActive()
        message.value = "기록을 저장했습니다."
    }

    fun skipNote(segment: RoutineSegmentEntity) = viewModelScope.launch {
        repository.saveNote(segment, null, null, false, TranscriptionSource.NONE, TranscriptionStatus.NONE, skipped = true)
        refreshActive()
        message.value = "기록을 건너뛰었습니다."
    }

    fun saveSettings(settings: RoutineSettings, apiKey: String?) {
        settingsRepository.save(settings)
        if (!apiKey.isNullOrBlank()) settingsRepository.saveApiKey(apiKey)
        DailyDiaryWorker.schedule(container.application, settings.diaryHour, settings.diaryMinute)
        message.value = "설정을 저장했습니다."
    }

    fun generateDiary() = viewModelScope.launch {
        val settings = settingsRepository.readSettings()
        val apiKey = settingsRepository.apiKey()
        if (!settings.openAiDiaryEnabled) {
            saveLocalJournal("OpenAI 없이 오늘 기록을 정리했습니다.")
            return@launch
        }
        if (apiKey.isNullOrBlank()) {
            saveLocalJournal("API 키가 없어 로컬 기록 초안을 저장했습니다.")
            return@launch
        }
        generating.value = true
        if (settings.openAiTranscriptionEnabled) {
            repository.pendingAudioNotes().forEach { note ->
                val path = note.audioPath ?: return@forEach
                val result = container.openAiAudioTranscriptionProvider.transcribe(
                    File(path),
                    apiKey,
                    settings.transcriptionModel
                )
                if (result.status == TranscriptionStatus.COMPLETED) {
                    repository.updateNote(
                        note.copy(
                            transcript = result.text,
                            transcriptionSource = result.source,
                            transcriptionStatus = result.status
                        )
                    )
                }
            }
        }
        when (val prompt = DiaryPromptBuilder.build(repository.getTodayNotes())) {
            is DiaryPrompt.Empty -> {
                repository.saveDiary(LocalDate.now(), prompt.message, emptyList(), DiaryStatus.DRAFT)
                message.value = prompt.message
            }
            is DiaryPrompt.Ready -> {
                val result = container.openAiDiaryGenerator.generate(apiKey, settings.diaryModel, prompt.prompt)
                result.fold(
                    onSuccess = {
                        repository.saveDiary(LocalDate.now(), it, prompt.sourceNoteIds, DiaryStatus.GENERATED)
                        message.value = "오늘 일기를 생성했습니다."
                    },
                    onFailure = {
                        repository.saveDiary(LocalDate.now(), "일기 생성 실패: ${it.message}", prompt.sourceNoteIds, DiaryStatus.FAILED)
                        message.value = "일기 생성에 실패했습니다. 나중에 다시 시도할 수 있습니다."
                    }
                )
            }
        }
        generating.value = false
    }

    private suspend fun saveLocalJournal(successMessage: String) {
        val local = LocalDailyJournalBuilder.build(repository.getTodayNotes())
        repository.saveDiary(LocalDate.now(), local.content, local.sourceNoteIds, DiaryStatus.DRAFT)
        message.value = successMessage
    }

    fun clearMessage() {
        message.value = null
    }

    private fun segmentLabel(segment: RoutineSegmentEntity): String =
        if (segment.type.name == "WORK") "작업" else "명상"

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(container, container.repository, container.settingsRepository) as T
    }
}
