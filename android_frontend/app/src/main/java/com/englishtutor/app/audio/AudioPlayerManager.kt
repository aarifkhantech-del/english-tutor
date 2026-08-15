package com.englishtutor.app.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class AudioPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    var isPlaying: Boolean = false
        private set

    fun playBase64Audio(base64Audio: String, onCompletion: () -> Unit) {
        stop()

        if (base64Audio.isBlank()) return

        try {
            val audioBytes = Base64.decode(base64Audio, Base64.DEFAULT)
            val tempFile = File.createTempFile("tts_${System.currentTimeMillis()}", ".mp3", context.cacheDir)
            
            FileOutputStream(tempFile).use { fos ->
                fos.write(audioBytes)
            }

            val player = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                setOnCompletionListener {
                    this@AudioPlayerManager.isPlaying = false
                    onCompletion()
                    tempFile.delete()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("AudioPlayerManager", "MediaPlayer error: what=$what extra=$extra")
                    this@AudioPlayerManager.isPlaying = false
                    onCompletion()
                    tempFile.delete()
                    true
                }
                start()
            }

            mediaPlayer = player
            isPlaying = true
            Log.d("AudioPlayerManager", "Playback started (${audioBytes.size} bytes)")
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Failed to play Base64 audio", e)
            isPlaying = false
            onCompletion()
        }
    }

    fun stop() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.stop()
                }
                mediaPlayer?.release()
            } catch (e: Exception) {
                Log.e("AudioPlayerManager", "Error stopping MediaPlayer", e)
            } finally {
                mediaPlayer = null
                isPlaying = false
            }
        }
    }

    fun release() {
        stop()
    }
}
