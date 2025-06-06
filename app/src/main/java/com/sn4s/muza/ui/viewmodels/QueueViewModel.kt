package com.sn4s.muza.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sn4s.muza.data.model.Song
import com.sn4s.muza.player.MusicPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val playerManager: MusicPlayerManager
) : ViewModel() {

    // Direct access to player manager state
    val queue = playerManager.queue
    val queueIndex = playerManager.queueIndex
    val currentSong = playerManager.currentSong
    val isShuffled = playerManager.isShuffled
    val repeatMode = playerManager.repeatMode
    val connectionState = playerManager.connectionState
    val isPlaying = playerManager.isPlaying

    // UI state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isReordering = MutableStateFlow(false)
    val isReordering: StateFlow<Boolean> = _isReordering.asStateFlow()

    // Computed properties
    val upNext: StateFlow<List<Song>> = combine(queue, queueIndex) { songs, index ->
        if (songs.isEmpty() || index >= songs.size - 1) {
            emptyList()
        } else {
            songs.subList(index + 1, songs.size)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val previousSongs: StateFlow<List<Song>> = combine(queue, queueIndex) { songs, index ->
        if (songs.isEmpty() || index <= 0) {
            emptyList()
        } else {
            songs.subList(0, index)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val queueStats: StateFlow<QueueStats> = combine(queue, queueIndex) { songs, index ->
        QueueStats(
            totalSongs = songs.size,
            currentPosition = if (songs.isNotEmpty()) index + 1 else 0,
            remainingSongs = if (songs.isNotEmpty()) songs.size - index - 1 else 0,
            totalDuration = songs.sumOf { it.duration ?: 0 }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = QueueStats()
    )

    data class QueueStats(
        val totalSongs: Int = 0,
        val currentPosition: Int = 0,
        val remainingSongs: Int = 0,
        val totalDuration: Int = 0
    )

    // Queue management methods
    fun addToQueue(songs: List<Song>, playNext: Boolean = false) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                playerManager.addToQueue(songs, playNext)

                val position = if (playNext) "next" else "end of queue"
                Log.d("QueueViewModel", "Added ${songs.size} songs to $position")

            } catch (e: Exception) {
                Log.e("QueueViewModel", "Failed to add songs to queue", e)
                _error.value = "Failed to add songs to queue: ${e.message}"
            } finally {
                _isLoading.value = false
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
                val songs = queue.value

                if (index in songs.indices) {
                    val songTitle = songs[index].title
                    playerManager.removeFromQueue(index)
                    Log.d("QueueViewModel", "Removed song: $songTitle from queue")
                } else {
                    _error.value = "Invalid queue position"
                }

            } catch (e: Exception) {
                Log.e("QueueViewModel", "Failed to remove song from queue", e)
                _error.value = "Failed to remove song: ${e.message}"
            }
        }
    }

    fun moveInQueue(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            try {
                _isReordering.value = true
                _error.value = null

                val songs = queue.value
                if (fromIndex in songs.indices && toIndex in songs.indices) {
                    playerManager.moveInQueue(fromIndex, toIndex)
                    Log.d("QueueViewModel", "Moved song from $fromIndex to $toIndex")
                } else {
                    _error.value = "Invalid queue positions"
                }

            } catch (e: Exception) {
                Log.e("QueueViewModel", "Failed to move song in queue", e)
                _error.value = "Failed to reorder queue: ${e.message}"
            } finally {
                _isReordering.value = false
            }
        }
    }

    fun playFromQueue(index: Int) {
        viewModelScope.launch {
            try {
                _error.value = null

                if (index in 0 until queue.value.size) {
                    playerManager.seekToIndex(index)
                    Log.d("QueueViewModel", "Playing song at index $index")
                } else {
                    _error.value = "Invalid song position"
                }

            } catch (e: Exception) {
                Log.e("QueueViewModel", "Failed to play song from queue", e)
                _error.value = "Failed to play song: ${e.message}"
            }
        }
    }

    fun toggleShuffle() {
        viewModelScope.launch {
            try {
                _error.value = null
                playerManager.toggleShuffle()

                val shuffleState = if (isShuffled.value) "enabled" else "disabled"
                Log.d("QueueViewModel", "Shuffle $shuffleState")

            } catch (e: Exception) {
                Log.e("QueueViewModel", "Failed to toggle shuffle", e)
                _error.value = "Failed to toggle shuffle: ${e.message}"
            }
        }
    }

    fun toggleRepeatMode() {
        viewModelScope.launch {
            try {
                _error.value = null
                playerManager.toggleRepeatMode()

                Log.d("QueueViewModel", "Repeat mode: ${repeatMode.value}")

            } catch (e: Exception) {
                Log.e("QueueViewModel", "Failed to toggle repeat mode", e)
                _error.value = "Failed to toggle repeat: ${e.message}"
            }
        }
    }

    fun clearQueue() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                playerManager.clearQueue()
                Log.d("QueueViewModel", "Queue cleared")

            } catch (e: Exception) {
                Log.e("QueueViewModel", "Failed to clear queue", e)
                _error.value = "Failed to clear queue: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun skipToNext() {
        viewModelScope.launch {
            try {
                _error.value = null
                playerManager.skipToNext()
            } catch (e: Exception) {
                Log.e("QueueViewModel", "Failed to skip to next", e)
                _error.value = "Failed to skip to next song: ${e.message}"
            }
        }
    }

    fun skipToPrevious() {
        viewModelScope.launch {
            try {
                _error.value = null
                playerManager.skipToPrevious()
            } catch (e: Exception) {
                Log.e("QueueViewModel", "Failed to skip to previous", e)
                _error.value = "Failed to skip to previous song: ${e.message}"
            }
        }
    }

    fun togglePlayPause() {
        viewModelScope.launch {
            try {
                _error.value = null
                playerManager.togglePlayPause()
            } catch (e: Exception) {
                Log.e("QueueViewModel", "Failed to toggle play/pause", e)
                _error.value = "Failed to toggle playback: ${e.message}"
            }
        }
    }

    // Convenience methods for common queue operations
    fun addAlbumToQueue(songs: List<Song>, playNext: Boolean = false) {
        addToQueue(songs, playNext)
    }

    fun addPlaylistToQueue(songs: List<Song>, playNext: Boolean = false) {
        addToQueue(songs, playNext)
    }

    fun replaceQueueWith(songs: List<Song>, startIndex: Int = 0) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                playerManager.playPlaylist(songs, startIndex)
                Log.d("QueueViewModel", "Replaced queue with ${songs.size} songs, starting at $startIndex")

            } catch (e: Exception) {
                Log.e("QueueViewModel", "Failed to replace queue", e)
                _error.value = "Failed to replace queue: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun insertAfterCurrent(songs: List<Song>) {
        addToQueue(songs, playNext = true)
    }

    fun removeMultipleFromQueue(indices: List<Int>) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Remove in reverse order to maintain indices
                indices.sortedDescending().forEach { index ->
                    playerManager.removeFromQueue(index)
                }

                Log.d("QueueViewModel", "Removed ${indices.size} songs from queue")

            } catch (e: Exception) {
                Log.e("QueueViewModel", "Failed to remove multiple songs", e)
                _error.value = "Failed to remove songs: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Utility methods
    fun getSongAtIndex(index: Int): Song? {
        return queue.value.getOrNull(index)
    }

    fun getCurrentSongIndex(): Int {
        return queueIndex.value
    }

    fun isCurrentSong(index: Int): Boolean {
        return index == queueIndex.value
    }

    fun hasNextSong(): Boolean {
        val currentQueue = queue.value
        val currentIndex = queueIndex.value
        return when (repeatMode.value) {
            MusicPlayerManager.RepeatMode.ALL -> currentQueue.isNotEmpty()
            MusicPlayerManager.RepeatMode.ONE -> true
            MusicPlayerManager.RepeatMode.OFF -> currentIndex < currentQueue.size - 1
        }
    }

    fun hasPreviousSong(): Boolean {
        val currentQueue = queue.value
        val currentIndex = queueIndex.value
        return when (repeatMode.value) {
            MusicPlayerManager.RepeatMode.ALL -> currentQueue.isNotEmpty()
            MusicPlayerManager.RepeatMode.ONE -> true
            MusicPlayerManager.RepeatMode.OFF -> currentIndex > 0
        }
    }

    fun getRepeatModeText(): String {
        return when (repeatMode.value) {
            MusicPlayerManager.RepeatMode.OFF -> "Repeat Off"
            MusicPlayerManager.RepeatMode.ONE -> "Repeat One"
            MusicPlayerManager.RepeatMode.ALL -> "Repeat All"
        }
    }

    fun getShuffleStateText(): String {
        return if (isShuffled.value) "Shuffle On" else "Shuffle Off"
    }

    fun formatTotalDuration(): String {
        val totalSeconds = queueStats.value.totalDuration
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return if (hours > 0) {
            String.format("%d:%02d hr", hours, minutes)
        } else {
            String.format("%d min", minutes)
        }
    }

    fun clearError() {
        _error.value = null
    }

    // State management for UI
    fun refreshQueue() {
        // The StateFlows will automatically update when the player manager state changes
        Log.d("QueueViewModel", "Queue refresh requested")
    }
}