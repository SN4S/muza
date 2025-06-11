package com.sn4s.muza.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sn4s.muza.data.model.Song
import com.sn4s.muza.di.NetworkModule
import com.sn4s.muza.ui.components.PlaybackMode
import com.sn4s.muza.ui.components.USongItem
import com.sn4s.muza.ui.viewmodels.HomeViewModel
import com.sn4s.muza.ui.viewmodels.LikeViewModel
import com.sn4s.muza.ui.viewmodels.PlayerController
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
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
    val recentlyPlayed by playerViewModel.recentlyPlayed.collectAsState()

    LaunchedEffect(songs) {
        if (songs.isNotEmpty()) {
            likeViewModel.checkMultipleLikes(songs.map { it.id })
        }
    }

    LaunchedEffect(recentlyPlayed) {
        if (recentlyPlayed.isNotEmpty()) {
            likeViewModel.checkMultipleLikes(recentlyPlayed.map { it.id })
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header with greeting
        item {
            HomeHeader()
        }

        // Quick access buttons
        item {
            QuickAccessSection(
                navController = navController,
                hasRecentlyPlayed = recentlyPlayed.isNotEmpty()
            )
        }

        // Recently played section
        if (recentlyPlayed.isNotEmpty()) {
            item {
                RecentlyPlayedSection(
                    songs = recentlyPlayed.take(5),
                    onSongClick = { song ->
                        playerViewModel.playSong(song)
                    },
                    onSeeAllClick = {
                        navController.navigate("recently_played")
                    }
                )
            }
        }

        // Recently added section
        if (songs.isNotEmpty()) {
            item {
                RecentlyAddedSection(
                    songs = songs.sortedByDescending { it.createdAt }.take(6),
                    onSongClick = { song ->
                        playerViewModel.playSong(song)
                    }
                )
            }
        }

        // Bottom spacing for mini player
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun HomeHeader() {
    val currentHour = remember { LocalDateTime.now().hour }
    val greeting = when (currentHour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
    ) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "What would you like to listen to?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun QuickAccessSection(
    navController: NavController,
    hasRecentlyPlayed: Boolean
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Quick Access",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                QuickAccessCard(
                    title = "Liked Songs",
                    icon = Icons.Default.Favorite,
                    onClick = { navController.navigate("liked_songs") }
                )
            }

            if (hasRecentlyPlayed) {
                item {
                    QuickAccessCard(
                        title = "Recently Played",
                        icon = Icons.Default.History,
                        onClick = { navController.navigate("recently_played") }
                    )
                }
            }

            item {
                QuickAccessCard(
                    title = "Search",
                    icon = Icons.Default.Search,
                    onClick = { navController.navigate("search") }
                )
            }
        }
    }
}

@Composable
private fun QuickAccessCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(80.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecentlyPlayedSection(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onSeeAllClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        SectionHeader(
            title = "Recently Played",
            onSeeAllClick = onSeeAllClick
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            songs.forEach { song ->
                USongItem(
                    song = song,
                    playbackMode = PlaybackMode.FROM_COLLECTION,
                    collectionSongs = songs,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}



@Composable
private fun RecentlyAddedSection(
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Recently Added",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(songs) { song ->
                RecentSongCard(
                    song = song,
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}

@Composable
private fun RecentSongCard(
    song: Song,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val coverUrl = song.coverImage?.let {
        "${NetworkModule.BASE_URL}songs/${song.id}/cover"
    }

    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable { onClick() }
    ) {
        Card(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (coverUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Song cover",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = song.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = song.creator.username,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    onSeeAllClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        TextButton(onClick = onSeeAllClick) {
            Text("See all")
        }
    }
}