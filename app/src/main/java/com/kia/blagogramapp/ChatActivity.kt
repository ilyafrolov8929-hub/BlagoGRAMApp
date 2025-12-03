package com.kia.blagogramapp

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsetsController
import android.webkit.MimeTypeMap
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.imageview.ShapeableImageView
import com.kia.blagogramapp.adapters.AttachmentsAdapter
import com.kia.blagogramapp.adapters.MessagesAdapter
import com.kia.blagogramapp.api.ApiClient
import com.kia.blagogramapp.api.ApiRepository
import com.kia.blagogramapp.model.Message
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
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: MessagesAdapter

    private lateinit var sendBtn: ImageView
    private lateinit var attachBtn: ImageView
    private lateinit var messageInput: EditText
    private lateinit var attachmentsRecycler: RecyclerView
    private lateinit var attachmentsAdapter: AttachmentsAdapter
    private val selectedImages = mutableListOf<Uri>()
    private var isFirstLoad = true
    private var editingPosition = -1

    private val headerContent by lazy { findViewById<View>(R.id.headerContent) }

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

    private fun setupKeyboardListener() {
        val rootView = findViewById<View>(android.R.id.content)

        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val heightDiff = rootView.rootView.height - rootView.height
            val isKeyboardOpen = heightDiff > 300
            val count = adapter.itemCount

            if (count > 0) {
                if (isKeyboardOpen) {
                    recycler.post {
                        recycler.smoothScrollToPosition(adapter.itemCount - 1)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        setupKeyboardListener()

        val avatar = intent.getStringExtra("chat_avatar")
        val avatarView = findViewById<ShapeableImageView>(R.id.chatAvatar)

        sendBtn = findViewById(R.id.sendBtn)
        attachBtn = findViewById(R.id.attachBtn)
        messageInput = findViewById(R.id.messageInput)
        attachmentsRecycler = findViewById(R.id.attachmentsPreview)

        applyThemeAuto()


        avatarView.load("https://blagogram.ru/avatars/$avatar") {
            crossfade(true)
            placeholder(R.drawable.ic_user_placeholder)
            error(R.drawable.ic_user_placeholder)
        }

        chatId = intent.getIntExtra("chat_id", 0)
        val chatName = intent.getStringExtra("chat_name") ?: "Чат"
        findViewById<TextView>(R.id.chatName).text = chatName

        if (chatId == 0) {
            finish()
            return
        }

        repo = ApiRepository(ApiClient.retrofit.create(BlagoApi::class.java))

        recycler = findViewById(R.id.messagesList)
        recycler.layoutManager = LinearLayoutManager(this)

        val myId = getSharedPreferences("app", MODE_PRIVATE).getInt("user_id", 0)

        // В методе onCreate:
        adapter = MessagesAdapter(emptyList(), myId) { url, message ->
            // Предполагаем, что ваш адаптер теперь возвращает не только url, но и объект message
            openViewer(
                url = url,
                senderName = message.display_name ?: "Неизвестный", // Или имя из message
                sentTime = message.sent_at, // Время отправки
                caption = message.content   // Текст сообщения под фото
            )
        }

        recycler.adapter = adapter

        findViewById<ImageView>(R.id.backBtn).setOnClickListener {
            finish()
        }

        // Attachments setup
        attachmentsRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
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
            pickImagesLauncher.launch("image/*")
        }

        loadMessages()

        sendBtn.setOnClickListener {
            val text = messageInput.text.toString().trim()
            if (text.isNotBlank() || selectedImages.isNotEmpty()) {
                sendMessage(text)
                messageInput.text.clear()
            }
        }

        messageInput.setOnTouchListener { v, event ->
            if (v.hasFocus()) {
                v.parent.requestDisallowInterceptTouchEvent(true)

                when (event.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_SCROLL -> {
                        v.parent.requestDisallowInterceptTouchEvent(true)
                        return@setOnTouchListener false
                    }
                    MotionEvent.ACTION_UP -> {
                        v.performClick()
                        return@setOnTouchListener false
                    }
                }
            }
            return@setOnTouchListener false
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
        intent.putExtra("caption", caption) // Передаем текст сообщения
        startActivity(intent)
    }


    private fun updateAttachmentsPreview() {
        attachmentsAdapter.notifyDataSetChanged()
        attachmentsRecycler.visibility = if (selectedImages.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun setStatusBarIconsDark(isDarkIcons: Boolean) {
        val window = window

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            if (controller != null) {
                if (isDarkIcons) {
                    controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                } else {
                    controller.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                }
            }
        } else {
            @Suppress("DEPRECATION")
            var flags = window.decorView.systemUiVisibility
            flags = if (isDarkIcons) {
                flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
            window.decorView.systemUiVisibility = flags
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyThemeAuto()
    }

    private fun applyThemeAuto() {
        val isDark = (resources.configuration.uiMode
                and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        if (isDark) {
            // ТЁМНАЯ ТЕМА → светлые элементы
            sendBtn.setColorFilter(Color.WHITE)
            attachBtn.setColorFilter(Color.WHITE)
            headerContent.setBackgroundColor(Color.parseColor("#333333"))
            setStatusBarIconsDark(false)  // ← ИКОНКИ СТАТУСБАРА СВЕТЛЫЕ

        } else {
            // СВЕТЛАЯ ТЕМА → тёмные элементы
            sendBtn.setColorFilter(Color.BLACK)
            attachBtn.setColorFilter(Color.BLACK)
            headerContent.setBackgroundColor(Color.parseColor("#858585"))
            setStatusBarIconsDark(true)  // ← ИКОНКИ СТАТУСБАРА СВЕТЛЫЕ
        }
    }

    private fun loadMessages() {
        val token = getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
        CoroutineScope(Dispatchers.IO).launch {

            try {
                val messages: List<Message> = repo.getMessages(token, chatId)

                runOnUiThread {
                    val emptyText = findViewById<TextView>(R.id.emptyMessagesText)

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
            }
        }
    }

    private fun sendMessage(text: String) {
        val token = getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
        val myId = getSharedPreferences("app", MODE_PRIVATE).getInt("user_id", 0)

        // 1. Optimistic Update in UI
        val localImageUris = selectedImages.map { it.toString() }
        val sentAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val tempMessage = Message(
            id = 0, // Temp ID
            chat_id = chatId,
            sender_id = myId,
            content = text,
            sent_at = sentAt,
            is_read = 0,
            display_name = "", // Not needed for own messages
            avatar = "",
            images = localImageUris
        )

        runOnUiThread {
            adapter.addMessage(tempMessage)
            recycler.scrollToPosition(adapter.itemCount - 1)
            selectedImages.clear()
            updateAttachmentsPreview()
        }

        // 2. Background Upload
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val filesToUpload = localImageUris.mapNotNull { uriString ->
                    uriToCompressedFile(Uri.parse(uriString))
                }
                repo.sendMessage(token, chatId, text, filesToUpload)
                // Optional: reload messages from server to get real IDs, or update local message with server response
                // For now, we will just reload all
                loadMessages()
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
            val contentResolver = contentResolver
            val mimeType = contentResolver.getType(uri)
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"

            val inputStream = contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            var width = options.outWidth
            var height = options.outHeight
            var inSampleSize = 1
            val reqWidth = 1280
            val reqHeight = 1280

            if (height > reqHeight || width > reqWidth) {
                val halfHeight: Int = height / 2
                val halfWidth: Int = width / 2
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            val finalOptions = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
            val finalInputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(finalInputStream, null, finalOptions)
            finalInputStream?.close()

            val file = File(cacheDir, "img_${System.currentTimeMillis()}.$extension")
            val outputStream = FileOutputStream(file)
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.close()
            bitmap?.recycle()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
