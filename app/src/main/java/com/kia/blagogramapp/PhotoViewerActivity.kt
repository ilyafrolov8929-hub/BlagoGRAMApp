package com.kia.blagogramapp

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.github.chrisbanes.photoview.PhotoView

class PhotoViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_viewer)

        // Получаем данные из Intent
        val url = intent.getStringExtra("image_url")
        val senderName = intent.getStringExtra("sender_name")
        val sentTime = intent.getStringExtra("sent_time")
        val caption = intent.getStringExtra("caption") // Текст под фото

        // Находим Views
        val imageView = findViewById<PhotoView>(R.id.fullScreenImage)
        val tvSenderName = findViewById<TextView>(R.id.tvSenderName)
        val tvSentTime = findViewById<TextView>(R.id.tvSentTime)
        val tvCaption = findViewById<TextView>(R.id.tvCaption)
        val bottomOverlay = findViewById<View>(R.id.bottomOverlay)
        val btnClose = findViewById<ImageView>(R.id.btnClose)

        // Установка данных
        tvSenderName.text = senderName ?: "Неизвестный"
        tvSentTime.text = sentTime ?: ""

        // Логика для подписи (caption)
        if (!caption.isNullOrEmpty()) {
            tvCaption.text = caption
            tvCaption.visibility = View.VISIBLE
            bottomOverlay.visibility = View.VISIBLE
        } else {
            tvCaption.visibility = View.GONE
            bottomOverlay.visibility = View.GONE
        }

        // Загрузка изображения
        if (url != null) {
            imageView.load(url) {
                crossfade(true)
                // Можно добавить placeholder или error drawable
                // placeholder(R.drawable.loading_spinner)
            }
        }

        // Закрытие
        btnClose.setOnClickListener {
            finish()
        }

        // Опционально: скрывать интерфейс при тапе по фото
        imageView.setOnClickListener {
            toggleUiVisibility(tvSenderName, tvSentTime, tvCaption, bottomOverlay, btnClose, findViewById(R.id.topOverlay))
        }
    }

    private var isUiVisible = true

    private fun toggleUiVisibility(vararg views: View) {
        isUiVisible = !isUiVisible
        val visibility = if (isUiVisible) View.VISIBLE else View.GONE

        views.forEach { view ->
            // Скрываем подпись только если она вообще должна быть (есть текст)
            if (view.id == R.id.tvCaption || view.id == R.id.bottomOverlay) {
                val caption = intent.getStringExtra("caption")
                if (!caption.isNullOrEmpty()) view.visibility = visibility
            } else {
                view.visibility = visibility
            }
        }
    }
}
