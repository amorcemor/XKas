package com.mibi.xkas.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.mibi.xkas.R
import com.mibi.xkas.data.InterpretedDomainType
import com.mibi.xkas.data.Transaction
import com.mibi.xkas.ui.components.BusinessUnitSelectionDialog
import com.mibi.xkas.ui.navigation.Screen
import com.mibi.xkas.ui.theme.XKasTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.LaunchedEffect // Pastikan ini di-import
import android.widget.Toast // Untuk pesan jika BU belum dipilih
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext // Untuk Toast
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth

private val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
private val monthNameFormat = SimpleDateFormat("MMMM", Locale("id", "ID")) // Misal untuk Indonesia
private val customRangeDisplayFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHomeScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = hiltViewModel(),
    onFabActionReady: (action: () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val userDisplayName = currentUser?.displayName ?: currentUser?.email?.substringBefore("@") ?: "User"
    val userPhotoUrl = currentUser?.photoUrl?.toString()
    // --- PANGGIL SEKALI SAAT KOMPOSISI AWAL ---
    LaunchedEffect(key1 = Unit) {
        if (businessUnitUiState !is HomeViewModel.BusinessUnitUiState.Success) {
            homeViewModel.triggerInitialBusinessUnitCheck()
        }
    }

    // --- State BusinessUnit ---
    val businessUnitUiState by homeViewModel.businessUnitUiState.collectAsStateWithLifecycle()
    val selectedBusinessUnit by homeViewModel.selectedBusinessUnit.collectAsStateWithLifecycle()
    val showBusinessUnitSelectionDialog by homeViewModel.showBusinessUnitSelectionDialog.collectAsStateWithLifecycle()

    val currentFilterType by homeViewModel.currentDateFilterType.collectAsStateWithLifecycle() // Cukup satu deklarasi
    val selectedStartDateMillis by homeViewModel.selectedStartDate.collectAsStateWithLifecycle()
    val selectedEndDateMillis by homeViewModel.selectedEndDate.collectAsStateWithLifecycle()
    val showNewUserDialog by homeViewModel.showAddFirstTransactionDialog.collectAsStateWithLifecycle()

    var showFilterDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedStartDateMillis ?: System.currentTimeMillis()
    )

    val topAppBarContentColor = MaterialTheme.colorScheme.onSurface
    val filterBoxBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val filterBoxBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    val filterText = when (currentFilterType) {
        DateFilterType.TODAY -> "Hari Ini"
        DateFilterType.THIS_WEEK -> "Minggu Ini"
        DateFilterType.THIS_MONTH -> "Bulan Ini"
        DateFilterType.ALL_TIME -> "Semua Transaksi"
        DateFilterType.CUSTOM_RANGE -> {
            if (selectedStartDateMillis != null && selectedEndDateMillis != null) {
                val startDateFormatted = customRangeDisplayFormat.format(Date(selectedStartDateMillis!!))
                val endDateFormatted = customRangeDisplayFormat.format(Date(selectedEndDateMillis!!))
                if (startDateFormatted == endDateFormatted) {
                    startDateFormatted
                } else {
                    "$startDateFormatted - $endDateFormatted"
                }
            } else {
                "Pilih Tanggal"
            }
        }
    }

    // --- Dialog Transaksi Pertama ---
