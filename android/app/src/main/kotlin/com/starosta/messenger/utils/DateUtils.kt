package com.starosta.messenger.utils

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object DateUtils {

    fun formatMessageTime(dateString: String?): String {
        if (dateString == null) return ""
        return try {
            val date = parseIso8601(dateString)
            val now = Date()
            val diff = now.time - date.time
            val days = TimeUnit.MILLISECONDS.toDays(diff)

            when {
                days == 0L -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                days == 1L -> "Yesterday"
                days < 7L -> SimpleDateFormat("EEE", Locale.getDefault()).format(date)
                else -> SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(date)
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun formatLastSeen(dateString: String?): String {
        if (dateString == null) return "last seen a long time ago"
        return try {
            val date = parseIso8601(dateString)
            val now = Date()
            val diff = now.time - date.time
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)

            when {
                minutes < 1 -> "last seen just now"
                minutes < 60 -> "last seen $minutes min ago"
                hours < 24 -> "last seen ${hours}h ago"
                days == 1L -> "last seen yesterday"
                else -> "last seen ${SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)}"
            }
        } catch (e: Exception) {
            "last seen recently"
        }
    }

    fun isSameDay(date1: String?, date2: String?): Boolean {
        if (date1 == null || date2 == null) return false
        return try {
            val d1 = parseIso8601(date1)
            val d2 = parseIso8601(date2)
            val cal1 = Calendar.getInstance().apply { time = d1 }
            val cal2 = Calendar.getInstance().apply { time = d2 }
            cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
        } catch (e: Exception) {
            false
        }
    }

    fun formatDateHeader(dateString: String?): String {
        if (dateString == null) return ""
        return try {
            val date = parseIso8601(dateString)
            val now = Date()
            val diff = now.time - date.time
            val days = TimeUnit.MILLISECONDS.toDays(diff)

            when {
                days == 0L -> "Today"
                days == 1L -> "Yesterday"
                else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(date)
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun parseIso8601(dateString: String): Date {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
        )
        for (format in formats) {
            try {
                return SimpleDateFormat(format, Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(dateString)!!
            } catch (_: Exception) {}
        }
        return Date()
    }
}
