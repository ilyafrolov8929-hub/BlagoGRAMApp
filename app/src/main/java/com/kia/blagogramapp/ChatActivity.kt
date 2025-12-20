package com.kia.blagogramapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import coil.load
import com.google.android.material.imageview.ShapeableImageView
import com.kia.blagogramapp.adapters.AttachmentsAdapter
import com.kia.blagogramapp.adapters.MessagesAdapter
import com.kia.blagogramapp.api.ApiClient
import com.kia.blagogramapp.api.ApiRepository
import com.kia.blagogramapp.BlagoApi
import com.kia.blagogramapp.model.Message
import com.kia.blagogramapp.ui.modules.MessageOptionsSheet
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity() {

    private lateinit var repo: ApiRepository
    private var chatId: Int = 0
    private var targetUserId: Int = 0 

    // UI Elements
    private lateinit var rootLayout: FrameLayout
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: MessagesAdapter
    private lateinit var sendBtn: ImageView
    private lateinit var attachBtn: ImageView
    private lateinit var messageInput: EditText
    private lateinit var attachmentsRecycler: RecyclerView
    private lateinit var attachmentsAdapter: AttachmentsAdapter
    private lateinit var headerContainer: LinearLayout
    private lateinit var emptyText: TextView
    private lateinit var chatNameView: TextView
    private lateinit var chatAvatarView: ShapeableImageView

    private val selectedImages = mutableListOf<Uri>()
    private var isFirstLoad = true
    private var editingPosition = -1
    private var editingMessageId: Int = -1
    
    // Флаг для принудительного разрешения обновления при свайпе хедера
    private var forceRefresh = false

    // Launchers
    private val pickImagesLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            selectedImages.addAll(uris)
            updateAttachmentsPreview()
        }
    }

    private val cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let { uri ->
                if (editingPosition >= 0 && editingPosition < selectedImages.size) {
                    selectedImages[editingPosition] = uri
                    updateAttachmentsPreview()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DesignEngine.init(this)
        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        val isDarkTheme = themePrefs.getBoolean("is_dark_theme", false)
        val useMonet = themePrefs.getBoolean("use_monet", false)
        DesignEngine.Theme.apply(isDarkTheme, useMonet)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = !isDarkTheme
        controller.isAppearanceLightNavigationBars = !isDarkTheme

        createUI()

        chatId = intent.getIntExtra("chat_id", 0)
        if (chatId == 0) {
            finish()
            return
        }

        val chatName = intent.getStringExtra("chat_name") ?: "Чат"
        val avatarUrl = intent.getStringExtra("chat_avatar")

        chatNameView.text = chatName
        chatAvatarView.load("https://blagogram.ru/avatars/$avatarUrl") {
            crossfade(true)
            placeholder(R.drawable.ic_user_placeholder)
            error(R.drawable.ic_user_placeholder)
        }

        val headerClickListener = View.OnClickListener {
            if (targetUserId != 0) {
                val i = Intent(this, ProfileActivity::class.java)
                i.putExtra("user_id", targetUserId)
                startActivity(i)
            } else {
                Toast.makeText(this, "Пользователь не найден", Toast.LENGTH_SHORT).show()
            }
        }
        
        setupHeaderSwipe(headerClickListener)

        repo = ApiRepository(ApiClient.retrofit.create(BlagoApi::class.java))
        val myId = getSharedPreferences("app", MODE_PRIVATE).getInt("user_id", 0)

        adapter = MessagesAdapter(
            itemsInput = emptyList(),
            myId = myId,
            onImageClick = { url, message ->
                openViewer(
                    url = url,
                    senderName = message.display_name ?: "Неизвестный",
                    sentTime = message.sent_at,
                    caption = message.content
                )
            },
            onLongClick = { message ->
                if (message.sender_id == myId) {
                    showMessageOptions(message)
                }
            }
        )
        recycler.adapter = adapter

        attachmentsAdapter = AttachmentsAdapter(
            items = selectedImages,
            onRemove = { position ->
                selectedImages.removeAt(position)
                updateAttachmentsPreview()
            },
            onClick = { position ->
                editingPosition = position
                startCrop(selectedImages[position])
            }
        )
        attachmentsRecycler.adapter = attachmentsAdapter

        attachBtn.setOnClickListener {
            if (editingMessageId != -1) return@setOnClickListener 
            pickImagesLauncher.launch("image/*")
        }

        sendBtn.setOnClickListener {
            val text = messageInput.text.toString().trim()
            if (editingMessageId != -1) {
                if (text.isNotBlank()) {
                    saveEditedMessage(editingMessageId, text)
                }
            } else {
                if (text.isNotBlank() || selectedImages.isNotEmpty()) {
                    sendMessage(text)
                    messageInput.text.clear()
                }
            }
        }

        swipeRefresh.setOnRefreshListener {
            loadMessages(myId)
        }

        loadMessages(myId)
    }

    private fun setupHeaderSwipe(clickListener: View.OnClickListener) {
        var startY = 0f
        var isProxying = false

        val touchListener = View.OnTouchListener { v, event ->
            // Подготавливаем событие для проброса в SwipeRefreshLayout
            val offsetEvent = MotionEvent.obtain(event)
            val loc = IntArray(2)
            swipeRefresh.getLocationOnScreen(loc)
            val vLoc = IntArray(2)
            v.getLocationOnScreen(vLoc)
            // Корректируем координаты относительно SwipeRefreshLayout
            offsetEvent.offsetLocation((vLoc[0] - loc[0]).toFloat(), (vLoc[1] - loc[1]).toFloat())

            val result = when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.rawY
                    isProxying = false
                    forceRefresh = true // Разрешаем перехват
                    swipeRefresh.dispatchTouchEvent(offsetEvent)
                    true // Забираем тач, чтобы отследить движение
                }
                MotionEvent.ACTION_MOVE -> {
                    val diff = event.rawY - startY
                    if (!isProxying && diff > 10) {
                        isProxying = true
                    }
                    if (isProxying) {
                        swipeRefresh.dispatchTouchEvent(offsetEvent)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    swipeRefresh.dispatchTouchEvent(offsetEvent)
                    forceRefresh = false
                    
                    if (!isProxying && event.action == MotionEvent.ACTION_UP) {
                        // Если это был просто тап (не свайп), вызываем клик
                        clickListener.onClick(v)
                    }
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
        attachBtn.setOnClickListener {
            cancelEditing()
        }
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(messageInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    private fun cancelEditing() {
        editingMessageId = -1
        messageInput.text.clear()
        sendBtn.setImageResource(R.drawable.ic_send)
        attachBtn.setImageResource(R.drawable.ic_add)
        attachBtn.setOnClickListener {
            pickImagesLauncher.launch("image/*")
        }
    }

    private fun saveEditedMessage(messageId: Int, text: String) {
        val token = getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
        val myId = getSharedPreferences("app", MODE_PRIVATE).getInt("user_id", 0)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repo.editMessage(token, messageId, text)
                runOnUiThread {
                    cancelEditing()
                    loadMessages(myId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@ChatActivity, "Ошибка при редактировании: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun confirmDeleteMessage(message: Message) {
        AlertDialog.Builder(this)
            .setTitle("Удалить сообщение?")
            .setMessage("Это действие нельзя отменить.")
            .setPositiveButton("Удалить") { _, _ ->
                deleteMessage(message.id)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteMessage(messageId: Int) {
        val token = getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
        val myId = getSharedPreferences("app", MODE_PRIVATE).getInt("user_id", 0)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repo.deleteMessage(token, messageId)
                runOnUiThread {
                    loadMessages(myId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@ChatActivity, "Ошибка при удалении: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun createUI() {
        rootLayout = FrameLayout(this)
        rootLayout.setBackgroundColor(DesignEngine.Theme.colorBackground)
        setContentView(rootLayout)

        headerContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(DesignEngine.Theme.colorCardBackground)
            elevation = LayoutHelper.dp(4).toFloat()
        }

        val backBtn = ImageView(this).apply {
            setImageResource(R.drawable.ic_back)
            setColorFilter(DesignEngine.Theme.colorIconPrimary, PorterDuff.Mode.SRC_IN)
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(12), LayoutHelper.dp(12), LayoutHelper.dp(12))
            background = getRippleDrawable()
            setOnClickListener { finish() }
        }
        headerContainer.addView(backBtn, LayoutHelper.createLinear(48, 48, 0f, 0, 4, 0, 4, 0))

        chatAvatarView = ShapeableImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.LTGRAY)
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(LayoutHelper.dp(20).toFloat())
                .build()
        }
        headerContainer.addView(chatAvatarView, LayoutHelper.createLinear(40, 40))

        val infoContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        
        chatNameView = TextView(this).apply {
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        infoContainer.addView(chatNameView)

        headerContainer.addView(infoContainer, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 1f, 0, 12, 0, 16, 0))
        rootLayout.addView(headerContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP))

        val inputContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(DesignEngine.Theme.colorCardBackground)
            elevation = LayoutHelper.dp(8).toFloat() 
        }

        attachmentsRecycler = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@ChatActivity, LinearLayoutManager.HORIZONTAL, false)
            visibility = View.GONE
            setPadding(LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8))
            clipToPadding = false
        }
        inputContainer.addView(attachmentsRecycler, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            setPadding(LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8))
        }

        attachBtn = ImageView(this).apply {
            setImageResource(R.drawable.ic_add)
            setColorFilter(DesignEngine.Theme.colorIconPrimary, PorterDuff.Mode.SRC_IN)
            setPadding(LayoutHelper.dp(10), LayoutHelper.dp(10), LayoutHelper.dp(10), LayoutHelper.dp(10))
            background = getRippleDrawable()
        }
        inputRow.addView(attachBtn, LayoutHelper.createLinear(48, 48, 0f, 0, 0, 0, 0, 2))

        messageInput = EditText(this).apply {
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorDivider)
                cornerRadius = LayoutHelper.dp(20).toFloat()
            }
            hint = "Сообщение"
            setHintTextColor(DesignEngine.Theme.colorTextSecondary)
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            textSize = 16f
            maxLines = 6
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(10), LayoutHelper.dp(16), LayoutHelper.dp(10))
        }
        inputRow.addView(messageInput, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 0, 4, 0, 4, 0))

        sendBtn = ImageView(this).apply {
            setImageResource(R.drawable.ic_send)
            setColorFilter(DesignEngine.Theme.colorBlue, PorterDuff.Mode.SRC_IN)
            setPadding(LayoutHelper.dp(10), LayoutHelper.dp(10), LayoutHelper.dp(10), LayoutHelper.dp(10))
            background = getRippleDrawable()
        }
        inputRow.addView(sendBtn, LayoutHelper.createLinear(48, 48, 0f, 0, 0, 0, 0, 2))

        inputContainer.addView(inputRow)
        rootLayout.addView(inputContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM))

        val listContainer = FrameLayout(this)
        
        // Кастомный SwipeRefreshLayout для поддержки свайпа за хедер
        swipeRefresh = object : SwipeRefreshLayout(this) {
            override fun canChildScrollUp(): Boolean {
                return if (forceRefresh) false else super.canChildScrollUp()
            }
        }.apply {
            setProgressBackgroundColorSchemeColor(DesignEngine.Theme.colorCardBackground)
            setColorSchemeColors(DesignEngine.Theme.colorBlue)
        }

        recycler = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_NEVER
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

        rootLayout.addView(listContainer, 0, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { _, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            headerContainer.setPadding(0, systemBars.top + LayoutHelper.dp(4), 0, LayoutHelper.dp(4))
            headerContainer.layoutParams.height = systemBars.top + LayoutHelper.dp(60)
            val bottomPadding = if (ime.bottom > 0) ime.bottom else systemBars.bottom
            inputContainer.setPadding(0, 0, 0, bottomPadding)
            recycler.setPadding(0, systemBars.top + LayoutHelper.dp(60), 0, LayoutHelper.dp(80))
            
            // Настройка индикатора: он стартует под хедером и выезжает чуть ниже него
            val headerHeight = systemBars.top + LayoutHelper.dp(60)
            swipeRefresh.setProgressViewOffset(false, systemBars.top, headerHeight + LayoutHelper.dp(30))
            
            WindowInsetsCompat.CONSUMED
        }
        
        ViewCompat.setOnApplyWindowInsetsListener(inputContainer) { view, windowInsets ->
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val bottomInset = if (ime.bottom > 0) ime.bottom else systemBars.bottom
            view.setPadding(0, 0, 0, bottomInset)
            recycler.setPadding(0, headerContainer.height, 0, LayoutHelper.dp(60) + bottomInset)
            if (ime.bottom > 0 && adapter.itemCount > 0) {
                 recycler.post { recycler.smoothScrollToPosition(adapter.itemCount - 1) }
            }
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun startCrop(uri: Uri) {
        val destinationFileName = "cropped_${System.currentTimeMillis()}.jpg"
        val destinationUri = Uri.fromFile(File(cacheDir, destinationFileName))
        val uCrop = UCrop.of(uri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(1080, 1080)
        cropLauncher.launch(uCrop.getIntent(this))
    }

    private fun openViewer(url: String, senderName: String, sentTime: String, caption: String?) {
        val intent = Intent(this, PhotoViewerActivity::class.java)
        intent.putExtra("image_url", url)
        intent.putExtra("sender_name", senderName)
        intent.putExtra("sent_time", sentTime)
        intent.putExtra("caption", caption)
        startActivity(intent)
    }

    private fun updateAttachmentsPreview() {
        attachmentsAdapter.notifyDataSetChanged()
        attachmentsRecycler.visibility = if (selectedImages.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun getRippleDrawable(): android.graphics.drawable.Drawable {
        val outValue = android.util.TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
        return getDrawable(outValue.resourceId)!!
    }

    private fun loadMessages(myId: Int) {
        val token = getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val messages: List<Message> = repo.getMessages(token, chatId)
                if (targetUserId == 0) {
                    val otherMsg = messages.find { it.sender_id != myId }
                    if (otherMsg != null) {
                        targetUserId = otherMsg.sender_id
                    }
                }
                runOnUiThread {
                    swipeRefresh.isRefreshing = false
                    if (messages.isEmpty()) {
                        emptyText.visibility = View.VISIBLE
                    } else {
                        emptyText.visibility = View.GONE
                    }
                    adapter.update(messages)
                    if (isFirstLoad && messages.isNotEmpty()) {
                        recycler.scrollToPosition(adapter.itemCount - 1)
                        isFirstLoad = false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    swipeRefresh.isRefreshing = false
                }
            }
        }
    }

    private fun sendMessage(text: String) {
        val token = getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
        val myId = getSharedPreferences("app", MODE_PRIVATE).getInt("user_id", 0)
        val localImageUris = selectedImages.map { it.toString() }
        val sentAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val tempMessage = Message(
            id = 0,
            chat_id = chatId,
            sender_id = myId,
            content = text,
            sent_at = sentAt,
            is_read = 0,
            display_name = "",
            avatar = "",
            images = localImageUris
        )
        runOnUiThread {
            adapter.addMessage(tempMessage)
            recycler.scrollToPosition(adapter.itemCount - 1)
            selectedImages.clear()
            updateAttachmentsPreview()
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val filesToUpload = localImageUris.mapNotNull { uriString ->
                    uriToCompressedFile(Uri.parse(uriString))
                }
                repo.sendMessage(token, chatId, text, filesToUpload)
                loadMessages(myId)
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@ChatActivity, "Ошибка отправки: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun uriToCompressedFile(uri: Uri): File? {
        return try {
            val cr = contentResolver
            val mimeType = cr.getType(uri)
            val extension = MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(mimeType) ?: "jpg"
            val originalFile = File(cacheDir, "original_${System.currentTimeMillis()}.$extension")
            cr.openInputStream(uri)?.use { input ->
                FileOutputStream(originalFile).use { output ->
                    input.copyTo(output)
                }
            }
            val exif = androidx.exifinterface.media.ExifInterface(originalFile.absolutePath)
            val orientation = exif.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
            )
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
            val newWidth: Int
            val newHeight: Int
            if (ratio > 1) {
                newWidth = maxSize
                newHeight = (maxSize / ratio).toInt()
            } else {
                newHeight = maxSize
                newWidth = (maxSize * ratio).toInt()
            }
            val resized = Bitmap.createScaledBitmap(rotated, newWidth, newHeight, true)
            val finalFile = File(cacheDir, "img_${System.currentTimeMillis()}.jpg")
            val out = FileOutputStream(finalFile)
            resized.compress(Bitmap.CompressFormat.JPEG, 80, out)
            out.flush()
            out.close()
            finalFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
