package com.kia.blagogramapp.ui.modules

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kia.blagogramapp.LayoutHelper
import com.kia.blagogramapp.R
import com.vanniktech.emoji.ios.IosEmojiProvider

class EmojiKeyboardModule(
    context: Context,
    private val onEmojiClick: (String) -> Unit,
    private val onBackspaceClick: () -> Unit,
    private val onKeyboardClick: () -> Unit,
    private val onHeightChanged: ((Int) -> Unit)? = null
) : LinearLayout(context) {

    private val emojiRecycler: RecyclerView
    private val categoryBar: LinearLayout
    private val searchBar: LinearLayout
    private val searchEditText: EditText
    private val bottomTabs: LinearLayout
    
    private val provider = IosEmojiProvider()
    private var allEmojis = mutableListOf<com.vanniktech.emoji.Emoji>()
    private var filteredEmojis = mutableListOf<com.vanniktech.emoji.Emoji>()
    private val categoryStartPositions = mutableListOf<Int>()
    
    private var currentKeyboardHeight = LayoutHelper.dp(300)
    var isSearching = false
        private set

    // Загрузка вашего JSON файла с ключевыми словами
    private val emojiKeywords: Map<String, List<String>> by lazy {
        try {
            val json = context.assets.open("emoji_keywords.json").bufferedReader().use { it.readText() }
            Gson().fromJson(json, object : TypeToken<Map<String, List<String>>>() {}.type)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // Карта основ слов для автоматического перевода запросов (TG-Style)
    private val ruToEn = mapOf(
        "улыб" to "smile", "смех" to "laugh", "радост" to "joy", "весел" to "grin",
        "груст" to "sad", "печал" to "pensive", "плач" to "cry", "слез" to "tear",
        "зл" to "angry", "черт" to "devil", "сон" to "sleep", "устал" to "tired",
        "сердце" to "heart", "любов" to "love", "поцелу" to "kiss", "губы" to "kiss",
        "привет" to "wave", "пока" to "wave", "ладонь" to "hand",
        "оке" to "ok", "да" to "check", "хор" to "good", "лайк" to "thumbs",
        "огон" to "fire", "жар" to "hot", "холод" to "cold", "лед" to "ice", "снег" to "snow",
        "солн" to "sun", "лун" to "moon", "звезд" to "star", "дожд" to "rain", "облак" to "cloud",
        "цвет" to "flower", "роз" to "rose", "дерев" to "tree", "лист" to "leaf",
        "кот" to "cat", "собак" to "dog", "животн" to "animal", "обезьян" to "monkey",
        "еда" to "food", "бургер" to "burger", "пицц" to "pizza", "кофе" to "coffee", "пив" to "beer",
        "машин" to "car", "авто" to "car", "такс" to "taxi", "дом" to "house", "здан" to "building",
        "теле" to "phone", "комп" to "computer", "музык" to "music", "гитар" to "guitar", "песн" to "note",
        "спорт" to "sport", "мяч" to "ball", "футбол" to "soccer", "бег" to "run",
        "салют" to "firework", "фейерверк" to "firework", "праздник" to "celebrat", "торжеств" to "party", "шар" to "balloon",
        "деньг" to "money", "доллар" to "dollar", "кошелек" to "wallet", "лицо" to "face"
    )

    init {
        orientation = VERTICAL
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#1C1C1E"))
            val r = LayoutHelper.dp(24).toFloat()
            cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        }
        visibility = GONE

        val headerContainer = FrameLayout(context)
        
        categoryBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8))
        }

        searchBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = GONE
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#2C2C2E"))
                cornerRadius = LayoutHelper.dp(12).toFloat()
            }
            setPadding(LayoutHelper.dp(12), 0, LayoutHelper.dp(8), 0)
            
            val searchIcon = ImageView(context).apply {
                setImageResource(R.drawable.ic_search)
                setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN)
            }
            addView(searchIcon, LayoutHelper.createLinear(18, 18, gravity = Gravity.CENTER_VERTICAL))
            
            searchEditText = EditText(context).apply {
                hint = "Поиск эмодзи..."
                setHintTextColor(Color.GRAY)
                setTextColor(Color.WHITE)
                background = null
                textSize = 15f
                maxLines = 1
                isSingleLine = true
                gravity = Gravity.CENTER_VERTICAL
                setPadding(LayoutHelper.dp(8), 0, LayoutHelper.dp(8), 0)
            }
            addView(searchEditText, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, weight = 1f))
            
            val closeSearch = ImageView(context).apply {
                setImageResource(R.drawable.ic_close_small)
                setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN)
                setOnClickListener { toggleSearch(false) }
            }
            addView(closeSearch, LayoutHelper.createLinear(24, 24, gravity = Gravity.CENTER_VERTICAL))
        }

        headerContainer.addView(categoryBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 56))
        headerContainer.addView(searchBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 40, Gravity.CENTER, 16, 8, 16, 8))
        
        addView(headerContainer)

        emojiRecycler = RecyclerView(context).apply {
            layoutManager = GridLayoutManager(context, 8)
            clipToPadding = false
            setPadding(LayoutHelper.dp(4), 0, LayoutHelper.dp(4), LayoutHelper.dp(8))
        }
        addView(emojiRecycler, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, weight = 1f))

        bottomTabs = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, LayoutHelper.dp(8), 0, LayoutHelper.dp(12))
        }
        
        bottomTabs.addView(createSeparateTab("Эмодзи", true))
        bottomTabs.addView(createSeparateTab("GIF", false))
        bottomTabs.addView(createSeparateTab("Стикеры", false))
        
        val backspace = ImageView(context).apply {
            setImageResource(R.drawable.ic_backspace)
            setColorFilter(Color.parseColor("#8E8E93"), PorterDuff.Mode.SRC_IN)
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(8), LayoutHelper.dp(12), LayoutHelper.dp(8))
            setOnClickListener { onBackspaceClick() }
        }
        
        val kbToggle = ImageView(context).apply {
            setImageResource(R.drawable.ic_keyboard)
            setColorFilter(Color.parseColor("#8E8E93"), PorterDuff.Mode.SRC_IN)
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(8), LayoutHelper.dp(12), LayoutHelper.dp(8))
            setOnClickListener { onKeyboardClick() }
        }

        val bottomWrapper = FrameLayout(context)
        bottomWrapper.addView(LinearLayout(context).apply {
            addView(kbToggle, LayoutHelper.createLinear(48, 48))
            addView(View(context), LayoutHelper.createLinear(0, 0, 1f))
            addView(backspace, LayoutHelper.createLinear(48, 48))
        }, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL))
        bottomWrapper.addView(bottomTabs, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER))
        
        addView(bottomWrapper)

        initData()
        setupSearch()
    }

    private fun initData() {
        allEmojis.clear()
        categoryStartPositions.clear()
        
        val recentUnicodes = getRecentEmojis()
        val recentList = mutableListOf<com.vanniktech.emoji.Emoji>()
        val emojiMap = mutableMapOf<String, com.vanniktech.emoji.Emoji>()
        provider.categories.forEach { cat -> cat.emojis.forEach { e -> emojiMap[e.unicode] = e } }
        
        recentUnicodes.forEach { unicode -> emojiMap[unicode]?.let { recentList.add(it) } }

        if (recentList.isNotEmpty()) {
            categoryStartPositions.add(0)
            allEmojis.addAll(recentList)
        } else {
            categoryStartPositions.add(0)
        }

        provider.categories.forEach { category ->
            categoryStartPositions.add(allEmojis.size)
            allEmojis.addAll(category.emojis)
        }
        
        filteredEmojis.clear()
        filteredEmojis.addAll(allEmojis)
        setupEmojiList()
        setupCategoryBar()
    }

    private fun setupCategoryBar() {
        categoryBar.removeAllViews()
        val searchBtn = createCategoryIcon(R.drawable.ic_search, true) { toggleSearch(true) }
        categoryBar.addView(searchBtn)
        categoryBar.addView(View(context).apply { setBackgroundColor(Color.parseColor("#33FFFFFF")) }, LayoutHelper.createLinear(1, 24, gravity = Gravity.CENTER, left = 8, right = 8))

        val icons = listOf(R.drawable.ic_history, R.drawable.ic_emoji, R.drawable.ic_user, R.drawable.ic_music, R.drawable.ic_photo, R.drawable.ic_grid, R.drawable.ic_settings, R.drawable.ic_check)
        icons.forEachIndexed { index, resId ->
            val icon = createCategoryIcon(resId, false) {
                if (index < categoryStartPositions.size) (emojiRecycler.layoutManager as? GridLayoutManager)?.scrollToPositionWithOffset(categoryStartPositions[index], 0)
            }
            categoryBar.addView(icon, LayoutHelper.createLinear(0, 40, weight = 1f))
        }
    }

    private fun createCategoryIcon(resId: Int, isCircle: Boolean, onClick: () -> Unit): View {
        val container = FrameLayout(context).apply {
            if (isCircle) background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#2C2C2E")) }
            setOnClickListener { onClick() }
        }
        val img = ImageView(context).apply {
            setImageResource(resId); setColorFilter(if (isCircle) Color.WHITE else Color.parseColor("#8E8E93"), PorterDuff.Mode.SRC_IN); setPadding(LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8))
        }
        container.addView(img, LayoutHelper.createFrame(36, 36, Gravity.CENTER))
        return container
    }

    private fun createSeparateTab(text: String, isSelected: Boolean): View {
        return TextView(context).apply {
            this.text = text; textSize = 13f; typeface = Typeface.DEFAULT_BOLD; setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#8E8E93")); gravity = Gravity.CENTER; setPadding(LayoutHelper.dp(16), LayoutHelper.dp(8), LayoutHelper.dp(16), LayoutHelper.dp(8))
            background = GradientDrawable().apply { setColor(if (isSelected) Color.parseColor("#48484A") else Color.parseColor("#2C2C2E")); cornerRadius = LayoutHelper.dp(18).toFloat() }
            layoutParams = LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, left = 4, right = 4)
        }
    }

    private fun toggleSearch(show: Boolean) {
        if (isSearching == show) return
        isSearching = show
        searchBar.visibility = if (show) View.VISIBLE else View.GONE
        categoryBar.visibility = if (show) View.GONE else View.VISIBLE
        val targetHeight = if (show) LayoutHelper.dp(500) else currentKeyboardHeight
        animateHeightTo(targetHeight)
        if (show) {
            searchEditText.requestFocus()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT)
        } else {
            searchEditText.text.clear(); updateFilter(""); val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager; imm.hideSoftInputFromWindow(searchEditText.windowToken, 0)
        }
    }

    private fun animateHeightTo(target: Int) {
        val startHeight = height
        ValueAnimator.ofInt(startHeight, target).apply {
            duration = 250
            addUpdateListener { val v = it.animatedValue as Int; layoutParams?.height = v; requestLayout(); onHeightChanged?.invoke(v) }
            start()
        }
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = updateFilter(s?.toString() ?: "")
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun updateFilter(query: String) {
        filteredEmojis.clear()
        if (query.isEmpty()) {
            filteredEmojis.addAll(allEmojis)
        } else {
            val q = query.lowercase().trim()
            
            // Находим английские эквиваленты для русского запроса
            val searchTerms = mutableSetOf<String>()
            searchTerms.add(q)
            ruToEn.forEach { (ru, en) ->
                if (q.contains(ru) || ru.contains(q)) {
                    searchTerms.add(en)
                }
            }

            val matchedUnicodes = mutableSetOf<String>()
            
            // Поиск по вашему JSON файлу (интеграция тегов)
            emojiKeywords.forEach { (emoji, keywords) ->
                if (keywords.any { keyword -> 
                    searchTerms.any { term -> keyword.contains(term, ignoreCase = true) }
                }) {
                    matchedUnicodes.add(emoji)
                }
            }
            
            filteredEmojis.addAll(allEmojis.filter { emoji ->
                matchedUnicodes.contains(emoji.unicode) || emoji.unicode.contains(q)
            })
        }
        emojiRecycler.adapter?.notifyDataSetChanged()
    }

    private fun setupEmojiList() {
        emojiRecycler.adapter = object : RecyclerView.Adapter<EmojiViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiViewHolder {
                val iv = ImageView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER; setPadding(LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8)); layoutParams = RecyclerView.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.dp(48)) }
                return EmojiViewHolder(iv)
            }
            override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
                val emoji = filteredEmojis[position]; val iv = holder.itemView as ImageView; iv.setImageDrawable(provider.getDrawable(emoji, context)); iv.setOnClickListener { saveRecentEmoji(emoji.unicode); onEmojiClick(emoji.unicode) }
            }
            override fun getItemCount() = filteredEmojis.size
        }
    }

    private fun saveRecentEmoji(unicode: String) {
        val prefs = context.getSharedPreferences("emoji_prefs", Context.MODE_PRIVATE); val recent = prefs.getString("recent_emojis", "")?.split(",")?.toMutableList() ?: mutableListOf(); recent.remove(unicode); recent.add(0, unicode); if (recent.size > 50) recent.removeAt(recent.size - 1); prefs.edit().putString("recent_emojis", recent.filter { it.isNotEmpty() }.joinToString(",")).apply(); initData()
    }

    private fun getRecentEmojis(): List<String> = context.getSharedPreferences("emoji_prefs", Context.MODE_PRIVATE).getString("recent_emojis", "")?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()

    class EmojiViewHolder(view: View) : RecyclerView.ViewHolder(view)

    fun setHeight(height: Int) {
        val h = if (height <= 0) LayoutHelper.dp(300) else height; currentKeyboardHeight = h
        if (!isSearching && layoutParams?.height != h) { layoutParams?.height = h; requestLayout(); onHeightChanged?.invoke(h) }
    }
}
