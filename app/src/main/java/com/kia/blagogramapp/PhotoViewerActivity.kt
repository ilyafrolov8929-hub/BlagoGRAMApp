package com.kia.blagogramapp

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.github.chrisbanes.photoview.PhotoView
import com.kia.blagogramapp.utils.TimeUtils

class PhotoViewerActivity : AppCompatActivity() {

    private var isUiVisible = true
    private lateinit var captionContainer: View
    private lateinit var toolbarContainer: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Используем новый метод для подготовки экрана
        val rootLayout = DesignEngine.initializeScreenWithFrameLayout(this)
        rootLayout.setBackgroundColor(Color.BLACK) // Для фото фон всегда черный
        setContentView(rootLayout)

        // 1. PhotoView (Full Screen)
        val photoView = PhotoView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setOnClickListener { toggleUiVisibility() }
        }
        rootLayout.addView(photoView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))

        // 2. Новый "таблеточный" тулбар
        val (toolbar, tvSenderName, tvSentTime) = DesignEngine.createPhotoViewerToolbar(this) {
            finish() // Закрываем активити по нажатию на "назад"
        }
        toolbarContainer = toolbar // Сохраняем ссылку для скрытия/показа
        rootLayout.addView(toolbarContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP))

        // 3. Новый контейнер для подписи
        val (captionView, tvCaption) = DesignEngine.createPhotoCaptionView(this)
        captionContainer = captionView // Сохраняем ссылку
        rootLayout.addView(captionContainer, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 16, 0, 16, 16))


        // --- Logic ---
        val url = intent.getStringExtra("image_url")
        val senderName = intent.getStringExtra("sender_name")
        val sentTime = intent.getStringExtra("sent_time")
        val caption = intent.getStringExtra("caption")

        tvSenderName.text = senderName ?: "Неизвестный"
        tvSentTime.text = TimeUtils.formatPhotoViewerDate(sentTime ?: "")

        if (!caption.isNullOrEmpty()) {
            tvCaption.text = caption
            captionContainer.visibility = View.VISIBLE
        } else {
            captionContainer.visibility = View.GONE
        }

        if (url != null) {
            photoView.load(url) { crossfade(true) }
        }
    }

    private fun toggleUiVisibility() {
        isUiVisible = !isUiVisible
        val newVisibility = if (isUiVisible) View.VISIBLE else View.GONE
        val caption = intent.getStringExtra("caption")

        // Скрываем/показываем только корневые контейнеры
        toolbarContainer.visibility = newVisibility
        if (!caption.isNullOrEmpty()) {
            captionContainer.visibility = newVisibility
        }
    }
}
