package com.mibi.xkas.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mibi.xkas.data.model.FirestoreContact

data class EditContactData(
    val name: String = "",
    val phoneNumber: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditContactDialog(
    showDialog: Boolean,
    contact: FirestoreContact?,
    onDismiss: () -> Unit,
    onUpdate: (FirestoreContact, EditContactData) -> Unit,
    isLoading: Boolean = false
) {
    if (!showDialog || contact == null) return

    var contactData by remember(contact) {
        mutableStateOf(
            EditContactData(
                name = contact.name,
                phoneNumber = contact.phoneNumber
            )
        )
    }

    // Reset data when dialog opens with new contact
    LaunchedEffect(showDialog, contact.contactId) {
        if (showDialog && contact != null) {
            contactData = EditContactData(
                name = contact.name,
                phoneNumber = contact.phoneNumber
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
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Edit Kontak",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Perbarui informasi kontak ${contact.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Contact Source Info
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (contact.source) {
                            "MANUAL" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            "DEVICE" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sourceText = when (contact.source) {
                            "MANUAL" -> "📝 Kontak Manual"
                            "DEVICE" -> "📱 Dari Perangkat"
                            "SHARED" -> "☁️ Kontak Shared"
                            else -> "❓ Sumber Tidak Diketahui"
                        }
                        Text(
                            text = sourceText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Name Field
                OutlinedTextField(
                    value = contactData.name,
                    onValueChange = { contactData = contactData.copy(name = it) },
                    label = { Text("Nama Lengkap *") },
                    placeholder = { Text("Masukkan nama kontak") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                // Phone Field
                OutlinedTextField(
                    value = contactData.phoneNumber,
                    onValueChange = { contactData = contactData.copy(phoneNumber = it) },
                    label = { Text("Nomor Telepon *") },
                    placeholder = { Text("08123456789 atau +628123456789") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                // Warning for device contacts
                if (contact.source == "DEVICE") {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "⚠️ Perhatian",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Kontak ini berasal dari perangkat. Perubahan hanya akan tersimpan di aplikasi ini, tidak mengubah kontak asli di perangkat.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    ) {
                        Text(
                            "Batal",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Button(
                        onClick = { onUpdate(contact, contactData) },
                        enabled = contactData.name.isNotBlank() &&
                                contactData.phoneNumber.isNotBlank() &&
                                !isLoading &&
                                (contactData.name != contact.name || contactData.phoneNumber != contact.phoneNumber),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            if (isLoading) "Menyimpan..." else "Perbarui",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (isLoading) Color.White.copy(alpha = 0.7f) else Color.White
                        )
                    }
                }
            }
        }
    }
}