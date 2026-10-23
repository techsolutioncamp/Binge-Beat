package com.example.audio

import kotlin.math.*

enum class DrumType(val displayName: String, val category: String) {
    KICK_PUNCH("Punch Kick", "Kick"),
    KICK_808("808 Sub", "Kick"),
    SNARE_CRISP("Crisp Snare", "Snare"),
    SNARE_TRAP("Trap Snare", "Snare"),
    CLAP_LAYERED("Layered Clap", "Clap"),
    HIHAT_CLOSED("Closed Hat", "Hi-Hat"),
    HIHAT_OPEN("Open Hat", "Hi-Hat"),
    PERC_RIM("808 Rimshot", "Percussion"),
    PERC_COWBELL("Cowbell", "Percussion"),
    PERC_CONGA("Afro Conga", "Percussion"),
    CRASH_CYMBAL("Crash Cymbal", "Cymbal")
}

/**
 * High-performance DSP drum voice generator.
 * Produces crisp, punchy, authentic drum sounds on the fly
 * without needing external large assets, but with full support for loading PCM samples.
 */
class DrumSynthesizer(private val sampleRate: Int = 44100) {

    class ActiveDrumVoice(
        val type: DrumType,
        val velocity: Float,
        val pitchMultiplier: Float,
        val decayMultiplier: Float,
        var sampleIndex: Int = 0,
        val totalSamples: Int,
        val pan: Float = 0f,
        val customPcmData: FloatArray? = null
    ) {
        var isFinished: Boolean = false
    }

    private val activeVoices = mutableListOf<ActiveDrumVoice>()
    private val voiceLock = Any()

    fun trigger(
        type: DrumType,
        velocity: Float = 0.9f,
        pitchSemitones: Float = 0f,
        decay: Float = 1.0f,
        pan: Float = 0f,
        customPcmData: FloatArray? = null
    ) {
        val pitchMultiplier = 2.0.pow(pitchSemitones / 12.0).toFloat()
        val durationSeconds = when (type) {
            DrumType.KICK_PUNCH -> 0.35f * decay
            DrumType.KICK_808 -> 0.85f * decay
            DrumType.SNARE_CRISP, DrumType.SNARE_TRAP -> 0.28f * decay
            DrumType.CLAP_LAYERED -> 0.25f * decay
            DrumType.HIHAT_CLOSED -> 0.08f * decay
            DrumType.HIHAT_OPEN -> 0.40f * decay
            DrumType.PERC_RIM -> 0.12f * decay
            DrumType.PERC_COWBELL -> 0.35f * decay
            DrumType.PERC_CONGA -> 0.30f * decay
            DrumType.CRASH_CYMBAL -> 1.20f * decay
        }
        val totalSamples = if (customPcmData != null) {
            (customPcmData.size / pitchMultiplier).toInt().coerceAtLeast(100)
        } else {
            (durationSeconds * sampleRate).toInt().coerceAtLeast(100)
        }

        synchronized(voiceLock) {
            // Cap voices to prevent runaway CPU
            if (activeVoices.size > 24) {
                activeVoices.removeAt(0)
            }
            activeVoices.add(
                ActiveDrumVoice(
                    type = type,
                    velocity = velocity.coerceIn(0.1f, 1.0f),
                    pitchMultiplier = pitchMultiplier,
                    decayMultiplier = decay,
                    totalSamples = totalSamples,
                    pan = pan.coerceIn(-1.0f, 1.0f),
                    customPcmData = customPcmData
                )
            )
        }
    }

    /**
     * Synthesizes and sums all active drum voices into left and right audio buffers.
     */
    fun process(leftOut: FloatArray, rightOut: FloatArray, numFrames: Int) {
        synchronized(voiceLock) {
            val iterator = activeVoices.iterator()
            while (iterator.hasNext()) {
                val voice = iterator.next()
                renderVoice(voice, leftOut, rightOut, numFrames)
                if (voice.isFinished) {
                    iterator.remove()
                }
            }
        }
    }

    private fun renderVoice(
        voice: ActiveDrumVoice,
        leftOut: FloatArray,
        rightOut: FloatArray,
        numFrames: Int
    ) {
        val leftGain = (1.0f - voice.pan) * 0.5f * voice.velocity
        val rightGain = (1.0f + voice.pan) * 0.5f * voice.velocity

        val customPcm = voice.customPcmData
        if (customPcm != null) {
            // Render user custom PCM sample with pitch interpolation
            for (i in 0 until numFrames) {
                val readPos = voice.sampleIndex * voice.pitchMultiplier
                val idx = readPos.toInt()
                if (idx >= customPcm.size) {
                    voice.isFinished = true
                    break
                }
                val frac = readPos - idx
                val s1 = customPcm[idx]
                val s2 = if (idx + 1 < customPcm.size) customPcm[idx + 1] else 0f
                val sample = s1 + frac * (s2 - s1)

                leftOut[i] += sample * leftGain
                rightOut[i] += sample * rightGain
                voice.sampleIndex++
            }
            return
        }

        for (i in 0 until numFrames) {
            if (voice.sampleIndex >= voice.totalSamples) {
                voice.isFinished = true
                break
            }

            val t = voice.sampleIndex.toFloat() / sampleRate
            val progress = voice.sampleIndex.toFloat() / voice.totalSamples
            val sample = generateSample(voice.type, t, progress, voice.pitchMultiplier)

            leftOut[i] += sample * leftGain
            rightOut[i] += sample * rightGain

            voice.sampleIndex++
        }
    }

