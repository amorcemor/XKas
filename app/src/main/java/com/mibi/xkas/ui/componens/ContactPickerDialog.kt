package com.mibi.xkas.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.ImportContacts
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonAddAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mibi.xkas.data.model.FirestoreContact
import com.mibi.xkas.data.model.ContactSource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit

/**
 * ✅ ENHANCED ContactPickerDialog dengan real data integration
 * Compatible dengan ContactDebtViewModel dan FirestoreContactRepository
 * Added Edit Contact functionality
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedContactPickerDialog(
    showDialog: Boolean = true,
    contacts: List<FirestoreContact>,
    searchQuery: String = "",
    isLoading: Boolean = false,
    isCreatingContact: Boolean = false,
    isImportingContacts: Boolean = false,
    onContactSelected: (FirestoreContact) -> Unit,
    onDismiss: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCreateNewContact: (String, String) -> Unit,
    onImportDeviceContacts: () -> Unit,
    onDeleteContact: (FirestoreContact) -> Unit = {},
    onEditContact: (FirestoreContact) -> Unit = {}, // ✅ NEW: Edit contact callback
    // ✅ NEW: Dialog mode untuk distinguish antara picker vs management
    dialogMode: ContactDialogMode = ContactDialogMode.PICKER
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    // ✅ FILTERING LOGIC
    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) {
            contacts
        } else {
            contacts.filter { contact ->
                contact.name.contains(searchQuery, ignoreCase = true) ||
                        contact.phoneNumber.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    if (!showDialog) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White // Background dialog putih
            )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ✅ Header - Dynamic based on dialog mode
                Text(
                    text = when (dialogMode) {
                        ContactDialogMode.PICKER -> "Pilih Kontak"
                        ContactDialogMode.MANAGEMENT -> "Kelola Kontak"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )

                // Search - Simple like original
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Cari nama atau nomor telepon...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))

                // ✅ Action buttons - Show only in PICKER mode or always show
                if (dialogMode == ContactDialogMode.PICKER || dialogMode == ContactDialogMode.MANAGEMENT) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showCreateDialog = true },
                            enabled = !isCreatingContact && !isImportingContacts,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isCreatingContact) CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            else Icon(Icons.Default.PersonAddAlt, null)
                            Spacer(Modifier.width(4.dp))
                            Text(if (isCreatingContact) "Membuat..." else "Buat Baru")
                        }

                        OutlinedButton(
                            onClick = onImportDeviceContacts,
                            enabled = !isCreatingContact && !isImportingContacts,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isImportingContacts) CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            else Icon(Icons.Default.ImportContacts, null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(4.dp))
                            Text(if (isImportingContacts) "Mengimpor..." else "Import ")
                        }
                    }
                }

                Divider(Modifier.padding(vertical = 8.dp))

                // Contact list / loading / empty
                Box(Modifier.weight(1f)) {
                    when {
                        isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(8.dp))
                                Text("Memuat kontak...")
                            }
                        }

                        filteredContacts.isEmpty() && searchQuery.isNotBlank() -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "Tidak ada kontak ditemukan",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        "untuk pencarian \"$searchQuery\"",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        filteredContacts.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Belum ada kontak")
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Klik 'Buat Baru' atau 'Import Device' untuk menambah kontak",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        else -> LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredContacts, key = { it.contactId }) { contact ->
                                SimpleContactItem(
                                    contact = contact,
                                    onClick = { onContactSelected(contact) },
                                    onDelete = { onDeleteContact(contact) },
                                    onEdit = { onEditContact(contact) }, // ✅ NEW: Edit callback
                                    dialogMode = dialogMode
                                )
                            }
                        }
                    }
                }

                // Search result counter - Simple
                if (searchQuery.isNotBlank()) {
                    Text(
                        text = "${filteredContacts.size} kontak ditemukan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }

    // Create Contact Dialog - Simple
    if (showCreateDialog) {
        SimpleCreateContactDialog(
            onDismiss = { showCreateDialog = false },
            onCreateContact = { name, phone ->
                onCreateNewContact(name, phone)
                showCreateDialog = false
            }
        )
    }
}

/**
 * ✅ ENHANCED ContactItem dengan Edit & Delete functionality
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SimpleContactItem(
    contact: FirestoreContact,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    onEdit: () -> Unit = {}, // ✅ NEW: Edit callback
    dialogMode: ContactDialogMode = ContactDialogMode.PICKER
) {
    var showActionIcons by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (showActionIcons) {
                        showActionIcons = false
                    } else {
                        onClick()
                    }
                },
                onLongClick = {
                    // ✅ Only show action icons in MANAGEMENT mode or always allow long press
                    showActionIcons = !showActionIcons
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Simple Avatar
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Contact Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (contact.phoneNumber.isNotBlank()) {
                    Text(
                        text = contact.phoneNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // ✅ Icon area - Show edit/delete or source icon
            if (showActionIcons) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ✅ Edit button
                    IconButton(
                        onClick = {
                            onEdit()
                            showActionIcons = false
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Contact",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Delete button
                    IconButton(
                        onClick = {
                            onDelete()
                            showActionIcons = false
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Contact",
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Cancel button
                    IconButton(
                        onClick = { showActionIcons = false }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                // Source Icon
                val (sourceIcon, sourceColor) = when (contact.source) {
                    ContactSource.MANUAL.name -> Icons.Default.PersonAdd to MaterialTheme.colorScheme.primary
                    ContactSource.DEVICE.name -> Icons.Default.ContactPhone to MaterialTheme.colorScheme.secondary
                    ContactSource.SHARED.name -> Icons.Default.CloudSync to MaterialTheme.colorScheme.tertiary
                    else -> Icons.Default.AccountCircle to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                }

                Icon(
                    imageVector = sourceIcon,
                    contentDescription = when (contact.source) {
                        ContactSource.MANUAL.name -> "Manual Contact"
                        ContactSource.DEVICE.name -> "Device Contact"
                        ContactSource.SHARED.name -> "Shared Contact"
                        else -> "Unknown Source"
                    },
                    tint = sourceColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * ✅ Dialog Mode Enum
 */
enum class ContactDialogMode {
    PICKER,      // Mode untuk memilih kontak (dari debt creation, dll)
    MANAGEMENT   // Mode untuk manage kontak (dari DebtSummaryCard)
}

/**
 * ✅ SIMPLE CreateContactDialog - Back to original style
 */
@Composable
fun SimpleCreateContactDialog(
    onDismiss: () -> Unit,
    onCreateContact: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    val canCreate = name.trim().isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White // Background create dialog putih
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Buat Kontak Baru",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap *") },
                    placeholder = { Text("Masukkan nama kontak") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Nomor Telepon (Opsional)") },
                    placeholder = { Text("08123456789") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

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
                        onClick = {
                            if (canCreate) {
                                onCreateContact(name.trim(), phone.trim())
                            }
                        },
                        enabled = canCreate,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Simpan",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}