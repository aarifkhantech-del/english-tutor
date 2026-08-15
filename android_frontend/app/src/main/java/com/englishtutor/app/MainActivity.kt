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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.englishtutor.app.ui.AuthViewModel
import com.englishtutor.app.ui.GrammarViewModel
import com.englishtutor.app.ui.SubscriptionViewModel
import com.englishtutor.app.ui.TutorViewModel
import com.englishtutor.app.ui.components.*
import com.englishtutor.app.ui.screens.GrammarScreen
import com.englishtutor.app.ui.screens.LoginScreen
import com.englishtutor.app.ui.screens.SubscriptionScreen
import com.englishtutor.app.ui.theme.*
import kotlinx.coroutines.launch

// ── Navigation Destinations ─────────────────────────────────────────────────
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home         : Screen("home",         "Hindi to English", Icons.Default.Translate)
    object Grammar      : Screen("grammar",      "Grammar",          Icons.Default.School)
    object Subscription : Screen("subscription", "Plans & ₹5 Trial", Icons.Default.Stars)
    object Login        : Screen("login",        "My Account",       Icons.Default.AccountCircle)
}

private val screens = listOf(Screen.Home, Screen.Grammar, Screen.Subscription, Screen.Login)

class MainActivity : ComponentActivity() {

    private val tutorViewModel: TutorViewModel by viewModels()
    private val grammarViewModel: GrammarViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private val subscriptionViewModel: SubscriptionViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) tutorViewModel.toggleRecording()
        else Toast.makeText(this, "Microphone permission required", Toast.LENGTH_LONG).show()
    }

    private val grammarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) grammarViewModel.startVoiceRecognition()
        else Toast.makeText(this, "Microphone permission required", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EnglishTutorTheme {
                AppShell(
                    tutorViewModel = tutorViewModel,
                    grammarViewModel = grammarViewModel,
                    authViewModel = authViewModel,
                    subscriptionViewModel = subscriptionViewModel,
                    onRequestTutorMic = { checkAndRequestPermission() },
                    onRequestGrammarMic = { checkGrammarPermission() }
                )
            }
        }
    }

    private fun checkAndRequestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
            tutorViewModel.toggleRecording()
        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun checkGrammarPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
            grammarViewModel.startVoiceRecognition()
        else grammarPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
}

