package com.mibi.xkas.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mibi.xkas.data.InterpretedDomainType
import com.mibi.xkas.data.Transaction
import com.mibi.xkas.ui.navigation.Screen
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone


// Helper function untuk format mata uang
fun formatCurrency(amount: Double): String {
    val localeID = Locale("in", "ID")
    val currencyFormat = NumberFormat.getCurrencyInstance(localeID)
    currencyFormat.maximumFractionDigits = 0 // Jika tidak ingin ada angka di belakang koma untuk Rupiah
    return currencyFormat.format(amount)
}

// Helper function untuk format tanggal
fun formatDate(date: java.util.Date?): String {
    if (date == null) return "N/A"
    // Anda bisa memilih format yang lebih sesuai, misal: "EEEE, dd MMMM yyyy HH:mm"
    val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID"))
    sdf.timeZone = TimeZone.getDefault() // Gunakan timezone device
    return sdf.format(date)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    navController: NavController,
    transactionDetailViewModel: TransactionDetailViewModel = hiltViewModel()
) {
    val uiState by transactionDetailViewModel.uiState.collectAsState()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Handle navigasi setelah delete success atau acknowledge error
    LaunchedEffect(uiState) {
        when (uiState) {
            is TransactionDetailUiState.DeleteSuccess -> {
                // Navigasi kembali setelah penghapusan berhasil
                navController.popBackStack()
                // Anda mungkin ingin menampilkan Snackbar singkat di layar sebelumnya
                // tentang keberhasilan penghapusan, tapi itu di luar scope ini.
            }
            // Anda bisa menangani DeleteError di sini jika ingin melakukan sesuatu secara otomatis,
            // tapi biasanya pesan error sudah ditampilkan di UI.
            // Jika ada aksi spesifik setelah error (misalnya reset state),
            // viewModel.acknowledgeDeleteResult() bisa dipanggil di sini atau dari UI.
            else -> { /* Tidak ada aksi khusus untuk state lain saat ini */ }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Transaksi") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    // Hanya tampilkan tombol Edit dan Hapus jika data berhasil dimuat
                    val currentUiState = uiState // Ambil state saat ini untuk pemeriksaan
                    if (currentUiState is TransactionDetailUiState.Success) {
                        // Akses transaksi dari state yang sudah diperiksa tipenya
                        val currentTransaction = currentUiState.transaction

                        IconButton(onClick = {
                            // TODO: Navigasi ke layar Edit Transaksi
                            navController.navigate(Screen.AddEditTransaction.createRouteForEdit(currentTransaction.transactionId))
                            println("Tombol Edit diklik untuk transaksi: ${currentTransaction.transactionId}")
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit Transaksi",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        IconButton(onClick = {
                            showDeleteConfirmDialog = true // Tampilkan dialog konfirmasi
                            // Tidak perlu currentTransaction di sini karena ViewModel sudah tahu
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Hapus Transaksi",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        // Tampilkan dialog jika showDeleteConfirmDialog adalah true
        if (showDeleteConfirmDialog) {
            DeleteConfirmationDialog(
                onConfirm = {
                    transactionDetailViewModel.deleteCurrentTransaction()
                    showDeleteConfirmDialog = false
                },
                onDismiss = {
                    showDeleteConfirmDialog = false
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val state = uiState) {
                is TransactionDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                is TransactionDetailUiState.Success -> {
                    TransactionDetailContent(transaction = state.transaction)
                }
                is TransactionDetailUiState.Error -> {
                    Text(
                        text = "Gagal memuat detail: ${state.message}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                is TransactionDetailUiState.NotFound -> {
                    Text(
                        text = "Transaksi tidak ditemukan.",
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                is TransactionDetailUiState.Deleting -> { // State baru: sedang menghapus
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    Text("Menghapus transaksi...", modifier = Modifier.padding(top = 8.dp))
                }
                is TransactionDetailUiState.DeleteError -> { // State baru: error saat menghapus
                    // Tampilkan detail transaksi sebelumnya jika masih ada, atau pesan error saja
                    // Untuk saat ini, kita tampilkan pesan error dan mungkin tombol untuk mencoba lagi/kembali
                    Text(
                        text = "Gagal menghapus: ${state.message}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    // Anda bisa menambahkan tombol di sini untuk memanggil
                    // transactionDetailViewModel.acknowledgeDeleteResult()
                    // atau membiarkan pengguna navigasi manual
                }
                // TransactionDetailUiState.DeleteSuccess ditangani oleh LaunchedEffect untuk navigasi
                is TransactionDetailUiState.DeleteSuccess -> {
                    // Biasanya tidak perlu menampilkan apa-apa di sini karena LaunchedEffect akan navigasi
                    // Tapi sebagai fallback, bisa tampilkan pesan singkat atau loading
                    Text("Transaksi berhasil dihapus. Mengarahkan kembali...", modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
    }
}

// Composable baru untuk dialog konfirmasi
@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Konfirmasi Hapus") },
        text = { Text("Apakah Anda yakin ingin menghapus transaksi ini? Tindakan ini tidak dapat diurungkan.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Hapus")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun TransactionDetailContent(transaction: Transaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White // Latar belakang Card putih
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            DetailRow(label = "Deskripsi:", value = transaction.description)
            StyledDivider() // Jika Anda masih ingin menggunakan divider

            DetailRow(label = "Tanggal:", value = formatDate(transaction.date))
            StyledDivider()

            val amountColor = when (transaction.interpretedType) {
                InterpretedDomainType.SALE_WITH_COST, InterpretedDomainType.PURE_INCOME -> Color(0xFF008000)
                InterpretedDomainType.PURE_EXPENSE -> Color.Red
            }

            when (transaction.interpretedType) {
                InterpretedDomainType.SALE_WITH_COST -> {
                    DetailRow(label = "Harga Jual:", value = formatCurrency(transaction.sellingPrice ?: 0.0), valueColor = amountColor)
                    StyledDivider()
                    DetailRow(label = "Modal (HPP):", value = formatCurrency(transaction.amount), valueColor = Color.Red)
                    StyledDivider()
                    val profit = (transaction.sellingPrice ?: 0.0) - transaction.amount
                    val profitColor = if (profit >= 0) Color(0xFF008000) else Color.Red
                    DetailRow(label = "Keuntungan:", value = formatCurrency(profit), valueColor = profitColor, isBoldValue = true)
                }
                InterpretedDomainType.PURE_INCOME -> {
                    DetailRow(label = "Jumlah Pemasukan:", value = formatCurrency(transaction.amount), valueColor = amountColor, isBoldValue = true)
                }
                InterpretedDomainType.PURE_EXPENSE -> {
                    DetailRow(label = "Jumlah Pengeluaran:", value = formatCurrency(transaction.amount), valueColor = amountColor, isBoldValue = true)
                }
            }
            StyledDivider()

            Spacer(modifier = Modifier.height(8.dp))
            DetailRow(label = "Tipe Asli:", value = transaction.type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
            StyledDivider()

            DetailRow(label = "ID Transaksi:", value = transaction.transactionId, valueFontSize = 10.sp, labelFontSize = 10.sp)
            StyledDivider()

            DetailRow(label = "Dibuat Pada:", value = formatDate(transaction.createdAt), valueFontSize = 10.sp, labelFontSize = 10.sp)
            transaction.updatedAt?.let {
                StyledDivider()
                DetailRow(label = "Diperbarui Pada:", value = formatDate(it), valueFontSize = 10.sp, labelFontSize = 10.sp)
            }
        }
    }
}

// Composable helper untuk Divider (jika Anda masih menggunakannya)
@Composable
fun StyledDivider() {
    Divider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), // Warna lebih lembut lagi
        thickness = 0.5.dp,
        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp) // Kurangi padding jika baris lebih rapat
    )
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    isBoldValue: Boolean = false,
    labelFontSize: TextUnit = 14.sp, // Ukuran font label sedikit lebih besar dari sebelumnya
    valueFontSize: TextUnit = 16.sp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp), // Padding vertikal antar baris
        verticalAlignment = Alignment.CenterVertically // Pusatkan item secara vertikal dalam Row
    ) {
        Text(
            text = label,
            fontSize = labelFontSize,
            color = MaterialTheme.colorScheme.onSurfaceVariant, // Warna untuk label agar sedikit berbeda
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.4f) // Label mengambil 40% dari lebar yang tersedia
        )
        // Spacer(modifier = Modifier.width(8.dp)) // Bisa dihilangkan jika weight sudah cukup mengatur jarak
        Text(
            text = value,
            fontSize = valueFontSize,
            fontWeight = if (isBoldValue) FontWeight.Bold else FontWeight.Normal,
            color = valueColor,
            modifier = Modifier.weight(0.6f) // Nilai mengambil 60% dari lebar yang tersedia
            // textAlign = TextAlign.End // Opsional: jika ingin nilai rata kanan
        )
    }
}