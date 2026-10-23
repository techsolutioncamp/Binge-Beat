package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class GenreTemplate(val displayName: String, val defaultBpm: Float, val defaultKey: String) {
    TRAP("Trap", 140f, "C Minor"),
    AFROBEATS("Afrobeats", 104f, "F Minor"),
    AMAPIANO("Amapiano", 112f, "G Minor"),
    HIPHOP("Hip-Hop", 92f, "D Minor"),
    LOFI("Lo-Fi Chill", 84f, "A Minor"),
    EDM("EDM / House", 128f, "E Minor"),
    DRILL("UK / NY Drill", 142f, "B Minor"),
    RNB("R&B Soul", 96f, "F Major")
}

data class StepData(
    val stepIndex: Int,
    val isActive: Boolean = false,
    val velocity: Float = 0.85f,
    val pitchOffset: Float = 0f,
    val probability: Float = 1.0f,
    val ratchet: Int = 1
)

data class DrumTrack(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sampleType: String, // from DrumType.name
    val colorHex: Long = 0xFFA855F7,
    val volume: Float = 0.85f,
    val pan: Float = 0f,
    val pitchSemitones: Float = 0f,
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    val steps: List<StepData> = List(16) { StepData(it) }
)

data class MidiNote(
    val id: String = UUID.randomUUID().toString(),
    val midiNote: Int = 60, // Middle C
    val startBeat: Float = 0f,
    val durationBeats: Float = 1.0f,
    val velocity: Float = 0.8f
)

data class ArrangementTrack(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String, // "DRUMS", "SYNTH", "BASS", "AUDIO"
    val colorHex: Long,
    val volume: Float = 0.85f,
    val pan: Float = 0f,
    val isMuted: Boolean = false,
    val isSolo: Boolean = false
)

data class ArrangementClip(
    val id: String = UUID.randomUUID().toString(),
    val trackId: String,
    val title: String,
    val startBar: Int = 1,
    val lengthBars: Int = 4,
    val colorHex: Long = 0xFFA855F7,
    val isAudioClip: Boolean = false,
    val audioFilePath: String? = null
)

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val genre: String = GenreTemplate.TRAP.displayName,
    val bpm: Float = 140f,
    val musicalKey: String = "C Minor",
    val timeSignature: String = "4/4",
    val totalBars: Int = 16,
    val drumTracksJson: String = "",
    val midiNotesJson: String = "",
    val synthPatchJson: String = "",
    val arrangementClipsJson: String = "",
    val arrangementTracksJson: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
