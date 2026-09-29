package com.example.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class VoiceRecorderHelper(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTimeMillis: Long = 0

    val isRecording: Boolean
        get() = mediaRecorder != null

    fun startRecording(): File? {
        stopRecording() // Clean up any existing instance

        val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        outputFile = file

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            try {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
                startTimeMillis = System.currentTimeMillis()
            } catch (e: Exception) {
                release()
                mediaRecorder = null
                return null
            }
        }

        return file
    }

    fun stopRecording(): Pair<File?, Int> {
        val recorder = mediaRecorder ?: return Pair(null, 0)
        val file = outputFile
        var durationSeconds = 0

        try {
            val durationMillis = System.currentTimeMillis() - startTimeMillis
            durationSeconds = (durationMillis / 1000).toInt()
            recorder.stop()
            recorder.release()
        } catch (e: Exception) {
            // Ignore stop errors if too short
        } finally {
            mediaRecorder = null
            outputFile = null
        }

        return Pair(file, maxOf(1, durationSeconds))
    }
}
