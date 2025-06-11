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
import androidx.compose.foundation.shape.CircleShape
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
            }
        )

        // Tab Row - Analytics moved to left, Overview removed
        TabRow(selectedTabIndex = selectedTab) {
            val tabs = listOf(
                "Analytics" to Icons.Default.Analytics,
                "Songs" to Icons.Default.MusicNote,
                "Albums" to Icons.Default.Album,
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

        // Content - Updated tab indices
        when (selectedTab) {
            0 -> AnalyticsTab(
                songs = userSongs,
                albums = userAlbums
            )
            1 -> SongsTab(
                songs = userSongs,
                isLoading = isLoading,
                error = error,
                playerController = playerController,
                userAlbums = userAlbums,
                onDeleteSong = { songId -> viewModel.deleteSong(songId) },
                onEditSong = { songId, title, albumId, coverUri ->
                    viewModel.updateSong(songId, title, albumId, coverUri)
                }
            )
            2 -> AlbumsTab(
                albums = userAlbums,
                isLoading = isLoading,
                error = error,
                navController = navController,
                onDeleteAlbum = { albumId -> viewModel.deleteAlbum(albumId) },
                onCreateAlbum = { title, coverUri -> viewModel.createAlbum(title = title, coverUri = coverUri) },
                onEditAlbum = { albumId, title, releaseDate, coverUri ->
                    viewModel.updateAlbum(albumId, title, releaseDate, coverUri)
                }
            )
            3 -> UploadTab(
                viewModel = viewModel,
                isLoading = isLoading,
                uploadSuccess = uploadSuccess,
                userAlbums = userAlbums
            )
        }
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
private fun SongsTab(
    songs: List<Song>,
    isLoading: Boolean,
    error: String?,
    playerController: PlayerController,
    userAlbums: List<Album>,
    onDeleteSong: (Int) -> Unit,
    onEditSong: (Int, String?, Int?, Uri?) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (error != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        if (isLoading && songs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (songs.isEmpty()) {
            EmptyState(
                icon = Icons.Default.MusicNote,
                title = "No songs yet",
                subtitle = "Upload your first song to get started"
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(songs) { song ->
                    var showDeleteDialog by remember { mutableStateOf(false) }
                    var showEditDialog by remember { mutableStateOf(false) }

                    Card {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            USongItem(
                                song = song,
                                modifier = Modifier.weight(1f),
                                collectionSongs = songs,
                                playbackMode = PlaybackMode.FROM_COLLECTION,
                                showMoreOptions = false
                            )

                            Row {
                                IconButton(
                                    onClick = { showEditDialog = true }
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
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

                            if (showEditDialog) {
                                EditSongDialog(
                                    song = song,
                                    userAlbums = userAlbums,
                                    onDismiss = { showEditDialog = false },
                                    onConfirm = { title, albumId, coverUri ->
                                        onEditSong(song.id,title, albumId, coverUri)
                                        showEditDialog = false
                                    }
                                )
                            }
                        }
                    }

                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Delete Song") },
                            text = { Text("Are you sure you want to delete \"${song.title}\"?") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        onDeleteSong(song.id)
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
            }
        }
    }
}

@Composable
private fun AlbumsTab(
    albums: List<Album>,
    isLoading: Boolean,
    error: String?,
    navController: NavController,
    onDeleteAlbum: (Int) -> Unit,
    onCreateAlbum: (String, Uri?) -> Unit,
    onEditAlbum: (Int, String?, String?, Uri?) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header with Create Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Albums",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = { showCreateDialog = true },
                enabled = !isLoading
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Album")
            }
        }

        if (error != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        if (isLoading && albums.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (albums.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Album,
                title = "No albums yet",
                subtitle = "Create your first album to organize your songs"
            )
        } else {
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
                        onDelete = { onDeleteAlbum(album.id) },
                        onEdit = { title, releaseDate, coverUri ->
                            onEditAlbum(album.id, title, releaseDate, coverUri)
                        }
                    )
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
private fun EditSongDialog(
    song: Song,
    userAlbums: List<Album>,
    onDismiss: () -> Unit,
    onConfirm: (String?, Int?, Uri?) -> Unit
) {
    var songTitle by remember { mutableStateOf(song.title) }
    var selectedAlbumId by remember { mutableStateOf(song.albumId) }
    var selectedCoverUri by remember { mutableStateOf<Uri?>(null) }
    var showAlbumDropdown by remember { mutableStateOf(false) }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedCoverUri = uri }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Song") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Song Title
                OutlinedTextField(
                    value = songTitle,
                    onValueChange = { songTitle = it },
                    label = { Text("Song Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Album Selection (Simple Button instead of dropdown)
                val selectedAlbum = userAlbums.find { it.id == selectedAlbumId }
                OutlinedButton(
                    onClick = { showAlbumDropdown = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectedAlbum?.title ?: "No Album (Single)")
                }

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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (selectedCoverUri != null) "New cover selected" else "Update cover",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = if (selectedCoverUri != null) "New image ready" else "Optional",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
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
                onClick = {
                    onConfirm(
                        if (songTitle != song.title) songTitle else null,
                        selectedAlbumId,
                        selectedCoverUri
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Simple Album Selection Dialog
    if (showAlbumDropdown) {
        AlertDialog(
            onDismissRequest = { showAlbumDropdown = false },
            title = { Text("Select Album") },
            text = {
                LazyColumn {
                    item {
                        TextButton(
                            onClick = {
                                selectedAlbumId = null
                                showAlbumDropdown = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("No Album (Single)")
                        }
                    }
                    items(userAlbums) { album ->
                        TextButton(
                            onClick = {
                                selectedAlbumId = album.id
                                showAlbumDropdown = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(album.title)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAlbumDropdown = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EditAlbumDialog(
    album: Album,
    onDismiss: () -> Unit,
    onConfirm: (String?, String?, Uri?) -> Unit
) {
    var albumTitle by remember { mutableStateOf(album.title) }
    var releaseDate by remember { mutableStateOf(album.releaseDate ?: "") }
    var selectedCoverUri by remember { mutableStateOf<Uri?>(null) }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedCoverUri = uri }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Album") },
        text = {
            Column {
                // Album Title
                OutlinedTextField(
                    value = albumTitle,
                    onValueChange = { albumTitle = it },
                    label = { Text("Album Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Release Date
                OutlinedTextField(
                    value = releaseDate,
                    onValueChange = { releaseDate = it },
                    label = { Text("Release Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("2024-01-01") }
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (selectedCoverUri != null) "New cover selected" else "Update cover",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = if (selectedCoverUri != null) "New image ready" else "Optional",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
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
                onClick = {
                    onConfirm(
                        if (albumTitle != album.title) albumTitle else null,
                        if (releaseDate.isNotBlank() && releaseDate != album.releaseDate) releaseDate else null,
                        selectedCoverUri
                    )
                }
            ) {
                Text("Save")
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (selectedCoverUri != null) "Cover image selected" else "Select cover image",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = if (selectedCoverUri != null) "Image ready" else "Optional",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
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
    onDelete: () -> Unit,
    onEdit: (String?, String?, Uri?) -> Unit
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
                    var showEditDialog by remember { mutableStateOf(false) }

                    Row {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = { showEditDialog = true },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (showEditDialog) {
                        EditAlbumDialog(
                            album = album,
                            onDismiss = { showEditDialog = false },
                            onConfirm = { title, releaseDate, coverUri ->
                                onEdit(title, releaseDate, coverUri)
                                showEditDialog = false
                            }
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

@OptIn(ExperimentalMaterial3Api::class)
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
            enabled = !isLoading,
            singleLine = true
        )

        // Album Selection
        var showAlbumMenu by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = showAlbumMenu,
            onExpandedChange = { showAlbumMenu = !showAlbumMenu && !isLoading }
        ) {
            OutlinedTextField(
                value = selectedAlbum?.title ?: "No Album (Single)",
                onValueChange = { },
                readOnly = true,
                label = { Text("Album") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showAlbumMenu) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                enabled = !isLoading
            )

            ExposedDropdownMenu(
                expanded = showAlbumMenu,
                onDismissRequest = { showAlbumMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("No Album (Single)") },
                    onClick = {
                        selectedAlbum = null
                        showAlbumMenu = false
                    }
                )
                userAlbums.forEach { album ->
                    DropdownMenuItem(
                        text = { Text(album.title) },
                        onClick = {
                            selectedAlbum = album
                            showAlbumMenu = false
                        }
                    )
                }
            }
        }

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
                            contentDescription = "Cover preview",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Column {
                        Text(
                            text = if (selectedCoverUri != null) "Cover art selected" else "Select cover art",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (selectedCoverUri != null) "Image ready" else "Optional - JPG, PNG",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = { coverPickerLauncher.launch("image/*") },
                    enabled = !isLoading
                ) {
                    Text("Choose")
                }
            }
        }

        // Upload Button
        Button(
            onClick = {
                if (selectedAudioUri != null && songTitle.isNotBlank()) {
                    viewModel.uploadSong(
                        title = songTitle,
                        albumId = selectedAlbum?.id,
                        audioFileUri = selectedAudioUri!!,
                        coverUri = selectedCoverUri
                    )
                }
            },
            enabled = !isLoading && selectedAudioUri != null && songTitle.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Uploading...")
            } else {
                Icon(Icons.Default.CloudUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload Song")
            }
        }

        if (uploadSuccess) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "Song uploaded successfully!",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}