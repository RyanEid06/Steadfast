package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.security.BackupManager
import com.example.core.security.BiometricLockManager
import com.example.core.time.StreakDurationCalculator
import com.example.data.datastore.SteadfastPreferences
import com.example.data.db.SteadfastDatabase
import com.example.data.model.MilestoneDefinition
import com.example.data.repository.BibleRepository
import com.example.data.repository.JournalRepository
import com.example.data.repository.MilestoneRepository
import com.example.data.repository.SteadfastRepository
import com.example.data.repository.VerseRepository
import com.example.data.seed.AssetSeedLoader
import com.example.ui.bible.BibleScreen
import com.example.ui.bible.BookDetailScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.journey.JourneyScreen
import com.example.ui.milestones.MilestonesScreen
import com.example.ui.motivation.MotivationScreen
import com.example.ui.navigation.BottomBarDestination
import com.example.ui.navigation.Screen
import com.example.ui.onboarding.OnboardingWizardScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.struggling.BreathingExerciseScreen
import com.example.ui.struggling.StrugglingInterventionScreen
import com.example.ui.theme.SteadfastGold
import com.example.ui.theme.SteadfastNavyDark
import com.example.ui.theme.SteadfastTheme
import com.example.ui.theme.ThemeSetting
import kotlinx.coroutines.launch

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

    private var isAppLockedState by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        database = SteadfastDatabase.getInstance(this)
        preferences = SteadfastPreferences(this)
        seedLoader = AssetSeedLoader(this)

        journalRepository = JournalRepository(database.journalDao())
        steadfastRepository = SteadfastRepository(database.streakDao(), journalRepository, preferences)
        bibleRepository = BibleRepository(database.bibleDao(), seedLoader, preferences)
        verseRepository = VerseRepository(seedLoader, database.bibleDao())
        milestoneRepository = MilestoneRepository(seedLoader, preferences)
        backupManager = BackupManager(this, database.streakDao(), database.journalDao(), database.bibleDao(), preferences)

        setContent {
            val themeSetting by preferences.themeSetting.collectAsState(initial = ThemeSetting.SYSTEM)
            val isAppLockEnabled by preferences.isAppLockEnabled.collectAsState(initial = false)
            val isOnboardingComplete by preferences.isOnboardingComplete.collectAsState(initial = false)

            // Cold start lock check
            LaunchedEffect(isAppLockEnabled) {
                if (isAppLockEnabled && !isAppLockedState) {
                    isAppLockedState = true
                    authenticateWithBiometrics()
                }
            }

            // Background timeout lock check (30 seconds)
            DisposableEffect(lifecycle) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_STOP -> {
                            val now = System.currentTimeMillis()
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                preferences.setLastBackgroundEpoch(now)
                            }
                        }
                        Lifecycle.Event.ON_START -> {
                            if (isAppLockEnabled) {
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                    val last = preferences.getLastBackgroundEpoch()
                                    if (last > 0 && System.currentTimeMillis() - last > 30_000) {
                                        isAppLockedState = true
                                        authenticateWithBiometrics()
                                    }
                                }
                            }
                        }
                        else -> {}
                    }
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }

            SteadfastTheme(themeSetting = themeSetting) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isAppLockedState) {
                        AppLockedOverlay(
                            onUnlockClicked = { authenticateWithBiometrics() }
                        )
                    } else {
                        MainAppContent(
                            isOnboardingComplete = isOnboardingComplete,
                            steadfastRepository = steadfastRepository,
                            journalRepository = journalRepository,
                            bibleRepository = bibleRepository,
                            verseRepository = verseRepository,
                            milestoneRepository = milestoneRepository,
                            backupManager = backupManager,
                            preferences = preferences,
                            seedLoader = seedLoader,
                            currentTheme = themeSetting,
                            isAppLockEnabled = isAppLockEnabled
                        )
                    }
                }
            }
        }
    }

    private fun authenticateWithBiometrics() {
        if (!BiometricLockManager.canAuthenticate(this)) {
            isAppLockedState = false
            return
        }
        BiometricLockManager.showPrompt(
            activity = this,
            title = "Unlock Steadfast",
            subtitle = "Verify to access your private recovery data",
            onSuccess = { isAppLockedState = false },
            onError = { /* Keep locked */ }
        )
    }
}

@Composable
fun AppLockedOverlay(onUnlockClicked: () -> Unit) {
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
                text = "Your private sanctuary is protected by device credentials.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = onUnlockClicked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SteadfastGold,
                    contentColor = SteadfastNavyDark
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("app_locked_unlock_button")
            ) {
                Text("Unlock App", fontWeight = FontWeight.Bold)
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

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // State flows
    val activeSession by steadfastRepository.activeSession.collectAsState(initial = null)
    val allSessions by steadfastRepository.allSessions.collectAsState(initial = emptyList())
    val archivedSessions by steadfastRepository.archivedSessions.collectAsState(initial = emptyList())
    val journalEntries by journalRepository.allEntries.collectAsState(initial = emptyList())

    val bibleProgress by bibleRepository.progress.collectAsState(
        initial = com.example.data.repository.BibleProgress(0, 1189, 0f, 0, 929, 0f, 0, 260, 0f, 0, 66)
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
    val now = remember { System.currentTimeMillis() }
    val currentDuration = remember(activeSession, now) {
        if (activeSession != null) {
            StreakDurationCalculator.calculate(
                startEpochMillis = activeSession!!.startEpochMillis,
                startZoneId = activeSession!!.startZoneId
            )
        } else {
            StreakDurationCalculator.calculate(now, "UTC")
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
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = when (dest) {
                                        BottomBarDestination.DASHBOARD -> Icons.Default.Home
                                        BottomBarDestination.JOURNEY -> Icons.Default.TrendingUp
                                        BottomBarDestination.BIBLE -> Icons.Default.AutoStories
                                    },
                                    contentDescription = dest.title
                                )
                            },
                            label = { Text(dest.title, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.surface,
                                indicatorColor = SteadfastGold,
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
                            scope.launch {
                                steadfastRepository.startInitialSession(focusId, startMillis, zoneId)
                                bibleRepository.setReadingGoal(goal)
                                bibleRepository.setOtCurrentReading(otBookId, otCh)
                                bibleRepository.setNtCurrentReading(ntBookId, ntCh)
                                preferences.setThemeSetting(theme)
                                preferences.setAppLockEnabled(lock)
                                preferences.setOnboardingComplete(true)
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Onboarding.route) { inclusive = true }
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
                composable(Screen.Journey.route) {
                    JourneyScreen(
                        activeSession = activeSession,
                        allSessions = allSessions,
                        archivedSessions = archivedSessions,
                        journalEntries = journalEntries,
                        nextMilestone = nextMilestone,
                        longestStreakDays = longestStreakDays,
                        onStartAgainConfirmed = { startMillis, note ->
                            scope.launch {
                                steadfastRepository.restartStreak(
                                    newStartEpochMillis = startMillis,
                                    optionalNote = note
                                )
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
                            navController.navigate(Screen.Journey.route)
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
                            scope.launch { preferences.setAppLockEnabled(enabled) }
                        },
                        onFocusChanged = { focus ->
                            scope.launch { steadfastRepository.updateActiveSessionFocus(focus) }
                        },
                        onStreakStartChanged = { newStart ->
                            scope.launch {
                                activeSession?.let {
                                    steadfastRepository.updateSessionStart(it.id, newStart)
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
                            navController.navigate(Screen.Onboarding.route)
                        },
                        onBackClicked = { navController.popBackStack() }
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
