package com.kia.blagogramapp.ui.modules

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
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
    val postImage: ImageView
    val likesCount: TextView
    private val expandBtn: LinearLayout
    val moreBtn: ImageView
    
    val likeButton: View
    val sharePill: View
    private val likeIcon: ImageView

    var onDeleteClick: (() -> Unit)? = null

    init {
        radius = LayoutHelper.dp(32).toFloat()
        cardElevation = 0f
        setCardBackgroundColor(DesignEngine.Theme.colorDivider) 
        val params = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT)
        params.setMargins(0, LayoutHelper.dp(8), 0, LayoutHelper.dp(8))
        layoutParams = params

        val contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(16))
        }

        // Header Pill
        val headerPill = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(12), LayoutHelper.dp(8))
            background = GradientDrawable().apply {
                setColor(0x33000000) 
                cornerRadius = LayoutHelper.dp(20).toFloat()
            }
        }

        avatarView = ShapeableImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(LayoutHelper.dp(16).toFloat())
                .build()
        }
        headerPill.addView(avatarView, LayoutHelper.createLinear(32, 32))

        val nameContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        nameView = TextView(context).apply {
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(DesignEngine.Theme.colorTextPrimary)
        }
        timeView = TextView(context).apply {
            textSize = 10f
            setTextColor(DesignEngine.Theme.colorTextSecondary)
        }
        nameContainer.addView(nameView)
        nameContainer.addView(timeView)
        headerPill.addView(nameContainer, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 0, 8, 0, 0, 0))

        moreBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_more)
            colorFilter = PorterDuffColorFilter(DesignEngine.Theme.colorTextPrimary, PorterDuff.Mode.SRC_IN)
            setPadding(LayoutHelper.dp(4), LayoutHelper.dp(4), LayoutHelper.dp(4), LayoutHelper.dp(4))
            setOnClickListener { showPopupMenu(it) }
        }
        headerPill.addView(moreBtn, LayoutHelper.createLinear(24, 24))

        contentLayout.addView(headerPill, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))

        // Text
        postText = TextView(context).apply {
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            val p = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT)
            p.topMargin = LayoutHelper.dp(12)
            layoutParams = p
        }
        contentLayout.addView(postText)

        // Expand Button
        expandBtn = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            val p = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT)
            p.topMargin = LayoutHelper.dp(4)
            layoutParams = p
            
            val tv = TextView(context).apply {
                text = "Развернуть"
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(DesignEngine.Theme.colorTextPrimary)
            }
            val icon = ImageView(context).apply {
                setImageResource(R.drawable.ic_back) 
                rotation = -90f
                colorFilter = PorterDuffColorFilter(DesignEngine.Theme.colorTextPrimary, PorterDuff.Mode.SRC_IN)
            }
            addView(tv)
            addView(icon, LayoutHelper.createLinear(20, 20, 0f, 0, 4, 0, 0, 0))
        }
        contentLayout.addView(expandBtn)

        // Image
        postImage = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            val shapeDrawable = GradientDrawable().apply {
                cornerRadius = LayoutHelper.dp(16).toFloat()
            }
            clipToOutline = true
            background = shapeDrawable
        }
        val imgParams = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 200)
        imgParams.topMargin = LayoutHelper.dp(12)
        contentLayout.addView(postImage, imgParams)

        // Actions
        val actionsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val p = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT)
            p.topMargin = LayoutHelper.dp(12)
            layoutParams = p
        }

        likeButton = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(6), LayoutHelper.dp(12), LayoutHelper.dp(6))
            background = GradientDrawable().apply {
                setColor(0x33000000)
                cornerRadius = LayoutHelper.dp(20).toFloat()
            }
            
            likeIcon = ImageView(context).apply {
                setImageResource(R.drawable.ic_heart_filled)
                colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            }
            likesCount = TextView(context).apply {
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                setPadding(LayoutHelper.dp(6), 0, 0, 0)
            }
            addView(likeIcon, LayoutHelper.createLinear(20, 20))
            addView(likesCount)
        }
        actionsLayout.addView(likeButton)

        sharePill = ImageView(context).apply {
            setImageResource(R.drawable.ic_send) 
            setPadding(LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8))
            background = GradientDrawable().apply {
                setColor(0x33000000)
                cornerRadius = LayoutHelper.dp(20).toFloat()
            }
            colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        }
        val shareParams = LayoutHelper.createLinear(36, 36)
        shareParams.leftMargin = LayoutHelper.dp(8)
        actionsLayout.addView(sharePill, shareParams)

        contentLayout.addView(actionsLayout)
        addView(contentLayout)
    }

    private fun showPopupMenu(view: View) {
        val themeWrapper = ContextThemeWrapper(context, R.style.CustomPopupMenu)
        val popup = PopupMenu(themeWrapper, view)
        
        popup.menu.add("Удалить").setOnMenuItemClickListener {
            onDeleteClick?.invoke()
            true
        }
        
        popup.show()
    }

    fun setData(name: String, time: String, text: String, imageUrl: String?, likes: Int, comments: Int, views: String, avatarUrl: String?, isMyPost: Boolean = false) {
        nameView.text = name
        timeView.text = time
        postText.text = text
        likesCount.text = likes.toString()
        moreBtn.visibility = if (isMyPost) View.VISIBLE else View.GONE
        
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

    fun setLiked(liked: Boolean) {
        if (liked) {
            likeIcon.colorFilter = PorterDuffColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)
        } else {
            likeIcon.colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        }
    }
}
