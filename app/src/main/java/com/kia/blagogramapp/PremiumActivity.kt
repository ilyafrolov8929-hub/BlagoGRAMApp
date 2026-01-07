package com.kia.blagogramapp

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

class PremiumActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private var selectedPriceIndex = 1 // Default to annual

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        DesignEngine.init(this)
        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        DesignEngine.Theme.apply(themePrefs.getBoolean("is_dark_theme", false), themePrefs.getBoolean("use_monet", false))

        // Прозрачный статус-бар
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        val root = FrameLayout(this).apply {
            setBackgroundColor(DesignEngine.COLOR_BACKGROUND)
        }

        // Фоновое свечение
        val bgGlow = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                colors = intArrayOf(DesignEngine.COLOR_CARD_BACKGROUND, 0x00000000)
                gradientType = GradientDrawable.RADIAL_GRADIENT
                gradientRadius = LayoutHelper.dp(400).toFloat()
            }
        }
        root.addView(bgGlow, LayoutHelper.createFrame(800, 800, Gravity.CENTER_HORIZONTAL or Gravity.TOP, 0, -200, 0, 0))

        ValueAnimator.ofFloat(0.9f, 1.1f).apply {
            duration = 5000
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val scale = it.animatedValue as Float
                bgGlow.scaleX = scale
                bgGlow.scaleY = scale
            }
            start()
        }

        val scrollView = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, LayoutHelper.dp(90), 0, LayoutHelper.dp(60))
        }

        // Заголовок
        val titleText = TextView(this).apply {
            text = "Lume Infinity"
            textSize = 28f
            typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
            setTextColor(DesignEngine.COLOR_TEXT_PRIMARY)
            gravity = Gravity.CENTER
        }
        content.addView(titleText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        val subtitleText = TextView(this).apply {
            text = "Раскрой безграничные возможности"
            textSize = 15f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            setTextColor(DesignEngine.COLOR_TEXT_SECONDARY)
            gravity = Gravity.CENTER
        }
        content.addView(subtitleText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, Gravity.CENTER_HORIZONTAL, 0, 4, 0, 30))

        // Пейджер преимуществ
        viewPager = ViewPager2(this).apply {
            adapter = FeaturesAdapter(getFeatures())
            offscreenPageLimit = 3
            setPageTransformer { page, position ->
                val absPos = abs(position)
                page.scaleY = 0.85f + (1 - absPos) * 0.15f
                page.scaleX = 0.85f + (1 - absPos) * 0.15f
                page.alpha = 0.4f + (1 - absPos) * 0.6f
            }
        }
        content.addView(viewPager, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 200, 0f, 0, 0, 0, 0, 0))

        // Индикаторы
        val dotsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        for (i in 0 until getFeatures().size) {
            val dot = View(this).apply {
                background = GradientDrawable().apply {
                    cornerRadius = LayoutHelper.dp(3).toFloat()
                    setColor(if (i == 0) DesignEngine.COLOR_BLUE else DesignEngine.COLOR_DIVIDER)
                }
            }
            dotsContainer.addView(dot, LayoutHelper.createLinear(if (i == 0) 14 else 5, 5, 0f, Gravity.CENTER_VERTICAL, 3, 0, 3, 0))
        }
        content.addView(dotsContainer, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, Gravity.CENTER_HORIZONTAL, 0, 15, 0, 30))

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                for (i in 0 until dotsContainer.childCount) {
                    val dot = dotsContainer.getChildAt(i)
                    val lp = dot.layoutParams as LinearLayout.LayoutParams
                    val isSelected = i == position
                    ValueAnimator.ofInt(dot.width, if (isSelected) LayoutHelper.dp(14) else LayoutHelper.dp(5)).apply {
                        duration = 200
                        addUpdateListener {
                            lp.width = it.animatedValue as Int
                            dot.layoutParams = lp
                        }
                        start()
                    }
                    (dot.background as GradientDrawable).setColor(if (isSelected) DesignEngine.COLOR_BLUE else DesignEngine.COLOR_DIVIDER)
                }
            }
        })

        // Секция цен
        content.addView(HeaderCell(this).apply { 
            text = "ВЫБЕРИТЕ ПОДПИСКУ" 
            alpha = 0.6f
        }, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 24, 0, 24, 8))

        val priceOptions = listOf(
            PriceOption("Ежемесячно", "249 ₽", "в месяц"),
            PriceOption("Ежегодно", "1 790 ₽", "149 ₽ / мес", "ЭКОНОМИЯ 40%"),
            PriceOption("На 2 года", "2 990 ₽", "125 ₽ / мес", "ЛУЧШАЯ ЦЕНА")
        )

        val priceViews = mutableListOf<PriceCardView>()
        priceOptions.forEachIndexed { index, option ->
            val card = PriceCardView(this, option, index == selectedPriceIndex).apply {
                setOnClickListener {
                    selectedPriceIndex = index
                    priceViews.forEachIndexed { i, view -> view.setChecked(i == index) }
                }
            }
            priceViews.add(card)
            content.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 16, 4, 16, 4))
        }

        // Кнопка оплаты
        val subscribeBtn = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(DesignEngine.COLOR_BLUE, 0xFF00C6FF.toInt())
            ).apply {
                cornerRadius = LayoutHelper.dp(16).toFloat()
            }
            val outValue = android.util.TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            foreground = AppCompatResources.getDrawable(context, outValue.resourceId)
            isClickable = true
        }
        val btnText = TextView(this).apply {
            text = "Подключить Lume Infinity"
            setTextColor(Color.WHITE)
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        subscribeBtn.addView(btnText, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER))
        
        content.addView(subscribeBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56, 0f, Gravity.CENTER_HORIZONTAL, 16, 25, 16, 10))

        val footerText = TextView(this).apply {
            text = "Подписка продлевается автоматически. Отмена в любой момент."
            textSize = 11f
            setTextColor(DesignEngine.COLOR_TEXT_SECONDARY)
            gravity = Gravity.CENTER
            alpha = 0.6f
        }
        content.addView(footerText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, Gravity.CENTER_HORIZONTAL, 40, 0, 40, 40))

        scrollView.addView(content)
        root.addView(scrollView)

        // Кнопка Назад
        val isDarkTheme = themePrefs.getBoolean("is_dark_theme", false)
        val backBtn = LinearLayout(this).apply {
            setBackgroundResource(R.drawable.bg_stat_pill)
            backgroundTintList = android.content.res.ColorStateList.valueOf(if (isDarkTheme) 0x30FFFFFF.toInt() else 0x20000000.toInt())
            setPadding(LayoutHelper.dp(10), LayoutHelper.dp(7), LayoutHelper.dp(10), LayoutHelper.dp(7))
            setOnClickListener { finish() }
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_back)
                setColorFilter(if (isDarkTheme) Color.WHITE else Color.BLACK)
                layoutParams = LinearLayout.LayoutParams(LayoutHelper.dp(18), LayoutHelper.dp(18))
            })
        }
        root.addView(backBtn, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP or Gravity.START, 16, 45, 0, 0))

        setContentView(root)
    }

    private fun getFeatures() = listOf(
        Feature("Безлимитное облако", "Файлы до 10 ГБ, неограниченный объём для ваших медиа", "☁️"),
        Feature("Темы и кастомизация", "Анимированные фоны, свои шрифты и кастомные звуки", "🎨"),
        Feature("Расширенные лимиты", "Больше папок, закрепов и длительность историй >24ч", "🚀"),
        Feature("Приоритет 24/7", "Ваши обращения рассматриваются в первую очередь", "🛡️"),
        Feature("Режим Инкогнито+", "Чтение без галочек и полное скрытие онлайн-статуса", "🕵️"),
        Feature("Реакции и стикеры", "Эксклюзивные наборы с эффектами свечения", "🔥"),
        Feature("Без рекламы", "Чистый интерфейс без рекламных вставок навсегда", "🚫"),
        Feature("Infinity Статус", "Особый значок и анимированная рамка профиля", "💎")
    )

    data class Feature(val title: String, val desc: String, val icon: String)
    data class PriceOption(val title: String, val price: String, val sub: String, val badge: String? = null)

    inner class FeaturesAdapter(private val items: List<Feature>) : RecyclerView.Adapter<FeaturesAdapter.ViewHolder>() {
        inner class ViewHolder(val cell: FeatureCard) : RecyclerView.ViewHolder(cell)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val cell = FeatureCard(parent.context)
            cell.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            return ViewHolder(cell)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.cell.setData(items[position])
        }
        override fun getItemCount() = items.size
    }

    class FeatureCard(context: Context) : FrameLayout(context) {
        private val iconContainer = FrameLayout(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0x10007AFF.toInt())
            }
        }
        private val iconView = TextView(context).apply { textSize = 40f; gravity = Gravity.CENTER }
        private val titleView = TextView(context).apply {
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(DesignEngine.COLOR_TEXT_PRIMARY)
            gravity = Gravity.CENTER
        }
        private val descView = TextView(context).apply {
            textSize = 14f
            setTextColor(DesignEngine.COLOR_TEXT_SECONDARY)
            gravity = Gravity.CENTER
            maxLines = 2
        }

        init {
            val inner = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(LayoutHelper.dp(20), 0, LayoutHelper.dp(20), 0)
            }
            iconContainer.addView(iconView, LayoutHelper.createFrame(70, 70, Gravity.CENTER))
            inner.addView(iconContainer, LayoutHelper.createLinear(80, 80, 0f, Gravity.CENTER_HORIZONTAL))
            inner.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, Gravity.CENTER_HORIZONTAL, 0, 12, 0, 0))
            inner.addView(descView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, Gravity.CENTER_HORIZONTAL, 0, 6, 0, 0))
            addView(inner, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))
        }
        fun setData(f: Feature) {
            iconView.text = f.icon
            titleView.text = f.title
            descView.text = f.desc
        }
    }

    class PriceCardView(context: Context, option: PriceOption, isChecked: Boolean) : FrameLayout(context) {
        private val bg = GradientDrawable().apply {
            setColor(DesignEngine.Theme.colorCardBackground)
            cornerRadius = LayoutHelper.dp(14).toFloat()
            setStroke(LayoutHelper.dp(2), if (isChecked) DesignEngine.COLOR_BLUE else Color.TRANSPARENT)
        }
        
        private val checkView = ImageView(context).apply {
            setImageResource(R.drawable.ic_double_check)
            setColorFilter(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(DesignEngine.COLOR_BLUE)
            }
            setPadding(LayoutHelper.dp(4), LayoutHelper.dp(4), LayoutHelper.dp(4), LayoutHelper.dp(4))
            visibility = if (isChecked) View.VISIBLE else View.INVISIBLE
        }

        init {
            background = bg
            setPadding(LayoutHelper.dp(14), LayoutHelper.dp(12), LayoutHelper.dp(14), LayoutHelper.dp(12))

            val content = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            
            val texts = LinearLayout(context).apply { 
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            }
            
            val titleRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            
            titleRow.addView(TextView(context).apply {
                text = option.title
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(DesignEngine.COLOR_TEXT_PRIMARY)
                isSingleLine = true
            })
            
            if (option.badge != null) {
                titleRow.addView(TextView(context).apply {
                    text = option.badge
                    textSize = 9f
                    isSingleLine = true
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.WHITE)
                    setPadding(LayoutHelper.dp(6), LayoutHelper.dp(1), LayoutHelper.dp(6), LayoutHelper.dp(1))
                    background = GradientDrawable().apply {
                        setColor(DesignEngine.Theme.colorBlue)
                        cornerRadius = LayoutHelper.dp(6).toFloat()
                    }
                }, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 8, 0, 0, 0))
            }
            
            texts.addView(titleRow)
            texts.addView(TextView(context).apply {
                text = option.sub
                textSize = 13f
                setTextColor(DesignEngine.COLOR_TEXT_SECONDARY)
                isSingleLine = true
            })

            content.addView(checkView, LayoutHelper.createLinear(22, 22, 0f, 0, 0, 0, 12, 0))
            content.addView(texts)
            content.addView(TextView(context).apply {
                text = option.price
                textSize = 16f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setTextColor(DesignEngine.COLOR_TEXT_PRIMARY)
                isSingleLine = true
            })

            addView(content)
            
            val outValue = android.util.TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            foreground = AppCompatResources.getDrawable(context, outValue.resourceId)
            isClickable = true
        }

        fun setChecked(checked: Boolean) {
            bg.setStroke(LayoutHelper.dp(2), if (checked) DesignEngine.COLOR_BLUE else Color.TRANSPARENT)
            checkView.visibility = if (checked) View.VISIBLE else View.INVISIBLE
            if (checked) {
                this.animate().scaleX(1.01f).scaleY(1.01f).setDuration(150).start()
            } else {
                this.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
            }
        }
    }
}
