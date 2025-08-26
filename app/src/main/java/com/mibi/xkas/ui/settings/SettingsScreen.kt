package com.mibi.xkas.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mibi.xkas.ui.components.EnhancedContactPickerDialog
import com.mibi.xkas.ui.components.EditContactDialog
import com.mibi.xkas.ui.components.ContactDeleteConfirmDialog
import com.mibi.xkas.ui.components.ContactDialogMode
import com.mibi.xkas.data.model.FirestoreContact

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val operationMessage by viewModel.operationMessage.collectAsState()

    // Dialog states
    var showContactManagementDialog by remember { mutableStateOf(false) }
    var showEditContactDialog by remember { mutableStateOf(false) }
    var showDeleteContactDialog by remember { mutableStateOf(false) }

    // Selected contact states
    var contactToEdit by remember { mutableStateOf<FirestoreContact?>(null) }
    var contactToDelete by remember { mutableStateOf<FirestoreContact?>(null) }

    // Loading states
    var isUpdatingContact by remember { mutableStateOf(false) }
    var isDeletingContact by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Handle snackbar messages
    LaunchedEffect(operationMessage) {
        operationMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearOperationMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color.White
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSectionHeader(title = "Kontak & Data")
            }

            item {
                ContactManagementCard(
                    totalContacts = uiState.totalContacts,
                    manualContacts = uiState.manualContacts,
                    deviceContacts = uiState.deviceContacts,
                    isLoading = uiState.isLoading,
                    onManageContactsClick = { showContactManagementDialog = true }
                )
            }

            item {
                SettingsSectionHeader(title = "Tentang Aplikasi")
            }

            item {
                SettingsMenuItem(
                    icon = Icons.Default.Info,
                    title = "Versi Aplikasi",
                    subtitle = "1.0.0 (Beta)",
                    onClick = { /* TODO: Show version info */ },
                    isDevelopment = true
                )
            }

            item {
                SettingsMenuItem(
                    icon = Icons.Default.Help,
                    title = "Bantuan & Dukungan",
                    subtitle = "FAQ dan kontak dukungan",
                    onClick = { /* TODO: Navigate to help */ },
                    isDevelopment = true
                )
            }

            item {
                SettingsSectionHeader(title = "Lainnya")
            }

            item {
                SettingsMenuItem(
                    icon = Icons.Default.PrivacyTip,
                    title = "Kebijakan Privasi",
                    subtitle = "Pelajari bagaimana kami melindungi data Anda",
                    onClick = { /* TODO: Show privacy policy */ },
                    isDevelopment = true
                )
            }
        }
    }

    // ✅ Contact Management Dialog
    if (showContactManagementDialog) {
        EnhancedContactPickerDialog(
            showDialog = true,
            contacts = contacts,
            searchQuery = searchQuery,
            isLoading = uiState.isLoadingContacts,
            isCreatingContact = uiState.isCreatingContact,
            isImportingContacts = uiState.isImportingContacts,
            onContactSelected = { /* No selection in management mode */ },
            onDismiss = {
                showContactManagementDialog = false
                // Reset search when closing management
                viewModel.updateSearchQuery("")
            },
            onSearchQueryChange = viewModel::updateSearchQuery,
            onCreateNewContact = { name, phone ->
                viewModel.createContact(name, phone)
            },
            onImportDeviceContacts = { viewModel.importDeviceContacts() },
            onEditContact = { contact ->
                contactToEdit = contact
                showEditContactDialog = true
                showContactManagementDialog = false
            },
            onDeleteContact = { contact ->
                contactToDelete = contact
                showDeleteContactDialog = true
                showContactManagementDialog = false
            },
            dialogMode = ContactDialogMode.MANAGEMENT
        )
    }

    // ✅ Edit Contact Dialog
    if (showEditContactDialog) {
        EditContactDialog(
            showDialog = true,
            contact = contactToEdit,
            onDismiss = {
                showEditContactDialog = false
                contactToEdit = null
                // Return to management dialog
                showContactManagementDialog = true
            },
            onUpdate = { contact, editData ->
                isUpdatingContact = true
                // Call ViewModel update method
                viewModel.updateContact(
                    contactId = contact.contactId,
                    newName = editData.name,
                    newPhoneNumber = editData.phoneNumber
                )
                showEditContactDialog = false
                contactToEdit = null
                showContactManagementDialog = true
                isUpdatingContact = false
            },
            isLoading = isUpdatingContact
        )
    }

    // ✅ Delete Contact Confirmation Dialog
    if (showDeleteContactDialog) {
        // Get contact debt summary for validation
        val contactSummary = remember(contactToDelete, uiState.contactSummaries) {
            contactToDelete?.let { contact ->
                uiState.contactSummaries.find { it.contactId == contact.contactId }
            }
        }

        ContactDeleteConfirmDialog(
            showDialog = true,
            contact = contactToDelete,
            contactSummary = contactSummary,
            onDismiss = {
                showDeleteContactDialog = false
                contactToDelete = null
                // Return to management dialog
                showContactManagementDialog = true
            },
            onConfirmDelete = {
                contactToDelete?.let { contact ->
                    isDeletingContact = true
                    viewModel.deleteContact(contact)
                    showDeleteContactDialog = false
                    contactToDelete = null
                    showContactManagementDialog = true
                    isDeletingContact = false
                }
            },
            isDeleting = isDeletingContact
        )
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun ContactManagementCard(
    totalContacts: Int,
    manualContacts: Int,
    deviceContacts: Int,
    isLoading: Boolean,
    onManageContactsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp),
//        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Kelola Kontak",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = "Tekan & tahan beberapa detik untuk edit dan hapus kontak",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            // Contact Statistics
            if (!isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ContactStatItem(
                        label = "Total",
                        value = totalContacts.toString(),
                        icon = Icons.Default.ContactPage,
                        modifier = Modifier.weight(1f)
                    )
                    ContactStatItem(
                        label = "Manual",
                        value = manualContacts.toString(),
                        icon = Icons.Default.Edit,
                        modifier = Modifier.weight(1f)
                    )
                    ContactStatItem(
                        label = "Perangkat",
                        value = deviceContacts.toString(),
                        icon = Icons.Default.PhoneAndroid,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Button(
                onClick = onManageContactsClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.ManageAccounts,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Kelola Kontak",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@Composable
private fun ContactStatItem(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    isDevelopment: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDevelopment) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(12.dp),
        onClick = {
            if (isDevelopment) {
                // TODO: Show development toast or navigate
            } else {
                onClick()
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = if (isDevelopment) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )

                    if (isDevelopment) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Dev",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (isDevelopment) 0.6f else 0.8f
                        )
                    )
                }
            }

            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}