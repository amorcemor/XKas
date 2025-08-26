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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.mibi.xkas.R
import com.mibi.xkas.data.InterpretedDomainType
import com.mibi.xkas.data.Transaction
import com.mibi.xkas.ui.components.BusinessUnitSelectionDialog
import com.mibi.xkas.ui.components.AvatarDisplay
import com.mibi.xkas.ui.navigation.Screen
import com.mibi.xkas.ui.profile.ProfileBottomSheet
import com.mibi.xkas.ui.profile.ProfileViewModel
import com.mibi.xkas.ui.theme.XKasTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.widget.Toast // Untuk pesan jika BU belum dipilih
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
    // --- TAMBAH ProfileViewModel ---
    val profileViewModel: ProfileViewModel = viewModel()
    val profileUiState by profileViewModel.uiState.collectAsStateWithLifecycle()

    // State untuk ProfileBottomSheet
    var showProfileBottomSheet by remember { mutableStateOf(false) }

    // --- Load profile data sekali saat komposisi awal ---
    LaunchedEffect(Unit) {
        profileViewModel.refreshProfile()
    }

    // --- PANGGIL SEKALI SAAT KOMPOSISI AWAL ---
    LaunchedEffect(key1 = Unit) {
        val previousRoute = navController.previousBackStackEntry?.destination?.route
        val isFromNavigation = previousRoute != null &&
                previousRoute !in listOf("login", "register", "welcome")

        if (isFromNavigation) {
            homeViewModel.onNavigatedFromOtherMenu()
        } else {
            homeViewModel.triggerInitialBusinessUnitCheck()
        }
    }

    val isCreatingBusinessUnit by homeViewModel.isCreatingBusinessUnit.collectAsStateWithLifecycle()

    // --- State BusinessUnit ---
    val businessUnitUiState by homeViewModel.businessUnitUiState.collectAsStateWithLifecycle()
    val selectedBusinessUnit by homeViewModel.selectedBusinessUnit.collectAsStateWithLifecycle()
    val showBusinessUnitSelectionDialog by homeViewModel.showBusinessUnitSelectionDialog.collectAsStateWithLifecycle()

    val currentFilterType by homeViewModel.currentDateFilterType.collectAsStateWithLifecycle()
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

    // --- Dialog Pemilihan Unit Bisnis ---
    if (showBusinessUnitSelectionDialog) {
        BusinessUnitSelectionDialog(
            businessUnitUiState = businessUnitUiState,
            selectedBusinessUnit = selectedBusinessUnit,
            onDismissRequest = { homeViewModel.onBusinessUnitDialogDismiss() },
            onBusinessUnitSelected = { bu ->
                homeViewModel.setSelectedBusinessUnit(bu)
                homeViewModel.onBusinessUnitDialogDismiss()
            },
            onAddBusinessUnitClicked = {
                homeViewModel.onBusinessUnitDialogDismiss()
                navController.navigate(Screen.AddEditBusinessUnit.route)
            },
            onCreateBusinessUnit = { name, type, description, initialBalance, customTypeName ->
                homeViewModel.createBusinessUnit(name, type, description, initialBalance, customTypeName)
            },
            onDeleteBusinessUnitConfirmed = { businessUnitId ->
                homeViewModel.deleteBusinessUnit(businessUnitId)
            },
            onUpdateBusinessUnitConfirmed = { businessUnitId, newName, newDescription, newType, customTypeName ->
                homeViewModel.updateBusinessUnit(businessUnitId, newName, newDescription, newType, customTypeName)
            },
            isCreatingBusinessUnit = isCreatingBusinessUnit
        )
    }

    // --- ProfileBottomSheet ---
    if (showProfileBottomSheet) {
        ProfileBottomSheet(
            onDismiss = { showProfileBottomSheet = false },
            onEditProfileClicked = {
                navController.navigate(Screen.EditProfile.route)
            },
            onSettingsClicked = {
                showProfileBottomSheet = false
                navController.navigate(Screen.Settings.route)
            },
            onLogoutConfirmed = {
                FirebaseAuth.getInstance().signOut()
                navController.navigate(Screen.Login.route) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
            }
        )
    }

    val context = LocalContext.current

    LaunchedEffect(selectedBusinessUnit) {
        onFabActionReady {
            val buId = selectedBusinessUnit?.businessUnitId
            if (!buId.isNullOrBlank()) {
                navController.navigate(Screen.AddEditTransaction.createRouteForNew(buId))
            } else {
                Toast.makeText(context, "Silahkan pilih Bisnis terlebih dahulu", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp)
                        .clickable(
                            onClick = { homeViewModel.onTopBarBusinessUnitClicked() },
                            enabled = businessUnitUiState !is HomeViewModel.BusinessUnitUiState.Loading
                        )
                        .padding(vertical = 8.dp)
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
                                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                                        fontWeight = FontWeight.SemiBold,
                                        color = topAppBarContentColor,
                                        maxLines = 1
                                    )
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
                                        text = "Tambah Bisnis",
                                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                                        fontWeight = FontWeight.SemiBold,
                                        color = topAppBarContentColor
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        contentDescription = "Tambah Bisnis",
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
                    containerColor = Color.Transparent,
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
                    // --- GANTI PARAMETER PROFIL ---
                    userName = profileUiState.userName,
                    userAvatarType = profileUiState.userAvatarType,
                    userAvatarValue = profileUiState.userAvatarValue,
                    userAvatarColor = profileUiState.userAvatarColor,
                    onProfileClick = { showProfileBottomSheet = true },
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
                            painter = painterResource(id = R.drawable.profit_card_background_decoration),
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
            is TransactionListUiState.FirstTimeEmpty -> {
                Box(modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_rocket_launch),
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
                            painter = painterResource(id = R.drawable.ic_cloud_search),
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
                            painter = painterResource(id = R.drawable.ic_business),
                            contentDescription = "Pilih Bisnis",
                            modifier = Modifier.size(280.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Pilih Bisnis",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Ketuk nama bisnis di atas untuk memilih atau menambah yang baru.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = { homeViewModel.onTopBarBusinessUnitClicked() }) {
                            Text("Pilih atau Tambah Bisnis")
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
                            painter = painterResource(id = R.drawable.ic_rocket_launch),
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
                            homeViewModel.triggerInitialBusinessUnitCheck()
                        }) {
                            Text("Coba Lagi")
                        }
                    }
                }
            }
        }
    }
}