// ── App Shell with Drawer ────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    tutorViewModel: TutorViewModel,
    grammarViewModel: GrammarViewModel,
    authViewModel: AuthViewModel,
    subscriptionViewModel: SubscriptionViewModel,
    onRequestTutorMic: () -> Unit,
    onRequestGrammarMic: () -> Unit
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val tutorState by tutorViewModel.uiState.collectAsState()
    val grammarState by grammarViewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val subscriptionState by subscriptionViewModel.uiState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    // Load user profile & subscription on start
    LaunchedEffect(tutorState.serverUrl, authState.isLoggedIn) {
        if (authState.isLoggedIn) {
            authViewModel.loadProfile(tutorState.serverUrl)
        }
        subscriptionViewModel.loadPlansAndStatus(tutorState.serverUrl)
    }

    val bgBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF0A0E1A), Color(0xFF0D1F3C), Color(0xFF0A2744))
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                currentRoute = currentRoute,
                authState = authState,
                subState = subscriptionState,
                navController = navController,
                onClose = { scope.launch { drawerState.close() } }
            )
        },
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(bgBrush)) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hamburger
                    IconButton(
                        onClick = { scope.launch { drawerState.open() } },
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(Icons.Default.Menu, "Menu", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))

                    // App brand
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(9.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF2979FF), AccentTeal))),
                            contentAlignment = Alignment.Center
                        ) { Text("VB", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = when (currentRoute) {
                                    Screen.Grammar.route      -> "Grammar Explorer"
                                    Screen.Subscription.route -> "Plans & Pricing"
                                    Screen.Login.route        -> "My Account"
                                    Screen.Home.route         -> "Hindi to English"
                                    else -> "VocalBharat"
                                },
                                color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(6.dp).clip(CircleShape)
                                        .background(if (tutorState.isServerOnline) Color(0xFF00E676) else RecordingRed)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (tutorState.isServerOnline) "Online" else "Offline",
                                    color = if (tutorState.isServerOnline) Color(0xFF00E676) else RecordingRed,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Plan Badge / Upgrade CTA
                    if (!subscriptionState.status.isActive) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(Color(0xFF00E676).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFF00E676).copy(alpha = 0.4f), RoundedCornerShape(100.dp))
                                .clickable { navController.navigate(Screen.Subscription.route) }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text("₹5 Trial", color = Color(0xFF00E676), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(8.dp))
                    }

                    // Settings
                    IconButton(
                        onClick = { tutorViewModel.openSettings() },
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(Icons.Default.Settings, "Settings", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

                // Nav content
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(Screen.Home.route) {
                        HomeScreenContent(
                            viewModel = tutorViewModel,
                            subState = subscriptionState,
                            onNavigateToSubscription = { navController.navigate(Screen.Subscription.route) },
                            onRequestPermission = onRequestTutorMic
                        )
                    }
                    composable(Screen.Grammar.route) {
                        GrammarScreen(
                            uiState = grammarState,
                            serverUrl = tutorState.serverUrl,
                            onTopicChange = { grammarViewModel.setTopic(it) },
                            onExplain = { grammarViewModel.explainTopic(tutorState.serverUrl) },
                            onStartVoice = onRequestGrammarMic,
                            onStopVoice = { grammarViewModel.stopVoiceRecognition() },
                            onClear = { grammarViewModel.clearResult() }
                        )
                    }
                    composable(Screen.Subscription.route) {
                        SubscriptionScreen(
                            uiState = subscriptionState,
                            isLoggedIn = authState.isLoggedIn,
                            userEmail = authState.userEmail,
                            serverUrl = tutorState.serverUrl,
                            onSelectPlan = { subscriptionViewModel.selectPlan(it) },
                            onSubscribe = { planId ->
                                subscriptionViewModel.subscribeToPlan(
                                    serverUrl = tutorState.serverUrl,
                                    planId = planId,
                                    onRequireLogin = {
                                        Toast.makeText(navController.context, "Please sign in first", Toast.LENGTH_SHORT).show()
                                        navController.navigate(Screen.Login.route)
                                    }
                                )
                            },
                            onNavigateToLogin = { navController.navigate(Screen.Login.route) }
                        )
                    }
                    composable(Screen.Login.route) {
                        LoginScreen(
                            uiState = authState,
                            serverUrl = tutorState.serverUrl,
                            onRequestOtp = { email -> authViewModel.requestOtp(tutorState.serverUrl, email) },
                            onVerifyOtp = { email, otp ->
                                authViewModel.verifyOtp(tutorState.serverUrl, email, otp) {
                                    subscriptionViewModel.loadPlansAndStatus(tutorState.serverUrl)
                                    navController.navigate(Screen.Home.route)
                                }
                            },
                            onResetOtp = { authViewModel.resetOtpState() },
                            onLogout = {
                                authViewModel.logout()
                                subscriptionViewModel.loadPlansAndStatus(tutorState.serverUrl)
                            },
                            onNavigateBack = { navController.navigate(Screen.Home.route) }
                        )
                    }
                }
            }

            // Settings dialog
            if (tutorState.showSettingsDialog) {
                ServerConfigDialog(
                    currentUrl = tutorState.serverUrl,
                    onDismiss = { tutorViewModel.closeSettings() },
                    onSave = { tutorViewModel.saveServerUrl(it) }
                )
            }
        }
    }
}

