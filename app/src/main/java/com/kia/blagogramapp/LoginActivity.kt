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
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.kia.blagogramapp.api.ApiClient
import com.kia.blagogramapp.views.BlurGradientBackgroundView
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

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

        val rootLayout = FrameLayout(this)
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
            setOnClickListener { finish() }
            alpha = 0f
        }
        val statusBarHeight = getStatusBarHeight()
        rootLayout.addView(backBtn, LayoutHelper.createFrame(48, 48, Gravity.TOP or Gravity.START, 16, 40, 0, 0))

        val centerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(LayoutHelper.dp(32), 0, LayoutHelper.dp(32), 0)
            alpha = 0f
            translationY = 40f
        }
        
        val titleView = TextView(this).apply {
            text = "С возвращением"
            textSize = 32f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            try { 
                typeface = ResourcesCompat.getFont(context, R.font.fira_sans_extrabold) 
            } catch(e: Exception) {
                typeface = Typeface.DEFAULT_BOLD
            }
        }
        centerLayout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 8))

        val subtitleView = TextView(this).apply {
            text = "Войдите в свой аккаунт"
            textSize = 16f
            setTextColor(Color.parseColor("#B3FFFFFF"))
            gravity = Gravity.CENTER
        }
        centerLayout.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 40))

        fun createModernInput(hintText: String, iconRes: Int, isPass: Boolean = false): Pair<LinearLayout, EditText> {
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#26FFFFFF"))
                    cornerRadius = LayoutHelper.dp(20).toFloat()
                    setStroke(LayoutHelper.dp(1), Color.parseColor("#33FFFFFF"))
                }
                setPadding(LayoutHelper.dp(18), 0, LayoutHelper.dp(18), 0)
            }

            val icon = ImageView(this).apply {
                setImageResource(iconRes)
                setColorFilter(Color.parseColor("#B3FFFFFF"), PorterDuff.Mode.SRC_IN)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
            }
            container.addView(icon, LayoutHelper.createLinear(22, 22, 0f, Gravity.CENTER_VERTICAL))

            val input = EditText(this).apply {
                hint = hintText
                inputType = if (isPass) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_CLASS_TEXT
                setTextColor(Color.WHITE)
                setHintTextColor(Color.parseColor("#80FFFFFF"))
                textSize = 17f
                background = null
                setPadding(LayoutHelper.dp(12), LayoutHelper.dp(18), LayoutHelper.dp(12), LayoutHelper.dp(18))
            }
            container.addView(input, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL))
            return container to input
        }

        val (loginBox, loginInp) = createModernInput("Логин", R.drawable.ic_user)
        centerLayout.addView(loginBox, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 16))

        val (passBox, passInp) = createModernInput("Пароль", R.drawable.ic_edit, true)
        centerLayout.addView(passBox, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        val btnLogin = Button(this).apply {
            text = "Войти"
            isAllCaps = false
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor("#8E2DE2"), Color.parseColor("#4A00E0"))
            ).apply {
                cornerRadius = LayoutHelper.dp(20).toFloat()
            }
            stateListAnimator = null
        }
        centerLayout.addView(btnLogin, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 60, 0f, 0, 0, 32, 0, 24))
        
        val registerLink = TextView(this).apply {
            text = "Нет аккаунта? Зарегистрироваться"
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(LayoutHelper.dp(10), LayoutHelper.dp(10), LayoutHelper.dp(10), LayoutHelper.dp(10))
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://blagogram.ru/register.php?fromApp=1")))
            }
        }
        centerLayout.addView(registerLink, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        rootLayout.addView(centerLayout, FrameLayout.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER))

        rootLayout.post {
            ObjectAnimator.ofFloat(backBtn, View.ALPHA, 1f).setDuration(400).start()
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(centerLayout, View.ALPHA, 1f),
                    ObjectAnimator.ofFloat(centerLayout, View.TRANSLATION_Y, 0f)
                )
                duration = 700
                interpolator = DecelerateInterpolator()
                start()
            }
        }

        btnLogin.setOnClickListener {
            val l = loginInp.text.toString().trim()
            val p = passInp.text.toString().trim()

            if (l.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, "Заполните поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            btnLogin.alpha = 0.5f

            lifecycleScope.launch {
                try {
                    val response = ApiClient.api.login(login = l, password = p)
                    if (response.ok && response.result != null) {
                        getSharedPreferences("app", MODE_PRIVATE).edit {
                            putString("token", response.result.token)
                            putInt("user_id", response.result.user_id ?: 0)
                            putString("avatar", response.result.avatar)
                            putString("login", response.result.username)
                            putString("display_name", response.result.display_name)
                            putString("status", response.result.status)
                        }
                        startActivity(Intent(this@LoginActivity, ChatsActivity::class.java))
                        finishAffinity()
                    } else {
                        Toast.makeText(this@LoginActivity, response.error ?: "Ошибка", Toast.LENGTH_LONG).show()
                        btnLogin.isEnabled = true
                        btnLogin.alpha = 1f
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, "Ошибка сети", Toast.LENGTH_SHORT).show()
                    btnLogin.isEnabled = true
                    btnLogin.alpha = 1f
                }
            }
        }
    }

    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) result = resources.getDimensionPixelSize(resourceId)
        return result
    }
}
