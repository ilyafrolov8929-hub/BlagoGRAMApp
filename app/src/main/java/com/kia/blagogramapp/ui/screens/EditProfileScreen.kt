package com.kia.blagogramapp.ui.screens

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.android.material.imageview.ShapeableImageView
import com.kia.blagogramapp.BaseScreen
import com.kia.blagogramapp.DesignEngine
import com.kia.blagogramapp.LayoutHelper
import com.kia.blagogramapp.R
import com.kia.blagogramapp.api.ApiRepository
import com.kia.blagogramapp.network.RetrofitClient
import com.kia.blagogramapp.ui.modules.ChatListLayout
import com.kia.blagogramapp.ui.modules.MediaPickerSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class EditProfileScreen(context: Context) : BaseScreen(context) {
    val activity = context as? AppCompatActivity
    private lateinit var inputName: EditText
    private lateinit var inputStatus: EditText
    private lateinit var avatarView: ShapeableImageView
    private lateinit var coverView: ShapeableImageView

    private var avatarFile: File? = null
    private var coverFile: File? = null

    private var repo = ApiRepository(RetrofitClient.api)


    override fun createView(): View {
        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(DesignEngine.Theme.colorBackground)
        }

        val themePrefs = context.getSharedPreferences("app_theme", MODE_PRIVATE)
        val isDarkTheme = themePrefs.getBoolean("is_dark_theme", false)


        val toolbar = DesignEngine.createToolBar(
            context = context,
            title = "Редактирование",
            leftButtonIconRes = R.drawable.ic_back,
            isDark = isDarkTheme,
            leftButtonClickListener = { finishFragment() },
            rightButtonIconRes = R.drawable.ic_check,
            rightButtonClickListener = { saveProfile() }
        )
        rootLayout.addView(toolbar, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP))


        val scrollView = ScrollView(context).apply {
            isFillViewport = true
            clipToPadding = false
        }

        val scrollContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(LayoutHelper.dp(20), LayoutHelper.dp(16), LayoutHelper.dp(20), LayoutHelper.dp(24))
        }

        val headerContainer = FrameLayout(context)

        coverView = ShapeableImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(DesignEngine.Theme.colorDivider)
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(LayoutHelper.dp(16).toFloat())
                .build()
            setOnClickListener {
                MediaPickerSheet.show(context, MediaPickerSheet.Mode.COVER) { uris, _ ->
                    if (uris.isNotEmpty()) handleSelectedCover(uris[0])
                }
            }
        }
        headerContainer.addView(coverView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 160))

        val editCoverBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_camera)
            setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            setPadding(LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8))
            background = GradientDrawable().apply {
                setColor(0x66000000)
                shape = GradientDrawable.OVAL
            }
            setOnClickListener {
                MediaPickerSheet.show(context, MediaPickerSheet.Mode.COVER) { uris, _ ->
                    if (uris.isNotEmpty()) handleSelectedCover(uris[0])
                }
            }
        }
        headerContainer.addView(editCoverBtn, LayoutHelper.createFrame(36, 36, Gravity.TOP or Gravity.END, 0, 12, 12, 0))

        avatarView = ShapeableImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.LTGRAY)
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(LayoutHelper.dp(50).toFloat())
                .build()
            strokeWidth = LayoutHelper.dp(3).toFloat()
            strokeColor = android.content.res.ColorStateList.valueOf(DesignEngine.Theme.colorBackground)
        }
        headerContainer.addView(avatarView, LayoutHelper.createFrame(100, 100, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL))

        val editAvatarBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_edit)
            setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            setPadding(LayoutHelper.dp(6), LayoutHelper.dp(6), LayoutHelper.dp(6), LayoutHelper.dp(6))
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorBlue)
                shape = GradientDrawable.OVAL
            }
            setOnClickListener {
                MediaPickerSheet.show(context, MediaPickerSheet.Mode.AVATAR) { uris, _ ->
                    if (uris.isNotEmpty()) handleSelectedAvatar(uris[0])
                }
            }
        }
        headerContainer.addView(editAvatarBtn, LayoutHelper.createFrame(32, 32, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 35, 0, 0, 0))

        scrollContainer.addView(headerContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 200, 0f, 0, 0, 0, 0, 24))

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorCardBackground)
                cornerRadius = LayoutHelper.dp(16).toFloat()
            }
            setPadding(LayoutHelper.dp(20), LayoutHelper.dp(20), LayoutHelper.dp(20), LayoutHelper.dp(20))
            elevation = 0f
        }

        card.addView(TextView(context).apply {
            text = "ИМЯ"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(DesignEngine.Theme.colorTextSecondary)
        }, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 8))

        inputName = EditText(context).apply {
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorDivider)
                cornerRadius = LayoutHelper.dp(24).toFloat()
            }
            hint = "Ваше имя"
            setHintTextColor(DesignEngine.Theme.colorTextSecondary)
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            textSize = 16f
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(16))
        }
        card.addView(inputName, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        card.addView(TextView(context).apply {
            text = "СТАТУС"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(DesignEngine.Theme.colorTextSecondary)
        }, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 20, 0, 8))

        inputStatus = EditText(context).apply {
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorDivider)
                cornerRadius = LayoutHelper.dp(24).toFloat()
            }
            hint = "О себе"
            setHintTextColor(DesignEngine.Theme.colorTextSecondary)
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            textSize = 16f
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(16))
        }
        card.addView(inputStatus, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        scrollContainer.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        val infoText = TextView(context).apply {
            text = "Имя и статус будут видны другим пользователям."
            gravity = Gravity.CENTER
            setTextColor(DesignEngine.Theme.colorTextSecondary)
            textSize = 12f
        }
        scrollContainer.addView(infoText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 16, 0, 32))

        scrollView.addView(scrollContainer)
        rootLayout.addView(scrollView)

        val prefs = context.getSharedPreferences("app", MODE_PRIVATE)
        inputName.setText(prefs.getString("display_name", ""))
        inputStatus.setText(prefs.getString("status", ""))

        avatarView.load("https://blagogram.ru/avatars/${prefs.getString("avatar", "")}") {
            placeholder(R.drawable.ic_user_placeholder)
            error(R.drawable.ic_user_placeholder)
        }

        coverView.load("https://blagogram.ru/covers/${prefs.getString("cover", "")}") {
            error(Color.LTGRAY)
        }

        return rootLayout
    }

    private fun handleSelectedAvatar(uri: Uri) {
        avatarView.load(uri)
        val path = uri.path ?: return
        avatarFile = compressImage(File(path))
    }

    private fun handleSelectedCover(uri: Uri) {
        coverView.load(uri)
        val path = uri.path ?: return
        coverFile = compressImage(File(path))
    }

    private fun compressImage(originalFile: File): File {
        val exif = androidx.exifinterface.media.ExifInterface(originalFile.absolutePath)
        val orientation = exif.getAttributeInt(
            androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
            androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
        )

        var bmp = BitmapFactory.decodeFile(originalFile.absolutePath) ?: return originalFile

        val matrix = android.graphics.Matrix()
        when (orientation) {
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }

        val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)

        val maxSize = 1200
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
        val compressedFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
        FileOutputStream(compressedFile).use { out ->
            resized.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        return compressedFile
    }

    private fun saveProfile() {
        val prefs = context.getSharedPreferences("app", MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""
        val name = inputName.text.toString().trim()
        val status = inputStatus.text.toString().trim()

        activity?.lifecycleScope?.launch(Dispatchers.IO) {
            try {
                val avatarPart = avatarFile?.let {
                    val req = it.asRequestBody("image/jpeg".toMediaType())
                    MultipartBody.Part.createFormData("avatar", it.name, req)
                }
                val coverPart = coverFile?.let {
                    val req = it.asRequestBody("image/jpeg".toMediaType())
                    MultipartBody.Part.createFormData("cover", it.name, req)
                }

                val response = repo.profileEditMultipart(token, name, status, avatarPart, coverPart)

                if (response["ok"] == true) {
                    val result = response["result"] as? Map<*, *> ?: response
                    prefs.edit().apply {
                        putString("display_name", name)
                        putString("status", status)
                        (result["avatar"] as? String)?.let { putString("avatar", it) }
                        (result["cover"] as? String)?.let { putString("cover", it) }
                        apply()
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Сохранено!", Toast.LENGTH_SHORT).show()
                        finishFragment()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}