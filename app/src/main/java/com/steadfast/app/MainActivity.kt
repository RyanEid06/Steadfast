package com.steadfast.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.steadfast.app.core.security.BackupManager
import com.steadfast.app.core.security.BiometricLockManager
import com.steadfast.app.core.security.AppLockSession
import com.steadfast.app.core.security.RestoreState
import com.steadfast.app.core.time.rememberForegroundTimeMillis
import com.steadfast.app.core.time.StreakDurationCalculator
import com.steadfast.app.data.datastore.SteadfastPreferences
import com.steadfast.app.data.db.SteadfastDatabase
import com.steadfast.app.data.model.MilestoneDefinition
import com.steadfast.app.data.repository.BibleRepository
import com.steadfast.app.data.repository.JournalRepository
import com.steadfast.app.data.repository.MilestoneRepository
import com.steadfast.app.data.repository.SteadfastRepository
import com.steadfast.app.data.repository.VerseRepository
import com.steadfast.app.data.seed.AssetSeedLoader
import com.steadfast.app.ui.bible.BibleScreen
import com.steadfast.app.ui.bible.BookDetailScreen
import com.steadfast.app.ui.dashboard.DashboardScreen
import com.steadfast.app.ui.journey.JourneyScreen
import com.steadfast.app.ui.milestones.MilestonesScreen
import com.steadfast.app.ui.motivation.MotivationScreen
import com.steadfast.app.ui.navigation.BottomBarDestination
import com.steadfast.app.ui.navigation.Screen
import com.steadfast.app.ui.onboarding.OnboardingWizardScreen
import com.steadfast.app.ui.settings.SettingsScreen
import com.steadfast.app.ui.struggling.BreathingExerciseScreen
import com.steadfast.app.ui.struggling.StrugglingInterventionScreen
import com.steadfast.app.ui.theme.SteadfastGold
import com.steadfast.app.ui.theme.SteadfastNavyDark
import com.steadfast.app.ui.theme.SteadfastTheme
import com.steadfast.app.ui.theme.ThemeSetting
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

class MainActivity : FragmentActivity() {

    private lateinit var database: SteadfastDatabase
    private lateinit var preferences: SteadfastPreferences
    private lateinit var seedLoader: AssetSeedLoader
    private lateinit var steadfastRepository: SteadfastRepository
    private lateinit var journalRepository: JournalRepository
    private lateinit var bibleRepository: BibleRepository
    private lateinit var verseRepository: VerseRepository
    private lateinit var milestoneRepository: MilestoneRepository
    private lateinit var backupManager: BackupManager
    private lateinit var lockSession: AppLockSession
    private lateinit var lockPrompt: BiometricLockManager.PromptController
    private var startupReady by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        database = SteadfastDatabase.getInstance(this)
        preferences = SteadfastPreferences(this)
        seedLoader = AssetSeedLoader(this)
        journalRepository = JournalRepository(database.journalDao())
        steadfastRepository = SteadfastRepository(database, journalRepository, preferences)
        bibleRepository = BibleRepository(database.bibleDao(), seedLoader, preferences)
        verseRepository = VerseRepository(seedLoader, database.bibleDao())
        milestoneRepository = MilestoneRepository(seedLoader, preferences)
        backupManager = BackupManager(this, database, preferences)
        lockSession = ViewModelProvider(this)[AppLockSession::class.java]
        lockPrompt = BiometricLockManager.PromptController(this, lockSession)

        lifecycleScope.launch {
            backupManager.restoreState.collectLatest { state ->
                startupReady = false
                if (state == RestoreState.READY) {
                    // Read the coherent restored preference before opening the content gate.
                    lockSession.updatePreferences(preferences.isAppLockEnabled.first())
                    startupReady = true
                    maybeAuthenticateAutomatically()
                }
            }
        }
        lifecycleScope.launch {
            preferences.isAppLockEnabled.collect { enabled ->
                if (startupReady) {
                    lockSession.updatePreferences(enabled)
                    maybeAuthenticateAutomatically()
                }
            }
        }
        lifecycleScope.launch { backupManager.recoverPendingRestore() }