//    if (showNewUserDialog) {
//        AddFirstTransactionDialog(
//            onDismissRequest = { homeViewModel.onDialogDismissed() },
//            onConfirm = {
//                homeViewModel.onDialogConfirmed()
//                navController.navigate(SecondaryAppDestinations.ADD_TRANSACTION_ROUTE)
//            }
//        )
//    }

    // --- Dialog Filter Tanggal ---
    if (showFilterDialog) {
        DateFilterDialog(
            onDismissRequest = { showFilterDialog = false },
            onFilterSelected = { filterType ->
                showFilterDialog = false
                if (filterType == DateFilterType.CUSTOM_RANGE) {
                    datePickerState.selectedDateMillis = selectedStartDateMillis ?: System.currentTimeMillis()
                    showDatePickerDialog = true
                } else {
                    homeViewModel.setDateFilter(filterType)
                }
            },
            currentSelectedType = currentFilterType
        )
    }

    // --- DatePickerDialog ---
    if (showDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePickerDialog = false
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val calendar = Calendar.getInstance().apply { timeInMillis = selectedMillis }
                            calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                            val startDate = calendar.timeInMillis
                            calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59); calendar.set(Calendar.MILLISECOND, 999)
                            val endDate = calendar.timeInMillis
                            homeViewModel.setCustomDateRange(startDate, endDate)
                        }
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // --- BARU: Dialog Pemilihan Unit Bisnis ---
    if (showBusinessUnitSelectionDialog) {
        BusinessUnitSelectionDialog(
            businessUnitUiState = businessUnitUiState,
            selectedBusinessUnit = selectedBusinessUnit,
            onDismissRequest = { homeViewModel.onBusinessUnitDialogDismiss() },
            onBusinessUnitSelected = { bu ->
                homeViewModel.setSelectedBusinessUnit(bu)
                homeViewModel.onBusinessUnitDialogDismiss()
            },
            onAddBusinessUnitClicked = { // <--- UBAH NAMA PARAMETER INI
                homeViewModel.onBusinessUnitDialogDismiss() // Tutup dialog sebelum navigasi
                navController.navigate(Screen.AddEditBusinessUnit.route) // Ganti dengan rute Anda
            },
            onDeleteBusinessUnitConfirmed = { businessUnitId ->
                // Panggil fungsi di ViewModel Anda untuk menghapus BU
                homeViewModel.deleteBusinessUnit(businessUnitId)
                // ViewModel Anda juga harus menangani logika jika BU yang dihapus adalah yang sedang dipilih
            },
            onUpdateBusinessUnitNameConfirmed = { businessUnitId, newName ->
                homeViewModel.updateBusinessUnitName(businessUnitId, newName)
            }
        )
    }

    val context = LocalContext.current // Dapatkan context untuk Toast

    // --- PERUBAHAN 2: Konfigurasikan aksi FAB menggunakan LaunchedEffect ---
    LaunchedEffect(selectedBusinessUnit) {
        onFabActionReady {
            val buId = selectedBusinessUnit?.businessUnitId
            if (!buId.isNullOrBlank()) {
                // Jika BU sudah dipilih, navigasi ke layar tambah transaksi dengan ID BU
                navController.navigate(Screen.AddEditTransaction.createRouteForNew(buId))
            } else {
                // Jika belum ada BU yang dipilih, beri tahu pengguna
                Toast.makeText(context, "Silakan pilih Unit Bisnis terlebih dahulu", Toast.LENGTH_SHORT).show()
                // Secara opsional, bisa juga langsung membuka dialog pemilihan BU
                // homeViewModel.onTopBarBusinessUnitClicked()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // --- MODIFIKASI: Business Unit Selector di TopAppBar ---
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp) // Beri padding agar tidak terlalu mepet ke filter
                        .clickable(
                            onClick = { homeViewModel.onTopBarBusinessUnitClicked() },
                            enabled = businessUnitUiState !is HomeViewModel.BusinessUnitUiState.Loading // Nonaktifkan klik saat loading BU
                        )
                        .padding(vertical = 8.dp) // Padding untuk area klik yang lebih baik
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            when (val buState = businessUnitUiState) {
                                is HomeViewModel.BusinessUnitUiState.Loading -> {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Memuat...",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = topAppBarContentColor.copy(alpha = 0.7f)
                                    )
                                }
                                is HomeViewModel.BusinessUnitUiState.Success -> {
                                    Text(
                                        text = selectedBusinessUnit?.name ?: "Pilih Bisnis",
                                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp), // Sesuaikan ukuran font
                                        fontWeight = FontWeight.SemiBold,
                                        color = topAppBarContentColor,
                                        maxLines = 1
                                    )
                                    // Tampilkan ikon dropdown hanya jika ada lebih dari satu BU atau tidak ada BU (untuk memicu dialog)
                                    if (buState.businessUnit.size > 1 || buState.businessUnit.isEmpty() || selectedBusinessUnit == null) {
                                        Icon(
                                            imageVector = Icons.Filled.ArrowDropDown,
                                            contentDescription = "Pilih Bisnis",
                                            tint = topAppBarContentColor,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                }
                                is HomeViewModel.BusinessUnitUiState.Empty -> {
                                    Text(
                                        text = "Tambah Unit Bisnis",
                                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                                        fontWeight = FontWeight.SemiBold,
                                        color = topAppBarContentColor
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        contentDescription = "Tambah Unit Bisnis",
                                        tint = topAppBarContentColor,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                                is HomeViewModel.BusinessUnitUiState.Error -> {
                                    Text(
                                        "Gagal memuat",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.ErrorOutline,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = filterBoxBackgroundColor,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable { showFilterDialog = true }
                            .border(
                                width = 1.dp,
                                color = filterBoxBorderColor,
                                shape = RoundedCornerShape(10.dp)
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(text = filterText, style = MaterialTheme.typography.bodyMedium, color = topAppBarContentColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(imageVector = Icons.Filled.CalendarMonth, contentDescription = "Filter Tanggal", tint = topAppBarContentColor)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent, // Atau MaterialTheme.colorScheme.surface jika ingin ada latar belakang
                    titleContentColor = topAppBarContentColor,
                    actionIconContentColor = topAppBarContentColor
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
    ) { innerPadding ->
        val transactionListUiState by homeViewModel.transactionsUiState.collectAsStateWithLifecycle()

        when (val state = transactionListUiState) {
            is TransactionListUiState.Loading -> {
                Box(modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is TransactionListUiState.Success -> {
                // ... (logika perhitungan totalSales, totalCost, profit tetap sama) ...
                val totalSales: Double = state.transactions.sumOf { transaction ->
                    when (transaction.interpretedType) {
                        InterpretedDomainType.SALE_WITH_COST -> transaction.sellingPrice ?: 0.0
                        InterpretedDomainType.PURE_INCOME -> transaction.amount
                        InterpretedDomainType.PURE_EXPENSE -> 0.0
                    }
                }
                val totalCost: Double = state.transactions.sumOf { transaction ->
                    when (transaction.interpretedType) {
                        InterpretedDomainType.SALE_WITH_COST -> transaction.amount
                        InterpretedDomainType.PURE_EXPENSE -> transaction.amount
                        InterpretedDomainType.PURE_INCOME -> 0.0
                    }
                }
                val profit = totalSales - totalCost

                val referenceDateForProfitCard = when (currentFilterType) {
                    DateFilterType.TODAY, DateFilterType.THIS_WEEK, DateFilterType.THIS_MONTH -> Date()
                    DateFilterType.CUSTOM_RANGE -> if (selectedStartDateMillis != null) Date(selectedStartDateMillis!!) else Date()
                    DateFilterType.ALL_TIME -> if (state.transactions.isNotEmpty()) state.transactions.first().date else Date()
                }
                TransactionContent(
                    navController = navController,
                    profit = profit,
                    totalSales = totalSales,
                    totalCost = totalCost,
                    transactions = state.transactions,
                    currentFilterTypeForProfitCard = currentFilterType,
                    referenceDateForProfitCard = referenceDateForProfitCard,
                    userDisplayName = userDisplayName, // Parameter baru
                    userPhotoUrl = userPhotoUrl, // Parameter baru
                    modifier = Modifier.padding(innerPadding)
                )
            }
            is TransactionListUiState.Empty -> {
                Box(modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.profit_card_background_decoration), // Ganti dengan ID drawable Anda
                            contentDescription = "Tidak ada data",
                            modifier = Modifier.size(180.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Belum Ada Transaksi",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Catat transaksi pertamamu untuk memulai.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // Tombol untuk menambah transaksi bisa juga diletakkan di sini
                        // jika dialog pengguna baru tidak muncul atau ditolak
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Klik tombol dibawah untuk menambah transaksi pertamamu",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            is TransactionListUiState.FirstTimeEmpty -> { // State ini mungkin bisa digabung dengan Empty
                // Atau tampilkan UI khusus jika dialog first time ditolak
                Box(modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_rocket_launch), // Ganti dengan ID drawable Anda
                            contentDescription = "Tidak ada data",
                            modifier = Modifier.size(180.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Siap Mencatat?",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Mulai catat transaksi keuangan unit bisnismu sekarang.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { navController.navigate(Screen.AddEditTransaction.route) }) {
                            Text("Tambah Transaksi Pertama")
                        }
                    }
                }
            }
            is TransactionListUiState.NoResultsForFilter -> {
                Box(modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_rocket_launch), // Ganti dengan ID drawable Anda
                            contentDescription = "Filter tidak menemukan hasil",
                            modifier = Modifier.size(180.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Tidak Ada Hasil",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Coba ubah filter tanggal atau kata kunci pencarianmu.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            is TransactionListUiState.SelectBusinessUnit -> {
                Box(modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_rocket_launch), // Ganti dengan ID drawable Anda
                            contentDescription = "Pilih Bisnis",
                            modifier = Modifier.size(180.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Pilih Bisnis",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Ketuk nama unit bisnis di atas untuk memilih atau menambah yang baru.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // Tambahkan tombol untuk langsung trigger dialog jika diperlukan,
                        // meskipun klik pada TopAppBar sudah seharusnya cukup.
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = { homeViewModel.onTopBarBusinessUnitClicked() }) {
                            Text("Pilih atau Tambah Unit Bisnis")
                        }
                    }
                }
            }
            is TransactionListUiState.Error -> {
                Box(modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_rocket_launch), // Ganti dengan ID drawable Anda
                            contentDescription = "Error",
                            modifier = Modifier.size(180.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Oops! Terjadi Kesalahan",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            state.message ?: "Tidak dapat memuat data. Periksa koneksi internet Anda.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,

                            )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            // Coba refresh BU dulu jika errornya mungkin terkait BU, lalu transaksi
                            homeViewModel.triggerInitialBusinessUnitCheck() // Atau fungsi refresh BU yang lebih spesifik jika ada
                            // homeViewModel.refreshTransactionsForCurrentBusinessUnit() // Ini akan dipanggil otomatis jika BU berhasil dipilih
                        }) {
                            Text("Coba Lagi")
                        }
                    }
                }
            }
        }
    }
}

// --- BARU: Composable untuk Dialog Pengguna Baru ---
@Composable
fun AddFirstTransactionDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Selamat Datang!") },
        text = { Text("Sepertinya Anda belum memiliki transaksi. Apakah Anda ingin menambahkannya sekarang?") },
        confirmButton = {
            Button(onClick = {
                onConfirm() // Panggil lambda onConfirm
            }) {
                Text("Tambah Transaksi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { // Panggil lambda onDismissRequest
                Text("Nanti Saja")
            }
        }
    )
}

@Composable
fun DateFilterDialog(
    onDismissRequest: () -> Unit,
    onFilterSelected: (DateFilterType) -> Unit,
    currentSelectedType: DateFilterType
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Pilih Filter Tanggal",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            LazyColumn { // Gunakan LazyColumn jika daftar bisa panjang
                items(DateFilterType.values()) { filterType ->
                    val isSelected = filterType == currentSelectedType
                    val cardBackgroundColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) // Warna aksen sangat tipis untuk item terpilih
                        // Atau jika ingin tetap putih/surface tapi dengan border:
                        // MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.surface // Warna dasar card (biasanya putih di tema terang)
                    }

                    val textColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary // Warna teks untuk item terpilih (jika card bg berubah)
                        // Atau tetap onSurface jika card bg tidak banyak berubah:
                        // MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant // Warna teks standar untuk item tidak terpilih
                    }

                    val iconColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                onFilterSelected(filterType)
                                // onDismissRequest() // Pertimbangkan untuk menutup dialog di sini atau dari MainHomeScreen
                            },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = cardBackgroundColor
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 0.dp // Hapus elevation jika ingin tampilan lebih flat
                        )
                        // Jika Anda memilih border untuk item terpilih daripada warna background:
                        // border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (filterType == DateFilterType.CUSTOM_RANGE) {
                                    Icons.Outlined.DateRange
                                } else {
                                    Icons.Outlined.CalendarToday
                                },
                                contentDescription = null,
                                tint = iconColor // Gunakan warna ikon yang sudah ditentukan
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = when (filterType) {
                                    DateFilterType.TODAY -> "Hari Ini"
                                    DateFilterType.THIS_WEEK -> "Minggu Ini"
                                    DateFilterType.THIS_MONTH -> "Bulan Ini"
                                    DateFilterType.ALL_TIME -> "Semua Transaksi"
                                    DateFilterType.CUSTOM_RANGE -> "Pilih Tanggal"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = textColor, // Gunakan warna teks yang sudah ditentukan
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                            )
                            Spacer(Modifier.weight(1f)) // Dorong ikon centang ke kanan
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Terpilih",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismissRequest,
            ) {
                Text(
                    "TUTUP",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary // Warna tombol konfirmasi
                )
            }
        },
        // Atur warna background AlertDialog itu sendiri jika perlu
        containerColor = MaterialTheme.colorScheme.surface, // Background utama dialog
        textContentColor = MaterialTheme.colorScheme.onSurface, // Warna teks default di dalam dialog
        titleContentColor = MaterialTheme.colorScheme.onSurface
    )
}