// ── Navigation Drawer Content ────────────────────────────────────────────────
@Composable
fun AppDrawer(
    currentRoute: String,
    authState: com.englishtutor.app.ui.AuthUiState,
    subState: com.englishtutor.app.ui.SubscriptionUiState,
    navController: NavController,
    onClose: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(270.dp),
        drawerContainerColor = Color(0xFF0D1B2E),
        drawerContentColor = Color.White
    ) {
        // Drawer Header with User Status
        Box(
            modifier = Modifier.fillMaxWidth().background(
                Brush.verticalGradient(listOf(Color(0xFF0A2744), Color(0xFF0D1B2E)))
            ).padding(20.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF2979FF), AccentTeal))),
                    contentAlignment = Alignment.Center
                ) { Text("VB", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }

                Spacer(Modifier.height(10.dp))
                Text("VocalBharat", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)

                if (authState.isLoggedIn) {
                    Text(
                        text = authState.userEmail ?: "Logged In",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(
                                if (subState.status.isActive) Color(0xFF00E676).copy(alpha = 0.2f)
                                else Color.White.copy(alpha = 0.1f)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (subState.status.isActive)
                                "⚡ ${if (subState.status.plan == "trial") "Trial" else "Monthly"} (${subState.status.daysRemaining}d left)"
                            else "Free Tier",
                            color = if (subState.status.isActive) Color(0xFF00E676) else Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            navController.navigate(Screen.Login.route)
                            onClose()
                        }
                    ) {
                        Text("👋 Guest · Sign in with OTP", color = AccentTeal, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
        Spacer(Modifier.height(8.dp))

        // Nav items
        screens.forEach { screen ->
            val isSelected = currentRoute == screen.route
            NavigationDrawerItem(
                icon = {
                    Icon(
                        screen.icon, null,
                        tint = if (isSelected) AccentTeal else Color.White.copy(alpha = 0.6f)
                    )
                },
                label = {
                    Text(
                        screen.label,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f),
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                selected = isSelected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                    onClose()
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = AccentTeal.copy(alpha = 0.15f),
                    unselectedContainerColor = Color.Transparent
                )
            )
        }

        Spacer(Modifier.weight(1f))
        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
        Spacer(Modifier.height(8.dp))
        Text(
            "VocalBharat v1.0",
            color = Color.White.copy(alpha = 0.25f),
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 20.dp, bottom = 16.dp)
        )
    }
}


// ── Home Screen Content ──────────────────────────────────────────────────────
@Composable
fun HomeScreenContent(
    viewModel: TutorViewModel,
    subState: com.englishtutor.app.ui.SubscriptionUiState,
    onNavigateToSubscription: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Page title & optional trial promo
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Hindi to English",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Hindi mein bolen, English mein seekhen 🎯",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            // Promo Banner if trial is available
            if (!subState.status.isActive) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color(0xFF00E676).copy(alpha = 0.12f))
                        .border(1.dp, Color(0xFF00E676).copy(alpha = 0.35f), RoundedCornerShape(100.dp))
                        .clickable(onClick = onNavigateToSubscription)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Stars, null, tint = Color(0xFF00E676), modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "5-Day Trial: Only ₹5 (then ₹300/mo) →",
                        color = Color(0xFF00E676),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        CompactMicButton(isRecording = uiState.isRecording, onClick = onRequestPermission)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!uiState.isServerOnline && !uiState.isRecording && !uiState.isProcessing) {
                GlassBanner(
                    icon = { Icon(Icons.Default.WarningAmber, null, tint = RecordingRed, modifier = Modifier.size(16.dp)) },
                    text = "Backend offline. Tap Settings to configure.",
                    tint = RecordingRed,
                    action = { TextButton(onClick = { viewModel.openSettings() }) { Text("Fix", fontSize = 11.sp, color = PrimaryLight) } }
                )
            }
            uiState.errorMessage?.let { error ->
                GlassBanner(
                    icon = { Icon(Icons.Default.ErrorOutline, null, tint = RecordingRed, modifier = Modifier.size(16.dp)) },
                    text = error,
                    tint = RecordingRed
                )
            }
            AnimatedVisibility(visible = uiState.isProcessing, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.07f)).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(color = AccentTeal, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("Translating to English...", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                }
            }
            AnimatedVisibility(visible = uiState.pendingTranscription != null, enter = slideInVertically { it / 2 } + fadeIn(), exit = slideOutVertically { it / 2 } + fadeOut()) {
                uiState.pendingTranscription?.let { pendingText ->
                    CompactTranscriptionPreview(
                        text = pendingText,
                        isTranslating = uiState.isTranslating,
                        onProceed = { viewModel.proceedToTranslate(pendingText) },
                        onCancel = { viewModel.cancelSpeakRequest() }
                    )
                }
            }
            AnimatedVisibility(visible = uiState.result != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                uiState.result?.let { response ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TranscriptionCard(text = response.transcription)
                        CorrectionCard(correction = response.correction, isPlayingAudio = uiState.isPlayingAudio, onPlayAudio = { viewModel.togglePlayAudio() })
                        PracticeCard(correction = response.correction)
                    }
                }
            }
        }
    }
}

// ── Shared Composables ───────────────────────────────────────────────────────
@Composable
fun CompactMicButton(isRecording: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = if (isRecording) 1.18f else 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "scale"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(110.dp)) {
            if (isRecording) {
                Box(modifier = Modifier.size(100.dp).scale(scale).clip(CircleShape).background(RecordingRed.copy(alpha = 0.18f)))
            }
            Box(
                modifier = Modifier.size(82.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(if (isRecording) RecordingRed else Color(0xFF2979FF), if (isRecording) Color(0xFFFF6D00) else AccentTeal)))
                    .border(2.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isRecording) "Stop" else "Speak",
                    tint = Color.White, modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (isRecording) "Listening... tap to stop" else "Tap to speak in Hindi",
            color = if (isRecording) RecordingRed else Color.White.copy(alpha = 0.55f),
            fontSize = 12.sp, textAlign = TextAlign.Center
        )
    }
}

@Composable
fun GlassBanner(icon: @Composable () -> Unit, text: String, tint: Color, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.10f))
            .border(1.dp, tint.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(8.dp))
        Text(text = text, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        action?.invoke()
    }
}

@Composable
fun CompactTranscriptionPreview(text: String, isTranslating: Boolean, onProceed: () -> Unit, onCancel: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.RecordVoiceOver, null, tint = AccentTeal, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("You said:", color = AccentTeal, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(text = text, color = Color.White, fontSize = 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onCancel, modifier = Modifier.weight(1f).height(36.dp), shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.7f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                contentPadding = PaddingValues(0.dp)
            ) { Text("Cancel", fontSize = 13.sp) }
            Button(
                onClick = onProceed, enabled = !isTranslating, modifier = Modifier.weight(1f).height(36.dp),
                shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF)),
                contentPadding = PaddingValues(0.dp)
            ) {
                if (isTranslating) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Proceed", fontSize = 13.sp)
            }
        }
    }
}
