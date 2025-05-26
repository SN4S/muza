package com.sn4s.muza.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.sn4s.muza.data.model.Album
import com.sn4s.muza.data.model.Playlist
import com.sn4s.muza.data.model.Song
import com.sn4s.muza.di.NetworkModule
import com.sn4s.muza.ui.components.CreatePlaylistDialog
import com.sn4s.muza.ui.viewmodels.LibraryViewModel
import com.sn4s.muza.ui.viewmodels.PlaylistViewModel
import com.sn4s.muza.ui.viewmodels.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    navController: NavController,
    viewModel: LibraryViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel? = null
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

    val playlists by playlistViewModel.playlists.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val likedSongs by viewModel.likedSongs.collectAsState()
    val isLoading by playlistViewModel.isLoading.collectAsState()
    val error by playlistViewModel.error.collectAsState()

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(LibraryFilter.ALL) }
    var sortBy by remember { mutableStateOf(SortOption.RECENTLY_ADDED) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            LibraryHeader(
                onCreatePlaylist = { showCreatePlaylistDialog = true }
            )
        }

        // Quick Access Section
        item {
            QuickAccessSection(
                likedSongs = likedSongs,
                playerViewModel = playerViewModel,
                navController=navController
            )
        }

        // Filter and Sort Section
        item {
            FilterSection(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
                sortBy = sortBy,
                onSortChanged = { sortBy = it }
            )
        }

        // Error handling
        if (error != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { playlistViewModel.clearError() }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        // Content based on filter
        when (selectedFilter) {
            LibraryFilter.ALL -> {
                // Mixed content sorted by type and date
                val allItems = buildList {
                    addAll(playlists.map { LibraryItem.PlaylistItem(it) })
                    addAll(albums.map { LibraryItem.AlbumItem(it) })
                }.sortedWith(getSortComparator(sortBy))

                if (allItems.isEmpty() && !isLoading) {
                    item {
                        LibraryEmptyState(
                            onCreatePlaylist = { showCreatePlaylistDialog = true }
                        )
                    }
                } else {
                    items(allItems) { item ->
                        when (item) {
                            is LibraryItem.PlaylistItem -> {
                                PlaylistListItem(
                                    playlist = item.playlist,
                                    onClick = { navController.navigate("playlist/${item.playlist.id}") }
                                )
                            }
                            is LibraryItem.AlbumItem -> {
                                AlbumListItem(
                                    album = item.album,
                                    onClick = { navController.navigate("album/${item.album.id}") }
                                )
                            }
                        }
                    }
                }
            }

            LibraryFilter.PLAYLISTS -> {
                val sortedPlaylists = playlists.sortedWith(getPlaylistSortComparator(sortBy))

                if (sortedPlaylists.isEmpty() && !isLoading) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.PlaylistAdd,
                            title = "No playlists yet",
                            subtitle = "Create your first playlist to organize your music",
                            actionText = "Create Playlist",
                            onActionClick = { showCreatePlaylistDialog = true }
                        )
                    }
                } else {
                    items(sortedPlaylists) { playlist ->
                        PlaylistListItem(
                            playlist = playlist,
                            onClick = { navController.navigate("playlist/${playlist.id}") }
                        )
                    }
                }
            }

            LibraryFilter.ALBUMS -> {
                val sortedAlbums = albums.sortedWith(getAlbumSortComparator(sortBy))

                if (sortedAlbums.isEmpty() && !isLoading) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.Album,
                            title = "No albums in your library",
                            subtitle = "Albums you save will appear here"
                        )
                    }
                } else {
                    items(sortedAlbums) { album ->
                        AlbumListItem(
                            album = album,
                            onClick = { navController.navigate("album/${album.id}") }
                        )
                    }
                }
            }
        }

        // Loading state
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    // Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name, description ->
                playlistViewModel.createPlaylist(name, description)
                showCreatePlaylistDialog = false
            },
            isLoading = isLoading
        )
    }
}

@Composable
private fun LibraryHeader(
    onCreatePlaylist: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Your Library",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        Row {
            IconButton(onClick = onCreatePlaylist) {
                Icon(Icons.Default.Add, contentDescription = "Create Playlist")
            }
        }
    }
}