        setContent {
            val themeSetting by preferences.themeSetting.collectAsState(initial = ThemeSetting.SYSTEM)
            val isOnboardingComplete by preferences.isOnboardingComplete.collectAsState(initial = null)
            val restoreState by backupManager.restoreState.collectAsState()
            SteadfastTheme(themeSetting = themeSetting) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when {
                        restoreState == RestoreState.RECOVERY_REQUIRED -> StartupRecoveryScreen {
                            lifecycleScope.launch { backupManager.recoverPendingRestore() }
                        }
                        restoreState != RestoreState.READY || !startupReady ||
                            !lockSession.preferencesLoaded || lockSession.checkingResume ||
                            isOnboardingComplete == null -> SteadfastSplashScreen()
                        lockSession.isLocked -> AppLockedOverlay(
                            onUnlockClicked = { authenticateWithBiometrics() },
                            errorMessage = lockSession.errorMessage,
                            isAuthenticating = lockSession.promptInFlight
                        )
                        lockSession.canShowProtectedContent -> MainAppContent(
                            isOnboardingComplete = isOnboardingComplete == true,
                            steadfastRepository = steadfastRepository,
                            journalRepository = journalRepository,
                            bibleRepository = bibleRepository,
                            verseRepository = verseRepository,
                            milestoneRepository = milestoneRepository,
                            backupManager = backupManager,
                            preferences = preferences,
                            seedLoader = seedLoader,
                            currentTheme = themeSetting,
                            isAppLockEnabled = lockSession.enabled
                        )
                        else -> SteadfastSplashScreen()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            val last = preferences.getLastBackgroundEpoch()
            lockSession.onForeground(System.currentTimeMillis(), last)
            maybeAuthenticateAutomatically()
        }
    }

    override fun onResume() {
        super.onResume()
        maybeAuthenticateAutomatically()
    }

    override fun onStop() {
        if (!isChangingConfigurations) {
            val now = System.currentTimeMillis()
            lockSession.onBackground(now)
            lifecycleScope.launch { preferences.setLastBackgroundEpoch(now) }
        }
        super.onStop()
    }

    override fun onDestroy() {
        lockPrompt.dispose(isChangingConfigurations)
        super.onDestroy()
    }

    private fun maybeAuthenticateAutomatically() {
        if (startupReady && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) &&
            lockSession.takeAutomaticPrompt()) authenticateWithBiometrics()
    }

    private fun authenticateWithBiometrics() {
        if (startupReady) lockPrompt.showPrompt()
    }
}

@Composable
private fun StartupRecoveryScreen(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Recovery needs attention", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Text("Steadfast could not finish restoring your data. Your data will stay hidden until recovery succeeds.", textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry, modifier = Modifier.testTag("startup_recovery_retry")) { Text("Retry recovery") }
    }
}
@Composable
fun SteadfastSplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F141E)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.hero_splash),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.45f
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1B2334))
                    .border(1.5.dp, SteadfastGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(30.dp)) {
                    val gold = Color(0xFFD4AF37)
                    val stroke = 3.dp.toPx()
                    drawLine(
                        color = gold,
                        start = androidx.compose.ui.geometry.Offset(size.width / 2f, 2.dp.toPx()),
                        end = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height - 2.dp.toPx()),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                    val horizY = size.height * 0.38f
                    drawLine(
                        color = gold,
                        start = androidx.compose.ui.geometry.Offset(3.dp.toPx(), horizY),
                        end = androidx.compose.ui.geometry.Offset(size.width - 3.dp.toPx(), horizY),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "STEADFAST",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = SteadfastGold
            )
        }
    }
}

