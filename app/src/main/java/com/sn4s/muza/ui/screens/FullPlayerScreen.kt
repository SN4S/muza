package com.sn4s.muza.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.sn4s.muza.player.MusicPlayerManager
import com.sn4s.muza.ui.viewmodels.PlayerController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerScreen(
    navController: NavController,
    playerController: PlayerController = hiltViewModel()
) {
    val currentSong by playerController.currentSong.collectAsStateWithLifecycle()
    val isPlaying by playerController.isPlaying.collectAsStateWithLifecycle()
    val currentPosition by playerController.currentPosition.collectAsStateWithLifecycle()
    val duration by playerController.duration.collectAsStateWithLifecycle()
    val queue by playerController.queue.collectAsStateWithLifecycle()
    val queueIndex by playerController.queueIndex.collectAsStateWithLifecycle()
    val isShuffled by playerController.isShuffled.collectAsStateWithLifecycle()
    val repeatMode by playerController.repeatMode.collectAsStateWithLifecycle()

    // Auto-close if no song is playing
    if (currentSong == null) {
        LaunchedEffect(Unit) {
            navController.popBackStack()
        }
        return
    }

    // Seek state for smooth dragging
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Minimize"
                )
            }

            Text(
                text = "Now Playing",
                style = MaterialTheme.typography.titleMedium
            )

            IconButton(onClick = { navController.navigate("queue") }) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = "Queue"
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Song info
        currentSong?.let { song ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = song.creator.username,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Progress bar
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            val progress = if (duration > 0) {
                if (isDragging) dragPosition else (currentPosition.toFloat() / duration.toFloat())
            } else 0f

            Slider(
                value = progress,
                onValueChange = { newValue ->
                    isDragging = true
                    dragPosition = newValue
                },
                onValueChangeFinished = {
                    isDragging = false
                    playerController.seekTo((dragPosition * duration).toLong())
                },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(if (isDragging) (dragPosition * duration).toLong() else currentPosition),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTime(duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Main controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { playerController.skipToPrevious() },
                enabled = queue.size > 1
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    modifier = Modifier.size(32.dp)
                )
            }

            FloatingActionButton(
                onClick = { playerController.togglePlayPause() },
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(
                onClick = { playerController.skipToNext() },
                enabled = queue.size > 1
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Secondary controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { playerController.toggleShuffle() }) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { playerController.toggleRepeatMode() }) {
                Icon(
                    imageVector = when (repeatMode) {
                        MusicPlayerManager.RepeatMode.ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    },
                    contentDescription = when (repeatMode) {
                        MusicPlayerManager.RepeatMode.OFF -> "Repeat off"
                        MusicPlayerManager.RepeatMode.ONE -> "Repeat one"
                        MusicPlayerManager.RepeatMode.ALL -> "Repeat all"
                    },
                    tint = if (repeatMode != MusicPlayerManager.RepeatMode.OFF) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Queue info
        if (queue.size > 1) {
            Text(
                text = "Playing from queue (${queueIndex + 1} of ${queue.size})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

private fun formatTime(milliseconds: Long): String {
    val seconds = (milliseconds / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}