package com.sn4s.muza.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sn4s.muza.data.model.*
import com.sn4s.muza.di.NetworkModule
import com.sn4s.muza.ui.components.USongItem
import com.sn4s.muza.ui.components.UserAvatar
import com.sn4s.muza.ui.viewmodels.ArtistProfileViewModel
import com.sn4s.muza.ui.viewmodels.PlayerController
import com.sn4s.muza.ui.viewmodels.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistProfileScreen(
    navController: NavController,
    artistId: Int,
    playerViewModel: PlayerController? = null,
    viewModel: ArtistProfileViewModel = hiltViewModel()
) {
    // Handle unauthorized like your other screens
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

    // Existing states
    val artist by viewModel.artist.collectAsState()
    val artistSongs by viewModel.artistSongs.collectAsState()
    val artistAlbums by viewModel.artistAlbums.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // New social states
    val userProfile by viewModel.userProfile.collectAsState()
    val followStatus by viewModel.followStatus.collectAsState()
    val hasSocialFeatures by viewModel.hasSocialFeatures.collectAsState()

    LaunchedEffect(artistId) {
        viewModel.loadArtist(artistId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(userProfile?.username ?: artist?.username ?: "Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            error != null && artist == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadArtist(artistId) }) {
                        Text("Retry")
                    }
                }
            }

            artist != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Header - Use enhanced version if available, fallback to basic
                    item {
                        if (hasSocialFeatures && userProfile != null) {
                            // Enhanced profile with social features
                            EnhancedProfileCard(
                                profile = userProfile!!,
                                followStatus = followStatus,
                                onFollowClick = { viewModel.toggleFollow(artistId) }
                            )
                        } else {
                            // Basic profile (fallback for when social features aren't available)
                            BasicProfileCard(artist = artist!!)
                        }
                    }

                    // Content Tabs (Songs/Albums for artists)
                    if (artist!!.isArtist && (artistSongs.isNotEmpty() || artistAlbums.isNotEmpty())) {
                        item {
                            var selectedTab by remember { mutableIntStateOf(0) }
                            val tabs = listOf("Songs", "Albums")

                            Column {
                                TabRow(selectedTabIndex = selectedTab) {
                                    tabs.forEachIndexed { index, title ->
                                        Tab(
                                            selected = selectedTab == index,
                                            onClick = { selectedTab = index },
                                            text = { Text(title) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                when (selectedTab) {
                                    0 -> {
                                        // Songs content
                                        if (artistSongs.isNotEmpty()) {
                                            Column {
                                                artistSongs.forEach { song ->
                                                    USongItem(
                                                        song = song,
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                }
                                            }
                                        } else {
                                            Text(
                                                "No songs yet",
                                                modifier = Modifier.padding(16.dp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                    1 -> {
                                        // Albums content
                                        if (artistAlbums.isNotEmpty()) {
                                            Column {
                                                artistAlbums.forEach { album ->
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp),
                                                        onClick = { navController.navigate("album/${album.id}") }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(16.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Album,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(48.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(16.dp))
                                                            Column {
                                                                Text(
                                                                    text = album.title,
                                                                    style = MaterialTheme.typography.titleMedium
                                                                )
                                                                Text(
                                                                    text = "${album.songs.size} songs",
                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                }
                                            }
                                        } else {
                                            Text(
                                                "No albums yet",
                                                modifier = Modifier.padding(16.dp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (artistSongs.isNotEmpty()) {
                        // Non-artist with songs - just show songs list
                        items(artistSongs) { song ->
                            USongItem(
                                song = song
                            )
                        }
                    } else {
                        item {
                            Text(
                                "No content available",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

// Enhanced profile card with social features
@Composable
fun EnhancedProfileCard(
    profile: UserProfile,
    followStatus: FollowResponse?,
    onFollowClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
//            Card(
//                modifier = Modifier.size(120.dp),
//                shape = CircleShape,
//                colors = CardDefaults.cardColors(
//                    containerColor = MaterialTheme.colorScheme.surfaceVariant
//                )
//            ) {
//                Box(
//                    modifier = Modifier.fillMaxSize(),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.Person,
//                        contentDescription = "Avatar",
//                        modifier = Modifier.size(60.dp),
//                        tint = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//            }

            UserAvatar(
                userId = profile.id,
                username = profile.username,
                imageUrl = profile.image,
                size = 120.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = profile.username,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (profile.isArtist) {
                Spacer(modifier = Modifier.height(8.dp))
                AssistChip(
                    onClick = { },
                    label = { Text("Artist") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            profile.bio?.let { bio ->
                if (bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = bio,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats row
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                StatColumn(
                    count = profile.songCount,
                    label = "Songs"
                )
                StatColumn(
                    count = profile.followerCount,
                    label = "Followers"
                )
                StatColumn(
                    count = profile.followingCount,
                    label = "Following"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Follow button
            followStatus?.let { status ->
                Button(
                    onClick = onFollowClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (status.isFollowing) Icons.Default.PersonRemove else Icons.Default.PersonAdd,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (status.isFollowing) "Unfollow" else "Follow")
                }
            }
        }
    }
}

// Basic profile card (fallback when social features not available)
@Composable
fun BasicProfileCard(artist: UserNested) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
//            Card(
//                modifier = Modifier.size(120.dp),
//                shape = CircleShape,
//                colors = CardDefaults.cardColors(
//                    containerColor = MaterialTheme.colorScheme.surfaceVariant
//                )
//            ) {
//                Box(
//                    modifier = Modifier.fillMaxSize(),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.Person,
//                        contentDescription = "Avatar",
//                        modifier = Modifier.size(60.dp),
//                        tint = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//            }

            UserAvatar(
                userNested = artist,
                size = 120.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = artist.username,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (artist.isArtist) {
                Spacer(modifier = Modifier.height(8.dp))
                AssistChip(
                    onClick = { },
                    label = { Text("Artist") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            artist.bio?.let { bio ->
                if (bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = bio,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun StatColumn(
    count: Int,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}