package com.sn4s.muza.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.sn4s.muza.data.model.Song
import com.sn4s.muza.player.MusicPlayerManager
import com.sn4s.muza.ui.viewmodels.PlayerController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    navController: NavController,
    viewModel: PlayerController = hiltViewModel()
) {
    val queue by viewModel.queue.collectAsState()
    val queueIndex by viewModel.queueIndex.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isShuffled by viewModel.isShuffled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val queueStats by viewModel.queueStats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    val listState = rememberLazyListState()
    var showClearDialog by remember { mutableStateOf(false) }

    // Auto-scroll to current song when queue changes
    LaunchedEffect(queueIndex) {
        if (queue.isNotEmpty() && queueIndex in queue.indices) {
            listState.animateScrollToItem(queueIndex)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Queue") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (queue.isNotEmpty()) {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear Queue")
                    }
                }
            }
        )

        // Connection State Indicator
        if (connectionState != MusicPlayerManager.ConnectionState.CONNECTED) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (connectionState) {
                        MusicPlayerManager.ConnectionState.CONNECTING,
                        MusicPlayerManager.ConnectionState.RECONNECTING -> MaterialTheme.colorScheme.primaryContainer
                        MusicPlayerManager.ConnectionState.FAILED -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (connectionState == MusicPlayerManager.ConnectionState.CONNECTING ||
                        connectionState == MusicPlayerManager.ConnectionState.RECONNECTING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = when (connectionState) {
                            MusicPlayerManager.ConnectionState.CONNECTING -> "Connecting to player..."
                            MusicPlayerManager.ConnectionState.RECONNECTING -> "Reconnecting..."
                            MusicPlayerManager.ConnectionState.FAILED -> "Player connection failed"
                            else -> "Player disconnected"
                        }
                    )
                }
            }
        }

        // Error Display
        error?.let { errorMessage ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
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
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.clearError() }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        if (queue.isEmpty()) {
            // Empty Queue State
            QueueEmptyState()
        } else {
            // Queue Header with Stats and Controls
            QueueHeader(
                queueStats = queueStats,
                isShuffled = isShuffled,
                repeatMode = repeatMode,
                onShuffleToggle = { viewModel.toggleShuffle() },
                onRepeatToggle = { viewModel.toggleRepeatMode() }
            )

            // Queue List
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(
                    items = queue,
                    key = { index, song -> "${song.id}_$index" }
                ) { index, song ->
                    QueueSongItem(
                        song = song,
                        index = index,
                        isCurrentSong = index == queueIndex,
                        onPlayClick = { viewModel.playFromQueue(index) },
                        onRemoveClick = { viewModel.removeFromQueue(index) },
                        isLoading = isLoading
                    )
                }

                // Bottom padding for mini player
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Clear Queue Confirmation Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Queue") },
            text = { Text("Are you sure you want to clear the entire queue? This will stop playback.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearQueue()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun QueueEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.QueueMusic,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No songs in queue",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Play a song or playlist to see it here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun QueueHeader(
    queueStats: PlayerController.QueueStats,
    isShuffled: Boolean,
    repeatMode: MusicPlayerManager.RepeatMode,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Playing from Queue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${queueStats.currentPosition} of ${queueStats.totalSongs} songs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${queueStats.remainingSongs} left",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (queueStats.totalDuration > 0) {
                        val totalMinutes = queueStats.totalDuration / 60
                        val hours = totalMinutes / 60
                        val minutes = totalMinutes % 60
                        val durationText = if (hours > 0) "$hours h $minutes min" else "$minutes min"
                        Text(
                            text = durationText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Shuffle Button
                FilterChip(
                    onClick = onShuffleToggle,
                    label = { Text("Shuffle") },
                    selected = isShuffled,
                    leadingIcon = {
                        Icon(
                            Icons.Default.Shuffle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                // Repeat Button
                FilterChip(
                    onClick = onRepeatToggle,
                    label = {
                        Text(when (repeatMode) {
                            MusicPlayerManager.RepeatMode.OFF -> "Repeat"
                            MusicPlayerManager.RepeatMode.ONE -> "Repeat 1"
                            MusicPlayerManager.RepeatMode.ALL -> "Repeat All"
                        })
                    },
                    selected = repeatMode != MusicPlayerManager.RepeatMode.OFF,
                    leadingIcon = {
                        Icon(
                            when (repeatMode) {
                                MusicPlayerManager.RepeatMode.ONE -> Icons.Default.RepeatOne
                                else -> Icons.Default.Repeat
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun QueueSongItem(
    song: Song,
    index: Int,
    isCurrentSong: Boolean,
    onPlayClick: () -> Unit,
    onRemoveClick: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading) { onPlayClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentSong) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Position indicator
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCurrentSong) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrentSong) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = "Now Playing",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Song info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCurrentSong) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.creator.username,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCurrentSong) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Duration
            song.duration?.let { duration ->
                val minutes = duration / 60
                val seconds = duration % 60
                Text(
                    text = String.format("%d:%02d", minutes, seconds),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCurrentSong) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Remove button
            IconButton(
                onClick = onRemoveClick,
                enabled = !isLoading
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove from queue",
                    modifier = Modifier.size(20.dp),
                    tint = if (isCurrentSong) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}