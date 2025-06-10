package com.sn4s.muza.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sn4s.muza.data.model.Album
import com.sn4s.muza.data.model.Song
import com.sn4s.muza.di.NetworkModule
import com.sn4s.muza.ui.components.PlaybackMode
import com.sn4s.muza.ui.components.USongItem
import com.sn4s.muza.ui.viewmodels.ArtistViewModel
import com.sn4s.muza.ui.viewmodels.PlayerController
import com.sn4s.muza.ui.viewmodels.ProfileViewModel
import com.sn4s.muza.ui.viewmodels.UserProfileViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(
    navController: NavController,
    viewModel: ArtistViewModel = hiltViewModel(),
    playerController: PlayerController = hiltViewModel()
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
    val uploadSuccess by viewModel.uploadSuccess.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        TopAppBar(
            title = { Text("Artist Dashboard") },
            actions = {
                IconButton(onClick = { viewModel.refreshContent() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
                IconButton(onClick = { navController.navigate("profile") }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        )

        // Tab Row - removed Settings tab
        ScrollableTabRow(selectedTabIndex = selectedTab) {
            val tabs = listOf(
                "Overview" to Icons.Default.Dashboard,
                "Songs" to Icons.Default.MusicNote,
                "Albums" to Icons.Default.Album,
                "Analytics" to Icons.Default.Analytics,
                "Upload" to Icons.Default.CloudUpload
            )

            tabs.forEachIndexed { index, (title, icon) ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { viewModel.setSelectedTab(index) },
                    text = { Text(title) },
                    icon = { Icon(icon, contentDescription = title) }
                )
            }
        }

        // Content
        when (selectedTab) {
            0 -> OverviewTab(
                songs = userSongs,
                albums = userAlbums,
                playerController = playerController,
                navController = navController,
                onQuickUpload = { viewModel.setSelectedTab(4) },
                onCreateAlbum = { title, coverUri ->
                    // TODO: Update ArtistViewModel.createAlbum to accept coverUri parameter
                    // For now, just create album without cover
                    viewModel.createAlbum(title)
                },
                onViewAnalytics = { viewModel.setSelectedTab(3) }
            )
            1 -> SongsTab(
                songs = userSongs,
                isLoading = isLoading,
                error = error,
                playerController = playerController,
                onDeleteSong = { songId -> viewModel.deleteSong(songId) }
            )
            2 -> AlbumsTab(
                albums = userAlbums,
                isLoading = isLoading,
                error = error,
                navController = navController,
                onDeleteAlbum = { albumId -> viewModel.deleteAlbum(albumId) },
                onCreateAlbum = { title, coverUri ->
                    // TODO: Update ArtistViewModel.createAlbum to accept coverUri parameter
                    // For now, just create album without cover
                    viewModel.createAlbum(title)
                }
            )
            3 -> AnalyticsTab(
                songs = userSongs,
                albums = userAlbums
            )
            4 -> UploadTab(
                viewModel = viewModel,
                isLoading = isLoading,
                uploadSuccess = uploadSuccess,
                userAlbums = userAlbums
            )
        }
    }
}

@Composable
private fun OverviewTab(
    songs: List<Song>,
    albums: List<Album>,
    playerController: PlayerController,
    navController: NavController,
    onQuickUpload: () -> Unit,
    onCreateAlbum: (String, Uri?) -> Unit,
    onViewAnalytics: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats Cards - replaced Plays with Likes
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard(
                    title = "Songs",
                    count = songs.size,
                    icon = Icons.Default.MusicNote
                )
                StatCard(
                    title = "Albums",
                    count = albums.size,
                    icon = Icons.Default.Album
                )
                StatCard(
                    title = "Likes",
                    count = songs.sumOf { it.likeCount ?: 0 },
                    icon = Icons.Default.Favorite
                )
            }
        }

        // Quick Actions
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        QuickActionButton(
                            title = "Upload Song",
                            icon = Icons.Default.Add,
                            onClick = onQuickUpload
                        )
                        QuickActionButton(
                            title = "Create Album",
                            icon = Icons.Default.Album,
                            onClick = { /* Show create album dialog */ }
                        )
                        QuickActionButton(
                            title = "View Analytics",
                            icon = Icons.Default.Analytics,
                            onClick = onViewAnalytics
                        )
                    }
                }
            }
        }

        // Recent Songs
        if (songs.isNotEmpty()) {
            item {
                Text(
                    text = "Recent Songs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(songs.take(3)) { song ->
                USongItem(
                    song = song,
                    playerController = playerController,
                    playbackMode = PlaybackMode.FROM_COLLECTION,
                    collectionSongs = songs
                )
            }
        }

        // Recent Albums
        if (albums.isNotEmpty()) {
            item {
                Text(
                    text = "Recent Albums",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(albums.take(3)) { album ->
                        AlbumCard(
                            album = album,
                            onClick = { navController.navigate("album/${album.id}") }
                        )
                    }
                }
            }
        }

        // Empty State
        if (songs.isEmpty() && albums.isEmpty()) {
            item {
                EmptyOverviewState(onGetStarted = onQuickUpload)
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AlbumCard(
    album: Album,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val coverUrl = album.coverImage?.let {
        "${NetworkModule.BASE_URL}albums/${album.id}/cover"
    }

    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
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
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Album,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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

@Composable
private fun EmptyOverviewState(onGetStarted: () -> Unit) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CloudUpload,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Welcome to your Artist Dashboard!",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Start by uploading your first song to share your music with the world.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onGetStarted) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload Your First Song")
            }
        }
    }
}

