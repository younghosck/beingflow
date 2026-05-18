package com.younghosck.beingflow.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        RoutineSessionEntity::class,
        RoutineSegmentEntity::class,
        VoiceNoteEntity::class,
        DailyDiaryEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class BeingFlowDatabase : RoomDatabase() {
    abstract fun routineDao(): RoutineDao
    abstract fun voiceNoteDao(): VoiceNoteDao
    abstract fun dailyDiaryDao(): DailyDiaryDao

    companion object {
        fun create(context: Context): BeingFlowDatabase =
            Room.databaseBuilder(context, BeingFlowDatabase::class.java, "being-flow.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}

