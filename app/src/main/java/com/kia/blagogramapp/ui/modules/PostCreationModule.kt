package com.kia.blagogramapp.ui.modules

import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.DialogFragment
import coil.load
import com.google.android.material.imageview.ShapeableImageView
import com.kia.blagogramapp.*
import com.kia.blagogramapp.api.ApiClient
import com.kia.blagogramapp.api.ApiRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.properties.Delegates

class PostCreationModule(private val onPostPublished: () -> Unit) : DialogFragment() {

    var padding by Delegates.notNull<Int>()
    private lateinit var postInput: EditText
    private lateinit var overlayImagePreview: ShapeableImageView
    private lateinit var overlayImageContainer: FrameLayout
    private var selectedImageFile: File? = null
    private val apiRepo = ApiRepository(ApiClient.retrofit.create(BlagoApi::class.java))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Делаем диалог на весь экран
        setStyle(STYLE_NORMAL, android.R.style.Theme_DeviceDefault_NoActionBar)
    }


    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            window.setDimAmount(0f)
            window.setBackgroundDrawableResource(android.R.color.transparent)

            // Подключаем созданную анимацию
            window.setWindowAnimations(R.style.DialogAnimation)

            // EDGE-TO-EDGE: чтобы убрать черный бар снизу и рисовать под статус-баром
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val isDark = context.getSharedPreferences("app_theme", android.content.Context.MODE_PRIVATE)
            .getBoolean("is_dark_theme", false)

        // Основной контейнер (тот самый оверлей)
        val root = FrameLayout(context).apply {
            setBackgroundColor(DesignEngine.Theme.colorBackground)
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(content)

        // Тулбар
        val toolbar = DesignEngine.createToolBar(
            context = context,
            title = "Создание поста",
            leftButtonIconRes = R.drawable.ic_back,
            isDark = isDark,
            leftButtonClickListener = { dismiss() }
        )
        content.addView(toolbar)

        // Добавляем отступ снизу, чтобы контент не перекрывался плавающим нижним тулбаром
        val scroll = NestedScrollView(context).apply {
            isFillViewport = true
            setPadding(0, 0, 0, LayoutHelper.dp(100)) // Резервируем место под нижний бар
            clipToPadding = false
        }
        val innerContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(LayoutHelper.dp(16))
        }
        scroll.addView(innerContent)
        content.addView(scroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1f))

        // Кнопка фото
        val headerBox = RelativeLayout(context)
        val photoBtn = TextView(context).apply {
            text = "Фото"
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(6), LayoutHelper.dp(12), LayoutHelper.dp(6))
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorCardBackground)
                cornerRadius = LayoutHelper.dp(18).toFloat()
            }
            setOnClickListener { selectPostImage() }
        }
        headerBox.addView(photoBtn, LayoutHelper.createRelative(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT).apply {
            addRule(RelativeLayout.ALIGN_PARENT_END)
        })
        innerContent.addView(headerBox, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 16))

        // Поле ввода
        postInput = EditText(context).apply {
            hint = "Текст..."
            setHintTextColor(DesignEngine.Theme.colorTextSecondary)
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            gravity = Gravity.TOP
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorCardBackground)
                cornerRadius = LayoutHelper.dp(20).toFloat()
            }
            setPadding(LayoutHelper.dp(16))
            minHeight = LayoutHelper.dp(200)
        }
        innerContent.addView(postInput, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 16))

        // Превью изображения
        overlayImageContainer = FrameLayout(context).apply {
            visibility = View.GONE
            setPadding(0, 0, 0, LayoutHelper.dp(16))
        }
        overlayImagePreview = ShapeableImageView(context).apply {
            shapeAppearanceModel = shapeAppearanceModel.toBuilder().setAllCornerSizes(LayoutHelper.dp(12).toFloat()).build()
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        overlayImageContainer.addView(overlayImagePreview, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 200))

        val removeImg = ImageView(context).apply {
            setImageResource(R.drawable.ic_close_small)
            setColorFilter(Color.WHITE)
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0x80000000.toInt()) }
            setOnClickListener {
                selectedImageFile = null
                overlayImageContainer.visibility = View.GONE
            }
        }
        overlayImageContainer.addView(removeImg, LayoutHelper.createFrame(24, 24, Gravity.TOP or Gravity.END, 0, 8, 8, 0))
        innerContent.addView(overlayImageContainer)

        // Кнопка опубликовать
        val publishBtn = TextView(context).apply {
            text = "Опубликовать"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorCardBackground)
                cornerRadius = LayoutHelper.dp(25).toFloat()
            }
            setPadding(0, LayoutHelper.dp(16), 0, LayoutHelper.dp(16))
            setOnClickListener { publishPost(postInput.text.toString()) }
        }
        innerContent.addView(publishBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 16))

        return root
    }

    private fun selectPostImage() {
        MediaPickerSheet.show(requireActivity() as AppCompatActivity, mode = MediaPickerSheet.Mode.POST) { uris, _ ->
            if (uris.isNotEmpty()) {
                val uri = uris.first()
                selectedImageFile = (requireActivity() as NewsActivity).uriToFile(uri) // Важно: метод uriToFile лучше тоже вынести в Utils
                overlayImageContainer.visibility = View.VISIBLE
                overlayImagePreview.load(uri)
            }
        }
    }

    private fun publishPost(text: String) {
        if (text.isBlank()) return
        val token = requireContext().getSharedPreferences("app", android.content.Context.MODE_PRIVATE).getString("token", "") ?: ""
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    apiRepo.createPost(token, text, selectedImageFile)
                }
                if (success) {
                    onPostPublished()
                    dismiss()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        fun show(activity: AppCompatActivity, onPublished: () -> Unit) {
            PostCreationModule(onPublished).show(activity.supportFragmentManager, "post_creation")
        }
    }
}