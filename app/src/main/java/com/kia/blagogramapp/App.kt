package com.kia.blagogramapp

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.ios.IosEmojiProvider
import java.util.Calendar

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        EmojiManager.install(IosEmojiProvider())
    }
}
