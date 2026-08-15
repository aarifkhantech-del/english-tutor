package com.englishtutor.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.englishtutor.app.ui.TutorViewModel
import com.englishtutor.app.ui.components.*
import com.englishtutor.app.ui.theme.*
import androidx.compose.ui.graphics.Brush
class MainActivity : ComponentActivity() {

    private val viewModel: TutorViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleRecording()
        } else {
            Toast.makeText(this, "Microphone permission is required to record speech", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EnglishTutorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TutorMainScreen(
                        viewModel = viewModel,
                        onRequestPermission = { checkAndRequestPermission() }
                    )
                }
            }
        }
    }

    private fun checkAndRequestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            viewModel.toggleRecording()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorMainScreen(
    viewModel: TutorViewModel,
    onRequestPermission: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(DarkBackground, AccentTeal)
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🗣️ Jio English",
                                style = MaterialTheme.typography.titleLarge,
                                fontSize = 22.sp
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.isServerOnline) AccentTeal else RecordingRed)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.openSettings() }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextSecondary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
                )

                // Header Navigation Bar with Home Tab
                PrimaryTabRow(
                    selectedTabIndex = 0,
                    containerColor = DarkBackground,
                    contentColor = PrimaryLight,
                    indicator = {
                        TabRowDefaults.PrimaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(selectedTabIndex = 0),
                            color = PrimaryLight,
                            height = 3.dp
                        )
                    },
                    divider = {
                        HorizontalDivider(color = SurfaceBorder.copy(alpha = 0.5f))
                    }
                ) {
                    Tab(
                        selected = true,
                        onClick = { /* Currently on Home tab */ },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Home",
                                    tint = PrimaryLight,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("Home", fontSize = 14.sp, color = PrimaryLight)
                            }
                        }
                    )
                }
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Subtitle
            Text(
                text = "Hindi में बोलें, English में सीखें 🎯",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Main Recording Button Component
            RecordButton(
                isRecording = uiState.isRecording,
                onClick = onRequestPermission
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Server Offline Banner Notice
            if (!uiState.isServerOnline && !uiState.isRecording && !uiState.isProcessing) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = RecordingRed.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = RecordingRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Backend offline (${uiState.serverUrl}). Ensure FastAPI server is running.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 12.sp,
                            color = RecordingRed,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.openSettings() }) {
                            Text("Configure", fontSize = 12.sp, color = PrimaryLight)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Error Banner Notice
            uiState.errorMessage?.let { error ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = RecordingRed.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = RecordingRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Step 1: Translating Loader (shows while converting text to English)
            AnimatedVisibility(
                visible = uiState.isProcessing,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    CircularProgressIndicator(color = PrimaryLight)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Translating to English...", color = TextSecondary, fontSize = 14.sp)
                }
            }

            // Step 1 Preview: Display Speech Transcription with Proceed & Cancel Buttons
            AnimatedVisibility(
                visible = uiState.pendingTranscription != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                uiState.pendingTranscription?.let { pendingText ->
                    TranscriptionPreviewCard(
                        initialText = pendingText,
                        isTranslating = uiState.isTranslating,
                        onProceed = { confirmedText -> viewModel.proceedToTranslate(confirmedText) },
                        onCancel = { viewModel.cancelSpeakRequest() }
                    )
                }
            }

            if (uiState.pendingTranscription != null) {
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Step 2 Output: English Translation, Practice & Audio Pronunciation Display
            AnimatedVisibility(
                visible = uiState.result != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                uiState.result?.let { response ->
                    Column {
                        // User Hindi Transcription Card
                        TranscriptionCard(text = response.transcription)

                        Spacer(modifier = Modifier.height(20.dp))

                        // English Correction Card with Audio Playback Button
                        CorrectionCard(
                            correction = response.correction,
                            isPlayingAudio = uiState.isPlayingAudio,
                            onPlayAudio = { viewModel.togglePlayAudio() }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Practice Sentence & Encouragement Card
                        PracticeCard(correction = response.correction)

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }

        // Server Settings Modal Dialog
        if (uiState.showSettingsDialog) {
            ServerConfigDialog(
                currentUrl = uiState.serverUrl,
                onDismiss = { viewModel.closeSettings() },
                onSave = { newUrl -> viewModel.saveServerUrl(newUrl) }
            )
        }
    }
}
