package com.kia.blagogramapp.ui.modules

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import coil.load
import com.google.android.material.imageview.ShapeableImageView
import com.kia.blagogramapp.DesignEngine
import com.kia.blagogramapp.LayoutHelper
import com.kia.blagogramapp.R

class SideMenuModule(val context: Context) {

    lateinit var drawerOverlay: FrameLayout
    lateinit var drawerPanel: LinearLayout
    lateinit var itemsContainer: LinearLayout
    lateinit var avatarView: ShapeableImageView
    lateinit var nameTv: TextView
    lateinit var loginTv: TextView
    lateinit var statusTv: TextView

    fun create(root: FrameLayout, statusBarHeight: Int): SideMenuModule {
        // 1. Overlay - более мягкое затемнение фона
        drawerOverlay = FrameLayout(context).apply {
            setBackgroundColor(Color.parseColor("#80000000"))
            alpha = 0f
            visibility = View.GONE
            elevation = LayoutHelper.dp(10).toFloat()
        }
        root.addView(drawerOverlay, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))

        // 2. Panel - плавающая панель с закруглением справа
        drawerPanel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            isClickable = true
            elevation = LayoutHelper.dp(20).toFloat()

            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorSheetBackground)
                val r = LayoutHelper.dp(32).toFloat()
                cornerRadii = floatArrayOf(0f, 0f, r, r, r, r, 0f, 0f)
            }

            // Header - Оформлен как стильная карточка внутри меню
            val headerWrapper = FrameLayout(context).apply {
                setPadding(LayoutHelper.dp(12), statusBarHeight + LayoutHelper.dp(16), LayoutHelper.dp(12), LayoutHelper.dp(12))
            }

            val headerLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(LayoutHelper.dp(20), LayoutHelper.dp(20), LayoutHelper.dp(20), LayoutHelper.dp(20))

                background = GradientDrawable().apply {
                    cornerRadius = LayoutHelper.dp(24).toFloat()
                    orientation = GradientDrawable.Orientation.TL_BR
                    setColors(intArrayOf(
                        DesignEngine.Theme.colorCardBackground,
                        DesignEngine.Theme.colorSheetBackground
                    ))
                }
            }

            avatarView = ShapeableImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(DesignEngine.Theme.colorDivider)
                shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                    .setAllCornerSizes(LayoutHelper.dp(28).toFloat())
                    .build()

                // 1. Устанавливаем strokeWidth
                val sw = LayoutHelper.dp(3)
                strokeWidth = sw.toFloat()
                strokeColor = android.content.res.ColorStateList.valueOf(DesignEngine.Theme.colorBlue)

                // 2. Добавляем Padding, чтобы обводка не обрезалась краями вью
                // Попробуйте значение равное толщине обводки
                val p = sw
                setPadding(p, p, p, p)
            }

            headerLayout.addView(avatarView, LayoutHelper.createLinear(80, 80, 0f, 0, 0, 0, 0, 16))

            nameTv = TextView(context).apply {
                textSize = 22f
                typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
                setTextColor(DesignEngine.Theme.colorTextPrimary)
                letterSpacing = -0.01f
            }
            headerLayout.addView(nameTv)

            loginTv = TextView(context).apply {
                textSize = 15f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setTextColor(DesignEngine.Theme.colorBlue)
            }
            headerLayout.addView(loginTv, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 2, 0, 8))

            statusTv = TextView(context).apply {
                textSize = 14f
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                setTextColor(DesignEngine.Theme.colorTextSecondary)
                setLineSpacing(LayoutHelper.dp(2).toFloat(), 1f)
            }
            headerLayout.addView(statusTv)

            headerWrapper.addView(headerLayout)
            addView(headerWrapper, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

            // Скроллируемая область для пунктов меню
            val scrollView = ScrollView(context).apply {
                isVerticalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
                isFillViewport = true
            }
            itemsContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, LayoutHelper.dp(8), 0, LayoutHelper.dp(16))
            }
            scrollView.addView(itemsContainer)
            addView(scrollView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1f))
        }

        val width = (context.resources.displayMetrics.widthPixels * 0.82).toInt()
        root.addView(drawerPanel, FrameLayout.LayoutParams(width, FrameLayout.LayoutParams.MATCH_PARENT).apply {
            gravity = Gravity.START
        })

        drawerPanel.translationX = (-width).toFloat()
        return this
    }

    fun addItem(text: String, icon: Int, onClick: () -> Unit) {
        val container = FrameLayout(context).apply {
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(2), LayoutHelper.dp(12), LayoutHelper.dp(2))
        }

        val item = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(14), LayoutHelper.dp(16), LayoutHelper.dp(14))

            // Форма пилюли и эффект нажатия
            background = GradientDrawable().apply {
                cornerRadius = LayoutHelper.dp(24).toFloat()
            }

            val outValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            foreground = context.getDrawable(outValue.resourceId)

            setOnClickListener { onClick() }
        }

        val iconView = ImageView(context).apply {
            setImageResource(icon)
            setColorFilter(DesignEngine.Theme.colorIconPrimary)
        }
        item.addView(iconView, LayoutHelper.createLinear(24, 24))

        val tv = TextView(context).apply {
            this.text = text
            textSize = 16f
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(LayoutHelper.dp(16), 0, 0, 0)
        }
        item.addView(tv, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f))

        val chevron = ImageView(context).apply {
            setImageResource(R.drawable.ic_chevron_right)
            setColorFilter(DesignEngine.Theme.colorChevron)
            alpha = 0.5f
        }
        item.addView(chevron, LayoutHelper.createLinear(20, 20))

        container.addView(item)
        itemsContainer.addView(container)
    }

    fun addSpacer(weight: Float = 1f) {
        itemsContainer.addView(View(context), LinearLayout.LayoutParams(LayoutHelper.MATCH_PARENT, 0, weight))
    }

    fun addDivider() {
        val dividerWrapper = FrameLayout(context).apply {
            setPadding(LayoutHelper.dp(28), LayoutHelper.dp(12), LayoutHelper.dp(28), LayoutHelper.dp(12))
        }
        val divider = View(context)
        divider.setBackgroundColor(DesignEngine.Theme.colorDivider)
        divider.alpha = 0.6f
        dividerWrapper.addView(divider, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 1))
        itemsContainer.addView(dividerWrapper)
    }

    fun setPaddingBottom(dp: Int) {
        itemsContainer.addView(View(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, dp))
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