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
import com.sn4s.muza.data.model.Song
import com.sn4s.muza.ui.viewmodels.LikeViewModel
import com.sn4s.muza.ui.viewmodels.PlayerViewModel
import com.sn4s.muza.ui.viewmodels.QueueViewModel
import kotlinx.coroutines.launch

@Composable
fun SongItem(
    song: Song,
    onPlayClick: ((Song) -> Unit)? = null,
    playerViewModel: PlayerViewModel? = null,
    likeViewModel: LikeViewModel = hiltViewModel(),
    queueViewModel: QueueViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    enableSwipeToQueue: Boolean = true
) {
    val likedSongs by likeViewModel.likedSongs.collectAsState()
    val isLiked = likedSongs.contains(song.id)
    val isLikeLoading by likeViewModel.isLoading.collectAsState()

    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // Always check like status when song changes
//    LaunchedEffect(song.id) {
//        likeViewModel.checkIfLiked(song.id)
//    }

    if (enableSwipeToQueue) {
        SwipeToQueueSongItem(
            song = song,
            isLiked = isLiked,
            isLikeLoading = isLikeLoading,
            onPlayClick = { onPlayClick?.invoke(song) ?: playerViewModel?.playSong(song) },
            onLikeToggle = { likeViewModel.toggleLike(song.id) },
            onAddToPlaylist = { showAddToPlaylistDialog = true },
            onMoreClick = { showMoreMenu = true },
            onAddToQueue = { playNext ->
                queueViewModel.addSongToQueue(song, playNext)
            },
            modifier = modifier
        )
    } else {
        RegularSongItem(
            song = song,
            isLiked = isLiked,
            isLikeLoading = isLikeLoading,
            onPlayClick = { onPlayClick?.invoke(song) ?: playerViewModel?.playSong(song) },
            onLikeToggle = { likeViewModel.toggleLike(song.id) },
            onAddToPlaylist = { showAddToPlaylistDialog = true },
            onMoreClick = { showMoreMenu = true },
            modifier = modifier
        )
    }

    // Add to Playlist Dialog
    if (showAddToPlaylistDialog) {
        AddToPlaylistDialog(
            songId = song.id,
            onDismiss = { showAddToPlaylistDialog = false }
        )
    }

    // More options menu
    if (showMoreMenu) {
        MoreOptionsMenu(
            song = song,
            queueViewModel = queueViewModel,
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
    onMoreClick: () -> Unit,
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

    // Reset function
    fun resetSwipe() {
        scope.launch {
            offsetX.animateTo(0f, animationSpec = tween(300))
            hasTriggeredAction = false
        }
    }

    // Trigger action function
    fun triggerAddToQueue() {
        if (!hasTriggeredAction) {
            hasTriggeredAction = true
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

            // Determine action based on swipe distance
            val playNext = offsetX.value > swipeThreshold * 1.5f
            onAddToQueue(playNext)

            // Reset after a delay
            scope.launch {
                kotlinx.coroutines.delay(300)
                resetSwipe()
            }
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        // Background layer (revealed when swiping)
        SwipeBackground(
            offsetX = offsetX.value,
            maxSwipe = maxSwipe,
            swipeThreshold = swipeThreshold,
            song = song
        )

        // Foreground content (the actual song item)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationX = offsetX.value
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isDragging = true
                            hasTriggeredAction = false
                        },
                        onDragEnd = {
                            isDragging = false
                            when {
                                offsetX.value >= swipeThreshold -> {
                                    triggerAddToQueue()
                                }
                                else -> {
                                    resetSwipe()
                                }
                            }
                        }
                    ) { _, dragAmount ->
                        scope.launch {
                            val newValue = (offsetX.value + dragAmount).coerceIn(0f, maxSwipe)
                            offsetX.snapTo(newValue)

                            // Haptic feedback at threshold
                            if (!hasTriggeredAction && newValue >= hapticThreshold && offsetX.value < hapticThreshold) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    }
                }
        ) {
            SongItemContent(
                song = song,
                isLiked = isLiked,
                isLikeLoading = isLikeLoading,
                onPlayClick = onPlayClick,
                onLikeToggle = onLikeToggle,
                onAddToPlaylist = onAddToPlaylist,
                onMoreClick = onMoreClick
            )
        }
    }
}

