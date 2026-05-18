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

    @Test
    fun localJournalBuildsDraftWithoutOpenAi() {
        val journal = LocalDailyJournalBuilder.build(
            listOf(
                note(1, "호흡이 짧아졌다.", TranscriptionStatus.COMPLETED),
                note(2, "코딩 작업을 했다.", TranscriptionStatus.COMPLETED, NoteType.WORK_REPORT)
            )
        )

        assertTrue(journal.content.contains("오늘 한 일"))
        assertTrue(journal.content.contains("코딩 작업을 했다."))
        assertTrue(journal.content.contains("호흡이 짧아졌다."))
        assertTrue(journal.sourceNoteIds == listOf(1L, 2L))
    }

    private fun note(
        id: Long,
        transcript: String?,
        status: TranscriptionStatus,
        noteType: NoteType = NoteType.MEDITATION_OBSERVATION
    ) = VoiceNoteEntity(
        id = id,
        sessionId = 1,
        segmentId = 1,
        noteType = noteType,
        audioPath = null,
        transcript = transcript,
        transcriptEdited = false,
        transcriptionSource = TranscriptionSource.ANDROID_SPEECH_RECOGNIZER,
        transcriptionStatus = status,
        createdAt = 0
    )
}
