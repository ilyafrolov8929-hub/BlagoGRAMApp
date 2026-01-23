package com.kia.blagogramapp.ui.screens

import android.Manifest
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.gson.Gson
import com.kia.blagogramapp.BaseScreen
import com.kia.blagogramapp.BlagoApi
import com.kia.blagogramapp.DesignEngine
import com.kia.blagogramapp.LayoutHelper
import com.kia.blagogramapp.MainActivity
import com.kia.blagogramapp.PostCell
import com.kia.blagogramapp.R
import com.kia.blagogramapp.SubscriptionManager
import com.kia.blagogramapp.api.ApiClient
import com.kia.blagogramapp.api.ApiRepository
import com.kia.blagogramapp.model.Post
import com.kia.blagogramapp.model.UserProfile
import com.kia.blagogramapp.ui.modules.FullPlayer
import com.kia.blagogramapp.ui.modules.PostCreationModule
import com.kia.blagogramapp.ui.modules.ProfileLayout
import com.kia.blagogramapp.ui.modules.ShareModule
import com.kia.blagogramapp.utils.MusicManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.forEach

// Добавляем параметр в конструктор
class ProfileScreen(context: Context, private val targetUserId: Int = 0) : BaseScreen(context) {
    val activity = context as? AppCompatActivity

    private lateinit var layout: ProfileLayout
    private lateinit var repo: ApiRepository
    private var fullPlayer: FullPlayer? = null

    private var isCurrentThemeDark: Boolean = false
    private var isCurrentUseMonet: Boolean = false

    private var currentProfileAvatar: String? = null
    private var currentProfileName: String? = null

    override fun createView(): View {
        layout = ProfileLayout(context)

        repo = ApiRepository(ApiClient.retrofit.create(BlagoApi::class.java))

        // Теперь используем значение из конструктора, а не из intent
        val myId = context.getSharedPreferences("app", MODE_PRIVATE).getInt("user_id", 0)
        val isMyProfile = (targetUserId == 0 || targetUserId == myId)
        val effectiveUserId = if (targetUserId == 0) myId else targetUserId

        layout.btnSettings.setOnClickListener {
            // Переход на экран настроек (теперь это тоже Screen)
            // presentFragment(SettingsScreen(context))
        }

        if (isMyProfile) {
            layout.btnSettings.visibility = View.VISIBLE
            layout.btnCreatePost.visibility = View.VISIBLE
            loadMyProfile()
            checkPermissions()
        } else {
            layout.btnSettings.visibility = View.GONE
            layout.btnCreatePost.visibility = View.GONE
            loadUserProfileFromCache(targetUserId)
            loadUserProfile(targetUserId)
        }

        layout.backButton.setOnClickListener { finishFragment() }
        layout.btnCreatePost.setOnClickListener {
            activity?.let { act ->
                PostCreationModule.show(act) {
                    loadUserPosts(effectiveUserId)
                }
            }
        }

        layout.onTrackClicked = { track ->
            MusicManager.playTrack(track)
        }

        layout.onAddMusicClicked = {
            (context as? MainActivity)?.musicPicker?.launch("audio/*")
        }

        layout.onMusicBadgeClick = {
            showFullPlayer()
        }

        observeMusic()

        return layout
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // ИСПРАВЛЕНО: Вызываем через activity
                activity?.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        MusicManager.scanMusic(context)
        layout.updateMusicList()
    }

