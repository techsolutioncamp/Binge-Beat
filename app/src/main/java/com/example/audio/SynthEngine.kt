package com.example.audio

import kotlin.math.*

enum class OscillatorWaveform(val displayName: String) {
    SAWTOOTH("Saw"),
    SQUARE("Square"),
    SINE("Sine"),
    TRIANGLE("Triangle"),
    NOISE("Noise")
}

enum class FilterType {
    LOW_PASS,
    HIGH_PASS,
    BAND_PASS
}

enum class LfoDestination {
    FILTER_CUTOFF,
    PITCH,
    PAN
}

data class SynthPatch(
    val name: String = "Default Lead",
    val osc1Wave: OscillatorWaveform = OscillatorWaveform.SAWTOOTH,
    val osc2Wave: OscillatorWaveform = OscillatorWaveform.SQUARE,
    val osc2DetuneSemitones: Float = 0.05f,
    val oscMix: Float = 0.5f, // 0 = osc1 only, 1 = osc2 only
    val fmAmount: Float = 0.0f,
    // ADSR Envelope
    val attackSec: Float = 0.02f,
    val decaySec: Float = 0.15f,
    val sustainLevel: Float = 0.7f,
    val releaseSec: Float = 0.25f,
    // Filter
    val filterType: FilterType = FilterType.LOW_PASS,
    val filterCutoffHz: Float = 3200f,
    val filterResonance: Float = 1.8f,
    val filterEnvAmount: Float = 0.4f,
    // LFO
    val lfoRateHz: Float = 4.0f,
    val lfoDepth: Float = 0.15f,
    val lfoDestination: LfoDestination = LfoDestination.FILTER_CUTOFF,
    // Global
    val glideSec: Float = 0.0f,
    val masterVolume: Float = 0.8f
)

class SynthEngine(private val sampleRate: Int = 44100) {

    class Voice(
        val midiNote: Int,
        val targetFreq: Float,
        var currentFreq: Float,
        var velocity: Float,
        var patch: SynthPatch
    ) {
        var phaseOsc1: Double = 0.0
        var phaseOsc2: Double = 0.0
        var phaseLfo: Double = 0.0

        // Envelope state
        enum class EnvStage { ATTACK, DECAY, SUSTAIN, RELEASE, IDLE }
        var stage: EnvStage = EnvStage.ATTACK
        var envLevel: Float = 0f
        var timeInStage: Float = 0f

        // Biquad filter state
        var x1: Float = 0f
        var x2: Float = 0f
        var y1: Float = 0f
        var y2: Float = 0f

        var isActive: Boolean = true
        var ageSamples: Long = 0
    }

    private val voices = mutableListOf<Voice>()
    private val voiceLock = Any()
    var currentPatch: SynthPatch = SynthPatch()

    companion object {
        fun midiNoteToFreq(midiNote: Int): Float {
            return (440.0 * 2.0.pow((midiNote - 69.0) / 12.0)).toFloat()
        }

        val BUILT_IN_PRESETS = listOf(
            SynthPatch(
                name = "Cyber Lead",
                osc1Wave = OscillatorWaveform.SAWTOOTH,
                osc2Wave = OscillatorWaveform.SQUARE,
                osc2DetuneSemitones = 0.12f,
                oscMix = 0.5f,
                attackSec = 0.01f,
                decaySec = 0.12f,
                sustainLevel = 0.75f,
                releaseSec = 0.2f,
                filterCutoffHz = 4500f,
                filterResonance = 2.2f,
                lfoRateHz = 5.0f,
                lfoDepth = 0.2f
            ),
            SynthPatch(
                name = "Deep 808 Bass",
                osc1Wave = OscillatorWaveform.SINE,
                osc2Wave = OscillatorWaveform.TRIANGLE,
                osc2DetuneSemitones = -12.0f, // 1 octave lower
                oscMix = 0.25f,
                fmAmount = 0.08f,
                attackSec = 0.015f,
                decaySec = 0.4f,
                sustainLevel = 0.5f,
                releaseSec = 0.35f,
                filterCutoffHz = 800f,
                filterResonance = 1.0f
            ),
            SynthPatch(
                name = "Warm Pad",
                osc1Wave = OscillatorWaveform.SAWTOOTH,
                osc2Wave = OscillatorWaveform.TRIANGLE,
                osc2DetuneSemitones = 0.08f,
                oscMix = 0.5f,
                attackSec = 0.35f,
                decaySec = 0.5f,
                sustainLevel = 0.85f,
                releaseSec = 0.6f,
                filterCutoffHz = 2200f,
                filterResonance = 1.2f,
                lfoRateHz = 1.5f,
                lfoDepth = 0.25f,
                lfoDestination = LfoDestination.FILTER_CUTOFF
            ),
            SynthPatch(
                name = "Neon Pluck",
                osc1Wave = OscillatorWaveform.SQUARE,
                osc2Wave = OscillatorWaveform.SAWTOOTH,
                osc2DetuneSemitones = 7.0f, // Fifth
                oscMix = 0.4f,
                attackSec = 0.005f,
                decaySec = 0.18f,
                sustainLevel = 0.1f,
                releaseSec = 0.15f,
                filterCutoffHz = 3800f,
                filterResonance = 3.5f,
                filterEnvAmount = 0.8f
            ),
            SynthPatch(
                name = "Electric Keys",
                osc1Wave = OscillatorWaveform.SINE,
                osc2Wave = OscillatorWaveform.SQUARE,
                osc2DetuneSemitones = 0.03f,
                oscMix = 0.35f,
                fmAmount = 0.12f,
                attackSec = 0.01f,
                decaySec = 0.35f,
                sustainLevel = 0.4f,
                releaseSec = 0.3f,
                filterCutoffHz = 2800f,
                filterResonance = 1.5f
            ),
            SynthPatch(
                name = "Chiptune 8-Bit",
                osc1Wave = OscillatorWaveform.SQUARE,
                osc2Wave = OscillatorWaveform.TRIANGLE,
                osc2DetuneSemitones = 0.0f,
                oscMix = 0.3f,
                attackSec = 0.001f,
                decaySec = 0.08f,
                sustainLevel = 0.9f,
                releaseSec = 0.05f,
                filterCutoffHz = 8000f,
                filterResonance = 1.0f
            )
        )
    }