@Composable
private fun SwipeBackground(
    offsetX: Float,
    maxSwipe: Float,
    swipeThreshold: Float,
    song: Song
) {
    val density = LocalDensity.current
    val backgroundColor = when {
        offsetX >= swipeThreshold * 1.5f -> MaterialTheme.colorScheme.tertiary
        offsetX >= swipeThreshold -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    val iconColor = when {
        offsetX >= swipeThreshold -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(with(density) { 80.dp }) // Match card height
            .background(backgroundColor)
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon changes based on swipe distance
            Icon(
                imageVector = when {
                    offsetX >= swipeThreshold * 1.5f -> Icons.Default.SkipNext
                    else -> Icons.Default.Add
                },
                contentDescription = when {
                    offsetX >= swipeThreshold * 1.5f -> "Play Next"
                    else -> "Add to Queue"
                },
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )

            if (offsetX >= swipeThreshold * 0.7f) {
                Column {
                    Text(
                        text = when {
                            offsetX >= swipeThreshold * 1.5f -> "Play Next"
                            else -> "Add to Queue"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = iconColor
                    )
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = iconColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun RegularSongItem(
    song: Song,
    isLiked: Boolean,
    isLikeLoading: Boolean,
    onPlayClick: () -> Unit,
    onLikeToggle: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        SongItemContent(
            song = song,
            isLiked = isLiked,
            isLikeLoading = isLikeLoading,
            onPlayClick = onPlayClick,
            onLikeToggle = onLikeToggle,
            onAddToPlaylist = onAddToPlaylist,
            onMoreClick = onMoreClick
        )
    }
}

@Composable
private fun SongItemContent(
    song: Song,
    isLiked: Boolean,
    isLikeLoading: Boolean,
    onPlayClick: () -> Unit,
    onLikeToggle: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Song Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
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
            if (song.likeCount > 0) {
                Text(
                    text = "${song.likeCount} likes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Controls
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // More options
            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Like Button
            IconButton(
                onClick = onLikeToggle,
                enabled = !isLikeLoading
            ) {
                if (isLikeLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
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

            // Play Button
            IconButton(onClick = onPlayClick) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play"
                )
            }
        }
    }
}

@Composable
private fun MoreOptionsMenu(
    song: Song,
    queueViewModel: QueueViewModel,
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
                queueViewModel.addSongToQueue(song, playNext = false)
                onDismiss()
            },
            leadingIcon = {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        )

        DropdownMenuItem(
            text = { Text("Play next") },
            onClick = {
                queueViewModel.addSongToQueue(song, playNext = true)
                onDismiss()
            },
            leadingIcon = {
                Icon(Icons.Default.SkipNext, contentDescription = null)
            }
        )

        Divider()

        DropdownMenuItem(
            text = { Text("Add to playlist") },
            onClick = {
                showAddToPlaylistDialog = true
                onDismiss()
            },
            leadingIcon = {
                Icon(Icons.Default.PlaylistAdd, contentDescription = null)
            }
        )

        DropdownMenuItem(
            text = { Text("View artist") },
            onClick = {
                // TODO: Navigate to artist
                onDismiss()
            },
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null)
            }
        )
    }

    if (showAddToPlaylistDialog) {
        AddToPlaylistDialog(
            songId = song.id,
            onDismiss = { showAddToPlaylistDialog = false }
        )
    }
}

// Extension functions for better UX feedback
@Composable
fun SongItemWithSwipeHint(
    song: Song,
    onPlayClick: ((Song) -> Unit)? = null,
    playerViewModel: PlayerViewModel? = null,
    likeViewModel: LikeViewModel = hiltViewModel(),
    queueViewModel: QueueViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    showSwipeHint: Boolean = false
) {
    Box(modifier = modifier) {
        SongItem(
            song = song,
            onPlayClick = onPlayClick,
            playerViewModel = playerViewModel,
            likeViewModel = likeViewModel,
            queueViewModel = queueViewModel,
            enableSwipeToQueue = true
        )

        // Swipe hint overlay
        if (showSwipeHint) {
            SwipeHintOverlay()
        }
    }
}

@Composable
private fun SwipeHintOverlay() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.SwipeRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Swipe right to add to queue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}