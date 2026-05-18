package com.younghosck.beingflow.diary

import com.younghosck.beingflow.data.VoiceNoteEntity
import com.younghosck.beingflow.domain.NoteType
import com.younghosck.beingflow.domain.TranscriptionStatus

object DiaryPromptBuilder {
    fun build(notes: List<VoiceNoteEntity>): DiaryPrompt {
        val completed = notes.filter {
            it.transcriptionStatus == TranscriptionStatus.COMPLETED && !it.transcript.isNullOrBlank()
        }
        if (completed.isEmpty()) {
            return DiaryPrompt.Empty("오늘 기록이 없어 일기를 생성하지 않았습니다.")
        }
        val body = completed.joinToString("\n") { note ->
            val type = when (note.noteType) {
                NoteType.MEDITATION_OBSERVATION -> "명상 관찰"
                NoteType.WORK_REPORT -> "작업 기록"
            }
            "- [$type] ${note.transcript!!.trim()}"
        }
        return DiaryPrompt.Ready(
            sourceNoteIds = completed.map { it.id },
            prompt = """
                You are writing a factual Korean daily diary for the user based only on the provided voice note transcripts.
                Do not invent events, emotions, achievements, or insights that are not supported by the notes.
                Use a calm first-person style. Keep it concise.
                Structure the diary with these sections: 오늘 한 일, 명상 중 관찰한 것, 작업 흐름, 한 줄 회고.
                If the notes are sparse, say so naturally.

                Voice note transcripts:
                $body
            """.trimIndent()
        )
    }
}

sealed interface DiaryPrompt {
    data class Ready(val prompt: String, val sourceNoteIds: List<Long>) : DiaryPrompt
    data class Empty(val message: String) : DiaryPrompt
}

