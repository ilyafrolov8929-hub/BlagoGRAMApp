package com.kia.blagogramapp.SingleActivity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import com.kia.blagogramapp.BaseScreen
import com.kia.blagogramapp.LayoutHelper
import java.util.*

class NavigationLayout(context: Context) : FrameLayout(context) {
    private val fragmentsStack = Stack<BaseScreen>()
    private var isAnimating = false

    fun presentFragment(fragment: BaseScreen) {
        if (isAnimating) return

        hideKeyboard()

        val width = width.toFloat()
        fragment.parentLayout = this
        val newView = fragment.createView()
        fragment.fragmentView = newView

        // Добавляем новый экран поверх всех
        addView(newView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))

        if (fragmentsStack.isNotEmpty()) {
            isAnimating = true
            val oldScreen = fragmentsStack.peek()
            val oldView = oldScreen.fragmentView

            // Начальное положение нового экрана — справа за пределами
            newView.translationX = width

            // Анимация наложения
            val newAnim = ObjectAnimator.ofFloat(newView, View.TRANSLATION_X, 0f)
            // Старый экран чуть-чуть уходит влево (эффект параллакса)
            val oldAnim = ObjectAnimator.ofFloat(oldView, View.TRANSLATION_X, -width / 3f)

            newAnim.duration = 300
            oldAnim.duration = 300
            newAnim.interpolator = DecelerateInterpolator(1.5f)

            newAnim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    oldView.visibility = View.GONE
                    isAnimating = false
                    fragment.onResume()
                }
            })

            oldScreen.onPause()
            newAnim.start()
            oldAnim.start()
        } else {
            fragment.onResume()
        }

        fragmentsStack.push(fragment)
    }

    fun closeLastFragment(): Boolean {
        if (fragmentsStack.size <= 1 || isAnimating) return false

        hideKeyboard()

        isAnimating = true
        val width = width.toFloat()
        val currentScreen = fragmentsStack.pop()
        val currentView = currentScreen.fragmentView
        val prevScreen = fragmentsStack.peek()

        // Если экран помечен как "грязный" — пересоздаем его перед показом
        if (prevScreen.isThemeDirty) {
            val oldView = prevScreen.fragmentView
            val index = indexOfChild(oldView)
            val newView = prevScreen.createView()
            prevScreen.fragmentView = newView
            prevScreen.isThemeDirty = false
            removeViewAt(index)
            addView(newView, index, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))
        }

        val prevView = prevScreen.fragmentView
        prevView.visibility = VISIBLE

        val currentAnim = ObjectAnimator.ofFloat(currentView, View.TRANSLATION_X, width)
        val prevAnim = ObjectAnimator.ofFloat(prevView, View.TRANSLATION_X, 0f)

        currentAnim.duration = 250
        prevAnim.duration = 250
        currentAnim.interpolator = DecelerateInterpolator(1.5f)

        currentAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                currentScreen.onPause()
                currentScreen.onFragmentDestroy()
                removeView(currentView)
                prevScreen.onResume()
                isAnimating = false
            }
        })

        currentAnim.start()
        prevAnim.start()

        return true
    }

    // 1. Метод для плавного обновления текущего экрана
    fun refreshTopScreenWithFade() {
        if (isAnimating || fragmentsStack.isEmpty()) return
        isAnimating = true

        val currentScreen = fragmentsStack.peek()
        val oldView = currentScreen.fragmentView

        // Пересоздаем View этого же экрана с новыми цветами
        val newView = currentScreen.createView()
        currentScreen.fragmentView = newView
        newView.alpha = 0f

        addView(newView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))

        newView.animate().alpha(1f).setDuration(300).start()
        oldView.animate().alpha(0f).setDuration(300).setListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                removeView(oldView)
                isAnimating = false
                currentScreen.isThemeDirty = false
                currentScreen.onResume()
            }
        }).start()
    }

    // 2. Метод пометки всех экранов как "грязных"
    fun updateThemeInStack() {
        fragmentsStack.forEach { it.isThemeDirty = true }
    }

    fun clearStack(newFragment: BaseScreen) {
        if (isAnimating) return
        
        // Очищаем текущие View и вызываем деструкторы для всех экранов в стеке
        while (fragmentsStack.isNotEmpty()) {
            val fragment = fragmentsStack.pop()
            fragment.onPause()
            fragment.onFragmentDestroy()
            removeView(fragment.fragmentView)
        }
        
        // Сбрасываем все параметры анимации (на случай если что-то зависло)
        isAnimating = false
        
        // Добавляем новый экран как единственный
        presentFragment(newFragment)
    }

    fun onBackPressed(): Boolean {
        return closeLastFragment()
    }

    fun getVisibleScreen(): BaseScreen? {
        return if (fragmentsStack.isNotEmpty()) fragmentsStack.peek() else null
    }

    private fun hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        // Скрываем клавиатуру, используя окно текущего NavigationLayout
        imm?.hideSoftInputFromWindow(windowToken, 0)
        // Снимаем фокус с любого элемента, чтобы при возврате клавиатура не прыгала
        findFocus()?.clearFocus()
    }

}
