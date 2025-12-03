package com.kia.blagogramapp

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.github.chrisbanes.photoview.PhotoView

class PhotoViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_viewer)

        val url = intent.getStringExtra("image_url")
        val imageView = findViewById<PhotoView>(R.id.fullScreenImage)
        
        // Загружаем изображение
        if (url != null) {
            imageView.load(url) {
                crossfade(true)
            }
        }

        findViewById<ImageView>(R.id.btnClose).setOnClickListener {
            finish()
        }
    }
}
