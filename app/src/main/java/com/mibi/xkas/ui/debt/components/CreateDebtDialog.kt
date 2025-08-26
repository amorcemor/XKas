package com.mibi.xkas.ui.debt.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mibi.xkas.data.model.FirestoreContact
import com.mibi.xkas.data.model.DebtType
import com.mibi.xkas.ui.addedit.NumberDecimalVisualTransformation
import com.mibi.xkas.ui.components.CustomNumericTextField
import com.mibi.xkas.ui.components.EnhancedContactPickerDialog
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState


/**
 * Data yang dikirim ke ViewModel saat hutang baru dibuat
 */
data class CreateDebtData(
    val contact: FirestoreContact? = null,
    val contactId: String = "",
    val amount: Double = 0.0,
    val description: String? = "",
    val dueDate: Date? = null,
    val debtType: DebtType = DebtType.GIVE_MONEY
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDebtDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onSave: (CreateDebtData) -> Unit,
    isLoading: Boolean = false,
    selectedContact: FirestoreContact? = null,
    onSelectContact: () -> Unit = {},
    initialDebtType: DebtType = DebtType.GIVE_MONEY
) {
    if (!showDialog) return

    var debtData by remember {
        mutableStateOf(
            CreateDebtData(
                dueDate = Date(),
                debtType = initialDebtType
            )
        )
    }

    // sinkronisasi state dengan selectedContact
    LaunchedEffect(selectedContact) {
        if (selectedContact != null) {
            debtData = debtData.copy(
                contact = selectedContact,
                contactId = selectedContact.contactId
            )
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val numberTransformation = remember { NumberDecimalVisualTransformation() }

    // Compose DatePicker
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = debtData.dueDate?.time ?: System.currentTimeMillis()
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        debtData = debtData.copy(dueDate = Date(millis))
                    }
                    showDatePicker = false
                }) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val dateLabel = debtData.dueDate?.let { sdf.format(it) } ?: "Pilih tanggal"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White // Background dialog putih
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Catat Hutang",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )

                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                if (debtData.debtType == DebtType.GIVE_MONEY) "BERIKAN" else "TERIMA",
                                color = Color.White
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (debtData.debtType == DebtType.GIVE_MONEY)
                                MaterialTheme.colorScheme.error
                            else
                                Color(0xFF2E7D32)
                        )
                    )
                }

                // Jenis transaksi
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        onClick = { debtData = debtData.copy(debtType = DebtType.GIVE_MONEY) },
                        label = { Text("Berikan Uang") },
                        selected = debtData.debtType == DebtType.GIVE_MONEY,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.error
                        )
                    )
                    FilterChip(
                        onClick = { debtData = debtData.copy(debtType = DebtType.RECEIVE_MONEY) },
                        label = { Text("Terima Uang") },
                        selected = debtData.debtType == DebtType.RECEIVE_MONEY,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF2E7D32).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFF2E7D32)
                        )
                    )
                }

                // Pilih kontak
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (debtData.contact != null)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectContact() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ContactPhone, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = debtData.contact?.name ?: "Pilih Kontak",
                                fontWeight = FontWeight.SemiBold
                            )
                            if (!debtData.contact?.phoneNumber.isNullOrBlank()) {
                                Text(
                                    text = debtData.contact?.phoneNumber ?: "",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Nominal
                CustomNumericTextField(
                    value = debtData.amount.takeIf { it > 0 }?.toString() ?: "",
                    onValueChange = { input ->
                        debtData = debtData.copy(amount = input.toDoubleOrNull() ?: 0.0)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Jumlah (Rp)") },
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = numberTransformation
                )

                // Deskripsi
                OutlinedTextField(
                    value = debtData.description ?: "",
                    onValueChange = { debtData = debtData.copy(description = it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Deskripsi (opsional)") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = false
                )

                // Due date
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(dateLabel)
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Button(
                        onClick = { onSave(debtData) },
                        enabled = !isLoading && debtData.contact != null && debtData.amount > 0
                    ) {
                        Text(if (isLoading) "Menyimpan..." else "Simpan")
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true, name = "Dialog - Berikan Uang")
@Composable
fun PreviewCreateDebtDialogGive() {
    MaterialTheme {
        CreateDebtDialog(
            showDialog = true,
            onDismiss = {},
            onSave = {},
            selectedContact = FirestoreContact(
                contactId = "1",
                name = "Budi Santoso",
                phoneNumber = "08123456789"
            ),
            initialDebtType = DebtType.GIVE_MONEY
        )
    }
}

@Preview(showBackground = true, name = "Dialog - Terima Uang")
@Composable
fun PreviewCreateDebtDialogReceive() {
    MaterialTheme {
        CreateDebtDialog(
            showDialog = true,
            onDismiss = {},
            onSave = {},
            selectedContact = FirestoreContact(
                contactId = "2",
                name = "Siti Aminah",
                phoneNumber = "08987654321"
            ),
            initialDebtType = DebtType.RECEIVE_MONEY
        )
    }
}

@Preview(showBackground = true, name = "Dialog - Tanpa Kontak")
@Composable
fun PreviewCreateDebtDialogEmpty() {
    MaterialTheme {
        CreateDebtDialog(
            showDialog = true,
            onDismiss = {},
            onSave = {},
            selectedContact = null,
            initialDebtType = DebtType.GIVE_MONEY
        )
    }
}