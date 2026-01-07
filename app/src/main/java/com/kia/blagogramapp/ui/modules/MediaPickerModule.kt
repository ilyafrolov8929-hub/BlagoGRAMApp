package com.kia.blagogramapp.ui.modules

import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.imageview.ShapeableImageView
import com.kia.blagogramapp.DesignEngine
import com.kia.blagogramapp.LayoutHelper
import com.kia.blagogramapp.R
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class MediaPickerSheet(context: Context, val mode: Mode = Mode.CHAT) : FrameLayout(context) {

    enum class Mode {
        CHAT, AVATAR
    }

    val gridContainer: LinearLayout
    val previewContainer: FrameLayout
    val editorContainer: FrameLayout

    private val recyclerView: RecyclerView
    val adapter: MediaAdapter
    private val albumTitle: TextView
    private val albumArrow: ImageView

    private val previewImage: com.github.chrisbanes.photoview.PhotoView
    private val previewTopGradient: View
    private val previewBottomGradient: View

    private lateinit var editorImage: com.github.chrisbanes.photoview.PhotoView
    private lateinit var editorOverlay: CircleOverlayView
    private lateinit var editorName: TextView
    private lateinit var editorSmallAvatar: ShapeableImageView

    private val floatingBottomBar: LinearLayout
    private val categoriesLayout: LinearLayout
    private val captionLayout: LinearLayout
    private val captionInput: EditText
    private val sendBtn: FrameLayout
    private val sendBadge: TextView
    private val toolsLayout: RelativeLayout
    private val toolsIconsContainer: LinearLayout
    private val toolsLabel: TextView
    private val toolIcons = mutableListOf<ImageView>()

    private val albums = mutableListOf<Album>()
    private var currentPreviewUri: Uri? = null

    var behavior: BottomSheetBehavior<*>? = null
    var dialog: BottomSheetDialog? = null
    var onResult: ((List<Uri>, String?) -> Unit)? = null

    private var lastSlideOffset = 0f
    private var currentKeyboardHeight = 0

    init {
        currentInstance = this
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        // 1. GRID LAYER
        gridContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val r = LayoutHelper.dp(20).toFloat()
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorSheetBackground)
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            }
        }

        val handle = View(context).apply {
            background = GradientDrawable().apply { setColor(0x40808080.toInt()); cornerRadius = LayoutHelper.dp(2).toFloat() }
        }
        gridContainer.addView(handle, LayoutHelper.createLinear(40, 4, 0f, Gravity.CENTER_HORIZONTAL, 0, 8, 0, 8))

        val header = RelativeLayout(context).apply { setPadding(LayoutHelper.dp(16), 0, LayoutHelper.dp(16), LayoutHelper.dp(8)) }
        val titleContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setOnClickListener { if (adapter.selected.isEmpty()) showAlbumPicker() }
        }
        albumTitle = TextView(context).apply { text = if (mode == Mode.AVATAR) "Выберите фото" else "Все медиа"; textSize = 18f; typeface = Typeface.DEFAULT_BOLD; setTextColor(DesignEngine.Theme.colorTextPrimary) }
        titleContainer.addView(albumTitle)
        albumArrow = ImageView(context).apply { setImageResource(R.drawable.ic_chevron_right); rotation = 90f; setColorFilter(DesignEngine.Theme.colorTextSecondary, PorterDuff.Mode.SRC_IN) }
        titleContainer.addView(albumArrow, LayoutHelper.createLinear(20, 20, 0f, 0, 4, 0, 0, 0))
        header.addView(titleContainer, LayoutHelper.createRelative(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))

        val closeBtn = ImageView(context).apply { setImageResource(R.drawable.ic_close_small); setColorFilter(DesignEngine.Theme.colorTextSecondary, PorterDuff.Mode.SRC_IN); setOnClickListener { dialog?.dismiss() } }
        header.addView(closeBtn, LayoutHelper.createRelative(24, 24).apply { addRule(RelativeLayout.ALIGN_PARENT_RIGHT) })
        gridContainer.addView(header)

        recyclerView = RecyclerView(context).apply {
            layoutManager = GridLayoutManager(context, 3); setPadding(2, 0, 2, LayoutHelper.dp(120)); clipToPadding = false
        }
        adapter = MediaAdapter(); recyclerView.adapter = adapter
        gridContainer.addView(recyclerView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1f))
        addView(gridContainer)

        // 2. PREVIEW LAYER
        previewContainer = FrameLayout(context).apply { visibility = View.GONE; setBackgroundColor(Color.BLACK) }
        previewImage = com.github.chrisbanes.photoview.PhotoView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
        previewContainer.addView(previewImage, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        previewTopGradient = View(context).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0x99000000.toInt(), Color.TRANSPARENT))
        }
        previewContainer.addView(previewTopGradient, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 80, Gravity.TOP))

        previewBottomGradient = View(context).apply {
            background = GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, intArrayOf(0x99000000.toInt(), Color.TRANSPARENT))
        }
        previewContainer.addView(previewBottomGradient, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 200, Gravity.BOTTOM))

        val previewHeader = FrameLayout(context)
        ViewCompat.setOnApplyWindowInsetsListener(previewHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(LayoutHelper.dp(16), status.top + LayoutHelper.dp(8), LayoutHelper.dp(16), 0)
            insets
        }
        previewHeader.addView(ImageView(context).apply { setImageResource(R.drawable.ic_back); setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN); setOnClickListener { showGrid() } }, LayoutHelper.createFrame(24, 24, top = 20, left = 20))
        val previewCircle = TextView(context).apply { textSize = 14f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); gravity = Gravity.CENTER; setOnClickListener { togglePreviewSelection() } }
        previewHeader.addView(previewCircle, LayoutHelper.createFrame(28, 28, Gravity.END))
        previewContainer.addView(previewHeader)
        addView(previewContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // 3. EDITOR LAYER
        editorContainer = FrameLayout(context).apply { visibility = View.GONE; setBackgroundColor(Color.BLACK) }
        initAvatarEditor()
        addView(editorContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // 4. FLOATING BAR
        floatingBottomBar = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            elevation = LayoutHelper.dp(20).toFloat()
        }

        captionLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.BOTTOM
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(8), LayoutHelper.dp(12), LayoutHelper.dp(8))
            visibility = View.GONE
        }
        
        captionInput = EditText(context).apply {
            hint = "Добавить подпись..."; setHintTextColor(0x80FFFFFF.toInt()); setTextColor(Color.WHITE); textSize = 16f
            setPadding(LayoutHelper.dp(20), LayoutHelper.dp(10), LayoutHelper.dp(20), LayoutHelper.dp(10))
            background = GradientDrawable().apply { setColor(0x66000000); cornerRadius = LayoutHelper.dp(30).toFloat() }
            setOnFocusChangeListener { _, hasFocus -> if (hasFocus) behavior?.state = BottomSheetBehavior.STATE_EXPANDED }
        }
        captionLayout.addView(captionInput, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f))

        sendBtn = FrameLayout(context).apply {
            background = GradientDrawable().apply { setColor(DesignEngine.Theme.colorBlue); cornerRadius = LayoutHelper.dp(22).toFloat() }
            setOnClickListener { onSendClicked() }
        }
        sendBtn.addView(ImageView(context).apply { setImageResource(R.drawable.ic_send); setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN) }, LayoutHelper.createFrame(24, 24, Gravity.CENTER))
        sendBadge = TextView(context).apply {
            textSize = 10f; typeface = Typeface.DEFAULT_BOLD; setTextColor(DesignEngine.Theme.colorBlue); gravity = Gravity.CENTER
            background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = LayoutHelper.dp(10).toFloat() }
            visibility = View.GONE
        }
        sendBtn.addView(sendBadge, LayoutHelper.createFrame(18, 18, Gravity.BOTTOM or Gravity.END))
        captionLayout.addView(sendBtn, LayoutHelper.createLinear(44, 44, 0f, 0, 8, 0, 0, 0))
        floatingBottomBar.addView(captionLayout)

        toolsLayout = RelativeLayout(context).apply {
            setPadding(LayoutHelper.dp(12), 0, LayoutHelper.dp(12), LayoutHelper.dp(12))
            visibility = View.GONE
        }
        toolsLabel = TextView(context).apply { id = View.generateViewId() }
        toolsLayout.addView(toolsLabel, LayoutHelper.createRelative(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT).apply {
            addRule(RelativeLayout.CENTER_HORIZONTAL); setMargins(0, 0, 0, LayoutHelper.dp(8))
        })
        toolsIconsContainer = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val icons = intArrayOf(R.drawable.ic_edit, R.drawable.ic_music, R.drawable.ic_more, R.drawable.ic_settings)
        icons.forEach { iconRes ->
            val icon = ImageView(context).apply { 
                setImageResource(iconRes); setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                setPadding(LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8))
                if (iconRes == R.drawable.ic_edit) {
                    setOnClickListener {
                        currentPreviewUri?.let { uri ->
                            if (mode == Mode.CHAT) openExternalEditor(uri)
                        }
                    }
                }
            }
            toolIcons.add(icon)
            toolsIconsContainer.addView(icon, LayoutHelper.createLinear(40, 40, 0f, 0, 0, 4, 0, 0))
        }
        toolsLayout.addView(toolsIconsContainer, LayoutHelper.createRelative(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT).apply {
            addRule(RelativeLayout.ALIGN_PARENT_LEFT); addRule(RelativeLayout.BELOW, toolsLabel.id)
        })
        floatingBottomBar.addView(toolsLayout)

        categoriesLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0, LayoutHelper.dp(12), 0, LayoutHelper.dp(16))
            background = GradientDrawable().apply { setColor(DesignEngine.Theme.colorSheetBackground) }
        }
        categoriesLayout.addView(createBottomItem(R.drawable.ic_photo, "Галерея", DesignEngine.Theme.colorBlue))
        categoriesLayout.addView(createBottomItem(R.drawable.ic_music, "Музыка", 0xFF2196F3.toInt()))
        categoriesLayout.addView(createBottomItem(R.drawable.ic_more, "Файл", 0xFF4CAF50.toInt()))
        categoriesLayout.addView(createBottomItem(R.drawable.ic_user, "Контакт", 0xFFFF9800.toInt()))
        floatingBottomBar.addView(categoriesLayout)

        addView(floatingBottomBar, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))

        adapter.onSelectionChanged = { updateUIState() }
        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> syncBarPosition() }
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            currentKeyboardHeight = if (ime.bottom > 0) ime.bottom else nav.bottom
            syncBarPosition(); insets
        }

        loadAlbums(); loadPhotos(null)
    }

    private fun initAvatarEditor() {
        // Используем FrameLayout как корень, чтобы накладывать слои
        val editorRoot = FrameLayout(context).apply {
            setBackgroundColor(Color.BLACK)
        }

        // 1. Слой с Кроппером (самый нижний)
        val cropContainer = FrameLayout(context)
        editorImage = com.github.chrisbanes.photoview.PhotoView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        cropContainer.addView(editorImage, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        editorOverlay = CircleOverlayView(context)
        cropContainer.addView(editorOverlay, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        editorRoot.addView(cropContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))

        // 2. ВЕРХНЯЯ ПАНЕЛЬ (Хедер + Миниатюра внутри)
        val headerStack = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(DesignEngine.Theme.colorSheetBackground)
        }

        // Верхняя строка: Кнопка назад и заголовок
        val headerTopRow = RelativeLayout(context)

        // Кнопка назад (исправлено создание RelativeLayout)
        val backBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_back)
            setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            setOnClickListener { showGrid() }
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(12), LayoutHelper.dp(12), LayoutHelper.dp(12))
        }
        headerTopRow.addView(backBtn, LayoutHelper.createRelative(48, 48, RelativeLayout.CENTER_VERTICAL))

        headerTopRow.addView(TextView(context).apply {
            text = "Миниатюра"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }, LayoutHelper.createRelative(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT).apply {
            addRule(RelativeLayout.CENTER_IN_PARENT)
        })

        headerStack.addView(headerTopRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56))

        // Блок ПРЕВЬЮ (внутри хедера)
        val previewRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(LayoutHelper.dp(30), 0, LayoutHelper.dp(30), LayoutHelper.dp(20))
        }

        editorSmallAvatar = ShapeableImageView(context).apply {
            shapeAppearanceModel = shapeAppearanceModel.toBuilder().setAllCornerSizes(LayoutHelper.dp(20).toFloat()).build()
            scaleType = ImageView.ScaleType.MATRIX
            setBackgroundColor(Color.GRAY)
        }
        previewRow.addView(editorSmallAvatar, LayoutHelper.createLinear(40, 40))

        val nameContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(LayoutHelper.dp(12), 0, 0, 0)
        }
        editorName = TextView(context).apply {
            val prefs = context.getSharedPreferences("app", Context.MODE_PRIVATE)
            text = prefs.getString("display_name", "Пользователь")
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        nameContainer.addView(editorName)
        nameContainer.addView(TextView(context).apply {
            text = "пример миниатюры"
            textSize = 12f
            setTextColor(0x80FFFFFF.toInt())
        })
        previewRow.addView(nameContainer)
        headerStack.addView(previewRow)

        // Игнорируем отступы системы для хедера, чтобы он заезжал под статус-бар
        ViewCompat.setOnApplyWindowInsetsListener(headerStack) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(0, status.top, 0, 0)
            insets
        }

        // Градиент ПОД хедером
        val topGradient = View(context).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(DesignEngine.Theme.colorSheetBackground, Color.TRANSPARENT)
            )
        }

        val topFixedContainer = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        topFixedContainer.addView(headerStack)
        topFixedContainer.addView(topGradient, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 80))

        editorRoot.addView(topFixedContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP))

        // 3. НИЖНЯЯ ПАНЕЛЬ (Кнопка + Градиент)
        val bottomFixedContainer = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

        val bottomGradient = View(context).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP,
                intArrayOf(DesignEngine.Theme.colorSheetBackground, Color.TRANSPARENT)
            )
        }

        val bottomBar = FrameLayout(context).apply {
            setBackgroundColor(DesignEngine.Theme.colorSheetBackground)
            setPadding(LayoutHelper.dp(24), LayoutHelper.dp(16), LayoutHelper.dp(24), LayoutHelper.dp(32))
        }

        bottomBar.addView(FrameLayout(context).apply {
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = LayoutHelper.dp(12).toFloat()
            }
            setOnClickListener { cropAndFinish() }
            addView(TextView(context).apply {
                text = "Продолжить"
                setTextColor(Color.BLACK)
                typeface = Typeface.DEFAULT_BOLD
                textSize = 16f
            }, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER))
        }, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 54))

        bottomFixedContainer.addView(bottomGradient, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 80))
        bottomFixedContainer.addView(bottomBar)

        editorRoot.addView(bottomFixedContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM))

        editorContainer.removeAllViews()
        editorContainer.addView(editorRoot, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        updateSystemBars()
        editorImage.attacher.setOnMatrixChangeListener { syncSmallPreview() }
    }



    private fun syncSmallPreview() {
        val drawable = editorImage.drawable ?: return
        val matrix = Matrix()
        editorImage.attacher.getDisplayMatrix(matrix)

        val overlaySize = editorOverlay.getSize()
        val cx = editorOverlay.getCenterX()
        val cy = editorOverlay.getCenterY()

        val smallPreviewSize = LayoutHelper.dp(40).toFloat()
        val scale = smallPreviewSize / overlaySize

        val finalMatrix = Matrix(matrix)
        finalMatrix.postTranslate(-cx, -cy)
        finalMatrix.postScale(scale, scale)
        finalMatrix.postTranslate(smallPreviewSize / 2f, smallPreviewSize / 2f)

        editorSmallAvatar.imageMatrix = finalMatrix
        editorSmallAvatar.invalidate()
    }

    private fun openExternalEditor(uri: Uri) {
        val activity = context as? Activity ?: return
        val destinationUri = Uri.fromFile(File(context.cacheDir, "crop_${System.currentTimeMillis()}.jpg"))
        val options = UCrop.Options().apply {
            setToolbarColor(DesignEngine.Theme.colorBackground)
            setStatusBarColor(DesignEngine.Theme.colorBackground)
            setToolbarWidgetColor(DesignEngine.Theme.colorTextPrimary)
            setActiveControlsWidgetColor(DesignEngine.Theme.colorBlue)
            setRootViewBackgroundColor(DesignEngine.Theme.colorBackground)
            setCropFrameColor(Color.WHITE)
            setToolbarTitle("Редактор")
        }
        UCrop.of(uri, destinationUri).withOptions(options).start(activity)
    }

    private fun cropAndFinish() {
        val drawable = editorImage.drawable as? BitmapDrawable ?: return
        val originalBitmap = drawable.bitmap
        val rect = editorOverlay.getCropRect(editorImage)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val matrix = Matrix(); editorImage.getDisplayMatrix(matrix)
                val inverse = Matrix(); matrix.invert(inverse)
                val mappedRect = RectF(); inverse.mapRect(mappedRect, rect)
                
                val cropped = Bitmap.createBitmap(
                    originalBitmap,
                    mappedRect.left.toInt().coerceAtLeast(0),
                    mappedRect.top.toInt().coerceAtLeast(0),
                    mappedRect.width().toInt().coerceAtMost(originalBitmap.width - mappedRect.left.toInt()),
                    mappedRect.height().toInt().coerceAtMost(originalBitmap.height - mappedRect.top.toInt())
                )
                
                val file = File(context.cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { cropped.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                
                (context as? Activity)?.runOnUiThread {
                    dialog?.dismiss()
                    onResult?.invoke(listOf(Uri.fromFile(file)), null)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun updateSystemBars() {
        val activity = context as? Activity ?: return
        val window = activity.window
        val isEditor = editorContainer.visibility == View.VISIBLE

        // 1. Говорим Activity игнорировать границы системных баров
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (isEditor) {
            val bgColor = DesignEngine.Theme.colorSheetBackground

            // --- ДОБАВЬТЕ ЭТУ СТРОКУ ---
            // Убирает системное затемнение фона (черную пелену) за панелью
            dialog?.window?.setDimAmount(0f)
            // ---------------------------

            // 2. Устанавливаем идентичный фон окна
            window.setBackgroundDrawable(ColorDrawable(bgColor))

            // 3. Делаем бары полностью прозрачными
            window.statusBarColor = bgColor
            window.navigationBarColor = bgColor

            // 4. Отключаем принудительный серый фильтр Android 10+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
                window.isStatusBarContrastEnforced = false
            }

            // 5. Иконки (белые на темном фоне)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.let {
                it.isAppearanceLightStatusBars = false
                it.isAppearanceLightNavigationBars = false
            }
        } else {
            // Возвращаем стандартное затемнение, если вышли из редактора (по желанию)
            dialog?.window?.setDimAmount(0.5f)
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }
    }




    private fun updateUIState() {
        val isEditor = editorContainer.visibility == View.VISIBLE
        val isPreview = previewContainer.visibility == View.VISIBLE
        
        behavior?.isDraggable = !(isEditor || isPreview)
        updateSystemBars()

        if (isEditor) {
            floatingBottomBar.visibility = View.GONE; return
        }
        
        floatingBottomBar.visibility = View.VISIBLE
        val count = adapter.selected.size
        if (isPreview) {
            albumTitle.text = "Предпросмотр"; albumArrow.visibility = View.GONE
            categoriesLayout.visibility = View.GONE; captionLayout.visibility = View.VISIBLE
            toolsLayout.visibility = View.VISIBLE; floatingBottomBar.background = null
        } else if (count > 0) {
            albumTitle.text = "Выбрано $count ${getPhotoWord(count)}"; albumArrow.visibility = View.GONE
            categoriesLayout.visibility = View.GONE; captionLayout.visibility = View.VISIBLE
            toolsLayout.visibility = View.GONE; floatingBottomBar.setBackgroundColor(DesignEngine.Theme.colorSheetBackground)
        } else {
            albumTitle.text = if (mode == Mode.AVATAR) "Выберите фото" else "Все медиа"; albumArrow.visibility = View.VISIBLE
            categoriesLayout.visibility = if (mode == Mode.AVATAR) View.GONE else View.VISIBLE
            captionLayout.visibility = View.GONE; toolsLayout.visibility = View.GONE; floatingBottomBar.setBackgroundColor(DesignEngine.Theme.colorSheetBackground)
        }
        sendBadge.text = count.toString(); sendBadge.visibility = if (count > 0) View.VISIBLE else View.GONE
        syncBarPosition()
    }

    fun syncBarPosition(slideOffset: Float = lastSlideOffset) {
        lastSlideOffset = slideOffset
        val totalHeight = height
        if (totalHeight <= 0) return
        if (previewContainer.visibility == View.VISIBLE || editorContainer.visibility == View.VISIBLE) {
            floatingBottomBar.translationY = -currentKeyboardHeight.toFloat(); return
        }
        val translation = (totalHeight - (behavior?.peekHeight ?: 0)) * (1f - slideOffset)
        floatingBottomBar.translationY = -translation - currentKeyboardHeight
    }

    private fun onSendClicked() {
        val list = if (adapter.selected.isEmpty()) currentPreviewUri?.let { listOf(it) } ?: emptyList() else adapter.selected.toList()
        dialog?.dismiss(); onResult?.invoke(list, captionInput.text.toString())
    }

    private fun showGrid() {
        previewContainer.visibility = View.GONE; editorContainer.visibility = View.GONE; gridContainer.visibility = View.VISIBLE
        behavior?.state = BottomSheetBehavior.STATE_COLLAPSED; updateUIState()
    }

    private fun showPreview(uri: Uri) {
        currentPreviewUri = uri
        if (mode == Mode.AVATAR) {
            editorImage.load(uri) { listener(onSuccess = { _, result -> 
                editorSmallAvatar.setImageDrawable(result.drawable)
                editorImage.post { syncSmallPreview() } 
            }) }
            gridContainer.visibility = View.GONE; editorContainer.visibility = View.VISIBLE
            behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
            previewImage.load(uri); gridContainer.visibility = View.GONE; previewContainer.visibility = View.VISIBLE
            behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        }
        updateUIState()
    }

    fun updatePreviewUri(uri: Uri) {
        currentPreviewUri = uri
        if (mode == Mode.AVATAR) { 
            editorImage.load(uri) { listener(onSuccess = { _, result -> 
                editorSmallAvatar.setImageDrawable(result.drawable)
                editorImage.post { syncSmallPreview() } 
            }) }
        } else previewImage.load(uri)
    }

    private fun togglePreviewSelection() {
        currentPreviewUri?.let { uri -> if (adapter.selected.contains(uri)) adapter.selected.remove(uri) else adapter.selected.add(uri); adapter.notifyDataSetChanged(); updateUIState() }
    }

    private fun getPhotoWord(count: Int) = when { count % 100 in 11..19 -> "фотографий"; count % 10 == 1 -> "фотография"; count % 10 in 2..4 -> "фотографии"; else -> "фотографий" }

    private fun createBottomItem(iconRes: Int, title: String, color: Int) = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(LayoutHelper.dp(12), 0, LayoutHelper.dp(12), 0)
        val circle = FrameLayout(context).apply { background = GradientDrawable().apply { setColor(color); cornerRadius = LayoutHelper.dp(24).toFloat() } }
        circle.addView(ImageView(context).apply { setImageResource(iconRes); setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN) }, LayoutHelper.createFrame(24, 24, Gravity.CENTER))
        addView(circle, LayoutHelper.createLinear(48, 48)); addView(TextView(context).apply { text = title; textSize = 11f; setTextColor(DesignEngine.Theme.colorTextSecondary); setPadding(0, LayoutHelper.dp(4), 0, 0) })
    }

    private fun loadAlbums() {
        albums.clear()
        val cursor = context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.Images.Media.BUCKET_ID, MediaStore.Images.Media._ID), null, null, "${MediaStore.Images.Media.DATE_ADDED} DESC")
        val albumMap = mutableMapOf<String, Album>()
        var allCount = 0; var firstUri: Uri? = null
        cursor?.use {
            val nameCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME); val idCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID); val mediaIdCol = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (it.moveToNext()) {
                val name = it.getString(nameCol) ?: "Unknown"; val id = it.getString(idCol); val mediaId = it.getLong(mediaIdCol); val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaId)
                if (firstUri == null) firstUri = uri; allCount++; val album = albumMap[id]
                if (album == null) albumMap[id] = Album(id, name, uri, 1) else album.count++
            }
        }
        albums.add(Album(null, "Все медиа", firstUri, allCount)); albums.addAll(albumMap.values.sortedByDescending { it.count })
    }

    private fun loadPhotos(bucketId: String?) {
        val photos = mutableListOf<Uri>()
        context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Images.Media._ID), if (bucketId != null) "${MediaStore.Images.Media.BUCKET_ID} = ?" else null, if (bucketId != null) arrayOf(bucketId) else null, "${MediaStore.Images.Media.DATE_ADDED} DESC")?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) photos.add(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getLong(idColumn)))
        }
        adapter.items = photos; adapter.notifyDataSetChanged()
    }

    data class Album (val id: String?, val name: String, val coverUri: Uri?, var count: Int)

    inner class MediaAdapter : RecyclerView.Adapter<MediaAdapter.ViewHolder>() {
        var items = listOf<Uri>(); val selected = mutableListOf<Uri>(); var onSelectionChanged: (() -> Unit)? = null
        inner class ViewHolder(view: View, val image: ShapeableImageView, val checkText: TextView) : RecyclerView.ViewHolder(view)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val frame = FrameLayout(context).apply { layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LayoutHelper.dp(120)); setPadding(1, 1, 1, 1) }
            val iv = ShapeableImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP }; frame.addView(iv, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            val tv = TextView(context).apply { textSize = 12f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); gravity = Gravity.CENTER }; frame.addView(tv, LayoutHelper.createFrame(24, 24, Gravity.TOP or Gravity.END, 0, 6, 6, 0))
            return ViewHolder(frame, iv, tv)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val uri = items[position]; holder.image.load(uri)
            val selIndex = selected.indexOf(uri)
            holder.checkText.apply {
                visibility = if (mode == Mode.AVATAR) View.GONE else View.VISIBLE
                if (selIndex != -1) { text = (selIndex + 1).toString(); background = GradientDrawable().apply { setColor(DesignEngine.Theme.colorBlue); cornerRadius = LayoutHelper.dp(12).toFloat(); setStroke(LayoutHelper.dp(2), Color.WHITE) } } else { text = ""; background = GradientDrawable().apply { setColor(0x33000000); cornerRadius = LayoutHelper.dp(12).toFloat(); setStroke(LayoutHelper.dp(2), Color.WHITE) } }
                setOnClickListener { if (selected.contains(uri)) selected.remove(uri) else selected.add(uri); notifyDataSetChanged(); onSelectionChanged?.invoke() }
            }
            holder.image.setOnClickListener { showPreview(uri) }
        }
        override fun getItemCount() = items.size
    }

    private fun showAlbumPicker() {
        val popup = PopupWindow(context); val list = RecyclerView(context).apply { layoutManager = LinearLayoutManager(context); background = GradientDrawable().apply { setColor(DesignEngine.Theme.colorCardBackground); cornerRadius = LayoutHelper.dp(12).toFloat() }; elevation = LayoutHelper.dp(8).toFloat() }
        list.adapter = object : RecyclerView.Adapter<AlbumViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = AlbumViewHolder(LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(LayoutHelper.dp(16), LayoutHelper.dp(8), LayoutHelper.dp(16), LayoutHelper.dp(8)); layoutParams = ViewGroup.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT); background = context.getDrawable(TypedValue().also { context.theme.resolveAttribute(android.R.attr.selectableItemBackground, it, true) }.resourceId) })
            override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) { val album = albums[position]; holder.name.text = album.name; holder.count.text = album.count.toString(); holder.img.load(album.coverUri); holder.itemView.setOnClickListener { albumTitle.text = album.name; loadPhotos(album.id); popup.dismiss() } }
            override fun getItemCount() = albums.size
        }
        popup.contentView = list; popup.width = LayoutHelper.dp(250); popup.height = LayoutHelper.dp(400); popup.isOutsideTouchable = true; popup.isFocusable = true; popup.showAsDropDown(albumTitle, 0, LayoutHelper.dp(8))
    }

    class AlbumViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val img = ShapeableImageView(view.context).apply { shapeAppearanceModel = shapeAppearanceModel.toBuilder().setAllCornerSizes(LayoutHelper.dp(8).toFloat()).build(); scaleType = ImageView.ScaleType.CENTER_CROP }
        val name = TextView(view.context).apply { textSize = 16f; setTextColor(DesignEngine.Theme.colorTextPrimary); typeface = Typeface.DEFAULT_BOLD }
        val count = TextView(view.context).apply { textSize = 14f; setTextColor(DesignEngine.Theme.colorTextSecondary) }
        init { (view as LinearLayout).addView(img, LayoutHelper.createLinear(48, 48)); val tc = LinearLayout(view.context).apply { orientation = LinearLayout.VERTICAL; setPadding(LayoutHelper.dp(12), 0, 0, 0) }; tc.addView(name); tc.addView(count); view.addView(tc) }
    }

    class CircleOverlayView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCC000000.toInt(); style = Paint.Style.FILL }
        private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = LayoutHelper.dp(2).toFloat() }
        
        fun getCenterX() = width / 2f
        fun getCenterY() = height / 2f
        fun getSize() = (width.coerceAtMost(height) * 0.9f)

        fun getCropRect(view: View): RectF {
            val size = getSize(); val cx = getCenterX(); val cy = getCenterY()
            return RectF(cx - size/2, cy - size/2, cx + size/2, cy + size/2)
        }

        override fun onDraw(canvas: Canvas) {
            val layer = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            val size = getSize(); canvas.drawCircle(width / 2f, height / 2f, size / 2f, clearPaint)
            canvas.drawCircle(width / 2f, height / 2f, size / 2f, borderPaint); canvas.restoreToCount(layer)
        }
    }

    companion object {
        var currentInstance: MediaPickerSheet? = null
        fun show(context: Context, mode: Mode = Mode.CHAT, onResult: (List<Uri>, String?) -> Unit) {
            val dialog = BottomSheetDialog(context, R.style.BottomSheetDialogTheme); val sheet = MediaPickerSheet(context, mode); sheet.onResult = onResult; sheet.dialog = dialog
            dialog.window?.let { window -> 
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = Color.TRANSPARENT
                window.navigationBarColor = DesignEngine.Theme.colorSheetBackground
                
                val isDark = context.getSharedPreferences("app_theme", Context.MODE_PRIVATE).getBoolean("is_dark_theme", false)
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }
            dialog.setContentView(sheet)
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                bottomSheet.setBackgroundColor(Color.TRANSPARENT); val behavior = BottomSheetBehavior.from(bottomSheet); sheet.behavior = behavior; behavior.peekHeight = LayoutHelper.dp(450); behavior.state = BottomSheetBehavior.STATE_COLLAPSED; behavior.isFitToContents = false; behavior.expandedOffset = 0; bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() { override fun onStateChanged(bottomSheet: View, newState: Int) { if (newState == BottomSheetBehavior.STATE_HIDDEN) { dialog.dismiss(); currentInstance = null } }; override fun onSlide(bottomSheet: View, slideOffset: Float) { sheet.syncBarPosition(slideOffset) } })
            }
            dialog.show()
        }
    }
}
