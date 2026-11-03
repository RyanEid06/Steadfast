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
import androidx.compose.runtime.produceState
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
import androidx.lifecycle.repeatOnLifecycle
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
import com.steadfast.app.core.time.rememberForegroundBoundaryTimeMillis
import com.steadfast.app.core.time.StreakDurationCalculator
import com.steadfast.app.data.datastore.SteadfastPreferences
import com.steadfast.app.data.db.SteadfastDatabase
import com.steadfast.app.data.model.MilestoneDefinition
import com.steadfast.app.data.repository.BibleRepository
import com.steadfast.app.data.repository.JournalRepository
import com.steadfast.app.data.repository.MilestoneRepository
import com.steadfast.app.data.repository.SteadfastRepository
import com.steadfast.app.data.repository.StartupState
import com.steadfast.app.data.datastore.UserPreferencesSnapshot
import com.steadfast.app.data.datastore.ReadingLocation
import com.steadfast.app.ui.onboarding.OnboardingCompletionModel
import com.steadfast.app.data.repository.VerseRepository
import com.steadfast.app.data.seed.AssetSeedLoader
import com.steadfast.app.widget.requestWidgetRefresh
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
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
    private var catalogsReady by mutableStateOf(false)
    private var catalogError by mutableStateOf(false)
    private var startupRepair by mutableStateOf<String?>(null)
    private var startupSettings by mutableStateOf<UserPreferencesSnapshot?>(null)
    private var catalogJob: kotlinx.coroutines.Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        database = SteadfastDatabase.getInstance(this)
        preferences = SteadfastPreferences(applicationContext)
        seedLoader = AssetSeedLoader(applicationContext)
        journalRepository = JournalRepository(database.journalDao())
        steadfastRepository = SteadfastRepository(database, journalRepository, preferences)
        bibleRepository = BibleRepository(database.bibleDao(), seedLoader, preferences)
        verseRepository = VerseRepository(seedLoader, database.bibleDao(), preferences)
        milestoneRepository = MilestoneRepository(seedLoader, preferences)
        backupManager = BackupManager(applicationContext, database, preferences)
        lockSession = ViewModelProvider(this)[AppLockSession::class.java]
        lockPrompt = BiometricLockManager.PromptController(this, lockSession)

        lifecycleScope.launch {
            backupManager.restoreState.collectLatest { state ->
                startupReady = false
                catalogJob?.cancel()
                if (state == RestoreState.READY) {
                    // Read the coherent restored preference before opening the content gate.
                    lockSession.updatePreferences(preferences.isAppLockEnabled.first())
                    startupReady = true
                    requestWidgetRefresh(applicationContext)
                    loadStartupCatalogs()
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
                        catalogError -> StartupCatalogErrorScreen { loadStartupCatalogs() }
                        !catalogsReady -> SteadfastSplashScreen()
                        startupRepair != null -> StartupRepairFlow(
                            startupRepair!!, startupSettings!!, themeSetting, seedLoader, backupManager,
                            onRetry = { loadStartupCatalogs() }
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
                            isAppLockEnabled = lockSession.enabled,
                            initialPreferences = startupSettings!!
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

    private fun loadStartupCatalogs() {
        catalogError = false
        catalogsReady = false
        startupRepair = null
        catalogJob?.cancel()
        catalogJob = lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    backupManager.restoreState.first { it == RestoreState.READY }
                    seedLoader.preload()
                    val state = steadfastRepository.reconcileStartup(seedLoader.loadRecoveryFocuses().map { it.id }.toSet())
                    startupRepair = (state as? StartupState.RepairRequired)?.reason
                    startupSettings = preferences.snapshotUserSettings()
                }
                catalogsReady = true
                requestWidgetRefresh(applicationContext)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                catalogError = true
            }
        }
    }
}

@Composable
internal fun StartupRepairScreen(reason: String, onRetry: () -> Unit, onRestore: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text("Recovery history needs attention", style = MaterialTheme.typography.headlineSmall)
        Text(reason, textAlign = TextAlign.Center)
        Text("Your records have been preserved.")
        Button(onClick = onRetry) { Text("Retry") }
        Button(onClick = onRestore) { Text("Restore a backup") }
    }
}

@Composable
private fun StartupRepairFlow(
    reason: String, settings: UserPreferencesSnapshot, theme: ThemeSetting,
    loader: AssetSeedLoader, backupManager: BackupManager, onRetry: () -> Unit
) {
    var restoring by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    if (!restoring) {
        StartupRepairScreen(reason, onRetry, onRestore = { restoring = true })
    } else {
        SettingsScreen(
            currentTheme = theme, isAppLockEnabled = settings.appLockEnabled,
            currentFocusId = settings.recoveryFocusId, focuses = loader.loadRecoveryFocuses(),
            activeSession = null, readingGoal = settings.readingGoalMinimum,
            otReading = settings.oldTestamentCurrent, ntReading = settings.newTestamentCurrent,
            backupManager = backupManager,
            onThemeChanged = {}, onAppLockChanged = {}, onFocusChanged = {}, onStreakStartChanged = {},
            onReadingGoalChanged = {}, onClearOtReading = {}, onClearNtReading = {}, onReplayTutorial = {},
            onBackClicked = { restoring = false }, bibleBooks = loader.loadBibleBooks(), recoveryOnly = true
        )
    }
}

