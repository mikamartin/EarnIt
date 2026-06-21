@file:OptIn(ExperimentalMaterial3Api::class)

package com.earnit.app.ui

import android.app.Activity
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.earnit.app.ui.theme.EarnItTheme
import com.earnit.app.viewmodel.EarnItViewModel
import com.google.android.play.core.review.ReviewManagerFactory

// ── Navigation ────────────────────────────────────────────────────────────────

sealed class Screen(
    val route: String,
) {
    object Home : Screen("home")

    object RewardDetail : Screen("reward_detail/{rewardId}") {
        fun route(rewardId: Long) = "reward_detail/$rewardId"
    }

    object RewardEdit : Screen("reward_edit/{rewardId}") {
        fun route(rewardId: Long = 0L) = "reward_edit/$rewardId"
    }

    object Tasks : Screen("tasks")

    object TaskDetail : Screen("task_detail/{taskId}") {
        fun route(taskId: Long) = "task_detail/$taskId"
    }

    object TaskEdit : Screen("task_edit/{taskId}/{fromRewardId}?fromRewardName={fromRewardName}") {
        fun route(
            taskId: Long = 0L,
            fromRewardId: Long = 0L,
            fromRewardName: String = "",
        ) = if (fromRewardName.isBlank()) {
            "task_edit/$taskId/$fromRewardId"
        } else {
            "task_edit/$taskId/$fromRewardId?fromRewardName=${android.net.Uri.encode(fromRewardName)}"
        }
    }

    object History : Screen("history")

    object Settings : Screen("settings")

    object About : Screen("about")

    object SettingsData : Screen("settings_data")

    object SettingsCleanUp : Screen("settings_cleanup")

    object TaskLibrary : Screen("task_library")
}

private data class NavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector? = null,
    val emoji: String? = null,
    val unselectedColor: Color = Color.Unspecified,
)

private val navItems =
    listOf(
        NavItem(Screen.Home, "Prizes", Icons.Default.EmojiEvents, unselectedColor = Color(0xFFE8A000)),
        NavItem(Screen.Tasks, "Tasks", emoji = "⚔️", unselectedColor = Color(0xFF2A9D8F)),
        NavItem(Screen.History, "History", Icons.Default.History, unselectedColor = Color(0xFF2A9D8F)),
        NavItem(Screen.Settings, "Settings", Icons.Default.Settings, unselectedColor = Color(0xFF8E7CC3)),
    )

private fun routeToTab(route: String?): String =
    when {
        route == null -> Screen.Home.route
        route.startsWith("reward_") || route == Screen.Home.route -> Screen.Home.route
        route.startsWith("task_") ||
            route == Screen.Tasks.route ||
            route == Screen.TaskLibrary.route -> Screen.Tasks.route
        route == Screen.History.route -> Screen.History.route
        route == Screen.Settings.route ||
            route == Screen.About.route ||
            route == Screen.SettingsData.route ||
            route == Screen.SettingsCleanUp.route -> Screen.Settings.route
        else -> Screen.Home.route
    }

