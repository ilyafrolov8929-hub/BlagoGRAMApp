package com.kia.blagogramapp.utils

import android.app.*
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import com.kia.blagogramapp.ProfileActivity
import com.kia.blagogramapp.R
import com.kia.blagogramapp.models.Track
import java.io.InputStream

class MusicService : Service() {

    private val CHANNEL_ID = "music_channel"
    private val NOTIFICATION_ID = 101
    private var mediaSession: MediaSessionCompat? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(this, "BlagoMusicSession").apply {
            isActive = true
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() { MusicManager.togglePlayPause() }
                override fun onPause() { MusicManager.togglePlayPause() }
                override fun onSkipToNext() { MusicManager.playNext() }
                override fun onSkipToPrevious() { MusicManager.playPrevious() }
                override fun onSeekTo(pos: Long) { 
                    MusicManager.seekTo(pos.toInt())
                    updateMediaSession(MusicManager.currentTrack.value ?: return)
                }
            })
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val track = MusicManager.currentTrack.value

        when (action) {
            "PLAY_PAUSE" -> MusicManager.togglePlayPause()
            "NEXT" -> MusicManager.playNext()
            "PREV" -> MusicManager.playPrevious()
            "STOP" -> {
                stopForeground(true)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        if (track != null) {
            updateMediaSession(track)
            showNotification(track, MusicManager.isPlaying.value)
        }

        return START_NOT_STICKY
    }

    private fun updateMediaSession(track: Track) {
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.duration)
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, getAlbumArt(track.albumArtUri))
            .build()
        mediaSession?.setMetadata(metadata)

        val state = if (MusicManager.isPlaying.value) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        mediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(state, MusicManager.getCurrentPosition().toLong(), 1f)
                // Добавляем ACTION_SEEK_TO чтобы появился ползунок
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or 
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or 
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SEEK_TO
                )
                .build()
        )
    }

    private fun showNotification(track: Track, isPlaying: Boolean) {
        createNotificationChannel()

        val intent = Intent(this, ProfileActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action(R.drawable.ic_pause, "Pause", getPendingIntent("PLAY_PAUSE"))
        } else {
            NotificationCompat.Action(R.drawable.ic_play, "Play", getPendingIntent("PLAY_PAUSE"))
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music)
            .setContentTitle(track.title)
            .setContentText(track.artist)
            .setLargeIcon(getAlbumArt(track.albumArtUri))
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_prev, "Prev", getPendingIntent("PREV"))
            .addAction(playPauseAction)
            .addAction(R.drawable.ic_next, "Next", getPendingIntent("NEXT"))
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession?.sessionToken)
                .setShowActionsInCompactView(0, 1, 2))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setSilent(true)
            .setOngoing(isPlaying)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun getPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply { this.action = action }
        return PendingIntent.getService(this, action.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun getAlbumArt(uriString: String?): Bitmap? {
        if (uriString == null) return null
        return try {
            val uri = Uri.parse(uriString)
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Music Player", NotificationManager.IMPORTANCE_LOW).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession?.release()
    }
}
