package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.TopStudioBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudioTab
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun StudioMainScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsState()

    // BackHandler: Return to DRUMS tab if on any sub-tab
    BackHandler(enabled = currentTab != StudioTab.DRUMS) {
        viewModel.selectTab(StudioTab.DRUMS)
    }

    Scaffold(
        modifier = modifier.fillMaxSize().background(StudioBackground),
        topBar = {
            TopStudioBar(viewModel = viewModel)
        },
        bottomBar = {
            NavigationBar(
                containerColor = StudioSurface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .height(64.dp)
            ) {
                NavigationBarItem(
                    selected = currentTab == StudioTab.DRUMS,
                    onClick = { viewModel.selectTab(StudioTab.DRUMS) },
                    icon = { Icon(Icons.Default.Apps, contentDescription = "Drums", modifier = Modifier.size(20.dp)) },
                    label = { Text("Drums", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = StudioPrimary,
                        indicatorColor = StudioPrimary,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )

                NavigationBarItem(
                    selected = currentTab == StudioTab.PIANO_ROLL,
                    onClick = { viewModel.selectTab(StudioTab.PIANO_ROLL) },
                    icon = { Icon(Icons.Default.Piano, contentDescription = "Piano Roll", modifier = Modifier.size(20.dp)) },
                    label = { Text("Piano", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = StudioSecondary,
                        indicatorColor = StudioSecondary,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )

                NavigationBarItem(
                    selected = currentTab == StudioTab.ARRANGEMENT,
                    onClick = { viewModel.selectTab(StudioTab.ARRANGEMENT) },
                    icon = { Icon(Icons.Default.ViewTimeline, contentDescription = "Arranger", modifier = Modifier.size(20.dp)) },
                    label = { Text("Arranger", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = StudioPrimary,
                        indicatorColor = StudioPrimary,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )

                NavigationBarItem(
                    selected = currentTab == StudioTab.MIXER,
                    onClick = { viewModel.selectTab(StudioTab.MIXER) },
                    icon = { Icon(Icons.Default.Equalizer, contentDescription = "Mixer", modifier = Modifier.size(20.dp)) },
                    label = { Text("Mixer", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = StudioYellow,
                        indicatorColor = StudioYellow,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )

                NavigationBarItem(
                    selected = currentTab == StudioTab.SYNTH,
                    onClick = { viewModel.selectTab(StudioTab.SYNTH) },
                    icon = { Icon(Icons.Default.GraphicEq, contentDescription = "Synth", modifier = Modifier.size(20.dp)) },
                    label = { Text("Synth", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = StudioTertiary,
                        indicatorColor = StudioTertiary,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )

                NavigationBarItem(
                    selected = currentTab == StudioTab.RECORD,
                    onClick = { viewModel.selectTab(StudioTab.RECORD) },
                    icon = { Icon(Icons.Default.Mic, contentDescription = "Record", modifier = Modifier.size(20.dp)) },
                    label = { Text("Record", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = StudioRed,
                        indicatorColor = StudioRed,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )

                NavigationBarItem(
                    selected = currentTab == StudioTab.PROJECTS,
                    onClick = { viewModel.selectTab(StudioTab.PROJECTS) },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Library", modifier = Modifier.size(20.dp)) },
                    label = { Text("Export", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = StudioGreen,
                        indicatorColor = StudioGreen,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                StudioTab.DRUMS -> DrumSequencerScreen(viewModel = viewModel)
                StudioTab.PIANO_ROLL -> PianoRollScreen(viewModel = viewModel)
                StudioTab.ARRANGEMENT -> ArrangementScreen(viewModel = viewModel)
                StudioTab.MIXER -> MixerScreen(viewModel = viewModel)
                StudioTab.SYNTH -> SynthScreen(viewModel = viewModel)
                StudioTab.RECORD -> RecordingScreen(viewModel = viewModel)
                StudioTab.PROJECTS -> ProjectsExportScreen(viewModel = viewModel)
            }
        }
    }
}
