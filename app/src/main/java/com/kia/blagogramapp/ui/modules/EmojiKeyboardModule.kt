package com.kia.blagogramapp.ui.modules

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kia.blagogramapp.LayoutHelper
import com.kia.blagogramapp.R
import com.vanniktech.emoji.ios.IosEmojiProvider

class EmojiKeyboardModule(
    context: Context,
    private val onEmojiClick: (String) -> Unit,
    private val onBackspaceClick: () -> Unit,
    private val onKeyboardClick: () -> Unit
) : LinearLayout(context) {

    private val emojiRecycler: RecyclerView
    private val bottomTabs: LinearLayout
    private val categoryBar: LinearLayout
    private var currentHeight = 0
    private val provider = IosEmojiProvider()

    init {
        orientation = VERTICAL
        // Цвет как в темной теме Telegram
        setBackgroundColor(Color.parseColor("#1C1C1E"))
        visibility = GONE

        // 1. Верхняя панель категорий (Иконки)
        categoryBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(LayoutHelper.dp(8), 0, LayoutHelper.dp(8), 0)
            setBackgroundColor(Color.parseColor("#1C1C1E"))
        }
        setupCategoryBar()
        addView(categoryBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 40))

        // 2. Сетка эмодзи (7 колонок для крупного вида)
        emojiRecycler = RecyclerView(context).apply {
            layoutManager = GridLayoutManager(context, 7)
            clipToPadding = false
            setPadding(LayoutHelper.dp(4), LayoutHelper.dp(4), LayoutHelper.dp(4), LayoutHelper.dp(4))
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        addView(emojiRecycler, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1f))

        // 3. Нижняя навигация
        bottomTabs = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#1C1C1E"))
            setPadding(LayoutHelper.dp(12), 0, LayoutHelper.dp(12), 0)
        }

        // Кнопка клавиатуры
        val kbBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_keyboard)
            setColorFilter(Color.parseColor("#8E8E93"), PorterDuff.Mode.SRC_IN)
            setPadding(LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8))
            setOnClickListener { onKeyboardClick() }
        }
        bottomTabs.addView(kbBtn, LayoutHelper.createLinear(40, 40))

        // Переключатели режимов (таблетка)
        val tabsContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#2C2C2E"))
                cornerRadius = LayoutHelper.dp(20).toFloat()
            }
            setPadding(LayoutHelper.dp(2), LayoutHelper.dp(2), LayoutHelper.dp(2), LayoutHelper.dp(2))
        }

        tabsContainer.addView(createTabButton("Эмодзи", true))
        tabsContainer.addView(createTabButton("GIF", false))
        tabsContainer.addView(createTabButton("Стикеры", false))

        bottomTabs.addView(tabsContainer, LayoutHelper.createLinear(0, 36, 1f, Gravity.CENTER, 8, 0, 8, 0))

        // Backspace
        val backBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_backspace)
            setColorFilter(Color.parseColor("#8E8E93"), PorterDuff.Mode.SRC_IN)
            setPadding(LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8))
            setOnClickListener { onBackspaceClick() }
        }
        bottomTabs.addView(backBtn, LayoutHelper.createLinear(40, 40))

        addView(bottomTabs, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56))

        setupEmojiList()
    }

    private fun setupCategoryBar() {
        val icons = listOf(
            R.drawable.ic_emoji, R.drawable.ic_chat, R.drawable.ic_user,
            R.drawable.ic_music, R.drawable.ic_photo, R.drawable.ic_grid,
            R.drawable.ic_settings, R.drawable.ic_check
        )
        icons.forEach { resId ->
            val img = ImageView(context).apply {
                setImageResource(resId)
                setColorFilter(Color.parseColor("#8E8E93"), PorterDuff.Mode.SRC_IN)
                setPadding(LayoutHelper.dp(10), LayoutHelper.dp(10), LayoutHelper.dp(10), LayoutHelper.dp(10))
            }
            categoryBar.addView(img, LayoutHelper.createLinear(0, 40, 1f))
        }
    }

    private fun createTabButton(text: String, isSelected: Boolean): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#8E8E93"))
            gravity = Gravity.CENTER
            setPadding(LayoutHelper.dp(16), 0, LayoutHelper.dp(16), 0)
            if (isSelected) {
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#48484A"))
                    cornerRadius = LayoutHelper.dp(18).toFloat()
                }
            }
        }
    }

    private fun setupEmojiList() {
        val allEmojis = mutableListOf<com.vanniktech.emoji.Emoji>()
        provider.categories.forEach { category ->
            category.emojis.forEach { allEmojis.add(it) }
        }

        emojiRecycler.adapter = object : RecyclerView.Adapter<EmojiViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiViewHolder {
                val iv = ImageView(context).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    val p = LayoutHelper.dp(10)
                    setPadding(p, p, p, p)
                    layoutParams = RecyclerView.LayoutParams(
                        RecyclerView.LayoutParams.MATCH_PARENT,
                        LayoutHelper.dp(54)
                    )
                }
                return EmojiViewHolder(iv)
            }

            override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
                val emoji = allEmojis[position]
                val iv = holder.itemView as ImageView
                iv.setImageDrawable(provider.getDrawable(emoji, context))
                iv.setOnClickListener { onEmojiClick(emoji.unicode) }
            }

            override fun getItemCount() = allEmojis.size
        }
    }

    class EmojiViewHolder(view: View) : RecyclerView.ViewHolder(view)

    fun setHeight(height: Int) {
        val finalHeight = if (height <= 0) LayoutHelper.dp(300) else height
        currentHeight = finalHeight
        layoutParams?.let {
            if (it.height != finalHeight) {
                it.height = finalHeight
                requestLayout()
            }
        }
    }
}
