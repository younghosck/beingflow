package com.younghosck.beingflow.data

import androidx.room.TypeConverter
import com.younghosck.beingflow.domain.DiaryStatus
import com.younghosck.beingflow.domain.DiaryStyle
import com.younghosck.beingflow.domain.MeditationType
import com.younghosck.beingflow.domain.NoteType
import com.younghosck.beingflow.domain.SegmentStatus
import com.younghosck.beingflow.domain.SegmentType
import com.younghosck.beingflow.domain.SessionStatus
import com.younghosck.beingflow.domain.TranscriptionSource
import com.younghosck.beingflow.domain.TranscriptionStatus

class Converters {
    @TypeConverter fun toSessionStatus(value: String): SessionStatus = enumValueOf(value)
    @TypeConverter fun fromSessionStatus(value: SessionStatus): String = value.name
    @TypeConverter fun toSegmentType(value: String): SegmentType = enumValueOf(value)
    @TypeConverter fun fromSegmentType(value: SegmentType): String = value.name
    @TypeConverter fun toMeditationType(value: String?): MeditationType? = value?.let { enumValueOf<MeditationType>(it) }
    @TypeConverter fun fromMeditationType(value: MeditationType?): String? = value?.name
    @TypeConverter fun toSegmentStatus(value: String): SegmentStatus = enumValueOf(value)
    @TypeConverter fun fromSegmentStatus(value: SegmentStatus): String = value.name
    @TypeConverter fun toNoteType(value: String): NoteType = enumValueOf(value)
    @TypeConverter fun fromNoteType(value: NoteType): String = value.name
    @TypeConverter fun toTranscriptionSource(value: String): TranscriptionSource = enumValueOf(value)
    @TypeConverter fun fromTranscriptionSource(value: TranscriptionSource): String = value.name
    @TypeConverter fun toTranscriptionStatus(value: String): TranscriptionStatus = enumValueOf(value)
    @TypeConverter fun fromTranscriptionStatus(value: TranscriptionStatus): String = value.name
    @TypeConverter fun toDiaryStyle(value: String): DiaryStyle = enumValueOf(value)
    @TypeConverter fun fromDiaryStyle(value: DiaryStyle): String = value.name
    @TypeConverter fun toDiaryStatus(value: String): DiaryStatus = enumValueOf(value)
    @TypeConverter fun fromDiaryStatus(value: DiaryStatus): String = value.name
}

