package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import com.example.ui.components.KnobControl
import com.example.ui.components.SingleMeterBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun MixerScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val meterState by viewModel.meterState.collectAsState()
    val masterVol by viewModel.masterVolume.collectAsState()
    val arrTracks by viewModel.arrangementTracks.collectAsState()

    var selectedSection by remember { mutableStateOf(0) } // 0 = Faders, 1 = Master FX Rack

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
    ) {
        // Switch between Channels and Master FX Rack
        TabRow(
            selectedTabIndex = selectedSection,
            containerColor = StudioSurfaceVariant,
            contentColor = StudioPrimary
        ) {
            Tab(
                selected = selectedSection == 0,
                onClick = { selectedSection = 0 },
                text = { Text("CHANNEL FADERS", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedSection == 1,
                onClick = { selectedSection = 1 },
                text = { Text("MASTER FX RACK", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
        }

        if (selectedSection == 0) {
            // Channel Strips View
            val horizontalScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .horizontalScroll(horizontalScrollState),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Track Channel Strips
                arrTracks.forEach { track ->
                    ChannelStrip(
                        name = track.name,
                        color = Color(track.colorHex),
                        volume = track.volume,
                        pan = track.pan,
                        isMuted = track.isMuted,
                        isSolo = track.isSolo,
                        level = meterState.leftPeak * track.volume,
                        onVolumeChange = { /* Adjust volume */ },
                        onPanChange = { /* Adjust pan */ },
                        onToggleMute = { /* Mute */ },
                        onToggleSolo = { /* Solo */ }
                    )
                }

                // Master Channel Strip
                MasterChannelStrip(
                    volume = masterVol,
                    leftLevel = meterState.leftPeak,
                    rightLevel = meterState.rightPeak,
                    isClipping = meterState.isClipping,
                    onVolumeChange = { viewModel.setMasterVolume(it) }
                )
            }
        } else {
            // Master FX Rack View
            MasterFxRack(viewModel = viewModel)
        }
    }
}

@Composable
fun ChannelStrip(
    name: String,
    color: Color,
    volume: Float,
    pan: Float,
    isMuted: Boolean,
    isSolo: Boolean,
    level: Float,
    onVolumeChange: (Float) -> Unit,
    onPanChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onToggleSolo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(82.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(8.dp),
        color = StudioSurface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Track label & color strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Pan Rotary Knob
            KnobControl(
                value = (pan + 1f) / 2f,
                onValueChange = { onPanChange(it * 2f - 1f) },
                label = "PAN",
                activeColor = StudioSecondary,
                sizeDp = 44,
                displayValue = if (pan < -0.05f) "L" else if (pan > 0.05f) "R" else "C"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Meter & Fader area
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center
            ) {
                // Peak meter
                SingleMeterBar(
                    level = level,
                    modifier = Modifier
                        .width(10.dp)
                        .fillMaxHeight()
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Vertical volume slider
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Slider(
                        value = volume,
                        onValueChange = onVolumeChange,
                        modifier = Modifier
                            .weight(1f)
                            .width(28.dp),
                        colors = SliderDefaults.colors(thumbColor = StudioPrimary, activeTrackColor = StudioPrimary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("${(volume * 100).toInt()}%", fontSize = 10.sp, color = TextSecondary)
        }
    }
}

@Composable
fun MasterChannelStrip(
    volume: Float,
    leftLevel: Float,
    rightLevel: Float,
    isClipping: Boolean,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(96.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(8.dp),
        color = StudioSurfaceElevated,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(StudioPrimary)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "MASTER",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = StudioPrimary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Dual Stereo Meters and Fader
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center
            ) {
                SingleMeterBar(level = leftLevel, modifier = Modifier.width(8.dp).fillMaxHeight())
                Spacer(modifier = Modifier.width(2.dp))
                SingleMeterBar(level = rightLevel, modifier = Modifier.width(8.dp).fillMaxHeight())

                Spacer(modifier = Modifier.width(8.dp))

                Slider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    colors = SliderDefaults.colors(thumbColor = StudioGreen, activeTrackColor = StudioGreen)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isClipping) "CLIP!" else "${(volume * 100).toInt()}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isClipping) StudioRed else StudioGreen
            )
        }
    }
}

@Composable
fun MasterFxRack(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val audioEngine = viewModel.audioEngine

    var eqLow by remember { mutableFloatStateOf(audioEngine.eqParams.lowGainDb) }
    var eqMid by remember { mutableFloatStateOf(audioEngine.eqParams.midGainDb) }
    var eqHigh by remember { mutableFloatStateOf(audioEngine.eqParams.highGainDb) }
    var eqEnabled by remember { mutableStateOf(audioEngine.eqParams.isEnabled) }

    var delayTime by remember { mutableFloatStateOf(audioEngine.delayParams.delayTimeMs) }
    var delayFeedback by remember { mutableFloatStateOf(audioEngine.delayParams.feedback) }
    var delayWet by remember { mutableFloatStateOf(audioEngine.delayParams.wetDryMix) }
    var delayEnabled by remember { mutableStateOf(audioEngine.delayParams.isEnabled) }

    var reverbRoom by remember { mutableFloatStateOf(audioEngine.reverbParams.roomSize) }
    var reverbWet by remember { mutableFloatStateOf(audioEngine.reverbParams.wetDryMix) }
    var reverbEnabled by remember { mutableStateOf(audioEngine.reverbParams.isEnabled) }

    var compRatio by remember { mutableFloatStateOf(audioEngine.compParams.ratio) }
    var compThresh by remember { mutableFloatStateOf(audioEngine.compParams.thresholdDb) }
    var compEnabled by remember { mutableStateOf(audioEngine.compParams.isEnabled) }

    var distDrive by remember { mutableFloatStateOf(audioEngine.distParams.drive) }
    var distEnabled by remember { mutableStateOf(audioEngine.distParams.isEnabled) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Parametric EQ Card
        FxCard(
            title = "3-BAND PARAMETRIC EQ",
            isEnabled = eqEnabled,
            onToggleEnabled = {
                eqEnabled = it
                audioEngine.eqParams.isEnabled = it
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                KnobControl(
                    value = (eqLow + 12f) / 24f,
                    onValueChange = {
                        eqLow = it * 24f - 12f
                        audioEngine.eqParams.lowGainDb = eqLow
                    },
                    label = "LOW (120Hz)",
                    displayValue = "${eqLow.toInt()}dB",
                    activeColor = StudioPrimary
                )
                KnobControl(
                    value = (eqMid + 12f) / 24f,
                    onValueChange = {
                        eqMid = it * 24f - 12f
                        audioEngine.eqParams.midGainDb = eqMid
                    },
                    label = "MID (1.4kHz)",
                    displayValue = "${eqMid.toInt()}dB",
                    activeColor = StudioSecondary
                )
                KnobControl(
                    value = (eqHigh + 12f) / 24f,
                    onValueChange = {
                        eqHigh = it * 24f - 12f
                        audioEngine.eqParams.highGainDb = eqHigh
                    },
                    label = "HIGH (7.5kHz)",
                    displayValue = "${eqHigh.toInt()}dB",
                    activeColor = StudioTertiary
                )
            }
        }

        // 2. Stereo Ping-Pong Delay Card
        FxCard(
            title = "STEREO PING-PONG DELAY",
            isEnabled = delayEnabled,
            onToggleEnabled = {
                delayEnabled = it
                audioEngine.delayParams.isEnabled = it
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                KnobControl(
                    value = delayTime / 800f,
                    onValueChange = {
                        delayTime = (it * 800f).coerceIn(40f, 800f)
                        audioEngine.delayParams.delayTimeMs = delayTime
                    },
                    label = "TIME",
                    displayValue = "${delayTime.toInt()}ms",
                    activeColor = StudioSecondary
                )
                KnobControl(
                    value = delayFeedback,
                    onValueChange = {
                        delayFeedback = it
                        audioEngine.delayParams.feedback = it
                    },
                    label = "FEEDBACK",
                    activeColor = StudioPrimary
                )
                KnobControl(
                    value = delayWet,
                    onValueChange = {
                        delayWet = it
                        audioEngine.delayParams.wetDryMix = it
                    },
                    label = "WET/DRY",
                    activeColor = StudioGreen
                )
            }
        }

        // 3. Lush Studio Reverb Card
        FxCard(
            title = "LUSH STUDIO REVERB",
            isEnabled = reverbEnabled,
            onToggleEnabled = {
                reverbEnabled = it
                audioEngine.reverbParams.isEnabled = it
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                KnobControl(
                    value = reverbRoom,
                    onValueChange = {
                        reverbRoom = it
                        audioEngine.reverbParams.roomSize = it
                    },
                    label = "ROOM SIZE",
                    activeColor = StudioBlue
                )
                KnobControl(
                    value = reverbWet,
                    onValueChange = {
                        reverbWet = it
                        audioEngine.reverbParams.wetDryMix = it
                    },
                    label = "MIX",
                    activeColor = StudioPrimary
                )
            }
        }

        // 4. Compressor & Saturation Card
        FxCard(
            title = "DYNAMICS & WARMTH",
            isEnabled = compEnabled || distEnabled,
            onToggleEnabled = {
                compEnabled = it
                distEnabled = it
                audioEngine.compParams.isEnabled = it
                audioEngine.distParams.isEnabled = it
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                KnobControl(
                    value = (compThresh + 40f) / 40f,
                    onValueChange = {
                        compThresh = it * 40f - 40f
                        audioEngine.compParams.thresholdDb = compThresh
                    },
                    label = "THRESH",
                    displayValue = "${compThresh.toInt()}dB",
                    activeColor = StudioYellow
                )
                KnobControl(
                    value = compRatio / 12f,
                    onValueChange = {
                        compRatio = (it * 12f).coerceIn(1f, 12f)
                        audioEngine.compParams.ratio = compRatio
                    },
                    label = "RATIO",
                    displayValue = "${compRatio.toInt()}:1",
                    activeColor = StudioYellow
                )
                KnobControl(
                    value = (distDrive - 1f) / 5f,
                    onValueChange = {
                        distDrive = 1f + it * 5f
                        audioEngine.distParams.drive = distDrive
                    },
                    label = "DRIVE",
                    displayValue = "${distDrive.toInt()}x",
                    activeColor = StudioRed
                )
            }
        }
    }
}

@Composable
fun FxCard(
    title: String,
    isEnabled: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = StudioSurface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = StudioPrimary,
                        checkedTrackColor = StudioPrimary.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}
