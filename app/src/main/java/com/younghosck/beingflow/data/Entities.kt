package com.younghosck.beingflow.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.younghosck.beingflow.domain.DiaryStatus
import com.younghosck.beingflow.domain.DiaryStyle
import com.younghosck.beingflow.domain.MeditationType
import com.younghosck.beingflow.domain.NoteType
import com.younghosck.beingflow.domain.SegmentStatus
import com.younghosck.beingflow.domain.SegmentType
import com.younghosck.beingflow.domain.SessionStatus
import com.younghosck.beingflow.domain.TranscriptionSource
import com.younghosck.beingflow.domain.TranscriptionStatus

@Entity(tableName = "routine_sessions")
data class RoutineSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long? = null,
    val startedAt: Long,
    val completedAt: Long? = null,
    val status: SessionStatus
)

@Entity(tableName = "routine_definitions")
data class RoutineDefinitionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val meditationSeconds: Int,
    val workSeconds: Int,
    val finalMeditationSeconds: Int,
    val createdAt: Long
)

@Entity(
    tableName = "routine_segments",
    foreignKeys = [
        ForeignKey(
            entity = RoutineSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class RoutineSegmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val orderIndex: Int,
    val type: SegmentType,
    val meditationType: MeditationType? = null,
    val taskLabel: String? = null,
    val plannedDurationSeconds: Int,
    val startedAt: Long? = null,
    val expectedEndAt: Long? = null,
    val endedAt: Long? = null,
    val status: SegmentStatus
)

@Entity(
    tableName = "voice_notes",
    foreignKeys = [
        ForeignKey(
            entity = RoutineSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RoutineSegmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["segmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("segmentId")]
)
data class VoiceNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val segmentId: Long,
    val noteType: NoteType,
    val audioPath: String? = null,
    val transcript: String? = null,
    val transcriptEdited: Boolean = false,
    val transcriptionSource: TranscriptionSource,
    val transcriptionStatus: TranscriptionStatus,
    val createdAt: Long
)

@Entity(tableName = "daily_diaries")
data class DailyDiaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val localDate: String,
    val generatedAt: Long,
    val style: DiaryStyle = DiaryStyle.FACTUAL,
    val content: String,
    val sourceNoteIds: String,
    val status: DiaryStatus
)
