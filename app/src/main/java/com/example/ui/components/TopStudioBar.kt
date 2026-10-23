package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun TopStudioBar(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val bpm by viewModel.bpm.collectAsState()
    val isMetronome by viewModel.isMetronomeEnabled.collectAsState()
    val meterState by viewModel.meterState.collectAsState()
    val activeProject by viewModel.activeProject.collectAsState()

    var showBpmDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = StudioSurface,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
            // Row 1: Project Name, Genre, and Master Meters
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(StudioPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "BeatForge",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = activeProject?.title ?: "BeatForge Studio",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1
                        )
                        Text(
                            text = activeProject?.genre ?: "Music Studio",
                            color = StudioSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Time Counter: BAR : BEAT : 16TH
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(StudioSurfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val barStr = meterState.currentBar.toString().padStart(2, '0')
                    val beatStr = meterState.currentBeat.toString().padStart(2, '0')
                    val stepStr = ((meterState.currentStep % 4) + 1).toString().padStart(2, '0')

                    Text(
                        text = "$barStr : $beatStr : $stepStr",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isPlaying) StudioGreen else TextSecondary
                    )
                }

                // Master Peak Meters
                StereoVuMeter(
                    leftPeak = meterState.leftPeak,
                    rightPeak = meterState.rightPeak,
                    isClipping = meterState.isClipping,
                    heightDp = 30,
                    widthDp = 18
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Transport Controls (Play, Stop, BPM, Metronome)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Play / Pause Button
                Button(
                    onClick = { viewModel.togglePlay() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) StudioGreen else StudioPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPlaying) "PAUSE" else "PLAY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Stop Button
                IconButton(
                    onClick = { viewModel.stop() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // BPM / Tempo Button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StudioSurfaceVariant,
                    modifier = Modifier
                        .height(38.dp)
                        .clickable { showBpmDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Tempo",
                            tint = StudioSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${bpm.toInt()} BPM",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                    }
                }

                // Tap Tempo Button
                FilledTonalButton(
                    onClick = { viewModel.tapTempo() },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = StudioSurfaceElevated
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text("TAP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                // Metronome Toggle
                IconButton(
                    onClick = { viewModel.toggleMetronome() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isMetronome) StudioPrimary.copy(alpha = 0.25f) else StudioSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Metronome",
                        tint = if (isMetronome) StudioPrimary else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    // BPM Edit Dialog
    if (showBpmDialog) {
        var tempoInput by remember { mutableStateOf(bpm.toInt().toString()) }
        AlertDialog(
            onDismissRequest = { showBpmDialog = false },
            title = { Text("Set Project BPM", color = TextPrimary) },
            text = {
                Column {
                    Text("Enter project tempo (40 - 260 BPM):", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempoInput,
                        onValueChange = { tempoInput = it.filter { c -> c.isDigit() } },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = StudioPrimary,
                            unfocusedBorderColor = StudioSurfaceBorder
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    // Quick tempo presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(85, 105, 120, 140, 160).forEach { pBpm ->
                            FilledTonalButton(
                                onClick = {
                                    viewModel.setBpm(pBpm.toFloat())
                                    showBpmDialog = false
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("$pBpm", fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = tempoInput.toFloatOrNull() ?: bpm
                        viewModel.setBpm(parsed)
                        showBpmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBpmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = StudioSurface
        )
    }
}
