package com.mibi.xkas.ui.components

import android.R.attr.onClick
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mibi.xkas.data.BusinessUnitType

@Composable
fun AddBusinessUnitDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (name: String, type: BusinessUnitType, description: String?, initialBalance: Double, customTypeName: String?) -> Unit,
    isLoading: Boolean = false
) {
    var businessName by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf(BusinessUnitType.OTHER) }
    var description by rememberSaveable { mutableStateOf("") }
    var initialBalance by rememberSaveable { mutableStateOf("0") }
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    var customTypeName by rememberSaveable { mutableStateOf("") }
    var customTypeError by remember { mutableStateOf<String?>(null) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var balanceError by remember { mutableStateOf<String?>(null) }

    // Validasi input
    fun validateInputs(): Boolean {
        var isValid = true

        if (businessName.isBlank()) {
            nameError = "Nama bisnis tidak boleh kosong"
            isValid = false
        } else {
            nameError = null
        }

        val balanceValue = initialBalance.toDoubleOrNull()
        if (balanceValue == null || balanceValue < 0) {
            balanceError = "Saldo awal harus berupa angka valid (≥ 0)"
            isValid = false
        } else {
            balanceError = null
        }

        return isValid
    }

    AlertDialog(
        onDismissRequest = if (!isLoading) onDismissRequest else { {} },
        containerColor = Color.White, // Atau MaterialTheme.colorScheme.surface, atau MaterialTheme.colorScheme.surfaceBright
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Store,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Buat Bisnis Baru",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Input Nama Bisnis
                OutlinedTextField(
                    value = businessName,
                    onValueChange = {
                        businessName = it
                        nameError = null
                    },
                    label = { Text("Nama Bisnis") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences, // DITAMBAHKAN/DIUBAH
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    placeholder = { Text("Contoh: MIBI CELL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp)
                )

                // Deskripsi
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Deskripsi (Opsional)") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences, // DITAMBAHKAN/DIUBAH
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    placeholder = { Text("Deskripsi singkat tentang bisnis Anda") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp)
                )

                // Tombol untuk opsi lanjutan
                TextButton(
                    onClick = { showAdvanced = !showAdvanced },
                    enabled = !isLoading
                ) {
                    Text(if (showAdvanced) "Sembunyikan Jenis Bisnis" else "Tampilkan Jenis Bisnis")
                }

                // Opsi Lanjutan (Collapsible)
                if (showAdvanced) {
                    // Tipe Bisnis (Quick Select)
                    Text(
                        text = "Jenis Bisnis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Column(
                        modifier = Modifier.selectableGroup(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val commonTypes = listOf(
                            BusinessUnitType.RETAIL_BUSINESS to "Usaha Retail",
                            BusinessUnitType.CULUNARY_BUSINESS to "Usaha Kuliner",
                            BusinessUnitType.SERVICE_BUSINESS to "Penyedia Jasa",
                            BusinessUnitType.ONLINE_BUSINESS to "Bisnis Online",
                            BusinessUnitType.OTHER to "Lainnya"
                        )

                        commonTypes.forEach { (type, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = selectedType == type,
                                        onClick = {
                                            selectedType = type
                                            if (type != BusinessUnitType.OTHER) {
                                                customTypeName = ""
                                                customTypeError = null
                                            }
                                        },
                                        role = Role.RadioButton,
                                        enabled = !isLoading
                                    )
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedType == type,
                                    onClick = null,
                                    enabled = !isLoading
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        // Input custom type jika "Lainnya" dipilih
                        if (selectedType == BusinessUnitType.OTHER) {
                            OutlinedTextField(
                                value = customTypeName,
                                onValueChange = {
                                    customTypeName = it
                                    customTypeError = null
                                },
                                label = { Text("Jenis Bisnis Custom (Opsional)") }, // Tambah "(Opsional)"
                                placeholder = { Text("Kosongkan jika ingin tetap 'Lainnya'") }, // Update placeholder
                                singleLine = true,
                                isError = customTypeError != null,
                                supportingText = if (customTypeError != null) {
                                    { Text(customTypeError!!) }
                                } else {
                                    { Text("Opsional: Isi jika ingin menspesifikasi jenis bisnis") } // Tambah helper text
                                },
                                enabled = !isLoading,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Saldo Awal
                    /**
                     * OutlinedTextField(
                        value = initialBalance,
                        onValueChange = {
                            initialBalance = it
                            balanceError = null
                        },
                        label = { Text("Saldo Awal") },
                        placeholder = { Text("0") },
                        prefix = { Text("Rp ") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = balanceError != null,
                        supportingText = balanceError?.let { { Text(it) } },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp)
                    )*/
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validateInputs()) {
                        val finalDescription = if (description.isBlank()) null else description.trim()
                        val finalBalance = initialBalance.toDoubleOrNull() ?: 0.0
                        val finalCustomTypeName = if (selectedType == BusinessUnitType.OTHER && customTypeName.isNotBlank()) {
                            customTypeName.trim()
                        } else null
                        onConfirm(businessName.trim(), selectedType, finalDescription, finalBalance, finalCustomTypeName)
                    }
                },
                enabled = !isLoading && businessName.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Simpan")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismissRequest,
                enabled = !isLoading
            ) {
                Text("Batal")
            }
        }
    )
}