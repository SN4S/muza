package com.sn4s.muza.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sn4s.muza.ui.viewmodels.PlayerViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerScreen(
    navController: NavController,
    playerViewModel: PlayerViewModel
) {
    val currentSong by playerViewModel.currentSong.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val currentPosition by playerViewModel.currentPosition.collectAsState()
    val duration by playerViewModel.duration.collectAsState()
    val playlist by playerViewModel.playlist.collectAsState()
    val currentIndex by playerViewModel.currentIndex.collectAsState()

    if (currentSong == null) {
        LaunchedEffect(Unit) {
            navController.popBackStack()
        }
        return
    }

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

            IconButton(onClick = { /* TODO: More options */ }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More"
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Album Art Placeholder
        Card(
            modifier = Modifier
                .size(300.dp)
                .clip(MaterialTheme.shapes.large),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Album Art",
                    modifier = Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Song Info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = currentSong!!.title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currentSong!!.creator.username,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Progress Bar
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            var isDragging by remember { mutableStateOf(false) }
            var dragPosition by remember { mutableStateOf(0f) }

            val sliderPosition = if (isDragging) {
                dragPosition
            } else {
                if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
            }

            Slider(
                value = sliderPosition,
                onValueChange = { progress ->
                    isDragging = true
                    dragPosition = progress
                },
                onValueChangeFinished = {
                    val newPosition = (dragPosition * duration).toLong()
                    playerViewModel.seekTo(newPosition)
                    isDragging = false
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

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { playerViewModel.skipToPrevious() },
                enabled = playlist.size > 1
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    modifier = Modifier.size(32.dp)
                )
            }

            FloatingActionButton(
                onClick = { playerViewModel.togglePlayPause() },
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(
                onClick = { playerViewModel.skipToNext() },
                enabled = playlist.size > 1
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Queue info
        if (playlist.size > 1) {
            Text(
                text = "Playing from playlist (${currentIndex + 1} of ${playlist.size})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val seconds = (milliseconds / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}