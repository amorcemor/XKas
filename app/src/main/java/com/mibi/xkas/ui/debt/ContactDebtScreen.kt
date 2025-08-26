package com.mibi.xkas.ui.debt

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mibi.xkas.model.ContactDebtSummary
import com.mibi.xkas.ui.debt.components.CreateDebtDialog
import com.mibi.xkas.ui.debt.components.DebtSummaryCard
import com.mibi.xkas.ui.components.EnhancedContactPickerDialog
import com.mibi.xkas.ui.components.EditContactDialog
import com.mibi.xkas.ui.components.ContactDeleteConfirmDialog
import com.mibi.xkas.ui.components.ContactDialogMode
import com.mibi.xkas.data.model.DebtType
import com.mibi.xkas.data.model.FirestoreContact
import com.mibi.xkas.utils.formatRupiah

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun EnhancedContactDebtScreen(
    viewModel: ContactDebtViewModel = hiltViewModel(),
    onContactClick: (String) -> Unit,
    onNavigateToCreateDebt: () -> Unit = {},
    onFabActionReady: (() -> Unit) -> Unit
) {
    val summaries by viewModel.contactSummaries.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val isLoadingContacts by viewModel.isLoadingContacts.collectAsState()
    val isCreatingContact by viewModel.isCreatingContact.collectAsState()
    val isImportingContacts by viewModel.isImportingContacts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val operationMessage by viewModel.operationMessage.collectAsState()

    // Dialog states
    var showCreateDebtDialog by remember { mutableStateOf(false) }
    var showSelectContactDialog by remember { mutableStateOf(false) }

    // Selected contact states
    var selectedContact by remember { mutableStateOf<FirestoreContact?>(null) }

    // Loading states
    var showOnlyActiveDebts by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Filter summaries
    val filteredSummaries = remember(summaries, searchQuery, showOnlyActiveDebts) {
        val searchFiltered = if (searchQuery.isBlank()) {
            summaries
        } else {
            summaries.filter { summary ->
                summary.contactName.contains(searchQuery, ignoreCase = true) ||
                        summary.contactPhone.contains(searchQuery, ignoreCase = true)
            }
        }
        if (showOnlyActiveDebts) searchFiltered.filter { it.hasActiveDebt } else searchFiltered
    }

    // Stats
    val totalCustomerOwes = remember(summaries) { summaries.sumOf { it.customerOwesAmount } }
    val totalBusinessOwes = remember(summaries) { summaries.sumOf { it.businessOwesAmount } }
    val totalActiveDebtors = remember(summaries) { summaries.count { it.hasActiveDebt } }
    val totalUnpaidTransactions = remember(summaries) {
        summaries.sumOf { summary ->
            summary.debts.count { debt -> debt.totalAmount > debt.paidAmount }
        }
    }

    // Handle snackbar error & operation messages
    LaunchedEffect(error) {
        error?.let { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(operationMessage) {
        operationMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearOperationMessage()
        }
    }

    // Setup FAB
    LaunchedEffect(Unit) {
        onFabActionReady { showCreateDebtDialog = true }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Terjadi kesalahan", color = MaterialTheme.colorScheme.error)
                        Text(error ?: "")
                        Button(onClick = { viewModel.refreshContacts() }) { Text("Coba Lagi") }
                    }
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        // ✅ DebtSummaryCard with contact management
                        DebtSummaryCard(
                            totalCustomerOwes = totalCustomerOwes,
                            totalBusinessOwes = totalBusinessOwes,
                            totalDebtors = totalActiveDebtors,
                            totalDebts = totalUnpaidTransactions
                        )
                    }
                    item {
                        SearchBarWithSimpleToggle(
                            query = searchQuery,
                            onQueryChange = viewModel::updateSearchQuery,
                            showOnlyActiveDebts = showOnlyActiveDebts,
                            onToggleFilter = { showOnlyActiveDebts = it },
                            totalActive = totalActiveDebtors,
                            totalAll = summaries.size,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    if (filteredSummaries.isEmpty() && searchQuery.isNotBlank()) {
                        item { EmptySearchResult(searchQuery, Modifier.padding(16.dp)) }
                    } else if (filteredSummaries.isEmpty()) {
                        item { EmptyDebtList({ showCreateDebtDialog = true }, Modifier.padding(16.dp)) }
                    } else {
                        items(filteredSummaries) { contact ->
                            SimplifiedContactDebtItem(
                                summary = contact,
                                onClick = { onContactClick(contact.contactId) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // ✅ Create Debt Dialog
    if (showCreateDebtDialog) {
        CreateDebtDialog(
            showDialog = true,
            onDismiss = {
                showCreateDebtDialog = false
                selectedContact = null
            },
            onSave = { debtData ->
                viewModel.createDebt(debtData)
                showCreateDebtDialog = false
                selectedContact = null
            },
            initialDebtType = DebtType.GIVE_MONEY,
            selectedContact = selectedContact,
            onSelectContact = { showSelectContactDialog = true },
            isLoading = loading
        )
    }

    // ✅ Contact Picker Dialog (for debt creation)
    if (showSelectContactDialog) {
        EnhancedContactPickerDialog(
            showDialog = true,
            contacts = contacts,
            searchQuery = searchQuery,
            isLoading = isLoadingContacts,
            isCreatingContact = isCreatingContact,
            isImportingContacts = isImportingContacts,
            onContactSelected = { contact ->
                selectedContact = contact
                showSelectContactDialog = false
            },
            onDismiss = { showSelectContactDialog = false },
            onSearchQueryChange = viewModel::updateSearchQuery,
            onCreateNewContact = { name, phone -> viewModel.createContact(name, phone) },
            onImportDeviceContacts = { viewModel.importDeviceContacts() },
            onDeleteContact = { /* Not used in PICKER mode */ },
            onEditContact = { /* Not used in PICKER mode */ },
            dialogMode = ContactDialogMode.PICKER
        )
    }
}

@Composable
private fun SearchBarWithSimpleToggle(
    query: String,
    onQueryChange: (String) -> Unit,
    showOnlyActiveDebts: Boolean,
    onToggleFilter: (Boolean) -> Unit,
    totalActive: Int,
    totalAll: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Search Bar
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Cari kontak...") },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        // Simple Toggle Icon Button with Badge
        Box {
            Surface(
                onClick = { onToggleFilter(!showOnlyActiveDebts) },
                shape = CircleShape,
                color = if (showOnlyActiveDebts)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(56.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (showOnlyActiveDebts)
                            Icons.Default.FilterList
                        else
                            Icons.Default.People,
                        contentDescription = if (showOnlyActiveDebts)
                            "Tampilkan semua kontak ($totalAll)"
                        else
                            "Tampilkan hutang aktif ($totalActive)",
                        tint = if (showOnlyActiveDebts)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Badge indicator
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp),
                shape = CircleShape,
                color = if (showOnlyActiveDebts)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outline
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (showOnlyActiveDebts) "$totalActive" else "$totalAll",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (showOnlyActiveDebts)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.surface
                    )
                }
            }
        }
    }
}

@Composable
private fun SimplifiedContactDebtItem(
    summary: ContactDebtSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customerOwes = summary.customerOwesAmount
    val businessOwes = summary.businessOwesAmount

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (summary.hasActiveDebt) 2.dp else 0.dp
        ),
        shape = if (summary.hasActiveDebt) {
            RoundedCornerShape(12.dp)
        } else {
            RoundedCornerShape(10.dp)
        },
        colors = CardDefaults.cardColors(
            containerColor = when {
                summary.hasActiveDebt -> MaterialTheme.colorScheme.surface
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        ),
        border = if (!summary.hasActiveDebt) {
            BorderStroke(
                0.5.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contact info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = when {
                        !summary.hasActiveDebt -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        summary.netBalance > 0 -> Color(0xFFFF5252).copy(alpha = 0.12f)
                        summary.netBalance < 0 -> Color(0xFF4CAF50).copy(alpha = 0.12f)
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (summary.hasActiveDebt) Icons.Default.Person else Icons.Default.PersonOutline,
                        contentDescription = "Kontak",
                        tint = when {
                            !summary.hasActiveDebt -> MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
                            summary.netBalance > 0 -> Color(0xFFFF5252)
                            summary.netBalance < 0 -> Color(0xFF4CAF50)
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = summary.contactName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (summary.hasActiveDebt) FontWeight.Medium else FontWeight.Normal,
                            color = if (summary.hasActiveDebt)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    )
                    if (summary.contactPhone.isNotBlank()) {
                        Text(
                            text = summary.contactPhone,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (summary.hasActiveDebt) 0.8f else 0.6f
                            )
                        )
                    }

                    val totalTransactions = summary.debts.size
                    val unpaidTransactions = summary.debts.count { it.totalAmount > it.paidAmount }

                    Text(
                        text = if (summary.hasActiveDebt) {
                            "$unpaidTransactions dari $totalTransactions transaksi belum lunas"
                        } else {
                            "$totalTransactions transaksi (semua lunas)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (summary.hasActiveDebt) 0.7f else 0.6f
                        )
                    )
                }
            }

            // Debt status
            when {
                businessOwes > customerOwes -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Piutang",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatRupiah(businessOwes - customerOwes),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color(0xFF4CAF50)
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF4CAF50).copy(alpha = 0.12f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingDown,
                                contentDescription = "Piutang",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                }
                customerOwes > businessOwes -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Hutang",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatRupiah(customerOwes - businessOwes),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color(0xFFFF5252)
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFF5252).copy(alpha = 0.12f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "Hutang",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                }
                else -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Status",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "LUNAS",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Lunas",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySearchResult(
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Tidak ditemukan",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                text = "Tidak ada kontak yang cocok dengan \"$searchQuery\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyDebtList(
    onCreateDebt: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Belum Ada Catatan Hutang",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                text = "Tekan tombol tambah dibawah untuk menulis catatan hutang baru",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}