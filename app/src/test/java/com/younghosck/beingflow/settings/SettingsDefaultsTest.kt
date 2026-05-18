package com.younghosck.beingflow.settings

import com.younghosck.beingflow.domain.RoutineSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SettingsDefaultsTest {
    @Test
    fun defaultsMatchMvpRoutine() {
        val settings = RoutineSettings()

        assertEquals(5 * 60, settings.meditationSeconds)
        assertEquals(40 * 60, settings.workSeconds)
        assertEquals(5 * 60, settings.finalMeditationSeconds)
        assertEquals(22, settings.diaryHour)
        assertEquals(30, settings.diaryMinute)
        assertFalse(settings.openAiDiaryEnabled)
        assertFalse(settings.openAiTranscriptionEnabled)
    }
}

