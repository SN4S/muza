package com.sn4s.muza.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sn4s.muza.data.model.Album
import com.sn4s.muza.data.model.User
import com.sn4s.muza.di.NetworkModule
import com.sn4s.muza.ui.components.SongItem
import com.sn4s.muza.ui.viewmodels.PlayerViewModel
import com.sn4s.muza.ui.viewmodels.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel(),
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

    val searchQuery by viewModel.searchQuery.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val albums by viewModel.albums.collectAsState()

    var selectedFilter by remember { mutableStateOf(SearchFilter.ALL) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Search Header
        SearchHeader(
            searchQuery = searchQuery,
            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
            selectedFilter = selectedFilter,
            onFilterChange = { selectedFilter = it }
        )

        // Search Results
        if (searchQuery.isBlank()) {
            SearchEmptyState()
        } else {
            SearchResults(
                songs = songs,
                artists = artists,
                albums = albums,
                selectedFilter = selectedFilter,
                playerViewModel = playerViewModel,
                navController = navController
            )
        }
    }
}

@Composable
private fun SearchHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: SearchFilter,
    onFilterChange: (SearchFilter) -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search songs, artists, albums...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge
        )

        if (searchQuery.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(SearchFilter.values()) { filter ->
                    FilterChip(
                        onClick = { onFilterChange(filter) },
                        label = { Text(filter.displayName) },
                        selected = selectedFilter == filter,
                        leadingIcon = if (selectedFilter == filter) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Search for music",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Find your favorite songs, artists, and albums",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun SearchResults(
    songs: List<com.sn4s.muza.data.model.Song>,
    artists: List<User>,
    albums: List<Album>,
    selectedFilter: SearchFilter,
    playerViewModel: PlayerViewModel?,
    navController: NavController
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (selectedFilter) {
            SearchFilter.ALL -> {
                // Show all results with section headers
                if (songs.isNotEmpty()) {
                    item {
                        SearchSectionHeader(
                            title = "Songs",
                            count = songs.size,
                            onPlayAll = if (playerViewModel != null && songs.size > 1) {
                                { playerViewModel.playPlaylist(songs) }
                            } else null
                        )
                    }
                    items(songs.take(5)) { song ->
                        SongItem(
                            song = song,
                            playerViewModel = playerViewModel,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (songs.size > 5) {
                        item {
                            TextButton(
                                onClick = { /* TODO: Show all songs */ },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("See all ${songs.size} songs")
                            }
                        }
                    }
                }

                if (artists.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SearchSectionHeader("Artists", artists.size)
                    }
                    items(artists.take(3)) { artist ->
                        ArtistResultItem(
                            artist = artist,
                            onClick = { navController.navigate("artist/${artist.id}") }
                        )
                    }
                    if (artists.size > 3) {
                        item {
                            TextButton(
                                onClick = { /* TODO: Show all artists */ },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("See all ${artists.size} artists")
                            }
                        }
                    }
                }

                if (albums.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SearchSectionHeader("Albums", albums.size)
                    }
                    items(albums.take(3)) { album ->
                        AlbumResultItem(
                            album = album,
                            onClick = { navController.navigate("album/${album.id}") }
                        )
                    }
                    if (albums.size > 3) {
                        item {
                            TextButton(
                                onClick = { /* TODO: Show all albums */ },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("See all ${albums.size} albums")
                            }
                        }
                    }
                }

                // No results state
                if (songs.isEmpty() && artists.isEmpty() && albums.isEmpty()) {
                    item {
                        NoResultsState()
                    }
                }
            }

            SearchFilter.SONGS -> {
                if (songs.isNotEmpty()) {
                    item {
                        SearchSectionHeader(
                            title = "Songs",
                            count = songs.size,
                            onPlayAll = if (playerViewModel != null && songs.size > 1) {
                                { playerViewModel.playPlaylist(songs) }
                            } else null
                        )
                    }
                    items(songs) { song ->
                        SongItem(
                            song = song,
                            playerViewModel = playerViewModel,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    item { NoResultsState("No songs found") }
                }
            }

            SearchFilter.ARTISTS -> {
                if (artists.isNotEmpty()) {
                    item {
                        SearchSectionHeader("Artists", artists.size)
                    }
                    items(artists) { artist ->
                        ArtistResultItem(
                            artist = artist,
                            onClick = { navController.navigate("artist/${artist.id}") }
                        )
                    }
                } else {
                    item { NoResultsState("No artists found") }
                }
            }

            SearchFilter.ALBUMS -> {
                if (albums.isNotEmpty()) {
                    item {
                        SearchSectionHeader("Albums", albums.size)
                    }
                    items(albums) { album ->
                        AlbumResultItem(
                            album = album,
                            onClick = { navController.navigate("album/${album.id}") }
                        )
                    }
                } else {
                    item { NoResultsState("No albums found") }
                }
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(
    title: String,
    count: Int,
    onPlayAll: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "$count results",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (onPlayAll != null) {
            TextButton(onClick = onPlayAll) {
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

@Composable
private fun ArtistResultItem(
    artist: User,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Artist Avatar
            Card(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = artist.username.take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = artist.username,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (artist.isArtist) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Artist",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                artist.bio?.let { bio ->
                    Text(
                        text = bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "View Artist",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AlbumResultItem(
    album: Album,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Cover Placeholder
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
                        contentDescription = "Album",
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = album.creator.username,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${album.songs.size} songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "View Album",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NoResultsState(
    message: String = "No results found"
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Try different keywords or filters",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

enum class SearchFilter(val displayName: String) {
    ALL("All"),
    SONGS("Songs"),
    ARTISTS("Artists"),
    ALBUMS("Albums")
}