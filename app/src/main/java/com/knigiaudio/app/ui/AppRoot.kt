package com.knigiaudio.app.ui

import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.knigiaudio.app.data.BookShort
import com.knigiaudio.app.player.PlayerHub

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Главная", Icons.Rounded.Home),
    Tab("search", "Поиск", Icons.Rounded.Search),
    Tab("library", "Моя полка", Icons.Rounded.Bookmarks),
)

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val player by PlayerHub.state.collectAsStateWithLifecycle()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val onPlayer = route == "player"

    val openBook: (BookShort) -> Unit = { nav.navigate("book/" + Uri.encode(it.url)) }

    Scaffold(
        containerColor = Palette.Bg,
        bottomBar = {
            if (!onPlayer) Column {
                if (player.book != null) {
                    MiniPlayer(player, onOpen = { nav.navigate("player") }, onToggle = PlayerHub::toggle)
                }
                NavigationBar(containerColor = Palette.Surface) {
                    tabs.forEach { tab ->
                        val selected = entry?.destination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, null) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Palette.OnAmber,
                                indicatorColor = Palette.Amber,
                                selectedTextColor = Palette.Amber,
                                unselectedIconColor = Palette.Muted,
                                unselectedTextColor = Palette.Muted,
                            ),
                        )
                    }
                }
            }
        },
    ) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
            composable("home") { HomeScreen(onBook = openBook) }
            composable("search") { SearchScreen(onBook = openBook) }
            composable("library") { LibraryScreen(onBook = openBook) }
            composable(
                "book/{url}",
                arguments = listOf(navArgument("url") { type = NavType.StringType }),
            ) { e ->
                BookScreen(
                    url = e.arguments?.getString("url").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onOpenPlayer = { nav.navigate("player") { launchSingleTop = true } },
                )
            }
            composable(
                "player",
                enterTransition = { slideInVertically { it } + fadeIn() },
                exitTransition = { slideOutVertically { it } + fadeOut() },
                popExitTransition = { slideOutVertically { it } + fadeOut() },
            ) {
                PlayerScreen(onBack = { nav.popBackStack() })
            }
        }
    }
}
