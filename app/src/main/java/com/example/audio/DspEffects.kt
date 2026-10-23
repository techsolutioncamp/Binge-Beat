package com.example.audio

import kotlin.math.*

/**
 * High-performance DSP Audio Effects Rack.
 * Processes stereo audio buffers in-place or into output buffers.
 */
class DspEffects(private val sampleRate: Int = 44100) {

    // --- 3-Band Parametric EQ ---
    data class EqParams(
        var lowGainDb: Float = 0f,   // Shelf at ~100Hz (-12dB to +12dB)
        var midGainDb: Float = 0f,   // Peaking at ~1200Hz (-12dB to +12dB)
        var highGainDb: Float = 0f,  // Shelf at ~8000Hz (-12dB to +12dB)
        var isEnabled: Boolean = true
    )

    class BiquadFilter {
        private var x1 = 0f
        private var x2 = 0f
        private var y1 = 0f
        private var y2 = 0f
        private var b0 = 1f
        private var b1 = 0f
        private var b2 = 0f
        private var a1 = 0f
        private var a2 = 0f

        fun process(x: Float): Float {
            val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1
            x1 = x
            y2 = y1
            y1 = y
            return y
        }

        fun setLowShelf(cutoff: Float, gainDb: Float, sampleRate: Int) {
            val a = 10.0.pow(gainDb / 40.0)
            val w0 = 2.0 * Math.PI * cutoff / sampleRate
            val cosw = cos(w0)
            val sinw = sin(w0)
            val alpha = sinw / 2.0 * sqrt(2.0)
            val sqrtA = sqrt(a)

            val a0 = (a + 1.0) + (a - 1.0) * cosw + 2.0 * sqrtA * alpha
            b0 = (a * ((a + 1.0) - (a - 1.0) * cosw + 2.0 * sqrtA * alpha) / a0).toFloat()
            b1 = (2.0 * a * ((a - 1.0) - (a + 1.0) * cosw) / a0).toFloat()
            b2 = (a * ((a + 1.0) - (a - 1.0) * cosw - 2.0 * sqrtA * alpha) / a0).toFloat()
            a1 = (-2.0 * ((a - 1.0) + (a + 1.0) * cosw) / a0).toFloat()
            a2 = (((a + 1.0) + (a - 1.0) * cosw - 2.0 * sqrtA * alpha) / a0).toFloat()
        }

        fun setPeaking(cutoff: Float, gainDb: Float, q: Float, sampleRate: Int) {
            val a = 10.0.pow(gainDb / 40.0)
            val w0 = 2.0 * Math.PI * cutoff / sampleRate
            val cosw = cos(w0)
            val alpha = sin(w0) / (2.0 * q)

            val a0 = 1.0 + alpha / a
            b0 = (1.0 + alpha * a / a0).toFloat()
            b1 = (-2.0 * cosw / a0).toFloat()
            b2 = (1.0 - alpha * a / a0).toFloat()
            a1 = (-2.0 * cosw / a0).toFloat()
            a2 = (1.0 - alpha / a / a0).toFloat()
        }

        fun setHighShelf(cutoff: Float, gainDb: Float, sampleRate: Int) {
            val a = 10.0.pow(gainDb / 40.0)
            val w0 = 2.0 * Math.PI * cutoff / sampleRate
            val cosw = cos(w0)
            val sinw = sin(w0)
            val alpha = sinw / 2.0 * sqrt(2.0)
            val sqrtA = sqrt(a)

            val a0 = (a + 1.0) - (a - 1.0) * cosw + 2.0 * sqrtA * alpha
            b0 = (a * ((a + 1.0) + (a - 1.0) * cosw + 2.0 * sqrtA * alpha) / a0).toFloat()
            b1 = (-2.0 * a * ((a - 1.0) + (a + 1.0) * cosw) / a0).toFloat()
            b2 = (a * ((a + 1.0) + (a - 1.0) * cosw - 2.0 * sqrtA * alpha) / a0).toFloat()
            a1 = (2.0 * ((a - 1.0) - (a + 1.0) * cosw) / a0).toFloat()
            a2 = (((a + 1.0) - (a - 1.0) * cosw - 2.0 * sqrtA * alpha) / a0).toFloat()
        }
    }

    class StereoEq(private val sampleRate: Int) {
        val lowL = BiquadFilter()
        val lowR = BiquadFilter()
        val midL = BiquadFilter()
        val midR = BiquadFilter()
        val highL = BiquadFilter()
        val highR = BiquadFilter()

