package com.kia.blagogramapp.ui.modules

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.kia.blagogramapp.*
import com.kia.blagogramapp.api.ApiClient
import com.kia.blagogramapp.api.ApiRepository
import com.kia.blagogramapp.model.Chat
import com.kia.blagogramapp.model.Post
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ShareModule {
    companion object {
        fun show(context: Context, post: Post) {
            val dialog = BottomSheetDialog(context, R.style.BottomSheetDialogTheme)
            val root = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                background = GradientDrawable().apply {
                    setColor(DesignEngine.Theme.colorSheetBackground)
                    val r = LayoutHelper.dp(20).toFloat()
                    cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
                }
                setPadding(0, LayoutHelper.dp(8), 0, LayoutHelper.dp(16))
            }

            // Полоска сверху (handle)
            val handle = View(context).apply {
                background = GradientDrawable().apply {
                    setColor(0x40808080.toInt())
                    cornerRadius = LayoutHelper.dp(2).toFloat()
                }
            }
            root.addView(handle, LayoutHelper.createLinear(40, 4, 0f, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 16))

            val title = TextView(context).apply {
                text = "Поделиться"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(DesignEngine.Theme.colorTextPrimary)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, LayoutHelper.dp(16))
            }
            root.addView(title)

            val recyclerView = RecyclerView(context).apply {
                layoutManager = LinearLayoutManager(context)
                layoutParams = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.dp(400))
            }
            root.addView(recyclerView)

            val token = context.getSharedPreferences("app", Context.MODE_PRIVATE).getString("token", "") ?: ""
            val apiRepo = ApiRepository(ApiClient.retrofit.create(BlagoApi::class.java))

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val chats = withContext(Dispatchers.IO) { apiRepo.getChats(token) }

                    recyclerView.adapter = ShareAdapter(chats, post) { chat, comment ->
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                // Новый JSON-формат
                                val jsonMap = mutableMapOf<String, Any>(
                                    "type" to "shared_post",
                                    "post_id" to post.id
                                )

                                val trimmedComment = comment.trim()
                                if (trimmedComment.isNotEmpty()) {
                                    jsonMap["comment"] = trimmedComment
                                }

                                val msgContent = JSONObject(jsonMap).toString()

                                withContext(Dispatchers.IO) {
                                    apiRepo.sendMessage(token, chat.id, msgContent)
                                }

                                Toast.makeText(context, "Отправлено в ${chat.name}", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Ошибка отправки: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Ошибка загрузки чатов", Toast.LENGTH_SHORT).show()
                }
            }

            dialog.setContentView(root)
            dialog.show()
        }
    }

    private class ShareAdapter(
        private val chats: List<Chat>,
        private val post: Post, // Теперь адаптер знает, чем делимся
        private val onSend: (Chat, String) -> Unit // Принимает чат и текст сообщения
    ) : RecyclerView.Adapter<ShareAdapter.VH>() {

        class VH(val cell: ChatCell) : RecyclerView.ViewHolder(cell)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val cell = ChatCell(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            return VH(cell)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val chat = chats[position]
            val context = holder.itemView.context // Берем контекст из view

            holder.cell.setData(
                name = chat.name ?: "Чат",
                message = chat.last_msg ?: "",
                time = chat.last_time ?: "",
                unread = 0,
                avatar = chat.avatar,
                pinned = false,
                isPremium = chat.is_premium,
            )

            // При клике открываем диалог подтверждения с вводом текста
            holder.cell.setOnClickListener {
                showConfirmDialog(context, chat, post) { comment ->
                    onSend(chat, comment)
                }
            }
        }

        private fun showConfirmDialog(context: Context, chat: Chat, post: Post, onConfirm: (String) -> Unit) {
            val dialog = BottomSheetDialog(context, R.style.BottomSheetDialogTheme)
            val root = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                background = GradientDrawable().apply {
                    setColor(DesignEngine.Theme.colorSheetBackground)
                    cornerRadius = LayoutHelper.dp(16).toFloat()
                }
                setPadding(LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(16))
            }

            val header = TextView(context).apply {
                text = "Отправить в ${chat.name}"
                setTextColor(DesignEngine.Theme.colorTextPrimary)
                typeface = Typeface.DEFAULT_BOLD
                textSize = 16f
            }

            val input = EditText(context).apply {
                hint = "Ваше сообщение..."
                setHintTextColor(0x80808080.toInt())
                setTextColor(DesignEngine.Theme.colorTextPrimary)
                background = null
                setPadding(0, LayoutHelper.dp(12), 0, LayoutHelper.dp(12))
            }

            // Превью поста (карточка внутри диалога)
            val preview = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                background = GradientDrawable().apply {
                    setColor(0x10808080)
                    cornerRadius = LayoutHelper.dp(8).toFloat()
                }
                setPadding(LayoutHelper.dp(12), LayoutHelper.dp(12), LayoutHelper.dp(12), LayoutHelper.dp(12))

                val previewTitle = TextView(context).apply {
                    text = "Запись со стены"
                    textSize = 12f
                    setTextColor(DesignEngine.Theme.colorBlue)
                    typeface = Typeface.DEFAULT_BOLD
                }
                val previewBody = TextView(context).apply {
                    text = post.content ?: "Без текста"
                    maxLines = 2
                    textSize = 14f
                    setTextColor(DesignEngine.Theme.colorTextSecondary)
                }
                addView(previewTitle)
                addView(previewBody)
            }

            val sendBtn = Button(context).apply {
                text = "Отправить"
                background = GradientDrawable().apply {
                    setColor(DesignEngine.Theme.colorBlue)
                    cornerRadius = LayoutHelper.dp(8).toFloat()
                }
                setTextColor(Color.WHITE)

                setOnClickListener {
                    onConfirm(input.text.toString())
                    dialog.dismiss()
                }
            }

            root.addView(header)
            root.addView(input)
            root.addView(preview)
            root.addView(View(context), LayoutHelper.createLinear(1, 16)) // Отступ
            root.addView(sendBtn)

            dialog.setContentView(root)
            dialog.show()
        }

        override fun getItemCount() = chats.size
    }
}