package com.sn4s.muza.ui.viewmodels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sn4s.muza.data.model.Song
import com.sn4s.muza.player.MusicPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerManager: MusicPlayerManager
) : ViewModel() {

    // Direct access to player manager state
    val currentSong: StateFlow<Song?> = playerManager.currentSong
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val playbackState: StateFlow<Int> = playerManager.playbackState
    val currentPosition: StateFlow<Long> = playerManager.currentPosition
    val duration: StateFlow<Long> = playerManager.duration
    val connectionState: StateFlow<MusicPlayerManager.ConnectionState> = playerManager.connectionState

    // Queue state (legacy compatibility)
    val playlist: StateFlow<List<Song>> = playerManager.queue
    val currentIndex: StateFlow<Int> = playerManager.queueIndex

    // Advanced queue features
    val queue: StateFlow<List<Song>> = playerManager.queue
    val queueIndex: StateFlow<Int> = playerManager.queueIndex
    val isShuffled: StateFlow<Boolean> = playerManager.isShuffled
    val repeatMode: StateFlow<MusicPlayerManager.RepeatMode> = playerManager.repeatMode

    // UI state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Computed properties for UI
    val hasNextSong: StateFlow<Boolean> = combine(queue, queueIndex, repeatMode) { songs, index, repeat ->
        when (repeat) {
            MusicPlayerManager.RepeatMode.ALL -> songs.isNotEmpty()
            MusicPlayerManager.RepeatMode.ONE -> true
            MusicPlayerManager.RepeatMode.OFF -> index < songs.size - 1
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val hasPreviousSong: StateFlow<Boolean> = combine(queue, queueIndex, repeatMode) { songs, index, repeat ->
        when (repeat) {
            MusicPlayerManager.RepeatMode.ALL -> songs.isNotEmpty()
            MusicPlayerManager.RepeatMode.ONE -> true
            MusicPlayerManager.RepeatMode.OFF -> index > 0
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val progressPercentage: StateFlow<Float> = combine(currentPosition, duration) { position, totalDuration ->
        if (totalDuration > 0) {
            (position.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0f
    )

    // Basic playback controls
    fun playSong(song: Song) {
        viewModelScope.launch {
            try {
                _error.value = null
                playerManager.playSong(song)
            } catch (e: Exception) {
                _error.value = "Failed to play song: ${e.message}"
            }
        }
    }

    fun playPlaylist(songs: List<Song>, startIndex: Int = 0) {
        viewModelScope.launch {
            try {
                _error.value = null
                _isLoading.value = true
                playerManager.playPlaylist(songs, startIndex)
            } catch (e: Exception) {
                _error.value = "Failed to play playlist: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun play() {
        viewModelScope.launch {
            try {
                _error.value = null
                playerManager.play()
            } catch (e: Exception) {
                _error.value = "Failed to resume playback: ${e.message}"
            }
        }
    }

    fun pause() {
        viewModelScope.launch {
            try {
                _error.value = null
                playerManager.pause()
            } catch (e: Exception) {
                _error.value = "Failed to pause playback: ${e.message}"
            }
        }
    }

    fun togglePlayPause() {
        viewModelScope.launch {
            try {
                _error.value = null
                playerManager.togglePlayPause()
            } catch (e: Exception) {
                _error.value = "Failed to toggle playback: ${e.message}"
            }
        }
    }

    fun seekTo(position: Long) {
        viewModelScope.launch {
            try {
                _error.value = null
                playerManager.seekTo(position)
            } catch (e: Exception) {
                _error.value = "Failed to seek: ${e.message}"
            }
        }
    }

    fun skipToNext() {
        viewModelScope.launch {
            try {
                _error.value = null
                playerManager.skipToNext()
            } catch (e: Exception) {
                _error.value = "Failed to skip to next: ${e.message}"
            }
        }
    }

    fun skipToPrevious() {
        viewModelScope.launch {
            try {
                _error.value = null
                playerManager.skipToPrevious()
            } catch (e: Exception) {
                _error.value = "Failed to skip to previous: ${e.message}"
            }
        }
    }

    fun seekToIndex(index: Int) {
        viewModelScope.launch {
            try {
                _error.value = null
                playerManager.seekToIndex(index)
            } catch (e: Exception) {
                _error.value = "Failed to play song at index $index: ${e.message}"
            }
        }
    }

    fun stop() {
        viewModelScope.launch {
            try {
                _error.value = null
                playerManager.stop()
            } catch (e: Exception) {
                _error.value = "Failed to stop playback: ${e.message}"
            }
        }
    }

    // Queue management (for backward compatibility and convenience)
    fun addToQueue(songs: List<Song>, playNext: Boolean = false) {
        viewModelScope.launch {
            try {
                _error.value = null
                playerManager.addToQueue(songs, playNext)
            } catch (e: Exception) {
                _error.value = "Failed to add to queue: ${e.message}"
            }
        }
    }

    fun addSongToQueue(song: Song, playNext: Boolean = false) {
        addToQueue(listOf(song), playNext)
    }

    fun removeFromQueue(index: Int) {
        viewModelScope.launch {
            try {
                _error.value = null
                playerManager.removeFromQueue(index)
            } catch (e: Exception) {
                _error.value = "Failed to remove from queue: ${e.message}"
            }
        }
    }

    fun clearQueue() {
        viewModelScope.launch {
            try {
                _error.value = null
                playerManager.clearQueue()
            } catch (e: Exception) {
                _error.value = "Failed to clear queue: ${e.message}"
            }
        }
    }

    // Advanced features
    fun toggleShuffle() {
        viewModelScope.launch {
            try {
                _error.value = null
                playerManager.toggleShuffle()
            } catch (e: Exception) {
                _error.value = "Failed to toggle shuffle: ${e.message}"
            }
        }
    }

    fun toggleRepeatMode() {
        viewModelScope.launch {
            try {
                _error.value = null
                playerManager.toggleRepeatMode()
            } catch (e: Exception) {
                _error.value = "Failed to toggle repeat: ${e.message}"
            }
        }
    }

    // Convenience methods for common UI operations
    fun playAlbum(songs: List<Song>, startFromSong: Song? = null) {
        val startIndex = startFromSong?.let { songs.indexOf(it) }?.takeIf { it >= 0 } ?: 0
        playPlaylist(songs, startIndex)
    }

    fun playShuffled(songs: List<Song>) {
        playPlaylist(songs.shuffled())
    }

    fun addAlbumToQueue(songs: List<Song>, playNext: Boolean = false) {
        addToQueue(songs, playNext)
    }

    fun replaceQueueAndPlay(songs: List<Song>, startIndex: Int = 0) {
        playPlaylist(songs, startIndex)
    }

    // Utility methods
    fun formatPosition(): String {
        val positionMs = currentPosition.value
        val minutes = (positionMs / 1000) / 60
        val seconds = (positionMs / 1000) % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    fun formatDuration(): String {
        val durationMs = duration.value
        val minutes = (durationMs / 1000) / 60
        val seconds = (durationMs / 1000) % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    fun formatProgress(): String {
        return "${formatPosition()} / ${formatDuration()}"
    }

    fun getConnectionStatusText(): String {
        return when (connectionState.value) {
            MusicPlayerManager.ConnectionState.CONNECTED -> "Connected"
            MusicPlayerManager.ConnectionState.CONNECTING -> "Connecting..."
            MusicPlayerManager.ConnectionState.RECONNECTING -> "Reconnecting..."
            MusicPlayerManager.ConnectionState.DISCONNECTED -> "Disconnected"
            MusicPlayerManager.ConnectionState.FAILED -> "Connection Failed"
        }
    }

    fun isConnected(): Boolean {
        return connectionState.value == MusicPlayerManager.ConnectionState.CONNECTED
    }

    fun getCurrentSongTitle(): String {
        return currentSong.value?.title ?: "No song playing"
    }

    fun getCurrentArtist(): String {
        return currentSong.value?.creator?.username ?: "Unknown artist"
    }

    fun getQueuePosition(): String {
        val current = queueIndex.value + 1
        val total = queue.value.size
        return if (total > 0) "$current of $total" else "Empty queue"
    }

    fun getRepeatModeIcon(): ImageVector {
        return when (repeatMode.value) {
            MusicPlayerManager.RepeatMode.ONE -> Icons.Default.RepeatOne
            else -> Icons.Default.Repeat
        }
    }

    fun getRepeatModeDescription(): String {
        return when (repeatMode.value) {
            MusicPlayerManager.RepeatMode.OFF -> "Repeat off"
            MusicPlayerManager.RepeatMode.ONE -> "Repeat current song"
            MusicPlayerManager.RepeatMode.ALL -> "Repeat queue"
        }
    }

    fun getShuffleDescription(): String {
        return if (isShuffled.value) "Shuffle on" else "Shuffle off"
    }

    // Error handling
    fun clearError() {
        _error.value = null
    }

    fun hasError(): Boolean {
        return _error.value != null
    }

    // Lifecycle
    override fun onCleared() {
        super.onCleared()
        // PlayerManager is a singleton, so we don't release it here
        // It will be released when the app is destroyed
    }
}