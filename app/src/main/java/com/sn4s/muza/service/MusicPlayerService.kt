package com.sn4s.muza.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.sn4s.muza.MainActivity
import com.sn4s.muza.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MusicPlayerService : MediaSessionService() {

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL_ID = "music_playback_channel"
        private const val NOTIFICATION_CHANNEL_NAME = "Music Playback"
    }

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()

        initializePlayer()
        initializeMediaSession()
        setupNotificationChannel()
    }

    private fun initializePlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                updateNotification()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateNotification()
            }
        })
    }

    private fun initializeMediaSession() {
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(MediaSessionCallback())
            .build()
    }

    private fun setupNotificationChannel() {
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music playback controls"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val currentMediaItem = player.currentMediaItem
        if (currentMediaItem != null && player.playbackState != Player.STATE_IDLE) {
            val notification = createNotification(currentMediaItem)
            startForeground(NOTIFICATION_ID, notification)
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    private fun createNotification(mediaItem: MediaItem): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(mediaItem.mediaMetadata.title ?: "Unknown Title")
            .setContentText(mediaItem.mediaMetadata.artist ?: "Unknown Artist")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private inner class MediaSessionCallback : MediaSession.Callback {

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val updatedMediaItems = mediaItems.map { mediaItem ->
                // Ensure URI is properly set
                val uri = mediaItem.requestMetadata.mediaUri ?: mediaItem.localConfiguration?.uri
                if (uri != null) {
                    mediaItem.buildUpon()
                        .setUri(uri)
                        .build()
                } else {
                    // If no URI, create a proper MediaItem
                    MediaItem.Builder()
                        .setMediaId(mediaItem.mediaId)
                        .setUri(mediaItem.mediaId) // Fallback to mediaId as URI
                        .setMediaMetadata(mediaItem.mediaMetadata)
                        .build()
                }
            }.toMutableList()
            return Futures.immediateFuture(updatedMediaItems)
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            // Return empty list - no resumption for now
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(
                    emptyList(),
                    0,
                    0L
                )
            )
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            // Process media items to ensure they have valid URIs
            val processedItems = mediaItems.map { mediaItem ->
                val uri = mediaItem.requestMetadata.mediaUri ?: mediaItem.localConfiguration?.uri
                if (uri != null) {
                    mediaItem.buildUpon()
                        .setUri(uri)
                        .build()
                } else {
                    // Create proper MediaItem with URI
                    MediaItem.Builder()
                        .setMediaId(mediaItem.mediaId)
                        .setUri(mediaItem.mediaId) // Use mediaId as URI
                        .setMediaMetadata(mediaItem.mediaMetadata)
                        .build()
                }
            }

            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(
                    processedItems,
                    startIndex.coerceAtLeast(0),
                    startPositionMs.coerceAtLeast(0L)
                )
            )
        }
    }
}