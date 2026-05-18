package com.younghosck.beingflow.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class RoutineStateMachineTest {
    @Test
    fun startingSessionCreatesMeditationWorkSpecs() {
        val specs = RoutinePlanner.defaultSegments(RoutineSettings())

        assertEquals(listOf(SegmentType.MEDITATION, SegmentType.WORK), specs.map { it.type })
        assertEquals(listOf(300, 2400), specs.map { it.plannedDurationSeconds })
    }

    @Test
    fun segmentCompletionMovesToNoteRequired() {
        val machine = RoutineStateMachine()

        val ended = machine.markTimerEnded(machine.start())

        assertEquals(SegmentStatus.NOTE_REQUIRED, ended.statuses[0])
        assertEquals(SessionStatus.IN_PROGRESS, ended.sessionStatus)
    }

    @Test
    fun savingOrSkippingNoteAdvancesToNextSegment() {
        val machine = RoutineStateMachine()
        val ended = machine.markTimerEnded(machine.start())

        val advanced = machine.completeNote(ended, skipped = false)

        assertEquals(SegmentStatus.COMPLETED, advanced.statuses[0])
        assertEquals(SegmentStatus.READY, advanced.statuses[1])
        assertEquals(1, advanced.currentIndex)
    }

    @Test
    fun finalNoteCompletionMarksSessionCompleted() {
        val machine = RoutineStateMachine()
        val firstDone = machine.completeNote(machine.markTimerEnded(machine.start()), skipped = false)
        val finalDone = machine.completeNote(machine.markTimerEnded(firstDone), skipped = true)

        assertEquals(SessionStatus.COMPLETED, finalDone.sessionStatus)
        assertEquals(SegmentStatus.SKIPPED, finalDone.statuses[1])
    }
}
