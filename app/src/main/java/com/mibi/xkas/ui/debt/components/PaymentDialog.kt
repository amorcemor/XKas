package com.mibi.xkas.ui.debt.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mibi.xkas.data.model.DebtPayment
import com.mibi.xkas.ui.addedit.NumberDecimalVisualTransformation
import com.mibi.xkas.utils.formatRupiah
import java.text.SimpleDateFormat
import java.util.*
import com.mibi.xkas.ui.components.CustomNumericTextField

enum class PaymentType {
    RECEIVE, // Terima uang dari debitur (mengurangi hutang)
    GIVE     // Berikan uang ke debitur (menambah hutang)
}

data class PaymentData(
    val amount: String = "",
    val description: String = "",
    val date: String = "",
    val paymentType: PaymentType = PaymentType.RECEIVE
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDialog(
    showDialog: Boolean,
    contactName: String,
    paymentType: PaymentType,
    currentDebtAmount: Double = 0.0,
    editingPayment: DebtPayment? = null, // TAMBAH
    onDismiss: () -> Unit,
    onSave: (PaymentData) -> Unit,
    isLoading: Boolean = false
) {
    if (!showDialog) return

    var paymentData by remember {
        mutableStateOf(
            PaymentData(
                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                paymentType = paymentType
            )
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val numberTransformation = remember { NumberDecimalVisualTransformation () }

    // Reset data when dialog opens
    LaunchedEffect(showDialog, paymentType) {
        if (showDialog) {
            paymentData = PaymentData(
                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                paymentType = paymentType
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White // Background dialog putih
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (paymentType == PaymentType.RECEIVE)
                            "Terima Pembayaran" else "Berikan Pinjaman",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = contactName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (paymentType == PaymentType.RECEIVE && currentDebtAmount > 0) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Sisa Hutang",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = formatRupiah(currentDebtAmount),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // Amount Input
                CustomNumericTextField(
                    value = paymentData.amount,
                    onValueChange = { newValue ->
                        paymentData = paymentData.copy(amount = newValue)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            if (paymentType == PaymentType.RECEIVE)
                                "Jumlah Diterima (Rp)" else "Jumlah Dipinjamkan (Rp)"
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = numberTransformation
                )

                // Quick Amount Buttons (only for receive payment)
                if (paymentType == PaymentType.RECEIVE && currentDebtAmount > 0) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Jumlah Cepat:",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Half payment
                            val halfAmount = (currentDebtAmount / 2).toLong()
                            OutlinedButton(
                                onClick = {
                                    paymentData = paymentData.copy(amount = halfAmount.toString())
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("50%", style = MaterialTheme.typography.bodySmall)
                            }

                            // Full payment
                            OutlinedButton(
                                onClick = {
                                    paymentData = paymentData.copy(amount = currentDebtAmount.toLong().toString())
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Lunas", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                // Description
                OutlinedTextField(
                    value = paymentData.description,
                    onValueChange = { paymentData = paymentData.copy(description = it) },
                    label = { Text("Catatan (Opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    maxLines = 3
                )

                // Date
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(paymentData.date) // atau format tanggal sesuai kebutuhan
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = { onSave(paymentData) },
                        enabled = paymentData.amount.isNotBlank() &&
                                paymentData.amount.toDoubleOrNull() != null &&
                                paymentData.amount.toDouble() > 0 &&
                                !isLoading,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (paymentType == PaymentType.RECEIVE)
                                Color(0xFF4CAF50) else Color(0xFFFF9800)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text(
                                if (paymentType == PaymentType.RECEIVE) "Terima" else "Berikan",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        datePickerState.selectedDateMillis?.let { millis ->
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            paymentData = paymentData.copy(date = sdf.format(Date(millis)))
                        }
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}