        fun update(params: EqParams) {
            lowL.setLowShelf(120f, params.lowGainDb, sampleRate)
            lowR.setLowShelf(120f, params.lowGainDb, sampleRate)
            midL.setPeaking(1400f, params.midGainDb, 1.2f, sampleRate)
            midR.setPeaking(1400f, params.midGainDb, 1.2f, sampleRate)
            highL.setHighShelf(7500f, params.highGainDb, sampleRate)
            highR.setHighShelf(7500f, params.highGainDb, sampleRate)
        }

        fun process(left: FloatArray, right: FloatArray, count: Int) {
            for (i in 0 until count) {
                left[i] = highL.process(midL.process(lowL.process(left[i])))
                right[i] = highR.process(midR.process(lowR.process(right[i])))
            }
        }
    }

    // --- Stereo Ping-Pong Delay ---
    data class DelayParams(
        var delayTimeMs: Float = 300f,
        var feedback: Float = 0.45f,
        var wetDryMix: Float = 0.35f,
        var isPingPong: Boolean = true,
        var isEnabled: Boolean = false
    )

    class StereoDelay(private val sampleRate: Int) {
        private val maxDelaySamples = sampleRate * 2 // 2 seconds max
        private val bufferL = FloatArray(maxDelaySamples)
        private val bufferR = FloatArray(maxDelaySamples)
        private var writeIdx = 0

        fun process(left: FloatArray, right: FloatArray, count: Int, params: DelayParams) {
            if (!params.isEnabled || params.wetDryMix <= 0.001f) return

            val delaySamples = (params.delayTimeMs * 0.001f * sampleRate).toInt().coerceIn(10, maxDelaySamples - 1)
            val fb = params.feedback.coerceIn(0f, 0.95f)
            val wet = params.wetDryMix.coerceIn(0f, 1f)
            val dry = 1.0f - wet

            for (i in 0 until count) {
                val inL = left[i]
                val inR = right[i]

                val readIdx = (writeIdx - delaySamples + maxDelaySamples) % maxDelaySamples
                val delayedL = bufferL[readIdx]
                val delayedR = bufferR[readIdx]

                if (params.isPingPong) {
                    bufferL[writeIdx] = inL + delayedR * fb
                    bufferR[writeIdx] = inR + delayedL * fb
                } else {
                    bufferL[writeIdx] = inL + delayedL * fb
                    bufferR[writeIdx] = inR + delayedR * fb
                }

                writeIdx = (writeIdx + 1) % maxDelaySamples

                left[i] = inL * dry + delayedL * wet
                right[i] = inR * dry + delayedR * wet
            }
        }
    }

    // --- Lush Studio Reverb (Comb + Allpass Schroeder Model) ---
    data class ReverbParams(
        var roomSize: Float = 0.7f,
        var damping: Float = 0.3f,
        var wetDryMix: Float = 0.3f,
        var isEnabled: Boolean = false
    )

    class StudioReverb(sampleRate: Int) {
        // Tuned delays for rich reverb diffusion
        private val combDelays = intArrayOf(1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617)
        private val combBuffers = Array(combDelays.size) { FloatArray(combDelays[it]) }
        private val combIndices = IntArray(combDelays.size)
        private val combFilters = FloatArray(combDelays.size)

        private val allpassDelays = intArrayOf(556, 441, 341, 225)
        private val allpassBuffers = Array(allpassDelays.size) { FloatArray(allpassDelays[it]) }
        private val allpassIndices = IntArray(allpassDelays.size)

