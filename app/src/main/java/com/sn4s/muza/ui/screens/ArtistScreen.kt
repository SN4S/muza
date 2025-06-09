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
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sn4s.muza.di.NetworkModule
import com.sn4s.muza.ui.viewmodels.ArtistViewModel
import com.sn4s.muza.ui.viewmodels.PlayerController
import com.sn4s.muza.ui.viewmodels.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(
    navController: NavController,
    playerViewModel: PlayerController = hiltViewModel(),
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

    val selectedTab by viewModel.selectedTab.collectAsState()
    val userSongs by viewModel.userSongs.collectAsState()
    val userAlbums by viewModel.userAlbums.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

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
                icon = { Icon(Icons.Default.Upload, contentDescription = null) }
            )
        }

        // Error display
        if (error != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.clearError() }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Tab content
        when (selectedTab) {
            0 -> SongsTab(
                songs = userSongs,
                isLoading = isLoading,
                onDeleteSong = { viewModel.deleteSong(it) },
                playerViewModel = playerViewModel
            )
            1 -> AlbumsTab(
                albums = userAlbums,
                isLoading = isLoading,
                onDeleteAlbum = { viewModel.deleteAlbum(it) },
                onCreateAlbum = { title -> viewModel.createAlbum(title) },
                playerViewModel = playerViewModel
            )
            2 -> UploadTab(
                albums = userAlbums,
                onUpload = { title, albumId, uri -> viewModel.uploadSong(title, albumId, uri) },
                onCreateAlbum = { title -> viewModel.createAlbum(title) },
                isLoading = isLoading,
                uploadProgress = viewModel.uploadProgress.collectAsState().value,
                uploadSuccess = viewModel.uploadSuccess.collectAsState().value,
                onClearSuccess = { viewModel.clearUploadSuccess() }
            )
        }
    }
}

@Composable
private fun SongsTab(
    songs: List<com.sn4s.muza.data.model.Song>,
    isLoading: Boolean,
    onDeleteSong: (Int) -> Unit,
    playerViewModel: PlayerController = hiltViewModel()
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Songs (${songs.size})",
                    style = MaterialTheme.typography.titleLarge
                )
                if (songs.isNotEmpty() && playerViewModel != null) {
                    TextButton(
                        onClick = { playerViewModel.playPlaylist(songs) }
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play All")
                    }
                }
            }
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        if (songs.isEmpty() && !isLoading) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No songs uploaded yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Upload your first song using the Upload tab",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(songs) { song ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${song.likeCount} likes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { playerViewModel?.playSong(song) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                    }

                    IconButton(onClick = { onDeleteSong(song.id) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumsTab(
    albums: List<com.sn4s.muza.data.model.Album>,
    isLoading: Boolean,
    onDeleteAlbum: (Int) -> Unit,
    onCreateAlbum: (String) -> Unit,
    playerViewModel: PlayerController = hiltViewModel()
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Albums (${albums.size})",
                    style = MaterialTheme.typography.titleLarge
                )
                Button(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Album")
                }
            }
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        if (albums.isEmpty() && !isLoading) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Album,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No albums created yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Create your first album to organize your songs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(albums) { album ->
            Card(modifier = Modifier.fillMaxWidth()) {
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
                            Text(
                                text = "${album.songs.size} songs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row {
                            if (album.songs.isNotEmpty() && playerViewModel != null) {
                                IconButton(
                                    onClick = {
                                        // Convert SongNested to Song - you might need to adjust this
                                        val songs = album.songs.mapNotNull { songNested ->
                                            // You'll need to implement this conversion
                                            // or modify your data models
                                            null // Placeholder
                                        }
                                        // playerViewModel.playPlaylist(songs)
                                    }
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play Album")
                                }
                            }

                            IconButton(onClick = { onDeleteAlbum(album.id) }) {
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
                                        maxLines = 1
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
                                            style = MaterialTheme.typography.bodySmall
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

    if (showCreateDialog) {
        var albumTitle by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Album") },
            text = {
                OutlinedTextField(
                    value = albumTitle,
                    onValueChange = { albumTitle = it },
                    label = { Text("Album Title") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (albumTitle.isNotBlank()) {
                            onCreateAlbum(albumTitle)
                            showCreateDialog = false
                        }
                    },
                    enabled = albumTitle.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UploadTab(
    albums: List<com.sn4s.muza.data.model.Album>,
    onUpload: (String, Int?, Uri) -> Unit,
    onCreateAlbum: (String) -> Unit,
    isLoading: Boolean,
    uploadProgress: Float,
    uploadSuccess: Boolean,
    onClearSuccess: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedAlbumId by remember { mutableStateOf<Int?>(null) }
    var selectedAudioUri by remember { mutableStateOf<Uri?>(null) }
    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var newAlbumTitle by remember { mutableStateOf("") }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedAudioUri = uri
    }

    LaunchedEffect(uploadSuccess) {
        if (uploadSuccess) {
            title = ""
            selectedAlbumId = null
            selectedAudioUri = null
            onClearSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Upload New Song",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Song Title") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            enabled = !isLoading
        )

        // Album Selection
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Album:",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(end = 16.dp)
            )

            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = albums.find { it.id == selectedAlbumId }?.title ?: "No Album",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    enabled = !isLoading
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("No Album") },
                        onClick = {
                            selectedAlbumId = null
                            expanded = false
                        }
                    )
                    albums.forEach { album ->
                        DropdownMenuItem(
                            text = { Text(album.title) },
                            onClick = {
                                selectedAlbumId = album.id
                                expanded = false
                            }
                        )
                    }
                }
            }

            TextButton(
                onClick = { showCreateAlbumDialog = true },
                enabled = !isLoading
            ) {
                Text("New Album")
            }
        }

        // Audio File Selection
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            onClick = { audioPickerLauncher.launch("audio/*") },
            enabled = !isLoading
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AudioFile,
                    contentDescription = "Select Audio File",
                    modifier = Modifier.padding(end = 16.dp)
                )
                Column {
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
        }

        // Upload Progress
        if (isLoading) {
            LinearProgressIndicator(
                progress = uploadProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
            Text(
                text = "Uploading... ${(uploadProgress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Upload Button
        Button(
            onClick = {
                selectedAudioUri?.let { uri ->
                    onUpload(title, selectedAlbumId, uri)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && title.isNotBlank() && selectedAudioUri != null
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Upload,
                    contentDescription = "Upload",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Upload Song")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Upload Tips
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Upload Tips",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "• Supported formats: MP3, WAV, FLAC, M4A, OGG",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "• Maximum file size: 50MB",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }

    // Create Album Dialog
    if (showCreateAlbumDialog) {
        AlertDialog(
            onDismissRequest = { showCreateAlbumDialog = false },
            title = { Text("Create New Album") },
            text = {
                OutlinedTextField(
                    value = newAlbumTitle,
                    onValueChange = { newAlbumTitle = it },
                    label = { Text("Album Title") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newAlbumTitle.isNotBlank()) {
                            onCreateAlbum(newAlbumTitle)
                            newAlbumTitle = ""
                            showCreateAlbumDialog = false
                        }
                    },
                    enabled = newAlbumTitle.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreateAlbumDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}