package com.mibi.xkas.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/**
 * Custom keyboard manager untuk mengelola tampilan keyboard kustom
 */
class CustomKeyboardManager {
    var isCustomKeyboardVisible by mutableStateOf(false)
        private set

    var currentFieldId by mutableStateOf<String?>(null)
        private set

    fun showCustomKeyboard(fieldId: String) {
        currentFieldId = fieldId
        isCustomKeyboardVisible = true
    }

    fun hideCustomKeyboard() {
        isCustomKeyboardVisible = false
        currentFieldId = null
    }

    fun isKeyboardVisibleFor(fieldId: String): Boolean {
        return isCustomKeyboardVisible && currentFieldId == fieldId
    }
}

/**
 * Hook untuk menggunakan custom keyboard manager
 */
@Composable
fun rememberCustomKeyboardManager(): CustomKeyboardManager {
    return remember { CustomKeyboardManager() }
}

/**
 * Efek untuk menyembunyikan system keyboard saat custom keyboard aktif
 */
@Composable
fun CustomKeyboardEffect(
    keyboardManager: CustomKeyboardManager
) {
    val systemKeyboardController = LocalSoftwareKeyboardController.current

    DisposableEffect(keyboardManager.isCustomKeyboardVisible) {
        if (keyboardManager.isCustomKeyboardVisible) {
            systemKeyboardController?.hide()
        }

        onDispose { }
    }
}

/**
 * Utility functions untuk format angka
 */
object NumberUtils {
    fun formatToRupiah(amount: String): String {
        if (amount.isBlank()) return "Rp 0"

        return try {
            val number = amount.toLongOrNull() ?: 0L
            "Rp ${number.toString().reversed().chunked(3).joinToString(".").reversed()}"
        } catch (e: Exception) {
            "Rp 0"
        }
    }

    fun formatNumber(number: String): String {
        if (number.isBlank()) return "0"

        return try {
            val num = number.toLongOrNull() ?: 0L
            num.toString().reversed().chunked(3).joinToString(".").reversed()
        } catch (e: Exception) {
            "0"
        }
    }

    fun calculateExpression(expression: String): Double? {
        return try {
            // Simple calculator logic
            // This is a basic implementation, you might want to use a proper math expression evaluator
            val cleanExpression = expression.replace("×", "*").replace("÷", "/")

            // Basic arithmetic operations
            when {
                "+" in cleanExpression -> {
                    val parts = cleanExpression.split("+")
                    if (parts.size == 2) {
                        parts[0].trim().toDouble() + parts[1].trim().toDouble()
                    } else null
                }
                "-" in cleanExpression -> {
                    val parts = cleanExpression.split("-")
                    if (parts.size == 2) {
                        parts[0].trim().toDouble() - parts[1].trim().toDouble()
                    } else null
                }
                "*" in cleanExpression -> {
                    val parts = cleanExpression.split("*")
                    if (parts.size == 2) {
                        parts[0].trim().toDouble() * parts[1].trim().toDouble()
                    } else null
                }
                "/" in cleanExpression -> {
                    val parts = cleanExpression.split("/")
                    if (parts.size == 2 && parts[1].trim().toDouble() != 0.0) {
                        parts[0].trim().toDouble() / parts[1].trim().toDouble()
                    } else null
                }
                else -> cleanExpression.toDoubleOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }
}