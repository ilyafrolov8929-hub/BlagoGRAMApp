package com.kia.blagogramapp.ui.screens

import android.content.Context
import android.graphics.Color
import android.hardware.SensorEventListener
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import coil.load
import com.github.chrisbanes.photoview.PhotoView
import com.kia.blagogramapp.BaseScreen
import com.kia.blagogramapp.DesignEngine
import com.kia.blagogramapp.LayoutHelper
import com.kia.blagogramapp.utils.TimeUtils

class PhotoViewerScreen(context: Context, private val url: String = "", private var senderName: String = "", private var sentTime: String = "", private var caption: String? = "") : BaseScreen(context) {
    private var isUiVisible = true
    private lateinit var captionContainer: View
    private lateinit var toolbarContainer: View

    override fun createView(): View {
        val rootLayout = FrameLayout(context)
        rootLayout.setBackgroundColor(Color.BLACK) // Для фото фон всегда черный

        // 1. PhotoView (Full Screen)
        val photoView = PhotoView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setOnClickListener { toggleUiVisibility() }
        }
        rootLayout.addView(photoView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))

        // 2. Новый "таблеточный" тулбар
        val (toolbar, tvSenderName, tvSentTime) = DesignEngine.createPhotoViewerToolbar(context) {
            finishFragment() // Закрываем активити по нажатию на "назад"
        }
        toolbarContainer = toolbar // Сохраняем ссылку для скрытия/показа
        rootLayout.addView(toolbarContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP))

        // 3. Новый контейнер для подписи
        val (captionView, tvCaption) = DesignEngine.createPhotoCaptionView(context)
        captionContainer = captionView // Сохраняем ссылку
        rootLayout.addView(captionContainer, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 16, 0, 16, 16))


        // --- Logic ---

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

        return  rootLayout
    }

    private fun toggleUiVisibility() {
        isUiVisible = !isUiVisible
        val newVisibility = if (isUiVisible) View.VISIBLE else View.GONE

        // Скрываем/показываем только корневые контейнеры
        toolbarContainer.visibility = newVisibility
        if (!caption.isNullOrEmpty()) {
            captionContainer.visibility = newVisibility
        }
    }
}