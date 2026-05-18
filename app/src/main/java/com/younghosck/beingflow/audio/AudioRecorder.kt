package com.younghosck.beingflow.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null

    fun start(): File {
        val dir = File(context.filesDir, "voice-notes").also { it.mkdirs() }
        val file = File(dir, "note-${System.currentTimeMillis()}.m4a")
        val mediaRecorder = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder.setOutputFile(file.absolutePath)
        mediaRecorder.prepare()
        mediaRecorder.start()
        recorder = mediaRecorder
        currentFile = file
        return file
    }

    fun stop(): File? {
        val file = currentFile
        runCatching { recorder?.stop() }
        recorder?.release()
        recorder = null
        currentFile = null
        return file?.takeIf { it.exists() && it.length() > 0 }
    }
}

