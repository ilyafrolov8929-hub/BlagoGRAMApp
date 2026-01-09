package com.kia.blagogramapp

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import java.io.File
import java.io.FileOutputStream

class WallpaperPreviewActivity : AppCompatActivity(), SensorEventListener {

    private var wallpaperUri: String? = null
    private var isGenerated: Boolean = false

    private lateinit var wallpaperView: ImageView
    private lateinit var wallpaperBlurView: ImageView
    private lateinit var motionSwitch: SwitchMaterial

    private lateinit var sensorManager: SensorManager
    private var gyroscope: Sensor? = null

    private var ts: Long = 0
    private var targetTranslationX = 0f
    private var targetTranslationY = 0f
    private var parallaxAnimator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DesignEngine.init(this)
        val prefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        val isDark = prefs.getBoolean("is_dark_theme", false)
        val useMonet = prefs.getBoolean("use_monet", false)
        DesignEngine.Theme.apply(isDark, useMonet)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = !isDark
        controller.isAppearanceLightNavigationBars = !isDark

        val statusBarHeight = getStatusBarHeight()

        val rootLayout = FrameLayout(this)
        rootLayout.setBackgroundColor(DesignEngine.Theme.colorBackground)
        setContentView(rootLayout)

        wallpaperUri = intent.getStringExtra("wallpaper_uri")
        isGenerated = intent.getBooleanExtra("is_generated", false)

