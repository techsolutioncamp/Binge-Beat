package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.*

/**
 * Microphone Audio Recording Studio engine.
 * Records high-fidelity 16-bit PCM audio, streams live input VU meter dB,
 * captures waveform peaks for instant visual rendering, and packages into standard WAV.
 */
class AudioRecorder(private val context: Context) {

    private val TAG = "BeatForgeRecorder"
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private val isRecording = AtomicBoolean(false)

    data class RecordingState(
        val isRecording: Boolean = false,
        val durationMs: Long = 0L,
        val inputLevelDb: Float = -60f,
        val waveformPoints: List<Float> = emptyList(),
        val lastSavedFilePath: String? = null
    )

    private val _recordingState = MutableStateFlow(RecordingState())
    val recordingState = _recordingState.asStateFlow()

    @SuppressLint("MissingPermission")
    fun startRecording(takeName: String = "Vocal_Take_${System.currentTimeMillis()}"): Boolean {
        if (isRecording.get()) return false

        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBufferSize <= 0) {
            Log.e(TAG, "AudioRecord minBufferSize invalid: $minBufferSize")
            return false
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                minBufferSize * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                return false
            }

            audioRecord?.startRecording()
            isRecording.set(true)

            val dir = File(context.filesDir, "recordings").apply { mkdirs() }
            val outputFile = File(dir, "$takeName.wav")

            recordingThread = Thread({ recordLoop(outputFile) }, "BeatForgeRecordThread").apply {
                start()
            }

            _recordingState.value = RecordingState(isRecording = true)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting recording: ${e.message}", e)
            return false
        }
    }

    fun stopRecording(): String? {
        if (!isRecording.get()) return null
        isRecording.set(false)

        try {
            recordingThread?.join(1000)
            recordingThread = null

            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recorder: ${e.message}", e)
        }

        val savedPath = _recordingState.value.lastSavedFilePath
        _recordingState.value = _recordingState.value.copy(
            isRecording = false,
            inputLevelDb = -60f
        )
        return savedPath
    }

    private fun recordLoop(outputWavFile: File) {
        val buffer = ShortArray(1024)
        val waveform = mutableListOf<Float>()
        val startTime = System.currentTimeMillis()

        var totalAudioLen: Long = 0
        val fos = FileOutputStream(outputWavFile)

        // Write 44-byte placeholder WAV header
        fos.write(ByteArray(44))

        var peakAccumulator = 0f
        var sampleCountSinceLastPoint = 0

        try {
            while (isRecording.get() && audioRecord != null) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                if (read > 0) {
                    var chunkPeak = 0f
                    for (i in 0 until read) {
                        val sampleShort = buffer[i]
                        // Little-endian write
                        fos.write(sampleShort.toInt() and 0xFF)
                        fos.write((sampleShort.toInt() shr 8) and 0xFF)
                        totalAudioLen += 2

                        val sampleNorm = abs(sampleShort / 32768.0f)
                        if (sampleNorm > chunkPeak) chunkPeak = sampleNorm
                        if (sampleNorm > peakAccumulator) peakAccumulator = sampleNorm

                        sampleCountSinceLastPoint++
                        // Every ~1500 samples (~35ms), record a waveform point
                        if (sampleCountSinceLastPoint >= 1500) {
                            waveform.add(peakAccumulator)
                            if (waveform.size > 120) {
                                waveform.removeAt(0)
                            }
                            peakAccumulator = 0f
                            sampleCountSinceLastPoint = 0
                        }
                    }

                    val peakDb = if (chunkPeak > 0.0001f) (20.0 * log10(chunkPeak.toDouble())).toFloat() else -60f
                    val elapsed = System.currentTimeMillis() - startTime

                    _recordingState.value = RecordingState(
                        isRecording = true,
                        durationMs = elapsed,
                        inputLevelDb = peakDb.coerceIn(-60f, 0f),
                        waveformPoints = ArrayList(waveform),
                        lastSavedFilePath = outputWavFile.absolutePath
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Recording stream error: ${e.message}", e)
        } finally {
            fos.close()
            // Fix up WAV header with exact sizes
            writeWavHeader(outputWavFile, totalAudioLen, sampleRate, 1, 16)
        }
    }

    private fun writeWavHeader(
        file: File,
        totalAudioLen: Long,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ) {
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8

        val raf = RandomAccessFile(file, "rw")
        raf.seek(0)

        raf.writeBytes("RIFF")
        raf.write(intToByteArrayLE(totalDataLen.toInt()))
        raf.writeBytes("WAVE")
        raf.writeBytes("fmt ")
        raf.write(intToByteArrayLE(16)) // Subchunk1Size for PCM
        raf.write(shortToByteArrayLE(1.toShort())) // AudioFormat 1 = PCM
        raf.write(shortToByteArrayLE(channels.toShort()))
        raf.write(intToByteArrayLE(sampleRate))
        raf.write(intToByteArrayLE(byteRate))
        raf.write(shortToByteArrayLE((channels * bitsPerSample / 8).toShort())) // BlockAlign
        raf.write(shortToByteArrayLE(bitsPerSample.toShort()))
        raf.writeBytes("data")
        raf.write(intToByteArrayLE(totalAudioLen.toInt()))

        raf.close()
    }

    private fun intToByteArrayLE(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }

    private fun shortToByteArrayLE(value: Short): ByteArray {
        return byteArrayOf(
            (value.toInt() and 0xFF).toByte(),
            ((value.toInt() shr 8) and 0xFF).toByte()
        )
    }
}
