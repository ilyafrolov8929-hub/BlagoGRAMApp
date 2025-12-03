package com.kia.blagogramapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import coil.load
import androidx.core.graphics.drawable.toBitmap
import com.kia.blagogramapp.api.ApiClient
import com.kia.blagogramapp.api.ApiRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.kia.blagogramapp.model.UserProfile

class ProfileActivity : AppCompatActivity() {

    private lateinit var profileUsername: TextView
    private lateinit var userID: TextView
    private lateinit var status: TextView
    private lateinit var friendsCount: TextView
    private lateinit var subsCount: TextView
    private lateinit var avatarView: ImageView
    private lateinit var profileCover: ImageView
    private lateinit var repo: ApiRepository

    override fun onCreate(savedInstanceState: Bundle?) {

        // делать обложку под статусбар
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.statusBarColor = Color.TRANSPARENT

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        profileUsername = findViewById(R.id.profileUsername)
        userID = findViewById(R.id.userID)
        status = findViewById(R.id.status)
        friendsCount = findViewById(R.id.friendsCount)
        subsCount = findViewById(R.id.subsCount)

        avatarView = findViewById(R.id.profileAvatar)
        profileCover = findViewById(R.id.profileCover)

        repo = ApiRepository(ApiClient.retrofit.create(BlagoApi::class.java))

        val targetUserId = intent.getIntExtra("user_id", 0)
        val myId = getSharedPreferences("app", MODE_PRIVATE).getInt("user_id", 0)
        val isMyProfile = (targetUserId == 0 || targetUserId == myId)

        // кнопка настройки (⚙️)
        val settingsBtn = findViewById<ImageView>(R.id.btnSettings)
        settingsBtn.setOnClickListener {
            startActivity(Intent(this, ProfileEditActivity::class.java))
        }

        if (isMyProfile) {
            settingsBtn.visibility = View.VISIBLE
            loadMyProfile()
        } else {
            settingsBtn.visibility = View.GONE
            // Попытаемся загрузить кэш для чужого профиля, если есть
            loadUserProfileFromCache(targetUserId)
            // И грузим с сети
            loadUserProfile(targetUserId)
        }

        // кнопка назад
        findViewById<ImageView>(R.id.exit_profile).setOnClickListener {
            finish()
        }
    }

    private fun loadMyProfile() {
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        val login = prefs.getString("login", "")
        val displayName = prefs.getString("display_name", "")
        val statusText = prefs.getString("status", "")
        val avatar = prefs.getString("avatar", "")
        
        // Сохранять эти данные тоже локально, пока по нулям, если не обновляли
        // Но мы можем попробовать достать из кэша профиля
        val myId = prefs.getInt("user_id", 0)
        
        loadUserProfileFromCache(myId) // Это может перезаписать данные из prefs если кэш свежее

        // Если кэша не было, покажем что есть в prefs
        if (userID.text.isNullOrEmpty()) {
             updateUI(login, displayName, statusText, avatar, 0, 0)
        }
        
        // Обновить данные с сервера
        if (myId != 0) {
            loadUserProfile(myId)
        }
    }

    private fun loadUserProfileFromCache(userId: Int) {
        val prefs = getSharedPreferences("app_cache", MODE_PRIVATE)
        val json = prefs.getString("profile_$userId", null)
        if (!json.isNullOrEmpty()) {
            try {
                val profile = Gson().fromJson(json, UserProfile::class.java)
                updateUI(
                    profile.username, 
                    profile.display_name, 
                    profile.status, 
                    profile.avatar,
                    profile.friends_count,
                    profile.followers_count
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadUserProfile(userId: Int) {
        val token = getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val profile = repo.getUserProfile(token, userId)
                
                // Кэшируем профиль
                val json = Gson().toJson(profile)
                getSharedPreferences("app_cache", MODE_PRIVATE)
                    .edit()
                    .putString("profile_$userId", json)
                    .apply()

                runOnUiThread {
                    updateUI(
                        profile.username, 
                        profile.display_name, 
                        profile.status, 
                        profile.avatar,
                        profile.friends_count,
                        profile.followers_count
                    )
                    
                    // Если это наш профиль, обновим и основные настройки
                    val myId = getSharedPreferences("app", MODE_PRIVATE).getInt("user_id", 0)
                    if (userId == myId) {
                        getSharedPreferences("app", MODE_PRIVATE).edit()
                            .putString("display_name", profile.display_name)
                            .putString("status", profile.status)
                            .putString("avatar", profile.avatar)
                            .apply()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateUI(login: String?, displayName: String?, statusText: String?, avatar: String?, friends: Int, followers: Int) {
        userID.text = "@$login"
        profileUsername.text = displayName
        status.text = statusText
        
        friendsCount.text = friends.toString()
        subsCount.text = followers.toString()

        val avatarUrl = "https://blagogram.ru/avatars/$avatar"
        
        avatarView.load(avatarUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_user_placeholder)
            error(R.drawable.ic_user_placeholder)
        }

        profileCover.load(avatarUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_user_placeholder)
            error(R.drawable.ic_user_placeholder)

            listener(
                onSuccess = { _, result ->
                    try {
                        val bmp: Bitmap = result.drawable.toBitmap()
                        val height = bmp.height.coerceAtMost(70)
                        if (height < 10) return@listener

                        val crop = Bitmap.createBitmap(bmp, 0, 0, bmp.width, height)

                        var brightness = 0
                        val total = crop.width * crop.height

                        for (x in 0 until crop.width) {
                            for (y in 0 until crop.height) {
                                val color = crop.getPixel(x, y)
                                brightness += (Color.red(color) * 0.299 +
                                        Color.green(color) * 0.587 +
                                        Color.blue(color) * 0.114).toInt()
                            }
                        }
                        brightness /= total
                        applyTopBarStyle(brightness > 140)

                    } catch (_: Exception) { }
                }
            )
        }
    }

    private fun applyTopBarStyle(isLight: Boolean) {
        val exit = findViewById<ImageView>(R.id.exit_profile)
        val settings = findViewById<ImageView>(R.id.btnSettings)
        val user = findViewById<TextView>(R.id.userID)

        if (isLight) {
            exit.setColorFilter(Color.BLACK)
            settings.setColorFilter(Color.BLACK)
            user.setTextColor(Color.BLACK)
        } else {
            exit.setColorFilter(Color.WHITE)
            settings.setColorFilter(Color.WHITE)
            user.setTextColor(Color.WHITE)
        }
    }
}
