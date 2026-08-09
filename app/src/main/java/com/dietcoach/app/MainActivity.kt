package com.dietcoach.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dietcoach.app.ui.AppViewModel
import com.dietcoach.app.ui.calendar.CalendarScreen
import com.dietcoach.app.ui.calendar.DayDetailScreen
import com.dietcoach.app.ui.chat.ChatScreen
import com.dietcoach.app.ui.log.LogScreen
import com.dietcoach.app.ui.profile.MetricsGuideScreen
import com.dietcoach.app.ui.profile.ProfileScreen
import com.dietcoach.app.ui.strength.StrengthScreen
import com.dietcoach.app.ui.theme.DietCoachTheme
import com.dietcoach.app.ui.today.TodayScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DietCoachTheme {
                val vm: AppViewModel = viewModel(factory = AppViewModel.factory(application))
                DietCoachRoot(vm)
            }
        }
    }
}

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Today("today", "今日", Icons.Default.Today),
    Log("log", "记录", Icons.Default.EditNote),
    Calendar("calendar", "日历", Icons.Default.CalendarMonth),
    Strength("strength", "力量", Icons.Default.FitnessCenter),
    Chat("chat", "助手", Icons.Default.Chat),
    Me("me", "我的", Icons.Default.Person)
}

@Composable
private fun DietCoachRoot(vm: AppViewModel) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val chat by vm.chatMessages.collectAsStateWithLifecycle()
    val streamingAssistant by vm.streamingAssistantText.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute?.startsWith("day") != true && currentRoute != "me/guide"

    LaunchedEffect(state.banner) {
        state.banner?.let {
            snackbarHostState.showSnackbar(it.message)
            vm.clearBanner()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            alwaysShowLabel = false
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = Tab.Today.route
            ) {
                composable(Tab.Today.route) {
                    TodayScreen(
                        state = state,
                        onPrevDay = { vm.shiftDay(-1) },
                        onNextDay = { vm.shiftDay(1) },
                        onToday = { vm.goToday() }
                    )
                }
                composable(Tab.Log.route) {
                    LogScreen(
                        state = state,
                        onParseAi = vm::parseFoodNlp,
                        onParsePhoto = vm::parseFoodPhoto,
                        onConfirmAi = vm::confirmAiFoods,
                        onDismissAi = vm::dismissAiPreview,
                        onAddFood = vm::addManualFood,
                        onDeleteFood = vm::deleteFood,
                        onAddWorkout = vm::addWorkout,
                        onDeleteWorkout = vm::deleteWorkout,
                        onExtraBurn = vm::setExtraBurn,
                        onAnalyzeWorkoutAi = vm::analyzeWorkoutAi
                    )
                }
                composable(Tab.Calendar.route) {
                    CalendarScreen(
                        state = state,
                        onOpenDay = { date ->
                            vm.selectDate(date)
                            navController.navigate("day/$date")
                        },
                        onLogWeightToday = { kg ->
                            vm.goToday()
                            vm.logWeight(kg)
                        }
                    )
                }
                composable(Tab.Strength.route) {
                    StrengthScreen(
                        state = state,
                        onAdd = vm::addStrength,
                        onDelete = vm::deleteStrength
                    )
                }
                composable(Tab.Chat.route) {
                    ChatScreen(
                        state = state,
                        chat = chat,
                        streamingAssistant = streamingAssistant,
                        onSend = vm::sendChat,
                        onClear = vm::clearChat
                    )
                }
                composable(Tab.Me.route) {
                    ProfileScreen(
                        state = state,
                        onSaveProfile = vm::saveProfile,
                        onSaveApiKey = vm::saveApiKey,
                        onLogWeight = vm::logWeight,
                        onOpenGuide = { navController.navigate("me/guide") }
                    )
                }
                composable("me/guide") {
                    MetricsGuideScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    route = "day/{date}",
                    arguments = listOf(navArgument("date") { type = NavType.StringType })
                ) { entry ->
                    val date = entry.arguments?.getString("date").orEmpty()
                    LaunchedEffect(date) {
                        if (date.isNotBlank()) vm.selectDate(date)
                    }
                    DayDetailScreen(
                        date = date,
                        state = state,
                        onBack = { navController.popBackStack() },
                        onSaveWeight = { kg -> vm.logWeight(kg, date) }
                    )
                }
            }
        }
    }
}
