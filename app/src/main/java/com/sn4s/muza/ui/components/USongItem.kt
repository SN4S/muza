package com.sn4s.muza.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sn4s.muza.data.model.Song
import com.sn4s.muza.data.repository.MusicRepository
import com.sn4s.muza.ui.viewmodels.LikeViewModel
import com.sn4s.muza.ui.viewmodels.PlayerController
import kotlinx.coroutines.launch

/**
 * Unified SongItem with cover art support
 */
@Composable
fun USongItem(
    song: Song,
    modifier: Modifier = Modifier,
    enableSwipeToQueue: Boolean = true,
    showMoreOptions: Boolean = true,
    // Collection context for smart playback
    collectionSongs: List<Song>? = null,
    playbackMode: PlaybackMode = PlaybackMode.SINGLE_SONG,
    // Controllers
    playerController: PlayerController = hiltViewModel(),
    likeViewModel: LikeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    // State management
    val likedSongs by likeViewModel.likedSongs.collectAsStateWithLifecycle()
    val isLiked = likedSongs.contains(song.id)
    val isLikeLoading by likeViewModel.isLoading.collectAsStateWithLifecycle()

    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // Swipe animation
    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    // Smart play action
    val onPlayClick = {
        when (playbackMode) {
            PlaybackMode.SINGLE_SONG -> playerController.playSong(song)
            PlaybackMode.FROM_COLLECTION -> {
                collectionSongs?.let { songs ->
                    playerController.playFromCollectionStartingAt(songs, song)
                } ?: playerController.playSong(song)
            }
            PlaybackMode.SHUFFLE_COLLECTION -> {
                collectionSongs?.let { songs ->
                    playerController.playShuffledFromCollection(songs, song)
                } ?: playerController.playSong(song)
            }
        }
    }

    val onLikeToggle = { likeViewModel.toggleLike(song.id) }

    val onAddToQueue = {
        playNext: Boolean -> playerController.addSongToQueue(song, playNext)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        coroutineScope.launch {
            offsetX.animateTo(0f, animationSpec = tween(300))
        }
    }

    // Swipe gesture handling
    val swipeModifier = if (enableSwipeToQueue) {
        Modifier.pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    coroutineScope.launch {
                        val threshold = 150.dp.toPx()
                        if (offsetX.value > threshold) {
                            onAddToQueue
                        } else {
                            offsetX.animateTo(0f, animationSpec = tween(300))
                        }
                    }
                }
            ) { _, dragAmount ->
                coroutineScope.launch {
                    val newValue = (offsetX.value + dragAmount).coerceAtLeast(0f)
                    offsetX.snapTo(newValue)
                }
            }
        }
    } else Modifier

    // Cover art URL
    val coverUrl = song.coverImage?.let {
        "${com.sn4s.muza.di.NetworkModule.BASE_URL}songs/${song.id}/cover"
    }

    Card(
        modifier = modifier
            .then(swipeModifier)
            .graphicsLayer { translationX = offsetX.value },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cover Art
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
                        contentDescription = "Song cover",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                        fallback = null, // Let the Box below handle fallback
                        error = null
                    )
                }

                // Fallback when no cover art
                if (coverUrl == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Song info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.creator.username,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Duration if available
                song.duration?.let { duration ->
                    val minutes = duration / 60
                    val seconds = duration % 60
                    Text(
                        text = "%d:%02d".format(minutes, seconds),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like button
                IconButton(
                    onClick = onLikeToggle,
                    enabled = !isLikeLoading
                ) {
                    if (isLikeLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isLiked) "Unlike" else "Like",
                            tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // More options
                if (showMoreOptions) {
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
                    }
                }

                // Play button
                IconButton(onClick = onPlayClick) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Show queue hint on swipe
        if (enableSwipeToQueue && offsetX.value > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = (offsetX.value / with(density) { 150.dp.toPx() }).coerceAtMost(1f)
                        )
                    )
            )
        }
    }

    // More options dropdown
    if (showMoreMenu) {
        DropdownMenu(
            expanded = showMoreMenu,
            onDismissRequest = { showMoreMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Add to Queue") },
                onClick = {
                    onAddToQueue
                    showMoreMenu = false
                },
                leadingIcon = { Icon(Icons.Default.Queue, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("Add to Playlist") },
                onClick = {
                    showAddToPlaylistDialog = true
                    showMoreMenu = false
                },
                leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("Share") },
                onClick = {
                    // TODO: Implement share
                    showMoreMenu = false
                },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
            )
        }
    }

    // Add to playlist dialog
    if (showAddToPlaylistDialog) {
        AddToPlaylistDialog(
            songId = song.id,
            onDismiss = { showAddToPlaylistDialog = false }
        )
    }
}

enum class PlaybackMode {
    SINGLE_SONG,           // Just play this song
    FROM_COLLECTION,       // Play from collection starting at this song
    SHUFFLE_COLLECTION     // Shuffle collection starting with this song
}

// Backward compatibility wrapper
@Composable
fun SongItem(
    song: Song,
    onPlayClick: ((Song) -> Unit)? = null,
    playerViewModel: Any? = null, // Deprecated parameter
    modifier: Modifier = Modifier,
    enableSwipeToQueue: Boolean = true
) {
    // Use unified implementation regardless of old parameters
    USongItem(
        song = song,
        modifier = modifier,
        enableSwipeToQueue = enableSwipeToQueue
    )
}