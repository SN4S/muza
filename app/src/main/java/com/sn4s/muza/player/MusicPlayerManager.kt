package com.sn4s.muza.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sn4s.muza.data.model.Song
import com.sn4s.muza.di.NetworkModule
import com.sn4s.muza.service.MusicPlayerService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

@Singleton
class MusicPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var positionUpdateJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var connectionRetryJob: Job? = null

    // Connection management
    private var connectionAttempts = 0
    private val maxRetryAttempts = 5
    private var isConnectionInProgress = false
    private var serviceStarted = false

    // Queue persistence
    private val queuePrefs = context.getSharedPreferences("music_queue", Context.MODE_PRIVATE)

    // Basic player state
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

    // Connection state
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Advanced queue system
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _queueIndex = MutableStateFlow(0)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()

    private val _isShuffled = MutableStateFlow(false)
    val isShuffled: StateFlow<Boolean> = _isShuffled.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    // Legacy compatibility
    val playlist: StateFlow<List<Song>> = _queue
    val currentIndex: StateFlow<Int> = _queueIndex

    enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, FAILED
    }

    enum class RepeatMode { OFF, ONE, ALL }

    init {
        initializeController()
        restoreQueueState()
    }

    private fun ensureServiceStarted() {
        if (!serviceStarted) {
            val intent = Intent(context, MusicPlayerService::class.java)
            context.startForegroundService(intent)
            serviceStarted = true
        }
    }

    private fun updateConnectionState(state: ConnectionState) {
        _connectionState.value = state
        Log.d("MusicPlayerManager", "Connection state: $state")
    }

    private fun initializeController() {
        if (isConnectionInProgress) return
        isConnectionInProgress = true
        updateConnectionState(ConnectionState.CONNECTING)

        ensureServiceStarted()
        connectionRetryJob?.cancel()

        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicPlayerService::class.java)
        )

        controllerFuture?.let { future ->
            MediaController.releaseFuture(future)
        }

        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController?.release()
                mediaController = controllerFuture?.get()
                if (mediaController?.isConnected == true) {
                    Log.d("MusicPlayerManager", "MediaController connected successfully")
                    connectionAttempts = 0 // Reset on success
                    updateConnectionState(ConnectionState.CONNECTED)
                    setupPlayerListener()
                } else {
                    Log.w("MusicPlayerManager", "MediaController not connected")
                    updateConnectionState(ConnectionState.DISCONNECTED)
                    scheduleReconnection()
                }
            } catch (e: Exception) {
                Log.e("MusicPlayerManager", "Failed to connect to service", e)
                updateConnectionState(ConnectionState.FAILED)
                scheduleReconnection()
            } finally {
                isConnectionInProgress = false
            }
        }, MoreExecutors.directExecutor())
    }

    private fun scheduleReconnection() {
        if (connectionAttempts >= maxRetryAttempts || isConnectionInProgress) {
            if (connectionAttempts >= maxRetryAttempts) {
                updateConnectionState(ConnectionState.FAILED)
            }
            return
        }

        updateConnectionState(ConnectionState.RECONNECTING)
        connectionRetryJob?.cancel()
        connectionRetryJob = scope.launch {
            val backoffDelay = (1000 * (2.0.pow(connectionAttempts))).toLong().coerceAtMost(30000)
            delay(backoffDelay)
            connectionAttempts++
            Log.d("MusicPlayerManager", "Retry attempt $connectionAttempts after ${backoffDelay}ms")
            initializeController()
        }
    }

    private fun ensureConnected(): Boolean {
        return if (mediaController?.isConnected == true) {
            true
        } else {
            Log.w("MusicPlayerManager", "Controller not connected, attempting reconnection")
            initializeController()
            false
        }
    }

    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                _playbackState.value = playbackState
                updatePositionTracking()
                updateDuration()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                updatePositionTracking()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.let { item ->
                    val songId = item.mediaId.toIntOrNull()
                    if (songId != null) {
                        val song = _queue.value.find { it.id == songId }
                        _currentSong.value = song
                        val newIndex = _queue.value.indexOf(song)
                        if (newIndex >= 0) {
                            _queueIndex.value = newIndex
                        }
                    }
                }
                updateDuration()
                saveQueueState()
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                // Handle position changes (seeking, track changes)
                updatePosition()
                updateDuration()
            }
        })
    }

    private fun updatePositionTracking() {
        positionUpdateJob?.cancel()

        if (_isPlaying.value && _playbackState.value == Player.STATE_READY) {
            positionUpdateJob = scope.launch {
                while (isActive && _isPlaying.value) {
                    updatePosition()
                    updateDuration()
                    delay(500) // Update every half second
                }
            }
        }
    }

    private fun updatePosition() {
        mediaController?.let { controller ->
            _currentPosition.value = controller.currentPosition.coerceAtLeast(0L)
        }
    }

    private fun updateDuration() {
        mediaController?.let { controller ->
            val duration = controller.duration
            if (duration != androidx.media3.common.C.TIME_UNSET) {
                _duration.value = duration.coerceAtLeast(0L)
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
                    .build()
            )
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setMediaUri(android.net.Uri.parse(streamUrl))
                    .build()
            )
            .build()
    }

    private fun updateMediaController() {
        val currentQueue = _queue.value
        if (currentQueue.isEmpty()) return

        val mediaItems = currentQueue.map { song ->
            createMediaItem(song)
        }

        mediaController?.let { controller ->
            controller.setMediaItems(mediaItems, _queueIndex.value, 0)
            controller.prepare()
        }

        saveQueueState()
    }

    // Queue Management Methods
    fun addToQueue(songs: List<Song>, playNext: Boolean = false) {
        if (songs.isEmpty()) return

        val currentQueue = _queue.value.toMutableList()
        val insertIndex = if (playNext && currentQueue.isNotEmpty()) {
            (_queueIndex.value + 1).coerceAtMost(currentQueue.size)
        } else {
            currentQueue.size
        }

        currentQueue.addAll(insertIndex, songs)
        _queue.value = currentQueue

        if (ensureConnected()) {
            updateMediaController()
        }

        Log.d("MusicPlayerManager", "Added ${songs.size} songs to queue at index $insertIndex")
    }

    fun removeFromQueue(index: Int) {
        val currentQueue = _queue.value.toMutableList()
        if (index !in currentQueue.indices) return

        val removedSong = currentQueue.removeAt(index)
        _queue.value = currentQueue

        // Adjust current index if needed
        when {
            index < _queueIndex.value -> {
                _queueIndex.value = _queueIndex.value - 1
            }
            index == _queueIndex.value -> {
                if (currentQueue.isEmpty()) {
                    // Queue is now empty
                    _currentSong.value = null
                    _queueIndex.value = 0
                } else {
                    // Move to next song or stay at same index if it's now the last
                    _queueIndex.value = _queueIndex.value.coerceAtMost(currentQueue.size - 1)
                    _currentSong.value = currentQueue.getOrNull(_queueIndex.value)
                }
            }
        }

        if (ensureConnected()) {
            updateMediaController()
        }

        Log.d("MusicPlayerManager", "Removed song: ${removedSong.title} from queue")
    }

    fun moveInQueue(fromIndex: Int, toIndex: Int) {
        val currentQueue = _queue.value.toMutableList()
        if (fromIndex !in currentQueue.indices || toIndex !in currentQueue.indices) return

        val song = currentQueue.removeAt(fromIndex)
        currentQueue.add(toIndex, song)
        _queue.value = currentQueue

        // Update current index if it was affected
        _queueIndex.value = when {
            fromIndex == _queueIndex.value -> toIndex
            fromIndex < _queueIndex.value && toIndex >= _queueIndex.value -> _queueIndex.value - 1
            fromIndex > _queueIndex.value && toIndex <= _queueIndex.value -> _queueIndex.value + 1
            else -> _queueIndex.value
        }

        if (ensureConnected()) {
            updateMediaController()
        }

        Log.d("MusicPlayerManager", "Moved song from $fromIndex to $toIndex")
    }

    fun toggleShuffle() {
        _isShuffled.value = !_isShuffled.value
        Log.d("MusicPlayerManager", "Shuffle toggled: ${_isShuffled.value}")
        saveQueueState()
    }

    fun toggleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        Log.d("MusicPlayerManager", "Repeat mode: ${_repeatMode.value}")
        saveQueueState()
    }

    // Basic Playback Methods
    fun playSong(song: Song) {
        if (!ensureConnected()) {
            // Schedule for when connected
            scope.launch {
                delay(1000)
                playSong(song)
            }
            return
        }

        _queue.value = listOf(song)
        _queueIndex.value = 0
        _currentSong.value = song

        updateMediaController()
        mediaController?.play()

        Log.d("MusicPlayerManager", "Playing single song: ${song.title}")
    }

    fun playPlaylist(songs: List<Song>, startIndex: Int = 0) {
        if (!ensureConnected()) {
            // Schedule for when connected
            scope.launch {
                delay(1000)
                playPlaylist(songs, startIndex)
            }
            return
        }

        if (songs.isEmpty()) return

        _queue.value = songs
        _queueIndex.value = startIndex.coerceAtMost(songs.size - 1)
        _currentSong.value = songs.getOrNull(_queueIndex.value)

        updateMediaController()
        mediaController?.play()

        Log.d("MusicPlayerManager", "Playing playlist with ${songs.size} songs, starting at $startIndex")
    }

    fun play() {
        if (ensureConnected()) {
            mediaController?.play()
        }
    }

    fun pause() {
        if (ensureConnected()) {
            mediaController?.pause()
        }
    }

    fun seekTo(position: Long) {
        if (ensureConnected()) {
            mediaController?.seekTo(position)
            _currentPosition.value = position // Immediate UI update
        }
    }

    fun skipToNext() {
        if (ensureConnected()) {
            val nextIndex = when {
                _repeatMode.value == RepeatMode.ONE -> _queueIndex.value
                _queueIndex.value < _queue.value.size - 1 -> _queueIndex.value + 1
                _repeatMode.value == RepeatMode.ALL -> 0
                else -> _queueIndex.value // Stay on current if no repeat and at end
            }

            if (nextIndex != _queueIndex.value || _repeatMode.value == RepeatMode.ONE) {
                seekToIndex(nextIndex)
            }
        }
    }

    fun skipToPrevious() {
        if (ensureConnected()) {
            val prevIndex = when {
                _repeatMode.value == RepeatMode.ONE -> _queueIndex.value
                _queueIndex.value > 0 -> _queueIndex.value - 1
                _repeatMode.value == RepeatMode.ALL -> _queue.value.size - 1
                else -> _queueIndex.value // Stay on current if no repeat and at beginning
            }

            if (prevIndex != _queueIndex.value || _repeatMode.value == RepeatMode.ONE) {
                seekToIndex(prevIndex)
            }
        }
    }

    fun seekToIndex(index: Int) {
        if (ensureConnected() && index in 0 until _queue.value.size) {
            _queueIndex.value = index
            _currentSong.value = _queue.value.getOrNull(index)
            mediaController?.seekTo(index, 0)
        }
    }

    fun togglePlayPause() {
        mediaController?.let { controller ->
            if (controller.isPlaying) {
                pause()
            } else {
                play()
            }
        }
    }

    fun stop() {
        if (ensureConnected()) {
            mediaController?.stop()
        }
        _currentSong.value = null
        _currentPosition.value = 0L
        _duration.value = 0L
        _isPlaying.value = false
        _playbackState.value = Player.STATE_IDLE
        positionUpdateJob?.cancel()
    }

    fun clearQueue() {
        if (ensureConnected()) {
            mediaController?.clearMediaItems()
        }
        _queue.value = emptyList()
        _currentSong.value = null
        _queueIndex.value = 0
        _currentPosition.value = 0L
        _duration.value = 0L

        saveQueueState()
        Log.d("MusicPlayerManager", "Queue cleared")
    }

    // Queue State Persistence
    private fun saveQueueState() {
        scope.launch(Dispatchers.IO) {
            try {
                val queueJson = Gson().toJson(_queue.value.map { it.id })
                queuePrefs.edit()
                    .putString("queue_ids", queueJson)
                    .putInt("queue_index", _queueIndex.value)
                    .putBoolean("is_shuffled", _isShuffled.value)
                    .putString("repeat_mode", _repeatMode.value.name)
                    .putLong("position", _currentPosition.value)
                    .apply()
            } catch (e: Exception) {
                Log.e("MusicPlayerManager", "Failed to save queue state", e)
            }
        }
    }

    private fun restoreQueueState() {
        scope.launch(Dispatchers.IO) {
            try {
                val queueIdsJson = queuePrefs.getString("queue_ids", null)
                if (queueIdsJson != null) {
                    val queueIds = Gson().fromJson<List<Int>>(queueIdsJson, object : TypeToken<List<Int>>(){}.type)

                    // Note: You'll need to inject repository to fetch songs by IDs
                    // For now, we just restore the other state
                    _queueIndex.value = queuePrefs.getInt("queue_index", 0)
                    _isShuffled.value = queuePrefs.getBoolean("is_shuffled", false)
                    _repeatMode.value = try {
                        RepeatMode.valueOf(queuePrefs.getString("repeat_mode", "OFF") ?: "OFF")
                    } catch (e: Exception) {
                        RepeatMode.OFF
                    }

                    Log.d("MusicPlayerManager", "Restored queue state: ${queueIds.size} songs, index ${_queueIndex.value}")
                }
            } catch (e: Exception) {
                Log.e("MusicPlayerManager", "Failed to restore queue state", e)
            }
        }
    }

    fun release() {
        saveQueueState()
        positionUpdateJob?.cancel()
        connectionRetryJob?.cancel()
        scope.cancel()
        mediaController?.release()
        controllerFuture?.let { future ->
            MediaController.releaseFuture(future)
        }
    }
}