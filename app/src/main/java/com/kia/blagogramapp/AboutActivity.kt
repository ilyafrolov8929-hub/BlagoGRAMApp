package com.kia.blagogramapp

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.kia.blagogramapp.utils.DevModeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.*

class AboutActivity : AppCompatActivity() {
    private var isCurrentThemeDark: Boolean = false
    private var isCurrentUseMonet: Boolean = false

    private val UPDATE_JSON_URL = "https://blagogram.ru/download/update.json"
    private val INSTALL_PERMISSION_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = DesignEngine.initializeScreenWithFrameLayout(this)
        setContentView(rootLayout)

        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val vName = packageInfo.versionName ?: "0.0.0"
        val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode else packageInfo.versionCode

        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        isCurrentThemeDark = themePrefs.getBoolean("is_dark_theme", false)
        isCurrentUseMonet = themePrefs.getBoolean("use_monet", false)

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, LayoutHelper.dp(40))
        }
        scrollView.addView(contentLayout)
        contentLayout.addView(SpacerCell(this, LayoutHelper.dp(50)))

        val logoView = ImageView(this).apply { setImageResource(R.mipmap.ic_launcher) }
        contentLayout.addView(logoView, LinearLayout.LayoutParams(LayoutHelper.dp(90), LayoutHelper.dp(90)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = LayoutHelper.dp(12)
        })

        val appNameView = TextView(this).apply {
            text = "Lume"
            textSize = 32f
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            typeface = Typeface.create("sans-serif-rounded", Typeface.BOLD)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        contentLayout.addView(appNameView, LinearLayout.LayoutParams(-1, -2))

        val versionTextView = TextView(this).apply {
            textSize = 16f
            setTextColor(DesignEngine.Theme.colorTextSecondary)
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, LayoutHelper.dp(4), 0, LayoutHelper.dp(40))
            text = "Версия $vName ($vCode)"
        }
        contentLayout.addView(versionTextView, LinearLayout.LayoutParams(-1, -2))

        addSectionHeader(contentLayout, "ИНФОРМАЦИЯ")
        val mainCard = ContainerCell(this)
        addInfoRow(mainCard, "Имя пакета", packageName)
        addInfoRow(mainCard, "Сайт проекта", "blagogram.ru", true) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://blagogram.ru")))
        }

        addInfoRow(mainCard, "Проверить обновления", vName, isLast = true) {
            checkForUpdates(vName)
        }
        contentLayout.addView(mainCard, getCardParams())

        rootLayout.addView(scrollView)
        setupToolbar(rootLayout)
    }

    private fun checkForUpdates(currentVersion: String) {
        Toast.makeText(this, "Поиск обновлений...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
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
                val downloadUrl = jsonData.substringAfter("\"url\":").substringAfter("\"").substringBefore("\"").trim()

                withContext(Dispatchers.Main) {
                    if (isNewer(latestVersion, currentVersion)) {
                        val file = File(externalCacheDir, "Lume-$latestVersion.apk")
                        val isDownloaded = file.exists() && file.length() > 1024 // файл больше 1кб
                        showUpdateBottomSheet(currentVersion, latestVersion, downloadUrl, isDownloaded)
                    } else {
                        Toast.makeText(this@AboutActivity, "У вас последняя версия ($currentVersion)", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AboutActivity, "Ошибка сети", Toast.LENGTH_SHORT).show()
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

    private fun showUpdateBottomSheet(oldVer: String, newVer: String, url: String, isDownloaded: Boolean) {
        val dialog = BottomSheetDialog(this, R.style.CustomBottomSheetDialogTheme)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(LayoutHelper.dp(20), LayoutHelper.dp(24), LayoutHelper.dp(20), LayoutHelper.dp(30))
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorBackground)
                cornerRadius = LayoutHelper.dp(28).toFloat()
            }
        }

        val title = TextView(this).apply {
            text = "Доступно обновление!"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            gravity = Gravity.CENTER
        }
        root.addView(title)

        val subTitle = TextView(this).apply {
            text = "$oldVer → $newVer"
            textSize = 14f
            typeface = Typeface.MONOSPACE
            setTextColor(DesignEngine.Theme.colorTextSecondary)
            gravity = Gravity.CENTER
            setPadding(0, LayoutHelper.dp(8), 0, LayoutHelper.dp(24))
        }
        root.addView(subTitle)

        val row = RelativeLayout(this)
        val icon = ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher)
            id = View.generateViewId()
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorBackground)
                cornerRadius = LayoutHelper.dp(12).toFloat()
            }
            setPadding(LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8))
        }
        row.addView(icon, RelativeLayout.LayoutParams(LayoutHelper.dp(64), LayoutHelper.dp(64)))

        val textContainer = LinearLayout(this).apply {
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

        val btnUpdate = Button(this).apply {
            text = if (isDownloaded) "Установить" else "Обновить"
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#007AFF"))
                cornerRadius = LayoutHelper.dp(20).toFloat()
            }
        }
        val btnParams = RelativeLayout.LayoutParams(LayoutHelper.dp(110), LayoutHelper.dp(42)).apply {
            addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            addRule(RelativeLayout.CENTER_VERTICAL)
        }
        row.addView(btnUpdate, btnParams)
        root.addView(row)

        val progressLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, LayoutHelper.dp(30), 0, 0)
        }

        val labels = RelativeLayout(this)
        val tvSize = TextView(this).apply { textSize = 13f; setTextColor(DesignEngine.Theme.colorTextPrimary); typeface = Typeface.DEFAULT_BOLD }
        val tvPercent = TextView(this).apply { textSize = 13f; setTextColor(DesignEngine.Theme.colorTextPrimary); typeface = Typeface.DEFAULT_BOLD }
        labels.addView(tvSize, RelativeLayout.LayoutParams(-2,-2).apply { addRule(RelativeLayout.ALIGN_PARENT_LEFT)})
        labels.addView(tvPercent, RelativeLayout.LayoutParams(-2,-2).apply { addRule(RelativeLayout.ALIGN_PARENT_RIGHT)})

        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#4A90E2"))
        }

        progressLayout.addView(labels)
        progressLayout.addView(progressBar, LinearLayout.LayoutParams(-1, LayoutHelper.dp(10)).apply { topMargin = LayoutHelper.dp(8) })
        root.addView(progressLayout)

        btnUpdate.setOnClickListener {
            val file = File(externalCacheDir, "Lume-$newVer.apk")
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
        lifecycleScope.launch(Dispatchers.IO) {
            val file = File(externalCacheDir, "Lume-$version.apk")
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
                withContext(Dispatchers.Main) { Toast.makeText(this@AboutActivity, "Ошибка загрузки", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun installApk(file: File) {
        if (!file.exists()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivityForResult(intent, INSTALL_PERMISSION_CODE)
                return
            }
        }

        try {
            val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == INSTALL_PERMISSION_CODE) {
            // Файл ищется по версии, но тут упростим до поиска последнего скачанного в кэше
            val files = externalCacheDir?.listFiles { _, name -> name.endsWith(".apk") }
            files?.maxByOrNull { it.lastModified() }?.let { installApk(it) }
        }
    }

    private fun addSectionHeader(container: LinearLayout, title: String) {
        container.addView(TextView(this).apply {
            text = title
            textSize = 13f
            setTextColor(DesignEngine.Theme.colorHeader)
            setPadding(LayoutHelper.dp(24), LayoutHelper.dp(16), 0, LayoutHelper.dp(8))
        })
    }

    private fun addInfoRow(card: ContainerCell, label: String, value: String, isLink: Boolean = false, isLast: Boolean = false, onClick: (() -> Unit)? = null) {
        val frame = FrameLayout(this).apply { if (onClick != null) setOnClickListener { onClick() } }
        frame.addView(TextView(this).apply { text = label; textSize = 16f; setTextColor(DesignEngine.Theme.colorTextPrimary) },
            LayoutHelper.createFrame(-2, -2, Gravity.START or Gravity.CENTER_VERTICAL, 16, 0, 0, 0))
        frame.addView(TextView(this).apply {
            text = value
            textSize = 15f
            setTextColor(if (isLink) Color.parseColor("#007AFF") else DesignEngine.Theme.colorTextSecondary)
        }, LayoutHelper.createFrame(-2, -2, Gravity.END or Gravity.CENTER_VERTICAL, 0, 0, 16, 0))
        card.addView(frame, LinearLayout.LayoutParams(-1, LayoutHelper.dp(50)))
    }

    private fun setupToolbar(root: FrameLayout) {
        val toolbar = DesignEngine.createToolBar(
            context = this,
            title = "О приложении",
            leftButtonIconRes = R.drawable.ic_back,
            leftButtonClickListener = { finish() },
            isDark = isCurrentThemeDark
        )
        root.addView(toolbar)
    }

    private fun getCardParams() = LinearLayout.LayoutParams(-1, -2).apply { leftMargin = LayoutHelper.dp(16); rightMargin = LayoutHelper.dp(16) }
}
