package com.kia.blagogramapp.ui.screens

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.kia.blagogramapp.*
import com.kia.blagogramapp.views.BlurGradientBackgroundView

class StartScreen(context: Context) : BaseScreen(context) {

    override fun createView(): View {
        val rootLayout = FrameLayout(context)

        // Фон
        rootLayout.addView(BlurGradientBackgroundView(context), LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))

        val centerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(LayoutHelper.dp(32), 0, LayoutHelper.dp(32), 0)
        }

        // Логотип
        val logoView = ImageView(context).apply {
            setImageResource(R.mipmap.ic_launcher)
            alpha = 0f
            scaleX = 0.4f
            scaleY = 0.4f
        }
        centerLayout.addView(logoView, LayoutHelper.createLinear(160, 160, 0f, Gravity.CENTER, 0, 0, 0, 32))

        // Название Lume
        val titleView = com.kia.blagogramapp.views.GradientTextView(context).apply {
            text = "Lume"
            textSize = 56f
            alpha = 0f
            translationY = 100f
            typeface = try { ResourcesCompat.getFont(context, R.font.fira_sans_extrabold) } catch(e:Exception) { Typeface.DEFAULT_BOLD }
            gravity = Gravity.CENTER
        }
        centerLayout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, Gravity.CENTER, 0, 0, 0, 8))

        // Кнопка входа
        val buttonWrapper = FrameLayout(context).apply {
            alpha = 0f
            translationY = 500f
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Color.parseColor("#8E2DE2"), Color.parseColor("#4A00E0"))).apply {
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

            setOnClickListener {
                // ПЕРЕХОД: Вместо startActivity вызываем presentFragment
                presentFragment(LoginScreen(context))
            }
        }
        centerLayout.addView(buttonWrapper, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        rootLayout.addView(centerLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER))

        // Запуск анимаций
        rootLayout.post { startAnimations(logoView, titleView, buttonWrapper, centerLayout) }

        return rootLayout
    }

    private fun startAnimations(logo: View, title: View, btn: View, center: View) {
        val logoAnim = AnimatorSet().apply {
            playTogether(ObjectAnimator.ofFloat(logo, View.ALPHA, 1f), ObjectAnimator.ofFloat(logo, View.SCALE_X, 1f), ObjectAnimator.ofFloat(logo, View.SCALE_Y, 1f))
            duration = 1100
            interpolator = OvershootInterpolator(1.4f)
        }

        val btnAnim = AnimatorSet().apply {
            playTogether(ObjectAnimator.ofFloat(btn, View.ALPHA, 1f), ObjectAnimator.ofFloat(btn, View.TRANSLATION_Y, 0f))
            duration = 1200
            startDelay = 800
            interpolator = OvershootInterpolator(1.1f)
        }

        AnimatorSet().apply {
            playTogether(logoAnim, btnAnim, ObjectAnimator.ofFloat(title, View.ALPHA, 1f).setDuration(900), ObjectAnimator.ofFloat(title, View.TRANSLATION_Y, 0f).setDuration(900))
            start()
        }
    }
}