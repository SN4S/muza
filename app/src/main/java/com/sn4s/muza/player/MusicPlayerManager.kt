package com.sn4s.muza.player

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.sn4s.muza.data.model.Song
import com.sn4s.muza.di.NetworkModule
import com.sn4s.muza.service.MusicPlayerService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class MusicPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var playerListener: Player.Listener? = null

    // Use SupervisorJob to prevent cancellation propagation
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var reconnectJob: Job? = null
    private var positionJob: Job? = null

    // Connection state management
    private var isConnecting = false
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5

    // State flows
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    val playbackState: StateFlow<Int> = _playbackState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _queueIndex = MutableStateFlow(0)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()

    private val _isShuffled = MutableStateFlow(false)
    val isShuffled: StateFlow<Boolean> = _isShuffled.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, FAILED }
    enum class RepeatMode { OFF, ONE, ALL }

    init {
        initializeController()
        startPositionUpdates()
        observeConnectionState()

        // Debug connection state
        scope.launch {
            connectionState.collect { state ->
                Log.d("MusicPlayerManager", "Connection state changed to: $state")
            }
        }
    }

    private fun observeConnectionState() {
        scope.launch {
            connectionState.collect { state ->
                when (state) {
                    ConnectionState.FAILED -> {
                        if (reconnectAttempts < maxReconnectAttempts) {
                            scheduleReconnect()
                        }
                    }
                    ConnectionState.CONNECTED -> {
                        reconnectAttempts = 0 // Reset on successful connection
                    }
                    else -> { /* No action needed */ }
                }
            }
        }
    }

    private fun initializeController() {
        if (isConnecting) return

        // Clean up existing resources first
        cleanupController()

        isConnecting = true
        _connectionState.value = if (reconnectAttempts > 0) ConnectionState.RECONNECTING else ConnectionState.CONNECTING

        val sessionToken = SessionToken(context, ComponentName(context, MusicPlayerService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture?.addListener({
            try {
                val controller = controllerFuture?.get()
                if (controller?.isConnected == true) {
                    mediaController = controller
                    setupPlayerListener(controller)
                    _connectionState.value = ConnectionState.CONNECTED
                    Log.d("MusicPlayerManager", "MediaController connected successfully")
                } else {
                    _connectionState.value = ConnectionState.FAILED
                    Log.w("MusicPlayerManager", "MediaController not connected")
                }
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.FAILED
                Log.e("MusicPlayerManager", "Failed to connect to MediaController", e)
            } finally {
                isConnecting = false
            }
        }, MoreExecutors.directExecutor())
    }

    private fun cleanupController() {
        // Remove old listener
        playerListener?.let { listener ->
            mediaController?.removeListener(listener)
        }

        // Cancel old future
        controllerFuture?.cancel(true)

        // Don't release mediaController here as it might be in use
        // It will be released when the new one is set or in release()
    }

    private fun setupPlayerListener(controller: MediaController) {
        // Remove old listener first
        playerListener?.let { mediaController?.removeListener(it) }

        playerListener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                _playbackState.value = playbackState
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateCurrentSongFromMediaItem(mediaItem)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatMode.value = when (repeatMode) {
                    Player.REPEAT_MODE_OFF -> RepeatMode.OFF
                    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                    else -> RepeatMode.OFF
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _isShuffled.value = shuffleModeEnabled
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e("MusicPlayerManager", "Player error: ${error.message}", error)
                // Don't automatically reconnect on player errors
            }
        }

        controller.addListener(playerListener!!)
    }

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (true) {
                try {
                    mediaController?.let { controller ->
                        if (controller.isPlaying) {
                            _currentPosition.value = controller.currentPosition.coerceAtLeast(0L)
                            val duration = controller.duration
                            if (duration != androidx.media3.common.C.TIME_UNSET) {
                                _duration.value = duration.coerceAtLeast(0L)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("MusicPlayerManager", "Error updating position", e)
                }
                delay(500)
            }
        }
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val backoffDelay = min(1000L * (1L shl reconnectAttempts), 30000L) // Exponential backoff, max 30s
            delay(backoffDelay)
            reconnectAttempts++
            Log.d("MusicPlayerManager", "Reconnect attempt $reconnectAttempts after ${backoffDelay}ms")
            initializeController()
        }
    }

    private fun updateCurrentSongFromMediaItem(mediaItem: MediaItem?) {
        mediaItem?.let { item ->
            val songId = item.mediaId.toIntOrNull()
            val song = _queue.value.find { it.id == songId }
            _currentSong.value = song
            song?.let { foundSong ->
                val newIndex = _queue.value.indexOf(foundSong)
                if (newIndex >= 0) {
                    _queueIndex.value = newIndex
                }
            }
        }
    }

    private fun createMediaItem(song: Song): MediaItem {
        val streamUrl = "${NetworkModule.BASE_URL}songs/${song.id}/stream"
        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.creator.username)
                    .setArtworkUri(song.creator.image?.let {
                        android.net.Uri.parse("${NetworkModule.BASE_URL}$it")
                    })
                    .build()
            )
            .build()
    }

    // Public API methods
    fun playSong(song: Song) {
        val controller = mediaController
        if (controller?.isConnected != true) {
            Log.w("MusicPlayerManager", "Controller not connected, cannot play song")
            return
        }

        _queue.value = listOf(song)
        _queueIndex.value = 0
        _currentSong.value = song

        val mediaItem = createMediaItem(song)
        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.play()

        Log.d("MusicPlayerManager", "Playing song: ${song.title}")
    }

    fun playPlaylist(songs: List<Song>, startIndex: Int = 0) {
        val controller = mediaController
        if (controller?.isConnected != true) {
            Log.w("MusicPlayerManager", "Controller not connected, cannot play playlist")
            return
        }
        if (songs.isEmpty()) return

        _queue.value = songs
        _queueIndex.value = startIndex.coerceIn(0, songs.size - 1)
        _currentSong.value = songs.getOrNull(_queueIndex.value)

        val mediaItems = songs.map { createMediaItem(it) }
        controller.setMediaItems(mediaItems, startIndex, 0L)
        controller.prepare()
        controller.play()

        Log.d("MusicPlayerManager", "Playing playlist: ${songs.size} songs, starting at $startIndex")
    }

    fun play() {
        val controller = mediaController
        if (controller?.isConnected == true) {
            controller.play()
            Log.d("MusicPlayerManager", "Play command sent")
        } else {
            Log.w("MusicPlayerManager", "Controller not connected, cannot play")
        }
    }

    fun pause() {
        val controller = mediaController
        if (controller?.isConnected == true) {
            controller.pause()
            Log.d("MusicPlayerManager", "Pause command sent")
        } else {
            Log.w("MusicPlayerManager", "Controller not connected, cannot pause")
        }
    }

    fun togglePlayPause() {
        val controller = mediaController
        if (controller?.isConnected == true) {
            if (controller.isPlaying) {
                controller.pause()
                Log.d("MusicPlayerManager", "Toggled to pause")
            } else {
                controller.play()
                Log.d("MusicPlayerManager", "Toggled to play")
            }
        } else {
            Log.w("MusicPlayerManager", "Controller not connected, cannot toggle play/pause")
        }
    }

    fun seekTo(position: Long) {
        val controller = mediaController
        if (controller?.isConnected == true) {
            controller.seekTo(position)
            _currentPosition.value = position // Immediate UI feedback
            Log.d("MusicPlayerManager", "Seek to position: ${position}ms")
        } else {
            Log.w("MusicPlayerManager", "Controller not connected, cannot seek")
        }
    }

    fun skipToNext() {
        val controller = mediaController
        if (controller?.isConnected == true) {
            controller.seekToNext()
            Log.d("MusicPlayerManager", "Skip to next")
        } else {
            Log.w("MusicPlayerManager", "Controller not connected, cannot skip to next")
        }
    }

    fun skipToPrevious() {
        val controller = mediaController
        if (controller?.isConnected == true) {
            controller.seekToPrevious()
            Log.d("MusicPlayerManager", "Skip to previous")
        } else {
            Log.w("MusicPlayerManager", "Controller not connected, cannot skip to previous")
        }
    }

    fun seekToIndex(index: Int) {
        val controller = mediaController
        if (controller?.isConnected == true && index in 0 until _queue.value.size) {
            controller.seekTo(index, 0L)
            Log.d("MusicPlayerManager", "Seek to index: $index")
        } else {
            Log.w("MusicPlayerManager", "Controller not connected or invalid index, cannot seek to index")
        }
    }

    fun addToQueue(songs: List<Song>, playNext: Boolean = false) {
        val controller = mediaController ?: return
        if (songs.isEmpty()) return

        val currentQueue = _queue.value.toMutableList()
        val insertIndex = if (playNext) _queueIndex.value + 1 else currentQueue.size

        currentQueue.addAll(insertIndex, songs)
        _queue.value = currentQueue

        val mediaItems = songs.map { createMediaItem(it) }
        val actualIndex = insertIndex.coerceAtMost(controller.mediaItemCount)

        mediaItems.forEachIndexed { offset, mediaItem ->
            controller.addMediaItem(actualIndex + offset, mediaItem)
        }

        Log.d("MusicPlayerManager", "Added ${songs.size} songs to queue at position $insertIndex")
    }

    fun removeFromQueue(index: Int) {
        val controller = mediaController ?: return
        val currentQueue = _queue.value.toMutableList()

        if (index in currentQueue.indices) {
            val removedSong = currentQueue.removeAt(index)
            _queue.value = currentQueue

            controller.removeMediaItem(index)

            // Update current index if needed
            when {
                index < _queueIndex.value -> _queueIndex.value = _queueIndex.value - 1
                index == _queueIndex.value && currentQueue.isNotEmpty() -> {
                    _queueIndex.value = _queueIndex.value.coerceAtMost(currentQueue.size - 1)
                    _currentSong.value = currentQueue.getOrNull(_queueIndex.value)
                }
            }

            Log.d("MusicPlayerManager", "Removed ${removedSong.title} from queue")
        }
    }

    fun moveInQueue(fromIndex: Int, toIndex: Int) {
        val controller = mediaController ?: return
        val currentQueue = _queue.value.toMutableList()

        if (fromIndex in currentQueue.indices && toIndex in currentQueue.indices) {
            val song = currentQueue.removeAt(fromIndex)
            currentQueue.add(toIndex, song)
            _queue.value = currentQueue

            controller.moveMediaItem(fromIndex, toIndex)

            // Update current index if needed
            _queueIndex.value = when {
                fromIndex == _queueIndex.value -> toIndex
                fromIndex < _queueIndex.value && toIndex >= _queueIndex.value -> _queueIndex.value - 1
                fromIndex > _queueIndex.value && toIndex <= _queueIndex.value -> _queueIndex.value + 1
                else -> _queueIndex.value
            }

            Log.d("MusicPlayerManager", "Moved song from $fromIndex to $toIndex")
        }
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        val newShuffleMode = !_isShuffled.value
        controller.shuffleModeEnabled = newShuffleMode
    }

    fun toggleRepeatMode() {
        val controller = mediaController ?: return
        val newMode = when (_repeatMode.value) {
            RepeatMode.OFF -> Player.REPEAT_MODE_ALL
            RepeatMode.ALL -> Player.REPEAT_MODE_ONE
            RepeatMode.ONE -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = newMode
    }

    fun clearQueue() {
        val controller = mediaController ?: return
        controller.clearMediaItems()
        _queue.value = emptyList()
        _queueIndex.value = 0
        _currentSong.value = null
    }

    fun stop() {
        mediaController?.stop()
        _currentSong.value = null
        _isPlaying.value = false
        _playbackState.value = Player.STATE_IDLE
        _currentPosition.value = 0L
        _duration.value = 0L
    }

    fun release() {
        // Cancel all coroutines
        scope.cancel()

        // Remove listener
        playerListener?.let { listener ->
            mediaController?.removeListener(listener)
        }
        playerListener = null

        // Cancel and release controller future
        controllerFuture?.cancel(true)
        controllerFuture = null

        // Release controller
        mediaController?.release()
        mediaController = null

        Log.d("MusicPlayerManager", "Released all resources")
    }
}