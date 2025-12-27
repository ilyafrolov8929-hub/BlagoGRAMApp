package com.kia.blagogramapp

import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.kia.blagogramapp.adapters.MessagesAdapter
import com.kia.blagogramapp.api.*
import com.kia.blagogramapp.database.AiMessage
import com.kia.blagogramapp.database.AppDatabase
import com.kia.blagogramapp.model.Message
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class AiChatActivity : AppCompatActivity() {

    private lateinit var rootLayout: FrameLayout
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: MessagesAdapter
    private lateinit var sendBtn: ImageView
    private lateinit var messageInput: EditText
    private lateinit var headerContainer: LinearLayout
    private lateinit var inputContainer: LinearLayout
    private lateinit var chatNameView: TextView

    private val db by lazy { AppDatabase.getDatabase(this) }
    private val myId = -1 
    private val aiId = -2 

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DesignEngine.init(this)
        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        DesignEngine.Theme.apply(themePrefs.getBoolean("is_dark_theme", false), themePrefs.getBoolean("use_monet", false))

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        createUI()
        setupRecycler()

        lifecycleScope.launch {
            db.aiMessageDao().getAllMessages().collect { messages ->
                val uiMessages = messages.map {
                    Message(
                        id = it.id,
                        chat_id = 0,
                        sender_id = if (it.isUser) myId else aiId,
                        content = it.content,
                        sent_at = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(it.timestamp)),
                        is_read = 1,
                        display_name = if (it.isUser) "Я" else "AI Assistant",
                        avatar = ""
                    )
                }
                adapter.update(uiMessages) {
                    if (adapter.itemCount > 0) {
                        recycler.scrollToPosition(adapter.itemCount - 1)
                    }
                }
            }
        }

        sendBtn.setOnClickListener {
            val text = messageInput.text.toString().trim()
            if (text.isNotBlank()) {
                sendMessage(text)
                messageInput.text.clear()
            }
        }

        setupInsets()
    }

    private fun setupRecycler() {
        adapter = MessagesAdapter(
            myId = myId,
            onImageClick = { _, _ -> },
            onLongClick = { msg ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("AI Message", msg.content)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Текст скопирован", Toast.LENGTH_SHORT).show()
            }
        )
        recycler.adapter = adapter
    }

    private fun sendMessage(text: String) {
        lifecycleScope.launch {
            db.aiMessageDao().insert(AiMessage(content = text, isUser = true))
            
            val typingId = db.aiMessageDao().insert(AiMessage(content = "AI печатает ответ...", isUser = false))
            
            val aiPrefs = getSharedPreferences("ai_settings", MODE_PRIVATE)
            val token = aiPrefs.getString("api_token", "") ?: ""
            val apiUrl = aiPrefs.getString("api_url", "https://api.openai.com/v1/chat/completions") ?: "https://api.openai.com/v1/chat/completions"
            val model = aiPrefs.getString("api_model", "gpt-4o-mini") ?: "gpt-4o-mini"

            if (token.isBlank()) {
                updateMessage(typingId, "⚠️ Ошибка: API Token не указан. Перейдите в настройки.")
                return@launch
            }

            try {
                val aiContent = withTimeout(60000L) {
                    val retrofit = Retrofit.Builder()
                        .baseUrl("https://api.openai.com/")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                    val api = retrofit.create(AiApi::class.java)

                    if (apiUrl.contains("generativelanguage")) {
                        val rawMessages = db.aiMessageDao().getAllMessages().first().filter { it.id.toLong() != typingId }
                        val filteredHistory = mutableListOf<GeminiContent>()
                        var lastRole = ""
                        
                        rawMessages.takeLast(20).forEach { msg ->
                            val currentRole = if (msg.isUser) "user" else "model"
                            if (currentRole != lastRole) {
                                filteredHistory.add(GeminiContent(role = currentRole, parts = listOf(GeminiPart(msg.content))))
                                lastRole = currentRole
                            }
                        }
                        if (filteredHistory.isNotEmpty() && filteredHistory.first().role == "model") filteredHistory.removeAt(0)

                        val response = api.getGeminiCompletion(url = apiUrl, apiKey = token, request = GeminiRequest(contents = filteredHistory))
                        response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Gemini не дал ответа"
                    } else {
                        val history = db.aiMessageDao().getAllMessages().first().filter { it.id.toLong() != typingId }.takeLast(15).map {
                            AiChatMessage(role = if (it.isUser) "user" else "assistant", content = it.content)
                        }
                        val response = api.getCompletion(url = apiUrl, auth = "Bearer $token", request = AiRequest(model = model, messages = history))
                        response.choices.firstOrNull()?.message?.content ?: "OpenAI не дал ответа"
                    }
                }

                updateMessage(typingId, aiContent)

            } catch (e: TimeoutCancellationException) {
                updateMessage(typingId, "⚠️ Ошибка: Превышено время ожидания (60 сек). Бот слишком долго думал или текст слишком большой.")
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string() ?: "Нет деталей"
                updateMessage(typingId, "⚠️ Ошибка API (${e.code()}):\n$errorBody")
            } catch (e: Exception) {
                e.printStackTrace()
                updateMessage(typingId, "⚠️ Системная ошибка:\n${e.message}")
            }
        }
    }

    private suspend fun updateMessage(id: Long, newContent: String) {
        withContext(Dispatchers.IO) {
            val messages = db.aiMessageDao().getAllMessages().first()
            val msg = messages.find { it.id.toLong() == id }
            if (msg != null) {
                db.aiMessageDao().insert(msg.copy(content = newContent))
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
            val outValue = android.util.TypedValue()
            theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
            background = getDrawable(outValue.resourceId)
            setOnClickListener { finish() }
        }
        headerContainer.addView(backBtn, LayoutHelper.createLinear(48, 48, 0f, 0, 4, 0, 4, 0))

        val aiAvatar = ShapeableImageView(this).apply {
            setImageResource(R.drawable.ic_launcher_foreground)
            setBackgroundColor(DesignEngine.Theme.colorBlue)
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(LayoutHelper.dp(20).toFloat())
                .build()
        }
        headerContainer.addView(aiAvatar, LayoutHelper.createLinear(40, 40))

        chatNameView = TextView(this).apply {
            text = "AI Assistant"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        headerContainer.addView(chatNameView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 1f, 0, 12, 0, 16, 0))
        rootLayout.addView(headerContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.dp(30), Gravity.TOP))

        inputContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            setBackgroundColor(DesignEngine.Theme.colorCardBackground)
            setPadding(LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8))
            elevation = LayoutHelper.dp(8).toFloat()
        }

        messageInput = EditText(this).apply {
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorDivider)
                cornerRadius = LayoutHelper.dp(20).toFloat()
            }
            hint = "Спросите что-нибудь..."
            setHintTextColor(DesignEngine.Theme.colorTextSecondary)
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            textSize = 16f
            maxLines = 6
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(10), LayoutHelper.dp(16), LayoutHelper.dp(10))
        }
        inputContainer.addView(messageInput, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 0, 8, 0, 8, 0))

        sendBtn = ImageView(this).apply {
            setImageResource(R.drawable.ic_send)
            setColorFilter(DesignEngine.Theme.colorBlue, PorterDuff.Mode.SRC_IN)
            setPadding(LayoutHelper.dp(10), LayoutHelper.dp(10), LayoutHelper.dp(10), LayoutHelper.dp(10))
            val outValue = android.util.TypedValue()
            theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
            background = getDrawable(outValue.resourceId)
        }
        inputContainer.addView(sendBtn, LayoutHelper.createLinear(48, 48, 0f, 0, 0, 0, 0, 2))
        rootLayout.addView(inputContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM))

        recycler = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@AiChatActivity).apply {
                stackFromEnd = true
            }
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        rootLayout.addView(recycler, 0, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { _, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            val imeVisible = windowInsets.isVisible(WindowInsetsCompat.Type.ime())
            
            headerContainer.setPadding(0, systemBars.top, 0, 0)
            val bottomInset = if (imeVisible) ime.bottom else systemBars.bottom
            inputContainer.setPadding(LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8), bottomInset + LayoutHelper.dp(8))
            
            val headerHeight = systemBars.top + LayoutHelper.dp(30)
            recycler.setPadding(0, headerHeight, 0, LayoutHelper.dp(80) + bottomInset)
            
            windowInsets
        }

        ViewCompat.setWindowInsetsAnimationCallback(
            inputContainer,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat {
                    val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
                    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    val bottom = if (ime.bottom > systemBars.bottom) ime.bottom else systemBars.bottom
                    inputContainer.setPadding(LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8), bottom + LayoutHelper.dp(8))
                    recycler.setPadding(0, headerContainer.height, 0, inputContainer.height + LayoutHelper.dp(16))
                    return insets
                }
            }
        )
    }
}
