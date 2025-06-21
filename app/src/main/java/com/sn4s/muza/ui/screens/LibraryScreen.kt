package com.sn4s.muza.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sn4s.muza.data.model.Album
import com.sn4s.muza.data.model.Playlist
import com.sn4s.muza.data.model.Song
import com.sn4s.muza.data.model.User
import com.sn4s.muza.data.model.UserProfile
import com.sn4s.muza.di.NetworkModule
import com.sn4s.muza.ui.components.CreatePlaylistDialog
import com.sn4s.muza.ui.components.UserAvatar
import com.sn4s.muza.ui.viewmodels.LibraryViewModel
import com.sn4s.muza.ui.viewmodels.PlayerController
import com.sn4s.muza.ui.viewmodels.PlaylistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    navController: NavController,
    viewModel: LibraryViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel(),
    playerViewModel: PlayerController = hiltViewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val followedArtists by viewModel.followedArtists.collectAsState()

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
    val albums by viewModel.likedAlbums.collectAsState()
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
                navController = navController
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
                    addAll(followedArtists.map { LibraryItem.ArtistItem(it)})
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
                            is LibraryItem.ArtistItem ->{
                                FollowedArtistItem(
                                    artist = item.artist,
                                    navController
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
                            icon = Icons.Default.PlaylistPlay,
                            title = "No playlists yet",
                            subtitle = "Create your first playlist to get started",
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
                            title = "No albums yet",
                            subtitle = "Albums you save will appear here",
                            actionText = null,
                            onActionClick = null
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
            LibraryFilter.FOLLOWED_ARTISTS -> {
                val sortedArt = followedArtists

                if (sortedArt.isEmpty() && !isLoading) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.Album,
                            title = "No followed artist yet",
                            subtitle = "Artists you subscribe will appear here",
                            actionText = null,
                            onActionClick = null
                        )
                    }
                } else {
                    items(sortedArt) { artist ->
                        FollowedArtistItem(
                            artist = artist,
                            navController) }
                    }
                }

        }

        // Loading indicator
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
    }

    // Create playlist dialog
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name, description ->
                playlistViewModel.createPlaylist(name, description)
                showCreatePlaylistDialog = false
            }
        )
    }
}

@Composable
private fun FollowedArtistItem(
    artist: UserProfile,
    navController: NavController
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate("artist/${artist.id}") },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UserAvatar(
                userId = artist.id,
                username = artist.username,
                imageUrl = artist.image,
                size = 48.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = artist.username,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Artist • ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "View artist",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
    playerViewModel: PlayerController,
    navController: NavController
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
                    subtitle = "In future update",
                    onClick = { /* TODO: Implement downloaded songs */ },
                    enabled = false,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
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
            .width(200.dp)
            .clickable(enabled = enabled) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) containerColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
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
                        }
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
            Text("Playlist • ${playlist.songs.size} songs")
        },
        leadingContent = {
            Card(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(8.dp),
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
                        modifier = Modifier.size(24.dp),
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
    val context = LocalContext.current
    val coverUrl = album.coverImage?.let {
        "${NetworkModule.BASE_URL}albums/${album.id}/cover"
    }

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
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                if (coverUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(coverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Album cover",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
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
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            if (actionText != null && onActionClick != null) {
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
                imageVector = Icons.Default.LibraryMusic,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Your library is empty",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Start by creating a playlist or saving some music",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Button(onClick = onCreatePlaylist) {
                Text("Create Playlist")
            }
        }
    }
}

// Data classes and enums
sealed class LibraryItem {
    data class PlaylistItem(val playlist: Playlist) : LibraryItem()
    data class AlbumItem(val album: Album) : LibraryItem()
    data class ArtistItem(val artist: UserProfile) : LibraryItem()
}

enum class LibraryFilter(val displayName: String) {
    ALL("All"),
    PLAYLISTS("Playlists"),
    ALBUMS("Albums"),
    FOLLOWED_ARTISTS("Followed")
}

enum class SortOption(val displayName: String) {
    RECENTLY_ADDED("Recently Added"),
    ALPHABETICAL("A-Z"),
    MOST_PLAYED("Most Played")
}

// Sorting functions
private fun getSortComparator(sortBy: SortOption): Comparator<LibraryItem> {
    return when (sortBy) {
        SortOption.RECENTLY_ADDED -> compareByDescending { item ->
            when (item) {
                is LibraryItem.PlaylistItem -> item.playlist.createdAt
                is LibraryItem.AlbumItem -> item.album.createdAt
                is LibraryItem.ArtistItem -> item.artist.songCount
            }
        }
        SortOption.ALPHABETICAL -> compareBy { item ->
            when (item) {
                is LibraryItem.PlaylistItem -> item.playlist.name
                is LibraryItem.AlbumItem -> item.album.title
                is LibraryItem.ArtistItem -> item.artist.username
            }
        }
        SortOption.MOST_PLAYED -> compareByDescending { item ->
            when (item) {
                is LibraryItem.PlaylistItem -> item.playlist.songs.size
                is LibraryItem.AlbumItem -> item.album.songs.size
                is LibraryItem.ArtistItem -> item.artist.followerCount
            }
        }
    }
}

private fun getPlaylistSortComparator(sortBy: SortOption): Comparator<Playlist> {
    return when (sortBy) {
        SortOption.RECENTLY_ADDED -> compareByDescending { it.createdAt }
        SortOption.ALPHABETICAL -> compareBy { it.name }
        SortOption.MOST_PLAYED -> compareByDescending { it.songs.size }
    }
}

private fun getAlbumSortComparator(sortBy: SortOption): Comparator<Album> {
    return when (sortBy) {
        SortOption.RECENTLY_ADDED -> compareByDescending { it.createdAt }
        SortOption.ALPHABETICAL -> compareBy { it.title }
        SortOption.MOST_PLAYED -> compareByDescending { it.songs.size }
    }
}