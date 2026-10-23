package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.DrumType
import com.example.audio.SynthEngine
import com.example.data.db.ProjectDatabase
import com.example.data.model.GenreTemplate
import com.example.data.repository.ProjectRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("BeatForge", appName)
    }

    @Test
    fun `test midi note to frequency conversion`() {
        // A4 = MIDI note 69 = 440.0 Hz
        val a4Freq = SynthEngine.midiNoteToFreq(69)
        assertEquals(440.0f, a4Freq, 0.1f)

        // Middle C (C4 = 60) ~ 261.63 Hz
        val c4Freq = SynthEngine.midiNoteToFreq(60)
        assertEquals(261.63f, c4Freq, 0.5f)
    }

    @Test
    fun `test template project creation and persistence`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = ProjectDatabase.getDatabase(context)
        val repository = ProjectRepository(db.projectDao())

        val trapProject = repository.createTemplateProject(GenreTemplate.TRAP, "Test Trap Beat")
        assertEquals(140f, trapProject.bpm)
        assertEquals("C Minor", trapProject.musicalKey)

        val deserializedDrums = repository.deserializeDrumTracks(trapProject.drumTracksJson)
        assertTrue(deserializedDrums.isNotEmpty())
        assertEquals(DrumType.KICK_PUNCH.name, deserializedDrums[0].sampleType)

        repository.saveProject(trapProject)
        val loaded = repository.getProjectById(trapProject.id)
        assertNotNull(loaded)
        assertEquals("Test Trap Beat", loaded?.title)
    }
}
