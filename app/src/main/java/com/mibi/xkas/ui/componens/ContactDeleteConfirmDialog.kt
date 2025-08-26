package com.mibi.xkas.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mibi.xkas.data.model.FirestoreContact
import com.mibi.xkas.model.ContactDebtSummary
import com.mibi.xkas.utils.formatRupiah

/**
 * ✅ Confirmation dialog untuk delete contact dengan debt validation
 */
@Composable
fun ContactDeleteConfirmDialog(
    showDialog: Boolean,
    contact: FirestoreContact?,
    contactSummary: ContactDebtSummary? = null,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit,
    isDeleting: Boolean = false
) {
    if (!showDialog || contact == null) return

    val hasActiveDebt = contactSummary?.hasActiveDebt == true
    val canDelete = !hasActiveDebt

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Warning Icon
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = if (hasActiveDebt) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    } else {
                        Color.Red.copy(alpha = 0.1f)
                    },
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector = if (hasActiveDebt) Icons.Default.Warning else Icons.Default.Delete,
                        contentDescription = null,
                        tint = if (hasActiveDebt) {
                            MaterialTheme.colorScheme.error
                        } else {
                            Color.Red
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp)
                    )
                }

                // Title & Description
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (hasActiveDebt) "Tidak Dapat Dihapus" else "Hapus Kontak",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (hasActiveDebt) MaterialTheme.colorScheme.error else Color.Red,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (hasActiveDebt) {
                            "Kontak '${contact.name}' masih memiliki hutang aktif dan tidak dapat dihapus."
                        } else {
                            "Apakah Anda yakin ingin menghapus kontak '${contact.name}'?"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Contact Details
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Contact Info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Nama:",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                contact.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        if (contact.phoneNumber.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Telepon:",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Text(
                                    contact.phoneNumber,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        // Debt Summary (if has active debt)
                        if (hasActiveDebt && contactSummary != null) {
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "Hutang Aktif:",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        contactSummary.debtorLabel + ":",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        formatRupiah(contactSummary.absoluteBalance),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    )
                                }
                            }
                        }

                        // Source Info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Sumber:",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                when (contact.source) {
                                    "MANUAL" -> "📝 Manual"
                                    "DEVICE" -> "📱 Perangkat"
                                    "SHARED" -> "☁️ Shared"
                                    else -> "❓ Tidak Diketahui"
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Additional warning for active debt
                if (hasActiveDebt) {
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
                                text = "💡 Saran",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Selesaikan semua hutang dengan kontak ini terlebih dahulu, lalu coba hapus lagi.",
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
                    // Cancel Button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isDeleting
                    ) {
                        Text(
                            if (hasActiveDebt) "Tutup" else "Batal",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    // Delete Button (only if can delete)
                    if (canDelete) {
                        Button(
                            onClick = onConfirmDelete,
                            enabled = !isDeleting,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red,
                                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            )
                        ) {
                            if (isDeleting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                if (isDeleting) "Menghapus..." else "Hapus",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}