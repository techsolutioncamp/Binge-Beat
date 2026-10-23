package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.data.model.ArrangementClip
import com.example.data.model.ArrangementTrack
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun ArrangementScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val arrTracks by viewModel.arrangementTracks.collectAsState()
    val arrClips by viewModel.arrangementClips.collectAsState()
    val meterState by viewModel.meterState.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    val horizontalScrollState = rememberScrollState()
    val totalBars = 16

    var addingClipForTrack by remember { mutableStateOf<ArrangementTrack?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
    ) {
        // Toolbar: Song Structure Markers (Intro, Verse, Chorus, Outro)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = StudioSurfaceVariant,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ARRANGEMENT TIMELINE",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Intro" to 0xFF3B82F6, "Verse" to 0xFF10B981, "Chorus" to 0xFFA855F7, "Outro" to 0xFFF97316).forEach { (section, colorHex) ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(colorHex).copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(colorHex))
                        ) {
                            Text(
                                text = section,
                                fontSize = 10.sp,
                                color = Color(colorHex),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Timeline Bar Header: Bar 1 through 16
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioSurface)
                .padding(vertical = 4.dp)
        ) {
            Box(modifier = Modifier.width(110.dp).padding(start = 8.dp)) {
                Text("TRACKS", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScrollState)
            ) {
                for (bar in 1..totalBars) {
                    val isCurrentBar = isPlaying && (meterState.currentBar == bar)
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(22.dp)
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isCurrentBar) StudioGreen else StudioSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Bar $bar",
                            fontSize = 10.sp,
                            fontWeight = if (isCurrentBar) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrentBar) Color.Black else TextSecondary
                        )
                    }
                }
            }
        }

        // Tracks List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(arrTracks, key = { it.id }) { track ->
                val trackClips = arrClips.filter { it.trackId == track.id }
                ArrangementTrackRow(
                    track = track,
                    clips = trackClips,
                    currentBar = if (isPlaying) meterState.currentBar else -1,
                    horizontalScrollState = horizontalScrollState,
                    totalBars = totalBars,
                    onAddClip = { addingClipForTrack = track },
                    onDeleteClip = { clipId -> viewModel.deleteArrangementClip(clipId) }
                )
            }
        }
    }

    // Add Clip Dialog
    addingClipForTrack?.let { track ->
        var clipTitle by remember { mutableStateOf("${track.name} Pattern") }
        var startBar by remember { mutableStateOf(1) }
        var lengthBars by remember { mutableStateOf(4) }

        AlertDialog(
            onDismissRequest = { addingClipForTrack = null },
            title = { Text("Add Clip to ${track.name}", color = TextPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = clipTitle,
                        onValueChange = { clipTitle = it },
                        label = { Text("Clip Title") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = StudioPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Start Bar: $startBar", color = TextSecondary, fontSize = 12.sp)
                    Slider(
                        value = startBar.toFloat(),
                        onValueChange = { startBar = it.toInt() },
                        valueRange = 1f..13f,
                        steps = 11,
                        colors = SliderDefaults.colors(thumbColor = StudioPrimary, activeTrackColor = StudioPrimary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Length: $lengthBars Bars", color = TextSecondary, fontSize = 12.sp)
                    Slider(
                        value = lengthBars.toFloat(),
                        onValueChange = { lengthBars = it.toInt() },
                        valueRange = 2f..8f,
                        steps = 5,
                        colors = SliderDefaults.colors(thumbColor = StudioSecondary, activeTrackColor = StudioSecondary)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addArrangementClip(
                            trackId = track.id,
                            title = clipTitle,
                            startBar = startBar,
                            lengthBars = lengthBars,
                            colorHex = track.colorHex
                        )
                        addingClipForTrack = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary)
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { addingClipForTrack = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = StudioSurface
        )
    }
}

@Composable
fun ArrangementTrackRow(
    track: ArrangementTrack,
    clips: List<ArrangementClip>,
    currentBar: Int,
    horizontalScrollState: androidx.compose.foundation.ScrollState,
    totalBars: Int,
    onAddClip: () -> Unit,
    onDeleteClip: (String) -> Unit
) {
    val trackColor = Color(track.colorHex)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(StudioSurface)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track Header (Name + Add Clip Button)
        Row(
            modifier = Modifier
                .width(110.dp)
                .fillMaxHeight()
                .padding(horizontal = 6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(StudioSurfaceElevated)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.name,
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = track.type,
                    color = trackColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(
                onClick = onAddClip,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add clip",
                    tint = StudioSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Timeline Track Lanes (Clips & Grid)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .horizontalScroll(horizontalScrollState)
        ) {
            // Background bar grid lines
            Row(modifier = Modifier.fillMaxHeight()) {
                for (b in 1..totalBars) {
                    val isPlayhead = (currentBar == b)
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .fillMaxHeight()
                            .border(
                                width = 0.5.dp,
                                color = if (isPlayhead) StudioGreen.copy(alpha = 0.7f) else StudioSurfaceBorder.copy(alpha = 0.3f)
                            )
                            .background(if (isPlayhead) StudioGreen.copy(alpha = 0.1f) else Color.Transparent)
                    )
                }
            }

            // Clips
            clips.forEach { clip ->
                val startOffset = (clip.startBar - 1) * 64
                val clipWidth = clip.lengthBars * 64

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(clip.colorHex).copy(alpha = 0.85f),
                    modifier = Modifier
                        .offset(x = startOffset.dp)
                        .width(clipWidth.dp)
                        .height(52.dp)
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (clip.isAudioClip) Icons.Default.Mic else Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = clip.title,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        IconButton(
                            onClick = { onDeleteClip(clip.id) },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
