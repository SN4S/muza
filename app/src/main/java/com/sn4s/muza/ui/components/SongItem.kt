package com.sn4s.muza.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sn4s.muza.data.model.Song
import com.sn4s.muza.ui.viewmodels.LikeViewModel
import com.sn4s.muza.ui.viewmodels.PlayerViewModel
import androidx.compose.material.icons.filled.PlaylistAdd
import com.sn4s.muza.ui.components.AddToPlaylistDialog

@Composable
fun SongItem(
    song: Song,
    onPlayClick: ((Song) -> Unit)? = null,
    playerViewModel: PlayerViewModel? = null,
    likeViewModel: LikeViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val likedSongs by likeViewModel.likedSongs.collectAsState()
    val isLiked = likedSongs.contains(song.id)
    val isLikeLoading by likeViewModel.isLoading.collectAsState()

    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    // Always check like status when song changes
    LaunchedEffect(song.id) {
        likeViewModel.checkIfLiked(song.id)
    }

    Card(
        modifier = modifier.fillMaxWidth()
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
                // Add to Playlist Button
                IconButton(
                    onClick = { showAddToPlaylistDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistAdd,
                        contentDescription = "Add to Playlist",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Like Button
                IconButton(
                    onClick = { likeViewModel.toggleLike(song.id) },
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
                if (playerViewModel != null || onPlayClick != null) {
                    IconButton(
                        onClick = {
                            onPlayClick?.invoke(song) ?: playerViewModel?.playSong(song)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play"
                        )
                    }
                }
            }
        }
    }

    // Add to Playlist Dialog
    if (showAddToPlaylistDialog) {
        AddToPlaylistDialog(
            songId = song.id,
            onDismiss = { showAddToPlaylistDialog = false }
        )
    }
}