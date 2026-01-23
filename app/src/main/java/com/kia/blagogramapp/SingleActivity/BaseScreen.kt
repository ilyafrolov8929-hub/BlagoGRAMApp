package com.kia.blagogramapp

import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.kia.blagogramapp.SingleActivity.NavigationLayout

abstract class BaseScreen(val context: Context) {
    var parentLayout: NavigationLayout? = null
    lateinit var fragmentView: View

    var isThemeDirty = false

    // Вызывается для создания интерфейса
    abstract fun createView(): View

    // Жизненный цикл
    open fun onResume() {}
    open fun onPause() {}
    open fun onFragmentDestroy() {}

    // В BaseScreen.kt
    open fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {}

    // Вспомогательный метод для навигации
    fun presentFragment(fragment: BaseScreen) {
        parentLayout?.presentFragment(fragment)
    }

    fun finishFragment() {
        parentLayout?.closeLastFragment()
    }
}