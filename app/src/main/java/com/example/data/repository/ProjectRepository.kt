package com.example.data.repository

import com.example.audio.DrumType
import com.example.audio.SynthEngine
import com.example.audio.SynthPatch
import com.example.data.db.ProjectDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ProjectRepository(private val projectDao: ProjectDao) {

    val allProjects: Flow<List<Project>> = projectDao.getAllProjects()

    suspend fun saveProject(project: Project) {
        projectDao.insertProject(project.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteProject(id: String) {
        projectDao.deleteProjectById(id)
    }

    suspend fun getProjectById(id: String): Project? {
        return projectDao.getProjectById(id)
    }

    fun createTemplateProject(genre: GenreTemplate, customTitle: String? = null): Project {
        val title = customTitle ?: "${genre.displayName} Beat"
        val drumTracks = getTemplateDrumTracks(genre)
        val midiNotes = getTemplateMidiNotes(genre)
        val synthPatch = SynthEngine.BUILT_IN_PRESETS.first()
        val arrTracks = getTemplateArrangementTracks()
        val arrClips = listOf(
            ArrangementClip(trackId = arrTracks[0].id, title = "Drums Main", startBar = 1, lengthBars = 8, colorHex = 0xFFEF4444),
            ArrangementClip(trackId = arrTracks[1].id, title = "Synth Hook", startBar = 1, lengthBars = 8, colorHex = 0xFF3B82F6),
            ArrangementClip(trackId = arrTracks[2].id, title = "808 Bassline", startBar = 1, lengthBars = 8, colorHex = 0xFFA855F7)
        )

        return Project(
            id = UUID.randomUUID().toString(),
            title = title,
            genre = genre.displayName,
            bpm = genre.defaultBpm,
            musicalKey = genre.defaultKey,
            timeSignature = "4/4",
            totalBars = 16,
            drumTracksJson = serializeDrumTracks(drumTracks),
            midiNotesJson = serializeMidiNotes(midiNotes),
            synthPatchJson = serializeSynthPatch(synthPatch),
            arrangementTracksJson = serializeArrangementTracks(arrTracks),
            arrangementClipsJson = serializeArrangementClips(arrClips),
            updatedAt = System.currentTimeMillis()
        )
    }

    fun getTemplateArrangementTracks(): List<ArrangementTrack> {
        return listOf(
            ArrangementTrack(name = "Drums", type = "DRUMS", colorHex = 0xFFEF4444),
            ArrangementTrack(name = "Synth Melody", type = "SYNTH", colorHex = 0xFF3B82F6),
            ArrangementTrack(name = "Bassline", type = "BASS", colorHex = 0xFFA855F7),
            ArrangementTrack(name = "Vocals / Audio", type = "AUDIO", colorHex = 0xFF10B981)
        )
    }

    fun getTemplateDrumTracks(genre: GenreTemplate): List<DrumTrack> {
        val kick = DrumTrack(name = "Kick", sampleType = DrumType.KICK_PUNCH.name, colorHex = 0xFFEF4444)
        val sub808 = DrumTrack(name = "808 Sub", sampleType = DrumType.KICK_808.name, colorHex = 0xFF8B5CF6)
        val snare = DrumTrack(name = "Snare", sampleType = DrumType.SNARE_CRISP.name, colorHex = 0xFF06B6D4)
        val clap = DrumTrack(name = "Clap", sampleType = DrumType.CLAP_LAYERED.name, colorHex = 0xFF10B981)
        val hatClosed = DrumTrack(name = "Closed Hat", sampleType = DrumType.HIHAT_CLOSED.name, colorHex = 0xFFFBBF24)
        val hatOpen = DrumTrack(name = "Open Hat", sampleType = DrumType.HIHAT_OPEN.name, colorHex = 0xFFF59E0B)
        val perc1 = DrumTrack(name = "Perc 1", sampleType = DrumType.PERC_RIM.name, colorHex = 0xFFEC4899)
        val perc2 = DrumTrack(name = "Perc 2", sampleType = DrumType.PERC_CONGA.name, colorHex = 0xFFF97316)

        val kickSteps = MutableList(16) { StepData(it) }
        val subSteps = MutableList(16) { StepData(it) }
        val snareSteps = MutableList(16) { StepData(it) }
        val clapSteps = MutableList(16) { StepData(it) }
        val hatSteps = MutableList(16) { StepData(it) }
        val openHatSteps = MutableList(16) { StepData(it) }
        val percSteps = MutableList(16) { StepData(it) }

        when (genre) {
            GenreTemplate.TRAP, GenreTemplate.DRILL -> {
                listOf(0, 10).forEach { kickSteps[it] = kickSteps[it].copy(isActive = true, velocity = 0.95f) }
                listOf(0, 6, 10).forEach { subSteps[it] = subSteps[it].copy(isActive = true, velocity = 0.9f) }
                listOf(4, 12).forEach { clapSteps[it] = clapSteps[it].copy(isActive = true, velocity = 0.9f) }
                // Rolling hi-hats
                listOf(0, 2, 4, 6, 8, 10, 12, 14, 15).forEach { hatSteps[it] = hatSteps[it].copy(isActive = true, velocity = 0.8f) }
                openHatSteps[2] = openHatSteps[2].copy(isActive = true, velocity = 0.7f)
                percSteps[7] = percSteps[7].copy(isActive = true, velocity = 0.65f)
            }
            GenreTemplate.AFROBEATS, GenreTemplate.AMAPIANO -> {
                // Syncopated Afro rhythm
                listOf(0, 6, 10).forEach { kickSteps[it] = kickSteps[it].copy(isActive = true, velocity = 0.9f) }
                listOf(4, 12).forEach { snareSteps[it] = snareSteps[it].copy(isActive = true, velocity = 0.85f) }
                listOf(0, 2, 4, 6, 8, 10, 12, 14).forEach { hatSteps[it] = hatSteps[it].copy(isActive = true, velocity = 0.75f) }
                listOf(3, 7, 11, 14).forEach { percSteps[it] = percSteps[it].copy(isActive = true, velocity = 0.8f) }
                listOf(2, 8, 14).forEach { openHatSteps[it] = openHatSteps[it].copy(isActive = true, velocity = 0.7f) }
            }
            GenreTemplate.EDM -> {
                // Four-on-the-floor
                listOf(0, 4, 8, 12).forEach { kickSteps[it] = kickSteps[it].copy(isActive = true, velocity = 1.0f) }
                listOf(4, 12).forEach { clapSteps[it] = clapSteps[it].copy(isActive = true, velocity = 0.9f) }
                listOf(2, 6, 10, 14).forEach { openHatSteps[it] = openHatSteps[it].copy(isActive = true, velocity = 0.85f) }
                listOf(0, 2, 4, 6, 8, 10, 12, 14).forEach { hatSteps[it] = hatSteps[it].copy(isActive = true, velocity = 0.6f) }
            }
            else -> { // LOFI, HIPHOP, RNB
                listOf(0, 8, 10).forEach { kickSteps[it] = kickSteps[it].copy(isActive = true, velocity = 0.85f) }
                listOf(4, 12).forEach { snareSteps[it] = snareSteps[it].copy(isActive = true, velocity = 0.85f) }
                listOf(0, 2, 4, 6, 8, 10, 12, 14).forEach { hatSteps[it] = hatSteps[it].copy(isActive = true, velocity = 0.7f) }
                openHatSteps[14] = openHatSteps[14].copy(isActive = true, velocity = 0.6f)
            }
        }

        return listOf(
            kick.copy(steps = kickSteps),
            sub808.copy(steps = subSteps),
            snare.copy(steps = snareSteps),
            clap.copy(steps = clapSteps),
            hatClosed.copy(steps = hatSteps),
            hatOpen.copy(steps = openHatSteps),
            perc1.copy(steps = percSteps),
            perc2
        )
    }

    fun getTemplateMidiNotes(genre: GenreTemplate): List<MidiNote> {
        return when (genre) {
            GenreTemplate.TRAP, GenreTemplate.DRILL -> listOf(
                MidiNote(midiNote = 60, startBeat = 0f, durationBeats = 1f),
                MidiNote(midiNote = 63, startBeat = 1f, durationBeats = 1f),
                MidiNote(midiNote = 67, startBeat = 2f, durationBeats = 1.5f),
                MidiNote(midiNote = 65, startBeat = 3.5f, durationBeats = 0.5f)
            )
            GenreTemplate.AFROBEATS, GenreTemplate.AMAPIANO -> listOf(
                MidiNote(midiNote = 65, startBeat = 0f, durationBeats = 0.75f),
                MidiNote(midiNote = 68, startBeat = 1f, durationBeats = 0.75f),
                MidiNote(midiNote = 70, startBeat = 2f, durationBeats = 1f),
                MidiNote(midiNote = 68, startBeat = 3f, durationBeats = 1f)
            )
            else -> listOf(
                MidiNote(midiNote = 60, startBeat = 0f, durationBeats = 2f),
                MidiNote(midiNote = 64, startBeat = 0f, durationBeats = 2f),
                MidiNote(midiNote = 67, startBeat = 0f, durationBeats = 2f),
                MidiNote(midiNote = 58, startBeat = 2f, durationBeats = 2f),
                MidiNote(midiNote = 62, startBeat = 2f, durationBeats = 2f),
                MidiNote(midiNote = 65, startBeat = 2f, durationBeats = 2f)
            )
        }
    }

    // --- JSON Serialization Helpers ---
    fun serializeDrumTracks(tracks: List<DrumTrack>): String {
        val array = JSONArray()
        tracks.forEach { t ->
            val obj = JSONObject().apply {
                put("id", t.id)
                put("name", t.name)
                put("sampleType", t.sampleType)
                put("colorHex", t.colorHex)
                put("volume", t.volume.toDouble())
                put("pan", t.pan.toDouble())
                put("pitchSemitones", t.pitchSemitones.toDouble())
                put("isMuted", t.isMuted)
                put("isSolo", t.isSolo)

                val stepsArray = JSONArray()
                t.steps.forEach { s ->
                    stepsArray.put(JSONObject().apply {
                        put("stepIndex", s.stepIndex)
                        put("isActive", s.isActive)
                        put("velocity", s.velocity.toDouble())
                        put("pitchOffset", s.pitchOffset.toDouble())
                    })
                }
                put("steps", stepsArray)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeDrumTracks(json: String): List<DrumTrack> {
        if (json.isBlank()) return emptyList()
        val list = mutableListOf<DrumTrack>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val stepsList = mutableListOf<StepData>()
                val stepsArr = obj.optJSONArray("steps")
                if (stepsArr != null) {
                    for (s in 0 until stepsArr.length()) {
                        val sObj = stepsArr.getJSONObject(s)
                        stepsList.add(
                            StepData(
                                stepIndex = sObj.optInt("stepIndex", s),
                                isActive = sObj.optBoolean("isActive", false),
                                velocity = sObj.optDouble("velocity", 0.85).toFloat(),
                                pitchOffset = sObj.optDouble("pitchOffset", 0.0).toFloat()
                            )
                        )
                    }
                }
                list.add(
                    DrumTrack(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", "Track"),
                        sampleType = obj.optString("sampleType", DrumType.KICK_PUNCH.name),
                        colorHex = obj.optLong("colorHex", 0xFFA855F7),
                        volume = obj.optDouble("volume", 0.85).toFloat(),
                        pan = obj.optDouble("pan", 0.0).toFloat(),
                        pitchSemitones = obj.optDouble("pitchSemitones", 0.0).toFloat(),
                        isMuted = obj.optBoolean("isMuted", false),
                        isSolo = obj.optBoolean("isSolo", false),
                        steps = if (stepsList.isNotEmpty()) stepsList else List(16) { StepData(it) }
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun serializeMidiNotes(notes: List<MidiNote>): String {
        val array = JSONArray()
        notes.forEach { n ->
            array.put(JSONObject().apply {
                put("id", n.id)
                put("midiNote", n.midiNote)
                put("startBeat", n.startBeat.toDouble())
                put("durationBeats", n.durationBeats.toDouble())
                put("velocity", n.velocity.toDouble())
            })
        }
        return array.toString()
    }

    fun deserializeMidiNotes(json: String): List<MidiNote> {
        if (json.isBlank()) return emptyList()
        val list = mutableListOf<MidiNote>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    MidiNote(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        midiNote = obj.optInt("midiNote", 60),
                        startBeat = obj.optDouble("startBeat", 0.0).toFloat(),
                        durationBeats = obj.optDouble("durationBeats", 1.0).toFloat(),
                        velocity = obj.optDouble("velocity", 0.8).toFloat()
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun serializeSynthPatch(patch: SynthPatch): String {
        return JSONObject().apply {
            put("name", patch.name)
            put("attackSec", patch.attackSec.toDouble())
            put("decaySec", patch.decaySec.toDouble())
            put("sustainLevel", patch.sustainLevel.toDouble())
            put("releaseSec", patch.releaseSec.toDouble())
            put("filterCutoffHz", patch.filterCutoffHz.toDouble())
            put("filterResonance", patch.filterResonance.toDouble())
            put("osc1Wave", patch.osc1Wave.name)
            put("osc2Wave", patch.osc2Wave.name)
            put("osc2DetuneSemitones", patch.osc2DetuneSemitones.toDouble())
            put("oscMix", patch.oscMix.toDouble())
        }.toString()
    }

    fun serializeArrangementTracks(tracks: List<ArrangementTrack>): String {
        val array = JSONArray()
        tracks.forEach { t ->
            array.put(JSONObject().apply {
                put("id", t.id)
                put("name", t.name)
                put("type", t.type)
                put("colorHex", t.colorHex)
                put("volume", t.volume.toDouble())
                put("pan", t.pan.toDouble())
                put("isMuted", t.isMuted)
                put("isSolo", t.isSolo)
            })
        }
        return array.toString()
    }

    fun deserializeArrangementTracks(json: String): List<ArrangementTrack> {
        if (json.isBlank()) return emptyList()
        val list = mutableListOf<ArrangementTrack>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ArrangementTrack(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", "Track"),
                        type = obj.optString("type", "DRUMS"),
                        colorHex = obj.optLong("colorHex", 0xFFEF4444),
                        volume = obj.optDouble("volume", 0.85).toFloat(),
                        pan = obj.optDouble("pan", 0.0).toFloat(),
                        isMuted = obj.optBoolean("isMuted", false),
                        isSolo = obj.optBoolean("isSolo", false)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun serializeArrangementClips(clips: List<ArrangementClip>): String {
        val array = JSONArray()
        clips.forEach { c ->
            array.put(JSONObject().apply {
                put("id", c.id)
                put("trackId", c.trackId)
                put("title", c.title)
                put("startBar", c.startBar)
                put("lengthBars", c.lengthBars)
                put("colorHex", c.colorHex)
                put("isAudioClip", c.isAudioClip)
                put("audioFilePath", c.audioFilePath ?: "")
            })
        }
        return array.toString()
    }

    fun deserializeArrangementClips(json: String): List<ArrangementClip> {
        if (json.isBlank()) return emptyList()
        val list = mutableListOf<ArrangementClip>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ArrangementClip(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        trackId = obj.optString("trackId", ""),
                        title = obj.optString("title", "Clip"),
                        startBar = obj.optInt("startBar", 1),
                        lengthBars = obj.optInt("lengthBars", 4),
                        colorHex = obj.optLong("colorHex", 0xFFA855F7),
                        isAudioClip = obj.optBoolean("isAudioClip", false),
                        audioFilePath = obj.optString("audioFilePath").takeIf { it.isNotBlank() }
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
