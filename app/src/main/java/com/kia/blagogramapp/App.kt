package com.kia.blagogramapp

import android.app.Application
import com.kia.blagogramapp.utils.IconManager
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.ios.IosEmojiProvider

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        DesignEngine.init(this) // Инициализируем DesignEngine для доступа к контексту
        EmojiManager.install(IosEmojiProvider())
        
        // Автоматическая смена иконки в зависимости от сезона
        IconManager.updateIconIfNeeded(this)
    }
}
