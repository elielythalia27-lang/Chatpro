package com.example.util

import android.media.MediaPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AudioPlayerHelper {

    private var mediaPlayer: MediaPlayer? = null

    private val _currentlyPlayingId = MutableStateFlow<String?>(null)
    val currentlyPlayingId: StateFlow<String?> = _currentlyPlayingId.asStateFlow()

    fun playAudio(id: String, filePathOrUrl: String) {
        if (_currentlyPlayingId.value == id) {
            stopAudio()
            return
        }

        stopAudio()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePathOrUrl)
                prepare()
                start()
                _currentlyPlayingId.value = id
                setOnCompletionListener {
                    stopAudio()
                }
                setOnErrorListener { _, _, _ ->
                    stopAudio()
                    true
                }
            }
        } catch (e: Exception) {
            stopAudio()
        }
    }

    fun stopAudio() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // Ignore
        } finally {
            mediaPlayer = null
            _currentlyPlayingId.value = null
        }
    }
}
