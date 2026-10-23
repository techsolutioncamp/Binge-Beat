package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GenreTemplate
import com.example.data.model.Project
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudioTab
import com.example.ui.viewmodel.StudioViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProjectsExportScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeProject by viewModel.activeProject.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState(initial = emptyList())
    val exportProgress by viewModel.exportProgress.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Export, 1 = Project Library
    var showNewProjectModal by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = StudioSurfaceVariant,
            contentColor = StudioPrimary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("MASTERING & EXPORT", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("PROJECT LIBRARY", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
        }

        if (selectedTab == 0) {
            // Export & Mastering View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Project Summary Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StudioSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "PROJECT RENDER SETTINGS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = activeProject?.title ?: "BeatForge Project",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${activeProject?.bpm?.toInt()} BPM", color = StudioSecondary, fontSize = 12.sp)
                            Text("•", color = TextMuted)
                            Text(activeProject?.musicalKey ?: "C Minor", color = StudioPrimary, fontSize = 12.sp)
                            Text("•", color = TextMuted)
                            Text(activeProject?.genre ?: "Music", color = StudioTertiary, fontSize = 12.sp)
                        }
                    }
                }

                // Format & Mastering Options
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StudioSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("FORMAT & MASTERING CHAIN", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Output Format", color = TextSecondary, fontSize = 13.sp)
                            Text("Standard WAV (16-bit / 44.1kHz Stereo)", color = StudioPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Master Brickwall Limiter", color = TextSecondary, fontSize = 13.sp)
                            Text("Active (-0.2 dB Ceiling)", color = StudioGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Master Processing", color = TextSecondary, fontSize = 13.sp)
                            Text("3-Band EQ + Reverb + Compressor", color = StudioSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // Render Progress Bar
                if (exportProgress.isExporting) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StudioSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Rendering Audio...", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${(exportProgress.progress * 100).toInt()}%", color = StudioPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { exportProgress.progress },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = StudioPrimary,
                                trackColor = StudioSurfaceElevated
                            )
                        }
                    }
                }

                // Export Completed Card
                exportProgress.exportedFilePath?.let { filePath ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StudioGreen.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StudioGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Export Completed!", fontWeight = FontWeight.Bold, color = StudioGreen, fontSize = 14.sp)
                                Text(File(filePath).name, color = TextSecondary, fontSize = 11.sp, maxLines = 1)
                            }

                            Button(
                                onClick = {
                                    viewModel.audioExporter.shareExportedFile(context, File(filePath))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = StudioGreen)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share WAV", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Action Button: Render & Export
                Button(
                    onClick = {
                        viewModel.exportProject(activeProject?.title ?: "BeatForge_Beat")
                    },
                    enabled = !exportProgress.isExporting,
                    colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (exportProgress.isExporting) "RENDERING OFFLINE AUDIO..." else "RENDER & EXPORT MASTER WAV",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            // Project Library View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header with New Project Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SAVED BEATS (${allProjects.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextMuted
                    )

                    Button(
                        onClick = { showNewProjectModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Project", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allProjects, key = { it.id }) { proj ->
                        val isActive = (activeProject?.id == proj.id)
                        val dateFormatted = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(proj.updatedAt))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isActive) StudioSurfaceElevated else StudioSurface,
                            border = if (isActive) androidx.compose.foundation.BorderStroke(1.5.dp, StudioPrimary) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.loadProject(proj)
                                    viewModel.selectTab(StudioTab.DRUMS)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = proj.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = TextPrimary
                                        )
                                        if (isActive) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = StudioPrimary.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    "ACTIVE",
                                                    color = StudioPrimary,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(proj.genre, color = StudioSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        Text("•", color = TextMuted)
                                        Text("${proj.bpm.toInt()} BPM", color = TextSecondary, fontSize = 11.sp)
                                        Text("•", color = TextMuted)
                                        Text(dateFormatted, color = TextMuted, fontSize = 11.sp)
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = { viewModel.duplicateCurrentProject() },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteProject(proj.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // New Project Dialog
    if (showNewProjectModal) {
        var projectTitle by remember { mutableStateOf("") }
        var selectedGenre by remember { mutableStateOf(GenreTemplate.TRAP) }

        AlertDialog(
            onDismissRequest = { showNewProjectModal = false },
            title = { Text("Create New Project", color = TextPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = projectTitle,
                        onValueChange = { projectTitle = it },
                        label = { Text("Project Name") },
                        placeholder = { Text("e.g. Midnight Beats") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = StudioPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Select Genre Template:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(modifier = Modifier.height(180.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(GenreTemplate.values()) { genre ->
                            val isSel = (genre == selectedGenre)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) StudioPrimary.copy(alpha = 0.25f) else StudioSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedGenre = genre }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        genre.displayName,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) StudioPrimary else TextPrimary
                                    )
                                    Text("${genre.defaultBpm.toInt()} BPM", fontSize = 11.sp, color = TextMuted)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = projectTitle.ifBlank { "${selectedGenre.displayName} Beat" }
                        viewModel.createNewProject(title, selectedGenre)
                        showNewProjectModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewProjectModal = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = StudioSurface
        )
    }
}
