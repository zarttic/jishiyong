package com.jishiyong.speech

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.abs
import kotlin.math.max

class PcmSpeechRecorder {
    suspend fun recordSpeechPcm(
        maxDurationMillis: Long = MAX_DURATION_MILLIS
    ): ByteArray {
        return withContext(Dispatchers.IO) {
            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBufferSize <= 0) {
                throw IOException("Microphone is not available")
            }

            val bufferSize = max(minBufferSize, MIN_BUFFER_SIZE)
            val recorder = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                recorder.release()
                throw IOException("Microphone recorder failed to initialize")
            }

            val output = ByteArrayOutputStream()
            val buffer = ByteArray(bufferSize)
            var hasSpeech = false
            var lastSpeechAt = 0L
            val startedAt = SystemClock.elapsedRealtime()

            try {
                recorder.startRecording()
                while (isActive && SystemClock.elapsedRealtime() - startedAt < maxDurationMillis) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read <= 0) continue

                    output.write(buffer, 0, read)
                    val now = SystemClock.elapsedRealtime()
                    if (isSpeechFrame(buffer, read)) {
                        hasSpeech = true
                        lastSpeechAt = now
                    }
                    if (hasSpeech &&
                        now - lastSpeechAt >= SILENCE_AFTER_SPEECH_MILLIS &&
                        now - startedAt >= MIN_CAPTURE_MILLIS
                    ) {
                        break
                    }
                }
            } finally {
                runCatching { recorder.stop() }
                recorder.release()
            }

            if (!hasSpeech) {
                throw IOException("No speech detected")
            }

            output.toByteArray()
        }
    }

    private fun isSpeechFrame(buffer: ByteArray, read: Int): Boolean {
        var sum = 0L
        var samples = 0
        var index = 0
        while (index + 1 < read) {
            val low = buffer[index].toInt() and 0xFF
            val high = buffer[index + 1].toInt()
            val sample = ((high shl 8) or low).toShort().toInt()
            sum += abs(sample)
            samples += 1
            index += 2
        }
        if (samples == 0) return false
        return sum / samples >= SPEECH_AVERAGE_AMPLITUDE
    }

    private companion object {
        private const val SAMPLE_RATE = 16000
        private const val MIN_BUFFER_SIZE = 3200
        private const val MAX_DURATION_MILLIS = 10_000L
        private const val MIN_CAPTURE_MILLIS = 700L
        private const val SILENCE_AFTER_SPEECH_MILLIS = 1_000L
        private const val SPEECH_AVERAGE_AMPLITUDE = 650
    }
}
