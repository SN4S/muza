package com.sn4s.muza.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sn4s.muza.data.repository.MusicRepository
import com.sn4s.muza.di.NetworkModule
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
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadAlbum(albumId) }) {
                        Text("Retry")
                    }
                }
            }
            album != null -> {
                AlbumContent(
                    album = album!!,
                    songs = albumSongs,
                    onArtistClick = {
                        navController.navigate("artist/${album!!.creator.id}")
                    },
                    onPlayAll = {
                        if (albumSongs.isNotEmpty()) {
                            playerController.playFromCollectionStartingAt(albumSongs, albumSongs.first())
                        }
                    },
                    onShufflePlay = {
                        if (albumSongs.isNotEmpty()) {
                            playerController.playShuffledFromCollection(albumSongs, albumSongs.random())
                        }
                    },
                    playerController = playerController,
                )
            }
        }
    }
}

@Composable
private fun AlbumContent(
    album: com.sn4s.muza.data.model.Album,
    songs: List<com.sn4s.muza.data.model.Song>,
    onArtistClick: () -> Unit,
    onPlayAll: () -> Unit,
    onShufflePlay: () -> Unit,
    playerController: PlayerController
) {
    val context = LocalContext.current
    val coverUrl = album.coverImage?.let {
        "${NetworkModule.BASE_URL}albums/${album.id}/cover"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Album Header
        item {
            AlbumHeader(
                album = album,
                songs = songs,
                coverUrl = coverUrl,
                onArtistClick = onArtistClick,
                context = context
            )
        }

        // Play Controls
        item {
            AlbumPlayControls(
                onPlayAll = onPlayAll,
                onShufflePlay = onShufflePlay,
                songsCount = songs.size
            )
        }

        // Songs List
        if (songs.isNotEmpty()) {
            item {
                Text(
                    text = "Songs",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            itemsIndexed(songs) { index, song ->
                USongItem(
                    song = song,
                    playerController = playerController,
                    playbackMode = PlaybackMode.FROM_COLLECTION,
                    collectionSongs = songs,
                    showMoreOptions = true
                )
            }
        } else {
            item {
                EmptyAlbumState()
            }
        }
    }
}

@Composable
private fun AlbumHeader(
    album: com.sn4s.muza.data.model.Album,
    songs: List<com.sn4s.muza.data.model.Song>,
    coverUrl: String?,
    onArtistClick: () -> Unit,
    context: android.content.Context
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Album Cover
        Card(
            modifier = Modifier.size(160.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            if (coverUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(coverUrl)
                        .crossfade(true)
                        .build(),
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

            val formattedDate = remember(album.releaseDate) {
                try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    val date = inputFormat.parse(album.releaseDate)
                    outputFormat.format(date ?: Date())
                } catch (e: Exception) {
                    album.releaseDate.substringBefore('T')
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
                val hours = totalDuration / 3600
                val minutes = (totalDuration % 3600) / 60
                val durationText = if (hours > 0) {
                    "${hours}h ${minutes}m"
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
}

@Composable
private fun AlbumPlayControls(
    onPlayAll: () -> Unit,
    onShufflePlay: () -> Unit,
    songsCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Play All Button
        Button(
            onClick = onPlayAll,
            modifier = Modifier.weight(1f),
            enabled = songsCount > 0
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Play All")
        }

        // Shuffle Button
        OutlinedButton(
            onClick = onShufflePlay,
            modifier = Modifier.weight(1f),
            enabled = songsCount > 0
        ) {
            Icon(
                Icons.Default.Shuffle,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Shuffle")
        }
    }
}

@Composable
private fun EmptyAlbumState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "This album is empty",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Songs will appear here once they're added to the album",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}