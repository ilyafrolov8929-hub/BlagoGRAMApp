# Документация разработчика DesignEngine

> **Версия:** 1.0  
> **Проект:** BlagoGRAM App  
> **Язык:** Kotlin  

---

## Введение
**DesignEngine** — это собственная библиотека для создания интерфейсов в коде, без использования XML-файлов разметки. Она позволяет:
1.  Управлять темами (светлая/темная) из единого центра.
2.  Создавать интерфейсы быстрее за счет переиспользования компонентов (Cells).
3.  Получать максимальную производительность (нет парсинга XML).

Эта документация подробно описывает, как работать с движком.

---

## Оглавление
1.  [Быстрый старт](#1-быстрый-старт)
2.  [Работа с цветами и темами](#2-работа-с-цветами-и-темами)
3.  [LayoutHelper: Верстка без боли](#3-layouthelper-верстка-без-боли)
4.  [Создание экранов (Activity)](#4-создание-экранов-activity)
5.  [Создание собственных компонентов (Cells)](#5-создание-собственных-компонентов-cells)
6.  [Справочник существующих ячеек](#6-справочник-существующих-ячеек)
7.  [Частые вопросы (FAQ)](#7-частые-вопросы-faq)

---

## 1. Быстрый старт

Чтобы начать использовать движок в новой Activity, нужно выполнить 3 шага:

### Шаг 1. Инициализация в `onCreate`
В самом начале метода `onCreate` вызови инициализацию движка и примени тему.

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // 1. Инициализируем расчет пикселей (плотность экрана)
    DesignEngine.init(this)
    
    // 2. Получаем настройки темы (сохраняем их где-то в SharedPreferences)
    val isDark = getSharedPreferences("app_theme", MODE_PRIVATE)
                     .getBoolean("is_dark_theme", false)
                     
    // 3. Применяем цвета
    DesignEngine.Theme.apply(isDark)
    
    // Дальше идет верстка...
}
```

### Шаг 2. Создание корневого контейнера
Вместо `setContentView(R.layout.my_layout)` создай контейнер программно:

```kotlin
val root = FrameLayout(this)
root.setBackgroundColor(DesignEngine.Theme.colorBackground) // Фон из темы
setContentView(root)
```

Теперь у тебя есть пустой экран с правильным цветом фона.

---

## 2. Работа с цветами и темами

Все цвета хранятся в объекте `DesignEngine.Theme`. **Никогда** не используй хардкод цвета (например `Color.WHITE` или `#000000`), если хочешь, чтобы смена темы работала корректно.

### Как получить цвет?
Используй переменные из `DesignEngine`:

*   `DesignEngine.COLOR_BACKGROUND` — основной фон экрана.
*   `DesignEngine.COLOR_TEXT_PRIMARY` — основной текст (черный/белый).
*   `DesignEngine.COLOR_TEXT_SECONDARY` — серый текст (подсказки, даты).
*   `DesignEngine.COLOR_BLUE` — акцентный цвет (кнопки, ссылки).
*   `DesignEngine.COLOR_DIVIDER` — цвет разделительных линий.

Пример назначения цвета тексту:
```kotlin
val tv = TextView(context)
tv.setTextColor(DesignEngine.COLOR_TEXT_PRIMARY)
```

### Как добавить новый цвет?
1.  Открой файл `DesignEngine.kt`.
2.  Найди объект `Theme`.
3.  Добавь переменную (например, `var colorWarning: Int`).
4.  В методе `apply(isDark: Boolean)` задай значения для `if (isDark)` и `else`.

---

## 3. LayoutHelper: Верстка без боли

Класс `LayoutHelper` — это твой главный инструмент. Он генерирует `LayoutParams` (параметры расположения) одной строкой.

### Основные методы

#### `dp(value: Int): Int`
Преобразует dp в пиксели.
*   Пример: `LayoutHelper.dp(16)` — вернет 16dp в пикселях.

#### `createLinear(...)`
Создает параметры для элементов внутри `LinearLayout`.

**Синтаксис:**
```kotlin
createLinear(width, height, weight, gravity, left, top, right, bottom)
```
*   `width`: Ширина (или `LayoutHelper.MATCH_PARENT` / `WRAP_CONTENT`).
*   `height`: Высота.
*   `weight`: Вес (0f по умолчанию).
*   `gravity`: Гравитация (Gravity.CENTER и т.д.).
*   `left, top...`: Отступы (margins) в dp.

**Пример:** Кнопка на всю ширину с отступами 20dp.
```kotlin
container.addView(button, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 50, 0f, 0, 20, 10, 20, 10))
```

#### `createFrame(...)`
Создает параметры для элементов внутри `FrameLayout`.

**Синтаксис:**
```kotlin
createFrame(width, height, gravity, left, top, right, bottom)
```
**Пример:** Текст по центру экрана.
```kotlin
root.addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER))
```

---

## 4. Создание экранов (Activity)

Пример создания простого экрана с заголовком и кнопкой.

```kotlin
class DemoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DesignEngine.init(this)
        DesignEngine.Theme.apply(false) // Принудительно светлая для примера

        // 1. Root
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(DesignEngine.COLOR_BACKGROUND)
        root.gravity = Gravity.CENTER // Все по центру
        setContentView(root)

        // 2. Заголовок
        val title = TextView(this)
        title.text = "Привет, DesignEngine!"
        title.textSize = 24f // Размер шрифта
        title.typeface = Typeface.DEFAULT_BOLD
        title.setTextColor(DesignEngine.COLOR_TEXT_PRIMARY)
        
        // Добавляем с отступом снизу 20dp
        root.addView(title, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 20))

        // 3. Кнопка
        val btn = TextView(this)
        btn.text = "Нажми меня"
        btn.setTextColor(Color.WHITE)
        btn.gravity = Gravity.CENTER
        btn.setBackgroundColor(DesignEngine.COLOR_BLUE)
        // Скругление кнопки через GradientDrawable
        btn.background = GradientDrawable().apply {
            setColor(DesignEngine.COLOR_BLUE)
            cornerRadius = LayoutHelper.dp(8).toFloat()
        }

        // Кнопка размером 200x50
        root.addView(btn, LayoutHelper.createLinear(200, 50))
    }
}
```

---

## 5. Создание собственных компонентов (Cells)

В `DesignEngine` принято выносить повторяющиеся элементы в отдельные классы — **Cells** (Ячейки). Это аналог `<include layout="..." />` в XML.

### Алгоритм создания Cell
1.  Создай класс (например `MyCustomCell`), наследуемый от `FrameLayout` или `LinearLayout`.
2.  В блоке `init { ... }` создай и добавь все View (иконки, тексты).
3.  Создай метод `setData(...)` для заполнения данными.

### Пример: Ячейка с иконкой и переключателем (Switch)

```kotlin
class SwitchCell(context: Context) : FrameLayout(context) {
    
    val textView: TextView
    val switchView: Switch

    init {
        // Паддинги ячейки
        setPadding(LayoutHelper.dp(16), 0, LayoutHelper.dp(16), 0)

        // Текст слева
        textView = TextView(context)
        textView.textSize = 16f
        textView.setTextColor(DesignEngine.COLOR_TEXT_PRIMARY)
        addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL or Gravity.START))

        // Свитч справа
        switchView = Switch(context)
        addView(switchView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL or Gravity.END))
    }

    fun setData(text: String, isChecked: Boolean) {
        textView.text = text
        switchView.isChecked = isChecked
    }
}
```

**Использование:**
```kotlin
val mySwitch = SwitchCell(context)
mySwitch.setData("Темная тема", true)
layout.addView(mySwitch, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 50))
```

---

## 6. Справочник существующих ячеек

В проекте уже есть готовые ячейки (в файле `DesignEngine.kt`):

| Название класса | Описание |
| :--- | :--- |
| **HeaderCell** | Серый заголовок секции (например, "НАСТРОЙКИ"). |
| **TextCell** | Стандартный пункт меню: Иконка + Текст + Стрелка. |
| **ProfileHeaderCell** | Шапка профиля с большим аватаром. |
| **ContainerCell** | Белая подложка с закругленными углами для группировки ячеек. |
| **SpacerCell** | Пустое место (отступ) заданной высоты. |
| **ChatCell** | **(NEW)** Ячейка чата: Аватар, Имя, Сообщение, Время, Бейдж. |

---

## 7. Частые вопросы (FAQ)

**В: Как сделать тень?**  
О: Используй свойство `elevation`.
```kotlin
view.elevation = LayoutHelper.dp(4).toFloat()
```
*Примечание: для тени у View должен быть фон (background).*

**В: Как загрузить картинку?**  
О: Используем библиотеку Coil.
```kotlin
imageView.load("https://...")
```

**В: Как обработать клик?**  
О: Как обычно.
```kotlin
cell.setOnClickListener { 
    Toast.makeText(context, "Clicked!", Toast.LENGTH_SHORT).show() 
}
```
Не забудь добавить `isClickable = true` и `background` (ripple) в самой ячейке, чтобы был визуальный эффект нажатия.

---
*Документация обновлена автоматически.*
