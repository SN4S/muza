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
import com.sn4s.muza.data.model.Song
import com.sn4s.muza.service.MusicPlayerService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var positionUpdateJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var connectionRetryJob: Job? = null

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

    private val _playlist = MutableStateFlow<List<Song>>(emptyList())
    val playlist: StateFlow<List<Song>> = _playlist.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    init {
        startService()
        initializeController()
    }

    private fun startService() {
        val intent = Intent(context, MusicPlayerService::class.java)
        context.startForegroundService(intent)
    }

    private fun initializeController() {
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
                    setupPlayerListener()
                } else {
                    Log.w("MusicPlayerManager", "MediaController not connected")
                    scheduleReconnection()
                }
            } catch (e: Exception) {
                Log.e("MusicPlayerManager", "Failed to connect to service", e)
                scheduleReconnection()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun scheduleReconnection() {
        connectionRetryJob?.cancel()
        connectionRetryJob = scope.launch {
            delay(2000) // Wait 2 seconds before retry
            Log.d("MusicPlayerManager", "Retrying connection to MediaController")
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
                        val song = _playlist.value.find { it.id == songId }
                        _currentSong.value = song
                        _currentIndex.value = _playlist.value.indexOf(song)
                    }
                }
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

    fun playSong(song: Song, baseUrl: String = "http://192.168.88.188:8000") {
        if (!ensureConnected()) {
            // Schedule for when connected
            scope.launch {
                delay(1000)
                playSong(song, baseUrl)
            }
            return
        }

        val streamUrl = "$baseUrl/songs/${song.id}/stream"
        val mediaItem = MediaItem.Builder()
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

        mediaController?.let { controller ->
            controller.setMediaItem(mediaItem)
            controller.prepare()
            controller.play()
        }

        _currentSong.value = song
        _playlist.value = listOf(song)
        _currentIndex.value = 0
    }

    fun playPlaylist(songs: List<Song>, startIndex: Int = 0, baseUrl: String = "http://192.168.88.188:8000") {
        if (!ensureConnected()) {
            // Schedule for when connected
            scope.launch {
                delay(1000)
                playPlaylist(songs, startIndex, baseUrl)
            }
            return
        }

        val mediaItems = songs.map { song ->
            val streamUrl = "$baseUrl/songs/${song.id}/stream"
            MediaItem.Builder()
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

        mediaController?.let { controller ->
            controller.setMediaItems(mediaItems, startIndex, 0)
            controller.prepare()
            controller.play()
        }

        _playlist.value = songs
        _currentIndex.value = startIndex
        _currentSong.value = songs.getOrNull(startIndex)
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
            mediaController?.seekToNext()
        }
    }

    fun skipToPrevious() {
        if (ensureConnected()) {
            mediaController?.seekToPrevious()
        }
    }

    fun seekToIndex(index: Int) {
        if (ensureConnected() && index in 0 until _playlist.value.size) {
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
        _playlist.value = emptyList()
        _currentIndex.value = 0
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
        _currentSong.value = null
        _playlist.value = emptyList()
        _currentIndex.value = 0
        _currentPosition.value = 0L
        _duration.value = 0L
    }

    fun release() {
        positionUpdateJob?.cancel()
        connectionRetryJob?.cancel()
        scope.cancel()
        mediaController?.release()
        controllerFuture?.let { future ->
            MediaController.releaseFuture(future)
        }
    }
}