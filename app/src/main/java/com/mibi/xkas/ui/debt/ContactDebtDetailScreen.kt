package com.mibi.xkas.ui.debt

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mibi.xkas.data.model.*
import com.mibi.xkas.ui.debt.components.PaymentDialog
import com.mibi.xkas.ui.debt.components.PaymentType
import com.mibi.xkas.utils.formatRupiah
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import com.google.firebase.auth.FirebaseAuth
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material.pullrefresh.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.filled.Payment
import androidx.compose.ui.text.style.TextOverflow

// Data class untuk menggabungkan transaksi dan pembayaran
sealed class TransactionItem {
    abstract val date: Date
    abstract val id: String

    data class DebtTransaction(
        val debt: Debt,
        override val date: Date = debt.createdAt.toDate(),
        override val id: String = debt.debtId
    ) : TransactionItem()

    data class PaymentTransaction(
        val payment: DebtPayment,
        override val date: Date = payment.paidAt.toDate(),
        override val id: String = payment.paymentId
    ) : TransactionItem()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, ExperimentalMaterialApi::class)
@Composable
fun EnhancedContactDebtDetailScreen(
    contactId: String,
    viewModel: ContactDebtDetailViewModel = hiltViewModel(),
    mainViewModel: ContactDebtViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onEditDebt: (String) -> Unit = {},
    onDeleteDebt: (String) -> Unit = {},
    onFabActionReady: (() -> Unit) -> Unit

) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    if (currentUserId == null) {
        Text("User belum login")
        return
    }

    // ✅ State Collection
    val summary by viewModel.contactSummary.collectAsState()
    val error by viewModel.error.collectAsState()
    val operationMessage by viewModel.operationMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val mainOperationMessage by mainViewModel.operationMessage.collectAsState()

    // ✅ UI States
    var showCreateDebtDialog by remember { mutableStateOf(false) }

    var showPaymentDialog by remember { mutableStateOf(false) }
    var currentPaymentType by remember { mutableStateOf(PaymentType.RECEIVE) }
    var fabExpanded by remember { mutableStateOf(false) }
    var showPayOffDialog by remember { mutableStateOf(false) }

    // ✅ PULL TO REFRESH STATE
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = {
            viewModel.refreshSummary(contactId, currentUserId)
        }
    )

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        onFabActionReady {
            fabExpanded = !fabExpanded
        }
    }


    // ✅ Load data saat pertama kali dan ketika contactId berubah
    LaunchedEffect(contactId, currentUserId) {
        viewModel.loadSummary(contactId, currentUserId)
    }

    // ✅ Handle operation messages
    LaunchedEffect(mainOperationMessage) {
        mainOperationMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            mainViewModel.clearOperationMessage()
            // ✅ Refresh data setelah operasi dari main view model
            viewModel.refreshSummary(contactId, currentUserId)
        }
    }

    LaunchedEffect(operationMessage) {
        operationMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearOperationMessage()
        }
    }

    LaunchedEffect(error) {
        error?.let { errorMsg ->
            snackbarHostState.showSnackbar("Error: $errorMsg")
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("Detail Kontak")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    // ✅ Manual Refresh Button
                    IconButton(
                        onClick = {
                            viewModel.refreshSummary(contactId, currentUserId)
                        },
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        // ✅ FAB di tengah dengan posisi lebih ke bawah
        floatingActionButton = {
            CenteredAnimatedMultiFab(
                expanded = fabExpanded,
                onExpandedChange = { fabExpanded = it },
                onReceiveMoney = {
                    currentPaymentType = PaymentType.RECEIVE
                    showPaymentDialog = true
                    fabExpanded = false
                },
                onGiveMoney = {
                    currentPaymentType = PaymentType.GIVE
                    showPaymentDialog = true
                    fabExpanded = false
                }
            )
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullRefreshState) // ✅ Pull to refresh
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                summary?.let { contactSummary ->
                    // Prepare combined transaction list
                    val combinedTransactions = remember(contactSummary) {
                        val debtTransactions = contactSummary.debts.map {
                            TransactionItem.DebtTransaction(it)
                        }
                        val paymentTransactions = contactSummary.payments.map {
                            TransactionItem.PaymentTransaction(it)
                        }

                        (debtTransactions + paymentTransactions)
                            .sortedByDescending { it.date }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp), // ✅ Tambah padding untuk FAB yang lebih rendah
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // ✅ Updated Contact Header dengan logika baru
                        val contact = mainViewModel.getContactById(contactId)
                        item {
                            SimplifiedModernContactHeader(
                                summary = contactSummary,
                                onPayOff = { viewModel.payOffAllDebt(contactSummary.contactId, currentUserId) },
                                contactName = contact?.name ?: contactSummary.contactName ?: "Tanpa Nama",
                                contactPhone = contact?.phoneNumber ?: contactSummary.contactPhone.orEmpty()
                            )
                        }

                        // Section Title
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Riwayat Hutang Piutang",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // ✅ Manual refresh button in section header
                                TextButton(
                                    onClick = {
                                        viewModel.refreshSummary(contactId, currentUserId)
                                    },
                                    enabled = !isLoading
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = "Refresh",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Refresh")
                                    }
                                }
                            }
                        }

                        if (combinedTransactions.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = "Belum Ada Transaksi",
                                    subtitle = "Belum ada catatan hutang atau pembayaran untuk kontak ini",
                                    icon = Icons.Default.Receipt,
                                    onRefresh = {
                                        viewModel.refreshSummary(contactId, currentUserId)
                                    }
                                )
                            }
                        } else {
                            items(
                                combinedTransactions,
                                key = { "${it.javaClass.simpleName}_${it.id}" }) { transaction ->
                                when (transaction) {
                                    is TransactionItem.DebtTransaction -> {
                                        SimplifiedDebtTransactionCard(
                                            debt = transaction.debt,
                                            onEdit = { onEditDebt(transaction.debt.debtId) },
                                            onDelete = {
                                                viewModel.deleteDebt(transaction.debt.debtId)
                                            },
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }

                                    is TransactionItem.PaymentTransaction -> {
                                        SimplifiedPaymentHistoryCard(
                                            payment = transaction.payment,
                                            onEdit = {
                                                viewModel.editPayment(
                                                    paymentId = transaction.payment.paymentId,
                                                    newAmount = transaction.payment.amount, // placeholder
                                                    newDescription = transaction.payment.description // placeholder
                                                )
                                            },
                                            onDelete = {
                                                viewModel.deletePayment(
                                                    paymentId = transaction.payment.paymentId,
                                                    debtId = transaction.payment.debtId,
                                                    amount = transaction.payment.amount
                                                )
                                            },
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } ?: run {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        error?.let { errorMsg ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Terjadi Kesalahan",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = errorMsg,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Button(
                                    onClick = {
                                        viewModel.refreshSummary(contactId, currentUserId)
                                    }
                                ) {
                                    Text("Coba Lagi")
                                }
                            }
                        } ?: CircularProgressIndicator()
                    }
                }
            }

            // ✅ Pull to refresh indicator
            PullRefreshIndicator(
                refreshing = isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    // Payment Dialog
    PaymentDialog(
        showDialog = showPaymentDialog,
        contactName = run {
            val contact = mainViewModel.getContactById(contactId)
            contact?.name ?: summary?.contactName ?: "Tanpa Nama"
        },
        paymentType = currentPaymentType,
        currentDebtAmount = summary?.totalOutstanding ?: 0.0,
        onDismiss = { showPaymentDialog = false },
        onSave = { paymentData ->
            mainViewModel.processPayment(contactId, paymentData)
            showPaymentDialog = false
        },
        isLoading = false
    )

    // Pay Off Dialog
    if (showPayOffDialog) {
        SimplifiedPayOffConfirmationDialog(
            contactName = summary?.contactName ?: "",
            summary = summary!!, // pastikan summary tidak null
            onConfirm = {
                viewModel.payOffAllDebt(contactId, currentUserId)
                showPayOffDialog = false
            },
            onDismiss = { showPayOffDialog = false }
        )
    }
}

// ✅ UPDATED HEADER dengan logika baru
@Composable
private fun SimplifiedModernContactHeader(
    summary: com.mibi.xkas.model.ContactDebtSummary,
    onPayOff: () -> Unit,
    contactName: String,
    contactPhone: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Contact Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = contactName,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        if (contactPhone.isNotBlank()) {
                            Text(
                                text = contactPhone,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Status Indicator - berdasarkan debtorType
                    Surface(
                        shape = CircleShape,
                        color = when (summary.debtorType) {
                            com.mibi.xkas.model.DebtorType.CUSTOMER_OWES -> Color.Red.copy(alpha = 0.8f)
                            com.mibi.xkas.model.DebtorType.BUSINESS_OWES -> Color.Green.copy(alpha = 0.3f)
                            com.mibi.xkas.model.DebtorType.NO_DEBT -> Color.Green.copy(alpha = 0.8f)
                        }
                    ) {
                        Icon(
                            imageVector = when (summary.debtorType) {
                                com.mibi.xkas.model.DebtorType.CUSTOMER_OWES -> Icons.Default.Schedule
                                com.mibi.xkas.model.DebtorType.BUSINESS_OWES -> Icons.Default.Schedule
                                com.mibi.xkas.model.DebtorType.NO_DEBT -> Icons.Default.CheckCircle
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // ✅ SALDO NETO CARD - hanya 1 tampilan berdasarkan yang dominan
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = summary.debtorLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = if (summary.debtorType == com.mibi.xkas.model.DebtorType.NO_DEBT)
                                    "Rp 0" else formatRupiah(summary.absoluteBalance),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }

                        // Tombol Lunasi - hanya tampil jika ada hutang
                        if (summary.hasActiveDebt) {
                            Button(
                                onClick = onPayOff,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    "LUNASI",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ✅ CENTERED ANIMATED MULTI FAB - FAB di tengah dengan bentuk bulat
@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun CenteredAnimatedMultiFab(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onReceiveMoney: () -> Unit,
    onGiveMoney: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300)
            ) + fadeOut()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.offset(y = 30.dp) // 🔥 geser ke bawah 20dp
            ) {
                SubFab(
                    color = Color(0xFF4CAF50),
                    icon = Icons.Default.GetApp,
                    label = "Terima Uang",
                    onClick = onReceiveMoney
                )

                SubFab(
                    color = Color(0xFFFF9800),
                    icon = Icons.Default.Publish,
                    label = "Berikan Uang",
                    onClick = onGiveMoney
                )
            }
        }

        FloatingActionButton(
            onClick = { onExpandedChange(!expanded) },
            containerColor = Color.Transparent, // 🔥 FAB jadi transparan
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp),
            modifier = Modifier
                .size(52.dp)
                .offset(y = 28.dp)
        ) {
            AnimatedContent(
                targetState = expanded,
                transitionSpec = {
                    (slideInVertically { height -> height } + fadeIn()) with
                            (slideOutVertically { height -> -height } + fadeOut())
                },
                label = ""
            ) { isExpanded ->
                Icon(
                    imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = if (isExpanded) "Tutup" else "Tambah",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun SubFab(
    color: Color,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = color,
            contentColor = Color.White,
            modifier = Modifier.size(48.dp),
            shape = CircleShape
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
        }

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                color = Color.White
            ),
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}


// ✅ KOMPONEN LAINNYA TETAP SAMA
@Composable
private fun EmptyStateCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = onRefresh,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Refresh")
            }
        }
    }
}

// Perbaikan untuk SimplifiedDebtTransactionCard dan SimplifiedPaymentHistoryCard
// dengan desain yang konsisten, simpel, dan elegan

// Perbaikan untuk SimplifiedDebtTransactionCard dan SimplifiedPaymentHistoryCard
// dengan desain yang konsisten, simpel, dan elegan

@Composable
private fun SimplifiedDebtTransactionCard(
    debt: Debt,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bagian kiri (icon & teks)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (debt.debtDirection == "CUSTOMER_OWES")
                        Color(0xFFF44336).copy(alpha = 0.1f)
                    else
                        Color(0xFF2196F3).copy(alpha = 0.1f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (debt.debtDirection == "CUSTOMER_OWES")
                            Icons.Default.TrendingUp
                        else
                            Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (debt.debtDirection == "CUSTOMER_OWES")
                            Color(0xFFF44336)
                        else
                            Color(0xFF2196F3),
                        modifier = Modifier.padding(6.dp)
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = debt.description.ifBlank {
                            if (debt.debtDirection == "CUSTOMER_OWES")
                                "Pinjaman Diberikan"
                            else
                                "Uang Diterima"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = SimpleDateFormat("dd MMM yyy • HH:mm", Locale.getDefault())
                                .format(debt.createdAt.toDate()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = formatRupiah(debt.totalAmount),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (debt.debtDirection == "CUSTOMER_OWES")
                                Color(0xFFF44336)
                            else
                                Color(0xFF2196F3)
                        )
                    }
                }
            }

            // Bagian kanan (icon edit & delete)
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}


