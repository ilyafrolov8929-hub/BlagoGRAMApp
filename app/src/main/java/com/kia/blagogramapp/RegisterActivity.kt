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
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.imageview.ShapeableImageView
import com.kia.blagogramapp.api.ApiClient
import com.kia.blagogramapp.ui.modules.MediaPickerSheet
import com.kia.blagogramapp.utils.TimeUtils.normalizeRussianPhone
import com.kia.blagogramapp.views.BlurGradientBackgroundView
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class RegisterActivity : AppCompatActivity() {

    private lateinit var rootLayout: FrameLayout
    private lateinit var step1Layout: LinearLayout
    private lateinit var step2Layout: LinearLayout
    private lateinit var verificationLayout: LinearLayout
    
    private lateinit var phoneInp: EditText
    private lateinit var passwordInp: EditText
    private lateinit var confirmPasswordInp: EditText
    
    private lateinit var displayNameInp: EditText
    private lateinit var avatarView: ShapeableImageView
    private var selectedAvatarUri: Uri? = null

    private var verificationToken: String? = null
    
    private lateinit var statusView: TextView
    private lateinit var loadingView: ProgressBar
    private lateinit var btnConfirm: Button
    
    private val viewModel: RegisterViewModel by viewModels()

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
                when {
                    verificationLayout.visibility == View.VISIBLE -> {
                        viewModel.resetState()
                        showStep2()
                    }
                    step2Layout.visibility == View.VISIBLE -> showStep1()
                    else -> finish()
                }
            }
        }
        rootLayout.addView(backBtn, LayoutHelper.createFrame(48, 48, Gravity.TOP or Gravity.START, 16, 48, 0, 0))

        initStep1()
        initStep2()
        initVerificationStep()

        showStep1(false)
        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    Log.d("RegisterActivity", "New state: $state")
                    when (state) {
                        is VerificationState.Loading -> {
                            btnConfirm.isEnabled = false
                            btnConfirm.alpha = 0.5f
                            loadingView.visibility = View.VISIBLE
                            statusView.text = "Проверяем подтверждение..."
                        }
                        is VerificationState.Success -> {
                            Log.d("RegisterActivity", "Success! Transitioning...")
                            saveUserAndFinish(state.result)
                        }
                        is VerificationState.Error -> {
                            btnConfirm.isEnabled = true
                            btnConfirm.alpha = 1.0f
                            loadingView.visibility = View.GONE
                            statusView.text = state.message
                            Toast.makeText(this@RegisterActivity, state.message, Toast.LENGTH_SHORT).show()
                        }
                        is VerificationState.Idle -> {
                            btnConfirm.isEnabled = true
                            btnConfirm.alpha = 1.0f
                            loadingView.visibility = View.GONE
                            statusView.text = "Нажмите кнопку выше после подтверждения в боте"
                        }
                    }
                }
            }
        }
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
            text = "Введите ваш номер телефона"
            textSize = 17f
            setTextColor(Color.parseColor("#B3FFFFFF"))
            gravity = Gravity.CENTER
        }
        step1Layout.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 48))

        val (phoneBox, pInp) = createModernInput("+7 (___) ___-__-__", R.drawable.ic_user)
        phoneInp = pInp
        phoneInp.inputType = InputType.TYPE_CLASS_PHONE
        step1Layout.addView(phoneBox, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 16))

        val (passBox, pwInp) = createModernInput("Пароль", R.drawable.ic_edit, true)
        passwordInp = pwInp
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
            text = "Как вас называть?"
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

        val (nameBox, nInp) = createModernInput("Ваше имя", R.drawable.ic_user)
        displayNameInp = nInp
        step2Layout.addView(nameBox, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        val btnFinish = Button(this).apply {
            text = "Продолжить"
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
        step2Layout.addView(btnFinish, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 60, 0f, 0, 0, 48, 0, 24))

        rootLayout.addView(step2Layout, FrameLayout.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER))
    }

    private fun initVerificationStep() {
        verificationLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(LayoutHelper.dp(32), 0, LayoutHelper.dp(32), 0)
            visibility = View.GONE
        }

        val icon = ImageView(this).apply {
            setImageResource(R.drawable.ic_send)
            setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        }
        verificationLayout.addView(icon, LayoutHelper.createLinear(80, 80, 0f, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 24))

        val titleView = TextView(this).apply {
            text = "Верификация"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        verificationLayout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 16))

        val descView = TextView(this).apply {
            text = "Для завершения регистрации подтвердите ваш номер через нашего Telegram-бота"
            textSize = 16f
            setTextColor(Color.parseColor("#B3FFFFFF"))
            gravity = Gravity.CENTER
        }
        verificationLayout.addView(descView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 32))

        val btnOpenTG = Button(this).apply {
            text = "1. Подтвердить в Telegram"
            isAllCaps = false
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0088CC"))
                cornerRadius = LayoutHelper.dp(24).toFloat()
            }
            stateListAnimator = null
            setOnClickListener {
                openTelegramBot()
            }
        }
        verificationLayout.addView(btnOpenTG, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 60, 0f, 0, 0, 0, 0, 16))

        btnConfirm = Button(this).apply {
            text = "2. Я подтвердил"
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
                verificationToken?.let { token ->
                    viewModel.checkStatus(token)
                }
            }
        }
        verificationLayout.addView(btnConfirm, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 60, 0f, 0, 0, 0, 0, 16))

        loadingView = ProgressBar(this).apply {
            visibility = View.GONE
            indeterminateDrawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        }
        verificationLayout.addView(loadingView, LayoutHelper.createLinear(40, 40, 0f, Gravity.CENTER_HORIZONTAL))

        statusView = TextView(this).apply {
            text = "Нажмите кнопку выше после подтверждения в боте"
            textSize = 14f
            setTextColor(Color.parseColor("#80FFFFFF"))
            gravity = Gravity.CENTER
        }
        verificationLayout.addView(statusView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 8, 0, 0))

        val btnRetry = Button(this).apply {
            text = "Начать заново"
            isAllCaps = false
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#26FFFFFF"))
                cornerRadius = LayoutHelper.dp(24).toFloat()
                setStroke(LayoutHelper.dp(1), Color.WHITE)
            }
            stateListAnimator = null
            setOnClickListener {
                viewModel.resetState()
                showStep1()
            }
        }
        verificationLayout.addView(btnRetry, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 60, 0f, 0, 0, 32, 0, 0))

        rootLayout.addView(verificationLayout, FrameLayout.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER))
    }

    private fun showStep1(animate: Boolean = true) {
        switchLayout(step1Layout, animate)
    }

    private fun showStep2(animate: Boolean = true) {
        switchLayout(step2Layout, animate)
    }

    private fun showVerification(animate: Boolean = true) {
        switchLayout(verificationLayout, animate)
    }

    private fun switchLayout(newLayout: View, animate: Boolean) {
        val currentLayout = when {
            step1Layout.visibility == View.VISIBLE -> step1Layout
            step2Layout.visibility == View.VISIBLE -> step2Layout
            verificationLayout.visibility == View.VISIBLE -> verificationLayout
            else -> null
        }

        if (currentLayout == newLayout) return

        if (animate && currentLayout != null) {
            currentLayout.animate().alpha(0f).translationX(if (newLayout == step1Layout) 100f else -100f).setDuration(300).withEndAction {
                currentLayout.visibility = View.GONE
                newLayout.visibility = View.VISIBLE
                newLayout.alpha = 0f
                newLayout.translationX = if (newLayout == step1Layout) -100f else 100f
                newLayout.animate().alpha(1f).translationX(0f).setDuration(300).start()
            }.start()
        } else {
            step1Layout.visibility = if (newLayout == step1Layout) View.VISIBLE else View.GONE
            step2Layout.visibility = if (newLayout == step2Layout) View.VISIBLE else View.GONE
            verificationLayout.visibility = if (newLayout == verificationLayout) View.VISIBLE else View.GONE
            newLayout.alpha = 1f
            newLayout.translationX = 0f
        }
    }

    private fun validateStep1(): Boolean {
        val normalized = phoneInp.text.toString().trim()
        val p = passwordInp.text.toString().trim()
        val cp = confirmPasswordInp.text.toString().trim()

        val phone = normalizeRussianPhone(normalized)

        if (phone.toString().length < 10) {
            Toast.makeText(this, "Введите корректный номер телефона", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Введите ваше имя", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private var tgDeepLink: String? = null

    private fun performRegistration() {
        val phone = phoneInp.text.toString().trim()
        val p = passwordInp.text.toString().trim()
        val d = displayNameInp.text.toString().trim()

        lifecycleScope.launch {
            try {
                val response = ApiClient.api.registerPhone(
                    phone = phone,
                    password = p,
                    displayName = d
                )

                if (response.ok && response.result != null) {
                    if (response.result.status == "need_verification") {
                        tgDeepLink = response.result.deep_link
                        
                        // Пытаемся извлечь tg_verify из URL
                        val uri = Uri.parse(tgDeepLink)
                        val tgVerify = uri.getQueryParameter("tg_verify")
                        
                        // Используем tg_verify, если он есть, иначе verify_token из ответа, иначе режем reg_
                        verificationToken = tgVerify ?: response.result.verify_token ?: tgDeepLink?.substringAfter("reg_")
                        
                        Log.d("RegisterActivity", "Parsed Verification Token: $verificationToken")
                        
                        showVerification()
                        openTelegramBot()
                    } else if (response.result.token != null) {
                        saveUserAndFinish(response.result)
                    }
                } else {
                    Toast.makeText(this@RegisterActivity, response.error ?: "Ошибка регистрации", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@RegisterActivity, "Ошибка сети: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openTelegramBot() {
        tgDeepLink?.let { link ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Telegram не установлен", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUserAndFinish(result: com.kia.blagogramapp.api.LoginResult) {
        getSharedPreferences("app", MODE_PRIVATE).edit {
            putString("token", result.token)
            putInt("user_id", result.user_id ?: 0)
            putString("phone", result.phone)
            putString("avatar", result.avatar)
            putString("login", result.username)
            putString("display_name", result.display_name)
            putString("status", result.status)
            putString("cover", result.cover)
        }
        
        if (selectedAvatarUri != null) {
            uploadAvatarAndFinish()
        } else {
            Log.d("RegisterActivity", "Transitioning to ChatsActivity")
            val intent = Intent(this@RegisterActivity, ChatsActivity::class.java)
            startActivity(intent)
            finishAffinity()
        }
    }

    private fun uploadAvatarAndFinish() {
        val uri = selectedAvatarUri ?: return
        val token = getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: return
        
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val file = File(getPathFromUri(uri) ?: "")
                    if (file.exists()) {
                        val actionPart = "profile.edit".toRequestBody("text/plain".toMediaTypeOrNull())
                        val tokenPart = token.toRequestBody("text/plain".toMediaTypeOrNull())
                        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                        val avatarPart = MultipartBody.Part.createFormData("avatar", file.name, requestFile)

                        ApiClient.api.profileEditMultipart(
                            action = actionPart,
                            token = tokenPart,
                            displayName = null,
                            status = null,
                            avatar = avatarPart,
                            cover = null
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) {
                    Log.d("RegisterActivity", "Avatar uploaded, transitioning to ChatsActivity")
                    val intent = Intent(this@RegisterActivity, ChatsActivity::class.java)
                    startActivity(intent)
                    finishAffinity()
                }
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

    override fun onDestroy() {
        super.onDestroy()
    }
}
