package com.kia.blagogramapp.ui.modules

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.kia.blagogramapp.DesignEngine
import com.kia.blagogramapp.LayoutHelper

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
