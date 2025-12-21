package com.kia.blagogramapp.utils

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import com.kia.blagogramapp.models.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.FileOutputStream

object MusicManager : AudioManager.OnAudioFocusChangeListener {
    private var mediaPlayer: MediaPlayer? = null
    private var appContext: Context? = null
    private var audioManager: AudioManager? = null
    
    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _playlist = MutableStateFlow<List<Track>>(emptyList())
    val playlist: StateFlow<List<Track>> = _playlist

    fun initPlayer(context: Context) {
        appContext = context.applicationContext
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setOnCompletionListener { playNext() }
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        if (audioManager == null) return false
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(this)
                .build()
            audioManager!!.requestAudioFocus(request)
        } else {
            audioManager!!.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> pauseWithFade()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pauseWithFade()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> fadeVolume(0.2f)
            AudioManager.AUDIOFOCUS_GAIN -> {
                fadeVolume(1.0f)
                if (!_isPlaying.value) togglePlayPause()
            }
        }
    }

    private fun fadeVolume(target: Float) {
        ValueAnimator.ofFloat(if (target > 0.5) 0.2f else 1.0f, target).apply {
            duration = 500
            addUpdateListener { 
                val volume = it.animatedValue as Float
                mediaPlayer?.setVolume(volume, volume) 
            }
            start()
        }
    }

    private fun pauseWithFade() {
        if (_isPlaying.value) togglePlayPause()
    }

    fun scanMusic(context: Context) {
        initPlayer(context)
        val musicDir = File(context.filesDir, "music")
        if (!musicDir.exists()) musicDir.mkdirs()
        
        val coversDir = File(context.filesDir, "music_covers")
        if (!coversDir.exists()) coversDir.mkdirs()

        val tracks = mutableListOf<Track>()
        val mmr = MediaMetadataRetriever()
        
        musicDir.listFiles()?.forEach { file ->
            if (file.extension == "mp3" || file.extension == "m4a" || file.extension == "wav") {
                try {
                    mmr.setDataSource(file.absolutePath)
                    val title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: file.nameWithoutExtension
                    val artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
                    val duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
                    
                    val coverFile = File(coversDir, "${file.nameWithoutExtension}.jpg")
                    if (!coverFile.exists()) {
                        val art = mmr.embeddedPicture
                        if (art != null) {
                            FileOutputStream(coverFile).use { it.write(art) }
                        }
                    }

                    tracks.add(Track(
                        id = file.hashCode().toLong(),
                        title = title,
                        artist = artist,
                        data = file.absolutePath,
                        duration = duration,
                        albumArtUri = if (coverFile.exists()) coverFile.absolutePath else ""
                    ))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        _playlist.value = tracks
    }

    fun addTrack(context: Context, uri: Uri) {
        val musicDir = File(context.filesDir, "music")
        if (!musicDir.exists()) musicDir.mkdirs()

        val fileName = "track_${System.currentTimeMillis()}.mp3"
        val destFile = File(musicDir, fileName)

        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            scanMusic(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteTrack(context: Context, track: Track) {
        val file = File(track.data)
        if (file.exists()) {
            if (file.delete()) {
                val coverFile = File(track.albumArtUri)
                if (coverFile.exists()) coverFile.delete()

                if (_currentTrack.value?.data == track.data) {
                    mediaPlayer?.stop()
                    _currentTrack.value = null
                    _isPlaying.value = false
                }
                scanMusic(context)
            }
        }
    }

    private fun updateService() {
        appContext?.let { it.startService(Intent(it, MusicService::class.java)) }
    }

    fun playTrack(track: Track) {
        if (!requestAudioFocus()) return
        mediaPlayer?.reset()
        mediaPlayer?.setDataSource(track.data)
        mediaPlayer?.prepare()
        mediaPlayer?.start()
        _currentTrack.value = track
        _isPlaying.value = true
        updateService()
    }

    fun togglePlayPause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
            } else {
                if (requestAudioFocus()) {
                    it.start()
                    _isPlaying.value = true
                }
            }
            updateService()
        }
    }

    fun playNext() {
        val current = _currentTrack.value ?: return
        val list = _playlist.value
        val index = list.indexOf(current)
        if (index != -1 && index < list.size - 1) playTrack(list[index + 1])
    }

    fun playPrevious() {
        val current = _currentTrack.value ?: return
        val list = _playlist.value
        val index = list.indexOf(current)
        if (index > 0) playTrack(list[index - 1])
    }

    fun getDuration(): Int = mediaPlayer?.duration ?: 0
    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0
    fun seekTo(pos: Int) = mediaPlayer?.seekTo(pos)
}
