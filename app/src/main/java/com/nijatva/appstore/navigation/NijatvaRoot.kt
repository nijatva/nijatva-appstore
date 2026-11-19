package com.nijatva.appstore.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nijatva.appstore.core.theme.ThemeState
import com.nijatva.appstore.data.SettingsRepository
import com.nijatva.appstore.data.StoreViewModel
import com.nijatva.appstore.feature.search.SearchScreen
import com.nijatva.appstore.feature.settings.AboutScreen
import com.nijatva.appstore.feature.settings.SettingsScreen
import com.nijatva.appstore.feature.store.AppDetailsScreen
import com.nijatva.appstore.feature.store.StoreScreen
import com.nijatva.appstore.feature.updates.UpdatesScreen
import com.nijatva.signature.Signature

object Routes {
    const val STORE = "store"
    const val SEARCH = "search"
    const val UPDATES = "updates"
    const val SETTINGS = "settings"
    const val APP_DETAILS = "app/{appId}"
    const val ABOUT = "about"

    fun appDetails(id: String) = "app/$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.STORE, "Store", Icons.Filled.Home),
    Tab(Routes.SEARCH, "Search", Icons.Filled.Search),
    Tab(Routes.UPDATES, "Updates", Icons.Filled.Refresh),
    Tab(Routes.SETTINGS, "Settings", Icons.Filled.Settings),
)

@Composable
fun NijatvaRoot(settings: SettingsRepository, themeState: ThemeState) {
    val navController: NavHostController = rememberNavController()
    val viewModel: StoreViewModel = viewModel()
    val context = LocalContext.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = tabs.any { it.route == currentRoute }

    // Appearance and Developer come from the signature library, identical to every other
    // Nijatva app, so they are launched rather than drawn here.
    val openAppearance: () -> Unit = { context.startActivity(Signature.appearance(context)) }
    val openDeveloper: () -> Unit = { context.startActivity(Signature.developer(context)) }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        popUpTo(Routes.STORE) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.STORE,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.STORE) {
                StoreScreen(
                    viewModel = viewModel,
                    onOpenApp = { navController.navigate(Routes.appDetails(it)) },
                    onOpenDeveloper = openDeveloper,
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    viewModel = viewModel,
                    settings = settings,
                    onOpenApp = { navController.navigate(Routes.appDetails(it)) },
                )
            }
            composable(Routes.UPDATES) {
                UpdatesScreen(
                    viewModel = viewModel,
                    settings = settings,
                    onOpenApp = { navController.navigate(Routes.appDetails(it)) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    settings = settings,
                    themeState = themeState,
                    onAppearance = openAppearance,
                    onDeveloper = openDeveloper,
                    onAbout = { navController.navigate(Routes.ABOUT) },
                )
            }
            composable(
                route = Routes.APP_DETAILS,
                arguments = listOf(navArgument("appId") { type = NavType.StringType }),
            ) { entry ->
                AppDetailsScreen(
                    viewModel = viewModel,
                    appId = entry.arguments?.getString("appId"),
                    onBack = { navController.popBackStack() },
                    onOpenDeveloper = openDeveloper,
                )
            }
            composable(Routes.ABOUT) {
                AboutScreen(
                    developer = viewModel.developer.collectAsStateWithLifecycle().value,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
