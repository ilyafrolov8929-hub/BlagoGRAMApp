package com.kia.blagogramapp

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.kia.blagogramapp.api.ApiClient
import com.kia.blagogramapp.api.ApiRepository
import com.kia.blagogramapp.api.SubscriptionCheckResult
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class PremiumActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private var selectedPriceIndex = 1
    private lateinit var apiRepository: ApiRepository
    private var token: String = ""

    private lateinit var pricingContainer: LinearLayout
    private lateinit var statusContainer: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DesignEngine.init(this)
        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        DesignEngine.Theme.apply(themePrefs.getBoolean("is_dark_theme", false), themePrefs.getBoolean("use_monet", false))

        token = getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
        apiRepository = ApiRepository(ApiClient.retrofit.create(BlagoApi::class.java))

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

        // Заголовки
        content.addView(TextView(this).apply {
            text = "Lume Infinity"
            textSize = 28f
            typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
            setTextColor(DesignEngine.COLOR_TEXT_PRIMARY)
            gravity = Gravity.CENTER
        }, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        content.addView(TextView(this).apply {
            text = "Раскрой безграничные возможности"
            textSize = 15f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            setTextColor(DesignEngine.COLOR_TEXT_SECONDARY)
            gravity = Gravity.CENTER
        }, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, Gravity.CENTER_HORIZONTAL, 0, 4, 0, 30))

        // Пейджер
        viewPager = ViewPager2(this).apply {
            adapter = FeaturesAdapter(getFeatures())
            offscreenPageLimit = 1
            clipToPadding = false
            clipChildren = false
            setPadding(LayoutHelper.dp(45), 0, LayoutHelper.dp(45), 0)
            setPageTransformer { page, position ->
                val absPos = abs(position)
                page.rotationY = position * 40f
                page.scaleX = 1f - (absPos * 0.15f)
                page.scaleY = 1f - (absPos * 0.15f)
                page.translationX = -position * LayoutHelper.dp(25)
                page.alpha = (1.0f - absPos * 1.1f).coerceAtLeast(0f)
            }
        }
        content.addView(viewPager, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 200))

        // Точки
        val dotsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        for (i in 0 until getFeatures().size) {
            dotsContainer.addView(View(this).apply {
                background = GradientDrawable().apply {
                    cornerRadius = LayoutHelper.dp(3).toFloat()
                    setColor(if (i == 0) DesignEngine.COLOR_BLUE else DesignEngine.COLOR_DIVIDER)
                }
            }, LayoutHelper.createLinear(if (i == 0) 14 else 5, 5, 0f, Gravity.CENTER_VERTICAL, 3, 0, 3, 0))
        }
        content.addView(dotsContainer, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, Gravity.CENTER_HORIZONTAL, 0, 15, 0, 30))

        // Индикатор загрузки
        progressBar = ProgressBar(this).apply {
            visibility = View.VISIBLE
        }
        content.addView(progressBar, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, Gravity.CENTER_HORIZONTAL, 0, 20, 0, 20))

        // --- PRICING SECTION ---
        pricingContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        pricingContainer.addView(HeaderCell(this).apply {
            text = "ВЫБЕРИТЕ ПОДПИСКУ"
            alpha = 0.6f
        }, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 24, 0, 24, 8))

        val priceOptions = listOf(
            PriceOption("Ежемесячно", "150 ₽", "в месяц"),
            PriceOption("Ежегодно", "1 080 ₽", "90 ₽ / мес", "ЭКОНОМИЯ 40%"),
            PriceOption("Навсегда", "1 800 ₽", "Разово", "ЛУЧШАЯ ЦЕНА")
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
            pricingContainer.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 16, 4, 16, 4))
        }

        val subscribeBtn = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(DesignEngine.COLOR_BLUE, 0xFF00C6FF.toInt())
            ).apply { cornerRadius = LayoutHelper.dp(16).toFloat() }
            isClickable = true
            addView(TextView(context).apply {
                text = "Подключить Lume Infinity"
                setTextColor(Color.WHITE)
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            }, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER))

            setOnClickListener {
                val plans = listOf("month", "year", "lifetime")
                performPurchase(plans[selectedPriceIndex])
            }
        }
        pricingContainer.addView(subscribeBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56, 0f, Gravity.CENTER_HORIZONTAL, 16, 25, 16, 10))
        content.addView(pricingContainer)

        // --- STATUS SECTION ---
        statusContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            visibility = View.GONE
            setPadding(LayoutHelper.dp(32), LayoutHelper.dp(32), LayoutHelper.dp(32), LayoutHelper.dp(32))
            background = GradientDrawable().apply {
                setColor(DesignEngine.COLOR_CARD_BACKGROUND)
                cornerRadius = LayoutHelper.dp(24).toFloat()
            }
        }
        statusContainer.addView(TextView(this).apply {
            text = "💎"
            textSize = 54f
            gravity = Gravity.CENTER
        }, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, Gravity.CENTER_HORIZONTAL))

        statusText = TextView(this).apply {
            textSize = 16f
            setTextColor(DesignEngine.COLOR_TEXT_PRIMARY)
            gravity = Gravity.CENTER
            setLineSpacing(LayoutHelper.dp(6).toFloat(), 1f)
        }
        statusContainer.addView(statusText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 20, 0, 0))
        content.addView(statusContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 20, 20, 20, 20))

        content.addView(TextView(this).apply {
            text = "Подписка продлевается автоматически. Отмена в любой момент."
            textSize = 11f
            setTextColor(DesignEngine.COLOR_TEXT_SECONDARY)
            gravity = Gravity.CENTER
            alpha = 0.6f
        }, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, Gravity.CENTER_HORIZONTAL, 40, 0, 40, 40))

        scrollView.addView(content)
        root.addView(scrollView)

        val isDarkThemeNow = themePrefs.getBoolean("is_dark_theme", false)
        val backBtn = LinearLayout(this).apply {
            setBackgroundResource(R.drawable.bg_stat_pill)
            backgroundTintList = android.content.res.ColorStateList.valueOf(if (isDarkThemeNow) 0x30FFFFFF.toInt() else 0x20000000.toInt())
            setPadding(LayoutHelper.dp(10), LayoutHelper.dp(7), LayoutHelper.dp(10), LayoutHelper.dp(7))
            setOnClickListener { finish() }
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_back)
                setColorFilter(if (isDarkThemeNow) Color.WHITE else Color.BLACK)
                layoutParams = LinearLayout.LayoutParams(LayoutHelper.dp(18), LayoutHelper.dp(18))
            })
        }
        root.addView(backBtn, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP or Gravity.START, 16, 45, 0, 0))

        setContentView(root)
        checkSubscription()
    }

    private fun checkSubscription() {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            pricingContainer.visibility = View.GONE
            statusContainer.visibility = View.GONE
            try {
                val sub = apiRepository.checkSubscription(token)
                updateUI(sub)
            } catch (e: Exception) {
                updateUI(null)
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun performPurchase(plan: String) {
        lifecycleScope.launch {
            try {
                val result = apiRepository.buySubscription(token, plan)
                if (result != null) {
                    Toast.makeText(this@PremiumActivity, "Успешно: ${result.message}", Toast.LENGTH_SHORT).show()
                    updateUI(SubscriptionCheckResult(active = 1, plan = result.plan, expires_at = result.expires_at))
                }
            } catch (e: Exception) {
                Toast.makeText(this@PremiumActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatDate(dateStr: String?): String {
        if (dateStr == null) return ""
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = sdf.parse(dateStr)
            val outputSdf = SimpleDateFormat("d MMMM yyyy", Locale("ru"))
            outputSdf.format(date!!)
        } catch (e: Exception) {
            dateStr ?: ""
        }
    }

    private fun updateUI(sub: SubscriptionCheckResult?) {
        android.util.Log.d("PremiumAPI", "Sub result: $sub")

        val isActive = when (val a = sub?.active) {
            is Boolean -> a
            is Number -> a.toInt() == 1
            is String -> a == "1" || a == "true"
            else -> false
        }

        // Сохраняем статус для других активити
        getSharedPreferences("app", MODE_PRIVATE).edit()
            .putBoolean("is_premium", isActive)
            .apply()

        if (isActive) {
            pricingContainer.visibility = View.GONE
            statusContainer.visibility = View.VISIBLE

            val planName = when(sub?.plan) {
                "month" -> "Ежемесячная"
                "year" -> "Ежегодная"
                "lifetime" -> "Пожизненная"
                else -> sub?.plan ?: "Активна"
            }

            val builder = SpannableStringBuilder()

            val title = "Ваша подписка Lume Infinity активна!"
            builder.append(title)
            builder.setSpan(StyleSpan(Typeface.BOLD), 0, title.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            builder.append("\n\nТариф: ")
            val startPlan = builder.length
            builder.append(planName)
            builder.setSpan(StyleSpan(Typeface.BOLD), startPlan, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.setSpan(ForegroundColorSpan(DesignEngine.COLOR_BLUE), startPlan, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            if (sub?.expires_at != null) {
                builder.append("\nДействует до: ")
                val startDate = builder.length
                builder.append(formatDate(sub.expires_at))
                builder.setSpan(StyleSpan(Typeface.BOLD), startDate, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            statusText.text = builder
        } else {
            pricingContainer.visibility = View.VISIBLE
            statusContainer.visibility = View.GONE
        }
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
        override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.cell.setData(items[position])
        override fun getItemCount() = items.size
    }

    class FeatureCard(context: Context) : FrameLayout(context) {
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
                addView(iconView, LayoutHelper.createLinear(80, 80, 0f, Gravity.CENTER_HORIZONTAL))
                addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, Gravity.CENTER_HORIZONTAL, 0, 12, 0, 0))
                addView(descView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, Gravity.CENTER_HORIZONTAL, 0, 6, 0, 0))
            }
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
            setImageResource(R.drawable.ic_check)
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
            val texts = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
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
            if (checked) this.animate().scaleX(1.01f).scaleY(1.01f).setDuration(150).start()
            else this.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
        }
    }
}
