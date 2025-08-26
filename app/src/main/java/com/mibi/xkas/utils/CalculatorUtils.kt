package com.mibi.xkas.utils

object CalculatorUtils {

    /**
     * Melakukan operasi matematika sederhana
     */
    fun calculate(leftOperand: String, operator: String, rightOperand: String): String {
        val left = leftOperand.toDoubleOrNull() ?: 0.0
        val right = rightOperand.toDoubleOrNull() ?: 0.0

        val result = when (operator) {
            "+" -> left + right
            "-" -> left - right
            "×" -> left * right
            "÷" -> if (right != 0.0) left / right else left
            else -> right
        }

        // Kembalikan sebagai Long jika hasilnya adalah bilangan bulat
        return if (result == result.toLong().toDouble()) {
            result.toLong().toString()
        } else {
            result.toString()
        }
    }

    /**
     * Validasi input digit agar tidak melebihi batas maksimum
     */
    fun validateDigitInput(currentValue: String, newDigit: String, maxLength: Int = 12): String {
        val combined = currentValue + newDigit
        val digitsOnly = combined.filter { it.isDigit() }

        return if (digitsOnly.length <= maxLength) {
            digitsOnly
        } else {
            currentValue
        }
    }

    /**
     * Membersihkan nilai dan mereset ke "0"
     */
    fun clearValue(): String = "0"

    /**
     * Menghapus digit terakhir
     */
    fun backspace(currentValue: String): String {
        return if (currentValue.length > 1) {
            currentValue.dropLast(1)
        } else {
            "0"
        }
    }

    /**
     * Menambahkan beberapa nol sekaligus (00, 000)
     */
    fun addZeros(currentValue: String, zerosCount: Int, maxLength: Int = 12): String {
        val zeros = "0".repeat(zerosCount)
        return validateDigitInput(currentValue, zeros, maxLength)
    }
}