    private fun generateSample(
        type: DrumType,
        t: Float,
        progress: Float,
        pitchMult: Float
    ): Float {
        return when (type) {
            DrumType.KICK_PUNCH -> {
                // Pitch envelope drops from 160Hz to 48Hz
                val pitch = (50.0 + 120.0 * exp(-t * 35.0)) * pitchMult
                val phase = 2.0 * Math.PI * pitch * t
                val body = sin(phase).toFloat()
                val env = exp(-t * 14.0f).toFloat()
                // Click transient at start
                val click = if (t < 0.006f) sin(2.0 * Math.PI * 1800.0 * t).toFloat() * (1f - t / 0.006f) else 0f
                val out = (body * 0.85f + click * 0.35f) * env
                tanh(out * 1.4f)
            }
            DrumType.KICK_808 -> {
                // Low sub sine sweep with warm saturation
                val pitch = (38.0 + 95.0 * exp(-t * 18.0)) * pitchMult
                val phase = 2.0 * Math.PI * pitch * t
                val body = sin(phase).toFloat()
                val env = exp(-t * 4.5f).toFloat()
                val out = body * env
                tanh(out * 1.6f)
            }
            DrumType.SNARE_CRISP -> {
                // Tone + snappy white noise
                val tonePitch = 190.0 * pitchMult
                val tone = sin(2.0 * Math.PI * tonePitch * t).toFloat() * exp(-t * 24.0f).toFloat()
                val noise = ((Math.random().toFloat() * 2f - 1f) * exp(-t * 18.0f).toFloat())
                (tone * 0.45f + noise * 0.65f)
            }
            DrumType.SNARE_TRAP -> {
                val tonePitch = 220.0 * pitchMult
                val tone = sin(2.0 * Math.PI * tonePitch * t).toFloat() * exp(-t * 30.0f).toFloat()
                val noise = ((Math.random().toFloat() * 2f - 1f) * exp(-t * 22.0f).toFloat())
                tanh((tone * 0.5f + noise * 0.75f) * 1.3f)
            }
            DrumType.CLAP_LAYERED -> {
                // Multiple transient bursts (0ms, 12ms, 24ms) then main burst
                val burst1 = if (t in 0.0f..0.008f) (Math.random().toFloat() * 2f - 1f) else 0f
                val burst2 = if (t in 0.012f..0.020f) (Math.random().toFloat() * 2f - 1f) else 0f
                val burst3 = if (t in 0.024f..0.032f) (Math.random().toFloat() * 2f - 1f) else 0f
                val mainTail = (Math.random().toFloat() * 2f - 1f) * exp(-t * 22.0f).toFloat()
                (burst1 * 0.6f + burst2 * 0.7f + burst3 * 0.8f + mainTail * 0.8f) * 0.9f
            }
            DrumType.HIHAT_CLOSED -> {
                // High frequency metallic mix
                val n = (Math.random().toFloat() * 2f - 1f)
                val m1 = sin(2.0 * Math.PI * (3450.0 * pitchMult) * t).toFloat()
                val m2 = sin(2.0 * Math.PI * (5230.0 * pitchMult) * t).toFloat()
                val metallic = (n * 0.6f + m1 * 0.2f + m2 * 0.2f)
                val env = exp(-t * 85.0f).toFloat()
                metallic * env * 0.75f
            }
            DrumType.HIHAT_OPEN -> {
                val n = (Math.random().toFloat() * 2f - 1f)
                val m1 = sin(2.0 * Math.PI * (3450.0 * pitchMult) * t).toFloat()
                val m2 = sin(2.0 * Math.PI * (6120.0 * pitchMult) * t).toFloat()
                val metallic = (n * 0.7f + m1 * 0.15f + m2 * 0.15f)
                val env = exp(-t * 12.0f).toFloat()
                metallic * env * 0.7f
            }
            DrumType.PERC_RIM -> {
                val pitch = 850.0 * pitchMult
                val tone = sin(2.0 * Math.PI * pitch * t).toFloat()
                val noise = (Math.random().toFloat() * 2f - 1f) * 0.3f
                val env = exp(-t * 60.0f).toFloat()
                (tone * 0.7f + noise) * env * 0.9f
            }
            DrumType.PERC_COWBELL -> {
                val f1 = 587.0 * pitchMult
                val f2 = 845.0 * pitchMult
                val osc1 = if (sin(2.0 * Math.PI * f1 * t) > 0) 0.5f else -0.5f
                val osc2 = if (sin(2.0 * Math.PI * f2 * t) > 0) 0.5f else -0.5f
                val env = exp(-t * 15.0f).toFloat()
                (osc1 + osc2) * env * 0.6f
            }
            DrumType.PERC_CONGA -> {
                val pitch = (240.0 + 100.0 * exp(-t * 40.0)) * pitchMult
                val tone = sin(2.0 * Math.PI * pitch * t).toFloat()
                val env = exp(-t * 18.0f).toFloat()
                tone * env * 0.85f
            }
            DrumType.CRASH_CYMBAL -> {
                val n = (Math.random().toFloat() * 2f - 1f)
                val env = exp(-t * 3.5f).toFloat()
                n * env * 0.5f
            }
        }
    }
}
