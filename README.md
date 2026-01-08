# 📘 BlagoGRAM App v2.0 — Полная техническая спецификация

Проект построен на движке **BlagoEngine** (UI-in-Code). Ниже приведено описание каждого модуля, класса, метода и переменной.

---

## 🏗 1. Глобальный движок: DesignEngine.kt

### 1.1 Объект `DesignEngine`
Центральный синглтон для управления состоянием UI.
*   `density: Float` — Коэффициент плотности пикселей. Используется для адаптации размеров под разные экраны.
*   `context: Context?` — Ссылка на Application Context для доступа к ресурсам из любого места.
*   `IC_PREMIUM: Int` — Ссылка на ресурс иконки премиума (`R.drawable.ic_premium_diamond`).
*   `init(ctx: Context)` — Метод инициализации. Устанавливает `density` и `context`.
*   `createPremiumBadge(context): ImageView` — **(New 2.0)** Создает настроенный ImageView с иконкой алмаза, синим цветом и `translationY` (смещение на 2dp вниз для выравнивания с текстом).
*   `setPremiumStatus(badge, isPremium)` — Метод управления видимостью значка.
*   **Геттеры цветов** (`COLOR_BACKGROUND`, `COLOR_BLUE` и др.) — Передают актуальные цвета из объекта `Theme`.

### 1.2 Объект `DesignEngine.Theme`
Отвечает за цветовую палитру.
*   `apply(isDark, isMonet)` — Применяет цветовую схему. Содержит логику для Monet (динамические цвета Android 12) и стандартные палитры.
*   **Переменные цветов** (например, `colorBackground`, `colorTextPrimary`) — Хранят текущие HEX-значения цветов в формате `Int`.

### 1.3 Объект `LayoutHelper`
Утилита для создания LayoutParams.
*   `createLinear(...)` / `createFrame(...)` / `createRelative(...)` — Генерируют параметры для соответствующих контейнеров.
*   `dp(value: Int)` — Математическая функция: `value * density`.

---

## 🧱 2. Компоненты (Cells) — Детальное описание

### 2.1 `ChatCell` (Ячейка чата)
*   `avatarView: ShapeableImageView` — Отображение аватара.
*   `nameView: TextView` — Имя пользователя (жирный шрифт).
*   `messageView: TextView` — Текст последнего сообщения.
*   `unreadBadge: TextView` — Круглый индикатор с числом непрочитанных.
*   `pinView: ImageView` — Иконка "закрепленного" чата.
*   `setData(...)` — Метод заполнения данными. Если `avatar == "action_saved"`, подставляет иконку Избранного.

### 2.2 `TextCell` (Ячейка настроек)
*   `imageView` / `textView` — Иконка и текст пункта меню.
*   `chevronView` — Иконка стрелочки вправо.
*   `deleteView` — Иконка корзины (по умолчанию скрыта).
*   `setData(text, iconResId)` — Базовая инициализация.
*   `setIconColor(color)` / `setTextColor(color)` — Методы кастомизации.

### 2.3 `ContainerCell`
*   `addCell(view, isLast)` — Основной метод. Если `isLast = false`, добавляет `View` разделителя с отступом 56dp слева.

---

## 🎵 3. Музыкальный менеджер: MusicManager.kt

*   `currentTrack: MutableStateFlow<Track?>` — Переменная-поток с текущим треком. UI подписывается на неё для обновления плеера.
*   `isPlaying: MutableStateFlow<Boolean>` — Статус проигрывания.
*   `playlist: MutableStateFlow<List<Track>>` — Список всех доступных треков.
*   `playTrack(track)` — Начинает воспроизведение, обновляет `currentTrack`.
*   `pauseTrack()` — Ставит на паузу.
*   `scanMusic(context)` — Сканирует папку `/music` во внутреннем хранилище и заполняет `playlist`.

---

## 📱 4. Модули UI (ui.modules)

### 4.1 `SideMenuModule` (Боковое меню)
*   `drawerPanel: LinearLayout` — Сама выезжающая панель.
*   `create(root, statusBarHeight)` — Инициализирует панель, настраивает шапку с аватаром и именем.
*   `addItem(text, icon, onClick)` — Создает новый пункт меню с иконкой и действием.
*   `updateHeader(...)` — Обновляет данные пользователя и статус премиума.

### 4.2 `ProfileLayout` (Профиль)
*   `profileCover` / `profileAvatar` — Изображения в шапке.
*   `switchTab(index)` — Логика переключения контента. Анимирует `tabSelector` (полоска под вкладками).
*   `updateMusicBadge(track, isPlaying)` — Управляет плашкой "Слушает сейчас".

---

## 🚀 5. Примеры использования (Code Snippets)

### Создание экрана "с нуля"
```kotlin
class MyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DesignEngine.init(this)
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            backgroundColor = DesignEngine.COLOR_BACKGROUND
        }
        
        val badge = DesignEngine.createPremiumBadge(this)
        DesignEngine.setPremiumStatus(badge, true)
        
        layout.addView(badge, LayoutHelper.createLinear(24, 24))
        setContentView(layout)
    }
}
```

---

## 📡 6. Сетевой слой

### `BlagoApi`
*   `call(request: ApiRequest)` — Универсальный метод. Принимает объект запроса (тип действия + параметры) и возвращает `Map<String, Any>`.

---
*Документация v2.0. Описаны все методы и переменные, актуальные на момент последнего коммита.*
