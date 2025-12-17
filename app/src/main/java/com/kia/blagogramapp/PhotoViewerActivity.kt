package com.kia.blagogramapp

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.github.chrisbanes.photoview.PhotoView

class PhotoViewerActivity : AppCompatActivity() {

    private var isUiVisible = true
    private lateinit var captionContainer: FrameLayout
    private lateinit var topOverlay: View
    private lateinit var toolbarLayout: LinearLayout
    private lateinit var closeButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DesignEngine.init(this)

        // 1. Root Layout (FrameLayout)
        val rootLayout = FrameLayout(this)
        rootLayout.setBackgroundColor(Color.BLACK)
        setContentView(rootLayout)

        // 2. PhotoView (Full Screen)
        val photoView = PhotoView(this)
        photoView.scaleType = ImageView.ScaleType.FIT_CENTER
        rootLayout.addView(photoView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))

        // 3. Top Overlay Background (Gradient)
        topOverlay = View(this)
        val topGradient = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0x99000000.toInt(), Color.TRANSPARENT))
        topOverlay.background = topGradient
        rootLayout.addView(topOverlay, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 80, Gravity.TOP))

        // 4. Toolbar Container (Close Button + Name/Time)
        toolbarLayout = LinearLayout(this)
        toolbarLayout.orientation = LinearLayout.HORIZONTAL
        toolbarLayout.gravity = Gravity.CENTER_VERTICAL
        rootLayout.addView(toolbarLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 0, 0, 0, 0))

        // Close Button
        closeButton = ImageView(this)
        closeButton.setImageResource(R.drawable.ic_back)
        closeButton.setColorFilter(Color.WHITE)
        closeButton.setPadding(LayoutHelper.dp(12), LayoutHelper.dp(12), LayoutHelper.dp(12), LayoutHelper.dp(12))
        val outValue = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
        closeButton.setBackgroundResource(outValue.resourceId)

        // Margin 8dp around button
        toolbarLayout.addView(closeButton, LayoutHelper.createLinear(48, 48, 0f, 0, 8, 8, 0, 8))

        // Text Container
        val textContainer = LinearLayout(this)
        textContainer.orientation = LinearLayout.VERTICAL

        val tvSenderName = TextView(this)
        tvSenderName.setTextColor(Color.WHITE)
        tvSenderName.textSize = 18f
        tvSenderName.typeface = Typeface.DEFAULT_BOLD
        textContainer.addView(tvSenderName, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))

        val tvSentTime = TextView(this)
        tvSentTime.setTextColor(0xFFD3D3D3.toInt()) // Light Gray
        tvSentTime.textSize = 12f
        textContainer.addView(tvSentTime, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))

        // Add text container to toolbar
        toolbarLayout.addView(textContainer, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL, 8, 0, 16, 0))

        // 5. Bottom Caption Container (Gradient + Text)
        captionContainer = FrameLayout(this)
        val bottomGradient = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Color.TRANSPARENT, 0x99000000.toInt()))
        captionContainer.background = bottomGradient
        rootLayout.addView(captionContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM))

        val tvCaption = TextView(this)
        tvCaption.setTextColor(Color.WHITE)
        tvCaption.textSize = 16f
        tvCaption.setPadding(LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(16))
        tvCaption.setShadowLayer(2f, 1f, 1f, 0x80000000.toInt())
        tvCaption.maxLines = 6
        tvCaption.ellipsize = TextUtils.TruncateAt.END
        captionContainer.addView(tvCaption, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM))

        // --- Logic ---

        // Get Intent Data
        val url = intent.getStringExtra("image_url")
        val senderName = intent.getStringExtra("sender_name")
        val sentTime = intent.getStringExtra("sent_time")
        val caption = intent.getStringExtra("caption")

        tvSenderName.text = senderName ?: "Неизвестный"
        tvSentTime.text = sentTime ?: ""

        if (!caption.isNullOrEmpty()) {
            tvCaption.text = caption
            captionContainer.visibility = View.VISIBLE
        } else {
            captionContainer.visibility = View.GONE
        }

        if (url != null) {
            photoView.load(url) {
                crossfade(true)
            }
        }

        closeButton.setOnClickListener {
            finish()
        }

        photoView.setOnClickListener {
            toggleUiVisibility()
        }
    }

    private fun toggleUiVisibility() {
        isUiVisible = !isUiVisible
        val visibility = if (isUiVisible) View.VISIBLE else View.GONE
        val caption = intent.getStringExtra("caption")

        topOverlay.visibility = visibility
        toolbarLayout.visibility = visibility
        closeButton.isClickable = isUiVisible

        if (!caption.isNullOrEmpty()) {
            captionContainer.visibility = visibility
        }
    }
}
