package com.younghosck.beingflow.timer

import com.younghosck.beingflow.data.RoutineSegmentEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.max

data class TimerSnapshot(
    val remainingSeconds: Long,
    val isFinished: Boolean
)

object TimerController {
    fun observe(segment: RoutineSegmentEntity): Flow<TimerSnapshot> = flow {
        while (true) {
            val end = segment.expectedEndAt
            val remaining = if (end == null) {
                segment.plannedDurationSeconds.toLong()
            } else {
                max(0, (end - System.currentTimeMillis()) / 1000)
            }
            emit(TimerSnapshot(remaining, remaining <= 0))
            if (remaining <= 0) break
            delay(1000)
        }
    }
}

fun Long.formatMinutesSeconds(): String {
    val minutes = this / 60
    val seconds = this % 60
    return "%02d:%02d".format(minutes, seconds)
}

