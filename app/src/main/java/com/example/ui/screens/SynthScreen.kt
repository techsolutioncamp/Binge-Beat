package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.LfoDestination
import com.example.audio.OscillatorWaveform
import com.example.audio.SynthEngine
import com.example.audio.SynthPatch
import com.example.ui.components.KnobControl
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun SynthScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val synthPatch by viewModel.synthPatch.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
    ) {
        // Preset Browser Toolbar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = StudioSurfaceVariant,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "VIRTUAL SYNTHESIZER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SynthEngine.BUILT_IN_PRESETS.take(4).forEach { preset ->
                        val isSelected = (synthPatch.name == preset.name)
                        FilledTonalButton(
                            onClick = { viewModel.selectSynthPreset(preset) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isSelected) StudioPrimary else StudioSurfaceElevated,
                                contentColor = if (isSelected) Color.White else TextPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(preset.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Synth Sound Controls Area (Scrollable)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(10.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Section 1: Dual Oscillators
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = StudioSurface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("OSCILLATORS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StudioPrimary)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Osc 1 Waveform
                        Column {
                            Text("Osc 1 Wave", fontSize = 10.sp, color = TextMuted)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(OscillatorWaveform.SAWTOOTH, OscillatorWaveform.SQUARE, OscillatorWaveform.SINE).forEach { wave ->
                                    val sel = (synthPatch.osc1Wave == wave)
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (sel) StudioPrimary else StudioSurfaceElevated,
                                        modifier = Modifier.clickable {
                                            viewModel.updateSynthPatch(synthPatch.copy(osc1Wave = wave))
                                        }
                                    ) {
                                        Text(
                                            wave.displayName,
                                            fontSize = 10.sp,
                                            color = if (sel) Color.White else TextSecondary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Osc 2 Waveform
                        Column {
                            Text("Osc 2 Wave", fontSize = 10.sp, color = TextMuted)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(OscillatorWaveform.SQUARE, OscillatorWaveform.SAWTOOTH, OscillatorWaveform.TRIANGLE).forEach { wave ->
                                    val sel = (synthPatch.osc2Wave == wave)
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (sel) StudioSecondary else StudioSurfaceElevated,
                                        modifier = Modifier.clickable {
                                            viewModel.updateSynthPatch(synthPatch.copy(osc2Wave = wave))
                                        }
                                    ) {
                                        Text(
                                            wave.displayName,
                                            fontSize = 10.sp,
                                            color = if (sel) Color.White else TextSecondary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        KnobControl(
                            value = synthPatch.oscMix,
                            onValueChange = { viewModel.updateSynthPatch(synthPatch.copy(oscMix = it)) },
                            label = "OSC MIX",
                            activeColor = StudioPrimary,
                            sizeDp = 52
                        )
                        KnobControl(
                            value = (synthPatch.osc2DetuneSemitones + 12f) / 24f,
                            onValueChange = { viewModel.updateSynthPatch(synthPatch.copy(osc2DetuneSemitones = it * 24f - 12f)) },
                            label = "DETUNE",
                            displayValue = "${synthPatch.osc2DetuneSemitones.toInt()}st",
                            activeColor = StudioSecondary,
                            sizeDp = 52
                        )
                        KnobControl(
                            value = synthPatch.fmAmount,
                            onValueChange = { viewModel.updateSynthPatch(synthPatch.copy(fmAmount = it)) },
                            label = "FM MOD",
                            activeColor = StudioTertiary,
                            sizeDp = 52
                        )
                    }
                }
            }

            // Section 2: Resonant Filter & LFO
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = StudioSurface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("FILTER & MODULATION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StudioSecondary)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        KnobControl(
                            value = (synthPatch.filterCutoffHz - 100f) / 10000f,
                            onValueChange = {
                                viewModel.updateSynthPatch(synthPatch.copy(filterCutoffHz = 100f + it * 10000f))
                            },
                            label = "CUTOFF",
                            displayValue = "${(synthPatch.filterCutoffHz / 1000).toInt()}k",
                            activeColor = StudioSecondary,
                            sizeDp = 52
                        )
                        KnobControl(
                            value = (synthPatch.filterResonance - 0.5f) / 6f,
                            onValueChange = {
                                viewModel.updateSynthPatch(synthPatch.copy(filterResonance = 0.5f + it * 6f))
                            },
                            label = "RESONANCE",
                            activeColor = StudioYellow,
                            sizeDp = 52
                        )
                        KnobControl(
                            value = synthPatch.lfoRateHz / 15f,
                            onValueChange = {
                                viewModel.updateSynthPatch(synthPatch.copy(lfoRateHz = it * 15f))
                            },
                            label = "LFO RATE",
                            displayValue = "${synthPatch.lfoRateHz.toInt()}Hz",
                            activeColor = StudioPrimary,
                            sizeDp = 52
                        )
                        KnobControl(
                            value = synthPatch.lfoDepth,
                            onValueChange = {
                                viewModel.updateSynthPatch(synthPatch.copy(lfoDepth = it))
                            },
                            label = "LFO DEPTH",
                            activeColor = StudioBlue,
                            sizeDp = 52
                        )
                    }
                }
            }

            // Section 3: ADSR Volume Envelope
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = StudioSurface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("ADSR ENVELOPE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StudioGreen)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        KnobControl(
                            value = synthPatch.attackSec / 1.5f,
                            onValueChange = { viewModel.updateSynthPatch(synthPatch.copy(attackSec = it * 1.5f)) },
                            label = "ATTACK",
                            displayValue = "${(synthPatch.attackSec * 1000).toInt()}ms",
                            activeColor = StudioGreen,
                            sizeDp = 52
                        )
                        KnobControl(
                            value = synthPatch.decaySec / 1.5f,
                            onValueChange = { viewModel.updateSynthPatch(synthPatch.copy(decaySec = it * 1.5f)) },
                            label = "DECAY",
                            displayValue = "${(synthPatch.decaySec * 1000).toInt()}ms",
                            activeColor = StudioYellow,
                            sizeDp = 52
                        )
                        KnobControl(
                            value = synthPatch.sustainLevel,
                            onValueChange = { viewModel.updateSynthPatch(synthPatch.copy(sustainLevel = it)) },
                            label = "SUSTAIN",
                            activeColor = StudioPrimary,
                            sizeDp = 52
                        )
                        KnobControl(
                            value = synthPatch.releaseSec / 2.0f,
                            onValueChange = { viewModel.updateSynthPatch(synthPatch.copy(releaseSec = it * 2.0f)) },
                            label = "RELEASE",
                            displayValue = "${(synthPatch.releaseSec * 1000).toInt()}ms",
                            activeColor = StudioRed,
                            sizeDp = 52
                        )
                    }
                }
            }
        }

        // Live Audition Piano Keyboard (2 Octaves C3 to B4)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp),
            color = StudioSurfaceElevated,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp)) {
                Text(
                    text = "LIVE AUDITION KEYS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    modifier = Modifier.padding(start = 6.dp, top = 2.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // White keys with embedded black keys
                    val whiteNotes = listOf(48, 50, 52, 53, 55, 57, 59, 60, 62, 64, 65, 67, 69, 71)
                    whiteNotes.forEach { midi ->
                        val noteName = getMidiNoteName(midi)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(1.dp)
                                .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                                .background(Color(0xFFE2E8F0))
                                .clickable {
                                    viewModel.previewNoteOn(midi)
                                },
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Text(
                                text = noteName,
                                fontSize = 9.sp,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
