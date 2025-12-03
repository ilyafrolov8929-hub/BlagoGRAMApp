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

class ProfileActivity : AppCompatActivity() {

    private lateinit var profileUsername: TextView
    private lateinit var userID: TextView
    private lateinit var status: TextView

    private lateinit var avatarView: ImageView
    private lateinit var profileCover: ImageView

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

        avatarView = findViewById(R.id.profileAvatar)
        profileCover = findViewById(R.id.profileCover)

        val prefs = getSharedPreferences("app", MODE_PRIVATE)

        // подставляем текст
        userID.text = "@" + prefs.getString("login", "")
        profileUsername.text = prefs.getString("display_name", "")
        status.text = prefs.getString("status", "")

        // грузим аватар
        val avatar = prefs.getString("avatar", "")
        avatarView.load("https://blagogram.ru/avatars/$avatar") {
            crossfade(true)
            placeholder(R.drawable.ic_user_placeholder)
            error(R.drawable.ic_user_placeholder)
        }

        // грузим обложку и анализируем яркость
        profileCover.load("https://blagogram.ru/avatars/$avatar") {
            crossfade(true)
            placeholder(R.drawable.ic_user_placeholder)
            error(R.drawable.ic_user_placeholder)

            listener(
                onSuccess = { _, result ->
                    try {
                        val bmp: Bitmap = result.drawable.toBitmap()

                        // берём верхнюю полоску (под статусбар)
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

        // кнопка назад
        findViewById<ImageView>(R.id.exit_profile).setOnClickListener {
            finish()
        }

        // кнопка настройки (⚙️)
        findViewById<ImageView>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, ProfileEditActivity::class.java))
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