    // ВНИМАНИЕ: Чтобы этот метод работал, его нужно вызвать из MainActivity.onRequestPermissionsResult
    fun handlePermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == 101) {
            MusicManager.scanMusic(context)
            layout.updateMusicList()
        }
    }

    private fun observeMusic() {
        activity?.lifecycleScope?.launchWhenStarted {
            combine(MusicManager.currentTrack, MusicManager.isPlaying) { track, playing ->
                track to playing
            }.collect { (track, playing) ->
                layout.updateMusicBadge(track, playing)
                if (track != null) {
                    fullPlayer?.bind(track, playing)
                }
            }
        }
    }

    fun closeFullPlayer() {
        fullPlayer?.animateClose(layout.musicBadge) {
            layout.removeView(fullPlayer)
            layout.topBar.visibility = View.VISIBLE
        }
    }

    private fun showFullPlayer() {
        val track = MusicManager.currentTrack.value ?: return
        if (fullPlayer == null) {
            fullPlayer = FullPlayer(context).apply {
                layoutParams = FrameLayout.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT)
                onCloseListener = { closeFullPlayer() }
            }
        }
        if (fullPlayer?.parent == null) {
            layout.addView(fullPlayer)
            fullPlayer?.animateOpen(layout.musicBadge)
            layout.topBar.visibility = View.GONE
        }
        fullPlayer?.bind(track, MusicManager.isPlaying.value)
    }

    private fun loadUserPosts(userId: Int) {
        val token = context.getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val posts = withContext(Dispatchers.IO) { repo.getUserPosts(token, userId) }
                displayPosts(posts)
            } catch (e: Exception) {
            }
        }
    }

    private fun displayPosts(posts: List<Post>) {
        layout.postsContainer.removeAllViews()
        val myId = context.getSharedPreferences("app", MODE_PRIVATE).getInt("user_id", 0)
        // ← Вот этого может не быть ↓
        val postsContainer = layout.postsContainer // или findViewById<LinearLayout>(R.id.posts_container)
        if (postsContainer.parent == null) {
            Log.e("PROFILE", "postsContainer НЕ добавлен в layout!!!")
        }
        if (posts.isEmpty()) {
            val emptyTv = android.widget.TextView(context).apply {
                text = "Нет записей"
                setTextColor(DesignEngine.Theme.colorTextSecondary)
                gravity = android.view.Gravity.CENTER
                setPadding(0, LayoutHelper.dp(20), 0, LayoutHelper.dp(20))
            }
            layout.postsContainer.addView(emptyTv)
            return
        }

        posts.forEach { post ->
            val cell = PostCell(context)

            // Заполняем данные из поста
            cell.contentText.text = post.content
            cell.timeView.text = post.createdAt.split(" ").lastOrNull()?.take(5) ?: ""

            // Картинка поста
            if (!post.image.isNullOrEmpty()) {
                cell.postImage.visibility = View.VISIBLE
                cell.postImage.load("https://blagogram.ru/posts/${post.image}")
            } else {
                cell.postImage.visibility = View.GONE
            }

            // Лайки
            cell.setLiked(post.liked)
            updateLikesCount(cell, post.likes)

            cell.likePill.setOnClickListener {
                toggleLike(post, cell)
            }

            cell.sharePill.setOnClickListener {
                ShareModule.show(context, post)
            }

            // Информация об авторе
            cell.setUserInfo(currentProfileName ?: "", currentProfileAvatar)

            // Удаление
            if (post.authorId == myId) {
                cell.setOnLongClickListener {
                    deletePost(post, cell)
                    true
                }
            }

            layout.postsContainer.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 12))
        }
    }

    private fun toggleLike(post: Post, cell: PostCell) {
        val token = context.getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val isLikedNow = withContext(Dispatchers.IO) { repo.likePost(token, post.id) }
                cell.setLiked(isLikedNow)

                // Визуальное обновление счетчика
                val currentCount = try { cell.likesCount.text.toString().toInt() } catch (e: Exception) { 0 }
                val newCount = if (isLikedNow) currentCount + 1 else (currentCount - 1).coerceAtLeast(0)
                updateLikesCount(cell, newCount)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun updateLikesCount(cell: PostCell, count: Int) {
        cell.likesCount.text = if (count > 0) count.toString() else ""
        cell.likesCount.visibility = if (count > 0) View.VISIBLE else View.GONE
    }

    private fun deletePost(post: Post, cell: View) {
        val token = context.getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val success = withContext(Dispatchers.IO) { repo.deletePost(token, post.id) }
                if (success) {
                    layout.postsContainer.removeView(cell)
                    Toast.makeText(context, "Пост удален", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка удаления", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadMyProfile() {
        val prefs = context.getSharedPreferences("app", MODE_PRIVATE)
        val login = prefs.getString("login", "")
        val displayName = prefs.getString("display_name", "")
        val statusText = prefs.getString("status", "")
        val avatar = prefs.getString("avatar", "")
        val myId = prefs.getInt("user_id", 0)
        val cover = prefs.getString("cover", "")

        loadUserProfileFromCache(myId)
        if (layout.userID.text.isNullOrEmpty() || layout.userID.text == "@") {
            updateUI(login, displayName, statusText, avatar, 0, 0, SubscriptionManager.isPremium(context), cover, myId)
        }
        if (myId != 0) loadUserProfile(myId)
    }

    private fun loadUserProfileFromCache(userId: Int) {
        val prefs = context.getSharedPreferences("app_cache", MODE_PRIVATE)
        val json = prefs.getString("profile_$userId", null)
        if (!json.isNullOrEmpty()) {
            try {
                val profile = Gson().fromJson(json, UserProfile::class.java)
                updateUI(profile.username, profile.display_name, profile.status, profile.avatar, profile.friends_count, profile.followers_count, profile.is_premium, profile.cover, profile.id)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun loadUserProfile(userId: Int) {
        val token = context.getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val profile = repo.getUserProfile(token, userId)
                val json = Gson().toJson(profile)
                context.getSharedPreferences("app_cache", MODE_PRIVATE).edit().putString("profile_$userId", json).apply()
                withContext(Dispatchers.Main) {
                    updateUI(profile.username, profile.display_name, profile.status, profile.avatar, profile.friends_count, profile.followers_count, profile.is_premium, profile.cover, profile.id)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun updateUI(login: String?, displayName: String?, statusText: String?, avatar: String?, friends: Int, followers: Int, isPremium: Boolean, cover: String?, userId: Int) {
        currentProfileName = displayName
        currentProfileAvatar = avatar

        layout.userID.text = "@$login"
        layout.profileUsername.text = displayName
        layout.status.text = statusText
        layout.friendsCount.text = friends.toString()
        layout.subsCount.text = followers.toString()

        val myLogin = context.getSharedPreferences("app", MODE_PRIVATE).getString("login", "")
        if (myLogin == login) {
            DesignEngine.setPremiumStatus(layout.premiumBadge, SubscriptionManager.isPremium(context))
        } else {
            DesignEngine.setPremiumStatus(layout.premiumBadge, isPremium)
        }

        val avatarUrl = "https://blagogram.ru/avatars/$avatar"
        layout.profileAvatar.load(avatarUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_user_placeholder)
            error(R.drawable.ic_user_placeholder)
        }

        if (!cover.isNullOrEmpty()) {
            val coverUrl = "https://blagogram.ru/covers/$cover"
            layout.profileCover.load(coverUrl) {
                crossfade(true)
                listener(onSuccess = { _, result ->
                    try {
                        val bmp = result.drawable.toBitmap()
                        val height = bmp.height.coerceAtMost(70)
                        if (height < 10) return@listener

                        val crop = Bitmap.createBitmap(bmp, 0, 0, bmp.width, height)

                        var brightness = 0
                        for (x in 0 until crop.width) {
                            for (y in 0 until crop.height) {
                                val c = crop.getPixel(x, y)
                                brightness += (
                                        Color.red(c) * 0.299 +
                                                Color.green(c) * 0.587 +
                                                Color.blue(c) * 0.114
                                        ).toInt()
                            }
                        }

                        brightness /= (crop.width * crop.height)
                        applyTopBarStyle(brightness > 140)
                    } catch (_: Exception) {}
                }, onError = { _, _ ->
                    applyTopBarStyle(false)
                })
            }
        } else {
            layout.profileCover.setImageDrawable(null)
            applyTopBarStyle(false)
        }

        // Загружаем посты только когда UI обновлен базовой инфой
        loadUserPosts(userId)
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