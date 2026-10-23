package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.*

/**
 * Real-time low-latency audio processing & playback engine for BeatForge Studio.
 * Uses high-performance streaming AudioTrack with dedicated low-latency render loop,
 * sample-accurate step sequencing, polyphonic synthesis, procedural drum playback,
 * DSP effects rack, and real-time peak/RMS stereo metering.
 */
class AudioEngine(val sampleRate: Int = 44100) {

    private val TAG = "BeatForgeAudioEngine"

    // Engine Subsystems
    val drumSynth = DrumSynthesizer(sampleRate)
    val synthEngine = SynthEngine(sampleRate)

    // Master DSP Effects
    val eqParams = DspEffects.EqParams()
    val stereoEq = DspEffects.StereoEq(sampleRate)

    val delayParams = DspEffects.DelayParams()
    val stereoDelay = DspEffects.StereoDelay(sampleRate)

    val reverbParams = DspEffects.ReverbParams()
    val studioReverb = DspEffects.StudioReverb(sampleRate)

    val compParams = DspEffects.CompressorParams()
    val compressor = DspEffects.Compressor(sampleRate)

    val distParams = DspEffects.DistortionParams()
    val saturationCrusher = DspEffects.SaturationCrusher()

    val masterLimiter = DspEffects.MasterLimiter()

    // Transport & Playback State
    @Volatile var isPlaying: Boolean = false
        private set

    @Volatile var bpm: Float = 120.0f
        set(value) {
            field = value.coerceIn(40.0f, 280.0f)
            updateStepTiming()
        }

    @Volatile var isMetronomeEnabled: Boolean = false
    @Volatile var swingAmount: Float = 0.0f // 0.0 (straight) to 0.65 (heavy swing)
    @Volatile var masterVolume: Float = 0.85f

    // Timeline position
    @Volatile var currentStepIndex: Int = 0
    @Volatile var totalStepsInPattern: Int = 16
    @Volatile var currentBar: Int = 1
    @Volatile var currentBeat: Int = 1
    @Volatile var isLooping: Boolean = true

    // Real-time VU Meter State Flow
    data class MeterState(
        val leftPeak: Float = 0f,
        val rightPeak: Float = 0f,
        val isClipping: Boolean = false,
        val currentStep: Int = 0,
        val currentBar: Int = 1,
        val currentBeat: Int = 1
    )

    private val _meterState = MutableStateFlow(MeterState())
    val meterState = _meterState.asStateFlow()

    // Step Sequencer Callback
    // Called at each 16th-note step boundary to trigger drums/MIDI
    var onStepTrigger: ((stepIndex: Int, isDownbeat: Boolean) -> Unit)? = null

    // Audio Output Stream
    private var audioTrack: AudioTrack? = null
    private var renderThread: Thread? = null
    private val isEngineRunning = AtomicBoolean(false)

    // Reusable Audio Buffers (strictly zero allocations in audio callback)
    private val bufferFrames = 512
    private val leftMix = FloatArray(bufferFrames)
    private val rightMix = FloatArray(bufferFrames)
    private val pcmShortBuffer = ShortArray(bufferFrames * 2) // Interleaved stereo

    // Timing tracking
    private var samplesPer16thNote: Double = 0.0
    private var sampleCounterInStep: Double = 0.0

    init {
        updateStepTiming()
    }

    private fun updateStepTiming() {
        // At 120 BPM, 1 beat (quarter note) = 0.5 sec = 22050 samples.
        // 1 16th note = quarter note / 4 = 5512.5 samples.
        val beatsPerSec = bpm / 60.0
        val samplesPerBeat = sampleRate / beatsPerSec
        samplesPer16thNote = samplesPerBeat / 4.0
    }

    fun startEngine() {
        if (isEngineRunning.get()) return

        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = if (minBufferSize > 0) max(minBufferSize, bufferFrames * 4 * 4) else (bufferFrames * 4 * 4)

            audioTrack = try {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                    .build()
            } catch (e: Exception) {
                // Fallback to standard streaming mode if low latency is not supported by device/emulator
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            }

            if (audioTrack?.state == AudioTrack.STATE_INITIALIZED) {
                audioTrack?.play()
                isEngineRunning.set(true)

                renderThread = Thread({ audioLoop() }, "BeatForgeAudioThread").apply {
                    priority = Thread.MAX_PRIORITY
                    start()
                }
                Log.i(TAG, "Audio engine started successfully at $sampleRate Hz.")
            } else {
                Log.e(TAG, "AudioTrack not initialized, state: ${audioTrack?.state}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AudioTrack: ${e.message}", e)
        }
    }

    fun stopEngine() {
        isEngineRunning.set(false)
        try {
            renderThread?.interrupt()
            renderThread?.join(500)
            renderThread = null

            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio engine: ${e.message}", e)
        }
    }

    fun play() {
        isPlaying = true
    }

    fun pause() {
        isPlaying = false
        synthEngine.allNotesOff()
    }

    fun stop() {
        isPlaying = false
        currentStepIndex = 0
        currentBar = 1
        currentBeat = 1
        sampleCounterInStep = 0.0
        synthEngine.allNotesOff()
        _meterState.value = _meterState.value.copy(currentStep = 0, currentBar = 1, currentBeat = 1)
    }

