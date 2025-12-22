package com.kia.blagogramapp.ui.modules

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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

        val scroll = ScrollView(context).apply { isFillViewport = true }
        val scrollContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(LayoutHelper.dp(32), LayoutHelper.dp(100), LayoutHelper.dp(32), LayoutHelper.dp(200))
        }

        coverView = ShapeableImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            shapeAppearanceModel = shapeAppearanceModel.toBuilder().setAllCornerSizes(LayoutHelper.dp(24).toFloat()).build()
            elevation = LayoutHelper.dp(10).toFloat()
        }
        scrollContent.addView(coverView, LayoutHelper.createLinear(280, 280))

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
        addView(scroll, LayoutHelper.createRelative(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))

        val closeBtn = ImageView(context).apply {
            id = View.generateViewId()
            setImageResource(R.drawable.ic_back)
            setColorFilter(DesignEngine.Theme.colorTextPrimary, PorterDuff.Mode.SRC_IN)
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(12), LayoutHelper.dp(12), LayoutHelper.dp(12))
            val outValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
            if (outValue.resourceId != 0) background = context.getDrawable(outValue.resourceId)
            setOnClickListener { onCloseListener?.invoke() ?: (parent as? ViewGroup)?.removeView(this@FullPlayer) }
        }
        addView(closeBtn, LayoutHelper.createRelative(48, 48, 16, 40, 0, 0))

        val bottomControls = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(DesignEngine.Theme.colorBackground)
            setPadding(LayoutHelper.dp(24), LayoutHelper.dp(16), LayoutHelper.dp(24), LayoutHelper.dp(40))
        }

        seekBar = SeekBar(context).apply {
            progressDrawable.setColorFilter(DesignEngine.Theme.colorBlue, PorterDuff.Mode.SRC_IN)
            thumb.setColorFilter(DesignEngine.Theme.colorBlue, PorterDuff.Mode.SRC_IN)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { if (f) MusicManager.seekTo(p) }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        bottomControls.addView(seekBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        val timeBox = RelativeLayout(context)
        timeCurrent = TextView(context).apply { textSize = 11f; setTextColor(DesignEngine.Theme.colorTextSecondary) }
        timeTotal = TextView(context).apply { textSize = 11f; setTextColor(DesignEngine.Theme.colorTextSecondary) }
        val totalP = RelativeLayout.LayoutParams(-2, -2).apply { addRule(RelativeLayout.ALIGN_PARENT_END) }
        timeBox.addView(timeCurrent); timeBox.addView(timeTotal, totalP)
        bottomControls.addView(timeBox, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, -2))

        val buttons = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0, LayoutHelper.dp(24), 0, 0)
        }

        val prev = ImageView(context).apply { 
            setImageResource(R.drawable.ic_prev); setColorFilter(DesignEngine.Theme.colorTextPrimary, PorterDuff.Mode.SRC_IN)
            setOnClickListener { MusicManager.playPrevious() }
        }
        buttons.addView(prev, LayoutHelper.createLinear(48, 48))

        playPauseBtn = FrameLayout(context).apply {
            val circle = View(context).apply { background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(DesignEngine.Theme.colorBlue) } }
            addView(circle, LayoutHelper.createFrame(70, 70))
            val icon = ImageView(context).apply { setImageResource(R.drawable.ic_play); setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN) }
            addView(icon, LayoutHelper.createFrame(32, 32, Gravity.CENTER))
            setOnClickListener { MusicManager.togglePlayPause() }
        }
        buttons.addView(playPauseBtn, LayoutHelper.createLinear(70, 70, 0f, 0, 32, 0, 32, 0))

        val next = ImageView(context).apply { 
            setImageResource(R.drawable.ic_next); setColorFilter(DesignEngine.Theme.colorTextPrimary, PorterDuff.Mode.SRC_IN)
            setOnClickListener { MusicManager.playNext() }
        }
        buttons.addView(next, LayoutHelper.createLinear(48, 48))

        bottomControls.addView(buttons)
        val bParams = RelativeLayout.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT)
        bParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        addView(bottomControls, bParams)
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
