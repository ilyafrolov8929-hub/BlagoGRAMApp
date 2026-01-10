package com.kia.blagogramapp.ui.modules
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import coil.load
import com.google.android.material.imageview.ShapeableImageView
import com.kia.blagogramapp.DesignEngine
import com.kia.blagogramapp.LayoutHelper
import com.kia.blagogramapp.R
import com.kia.blagogramapp.models.Track
import com.kia.blagogramapp.utils.MusicManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MiniPlayer(context: Context) : CardView(context) {
    private val coverView: ShapeableImageView
    private val titleView: TextView
    private val artistView: TextView
    private val playPauseBtn: ImageView
    private val progressBar: ProgressBar

    private var updateJob: Job? = null

    init {
        radius = LayoutHelper.dp(16).toFloat()
        cardElevation = LayoutHelper.dp(8).toFloat()
        setCardBackgroundColor(DesignEngine.Theme.colorCardBackground)

        val root = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

        progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            progressDrawable.setColorFilter(DesignEngine.Theme.colorBlue, PorterDuff.Mode.SRC_IN)
            max = 100
            progress = 0
            scaleY = 0.5f
        }
        root.addView(progressBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 4))

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(8), LayoutHelper.dp(12), LayoutHelper.dp(8))
        }

        coverView = ShapeableImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            shapeAppearanceModel = shapeAppearanceModel.toBuilder().setAllCornerSizes(LayoutHelper.dp(8).toFloat()).build()
        }
        content.addView(coverView, LayoutHelper.createLinear(48, 48))

        val info = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(LayoutHelper.dp(12), 0, LayoutHelper.dp(12), 0)
        }
        titleView = TextView(context).apply {
            textSize = 14f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(DesignEngine.Theme.colorTextPrimary); maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        }
        artistView = TextView(context).apply {
            textSize = 12f; setTextColor(DesignEngine.Theme.colorTextSecondary); maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        }
        info.addView(titleView); info.addView(artistView)
        content.addView(info, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f))

        playPauseBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_play)
            setColorFilter(DesignEngine.Theme.colorTextPrimary, PorterDuff.Mode.SRC_IN)
            setPadding(LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8))
            setOnClickListener { MusicManager.togglePlayPause() }
        }
        content.addView(playPauseBtn, LayoutHelper.createLinear(40, 40))

        root.addView(content)
        addView(root)
    }

    fun bind(track: Track, isPlaying: Boolean) {
        titleView.text = track.title
        artistView.text = track.artist
        coverView.load(track.albumArtUri) { error(R.drawable.ic_music); placeholder(R.drawable.ic_music) }
        playPauseBtn.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)

        progressBar.max = track.duration.toInt()
        startUpdateTask()
    }

    private fun startUpdateTask() {
        updateJob?.cancel()
        updateJob = CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                progressBar.progress = MusicManager.getCurrentPosition()
                delay(1000)
            }
        }
    }
}

class FullPlayer(context: Context) : RelativeLayout(context) {
    private val coverView: ShapeableImageView
    private val titleView: TextView
    private val artistView: TextView
    private val playPauseBtn: FrameLayout
    // ВОЗВРАЩАЕМ SEEKBAR
    private val seekBar: SeekBar
    private val timeCurrent: TextView
    private val timeTotal: TextView

    private var updateJob: Job? = null
    var onCloseListener: (() -> Unit)? = null

