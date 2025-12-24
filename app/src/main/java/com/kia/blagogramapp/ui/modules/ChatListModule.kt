package com.kia.blagogramapp.ui.modules

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.kia.blagogramapp.DesignEngine
import com.kia.blagogramapp.LayoutHelper
import com.kia.blagogramapp.R

/**
 * Заголовок секции чатов (как на скриншоте: "пилюля" по центру)
 */
class ChatSectionHeaderCell(context: Context) : FrameLayout(context) {
    private val textView: TextView
    init {
        setPadding(0, LayoutHelper.dp(20), 0, LayoutHelper.dp(12))
        textView = TextView(context).apply {
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(LayoutHelper.dp(24), LayoutHelper.dp(6), LayoutHelper.dp(24), LayoutHelper.dp(6))
            background = GradientDrawable().apply {
                setColor(0xFFBCBCBC.toInt()) // Солидный серый как на скриншоте
                cornerRadius = LayoutHelper.dp(20).toFloat()
            }
        }
        addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER))
    }
    fun setTitle(title: String) {
        textView.text = title
    }
}

class ChatListLayout(context: Context) : FrameLayout(context) {

    val swipeRefresh: SwipeRefreshLayout
    val recycler: RecyclerView
    val menuBtn: ImageView
    val editBtn: ImageView
    val titleView: TextView

    init {
        setBackgroundColor(DesignEngine.Theme.colorBackground)

        val statusBarHeight = getStatusBarHeight()

        // List
        swipeRefresh = SwipeRefreshLayout(context).apply {
            setProgressBackgroundColorSchemeColor(DesignEngine.Theme.colorCardBackground)
            setColorSchemeColors(DesignEngine.Theme.colorBlue)
            setProgressViewOffset(false, statusBarHeight + LayoutHelper.dp(60), statusBarHeight + LayoutHelper.dp(120))
        }

        recycler = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            clipToPadding = false
            setPadding(0, statusBarHeight + LayoutHelper.dp(80), 0, LayoutHelper.dp(20))
        }

        swipeRefresh.addView(recycler)
        addView(swipeRefresh)

        // Header
        val headerContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(LayoutHelper.dp(16), statusBarHeight + LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(16))
            setBackgroundColor(DesignEngine.Theme.colorBackground)
            elevation = LayoutHelper.dp(4).toFloat()
        }

        menuBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_menu)
            colorFilter = android.graphics.PorterDuffColorFilter(DesignEngine.Theme.colorIconPrimary, PorterDuff.Mode.SRC_IN)
            setPadding(LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8))
        }

        titleView = TextView(context).apply {
            text = "Чаты"
            textSize = 28f
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        editBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_edit)
            colorFilter = android.graphics.PorterDuffColorFilter(DesignEngine.Theme.colorBlue, PorterDuff.Mode.SRC_IN)
            setPadding(LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8))
        }

        headerContainer.addView(menuBtn, LinearLayout.LayoutParams(LayoutHelper.dp(40), LayoutHelper.dp(40)))
        headerContainer.addView(titleView, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        headerContainer.addView(editBtn, LinearLayout.LayoutParams(LayoutHelper.dp(40), LayoutHelper.dp(40)))

        addView(headerContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP))
    }

    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }
}
