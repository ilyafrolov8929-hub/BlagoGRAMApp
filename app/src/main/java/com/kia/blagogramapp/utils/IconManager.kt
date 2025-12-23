package com.kia.blagogramapp.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.kia.blagogramapp.BuildConfig
import com.kia.blagogramapp.R
import java.util.Calendar

object IconManager {

    private const val DEFAULT_ALIAS = "com.kia.blagogramapp.LauncherDefault"
    private const val WINTER_ALIAS = "com.kia.blagogramapp.LauncherWinter"
    private const val SUMMER_ALIAS = "com.kia.blagogramapp.LauncherSummer"
    private const val MART8_ALIAS = "com.kia.blagogramapp.Launcher8Mart"

    fun updateIconIfNeeded(context: Context) {
        // В режиме отладки отключаем автоматическую смену, 
        // чтобы не ломать запуск из Android Studio (ошибка "Activity class does not exist")
        if (BuildConfig.DEBUG) {
            // Убеждаемся, что дефолтный алиас включен для отладки
            if (getCurrentEnabledAlias(context) != DEFAULT_ALIAS) {
                enableAlias(context, DEFAULT_ALIAS)
            }
            return
        }

        val targetAlias = getTargetAlias()
        val currentAlias = getCurrentEnabledAlias(context)

        if (targetAlias != currentAlias) {
            enableAlias(context, targetAlias)
        }
    }

    fun getSeasonalIconRes(): Int {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        return when {
            month == Calendar.MARCH && day == 8 -> R.mipmap.ic_launcher_8mart
            month == Calendar.DECEMBER || month == Calendar.JANUARY || month == Calendar.FEBRUARY -> R.mipmap.ic_launcher_winter
            month == Calendar.JUNE || month == Calendar.JULY || month == Calendar.AUGUST -> R.mipmap.ic_launcher_sumer
            else -> R.mipmap.ic_launcher
        }
    }

    private fun getTargetAlias(): String {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        return when {
            month == Calendar.MARCH && day == 8 -> MART8_ALIAS
            month == Calendar.DECEMBER || month == Calendar.JANUARY || month == Calendar.FEBRUARY -> WINTER_ALIAS
            month == Calendar.JUNE || month == Calendar.JULY || month == Calendar.AUGUST -> SUMMER_ALIAS
            else -> DEFAULT_ALIAS
        }
    }

    private fun getCurrentEnabledAlias(context: Context): String? {
        val pm = context.packageManager
        val aliases = listOf(DEFAULT_ALIAS, WINTER_ALIAS, SUMMER_ALIAS, MART8_ALIAS)
        return aliases.find { 
            pm.getComponentEnabledSetting(ComponentName(context, it)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED 
        }
    }

    private fun enableAlias(context: Context, aliasName: String) {
        val pm = context.packageManager
        val aliases = listOf(DEFAULT_ALIAS, WINTER_ALIAS, SUMMER_ALIAS, MART8_ALIAS)

        aliases.forEach { name ->
            val state = if (name == aliasName) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            pm.setComponentEnabledSetting(
                ComponentName(context, name),
                state,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
