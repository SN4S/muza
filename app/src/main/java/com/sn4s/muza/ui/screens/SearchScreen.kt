package com.sn4s.muza.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.sn4s.muza.data.model.Album
import com.sn4s.muza.data.model.Song
import com.sn4s.muza.data.model.User
import com.sn4s.muza.ui.components.PlaybackMode
import com.sn4s.muza.ui.components.USongItem
import com.sn4s.muza.ui.components.UserAvatar
import com.sn4s.muza.ui.viewmodels.PlayerController
import com.sn4s.muza.ui.viewmodels.SearchViewModel

enum class SearchFilter {
    ALL, SONGS, ARTISTS, ALBUMS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    playerController: PlayerController = hiltViewModel(),
    viewModel: SearchViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf(SearchFilter.ALL) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Bar
        TopAppBar(
            title = {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    placeholder = { Text("Search songs, artists, albums...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearSearch() }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            viewModel.search()
                        }
                    )
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        // Filter Tabs
        if (searchQuery.isNotEmpty()) {
            ScrollableTabRow(
                selectedTabIndex = selectedFilter.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                SearchFilter.values().forEach { filter ->
                    val count = when (filter) {
                        SearchFilter.ALL -> songs.size + artists.size + albums.size
                        SearchFilter.SONGS -> songs.size
                        SearchFilter.ARTISTS -> artists.size
                        SearchFilter.ALBUMS -> albums.size
                    }

                    Tab(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        text = {
                            Text(
                                text = if (count > 0) "${filter.name.lowercase().capitalize()} ($count)"
                                else filter.name.lowercase().capitalize()
                            )
                        }
                    )
                }
            }
        }

        // Content
        when {
            searchQuery.isEmpty() -> {
                SearchEmptyState()
            }
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
                    Button(onClick = { viewModel.search() }) {
                        Text("Retry")
                    }
                }
            }
            else -> {
                SearchResults(
                    filter = selectedFilter,
                    songs = songs,
                    artists = artists,
                    albums = albums,
                    playerController = playerController,
                    navController = navController
                )
            }
        }
    }
}

@Composable
private fun SearchResults(
    filter: SearchFilter,
    songs: List<Song>,
    artists: List<User>,
    albums: List<Album>,
    playerController: PlayerController,
    navController: NavController
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        when (filter) {
            SearchFilter.ALL -> {
                // Show all results with sections
                if (songs.isNotEmpty()) {
                    item {
                        SearchSectionHeader(
                            title = "Songs",
                            count = songs.size,
                            onPlayAll = if (songs.size > 1) {
                                { playerController.playPlaylist(songs) }
                            } else null
                        )
                    }
                    items(songs.take(5)) { song -> // Show only first 5 in ALL view
                        USongItem(
                            song = song,
                            playbackMode = PlaybackMode.SINGLE_SONG, // Individual songs in search
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    if (songs.size > 5) {
                        item {
                            TextButton(
                                onClick = { /* Switch to SONGS filter */ },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Text("Show all ${songs.size} songs")
                            }
                        }
                    }
                }

                if (artists.isNotEmpty()) {
                    item {
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
                                onClick = { /* Switch to ARTISTS filter */ },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Text("Show all ${artists.size} artists")
                            }
                        }
                    }
                }

                if (albums.isNotEmpty()) {
                    item {
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
                                onClick = { /* Switch to ALBUMS filter */ },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Text("Show all ${albums.size} albums")
                            }
                        }
                    }
                }

                if (songs.isEmpty() && artists.isEmpty() && albums.isEmpty()) {
                    item { NoResultsState("No results found") }
                }
            }

            SearchFilter.SONGS -> {
                if (songs.isNotEmpty()) {
                    item {
                        SearchSectionHeader(
                            title = "Songs",
                            count = songs.size,
                            onPlayAll = if (songs.size > 1) {
                                { playerController.playPlaylist(songs) }
                            } else null
                        )
                    }
                    items(songs) { song ->
                        USongItem(
                            song = song,
                            playbackMode = PlaybackMode.SINGLE_SONG,
                            modifier = Modifier.padding(horizontal = 16.dp)
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // FIXED: Use UserAvatar component
            UserAvatar(
                user = artist,
                size = 56.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artist.username,
                    style = MaterialTheme.typography.titleMedium
                )
                if (artist.isArtist) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Artist",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (!artist.bio.isNullOrBlank()) {
                    Text(
                        text = artist.bio!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
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
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album cover placeholder
            Card(
                modifier = Modifier.size(48.dp),
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

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = album.creator.username,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${album.songs.size} songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
private fun NoResultsState(message: String) {
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
            text = "Try adjusting your search terms",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}