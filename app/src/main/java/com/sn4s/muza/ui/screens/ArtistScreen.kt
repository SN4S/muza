package com.sn4s.muza.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sn4s.muza.data.model.Album
import com.sn4s.muza.data.model.Song
import com.sn4s.muza.di.NetworkModule
import com.sn4s.muza.ui.components.PlaybackMode
import com.sn4s.muza.ui.components.USongItem
import com.sn4s.muza.ui.viewmodels.ArtistViewModel
import com.sn4s.muza.ui.viewmodels.PlayerController
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(
    navController: NavController,
    playerController: PlayerController = hiltViewModel(),
    viewModel: ArtistViewModel = hiltViewModel()
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

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val userSongs by viewModel.userSongs.collectAsStateWithLifecycle()
    val userAlbums by viewModel.userAlbums.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val uploadProgress by viewModel.uploadProgress.collectAsStateWithLifecycle()
    val uploadSuccess by viewModel.uploadSuccess.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        TopAppBar(
            title = { Text("Artist Dashboard") },
            actions = {
                IconButton(onClick = { viewModel.refreshContent() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        )

        // Tab Row
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { viewModel.setSelectedTab(0) },
                text = { Text("Songs") },
                icon = { Icon(Icons.Default.MusicNote, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { viewModel.setSelectedTab(1) },
                text = { Text("Albums") },
                icon = { Icon(Icons.Default.Album, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { viewModel.setSelectedTab(2) },
                text = { Text("Upload") },
                icon = { Icon(Icons.Default.CloudUpload, contentDescription = null) }
            )
        }

        // Content based on selected tab
        when (selectedTab) {
            0 -> SongsTab(
                songs = userSongs,
                isLoading = isLoading,
                error = error,
                playerController = playerController,
                onRefresh = { viewModel.refreshContent() },
                onDeleteSong = { songId -> viewModel.deleteSong(songId) }
            )
            1 -> AlbumsTab(
                albums = userAlbums,
                isLoading = isLoading,
                error = error,
                playerController = playerController,
                onRefresh = { viewModel.refreshContent() },
                onDeleteAlbum = { albumId -> viewModel.deleteAlbum(albumId) },
                onNavigateToAlbum = { albumId -> navController.navigate("album/$albumId") }
            )
            2 -> UploadTab(
                viewModel = viewModel,
                isLoading = isLoading,
                uploadProgress = uploadProgress,
                uploadSuccess = uploadSuccess
            )
        }
    }
}

@Composable
private fun SongsTab(
    songs: List<Song>,
    isLoading: Boolean,
    error: String?,
    playerController: PlayerController,
    onRefresh: () -> Unit,
    onDeleteSong: (Int) -> Unit
) {
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
                    text = error,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRefresh) {
                    Text("Retry")
                }
            }
        }
        songs.isEmpty() -> {
            EmptyArtistSongsState()
        }
        else -> {
            LazyColumn {
                // Songs header with play all
                item {
                    SongsHeader(
                        songCount = songs.size,
                        onPlayAll = { playerController.playPlaylist(songs) },
                        onShuffle = { playerController.playShuffled(songs) }
                    )
                }

                // Songs list
                items(songs) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            USongItem(
                                song = song,
                                collectionSongs = songs,
                                playbackMode = PlaybackMode.FROM_COLLECTION
                            )
                        }
                        IconButton(onClick = { onDeleteSong(song.id) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete song",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumsTab(
    albums: List<Album>,
    isLoading: Boolean,
    error: String?,
    playerController: PlayerController,
    onRefresh: () -> Unit,
    onDeleteAlbum: (Int) -> Unit,
    onNavigateToAlbum: (Int) -> Unit
) {
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
                    text = error,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRefresh) {
                    Text("Retry")
                }
            }
        }
        albums.isEmpty() -> {
            EmptyAlbumsState()
        }
        else -> {
            LazyColumn {
                item {
                    AlbumsHeader(albumCount = albums.size)
                }

                items(albums) { album ->
                    AlbumItem(
                        album = album,
                        onPlayAlbum = {
                            // Convert SongNested to Song for playback
                            val playableSongs = album.songs.map { nested ->
                                Song(
                                    id = nested.id,
                                    title = nested.title,
                                    duration = nested.duration,
                                    filePath = nested.filePath,
                                    createdAt = nested.createdAt,
                                    albumId = album.id,
                                    creatorId = album.creatorId,
                                    creator = nested.creator,
                                    likeCount = 0,
                                    //album = null,
                                    //genres = emptyList(),
                                    //playlists = emptyList()
                                )
                            }
                            if (playableSongs.isNotEmpty()) {
                                playerController.playPlaylist(playableSongs)
                            }
                        },
                        onDeleteAlbum = { onDeleteAlbum(album.id) },
                        onNavigateToAlbum = { onNavigateToAlbum(album.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun UploadTab(
    viewModel: ArtistViewModel,
    isLoading: Boolean,
    uploadProgress: Float,
    uploadSuccess: Boolean
) {
    var selectedAudioUri by remember { mutableStateOf<Uri?>(null) }
    var songTitle by remember { mutableStateOf("") }
    var selectedAlbumId by remember { mutableStateOf<Int?>(null) }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedAudioUri = uri
    }

    LaunchedEffect(uploadSuccess) {
        if (uploadSuccess) {
            selectedAudioUri = null
            songTitle = ""
            selectedAlbumId = null
            viewModel.refreshContent()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Upload New Song",
                    style = MaterialTheme.typography.headlineSmall
                )

                // Audio file selection
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { audioPickerLauncher.launch("audio/*") }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            if (selectedAudioUri != null) Icons.Default.AudioFile else Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedAudioUri != null) "Audio file selected" else "Select audio file",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (selectedAudioUri != null) {
                            Text(
                                text = selectedAudioUri.toString().substringAfterLast("/"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Song title
                OutlinedTextField(
                    value = songTitle,
                    onValueChange = { songTitle = it },
                    label = { Text("Song Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Upload progress
                if (isLoading) {
                    LinearProgressIndicator(
                        progress = uploadProgress,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Uploading... ${(uploadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Upload button
                Button(
                    onClick = {
                        selectedAudioUri?.let { uri ->
                            viewModel.uploadSong(
                                audioFileUri = uri,
                                title = songTitle,
                                albumId = selectedAlbumId
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedAudioUri != null && songTitle.isNotBlank() && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload Song")
                    }
                }
            }
        }
    }
}

@Composable
private fun SongsHeader(
    songCount: Int,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Your Songs",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "$songCount songs",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (songCount > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onPlayAll) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play All")
                }
                TextButton(onClick = onShuffle) {
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
private fun AlbumsHeader(albumCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Your Albums",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "$albumCount albums",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AlbumItem(
    album: Album,
    onPlayAlbum: () -> Unit,
    onDeleteAlbum: () -> Unit,
    onNavigateToAlbum: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onNavigateToAlbum() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                    Text(
                        text = try {
                            // Parse ISO date string like "2024-01-15T10:30:00"
                            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                            val outputFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                            val date = inputFormat.parse(album.releaseDate)
                            outputFormat.format(date ?: Date())
                        } catch (e: Exception) {
                            // Fallback to just showing the year if parsing fails
                            album.releaseDate.substringBefore('-')
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "${album.songs.size} songs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    if (album.songs.isNotEmpty()) {
                        IconButton(onClick = onPlayAlbum) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play Album")
                        }
                    }

                    IconButton(onClick = onDeleteAlbum) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (album.songs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(album.songs.take(3)) { song ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = song.title,
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (album.songs.size > 3) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = "+${album.songs.size - 3} more",
                                    modifier = Modifier.padding(8.dp),
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
}

@Composable
private fun EmptyArtistSongsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No songs uploaded yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Upload your first song to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun EmptyAlbumsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Album,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No albums created yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Create your first album to organize your songs",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}