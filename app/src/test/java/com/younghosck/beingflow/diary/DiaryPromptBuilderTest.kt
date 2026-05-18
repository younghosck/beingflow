package com.younghosck.beingflow.diary

import com.younghosck.beingflow.data.VoiceNoteEntity
import com.younghosck.beingflow.domain.NoteType
import com.younghosck.beingflow.domain.TranscriptionSource
import com.younghosck.beingflow.domain.TranscriptionStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiaryPromptBuilderTest {
    @Test
    fun usesOnlyProvidedCompletedTranscripts() {
        val prompt = DiaryPromptBuilder.build(
            listOf(
                note(1, "좌선 중 호흡을 관찰했다.", TranscriptionStatus.COMPLETED),
                note(2, "이 내용은 대기 중이다.", TranscriptionStatus.PENDING)
            )
        ) as DiaryPrompt.Ready

        assertTrue(prompt.prompt.contains("좌선 중 호흡을 관찰했다."))
        assertFalse(prompt.prompt.contains("이 내용은 대기 중이다."))
        assertTrue(prompt.sourceNoteIds == listOf(1L))
    }

    @Test
    fun handlesEmptyNotes() {
        val prompt = DiaryPromptBuilder.build(emptyList())

        assertTrue(prompt is DiaryPrompt.Empty)
    }

    @Test
    fun handlesPendingTranscriptsAsEmptyWhenNoCompletedTextExists() {
        val prompt = DiaryPromptBuilder.build(
            listOf(note(1, null, TranscriptionStatus.PENDING))
        )

        assertTrue(prompt is DiaryPrompt.Empty)
    }

    private fun note(id: Long, transcript: String?, status: TranscriptionStatus) = VoiceNoteEntity(
        id = id,
        sessionId = 1,
        segmentId = 1,
        noteType = NoteType.MEDITATION_OBSERVATION,
        audioPath = null,
        transcript = transcript,
        transcriptEdited = false,
        transcriptionSource = TranscriptionSource.ANDROID_SPEECH_RECOGNIZER,
        transcriptionStatus = status,
        createdAt = 0
    )
}

