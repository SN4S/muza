package com.sn4s.muza.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sn4s.muza.data.model.Song
import com.sn4s.muza.data.repository.RecentlyPlayedRepository
import com.sn4s.muza.player.MusicPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Unified controller for all music playback and queue operations.
 * This replaces both PlayerViewModel and QueueViewModel functionality.
 */
@HiltViewModel
class PlayerController @Inject constructor(
    private val playerManager: MusicPlayerManager,
    private val recentlyPlayedRepository: RecentlyPlayedRepository
) : ViewModel() {

    // === Direct State Access ===
    val currentSong: StateFlow<Song?> = playerManager.currentSong
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val playbackState: StateFlow<Int> = playerManager.playbackState
    val currentPosition: StateFlow<Long> = playerManager.currentPosition
    val duration: StateFlow<Long> = playerManager.duration
    val connectionState: StateFlow<MusicPlayerManager.ConnectionState> = playerManager.connectionState
    val queue: StateFlow<List<Song>> = playerManager.queue
    val queueIndex: StateFlow<Int> = playerManager.queueIndex
    val isShuffled: StateFlow<Boolean> = playerManager.isShuffled
    val repeatMode: StateFlow<MusicPlayerManager.RepeatMode> = playerManager.repeatMode

    val recentlyPlayed: StateFlow<List<Song>> = recentlyPlayedRepository.recentlyPlayed

    // === UI State ===
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // === Computed Properties ===
    val hasNextSong: StateFlow<Boolean> = combine(queue, queueIndex, repeatMode) { songs, index, repeat ->
        when (repeat) {
            MusicPlayerManager.RepeatMode.ALL -> songs.isNotEmpty()
            MusicPlayerManager.RepeatMode.ONE -> true
            MusicPlayerManager.RepeatMode.OFF -> index < songs.size - 1
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val hasPreviousSong: StateFlow<Boolean> = combine(queue, queueIndex, repeatMode) { songs, index, repeat ->
        when (repeat) {
            MusicPlayerManager.RepeatMode.ALL -> songs.isNotEmpty()
            MusicPlayerManager.RepeatMode.ONE -> true
            MusicPlayerManager.RepeatMode.OFF -> index > 0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val progressPercentage: StateFlow<Float> = combine(currentPosition, duration) { position, totalDuration ->
        if (totalDuration > 0) (position.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f) else 0f
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val queueStats: StateFlow<QueueStats> = combine(queue, queueIndex) { songs, index ->
        QueueStats(
            totalSongs = songs.size,
            currentPosition = if (songs.isNotEmpty()) index + 1 else 0,
            remainingSongs = if (songs.isNotEmpty()) songs.size - index - 1 else 0,
            totalDuration = songs.sumOf { it.duration ?: 0 }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QueueStats())

    data class QueueStats(
        val totalSongs: Int = 0,
        val currentPosition: Int = 0,
        val remainingSongs: Int = 0,
        val totalDuration: Int = 0
    )

    // === Core Playback Operations (Single Implementation) ===

    /**
     * Play a single song immediately, replacing current queue
     */
    fun playSong(song: Song) = executeWithErrorHandling("play song") {
        playerManager.playSong(song)
        recentlyPlayedRepository.addSong(song)
    }

    /**
     * Play a list of songs, replacing current queue
     */
    fun playPlaylist(songs: List<Song>, startIndex: Int = 0) = executeWithLoadingAndErrorHandling("play playlist") {
        playerManager.playPlaylist(songs, startIndex)
        songs.getOrNull(startIndex)?.let { song ->
            recentlyPlayedRepository.addSong(song)
        }
    }

    /**
     * Resume/start playback
     */
    fun play() = executeWithErrorHandling("resume playback") {
        playerManager.play()
    }

    /**
     * Pause playback
     */
    fun pause() = executeWithErrorHandling("pause playback") {
        playerManager.pause()
    }

    /**
     * Toggle between play and pause
     */
    fun togglePlayPause() = executeWithErrorHandling("toggle play/pause") {
        playerManager.togglePlayPause()
    }

    // === Queue Management (Single Implementation) ===

    /**
     * Add songs to queue at specified position
     */
    fun addToQueue(songs: List<Song>, playNext: Boolean = false) = executeWithLoadingAndErrorHandling("add to queue") {
        playerManager.addToQueue(songs, playNext)
    }

    /**
     * Add single song to queue (convenience method)
     */
    fun addSongToQueue(song: Song, playNext: Boolean = false) = addToQueue(listOf(song), playNext)

    /**
     * Remove song from queue at index
     */
    fun removeFromQueue(index: Int) = executeWithErrorHandling("remove from queue") {
        playerManager.removeFromQueue(index)
    }

    /**
     * Move song within queue
     */
    fun moveInQueue(fromIndex: Int, toIndex: Int) = executeWithErrorHandling("reorder queue") {
        playerManager.moveInQueue(fromIndex, toIndex)
    }

    /**
     * Clear entire queue
     */
    fun clearQueue() = executeWithErrorHandling("clear queue") {
        playerManager.clearQueue()
    }

    // === Navigation Controls ===

    fun seekTo(position: Long) = executeWithErrorHandling("seek") {
        playerManager.seekTo(position)
    }

    fun skipToNext() = executeWithErrorHandling("skip to next") {
        playerManager.skipToNext()
        currentSong.value?.let { song ->
            recentlyPlayedRepository.addSong(song)
        }
    }

    fun skipToPrevious() = executeWithErrorHandling("skip to previous") {
        playerManager.skipToPrevious()
        currentSong.value?.let { song ->
            recentlyPlayedRepository.addSong(song)
        }
    }

    fun seekToIndex(index: Int) = executeWithErrorHandling("seek to song") {
        playerManager.seekToIndex(index)
        queue.value.getOrNull(index)?.let { song ->
            recentlyPlayedRepository.addSong(song)
        }
    }

    fun clearRecentlyPlayed() {
        recentlyPlayedRepository.clearRecentlyPlayed()
    }

    fun getRecentlyPlayedList(): List<Song> {
        return recentlyPlayedRepository.getRecentlyPlayedList()
    }

    fun playFromQueue(index: Int) = seekToIndex(index)

    // === Mode Controls ===

    fun toggleShuffle() = executeWithErrorHandling("toggle shuffle") {
        playerManager.toggleShuffle()
    }

    fun toggleRepeatMode() = executeWithErrorHandling("toggle repeat") {
        playerManager.toggleRepeatMode()
    }

    // === Convenience Methods (Unified Implementation) ===

    /**
     * Play album starting from specific song
     */
    fun playAlbum(songs: List<Song>, startFromSong: Song? = null) {
        val startIndex = startFromSong?.let { songs.indexOf(it) }?.takeIf { it >= 0 } ?: 0
        playPlaylist(songs, startIndex)
    }

    /**
     * Play songs in shuffled order
     */
    fun playShuffled(songs: List<Song>) = playPlaylist(songs.shuffled())

    /**
     * Replace queue and start playing
     */
    fun replaceQueueAndPlay(songs: List<Song>, startIndex: Int = 0) = playPlaylist(songs, startIndex)

    /**
     * Insert songs after currently playing song
     */
    fun insertAfterCurrent(songs: List<Song>) = addToQueue(songs, playNext = true)

    /**
     * Add entire album to queue
     */
    fun addAlbumToQueue(songs: List<Song>, playNext: Boolean = false) = addToQueue(songs, playNext)

    /**
     * Add entire playlist to queue
     */
    fun addPlaylistToQueue(songs: List<Song>, playNext: Boolean = false) = addToQueue(songs, playNext)

    /**
     * Play specific song from a collection and queue all remaining songs
     * This is the key method for your use case!
     */
    fun playFromCollectionStartingAt(
        allSongs: List<Song>,
        selectedSong: Song,
        shuffle: Boolean = false
    ) {
        val songIndex = allSongs.indexOf(selectedSong)
        if (songIndex == -1) {
            // Fallback: just play the song
            playSong(selectedSong)
            return
        }

        val finalSongs = if (shuffle) allSongs.shuffled() else allSongs
        val finalIndex = if (shuffle) {
            finalSongs.indexOf(selectedSong)
        } else {
            songIndex
        }

        playPlaylist(finalSongs, finalIndex)
    }

    /**
     * Play song from playlist/album and queue remaining songs
     */
    fun playFromPlaylist(songs: List<Song>, selectedSong: Song) =
        playFromCollectionStartingAt(songs, selectedSong, shuffle = false)

    /**
     * Play song from album and queue remaining songs
     */
    fun playFromAlbum(songs: List<Song>, selectedSong: Song) =
        playFromCollectionStartingAt(songs, selectedSong, shuffle = false)

    /**
     * Play song from liked songs and queue remaining
     */
    fun playFromLikedSongs(songs: List<Song>, selectedSong: Song) =
        playFromCollectionStartingAt(songs, selectedSong, shuffle = false)

    /**
     * Play shuffled from any collection
     */
    fun playShuffledFromCollection(allSongs: List<Song>, selectedSong: Song) =
        playFromCollectionStartingAt(allSongs, selectedSong, shuffle = true)

    // === Utility Methods ===

    fun stop() = executeWithErrorHandling("stop playback") {
        playerManager.stop()
    }

    fun formatPosition(): String = formatTime(currentPosition.value)
    fun formatDuration(): String = formatTime(duration.value)
    fun formatProgress(): String = "${formatPosition()} / ${formatDuration()}"

    fun isConnected(): Boolean = connectionState.value == MusicPlayerManager.ConnectionState.CONNECTED
    fun getCurrentSongTitle(): String = currentSong.value?.title ?: "No song playing"
    fun getCurrentArtist(): String = currentSong.value?.creator?.username ?: "Unknown artist"

    fun getConnectionStatusText(): String = when (connectionState.value) {
        MusicPlayerManager.ConnectionState.CONNECTED -> "Connected"
        MusicPlayerManager.ConnectionState.CONNECTING -> "Connecting..."
        MusicPlayerManager.ConnectionState.RECONNECTING -> "Reconnecting..."
        MusicPlayerManager.ConnectionState.DISCONNECTED -> "Disconnected"
        MusicPlayerManager.ConnectionState.FAILED -> "Connection Failed"
    }

    // === Error Handling ===

    fun clearError() {
        _error.value = null
    }

    // === Private Helper Methods ===

    private fun executeWithErrorHandling(operation: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                _error.value = null
                action()
            } catch (e: Exception) {
                _error.value = "Failed to $operation: ${e.message}"
            }
        }
    }

    private fun executeWithLoadingAndErrorHandling(operation: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                action()
            } catch (e: Exception) {
                _error.value = "Failed to $operation: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun formatTime(timeMs: Long): String {
        val minutes = (timeMs / 1000) / 60
        val seconds = (timeMs / 1000) % 60
        return String.format("%d:%02d", minutes, seconds)
    }
}