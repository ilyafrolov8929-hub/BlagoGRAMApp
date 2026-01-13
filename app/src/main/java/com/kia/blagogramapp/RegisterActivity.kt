package com.kia.blagogramapp

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.imageview.ShapeableImageView
import com.kia.blagogramapp.api.ApiClient
import com.kia.blagogramapp.ui.modules.MediaPickerSheet
import com.kia.blagogramapp.views.BlurGradientBackgroundView
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class RegisterActivity : AppCompatActivity() {

    private lateinit var rootLayout: FrameLayout
    private lateinit var step1Layout: LinearLayout
    private lateinit var step2Layout: LinearLayout
    
    private lateinit var usernameInp: EditText
    private lateinit var passwordInp: EditText
    private lateinit var confirmPasswordInp: EditText
    
    private lateinit var displayNameInp: EditText
    private lateinit var avatarView: ShapeableImageView
    private var selectedAvatarUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DesignEngine.init(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        rootLayout = FrameLayout(this)
        rootLayout.addView(BlurGradientBackgroundView(this), LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(0, 0, 0, insets.bottom)
            windowInsets
        }

        setContentView(rootLayout)

        // Кнопка назад
        val backBtn = ImageView(this).apply {
            setImageResource(R.drawable.ic_back)
            setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(12), LayoutHelper.dp(12), LayoutHelper.dp(12))
            val outValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
            setBackgroundResource(outValue.resourceId)
            setOnClickListener { 
                if (step2Layout.visibility == View.VISIBLE) {
                    showStep1()
                } else {
                    finish()
                }
            }
        }
        rootLayout.addView(backBtn, LayoutHelper.createFrame(48, 48, Gravity.TOP or Gravity.START, 16, 48, 0, 0))

        initStep1()
        initStep2()

        showStep1(false)
    }

    private fun initStep1() {
        step1Layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(LayoutHelper.dp(32), 0, LayoutHelper.dp(32), 0)
            visibility = View.GONE
        }

        val titleView = TextView(this).apply {
            text = "Регистрация"
            textSize = 36f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            try { 
                typeface = ResourcesCompat.getFont(context, R.font.fira_sans_extrabold) 
            } catch(e: Exception) {
                typeface = Typeface.DEFAULT_BOLD
            }
        }
        step1Layout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 8))

        val subtitleView = TextView(this).apply {
            text = "Создайте новый аккаунт"
            textSize = 17f
            setTextColor(Color.parseColor("#B3FFFFFF"))
            gravity = Gravity.CENTER
        }
        step1Layout.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 48))

        val (userBox, uInp) = createModernInput("Имя пользователя (логин)", R.drawable.ic_user)
        usernameInp = uInp
        step1Layout.addView(userBox, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 16))

        val (passBox, pInp) = createModernInput("Пароль", R.drawable.ic_edit, true)
        passwordInp = pInp
        step1Layout.addView(passBox, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 16))

        val (confirmBox, cpInp) = createModernInput("Повторите пароль", R.drawable.ic_edit, true)
        confirmPasswordInp = cpInp
        step1Layout.addView(confirmBox, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        val btnNext = Button(this).apply {
            text = "Далее"
            isAllCaps = false
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor("#8E2DE2"), Color.parseColor("#4A00E0"))
            ).apply {
                cornerRadius = LayoutHelper.dp(24).toFloat()
            }
            stateListAnimator = null
            setOnClickListener {
                if (validateStep1()) {
                    showStep2()
                }
            }
        }
        // Исправлено: 0 (left), 32 (top), 0 (right), 24 (bottom)
        step1Layout.addView(btnNext, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 60, 0f, 0, 0, 32, 0, 24))

        rootLayout.addView(step1Layout, FrameLayout.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER))
    }

    private fun initStep2() {
        step2Layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(LayoutHelper.dp(32), 0, LayoutHelper.dp(32), 0)
            visibility = View.GONE
        }

        val titleView = TextView(this).apply {
            text = "Профиль"
            textSize = 36f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            try { 
                typeface = ResourcesCompat.getFont(context, R.font.fira_sans_extrabold) 
            } catch(e: Exception) {
                typeface = Typeface.DEFAULT_BOLD
            }
        }
        step2Layout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 8))

        val subtitleView = TextView(this).apply {
            text = "Расскажите о себе"
            textSize = 17f
            setTextColor(Color.parseColor("#B3FFFFFF"))
            gravity = Gravity.CENTER
        }
        step2Layout.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 48))

        val avatarContainer = FrameLayout(this).apply {
            layoutParams = LayoutHelper.createLinear(110, 110, 0f, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 32)
        }
        avatarView = ShapeableImageView(this).apply {
            layoutParams = LayoutHelper.createLinear(100, 100, 0f, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 24)
            shapeAppearanceModel = shapeAppearanceModel.toBuilder().setAllCornerSizes(LayoutHelper.dp(50).toFloat()).build()
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageResource(R.drawable.ic_user_placeholder)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#26FFFFFF"))
                setStroke(LayoutHelper.dp(2), Color.WHITE)
            }
        }
        avatarContainer.addView(avatarView)
        
        val addIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_camera)
            setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(DesignEngine.Theme.colorBlue)
                setStroke(LayoutHelper.dp(2), Color.WHITE)
            }
            setPadding(LayoutHelper.dp(6), LayoutHelper.dp(6), LayoutHelper.dp(6), LayoutHelper.dp(6))
        }
        avatarContainer.addView(addIcon, LayoutHelper.createFrame(32, 32, Gravity.BOTTOM or Gravity.END))
        
        avatarContainer.setOnClickListener {
            MediaPickerSheet.show(this@RegisterActivity, MediaPickerSheet.Mode.AVATAR) { uris, _ ->
                if (uris.isNotEmpty()) {
                    selectedAvatarUri = uris[0]
                    avatarView.setImageURI(selectedAvatarUri)
                }
            }
        }
        step2Layout.addView(avatarContainer)

        val (nameBox, nInp) = createModernInput("Отображаемое имя", R.drawable.ic_user)
        displayNameInp = nInp
        step2Layout.addView(nameBox, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        val btnFinish = Button(this).apply {
            text = "Зарегистрироваться"
            isAllCaps = false
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor("#8E2DE2"), Color.parseColor("#4A00E0"))
            ).apply {
                cornerRadius = LayoutHelper.dp(24).toFloat()
            }
            stateListAnimator = null
            setOnClickListener {
                if (validateStep2()) {
                    performRegistration()
                }
            }
        }
        // Исправлено: 0 (left), 48 (top), 0 (right), 24 (bottom)
        step2Layout.addView(btnFinish, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 60, 0f, 0, 0, 48, 0, 24))

        rootLayout.addView(step2Layout, FrameLayout.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER))
    }

    private fun showStep1(animate: Boolean = true) {
        if (animate) {
            step2Layout.animate().alpha(0f).translationX(100f).setDuration(300).withEndAction {
                step2Layout.visibility = View.GONE
                step1Layout.visibility = View.VISIBLE
                step1Layout.alpha = 0f
                step1Layout.translationX = -100f
                step1Layout.animate().alpha(1f).translationX(0f).setDuration(300).start()
            }.start()
        } else {
            step2Layout.visibility = View.GONE
            step1Layout.visibility = View.VISIBLE
            step1Layout.alpha = 1f
            step1Layout.translationX = 0f
        }
    }

    private fun showStep2() {
        step1Layout.animate().alpha(0f).translationX(-100f).setDuration(300).withEndAction {
            step1Layout.visibility = View.GONE
            step2Layout.visibility = View.VISIBLE
            step2Layout.alpha = 0f
            step2Layout.translationX = 100f
            step2Layout.animate().alpha(1f).translationX(0f).setDuration(300).start()
        }.start()
    }

    private fun validateStep1(): Boolean {
        val u = usernameInp.text.toString().trim()
        val p = passwordInp.text.toString().trim()
        val cp = confirmPasswordInp.text.toString().trim()

        if (u.length < 3) {
            Toast.makeText(this, "Логин должен быть не менее 3 символов", Toast.LENGTH_SHORT).show()
            return false
        }
        if (p.length < 6) {
            Toast.makeText(this, "Пароль должен быть не менее 6 символов", Toast.LENGTH_SHORT).show()
            return false
        }
        if (p != cp) {
            Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun validateStep2(): Boolean {
        val d = displayNameInp.text.toString().trim()
        if (d.isEmpty()) {
            Toast.makeText(this, "Введите отображаемое имя", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun performRegistration() {
        val u = usernameInp.text.toString().trim()
        val p = passwordInp.text.toString().trim()
        val d = displayNameInp.text.toString().trim()

        lifecycleScope.launch {
            try {
                val actionPart = "auth.register".toRequestBody("text/plain".toMediaTypeOrNull())
                val userPart = u.toRequestBody("text/plain".toMediaTypeOrNull())
                val passPart = p.toRequestBody("text/plain".toMediaTypeOrNull())
                val displayPart = d.toRequestBody("text/plain".toMediaTypeOrNull())
                
                var avatarPart: MultipartBody.Part? = null
                selectedAvatarUri?.let { uri ->
                    val file = File(getPathFromUri(uri) ?: "")
                    if (file.exists()) {
                        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                        avatarPart = MultipartBody.Part.createFormData("avatar", file.name, requestFile)
                    }
                }

                val response = ApiClient.api.register(
                    action = actionPart,
                    username = userPart,
                    password = passPart,
                    displayName = displayPart,
                    avatar = avatarPart
                )

                if (response.ok && response.result != null) {
                    getSharedPreferences("app", MODE_PRIVATE).edit {
                        putString("token", response.result.token)
                        putInt("user_id", response.result.user_id ?: 0)
                        putString("avatar", response.result.avatar)
                        putString("login", response.result.username)
                        putString("display_name", response.result.display_name)
                        putString("status", response.result.status)
                        putString("cover", response.result.cover)
                    }
                    startActivity(Intent(this@RegisterActivity, ChatsActivity::class.java))
                    finishAffinity()
                } else {
                    Toast.makeText(this@RegisterActivity, response.error ?: "Ошибка регистрации", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@RegisterActivity, "Ошибка сети: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getPathFromUri(uri: Uri): String? {
        if (uri.scheme == "file") return uri.path
        val projection = arrayOf(android.provider.MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri, projection, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.DATA)
                it.getString(columnIndex)
            } else null
        }
    }

    private fun createModernInput(hintText: String, iconRes: Int, isPass: Boolean = false): Pair<LinearLayout, EditText> {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#26FFFFFF"))
                cornerRadius = LayoutHelper.dp(20).toFloat()
                setStroke(LayoutHelper.dp(1), Color.parseColor("#33FFFFFF"))
            }
            setPadding(LayoutHelper.dp(16), 0, LayoutHelper.dp(16), 0)
        }

        val icon = ImageView(this).apply {
            setImageResource(iconRes)
            setColorFilter(Color.parseColor("#B3FFFFFF"), PorterDuff.Mode.SRC_IN)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        container.addView(icon, LayoutHelper.createLinear(20, 20, 0f, Gravity.CENTER_VERTICAL))

        val input = EditText(this).apply {
            hint = hintText
            inputType = if (isPass) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_CLASS_TEXT
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#80FFFFFF"))
            textSize = 16f
            background = null
            setSingleLine(true)
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(16), LayoutHelper.dp(12), LayoutHelper.dp(16))
        }
        container.addView(input, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL))
        return container to input
    }
}
