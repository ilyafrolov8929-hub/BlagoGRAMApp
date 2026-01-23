package com.kia.blagogramapp.ui.screens

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.wrappers.Wrappers.packageManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.kia.blagogramapp.*
import com.kia.blagogramapp.ui.modules.ChatListLayout
import com.kia.blagogramapp.views.BlurGradientBackgroundView
import com.kia.blagogramapp.views.GradientTextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class AboutScreen(context: Context) : BaseScreen(context) {

    private var isCurrentThemeDark: Boolean = false
    private var isCurrentUseMonet: Boolean = false
    val activity = context as? AppCompatActivity

    private val UPDATE_JSON_URL = "https://blagogram.ru/download/update.json"
    private val INSTALL_PERMISSION_CODE = 1001

    override fun createView(): View {
        val rootLayout = ChatListLayout(context)

        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val vName = packageInfo.versionName ?: "0.0.0"
        val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode else packageInfo.versionCode

        val themePrefs = context.getSharedPreferences("app_theme", MODE_PRIVATE)
        isCurrentThemeDark = themePrefs.getBoolean("is_dark_theme", false)
        isCurrentUseMonet = themePrefs.getBoolean("use_monet", false)

        val scrollView = ScrollView(context).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, LayoutHelper.dp(40))
        }
        scrollView.addView(contentLayout)
        contentLayout.addView(SpacerCell(context, LayoutHelper.dp(50)))

        val logoView = ImageView(context).apply { setImageResource(R.mipmap.ic_launcher) }
        contentLayout.addView(logoView, LinearLayout.LayoutParams(LayoutHelper.dp(90), LayoutHelper.dp(90)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = LayoutHelper.dp(12)
        })

        val appNameView = TextView(context).apply {
            text = "Lume"
            textSize = 32f
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            typeface = Typeface.create("sans-serif-rounded", Typeface.BOLD)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        contentLayout.addView(appNameView, LinearLayout.LayoutParams(-1, -2))

        val versionTextView = TextView(context).apply {
            textSize = 16f
            setTextColor(DesignEngine.Theme.colorTextSecondary)
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, LayoutHelper.dp(4), 0, LayoutHelper.dp(40))
            text = "Версия $vName ($vCode)"
        }
        contentLayout.addView(versionTextView, LinearLayout.LayoutParams(-1, -2))

        addSectionHeader(contentLayout, "ИНФОРМАЦИЯ")
        val mainCard = ContainerCell(context)
        addInfoRow(mainCard, "Имя пакета", context.packageName)
        addInfoRow(mainCard, "Версия LumeEngine", "2.0") {
            Toast.makeText(context, "Это иновационый движок разработанный для рендеринга приложения.", Toast. LENGTH_LONG).show()
        }
        addInfoRow(mainCard, "Проверить обновления", vName, isLast = true) {
            checkForUpdates(vName)
        }
        contentLayout.addView(mainCard, getCardParams())

        rootLayout.addView(scrollView)
        setupToolbar(rootLayout)

        return rootLayout
    }

    private fun checkForUpdates(currentVersion: String) {
        Toast.makeText(context, "Поиск обновлений...", Toast.LENGTH_SHORT).show()

        activity?.lifecycleScope?.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(UPDATE_JSON_URL)
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) throw Exception("Ошибка сервера")

                val jsonData = response.body?.string() ?: ""
                val latestVersion = jsonData.substringAfter("\"version\":").substringAfter("\"").substringBefore("\"").trim()
                val info = jsonData.substringAfter("\"info\":").substringAfter("\"").substringBefore("\"").trim()
                val downloadUrl = jsonData.substringAfter("\"url\":").substringAfter("\"").substringBefore("\"").trim()

                withContext(Dispatchers.Main) {
                    if (isNewer(latestVersion, currentVersion)) {
                        val file = File(context.externalCacheDir, "Lume-$latestVersion.apk")
                        val isDownloaded = file.exists() && file.length() > 1024 // файл больше 1кб
                        showUpdateBottomSheet(currentVersion, latestVersion, info, downloadUrl, isDownloaded)
                    } else {
                        Toast.makeText(context, "У вас последняя версия ($currentVersion)", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Ошибка сети", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun isNewer(v1: String, v2: String): Boolean {
        if (v1 == v2) return false
        return try {
            val p1 = v1.split(".").map { it.toInt() }
            val p2 = v2.split(".").map { it.toInt() }
            val len = maxOf(p1.size, p2.size)
            for (i in 0 until len) {
                val n1 = p1.getOrNull(i) ?: 0
                val n2 = p2.getOrNull(i) ?: 0
                if (n1 > n2) return true
                if (n1 < n2) return false
            }
            false
        } catch (e: Exception) { v1 > v2 }
    }

    private fun showUpdateBottomSheet(oldVer: String, newVer: String, info: String, url: String, isDownloaded: Boolean) {
        val dialog = BottomSheetDialog(context, R.style.CustomBottomSheetDialogTheme)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(LayoutHelper.dp(20), LayoutHelper.dp(24), LayoutHelper.dp(20), LayoutHelper.dp(30))
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorBackground)
                cornerRadius = LayoutHelper.dp(28).toFloat()
            }
        }

        val title = TextView(context).apply {
            text = "Доступно обновление!"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            gravity = Gravity.CENTER
        }
        root.addView(title)

        val subTitle = TextView(context).apply {
            text = "$oldVer → $newVer"
            textSize = 14f
            typeface = Typeface.MONOSPACE
            setTextColor(DesignEngine.Theme.colorTextSecondary)
            gravity = Gravity.CENTER
            setPadding(0, LayoutHelper.dp(8), 0, LayoutHelper.dp(24))
        }
        root.addView(subTitle)

        val row = RelativeLayout(context)
        val icon = ImageView(context).apply {
            setImageResource(R.mipmap.ic_launcher)
            id = View.generateViewId()
            background = GradientDrawable().apply {
                setColor(if (isCurrentThemeDark) Color.parseColor("#303030") else Color.parseColor("#F0F0F0"))
                cornerRadius = LayoutHelper.dp(12).toFloat()
            }
            setPadding(LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8))
        }
        row.addView(icon, RelativeLayout.LayoutParams(LayoutHelper.dp(64), LayoutHelper.dp(64)))

        val textContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val p = RelativeLayout.LayoutParams(-2, -2).apply {
                addRule(RelativeLayout.RIGHT_OF, icon.id)
                addRule(RelativeLayout.CENTER_VERTICAL)
                leftMargin = LayoutHelper.dp(12)
            }
            addView(TextView(context).apply {
                text = "Lume"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(DesignEngine.Theme.colorTextPrimary)
            })
            addView(TextView(context).apply {
                text = "Lume team"
                textSize = 14f
                setTextColor(Color.parseColor("#4A90E2"))
            })
            layoutParams = p
        }
        row.addView(textContainer)

        val btnUpdate = Button(context).apply {
            text = if (isDownloaded) "Установить" else "Обновить"
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E0E0E0")) // Светлая кнопка как на макете
                cornerRadius = LayoutHelper.dp(20).toFloat()
            }
        }
        val btnParams = RelativeLayout.LayoutParams(LayoutHelper.dp(110), LayoutHelper.dp(42)).apply {
            addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            addRule(RelativeLayout.CENTER_VERTICAL)
        }
        row.addView(btnUpdate, btnParams)
        root.addView(row)

        // --- ОБНОВЛЕННЫЙ БЛОК: Контейнер для списка изменений с кружочками ---
        if (info.isNotEmpty()) {
            val infoContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(16))
                background = GradientDrawable().apply {
                    setColor(if (isCurrentThemeDark) Color.parseColor("#252525") else Color.parseColor("#E8E8E8"))
                    cornerRadius = LayoutHelper.dp(16).toFloat()
                }
            }
            val infoParams = LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = LayoutHelper.dp(24)
            }

            // Заголовок "Изменения:"
            val infoHeader = TextView(context).apply {
                text = "Изменения:"
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(DesignEngine.Theme.colorTextPrimary)
                setPadding(0, 0, 0, LayoutHelper.dp(8))
            }
            infoContainer.addView(infoHeader)

            // Разделяем текст на строки по \n или \\n
            val lines = info.split(Regex("\\\\n|\\n")).filter { it.isNotBlank() }

            lines.forEach { line ->
                val rowLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, LayoutHelper.dp(4), 0, LayoutHelper.dp(4))
                    gravity = Gravity.TOP
                }

                // Рисуем кружочек (Bullet)
                val dot = View(context).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(DesignEngine.Theme.colorTextPrimary) // Цвет точки как у текста
                    }
                }
                val dotParams = LinearLayout.LayoutParams(LayoutHelper.dp(6), LayoutHelper.dp(6)).apply {
                    topMargin = LayoutHelper.dp(8) // Центрируем точку относительно первой строки текста
                    rightMargin = LayoutHelper.dp(10)
                }
                rowLayout.addView(dot, dotParams)

                // Текст самого изменения
                val lineText = TextView(context).apply {
                    text = line.trim()
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(DesignEngine.Theme.colorTextPrimary)
                }
                rowLayout.addView(lineText, LinearLayout.LayoutParams(-1, -2))

                infoContainer.addView(rowLayout)
            }

            root.addView(infoContainer, infoParams)
        }
        // --- КОНЕЦ НОВОГО БЛОКА ---

        val progressLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, LayoutHelper.dp(30), 0, 0)
        }

        val labels = RelativeLayout(context)
        val tvSize = TextView(context).apply { textSize = 13f; setTextColor(DesignEngine.Theme.colorTextPrimary); typeface = Typeface.DEFAULT_BOLD }
        val tvPercent = TextView(context).apply { textSize = 13f; setTextColor(DesignEngine.Theme.colorTextPrimary); typeface = Typeface.DEFAULT_BOLD }
        labels.addView(tvSize, RelativeLayout.LayoutParams(-2,-2).apply { addRule(RelativeLayout.ALIGN_PARENT_LEFT)})
        labels.addView(tvPercent, RelativeLayout.LayoutParams(-2,-2).apply { addRule(RelativeLayout.ALIGN_PARENT_RIGHT)})

        val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#4A90E2"))
        }

        progressLayout.addView(labels)
        progressLayout.addView(progressBar, LinearLayout.LayoutParams(-1, LayoutHelper.dp(10)).apply { topMargin = LayoutHelper.dp(8) })
        root.addView(progressLayout)

        btnUpdate.setOnClickListener {
            val file = File(context.externalCacheDir, "Lume-$newVer.apk")
            if (isDownloaded && file.exists()) {
                installApk(file)
            } else {
                btnUpdate.visibility = View.GONE
                progressLayout.visibility = View.VISIBLE
                downloadAndInstall(url, newVer, progressBar, tvSize, tvPercent)
            }
        }

        dialog.setContentView(root)
        dialog.show()
    }


    private fun downloadAndInstall(url: String, version: String, bar: ProgressBar, tvSize: TextView, tvPercent: TextView) {
        activity?.lifecycleScope?.launch(Dispatchers.IO) {
            val file = File(context.externalCacheDir, "Lume-$version.apk")
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body ?: throw Exception("Body is null")

                val total = body.contentLength()
                var current = 0L
                val buffer = ByteArray(8192)

                body.byteStream().use { input ->
                    FileOutputStream(file).use { output ->
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            current += read
                            val progress = if (total > 0) ((current * 100) / total).toInt() else 0
                            withContext(Dispatchers.Main) {
                                bar.progress = progress
                                tvPercent.text = "$progress%"
                                tvSize.text = String.format("%.1f MB / %.1f MB", current/1024f/1024f, total/1024f/1024f)
                            }
                        }
                        output.flush()
                    }
                }
                withContext(Dispatchers.Main) { installApk(file) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(context, "Ошибка загрузки", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun installApk(file: File) {
        if (!file.exists()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                // Вызываем через activity, так как экран — это не Activity
                activity?.startActivityForResult(intent, INSTALL_PERMISSION_CODE)
                return
            }
        }

        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Исправлена опечатка contextcontext -> context
            Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == INSTALL_PERMISSION_CODE) {
            val files = context.externalCacheDir?.listFiles { _, name -> name.endsWith(".apk") }
            files?.maxByOrNull { it.lastModified() }?.let { installApk(it) }
        }
    }

    private fun addSectionHeader(container: LinearLayout, title: String) {
        container.addView(TextView(context).apply {
            text = title
            textSize = 13f
            setTextColor(DesignEngine.Theme.colorHeader)
            setPadding(LayoutHelper.dp(24), LayoutHelper.dp(16), 0, LayoutHelper.dp(8))
        })
    }

    private fun addInfoRow(card: ContainerCell, label: String, value: String, isLink: Boolean = false, isLast: Boolean = false, onClick: (() -> Unit)? = null) {
        val frame = FrameLayout(context).apply { if (onClick != null) setOnClickListener { onClick() } }
        frame.addView(TextView(context).apply { text = label; textSize = 16f; setTextColor(DesignEngine.Theme.colorTextPrimary) },
            LayoutHelper.createFrame(-2, -2, Gravity.START or Gravity.CENTER_VERTICAL, 16, 0, 0, 0))
        frame.addView(TextView(context).apply {
            text = value
            textSize = 15f
            setTextColor(if (isLink) Color.parseColor("#007AFF") else DesignEngine.Theme.colorTextSecondary)
        }, LayoutHelper.createFrame(-2, -2, Gravity.END or Gravity.CENTER_VERTICAL, 0, 0, 16, 0))
        card.addView(frame, LinearLayout.LayoutParams(-1, LayoutHelper.dp(50)))
    }

    private fun setupToolbar(root: FrameLayout) {
        val toolbar = DesignEngine.createToolBar(
            context = context,
            title = "О приложении",
            leftButtonIconRes = R.drawable.ic_back,
            leftButtonClickListener = { finishFragment() },
            isDark = isCurrentThemeDark
        )
        root.addView(toolbar)
    }

    private fun getCardParams() = LinearLayout.LayoutParams(-1, -2).apply { leftMargin = LayoutHelper.dp(16); rightMargin = LayoutHelper.dp(16) }
}