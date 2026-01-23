package com.kia.blagogramapp.ui.screens

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import com.kia.blagogramapp.BaseScreen
import com.kia.blagogramapp.ChangeUsernameActivity
import com.kia.blagogramapp.ChatSettingsActivity
import com.kia.blagogramapp.ContainerCell
import com.kia.blagogramapp.DesignEngine
import com.kia.blagogramapp.HeaderCell
import com.kia.blagogramapp.LayoutHelper
import com.kia.blagogramapp.MainActivity
import com.kia.blagogramapp.PremiumActivity
import com.kia.blagogramapp.PrivaciActivity
import com.kia.blagogramapp.ProfileEditActivity
import com.kia.blagogramapp.ProfileHeaderCell
import com.kia.blagogramapp.R
import com.kia.blagogramapp.SpacerCell
import com.kia.blagogramapp.StatisticsActivity
import com.kia.blagogramapp.TextCell
import com.kia.blagogramapp.TextDetailCell
import com.kia.blagogramapp.ThemeSelectionActivity
import com.kia.blagogramapp.ui.modules.BottomNavigationModule
import com.kia.blagogramapp.ui.modules.ChatListLayout
import com.kia.blagogramapp.utils.DevModeManager

class SettingsScreen(context: Context) : BaseScreen(context) {

    val activity = context as? AppCompatActivity
    private var isCurrentThemeDark: Boolean = false
    private var isCurrentUseMonet: Boolean = false

    private var usernameDetailCell: TextDetailCell? = null

    override fun createView(): View {
        val rootLayout = ChatListLayout(context)

        val themePrefs = context.getSharedPreferences("app_theme", MODE_PRIVATE)
        isCurrentThemeDark = themePrefs.getBoolean("is_dark_theme", false)
        isCurrentUseMonet = themePrefs.getBoolean("use_monet", false)

        BottomNavigationModule(context).create(rootLayout, BottomNavigationModule.Screen.SETTINGS)

        val prefs = context.getSharedPreferences("app", MODE_PRIVATE)
        val displayName = prefs.getString("display_name", "Пользователь") ?: "Пользователь"
        val status = prefs.getString("status", "") ?: "Нет статуса"
        val phone = prefs.getString("phone", "") ?: "Не известно"
        val avatar = prefs.getString("avatar", "") ?: ""
        val username = prefs.getString("login", "")
        val isPremium = prefs.getBoolean("is_premium", false)


        val scrollView = ScrollView(context).apply {
            isFillViewport = true
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_NEVER
            if (DesignEngine.useBottomNavigation) {
                setPadding(0, 0, 0, LayoutHelper.dp(80))
            }
        }

        val contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        contentLayout.addView(SpacerCell(context, LayoutHelper.dp(35)))

        val profileContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(LayoutHelper.dp(20), 0, LayoutHelper.dp(20), 0)
        }

        val profileHeader = ProfileHeaderCell(context)
        profileHeader.setData(displayName, status, "https://blagogram.ru/avatars/$avatar", isPremium)
        profileContainer.addView(profileHeader)

