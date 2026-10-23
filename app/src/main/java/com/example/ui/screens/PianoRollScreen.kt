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
import com.example.data.model.MidiNote
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudioViewModel

enum class MusicalScale(val displayName: String, val intervals: List<Int>) {
    CHROMATIC("All Notes", (0..11).toList()),
    NATURAL_MINOR("Minor", listOf(0, 2, 3, 5, 7, 8, 10)),
    MAJOR("Major", listOf(0, 2, 4, 5, 7, 9, 11)),
    PENTATONIC_MINOR("Pentatonic", listOf(0, 3, 5, 7, 10)),
    BLUES("Blues", listOf(0, 3, 5, 6, 7, 10)),
    DORIAN("Dorian", listOf(0, 2, 3, 5, 7, 9, 10))
}

@Composable
fun PianoRollScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val midiNotes by viewModel.midiNotes.collectAsState()
    val meterState by viewModel.meterState.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    var activeScale by remember { mutableStateOf(MusicalScale.NATURAL_MINOR) }
    var rootNoteOffset by remember { mutableStateOf(0) } // 0 = C
    var selectedOctaveRange by remember { mutableStateOf(3) } // Octave 3 (C3 to B4)

    val horizontalScrollState = rememberScrollState()

    // 24 MIDI notes for the selected 2-octave window (e.g. C3 = 48 to B4 = 71)
    val startNote = (selectedOctaveRange + 1) * 12
    val noteRange = (startNote + 23 downTo startNote).toList()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
    ) {
        // Toolbar: Scale Selector, Octave, Chord Helper, Clear
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = StudioSurfaceVariant,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Octave Range Buttons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Octave:", color = TextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        listOf(2, 3, 4, 5).forEach { oct ->
                            FilledTonalButton(
                                onClick = { selectedOctaveRange = oct },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (selectedOctaveRange == oct) StudioPrimary else StudioSurfaceElevated,
                                    contentColor = if (selectedOctaveRange == oct) Color.White else TextSecondary
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp).padding(horizontal = 2.dp)
                            ) {
                                Text("C$oct", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Scale Selector Dropdown
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Scale:", color = TextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        var scaleMenuExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { scaleMenuExpanded = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(activeScale.displayName, fontSize = 11.sp, color = StudioSecondary)
                            }
                            DropdownMenu(
                                expanded = scaleMenuExpanded,
                                onDismissRequest = { scaleMenuExpanded = false }
                            ) {
                                MusicalScale.values().forEach { scale ->
                                    DropdownMenuItem(
                                        text = { Text(scale.displayName) },
                                        onClick = {
                                            activeScale = scale
                                            scaleMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Clear Notes
                        IconButton(
                            onClick = { viewModel.clearAllMidiNotes() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Clear Notes",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Chord Helpers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("+ Chord:", color = TextMuted, fontSize = 11.sp)
                    listOf("Minor", "Major", "Minor 7", "Major 7").forEach { chord ->
                        FilledTonalButton(
                            onClick = {
                                val currentBeat = (meterState.currentStep / 4f)
                                viewModel.insertChord(startNote + 4, chord, currentBeat)
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text(chord, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Timeline Header: Beats 1 through 16 (4 bars)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioSurface)
                .padding(vertical = 4.dp)
        ) {
            Box(modifier = Modifier.width(60.dp).padding(start = 8.dp)) {
                Text("NOTE", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScrollState)
            ) {
                for (beat in 0 until 16) {
                    val isBarStart = (beat % 4 == 0)
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(20.dp)
                            .padding(horizontal = 1.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isBarStart) StudioSurfaceElevated else StudioSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isBarStart) "Bar ${(beat / 4) + 1}" else ".${(beat % 4) + 1}",
                            fontSize = 10.sp,
                            color = if (isBarStart) StudioPrimary else TextSecondary,
                            fontWeight = if (isBarStart) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Piano Grid (Left = Piano Key, Right = Grid timeline cells)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(noteRange) { midiNote ->
                val noteInOctave = midiNote % 12
                val isBlackKey = noteInOctave in listOf(1, 3, 6, 8, 10)
                val noteName = getMidiNoteName(midiNote)
                val isInScale = activeScale.intervals.contains((noteInOctave - rootNoteOffset + 12) % 12)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .background(if (isBlackKey) StudioSurface else StudioSurfaceVariant.copy(alpha = 0.6f))
                        .border(width = 0.5.dp, color = StudioSurfaceBorder.copy(alpha = 0.4f)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Piano Key (Auditions note on touch!)
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .fillMaxHeight()
                            .background(
                                when {
                                    !isInScale -> StudioSurfaceElevated.copy(alpha = 0.5f)
                                    isBlackKey -> Color(0xFF1E1E28)
                                    else -> Color(0xFF2E2E3E)
                                }
                            )
                            .clickable {
                                viewModel.previewNoteOn(midiNote)
                            }
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isInScale) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(StudioSecondary)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = noteName,
                                fontSize = 10.sp,
                                fontWeight = if (noteName.startsWith("C") && !isBlackKey) FontWeight.Bold else FontWeight.Normal,
                                color = if (noteName.startsWith("C") && !isBlackKey) StudioPrimary else TextPrimary
                            )
                        }
                    }

                    // 16-Beat Timeline Grid Cells
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .horizontalScroll(horizontalScrollState),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (beat in 0 until 16) {
                            val beatFloat = beat.toFloat()
                            val existingNote = midiNotes.find {
                                it.midiNote == midiNote && kotlin.math.abs(it.startBeat - beatFloat) < 0.2f
                            }
                            val hasNote = existingNote != null
                            val isBarStart = (beat % 4 == 0)
                            val isPlayheadOnBeat = isPlaying && ((meterState.currentStep / 4) == beat)

                            Box(
                                modifier = Modifier
                                    .width(44.dp)
                                    .fillMaxHeight()
                                    .border(
                                        width = 0.5.dp,
                                        color = if (isPlayheadOnBeat) StudioGreen.copy(alpha = 0.5f) else StudioSurfaceBorder.copy(alpha = 0.25f)
                                    )
                                    .background(
                                        when {
                                            hasNote -> StudioBlue
                                            isPlayheadOnBeat -> StudioGreen.copy(alpha = 0.15f)
                                            isBarStart -> StudioSurfaceElevated.copy(alpha = 0.35f)
                                            isInScale -> Color.Transparent
                                            else -> Color.Black.copy(alpha = 0.25f)
                                        }
                                    )
                                    .clickable {
                                        viewModel.toggleMidiNote(midiNote, beatFloat, durationBeats = 1.0f)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (hasNote) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(1.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(StudioBlue)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getMidiNoteName(midi: Int): String {
    val names = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val octave = (midi / 12) - 1
    val note = names[midi % 12]
    return "$note$octave"
}
