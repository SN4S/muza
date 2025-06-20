package com.sn4s.muza

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sn4s.muza.ui.components.BottomNavBar
import com.sn4s.muza.ui.components.MiniPlayer
import com.sn4s.muza.ui.screens.*
import com.sn4s.muza.ui.theme.MuzaTheme
import com.sn4s.muza.ui.viewmodels.AuthViewModel
import com.sn4s.muza.ui.viewmodels.PlayerController
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MuzaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val playerViewModel: PlayerController = hiltViewModel()

    val isAuthenticated by authViewModel.isAuthenticated.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentSong by playerViewModel.currentSong.collectAsState()

    val hideBottomBarRoutes = listOf("login", "register")
    val hidePlayerRoutes = listOf("login", "register", "player")

    Scaffold(
        bottomBar = {
            if (currentRoute !in hideBottomBarRoutes) {
                BottomNavBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isAuthenticated) "home" else "login",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                LoginScreen(
                    onNavigateToRegister = { navController.navigate("register") },
                    onLoginSuccess = {
                        Log.d("MainScreen", "Login success, navigating to home")
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }

            composable("register") {
                RegisterScreen(
                    onNavigateToLogin = { navController.navigate("login") },
                    onRegisterSuccess = {
                        Log.d("MainScreen", "Register success, navigating to home")
                        navController.navigate("home") {
                            popUpTo("register") { inclusive = true }
                        }
                    }
                )
            }

            composable("recently_played") {
                RecentlyPlayedScreenWithPlayer(navController)
            }

            composable("playlist/{playlistId}") { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getString("playlistId")?.toIntOrNull() ?: 0
                PlaylistDetailScreenWithPlayer(navController, playlistId)
            }

            composable("album/{albumId}") { backStackEntry ->
                val albumId = backStackEntry.arguments?.getString("albumId")?.toIntOrNull() ?: 0
                AlbumDetailScreenWithPlayer(navController, albumId)
            }

            composable("artist/{artistId}") { backStackEntry ->
                val artistId = backStackEntry.arguments?.getString("artistId")?.toIntOrNull() ?: 0
                ArtistProfileScreenWithPlayer(navController, artistId)
            }

            composable("home") {
                HomeScreenWithPlayer(navController)
            }

            composable("search") {
                SearchScreenWithPlayer(navController)
            }

            composable("library") {
                LibraryScreenWithPlayer(navController)
            }

            composable("artist") {
                ArtistScreenWithPlayer(navController)
            }

            composable("profile") {
                ProfileScreen(navController)
            }

            composable("player") {
                FullPlayerScreen(
                    navController = navController
                )
            }

            composable("queue") {
                QueueScreenWithPlayer(navController)
            }

            composable("liked_songs") {
                LikedSongsScreenWithPlayer(navController)
            }

        }
    }
}

@Composable
fun HomeScreenWithPlayer(
    navController: androidx.navigation.NavController,
    playerViewModel: PlayerController = hiltViewModel()
) {
    androidx.compose.foundation.layout.Column {
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier.weight(1f)
        ) {
            HomeScreen(navController, playerViewModel = playerViewModel)
        }
        MiniPlayer(
            onClick = { navController.navigate("player") },
            viewModel = playerViewModel
        )
    }
}

@Composable
fun RecentlyPlayedScreenWithPlayer(
    navController: androidx.navigation.NavController,
    playerViewModel: PlayerController = hiltViewModel()
) {
    androidx.compose.foundation.layout.Column {
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier.weight(1f)
        ) {
            RecentlyPlayedScreen(navController, playerViewModel)
        }
        MiniPlayer(
            onClick = { navController.navigate("player") },
            viewModel = playerViewModel
        )
    }
}

@Composable
fun QueueScreenWithPlayer(
    navController: androidx.navigation.NavController,
    playerViewModel: PlayerController = hiltViewModel()
) {
    androidx.compose.foundation.layout.Column {
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier.weight(1f)
        ) {
            QueueScreen(navController)
        }
        MiniPlayer(
            onClick = { navController.navigate("player") },
            viewModel = playerViewModel
        )
    }
}

@Composable
fun LikedSongsScreenWithPlayer(
    navController: androidx.navigation.NavController,
    playerViewModel: PlayerController = hiltViewModel()
) {
    androidx.compose.foundation.layout.Column {
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier.weight(1f)
        ) {
            LikedSongsScreen(navController, playerViewModel)
        }
        MiniPlayer(
            onClick = { navController.navigate("player") },
            viewModel = playerViewModel
        )
    }
}

@Composable
fun ArtistProfileScreenWithPlayer(
    navController: androidx.navigation.NavController,
    artistId: Int,
    playerViewModel: PlayerController = hiltViewModel()
) {
    androidx.compose.foundation.layout.Column {
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier.weight(1f)
        ) {
            ArtistProfileScreen(navController, artistId, playerViewModel)
        }
        MiniPlayer(
            onClick = { navController.navigate("player") },
            viewModel = playerViewModel
        )
    }
}

@Composable
fun AlbumDetailScreenWithPlayer(
    navController: androidx.navigation.NavController,
    albumId: Int,
    playerViewModel: PlayerController = hiltViewModel()
) {
    androidx.compose.foundation.layout.Column {
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier.weight(1f)
        ) {
            AlbumDetailScreen(navController, albumId, playerViewModel)
        }
        MiniPlayer(
            onClick = { navController.navigate("player") },
            viewModel = playerViewModel
        )
    }
}

@Composable
fun PlaylistDetailScreenWithPlayer(
    navController: androidx.navigation.NavController,
    playlistId: Int,
    playerViewModel: PlayerController = hiltViewModel()
) {
    androidx.compose.foundation.layout.Column {
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier.weight(1f)
        ) {
            PlaylistDetailScreen(navController, playlistId, playerViewModel)
        }
        MiniPlayer(
            onClick = { navController.navigate("player") },
            viewModel = playerViewModel
        )
    }
}

@Composable
fun SearchScreenWithPlayer(
    navController: androidx.navigation.NavController,
    playerViewModel: PlayerController = hiltViewModel()
) {
    androidx.compose.foundation.layout.Column {
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier.weight(1f)
        ) {
            SearchScreen(navController)
        }
        MiniPlayer(
            onClick = { navController.navigate("player") },
            viewModel = playerViewModel
        )
    }
}

@Composable
fun LibraryScreenWithPlayer(
    navController: androidx.navigation.NavController,
    playerViewModel: PlayerController = hiltViewModel()
) {
    androidx.compose.foundation.layout.Column {
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier.weight(1f)
        ) {
            LibraryScreen(navController, playerViewModel = playerViewModel)
        }
        MiniPlayer(
            onClick = { navController.navigate("player") },
            viewModel = playerViewModel
        )
    }
}

@Composable
fun ArtistScreenWithPlayer(
    navController: androidx.navigation.NavController,
    playerViewModel: PlayerController = hiltViewModel()
) {
    androidx.compose.foundation.layout.Column {
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier.weight(1f)
        ) {
            ArtistScreen(navController)
        }
        MiniPlayer(
            onClick = { navController.navigate("player") },
            viewModel = playerViewModel
        )
    }
}