@Composable
fun AppLockedOverlay(onUnlockClicked: () -> Unit, errorMessage: String? = null, isAuthenticating: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SteadfastNavyDark)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                tint = SteadfastGold,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Steadfast is Locked",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = errorMessage ?: "Your private sanctuary is protected by device credentials.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = onUnlockClicked,
                enabled = !isAuthenticating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SteadfastGold,
                    contentColor = SteadfastNavyDark
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("app_locked_unlock_button")
            ) {
                Text(if (isAuthenticating) "Verifying…" else "Unlock App", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MainAppContent(
    isOnboardingComplete: Boolean,
    steadfastRepository: SteadfastRepository,
    journalRepository: JournalRepository,
    bibleRepository: BibleRepository,
    verseRepository: VerseRepository,
    milestoneRepository: MilestoneRepository,
    backupManager: BackupManager,
    preferences: SteadfastPreferences,
    seedLoader: AssetSeedLoader,
    currentTheme: ThemeSetting,
    isAppLockEnabled: Boolean
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as FragmentActivity

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route?.substringBefore('?')

    // State flows
    val activeSession by steadfastRepository.activeSession.collectAsState(initial = null)
    val allSessions by steadfastRepository.allSessions.collectAsState(initial = emptyList())
    val archivedSessions by steadfastRepository.archivedSessions.collectAsState(initial = emptyList())
    val journalEntries by journalRepository.allEntries.collectAsState(initial = emptyList())

    val bibleProgress by bibleRepository.progress.collectAsState(
        initial = com.steadfast.app.data.repository.BibleProgress(0, 1189, 0f, 0, 929, 0f, 0, 260, 0f, 0, 66)
    )
    val booksWithProgress by bibleRepository.booksWithProgress.collectAsState(initial = emptyList())

    val otReading by bibleRepository.otCurrentReading.collectAsState(initial = null)
    val ntReading by bibleRepository.ntCurrentReading.collectAsState(initial = null)
    val readingGoal by bibleRepository.readingGoalMinimum.collectAsState(initial = 2)
    val currentFocusId by preferences.recoveryFocusId.collectAsState(initial = "pornography")

    val focuses = remember { seedLoader.loadRecoveryFocuses() }
    val bibleBooks = remember { seedLoader.loadBibleBooks() }
    val scaleComparisons = remember { seedLoader.getScaleComparisons() }

    val dailyEncouragement = remember { verseRepository.getRandomDailyEncouragement() }

    // Derive streak duration and check milestones
    val now = rememberForegroundTimeMillis(enabled = activeSession != null)
    val currentDuration = remember(activeSession, now) {
        if (activeSession != null) {
            StreakDurationCalculator.calculate(
                startEpochMillis = activeSession!!.startEpochMillis,
                startZoneId = activeSession!!.startZoneId,
                nowEpochMillis = now
            )
        } else {
            StreakDurationCalculator.calculate(now, "UTC", nowEpochMillis = now)
        }
    }

    val nextMilestone = remember(currentDuration) {
        milestoneRepository.getNextMilestone(currentDuration)
    }

    val longestStreakDays = remember(allSessions) {
        steadfastRepository.calculateLongestStreakDays(allSessions)
    }

    // Pending Celebration overlay
    var celebrationToShow by remember { mutableStateOf<MilestoneDefinition?>(null) }
    LaunchedEffect(currentDuration) {
        if (activeSession != null) {
            val pending = milestoneRepository.getPendingCelebration(currentDuration)
            if (pending != null) {
                celebrationToShow = pending
            }
        }
    }

    val isRootScreen = currentRoute in listOf(
        Screen.Dashboard.route,
        Screen.Journey.route,
        Screen.Bible.route
    )

    Scaffold(
        bottomBar = {
            if (isRootScreen && isOnboardingComplete) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = SteadfastGold
                ) {
                    listOf(
                        BottomBarDestination.DASHBOARD,
                        BottomBarDestination.JOURNEY,
                        BottomBarDestination.BIBLE
                    ).forEach { dest ->
                        val selected = currentRoute == dest.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != dest.route) {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        // Restoring the start destination can replay the saved
                                        // intervention/Journal stack instead of showing Dashboard.
                                        restoreState = dest != BottomBarDestination.DASHBOARD
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = when (dest) {
                                        BottomBarDestination.DASHBOARD -> Icons.Default.Home
                                        BottomBarDestination.JOURNEY -> Icons.AutoMirrored.Filled.TrendingUp
                                        BottomBarDestination.BIBLE -> Icons.Default.AutoStories
                                    },
                                    contentDescription = dest.title
                                )
                            },
                            label = { Text(dest.title, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SteadfastGold,
                                indicatorColor = Color.Transparent,
                                selectedTextColor = SteadfastGold,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_item_${dest.title.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NavHost(
                navController = navController,
                startDestination = if (isOnboardingComplete) Screen.Dashboard.route else Screen.Onboarding.route
            ) {
                // Onboarding Wizard
                composable(Screen.Onboarding.route) {
                    OnboardingWizardScreen(
                        focuses = focuses,
                        bibleBooks = bibleBooks,
                        onComplete = { focusId, startMillis, zoneId, otBookId, otCh, ntBookId, ntCh, goal, theme, lock ->
                            if (BiometricLockManager.canChangeAppLock(context, lock)) {
                                // Enabling lock removes this composition; finish persistence in the activity scope.
                                activity.lifecycleScope.launch {
                                    try {
                                        steadfastRepository.startInitialSession(focusId, startMillis, zoneId)
                                        bibleRepository.setReadingGoal(goal)
                                        bibleRepository.setOtCurrentReading(otBookId, otCh)
                                        bibleRepository.setNtCurrentReading(ntBookId, ntCh)
                                        preferences.setThemeSetting(theme)
                                        preferences.setAppLockEnabled(lock)
                                        preferences.setOnboardingComplete(true)
                                        if (!lock) navController.navigate(Screen.Dashboard.route) {
                                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                                        }
                                    } catch (error: IllegalArgumentException) {
                                        Toast.makeText(context, error.message ?: "Check your streak start date.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    )
                }

                // 1. Dashboard (Fixed, No Scroll)
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        activeSession = activeSession,
                        dailyEncouragement = dailyEncouragement,
                        onStrugglingClicked = { navController.navigate(Screen.Struggling.route) },
                        onSettingsClicked = { navController.navigate(Screen.Settings.route) }
                    )
                }

                // 2. Journey (Overview, History, Journal)
                composable(
                    Screen.Journey.route + "?journalContext={journalContext}",
                    arguments = listOf(navArgument("journalContext") { type = NavType.StringType; nullable = true; defaultValue = null })
                ) { journeyEntry ->
                    JourneyScreen(
                        initialJournalContext = journeyEntry.arguments?.getString("journalContext"),
                        activeSession = activeSession,
                        allSessions = allSessions,
                        archivedSessions = archivedSessions,
                        journalEntries = journalEntries,
                        nextMilestone = nextMilestone,
                        longestStreakDays = longestStreakDays,
                        onStartAgainConfirmed = { startMillis, note ->
                            scope.launch {
                                try {
                                    steadfastRepository.restartStreak(
                                        newStartEpochMillis = startMillis,
                                        optionalNote = note
                                    )
                                } catch (error: IllegalArgumentException) {
                                    Toast.makeText(context, error.message ?: "Check your streak start date.", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onAddJournalEntry = { body, ctx ->
                            scope.launch {
                                journalRepository.addEntry(body, ctx, activeSession?.id)
                            }
                        },
                        onUpdateJournalEntry = { id, body, ctx ->
                            scope.launch {
                                journalRepository.updateEntry(id, body, ctx)
                            }
                        },
                        onDeleteJournalEntry = { id ->
                            scope.launch {
                                journalRepository.deleteEntry(id)
                            }
                        },
                        onNavigateToMilestones = { navController.navigate(Screen.Milestones.route) },
                        onNavigateToMotivation = { navController.navigate(Screen.Motivation.route) },
                        onSettingsClicked = { navController.navigate(Screen.Settings.route) }
                    )
                }

                // 3. Bible (Progress, Book list, Currently Reading)
                composable(Screen.Bible.route) {
                    BibleScreen(
                        progress = bibleProgress,
                        books = booksWithProgress,
                        otReading = otReading,
                        ntReading = ntReading,
                        readingGoal = readingGoal,
                        onBookClicked = { bookId ->
                            navController.navigate(Screen.BookDetail.createRoute(bookId))
                        },
                        onSettingsClicked = { navController.navigate(Screen.Settings.route) }
                    )
                }

                // Book Detail (Chapter Grid)
                composable(
                    route = Screen.BookDetail.route,
                    arguments = listOf(navArgument("bookId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val bookId = backStackEntry.arguments?.getInt("bookId") ?: 1
                    val bookProgression by bibleRepository.getBookWithProgress(bookId).collectAsState(initial = null)
                    val bookObj = bibleRepository.getBook(bookId)
                    val currentLoc = if (bookObj?.testament == "OT") otReading else ntReading

                    BookDetailScreen(
                        bookWithProgress = bookProgression,
                        currentReading = currentLoc,
                        onToggleChapter = { ch ->
                            scope.launch {
                                bibleRepository.toggleChapter(bookId, ch)
                            }
                        },
                        onSetCurrentReading = { ch ->
                            scope.launch {
                                if (bookObj?.testament == "OT") {
                                    bibleRepository.setOtCurrentReading(bookId, ch)
                                } else {
                                    bibleRepository.setNtCurrentReading(bookId, ch)
                                }
                            }
                        },
                        onBackClicked = { navController.popBackStack() }
                    )
                }

                // I'm Struggling Sanctuary
                composable(Screen.Struggling.route) {
                    StrugglingInterventionScreen(
                        verseRepository = verseRepository,
                        onTakeBreathingExercise = { navController.navigate(Screen.Breathing.route) },
                        onWriteNote = {
                            navController.navigate(Screen.Journey.route + "?journalContext=URGE")
                        },
                        onViewProgress = {
                            navController.navigate(Screen.Journey.route)
                        },
                        onClose = { navController.popBackStack() }
                    )
                }

                // Guided Breathing Exercise
                composable(Screen.Breathing.route) {
                    BreathingExerciseScreen(
                        onDoneClicked = { navController.popBackStack() }
                    )
                }

                // Milestones Timeline
                composable(Screen.Milestones.route) {
                    MilestonesScreen(
                        activeSession = activeSession,
                        milestoneRepository = milestoneRepository,
                        onBackClicked = { navController.popBackStack() }
                    )
                }

                // Motivation Insights
                composable(Screen.Motivation.route) {
                    MotivationScreen(
                        activeSession = activeSession,
                        scaleComparisons = scaleComparisons,
                        onBackClicked = { navController.popBackStack() }
                    )
                }

                // Settings
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        currentTheme = currentTheme,
                        isAppLockEnabled = isAppLockEnabled,
                        currentFocusId = currentFocusId,
                        focuses = focuses,
                        activeSession = activeSession,
                        readingGoal = readingGoal,
                        otReading = otReading,
                        ntReading = ntReading,
                        backupManager = backupManager,
                        onThemeChanged = { theme ->
                            scope.launch { preferences.setThemeSetting(theme) }
                        },
                        onAppLockChanged = { enabled ->
                            scope.launch { BiometricLockManager.setAppLockEnabled(context, preferences, enabled) }
                        },
                        onFocusChanged = { focus ->
                            scope.launch { steadfastRepository.updateActiveSessionFocus(focus) }
                        },
                        onStreakStartChanged = { newStart ->
                            scope.launch {
                                try {
                                    activeSession?.let {
                                        steadfastRepository.updateSessionStart(it.id, newStart)
                                    }
                                } catch (error: IllegalArgumentException) {
                                    Toast.makeText(context, error.message ?: "Check your streak start date.", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onReadingGoalChanged = { goal ->
                            scope.launch { bibleRepository.setReadingGoal(goal) }
                        },
                        onClearOtReading = {
                            scope.launch { bibleRepository.setOtCurrentReading(null, null) }
                        },
                        onClearNtReading = {
                            scope.launch { bibleRepository.setNtCurrentReading(null, null) }
                        },
                        onReplayTutorial = {
                            navController.navigate(Screen.Tutorial.route)
                        },
                        onBackClicked = { navController.popBackStack() }
                    )
                }

                // Dedicated Tutorial Replay Screen
                composable(Screen.Tutorial.route) {
                    com.steadfast.app.ui.onboarding.TutorialReplayScreen(
                        onDismiss = { navController.popBackStack() }
                    )
                }
            }

            // Milestone Celebration Overlay
            celebrationToShow?.let { m ->
                MilestoneCelebrationOverlay(
                    milestone = m,
                    onDismiss = {
                        scope.launch {
                            milestoneRepository.markCelebrated(m.id)
                            celebrationToShow = null
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MilestoneCelebrationOverlay(
    milestone: MilestoneDefinition,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(2.dp, SteadfastGold),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = SteadfastGold,
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Milestone Reached!",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.2.sp,
                    color = SteadfastGold,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = milestone.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "You have remained steadfast in freedom. God is faithfully building your endurance day by day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SteadfastGold,
                        contentColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("celebration_continue_button")
                ) {
                    Text("Continue Forward in Grace", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