// --- Dialog Functions (unchanged) ---
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
                onConfirm()
            }) {
                Text("Tambah Transaksi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
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
            LazyColumn {
                items(DateFilterType.values()) { filterType ->
                    val isSelected = filterType == currentSelectedType
                    val cardBackgroundColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }

                    val textColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
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
                            },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = cardBackgroundColor
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 0.dp
                        )
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
                                tint = iconColor
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
                                color = textColor,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                            )
                            Spacer(Modifier.weight(1f))
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
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
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
    // --- GANTI PARAMETER PROFIL ---
    userName: String?,
    userAvatarType: String?,
    userAvatarValue: String?,
    userAvatarColor: String?,
    onProfileClick: () -> Unit
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
            onProfileClick = onProfileClick,
            // --- GANTI PARAMETER PROFIL ---
            userName = userName,
            userAvatarType = userAvatarType,
            userAvatarValue = userAvatarValue,
            userAvatarColor = userAvatarColor
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
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 2.dp)
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
    userName: String?,
    userAvatarType: String?,
    userAvatarValue: String?,
    userAvatarColor: String?,
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

            // Profile section - PERBAIKAN DI SINI
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
                    // OPSI 1: Gunakan AvatarDisplay dengan border default
                    AvatarDisplay(
                        avatarType = userAvatarType ?: "initial",
                        avatarValue = userAvatarValue ?: "",
                        avatarColor = userAvatarColor ?: "",
                        userName = userName ?: "User",
                        size = 32.dp,
                        showBorder = false  // matikan border default karena kita buat custom
                    )

                    // ATAU OPSI 2: Jika ingin border putih custom, bungkus dengan Box
                    /*
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .border(
                                width = 2.dp,
                                color = Color.White.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    ) {
                        AvatarDisplay(
                            avatarType = userAvatarType ?: "initial",
                            avatarValue = userAvatarValue ?: "",
                            avatarColor = userAvatarColor ?: "",
                            userName = userName ?: "User",
                            size = 32.dp,
                            showBorder = false
                        )
                    }
                    */

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "Halo,",
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                        Text(
                            text = (userName ?: "User").take(8),
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
    val cardBackgroundColor = Color(0xFF81B2CA)
    val cardTextColor = Color.White
    val accentGreen = Color(0xFF4CAF50)
    val accentRed = Color(0xFFE57373)

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Pemasukan",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = cardTextColor
                            )
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "Pemasukan",
                                tint = accentGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Rp ${"%,.0f".format(totalSales).replace(",", ".")}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = cardTextColor
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingDown,
                                contentDescription = "Pengeluaran",
                                tint = accentRed
                            )
                            Text(
                                "Pengeluaran",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = cardTextColor,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Rp ${"%,.0f".format(totalCost).replace(",", ".")}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = cardTextColor,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionRowItem(
    transaction: Transaction,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
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
                    text = try {
                        displayDateFormat.format(transaction.date)
                    } catch (e: Exception) {
                        "Tanggal tidak valid"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(horizontalAlignment = Alignment.End) {
                var displayedAmount = false
                when (transaction.interpretedType) {
                    InterpretedDomainType.SALE_WITH_COST -> {
                        Text(
                            text = "+ Rp ${"%,.0f".format(transaction.sellingPrice ?: 0.0).replace(",", ".")}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        displayedAmount = true
                        if (transaction.amount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "(Modal: Rp ${"%,.0f".format(transaction.amount).replace(",", ".")})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    InterpretedDomainType.PURE_INCOME -> {
                        Text(
                            text = "+ Rp ${"%,.0f".format(transaction.amount).replace(",", ".")}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        displayedAmount = true
                    }
                    InterpretedDomainType.PURE_EXPENSE -> {
                        Text(
                            text = "- Rp ${"%,.0f".format(transaction.amount).replace(",", ".")}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF44336)
                        )
                        displayedAmount = true
                    }
                }

                if (!displayedAmount && transaction.interpretedType !in listOf(InterpretedDomainType.SALE_WITH_COST, InterpretedDomainType.PURE_INCOME, InterpretedDomainType.PURE_EXPENSE)) {
                    Text(
                        text = "Rp 0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
fun TransactionRowItemDomainPreview() {
    XKasTheme {
        val now = Date()
        val navController = rememberNavController()
        Column {
            TransactionRowItem(
                transaction = Transaction(
                    transactionId ="1",
                    userId="user1",
                    description = "Penjualan Barang (Domain Preview)",
                    date = now,
                    type = "income",
                    amount = 100000.0,
                    sellingPrice = 150000.0,
                    createdAt = now,
                    updatedAt = null,
                    interpretedType = InterpretedDomainType.SALE_WITH_COST
                ),
                navController = navController
            )
            Spacer(Modifier.height(8.dp))
            TransactionRowItem(
                transaction = Transaction(
                    transactionId ="2",
                    userId="user1",
                    description = "Pembelian ATK (Domain Preview)",
                    date = Date(now.time - 86400000),
                    type = "expense",
                    amount = 75000.0,
                    sellingPrice = null,
                    createdAt = Date(now.time - 86400000),
                    updatedAt = null,
                    interpretedType = InterpretedDomainType.PURE_EXPENSE
                ),
                navController = navController
            )
            Spacer(Modifier.height(8.dp))
            TransactionRowItem(
                transaction = Transaction(
                    transactionId ="3",
                    userId="user1",
                    description = "Jasa Desain Logo (Domain Preview)",
                    date = Date(now.time - (2 * 86400000)),
                    type = "income",
                    amount = 0.0,
                    sellingPrice = 250000.0,
                    createdAt = Date(now.time - (2 * 86400000)),
                    updatedAt = null,
                    interpretedType = InterpretedDomainType.SALE_WITH_COST
                ),
                navController = navController
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
            }
        }

        val totalCostPreview: Double = dummyDomainTransactions.sumOf { transaction ->
            when (transaction.interpretedType) {
                InterpretedDomainType.SALE_WITH_COST -> transaction.amount
                InterpretedDomainType.PURE_EXPENSE -> transaction.amount
                InterpretedDomainType.PURE_INCOME -> 0.0
            }
        }

        val profitPreview = totalSalesPreview - totalCostPreview

        val previewFilterType = DateFilterType.THIS_MONTH
        val dateForPreviewProfitCard = if (dummyDomainTransactions.isNotEmpty() &&
            (previewFilterType == DateFilterType.ALL_TIME || previewFilterType == DateFilterType.THIS_MONTH)) {
            dummyDomainTransactions.first().date
        } else {
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
            onProfileClick = { },
            // --- PREVIEW DENGAN DATA PROFIL DUMMY ---
            userName = "John Doe",
            userAvatarType = "initial",
            userAvatarValue = "J",
            userAvatarColor = "#FF6B6B"
        )
    }
}