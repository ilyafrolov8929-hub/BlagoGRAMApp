package com.kia.blagogramapp.ui.modules

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import coil.load
import com.google.android.material.imageview.ShapeableImageView
import com.kia.blagogramapp.DesignEngine
import com.kia.blagogramapp.LayoutHelper
import com.kia.blagogramapp.R

class PostCell(context: Context) : CardView(context) {

    private val avatarView: ShapeableImageView
    private val nameView: TextView
    private val timeView: TextView
    private val postText: TextView
    private val postImage: ImageView
    private val likesCount: TextView
    private val commentsCount: TextView
    private val viewsCount: TextView

    init {
        radius = LayoutHelper.dp(16).toFloat()
        cardElevation = 0f
        setCardBackgroundColor(DesignEngine.Theme.colorCardBackground)
        val params = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT)
        params.setMargins(0, LayoutHelper.dp(8), 0, LayoutHelper.dp(8))
        layoutParams = params

        val contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, LayoutHelper.dp(12))
        }

        // Header
        val headerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(12), LayoutHelper.dp(12), LayoutHelper.dp(12))
        }

        avatarView = ShapeableImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(LayoutHelper.dp(20).toFloat())
                .build()
        }
        headerLayout.addView(avatarView, LayoutHelper.createLinear(40, 40))

        val nameContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        nameView = TextView(context).apply {
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(DesignEngine.Theme.colorTextPrimary)
        }
        timeView = TextView(context).apply {
            textSize = 12f
            setTextColor(DesignEngine.Theme.colorTextSecondary)
        }
        nameContainer.addView(nameView)
        nameContainer.addView(timeView)
        headerLayout.addView(nameContainer, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 0, 12, 0, 0, 0))

        val moreBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_more)
            colorFilter = PorterDuffColorFilter(DesignEngine.Theme.colorChevron, PorterDuff.Mode.SRC_IN)
        }
        headerLayout.addView(moreBtn, LayoutHelper.createLinear(24, 24))

        contentLayout.addView(headerLayout)

        // Text
        postText = TextView(context).apply {
            textSize = 15f
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            setPadding(LayoutHelper.dp(12), 0, LayoutHelper.dp(12), LayoutHelper.dp(8))
        }
        contentLayout.addView(postText)

        // Image
        postImage = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        contentLayout.addView(postImage, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 250))

        // Actions
        val actionsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(12), LayoutHelper.dp(12), 0)
        }

        fun createStatPill(iconRes: Int): Pair<LinearLayout, TextView> {
            val pill = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(LayoutHelper.dp(12), LayoutHelper.dp(6), LayoutHelper.dp(12), LayoutHelper.dp(6))
                background = GradientDrawable().apply {
                    setColor(DesignEngine.Theme.colorDivider) // Using divider color for pill bg
                    cornerRadius = LayoutHelper.dp(20).toFloat()
                }
            }
            val icon = ImageView(context).apply {
                setImageResource(iconRes)
                colorFilter = PorterDuffColorFilter(DesignEngine.Theme.colorTextSecondary, PorterDuff.Mode.SRC_IN)
            }
            val text = TextView(context).apply {
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(DesignEngine.Theme.colorTextSecondary)
                setPadding(LayoutHelper.dp(6), 0, 0, 0)
            }
            pill.addView(icon, LayoutHelper.createLinear(20, 20))
            pill.addView(text)
            return pill to text
        }

        val (likePill, likesTv) = createStatPill(R.drawable.ic_like_outline)
        likesCount = likesTv
        actionsLayout.addView(likePill)

        val (commentPill, commentsTv) = createStatPill(R.drawable.ic_chat)
        commentsCount = commentsTv
        actionsLayout.addView(commentPill, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 12, 0, 0, 0))

        val (sharePill, _) = createStatPill(R.drawable.ic_send)
        actionsLayout.addView(sharePill, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 12, 0, 0, 0))

        actionsLayout.addView(View(context), LayoutHelper.createLinear(0, 0, 1f))

        val viewsLayout = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
        }
        val viewsIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_user)
            colorFilter = PorterDuffColorFilter(DesignEngine.Theme.colorTextSecondary, PorterDuff.Mode.SRC_IN)
        }
        viewsCount = TextView(context).apply {
            textSize = 12f
            setTextColor(DesignEngine.Theme.colorTextSecondary)
            setPadding(LayoutHelper.dp(4), 0, 0, 0)
        }
        viewsLayout.addView(viewsIcon, LayoutHelper.createLinear(18, 18))
        viewsLayout.addView(viewsCount)
        actionsLayout.addView(viewsLayout)

        contentLayout.addView(actionsLayout)
        addView(contentLayout)
    }

    fun setData(name: String, time: String, text: String, imageUrl: String?, likes: Int, comments: Int, views: String, avatarUrl: String?) {
        nameView.text = name
        timeView.text = time
        postText.text = text
        likesCount.text = likes.toString()
        commentsCount.text = comments.toString()
        viewsCount.text = views
        
        avatarView.load(avatarUrl) {
            placeholder(R.drawable.ic_user_placeholder)
            error(R.drawable.ic_user_placeholder)
        }

        if (imageUrl != null) {
            postImage.visibility = View.VISIBLE
            postImage.load(imageUrl)
        } else {
            postImage.visibility = View.GONE
        }
    }
}
