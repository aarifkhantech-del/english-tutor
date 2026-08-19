package com.englishtutor.app.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class AudioRecorderManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var isRecording = false

    fun startRecording(): File? {
        stopRecording()

        val outputDir = context.cacheDir
        val audioFile = File.createTempFile("recording_${System.currentTimeMillis()}", ".m4a", outputDir)
        currentFile = audioFile

        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        try {
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            isRecording = true
            Log.d("AudioRecorderManager", "Recording started: ${audioFile.absolutePath}")
            return audioFile
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "Failed to start recording", e)
            recorder.release()
            mediaRecorder = null
            isRecording = false
            return null
        }
    }

    fun stopRecording(): File? {
        if (!isRecording || mediaRecorder == null) {
            return currentFile
        }

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            Log.d("AudioRecorderManager", "Recording stopped: ${currentFile?.absolutePath}")
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "Failed to stop recording cleanly", e)
        } finally {
            mediaRecorder = null
            isRecording = false
        }

        return currentFile
    }

    fun release() {
        stopRecording()
    }
}