@Composable
private fun SimplifiedPaymentHistoryCard(
    payment: DebtPayment,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bagian kiri (icon & teks)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.padding(6.dp)
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = if (payment.description.isNotBlank())
                            payment.description
                        else
                            "Pembayaran",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = SimpleDateFormat("dd MMM yyy • HH:mm", Locale.getDefault())
                                .format(payment.paidAt.toDate()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )

                        Text(
                            text = formatRupiah(payment.amount),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }

            // Bagian kanan (status indicator)
            Surface(
                shape = CircleShape,
                color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Pembayaran berhasil",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}


@Composable
private fun SimplifiedPayOffConfirmationDialog(
    contactName: String,
    summary: com.mibi.xkas.model.ContactDebtSummary,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Konfirmasi Pelunasan",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Apakah Anda yakin ingin melunasi:")
                Text(
                    text = summary.debtorLabel,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("untuk:")
                Text(
                    text = contactName,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("Jumlah yang akan dibayar:")
                Text(
                    text = formatRupiah(summary.absoluteBalance),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = when (summary.debtorType) {
                        com.mibi.xkas.model.DebtorType.CUSTOMER_OWES -> MaterialTheme.colorScheme.error
                        com.mibi.xkas.model.DebtorType.BUSINESS_OWES -> MaterialTheme.colorScheme.primary
                        com.mibi.xkas.model.DebtorType.NO_DEBT -> MaterialTheme.colorScheme.secondary
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Ya, Lunasi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun FinancialSummaryItem(
    label: String,
    amount: Double,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f)
        )
        Text(
            text = formatRupiah(amount),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        )
    }
}