package com.kia.blagogramapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class StartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        window.statusBarColor = Color.TRANSPARENT
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        val savedToken = prefs.getString("token", "")

        if (!savedToken.isNullOrEmpty()) {
            startActivity(Intent(this, ChatsActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_start)

        val btn = findViewById<Button>(R.id.btnStart)
        btn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}
