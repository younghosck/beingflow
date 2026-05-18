package com.younghosck.beingflow.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Insert suspend fun insertSession(session: RoutineSessionEntity): Long
    @Insert suspend fun insertSegments(segments: List<RoutineSegmentEntity>): List<Long>
    @Update suspend fun updateSession(session: RoutineSessionEntity)
    @Update suspend fun updateSegment(segment: RoutineSegmentEntity)

    @Query("SELECT * FROM routine_sessions ORDER BY startedAt DESC")
    fun observeSessions(): Flow<List<RoutineSessionEntity>>

    @Query("SELECT * FROM routine_sessions WHERE id = :id")
    suspend fun getSession(id: Long): RoutineSessionEntity?

    @Query("SELECT * FROM routine_sessions WHERE status = 'IN_PROGRESS' ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveSession(): RoutineSessionEntity?

    @Query("SELECT * FROM routine_segments WHERE sessionId = :sessionId ORDER BY orderIndex")
    fun observeSegments(sessionId: Long): Flow<List<RoutineSegmentEntity>>

    @Query("SELECT * FROM routine_segments WHERE sessionId = :sessionId ORDER BY orderIndex")
    suspend fun getSegments(sessionId: Long): List<RoutineSegmentEntity>

    @Query("SELECT * FROM routine_segments WHERE id = :id")
    suspend fun getSegment(id: Long): RoutineSegmentEntity?

    @Query("SELECT * FROM routine_segments WHERE sessionId = :sessionId AND status IN ('RUNNING', 'NOTE_REQUIRED') ORDER BY orderIndex LIMIT 1")
    suspend fun getActiveSegment(sessionId: Long): RoutineSegmentEntity?
}

@Dao
interface VoiceNoteDao {
    @Insert suspend fun insert(note: VoiceNoteEntity): Long
    @Update suspend fun update(note: VoiceNoteEntity)

    @Query("SELECT * FROM voice_notes WHERE createdAt BETWEEN :start AND :end ORDER BY createdAt")
    fun observeNotesBetween(start: Long, end: Long): Flow<List<VoiceNoteEntity>>

    @Query("SELECT * FROM voice_notes WHERE createdAt BETWEEN :start AND :end ORDER BY createdAt")
    suspend fun getNotesBetween(start: Long, end: Long): List<VoiceNoteEntity>

    @Query("SELECT * FROM voice_notes WHERE transcriptionStatus = 'PENDING' AND audioPath IS NOT NULL")
    suspend fun getPendingAudioNotes(): List<VoiceNoteEntity>
}

@Dao
interface DailyDiaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(diary: DailyDiaryEntity): Long

    @Query("SELECT * FROM daily_diaries WHERE localDate = :localDate ORDER BY generatedAt DESC LIMIT 1")
    fun observeDiary(localDate: String): Flow<DailyDiaryEntity?>

    @Query("SELECT * FROM daily_diaries WHERE localDate = :localDate ORDER BY generatedAt DESC LIMIT 1")
    suspend fun getDiary(localDate: String): DailyDiaryEntity?
}