@Composable
fun EarnItApp(
    viewModel: EarnItViewModel = hiltViewModel(),
    startRewardId: Long = 0L,
) {
    val settings by viewModel.settings.collectAsState()
    EarnItTheme(colorScheme = settings.colorScheme) {
        val uiState by viewModel.uiState.collectAsState()
        val hasNewMascot by viewModel.hasNewMascot.collectAsState()
        val navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }
        val context = LocalContext.current
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        LaunchedEffect(startRewardId) {
            if (startRewardId != 0L) {
                navController.navigate(Screen.RewardDetail.route(startRewardId))
            }
        }

        LaunchedEffect(Unit) {
            viewModel.triggerInAppReview.collect {
                val activity = context as? Activity ?: return@collect
                val manager = ReviewManagerFactory.create(context)
                manager.requestReviewFlow().addOnCompleteListener { task ->
                    if (task.isSuccessful) manager.launchReviewFlow(activity, task.result)
                }
            }
        }

        LaunchedEffect(currentRoute) {
            if (navItems.any { it.screen.route == currentRoute }) {
                snackbarHostState.currentSnackbarData?.dismiss()
            }
            if (routeToTab(currentRoute) == Screen.Settings.route) {
                viewModel.clearNewMascotBadge()
            }
        }

        LaunchedEffect(Unit) {
            viewModel.newlyUnlockedMascot.collect { mascotId ->
                val name =
                    com.earnit.app.data.Mascots.all
                        .find { it.id == mascotId }
                        ?.displayName ?: return@collect
                val result =
                    snackbarHostState.showSnackbar(
                        message = Strings.mascotUnlocked(name),
                        actionLabel = Strings.MASCOT_UNLOCK_ACTION,
                    )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.triggerMascotPickerFor(mascotId)
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(Screen.Home.route) { saveState = false }
                        launchSingleTop = true
                    }
                }
            }
        }

        Scaffold(
            topBar = {},
            bottomBar = { EarnItBottomBar(navController, hasNewMascot) },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(padding),
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(uiState, viewModel, navController)
                }
                composable(
                    Screen.RewardDetail.route,
                    arguments = listOf(navArgument("rewardId") { type = NavType.LongType }),
                ) { back ->
                    val id = back.arguments?.getLong("rewardId") ?: 0L
                    val rp = uiState.rewardProgressList.find { it.reward.id == id }
                    if (rp != null) RewardDetailScreen(rp, uiState, viewModel, navController)
                }
                composable(
                    Screen.RewardEdit.route,
                    arguments = listOf(navArgument("rewardId") { type = NavType.LongType }),
                ) { back ->
                    val id = back.arguments?.getLong("rewardId") ?: 0L
                    RewardEditScreen(id, uiState, viewModel, navController, snackbarHostState)
                }
                composable(Screen.Tasks.route) {
                    TasksScreen(uiState, settings, viewModel, navController)
                }
                composable(
                    Screen.TaskDetail.route,
                    arguments = listOf(navArgument("taskId") { type = NavType.LongType }),
                ) { back ->
                    val id = back.arguments?.getLong("taskId") ?: 0L
                    TaskDetailScreen(id, uiState, viewModel, navController)
                }
                composable(
                    Screen.TaskEdit.route,
                    arguments =
                        listOf(
                            navArgument("taskId") { type = NavType.LongType },
                            navArgument("fromRewardId") { type = NavType.LongType },
                            navArgument("fromRewardName") {
                                type = NavType.StringType
                                defaultValue = ""
                            },
                        ),
                ) { back ->
                    val taskId = back.arguments?.getLong("taskId") ?: 0L
                    val fromRewardId = back.arguments?.getLong("fromRewardId") ?: 0L
                    val fromRewardName = back.arguments?.getString("fromRewardName") ?: ""
                    TaskEditScreen(taskId, fromRewardId, fromRewardName, uiState, viewModel, navController, snackbarHostState)
                }
                composable(Screen.History.route) { HistoryScreen(uiState, viewModel) }
                composable(Screen.Settings.route) { SettingsScreen(viewModel, settings, navController) }
                composable(Screen.About.route) { AboutScreen(navController) }
                composable(Screen.SettingsData.route) { DataScreen(viewModel, navController) }
                composable(Screen.SettingsCleanUp.route) { CleanUpScreen(viewModel, navController) }
                composable(Screen.TaskLibrary.route) { TaskLibraryScreen(viewModel, navController) }
            }
        }
    } // end EarnItTheme
}

@Composable
fun EarnItBottomBar(
    navController: NavHostController,
    hasNewMascot: Boolean = false,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val activeTab = routeToTab(navBackStackEntry?.destination?.route)
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        navItems.forEach { item ->
            val selected = activeTab == item.screen.route
            NavigationBarItem(
                icon = {
                    if (item.emoji != null) {
                        // contentDescription makes this findable via onNodeWithContentDescription in tests.
                        Text(item.emoji, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { contentDescription = item.label })
                    } else {
                        BadgedBox(badge = {
                            if (item.screen == Screen.Settings && hasNewMascot) {
                                Badge { Text(Strings.MASCOT_SETTINGS_BADGE) }
                            }
                        }) {
                            Icon(item.icon!!, contentDescription = item.label)
                        }
                    }
                },
                selected = selected,
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = item.unselectedColor,
                        unselectedTextColor = item.unselectedColor,
                    ),
                onClick = {
                    navController.navigate(item.screen.route) {
                        popUpTo(Screen.Home.route) { saveState = false }
                        launchSingleTop = true
                        restoreState = false
                    }
                },
            )
        }
    }
}