@Composable
private fun SongsTab(
    songs: List<Song>,
    isLoading: Boolean,
    error: String?,
    playerController: PlayerController,
    onDeleteSong: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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
                        text = "My Songs",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${songs.size} tracks",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (songs.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                playerController.playShuffledFromCollection(songs, songs.random())
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Shuffle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Shuffle")
                        }
                        Button(
                            onClick = {
                                playerController.playFromCollectionStartingAt(songs, songs.first())
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play All")
                        }
                    }
                }
            }
        }

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
                ErrorState(error = error)
            }
            songs.isEmpty() -> {
                EmptyState(
                    icon = Icons.Default.MusicNote,
                    title = "No songs yet",
                    subtitle = "Upload your first song to get started"
                )
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(songs) { song ->
                        SongItem(
                            song = song,
                            playerController = playerController,
                            onDelete = { onDeleteSong(song.id) },
                            allSongs = songs
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SongItem(
    song: Song,
    playerController: PlayerController,
    onDelete: () -> Unit,
    allSongs: List<Song>
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cover
            Card(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                val coverUrl = song.coverImage?.let {
                    "${NetworkModule.BASE_URL}songs/${song.id}/cover"
                }

                if (coverUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(coverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Song cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null
                        )
                    }
                }
            }

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                song.duration?.let { duration ->
                    val minutes = duration / 60
                    val seconds = duration % 60
                    Text(
                        text = "%d:%02d".format(minutes, seconds),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Actions
            Row {
                IconButton(
                    onClick = {
                        playerController.playFromCollectionStartingAt(allSongs, song)
                    }
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                }

                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
            }
        }
    }

    // Menu
    if (showMenu) {
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Add to Queue") },
                onClick = {
                    playerController.addSongToQueue(song)
                    showMenu = false
                },
                leadingIcon = { Icon(Icons.Default.Queue, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    showDeleteDialog = true
                    showMenu = false
                },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
            )
        }
    }

    // Delete Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Song") },
            text = { Text("Are you sure you want to delete \"${song.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AlbumsTab(
    albums: List<Album>,
    isLoading: Boolean,
    error: String?,
    navController: NavController,
    onDeleteAlbum: (Int) -> Unit,
    onCreateAlbum: (String, Uri?) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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
                        text = "Your Albums",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${albums.size} albums",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Album")
                }
            }
        }

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
                ErrorState(error = error)
            }
            albums.isEmpty() -> {
                EmptyState(
                    icon = Icons.Default.Album,
                    title = "No albums yet",
                    subtitle = "Create albums to organize your music"
                )
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(albums) { album ->
                        AlbumGridCard(
                            album = album,
                            onClick = { navController.navigate("album/${album.id}") },
                            onDelete = { onDeleteAlbum(album.id) }
                        )
                    }
                }
            }
        }
    }

    // Create Album Dialog
    if (showCreateDialog) {
        CreateAlbumDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, coverUri ->
                onCreateAlbum(title, coverUri)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun CreateAlbumDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Uri?) -> Unit
) {
    var albumTitle by remember { mutableStateOf("") }
    var selectedCoverUri by remember { mutableStateOf<Uri?>(null) }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedCoverUri = uri }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Album") },
        text = {
            Column {
                Text("Enter album details:")
                Spacer(modifier = Modifier.height(16.dp))

                // Album Title
                OutlinedTextField(
                    value = albumTitle,
                    onValueChange = { albumTitle = it },
                    label = { Text("Album Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Cover Image Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedCoverUri != null)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedCoverUri != null) {
                                AsyncImage(
                                    model = selectedCoverUri,
                                    contentDescription = "Selected cover",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Text(
                                text = if (selectedCoverUri != null) "Cover selected" else "Add cover (optional)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        FilledTonalButton(
                            onClick = { coverPickerLauncher.launch("image/*") }
                        ) {
                            Text("Choose")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(albumTitle, selectedCoverUri) },
                enabled = albumTitle.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AlbumGridCard(
    album: Album,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val coverUrl = album.coverImage?.let {
        "${NetworkModule.BASE_URL}albums/${album.id}/cover"
    }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
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
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Album,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    IconButton(
                        onClick = { showDeleteDialog = true }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${album.songs.size} songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Album") },
            text = { Text("Are you sure you want to delete \"${album.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AnalyticsTab(
    songs: List<Song>,
    albums: List<Album>
) {
    // Use ProfileViewModel to get current user info and load their profile as UserProfile
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val userProfileViewModel: UserProfileViewModel = hiltViewModel()

    val currentUser by profileViewModel.user.collectAsStateWithLifecycle()
    val userProfile by userProfileViewModel.userProfile.collectAsStateWithLifecycle()

    // Load current user's profile with follower data when user is available
    LaunchedEffect(currentUser?.id) {
        currentUser?.id?.let { userId ->
            userProfileViewModel.loadUserProfile(userId)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Analytics & Insights",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AnalyticCard(
                            "Total Likes",
                            songs.sumOf { it.likeCount ?: 0 }.toString(),
                            Icons.Default.Favorite
                        )
                        AnalyticCard(
                            "Followers",
                            userProfile?.followerCount?.toString() ?: "0",
                            Icons.Default.People
                        )
                    }
                }
            }
        }

        if (songs.isNotEmpty()) {
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Top Songs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        songs.sortedByDescending { it.likeCount ?: 0 }.take(3).forEach { song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = song.title,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${song.likeCount ?: 0} likes",
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
private fun AnalyticCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun UploadTab(
    viewModel: ArtistViewModel,
    isLoading: Boolean,
    uploadSuccess: Boolean,
    userAlbums: List<Album>
) {
    var selectedAudioUri by remember { mutableStateOf<Uri?>(null) }
    var selectedCoverUri by remember { mutableStateOf<Uri?>(null) }
    var songTitle by remember { mutableStateOf("") }
    var selectedAlbum by remember { mutableStateOf<Album?>(null) }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedAudioUri = uri }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedCoverUri = uri }

    LaunchedEffect(uploadSuccess) {
        if (uploadSuccess) {
            selectedAudioUri = null
            selectedCoverUri = null
            songTitle = ""
            selectedAlbum = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Upload New Song",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Song Title
        OutlinedTextField(
            value = songTitle,
            onValueChange = { songTitle = it },
            label = { Text("Song Title") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        // Audio File
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (selectedAudioUri != null)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (selectedAudioUri != null) "Audio file selected" else "Select audio file",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = if (selectedAudioUri != null) "Ready to upload" else "Choose MP3, WAV, FLAC, or M4A",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { audioPickerLauncher.launch("audio/*") },
                    enabled = !isLoading
                ) {
                    Text("Choose File")
                }
            }
        }

        // Cover Art
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (selectedCoverUri != null)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedCoverUri != null) {
                        AsyncImage(
                            model = selectedCoverUri,
                            contentDescription = "Selected cover",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (selectedCoverUri != null) "Cover art selected" else "Add cover art (optional)",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Square image, at least 300x300px",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FilledTonalButton(
                    onClick = { coverPickerLauncher.launch("image/*") },
                    enabled = !isLoading
                ) {
                    Text("Choose Image")
                }
            }
        }

        // Album Selection
        var expanded by remember { mutableStateOf(false) }

        Box {
            OutlinedTextField(
                value = selectedAlbum?.title ?: "No Album",
                onValueChange = { },
                label = { Text("Album (optional)") },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { expanded = !expanded && !isLoading }) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Select album"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isLoading) {
                        expanded = !expanded
                    },
                enabled = !isLoading
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("No Album") },
                    onClick = {
                        selectedAlbum = null
                        expanded = false
                    }
                )
                if (userAlbums.isNotEmpty()) {
                    HorizontalDivider()
                    userAlbums.forEach { album ->
                        DropdownMenuItem(
                            text = { Text(album.title) },
                            onClick = {
                                selectedAlbum = album
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        // Upload Button
        Button(
            onClick = {
                selectedAudioUri?.let { audioUri ->
                    viewModel.uploadSong(
                        title = songTitle,
                        albumId = selectedAlbum?.id,
                        audioFileUri = audioUri,
                        coverUri = selectedCoverUri
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && songTitle.isNotBlank() && selectedAudioUri != null
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Uploading...")
            } else {
                Icon(Icons.Default.CloudUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload Song")
            }
        }
    }
}

// Utility Components
@Composable
private fun ErrorState(error: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}