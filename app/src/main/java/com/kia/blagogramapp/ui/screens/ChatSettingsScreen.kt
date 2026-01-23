package com.kia.blagogramapp.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.kia.blagogramapp.BaseScreen
import com.kia.blagogramapp.ContainerCell
import com.kia.blagogramapp.DesignEngine
import com.kia.blagogramapp.HeaderCell
import com.kia.blagogramapp.LayoutHelper
import com.kia.blagogramapp.R
import com.kia.blagogramapp.SpacerCell
import com.kia.blagogramapp.TextCell
import com.kia.blagogramapp.WallpaperPreviewActivity
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random

class ChatSettingsScreen(context: Context) : BaseScreen(context) {
    val activity = context as? AppCompatActivity

    // Константы для кодов запроса
    private val RC_PICK_IMAGE = 101
    private val RC_WALLPAPER_PREVIEW = 102

    private var isCurrentThemeDark: Boolean = false
    private var isCurrentUseMonet: Boolean = false

    private lateinit var bubbleLeft: View
    private lateinit var bubbleRight: View
    private lateinit var wallpaperPreview: ImageView

    // Переопределяем метод для получения результата
    override fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { openWallpaperPreview(it.toString()) }
        } else if (requestCode == RC_WALLPAPER_PREVIEW && resultCode == Activity.RESULT_OK) {
            updateWallpaper()
        }
    }

    override fun createView(): View {
        val rootLayout = FrameLayout(context).apply {
            setBackgroundColor(DesignEngine.COLOR_BACKGROUND)
        }

        val themePrefs = context.getSharedPreferences("app_theme", MODE_PRIVATE)
        val isDarkTheme = themePrefs.getBoolean("is_dark_theme", false)
        val cameraPrefs = context.getSharedPreferences("camera_settings", Context.MODE_PRIVATE)

        val scrollView = ScrollView(context)
        scrollView.isFillViewport = true
        scrollView.clipToPadding = false
        scrollView.overScrollMode = View.OVER_SCROLL_NEVER

        val contentLayout = LinearLayout(context)
        contentLayout.orientation = LinearLayout.VERTICAL
        scrollView.addView(contentLayout)

        // Toolbar height spacer
        contentLayout.addView(SpacerCell(context, LayoutHelper.dp(32)))

        val sheetContainer = LinearLayout(context)
        sheetContainer.orientation = LinearLayout.VERTICAL
        sheetContainer.setPadding(LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(50))
        contentLayout.addView(sheetContainer)

        // --- 1. РАЗМЕР ТЕКСТА СООБЩЕНИЙ ---
        sheetContainer.addView(HeaderCell(context).apply {
            text = "Размер текста сообщений"
            setTextColor(DesignEngine.Theme.colorBlue)
            isAllCaps = false
            textSize = 14f
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(8), LayoutHelper.dp(16), LayoutHelper.dp(8))
        })

        val fontSizeCard = ContainerCell(context)

        // Slider row
        val sliderLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(8))
        }
        val fontSizeSeek = SeekBar(context).apply {
            max = 14
            progress = DesignEngine.fontSize - 12
            layoutParams = LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f)
        }
        val fontSizeValue = TextView(context).apply {
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
        val previewFrame = FrameLayout(context).apply {
            layoutParams = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 180, 0f, 0, 16, 8, 16, 16)
            clipToOutline = true
            background = GradientDrawable().apply { cornerRadius = LayoutHelper.dp(12).toFloat() }
        }
        wallpaperPreview = ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        updateWallpaper()
        previewFrame.addView(wallpaperPreview, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))

        val bubblesContainer = LinearLayout(context).apply {
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
        fontSizeCard.addCell(TextCell(context).apply {
            setData("Изменить обои", R.drawable.ic_photo)
            setTextColor(DesignEngine.Theme.colorBlue)
            setIconColor(DesignEngine.Theme.colorBlue)
            setOnClickListener { showWallpaperDialog() }
        }, isLast = true)

        sheetContainer.addView(fontSizeCard)
        sheetContainer.addView(SpacerCell(context, LayoutHelper.dp(5)))

        // --- 2. УГЛЫ БЛОКОВ С СООБЩЕНИЯМИ ---
        sheetContainer.addView(HeaderCell(context).apply {
            text = "Углы блоков с сообщениями"
            setTextColor(DesignEngine.Theme.colorBlue)
            isAllCaps = false
            textSize = 14f
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(8), LayoutHelper.dp(16), LayoutHelper.dp(8))
        })

        val radiusCard = ContainerCell(context)
        val radiusLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(12), LayoutHelper.dp(16), LayoutHelper.dp(12))
        }
        val radiusSeek = SeekBar(context).apply {
            max = 24
            progress = DesignEngine.bubbleRadius
            layoutParams = LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f)
        }
        val radiusValue = TextView(context).apply {
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
        sheetContainer.addView(SpacerCell(context, LayoutHelper.dp(16)))

        // --- 3. НАСТРОЙКИ КАМЕРЫ ---
        sheetContainer.addView(HeaderCell(context).apply {
            text = "Настройки камеры"
            setTextColor(DesignEngine.Theme.colorBlue)
            isAllCaps = false
            textSize = 14f
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(8), LayoutHelper.dp(16), LayoutHelper.dp(8))
        })

        val cameraCard = ContainerCell(context)

        // Тип камеры
        val typeCell = TextCell(context).apply {
            val type = cameraPrefs.getString("camera_type", "Camera 2")
            setData("Тип камеры", R.drawable.ic_camera)
            val valueText = TextView(context).apply {
                text = type
                setTextColor(DesignEngine.Theme.colorBlue)
                textSize = 14f
            }
            addView(valueText, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL or Gravity.END, 0, 0, 48, 0))
            setOnClickListener {
                val types = arrayOf("Camera 1", "Camera 2", "Camera X")
                AlertDialog.Builder(context)
                    .setTitle("Тип камеры")
                    .setItems(types) { _, which ->
                        cameraPrefs.edit().putString("camera_type", types[which]).apply()
                        valueText.text = types[which]
                    }
                    .show()
            }
        }
        cameraCard.addCell(typeCell)

        // Бесшовное переключение
        cameraCard.addCell(createSwitchCell("Бесшовное переключение", cameraPrefs.getBoolean("seamless", true)) { isChecked ->
            cameraPrefs.edit().putBoolean("seamless", isChecked).apply()
        })

        // Стабилизация
        cameraCard.addCell(createSwitchCell("Стабилизация", cameraPrefs.getBoolean("stabilization", false)) { isChecked ->
            cameraPrefs.edit().putBoolean("stabilization", isChecked).apply()
        })

        // Запоминать последнюю камеру
        cameraCard.addCell(createSwitchCell("Запоминать последнюю камеру", cameraPrefs.getBoolean("remember_camera", false), "При записи видеосообщений будет автоматически выбрана последняя использованная камера.") { isChecked ->
            cameraPrefs.edit().putBoolean("remember_camera", isChecked).apply()
        })

        // Статичный зум
        cameraCard.addCell(createSwitchCell("Статичный зум", cameraPrefs.getBoolean("static_zoom", false)) { isChecked ->
            cameraPrefs.edit().putBoolean("static_zoom", isChecked).apply()
        }, isLast = true)

        sheetContainer.addView(cameraCard)

        rootLayout.addView(scrollView)

        val toolbar = DesignEngine.createToolBar(
            context = context,
            title = "Кастомизация",
            leftButtonIconRes = R.drawable.ic_back,
            leftButtonClickListener = { finishFragment() },
            isDark = isDarkTheme
        )
        rootLayout.addView(toolbar)

        // Sliders Logic
        fontSizeSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) {
                DesignEngine.fontSize = p + 12
                fontSizeValue.text = DesignEngine.fontSize.toString()
                updateBubbles()
                DesignEngine.saveChatSettings(context)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        radiusSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) {
                DesignEngine.bubbleRadius = p
                radiusValue.text = p.toString()
                updateBubbles()
                DesignEngine.saveChatSettings(context)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        updateBubbles()
        return  rootLayout
    }

    private fun createSwitchCell(title: String, isChecked: Boolean, description: String? = null, onCheckedChange: (Boolean) -> Unit): LinearLayout {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(12), LayoutHelper.dp(16), LayoutHelper.dp(12))
        }

        val topLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val textView = TextView(context).apply {
            text = title
            textSize = 16f
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val switch = Switch(context).apply {
            this.isChecked = isChecked
            setOnCheckedChangeListener { _, checked -> onCheckedChange(checked) }
        }

        topLayout.addView(textView)
        topLayout.addView(switch)
        container.addView(topLayout)

        if (description != null) {
            val descView = TextView(context).apply {
                text = description
                textSize = 13f
                setTextColor(DesignEngine.Theme.colorTextSecondary)
                setPadding(0, LayoutHelper.dp(4), LayoutHelper.dp(48), 0)
            }
            container.addView(descView)
        }

        return container
    }

    private fun showWallpaperDialog() {
        val items = arrayOf<CharSequence>("Выбрать из галереи", "Сгенерировать обои", "Сбросить обои", "Отмена")
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Изменить обои")
        builder.setItems(items) { dialog, item ->
            when (item) {
                0 -> {
                    // Вместо лаунчера вызываем через активность
                    val intent = Intent(Intent.ACTION_GET_CONTENT).setType("image/*")
                    activity?.startActivityForResult(intent, RC_PICK_IMAGE)
                }
                1 -> createPatternedWallpaper()
                2 -> {
                    DesignEngine.chatWallpaper = "chat_bg_default"
                    DesignEngine.wallpaperBlur = false
                    DesignEngine.wallpaperMotion = false
                    updateWallpaper()
                    DesignEngine.saveChatSettings(context)
                }
                3 -> dialog.dismiss()
            }
        }
        builder.show()
    }

    private fun openWallpaperPreview(uri: String, isGenerated: Boolean = false) {
        val intent = Intent(context, WallpaperPreviewActivity::class.java).apply {
            putExtra("wallpaper_uri", uri)
            putExtra("is_generated", isGenerated)
        }
        activity?.startActivityForResult(intent, RC_WALLPAPER_PREVIEW)
    }

    private fun saveWallpaperFromUri(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(context.filesDir, "wallpaper_${System.currentTimeMillis()}.jpg")
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

        val file = File(context.filesDir, "pattern_wallpaper_${System.currentTimeMillis()}.png")
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
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
    }

    private fun updateWallpaper() {
        val wallpaper = DesignEngine.chatWallpaper
        if (wallpaper != null && wallpaper.startsWith("content://")) {
            val newPath = saveWallpaperFromUri(Uri.parse(wallpaper))
            if (newPath != null) {
                DesignEngine.chatWallpaper = newPath
                DesignEngine.saveChatSettings(context)
                wallpaperPreview.setImageURI(Uri.fromFile(File(newPath)))
            } else {
                DesignEngine.chatWallpaper = "chat_bg_default"
                DesignEngine.saveChatSettings(context)
                updateWallpaper()
            }
        } else if (wallpaper != null && (wallpaper.startsWith("/") || wallpaper.startsWith("file://"))) {
            val uri = if (wallpaper.startsWith("file://")) Uri.parse(wallpaper) else Uri.fromFile(File(wallpaper))
            wallpaperPreview.setImageURI(uri)
        } else { // Drawable resource
            val resId = context.resources.getIdentifier(wallpaper ?: "chat_bg_default", "drawable", context.packageName)
            if (resId != 0) {
                wallpaperPreview.setImageResource(resId)
            } else {
                wallpaperPreview.setBackgroundColor(0xFFE5E5E5.toInt())
            }
        }
    }

    private fun createBubble(text: String, isOutgoing: Boolean): View {
        val bubble = TextView(context).apply {
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