package com.kia.blagogramapp

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import coil.load
import com.google.android.material.imageview.ShapeableImageView
import com.kia.blagogramapp.api.ApiRepository
import com.kia.blagogramapp.network.RetrofitClient
import com.kia.blagogramapp.ui.modules.MediaPickerSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class ProfileEditActivity : AppCompatActivity() {

    private lateinit var inputName: EditText
    private lateinit var inputStatus: EditText
    private var isCurrentThemeDark: Boolean = false
    private var isCurrentUseMonet: Boolean = false
    private lateinit var avatarView: ShapeableImageView

    private var avatarFile: File? = null
    private var avatarFileName: String? = null

    private var repo = ApiRepository(RetrofitClient.api)

    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DesignEngine.init(this)
        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        val isDarkTheme = themePrefs.getBoolean("is_dark_theme", false)
        val useMonet = themePrefs.getBoolean("use_monet", false)

        isCurrentThemeDark = isDarkTheme
        isCurrentUseMonet = useMonet
        DesignEngine.Theme.apply(isDarkTheme, useMonet)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val statusBarHeight = getStatusBarHeight()

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = !isDarkTheme
        controller.isAppearanceLightNavigationBars = !isDarkTheme

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(DesignEngine.Theme.colorBackground)
        }
        setContentView(rootLayout)

        val toolbar = DesignEngine.createToolBar(
            context = this,
            title = "Редактирование",
            leftButtonIconRes = R.drawable.ic_back,
            isDark = isDarkTheme,
            leftButtonClickListener = { finish() },
            rightButtonIconRes = R.drawable.ic_check,
            rightButtonClickListener = { saveProfile() }
        )
        rootLayout.addView(toolbar, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP))


        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
        }
        
        val scrollContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(LayoutHelper.dp(24), LayoutHelper.dp(24), LayoutHelper.dp(24), LayoutHelper.dp(24))
        }

        val avatarContainer = FrameLayout(this)
        
        avatarView = ShapeableImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.LTGRAY)
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(LayoutHelper.dp(60).toFloat())
                .build()
        }
        avatarContainer.addView(avatarView, LayoutHelper.createFrame(120, 120, Gravity.CENTER))

        val editAvatarBtn = ImageView(this).apply {
            setImageResource(R.drawable.ic_edit)
            setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            setPadding(LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8))
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorBlue)
                shape = GradientDrawable.OVAL
            }
            setOnClickListener {
                MediaPickerSheet.show(this@ProfileEditActivity, MediaPickerSheet.Mode.AVATAR) { uris, _ ->
                    if (uris.isNotEmpty()) {
                        handleSelectedAvatar(uris[0])
                    }
                }
            }
        }
        
        avatarContainer.addView(editAvatarBtn, LayoutHelper.createFrame(40, 40, Gravity.BOTTOM or Gravity.END))
        scrollContainer.addView(avatarContainer, LayoutHelper.createLinear(120, 120, 0f, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 32))

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorCardBackground)
                cornerRadius = LayoutHelper.dp(16).toFloat()
            }
            setPadding(LayoutHelper.dp(20), LayoutHelper.dp(20), LayoutHelper.dp(20), LayoutHelper.dp(20))
            elevation = 0f 
        }

        card.addView(TextView(this).apply {
            text = "ИМЯ"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(DesignEngine.Theme.colorTextSecondary)
        }, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 8))

        inputName = EditText(this).apply {
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

        card.addView(TextView(this).apply {
            text = "СТАТУС"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(DesignEngine.Theme.colorTextSecondary)
        }, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 20, 0, 8))

        inputStatus = EditText(this).apply {
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

        val infoText = TextView(this).apply {
            text = "Имя и статус будут видны другим пользователям."
            gravity = Gravity.CENTER
            setTextColor(DesignEngine.Theme.colorTextSecondary)
            textSize = 12f
        }
        scrollContainer.addView(infoText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 16, 0, 32))

        scrollView.addView(scrollContainer)
        rootLayout.addView(scrollView)

        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        inputName.setText(prefs.getString("display_name", ""))
        inputStatus.setText(prefs.getString("status", ""))
        avatarView.load("https://blagogram.ru/avatars/${prefs.getString("avatar", "")}") {
            placeholder(R.drawable.ic_user_placeholder)
            error(R.drawable.ic_user_placeholder)
        }
    }

    private fun handleSelectedAvatar(uri: Uri) {
        avatarView.load(uri)
        val path = uri.path ?: return
        val originalFile = File(path)
        
        // Since MediaPickerSheet already returned a cropped and somewhat compressed file,
        // we just need to ensure it's optimized for upload.
        val compressed = compressImage(originalFile)
        avatarFile = compressed
        avatarFileName = compressed.name
    }

    private fun compressImage(originalFile: File): File {
        val exif = androidx.exifinterface.media.ExifInterface(originalFile.absolutePath)
        val orientation = exif.getAttributeInt(
            androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
            androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
        )

        var bmp = BitmapFactory.decodeFile(originalFile.absolutePath)
        if (bmp == null) return originalFile

        val matrix = android.graphics.Matrix()
        when (orientation) {
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }

        val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        
        val maxSize = 1080
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
        val compressedFile = File(cacheDir, "upload_${System.currentTimeMillis()}.jpg")
        FileOutputStream(compressedFile).use { out ->
            resized.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        return compressedFile
    }

    private fun saveProfile() {
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""
        val name = inputName.text.toString().trim()
        val status = inputStatus.text.toString().trim()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val avatarLocal = avatarFile
                val response = if (avatarLocal != null) {
                    val req = avatarLocal.asRequestBody("image/jpeg".toMediaType())
                    val part = MultipartBody.Part.createFormData("avatar", avatarLocal.name, req)
                    repo.profileEditMultipart(token, name, status, part)
                } else {
                    repo.profileEdit(token, name, status)
                }

                if (response["ok"] == true) {
                    val result = response["result"] as Map<*, *>
                    prefs.edit().putString("display_name", name).putString("status", status).apply()
                    val avatarFromServer = result["avatar"]?.toString()
                    if (avatarFromServer != null) prefs.edit().putString("avatar", avatarFromServer).apply()
                    runOnUiThread {
                        Toast.makeText(this@ProfileEditActivity, "Сохранено!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@ProfileEditActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        val isNowDark = themePrefs.getBoolean("is_dark_theme", false)
        val isNowMonet = themePrefs.getBoolean("use_monet", false)
        if (isNowDark != isCurrentThemeDark || isNowMonet != isCurrentUseMonet) {
            recreate()
            return
        }
    }
}
