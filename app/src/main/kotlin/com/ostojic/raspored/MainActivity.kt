package com.ostojic.raspored

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ostojic.raspored.data.SchedulePreferencesStore
import com.ostojic.raspored.data.SettingsRepository
import com.ostojic.raspored.domain.ConstantTimetableRepository
import com.ostojic.raspored.domain.ScheduleService
import com.ostojic.raspored.presentation.ScheduleEditorViewModel
import com.ostojic.raspored.presentation.ScheduleViewModel
import com.ostojic.raspored.presentation.SettingsViewModel
import com.ostojic.raspored.ui.CalendarScreen
import com.ostojic.raspored.ui.DailyScreen
import com.ostojic.raspored.ui.ScheduleEditorScreen
import com.ostojic.raspored.ui.SettingsScreen
import com.ostojic.raspored.ui.theme.RasporedTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock

/** App-scoped DataStore holding the persisted schedule/owner state. */
private val Context.scheduleDataStore: DataStore<Preferences> by preferencesDataStore(name = "schedule")

/**
 * Single Activity host for the app.
 *
 * This is the integration point that wires the dependency graph
 * ([ConstantTimetableRepository] → [ScheduleService], plus a shared
 * [SettingsRepository]) into a Compose navigation graph. The Daily, Calendar,
 * Settings and Editor destinations are backed by ViewModels that share a single
 * [SettingsRepository] instance, so date changes and schedule edits made on any
 * screen are reflected on the others. The app is fully offline and requires no
 * account or sign-in. (Requirement 6.1, 6.2, 8.1, 8.11, 8.12)
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RasporedTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RasporedApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

/** Navigation route constants for the single-Activity graph. */
private object Routes {
    const val DAILY = "daily"
    const val CALENDAR = "calendar"
    const val SETTINGS = "settings"
    const val EDITOR = "editor"
}

/**
 * Root composable: owns the shared dependency graph and the navigation graph.
 *
 * The persistence-backed dependencies ([ConstantTimetableRepository],
 * [ScheduleService], [SchedulePreferencesStore], the app-lifetime
 * [CoroutineScope] and the single [SettingsRepository]) are created ONCE via
 * [remember] and shared across all three ViewModels. In particular
 * [SettingsViewModel] and [ScheduleEditorViewModel] share the same
 * [SettingsRepository] instance as [ScheduleViewModel], so edits made in the
 * editor propagate through the repository snapshot to the Daily/Calendar views.
 * (Requirement 6.1, 6.2, 8.1, 8.11, 8.12)
 *
 * A real system [Clock] is injected into [ScheduleViewModel] so "today"
 * reflects the device date in production while remaining swappable for
 * deterministic tests. (Requirement 6.2)
 *
 * `initialize()` and `initToToday()` each run exactly once on first composition
 * via keyless [LaunchedEffect]s. (Requirement 3.1, 6.2, 8.12)
 */
@Composable
private fun RasporedApp(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as Application

    // Shared dependency graph, created once. (Req 6.1, 8.1, 8.11, 8.12)
    val repository = remember { ConstantTimetableRepository() }
    val service = remember { ScheduleService(repository) }
    val store = remember { SchedulePreferencesStore(app.scheduleDataStore) }
    val appScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    val settingsRepository = remember {
        SettingsRepository(store, repository::updateSnapshot, appScope)
    }

    // Load persisted state once on start. (Req 6.2, 8.12)
    LaunchedEffect(Unit) {
        settingsRepository.initialize()
    }

    // Three ViewModels sharing the single SettingsRepository instance above.
    val scheduleViewModel: ScheduleViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ScheduleViewModel(service, repository, settingsRepository, Clock.systemDefaultZone())
            }
        }
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SettingsViewModel(settingsRepository) }
        }
    )
    val editorViewModel: ScheduleEditorViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ScheduleEditorViewModel(settingsRepository) }
        }
    )

    val navController = rememberNavController()

    // Set the Selected_Date to today once, on start. (Req 3.1, 6.2)
    LaunchedEffect(Unit) {
        scheduleViewModel.initToToday()
    }

    val state by scheduleViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Routes.DAILY,
        modifier = modifier
    ) {
        composable(Routes.DAILY) {
            DailyScreen(
                state = state,
                onPreviousDay = scheduleViewModel::previousDay,
                onNextDay = scheduleViewModel::nextDay,
                onOpenCalendar = { navController.navigate(Routes.CALENDAR) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenEditor = { navController.navigate(Routes.EDITOR) }
            )
        }
        composable(Routes.CALENDAR) {
            CalendarScreen(
                selectedDate = state.date,
                onDateSelected = { date ->
                    // Update the shared state, then return to the Daily view. (Req 4.2)
                    scheduleViewModel.selectDate(date)
                    navController.popBackStack()
                },
                onDismiss = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            val settingsState by settingsViewModel.uiState.collectAsState()
            SettingsScreen(
                state = settingsState,
                onOwnerTextChanged = settingsViewModel::onOwnerTextChanged,
                onSave = settingsViewModel::saveOwner,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.EDITOR) {
            val editorState by editorViewModel.uiState.collectAsState()
            ScheduleEditorScreen(
                state = editorState,
                onClassCodeChanged = editorViewModel::onClassCodeChanged,
                onGroupToggled = editorViewModel::onGroupToggled,
                onSave = editorViewModel::save,
                onRevert = editorViewModel::revert,
                onBack = { navController.popBackStack() },
                onConsumeNotice = editorViewModel::consumeNotice
            )
        }
    }
}
