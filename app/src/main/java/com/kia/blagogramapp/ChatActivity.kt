package com.kia.blagogramapp

import android.Manifest
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import coil.load
import com.google.android.material.imageview.ShapeableImageView
import com.kia.blagogramapp.adapters.AttachmentsAdapter
import com.kia.blagogramapp.adapters.MessagesAdapter
import com.kia.blagogramapp.api.ApiClient
import com.kia.blagogramapp.api.ApiRepository
import com.kia.blagogramapp.model.Message
import com.kia.blagogramapp.ui.ChatViewModel
import com.kia.blagogramapp.ui.modules.MessageOptionsSheet
import com.kia.blagogramapp.ui.modules.MediaPickerSheet
import com.kia.blagogramapp.ui.modules.EmojiKeyboardModule
import com.vanniktech.emoji.EmojiEditText
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var viewModel: ChatViewModel
    private var chatId: Int = 0
    private var targetUserId: Int = 0
    private var token: String = ""
    private var myId: Int = 0

    private lateinit var rootLayout: FrameLayout
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: MessagesAdapter
    private lateinit var sendBtn: ImageView
    private lateinit var attachBtn: ImageView
    private lateinit var emojiBtn: ImageView
    private lateinit var messageInput: EmojiEditText
    private lateinit var emojiKeyboard: EmojiKeyboardModule
    private lateinit var attachmentsRecycler: RecyclerView
    private lateinit var headerContainer: FrameLayout
    private lateinit var headerBlurView: View
    private lateinit var inputContainer: LinearLayout
    private lateinit var emptyText: TextView
    private lateinit var chatNameView: TextView
    private lateinit var chatAvatarView: ShapeableImageView
    private lateinit var listContainer: FrameLayout
    private lateinit var scrollToBottomBtn: FrameLayout
    private lateinit var wallpaperView: ImageView
    private lateinit var wallpaperBlurView: ImageView

    private val selectedImages = mutableListOf<Uri>()
    private var isFirstLoad = true
    private var editingMessageId: Int = -1

    private var forceRefresh = false
    private var keyboardHeight = 0

    private lateinit var sensorManager: SensorManager
    private var gyroscope: Sensor? = null
    private var ts: Long = 0
    private var targetTranslationX = 0f
    private var targetTranslationY = 0f
    private var parallaxAnimator: ValueAnimator? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openMediaPicker()
        } else {
            Toast.makeText(this, "Нужно разрешение для доступа к галерее", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = DesignEngine.initializeScreenWithFrameLayout(this)
        setContentView(root)

        createUI()
        
        chatId = intent.getIntExtra("chat_id", 0)
        targetUserId = intent.getIntExtra("user_id", 0)

        if (chatId == 0) {
            finish()
            return
        }

        keyboardHeight = getSharedPreferences("app_settings", MODE_PRIVATE)
            .getInt("kbd_height", LayoutHelper.dp(300))

        token = getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
        myId = getSharedPreferences("app", MODE_PRIVATE).getInt("user_id", 0)

        setupViewModel()
        val chatName = intent.getStringExtra("chat_name") ?: "Чат"
        val avatarUrl = intent.getStringExtra("chat_avatar")
        chatNameView.text = chatName
        if (avatarUrl == "action_saved") {
            chatAvatarView.setPadding(0, 0, 0, 0)
            chatAvatarView.scaleType = ImageView.ScaleType.CENTER
            chatAvatarView.setImageResource(R.drawable.ic_saved_messages)
            chatAvatarView.setBackgroundResource(R.drawable.bg_saved_messages_circle)
        } else {
            chatAvatarView.scaleType = ImageView.ScaleType.CENTER_CROP
            chatAvatarView.setPadding(0, 0, 0, 0)
            chatAvatarView.setBackgroundResource(0)
            chatAvatarView.load("https://blagogram.ru/avatars/$avatarUrl") {
                crossfade(true)
                placeholder(R.drawable.ic_user_placeholder)
                error(R.drawable.ic_user_placeholder)
            }
        }

        val headerClickListener = View.OnClickListener {
            if (targetUserId != 0) {
                val i = Intent(this, ProfileActivity::class.java)
                i.putExtra("user_id", targetUserId)
                startActivity(i)
            }
        }

        setupHeaderSwipe(headerClickListener)

        adapter = MessagesAdapter(
            myId = myId,
            onImageClick = { url, message ->
                openViewer(url, message.display_name ?: "Неизвестный", message.sent_at, message.content)
            },
            onLongClick = { message ->
                if (message.sender_id == myId) showMessageOptions(message)
            }
        )
        recycler.adapter = adapter

        attachBtn.setOnClickListener {
            if (editingMessageId != -1) cancelEditing() else checkPermissionAndOpenPicker()
        }

        sendBtn.setOnClickListener {
            val text = messageInput.text.toString().trim()
            if (editingMessageId != -1) {
                if (text.isNotBlank()) {
                    viewModel.editMessage(token, editingMessageId, text, chatId)
                    cancelEditing()
                }
            } else {
                if (text.isNotBlank() || selectedImages.isNotEmpty()) {
                    sendMessage(text)
                    messageInput.text?.clear()
                }
            }
        }

        messageInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrBlank() || selectedImages.isNotEmpty()
                if (editingMessageId == -1) {
                    sendBtn.setImageResource(if (hasText) R.drawable.ic_send else R.drawable.ic_mic)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val canScrollDown = recyclerView.canScrollVertically(1)
                scrollToBottomBtn.visibility = if (canScrollDown) View.VISIBLE else View.GONE
            }
        })

        swipeRefresh.setOnRefreshListener { viewModel.loadMessages(token, chatId) }
        viewModel.loadMessages(token, chatId)
        setupInsets()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (emojiKeyboard.visibility == View.VISIBLE) {
                    hideEmojiKeyboard()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun showEmojiKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(messageInput.windowToken, 0)
        
        emojiKeyboard.setHeight(keyboardHeight)
        emojiKeyboard.visibility = View.VISIBLE
        emojiBtn.setImageResource(R.drawable.ic_keyboard)
        
        inputContainer.translationY = -keyboardHeight.toFloat()
        listContainer.translationY = -keyboardHeight.toFloat()
        scrollToBottomBtn.translationY = -keyboardHeight.toFloat()
    }

    private fun hideEmojiKeyboard() {
        emojiKeyboard.visibility = View.GONE
        emojiBtn.setImageResource(R.drawable.ic_emoji)
        
        inputContainer.translationY = 0f
        listContainer.translationY = 0f
        scrollToBottomBtn.translationY = 0f
    }

    override fun onResume() {
        super.onResume()
        updateWallpaper()
        if (DesignEngine.wallpaperMotion) {
            registerSensor()
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterSensor()
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

    private fun updateWallpaper() {
        val wallpaper = DesignEngine.chatWallpaper
        val uri = if (wallpaper != null && wallpaper.startsWith("/")) {
            Uri.fromFile(File(wallpaper))
        } else if (wallpaper != null) {
            Uri.parse(wallpaper)
        } else {
            null
        }

        if (uri != null) {
            wallpaperView.setImageURI(uri)
            wallpaperBlurView.setImageURI(uri)
        } else {
            val resId = resources.getIdentifier("chat_bg_default", "drawable", packageName)
            if (resId != 0) {
                wallpaperView.setImageResource(resId)
                wallpaperBlurView.setImageResource(resId)
            } else {
                wallpaperView.setBackgroundColor(if (DesignEngine.Theme.colorBackground == Color.BLACK) Color.BLACK else 0xFFE5E5E5.toInt())
                wallpaperBlurView.setBackgroundColor(if (DesignEngine.Theme.colorBackground == Color.BLACK) Color.BLACK else 0xFFE5E5E5.toInt())
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (DesignEngine.wallpaperBlur) {
                wallpaperBlurView.setRenderEffect(RenderEffect.createBlurEffect(50f, 50f, Shader.TileMode.MIRROR))
                wallpaperBlurView.visibility = View.VISIBLE
            } else {
                wallpaperBlurView.visibility = View.GONE
            }
        }
    }

    private fun checkPermissionAndOpenPicker() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openMediaPicker()
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    private fun openMediaPicker() {
        MediaPickerSheet.show(this) { uris, caption ->
            if (uris.isNotEmpty()) {
                selectedImages.clear()
                selectedImages.addAll(uris)
                sendMessage(caption ?: "")
                messageInput.text?.clear()
            }
        }
    }

    private fun setupViewModel() {
        val repo = ApiRepository(ApiClient.retrofit.create(BlagoApi::class.java))
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(repo) as T
            }
        }
        viewModel = ViewModelProvider(this, factory)[ChatViewModel::class.java]

        viewModel.messages.observe(this) { messages ->
            swipeRefresh.isRefreshing = false
            if (targetUserId == 0) {
                messages.find { it.sender_id != myId }?.let { targetUserId = it.sender_id }
            }
            emptyText.visibility = if (messages.isEmpty()) View.VISIBLE else View.GONE

            val isAtBottom = !recycler.canScrollVertically(1) || (adapter.itemCount > 0 && recycler.computeVerticalScrollOffset() > 0)
            adapter.update(messages) {
                if (isFirstLoad) {
                    recycler.scrollToPosition(adapter.itemCount - 1)
                    isFirstLoad = false
                } else {
                    val lastMsg = messages.lastOrNull()
                    if (isAtBottom || (lastMsg != null && lastMsg.sender_id == myId)) {
                        recycler.smoothScrollToPosition(adapter.itemCount - 1)
                    }
                }
            }
        }
        viewModel.error.observe(this) { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { _, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            
            val imeHeight = (ime.bottom - systemBars.bottom).coerceAtLeast(0)
            if (imeHeight > 0) {
                keyboardHeight = imeHeight
                getSharedPreferences("app_settings", MODE_PRIVATE)
                    .edit().putInt("kbd_height", keyboardHeight).apply()
            }

            headerContainer.setPadding(0, systemBars.top, 0, 0)
            inputContainer.setPadding(0, 0, 0, systemBars.bottom)
            inputContainer.post {
                val h = headerContainer.height
                val b = inputContainer.height + LayoutHelper.dp(8)
                if (recycler.paddingTop != h || recycler.paddingBottom != b) {
                    recycler.setPadding(0, h, 0, b)
                    swipeRefresh.setProgressViewOffset(false, systemBars.top, h + LayoutHelper.dp(16))
                }
            }
            windowInsets
        }

        ViewCompat.setWindowInsetsAnimationCallback(
            rootLayout,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                override fun onProgress(insets: WindowInsetsCompat, runningAnimations: MutableList<WindowInsetsAnimationCompat>): WindowInsetsCompat {
                    val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
                    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    val imeHeight = (ime.bottom - systemBars.bottom).coerceAtLeast(0)
                    
                    val emojiOffset = if (emojiKeyboard.visibility == View.VISIBLE) emojiKeyboard.height else 0
                    val finalOffset = maxOf(imeHeight, emojiOffset)

                    inputContainer.translationY = -finalOffset.toFloat()
                    listContainer.translationY = -finalOffset.toFloat()
                    scrollToBottomBtn.translationY = -finalOffset.toFloat()
                    return insets
                }

                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    val insets = ViewCompat.getRootWindowInsets(rootLayout)
                    val ime = insets?.getInsets(WindowInsetsCompat.Type.ime())
                    val systemBars = insets?.getInsets(WindowInsetsCompat.Type.systemBars())
                    val imeHeight = if (ime != null && systemBars != null) (ime.bottom - systemBars.bottom).coerceAtLeast(0) else 0
                    
                    if (imeHeight > 0 && emojiKeyboard.visibility == View.VISIBLE && !emojiKeyboard.isSearching) {
                        emojiKeyboard.visibility = View.GONE
                        emojiBtn.setImageResource(R.drawable.ic_emoji)
                    }
                }
            }
        )
    }

    private fun setupHeaderSwipe(headerClickListener: View.OnClickListener) {
        var startY = 0f
        var isProxying = false
        val touchListener = View.OnTouchListener { v, event ->
            val offsetEvent = MotionEvent.obtain(event)
            val loc = IntArray(2)
            swipeRefresh.getLocationOnScreen(loc)
            val vLoc = IntArray(2)
            v.getLocationOnScreen(vLoc)
            offsetEvent.offsetLocation((vLoc[0] - loc[0]).toFloat(), (vLoc[1] - loc[1]).toFloat())
            val result = when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.rawY
                    isProxying = false
                    forceRefresh = true
                    swipeRefresh.dispatchTouchEvent(offsetEvent)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!isProxying && event.rawY - startY > 10) isProxying = true
                    if (isProxying) swipeRefresh.dispatchTouchEvent(offsetEvent)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    swipeRefresh.dispatchTouchEvent(offsetEvent)
                    forceRefresh = false
                    if (!isProxying && event.action == MotionEvent.ACTION_UP) headerClickListener.onClick(v)
                    isProxying = false
                    true
                }
                else -> false
            }
            offsetEvent.recycle()
            result
        }
        headerContainer.setOnTouchListener(touchListener)
        chatAvatarView.setOnTouchListener(touchListener)
    }

    private fun showMessageOptions(message: Message) {
        MessageOptionsSheet.show(this,
            onEdit = { startEditingMessage(message) },
            onDelete = { confirmDeleteMessage(message) }
        )
    }

    private fun startEditingMessage(message: Message) {
        editingMessageId = message.id
        messageInput.setText(message.content)
        messageInput.requestFocus()
        sendBtn.setImageResource(R.drawable.ic_edit)
        attachBtn.setImageResource(R.drawable.ic_close_small)
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(messageInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    private fun cancelEditing() {
        editingMessageId = -1
        messageInput.text?.clear()
        sendBtn.setImageResource(if (selectedImages.isNotEmpty()) R.drawable.ic_send else R.drawable.ic_mic)
        attachBtn.setImageResource(R.drawable.ic_pin)
    }

    private fun confirmDeleteMessage(message: Message) {
        AlertDialog.Builder(this)
            .setTitle("Удалить сообщение?")
            .setMessage("Это действие нельзя отменить.")
            .setPositiveButton("Удалить") { _, _ -> viewModel.deleteMessage(token, message.id, chatId) }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun createUI() {
        val isDark = getSharedPreferences("app_theme", MODE_PRIVATE).getBoolean("is_dark_theme", false)

        rootLayout = FrameLayout(this)
        rootLayout.setBackgroundColor(DesignEngine.Theme.colorBackground)
        setContentView(rootLayout)

        headerBlurView = View(this)
        rootLayout.addView(headerBlurView)

        wallpaperView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        rootLayout.addView(wallpaperView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))

        wallpaperBlurView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            visibility = View.GONE
        }
        rootLayout.addView(wallpaperBlurView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))

        listContainer = FrameLayout(this)
        swipeRefresh = object : SwipeRefreshLayout(this) {
            override fun canChildScrollUp(): Boolean = if (forceRefresh) false else super.canChildScrollUp()
        }.apply {
            setProgressBackgroundColorSchemeColor(DesignEngine.Theme.colorCardBackground)
            setColorSchemeColors(DesignEngine.Theme.colorBlue)
            setBackgroundColor(Color.TRANSPARENT)
        }
        recycler = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply { stackFromEnd = true }
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_NEVER
            setBackgroundColor(Color.TRANSPARENT)
        }
        swipeRefresh.addView(recycler)
        listContainer.addView(swipeRefresh, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))
        emptyText = TextView(this).apply {
            text = "Здесь пока пусто..."
            textSize = 14f
            setTextColor(DesignEngine.Theme.colorTextSecondary)
            visibility = View.GONE
            gravity = Gravity.CENTER
        }
        listContainer.addView(emptyText, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER))
        rootLayout.addView(listContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))

        val (header, avatar, name) = DesignEngine.createChatHeader(
            context = this,
            onBackClick = { finish() },
            onMenuClick = {
                Toast.makeText(this, "Меню чата", Toast.LENGTH_SHORT).show()
            },
            onHeaderClick = {
                if (targetUserId != 0) {
                    val i = Intent(this, ProfileActivity::class.java)
                    i.putExtra("user_id", targetUserId)
                    startActivity(i)
                }
            }
        )

        headerContainer = header
        chatAvatarView = avatar
        chatNameView = name

        val headerParams = LayoutHelper.createFrame(
            LayoutHelper.MATCH_PARENT,
            LayoutHelper.WRAP_CONTENT,
            Gravity.TOP or Gravity.FILL_HORIZONTAL,
            left = 8, top = 0, right = 8, bottom = 0
        )
        rootLayout.addView(headerContainer, headerParams)

        val bottomBlurView = View(this).apply {
            val footerBg = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                if (isDark)
                    intArrayOf(Color.TRANSPARENT, Color.parseColor("#B31A1A1A"), Color.parseColor("#FF1A1A1A"))
                else
                    intArrayOf(Color.TRANSPARENT, Color.parseColor("#B3F2F2F7"), Color.parseColor("#FFF2F2F7"))
            )
            background = footerBg
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRenderEffect(android.graphics.RenderEffect.createBlurEffect(25f, 25f, android.graphics.Shader.TileMode.CLAMP))
            }
        }
        rootLayout.addView(bottomBlurView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 0, Gravity.BOTTOM))

        scrollToBottomBtn = FrameLayout(this).apply {
            visibility = View.GONE
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                val btnColor = if (isDark) Color.parseColor("#CC2C2C2E") else Color.parseColor("#CCFFFFFF")
                setColor(btnColor)
                if (!isDark) setStroke(LayoutHelper.dp(1), Color.parseColor("#1A000000"))
            }
            elevation = LayoutHelper.dp(4).toFloat()
            val icon = ImageView(context).apply {
                setImageResource(R.drawable.ic_next)
                rotation = 90f
                setColorFilter(DesignEngine.Theme.colorIconPrimary, PorterDuff.Mode.SRC_IN)
                setPadding(LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8))
            }
            addView(icon, LayoutHelper.createFrame(36, 36))
            setOnClickListener { recycler.smoothScrollToPosition(adapter.itemCount - 1) }
        }
        rootLayout.addView(scrollToBottomBtn, LayoutHelper.createFrame(36, 36, Gravity.BOTTOM or Gravity.END, 0, 0, 16, 80))

        inputContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, LayoutHelper.dp(8), 0, LayoutHelper.dp(12))
        }

        inputContainer.addOnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ ->
            val height = bottom - top
            val params = bottomBlurView.layoutParams
            if (params.height != height) {
                params.height = height
                bottomBlurView.layoutParams = params
            }
        }

        attachmentsRecycler = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@ChatActivity, LinearLayoutManager.HORIZONTAL, false)
            visibility = View.GONE
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(8), LayoutHelper.dp(16), LayoutHelper.dp(8))
        }
        inputContainer.addView(attachmentsRecycler, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        val modernInputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                val baseColor = if (isDark) Color.parseColor("#CC262626") else Color.parseColor("#CCFDFDFD")
                setColor(baseColor)
                cornerRadius = LayoutHelper.dp(28).toFloat()
                setStroke(LayoutHelper.dp(1), if (isDark) Color.parseColor("#33FFFFFF") else Color.parseColor("#26000000"))
            }
            elevation = LayoutHelper.dp(6).toFloat()
            setPadding(LayoutHelper.dp(4), LayoutHelper.dp(4), LayoutHelper.dp(4), LayoutHelper.dp(4))
        }

        emojiBtn = ImageView(this).apply {
            setImageResource(R.drawable.ic_emoji)
            setColorFilter(DesignEngine.Theme.colorIconPrimary, PorterDuff.Mode.SRC_IN)
            setPadding(LayoutHelper.dp(10), LayoutHelper.dp(10), LayoutHelper.dp(10), LayoutHelper.dp(10))
            background = getRippleDrawable()
            setOnClickListener {
                if (emojiKeyboard.visibility == View.VISIBLE) {
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.showSoftInput(messageInput, 0)
                } else {
                    showEmojiKeyboard()
                }
            }
        }
        modernInputRow.addView(emojiBtn, LayoutHelper.createLinear(44, 44))

        messageInput = EmojiEditText(this).apply {
            background = null
            hint = "Сообщение"
            setHintTextColor(DesignEngine.Theme.colorTextSecondary)
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            textSize = 17f
            maxLines = 5
            setPadding(LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8))
        }
        modernInputRow.addView(messageInput, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f))

        attachBtn = ImageView(this).apply {
            setImageResource(R.drawable.ic_pin)
            setColorFilter(DesignEngine.Theme.colorIconPrimary, PorterDuff.Mode.SRC_IN)
            setPadding(LayoutHelper.dp(10), LayoutHelper.dp(10), LayoutHelper.dp(10), LayoutHelper.dp(10))
            background = getRippleDrawable()
        }
        modernInputRow.addView(attachBtn, LayoutHelper.createLinear(44, 44))

        sendBtn = ImageView(this).apply {
            setImageResource(R.drawable.ic_mic)
            setColorFilter(DesignEngine.Theme.colorBlue, PorterDuff.Mode.SRC_IN)
            setPadding(LayoutHelper.dp(10), LayoutHelper.dp(10), LayoutHelper.dp(10), LayoutHelper.dp(10))
            background = getRippleDrawable()
        }
        modernInputRow.addView(sendBtn, LayoutHelper.createLinear(44, 44))

        inputContainer.addView(modernInputRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 16, 0, 16, 0))
        rootLayout.addView(inputContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM))
        
        emojiKeyboard = EmojiKeyboardModule(this, 
            onEmojiClick = { emoji -> messageInput.append(emoji) },
            onBackspaceClick = { messageInput.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)) },
            onKeyboardClick = {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(messageInput, 0)
            },
            onHeightChanged = { height ->
                inputContainer.translationY = -height.toFloat()
                listContainer.translationY = -height.toFloat()
                scrollToBottomBtn.translationY = -height.toFloat()
            }
        )
        rootLayout.addView(emojiKeyboard, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM))
    }

    private fun openViewer(url: String, senderName: String, sentTime: String, caption: String?) {
        val intent = Intent(this, PhotoViewerActivity::class.java).apply {
            putExtra("image_url", url)
            putExtra("sender_name", senderName)
            putExtra("sent_time", sentTime)
            putExtra("caption", caption)
        }
        startActivity(intent)
    }

    private fun getRippleDrawable(): android.graphics.drawable.Drawable {
        val outValue = android.util.TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
        return getDrawable(outValue.resourceId)!!
    }

    private fun sendMessage(text: String) {
        val localImageUris = selectedImages.toList().map { it.toString() }
        val sentAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val tempId = -(System.currentTimeMillis() % 1000000).toInt()
        val tempMessage = Message(
            id = tempId,
            chat_id = chatId,
            sender_id = myId,
            content = text,
            sent_at = sentAt,
            is_read = 0,
            display_name = "",
            avatar = "",
            images = localImageUris
        )
        viewModel.addTempMessage(tempMessage)
        lifecycleScope.launch {
            val filesToUpload = withContext(Dispatchers.IO) {
                localImageUris.map { Uri.parse(it) }.mapNotNull { uri -> uriToCompressedFile(uri) }
            }
            viewModel.sendMessage(token, chatId, text, filesToUpload)
        }
        selectedImages.clear()
        attachmentsRecycler.visibility = View.GONE
    }

    private fun uriToCompressedFile(uri: Uri): File? {
        return try {
            val cr = contentResolver
            val mimeType = cr.getType(uri)
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
            val originalFile = File(cacheDir, "original_${System.currentTimeMillis()}.$extension")
            cr.openInputStream(uri)?.use { input -> FileOutputStream(originalFile).use { output -> input.copyTo(output) } }
            val exif = androidx.exifinterface.media.ExifInterface(originalFile.absolutePath)
            val orientation = exif.getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL)
            val bitmap = BitmapFactory.decodeFile(originalFile.absolutePath)
            val matrix = android.graphics.Matrix()
            when (orientation) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            val maxSize = 1280
            val ratio = rotated.width.toFloat() / rotated.height.toFloat()
            val (newWidth, newHeight) = if (ratio > 1) maxSize to (maxSize / ratio).toInt() else (maxSize * ratio).toInt() to maxSize
            val resized = Bitmap.createScaledBitmap(rotated, newWidth, newHeight, true)
            val finalFile = File(cacheDir, "img_${System.currentTimeMillis()}.jpg")
            FileOutputStream(finalFile).use { resized.compress(Bitmap.CompressFormat.JPEG, 80, it) }
            finalFile
        } catch (e: Exception) { null }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                UCrop.REQUEST_CROP -> {
                    UCrop.getOutput(data!!)?.let { MediaPickerSheet.currentInstance?.updatePreviewUri(it) }
                }
                1001 -> {
                    data?.data?.let { uri -> MediaPickerSheet.currentInstance?.showPreview(uri) }
                }
            }
        }
    }
}
