package com.sn4s.muza.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sn4s.muza.ui.viewmodels.ProfileViewModel

@Composable
fun BottomNavBar(
    navController: NavController,
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val user by profileViewModel.user.collectAsState()
    val isArtist = user?.isArtist ?: false

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = currentRoute == "home",
            onClick = {
                navController.navigate("home") {
                    popUpTo("home") { inclusive = false }
                    launchSingleTop = true
                }
            }
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            label = { Text("Search") },
            selected = currentRoute == "search",
            onClick = {
                navController.navigate("search") {
                    popUpTo("home") { inclusive = false }
                    launchSingleTop = true
                }
            }
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
            label = { Text("Library") },
            selected = currentRoute == "library",
            onClick = {
                navController.navigate("library") {
                    popUpTo("home") { inclusive = false }
                    launchSingleTop = true
                }
            }
        )

        if (isArtist) {
            NavigationBarItem(
                icon = { Icon(Icons.Default.Upload, contentDescription = "Upload") },
                label = { Text("Upload") },
                selected = currentRoute == "upload",
                onClick = {
                    navController.navigate("upload") {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            selected = currentRoute == "profile",
            onClick = {
                navController.navigate("profile") {
                    popUpTo("home") { inclusive = false }
                    launchSingleTop = true
                }
            }
        )
    }
}