package com.kia.blagogramapp.utils

import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {

    private val input = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("Europe/Moscow")
    }

    fun formatTime(raw: String): String {
        if (raw.isBlank()) return ""
        val date = input.parse(raw) ?: return ""
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    }
    // -------------------------
    // Вспомогательная безопасная парс-функция
    // -------------------------
    private fun safeParse(raw: String): Date? {
        return try {
            inputFormat.parse(raw)
        } catch (e: Exception) {
            null
        }
    }
    // Парсер входной строки "yyyy-MM-dd HH:mm:ss"
    private val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
        // НЕ заставляем конкретную зону по умолчанию.
        // Если сервер действительно отдаёт UTC or Europe/Moscow consistently,
        // можно изменить на TimeZone.getTimeZone("UTC") или "Europe/Moscow"
        timeZone = TimeZone.getDefault()
    }
    fun headerFor(raw: String): String {
        if (raw.isBlank()) return ""
        val date = safeParse(raw) ?: return ""

        val msg = Calendar.getInstance().apply { time = date }

        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val thisYear = Calendar.getInstance()  // год сравниваем только с ним!

        // Сегодня
        if (today.get(Calendar.YEAR) == msg.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == msg.get(Calendar.DAY_OF_YEAR)
        ) {
            return "Сегодня"
        }

        // Вчера
        if (yesterday.get(Calendar.YEAR) == msg.get(Calendar.YEAR) &&
            yesterday.get(Calendar.DAY_OF_YEAR) == msg.get(Calendar.DAY_OF_YEAR)
        ) {
            return "Вчера"
        }

        // Если год совпадает → просто "27 ноября"
        return if (msg.get(Calendar.YEAR) == thisYear.get(Calendar.YEAR)) {
            SimpleDateFormat("d MMMM", Locale("ru")).format(date)
                .replaceFirstChar { it.uppercase() }
        } else {
            // Если год другой → "27 ноября 2024"
            SimpleDateFormat("d MMMM yyyy", Locale("ru")).format(date)
                .replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * Форматирует дату для PhotoViewerActivity.
     * Формат: "сегодня в 14:30", "вчера в 09:15", "8 янв. в 12:00".
     * Год добавляется, если он не совпадает с текущим.
     * @param raw Исходная строка с датой (например, "2026-01-10 11:26:34").
     * @return Отформатированная строка.
     */
    fun formatPhotoViewerDate(raw: String): String {
        if (raw.isBlank()) return ""
        val date = safeParse(raw) ?: return "" // Используем вашу утилиту safeParse

        val msgCalendar = Calendar.getInstance().apply { time = date }
        val now = Calendar.getInstance()

        // Форматы времени и даты
        val timeFormat = SimpleDateFormat("HH:mm", Locale("ru"))
        val dayMonthFormat = SimpleDateFormat("d MMM", Locale("ru"))
        val dayMonthYearFormat = SimpleDateFormat("d MMM yyyy", Locale("ru"))

        val timeString = "в ${timeFormat.format(date)}"

        // Сегодня
        if (now.get(Calendar.YEAR) == msgCalendar.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == msgCalendar.get(Calendar.DAY_OF_YEAR)) {
            return "сегодня $timeString"
        }

        // Вчера
        val yesterday = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        if (yesterday.get(Calendar.YEAR) == msgCalendar.get(Calendar.YEAR) &&
            yesterday.get(Calendar.DAY_OF_YEAR) == msgCalendar.get(Calendar.DAY_OF_YEAR)) {
            return "вчера $timeString"
        }

        // Другая дата
        return if (now.get(Calendar.YEAR) == msgCalendar.get(Calendar.YEAR)) {
            // Этот год
            "${dayMonthFormat.format(date)} $timeString"
        } else {
            // Другой год
            "${dayMonthYearFormat.format(date)} $timeString"
        }
    }

}
