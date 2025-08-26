package com.mibi.xkas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomKeyboardBottomSheet(
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var expression by remember { mutableStateOf(if (value.isBlank() || value == "0") "" else value) }

    val white = Color.White
    val opBg = Color(0xFFF5F5F5)
    val deleteBg = Color(0xFFFF5252)
    val okBg = Color(0xFF4CAF50)

    fun inputDigit(d: String) {
        if (expression.isEmpty()) expression = d
        else expression += d
    }
    fun inputOperation(op: String) {
        if (expression.isNotEmpty() && expression.last().isDigit()) {
            expression += " $op "
        }
    }
    fun clearAll() { expression = "" }
    fun backspace() {
        if (expression.isNotEmpty()) expression = expression.dropLast(1).trimEnd()
    }

    fun evaluateExpressionToDouble(expr: String): Double? {
        val tokens = expr.split(" ").filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null
        val safeTokens = tokens.map {
            when (it) {
                "x", "×" -> "*"
                ":", "÷" -> "/"
                else -> it
            }
        }
        val values = mutableListOf<Double>()
        val ops = mutableListOf<String>()
        fun applyOp(op: String, b: Double, a: Double): Double = when (op) {
            "+" -> a + b
            "-" -> a - b
            "*" -> a * b
            "/" -> if (b != 0.0) a / b else Double.NaN
            else -> b
        }
        var i = 0
        while (i < safeTokens.size) {
            val token = safeTokens[i]
            val num = token.toDoubleOrNull()
            if (num != null) {
                values.add(num)
            } else if (token in listOf("+", "-", "*", "/")) {
                while (ops.isNotEmpty() && values.size >= 2 && precedence(ops.last()) >= precedence(token)) {
                    val b = values.popLastOrNull() ?: break
                    val a = values.popLastOrNull() ?: break
                    val op = ops.removeAt(ops.size - 1)
                    values.add(applyOp(op, b, a))
                }
                ops.add(token)
            }
            i++
        }
        while (ops.isNotEmpty() && values.size >= 2) {
            val b = values.popLastOrNull() ?: break
            val a = values.popLastOrNull() ?: break
            val op = ops.removeAt(ops.size - 1)
            values.add(applyOp(op, b, a))
        }
        return values.lastOrNull()
    }

    fun calculateExpressionRaw(expr: String): String {
        val d = evaluateExpressionToDouble(expr) ?: 0.0
        if (d.isNaN() || d.isInfinite()) return "0"
        return if (d % 1.0 == 0.0) d.toLong().toString() else d.toString()
    }

    fun formatNumberForDisplay(rawNumberString: String): String {
        val number = rawNumberString.toDoubleOrNull() ?: return rawNumberString
        val symbols = DecimalFormatSymbols(Locale("in", "ID")).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        val formatter = DecimalFormat("#,###.##", symbols)
        return formatter.format(number)
    }

    fun formatExpression(expr: String): String {
        if (expr.isBlank()) return "0"
        return expr.split(" ").joinToString(" ") { token ->
            token.toDoubleOrNull()?.let { formatNumberForDisplay(token) } ?: token
        }
    }

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        containerColor = white,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.DarkGray, shape = MaterialTheme.shapes.small)
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = formatExpression(expression),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.End
                )
            }

            val gap = 4.dp
            val keySize: Dp = 54.dp

            // Row 1
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(gap)) {
                KeyBox("C", keySize, opBg) { clearAll() }
                KeyBox(":", keySize, opBg) { inputOperation(":") }
                KeyBox("x", keySize, opBg) { inputOperation("x") }
                KeyBox("Hapus", keySize, deleteBg, Color.White) { backspace() }
            }

            // Row 2
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(gap)) {
                KeyBoxCircle("1", keySize, white) { inputDigit("1") }
                KeyBoxCircle("2", keySize, white) { inputDigit("2") }
                KeyBoxCircle("3", keySize, white) { inputDigit("3") }
                KeyBox("-", keySize, opBg) { inputOperation("-") }
            }

            // Row 3
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(gap)) {
                KeyBoxCircle("4", keySize, white) { inputDigit("4") }
                KeyBoxCircle("5", keySize, white) { inputDigit("5") }
                KeyBoxCircle("6", keySize, white) { inputDigit("6") }
                KeyBox("+", keySize, opBg) { inputOperation("+") }
            }

            // Row 4
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(gap)) {
                KeyBoxCircle("7", keySize, white) { inputDigit("7") }
                KeyBoxCircle("8", keySize, white) { inputDigit("8") }
                KeyBoxCircle("9", keySize, white) { inputDigit("9") }
                KeyBox("=", keySize, opBg) {
                    expression = calculateExpressionRaw(expression)
                }
            }

            // Row 5
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(gap)) {
                KeyBoxCircle("0", keySize, white) { inputDigit("0") }
                KeyBoxCircle("00", keySize, white) { inputDigit("00") }
                KeyBoxCircle("000", keySize, white) { inputDigit("000") }
                KeyBox("OK", keySize, okBg, Color.White) {
                    val raw = calculateExpressionRaw(expression)
                    onValueChange(raw)
                    onConfirm()
                }
            }
        }
    }
}

// Kotak rounded
@Composable
private fun RowScope.KeyBox(
    label: String,
    size: Dp,
    bg: Color,
    textColor: Color = Color.Black,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .weight(1f)
            .height(size)
            .clip(shape)
            .background(if (isPressed) bg.copy(alpha = 0.78f) else bg, shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = textColor, bounded = true)
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = textColor, style = MaterialTheme.typography.titleMedium)
    }
}

// Bulat
@Composable
private fun RowScope.KeyBoxCircle(
    label: String,
    size: Dp,
    bg: Color,
    textColor: Color = Color.Black,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .weight(1f)
            .size(size)
            .clip(CircleShape)
            .background(if (isPressed) bg.copy(alpha = 0.78f) else bg, shape = CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = textColor, bounded = true)
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = textColor, style = MaterialTheme.typography.titleMedium)
    }
}

private fun precedence(op: String) = when (op) {
    "+", "-" -> 1
    "*", "/" -> 2
    else -> 0
}

private fun <T> MutableList<T>.popLastOrNull(): T? = if (isNotEmpty()) removeAt(size - 1) else null
