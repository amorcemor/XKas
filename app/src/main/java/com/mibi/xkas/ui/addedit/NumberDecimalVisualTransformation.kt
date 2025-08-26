package com.mibi.xkas.ui.addedit

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class NumberDecimalVisualTransformation : VisualTransformation {

    private val symbols = DecimalFormat().decimalFormatSymbols // Dapatkan simbol desimal default
    private val decimalSeparator = symbols.decimalSeparator // Biasanya ',' atau '.' tergantung locale
    private val groupingSeparator = symbols.groupingSeparator // Biasanya '.' atau ','

    // Buat formatter untuk Indonesia (titik sebagai pemisah ribuan)
    // Jika Anda ingin lebih dinamis berdasarkan Locale sistem, Anda bisa menggunakan Locale.getDefault()
    // tapi untuk konsistensi "titik" sebagai pemisah ribuan, Locale("id", "ID") lebih eksplisit.
    private val numberFormatter = DecimalFormat("#,###", DecimalFormatSymbols(Locale("id", "ID")))

    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        // Hapus semua karakter non-digit untuk mendapatkan nilai angka murni
        val numberString = originalText.filter { it.isDigit() }
        if (numberString.isEmpty()) {
            return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
        }

        val formattedText = try {
            val number = numberString.toLong() // Konversi ke Long agar bisa diformat
            numberFormatter.format(number)
        } catch (e: NumberFormatException) {
            // Jika terjadi error (misalnya string terlalu panjang untuk Long),
            // kembalikan teks asli yang hanya berisi digit
            numberString
        }

        return TransformedText(
            AnnotatedString(formattedText),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    // Logika ini bisa lebih kompleks jika Anda perlu menangani posisi kursor secara presisi
                    // Untuk saat ini, kita coba estimasi sederhana
                    val transformedLength = formattedText.length
                    val originalLength = originalText.length
                    if (originalLength == 0) return 0
                    // Perkiraan kasar, mungkin perlu disesuaikan jika ada masalah kursor
                    val scale = transformedLength.toDouble() / originalLength.toDouble()
                    return (offset * scale).toInt().coerceIn(0, transformedLength)
                }

                override fun transformedToOriginal(offset: Int): Int {
                    // Logika ini juga bisa lebih kompleks
                    val originalLength = originalText.length
                    val transformedLength = formattedText.length
                    if (transformedLength == 0) return 0
                    // Perkiraan kasar, mungkin perlu disesuaikan
                    val scale = originalLength.toDouble() / transformedLength.toDouble()
                    return (offset * scale).toInt().coerceIn(0, originalLength)

                }
            }
        )
    }
}