        fun process(left: FloatArray, right: FloatArray, count: Int, params: ReverbParams) {
            if (!params.isEnabled || params.wetDryMix <= 0.001f) return

            val room = params.roomSize.coerceIn(0.1f, 0.96f)
            val damp = params.damping.coerceIn(0.05f, 0.95f)
            val wet = params.wetDryMix.coerceIn(0f, 1f)
            val dry = 1.0f - wet

            for (i in 0 until count) {
                val input = (left[i] + right[i]) * 0.5f
                var combSum = 0f

                // Parallel Comb filters
                for (c in combBuffers.indices) {
                    val buf = combBuffers[c]
                    val idx = combIndices[c]
                    val out = buf[idx]

                    combFilters[c] = out * (1f - damp) + combFilters[c] * damp
                    buf[idx] = input + combFilters[c] * room

                    combIndices[c] = (idx + 1) % buf.size
                    combSum += out
                }

                // Series Allpass filters for diffusion
                var allpassOut = combSum * 0.25f
                for (a in allpassBuffers.indices) {
                    val buf = allpassBuffers[a]
                    val idx = allpassIndices[a]
                    val bufOut = buf[idx]

                    val newIn = allpassOut + bufOut * 0.5f
                    buf[idx] = newIn
                    allpassOut = -newIn * 0.5f + bufOut

                    allpassIndices[a] = (idx + 1) % buf.size
                }

                left[i] = left[i] * dry + allpassOut * wet
                right[i] = right[i] * dry + allpassOut * wet
            }
        }
    }

    // --- Dynamic Compressor ---
    data class CompressorParams(
        var thresholdDb: Float = -16f,
        var ratio: Float = 4.0f,
        var attackMs: Float = 15f,
        var releaseMs: Float = 120f,
        var makeupGainDb: Float = 3f,
        var isEnabled: Boolean = false
    )

    class Compressor(private val sampleRate: Int) {
        private var env = 0f

        fun process(left: FloatArray, right: FloatArray, count: Int, params: CompressorParams) {
            if (!params.isEnabled) return

            val threshLin = 10.0.pow(params.thresholdDb / 20.0).toFloat()
            val makeupLin = 10.0.pow(params.makeupGainDb / 20.0).toFloat()
            val attackCoef = exp(-1.0 / (params.attackMs * 0.001 * sampleRate)).toFloat()
            val releaseCoef = exp(-1.0 / (params.releaseMs * 0.001 * sampleRate)).toFloat()

            for (i in 0 until count) {
                val peak = max(abs(left[i]), abs(right[i]))
                env = if (peak > env) {
                    attackCoef * env + (1f - attackCoef) * peak
                } else {
                    releaseCoef * env + (1f - releaseCoef) * peak
                }

                var gain = 1.0f
                if (env > threshLin && env > 0.0001f) {
                    val overDb = 20.0 * log10((env / threshLin).toDouble())
                    val gainReductionDb = overDb * (1.0 - 1.0 / params.ratio)
                    gain = 10.0.pow(-gainReductionDb / 20.0).toFloat()
                }

                val totalGain = gain * makeupLin
                left[i] *= totalGain
                right[i] *= totalGain
            }
        }
    }

    // --- Saturation & Bitcrusher ---
    data class DistortionParams(
        var drive: Float = 1.0f,      // 1.0 to 10.0
        var bitDepth: Int = 16,       // 4 to 16 bits
        var downsample: Int = 1,      // 1 to 16
        var wetDryMix: Float = 0.5f,
        var isEnabled: Boolean = false
    )

    class SaturationCrusher {
        private var lastSampleL = 0f
        private var lastSampleR = 0f
        private var holdCount = 0

        fun process(left: FloatArray, right: FloatArray, count: Int, params: DistortionParams) {
            if (!params.isEnabled || params.wetDryMix <= 0.001f) return

            val steps = 2.0.pow(params.bitDepth.toDouble()).toFloat()
            val wet = params.wetDryMix
            val dry = 1.0f - wet

            for (i in 0 until count) {
                val inL = left[i]
                val inR = right[i]

                holdCount++
                if (holdCount >= params.downsample) {
                    holdCount = 0
                    // Saturation (Drive + tanh)
                    val drivenL = tanh(inL * params.drive)
                    val drivenR = tanh(inR * params.drive)

                    // Bit reduction
                    lastSampleL = if (params.bitDepth < 16) {
                        (round(drivenL * steps) / steps)
                    } else drivenL

                    lastSampleR = if (params.bitDepth < 16) {
                        (round(drivenR * steps) / steps)
                    } else drivenR
                }

                left[i] = inL * dry + lastSampleL * wet
                right[i] = inR * dry + lastSampleR * wet
            }
        }
    }

    // --- Master Brickwall Limiter ---
    class MasterLimiter {
        fun process(left: FloatArray, right: FloatArray, count: Int, ceiling: Float = 0.98f) {
            for (i in 0 until count) {
                left[i] = tanh(left[i]).coerceIn(-ceiling, ceiling)
                right[i] = tanh(right[i]).coerceIn(-ceiling, ceiling)
            }
        }
    }
}
