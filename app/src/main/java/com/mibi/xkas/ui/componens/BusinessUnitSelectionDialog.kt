package com.mibi.xkas.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi // Untuk combinedClickable atau pointerInput
import androidx.compose.foundation.border
// import androidx.compose.foundation.clickable // Akan diganti dengan pointerInput atau combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete // Ikon Hapus
import androidx.compose.material.icons.filled.Edit // Nanti untuk Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput // Untuk detectTapGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mibi.xkas.data.BusinessUnit
import com.mibi.xkas.ui.home.HomeViewModel
import androidx.compose.material3.OutlinedTextField // Untuk input nama baru
import androidx.compose.runtime.saveable.rememberSaveable // Untuk menyimpan state textfield

@OptIn(ExperimentalFoundationApi::class) // Jika menggunakan combinedClickable
@Composable
fun BusinessUnitSelectionDialog(
    businessUnitUiState: HomeViewModel.BusinessUnitUiState,
    selectedBusinessUnit: BusinessUnit?,
    onBusinessUnitSelected: (BusinessUnit) -> Unit,
    onDismissRequest: () -> Unit,
    onAddBusinessUnitClicked: () -> Unit,
    onDeleteBusinessUnitConfirmed: (String) -> Unit,
    onUpdateBusinessUnitNameConfirmed: (businessUnitId: String, newName: String) -> Unit
) {
    // State untuk melacak item mana yang sedang dalam mode "long press"
    var longPressedItemId by remember { mutableStateOf<String?>(null) }
    // State untuk dialog konfirmasi hapus
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var businessUnitToDelete by remember { mutableStateOf<BusinessUnit?>(null) }
    // State untuk Dialog Edit Nama
    var showEditNameDialog by remember { mutableStateOf(false) }
    var businessUnitToEdit by remember { mutableStateOf<BusinessUnit?>(null) }



    // Saat dialog ditutup, reset state long press
    LaunchedEffect(Unit) {
        snapshotFlow { onDismissRequest }
            .collect {
                longPressedItemId = null
            }
    }


    Dialog(onDismissRequest = {
        if (longPressedItemId != null) {
            longPressedItemId = null // Jika ada item long press, tap di luar akan menutup mode long press dulu
        } else if (showDeleteConfirmDialog) {
            showDeleteConfirmDialog = false // Jika dialog konfirmasi terbuka, tap di luar akan menutupnya
            businessUnitToDelete = null
        } else if (showEditNameDialog) { // Tambahkan ini
            showEditNameDialog = false
            businessUnitToEdit = null
        } else {
            onDismissRequest()
        }
    }) {
        Card(
            // ... (Card seperti sebelumnya) ...
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text( /* ... Judul ... */
                    text = "Pilih Unit Bisnis",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                Text( /* ... Deskripsi ... */
                    text = "Silakan pilih untuk mengelola bisnis Anda",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                )

                when (businessUnitUiState) {
                    is HomeViewModel.BusinessUnitUiState.Loading -> { /* ... Loading ... */ }
                    is HomeViewModel.BusinessUnitUiState.Success -> {
                        if (businessUnitUiState.businessUnit.isEmpty()) {
                            EmptyStateBuDialogContent(onAddBusinessUnitClicked)
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(businessUnitUiState.businessUnit, key = { it.businessUnitId }) { bu ->
                                    BusinessUnitDialogItem(
                                        businessUnit = bu,
                                        isSelected = bu.businessUnitId == selectedBusinessUnit?.businessUnitId,
                                        isLongPressed = bu.businessUnitId == longPressedItemId, // Kirim status long press
                                        onClicked = {
                                            if (longPressedItemId == bu.businessUnitId) {
                                                // Jika item ini sedang long pressed, tap biasa akan menonaktifkan mode long press
                                                longPressedItemId = null
                                            } else if (longPressedItemId != null && longPressedItemId != bu.businessUnitId) {
                                                // Jika item lain sedang long pressed, tap item ini akan menonaktifkan mode long press item lain
                                                longPressedItemId = null
                                                // Dan kemudian pilih item ini (jika itu yang diinginkan)
                                                // atau biarkan tap berikutnya yang memilih
                                            }
                                            else {
                                                onBusinessUnitSelected(bu)
                                            }
                                        },
                                        onLongClicked = {
                                            longPressedItemId = if (longPressedItemId == bu.businessUnitId) {
                                                null // Toggle off jika sudah long pressed
                                            } else {
                                                bu.businessUnitId // Set item ini sebagai long pressed
                                            }
                                        },
                                        onDeleteIconClicked = {
                                            businessUnitToDelete = bu
                                            showDeleteConfirmDialog = true
                                        },
                                        // Tambahkan callback untuk ikon edit
                                        onEditIconClicked = {
                                            businessUnitToEdit = bu
                                            showEditNameDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                    is HomeViewModel.BusinessUnitUiState.Empty -> { /* ... Empty ... */ }
                    is HomeViewModel.BusinessUnitUiState.Error -> { /* ... Error ... */ }
                }

                Divider(modifier = Modifier.padding(vertical = 20.dp))
                Button( /* ... Tombol Tambah ... */
                    onClick = onAddBusinessUnitClicked,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Tambah Unit Bisnis", modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Tambah Unit Bisnis Baru")
                }
            }
        }
    }

    // Dialog Konfirmasi Hapus
    if (showDeleteConfirmDialog && businessUnitToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                businessUnitToDelete = null
            },
            title = { Text("Konfirmasi Hapus") },
            text = { Text("Apakah Anda yakin ingin menghapus unit bisnis \"${businessUnitToDelete!!.name}\"? Tindakan ini tidak dapat diurungkan dan akan menghapus semua data terkait.") }, // Tambahkan peringatan data terkait
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteBusinessUnitConfirmed(businessUnitToDelete!!.businessUnitId)
                        showDeleteConfirmDialog = false
                        longPressedItemId = null // Reset long press setelah hapus
                        businessUnitToDelete = null
                        // Pertimbangkan apa yang terjadi jika BU yang dihapus adalah selectedBusinessUnit
                        if (selectedBusinessUnit?.businessUnitId == businessUnitToDelete?.businessUnitId) {
                            // Mungkin panggil onBusinessUnitSelected(null) atau pilih BU lain
                            // Untuk sekarang, kita serahkan ke ViewModel untuk handle ini setelah onDeleteBusinessUnitConfirmed
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { // Gunakan OutlinedButton untuk "Batal"
                    showDeleteConfirmDialog = false
                    businessUnitToDelete = null
                }) {
                    Text("Batal")
                }
            }
        )
    }
    // Dialog untuk Edit Nama Unit Bisnis
    if (showEditNameDialog && businessUnitToEdit != null) {
        EditBusinessUnitNameDialog(
            businessUnit = businessUnitToEdit!!, // Pasti non-null karena kondisi if
            onDismiss = {
                showEditNameDialog = false
                businessUnitToEdit = null // Reset state
            },
            onConfirm = { newName ->
                if (businessUnitToEdit != null && newName.isNotBlank()) {
                    onUpdateBusinessUnitNameConfirmed(businessUnitToEdit!!.businessUnitId, newName)
                }
                showEditNameDialog = false
                longPressedItemId = null // Reset long press setelah edit
                businessUnitToEdit = null // Reset state
            }
        )
    }
}


@Composable
private fun BusinessUnitDialogItem(
    businessUnit: BusinessUnit,
    isSelected: Boolean,
    isLongPressed: Boolean, // Status apakah item ini sedang di-long press
    onClicked: () -> Unit,
    onLongClicked: () -> Unit,
    onDeleteIconClicked: () -> Unit,
    onEditIconClicked: () -> Unit
) {
    val itemShape = RoundedCornerShape(8.dp)
    val backgroundColor: Color
    val contentColor: Color
    val borderColor: Color

    if (isSelected && !isLongPressed) { // Hanya styling terpilih jika tidak sedang long press mode untuk item ini
        backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        contentColor = MaterialTheme.colorScheme.onSurface
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    } else {
        backgroundColor = Color.Transparent
        contentColor = MaterialTheme.colorScheme.onSurface
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = borderColor, shape = itemShape)
            .clip(itemShape)
            // Menggunakan pointerInput untuk detectTapGestures
            .pointerInput(businessUnit.businessUnitId) { // Key unik untuk recomposition
                detectTapGestures(
                    onTap = { onClicked() },
                    onLongPress = { onLongClicked() }
                )
            },
        shape = itemShape,
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = businessUnit.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isSelected && !isLongPressed) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = contentColor
                )
                if (!businessUnit.description.isNullOrBlank()) {
                    Text(
                        text = businessUnit.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }
            }

            // Wadah untuk ikon aksi (hapus, nanti edit)
            // Muncul dengan animasi jika isLongPressed
            AnimatedVisibility(
                visible = isLongPressed,
                enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 2 }),
                exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it / 2 })
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) { // Pastikan ikon sejajar
                    // Tombol Edit
                    IconButton(onClick = onEditIconClicked) { // Panggil callback edit
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit Nama Unit Bisnis"
                            // tint bisa default atau MaterialTheme.colorScheme.primary
                        )
                    }
                    // Spacer(modifier = Modifier.width(4.dp)) // Jarak antar ikon jika perlu

                    // Tombol Hapus
                    IconButton(onClick = onDeleteIconClicked) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Hapus Unit Bisnis",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isSelected && !isLongPressed,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row { // Dibutuhkan Row agar Spacer juga dianimasikan bersama Icon
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Terpilih",
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EditBusinessUnitNameDialog(
    businessUnit: BusinessUnit,
    onDismiss: () -> Unit,
    onConfirm: (newName: String) -> Unit
) {
    var newName by rememberSaveable(businessUnit.name) { mutableStateOf(businessUnit.name) } // Simpan state nama, inisialisasi dengan nama lama
    var nameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Nama Unit Bisnis") },
        text = {
            Column {
                Text("Masukkan nama baru untuk \"${businessUnit.name}\":", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        newName = it
                        nameError = null // Hapus error saat pengguna mulai mengetik
                    },
                    label = { Text("Nama Unit Bisnis") },
                    singleLine = true,
                    isError = nameError != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (nameError != null) {
                    Text(
                        text = nameError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newName.isBlank()) {
                        nameError = "Nama tidak boleh kosong"
                    } else if (newName == businessUnit.name) {
                        nameError = "Nama baru tidak boleh sama dengan nama lama"
                    }
                    else {
                        onConfirm(newName)
                    }
                }
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun EmptyStateBuDialogContent(onAddBusinessUnitClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp), // Beri padding yang cukup
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp) // Jarak antar teks
    ) {
        Text(
            "Anda belum memiliki Bisnis.",
            style = MaterialTheme.typography.titleMedium, // Sedikit lebih besar untuk pesan utama
            textAlign = TextAlign.Center
        )
        Text(
            "Silakan tambahkan bisnis pertama Anda untuk memulai.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        // Tombol di Empty State bisa dipertimbangkan, tapi karena sudah ada tombol utama di bawah,
        // mungkin tidak perlu untuk menghindari duplikasi. Jika ingin, bisa diaktifkan:
        // Spacer(modifier = Modifier.height(16.dp))
        // OutlinedButton(onClick = onAddBusinessUnitClicked) { // Atau Button
        //     Text("Tambah Unit Bisnis")
        // }
    }
}

