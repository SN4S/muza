package com.sn4s.muza.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sn4s.muza.di.NetworkModule
import com.sn4s.muza.ui.viewmodels.HomeViewModel
import com.sn4s.muza.ui.components.USongItem
import com.sn4s.muza.ui.viewmodels.LikeViewModel
import com.sn4s.muza.ui.viewmodels.PlayerController

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: PlayerController = hiltViewModel(),
    likeViewModel: LikeViewModel = hiltViewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    LaunchedEffect(Unit) {
        NetworkModule.unauthorizedEvent.collect {
            if (currentRoute != "login") {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    val songs by viewModel.songs.collectAsState()
    val albums by viewModel.albums.collectAsState()

    LaunchedEffect(songs) {
        if (songs.isNotEmpty()) {
            likeViewModel.checkMultipleLikes(songs.map { it.id })
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Recently Played",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(songs) { song ->
            USongItem(
                song = song,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Your Albums",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(albums) { album ->
                    Card(
                        modifier = Modifier
                            .width(160.dp)
                            .padding(vertical = 4.dp)
                            .clickable {
                                navController.navigate("album/${album.id}")
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = album.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = album.creator.username,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${album.songs.size} songs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}