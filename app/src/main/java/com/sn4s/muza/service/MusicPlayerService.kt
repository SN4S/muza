package com.sn4s.muza.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.sn4s.muza.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MusicPlayerService : MediaLibraryService() {

    private var mediaLibrarySession: MediaLibrarySession? = null
    private lateinit var player: ExoPlayer

    override fun onCreate() {
        super.onCreate()
        initializeSessionAndPlayer()
    }

    private fun initializeSessionAndPlayer() {
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        // Add player error handling
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("MusicPlayerService", "Player error: ${error.message}", error)
                // Attempt recovery
                if (player.playbackState == Player.STATE_IDLE) {
                    player.prepare()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_ENDED -> {
                        // Handle playback completion
                        android.util.Log.d("MusicPlayerService", "Playback ended")
                    }
                    Player.STATE_BUFFERING -> {
                        android.util.Log.d("MusicPlayerService", "Buffering...")
                    }
                }
            }
        })

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, MediaLibrarySessionCallback())
            .setSessionActivity(sessionActivityPendingIntent)
            .setId("MuzaPlayerSession") // Consistent session ID for restoration
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        mediaLibrarySession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Handle task removal gracefully
        val player = mediaLibrarySession?.player
        if (player?.playWhenReady != true || player.mediaItemCount == 0) {
            // Stop service if not playing
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        try {
            mediaLibrarySession?.run {
                // Save session state before destroying
                saveSessionState()

                // Properly release resources
                player.release()
                release()
                mediaLibrarySession = null
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicPlayerService", "Error during destroy", e)
        }
        super.onDestroy()
    }

    private fun saveSessionState() {
        // Save current queue and position for restoration
        val player = mediaLibrarySession?.player ?: return

        try {
            val prefs = getSharedPreferences("player_session", MODE_PRIVATE)
            prefs.edit()
                .putLong("position", player.currentPosition)
                .putInt("media_item_index", player.currentMediaItemIndex)
                .putBoolean("play_when_ready", player.playWhenReady)
                .apply()
        } catch (e: Exception) {
            android.util.Log.e("MusicPlayerService", "Failed to save session state", e)
        }
    }

    private fun restoreSessionState() {
        val player = mediaLibrarySession?.player ?: return

        try {
            val prefs = getSharedPreferences("player_session", MODE_PRIVATE)
            val position = prefs.getLong("position", 0L)
            val mediaItemIndex = prefs.getInt("media_item_index", 0)
            val playWhenReady = prefs.getBoolean("play_when_ready", false)

            if (player.mediaItemCount > 0 && mediaItemIndex < player.mediaItemCount) {
                player.seekTo(mediaItemIndex, position)
                player.playWhenReady = playWhenReady
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicPlayerService", "Failed to restore session state", e)
        }
    }

    private inner class MediaLibrarySessionCallback : MediaLibrarySession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            // Grant all available commands for full functionality
            val sessionCommands = androidx.media3.session.SessionCommands.Builder().build()
            val playerCommands = Player.Commands.Builder()
                .addAllCommands()  // This grants all available player commands
                .build()

            return MediaSession.ConnectionResult.accept(sessionCommands, playerCommands)
        }

        override fun onPostConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ) {
            // Restore session state when controller connects
            restoreSessionState()
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            // Validate and process media items
            val processedItems = mediaItems.mapNotNull { mediaItem ->
                try {
                    // Ensure valid URI and metadata
                    if (mediaItem.localConfiguration?.uri != null || mediaItem.requestMetadata.mediaUri != null) {
                        mediaItem
                    } else {
                        android.util.Log.w("MusicPlayerService", "Invalid media item: ${mediaItem.mediaId}")
                        null
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MusicPlayerService", "Error processing media item", e)
                    null
                }
            }.toMutableList()

            return Futures.immediateFuture(processedItems)
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            return when (customCommand.customAction) {
                "SAVE_SESSION" -> {
                    saveSessionState()
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                "RESTORE_SESSION" -> {
                    restoreSessionState()
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                else -> super.onCustomCommand(session, controller, customCommand, args)
            }
        }

        // Library methods for browse functionality
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: MediaLibraryService.LibraryParams?
        ): ListenableFuture<androidx.media3.session.LibraryResult<MediaItem>> {
            return Futures.immediateFuture(
                androidx.media3.session.LibraryResult.ofItem(createBrowsableMediaItem(), params)
            )
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: MediaLibraryService.LibraryParams?
        ): ListenableFuture<androidx.media3.session.LibraryResult<ImmutableList<MediaItem>>> {
            // Could be extended to provide browsable content
            return Futures.immediateFuture(
                androidx.media3.session.LibraryResult.ofItemList(
                    ImmutableList.of(),
                    params
                )
            )
        }

        private fun createBrowsableMediaItem(): MediaItem {
            return MediaItem.Builder()
                .setMediaId("root")
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setTitle("Muza Music")
                        .build()
                )
                .build()
        }
    }
}