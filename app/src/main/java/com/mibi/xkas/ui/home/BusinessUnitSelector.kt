package com.mibi.xkas.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mibi.xkas.data.BusinessUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessUnitSelector(
    businessUnitUiState: HomeViewModel.BusinessUnitUiState,
    selectedBusinessUnit: BusinessUnit?,
    onBusinessUnitSelected: (BusinessUnit) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        when (businessUnitUiState) {
            is HomeViewModel.BusinessUnitUiState.Loading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Memuat BU...")
                }
            }

            is HomeViewModel.BusinessUnitUiState.Success -> {
                val businessUnit = businessUnitUiState.businessUnit
                if (businessUnit.isEmpty()) {
                    Text("Tidak ada Unit Bisnis")
                    // Pertimbangkan menambahkan Button untuk membuat BU baru di sini juga,
                    // atau navigasi ke layar pembuatan BU
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expanded && enabled,
                        onExpandedChange = { if (enabled) expanded = !expanded },
                        modifier = Modifier.width(IntrinsicSize.Min)
                    ) {
                        TextField(
                            value = selectedBusinessUnit?.name ?: "Pilih Unit Bisnis",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            singleLine = true,
                            colors = ExposedDropdownMenuDefaults.textFieldColors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                errorContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            enabled = enabled
                        )

                        ExposedDropdownMenu(
                            expanded = expanded && enabled,
                            onDismissRequest = { expanded = false }
                        ) {
                            businessUnit.forEach { bu -> // Ini akan menggunakan forEach standar Kotlin
                                DropdownMenuItem(
                                    text = { Text(bu.name) },
                                    onClick = {
                                        onBusinessUnitSelected(bu)
                                        expanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Tambah Unit Bisnis Baru...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    // TODO: Navigasi ke layar tambah Business Unit
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            is HomeViewModel.BusinessUnitUiState.Error -> {
                Text(
                    "Gagal memuat BU: ${businessUnitUiState.message ?: "Kesalahan tidak diketahui"}", // Menggunakan elvis operator
                    color = MaterialTheme.colorScheme.error
                )
            }

            is HomeViewModel.BusinessUnitUiState.Empty -> {
                Text("Tidak ada Unit Bisnis terdaftar.")
                Button(onClick = { /* TODO: Navigasi ke layar tambah BU */ }) {
                    Text("Tambah Unit Bisnis")
                }
            }
        }
    }
}