        wallpaperView = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        rootLayout.addView(wallpaperView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))

        wallpaperBlurView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            if (DesignEngine.wallpaperBlur) {
                alpha = 1f
                visibility = View.VISIBLE
            } else {
                alpha = 0f
                visibility = View.GONE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRenderEffect(RenderEffect.createBlurEffect(50f, 50f, Shader.TileMode.MIRROR))
            }
        }
        rootLayout.addView(wallpaperBlurView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))

        if (wallpaperUri != null) {
            val uri = Uri.parse(wallpaperUri)
            wallpaperView.setImageURI(uri)
            wallpaperBlurView.setImageURI(uri)
        } else {
            finish()
            return
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        val bubblesContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(12), LayoutHelper.dp(12), LayoutHelper.dp(12))
        }
        bubblesContainer.addView(createBubble("Илья\nДоброе утро! 👋", false))
        bubblesContainer.addView(createBubble("В Токио утро 😎", true))
        rootLayout.addView(bubblesContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER))

        // Toolbar
        val toolbarLayout = RelativeLayout(this)
        // Мы убрали setPadding отсюда

        val backBtn = LinearLayout(this).apply {
            setBackgroundResource(R.drawable.bg_stat_pill)
            backgroundTintList = android.content.res.ColorStateList.valueOf(if (isDark) Color.parseColor("#333333") else Color.parseColor("#E0E0E0"))
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(6), LayoutHelper.dp(12), LayoutHelper.dp(6))
            setOnClickListener { finish() }
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_back)
                setColorFilter(if (isDark) Color.WHITE else Color.BLACK)
                layoutParams = LinearLayout.LayoutParams(LayoutHelper.dp(20), LayoutHelper.dp(20))
            })
        }

        val titleView = LinearLayout(this).apply {
            setBackgroundResource(R.drawable.bg_stat_pill)
            backgroundTintList = android.content.res.ColorStateList.valueOf(if (isDark) Color.parseColor("#333333") else Color.parseColor("#E0E0E0"))
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(6), LayoutHelper.dp(12), LayoutHelper.dp(6))
            gravity = Gravity.CENTER // Центрируем текст внутри
            addView(TextView(context).apply {
                text = "Кастомизация" // Добавляем заголовок
                textSize = 20f
                setTextColor(DesignEngine.Theme.colorTextPrimary)
                typeface = Typeface.DEFAULT_BOLD
            })
        }
        val toolbarContent = RelativeLayout(this)
        // Добавьте эту строку
        toolbarContent.layoutParams = RelativeLayout.LayoutParams(-1, -2) // -1 это MATCH_PARENT, -2 это WRAP_CONTENT
        toolbarContent.addView(backBtn, RelativeLayout.LayoutParams(-2, -2).apply { addRule(RelativeLayout.ALIGN_PARENT_START); addRule(RelativeLayout.CENTER_VERTICAL) })
        toolbarContent.addView(titleView, RelativeLayout.LayoutParams(-2, -2).apply { addRule(RelativeLayout.CENTER_IN_PARENT) })
        toolbarLayout.addView(toolbarContent)

        rootLayout.addView(toolbarLayout, FrameLayout.LayoutParams(-1, -2, Gravity.TOP))

        val bottomSheet = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0, 0x66000000))
            setPadding(LayoutHelper.dp(10), LayoutHelper.dp(16), LayoutHelper.dp(10), LayoutHelper.dp(16))
        }

        val controlsContainer = ContainerCell(this).apply {
            setPadding(0, LayoutHelper.dp(8), 0, LayoutHelper.dp(8))
        }

        val blurSwitch = SwitchMaterial(this).apply {
            text = "Размытие"
            textSize = 16f
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            isChecked = DesignEngine.wallpaperBlur
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(12), LayoutHelper.dp(16), LayoutHelper.dp(12))
            setOnCheckedChangeListener { _, isChecked ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (isChecked) {
                        wallpaperBlurView.alpha = 0f
                        wallpaperBlurView.visibility = View.VISIBLE
                    }
                    wallpaperBlurView.animate().alpha(if (isChecked) 1f else 0f).setDuration(200).withEndAction {
                        if (!isChecked) {
                            wallpaperBlurView.visibility = View.GONE
                        }
                    }.start()
                }
            }
        }
        controlsContainer.addCell(blurSwitch)

        motionSwitch = SwitchMaterial(this).apply {
            text = "Движение"
            textSize = 16f
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            isChecked = DesignEngine.wallpaperMotion
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(12), LayoutHelper.dp(16), LayoutHelper.dp(12))
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) registerSensor() else unregisterSensor()
            }
        }
        controlsContainer.addCell(motionSwitch, true)

        bottomSheet.addView(controlsContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        val applyButton = TextView(this).apply {
            text = "УСТАНОВИТЬ ОБОИ"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorBlue)
                cornerRadius = LayoutHelper.dp(12).toFloat()
            }
            setPadding(0, LayoutHelper.dp(14), 0, LayoutHelper.dp(14))
            setOnClickListener {
                val finalPath = if (isGenerated) {
                    wallpaperUri
                } else {
                    saveWallpaperFromUri(Uri.parse(wallpaperUri))
                }

                if (finalPath != null) {
                    DesignEngine.chatWallpaper = finalPath
                    DesignEngine.wallpaperBlur = blurSwitch.isChecked
                    DesignEngine.wallpaperMotion = motionSwitch.isChecked
                    DesignEngine.saveChatSettings(this@WallpaperPreviewActivity)
                    setResult(Activity.RESULT_OK)
                } else {
                    setResult(Activity.RESULT_CANCELED)
                }
                finish()
            }
        }
        bottomSheet.addView(applyButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 16, 0, 0))

        rootLayout.addView(bottomSheet, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM))

        // Обработка системных отступов (Insets)
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Правильно задаем отступы для тулбара, как в ChatSettingsActivity
            toolbarLayout.setPadding(
                LayoutHelper.dp(16),
                insets.top + LayoutHelper.dp(10),
                LayoutHelper.dp(16),
                LayoutHelper.dp(10)
            )

            // Всему корневому слою задаем нижний отступ (док-бар)
            view.setPadding(0, 0, 0, insets.bottom)

            windowInsets
        }
    }

    override fun onResume() {
        super.onResume()
        if (motionSwitch.isChecked) {
            registerSensor()
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterSensor()
    }

    private fun registerSensor() {
        ts = 0
        targetTranslationX = wallpaperView.translationX
        targetTranslationY = wallpaperView.translationY

        parallaxAnimator?.cancel()
        parallaxAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            interpolator = null
            addUpdateListener {
                wallpaperView.translationX += (targetTranslationX - wallpaperView.translationX) * 0.1f
                wallpaperView.translationY += (targetTranslationY - wallpaperView.translationY) * 0.1f
                wallpaperBlurView.translationX = wallpaperView.translationX
                wallpaperBlurView.translationY = wallpaperView.translationY
            }
            start()
        }
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME)

        wallpaperView.animate().scaleX(1.1f).scaleY(1.1f).setDuration(300).setInterpolator(DecelerateInterpolator()).start()
        wallpaperBlurView.animate().scaleX(1.1f).scaleY(1.1f).setDuration(300).setInterpolator(DecelerateInterpolator()).start()
    }

    private fun unregisterSensor() {
        sensorManager.unregisterListener(this)
        parallaxAnimator?.cancel()
        wallpaperView.animate().translationX(0f).translationY(0f).scaleX(1f).scaleY(1f).setDuration(300).setInterpolator(DecelerateInterpolator()).start()
        wallpaperBlurView.animate().translationX(0f).translationY(0f).scaleX(1f).scaleY(1f).setDuration(300).setInterpolator(DecelerateInterpolator()).start()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_GYROSCOPE) {
            if (ts == 0L) {
                ts = event.timestamp
                return
            }
            val dt = (event.timestamp - ts) / 1000000000.0f
            ts = event.timestamp

            val dx = event.values[1] * dt
            val dy = event.values[0] * dt

            val maxScroll = LayoutHelper.dp(20).toFloat()

            targetTranslationX -= dx * 100
            targetTranslationY += dy * 100

            targetTranslationX = targetTranslationX.coerceIn(-maxScroll, maxScroll)
            targetTranslationY = targetTranslationY.coerceIn(-maxScroll, maxScroll)
        }
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }

    private fun createBubble(text: String, isOutgoing: Boolean): View {
        val bubble = TextView(this).apply {
            this.text = text
            textSize = DesignEngine.fontSize.toFloat()
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(8), LayoutHelper.dp(12), LayoutHelper.dp(8))
            setTextColor(DesignEngine.Theme.colorTextPrimary)
        }

        val radius = LayoutHelper.dp(DesignEngine.bubbleRadius).toFloat()
        val smallRadius = LayoutHelper.dp(4).toFloat()
        bubble.background = GradientDrawable().apply {
            setColor(if (isOutgoing) DesignEngine.Theme.colorSheetBackground else DesignEngine.Theme.colorCardBackground)
            cornerRadii = if (isOutgoing) floatArrayOf(radius, radius, smallRadius, smallRadius, radius, radius, radius, radius) else floatArrayOf(radius, radius, radius, radius, smallRadius, smallRadius, radius, radius)
        }

        val lp = LinearLayout.LayoutParams(-2, -2).apply {
            gravity = if (isOutgoing) Gravity.END else Gravity.START
            topMargin = LayoutHelper.dp(4)
        }
        bubble.layoutParams = lp
        return bubble
    }

    private fun saveWallpaperFromUri(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val file = File(filesDir, "wallpaper_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}