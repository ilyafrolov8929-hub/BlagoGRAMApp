package com.kia.blagogramapp

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.random.Random

class ChatSettingsActivity : AppCompatActivity() {

    private lateinit var bubbleLeft: View
    private lateinit var bubbleRight: View
    private lateinit var wallpaperPreview: ImageView

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { openWallpaperPreview(it.toString()) }
    }

    private val wallpaperPreviewLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            updateWallpaper()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DesignEngine.init(this)
        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        val isDarkTheme = themePrefs.getBoolean("is_dark_theme", false)
        val useMonet = themePrefs.getBoolean("use_monet", false)

        DesignEngine.Theme.apply(isDarkTheme, useMonet)

        // Настройка прозрачности системных панелей
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = !isDarkTheme
        controller.isAppearanceLightNavigationBars = !isDarkTheme

        val statusBarHeight = getStatusBarHeight()

        val rootLayout = FrameLayout(this)
        rootLayout.setBackgroundColor(DesignEngine.Theme.colorBackground)
        setContentView(rootLayout)

        val scrollView = ScrollView(this)
        scrollView.isFillViewport = true
        scrollView.clipToPadding = false
        scrollView.overScrollMode = View.OVER_SCROLL_NEVER

        val contentLayout = LinearLayout(this)
        contentLayout.orientation = LinearLayout.VERTICAL
        scrollView.addView(contentLayout)

        // Toolbar height spacer
        contentLayout.addView(SpacerCell(this, LayoutHelper.dp(32)))

        val sheetContainer = LinearLayout(this)
        sheetContainer.orientation = LinearLayout.VERTICAL
        sheetContainer.setPadding(LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(50))
        contentLayout.addView(sheetContainer)

        // --- 1. РАЗМЕР ТЕКСТА СООБЩЕНИЙ ---
        sheetContainer.addView(HeaderCell(this).apply {
            text = "Размер текста сообщений"
            setTextColor(DesignEngine.Theme.colorBlue)
            isAllCaps = false
            textSize = 14f
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(8), LayoutHelper.dp(16), LayoutHelper.dp(8))
        })

        val fontSizeCard = ContainerCell(this)

        // Slider row
        val sliderLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(8))
        }
        val fontSizeSeek = SeekBar(this).apply {
            max = 14
            progress = DesignEngine.fontSize - 12
            layoutParams = LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f)
        }
        val fontSizeValue = TextView(this).apply {
            text = DesignEngine.fontSize.toString()
            textSize = 16f
            setTextColor(DesignEngine.Theme.colorBlue)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(LayoutHelper.dp(12), 0, 0, 0)
        }
        sliderLayout.addView(fontSizeSeek)
        sliderLayout.addView(fontSizeValue)
        fontSizeCard.addView(sliderLayout)

        // Preview area
        val previewFrame = FrameLayout(this).apply {
            layoutParams = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 180, 0f, 0, 16, 8, 16, 16)
            clipToOutline = true
            background = GradientDrawable().apply { cornerRadius = LayoutHelper.dp(12).toFloat() }
        }
        wallpaperPreview = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        updateWallpaper()
        previewFrame.addView(wallpaperPreview, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))

        val bubblesContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(12), LayoutHelper.dp(12), LayoutHelper.dp(12))
        }
        bubbleLeft = createBubble("Илья\nДоброе утро! 👋\nЗнаешь, который час?", false)
        bubbleRight = createBubble("В Токио утро 😎", true)
        bubblesContainer.addView(bubbleLeft)
        bubblesContainer.addView(bubbleRight)
        previewFrame.addView(bubblesContainer)
        fontSizeCard.addView(previewFrame)

        // Actions
        fontSizeCard.addCell(TextCell(this).apply {
            setData("Изменить обои", R.drawable.ic_photo)
            setTextColor(DesignEngine.Theme.colorBlue)
            setIconColor(DesignEngine.Theme.colorBlue)
            setOnClickListener { showWallpaperDialog() }
        }, isLast = true)

        sheetContainer.addView(fontSizeCard)
        sheetContainer.addView(SpacerCell(this, LayoutHelper.dp(5)))

        // --- 3. УГЛЫ БЛОКОВ С СООБЩЕНИЯМИ ---
        sheetContainer.addView(HeaderCell(this).apply {
            text = "Углы блоков с сообщениями"
            setTextColor(DesignEngine.Theme.colorBlue)
            isAllCaps = false
            textSize = 14f
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(8), LayoutHelper.dp(16), LayoutHelper.dp(8))
        })

        val radiusCard = ContainerCell(this)
        val radiusLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(12), LayoutHelper.dp(16), LayoutHelper.dp(12))
        }
        val radiusSeek = SeekBar(this).apply {
            max = 24
            progress = DesignEngine.bubbleRadius
            layoutParams = LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f)
        }
        val radiusValue = TextView(this).apply {
            text = DesignEngine.bubbleRadius.toString()
            textSize = 16f
            setTextColor(DesignEngine.Theme.colorBlue)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(LayoutHelper.dp(12), 0, 0, 0)
        }
        radiusLayout.addView(radiusSeek)
        radiusLayout.addView(radiusValue)
        radiusCard.addView(radiusLayout)
        sheetContainer.addView(radiusCard)

        rootLayout.addView(scrollView)

        val toolbar = DesignEngine.createToolBar(
            context = this,
            title = "Кастомизация",
            leftButtonIconRes = R.drawable.ic_back,
            leftButtonClickListener = { finish() },
            isDark = isDarkTheme
        )
        rootLayout.addView(toolbar)

        // Sliders Logic
        fontSizeSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) {
                DesignEngine.fontSize = p + 12
                fontSizeValue.text = DesignEngine.fontSize.toString()
                updateBubbles()
                DesignEngine.saveChatSettings(this@ChatSettingsActivity)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        radiusSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) {
                DesignEngine.bubbleRadius = p
                radiusValue.text = p.toString()
                updateBubbles()
                DesignEngine.saveChatSettings(this@ChatSettingsActivity)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        updateBubbles()
    }

    private fun showWallpaperDialog() {
        val items = arrayOf<CharSequence>("Выбрать из галереи", "Сгенерировать обои", "Сбросить обои", "Отмена")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Изменить обои")
        builder.setItems(items) { dialog, item ->
            when (item) {
                0 -> selectImageLauncher.launch("image/*")
                1 -> createPatternedWallpaper()
                2 -> {
                    DesignEngine.chatWallpaper = "chat_bg_default"
                    DesignEngine.wallpaperBlur = false
                    DesignEngine.wallpaperMotion = false
                    updateWallpaper()
                    DesignEngine.saveChatSettings(this)
                }
                3 -> dialog.dismiss()
            }
        }
        builder.show()
    }

    private fun openWallpaperPreview(uri: String, isGenerated: Boolean = false) {
        val intent = Intent(this, WallpaperPreviewActivity::class.java)
        intent.putExtra("wallpaper_uri", uri)
        intent.putExtra("is_generated", isGenerated)
        wallpaperPreviewLauncher.launch(intent)
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

    private fun createPatternedWallpaper() {
        val width = 500
        val height = 500
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val backgroundPaint = Paint()
        backgroundPaint.color = DesignEngine.Theme.colorBackground
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        // Pattern
        val patternPaint = Paint()
        patternPaint.color = DesignEngine.Theme.colorBlue
        patternPaint.style = Paint.Style.FILL

        for (i in 0..100) {
            val x = Random.nextFloat() * width
            val y = Random.nextFloat() * height
            val radius = Random.nextFloat() * 20 + 5
            canvas.drawCircle(x, y, radius, patternPaint)
        }

        val file = File(filesDir, "pattern_wallpaper_${System.currentTimeMillis()}.png")
        try {
            val fOut = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut)
            fOut.flush()
            fOut.close()

            openWallpaperPreview(Uri.fromFile(file).toString(), true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }

    private fun updateWallpaper() {
        val wallpaper = DesignEngine.chatWallpaper
        if (wallpaper != null && wallpaper.startsWith("content://")) {
            val newPath = saveWallpaperFromUri(Uri.parse(wallpaper))
            if (newPath != null) {
                DesignEngine.chatWallpaper = newPath
                DesignEngine.saveChatSettings(this)
                wallpaperPreview.setImageURI(Uri.fromFile(File(newPath)))
            } else {
                DesignEngine.chatWallpaper = "chat_bg_default"
                DesignEngine.saveChatSettings(this)
                updateWallpaper()
            }
        } else if (wallpaper != null && (wallpaper.startsWith("/") || wallpaper.startsWith("file://"))) { 
            val uri = if (wallpaper.startsWith("file://")) Uri.parse(wallpaper) else Uri.fromFile(File(wallpaper))
            wallpaperPreview.setImageURI(uri)
        } else { // Drawable resource
            val resId = resources.getIdentifier(wallpaper ?: "chat_bg_default", "drawable", packageName)
            if (resId != 0) {
                wallpaperPreview.setImageResource(resId)
            } else {
                wallpaperPreview.setBackgroundColor(0xFFE5E5E5.toInt())
            }
        }
    }

    private fun createBubble(text: String, isOutgoing: Boolean): View {
        val bubble = TextView(this).apply {
            this.text = text
            textSize = DesignEngine.fontSize.toFloat()
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(8), LayoutHelper.dp(12), LayoutHelper.dp(8))
            setTextColor(DesignEngine.Theme.colorTextPrimary)
        }

        bubble.layoutParams = LinearLayout.LayoutParams(-2, -2).apply {
            gravity = if (isOutgoing) Gravity.END else Gravity.START
            topMargin = LayoutHelper.dp(4)
        }
        return bubble
    }

    private fun updateBubbles() {
        applyBubbleStyle(bubbleLeft, false)
        applyBubbleStyle(bubbleRight, true)
    }

    private fun applyBubbleStyle(view: View, isOutgoing: Boolean) {
        val textView = view as TextView
        textView.textSize = DesignEngine.fontSize.toFloat()
        val radius = LayoutHelper.dp(DesignEngine.bubbleRadius).toFloat()
        val smallRadius = LayoutHelper.dp(4).toFloat()

        view.background = GradientDrawable().apply {
            val bgColor = if (isOutgoing) {
                DesignEngine.Theme.colorSheetBackground
            } else {
                DesignEngine.Theme.colorCardBackground
            }
            setColor(bgColor)

            cornerRadii = if (isOutgoing)
                floatArrayOf(radius, radius, radius, radius, smallRadius, smallRadius, radius, radius)
            else
                floatArrayOf(radius, radius, radius, radius, radius, radius, smallRadius, smallRadius)
        }

        if (isOutgoing && DesignEngine.Theme.colorBackground == 0xFF1A1A1A.toInt()) {
            textView.setTextColor(Color.WHITE)
        } else {
            textView.setTextColor(DesignEngine.Theme.colorTextPrimary)
        }
    }
}
