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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.sn4s.muza.ui.components.PlaybackMode
import com.sn4s.muza.ui.components.USongItem
import com.sn4s.muza.ui.viewmodels.AlbumDetailViewModel
import com.sn4s.muza.ui.viewmodels.PlayerController
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    navController: NavController,
    albumId: Int,
    playerController: PlayerController = hiltViewModel(),
    viewModel: AlbumDetailViewModel = hiltViewModel()
) {
    val album by viewModel.album.collectAsStateWithLifecycle()
    val albumSongs by viewModel.albumSongs.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    LaunchedEffect(albumId) {
        viewModel.loadAlbum(albumId)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top App Bar
        TopAppBar(
            title = { Text("Album") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (album != null) {
                    IconButton(onClick = { /* TODO: Share album */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            }
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
                    Button(onClick = { viewModel.loadAlbum(albumId) }) {
                        Text("Retry")
                    }
                }
            }
            album != null -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // Album Header
                    item {
                        AlbumHeader(
                            album = album!!,
                            songs = albumSongs,
                            onPlayAll = {
                                if (albumSongs.isNotEmpty()) {
                                    playerController.playPlaylist(albumSongs)
                                }
                            },
                            onShuffle = {
                                if (albumSongs.isNotEmpty()) {
                                    playerController.playShuffled(albumSongs)
                                }
                            },
                            onArtistClick = {
                                navController.navigate("artist/${album!!.creator.id}")
                            }
                        )
                    }

                    // Songs List
                    if (albumSongs.isEmpty()) {
                        item {
                            EmptyAlbumState()
                        }
                    } else {
                        itemsIndexed(albumSongs) { index, song ->
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(32.dp),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(modifier = Modifier.weight(1f)) {
                                        USongItem(
                                            song = song,
                                            collectionSongs = albumSongs,
                                            playbackMode = PlaybackMode.FROM_COLLECTION,
                                            showMoreOptions = true
                                        )
                                    }
                                }
                                if (index < albumSongs.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 48.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumHeader(
    album: com.sn4s.muza.data.model.Album,
    songs: List<com.sn4s.muza.data.model.Song>,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onArtistClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Album Cover
            Card(
                modifier = Modifier.size(120.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                if (album.coverImage != null) {
                    AsyncImage(
                        model = album.coverImage,
                        contentDescription = "Album cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Album,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Album Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.headlineSmall
                )

                TextButton(
                    onClick = onArtistClick,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = album.creator.username,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                val formattedDate = remember(album!!.releaseDate) {
                    try {
                        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        val outputFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                        val date = inputFormat.parse(album!!.releaseDate)
                        outputFormat.format(date ?: Date())
                    } catch (e: Exception) {
                        album!!.releaseDate.substringBefore('T')
                    }
                }
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "${songs.size} songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Calculate total duration
                val totalDuration = songs.sumOf { it.duration ?: 0 }
                if (totalDuration > 0) {
                    val minutes = totalDuration / 60
                    val hours = minutes / 60
                    val durationText = if (hours > 0) {
                        "${hours}h ${minutes % 60}m"
                    } else {
                        "${minutes}m"
                    }
                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (songs.isNotEmpty()) {
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
private fun EmptyAlbumState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Album,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No songs in this album",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "This album doesn't contain any songs yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}