@Composable
private fun QuickAccessSection(
    likedSongs: List<Song>,
    playerViewModel: PlayerViewModel?,
    navController: NavController,
) {
    Column {
        Text(
            text = "Made for you",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            item {
                QuickAccessCard(
                    icon = Icons.Default.Favorite,
                    title = "Liked Songs",
                    subtitle = "${likedSongs.size} songs",
                    onClick = {
                        navController.navigate("liked_songs")
                    },
                    enabled = likedSongs.isNotEmpty(),
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            }
            item {
                QuickAccessCard(
                    icon = Icons.Default.Download,
                    title = "Downloaded",
                    subtitle = "In future upd.",
                    onClick = { /* TODO */ }
                )
            }
        }
    }
}

@Composable
private fun QuickAccessCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(enabled = enabled) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) containerColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ){
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (enabled) {
                    if (containerColor == MaterialTheme.colorScheme.primaryContainer)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                } else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }}
    }
}

@Composable
private fun FilterSection(
    selectedFilter: LibraryFilter,
    onFilterSelected: (LibraryFilter) -> Unit,
    sortBy: SortOption,
    onSortChanged: (SortOption) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Filter chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(LibraryFilter.values()) { filter ->
                FilterChip(
                    onClick = { onFilterSelected(filter) },
                    label = { Text(filter.displayName) },
                    selected = selectedFilter == filter
                )
            }
        }

        // Sort dropdown
        var showSortMenu by remember { mutableStateOf(false) }

        Box {
            IconButton(onClick = { showSortMenu = true }) {
                Icon(Icons.Default.Sort, contentDescription = "Sort")
            }

            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                SortOption.values().forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option.displayName,
                                color = if (sortBy == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onSortChanged(option)
                            showSortMenu = false
                        },
                        leadingIcon = if (sortBy == option) {
                            { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistListItem(
    playlist: Playlist,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = playlist.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Column {
                Text("Playlist • ${playlist.songs.size} songs")
                playlist.description?.let { description ->
                    Text(
                        text = description,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        leadingContent = {
            Card(
                modifier = Modifier.size(56.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlaylistPlay,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        },
        trailingContent = {
            IconButton(onClick = { /* TODO: More options */ }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More options"
                )
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
private fun AlbumListItem(
    album: Album,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = album.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text("Album • ${album.creator.username} • ${album.songs.size} songs")
        },
        leadingContent = {
            Card(
                modifier = Modifier.size(56.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Album,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        trailingContent = {
            IconButton(onClick = { /* TODO: More options */ }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More options"
                )
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
private fun EmptyStateCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (actionText != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onActionClick) {
                    Text(actionText)
                }
            }
        }
    }
}

@Composable
private fun LibraryEmptyState(
    onCreatePlaylist: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.LibraryMusic,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your library is empty",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Start by liking songs or creating playlists",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onCreatePlaylist) {
            Text("Create Your First Playlist")
        }
    }
}

// Helper functions for sorting
private fun getSortComparator(sortBy: SortOption): Comparator<LibraryItem> {
    return when (sortBy) {
        SortOption.RECENTLY_ADDED -> compareByDescending {
            when (it) {
                is LibraryItem.PlaylistItem -> it.playlist.createdAt
                is LibraryItem.AlbumItem -> it.album.createdAt
            }
        }
        SortOption.ALPHABETICAL -> compareBy {
            when (it) {
                is LibraryItem.PlaylistItem -> it.playlist.name
                is LibraryItem.AlbumItem -> it.album.title
            }
        }
        SortOption.CREATOR -> compareBy {
            when (it) {
                is LibraryItem.PlaylistItem -> ""  // Playlists don't have creators in this context
                is LibraryItem.AlbumItem -> it.album.creator.username
            }
        }
    }
}

private fun getPlaylistSortComparator(sortBy: SortOption): Comparator<Playlist> {
    return when (sortBy) {
        SortOption.RECENTLY_ADDED -> compareByDescending { it.createdAt }
        SortOption.ALPHABETICAL -> compareBy { it.name }
        SortOption.CREATOR -> compareBy { it.name } // Playlists are all user's own
    }
}

private fun getAlbumSortComparator(sortBy: SortOption): Comparator<Album> {
    return when (sortBy) {
        SortOption.RECENTLY_ADDED -> compareByDescending { it.createdAt }
        SortOption.ALPHABETICAL -> compareBy { it.title }
        SortOption.CREATOR -> compareBy { it.creator.username }
    }
}

sealed class LibraryItem {
    data class PlaylistItem(val playlist: Playlist) : LibraryItem()
    data class AlbumItem(val album: Album) : LibraryItem()
}

enum class LibraryFilter(val displayName: String) {
    ALL("All"),
    PLAYLISTS("Playlists"),
    ALBUMS("Albums")
}

enum class SortOption(val displayName: String) {
    RECENTLY_ADDED("Recently Added"),
    ALPHABETICAL("Alphabetical"),
    CREATOR("Creator")
}