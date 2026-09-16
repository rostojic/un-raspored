package com.ostojic.raspored

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
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ostojic.raspored.domain.ConstantTimetableRepository
import com.ostojic.raspored.domain.ScheduleService
import com.ostojic.raspored.presentation.ScheduleViewModel
import com.ostojic.raspored.ui.CalendarScreen
import com.ostojic.raspored.ui.DailyScreen
import com.ostojic.raspored.ui.theme.RasporedTheme
import java.time.Clock

/**
 * Single Activity host for the app.
 *
 * This is the integration point that wires the dependency graph
 * ([ConstantTimetableRepository] → [ScheduleService] → [ScheduleViewModel] with a
 * real system [Clock]) into a Compose navigation graph. The Daily and Calendar
 * destinations share ONE [ScheduleViewModel] scoped to the Activity so date
 * changes made on either screen are reflected on the other. The app is fully
 * offline and requires no account or sign-in. (Requirement 6.1, 6.2)
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
}

/**
 * Factory that builds the dependency graph for [ScheduleViewModel]. A real
 * system [Clock] is injected so "today" reflects the device date in production
 * while remaining swappable for deterministic tests. (Requirement 6.2)
 */
private val scheduleViewModelFactory = viewModelFactory {
    initializer {
        val repository = ConstantTimetableRepository()
        val service = ScheduleService(repository)
        ScheduleViewModel(service, repository, Clock.systemDefaultZone())
    }
}

/**
 * Root composable: owns the single shared [ScheduleViewModel] and the navigation
 * graph. The ViewModel is scoped to the Activity (the default `viewModel()`
 * owner), so both destinations obtain the same instance. (Requirement 6.1)
 *
 * `initToToday()` runs exactly once on first composition via a keyless
 * [LaunchedEffect], setting the Selected_Date to the current system date.
 * (Requirement 3.1, 6.2)
 */
@Composable
private fun RasporedApp(modifier: Modifier = Modifier) {
    val viewModel: ScheduleViewModel = viewModel(factory = scheduleViewModelFactory)
    val navController = rememberNavController()

    // Set the Selected_Date to today once, on start. (Req 3.1, 6.2)
    LaunchedEffect(Unit) {
        viewModel.initToToday()
    }

    val state by viewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Routes.DAILY,
        modifier = modifier
    ) {
        composable(Routes.DAILY) {
            DailyScreen(
                state = state,
                onPreviousDay = viewModel::previousDay,
                onNextDay = viewModel::nextDay,
                onOpenCalendar = { navController.navigate(Routes.CALENDAR) }
            )
        }
        composable(Routes.CALENDAR) {
            CalendarScreen(
                selectedDate = state.date,
                onDateSelected = { date ->
                    // Update the shared state, then return to the Daily view. (Req 4.2)
                    viewModel.selectDate(date)
                    navController.popBackStack()
                },
                onDismiss = { navController.popBackStack() }
            )
        }
    }
}