    fun noteOn(midiNote: Int, velocity: Float = 0.8f, patch: SynthPatch = currentPatch) {
        val freq = midiNoteToFreq(midiNote)
        synchronized(voiceLock) {
            // Check if voice already playing this note
            val existing = voices.find { it.midiNote == midiNote && it.isActive }
            if (existing != null) {
                existing.stage = Voice.EnvStage.ATTACK
                existing.timeInStage = 0f
                existing.velocity = velocity
                return
            }

            // Limit polyphony to 8
            if (voices.size >= 8) {
                // Steal oldest voice
                voices.sortBy { it.ageSamples }
                voices.removeAt(0)
            }

            voices.add(
                Voice(
                    midiNote = midiNote,
                    targetFreq = freq,
                    currentFreq = freq,
                    velocity = velocity.coerceIn(0.1f, 1.0f),
                    patch = patch
                )
            )
        }
    }

    fun noteOff(midiNote: Int) {
        synchronized(voiceLock) {
            voices.filter { it.midiNote == midiNote && it.isActive }.forEach {
                it.stage = Voice.EnvStage.RELEASE
                it.timeInStage = 0f
            }
        }
    }

    fun allNotesOff() {
        synchronized(voiceLock) {
            voices.forEach {
                it.stage = Voice.EnvStage.RELEASE
                it.timeInStage = 0f
            }
        }
    }

    fun process(leftOut: FloatArray, rightOut: FloatArray, numFrames: Int) {
        synchronized(voiceLock) {
            val iterator = voices.iterator()
            while (iterator.hasNext()) {
                val voice = iterator.next()
                renderVoice(voice, leftOut, rightOut, numFrames)
                if (!voice.isActive) {
                    iterator.remove()
                }
            }
        }
    }

