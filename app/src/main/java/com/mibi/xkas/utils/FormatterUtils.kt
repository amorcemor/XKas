package com.mibi.xkas.utils

import java.text.NumberFormat
import java.util.Locale


fun formatRupiah(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
    formatter.maximumFractionDigits = 0
    return formatter.format(amount)
}
