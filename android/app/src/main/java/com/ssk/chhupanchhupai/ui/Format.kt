package com.ssk.chhupanchhupai.ui

import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/** 754000 -> "12:34", 3_754_000 -> "1:02:34". Negative values show as 00:00. */
fun formatClock(ms: Long): String {
    val totalSec = max(0L, (ms + 999) / 1000)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%02d:%02d", m, s)
}

fun formatDistance(metres: Number): String {
    val m = metres.toDouble()
    return if (m < 1000) "${m.roundToInt()} m" else String.format(Locale.US, "%.1f km", m / 1000)
}

fun formatSeconds(sec: Int): String = when {
    sec < 60 -> "$sec s"
    sec % 60 == 0 -> "${sec / 60} min"
    else -> "${sec / 60} min ${sec % 60} s"
}

fun formatAgo(ms: Long): String {
    val sec = max(0L, ms / 1000)
    return when {
        sec < 60 -> "${sec}s ago"
        sec < 3600 -> "${sec / 60}m ago"
        else -> "${sec / 3600}h ago"
    }
}