@Composable
private fun StartupCatalogErrorScreen(onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text("Steadfast could not load", style = MaterialTheme.typography.headlineSmall)
        Text("Your data has not been changed. Retry loading the app's bundled content.", textAlign = TextAlign.Center)
        Button(onClick = onRetry, modifier = Modifier.testTag("startup_catalog_retry")) { Text("Retry") }
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
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
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
    isAppLockEnabled: Boolean,
    initialPreferences: UserPreferencesSnapshot
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val completion = androidx.lifecycle.viewmodel.compose.viewModel<OnboardingCompletionModel>(viewModelStoreOwner = activity)
    val settingsWorkflow = androidx.lifecycle.viewmodel.compose.viewModel<com.steadfast.app.ui.settings.SettingsWorkflowModel>(viewModelStoreOwner = activity)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route?.substringBefore('?')

    // State flows
    val loadedSessions by steadfastRepository.allSessions.collectAsState(initial = null)
    val allSessions = loadedSessions ?: run { SteadfastSplashScreen(); return }
    val activeSession = allSessions.singleOrNull { it.endEpochMillis == null }
    val archivedSessions = allSessions.filter { it.endEpochMillis != null }
    val journalEntries by journalRepository.allEntries.collectAsState(initial = emptyList())

    val bibleProgress by bibleRepository.progress.collectAsState(initial = null)
    val booksWithProgress by bibleRepository.booksWithProgress.collectAsState(initial = null)

    val otReading by bibleRepository.otCurrentReading.collectAsState(initial = initialPreferences.oldTestamentCurrent)
    val ntReading by bibleRepository.ntCurrentReading.collectAsState(initial = initialPreferences.newTestamentCurrent)
    val readingGoal by bibleRepository.readingGoalMinimum.collectAsState(initial = initialPreferences.readingGoalMinimum)
    val bibleFilter by preferences.bibleTestamentFilter.collectAsState(initial = initialPreferences.bibleTestamentFilter)
    val journeyTab by preferences.journeyTab.collectAsState(initial = initialPreferences.journeyTab)
    val journalFilter by preferences.journalFilter.collectAsState(initial = initialPreferences.journalFilter)
    val currentFocusId by preferences.recoveryFocusId.collectAsState(initial = initialPreferences.recoveryFocusId)

    val focuses = remember { seedLoader.loadRecoveryFocuses() }
    val bibleBooks = remember { seedLoader.loadBibleBooks() }
    val scaleComparisons = remember { seedLoader.getScaleComparisons() }
    val contentLifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(settingsWorkflow, contentLifecycle) {
        contentLifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            settingsWorkflow.backupState.collect { state ->
                if (state.stage == com.steadfast.app.ui.settings.SettingsBackupStage.RESULT) {
                    settingsWorkflow.takeResultMessage()?.let { message ->
                        Toast.makeText(context.applicationContext, message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    LaunchedEffect(isOnboardingComplete, currentRoute) {
        if (isOnboardingComplete && currentRoute == Screen.Onboarding.route) {
            navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Onboarding.route) { inclusive = true } }
        }
    }

    val dailyEncouragement = remember { verseRepository.getRandomDailyEncouragement() }

    // Derive streak duration and check milestones
    val now = rememberForegroundBoundaryTimeMillis(
        activeSession?.startEpochMillis, activeSession?.startZoneId,
        enabled = activeSession != null && currentRoute in listOf(Screen.Dashboard.route, Screen.Journey.route)
    )
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

    LaunchedEffect(activeSession?.id, currentDuration.totalDays, currentDuration.years, currentDuration.months) {
        requestWidgetRefresh(context.applicationContext)
    }

    val longestStreakDays = remember(allSessions, now) {
        steadfastRepository.calculateLongestStreakDays(allSessions, now)
    }

    // Pending Celebration overlay
    var celebrationToShow by remember { mutableStateOf<Pair<Long, MilestoneDefinition>?>(null) }
    val shownMilestones by preferences.shownMilestones.collectAsState(initial = emptySet())
    LaunchedEffect(activeSession?.id, currentDuration, shownMilestones) {
        celebrationToShow = activeSession?.let { session ->
            milestoneRepository.getPendingCelebration(currentDuration)?.let { session.id to it }
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
                                    contentDescription = null
                                )
                            },
                            label = { Text(dest.title, style = MaterialTheme.typography.labelSmall, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
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
                        isCompleting = completion.isCompleting,
                        completionError = completion.error,
                        onComplete = { focusId, startMillis, zoneId, otBookId, otCh, ntBookId, ntCh, goal, theme, lock ->
                            if (BiometricLockManager.canChangeAppLock(context, lock)) {
                                completion.complete {
                                    val outcome = steadfastRepository.completeOnboarding(
                                        UserPreferencesSnapshot(
                                            themeMode = theme.name, recoveryFocusId = focusId,
                                            readingGoalMinimum = goal,
                                            oldTestamentCurrent = if (otBookId != null && otCh != null) ReadingLocation(otBookId, otCh) else null,
                                            newTestamentCurrent = if (ntBookId != null && ntCh != null) ReadingLocation(ntBookId, ntCh) else null,
                                            appLockEnabled = lock
                                        ), startMillis, zoneId, focuses.map { it.id }.toSet(), bibleBooks
                                    )
                                    check(outcome is StartupState.Ready) { "Setup needs attention." }
                                    requestWidgetRefresh(context.applicationContext)
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
                        focuses = focuses,
                        onStartAgainValidated = { startMillis, note ->
                            runCatching {
                                steadfastRepository.restartStreak(newStartEpochMillis = startMillis, optionalNote = note)
                                requestWidgetRefresh(context.applicationContext)
                            }
                        },
                        initialJournalContext = journeyEntry.arguments?.getString("journalContext"),
                        initialSelectedTab = journeyTab,
                        onSelectedTabChanged = { scope.launch { preferences.setJourneyTab(it) } },
                        initialJournalFilter = journalFilter,
                        onJournalFilterChanged = { scope.launch { preferences.setJournalFilter(it) } },
                        activeSession = activeSession,
                        allSessions = allSessions,
                        archivedSessions = archivedSessions,
                        journalEntries = journalEntries,
                        nextMilestone = nextMilestone,
                        longestStreakDays = longestStreakDays,
                        currentStreakDays = currentDuration.totalDays,
                        onStartAgainConfirmed = { startMillis, note ->
                            scope.launch {
                                try {
                                    steadfastRepository.restartStreak(
                                        newStartEpochMillis = startMillis,
                                        optionalNote = note
                                    )
                                    requestWidgetRefresh(context.applicationContext)
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
                        onSaveJournalEntryValidated = { entryId, body, ctx ->
                            try {
                                if (entryId == null) journalRepository.addEntry(body, ctx, activeSession?.id)
                                else journalRepository.updateEntry(entryId, body, ctx)
                                Result.success(Unit)
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (error: Exception) {
                                Result.failure(error)
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
                    val progress = bibleProgress
                    val loadedBooks = booksWithProgress
                    if (progress == null || loadedBooks == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Loading Bible progress…") }
                    } else BibleScreen(
                        progress = progress,
                        books = loadedBooks,
                        otReading = otReading,
                        ntReading = ntReading,
                        readingGoal = readingGoal,
                        initialSelectedFilter = bibleFilter,
                        onSelectedFilterChanged = { scope.launch { preferences.setBibleTestamentFilter(it) } },
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
                    val bookState by produceState<Result<com.steadfast.app.data.repository.BookWithProgress?>?>(null, bookId) {
                        try {
                            bibleRepository.getBookWithProgress(bookId).collect { value = Result.success(it) }
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (error: Exception) { value = Result.failure(error) }
                    }
                    val bookObj = bibleRepository.getBook(bookId)
                    val currentLoc = if (bookObj?.testament == "OT") otReading else ntReading

                    BookDetailScreen(
                        bookWithProgress = bookState?.getOrNull(),
                        isLoading = bookObj != null && bookState == null,
                        loadFailed = bookState?.isFailure == true,
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
                        workflow = settingsWorkflow,
                        bibleBooks = bibleBooks,
                        onSetOtReading = { book, chapter -> scope.launch { bibleRepository.setOtCurrentReading(book, chapter) } },
                        onSetNtReading = { book, chapter -> scope.launch { bibleRepository.setNtCurrentReading(book, chapter) } },
                        onSaveStreakStartValidated = { newStart ->
                            runCatching {
                                val session = requireNotNull(activeSession)
                                steadfastRepository.updateSessionStart(session.id, newStart)
                                requestWidgetRefresh(context.applicationContext)
                            }
                        },
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
                            scope.launch {
                                steadfastRepository.updateActiveSessionFocus(focus)
                                requestWidgetRefresh(context.applicationContext)
                            }
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
            celebrationToShow?.takeIf { isOnboardingComplete && currentRoute == Screen.Dashboard.route && it.first == activeSession?.id }?.let { (sessionId, m) ->
                MilestoneCelebrationOverlay(
                    milestone = m,
                    onDismiss = {
                        scope.launch {
                            if (steadfastRepository.getActiveSessionSync()?.id == sessionId) {
                                milestoneRepository.markCelebrated(m.id)
                            }
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
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
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
                    color = MaterialTheme.colorScheme.primary,
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
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
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
}
