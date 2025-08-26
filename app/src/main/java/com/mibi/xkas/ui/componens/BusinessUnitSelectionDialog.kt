package com.mibi.xkas.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mibi.xkas.data.BusinessUnit
import com.mibi.xkas.data.BusinessUnitType
import com.mibi.xkas.ui.home.HomeViewModel
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BusinessUnitSelectionDialog(
    businessUnitUiState: HomeViewModel.BusinessUnitUiState,
    selectedBusinessUnit: BusinessUnit?,
    onBusinessUnitSelected: (BusinessUnit) -> Unit,
    onDismissRequest: () -> Unit,
    onAddBusinessUnitClicked: () -> Unit, // Akan dipanggil untuk navigasi ke full screen
    onDeleteBusinessUnitConfirmed: (String) -> Unit,
    onUpdateBusinessUnitConfirmed: (businessUnitId: String, newName: String, newDescription: String?, newType: BusinessUnitType, customTypeName: String?) -> Unit,
    onCreateBusinessUnit: (name: String, type: BusinessUnitType, description: String?, initialBalance: Double, customTypeName: String?) -> Unit,
    isCreatingBusinessUnit: Boolean = false // State loading untuk create
) {
    // State untuk melacak item mana yang sedang dalam mode "long press"
    var longPressedItemId by remember { mutableStateOf<String?>(null) }
    // State untuk dialog konfirmasi hapus
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var businessUnitToDelete by remember { mutableStateOf<BusinessUnit?>(null) }
    // State untuk Dialog Edit Nama
    var showEditNameDialog by remember { mutableStateOf(false) }
    var businessUnitToEdit by remember { mutableStateOf<BusinessUnit?>(null) }
    // State untuk AddBusinessUnitDialog baru
    var showAddBusinessUnitDialog by remember { mutableStateOf(false) }

    // Saat dialog ditutup, reset state long press
    LaunchedEffect(Unit) {
        snapshotFlow { onDismissRequest }
            .collect {
                longPressedItemId = null
            }
    }

    Dialog(onDismissRequest = {
        if (longPressedItemId != null) {
            longPressedItemId = null
        } else if (showDeleteConfirmDialog) {
            showDeleteConfirmDialog = false
            businessUnitToDelete = null
        } else if (showEditNameDialog) {
            showEditNameDialog = false
            businessUnitToEdit = null
        } else if (showAddBusinessUnitDialog) {
            showAddBusinessUnitDialog = false
        } else {
            onDismissRequest()
        }
    }) {
        Card(
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
                Text(
                    text = "Pilih Bisnis",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                Text(
                    text = "Silahkan pilih untuk mengelola bisnis Anda",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                )

                when (businessUnitUiState) {
                    is HomeViewModel.BusinessUnitUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is HomeViewModel.BusinessUnitUiState.Success -> {
                        if (businessUnitUiState.businessUnit.isEmpty()) {
                            EmptyStateBuDialogContent(
                                onAddBusinessUnitClicked = { showAddBusinessUnitDialog = true }
                            )
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
                                        isLongPressed = bu.businessUnitId == longPressedItemId,
                                        onClicked = {
                                            if (longPressedItemId == bu.businessUnitId) {
                                                longPressedItemId = null
                                            } else if (longPressedItemId != null && longPressedItemId != bu.businessUnitId) {
                                                longPressedItemId = null
                                            } else {
                                                onBusinessUnitSelected(bu)
                                            }
                                        },
                                        onLongClicked = {
                                            longPressedItemId = if (longPressedItemId == bu.businessUnitId) {
                                                null
                                            } else {
                                                bu.businessUnitId
                                            }
                                        },
                                        onDeleteIconClicked = {
                                            businessUnitToDelete = bu
                                            showDeleteConfirmDialog = true
                                        },
                                        onEditIconClicked = {
                                            businessUnitToEdit = bu
                                            showEditNameDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                    is HomeViewModel.BusinessUnitUiState.Empty -> {
                        EmptyStateBuDialogContent(
                            onAddBusinessUnitClicked = { showAddBusinessUnitDialog = true }
                        )
                    }
                    is HomeViewModel.BusinessUnitUiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Gagal memuat bisnis",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                if (businessUnitUiState.message != null) {
                                    Text(
                                        text = businessUnitUiState.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 20.dp))

                // Tombol untuk menambah bisnis
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tombol cepat dengan dialog
                    OutlinedButton(
                        onClick = { showAddBusinessUnitDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        enabled = !isCreatingBusinessUnit,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                            containerColor = Color.Transparent
                        ),
                        // Perbaikan di sini: Buat BorderStroke baru
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) // Menggunakan warna primary dengan alpha
                    ) {
                        if (isCreatingBusinessUnit) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary // Sesuaikan warna indikator
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Tambah Bisnis",
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                                tint = MaterialTheme.colorScheme.primary // Sesuaikan warna ikon
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        }
                        Text("Tambah Bisnis", color = MaterialTheme.colorScheme.primary) // Sesuaikan warna teks
                    }
                }
            }
        }
    }

    // Dialog untuk menambah bisnis baru
    if (showAddBusinessUnitDialog) {
        AddBusinessUnitDialog(
            onDismissRequest = { showAddBusinessUnitDialog = false },
            onConfirm = { name, type, description, initialBalance, customTypeName ->
                onCreateBusinessUnit(name, type, description, initialBalance, customTypeName)
                showAddBusinessUnitDialog = false
            },
            isLoading = isCreatingBusinessUnit
        )
    }

    // Dialog Konfirmasi Hapus (sama seperti sebelumnya)
    if (showDeleteConfirmDialog && businessUnitToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                businessUnitToDelete = null
            },
            title = { Text("Konfirmasi Hapus") },
            text = {
                Text("Apakah Anda yakin ingin menghapus bisnis \"${businessUnitToDelete!!.name}\"? Tindakan ini tidak dapat diurungkan dan akan menghapus semua data terkait.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteBusinessUnitConfirmed(businessUnitToDelete!!.businessUnitId)
                        showDeleteConfirmDialog = false
                        longPressedItemId = null
                        businessUnitToDelete = null
                        if (selectedBusinessUnit?.businessUnitId == businessUnitToDelete?.businessUnitId) {
                            // Handle di ViewModel
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showDeleteConfirmDialog = false
                    businessUnitToDelete = null
                }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog untuk Edit Nama Bisnis (sama seperti sebelumnya)
    if (showEditNameDialog && businessUnitToEdit != null) {
        EditBusinessUnitDialog(
            businessUnit = businessUnitToEdit!!,
            onDismiss = {
                showEditNameDialog = false
                businessUnitToEdit = null
            },
            onConfirm = { newName, newDescription, newType, customTypeName ->
                if (businessUnitToEdit != null && newName.isNotBlank()) {
                    onUpdateBusinessUnitConfirmed(businessUnitToEdit!!.businessUnitId, newName, newDescription, newType, customTypeName)
                }
                showEditNameDialog = false
                longPressedItemId = null
                businessUnitToEdit = null
            }
        )
    }
}

// Komponen lainnya tetap sama...
@Composable
private fun BusinessUnitDialogItem(
    businessUnit: BusinessUnit,
    isSelected: Boolean,
    isLongPressed: Boolean,
    onClicked: () -> Unit,
    onLongClicked: () -> Unit,
    onDeleteIconClicked: () -> Unit,
    onEditIconClicked: () -> Unit
) {
    val itemShape = RoundedCornerShape(8.dp)
    val backgroundColor: Color
    val contentColor: Color
    val borderColor: Color

    if (isSelected && !isLongPressed) {
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
            .pointerInput(businessUnit.businessUnitId) {
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

            AnimatedVisibility(
                visible = isLongPressed,
                enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 2 }),
                exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it / 2 })
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditIconClicked) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit Nama Bisnis"
                        )
                    }
                    IconButton(onClick = onDeleteIconClicked) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Hapus Bisnis",
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
                Row {
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
private fun EditBusinessUnitDialog(
    businessUnit: BusinessUnit,
    onDismiss: () -> Unit,
    onConfirm: (newName: String, newDescription: String?, newType: BusinessUnitType, customTypeName: String?) -> Unit
) {
    var newName by rememberSaveable(businessUnit.name) { mutableStateOf(businessUnit.name) }
    var newDescription by rememberSaveable(businessUnit.description) { mutableStateOf(businessUnit.description ?: "") }
    var newType by rememberSaveable(businessUnit.type) { mutableStateOf(businessUnit.type) }
    var customTypeName by rememberSaveable(businessUnit.customTypeName) { mutableStateOf(businessUnit.customTypeName ?: "") }
    var showAdvanced by rememberSaveable { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var customTypeError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Bisnis") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Edit informasi bisnis \"${businessUnit.name}\":",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Input Nama Bisnis
                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        newName = it
                        nameError = null
                    },
                    label = { Text("Nama Bisnis") },
                    singleLine = true,
                    isError = nameError != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (nameError != null) {
                    Text(
                        text = nameError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Deskripsi
                OutlinedTextField(
                    value = newDescription,
                    onValueChange = { newDescription = it },
                    label = { Text("Deskripsi (Opsional)") },
                    placeholder = { Text("Deskripsi singkat tentang bisnis Anda") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )

                // Tombol untuk opsi lanjutan
                TextButton(
                    onClick = { showAdvanced = !showAdvanced }
                ) {
                    Text(if (showAdvanced) "Sembunyikan Jenis Bisnis" else "Tampilkan Jenis Bisnis")
                }

                // Opsi Lanjutan (Collapsible)
                if (showAdvanced) {
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
                                        selected = newType == type,
                                        onClick = {
                                            newType = type
                                            if (type != BusinessUnitType.OTHER) {
                                                customTypeName = ""
                                                customTypeError = null
                                            }
                                        },
                                        role = Role.RadioButton
                                    )
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = newType == type,
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    // Input custom type jika "Lainnya" dipilih
                    if (newType == BusinessUnitType.OTHER) {
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
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        newName.isBlank() -> {
                            nameError = "Nama tidak boleh kosong"
                        }
//                        newType == BusinessUnitType.OTHER && customTypeName.isBlank() -> {
//                            customTypeError = "Jenis bisnis custom tidak boleh kosong"
//                        }
                        newName == businessUnit.name &&
                                newDescription.ifBlank { null } == businessUnit.description &&
                                newType == businessUnit.type &&
                                customTypeName.ifBlank { null } == businessUnit.customTypeName -> {
                            nameError = "Tidak ada perubahan yang dilakukan"
                        }
                        else -> {
                            val finalDescription = if (newDescription.isBlank()) null else newDescription.trim()
                            val finalCustomTypeName = if (newType == BusinessUnitType.OTHER && customTypeName.isNotBlank()) {
                                customTypeName.trim()
                            } else null
                            onConfirm(newName.trim(), finalDescription, newType, finalCustomTypeName)
                        }
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
    // Gunakan Card untuk membungkus konten agar memiliki latar belakang dan bentuk
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp), // Sudut yang membulat
        colors = CardDefaults.cardColors(
            // Warna latar yang sedikit berbeda dari surface utama
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Beri padding di dalam Card, bukan di luar
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Anda belum memiliki Bisnis.",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                "Silakan tambahkan bisnis pertama Anda untuk memulai.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}