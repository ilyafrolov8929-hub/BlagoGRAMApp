# Документация разработчика BlagoEngine

> **Версия:** 1.1  
> **Проект:** BlagoGRAM App  
> **Язык:** Kotlin  

---

## Введение
**DesignEngine** — это собственная библиотека для создания интерфейсов в коде, без использования XML-файлов разметки. Она позволяет:
1.  Управлять темами (светлая/темная + Material You) из единого центра.
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

### Шаг 1. Инициализация в `onCreate`
Инициализацию движка нужно делать один раз (обычно в `Application` или в стартовой `Activity`). Теперь поддерживается Material You (динамические цвета Android 12+).

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // 1. Инициализируем контекст и расчет пикселей
    DesignEngine.init(this)
    
    // 2. Получаем настройки темы
    val isDark = true 
    val isMonet = true // Включить Material You
                     
    // 3. Применяем цвета
    DesignEngine.Theme.apply(isDark, isMonet)
}
```

### Шаг 2. Создание корневого контейнера
Вместо `setContentView(R.layout.my_layout)` создай контейнер программно:

```kotlin
val root = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    backgroundColor = DesignEngine.COLOR_BACKGROUND
}
setContentView(root)
```

---

## 2. Работа с цветами и темами

Все цвета доступны через геттеры в `DesignEngine`. Они автоматически меняются при вызове `Theme.apply()`.

### Список основных цветов
*   `COLOR_BACKGROUND` — общий фон приложения.
*   `COLOR_SHEET_BACKGROUND` — фон для "шторок" и нижних панелей.
*   `COLOR_CARD_BACKGROUND` — фон карточек и сгруппированных ячеек.
*   `COLOR_TEXT_PRIMARY` — основной текст.
*   `COLOR_TEXT_SECONDARY` — второстепенный текст.
*   `COLOR_ICON_PRIMARY` — цвет иконок.
*   `COLOR_BLUE` — акцентный цвет.
*   `COLOR_DIVIDER` — цвет разделителей.
*   `COLOR_LOGOUT` — красный цвет для опасных действий.

### Темы плеера
Движок поддерживает переключение стилей плеера через `DesignEngine.PlayerTheme`:
*   `DEFAULT` — классический вид.
*   `DYNAMIC` — адаптация под цвета обложки.
*   `BLUR` — размытый фон из обложки.

---

## 3. LayoutHelper: Верстка без боли

### Основные методы

#### `dp(value: Int): Int`
Преобразует dp в пиксели согласно плотности экрана.

#### `createLinear(width, height, weight, gravity, left, top, right, bottom)`
Параметры для `LinearLayout`.

#### `createFrame(width, height, gravity, left, top, right, bottom)`
Параметры для `FrameLayout`.

#### `createRelative(width, height, left, top, right, bottom)`
Параметры для `RelativeLayout`.

---

## 4. Создание экранов (Activity)

При создании UI кодом рекомендуется использовать `LinearLayout` с `MATCH_PARENT` как корневой элемент и добавлять в него ячейки.

```kotlin
val layout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    backgroundColor = DesignEngine.COLOR_BACKGROUND
}

// Добавляем заголовок
layout.addView(HeaderCell(this).apply { text = "АККАУНТ" })

// Добавляем контейнер с ячейками
val container = ContainerCell(this)
container.addCell(TextCell(this).apply { setData("Настройки", R.drawable.ic_settings) })
container.addCell(TextCell(this).apply { setData("Выход", R.drawable.ic_logout) }, isLast = true)

layout.addView(container, LayoutHelper.createLinear(MATCH_PARENT, WRAP_CONTENT, 0f, 0, 16, 8, 16, 8))
```

---

## 5. Создание собственных компонентов (Cells)

Алгоритм остается прежним: наследование от `FrameLayout`/`LinearLayout` + использование `LayoutHelper`.

**Рекомендация:** используйте `foreground` с селектором для визуального отклика на нажатие:
```kotlin
val outValue = TypedValue()
context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
foreground = context.getDrawable(outValue.resourceId)
isClickable = true
```

---

## 6. Справочник существующих ячеек

| Название класса | Описание | Особенности |
| :--- | :--- | :--- |
| **HeaderCell** | Заголовок секции. | Текст в CAPS, мелкий шрифт, серый цвет. |
| **TextCell** | Стандартная строка меню. | Поддерживает `setIconColor`, `setTextColor`, имеет `deleteView` (крестик). |
| **MusicCell** | **(NEW)** Ячейка трека. | Отображает обложку, название, артиста и **прогресс загрузки**. |
| **ChatCell** | **(NEW)** Ячейка чата. | Аватар, имя, последнее сообщение, время и **счетчик непрочитанных**. |
| **ProfileHeaderCell**| Блок профиля. | Большой аватар, имя и статус. Метод `setData`. |
| **ContainerCell** | Группировщик ячеек. | Авто-разделители. Используйте `addCell(view, isLast = true)` для последней ячейки. |
| **SpacerCell** | Отступ. | Просто пустое пространство заданной высоты. |

---

## 7. Частые вопросы (FAQ)

**В: Как работает авто-разделитель в ContainerCell?**  
О: Он добавляется автоматически после каждой ячейки, если при вызове `addCell` параметр `isLast` равен `false` (по умолчанию). Отступ линии слева — 56dp (под иконку).

**В: Как включить темную тему?**  
О: Вызовите `DesignEngine.Theme.apply(true, false)`. Все компоненты, использующие `DesignEngine.COLOR_*`, обновятся автоматически.

**В: Можно ли менять размер текста в HeaderCell?**  
О: Да, через свойство `textSize`, но по умолчанию там установлено 12f для соответствия стилю Telegram.

---
*Документация актуальна для DesignEngine v1.1.*