@Composable
fun TransactionContent(
    navController: NavController,
    modifier: Modifier = Modifier,
    profit: Double,
    totalSales: Double,
    totalCost: Double,
    transactions: List<Transaction>,
    currentFilterTypeForProfitCard: DateFilterType,
    referenceDateForProfitCard: Date,
    userDisplayName: String,          // <- tambahan
    userPhotoUrl: String?
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        ProfitCard(
            profit = profit,
            filterType = currentFilterTypeForProfitCard,
            referenceDate = referenceDateForProfitCard,
            onProfileClick = {
                // Navigasi ke profile
                navController.navigate(Screen.MainProfile.route)
            },
            userDisplayName = userDisplayName,   // ✅ tambahkan
            userPhotoUrl = userPhotoUrl          // ✅ tambahkan
        )
        Spacer(modifier = Modifier.height(16.dp))
        SummaryCard(totalSales = totalSales, totalCost = totalCost)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Riwayat Transaksi",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (transactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tidak ada riwayat transaksi untuk ditampilkan.")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(transactions, key = { it.transactionId }) { transaction ->
                    TransactionRowItem(
                        transaction = transaction,
                        navController = navController
                    )
                }
            }
        }
    }
}

@Composable
fun ProfitCard(
    profit: Double,
    filterType: DateFilterType,
    referenceDate: Date,
    onProfileClick: () -> Unit,
    userDisplayName: String = "User", // Parameter baru untuk nama user
    userPhotoUrl: String? = null, // Parameter baru untuk foto user
    modifier: Modifier = Modifier
) {
    val backgroundColor = Color(0xFF37474F)
    val textColor = Color.White

    val calendar = Calendar.getInstance()
    calendar.time = referenceDate

    val periodLabel = when (filterType) {
        DateFilterType.TODAY -> "Hari Ini"
        DateFilterType.THIS_WEEK -> "Minggu Ini"
        DateFilterType.THIS_MONTH -> {
            "Bulan ${monthNameFormat.format(calendar.time).capitalize(Locale.getDefault())}"
        }
        DateFilterType.CUSTOM_RANGE -> {
            "Tanggal ${customRangeDisplayFormat.format(referenceDate)}"
        }
        DateFilterType.ALL_TIME -> "Semua Transaksi"
    }

    Card(
        modifier = modifier.height(168.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background decoration
            Image(
                painter = painterResource(id = R.drawable.profit_card_background_decoration),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .width(150.dp)
                    .height(180.dp),
                contentScale = ContentScale.Fit
            )

            // Modern Profile Section - di kanan atas
            Surface(
                onClick = onProfileClick,
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.15f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clickable { onProfileClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar/Profile Picture
                    if (userPhotoUrl != null) {
                        AsyncImage(
                            model = userPhotoUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = R.drawable.ic_rocket_launch) // Fallback icon
                        )
                    } else {
                        // Default avatar
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userDisplayName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "Halo,",
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                        Text(
                            text = userDisplayName.take(8), // Batasi panjang nama
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = textColor,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = "Go to Profile",
                        tint = textColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Keuntungan",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = periodLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor.copy(alpha = 0.8f)
                    )
                }

                Text(
                    text = "Rp ${"%,.0f".format(profit).replace(",", ".")}",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (profit >= 0) Color(0xFFFFFFFF) else Color(0xFFEF5350),
                )
            }
        }
    }
}


