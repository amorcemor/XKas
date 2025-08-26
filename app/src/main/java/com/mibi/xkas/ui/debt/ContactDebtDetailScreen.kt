package com.mibi.xkas.ui.debt

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mibi.xkas.data.model.DebtPayment
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DebtDetailScreen(
    debtId: String,
    viewModel: DebtDetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit
) {
    LaunchedEffect(debtId) {
        if (debtId.isNotBlank()) {
            viewModel.observeDebtAndPayments(debtId)
        }
    }

    val debt by viewModel.debt.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val screenErrorMessage by viewModel.screenErrorMessage.collectAsState()
    val operationErrorMessage by viewModel.operationErrorMessage.collectAsState()
    val newPaymentAmount by viewModel.newPaymentAmount.collectAsState()
    val isDialogVisible by viewModel.showAddPaymentDialog.collectAsState()

    // State untuk dialog konfirmasi hapus pembayaran
    var showDeleteDialog by remember { mutableStateOf(false) }
    var paymentToDelete by remember { mutableStateOf<DebtPayment?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Hutang") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToEdit(debtId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Utang")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.setAddDialogVisible(true) }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Pelunasan")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {

            screenErrorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
            }

            debt?.let { currentDebt ->
                Text("Nama: ${currentDebt.contactName}", style = MaterialTheme.typography.titleLarge)
                Text("Nomor: ${currentDebt.contactPhone ?: "-"}", style = MaterialTheme.typography.bodyLarge)
                Text("Total Hutang: Rp ${"%,.0f".format(currentDebt.totalAmount)}", style = MaterialTheme.typography.bodyLarge)
                Text("Telah Dibayar: Rp ${"%,.0f".format(currentDebt.paidAmount)}", style = MaterialTheme.typography.bodyLarge)

                if (currentDebt.transactionId.isNotBlank()) {
                    Text(
                        text = "Dari Transaksi: #${currentDebt.transactionId}",
                        style = MaterialTheme.typography.bodyMedium,
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.combinedClickable(
                            onClick = { /* Navigasi ke detail transaksi */ }
                        )
                    )
                }

                val remainingAmount = currentDebt.totalAmount - currentDebt.paidAmount
                if (remainingAmount > 0) {
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.payOffDebt() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Lunasi Sekarang: ${formatRupiah(remainingAmount)}")
                    }
                }

                Text("Sisa Hutang: Rp ${"%,.0f".format(remainingAmount)}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = if (remainingAmount <= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    ))
                Spacer(Modifier.height(16.dp))
                Divider()
                Text("Riwayat Pelunasan", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))

                if (payments.isEmpty()) {
                    Text("Belum ada pelunasan.")
                } else {
                    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

                    LazyColumn {
                        items(payments) { payment ->
                            val formattedDate = payment.paidAt?.let { sdf.format(it) } ?: "Tanggal tidak diketahui"

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .combinedClickable(
                                        onClick = { /* Normal click - bisa untuk detail */ },
                                        onLongClick = {
                                            paymentToDelete = payment
                                            showDeleteDialog = true
                                        }
                                    ),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            "Rp ${"%,.0f".format(payment.amount)}",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    },
                                    supportingContent = {
                                        Text(formattedDate)
                                    },
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                paymentToDelete = payment
                                                showDeleteDialog = true
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Hapus Pembayaran",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Info untuk user
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tekan lama atau klik ikon hapus untuk menghapus pembayaran",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            } ?: run {
                if (screenErrorMessage == null) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    Text("Memuat data...", modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp))
                } else if (debtId.isBlank()) {
                    Text("ID Hutang tidak valid.", modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }

        // Dialog Tambah Pelunasan
        if (isDialogVisible) {
            AlertDialog(
                onDismissRequest = { viewModel.setAddDialogVisible(false) },
                title = { Text("Tambah Pelunasan") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newPaymentAmount,
                            onValueChange = { viewModel.setNewPaymentAmount(it) },
                            label = { Text("Jumlah") },
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                            isError = operationErrorMessage != null
                        )
                        operationErrorMessage?.let {
                            Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (debtId.isNotBlank()) {
                            val amount = newPaymentAmount.toDoubleOrNull() ?: 0.0
                            viewModel.submitPayment(debtId, amount)
                        }
                    }) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.setAddDialogVisible(false) }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Dialog Konfirmasi Hapus Pembayaran
        if (showDeleteDialog && paymentToDelete != null) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteDialog = false
                    paymentToDelete = null
                },
                title = { Text("Hapus Pembayaran") },
                text = {
                    Text("Apakah Anda yakin ingin menghapus pembayaran sebesar Rp ${"%,.0f".format(paymentToDelete!!.amount)}?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            paymentToDelete?.let { payment ->
                                viewModel.deletePayment(debtId, payment)
                            }
                            showDeleteDialog = false
                            paymentToDelete = null
                        }
                    ) {
                        Text("Hapus", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        paymentToDelete = null
                    }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}