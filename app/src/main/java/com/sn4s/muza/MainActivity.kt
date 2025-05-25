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
import com.sn4s.muza.ui.screens.*
import com.sn4s.muza.ui.theme.MuzaTheme
import com.sn4s.muza.ui.viewmodels.AuthViewModel
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
    val isAuthenticated by authViewModel.isAuthenticated.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val hideBottomBarRoutes = listOf("login", "register")
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(isAuthenticated) {
        Log.d("MainScreen", "Authentication state changed: $isAuthenticated")
    }

    // Handle unauthorized events
    LaunchedEffect(Unit) {
        NetworkModule.unauthorizedEvent.collect {
            Log.d("MainScreen", "Received unauthorized event, navigating to login")
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }
        }
    }
    
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
            
            composable("home") { HomeScreen() }
            composable("search") { SearchScreen() }
            composable("library") { LibraryScreen() }
            composable("profile") { ProfileScreen(navController) }
        }
    }
}