    private fun renderVoice(
        voice: Voice,
        leftOut: FloatArray,
        rightOut: FloatArray,
        numFrames: Int
    ) {
        val patch = voice.patch
        val dt = 1.0f / sampleRate
        val osc2PitchRatio = 2.0.pow(patch.osc2DetuneSemitones / 12.0).toFloat()

        for (i in 0 until numFrames) {
            // Update envelope
            voice.timeInStage += dt
            voice.ageSamples++

            when (voice.stage) {
                Voice.EnvStage.ATTACK -> {
                    val aTime = max(patch.attackSec, 0.001f)
                    voice.envLevel = (voice.timeInStage / aTime).coerceIn(0f, 1f)
                    if (voice.timeInStage >= aTime) {
                        voice.stage = Voice.EnvStage.DECAY
                        voice.timeInStage = 0f
                    }
                }
                Voice.EnvStage.DECAY -> {
                    val dTime = max(patch.decaySec, 0.001f)
                    val progress = (voice.timeInStage / dTime).coerceIn(0f, 1f)
                    voice.envLevel = 1.0f - progress * (1.0f - patch.sustainLevel)
                    if (voice.timeInStage >= dTime) {
                        voice.stage = Voice.EnvStage.SUSTAIN
                        voice.timeInStage = 0f
                    }
                }
                Voice.EnvStage.SUSTAIN -> {
                    voice.envLevel = patch.sustainLevel
                }
                Voice.EnvStage.RELEASE -> {
                    val rTime = max(patch.releaseSec, 0.001f)
                    val progress = (voice.timeInStage / rTime).coerceIn(0f, 1f)
                    voice.envLevel = patch.sustainLevel * (1.0f - progress)
                    if (voice.timeInStage >= rTime) {
                        voice.stage = Voice.EnvStage.IDLE
                        voice.isActive = false
                        break
                    }
                }
                Voice.EnvStage.IDLE -> {
                    voice.isActive = false
                    break
                }
            }

            // LFO
            val lfoVal = sin(2.0 * Math.PI * voice.phaseLfo).toFloat()
            voice.phaseLfo = (voice.phaseLfo + patch.lfoRateHz * dt) % 1.0

            var modulatedPitch = voice.currentFreq
            if (patch.lfoDestination == LfoDestination.PITCH) {
                modulatedPitch *= (1.0f + lfoVal * patch.lfoDepth * 0.1f)
            }

            // Oscillator 1
            val osc1Sample = renderWaveform(patch.osc1Wave, voice.phaseOsc1)
            voice.phaseOsc1 = (voice.phaseOsc1 + modulatedPitch * dt) % 1.0

            // Oscillator 2 with detune & FM
            val fmOffset = if (patch.fmAmount > 0f) osc1Sample * patch.fmAmount * 0.2 else 0.0
            val osc2Freq = modulatedPitch * osc2PitchRatio
            val osc2Sample = renderWaveform(patch.osc2Wave, voice.phaseOsc2)
            voice.phaseOsc2 = (voice.phaseOsc2 + osc2Freq * dt + fmOffset) % 1.0

            // Mix oscillators
            val rawMix = osc1Sample * (1.0f - patch.oscMix) + osc2Sample * patch.oscMix

            // Filter calculation with envelope modulation & LFO
            var cutoff = patch.filterCutoffHz + (patch.filterEnvAmount * 8000f * voice.envLevel)
            if (patch.lfoDestination == LfoDestination.FILTER_CUTOFF) {
                cutoff += lfoVal * patch.lfoDepth * 2500f
            }
            cutoff = cutoff.coerceIn(40f, 18000f)

            // Simple 2-pole resonant low-pass filter
            val q = patch.filterResonance.coerceIn(0.5f, 10f)
            val w0 = 2.0 * Math.PI * cutoff / sampleRate
            val alpha = sin(w0) / (2.0 * q)
            val cosw0 = cos(w0)

            val b0 = ((1.0 - cosw0) / 2.0).toFloat()
            val b1 = (1.0 - cosw0).toFloat()
            val b2 = ((1.0 - cosw0) / 2.0).toFloat()
            val a0 = (1.0 + alpha).toFloat()
            val a1 = (-2.0 * cosw0).toFloat()
            val a2 = (1.0 - alpha).toFloat()

            // Apply filter difference equation
            val filtered = (b0 / a0) * rawMix + (b1 / a0) * voice.x1 + (b2 / a0) * voice.x2 -
                    (a1 / a0) * voice.y1 - (a2 / a0) * voice.y2

            voice.x2 = voice.x1
            voice.x1 = rawMix
            voice.y2 = voice.y1
            voice.y1 = filtered

            val amp = voice.envLevel * voice.velocity * patch.masterVolume * 0.45f
            val out = tanh(filtered * amp)

            var pan = 0f
            if (patch.lfoDestination == LfoDestination.PAN) {
                pan = lfoVal * patch.lfoDepth
            }
            val leftGain = (1.0f - pan) * 0.5f
            val rightGain = (1.0f + pan) * 0.5f

            leftOut[i] += out * leftGain
            rightOut[i] += out * rightGain
        }
    }

    private fun renderWaveform(wave: OscillatorWaveform, phase: Double): Float {
        return when (wave) {
            OscillatorWaveform.SINE -> sin(2.0 * Math.PI * phase).toFloat()
            OscillatorWaveform.SAWTOOTH -> (2.0 * (phase - floor(phase + 0.5))).toFloat()
            OscillatorWaveform.SQUARE -> if (phase < 0.5) 1.0f else -1.0f
            OscillatorWaveform.TRIANGLE -> (2.0 * abs(2.0 * (phase - floor(phase + 0.5))) - 1.0).toFloat()
            OscillatorWaveform.NOISE -> (Math.random().toFloat() * 2f - 1f)
        }
    }
}
