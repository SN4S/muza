package com.sn4s.muza.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.sn4s.muza.data.model.Song
import com.sn4s.muza.ui.components.PlaybackMode
import com.sn4s.muza.ui.components.USongItem
import com.sn4s.muza.ui.viewmodels.PlayerController
import com.sn4s.muza.ui.viewmodels.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LikedSongsScreen(
    navController: NavController,
    playerController: PlayerController = hiltViewModel(),
    libraryViewModel: LibraryViewModel = hiltViewModel()
) {
    val likedSongs by libraryViewModel.likedSongs.collectAsStateWithLifecycle()
    val isLoading by libraryViewModel.isLoading.collectAsStateWithLifecycle()
    val error by libraryViewModel.error.collectAsStateWithLifecycle()

    var sortBy by remember { mutableStateOf(LikedSongsSortOption.RECENTLY_ADDED) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Sort songs based on selected option
    val sortedSongs = remember(likedSongs, sortBy) {
        when (sortBy) {
            LikedSongsSortOption.RECENTLY_ADDED -> likedSongs.sortedByDescending { it.id }
            LikedSongsSortOption.ALPHABETICAL -> likedSongs.sortedBy { it.title }
            LikedSongsSortOption.ARTIST -> likedSongs.sortedBy { it.creator.username }
        }
    }

    // Load liked songs when screen opens
    LaunchedEffect(Unit) {
        libraryViewModel.loadLikedSongs()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Liked Songs") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort")
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        LikedSongsSortOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.displayName,
                                        color = if (sortBy == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    sortBy = option
                                    showSortMenu = false
                                },
                                leadingIcon = if (sortBy == option) {
                                    { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                } else null
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { libraryViewModel.loadLikedSongs() }) {
                        Text("Retry")
                    }
                }
            }
            else -> {
                LazyColumn {
                    // Header Section
                    item {
                        LikedSongsHeader(
                            songCount = likedSongs.size,
                            onPlayAll = {
                                if (sortedSongs.isNotEmpty()) {
                                    playerController.playPlaylist(sortedSongs)
                                }
                            },
                            onShuffle = {
                                if (sortedSongs.isNotEmpty()) {
                                    playerController.playShuffled(sortedSongs)
                                }
                            }
                        )
                    }

                    // Songs List
                    if (sortedSongs.isEmpty()) {
                        item {
                            LikedSongsEmptyState()
                        }
                    } else {
                        itemsIndexed(sortedSongs) { index, song ->
                            IndexedSongItem(
                                index = index + 1,
                                song = song,
                                collectionSongs = sortedSongs,
                                playbackMode = PlaybackMode.FROM_COLLECTION
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LikedSongsHeader(
    songCount: Int,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Icon(
            Icons.Default.Favorite,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Liked Songs",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = "$songCount songs",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (songCount > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPlayAll) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play All")
                }
                OutlinedButton(onClick = onShuffle) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Shuffle")
                }
            }
        }
    }
}

@Composable
private fun IndexedSongItem(
    index: Int,
    song: Song,
    collectionSongs: List<Song>,
    playbackMode: PlaybackMode,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Index number
        Text(
            text = index.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Song item
        Box(modifier = Modifier.weight(1f)) {
            USongItem(
                song = song,
                collectionSongs = collectionSongs,
                playbackMode = playbackMode
            )
        }
    }
}

@Composable
private fun LikedSongsEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.FavoriteBorder,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No liked songs yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Songs you like will appear here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Find music you love by searching or browsing",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

enum class LikedSongsSortOption(val displayName: String) {
    RECENTLY_ADDED("Recently Added"),
    ALPHABETICAL("Title A-Z"),
    ARTIST("Artist A-Z")
}