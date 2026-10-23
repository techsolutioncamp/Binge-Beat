package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.SingleMeterBar
import com.example.ui.components.WaveformVisualizer
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun RecordingScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recordState by viewModel.audioRecorder.recordingState.collectAsState()
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
    }

    // Pulse animation when recording
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Studio Status Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = StudioSurface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (recordState.isRecording) StudioRed else StudioGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (recordState.isRecording) "RECORDING IN PROGRESS" else "RECORDING STUDIO READY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (recordState.isRecording) StudioRed else TextPrimary
                    )
                }

                Text(
                    text = "44.1kHz / 16-bit PCM",
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Large Duration Display
        val durationSec = recordState.durationMs / 1000
        val durationMs = (recordState.durationMs % 1000) / 10
        val minStr = (durationSec / 60).toString().padStart(2, '0')
        val secStr = (durationSec % 60).toString().padStart(2, '0')
        val msStr = durationMs.toString().padStart(2, '0')

        Text(
            text = "$minStr : $secStr . $msStr",
            fontFamily = FontFamily.Monospace,
            fontSize = 38.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (recordState.isRecording) StudioRed else TextPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Waveform Visualizer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            WaveformVisualizer(
                peaks = recordState.waveformPoints,
                modifier = Modifier.fillMaxSize(),
                waveColor = if (recordState.isRecording) StudioRed else StudioSecondary,
                backgroundColor = StudioSurface
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live dBFS Input Meter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(StudioSurface)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Input meter",
                tint = StudioSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("INPUT LEVEL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Text(
                        "${recordState.inputLevelDb.toInt()} dBFS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (recordState.inputLevelDb > -3f) StudioRed else TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                // Horizontal level bar
                val normLevel = ((recordState.inputLevelDb + 60f) / 60f).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { normLevel },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = when {
                        normLevel > 0.9f -> StudioRed
                        normLevel > 0.7f -> StudioYellow
                        else -> StudioGreen
                    },
                    trackColor = StudioSurfaceElevated
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Main Record Action Button
        if (!hasMicPermission) {
            Button(
                onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) },
                colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(imageVector = Icons.Default.Mic, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Grant Microphone Permission", fontWeight = FontWeight.Bold)
            }
        } else {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        if (recordState.isRecording) StudioRed.copy(alpha = pulseAlpha) else StudioSurfaceElevated
                    )
                    .border(
                        width = 3.dp,
                        color = if (recordState.isRecording) StudioRed else StudioPrimary,
                        shape = CircleShape
                    )
                    .clickable {
                        if (recordState.isRecording) {
                            viewModel.stopVocalRecording()
                        } else {
                            viewModel.startVocalRecording()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (recordState.isRecording) {
                    // Square Stop icon
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White)
                    )
                } else {
                    // Circle Record icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(StudioRed)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (recordState.isRecording) "TAP TO FINISH TAKE" else "TAP TO RECORD TAKE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (recordState.isRecording) StudioRed else TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Information banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = StudioSurfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = null,
                    tint = StudioSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tip: Use wired or low-latency headphones to prevent microphone audio feedback when recording over beats.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
