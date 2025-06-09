package com.sn4s.muza.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sn4s.muza.data.model.Song
import com.sn4s.muza.ui.viewmodels.LikeViewModel
import com.sn4s.muza.ui.viewmodels.PlayerController
import kotlinx.coroutines.launch

/**
 * Unified SongItem with single implementation for all play actions
 * No more duplicate implementations or callback confusion
 */
@Composable
fun USongItem(
    song: Song,
    modifier: Modifier = Modifier,
    enableSwipeToQueue: Boolean = true,
    showMoreOptions: Boolean = true,
    // NEW: Collection context for smart playback
    collectionSongs: List<Song>? = null, // All songs in current playlist/album/liked
    playbackMode: PlaybackMode = PlaybackMode.SINGLE_SONG,
    // Unified controller - single source of truth
    playerController: PlayerController = hiltViewModel(),
    likeViewModel: LikeViewModel = hiltViewModel()
) {
    // State management
    val likedSongs by likeViewModel.likedSongs.collectAsStateWithLifecycle()
    val isLiked = likedSongs.contains(song.id)
    val isLikeLoading by likeViewModel.isLoading.collectAsStateWithLifecycle()

    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // Smart play action based on context
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
    val onAddToQueue = { playNext: Boolean -> playerController.addSongToQueue(song, playNext) }

    if (enableSwipeToQueue) {
        SwipeToQueueSongItem(
            song = song,
            isLiked = isLiked,
            isLikeLoading = isLikeLoading,
            onPlayClick = onPlayClick,
            onLikeToggle = onLikeToggle,
            onAddToPlaylist = { showAddToPlaylistDialog = true },
            onMoreClick = if (showMoreOptions) {{ showMoreMenu = true }} else null,
            onAddToQueue = onAddToQueue,
            modifier = modifier
        )
    } else {
        StandardSongItem(
            song = song,
            isLiked = isLiked,
            isLikeLoading = isLikeLoading,
            onPlayClick = onPlayClick,
            onLikeToggle = onLikeToggle,
            onAddToPlaylist = { showAddToPlaylistDialog = true },
            onMoreClick = if (showMoreOptions) {{ showMoreMenu = true }} else null,
            modifier = modifier
        )
    }

    // Dialogs
    if (showAddToPlaylistDialog) {
        AddToPlaylistDialog(
            songId = song.id,
            onDismiss = { showAddToPlaylistDialog = false }
        )
    }

    if (showMoreMenu) {
        UnifiedMoreOptionsMenu(
            song = song,
            playerController = playerController,
            onDismiss = { showMoreMenu = false }
        )
    }
}

@Composable
private fun SwipeToQueueSongItem(
    song: Song,
    isLiked: Boolean,
    isLikeLoading: Boolean,
    onPlayClick: () -> Unit,
    onLikeToggle: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onMoreClick: (() -> Unit)?,
    onAddToQueue: (playNext: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Swipe state
    val offsetX = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var hasTriggeredAction by remember { mutableStateOf(false) }

    // Thresholds
    val swipeThreshold = with(density) { 80.dp.toPx() }
    val maxSwipe = with(density) { 120.dp.toPx() }
    val hapticThreshold = with(density) { 60.dp.toPx() }

    fun resetSwipe() {
        scope.launch {
            offsetX.animateTo(0f, animationSpec = tween(300))
            hasTriggeredAction = false
        }
    }

    fun triggerAddToQueue() {
        if (!hasTriggeredAction) {
            hasTriggeredAction = true
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

            val playNext = offsetX.value > swipeThreshold * 1.5f
            onAddToQueue(playNext)

            scope.launch {
                kotlinx.coroutines.delay(300)
                resetSwipe()
            }
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        // Background indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(
                    when {
                        offsetX.value > swipeThreshold * 1.5f -> MaterialTheme.colorScheme.primaryContainer
                        offsetX.value > swipeThreshold -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    }
                ),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (offsetX.value > hapticThreshold) {
                Icon(
                    imageVector = if (offsetX.value > swipeThreshold * 1.5f) Icons.Default.SkipNext else Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
        }

        // Main song item
        StandardSongItem(
            song = song,
            isLiked = isLiked,
            isLikeLoading = isLikeLoading,
            onPlayClick = onPlayClick,
            onLikeToggle = onLikeToggle,
            onAddToPlaylist = onAddToPlaylist,
            onMoreClick = onMoreClick,
            modifier = Modifier
                .graphicsLayer { translationX = offsetX.value }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = {
                            isDragging = false
                            if (offsetX.value > swipeThreshold) {
                                triggerAddToQueue()
                            } else {
                                resetSwipe()
                            }
                        }
                    ) { _, dragAmount ->
                        scope.launch {
                            val newValue = (offsetX.value + dragAmount).coerceIn(0f, maxSwipe)
                            offsetX.snapTo(newValue)

                            if (newValue > hapticThreshold && !hasTriggeredAction && !isDragging) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    }
                }
        )
    }
}

@Composable
private fun StandardSongItem(
    song: Song,
    isLiked: Boolean,
    isLikeLoading: Boolean,
    onPlayClick: () -> Unit,
    onLikeToggle: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onMoreClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onPlayClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Song info
            Column(
                modifier = Modifier.weight(1f)
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
                onMoreClick?.let { onClick ->
                    IconButton(onClick = onClick) {
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
                        contentDescription = "Play"
                    )
                }
            }
        }
    }
}

@Composable
private fun UnifiedMoreOptionsMenu(
    song: Song,
    playerController: PlayerController = hiltViewModel(),
    onDismiss: () -> Unit
) {
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("Add to queue") },
            onClick = {
                playerController.addSongToQueue(song, playNext = false)
                onDismiss()
            },
            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
        )

        DropdownMenuItem(
            text = { Text("Play next") },
            onClick = {
                playerController.addSongToQueue(song, playNext = true)
                onDismiss()
            },
            leadingIcon = { Icon(Icons.Default.SkipNext, contentDescription = null) }
        )

        HorizontalDivider()

        DropdownMenuItem(
            text = { Text("Add to playlist") },
            onClick = {
                showAddToPlaylistDialog = true
                onDismiss()
            },
            leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) }
        )

        DropdownMenuItem(
            text = { Text("View artist") },
            onClick = {
                // TODO: Navigate to artist profile
                onDismiss()
            },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
        )
    }

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

//backward compatibility
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