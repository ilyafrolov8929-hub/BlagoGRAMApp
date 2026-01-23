package com.kia.blagogramapp.utils

import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {

    // Формат API (серверный). Используем Московское время как эталон для приложения.
    private val apiFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("Europe/Moscow")
    }

    // Формат для отображения в чате (местное время пользователя)
    private val displayFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun normalizeRussianPhone(input: String): String? {
        var phone = input.trim()
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
            .replace("[^0-9+]", "")

        if (phone.isEmpty()) return null

        when {
            phone.startsWith("+7") || phone.startsWith("+79") -> {}
            phone.startsWith("8") && phone.length >= 11 -> phone = "+7" + phone.substring(1)
            phone.startsWith("7") && phone.length >= 10 -> phone = "+" + phone
            phone.startsWith("9") && phone.length == 10 -> phone = "+79" + phone
            else -> return null
        }
        return if (phone.matches(Regex("^\\+7\\d{10}$"))) phone else null
    }

    /**
     * Возвращает текущее время в формате МСК для временных сообщений.
     */
    fun getCurrentServerTime(): String {
        return apiFormat.format(Date())
    }

    fun formatTime(raw: String): String {
        if (raw.isBlank()) return ""
        return try {
            val date = apiFormat.parse(raw) ?: return ""
            displayFormat.format(date)
        } catch (e: Exception) {
            ""
        }
    }

    fun headerFor(raw: String): String {
        if (raw.isBlank()) return ""
        val date = try { apiFormat.parse(raw) } catch (e: Exception) { null } ?: return ""
        val msg = Calendar.getInstance().apply { time = date }
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        if (today.get(Calendar.YEAR) == msg.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == msg.get(Calendar.DAY_OF_YEAR)
        ) return "Сегодня"

        if (yesterday.get(Calendar.YEAR) == msg.get(Calendar.YEAR) &&
            yesterday.get(Calendar.DAY_OF_YEAR) == msg.get(Calendar.DAY_OF_YEAR)
        ) return "Вчера"

        return if (msg.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
            SimpleDateFormat("d MMMM", Locale("ru")).format(date)
                .replaceFirstChar { it.uppercase() }
        } else {
            SimpleDateFormat("d MMMM yyyy", Locale("ru")).format(date)
                .replaceFirstChar { it.uppercase() }
        }
    }

    fun formatPhotoViewerDate(raw: String): String {
        if (raw.isBlank()) return ""
        val date = try { apiFormat.parse(raw) } catch (e: Exception) { null } ?: return ""
        val msgCalendar = Calendar.getInstance().apply { time = date }
        val now = Calendar.getInstance()
        val timeString = "в ${displayFormat.format(date)}"

        if (now.get(Calendar.YEAR) == msgCalendar.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == msgCalendar.get(Calendar.DAY_OF_YEAR)
        ) return "сегодня $timeString"

        val yesterday = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        if (yesterday.get(Calendar.YEAR) == msgCalendar.get(Calendar.YEAR) &&
            yesterday.get(Calendar.DAY_OF_YEAR) == msgCalendar.get(Calendar.DAY_OF_YEAR)
        ) return "вчера $timeString"

        val dayMonthFormat = SimpleDateFormat("d MMM", Locale("ru"))
        val dayMonthYearFormat = SimpleDateFormat("d MMM yyyy", Locale("ru"))

        return if (now.get(Calendar.YEAR) == msgCalendar.get(Calendar.YEAR)) {
            "${dayMonthFormat.format(date)} $timeString"
        } else {
            "${dayMonthYearFormat.format(date)} $timeString"
        }
    }
}
