package com.kia.blagogramapp

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import com.kia.blagogramapp.views.BlurGradientBackgroundView

class StartActivity : AppCompatActivity() {
    private var isCurrentThemeDark: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Инициализация тем и Edge-to-Edge
        DesignEngine.init(this)
        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        isCurrentThemeDark = themePrefs.getBoolean("is_dark_theme", false)
        DesignEngine.Theme.apply(isCurrentThemeDark, themePrefs.getBoolean("use_monet", false))
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        // 2. Проверка токена
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        if (!prefs.getString("token", "").isNullOrEmpty()) {
            startActivity(Intent(this, ChatsActivity::class.java))
            finish()
            return
        }
        
        // 3. UI
        val rootLayout = FrameLayout(this)
        rootLayout.addView(BlurGradientBackgroundView(this), LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))
        
        val centerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(LayoutHelper.dp(32), 0, LayoutHelper.dp(32), 0)
        }
        
        // Логотип (теперь R.mipmap.ic_launcher)
        val logoView = ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher)
            alpha = 0f
            scaleX = 0.4f
            scaleY = 0.4f
        }
        centerLayout.addView(logoView, LayoutHelper.createLinear(160, 160, 0f, Gravity.CENTER, 0, 0, 0, 32))
        
        // Название (новый шрифт и размер)
        val titleView = com.kia.blagogramapp.views.GradientTextView(this).apply {
            text = "Lume"
            textSize = 56f
            alpha = 0f
            translationY = 100f
            try { 
                typeface = ResourcesCompat.getFont(context, R.font.fira_sans_extrabold) 
            } catch(e:Exception) {
                typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            }
            gravity = Gravity.CENTER
            letterSpacing = -0.02f // Плотный современный заголовок
        }
        centerLayout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, Gravity.CENTER, 0, 0, 0, 8))
        
        val subtitleView = TextView(this).apply {
            text = "Твое общение. Твои правила."
            textSize = 17f
            setTextColor(Color.parseColor("#B3FFFFFF"))
            alpha = 0f
            translationY = 50f
        }
        centerLayout.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, Gravity.CENTER, 0, 0, 0, 100))
        
        // Кнопка: Появляется "из-под низа" экрана
        val buttonWrapper = FrameLayout(this).apply {
            alpha = 0f
            // Ставим начальное смещение за пределы экрана (снизу)
            translationY = 500f 
            
            // Фон: Глубокий фиолетово-розовый градиент
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor("#8E2DE2"), Color.parseColor("#4A00E0"))
            ).apply {
                cornerRadius = LayoutHelper.dp(30).toFloat()
            }
            
            setPadding(LayoutHelper.dp(32), LayoutHelper.dp(18), LayoutHelper.dp(32), LayoutHelper.dp(18))
            
            val btnText = TextView(context).apply {
                text = "Присоединиться"
                setTextColor(Color.WHITE)
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
            }
            addView(btnText, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER))

            setOnClickListener { view ->
                view.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).withEndAction {
                    view.animate().scaleX(1f).scaleY(1f).setDuration(80).withEndAction {
                        val intent = Intent(this@StartActivity, LoginActivity::class.java)
                        val options = ActivityOptionsCompat.makeCustomAnimation(this@StartActivity, android.R.anim.fade_in, android.R.anim.fade_out)
                        startActivity(intent, options.toBundle())
                    }
                }
            }
        }
        centerLayout.addView(buttonWrapper, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        
        rootLayout.addView(centerLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER))
        
        val copyright = TextView(this).apply {
            text = "© 2025 BlagoGRAM Messenger"
            textSize = 13f
            setTextColor(Color.parseColor("#4DFFFFFF"))
            alpha = 0f
        }
        rootLayout.addView(copyright, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 0, 0, 50))
        
        setContentView(rootLayout)

        // АНИМАЦИИ
        rootLayout.post {
            // Логотип: Мягкое появление из прозрачности
            val logoAnim = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(logoView, View.ALPHA, 1f),
                    ObjectAnimator.ofFloat(logoView, View.SCALE_X, 1f),
                    ObjectAnimator.ofFloat(logoView, View.SCALE_Y, 1f)
                )
                duration = 1100
                interpolator = OvershootInterpolator(1.4f)
            }
            
            // Заголовок и подзаголовок: Вылет снизу
            fun ent(v: View, d: Long, dist: Float = 100f) = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(v, View.ALPHA, 1f), 
                    ObjectAnimator.ofFloat(v, View.TRANSLATION_Y, 0f)
                )
                duration = 900
                startDelay = d
                interpolator = DecelerateInterpolator()
            }

            // Кнопка: Мощный вылет снизу с эффектом инерции
            val btnAnim = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(buttonWrapper, View.ALPHA, 1f),
                    ObjectAnimator.ofFloat(buttonWrapper, View.TRANSLATION_Y, 0f)
                )
                duration = 1200
                startDelay = 800
                interpolator = OvershootInterpolator(1.1f) // Мягкий отскок в конце
            }

            AnimatorSet().apply {
                playTogether(
                    logoAnim, 
                    ent(titleView, 400), 
                    ent(subtitleView, 600), 
                    btnAnim,
                    ObjectAnimator.ofFloat(copyright, View.ALPHA, 1f).apply { duration = 1000; startDelay = 1400 }
                )
                start()
            }
            
            // "Плавающий" эффект для всего центрального блока
            ObjectAnimator.ofFloat(centerLayout, View.TRANSLATION_Y, -15f, 15f).apply {
                duration = 4000
                repeatMode = ObjectAnimator.REVERSE
                repeatCount = ObjectAnimator.INFINITE
                interpolator = AccelerateInterpolator()
                start()
            }
        }
    }
}
