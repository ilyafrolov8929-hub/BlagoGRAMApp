package com.kia.blagogramapp.ui.modules

import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.imageview.ShapeableImageView
import com.kia.blagogramapp.CameraActivity
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
        CHAT, AVATAR, COVER, POST
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
    private lateinit var editorImageContainer: FrameLayout
    private lateinit var editorOverlay: PickerOverlayView
    private var editorName: TextView? = null
    private var editorStatus: TextView? = null
    private var editorSmallAvatar: ShapeableImageView? = null
    private var editorSmallCover: ShapeableImageView? = null
    private var editorSmallAvatarOnCover: ShapeableImageView? = null

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
        albumTitle = TextView(context).apply { text = if (mode == Mode.CHAT || mode == Mode.POST) "Все медиа" else "Выберите фото"; textSize = 18f; typeface = Typeface.DEFAULT_BOLD; setTextColor(DesignEngine.Theme.colorTextPrimary) }
        titleContainer.addView(albumTitle)
        albumArrow = ImageView(context).apply { setImageResource(R.drawable.ic_chevron_right); rotation = 90f; setColorFilter(DesignEngine.Theme.colorTextSecondary, PorterDuff.Mode.SRC_IN) }
        titleContainer.addView(albumArrow, LayoutHelper.createLinear(20, 20, 0f, 0, 4, 0, 0, 0))
        header.addView(titleContainer, LayoutHelper.createRelative(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))

        val closeBtn = ImageView(context).apply { setImageResource(R.drawable.ic_close_small); setColorFilter(DesignEngine.Theme.colorTextSecondary, PorterDuff.Mode.SRC_IN); setOnClickListener { dialog?.dismiss() } }
        header.addView(closeBtn, LayoutHelper.createRelative(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT).apply { addRule(RelativeLayout.ALIGN_PARENT_RIGHT) })
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
        val previewCircle = TextView(context).apply { 
            textSize = 14f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); gravity = Gravity.CENTER; 
            setOnClickListener { togglePreviewSelection() } 
        }
        previewHeader.addView(previewCircle, LayoutHelper.createFrame(28, 28, Gravity.END))
        previewContainer.addView(previewHeader)
        addView(previewContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // 3. EDITOR LAYER
        editorContainer = FrameLayout(context).apply { visibility = View.GONE; setBackgroundColor(Color.BLACK) }
        initEditor()
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
                            if (mode == Mode.CHAT || mode == Mode.POST) openExternalEditor(uri)
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

    private fun initEditor() {
        val editorRoot = FrameLayout(context).apply { setBackgroundColor(Color.BLACK) }
        
        // CROP AREA
        val cropContainer = FrameLayout(context).apply { clipChildren = false }
        editorImageContainer = FrameLayout(context).apply { clipChildren = false }
        editorImage = com.github.chrisbanes.photoview.PhotoView(context).apply { 
            scaleType = ImageView.ScaleType.CENTER_CROP
            maximumScale = 10f
        }
        editorImageContainer.addView(editorImage, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        cropContainer.addView(editorImageContainer, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        
        val overlayType = when(mode) {
            Mode.COVER -> PickerOverlayView.Type.RECT
            Mode.POST -> PickerOverlayView.Type.POST_RECT
            else -> PickerOverlayView.Type.CIRCLE
        }
        editorOverlay = PickerOverlayView(context, overlayType).apply {
            onRectChanged = { rect ->
                editorImageContainer.layoutParams = LayoutParams(rect.width().toInt(), rect.height().toInt(), Gravity.CENTER)
                editorImageContainer.requestLayout()
            }
        }
        cropContainer.addView(editorOverlay, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        
        val touchForwarder = View(context).apply {
            setOnTouchListener { _, event -> editorImage.dispatchTouchEvent(event); true }
        }
        cropContainer.addView(touchForwarder, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        
        editorRoot.addView(cropContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))

        // HEADER & PREVIEW
        val headerStack = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(DesignEngine.Theme.colorSheetBackground) }
        val headerTopRow = RelativeLayout(context)
        val backBtn = ImageView(context).apply { 
            setImageResource(R.drawable.ic_back)
            setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            setOnClickListener { showGrid() }
            val p = LayoutHelper.dp(12)
            setPadding(p, p, p, p)
        }
        headerTopRow.addView(backBtn, LayoutHelper.createRelative(48, 48).apply { addRule(RelativeLayout.CENTER_VERTICAL) })
        val titleText = when(mode) {
            Mode.COVER -> "Обложка"
            Mode.POST -> "Кадрирование"
            else -> "Миниатюра"
        }
        headerTopRow.addView(TextView(context).apply { text = titleText; textSize = 20f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) }, LayoutHelper.createRelative(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT).apply { addRule(RelativeLayout.CENTER_IN_PARENT) })
        headerStack.addView(headerTopRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56))

        val previewRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(LayoutHelper.dp(30), 0, LayoutHelper.dp(30), LayoutHelper.dp(20)) }
        val prefs = context.getSharedPreferences("app", Context.MODE_PRIVATE)

        if (mode == Mode.COVER) {
            val profilePreview = FrameLayout(context).apply {
                background = GradientDrawable().apply { 
                    setColor(DesignEngine.Theme.colorSheetBackground)
                    cornerRadius = LayoutHelper.dp(16).toFloat()
                }
            }
            
            val coverContainer = FrameLayout(context)
            editorSmallCover = ShapeableImageView(context).apply { 
                scaleType = ImageView.ScaleType.MATRIX
                setBackgroundColor(Color.GRAY)
                shapeAppearanceModel = shapeAppearanceModel.toBuilder().setAllCornerSizes(LayoutHelper.dp(16).toFloat()).build()
            }
            coverContainer.addView(editorSmallCover!!, LayoutHelper.createFrame(140, 97))
            
            val cardOverlay = View(context).apply {
                background = GradientDrawable().apply {
                    setColor(DesignEngine.Theme.colorSheetBackground)
                    cornerRadius = LayoutHelper.dp(12).toFloat()
                }
            }
            profilePreview.addView(cardOverlay, LayoutHelper.createFrame(140, 60, Gravity.TOP, 0, 82, 0, 0))
            profilePreview.addView(coverContainer, LayoutHelper.createFrame(140, 97))
            
            editorSmallAvatarOnCover = ShapeableImageView(context).apply {
                shapeAppearanceModel = shapeAppearanceModel.toBuilder().setAllCornerSizes(LayoutHelper.dp(24).toFloat()).build()
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(DesignEngine.Theme.colorSheetBackground) }
                setPadding(LayoutHelper.dp(2), LayoutHelper.dp(2), LayoutHelper.dp(2), LayoutHelper.dp(2))
                load("https://blagogram.ru/avatars/${prefs.getString("avatar", "")}")
            }
            profilePreview.addView(editorSmallAvatarOnCover!!, LayoutHelper.createFrame(48, 48, Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 58, 0, 0))
            
            val infoContainer = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
            editorName = TextView(context).apply { text = prefs.getString("display_name", "Пользователь"); textSize = 11f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
            editorStatus = TextView(context).apply { text = prefs.getString("status", "пример статуса"); textSize = 8f; setTextColor(0x80FFFFFF.toInt()); gravity = Gravity.CENTER }
            infoContainer.addView(editorName)
            infoContainer.addView(editorStatus)
            profilePreview.addView(infoContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 0, 108, 0, 0))

            previewRow.addView(profilePreview, LayoutHelper.createLinear(140, 140))
        } else if (mode == Mode.AVATAR) {
            editorSmallAvatar = ShapeableImageView(context).apply { shapeAppearanceModel = shapeAppearanceModel.toBuilder().setAllCornerSizes(LayoutHelper.dp(20).toFloat()).build(); scaleType = ImageView.ScaleType.MATRIX; setBackgroundColor(Color.GRAY) }
            previewRow.addView(editorSmallAvatar!!, LayoutHelper.createLinear(40, 40))
            val nameContainer = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(LayoutHelper.dp(12), 0, 0, 0) }
            editorName = TextView(context).apply { text = prefs.getString("display_name", "Пользователь"); textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) }
            nameContainer.addView(editorName!!)
            nameContainer.addView(TextView(context).apply { text = "пример миниатюры"; textSize = 12f; setTextColor(0x80FFFFFF.toInt()) })
            previewRow.addView(nameContainer)
        } else {
            // For Mode.POST we don't show special preview UI
            previewRow.visibility = View.GONE
        }
        
        headerStack.addView(previewRow)

        ViewCompat.setOnApplyWindowInsetsListener(headerStack) { v, insets -> v.setPadding(0, insets.getInsets(WindowInsetsCompat.Type.statusBars()).top, 0, 0); insets }
        val topFixedContainer = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        topFixedContainer.addView(headerStack); topFixedContainer.addView(View(context).apply { background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(DesignEngine.Theme.colorSheetBackground, Color.TRANSPARENT)) }, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 80))
        editorRoot.addView(topFixedContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP))

        val bottomFixedContainer = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        bottomFixedContainer.addView(View(context).apply { background = GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, intArrayOf(DesignEngine.Theme.colorSheetBackground, Color.TRANSPARENT)) }, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 80))
        val bottomBar = FrameLayout(context).apply { setBackgroundColor(DesignEngine.Theme.colorSheetBackground); setPadding(LayoutHelper.dp(24), LayoutHelper.dp(16), LayoutHelper.dp(24), LayoutHelper.dp(32)) }
        val actionText = if (mode == Mode.COVER) "Установить обложку" else if (mode == Mode.POST) "Готово" else "Продолжить"
        bottomBar.addView(FrameLayout(context).apply { background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = LayoutHelper.dp(12).toFloat() }; setOnClickListener { cropAndFinish() }; addView(TextView(context).apply { text = actionText; setTextColor(Color.BLACK); typeface = Typeface.DEFAULT_BOLD; textSize = 16f }, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER)) }, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 54))
        bottomFixedContainer.addView(bottomBar)
        editorRoot.addView(bottomFixedContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM))

        editorContainer.removeAllViews(); editorContainer.addView(editorRoot, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        updateSystemBars()
        editorImage.attacher.setOnMatrixChangeListener { syncSmallPreview() }
    }

    private fun syncSmallPreview() {
        val drawable = editorImage.drawable ?: return
        val matrix = Matrix(); editorImage.attacher.getDisplayMatrix(matrix)
        
        if (mode == Mode.COVER && editorSmallCover != null) {
            val smallCoverWidth = LayoutHelper.dp(140).toFloat()
            val scale = smallCoverWidth / editorImageContainer.width
            val finalMatrix = Matrix(matrix).apply { postScale(scale, scale) }
            editorSmallCover?.imageMatrix = finalMatrix
            editorSmallCover?.invalidate()
        } else if (mode == Mode.AVATAR && editorSmallAvatar != null) {
            val smallPreviewSize = LayoutHelper.dp(40).toFloat()
            val scale = smallPreviewSize / editorImageContainer.width
            val finalMatrix = Matrix(matrix).apply { postScale(scale, scale) }
            editorSmallAvatar?.imageMatrix = finalMatrix; editorSmallAvatar?.invalidate()
        }
    }

    private fun openExternalEditor(uri: Uri) {
        val activity = context as? Activity ?: return
        val options = UCrop.Options().apply {
            setToolbarColor(DesignEngine.Theme.colorBackground); setStatusBarColor(DesignEngine.Theme.colorBackground)
            setToolbarWidgetColor(DesignEngine.Theme.colorTextPrimary); setActiveControlsWidgetColor(DesignEngine.Theme.colorBlue)
            setRootViewBackgroundColor(DesignEngine.Theme.colorBackground); setCropFrameColor(Color.WHITE); setToolbarTitle("Редактор")
        }
        UCrop.of(uri, Uri.fromFile(File(context.cacheDir, "crop_${System.currentTimeMillis()}.jpg"))).withOptions(options).start(activity)
    }

    private fun cropAndFinish() {
        val originalBitmap = (editorImage.drawable as? BitmapDrawable)?.bitmap ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val matrix = Matrix(); editorImage.getDisplayMatrix(matrix)
                val inverse = Matrix(); matrix.invert(inverse)
                val mappedRect = RectF(0f, 0f, editorImageContainer.width.toFloat(), editorImageContainer.height.toFloat())
                inverse.mapRect(mappedRect)
                
                val cropped = Bitmap.createBitmap(originalBitmap, mappedRect.left.toInt().coerceAtLeast(0), mappedRect.top.toInt().coerceAtLeast(0), mappedRect.width().toInt().coerceAtMost(originalBitmap.width - mappedRect.left.toInt()), mappedRect.height().toInt().coerceAtMost(originalBitmap.height - mappedRect.top.toInt()))
                val prefix = when(mode) {
                    Mode.COVER -> "cover_"
                    Mode.POST -> "post_"
                    else -> "avatar_"
                }
                val file = File(context.cacheDir, "${prefix}${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { cropped.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                (context as? Activity)?.runOnUiThread { dialog?.dismiss(); onResult?.invoke(listOf(Uri.fromFile(file)), null) }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun updateSystemBars() {
        val activity = context as? Activity ?: return
        val window = activity.window; val isEditor = editorContainer.visibility == View.VISIBLE
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (isEditor) {
            val bgColor = DesignEngine.Theme.colorSheetBackground
            dialog?.window?.setDimAmount(0f)
            window.setBackgroundDrawable(ColorDrawable(bgColor))
            window.statusBarColor = bgColor; window.navigationBarColor = bgColor
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { window.isNavigationBarContrastEnforced = false; window.isStatusBarContrastEnforced = false }
            WindowCompat.getInsetsController(window, window.decorView).let { it.isAppearanceLightStatusBars = false; it.isAppearanceLightNavigationBars = false }
        } else {
            dialog?.window?.setDimAmount(0.5f)
            window.statusBarColor = Color.TRANSPARENT; window.navigationBarColor = Color.TRANSPARENT
        }
    }

    private fun updateUIState() {
        val isEditor = editorContainer.visibility == View.VISIBLE; val isPreview = previewContainer.visibility == View.VISIBLE
        behavior?.isDraggable = !(isEditor || isPreview)
        updateSystemBars()
        if (isEditor) { floatingBottomBar.visibility = View.GONE; return }
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
            albumTitle.text = if (mode == Mode.CHAT || mode == Mode.POST) "Все медиа" else "Выберите фото"; albumArrow.visibility = View.VISIBLE
            categoriesLayout.visibility = if (mode == Mode.AVATAR || mode == Mode.COVER || mode == Mode.POST) View.GONE else View.VISIBLE
            captionLayout.visibility = View.GONE; toolsLayout.visibility = View.GONE; floatingBottomBar.setBackgroundColor(DesignEngine.Theme.colorSheetBackground)
        }
        sendBadge.text = count.toString(); sendBadge.visibility = if (count > 0) View.VISIBLE else View.GONE
        syncBarPosition()
    }

    fun syncBarPosition(slideOffset: Float = lastSlideOffset) {
        lastSlideOffset = slideOffset
        val totalHeight = height; if (totalHeight <= 0) return
        if (previewContainer.visibility == View.VISIBLE || editorContainer.visibility == View.VISIBLE) { floatingBottomBar.translationY = -currentKeyboardHeight.toFloat(); return }
        floatingBottomBar.translationY = -(totalHeight - (behavior?.peekHeight ?: 0)) * (1f - slideOffset) - currentKeyboardHeight
    }

    private fun onSendClicked() { val list = if (adapter.selected.isEmpty()) currentPreviewUri?.let { listOf(it) } ?: emptyList() else adapter.selected.toList(); dialog?.dismiss(); onResult?.invoke(list, captionInput.text.toString()) }
    private fun showGrid() { previewContainer.visibility = View.GONE; editorContainer.visibility = View.GONE; gridContainer.visibility = View.VISIBLE; behavior?.state = BottomSheetBehavior.STATE_COLLAPSED; updateUIState() }
    
    fun showPreview(uri: Uri) {
        currentPreviewUri = uri
        if (mode == Mode.AVATAR || mode == Mode.COVER || mode == Mode.POST) {
            editorImage.load(uri) { listener(onSuccess = { _, result -> 
                if (mode == Mode.COVER) editorSmallCover?.setImageDrawable(result.drawable)
                else if (mode == Mode.AVATAR) editorSmallAvatar?.setImageDrawable(result.drawable)
                editorImage.post { syncSmallPreview() } 
            }) }
            gridContainer.visibility = View.GONE; editorContainer.visibility = View.VISIBLE
        } else {
            previewImage.load(uri); gridContainer.visibility = View.GONE; previewContainer.visibility = View.VISIBLE
        }
        behavior?.state = BottomSheetBehavior.STATE_EXPANDED; updateUIState()
    }

    fun updatePreviewUri(uri: Uri) {
        currentPreviewUri = uri
        if (mode == Mode.AVATAR || mode == Mode.COVER || mode == Mode.POST) { 
            editorImage.load(uri) { listener(onSuccess = { _, result -> 
                if (mode == Mode.COVER) editorSmallCover?.setImageDrawable(result.drawable)
                else if (mode == Mode.AVATAR) editorSmallAvatar?.setImageDrawable(result.drawable)
                editorImage.post { syncSmallPreview() } 
            }) } 
        } 
        else previewImage.load(uri)
    }

    private fun togglePreviewSelection() { currentPreviewUri?.let { uri -> if (adapter.selected.contains(uri)) adapter.selected.remove(uri) else adapter.selected.add(uri); adapter.notifyDataSetChanged(); updateUIState() } }
    private fun getPhotoWord(count: Int) = when { count % 100 in 11..19 -> "фотографий"; count % 10 == 1 -> "фотография"; count % 10 in 2..4 -> "фотографии"; else -> "фотографий" }
    private fun createBottomItem(iconRes: Int, title: String, color: Int) = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(LayoutHelper.dp(12), 0, LayoutHelper.dp(12), 0); val circle = FrameLayout(context).apply { background = GradientDrawable().apply { setColor(color); cornerRadius = LayoutHelper.dp(24).toFloat() } }; circle.addView(ImageView(context).apply { setImageResource(iconRes); setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN) }, LayoutHelper.createFrame(24, 24, Gravity.CENTER)); addView(circle, LayoutHelper.createLinear(48, 48)); addView(TextView(context).apply { text = title; textSize = 11f; setTextColor(DesignEngine.Theme.colorTextSecondary); setPadding(0, LayoutHelper.dp(4), 0, 0) }) }

    private fun loadAlbums() {
        albums.clear(); val cursor = context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.Images.Media.BUCKET_ID, MediaStore.Images.Media._ID), null, null, "${MediaStore.Images.Media.DATE_ADDED} DESC")
        val albumMap = mutableMapOf<String, Album>(); var allCount = 0; var firstUri: Uri? = null
        cursor?.use { val nameCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME); val idCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID); val mediaIdCol = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID); while (it.moveToNext()) { val name = it.getString(nameCol) ?: "Unknown"; val id = it.getString(idCol); val mediaId = it.getLong(mediaIdCol); val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaId); if (firstUri == null) firstUri = uri; allCount++; val album = albumMap[id]; if (album == null) albumMap[id] = Album(id, name, uri, 1) else album.count++ } }
        albums.add(Album(null, "Все медиа", firstUri, allCount)); albums.addAll(albumMap.values.sortedByDescending { it.count })
    }

    private fun loadPhotos(bucketId: String?) {
        val photos = mutableListOf<Uri>(); context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Images.Media._ID), if (bucketId != null) "${MediaStore.Images.Media.BUCKET_ID} = ?" else null, if (bucketId != null) arrayOf(bucketId) else null, "${MediaStore.Images.Media.DATE_ADDED} DESC")?.use { cursor -> val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID); while (cursor.moveToNext()) photos.add(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getLong(idColumn))) }
        adapter.items = photos; adapter.notifyDataSetChanged()
    }

    data class Album (val id: String?, val name: String, val coverUri: Uri?, var count: Int)

    inner class MediaAdapter : RecyclerView.Adapter<MediaAdapter.ViewHolder>() {
        var items = listOf<Uri>(); val selected = mutableListOf<Uri>(); var onSelectionChanged: (() -> Unit)? = null
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val image: ShapeableImageView? = view.findViewWithTag("image")
            val checkText: TextView? = view.findViewWithTag("check")
            val previewView: PreviewView? = view.findViewWithTag("preview")
        }

        override fun getItemViewType(position: Int) = if (position == 0 && (mode == Mode.CHAT || mode == Mode.POST)) 0 else 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val frame = FrameLayout(context).apply { layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LayoutHelper.dp(120)); setPadding(1, 1, 1, 1) }
            if (viewType == 0) {
                val pv = PreviewView(context).apply { tag = "preview"; scaleType = PreviewView.ScaleType.FILL_CENTER }
                frame.addView(pv, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                val icon = ImageView(context).apply { 
                    setImageResource(R.drawable.ic_camera)
                    setColorFilter(Color.WHITE)
                    val p = LayoutHelper.dp(40)
                    setPadding(p, p, p, p)
                }
                frame.addView(icon, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            } else {
                val iv = ShapeableImageView(context).apply { tag = "image"; scaleType = ImageView.ScaleType.CENTER_CROP }
                frame.addView(iv, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                val tv = TextView(context).apply { tag = "check"; textSize = 12f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
                frame.addView(tv, LayoutHelper.createFrame(24, 24, Gravity.TOP or Gravity.END, 0, 6, 6, 0))
            }
            return ViewHolder(frame)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (getItemViewType(position) == 0) {
                holder.itemView.setOnClickListener { (context as? Activity)?.startActivityForResult(Intent(context, CameraActivity::class.java), 1001) }
                startTinyPreview(holder.previewView)
                return
            }
            val uri = items[if (mode == Mode.CHAT || mode == Mode.POST) position - 1 else position]
            holder.image?.load(uri); val selIndex = selected.indexOf(uri)
            holder.checkText?.apply {
                visibility = if (mode != Mode.CHAT) View.GONE else View.VISIBLE
                if (selIndex != -1) { text = (selIndex + 1).toString(); background = GradientDrawable().apply { setColor(DesignEngine.Theme.colorBlue); cornerRadius = LayoutHelper.dp(12).toFloat(); setStroke(LayoutHelper.dp(2), Color.WHITE) } } else { text = ""; background = GradientDrawable().apply { setColor(0x33000000); cornerRadius = LayoutHelper.dp(12).toFloat(); setStroke(LayoutHelper.dp(2), Color.WHITE) } }
                setOnClickListener { if (selected.contains(uri)) selected.remove(uri) else selected.add(uri); notifyDataSetChanged(); onSelectionChanged?.invoke() }
            }
            holder.image?.setOnClickListener { showPreview(uri) }
        }

        private fun startTinyPreview(previewView: PreviewView?) {
            if (previewView == null) return
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(context as LifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                } catch (e: Exception) {}
            }, ContextCompat.getMainExecutor(context))
        }

        override fun getItemCount() = if (mode == Mode.CHAT || mode == Mode.POST) items.size + 1 else items.size
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

    class PickerOverlayView(context: Context, val type: Type = Type.CIRCLE) : View(context) {
        enum class Type { CIRCLE, RECT, POST_RECT }
        
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCC000000.toInt(); style = Paint.Style.FILL }
        private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
            color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = LayoutHelper.dp(2).toFloat()
            if (type == Type.RECT || type == Type.POST_RECT) {
                pathEffect = DashPathEffect(floatArrayOf(LayoutHelper.dp(10).toFloat(), LayoutHelper.dp(5).toFloat()), 0f)
            }
        }
        
        var onRectChanged: ((RectF) -> Unit)? = null
        private var lastRect = RectF()
        
        fun getCenterX() = width / 2f
        fun getCenterY() = height / 2f
        fun getSize() = (width.coerceAtMost(height) * 0.9f)
        
        fun getCropRect(view: View): RectF { 
            val cx = getCenterX()
            val cy = getCenterY()
            val rect = when(type) {
                Type.CIRCLE -> {
                    val size = getSize()
                    RectF(cx - size/2, cy - size/2, cx + size/2, cy + size/2)
                }
                Type.RECT -> {
                    val w = width * 1.0f
                    val h = w * (260f / 375f)
                    RectF(cx - w/2, cy - h/2, cx + w/2, cy + h/2)
                }
                Type.POST_RECT -> {
                    val w = width * 0.9f
                    val h = w * (180f / 320f) // Ratio roughly as seen in designs
                    RectF(cx - w/2, cy - h/2, cx + w/2, cy + h/2)
                }
            }
            if (rect != lastRect) { lastRect = rect; onRectChanged?.invoke(rect) }
            return rect
        }
        
        override fun onDraw(canvas: Canvas) { 
            val layer = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            val rect = getCropRect(this)
            if (type == Type.CIRCLE) {
                canvas.drawCircle(width / 2f, height / 2f, rect.width() / 2f, clearPaint)
                canvas.drawCircle(width / 2f, height / 2f, rect.width() / 2f, borderPaint)
            } else {
                canvas.drawRect(rect, clearPaint)
                canvas.drawRect(rect, borderPaint)
            }
            canvas.restoreToCount(layer) 
        }
    }

    companion object {
        var currentInstance: MediaPickerSheet? = null
        fun show(context: Context, mode: Mode = Mode.CHAT, onResult: (List<Uri>, String?) -> Unit) {
            val dialog = BottomSheetDialog(context, R.style.BottomSheetDialogTheme); val sheet = MediaPickerSheet(context, mode); sheet.onResult = onResult; sheet.dialog = dialog
            dialog.window?.let { window -> WindowCompat.setDecorFitsSystemWindows(window, false); window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS); window.statusBarColor = Color.TRANSPARENT; window.navigationBarColor = DesignEngine.Theme.colorSheetBackground; val isDark = context.getSharedPreferences("app_theme", Context.MODE_PRIVATE).getBoolean("is_dark_theme", false); val controller = WindowCompat.getInsetsController(window, window.decorView); controller.isAppearanceLightStatusBars = !isDark; controller.isAppearanceLightNavigationBars = !isDark }
            dialog.setContentView(sheet)
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) { bottomSheet.setBackgroundColor(Color.TRANSPARENT); val behavior = BottomSheetBehavior.from(bottomSheet); sheet.behavior = behavior; behavior.peekHeight = LayoutHelper.dp(450); behavior.state = BottomSheetBehavior.STATE_COLLAPSED; behavior.isFitToContents = false; behavior.expandedOffset = 0; bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT; behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() { override fun onStateChanged(bottomSheet: View, newState: Int) { if (newState == BottomSheetBehavior.STATE_HIDDEN) { dialog.dismiss(); currentInstance = null } }; override fun onSlide(bottomSheet: View, slideOffset: Float) { sheet.syncBarPosition(slideOffset) } }) }
            dialog.show()
        }
    }
}
