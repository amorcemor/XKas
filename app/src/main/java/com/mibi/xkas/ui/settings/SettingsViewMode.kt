package com.mibi.xkas.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibi.xkas.data.model.FirestoreContact
import com.mibi.xkas.data.repository.FirestoreContactRepository
import com.mibi.xkas.data.repository.DebtRepository
import com.mibi.xkas.model.ContactDebtSummary
import com.mibi.xkas.ui.components.EditContactData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.firebase.auth.FirebaseAuth

data class SettingsUiState(
    val isLoading: Boolean = false,
    val totalContacts: Int = 0,
    val manualContacts: Int = 0,
    val deviceContacts: Int = 0,
    val isLoadingContacts: Boolean = false,
    val isCreatingContact: Boolean = false,
    val isImportingContacts: Boolean = false,
    val contactSummaries: List<ContactDebtSummary> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val contactRepository: FirestoreContactRepository,
    private val debtRepository: DebtRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _operationMessage = MutableStateFlow<String?>(null)
    val operationMessage: StateFlow<String?> = _operationMessage.asStateFlow()

    // ✅ Contacts flow from repository
    val contacts: StateFlow<List<FirestoreContact>> = contactRepository
        .getUserContacts()
        .catch { error ->
            _uiState.update { it.copy(error = error.message) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        observeContacts()
        loadContactSummaries()
    }

    private fun observeContacts() {
        viewModelScope.launch {
            contacts.collect { contactList ->
                val totalContacts = contactList.size
                val manualContacts = contactList.count { it.source == "MANUAL" }
                val deviceContacts = contactList.count { it.source == "DEVICE" }

                _uiState.update { currentState ->
                    currentState.copy(
                        totalContacts = totalContacts,
                        manualContacts = manualContacts,
                        deviceContacts = deviceContacts,
                        isLoading = false,
                        isLoadingContacts = false
                    )
                }
            }
        }
    }

    private fun loadContactSummaries() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    _uiState.update { it.copy(error = "User belum login") }
                    return@launch
                }

                // ✅ Gunakan method yang ada: getGroupedDebtsByContact
                debtRepository.getGroupedDebtsByContact(userId).collect { summaries: List<ContactDebtSummary> ->
                    // ✅ Gabungkan dengan data contact dari repository
                    val enrichedSummaries = summaries.map { summary ->
                        val contact = contacts.value.find { it.contactId == summary.contactId }
                        summary.copy(contact = contact)
                    }

                    _uiState.update { it.copy(contactSummaries = enrichedSummaries) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun createContact(name: String, phone: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingContact = true) }

            try {
                val result = contactRepository.createContact(name = name, phone = phone)
                result.fold(
                    onSuccess = {
                        _operationMessage.value = "Kontak '$name' berhasil dibuat"
                    },
                    onFailure = { error ->
                        _operationMessage.value = "Gagal membuat kontak: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _operationMessage.value = "Gagal membuat kontak: ${e.message}"
            } finally {
                _uiState.update { it.copy(isCreatingContact = false) }
            }
        }
    }

    fun importDeviceContacts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isImportingContacts = true) }

            try {
                val result = contactRepository.importDeviceContacts()
                result.fold(
                    onSuccess = { count ->
                        _operationMessage.value = if (count > 0) {
                            "$count kontak berhasil diimpor dari perangkat"
                        } else {
                            "Tidak ada kontak baru untuk diimpor"
                        }
                    },
                    onFailure = { error ->
                        _operationMessage.value = "Gagal mengimpor kontak: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _operationMessage.value = "Gagal mengimpor kontak: ${e.message}"
            } finally {
                _uiState.update { it.copy(isImportingContacts = false) }
            }
        }
    }

    fun updateContact(contactId: String, newName: String, newPhoneNumber: String) {
        viewModelScope.launch {
            try {
                val result = contactRepository.getContactById(contactId)
                result.fold(
                    onSuccess = { contact ->
                        val updatedContact = contact.copy(
                            name = newName.trim(),
                            phoneNumber = newPhoneNumber.trim(),
                            normalizedPhone = normalizePhone(newPhoneNumber.trim()),
                            updatedAt = com.google.firebase.Timestamp.now()
                        )

                        val updateResult = contactRepository.updateContact(updatedContact)
                        updateResult.fold(
                            onSuccess = {
                                _operationMessage.value = "Kontak '$newName' berhasil diperbarui"
                            },
                            onFailure = { error ->
                                _operationMessage.value = "Gagal memperbarui kontak: ${error.message}"
                            }
                        )
                    },
                    onFailure = { error ->
                        _operationMessage.value = "Gagal mendapatkan data kontak: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _operationMessage.value = "Gagal memperbarui kontak: ${e.message}"
            }
        }
    }

    fun deleteContact(contact: FirestoreContact) {
        viewModelScope.launch {
            try {
                // Check if contact has active debts
                val contactSummary = _uiState.value.contactSummaries.find { it.contactId == contact.contactId }

                if (contactSummary?.hasActiveDebt == true) {
                    _operationMessage.value = "Tidak dapat menghapus kontak '${contact.name}' karena masih memiliki hutang aktif"
                    return@launch
                }

                val result = contactRepository.deleteContact(contact.contactId)
                result.fold(
                    onSuccess = {
                        _operationMessage.value = "Kontak '${contact.name}' berhasil dihapus"
                    },
                    onFailure = { error ->
                        _operationMessage.value = "Gagal menghapus kontak: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _operationMessage.value = "Gagal menghapus kontak: ${e.message}"
            }
        }
    }

    fun clearOperationMessage() {
        _operationMessage.value = null
    }

    private fun normalizePhone(phone: String): String {
        return phone.replace(Regex("[^\\d+]"), "")
            .replace(Regex("^(\\+?62)"), "62")
            .replace(Regex("^0"), "62")
    }
}