package com.kia.blagogramapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.gson.Gson
import com.kia.blagogramapp.api.ApiClient
import com.kia.blagogramapp.api.ApiRepository
import com.kia.blagogramapp.model.UserProfile
import com.kia.blagogramapp.models.Track
import com.kia.blagogramapp.ui.modules.FullPlayer
import com.kia.blagogramapp.ui.modules.MiniPlayer
import com.kia.blagogramapp.ui.modules.PostCell
import com.kia.blagogramapp.ui.modules.ProfileLayout
import com.kia.blagogramapp.utils.MusicManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var layout: ProfileLayout
    private lateinit var repo: ApiRepository
    
    private var miniPlayer: MiniPlayer? = null
    private var fullPlayer: FullPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        DesignEngine.init(this)
        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        DesignEngine.Theme.apply(
            themePrefs.getBoolean("is_dark_theme", false),
            themePrefs.getBoolean("use_monet", false)
        )

        // Настройка Edge-to-Edge для прозрачности статус-бара и навигационной панели
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        super.onCreate(savedInstanceState)
        
        layout = ProfileLayout(this)
        setContentView(layout)

        repo = ApiRepository(ApiClient.retrofit.create(BlagoApi::class.java))

        val targetUserId = intent.getIntExtra("user_id", 0)
        val myId = getSharedPreferences("app", MODE_PRIVATE).getInt("user_id", 0)
        val isMyProfile = (targetUserId == 0 || targetUserId == myId)

        layout.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        if (isMyProfile) {
            layout.btnSettings.visibility = View.VISIBLE
            loadMyProfile()
            checkPermissions()
        } else {
            layout.btnSettings.visibility = View.GONE
            loadUserProfileFromCache(targetUserId)
            loadUserProfile(targetUserId)
        }

        layout.backButton.setOnClickListener { finish() }
        layout.btnCreatePost.setOnClickListener { createTestPost() }
        
        layout.onTrackClicked = { track ->
            MusicManager.playTrack(track)
        }

        observeMusic()
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        val toRequest = permissions.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (toRequest.isNotEmpty()) {
            requestPermissions(toRequest.toTypedArray(), 101)
        } else {
            MusicManager.scanMusic(this)
            layout.updateMusicList()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            MusicManager.scanMusic(this)
            layout.updateMusicList()
        }
    }

    private fun observeMusic() {
        lifecycleScope.launchWhenStarted {
            MusicManager.currentTrack.collectLatest { track ->
                if (track != null) {
                    updateMiniPlayer(track, MusicManager.isPlaying.value)
                    fullPlayer?.bind(track, MusicManager.isPlaying.value)
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            MusicManager.isPlaying.collectLatest { playing ->
                val track = MusicManager.currentTrack.value
                if (track != null) {
                    updateMiniPlayer(track, playing)
                    fullPlayer?.bind(track, playing)
                }
            }
        }
    }

    private fun updateMiniPlayer(track: Track, playing: Boolean) {
        if (miniPlayer == null) {
            miniPlayer = MiniPlayer(this).apply {
                val p = LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, android.view.Gravity.BOTTOM, 16, 0, 16, 32) // Увеличил отступ снизу для красоты
                layoutParams = p
                setOnClickListener { showFullPlayer() }
            }
            layout.addView(miniPlayer)
        }
        miniPlayer?.bind(track, playing)
        miniPlayer?.visibility = if (fullPlayer != null && fullPlayer!!.parent != null) View.GONE else View.VISIBLE
    }

    fun onFullPlayerClosed() {
        miniPlayer?.visibility = View.VISIBLE
        layout.topBar.visibility = View.VISIBLE
    }

    private fun showFullPlayer() {
        val track = MusicManager.currentTrack.value ?: return
        if (fullPlayer == null) {
            fullPlayer = FullPlayer(this).apply {
                layoutParams = FrameLayout.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT)
            }
        }
        if (fullPlayer?.parent == null) {
            layout.addView(fullPlayer)
        }
        miniPlayer?.visibility = View.GONE
        layout.topBar.visibility = View.GONE
        fullPlayer?.bind(track, MusicManager.isPlaying.value)
    }

    private fun createTestPost(text: String = "Новый тестовый пост создан через DesignEngine!") {
        val postCell = PostCell(this)
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        val name = prefs.getString("display_name", "Пользователь") ?: "Пользователь"
        val avatar = prefs.getString("avatar", "") ?: ""
        
        postCell.setData(
            name = name,
            time = "только что",
            text = text,
            imageUrl = "https://blagogram.ru/avatars/$avatar",
            likes = (0..100).random(),
            comments = (0..50).random(),
            views = "${(1..10).random()}K",
            avatarUrl = "https://blagogram.ru/avatars/$avatar"
        )
        layout.postsContainer.addView(postCell, 0)
    }

    private fun loadMyProfile() {
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        val login = prefs.getString("login", "")
        val displayName = prefs.getString("display_name", "")
        val statusText = prefs.getString("status", "")
        val avatar = prefs.getString("avatar", "")
        val myId = prefs.getInt("user_id", 0)
        
        loadUserProfileFromCache(myId)
        if (layout.userID.text.isNullOrEmpty() || layout.userID.text == "@") {
             updateUI(login, displayName, statusText, avatar, 0, 0)
        }
        if (myId != 0) loadUserProfile(myId)
    }

    private fun loadUserProfileFromCache(userId: Int) {
        val prefs = getSharedPreferences("app_cache", MODE_PRIVATE)
        val json = prefs.getString("profile_$userId", null)
        if (!json.isNullOrEmpty()) {
            try {
                val profile = Gson().fromJson(json, UserProfile::class.java)
                updateUI(profile.username, profile.display_name, profile.status, profile.avatar, profile.friends_count, profile.followers_count)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun loadUserProfile(userId: Int) {
        val token = getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val profile = repo.getUserProfile(token, userId)
                val json = Gson().toJson(profile)
                getSharedPreferences("app_cache", MODE_PRIVATE).edit().putString("profile_$userId", json).apply()
                runOnUiThread {
                    updateUI(profile.username, profile.display_name, profile.status, profile.avatar, profile.friends_count, profile.followers_count)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun updateUI(login: String?, displayName: String?, statusText: String?, avatar: String?, friends: Int, followers: Int) {
        layout.userID.text = "@$login"
        layout.profileUsername.text = displayName
        layout.status.text = statusText
        layout.friendsCount.text = friends.toString()
        layout.subsCount.text = followers.toString()
        val avatarUrl = "https://blagogram.ru/avatars/$avatar"
        layout.profileAvatar.load(avatarUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_user_placeholder)
            error(R.drawable.ic_user_placeholder)
        }
        layout.profileCover.load(avatarUrl) {
            crossfade(true)
            listener(onSuccess = { _, result ->
                try {
                    val bmp: Bitmap = result.drawable.toBitmap()
                    val height = bmp.height.coerceAtMost(70)
                    if (height < 10) return@listener
                    val crop = Bitmap.createBitmap(bmp, 0, 0, bmp.width, height)
                    var brightness = 0
                    for (x in 0 until crop.width) {
                        for (y in 0 until crop.height) {
                            val color = crop.getPixel(x, y)
                            brightness += (Color.red(color) * 0.299 + Color.green(color) * 0.587 + Color.blue(color) * 0.114).toInt()
                        }
                    }
                    brightness /= (crop.width * crop.height)
                    applyTopBarStyle(brightness > 140)
                } catch (_: Exception) { }
            })
        }
    }

    private fun applyTopBarStyle(isLight: Boolean) {
        if (isLight) {
            layout.userID.setTextColor(Color.BLACK)
            layout.userID.setShadowLayer(0f, 0f, 0f, 0)
        } else {
            layout.userID.setTextColor(Color.WHITE)
            layout.userID.setShadowLayer(2f, 1f, 1f, Color.BLACK)
        }
    }
}
