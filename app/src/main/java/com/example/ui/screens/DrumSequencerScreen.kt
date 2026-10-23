package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.DrumType
import com.example.data.model.DrumTrack
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun DrumSequencerScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val drumTracks by viewModel.drumTracks.collectAsState()
    val meterState by viewModel.meterState.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val swing by viewModel.swing.collectAsState()

    var editingTrackForControls by remember { mutableStateOf<DrumTrack?>(null) }
    var selectingSampleForTrack by remember { mutableStateOf<DrumTrack?>(null) }
    var isVelocityMode by remember { mutableStateOf(false) }

    val horizontalScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
    ) {
        // Sequencer Toolbar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = StudioSurfaceVariant,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "16-STEP SEQUENCER",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Velocity Mode Toggle
                    FilterChip(
                        selected = isVelocityMode,
                        onClick = { isVelocityMode = !isVelocityMode },
                        label = { Text("Velocity", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudioPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Swing Indicator
                    Text(
                        text = "Swing: ${(swing * 100).toInt()}%",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Randomize Button
                    IconButton(
                        onClick = { viewModel.randomizePattern() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Randomize Pattern",
                            tint = StudioSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Clear Pattern Button
                    IconButton(
                        onClick = { viewModel.clearPattern() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear Pattern",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Step numbers timeline header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioSurface)
                .padding(vertical = 4.dp)
        ) {
            // Track label placeholder
            Box(modifier = Modifier.width(115.dp).padding(start = 8.dp)) {
                Text("TRACK", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            // Steps row header (0 to 15)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScrollState)
            ) {
                for (step in 0 until 16) {
                    val isDownbeat = (step % 4 == 0)
                    val isCurrentPlayhead = isPlaying && (meterState.currentStep == step)

                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(20.dp)
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                when {
                                    isCurrentPlayhead -> StudioGreen
                                    isDownbeat -> StudioSurfaceElevated
                                    else -> StudioSurfaceVariant
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${step + 1}",
                            fontSize = 10.sp,
                            fontWeight = if (isDownbeat || isCurrentPlayhead) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrentPlayhead) Color.Black else TextSecondary
                        )
                    }
                }
            }
        }

        // Drum Tracks List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(drumTracks, key = { it.id }) { track ->
                DrumTrackRow(
                    track = track,
                    currentStep = if (isPlaying) meterState.currentStep else -1,
                    isVelocityMode = isVelocityMode,
                    horizontalScrollState = horizontalScrollState,
                    onToggleStep = { stepIdx -> viewModel.toggleStep(track.id, stepIdx) },
                    onSetStepVelocity = { stepIdx, vel -> viewModel.setStepVelocity(track.id, stepIdx, vel) },
                    onAudition = {
                        val type = runCatching { DrumType.valueOf(track.sampleType) }.getOrDefault(DrumType.KICK_PUNCH)
                        viewModel.previewDrumSound(type)
                    },
                    onOpenTrackSettings = { editingTrackForControls = track },
                    onChangeSample = { selectingSampleForTrack = track }
                )
            }
        }
    }

    // Track Settings Dialog (Volume, Pan, Pitch, Mute, Solo)
    editingTrackForControls?.let { track ->
        val currentTrack = drumTracks.find { it.id == track.id } ?: track
        TrackControlsDialog(
            track = currentTrack,
            onDismiss = { editingTrackForControls = null },
            onVolumeChange = { viewModel.setTrackVolume(currentTrack.id, it) },
            onPanChange = { viewModel.setTrackPan(currentTrack.id, it) },
            onPitchChange = { viewModel.setTrackPitch(currentTrack.id, it) },
            onToggleMute = { viewModel.toggleTrackMute(currentTrack.id) },
            onToggleSolo = { viewModel.toggleTrackSolo(currentTrack.id) },
            onChangeSample = {
                editingTrackForControls = null
                selectingSampleForTrack = currentTrack
            }
        )
    }

    // Sample Picker Dialog
    selectingSampleForTrack?.let { track ->
        SamplePickerDialog(
            currentType = runCatching { DrumType.valueOf(track.sampleType) }.getOrDefault(DrumType.KICK_PUNCH),
            onDismiss = { selectingSampleForTrack = null },
            onSelect = { newType ->
                viewModel.changeDrumSound(track.id, newType)
                selectingSampleForTrack = null
            },
            onAudition = { type -> viewModel.previewDrumSound(type) }
        )
    }
}

@Composable
fun DrumTrackRow(
    track: DrumTrack,
    currentStep: Int,
    isVelocityMode: Boolean,
    horizontalScrollState: androidx.compose.foundation.ScrollState,
    onToggleStep: (Int) -> Unit,
    onSetStepVelocity: (Int, Float) -> Unit,
    onAudition: () -> Unit,
    onOpenTrackSettings: () -> Unit,
    onChangeSample: () -> Unit
) {
    val trackColor = Color(track.colorHex)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(StudioSurface)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track Header (Audition tap + Name + Settings gear)
        Row(
            modifier = Modifier
                .width(115.dp)
                .fillMaxHeight()
                .padding(horizontal = 6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (track.isMuted) StudioSurfaceVariant.copy(alpha = 0.5f) else StudioSurfaceElevated)
                .clickable { onAudition() }
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(trackColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = track.name,
                    color = if (track.isMuted) TextMuted else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }

            IconButton(
                onClick = onOpenTrackSettings,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Track settings",
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // 16 Step Pads
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .horizontalScroll(horizontalScrollState),
            verticalAlignment = Alignment.CenterVertically
        ) {
            track.steps.forEachIndexed { stepIdx, step ->
                val isDownbeat = (stepIdx % 4 == 0)
                val isPlayheadHere = (currentStep == stepIdx)

                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .fillMaxHeight()
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when {
                                step.isActive -> trackColor.copy(alpha = step.velocity.coerceIn(0.4f, 1.0f))
                                isDownbeat -> PadInactive
                                else -> StudioSurfaceVariant
                            }
                        )
                        .border(
                            width = if (isPlayheadHere) 2.dp else 0.dp,
                            color = if (isPlayheadHere) Color.White else Color.Transparent,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable { onToggleStep(stepIdx) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isVelocityMode && step.isActive) {
                        // Velocity height indicator bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((step.velocity * 32).dp)
                                .align(Alignment.BottomCenter)
                                .background(Color.White.copy(alpha = 0.35f))
                        )
                    }

                    if (step.isActive) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrackControlsDialog(
    track: DrumTrack,
    onDismiss: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onPanChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onToggleSolo: () -> Unit,
    onChangeSample: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${track.name} Settings", color = TextPrimary, fontWeight = FontWeight.Bold)
                OutlinedButton(
                    onClick = onChangeSample,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Change Sound", fontSize = 11.sp, color = StudioSecondary)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Mute and Solo row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onToggleMute,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (track.isMuted) StudioRed else StudioSurfaceElevated
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (track.isMuted) "MUTED" else "MUTE", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onToggleSolo,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (track.isSolo) StudioYellow else StudioSurfaceElevated
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (track.isSolo) "SOLO ON" else "SOLO",
                            fontSize = 12.sp,
                            color = if (track.isSolo) Color.Black else Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Volume Slider
                Text("Volume: ${(track.volume * 100).toInt()}%", color = TextSecondary, fontSize = 12.sp)
                Slider(
                    value = track.volume,
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = StudioPrimary, activeTrackColor = StudioPrimary)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Pan Slider
                val panLabel = when {
                    track.pan < -0.05f -> "L ${(-track.pan * 100).toInt()}%"
                    track.pan > 0.05f -> "R ${(track.pan * 100).toInt()}%"
                    else -> "Center"
                }
                Text("Pan: $panLabel", color = TextSecondary, fontSize = 12.sp)
                Slider(
                    value = track.pan,
                    onValueChange = onPanChange,
                    valueRange = -1f..1f,
                    colors = SliderDefaults.colors(thumbColor = StudioSecondary, activeTrackColor = StudioSecondary)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Pitch semitones
                Text("Pitch Shift: ${track.pitchSemitones.toInt()} st", color = TextSecondary, fontSize = 12.sp)
                Slider(
                    value = track.pitchSemitones,
                    onValueChange = onPitchChange,
                    valueRange = -12f..12f,
                    steps = 23,
                    colors = SliderDefaults.colors(thumbColor = StudioTertiary, activeTrackColor = StudioTertiary)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary)
            ) {
                Text("Done")
            }
        },
        containerColor = StudioSurface
    )
}

@Composable
fun SamplePickerDialog(
    currentType: DrumType,
    onDismiss: () -> Unit,
    onSelect: (DrumType) -> Unit,
    onAudition: (DrumType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Drum Sound", color = TextPrimary) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(320.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(DrumType.values()) { drumType ->
                    val isSelected = (drumType == currentType)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) StudioPrimary.copy(alpha = 0.2f) else StudioSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onAudition(drumType)
                                onSelect(drumType)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = drumType.displayName,
                                    color = TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                                Text(text = drumType.category, color = TextMuted, fontSize = 11.sp)
                            }

                            IconButton(
                                onClick = { onAudition(drumType) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Audition",
                                    tint = StudioGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        },
        containerColor = StudioSurface
    )
}