@Composable
fun SummaryCard(totalSales: Double, totalCost: Double, modifier: Modifier = Modifier) {
    val cardBackgroundColor = Color(0xFF81B2CA) // Warna #81B2CA
    val cardTextColor = Color.White

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor) // Set warna background card
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Kolom Kiri: Total Penjualan
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        "Penjualan",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = cardTextColor // Set warna teks
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Rp ${"%,.0f".format(totalSales).replace(",", ".")}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cardTextColor // Set warna teks
                        // Jika ingin warna berbeda untuk nominal penjualan, bisa diubah di sini
                        // Misalnya: color = Color(0xFFC8E6C9) // Hijau muda yang kontras dengan biru
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Kolom Kanan: Total Pengeluaran
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        "Pengeluaran",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = cardTextColor // Set warna teks
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Rp ${"%,.0f".format(totalCost).replace(",", ".")}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cardTextColor // Set warna teks
                        // Jika ingin warna berbeda untuk nominal pengeluaran, bisa diubah di sini
                        // Misalnya: color = Color(0xFFFFCDD2) // Merah muda yang kontras dengan biru
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionRowItem(
    // --- PERUBAHAN ---
    transaction: Transaction, // Sekarang menggunakan model domain Transaction
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { // <<<--- INI BAGIAN PENTING UNTUK NAVIGASI
                navController.navigate(Screen.TransactionDetail.createRoute(transaction.transactionId))
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description.takeIf { it.isNotBlank() } ?: "Tanpa deskripsi",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    // --- PERUBAHAN --- Format objek Date
                    text = try {
                        displayDateFormat.format(transaction.date)
                    } catch (e: Exception) {
                        "Tanggal tidak valid" // Fallback jika format gagal
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(horizontalAlignment = Alignment.End) {
                // --- PERUBAHAN --- Logika berdasarkan transaction.interpretedType
                var displayedAmount = false // Variabel ini mungkin perlu direvisi sedikit logikanya
                when (transaction.interpretedType) {
                    InterpretedDomainType.SALE_WITH_COST -> {
                        // Tampilkan harga jual (sellingPrice) sebagai pemasukan
                        Text(
                            text = "+ Rp ${"%,.0f".format(transaction.sellingPrice ?: 0.0).replace(",", ".")}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50) // Hijau
                        )
                        displayedAmount = true
                        // Tampilkan harga modal (amount) jika ada
                        if (transaction.amount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "(Modal: Rp ${"%,.0f".format(transaction.amount).replace(",", ".")})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    InterpretedDomainType.PURE_INCOME -> { // <<< TAMBAHKAN CABANG INI
                        // Tampilkan 'amount' sebagai pemasukan murni
                        Text(
                            text = "+ Rp ${"%,.0f".format(transaction.amount).replace(",", ".")}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50) // Hijau
                        )
                        displayedAmount = true
                    }
                    InterpretedDomainType.PURE_EXPENSE -> {
                        // Tampilkan 'amount' sebagai pengeluaran
                        Text(
                            text = "- Rp ${"%,.0f".format(transaction.amount).replace(",", ".")}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF44336) // Merah
                        )
                        displayedAmount = true
                    }
                    // Jika enum Anda hanya memiliki 3 nilai ini, else tidak diperlukan.
                    // Jika ada kemungkinan nilai lain (meskipun seharusnya tidak), tambahkan else.
                }

                if (!displayedAmount && transaction.interpretedType !in listOf(InterpretedDomainType.SALE_WITH_COST, InterpretedDomainType.PURE_INCOME, InterpretedDomainType.PURE_EXPENSE)) {
                    // Logika fallback ini mungkin perlu disesuaikan atau dihilangkan
                    // jika semua kasus InterpretedDomainType yang valid sudah menangani displayedAmount
                    Text(
                        text = "Rp 0", // Fallback jika tidak ada nominal yang ditampilkan
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// --- PERUBAHAN --- Preview perlu disesuaikan
@Preview(showBackground = true, widthDp = 360)
@Composable
fun TransactionRowItemDomainPreview() { // Ubah nama preview agar jelas
    XKasTheme {
        // --- PERUBAHAN --- Gunakan model domain Transaction untuk data preview
        val now = Date() // Untuk createdAt, updatedAt, dan date
        val navController = rememberNavController() // <<<--- TAMBAHKAN INI
        Column {
            TransactionRowItem(
                transaction = Transaction(
                    transactionId ="1",
                    userId="user1",
                    description = "Penjualan Barang (Domain Preview)",
                    date = now, // Objek Date
                    type = "income", // String asli dari Firestore
                    amount = 100000.0,      // Modal
                    sellingPrice = 150000.0, // Harga Jual
                    createdAt = now,
                    updatedAt = null,
                    interpretedType = InterpretedDomainType.SALE_WITH_COST // Tipe domain
                ),
                navController = navController // <<<--- TERUSKAN INI
            )
            Spacer(Modifier.height(8.dp))
            TransactionRowItem(
                transaction = Transaction(
                    transactionId ="2",
                    userId="user1",
                    description = "Pembelian ATK (Domain Preview)",
                    date = Date(now.time - 86400000), // Kemarin
                    type = "expense",
                    amount = 75000.0,       // Nominal Pengeluaran
                    sellingPrice = null,
                    createdAt = Date(now.time - 86400000),
                    updatedAt = null,
                    interpretedType = InterpretedDomainType.PURE_EXPENSE
                ),
                navController = navController // <<<--- TERUSKAN INI
            )
            Spacer(Modifier.height(8.dp))
            TransactionRowItem(
                transaction = Transaction(
                    transactionId ="3",
                    userId="user1",
                    description = "Jasa Desain Logo (Domain Preview)",
                    date = Date(now.time - (2 * 86400000)), // 2 hari lalu
                    type = "income",
                    amount = 0.0,           // Modal (mungkin 0 untuk jasa)
                    sellingPrice = 250000.0, // Harga Jual Jasa
                    createdAt = Date(now.time - (2 * 86400000)),
                    updatedAt = null,
                    interpretedType = InterpretedDomainType.SALE_WITH_COST
                ),
                navController = navController // <<<--- TERUSKAN INI
            )
        }
    }
}


@Preview(showBackground = true, device = "spec:width=375dp,height=812dp,dpi=480")
@Composable
fun MainHomeScreenDomainPreview() {
    XKasTheme {
        val now = Date()
        val navController = rememberNavController()
        val dummyDomainTransactions = listOf(
            Transaction(
                transactionId = "1", userId = "user1", description = "Produk A (Domain)",
                date = now, type = "income", amount = 100000.0, sellingPrice = 150000.0,
                createdAt = now, updatedAt = null, interpretedType = InterpretedDomainType.SALE_WITH_COST
            ),
            Transaction(
                transactionId = "2", userId = "user1", description = "Bahan Baku (Domain)",
                date = Date(now.time - 86400000), type = "expense", amount = 75000.0, sellingPrice = null,
                createdAt = Date(now.time - 86400000), updatedAt = null, interpretedType = InterpretedDomainType.PURE_EXPENSE
            ),
            Transaction(
                transactionId = "3", userId = "user1", description = "Konsultasi (Domain)",
                date = Date(now.time - (2 * 86400000)), type = "income", amount = 20000.0, sellingPrice = 200000.0,
                createdAt = Date(now.time - (2 * 86400000)), updatedAt = null, interpretedType = InterpretedDomainType.SALE_WITH_COST
            )
        )

        val totalSalesPreview: Double = dummyDomainTransactions.sumOf { transaction ->
            when (transaction.interpretedType) {
                InterpretedDomainType.SALE_WITH_COST -> transaction.sellingPrice ?: 0.0
                InterpretedDomainType.PURE_INCOME -> transaction.amount
                InterpretedDomainType.PURE_EXPENSE -> 0.0
                // else -> 0.0 // Jika InterpretedDomainType bukan enum yang exhaustive
            }
        }

        val totalCostPreview: Double = dummyDomainTransactions.sumOf { transaction ->
            when (transaction.interpretedType) {
                InterpretedDomainType.SALE_WITH_COST -> transaction.amount
                InterpretedDomainType.PURE_EXPENSE -> transaction.amount
                InterpretedDomainType.PURE_INCOME -> 0.0
                // else -> 0.0 // Jika InterpretedDomainType bukan enum yang exhaustive
            }
        }

        val profitPreview = totalSalesPreview - totalCostPreview

        // --- PERUBAHAN UNTUK PREVIEW ---
        val previewFilterType = DateFilterType.THIS_MONTH // Atau filter lain untuk pengujian
        val dateForPreviewProfitCard = if (dummyDomainTransactions.isNotEmpty() &&
            (previewFilterType == DateFilterType.ALL_TIME || previewFilterType == DateFilterType.THIS_MONTH)) {
            // Untuk preview, jika THIS_MONTH atau ALL_TIME, coba gunakan tanggal transaksi pertama
            dummyDomainTransactions.first().date
        } else {
            // Untuk TODAY, THIS_WEEK, atau jika tidak ada transaksi, gunakan tanggal saat ini
            Date()
        }

        TransactionContent(
            navController = navController,
            profit = profitPreview,
            totalSales = totalSalesPreview,
            totalCost = totalCostPreview,
            transactions = dummyDomainTransactions,
            currentFilterTypeForProfitCard = previewFilterType,
            referenceDateForProfitCard = dateForPreviewProfitCard,
            userDisplayName = "User Preview",
            userPhotoUrl = null
        )
    }
}