        contentLayout.addView(profileContainer)
        val sheetContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(LayoutHelper.dp(20), LayoutHelper.dp(10), LayoutHelper.dp(20), LayoutHelper.dp(40))
        }

        contentLayout.addView(SpacerCell(context, LayoutHelper.dp(10)))

        sheetContainer.addView(HeaderCell(context).apply { text = "Аккаунт" })
        val infoUserCard = ContainerCell(context)

        infoUserCard.addCell(TextDetailCell(context).apply {
            setData(phone, "Нажмите, чтобы изменить номер телефона")
        })

        usernameDetailCell = TextDetailCell(context).apply {
            setData("@$username", "Ваш юзернейм")
            setOnClickListener {
                //startActivity(Intent(this@SettingsActivity, ChangeUsernameActivity::class.java))
            }
        }
        infoUserCard.addCell(usernameDetailCell!!)

        infoUserCard.addCell(TextDetailCell(context).apply {
            setData(status, "О себе")
        }, isLast = true)

        sheetContainer.addView(infoUserCard)
        sheetContainer.addView(SpacerCell(context, LayoutHelper.dp(10)))

        sheetContainer.addView(HeaderCell(context).apply { text = "Настройки" })
        val accountCard = ContainerCell(context)
        accountCard.addCell(TextCell(context).apply {
            setData("Редактировать профиль", R.drawable.ic_edit)
            setOnClickListener { presentFragment(EditProfileScreen(context)) }
        })
        accountCard.addCell(TextCell(context).apply {
            setData("Lume Infinity", R.drawable.ic_premium_diamond)
            setIconColor(DesignEngine.COLOR_BLUE)
            setOnClickListener { presentFragment(LumePremiumScreen(context)) }
        })
        accountCard.addCell(TextCell(context).apply {
            setData("Приватность", R.drawable.ic_user)
            setOnClickListener { presentFragment(PrivaciScreen(context)) }
        }, isLast = true)
        accountCard.addCell(TextCell(context).apply {
            setData("Настройки чатов", R.drawable.ic_chat)
            setOnClickListener { presentFragment(ChatSettingsScreen(context)) }
        })
        accountCard.addCell(TextCell(context).apply {
            setData("Уведомления и звуки", R.drawable.ic_music) })
        accountCard.addCell(TextCell(context).apply {
            setData("Кастомизация", R.drawable.ic_brush)
            //setOnClickListener { startActivity(Intent(context, ThemeSelectionActivity::class.java)) }
        })
        accountCard.addCell(TextCell(context).apply {
            setData("AI Настройки", R.drawable.ic_ai)
            //setOnClickListener { startActivity(Intent(context, AiSettingsActivity::class.java)) }
        }, isLast = true)
        sheetContainer.addView(accountCard)
        sheetContainer.addView(SpacerCell(context, LayoutHelper.dp(10)))

        sheetContainer.addView(HeaderCell(context).apply { text = "ИНФОРМАЦИЯ" })
        val infoCard = ContainerCell(context)
        infoCard.addCell(TextCell(context).apply {
            setData("Данные и память", R.drawable.ic_data_and_storage)
            //setOnClickListener { startActivity(Intent(context, StatisticsActivity::class.java)) }
        })
        infoCard.addCell(TextCell(context).apply {
            setData("О приложении", R.drawable.ic_more)
            setOnClickListener {
                presentFragment(AboutScreen(context))
            }
        })
        infoCard.addCell(TextCell(context).apply {
            val isDev = DevModeManager.isEnabled(context)
            setData("Разработчик: ${if (isDev) "ВКЛ" else "ВЫКЛ"}", R.drawable.ic_settings)
            if (isDev) setTextColor(DesignEngine.Theme.colorBlue)
            setOnClickListener {
                DevModeManager.setEnabled(context, !isDev)
            }
        }, isLast = true)
        sheetContainer.addView(infoCard)
        sheetContainer.addView(SpacerCell(context, LayoutHelper.dp(10)))

        val logoutCard = ContainerCell(context)
        // 3. Выход
        logoutCard.addCell(TextCell(context).apply {
            setData("Выйти", R.drawable.ic_logout)
            setOnClickListener {
                context.getSharedPreferences("app", MODE_PRIVATE).edit().clear().apply()
                // Чтобы полностью сбросить стек и уйти на вход:
                (context as? MainActivity)?.navigationLayout?.let { nav ->
                    // Очищаем стек (если добавишь такой метод) или просто:
                    nav.clearStack(StartScreen(context))
                }
            }
        }, isLast = true)
        sheetContainer.addView(logoutCard)

        contentLayout.addView(sheetContainer)
        scrollView.addView(contentLayout)
        rootLayout.addView(scrollView)

        val toolbar = DesignEngine.createToolBar(
            context = context,
            title = "Настройки",
            leftButtonIconRes = R.drawable.ic_back,
            isDark = isCurrentThemeDark,
            rightButtonIconRes = R.drawable.ic_moments,
            leftButtonClickListener = { finishFragment() },
            rightButtonClickListener = {
                val newIsDark = !isCurrentThemeDark

                // 1. Вызываем глобальное обновление данных
                (context as? MainActivity)?.applyAppTheme(newIsDark, isCurrentUseMonet)

                // 2. Плавно обновляем текущий экран настроек
                parentLayout?.refreshTopScreenWithFade()
            }
        )
        rootLayout.addView(toolbar, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP))

        return rootLayout
    }

}