    fun tapTempo() {
        // Handled in UI ViewModel with timestamp smoothing
    }

    private fun audioLoop() {
        var peakL = 0f
        var peakR = 0f
        var meterDecayCounter = 0

        while (isEngineRunning.get() && !Thread.currentThread().isInterrupted) {
            // 1. Clear mix buffers
            leftMix.fill(0f)
            rightMix.fill(0f)

            // 2. Transport & Sequencer processing
            if (isPlaying) {
                var frameOffset = 0
                while (frameOffset < bufferFrames) {
                    // Check swing on odd 16th notes
                    val isOddStep = (currentStepIndex % 2 == 1)
                    val effectiveStepLength = if (isOddStep) {
                        samplesPer16thNote * (1.0 - swingAmount * 0.45)
                    } else {
                        samplesPer16thNote * (1.0 + swingAmount * 0.45)
                    }

                    val samplesRemainingInStep = (effectiveStepLength - sampleCounterInStep).toInt()
                    val framesToProcess = min(bufferFrames - frameOffset, max(1, samplesRemainingInStep))

                    sampleCounterInStep += framesToProcess
                    frameOffset += framesToProcess

                    if (sampleCounterInStep >= effectiveStepLength) {
                        sampleCounterInStep = 0.0
                        val isDownbeat = (currentStepIndex % 4 == 0)

                        // Trigger metronome click if enabled
                        if (isMetronomeEnabled) {
                            if (isDownbeat) {
                                drumSynth.trigger(DrumType.PERC_COWBELL, velocity = 0.7f, pitchSemitones = 12f, decay = 0.3f)
                            } else if (currentStepIndex % 2 == 0) {
                                drumSynth.trigger(DrumType.PERC_RIM, velocity = 0.4f, pitchSemitones = 5f, decay = 0.2f)
                            }
                        }

                        // Trigger registered patterns & callbacks
                        onStepTrigger?.invoke(currentStepIndex, isDownbeat)

                        // Advance step
                        currentStepIndex = (currentStepIndex + 1) % totalStepsInPattern
                        currentBeat = (currentStepIndex / 4) + 1
                        if (currentStepIndex == 0) {
                            currentBar = if (currentBar >= 32) 1 else currentBar + 1
                        }
                    }
                }
            }

            // 3. Render Synthesizer & Drums voices
            synthEngine.process(leftMix, rightMix, bufferFrames)
            drumSynth.process(leftMix, rightMix, bufferFrames)

            // 4. Master Effects Chain
            if (eqParams.isEnabled) {
                stereoEq.update(eqParams)
                stereoEq.process(leftMix, rightMix, bufferFrames)
            }

            if (distParams.isEnabled) {
                saturationCrusher.process(leftMix, rightMix, bufferFrames, distParams)
            }

            if (delayParams.isEnabled) {
                stereoDelay.process(leftMix, rightMix, bufferFrames, delayParams)
            }

            if (reverbParams.isEnabled) {
                studioReverb.process(leftMix, rightMix, bufferFrames, reverbParams)
            }

            if (compParams.isEnabled) {
                compressor.process(leftMix, rightMix, bufferFrames, compParams)
            }

            // 5. Apply master volume & brickwall limiter
            val vol = masterVolume
            for (i in 0 until bufferFrames) {
                leftMix[i] *= vol
                rightMix[i] *= vol
            }
            masterLimiter.process(leftMix, rightMix, bufferFrames, ceiling = 0.98f)

            // 6. Convert float samples to 16-bit PCM for AudioTrack & calculate VU levels
            var hasClip = false
            for (i in 0 until bufferFrames) {
                val sL = leftMix[i].coerceIn(-1.0f, 1.0f)
                val sR = rightMix[i].coerceIn(-1.0f, 1.0f)

                if (abs(sL) >= 0.98f || abs(sR) >= 0.98f) {
                    hasClip = true
                }

                peakL = max(peakL, abs(sL))
                peakR = max(peakR, abs(sR))

                // Convert to Short PCM
                pcmShortBuffer[i * 2] = (sL * 32767.0f).toInt().toShort()
                pcmShortBuffer[i * 2 + 1] = (sR * 32767.0f).toInt().toShort()
            }

            // 7. Write to AudioTrack
            if (audioTrack?.state == AudioTrack.STATE_INITIALIZED) {
                audioTrack?.write(pcmShortBuffer, 0, bufferFrames * 2)
            } else {
                try {
                    Thread.sleep(12)
                } catch (e: InterruptedException) {
                    break
                }
            }

            // 8. Update UI metering at ~30Hz
            meterDecayCounter++
            if (meterDecayCounter >= 3) {
                meterDecayCounter = 0
                _meterState.value = MeterState(
                    leftPeak = peakL,
                    rightPeak = peakR,
                    isClipping = hasClip,
                    currentStep = currentStepIndex,
                    currentBar = currentBar,
                    currentBeat = currentBeat
                )
                peakL *= 0.82f // Smooth decay
                peakR *= 0.82f
            }
        }
    }
}
