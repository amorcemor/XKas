package com.mibi.xkas.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))

    fun formatTanggal(date: Date?): String {
        return if (date != null) sdf.format(date) else "-"
    }

    fun formatTanggal(millis: Long?): String {
        return if (millis != null) sdf.format(Date(millis)) else "-"
    }
}
