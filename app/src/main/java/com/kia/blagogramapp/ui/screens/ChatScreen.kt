package com.kia.blagogramapp.ui.screens

import android.Manifest
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Context.MODE_PRIVATE
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
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.webkit.MimeTypeMap
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_STOP
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import coil.load
import com.google.android.material.imageview.ShapeableImageView
import com.kia.blagogramapp.BaseScreen
import com.kia.blagogramapp.BlagoApi
import com.kia.blagogramapp.DesignEngine
import com.kia.blagogramapp.LayoutHelper
import com.kia.blagogramapp.ProfilePreviewActivity
import com.kia.blagogramapp.ProfilePreviewScreen
import com.kia.blagogramapp.R
import com.kia.blagogramapp.adapters.MessagesAdapter
import com.kia.blagogramapp.api.ApiClient
import com.kia.blagogramapp.api.ApiRepository
import com.kia.blagogramapp.model.Message
import com.kia.blagogramapp.models.ChatListItem
import com.kia.blagogramapp.ui.ChatViewModel
import com.kia.blagogramapp.ui.modules.ChatListLayout
import com.kia.blagogramapp.ui.modules.EmojiKeyboardModule
import com.kia.blagogramapp.ui.modules.MediaPickerSheet
import com.kia.blagogramapp.ui.modules.MessageOptionsSheet
import com.kia.blagogramapp.utils.TimeUtils
import com.vanniktech.emoji.EmojiEditText
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ChatScreen(context: Context, private val chatId: Int = 0, private var targetUserId: Int = 0, private val chatName: String = "", private val avatarUrl: String = "", private val isPremium: Boolean = false) : BaseScreen(context), SensorEventListener {

    val activity = context as? AppCompatActivity

    private var isCurrentThemeDark: Boolean = false
    private var isCurrentUseMonet: Boolean = false

    private lateinit var viewModel: ChatViewModel
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
    private lateinit var headerContainer: FrameLayout
    private lateinit var headerBlurView: View
    private lateinit var inputContainer: LinearLayout
    private lateinit var emptyText: TextView
    private lateinit var chatNameView: TextView
    private lateinit var chatAvatarView: ShapeableImageView
    private lateinit var premiumBadge: ImageView
    private lateinit var listContainer: FrameLayout
    private lateinit var scrollToBottomBtn: FrameLayout
    private lateinit var wallpaperView: ImageView
    private lateinit var wallpaperBlurView: ImageView

    private lateinit var replyPreview: LinearLayout
    private lateinit var replyPreviewName: TextView
    private lateinit var replyPreviewText: TextView
    private lateinit var replyPreviewClose: ImageView
    private lateinit var replySwipeOverlay: ImageView
    private var replyMessage: Message? = null

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

    override fun createView(): View {
        createUI()

        if (chatId == 0) {
            finishFragment()
        }

        keyboardHeight = context.getSharedPreferences("app_settings", MODE_PRIVATE)
            .getInt("kbd_height", LayoutHelper.dp(300))

        token = context.getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
        myId = context.getSharedPreferences("app", MODE_PRIVATE).getInt("user_id", 0)
        // setupViewModel() // УБРАНО ОТСЮДА (вызывается позже
        chatNameView.text = chatName
        premiumBadge.visibility = if (isPremium) View.VISIBLE else View.GONE

        if (avatarUrl == "action_saved") {
            chatAvatarView.setPadding(0, 0, 0, 0)
            chatAvatarView.scaleType = ImageView.ScaleType.CENTER
            chatAvatarView.setImageResource(R.drawable.ic_saved_messages)
            chatAvatarView.setBackgroundResource(R.drawable.ic_saved_messages)
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
                presentFragment(ProfilePreviewScreen(context, targetUserId))
            }
        }

        setupHeaderSwipe(headerClickListener)

        adapter = MessagesAdapter(
            myId = myId,
            onImageClick = { url, message ->
                openViewer(url, message.display_name ?: "Неизвестный", message.sent_at, message.content)
            },
            onLongClick = { message ->
                showMessageOptions(message)
            },
            onReplyClick = { message ->
                scrollToMessage(message.reply?.id ?: 0)
            }
        )

        setupViewModel()
        setupInsets()
        setupSwipeToReply()

        (recycler.itemAnimator as? DefaultItemAnimator)?.supportsChangeAnimations = false
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
                val hasText = !s.isNullOrBlank() || selectedImages.isNotEmpty() || replyMessage != null
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

        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        activity?.onBackPressedDispatcher?.addCallback(activity!!, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (emojiKeyboard.visibility == View.VISIBLE) {
                    hideEmojiKeyboard()
                } else if (replyMessage != null) {
                    hideReplyPreview()
                } else if (editingMessageId != -1) {
                    cancelEditing()
                } else {
                    isEnabled = false
                    finishFragment()
                }
            }
        })

        rootLayout.post {
            ViewCompat.requestApplyInsets(rootLayout)
        }

        return rootLayout
    }

    private fun setupSwipeToReply() {
        val threshold = LayoutHelper.dp(60).toFloat()
        val maxTranslation = LayoutHelper.dp(100).toFloat()

        var swipingView: View? = null
        var swipingViewHolder: RecyclerView.ViewHolder? = null
        var startX = 0f
        var startY = 0f
        var isSwiping = false
        var replyTriggered = false

        recycler.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                if (rv.scrollState != RecyclerView.SCROLL_STATE_IDLE && !isSwiping) return false

                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = e.x
                        startY = e.y
                        replyTriggered = false
                        val child = rv.findChildViewUnder(e.x, e.y)
                        if (child != null) {
                            val vh = rv.getChildViewHolder(child)
                            if (vh.itemViewType == 1 || vh.itemViewType == 2) {
                                swipingView = child
                                swipingViewHolder = vh
                            }
                        }
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (swipingView != null && !isSwiping) {
                            val dx = startX - e.x
                            val dy = Math.abs(startY - e.y)
                            if (dx > LayoutHelper.dp(8) && dx > dy * 1.5f) {
                                isSwiping = true
                                rv.parent.requestDisallowInterceptTouchEvent(true)
                                return true
                            }
                        }
                    }
                }
                return isSwiping
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                if (!isSwiping || swipingView == null) return

                when (e.action) {
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (startX - e.x).coerceIn(0f, maxTranslation * 1.2f)
                        val translationX = -dx.coerceAtMost(maxTranslation)

                        swipingView?.translationX = translationX

                        replySwipeOverlay.visibility = View.VISIBLE
                        val progress = (dx / threshold).coerceIn(0f, 1f)
                        replySwipeOverlay.alpha = progress

                        val loc = IntArray(2)
                        swipingView?.getLocationInWindow(loc)
                        val rootLoc = IntArray(2)
                        rootLayout.getLocationInWindow(rootLoc)

                        val itemCenterY = loc[1] - rootLoc[1] + (swipingView?.height ?: 0) / 2
                        replySwipeOverlay.translationY = itemCenterY.toFloat() - (rootLayout.height / 2f)

                        if (dx >= threshold && !replyTriggered) {
                            replyTriggered = true
                            rv.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

                            val pos = swipingViewHolder?.bindingAdapterPosition ?: -1
                            if (pos != RecyclerView.NO_POSITION) {
                                val item = adapter.currentList.getOrNull(pos) as? ChatListItem.Msg
                                item?.let { showReplyPreview(it.message) }
                            }
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        swipingView?.animate()
                            ?.translationX(0f)
                            ?.setDuration(200)
                            ?.setInterpolator(DecelerateInterpolator())
                            ?.start()

                        replySwipeOverlay.animate()
                            .alpha(0f)
                            .setDuration(200)
                            .withEndAction { replySwipeOverlay.visibility = View.GONE }
                            .start()

                        isSwiping = false
                        swipingView = null
                        swipingViewHolder = null
                    }
                }
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
    }

    private fun showReplyPreview(message: Message) {
        if (replyPreview.visibility == View.GONE) {
            val transition = android.transition.TransitionSet()
                .addTransition(android.transition.ChangeBounds())
                .addTransition(android.transition.Fade())
                .setDuration(250)
                .setInterpolator(android.view.animation.DecelerateInterpolator())

            android.transition.TransitionManager.beginDelayedTransition(inputContainer, transition)
        }

        replyMessage = message
        editingMessageId = -1
        attachBtn.setImageResource(R.drawable.ic_pin)

        replyPreviewName.text = "В ответ ${message.display_name ?: "пользователю"}"
        replyPreviewText.text = if (message.content.isNotEmpty()) message.content else "Фотография"

        replyPreview.visibility = View.VISIBLE

        messageInput.requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(messageInput, 0)

        sendBtn.setImageResource(R.drawable.ic_send)
    }

    private fun hideReplyPreview() {
        if (replyPreview.visibility == View.VISIBLE) {
            val transition = android.transition.TransitionSet()
                .addTransition(android.transition.ChangeBounds())
                .addTransition(android.transition.Fade())
                .setDuration(200)
                .setInterpolator(android.view.animation.DecelerateInterpolator())

            android.transition.TransitionManager.beginDelayedTransition(inputContainer, transition)
        }

        replyMessage = null
        replyPreview.visibility = View.GONE

        if (messageInput.text.isNullOrBlank() && selectedImages.isEmpty()) {
            sendBtn.setImageResource(R.drawable.ic_mic)
        }
    }

    private fun scrollToMessage(messageId: Int) {
        val position = adapter.currentList.indexOfFirst { it is ChatListItem.Msg && it.message.id == messageId }
        if (position != -1) {
            recycler.smoothScrollToPosition(position)
        }
    }

    private fun showEmojiKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
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
            val resId = context.resources.getIdentifier("chat_bg_default", "drawable", context.packageName)
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
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            openMediaPicker()
        } else {
            activity?.requestPermissions(arrayOf(permission), 101)
        }
    }

    private fun openMediaPicker() {
        MediaPickerSheet.show(context) { uris, caption ->
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
                val app = context.applicationContext as android.app.Application
                return ChatViewModel(app, repo) as T
            }
        }
        activity?.let { act ->
            viewModel = ViewModelProvider(act, factory)[ChatViewModel::class.java]

            viewModel.messages.observe(act) { messages ->
                swipeRefresh.isRefreshing = false
                if (targetUserId == 0) {
                    messages.find { it.sender_id != myId }?.let { targetUserId = it.sender_id }
                }
                emptyText.visibility = if (messages.isEmpty()) View.VISIBLE else View.GONE

                val layoutManager = recycler.layoutManager as LinearLayoutManager
                val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
                val isAtBottom = lastVisiblePosition >= adapter.itemCount - 2

                adapter.update(messages) {
                    if (isFirstLoad) {
                        recycler.scrollToPosition(adapter.itemCount - 1)
                        isFirstLoad = false
                    } else {
                        val lastMsg = messages.lastOrNull()
                        if (isAtBottom || (lastMsg != null && lastMsg.sender_id == myId)) {
                            if (lastMsg?.sender_id == myId) {
                                recycler.scrollToPosition(adapter.itemCount - 1)
                            } else {
                                recycler.smoothScrollToPosition(adapter.itemCount - 1)
                            }
                        }
                    }
                }
            }
            viewModel.error.observe(act) {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { _, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())

            val imeHeight = (ime.bottom - systemBars.bottom).coerceAtLeast(0)
            if (imeHeight > 0) {
                keyboardHeight = imeHeight
                context.getSharedPreferences("app_settings", MODE_PRIVATE)
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
        MessageOptionsSheet.show(context,
            onReply = { showReplyPreview(message) },
            onEdit = { if (message.sender_id == myId) startEditingMessage(message) },
            onDelete = { if (message.sender_id == myId) confirmDeleteMessage(message) }
        )
    }

    private fun startEditingMessage(message: Message) {
        hideReplyPreview()
        editingMessageId = message.id
        messageInput.setText(message.content)
        messageInput.requestFocus()
        sendBtn.setImageResource(R.drawable.ic_edit)
        attachBtn.setImageResource(R.drawable.ic_close_small)
        val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(messageInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    private fun cancelEditing() {
        editingMessageId = -1
        messageInput.text?.clear()
        sendBtn.setImageResource(if (selectedImages.isNotEmpty() || replyMessage != null) R.drawable.ic_send else R.drawable.ic_mic)
        attachBtn.setImageResource(R.drawable.ic_pin)
    }

    private fun confirmDeleteMessage(message: Message) {
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("Удалить сообщение?")
            .setMessage("Это действие нельзя отменить.")
            .setPositiveButton("Удалить") { dialog, which ->
                viewModel.deleteMessage(token, message.id, chatId)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun createUI() {
        val isDark = context.getSharedPreferences("app_theme", MODE_PRIVATE).getBoolean("is_dark_theme", false)
        rootLayout = FrameLayout(context)
        rootLayout.setBackgroundColor(DesignEngine.Theme.colorBackground)

        headerBlurView = View(context)
        rootLayout.addView(headerBlurView)

        wallpaperView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        rootLayout.addView(wallpaperView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))

        wallpaperBlurView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            visibility = View.GONE
        }
        rootLayout.addView(wallpaperBlurView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))

        listContainer = FrameLayout(context)
        swipeRefresh = object : SwipeRefreshLayout(context) {
            override fun canChildScrollUp(): Boolean = if (forceRefresh) false else super.canChildScrollUp()
        }.apply {
            setProgressBackgroundColorSchemeColor(DesignEngine.Theme.colorCardBackground)
            setColorSchemeColors(DesignEngine.Theme.colorBlue)
            setBackgroundColor(Color.TRANSPARENT)
        }
        recycler = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context).apply { stackFromEnd = true }
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_NEVER
            setBackgroundColor(Color.TRANSPARENT)
        }
        swipeRefresh.addView(recycler)
        listContainer.addView(swipeRefresh, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))

        replySwipeOverlay = ImageView(context).apply {
            setImageResource(R.drawable.ic_send)
            setColorFilter(DesignEngine.Theme.colorBlue, PorterDuff.Mode.SRC_IN)
            visibility = View.GONE
            alpha = 0f
        }
        rootLayout.addView(replySwipeOverlay, LayoutHelper.createFrame(24, 24, Gravity.CENTER_VERTICAL or Gravity.END, 0, 0, 16, 0))

        emptyText = TextView(context).apply {
            text = "Здесь пока пусто..."
            textSize = 14f
            setTextColor(DesignEngine.Theme.colorTextSecondary)
            visibility = View.GONE
            gravity = Gravity.CENTER
        }
        listContainer.addView(emptyText, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER))
        rootLayout.addView(listContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))

        val (header, avatar, name, badge) = DesignEngine.createChatHeader(
            context = context,
            onBackClick = { finishFragment() },
            onMenuClick = {
                Toast.makeText(context, "Меню чата", Toast.LENGTH_SHORT).show()
            },
            onHeaderClick = {}
        )

        headerContainer = header
        chatAvatarView = avatar
        chatNameView = name
        premiumBadge = badge

        val headerParams = LayoutHelper.createFrame(
            LayoutHelper.MATCH_PARENT,
            LayoutHelper.WRAP_CONTENT,
            Gravity.TOP or Gravity.FILL_HORIZONTAL,
            left = 8, top = 0, right = 8, bottom = 0
        )
        rootLayout.addView(headerContainer, headerParams)

        val bottomBlurView = View(context).apply {
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

        scrollToBottomBtn = FrameLayout(context).apply {
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

        inputContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, LayoutHelper.dp(8), 0, LayoutHelper.dp(12))
        }

        val inputPillContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                val baseColor = if (isDark) Color.parseColor("#CC262626") else Color.parseColor("#CCFDFDFD")
                setColor(baseColor)
                cornerRadius = LayoutHelper.dp(24).toFloat()
                setStroke(LayoutHelper.dp(1), if (isDark) Color.parseColor("#33FFFFFF") else Color.parseColor("#26000000"))
            }
            elevation = LayoutHelper.dp(6).toFloat()
        }

        replyPreview = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(8), LayoutHelper.dp(12), 0)

            val replyIcon = ImageView(context).apply {
                setImageResource(R.drawable.ic_send)
                setColorFilter(DesignEngine.Theme.colorBlue, PorterDuff.Mode.SRC_IN)
            }
            addView(replyIcon, LayoutHelper.createLinear(18, 18))

            val content = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(LayoutHelper.dp(12), 0, 0, 0)
            }
            replyPreviewName = TextView(context).apply {
                textSize = 13f
                setTextColor(DesignEngine.Theme.colorBlue)
                setTypeface(null, Typeface.BOLD)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            }
            replyPreviewText = TextView(context).apply {
                textSize = 13f
                setTextColor(DesignEngine.Theme.colorTextSecondary)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            }
            content.addView(replyPreviewName)
            content.addView(replyPreviewText)
            addView(content, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f))

            replyPreviewClose = ImageView(context).apply {
                setImageResource(R.drawable.ic_close_small)
                setColorFilter(DesignEngine.Theme.colorIconPrimary, PorterDuff.Mode.SRC_IN)
                setPadding(LayoutHelper.dp(4), LayoutHelper.dp(4), LayoutHelper.dp(4), LayoutHelper.dp(4))
                setOnClickListener { hideReplyPreview() }
            }
            addView(replyPreviewClose, LayoutHelper.createLinear(28, 28))
        }
        inputPillContainer.addView(replyPreview)

        val modernInputRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(LayoutHelper.dp(4), LayoutHelper.dp(2), LayoutHelper.dp(4), LayoutHelper.dp(2))
        }

        emojiBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_emoji)
            setColorFilter(DesignEngine.Theme.colorIconPrimary, PorterDuff.Mode.SRC_IN)
            setPadding(LayoutHelper.dp(10), LayoutHelper.dp(10), LayoutHelper.dp(10), LayoutHelper.dp(10))
            background = getRippleDrawable()
        }
        modernInputRow.addView(emojiBtn, LayoutHelper.createLinear(44, 44))

        messageInput = EmojiEditText(context).apply {
            background = null
            hint = "Сообщение"
            setHintTextColor(DesignEngine.Theme.colorTextSecondary)
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            textSize = 17f
            maxLines = 5
            setPadding(LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8))
        }
        modernInputRow.addView(messageInput, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f))

        attachBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_pin)
            setColorFilter(DesignEngine.Theme.colorIconPrimary, PorterDuff.Mode.SRC_IN)
            setPadding(LayoutHelper.dp(10), LayoutHelper.dp(10), LayoutHelper.dp(10), LayoutHelper.dp(10))
            background = getRippleDrawable()
        }
        modernInputRow.addView(attachBtn, LayoutHelper.createLinear(44, 44))

        sendBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_mic)
            setColorFilter(DesignEngine.Theme.colorBlue, PorterDuff.Mode.SRC_IN)
            setPadding(LayoutHelper.dp(10), LayoutHelper.dp(10), LayoutHelper.dp(10), LayoutHelper.dp(10))
            background = getRippleDrawable()
        }
        modernInputRow.addView(sendBtn, LayoutHelper.createLinear(44, 44))

        inputPillContainer.addView(modernInputRow)
        inputContainer.addView(inputPillContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 8, 0, 8, 0))

        rootLayout.addView(inputContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM))

        emojiKeyboard = EmojiKeyboardModule(context,
            onEmojiClick = { emoji -> messageInput.append(emoji) },
            onBackspaceClick = { messageInput.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)) },
            onKeyboardClick = {
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
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

    override fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
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

    private fun openViewer(url: String, senderName: String, sentTime: String, caption: String?) {
        presentFragment(PhotoViewerScreen(context, url, senderName, sentTime, caption))
    }

    private fun getRippleDrawable(): android.graphics.drawable.Drawable {
        val outValue = android.util.TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
        return getRippleDrawable(outValue.resourceId)
    }

    private fun getRippleDrawable(resId: Int): android.graphics.drawable.Drawable {
        return context.getDrawable(resId)!!
    }

    private fun sendMessage(text: String) {
        val localImageUris = selectedImages.toList().map { it.toString() }

        // Используем getCurrentServerTime из TimeUtils для синхронизации с сервером (МСК)
        val sentAt = TimeUtils.getCurrentServerTime()

        val tempId = -(System.currentTimeMillis() % 1000000).toInt()

        val currentReplyId = replyMessage?.id
        val currentReplyMsg = replyMessage?.let { com.kia.blagogramapp.model.ReplyMessage(it.id, it.sender_id, it.display_name, it.content, it.images) }

        val tempMessage = Message(
            id = tempId,
            chat_id = chatId,
            sender_id = myId,
            content = text,
            sent_at = sentAt,
            is_read = 0,
            display_name = "",
            avatar = "",
            images = localImageUris,
            reply = currentReplyMsg
        )

        viewModel.addTempMessage(tempMessage)
        hideReplyPreview()

        activity?.lifecycleScope?.launch {
            val filesToUpload = withContext(Dispatchers.IO) {
                localImageUris.map { Uri.parse(it) }.mapNotNull { uri -> uriToCompressedFile(uri) }
            }
            viewModel.sendMessage(token, chatId, text, filesToUpload, currentReplyId)
        }
        selectedImages.clear()
    }

    private fun uriToCompressedFile(uri: Uri): File? {
        return try {
            val cr = context.contentResolver
            val mimeType = cr.getType(uri)
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
            val originalFile = File(context.cacheDir, "original_${System.currentTimeMillis()}.$extension")
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
            val finalFile = File(context.cacheDir, "img_${System.currentTimeMillis()}.jpg")
            FileOutputStream(finalFile).use { resized.compress(Bitmap.CompressFormat.JPEG, 80, it) }
            finalFile
        } catch (e: Exception) { null }
    }
}
