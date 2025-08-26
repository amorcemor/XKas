package com.mibi.xkas.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mibi.xkas.ui.components.CustomKeyboardBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomNumericTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp),
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true
) {
    var showKeyboard by remember { mutableStateOf(false) }
    var currentValue by remember(value) { mutableStateOf(value) }

    val interactionSource = remember { MutableInteractionSource() }
    val focusManager = LocalFocusManager.current

    // Sync dengan parent value
    LaunchedEffect(value) {
        if (currentValue != value) {
            currentValue = value
        }
    }

    // Deteksi klik pada text field
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Press) {
                focusManager.clearFocus() // supaya tidak munculkan keyboard bawaan
                showKeyboard = true
            }
        }
    }

    OutlinedTextField(
        value = if (currentValue == "0") "" else currentValue,
        onValueChange = { }, // read-only, input lewat custom keyboard
        modifier = modifier.fillMaxWidth(),
        label = label,
        placeholder = placeholder,
        readOnly = true,
        enabled = enabled,
        shape = shape,
        visualTransformation = if (currentValue.isEmpty()) {
            VisualTransformation.None
        } else {
            visualTransformation
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        interactionSource = interactionSource
    )

    // Panggil custom bottomsheet
    if (showKeyboard) {
        CustomKeyboardBottomSheet(
            value = currentValue,   // ⬅️ ganti ke 'value'
            onValueChange = { newValue -> currentValue = newValue },
            onConfirm = {
                onValueChange(currentValue)
                showKeyboard = false
            },
            onDismiss = {
                currentValue = value // reset ke asal kalau batal
                showKeyboard = false
            }
        )
    }
}
