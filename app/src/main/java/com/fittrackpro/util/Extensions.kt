package com.fittrackpro.util

import android.content.Context
import android.view.View
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// View Extensions
fun View.visible() {
    visibility = View.VISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

// Context Extensions
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

// Date Extensions
fun Date.formatToString(pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(this)
}

fun Long.toFormattedDate(pattern: String = "MMM dd, yyyy"): String {
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(Date(this))
}

// Duration Extensions
fun Long.formatDuration(): String {
    val hours = TimeUnit.MILLISECONDS.toHours(this)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this) % 60

    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%02d:%02d", minutes, seconds)
    }
}

fun Long.formatDurationLong(): String {
    val hours = TimeUnit.MILLISECONDS.toHours(this)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this) % 60

    return buildString {
        if (hours > 0) append("${hours}h ")
        if (minutes > 0) append("${minutes}m ")
        append("${seconds}s")
    }.trim()
}

// Distance Extensions
fun Double.formatDistance(isMetric: Boolean = true): String {
    return if (isMetric) {
        when {
            this >= 1000 -> String.format("%.2f km", this / 1000)
            else -> String.format("%.0f m", this)
        }
    } else {
        val miles = this * 0.000621371
        String.format("%.2f mi", miles)
    }
}

fun Double.metersToKilometers(): Double = this / 1000.0

fun Double.metersToMiles(): Double = this * 0.000621371

fun Double.kilometersToMiles(): Double = this * 0.621371

fun Double.milesToKilometers(): Double = this * 1.60934

// Speed Extensions
fun Double.formatSpeed(isMetric: Boolean = true): String {
    return if (isMetric) {
        String.format("%.1f km/h", this * 3.6) // m/s to km/h
    } else {
        String.format("%.1f mph", this * 2.23694) // m/s to mph
    }
}

fun Double.formatPace(isMetric: Boolean = true): String {
    // Speed in m/s to pace (min/km or min/mi)
    if (this <= 0) return "--:--"

    val paceInSeconds = if (isMetric) {
        1000.0 / this // seconds per km
    } else {
        1609.34 / this // seconds per mile
    }

    val minutes = (paceInSeconds / 60).toInt()
    val seconds = (paceInSeconds % 60).toInt()

    val unit = if (isMetric) "/km" else "/mi"
    return String.format("%d:%02d%s", minutes, seconds, unit)
}

// Calorie Extensions
fun Int.formatCalories(): String = "$this kcal"

// Number Extensions
fun Int.ordinal(): String {
    return when {
        this % 100 in 11..13 -> "${this}th"
        this % 10 == 1 -> "${this}st"
        this % 10 == 2 -> "${this}nd"
        this % 10 == 3 -> "${this}rd"
        else -> "${this}th"
    }
}

// String Extensions
fun String.capitalizeWords(): String {
    return split(" ").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { it.uppercase() }
    }
}

// Collection Extensions
fun <T> List<T>.safeGet(index: Int): T? {
    return if (index in indices) this[index] else null
}
