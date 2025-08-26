package com.mibi.xkas.ui.addedit

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Versi yang mempertahankan leading zero (000...) + format ribuan
 */
class NumberDecimalVisualTransformation : VisualTransformation {

    private val symbols = DecimalFormatSymbols(Locale("id", "ID"))
    private val numberFormatter = DecimalFormat("#,###", symbols)

    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text

        if (originalText.isEmpty()) {
            return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
        }

        val digitsOnly = originalText.filter { it.isDigit() }
        if (digitsOnly.isEmpty()) {
            return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
        }

        // Hitung leading zeros (nol di depan)
        val leadingZeros = digitsOnly.takeWhile { it == '0' }
        val numberPart = digitsOnly.dropWhile { it == '0' }

        val formatted = if (numberPart.isEmpty()) {
            // Kasus hanya nol semua, tampilkan apa adanya
            leadingZeros
        } else {
            val formattedNumber = try {
                numberFormatter.format(numberPart.toLong())
            } catch (e: NumberFormatException) {
                numberPart
            }
            // Gabungkan nol di depan + hasil format ribuan
            leadingZeros + formattedNumber
        }

        return TransformedText(
            AnnotatedString(formatted),
            SafeOffsetMapping(originalText, formatted)
        )
    }
}

/**
 * OffsetMapping aman (tidak crash)
 */
private class SafeOffsetMapping(
    private val originalText: String,
    private val transformedText: String
) : OffsetMapping {

    override fun originalToTransformed(offset: Int): Int {
        val safeOffset = offset.coerceIn(0, originalText.length)

        if (originalText.isEmpty() || transformedText.isEmpty()) return 0
        if (safeOffset == 0) return 0
        if (safeOffset >= originalText.length) return transformedText.length

        val digitsBeforeOffset = originalText.take(safeOffset).count { it.isDigit() }
        if (digitsBeforeOffset == 0) return 0

        var digitCount = 0
        for (i in transformedText.indices) {
            if (transformedText[i].isDigit()) {
                digitCount++
                if (digitCount == digitsBeforeOffset) {
                    return (i + 1).coerceAtMost(transformedText.length)
                }
            }
        }
        return transformedText.length
    }

    override fun transformedToOriginal(offset: Int): Int {
        val safeOffset = offset.coerceIn(0, transformedText.length)

        if (originalText.isEmpty() || transformedText.isEmpty()) return 0
        if (safeOffset == 0) return 0
        if (safeOffset >= transformedText.length) return originalText.length

        val digitsBeforeOffset = transformedText.take(safeOffset).count { it.isDigit() }
        if (digitsBeforeOffset == 0) return 0

        var digitCount = 0
        for (i in originalText.indices) {
            if (originalText[i].isDigit()) {
                digitCount++
                if (digitCount == digitsBeforeOffset) {
                    return (i + 1).coerceAtMost(originalText.length)
                }
            }
        }
        return originalText.length
    }
}
