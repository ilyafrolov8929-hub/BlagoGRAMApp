package com.kia.blagogramapp.ui.modules

import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.kia.blagogramapp.*
import com.kia.blagogramapp.R
import com.kia.blagogramapp.models.Track
import com.kia.blagogramapp.utils.MusicManager

class ProfileLayout(context: Context) : FrameLayout(context) {

    val profileCover: ImageView
    val backButton: View
    val userID: TextView
    val btnSettings: View
    val profileAvatar: ShapeableImageView
    val profileUsername: TextView
    val status: TextView
    val subsCount: TextView
    val friendsCount: TextView
    val photoCount: TextView
    val btnCreatePost: Button
    val postsContainer: LinearLayout
    val topBar: RelativeLayout

    private val tabViews = mutableListOf<TextView>()
    private val contentContainer: FrameLayout
    private lateinit var tabSelector: View
    private lateinit var tabsLayout: LinearLayout
    
    var onTrackClicked: ((Track) -> Unit)? = null
    var onAddMusicClicked: (() -> Unit)? = null

    init {
        setBackgroundColor(DesignEngine.Theme.colorBackground)

        profileCover = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            alpha = 0.9f
        }
        addView(profileCover, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 300))

        topBar = RelativeLayout(context).apply {
            // Уменьшаем падинг, будем задавать отступ элементам вручную для точности
            setPadding(LayoutHelper.dp(16), 0, LayoutHelper.dp(16), LayoutHelper.dp(10))
            elevation = LayoutHelper.dp(10).toFloat()
        }

        val backContainer = LinearLayout(context).apply {
            id = View.generateViewId()
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(8), LayoutHelper.dp(12), LayoutHelper.dp(8))
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorDivider)
                cornerRadius = LayoutHelper.dp(20).toFloat()
            }
            minimumWidth = LayoutHelper.dp(46)
        }
        backButton = backContainer
        val backIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_back)
            setColorFilter(DesignEngine.Theme.colorTextPrimary, PorterDuff.Mode.SRC_IN)
        }
        backContainer.addView(backIcon, LayoutHelper.createLinear(20, 20))
        
        val backParams = RelativeLayout.LayoutParams(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT)
        backParams.topMargin = LayoutHelper.dp(45) // Фиксированный отступ от верха (зона статус-бара)
        topBar.addView(backContainer, backParams)

        userID = TextView(context).apply {
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setShadowLayer(2f, 1f, 1f, Color.BLACK)
            gravity = Gravity.CENTER
        }
        val userParams = RelativeLayout.LayoutParams(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT)
        userParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
        // Центрируем текст по вертикали относительно кнопки "Назад"
        userParams.addRule(RelativeLayout.ALIGN_TOP, backContainer.id)
        userParams.addRule(RelativeLayout.ALIGN_BOTTOM, backContainer.id)
        topBar.addView(userID, userParams)

        val settingsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(8), LayoutHelper.dp(12), LayoutHelper.dp(8))
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorDivider)
                cornerRadius = LayoutHelper.dp(20).toFloat()
            }
            minimumWidth = LayoutHelper.dp(46)
        }
        btnSettings = settingsContainer
        val settingsIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_settings)
            setColorFilter(DesignEngine.Theme.colorTextPrimary, PorterDuff.Mode.SRC_IN)
        }
        settingsContainer.addView(settingsIcon, LayoutHelper.createLinear(20, 20))
        
        val settingsParams = RelativeLayout.LayoutParams(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT)
        settingsParams.addRule(RelativeLayout.ALIGN_PARENT_END)
        settingsParams.addRule(RelativeLayout.ALIGN_TOP, backContainer.id)
        topBar.addView(settingsContainer, settingsParams)

        addView(topBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        val scrollView = NestedScrollView(context).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(0, LayoutHelper.dp(80), 0, 0)
        }

        val scrollContent = RelativeLayout(context)

        val mainContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        val mainContainerParams = RelativeLayout.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT)
        mainContainerParams.topMargin = LayoutHelper.dp(60)
        scrollContent.addView(mainContainer, mainContainerParams)

        val profileCard = CardView(context).apply {
            setCardBackgroundColor(DesignEngine.Theme.colorSheetBackground)
            radius = LayoutHelper.dp(32).toFloat()
            cardElevation = 0f
        }
        val profileCardContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(70), LayoutHelper.dp(16), LayoutHelper.dp(40))
        }

        val nameContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        profileUsername = TextView(context).apply {
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            gravity = Gravity.CENTER
        }
        val emojiTv = TextView(context).apply {
            text = " 😎"
            textSize = 24f
            setTextColor(DesignEngine.Theme.colorTextPrimary)
        }
        nameContainer.addView(profileUsername)
        nameContainer.addView(emojiTv)
        profileCardContent.addView(nameContainer)

        status = TextView(context).apply {
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(DesignEngine.Theme.colorTextSecondary)
            gravity = Gravity.CENTER
            val p = LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, gravity = Gravity.CENTER_HORIZONTAL)
            p.topMargin = LayoutHelper.dp(4)
            layoutParams = p
        }
        profileCardContent.addView(status)

        val statsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val p = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT)
            p.topMargin = LayoutHelper.dp(24)
            layoutParams = p
        }

        fun addStatPill(parent: LinearLayout, title: String, isLast: Boolean = false): TextView {
            val pill = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8))
                background = context.getDrawable(R.drawable.bg_stat_pill)
                backgroundTintList = android.content.res.ColorStateList.valueOf(DesignEngine.Theme.colorCardBackground)
            }
            val countTv = TextView(context).apply {
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(DesignEngine.Theme.colorBlue)
                gravity = Gravity.CENTER
            }
            val titleTv = TextView(context).apply {
                text = title
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(DesignEngine.Theme.colorTextPrimary)
                gravity = Gravity.CENTER
            }
            pill.addView(countTv)
            pill.addView(titleTv)

            val lp = LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f)
            if (!isLast) lp.rightMargin = LayoutHelper.dp(8)
            parent.addView(pill, lp)
            return countTv
        }

        subsCount = addStatPill(statsRow, "Подписчики")
        friendsCount = addStatPill(statsRow, "Друзья")
        photoCount = addStatPill(statsRow, "Подписки")

        profileCardContent.addView(statsRow)

        val bottomSection = CardView(context).apply {
            setCardBackgroundColor(DesignEngine.Theme.colorDivider)
            radius = LayoutHelper.dp(32).toFloat()
            cardElevation = 0f
            val p = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT)
            p.topMargin = LayoutHelper.dp(24)
            layoutParams = p
        }
        val bottomContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(12), LayoutHelper.dp(12), LayoutHelper.dp(12))
        }

        val tabsWrapper = FrameLayout(context).apply {
            setPadding(LayoutHelper.dp(4), LayoutHelper.dp(4), LayoutHelper.dp(4), LayoutHelper.dp(4))
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorCardBackground)
                cornerRadius = LayoutHelper.dp(20).toFloat()
            }
        }
        
        tabSelector = View(context).apply {
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorDivider)
                cornerRadius = LayoutHelper.dp(16).toFloat()
            }
        }
        tabsWrapper.addView(tabSelector, LayoutHelper.createFrame(0, 32, Gravity.CENTER_VERTICAL))

        tabsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val tabNames = listOf("Фото", "Музыка", "Видео", "Альбомы", "Статьи")
        tabNames.forEachIndexed { index, name ->
            val tv = TextView(context).apply {
                text = name
                gravity = Gravity.CENTER
                textSize = if (index == 3) 10f else 12f
                setPadding(0, LayoutHelper.dp(6), 0, LayoutHelper.dp(6))
                setOnClickListener { switchTab(index) }
            }
            tabViews.add(tv)
            tabsLayout.addView(tv, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f))
        }
        tabsWrapper.addView(tabsLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        bottomContent.addView(tabsWrapper)

        contentContainer = FrameLayout(context).apply {
            val p = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT)
            p.topMargin = LayoutHelper.dp(12)
            layoutParams = p
        }
        bottomContent.addView(contentContainer)
        
        post { switchTab(0, animate = false) }

        bottomSection.addView(bottomContent)
        profileCardContent.addView(bottomSection)

        btnCreatePost = Button(context).apply {
            text = "Создать пост"
            typeface = Typeface.DEFAULT_BOLD
            textSize = 14f
            setTextColor(Color.WHITE)
            background = context.getDrawable(R.drawable.bg_stat_pill)
            backgroundTintList = android.content.res.ColorStateList.valueOf(DesignEngine.Theme.colorBlue)
            val p = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.dp(20))
            p.topMargin = LayoutHelper.dp(32)
            layoutParams = p
        }
        profileCardContent.addView(btnCreatePost)

        postsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val p = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT)
            p.topMargin = LayoutHelper.dp(20)
            layoutParams = p
        }
        profileCardContent.addView(postsContainer)

        profileCard.addView(profileCardContent)
        mainContainer.addView(profileCard)

        profileAvatar = ShapeableImageView(context).apply {
            id = View.generateViewId()
            scaleType = ImageView.ScaleType.CENTER_CROP
            setPadding(LayoutHelper.dp(5), LayoutHelper.dp(5), LayoutHelper.dp(5), LayoutHelper.dp(5))
            background = context.getDrawable(R.drawable.circle_white_bg)
            backgroundTintList = android.content.res.ColorStateList.valueOf(DesignEngine.Theme.colorSheetBackground)
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(LayoutHelper.dp(65).toFloat())
                .build()
            elevation = LayoutHelper.dp(10).toFloat()
        }
        val avatarParams = RelativeLayout.LayoutParams(LayoutHelper.dp(130), LayoutHelper.dp(130))
        avatarParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
        avatarParams.topMargin = 0
        scrollContent.addView(profileAvatar, avatarParams)

        scrollView.addView(scrollContent)
        addView(scrollView)
    }

    private fun switchTab(index: Int, animate: Boolean = true) {
        tabViews.forEachIndexed { i, tv ->
            val isSelected = i == index
            tv.setTextColor(if (isSelected) DesignEngine.Theme.colorBlue else DesignEngine.Theme.colorTextSecondary)
            tv.typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

        // Animation for selector
        val currentTab = tabViews[index]
        if (currentTab.width > 0) {
            val lp = tabSelector.layoutParams
            lp.width = currentTab.width
            tabSelector.layoutParams = lp

            val targetX = currentTab.left.toFloat()
            if (animate) {
                ObjectAnimator.ofFloat(tabSelector, "translationX", targetX).apply {
                    duration = 250
                    interpolator = DecelerateInterpolator()
                    start()
                }
            } else {
                tabSelector.translationX = targetX
            }
        }

        contentContainer.removeAllViews()
        val content = when (index) {
            1 -> createMusicContent()
            4 -> createDocsContent()
            else -> createGridContent()
        }
        contentContainer.addView(content)
    }

    private fun createGridContent(): View {
        val gridCard = CardView(context).apply {
            setCardBackgroundColor(DesignEngine.Theme.colorCardBackground)
            radius = LayoutHelper.dp(24).toFloat()
        }
        val grid = GridLayout(context).apply {
            columnCount = 4
            rowCount = 2
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(12), LayoutHelper.dp(12), LayoutHelper.dp(12))
        }
        for (i in 0 until 8) {
            val cell = CardView(context).apply {
                setCardBackgroundColor(DesignEngine.Theme.colorDivider)
                radius = LayoutHelper.dp(12).toFloat()
            }
            val lp = GridLayout.LayoutParams()
            lp.width = 0
            lp.height = LayoutHelper.dp(60)
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            lp.setMargins(LayoutHelper.dp(6), LayoutHelper.dp(6), LayoutHelper.dp(6), LayoutHelper.dp(6))
            grid.addView(cell, lp)
        }
        gridCard.addView(grid)
        return gridCard
    }

    fun updateMusicList() {
        if (tabViews[1].currentTextColor == DesignEngine.Theme.colorBlue) {
            switchTab(1, animate = false)
        }
    }

    private fun createMusicContent(): View {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        
        val btnAdd = Button(context).apply {
            text = "Добавить музыку"
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = context.getDrawable(R.drawable.bg_stat_pill)
            backgroundTintList = android.content.res.ColorStateList.valueOf(DesignEngine.Theme.colorBlue)
            val p = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.dp(15))
            p.bottomMargin = LayoutHelper.dp(12)
            layoutParams = p
            setOnClickListener { onAddMusicClicked?.invoke() }
        }
        root.addView(btnAdd)

        val tracks = MusicManager.playlist.value
        
        if (tracks.isEmpty()) {
            val emptyTv = TextView(context).apply {
                text = "Музыка не найдена"
                setTextColor(DesignEngine.Theme.colorTextSecondary)
                gravity = Gravity.CENTER
                setPadding(0, LayoutHelper.dp(20), 0, LayoutHelper.dp(20))
            }
            root.addView(emptyTv)
            return root
        }

        val recycler = object : RecyclerView(context) {
            override fun onMeasure(widthSpec: Int, heightSpec: Int) {
                val hSpec = MeasureSpec.makeMeasureSpec(LayoutHelper.dp(250), MeasureSpec.AT_MOST)
                super.onMeasure(widthSpec, hSpec)
            }
        }.apply {
            layoutManager = LinearLayoutManager(context)
            layoutParams = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT)
            isNestedScrollingEnabled = true
            
            adapter = object : RecyclerView.Adapter<TrackViewHolder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
                    val cell = MusicCell(context)
                    cell.layoutParams = RecyclerView.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.dp(60))
                    return TrackViewHolder(cell)
                }

                override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
                    val track = tracks[position]
                    holder.cell.setData(track.title, track.artist, track.albumArtUri)
                    holder.cell.setOnClickListener { onTrackClicked?.invoke(track) }
                    
                    holder.cell.moreBtn.setImageResource(R.drawable.ic_close_small)
                    holder.cell.moreBtn.setColorFilter(DesignEngine.Theme.colorLogout, PorterDuff.Mode.SRC_IN)
                    holder.cell.moreBtn.visibility = View.VISIBLE
                    holder.cell.moreBtn.setOnClickListener {
                        showCustomConfirmDialog(context, "Удаление трека", "Вы уверены, что хотите удалить трек \"${track.title}\"?") {
                            MusicManager.deleteTrack(context, track)
                            updateMusicList()
                        }
                    }
                }

                override fun getItemCount() = tracks.size
            }
        }
        
        val card = CardView(context).apply {
            setCardBackgroundColor(DesignEngine.Theme.colorCardBackground)
            radius = LayoutHelper.dp(24).toFloat()
            val p = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT)
            layoutParams = p
            addView(recycler)
        }
        root.addView(card)
        
        return root
    }

    private fun showCustomConfirmDialog(context: Context, title: String, message: String, onConfirm: () -> Unit) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(LayoutHelper.dp(24), LayoutHelper.dp(24), LayoutHelper.dp(24), LayoutHelper.dp(24))
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorCardBackground)
                cornerRadius = LayoutHelper.dp(28).toFloat()
            }
            layoutParams = FrameLayout.LayoutParams(LayoutHelper.dp(300), LayoutHelper.WRAP_CONTENT)
        }
        
        val titleTv = TextView(context).apply {
            text = title
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(DesignEngine.Theme.colorTextPrimary)
        }
        root.addView(titleTv)
        
        val msgTv = TextView(context).apply {
            text = message
            textSize = 16f
            setTextColor(DesignEngine.Theme.colorTextSecondary)
            val p = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT)
            p.topMargin = LayoutHelper.dp(12)
            layoutParams = p
        }
        root.addView(msgTv)
        
        val buttons = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            val p = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT)
            p.topMargin = LayoutHelper.dp(24)
            layoutParams = p
        }
        
        val btnCancel = Button(context).apply {
            text = "ОТМЕНА"
            background = null
            setTextColor(DesignEngine.Theme.colorBlue)
            setOnClickListener { dialog.dismiss() }
        }
        buttons.addView(btnCancel)
        
        val btnOk = Button(context).apply {
            text = "УДАЛИТЬ"
            background = null
            setTextColor(DesignEngine.Theme.colorLogout)
            setOnClickListener {
                onConfirm()
                dialog.dismiss()
            }
        }
        buttons.addView(btnOk)
        
        root.addView(buttons)
        dialog.setContentView(root)
        dialog.show()
    }

    class TrackViewHolder(val cell: MusicCell) : RecyclerView.ViewHolder(cell)

    private fun createDocsContent(): View {
        val container = ContainerCell(context)
        val docs = listOf(
            "DesignEngine" to "Глобальный движок тем и утилит.",
            "Theme" to "Цветовая схема (Dark/Light/Monet).",
            "LayoutHelper" to "Упрощенное создание LayoutParams.",
            "Cells" to "Набор готовых UI-компонентов.",
            "Usage" to "DesignEngine.Theme.color..."
        )
        docs.forEachIndexed { index, (title, _) ->
            val cell = TextCell(context)
            cell.setData(title, R.drawable.ic_brush)
            cell.setIconColor(DesignEngine.Theme.colorBlue)
            container.addCell(cell, index == docs.size - 1)
        }
        return container
    }
}