    init {
        setBackgroundColor(DesignEngine.Theme.colorBackground)
        isClickable = true
        isFocusable = true
        elevation = LayoutHelper.dp(20).toFloat()

        val themePrefs = context.getSharedPreferences("app_theme", AppCompatActivity.MODE_PRIVATE)
        val isCurrentThemeDark = themePrefs.getBoolean("is_dark_theme", false)

        val toolbar = DesignEngine.createToolBar(
            context = context,
            title = "Ваша музыка",
            leftButtonIconRes = R.drawable.ic_back,
            rightButtonIconRes = null,
            isDark = isCurrentThemeDark,
            leftButtonClickListener = { onCloseListener?.invoke() },
            rightButtonClickListener = null
        ).apply {
            id = View.generateViewId()
        }
        addView(toolbar, LayoutHelper.createRelative(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        val bottomControls = LinearLayout(context).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(DesignEngine.Theme.colorBackground)
            setPadding(LayoutHelper.dp(24), LayoutHelper.dp(16), LayoutHelper.dp(24), LayoutHelper.dp(40))
        }

        val scroll = ScrollView(context).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val scrollContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(LayoutHelper.dp(32), LayoutHelper.dp(32), LayoutHelper.dp(32), LayoutHelper.dp(32))
        }

        coverView = ShapeableImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            shapeAppearanceModel = shapeAppearanceModel.toBuilder().setAllCornerSizes(LayoutHelper.dp(24).toFloat()).build()
            elevation = LayoutHelper.dp(10).toFloat()
        }
        scrollContent.addView(coverView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, gravity = Gravity.CENTER_HORIZONTAL, top = 24, bottom = 0).apply {
            height = context.resources.displayMetrics.widthPixels - LayoutHelper.dp(64)
        })

        titleView = TextView(context).apply {
            textSize = 22f; typeface = Typeface.DEFAULT_BOLD; setTextColor(DesignEngine.Theme.colorTextPrimary)
            setPadding(0, LayoutHelper.dp(32), 0, 0); gravity = Gravity.CENTER
        }
        scrollContent.addView(titleView)

        artistView = TextView(context).apply {
            textSize = 17f; setTextColor(DesignEngine.Theme.colorTextSecondary); gravity = Gravity.CENTER
        }
        scrollContent.addView(artistView)

        scroll.addView(scrollContent)
        val scrollParams = LayoutHelper.createRelative(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT)
        scrollParams.addRule(RelativeLayout.BELOW, toolbar.id)
        scrollParams.addRule(RelativeLayout.ABOVE, bottomControls.id)
        addView(scroll, scrollParams)

        // ИЗМЕНЕНИЕ: Создаем кастомный Drawable для SeekBar
        val cornerRadius = LayoutHelper.dp(23).toFloat()
        val progressDrawable = LayerDrawable(arrayOf(
            // Фон (неактивная часть)
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(DesignEngine.Theme.colorHeader) // Используем серый цвет для фона
                cornerRadii = floatArrayOf(cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius)
            },
            // Прогресс (активная часть)
            ClipDrawable(
                GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(DesignEngine.Theme.colorBlue)
                    cornerRadii = floatArrayOf(cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius)
                },
                Gravity.START,
                ClipDrawable.HORIZONTAL
            ).apply { id = android.R.id.progress }
        )).apply {
            setId(0, android.R.id.background)
        }

        // ИЗМЕНЕНИЕ: Создаем и стилизуем SeekBar
        seekBar = SeekBar(context).apply {
            this.progressDrawable = progressDrawable
            // Делаем "бегунок" полностью прозрачным, чтобы его не было видно
            this.thumb.alpha = 0
            // ↓↓↓ ДОБАВЬТЕ ЭТИ ДВЕ СТРОЧКИ ↓↓↓
            this.setPadding(0, 0, 0, 0)
            this.splitTrack = false
            // Возвращаем обработчик перемотки
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    if (fromUser) {
                        MusicManager.seekTo(p)
                        // Обновляем текст текущего времени сразу при перемотке
                        timeCurrent.text = formatTime(p.toLong())
                    }
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }

        // Добавляем SeekBar с нужной высотой и отступами, чтобы было легче нажимать
        bottomControls.addView(seekBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0))

        val timeBox = RelativeLayout(context)
        timeCurrent = TextView(context).apply { textSize = 11f; setTextColor(DesignEngine.Theme.colorTextSecondary) }
        timeTotal = TextView(context).apply { textSize = 11f; setTextColor(DesignEngine.Theme.colorTextSecondary) }
        val totalP = LayoutParams(-2, -2).apply { addRule(ALIGN_PARENT_END) }
        timeBox.addView(timeCurrent); timeBox.addView(timeTotal, totalP)
        bottomControls.addView(timeBox, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, -2, top = 4))

        val buttons = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0, LayoutHelper.dp(24), 0, 0)
        }

        val prev = ImageView(context).apply {
            setImageResource(R.drawable.ic_prev); setColorFilter(DesignEngine.Theme.colorTextPrimary, PorterDuff.Mode.SRC_IN)
            setOnClickListener { MusicManager.playPrevious() }
            background = createSelectableBackground(context)
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(12), LayoutHelper.dp(12), LayoutHelper.dp(12))
        }
        buttons.addView(prev, LayoutHelper.createLinear(48, 48))

        playPauseBtn = FrameLayout(context).apply {
            val circle = View(context).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (isCurrentThemeDark) Color.parseColor("#2A2A2A") else Color.parseColor("#F0F0F0"))
                }
            }
            addView(circle, LayoutHelper.createFrame(70, 70))
            val icon = ImageView(context).apply {
                setImageResource(R.drawable.ic_play)
                setColorFilter(DesignEngine.Theme.colorTextPrimary, PorterDuff.Mode.SRC_IN)
            }
            addView(icon, LayoutHelper.createFrame(32, 32, Gravity.CENTER))
            setOnClickListener { MusicManager.togglePlayPause() }
        }
        buttons.addView(playPauseBtn, LayoutHelper.createLinear(70, 70, 0f, 0, 48, 0, 48, 0))

        val next = ImageView(context).apply {
            setImageResource(R.drawable.ic_next); setColorFilter(DesignEngine.Theme.colorTextPrimary, PorterDuff.Mode.SRC_IN)
            setOnClickListener { MusicManager.playNext() }
            background = createSelectableBackground(context)
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(12), LayoutHelper.dp(12), LayoutHelper.dp(12))
        }
        buttons.addView(next, LayoutHelper.createLinear(48, 48))

        bottomControls.addView(buttons)
        val bParams = LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT)
        bParams.addRule(ALIGN_PARENT_BOTTOM)
        addView(bottomControls, bParams)
    }

    private fun createSelectableBackground(context: Context): Drawable? = with(TypedValue()) {
        context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, this, true)
        context.getDrawable(resourceId)
    }

    fun animateOpen(fromView: View) {
        val location = IntArray(2)
        fromView.getLocationInWindow(location)

        visibility = View.VISIBLE
        alpha = 0f

        post {
            val fromX = location[0].toFloat()
            val fromY = location[1].toFloat()
            val fromW = fromView.width.toFloat()
            val fromH = fromView.height.toFloat()

            val screenW = width.toFloat()
            val screenH = height.toFloat()

            if (screenW == 0f || screenH == 0f) return@post

            translationX = fromX + fromW / 2 - screenW / 2
            translationY = fromY + fromH / 2 - screenH / 2
            scaleX = fromW / screenW
            scaleY = fromH / screenH

            animate()
                .alpha(1f)
                .translationX(0f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator())
                .setListener(null)
                .start()
        }
    }

    fun animateClose(toView: View, onEnd: () -> Unit) {
        val location = IntArray(2)
        toView.getLocationInWindow(location)

        val toX = location[0].toFloat()
        val toY = location[1].toFloat()
        val toW = toView.width.toFloat()
        val toH = toView.height.toFloat()

        val screenW = width.toFloat()
        val screenH = height.toFloat()

        animate()
            .alpha(0f)
            .translationX(toX + toW / 2 - screenW / 2)
            .translationY(toY + toH / 2 - screenH / 2)
            .scaleX(toW / screenW)
            .scaleY(toH / screenH)
            .setDuration(350)
            .setInterpolator(AccelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = View.GONE
                    onEnd()
                }
            })
            .start()
    }

    fun bind(track: Track, isPlaying: Boolean) {
        titleView.text = track.title
        artistView.text = track.artist
        coverView.load(track.albumArtUri) { error(R.drawable.ic_music); placeholder(R.drawable.ic_music) }

        // Связываем данные с SeekBar
        seekBar.max = track.duration.toInt()
        timeTotal.text = formatTime(track.duration)
        val icon = playPauseBtn.getChildAt(1) as ImageView
        icon.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
        startUpdateTask()
    }

    private fun startUpdateTask() {
        updateJob?.cancel()
        updateJob = CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                val pos = MusicManager.getCurrentPosition()
                // Обновляем SeekBar
                seekBar.progress = pos
                timeCurrent.text = formatTime(pos.toLong())
                delay(1000)
            }
        }
    }

    private fun formatTime(ms: Long): String {
        val s = ms / 1000; return "%d:%02d".format(s / 60, s % 60)
    }
}
