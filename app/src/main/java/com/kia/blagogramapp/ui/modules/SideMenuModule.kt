package com.kia.blagogramapp.ui.modules

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import coil.load
import com.google.android.material.imageview.ShapeableImageView
import com.kia.blagogramapp.DesignEngine
import com.kia.blagogramapp.LayoutHelper
import com.kia.blagogramapp.R
import com.kia.blagogramapp.TextCell

class SideMenuModule(val context: Context) {

    lateinit var drawerOverlay: FrameLayout
    lateinit var drawerPanel: LinearLayout
    lateinit var avatarView: ShapeableImageView
    lateinit var nameTv: TextView
    lateinit var loginTv: TextView
    lateinit var statusTv: TextView

    fun create(root: FrameLayout, statusBarHeight: Int): SideMenuModule {
        // 1. Overlay
        drawerOverlay = FrameLayout(context).apply {
            setBackgroundColor(Color.parseColor("#99000000"))
            alpha = 0f
            visibility = View.GONE
            elevation = LayoutHelper.dp(10).toFloat()
        }
        root.addView(drawerOverlay, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))

        // 2. Panel
        drawerPanel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(DesignEngine.Theme.colorSheetBackground)
            isClickable = true
            elevation = LayoutHelper.dp(10).toFloat()

            // Header
            val headerLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(DesignEngine.Theme.colorCardBackground)
                setPadding(LayoutHelper.dp(20), statusBarHeight + LayoutHelper.dp(20), LayoutHelper.dp(20), LayoutHelper.dp(20))
            }

            avatarView = ShapeableImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(Color.LTGRAY)
                shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                    .setAllCornerSizes(LayoutHelper.dp(35).toFloat())
                    .build()
            }
            headerLayout.addView(avatarView, LayoutHelper.createLinear(70, 70, 0f, 0, 0, 0, 0, 16))

            nameTv = TextView(context).apply {
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(DesignEngine.Theme.colorTextPrimary)
            }
            headerLayout.addView(nameTv)

            loginTv = TextView(context).apply {
                textSize = 14f
                setTextColor(DesignEngine.Theme.colorBlue)
            }
            headerLayout.addView(loginTv, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 2, 0, 8))

            statusTv = TextView(context).apply {
                textSize = 14f
                maxLines = 1
                setTextColor(DesignEngine.Theme.colorTextSecondary)
            }
            headerLayout.addView(statusTv)

            addView(headerLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        }

        val width = (context.resources.displayMetrics.widthPixels * 0.8).toInt()
        root.addView(drawerPanel, FrameLayout.LayoutParams(width, FrameLayout.LayoutParams.MATCH_PARENT).apply {
            gravity = Gravity.START
        })

        drawerPanel.translationX = (-width).toFloat()
        return this
    }

    fun addItem(text: String, icon: Int, onClick: () -> Unit) {
        val item = TextCell(context).apply {
            setData(text, icon)
            setOnClickListener { onClick() }
        }
        drawerPanel.addView(item)
    }

    fun addSpacer(weight: Float = 1f) {
        drawerPanel.addView(View(context), LinearLayout.LayoutParams(LayoutHelper.MATCH_PARENT, 0, weight))
    }

    fun addDivider() {
        val divider = View(context)
        divider.setBackgroundColor(DesignEngine.Theme.colorDivider)
        drawerPanel.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1))
    }
    
    fun setPaddingBottom(dp: Int) {
        drawerPanel.addView(View(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, dp))
    }

    fun updateHeader(name: String, login: String, status: String, avatar: String) {
        nameTv.text = name
        loginTv.text = "@$login"
        statusTv.text = status
        avatarView.load("https://blagogram.ru/avatars/$avatar") {
            placeholder(R.drawable.ic_user_placeholder)
            error(R.drawable.ic_user_placeholder)
        }
    }
}
