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
import com.sn4s.muza.di.NetworkModule
import com.sn4s.muza.ui.components.BottomNavBar
import com.sn4s.muza.ui.components.MiniPlayer
import com.sn4s.muza.ui.screens.*
import com.sn4s.muza.ui.theme.MuzaTheme
import com.sn4s.muza.ui.viewmodels.AuthViewModel
import com.sn4s.muza.ui.viewmodels.PlayerViewModel
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
    val playerViewModel: PlayerViewModel = hiltViewModel()

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

            composable("playlist/{playlistId}") { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getString("playlistId")?.toIntOrNull() ?: 0
                PlaylistDetailScreenWithPlayer(navController, playlistId, playerViewModel)
            }

            composable("home") {
                HomeScreenWithPlayer(navController, playerViewModel)
            }

            composable("search") {
                SearchScreenWithPlayer(navController, playerViewModel)
            }

            composable("library") {
                LibraryScreenWithPlayer(navController, playerViewModel)
            }

            composable("upload") {
                UploadScreen(navController)
            }

            composable("profile") {
                ProfileScreen(navController)
            }

            composable("player") {
                FullPlayerScreen(
                    navController = navController,
                    playerViewModel = playerViewModel
                )
            }
        }
    }
}

@Composable
fun HomeScreenWithPlayer(
    navController: androidx.navigation.NavController,
    playerViewModel: PlayerViewModel
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
fun PlaylistDetailScreenWithPlayer(
    navController: androidx.navigation.NavController,
    playlistId: Int,
    playerViewModel: PlayerViewModel
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
    playerViewModel: PlayerViewModel
) {
    androidx.compose.foundation.layout.Column {
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier.weight(1f)
        ) {
            SearchScreen(navController, playerViewModel = playerViewModel)
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
    playerViewModel: PlayerViewModel
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