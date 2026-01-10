package com.kia.blagogramapp

import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import coil.load
import com.google.android.material.imageview.ShapeableImageView
import com.kia.blagogramapp.api.ApiClient
import com.kia.blagogramapp.api.JamendoTrack
import com.kia.blagogramapp.models.Track
import com.kia.blagogramapp.ui.modules.FullPlayer
import com.kia.blagogramapp.ui.modules.MiniPlayer
import com.kia.blagogramapp.utils.MusicManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MusicActivity : AppCompatActivity() {

    private lateinit var root: FrameLayout
    private lateinit var searchContainer: FrameLayout
    private lateinit var searchEdit: EditText
    private lateinit var recycler: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tabsLayout: LinearLayout
    private lateinit var emptyTv: TextView
    private lateinit var miniPlayer: MiniPlayer
    private var fullPlayer: FullPlayer? = null

    private var currentMode = 0 // 0 - Популярное, 1 - Моя музыка
    private var searchJob: Job? = null

    private val musicPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            MusicManager.addTrack(this, it)
            if (currentMode == 1) loadMyMusic()
            Toast.makeText(this, "Трек добавлен в вашу библиотеку", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Вызываем super.onCreate() в начале, что является хорошей практикой
        super.onCreate(savedInstanceState)

        // 1. Инициализируем экран и сразу получаем готовый корневой FrameLayout
        root = DesignEngine.initializeScreenWithFrameLayout(this)
        setContentView(root)

        // 2. Получаем тему, так как она нужна для логики тулбара и кнопки "Разработчик"
        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        val isCurrentThemeDark = themePrefs.getBoolean("is_dark_theme", false)

        val mainContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(mainContent)

        // Тулбар
        val toolbar = DesignEngine.createToolBar(
            context = this,
            title = "Музыка",
            leftButtonIconRes = R.drawable.ic_back,
            leftButtonClickListener = { finish() },
            rightButtonIconRes = R.drawable.ic_music,
            rightButtonClickListener = { musicPickerLauncher.launch("audio/*") },
            isDark = isCurrentThemeDark
        )
        mainContent.addView(toolbar)

        // Tabs
        tabsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(8), LayoutHelper.dp(16), LayoutHelper.dp(8))
        }
        addTab("Главная", 0)
        addTab("Моя музыка", 1)
        mainContent.addView(tabsLayout)

        // Search
        searchContainer = FrameLayout(this).apply {
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(8), LayoutHelper.dp(16), LayoutHelper.dp(8))
        }
        searchEdit = EditText(this).apply {
            hint = "Поиск музыки..."
            setHintTextColor(DesignEngine.Theme.colorTextSecondary)
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            textSize = 16f
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(12), LayoutHelper.dp(16), LayoutHelper.dp(12))
            background = GradientDrawable().apply {
                cornerRadius = LayoutHelper.dp(12).toFloat()
                setColor(DesignEngine.Theme.colorCardBackground)
            }
        }
        searchContainer.addView(searchEdit)
        mainContent.addView(searchContainer)

        // SwipeRefresh + Recycler
        swipeRefresh = SwipeRefreshLayout(this).apply {
            setProgressBackgroundColorSchemeColor(DesignEngine.Theme.colorCardBackground)
            setColorSchemeColors(DesignEngine.Theme.colorBlue)
            setOnRefreshListener {
                if (currentMode == 0) {
                    val query = searchEdit.text.toString()
                    if (query.isNotEmpty()) searchMusic(query) else loadPopular()
                } else {
                    loadMyMusic()
                }
            }
        }

        recycler = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MusicActivity)
            clipToPadding = false
            setPadding(0, 0, 0, LayoutHelper.dp(100))
        }
        swipeRefresh.addView(recycler)
        mainContent.addView(swipeRefresh, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1f))

        emptyTv = TextView(this).apply {
            text = "Ничего не найдено"
            setTextColor(DesignEngine.Theme.colorTextSecondary)
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        root.addView(emptyTv, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER))

        // MiniPlayer
        miniPlayer = MiniPlayer(this).apply {
            tag = "mini_player"
            visibility = View.GONE
            setOnClickListener { showFullPlayer() }
        }
        val miniParams = LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM, 16, 0, 16, 32)
        root.addView(miniPlayer, miniParams)

        setupListeners()
        observeMusic()
        loadPopular()
    }

    private fun addTab(text: String, mode: Int) {
        val tv = TextView(this).apply {
            this.text = text
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(8), LayoutHelper.dp(16), LayoutHelper.dp(8))
            setTextColor(if (currentMode == mode) DesignEngine.Theme.colorBlue else DesignEngine.Theme.colorTextSecondary)
            setOnClickListener { switchTab(mode) }
        }
        tabsLayout.addView(tv)
    }

    private fun switchTab(mode: Int) {
        if (currentMode == mode) return

        currentMode = mode
        recycler.adapter = null
        emptyTv.visibility = View.GONE

        for (i in 0 until tabsLayout.childCount) {
            val tv = tabsLayout.getChildAt(i) as TextView
            tv.setTextColor(if (i == mode) DesignEngine.Theme.colorBlue else DesignEngine.Theme.colorTextSecondary)
        }

        if (mode == 1) {
            val initialHeight = searchContainer.height
            val animator = ValueAnimator.ofInt(initialHeight, 0)
            animator.addUpdateListener { valueAnimator ->
                val lp = searchContainer.layoutParams
                lp.height = valueAnimator.animatedValue as Int
                searchContainer.layoutParams = lp
            }
            animator.duration = 250

            searchContainer.animate()
                .alpha(0f)
                .translationY(-LayoutHelper.dp(20).toFloat())
                .setDuration(250)
                .withEndAction {
                    searchContainer.visibility = View.GONE
                    val lp = searchContainer.layoutParams
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    searchContainer.layoutParams = lp
                }
                .start()
            animator.start()

            loadMyMusic()
        } else {
            searchContainer.visibility = View.VISIBLE
            searchContainer.alpha = 0f
            searchContainer.translationY = -LayoutHelper.dp(20).toFloat()

            searchContainer.measure(
                View.MeasureSpec.makeMeasureSpec(root.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val targetHeight = searchContainer.measuredHeight

            val animator = ValueAnimator.ofInt(0, targetHeight)
            animator.addUpdateListener { valueAnimator ->
                val lp = searchContainer.layoutParams
                lp.height = valueAnimator.animatedValue as Int
                searchContainer.layoutParams = lp
            }
            animator.duration = 250

            searchContainer.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(250)
                .withEndAction {
                    val lp = searchContainer.layoutParams
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    searchContainer.layoutParams = lp
                }
                .start()
            animator.start()

            loadPopular()
        }
    }

    private fun setupListeners() {
        searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(500)
                    val query = s.toString()
                    if (query.isNotEmpty()) searchMusic(query) else if (currentMode == 0) loadPopular()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadPopular() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val response = ApiClient.jamendoApi.getPopularTracks()
                showJamendoTracks(response.results)
            } catch (e: Exception) {
                e.printStackTrace()
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun searchMusic(query: String) {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val response = ApiClient.jamendoApi.searchTracks(query = query)
                showJamendoTracks(response.results)
            } catch (e: Exception) {
                e.printStackTrace()
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun loadMyMusic() {
        swipeRefresh.isRefreshing = true
        MusicManager.scanMusic(this)
        val tracks = MusicManager.playlist.value
        recycler.adapter = MyMusicAdapter(tracks)
        emptyTv.visibility = if (tracks.isEmpty()) View.VISIBLE else View.GONE
        swipeRefresh.isRefreshing = false
    }

    private fun showJamendoTracks(tracks: List<JamendoTrack>) {
        recycler.adapter = JamendoAdapter(tracks)
        emptyTv.visibility = if (tracks.isEmpty()) View.VISIBLE else View.GONE
        swipeRefresh.isRefreshing = false
    }

    private fun observeMusic() {
        lifecycleScope.launchWhenStarted {
            MusicManager.currentTrack.collectLatest { track ->
                if (track != null) {
                    if (fullPlayer == null || fullPlayer!!.parent == null) {
                        miniPlayer.visibility = View.VISIBLE
                    }
                    miniPlayer.bind(track, MusicManager.isPlaying.value)
                    fullPlayer?.bind(track, MusicManager.isPlaying.value)
                }
            }
        }
        lifecycleScope.launchWhenStarted {
            MusicManager.isPlaying.collectLatest { playing ->
                val track = MusicManager.currentTrack.value
                if (track != null) {
                    miniPlayer.bind(track, playing)
                    fullPlayer?.bind(track, playing)
                }
            }
        }
    }

    fun closeFullPlayer() {
        fullPlayer?.animateClose(miniPlayer) {
            root.removeView(fullPlayer)
            miniPlayer.visibility = View.VISIBLE
            miniPlayer.animate().alpha(1f).setDuration(200).start()
        }
    }

    private fun showFullPlayer() {
        val track = MusicManager.currentTrack.value ?: return
        if (fullPlayer == null) {
            fullPlayer = FullPlayer(this).apply {
                layoutParams = FrameLayout.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT)
                onCloseListener = { closeFullPlayer() }
            }
        }
        if (fullPlayer?.parent == null) {
            root.addView(fullPlayer)
            fullPlayer?.animateOpen(miniPlayer)
            miniPlayer.animate().alpha(0f).setDuration(200).start()
        }
        fullPlayer?.bind(track, MusicManager.isPlaying.value)
    }

    private fun showCustomConfirmDialog(context: Context, title: String, message: String, onConfirm: () -> Unit) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(LayoutHelper.dp(24), LayoutHelper.dp(24), LayoutHelper.dp(24), LayoutHelper.dp(24))
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorCardBackground)
                cornerRadius = LayoutHelper.dp(28).toFloat()
            }
            layoutParams = FrameLayout.LayoutParams(LayoutHelper.dp(300), LayoutHelper.WRAP_CONTENT)
        }

        val titleTv = TextView(context).apply {
            text = title
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(DesignEngine.Theme.colorTextPrimary)
        }
        root.addView(titleTv)

        val msgTv = TextView(context).apply {
            text = message
            textSize = 16f
            setTextColor(DesignEngine.Theme.colorTextSecondary)
            val p = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT)
            p.topMargin = LayoutHelper.dp(12)
            layoutParams = p
        }
        root.addView(msgTv)

        val buttons = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            val p = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT)
            p.topMargin = LayoutHelper.dp(24)
            layoutParams = p
        }

        val btnCancel = Button(context).apply {
            text = "ОТМЕНА"
            background = null
            setTextColor(DesignEngine.Theme.colorBlue)
            setOnClickListener { dialog.dismiss() }
        }
        buttons.addView(btnCancel)

        val btnOk = Button(context).apply {
            text = "УДАЛИТЬ"
            background = null
            setTextColor(DesignEngine.Theme.colorLogout)
            setOnClickListener {
                onConfirm()
                dialog.dismiss()
            }
        }
        buttons.addView(btnOk)

        root.addView(buttons)
        dialog.setContentView(root)
        dialog.show()
    }

    inner class JamendoAdapter(private val tracks: List<JamendoTrack>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val cell = MusicCell(this@MusicActivity)
            cell.layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT)
            return object : RecyclerView.ViewHolder(cell) {}
        }
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val track = tracks[position]
            val cell = holder.itemView as MusicCell
            cell.setData(track.name, track.artistName, track.image)

            val isDownloaded = MusicManager.playlist.value.any { it.title == track.name && it.artist == track.artistName }

            if (isDownloaded) {
                cell.moreBtn.setImageResource(R.drawable.ic_check)
                cell.moreBtn.setColorFilter(DesignEngine.Theme.colorBlue, android.graphics.PorterDuff.Mode.SRC_IN)
                cell.moreBtn.visibility = View.VISIBLE
                cell.loadingContainer.visibility = View.GONE
                cell.moreBtn.setOnClickListener(null)
            } else {
                cell.moreBtn.setImageResource(R.drawable.ic_download)
                cell.moreBtn.setColorFilter(DesignEngine.Theme.colorBlue, android.graphics.PorterDuff.Mode.SRC_IN)
                cell.moreBtn.visibility = View.VISIBLE
                cell.loadingContainer.visibility = View.GONE

                cell.moreBtn.setOnClickListener {
                    cell.moreBtn.visibility = View.GONE
                    cell.loadingContainer.visibility = View.VISIBLE
                    cell.progressBar.progress = 0
                    cell.progressText.text = "0%"

                    MusicManager.downloadAndAddTrack(this@MusicActivity, track.audio, track.name, track.artistName, track.image,
                        onProgress = { progress ->
                            cell.progressBar.progress = progress
                            cell.progressText.text = "$progress%"
                        },
                        onComplete = { success ->
                            cell.loadingContainer.visibility = View.GONE
                            if (success) {
                                cell.moreBtn.setImageResource(R.drawable.ic_check)
                                cell.moreBtn.visibility = View.VISIBLE
                                cell.moreBtn.setOnClickListener(null)
                                Toast.makeText(this@MusicActivity, "Успешно скачано!", Toast.LENGTH_SHORT).show()
                            } else {
                                cell.moreBtn.visibility = View.VISIBLE
                            }
                        }
                    )
                }
            }
        }
        override fun getItemCount() = tracks.size
    }

    inner class MyMusicAdapter(private val tracks: List<Track>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val cell = MusicCell(this@MusicActivity)
            cell.layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT)
            return object : RecyclerView.ViewHolder(cell) {}
        }
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val track = tracks[position]
            val cell = holder.itemView as MusicCell
            cell.setData(track.title, track.artist, track.albumArtUri)
            cell.moreBtn.setImageResource(R.drawable.ic_close_small)
            cell.moreBtn.setColorFilter(DesignEngine.Theme.colorLogout, android.graphics.PorterDuff.Mode.SRC_IN)
            cell.moreBtn.visibility = View.VISIBLE
            cell.loadingContainer.visibility = View.GONE

            cell.moreBtn.setOnClickListener {
                showCustomConfirmDialog(this@MusicActivity, "Удаление трека", "Вы уверены, что хотите удалить трек \"${track.title}\"?") {
                    MusicManager.deleteTrack(this@MusicActivity, track)
                    loadMyMusic()
                }
            }
            cell.setOnClickListener { MusicManager.playTrack(track) }
        }
        override fun getItemCount() = tracks.size
    }
}
