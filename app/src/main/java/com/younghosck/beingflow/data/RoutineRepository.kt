package com.younghosck.beingflow.data

import com.younghosck.beingflow.domain.DiaryStatus
import com.younghosck.beingflow.domain.DiaryStyle
import com.younghosck.beingflow.domain.MeditationType
import com.younghosck.beingflow.domain.NoteType
import com.younghosck.beingflow.domain.RoutinePlanner
import com.younghosck.beingflow.domain.RoutineSettings
import com.younghosck.beingflow.domain.SegmentStatus
import com.younghosck.beingflow.domain.SegmentType
import com.younghosck.beingflow.domain.SessionStatus
import com.younghosck.beingflow.domain.TranscriptionSource
import com.younghosck.beingflow.domain.TranscriptionStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

class RoutineRepository(
    private val routineDao: RoutineDao,
    private val noteDao: VoiceNoteDao,
    private val diaryDao: DailyDiaryDao
) {
    fun observeSessions(): Flow<List<RoutineSessionEntity>> = routineDao.observeSessions()
    fun observeSegments(sessionId: Long): Flow<List<RoutineSegmentEntity>> = routineDao.observeSegments(sessionId)
    fun observeTodayNotes(): Flow<List<VoiceNoteEntity>> {
        val (start, end) = todayBounds()
        return noteDao.observeNotesBetween(start, end)
    }
    fun observeDiary(localDate: LocalDate): Flow<DailyDiaryEntity?> = diaryDao.observeDiary(localDate.toString())

    suspend fun startSession(settings: RoutineSettings): Long {
        val now = System.currentTimeMillis()
        val sessionId = routineDao.insertSession(RoutineSessionEntity(startedAt = now, status = SessionStatus.IN_PROGRESS))
        val specs = RoutinePlanner.defaultSegments(settings)
        val segments = specs.map {
            RoutineSegmentEntity(
                sessionId = sessionId,
                orderIndex = it.orderIndex,
                type = it.type,
                meditationType = if (it.type == SegmentType.MEDITATION) settings.defaultMeditationType else null,
                plannedDurationSeconds = it.plannedDurationSeconds,
                status = if (it.orderIndex == 0) SegmentStatus.RUNNING else SegmentStatus.NOT_STARTED,
                startedAt = if (it.orderIndex == 0) now else null,
                expectedEndAt = if (it.orderIndex == 0) now + it.plannedDurationSeconds * 1000L else null
            )
        }
        routineDao.insertSegments(segments)
        return sessionId
    }

    suspend fun configureCurrentSegment(segmentId: Long, meditationType: MeditationType?, taskLabel: String?) {
        val segment = routineDao.getSegment(segmentId) ?: return
        routineDao.updateSegment(segment.copy(meditationType = meditationType ?: segment.meditationType, taskLabel = taskLabel))
    }

    suspend fun markTimerEnded(segmentId: Long) {
        val segment = routineDao.getSegment(segmentId) ?: return
        routineDao.updateSegment(
            segment.copy(
                endedAt = System.currentTimeMillis(),
                status = SegmentStatus.NOTE_REQUIRED
            )
        )
    }

    suspend fun saveNote(
        segment: RoutineSegmentEntity,
        audioPath: String?,
        transcript: String?,
        edited: Boolean,
        source: TranscriptionSource,
        status: TranscriptionStatus,
        skipped: Boolean
    ) {
        if (!skipped) {
            noteDao.insert(
                VoiceNoteEntity(
                    sessionId = segment.sessionId,
                    segmentId = segment.id,
                    noteType = if (segment.type == SegmentType.WORK) NoteType.WORK_REPORT else NoteType.MEDITATION_OBSERVATION,
                    audioPath = audioPath,
                    transcript = transcript?.takeIf { it.isNotBlank() },
                    transcriptEdited = edited,
                    transcriptionSource = source,
                    transcriptionStatus = status,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
        completeSegmentAndAdvance(segment, skipped)
    }

    private suspend fun completeSegmentAndAdvance(segment: RoutineSegmentEntity, skipped: Boolean) {
        val segments = routineDao.getSegments(segment.sessionId)
        val done = if (skipped) SegmentStatus.SKIPPED else SegmentStatus.COMPLETED
        routineDao.updateSegment(segment.copy(status = done))
        val next = segments.firstOrNull { it.orderIndex == segment.orderIndex + 1 }
        val now = System.currentTimeMillis()
        if (next == null) {
            val session = routineDao.getSession(segment.sessionId) ?: return
            routineDao.updateSession(session.copy(status = SessionStatus.COMPLETED, completedAt = now))
        } else {
            routineDao.updateSegment(
                next.copy(
                    status = SegmentStatus.RUNNING,
                    startedAt = now,
                    expectedEndAt = now + next.plannedDurationSeconds * 1000L
                )
            )
        }
    }

    suspend fun currentRunningOrNoteRequired(): Pair<RoutineSessionEntity, RoutineSegmentEntity>? {
        val session = routineDao.getActiveSession() ?: return null
        val segment = routineDao.getActiveSegment(session.id) ?: return null
        return session to segment
    }

    suspend fun getTodayNotes(): List<VoiceNoteEntity> {
        val (start, end) = todayBounds()
        return noteDao.getNotesBetween(start, end)
    }

    suspend fun pendingAudioNotes(): List<VoiceNoteEntity> = noteDao.getPendingAudioNotes()
    suspend fun updateNote(note: VoiceNoteEntity) = noteDao.update(note)
    suspend fun saveDiary(date: LocalDate, content: String, ids: List<Long>, status: DiaryStatus) {
        diaryDao.insert(
            DailyDiaryEntity(
                localDate = date.toString(),
                generatedAt = System.currentTimeMillis(),
                style = DiaryStyle.FACTUAL,
                content = content,
                sourceNoteIds = ids.joinToString(","),
                status = status
            )
        )
    }
}

fun todayBounds(date: LocalDate = LocalDate.now()): Pair<Long, Long> {
    val zone = ZoneId.systemDefault()
    val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
    val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
    return start to end
}
