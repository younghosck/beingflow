package com.younghosck.beingflow.domain

enum class SessionStatus { IN_PROGRESS, COMPLETED, CANCELLED }
enum class SegmentType { MEDITATION, WORK }
enum class MeditationType { SITTING, WALKING }
enum class SegmentStatus { NOT_STARTED, READY, RUNNING, ENDED, NOTE_REQUIRED, COMPLETED, SKIPPED }
enum class NoteType { MEDITATION_OBSERVATION, WORK_REPORT }
enum class TranscriptionSource { ANDROID_SPEECH_RECOGNIZER, OPENAI_AUDIO, MANUAL, NONE }
enum class TranscriptionStatus { NONE, PENDING, COMPLETED, FAILED }
enum class DiaryStyle { FACTUAL }
enum class DiaryStatus { DRAFT, GENERATED, FAILED }

data class RoutineSettings(
    val meditationSeconds: Int = 5 * 60,
    val workSeconds: Int = 40 * 60,
    val finalMeditationSeconds: Int = 5 * 60,
    val diaryHour: Int = 22,
    val diaryMinute: Int = 30,
    val defaultMeditationType: MeditationType = MeditationType.SITTING,
    val openAiDiaryEnabled: Boolean = false,
    val openAiTranscriptionEnabled: Boolean = false,
    val diaryModel: String = "gpt-4o-mini",
    val transcriptionModel: String = "gpt-4o-mini-transcribe"
)

data class RoutineSegmentSpec(
    val orderIndex: Int,
    val type: SegmentType,
    val plannedDurationSeconds: Int
)

object RoutinePlanner {
    fun defaultSegments(settings: RoutineSettings): List<RoutineSegmentSpec> = listOf(
        RoutineSegmentSpec(0, SegmentType.MEDITATION, settings.meditationSeconds),
        RoutineSegmentSpec(1, SegmentType.WORK, settings.workSeconds)
    )
}

data class RoutineProgress(
    val currentIndex: Int,
    val statuses: List<SegmentStatus>,
    val sessionStatus: SessionStatus
)

class RoutineStateMachine(segmentCount: Int = 2) {
    private val initialStatuses = List(segmentCount) { SegmentStatus.NOT_STARTED }

    fun start(): RoutineProgress = RoutineProgress(
        currentIndex = 0,
        statuses = initialStatuses.replace(0, SegmentStatus.READY),
        sessionStatus = SessionStatus.IN_PROGRESS
    )

    fun markTimerEnded(progress: RoutineProgress): RoutineProgress {
        val index = progress.currentIndex
        return progress.copy(statuses = progress.statuses.replace(index, SegmentStatus.NOTE_REQUIRED))
    }

    fun completeNote(progress: RoutineProgress, skipped: Boolean): RoutineProgress {
        val finishedIndex = progress.currentIndex
        val doneStatus = if (skipped) SegmentStatus.SKIPPED else SegmentStatus.COMPLETED
        val updated = progress.statuses.replace(finishedIndex, doneStatus)
        val next = finishedIndex + 1
        return if (next >= updated.size) {
            RoutineProgress(finishedIndex, updated, SessionStatus.COMPLETED)
        } else {
            RoutineProgress(next, updated.replace(next, SegmentStatus.READY), SessionStatus.IN_PROGRESS)
        }
    }
}

private fun <T> List<T>.replace(index: Int, value: T): List<T> =
    mapIndexed { i, old -> if (i == index) value else old }
