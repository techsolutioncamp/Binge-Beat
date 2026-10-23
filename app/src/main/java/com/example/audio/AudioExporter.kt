package com.example.audio

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.ArrangementTrack
import com.example.data.model.DrumTrack
import com.example.data.model.MidiNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlin.math.*

/**
 * Offline Audio Renderer and Mastering Exporter.
 * Renders complete project arrangement or drum loop to stereo 16-bit WAV
 * with EQ, Multiband/Master Compression, and Brickwall Limiter.
 */
class AudioExporter(private val context: Context) {

    data class ExportProgress(
        val isExporting: Boolean = false,
        val progress: Float = 0f, // 0.0 to 1.0
        val exportedFilePath: String? = null,
        val errorMessage: String? = null
    )

    private val _exportState = MutableStateFlow(ExportProgress())
    val exportState = _exportState.asStateFlow()

    suspend fun exportProjectToWav(
        projectTitle: String,
        bpm: Float,
        totalBars: Int,
        drumTracks: List<DrumTrack>,
        synthNotes: List<MidiNote>,
        synthPatch: SynthPatch,
        eqParams: DspEffects.EqParams,
        delayParams: DspEffects.DelayParams,
        reverbParams: DspEffects.ReverbParams,
        compParams: DspEffects.CompressorParams,
        distParams: DspEffects.DistortionParams,
        applyMasteringLimiter: Boolean = true
    ): File? = withContext(Dispatchers.IO) {
        _exportState.value = ExportProgress(isExporting = true, progress = 0.05f)

        val sampleRate = 44100
        val beatsPerBar = 4
        val totalBeats = totalBars * beatsPerBar
        val secondsPerBeat = 60.0 / bpm
        val totalSeconds = totalBeats * secondsPerBeat
        val totalFrames = (totalSeconds * sampleRate).toInt().coerceAtLeast(sampleRate)

        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val sanitizedTitle = projectTitle.replace(Regex("[^a-zA-Z0-9_]"), "_")
        val outputFile = File(dir, "${sanitizedTitle}_${System.currentTimeMillis()}.wav")

        val drumSynth = DrumSynthesizer(sampleRate)
        val synthEngine = SynthEngine(sampleRate)

        val stereoEq = DspEffects.StereoEq(sampleRate)
        val stereoDelay = DspEffects.StereoDelay(sampleRate)
        val studioReverb = DspEffects.StudioReverb(sampleRate)
        val compressor = DspEffects.Compressor(sampleRate)
        val saturationCrusher = DspEffects.SaturationCrusher()
        val masterLimiter = DspEffects.MasterLimiter()

        val fos = FileOutputStream(outputFile)
        // 44-byte placeholder header
        fos.write(ByteArray(44))

        val chunkSize = 1024
        val leftChunk = FloatArray(chunkSize)
        val rightChunk = FloatArray(chunkSize)
        val shortBytes = ByteArray(chunkSize * 4) // 2 channels * 2 bytes

        val samplesPer16th = (sampleRate * secondsPerBeat) / 4.0
        var currentFrame = 0
        var totalAudioBytes: Long = 0

        try {
            while (currentFrame < totalFrames) {
                leftChunk.fill(0f)
                rightChunk.fill(0f)

                val framesToProcess = min(chunkSize, totalFrames - currentFrame)

                // Check step triggers inside this chunk
                for (f in 0 until framesToProcess) {
                    val frameIdx = currentFrame + f
                    val stepPosition = frameIdx / samplesPer16th
                    val isStepStart = (frameIdx % samplesPer16th.toInt()) == 0
                    val currentStep16th = stepPosition.toInt() % 16

                    if (isStepStart) {
                        // Trigger drums
                        drumTracks.forEach { track ->
                            if (!track.isMuted) {
                                val step = track.steps.getOrNull(currentStep16th)
                                if (step != null && step.isActive) {
                                    val drumType = runCatching { DrumType.valueOf(track.sampleType) }
                                        .getOrDefault(DrumType.KICK_PUNCH)
                                    drumSynth.trigger(
                                        type = drumType,
                                        velocity = step.velocity * track.volume,
                                        pitchSemitones = track.pitchSemitones,
                                        pan = track.pan
                                    )
                                }
                            }
                        }

                        // Trigger synth notes scheduled at this beat
                        val currentBeatFloat = (stepPosition / 4.0).toFloat()
                        synthNotes.forEach { note ->
                            // Check if note starts approximately at this step
                            if (abs(note.startBeat - currentBeatFloat) < 0.15f) {
                                synthEngine.noteOn(note.midiNote, note.velocity, synthPatch)
                            }
                        }
                    }
                }

                // Render voices
                drumSynth.process(leftChunk, rightChunk, framesToProcess)
                synthEngine.process(leftChunk, rightChunk, framesToProcess)

                // Master FX chain
                if (eqParams.isEnabled) {
                    stereoEq.update(eqParams)
                    stereoEq.process(leftChunk, rightChunk, framesToProcess)
                }
                if (distParams.isEnabled) {
                    saturationCrusher.process(leftChunk, rightChunk, framesToProcess, distParams)
                }
                if (delayParams.isEnabled) {
                    stereoDelay.process(leftChunk, rightChunk, framesToProcess, delayParams)
                }
                if (reverbParams.isEnabled) {
                    studioReverb.process(leftChunk, rightChunk, framesToProcess, reverbParams)
                }
                if (compParams.isEnabled) {
                    compressor.process(leftChunk, rightChunk, framesToProcess, compParams)
                }
                if (applyMasteringLimiter) {
                    masterLimiter.process(leftChunk, rightChunk, framesToProcess, 0.98f)
                }

                // Write to byte array
                var byteIdx = 0
                for (f in 0 until framesToProcess) {
                    val sL = (leftChunk[f].coerceIn(-1.0f, 1.0f) * 32767f).toInt().toShort()
                    val sR = (rightChunk[f].coerceIn(-1.0f, 1.0f) * 32767f).toInt().toShort()

                    shortBytes[byteIdx++] = (sL.toInt() and 0xFF).toByte()
                    shortBytes[byteIdx++] = ((sL.toInt() shr 8) and 0xFF).toByte()
                    shortBytes[byteIdx++] = (sR.toInt() and 0xFF).toByte()
                    shortBytes[byteIdx++] = ((sR.toInt() shr 8) and 0xFF).toByte()
                }

                fos.write(shortBytes, 0, byteIdx)
                totalAudioBytes += byteIdx

                currentFrame += framesToProcess

                val prog = (currentFrame.toFloat() / totalFrames).coerceIn(0.05f, 0.95f)
                _exportState.value = _exportState.value.copy(progress = prog)
            }
        } finally {
            fos.close()
            // Fix up WAV header
            writeWavHeader(outputFile, totalAudioBytes, sampleRate, 2, 16)
        }

        _exportState.value = ExportProgress(
            isExporting = false,
            progress = 1.0f,
            exportedFilePath = outputFile.absolutePath
        )
        outputFile
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
        raf.write(intToByteArrayLE(16))
        raf.write(shortToByteArrayLE(1.toShort()))
        raf.write(shortToByteArrayLE(channels.toShort()))
        raf.write(intToByteArrayLE(sampleRate))
        raf.write(intToByteArrayLE(byteRate))
        raf.write(shortToByteArrayLE((channels * bitsPerSample / 8).toShort()))
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

    fun shareExportedFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/wav"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share BeatForge Audio"))
    }
}
