package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.*
import com.example.data.db.ProjectDatabase
import com.example.data.model.*
import com.example.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class StudioTab(val title: String) {
    DRUMS("Drums"),
    PIANO_ROLL("Piano Roll"),
    ARRANGEMENT("Arranger"),
    MIXER("Mixer & FX"),
    SYNTH("Synth"),
    RECORD("Record"),
    PROJECTS("Library")
}

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    val audioEngine = AudioEngine(sampleRate = 44100)
    val audioRecorder = AudioRecorder(application)
    val audioExporter = AudioExporter(application)

    // Navigation Tab
    private val _currentTab = MutableStateFlow(StudioTab.DRUMS)
    val currentTab = _currentTab.asStateFlow()

    fun selectTab(tab: StudioTab) {
        _currentTab.value = tab
    }

    // Active Project & State
    private val _activeProject = MutableStateFlow<Project?>(null)
    val activeProject = _activeProject.asStateFlow()

    private val _drumTracks = MutableStateFlow<List<DrumTrack>>(emptyList())
    val drumTracks = _drumTracks.asStateFlow()

    private val _midiNotes = MutableStateFlow<List<MidiNote>>(emptyList())
    val midiNotes = _midiNotes.asStateFlow()

    private val _synthPatch = MutableStateFlow(SynthPatch())
    val synthPatch = _synthPatch.asStateFlow()

    private val _arrangementTracks = MutableStateFlow<List<ArrangementTrack>>(emptyList())
    val arrangementTracks = _arrangementTracks.asStateFlow()

    private val _arrangementClips = MutableStateFlow<List<ArrangementClip>>(emptyList())
    val arrangementClips = _arrangementClips.asStateFlow()

    // Transport States
    val meterState = audioEngine.meterState
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _bpm = MutableStateFlow(140f)
    val bpm = _bpm.asStateFlow()

    private val _isMetronomeEnabled = MutableStateFlow(false)
    val isMetronomeEnabled = _isMetronomeEnabled.asStateFlow()

    private val _swing = MutableStateFlow(0f)
    val swing = _swing.asStateFlow()

    private val _masterVolume = MutableStateFlow(0.85f)
    val masterVolume = _masterVolume.asStateFlow()

    // Tap Tempo Calculation
    private val tapTimestamps = mutableListOf<Long>()

    private val database = ProjectDatabase.getDatabase(application)
    private val repository = ProjectRepository(database.projectDao())

    // Project Library
    val allProjects: StateFlow<List<Project>> = repository.allProjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Audio Export State
    val exportProgress = audioExporter.exportState

    init {
        // Start Audio Engine safely
        try {
            audioEngine.startEngine()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Hook up step sequencer playback callback
        audioEngine.onStepTrigger = { stepIndex, _ ->
            // Trigger active drum steps
            val tracks = _drumTracks.value
            tracks.forEach { track ->
                if (!track.isMuted) {
                    val step = track.steps.getOrNull(stepIndex)
                    if (step != null && step.isActive) {
                        val drumType = runCatching { DrumType.valueOf(track.sampleType) }
                            .getOrDefault(DrumType.KICK_PUNCH)
                        audioEngine.drumSynth.trigger(
                            type = drumType,
                            velocity = step.velocity * track.volume,
                            pitchSemitones = track.pitchSemitones,
                            pan = track.pan
                        )
                    }
                }
            }

            // Trigger MIDI notes at this step
            val notes = _midiNotes.value
            val currentBeatFloat = stepIndex / 4.0f
            notes.forEach { note ->
                if (kotlin.math.abs(note.startBeat - currentBeatFloat) < 0.15f) {
                    audioEngine.synthEngine.noteOn(note.midiNote, note.velocity, _synthPatch.value)
                }
            }
        }

        // Initialize default project (Trap starter)
        loadDefaultStarter()
    }

    private fun loadDefaultStarter() {
        val defaultProj = repository.createTemplateProject(GenreTemplate.TRAP, "Trap Genesis Beat")
        loadProject(defaultProj)
    }

    fun loadProject(project: Project) {
        _activeProject.value = project
        _bpm.value = project.bpm
        audioEngine.bpm = project.bpm

        val drums = repository.deserializeDrumTracks(project.drumTracksJson)
        _drumTracks.value = if (drums.isNotEmpty()) drums else repository.getTemplateDrumTracks(GenreTemplate.TRAP)

        val notes = repository.deserializeMidiNotes(project.midiNotesJson)
        _midiNotes.value = notes

        val arrTracks = repository.deserializeArrangementTracks(project.arrangementTracksJson)
        _arrangementTracks.value = if (arrTracks.isNotEmpty()) arrTracks else repository.getTemplateArrangementTracks()

        val arrClips = repository.deserializeArrangementClips(project.arrangementClipsJson)
        _arrangementClips.value = arrClips
    }

    fun saveCurrentProject() {
        val current = _activeProject.value ?: return
        viewModelScope.launch {
            val updated = current.copy(
                bpm = _bpm.value,
                drumTracksJson = repository.serializeDrumTracks(_drumTracks.value),
                midiNotesJson = repository.serializeMidiNotes(_midiNotes.value),
                synthPatchJson = repository.serializeSynthPatch(_synthPatch.value),
                arrangementTracksJson = repository.serializeArrangementTracks(_arrangementTracks.value),
                arrangementClipsJson = repository.serializeArrangementClips(_arrangementClips.value),
                updatedAt = System.currentTimeMillis()
            )
            repository.saveProject(updated)
            _activeProject.value = updated
        }
    }

    fun createNewProject(title: String, genre: GenreTemplate) {
        val newProj = repository.createTemplateProject(genre, title)
        viewModelScope.launch {
            repository.saveProject(newProj)
            loadProject(newProj)
            _currentTab.value = StudioTab.DRUMS
        }
    }

    fun duplicateCurrentProject() {
        val current = _activeProject.value ?: return
        val dup = current.copy(
            id = UUID.randomUUID().toString(),
            title = "${current.title} (Copy)",
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.saveProject(dup)
            loadProject(dup)
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            repository.deleteProject(id)
            if (_activeProject.value?.id == id) {
                loadDefaultStarter()
            }
        }
    }

    // --- Transport Controls ---
    fun togglePlay() {
        if (_isPlaying.value) {
            audioEngine.pause()
            _isPlaying.value = false
        } else {
            audioEngine.play()
            _isPlaying.value = true
        }
    }

    fun stop() {
        audioEngine.stop()
        _isPlaying.value = false
    }

    fun setBpm(newBpm: Float) {
        val clamped = newBpm.coerceIn(40f, 260f)
        _bpm.value = clamped
        audioEngine.bpm = clamped
        saveCurrentProject()
    }

    fun tapTempo() {
        val now = System.currentTimeMillis()
        tapTimestamps.add(now)
        if (tapTimestamps.size > 5) {
            tapTimestamps.removeAt(0)
        }
        if (tapTimestamps.size >= 2) {
            val diffs = mutableListOf<Long>()
            for (i in 1 until tapTimestamps.size) {
                val diff = tapTimestamps[i] - tapTimestamps[i - 1]
                if (diff in 200..2000) {
                    diffs.add(diff)
                }
            }
            if (diffs.isNotEmpty()) {
                val avgDiff = diffs.average()
                val calculatedBpm = (60000.0 / avgDiff).toFloat().coerceIn(50f, 220f)
                setBpm(calculatedBpm)
            }
        }
    }

    fun toggleMetronome() {
        val next = !_isMetronomeEnabled.value
        _isMetronomeEnabled.value = next
        audioEngine.isMetronomeEnabled = next
    }

    fun setSwing(amount: Float) {
        val clamped = amount.coerceIn(0f, 0.7f)
        _swing.value = clamped
        audioEngine.swingAmount = clamped
    }

    fun setMasterVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1.2f)
        _masterVolume.value = clamped
        audioEngine.masterVolume = clamped
    }

    // --- Drum Sequencer Actions ---
    fun toggleStep(trackId: String, stepIndex: Int) {
        _drumTracks.value = _drumTracks.value.map { track ->
            if (track.id == trackId) {
                val updatedSteps = track.steps.mapIndexed { idx, step ->
                    if (idx == stepIndex) step.copy(isActive = !step.isActive) else step
                }
                track.copy(steps = updatedSteps)
            } else track
        }
        saveCurrentProject()
    }

    fun setStepVelocity(trackId: String, stepIndex: Int, velocity: Float) {
        _drumTracks.value = _drumTracks.value.map { track ->
            if (track.id == trackId) {
                val updatedSteps = track.steps.mapIndexed { idx, step ->
                    if (idx == stepIndex) step.copy(velocity = velocity.coerceIn(0.1f, 1.0f)) else step
                }
                track.copy(steps = updatedSteps)
            } else track
        }
    }

    fun setTrackVolume(trackId: String, volume: Float) {
        _drumTracks.value = _drumTracks.value.map {
            if (it.id == trackId) it.copy(volume = volume.coerceIn(0f, 1f)) else it
        }
    }

    fun setTrackPan(trackId: String, pan: Float) {
        _drumTracks.value = _drumTracks.value.map {
            if (it.id == trackId) it.copy(pan = pan.coerceIn(-1f, 1f)) else it
        }
    }

    fun setTrackPitch(trackId: String, pitchSemitones: Float) {
        _drumTracks.value = _drumTracks.value.map {
            if (it.id == trackId) it.copy(pitchSemitones = pitchSemitones.coerceIn(-24f, 24f)) else it
        }
    }

    fun toggleTrackMute(trackId: String) {
        _drumTracks.value = _drumTracks.value.map {
            if (it.id == trackId) it.copy(isMuted = !it.isMuted) else it
        }
    }

    fun toggleTrackSolo(trackId: String) {
        _drumTracks.value = _drumTracks.value.map {
            if (it.id == trackId) it.copy(isSolo = !it.isSolo) else it
        }
    }

    fun changeDrumSound(trackId: String, newType: DrumType) {
        _drumTracks.value = _drumTracks.value.map {
            if (it.id == trackId) it.copy(sampleType = newType.name, name = newType.displayName) else it
        }
        previewDrumSound(newType)
        saveCurrentProject()
    }

    fun clearPattern() {
        _drumTracks.value = _drumTracks.value.map { track ->
            track.copy(steps = List(16) { StepData(it) })
        }
        saveCurrentProject()
    }

    fun randomizePattern() {
        _drumTracks.value = _drumTracks.value.map { track ->
            val randomSteps = track.steps.mapIndexed { idx, step ->
                val prob = when {
                    track.name.contains("Kick", true) -> if (idx % 4 == 0) 0.8f else 0.2f
                    track.name.contains("Snare", true) || track.name.contains("Clap", true) -> if (idx % 8 == 4) 0.9f else 0.1f
                    track.name.contains("Hat", true) -> 0.6f
                    else -> 0.25f
                }
                step.copy(isActive = Math.random() < prob, velocity = (0.5f + Math.random().toFloat() * 0.5f))
            }
            track.copy(steps = randomSteps)
        }
        saveCurrentProject()
    }

    fun previewDrumSound(drumType: DrumType) {
        audioEngine.drumSynth.trigger(drumType, velocity = 0.9f)
    }

    // --- Piano Roll & Synthesizer Actions ---
    fun toggleMidiNote(midiNote: Int, startBeat: Float, durationBeats: Float = 1.0f) {
        val existing = _midiNotes.value.find { it.midiNote == midiNote && kotlin.math.abs(it.startBeat - startBeat) < 0.2f }
        if (existing != null) {
            _midiNotes.value = _midiNotes.value.filter { it.id != existing.id }
        } else {
            _midiNotes.value = _midiNotes.value + MidiNote(
                midiNote = midiNote,
                startBeat = startBeat,
                durationBeats = durationBeats
            )
            previewNoteOn(midiNote)
        }
        saveCurrentProject()
    }

    fun previewNoteOn(midiNote: Int) {
        audioEngine.synthEngine.noteOn(midiNote, 0.85f, _synthPatch.value)
    }

    fun previewNoteOff(midiNote: Int) {
        audioEngine.synthEngine.noteOff(midiNote)
    }

    fun clearAllMidiNotes() {
        _midiNotes.value = emptyList()
        saveCurrentProject()
    }

    fun insertChord(rootMidi: Int, chordType: String, startBeat: Float) {
        val intervals = when (chordType) {
            "Minor" -> listOf(0, 3, 7)
            "Major" -> listOf(0, 4, 7)
            "Minor 7" -> listOf(0, 3, 7, 10)
            "Major 7" -> listOf(0, 4, 7, 11)
            "Sus4" -> listOf(0, 5, 7)
            else -> listOf(0, 4, 7)
        }
        val newNotes = intervals.map { interval ->
            MidiNote(
                midiNote = rootMidi + interval,
                startBeat = startBeat,
                durationBeats = 1.0f
            )
        }
        _midiNotes.value = _midiNotes.value + newNotes
        previewNoteOn(rootMidi)
        saveCurrentProject()
    }

    fun updateSynthPatch(newPatch: SynthPatch) {
        _synthPatch.value = newPatch
        audioEngine.synthEngine.currentPatch = newPatch
    }

    fun selectSynthPreset(preset: SynthPatch) {
        updateSynthPatch(preset)
    }

    // --- Arrangement Actions ---
    fun addArrangementClip(trackId: String, title: String, startBar: Int, lengthBars: Int = 4, colorHex: Long = 0xFFA855F7) {
        val clip = ArrangementClip(
            trackId = trackId,
            title = title,
            startBar = startBar,
            lengthBars = lengthBars,
            colorHex = colorHex
        )
        _arrangementClips.value = _arrangementClips.value + clip
        saveCurrentProject()
    }

    fun deleteArrangementClip(clipId: String) {
        _arrangementClips.value = _arrangementClips.value.filter { it.id != clipId }
        saveCurrentProject()
    }

    // --- Audio Recording Actions ---
    fun startVocalRecording(): Boolean {
        return audioRecorder.startRecording("Vocal_Take_${System.currentTimeMillis()}")
    }

    fun stopVocalRecording(): String? {
        val savedPath = audioRecorder.stopRecording()
        if (savedPath != null) {
            // Add clip to arrangement
            val vocalTrack = _arrangementTracks.value.find { it.type == "AUDIO" } ?: _arrangementTracks.value.firstOrNull()
            if (vocalTrack != null) {
                addArrangementClip(
                    trackId = vocalTrack.id,
                    title = File(savedPath).nameWithoutExtension,
                    startBar = 1,
                    lengthBars = 4,
                    colorHex = 0xFF10B981
                )
            }
        }
        return savedPath
    }

    // --- Audio Export Action ---
    fun exportProject(title: String) {
        viewModelScope.launch {
            audioExporter.exportProjectToWav(
                projectTitle = title,
                bpm = _bpm.value,
                totalBars = 8,
                drumTracks = _drumTracks.value,
                synthNotes = _midiNotes.value,
                synthPatch = _synthPatch.value,
                eqParams = audioEngine.eqParams,
                delayParams = audioEngine.delayParams,
                reverbParams = audioEngine.reverbParams,
                compParams = audioEngine.compParams,
                distParams = audioEngine.distParams
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stopEngine()
    }
}
