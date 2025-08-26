package com.mibi.xkas.ui.debt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.mibi.xkas.data.model.FirestoreContact
import com.mibi.xkas.data.model.Debt
import com.mibi.xkas.data.model.DebtPayment
import com.mibi.xkas.data.model.DebtType
import com.mibi.xkas.data.repository.FirestoreContactRepository
import com.mibi.xkas.data.repository.DebtRepository
import com.mibi.xkas.model.ContactDebtSummary
import com.mibi.xkas.model.DebtorType
import com.mibi.xkas.ui.debt.components.CreateDebtData
import com.mibi.xkas.ui.debt.components.PaymentData
import com.mibi.xkas.ui.debt.components.PaymentType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ContactDebtViewModel @Inject constructor(
    private val debtRepository: DebtRepository,
    private val firestoreContactRepository: FirestoreContactRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    // ═══════════════════════════════════════════════════════════════════
    // STATE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ✅ PRIMARY: FirestoreContact sebagai single source of truth
    private val _contacts = MutableStateFlow<List<FirestoreContact>>(emptyList())
    val contacts: StateFlow<List<FirestoreContact>> = _contacts.asStateFlow()

    // ✅ MERGED: ContactDebtSummary dengan FirestoreContact data
    private val _contactSummaries = MutableStateFlow<List<ContactDebtSummary>>(emptyList())
    val contactSummaries: StateFlow<List<ContactDebtSummary>> = _contactSummaries.asStateFlow()

    // Contact operations states
    private val _isLoadingContacts = MutableStateFlow(false)
    val isLoadingContacts: StateFlow<Boolean> = _isLoadingContacts.asStateFlow()

    private val _isCreatingContact = MutableStateFlow(false)
    val isCreatingContact: StateFlow<Boolean> = _isCreatingContact.asStateFlow()

    private val _isImportingContacts = MutableStateFlow(false)
    val isImportingContacts: StateFlow<Boolean> = _isImportingContacts.asStateFlow()

    // General operations
    private val _operationLoading = MutableStateFlow(false)
    val operationLoading: StateFlow<Boolean> = _operationLoading.asStateFlow()

    private val _operationMessage = MutableStateFlow<String?>(null)
    val operationMessage: StateFlow<String?> = _operationMessage.asStateFlow()

    // Search functionality
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredContacts: StateFlow<List<FirestoreContact>> = searchQuery.combine(contacts) { query, contactList ->
        if (query.isBlank()) {
            contactList
        } else {
            contactList.filter { contact ->
                contact.name.contains(query, ignoreCase = true) ||
                        contact.phoneNumber.contains(query) ||
                        contact.normalizedPhone.contains(normalizePhone(query))
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadContactsAndDebts()
    }

    init {
        loadContactsAndDebts()
    }

    // ═══════════════════════════════════════════════════════════════════
    // CORE DATA LOADING
    // ═══════════════════════════════════════════════════════════════════

    /**
     * ✅ UNIFIED: Load contacts dan debts secara bersamaan dengan proper merge
     */
    private fun loadContactsAndDebts() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _error.value = "User belum login"
            _loading.value = false
            return
        }

        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null

                combine(
                    firestoreContactRepository.getUserContacts(userId),
                    debtRepository.getGroupedDebtsByContact(userId)
                ) { contactList, debtSummaries ->
                    Pair(contactList, debtSummaries)
                }.collect { (contactList, debtSummaries) ->
                    _contacts.value = contactList
                    val mergedSummaries = debtSummaries.map { summary ->
                        val contact = contactList.find { it.contactId == summary.contactId }
                        summary.copy(contact = contact)
                    }
                    _contactSummaries.value = mergedSummaries
                    _loading.value = false
                }

            } catch (e: Exception) {
                _error.value = "Gagal memuat data: ${e.message}"
                _loading.value = false
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // REFRESH DATA
    // ═══════════════════════════════════════════════════════════════════

    /**
     * ✅ Public refresh function agar bisa dipanggil dari UI
     */
    fun refreshContacts() {
        loadContactsAndDebts()
    }

    // ═══════════════════════════════════════════════════════════════════
    // CONTACT OPERATIONS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * ✅ ENHANCED: Create contact dengan proper loading state
     */
    fun createContact(name: String, phoneNumber: String) {
        if (name.isBlank()) {
            _operationMessage.value = "Nama kontak tidak boleh kosong"
            return
        }

        viewModelScope.launch {
            _isCreatingContact.value = true
            _operationMessage.value = null

            try {
                val result = firestoreContactRepository.createContact(
                    name = name.trim(),
                    phone = phoneNumber.trim(),
                    checkDuplication = true
                )

                if (result.isSuccess) {
                    val contact = result.getOrNull()!!
                    _operationMessage.value = "Kontak '${contact.name}' berhasil dibuat"
                } else {
                    _operationMessage.value = "Gagal membuat kontak: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _operationMessage.value = "Error membuat kontak: ${e.message}"
            } finally {
                _isCreatingContact.value = false
            }
        }
    }


    /**
     * ✅ ENHANCED: Delete contact dengan proper validation
     */
    fun deleteContact(contact: FirestoreContact) {
        viewModelScope.launch {
            _operationLoading.value = true
            _operationMessage.value = null

            try {
                // Check if contact has active debts
                val contactSummary = getContactSummaryById(contact.contactId)

                if (contactSummary != null && contactSummary.hasActiveDebt) {
                    _operationMessage.value = "Tidak dapat menghapus kontak '${contact.name}' karena masih memiliki hutang aktif"
                    return@launch
                }

                val result = firestoreContactRepository.deleteContact(contact.contactId)

                if (result.isSuccess) {
                    _operationMessage.value = "Kontak '${contact.name}' berhasil dihapus"
                } else {
                    _operationMessage.value = "Gagal menghapus kontak: ${result.exceptionOrNull()?.message}"
                }

            } catch (e: Exception) {
                _operationMessage.value = "Error menghapus kontak: ${e.message}"
            } finally {
                _operationLoading.value = false
            }
        }
    }

    /**
     * ✅ Update contact dengan FirestoreContactRepository
     */
    fun updateContact(contactId: String, newName: String, newPhoneNumber: String) {
        viewModelScope.launch {
            _operationLoading.value = true
            _operationMessage.value = null

            try {
                val result = firestoreContactRepository.getContactById(contactId)

                result.onSuccess { oldContact ->
                    val updated = oldContact.copy(
                        name = newName.trim(),
                        phoneNumber = newPhoneNumber.trim()
                    )

                    val updateResult = firestoreContactRepository.updateContact(updated)

                    if (updateResult.isSuccess) {
                        _operationMessage.value = "Kontak '${updated.name}' berhasil diperbarui"
                    } else {
                        _operationMessage.value = "Gagal memperbarui kontak: ${updateResult.exceptionOrNull()?.message}"
                    }
                }.onFailure { e ->
                    _operationMessage.value = "Gagal mengambil kontak: ${e.message}"
                }
            } catch (e: Exception) {
                _operationMessage.value = "Error update kontak: ${e.message}"
            } finally {
                _operationLoading.value = false
            }
        }
    }

    /**
     * ✅ ENHANCED: Import device contacts dengan progress tracking
     */
    fun importDeviceContacts() {
        viewModelScope.launch {
            _isImportingContacts.value = true
            _operationMessage.value = null

            try {
                val result = firestoreContactRepository.importDeviceContacts()

                if (result.isSuccess) {
                    val importedCount = result.getOrNull() ?: 0
                    _operationMessage.value = when (importedCount) {
                        0 -> "Tidak ada kontak baru yang diimpor"
                        1 -> "1 kontak berhasil diimpor"
                        else -> "$importedCount kontak berhasil diimpor"
                    }
                } else {
                    _operationMessage.value = "Gagal impor kontak: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _operationMessage.value = "Error impor kontak: ${e.message}"
            } finally {
                _isImportingContacts.value = false
            }
        }
    }

    /**
     * ✅ ENHANCED: Find or create contact dengan duplicate handling
     */
    fun findOrCreateContact(name: String, phoneNumber: String): Flow<Result<FirestoreContact>> = flow {
        try {
            val result = firestoreContactRepository.findOrCreateContact(
                name = name.trim(),
                phone = phoneNumber.trim()
            )
            emit(result)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // DEBT OPERATIONS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * ✅ ENHANCED: Create debt dengan FirestoreContact validation
     */
    fun createDebt(debtData: CreateDebtData) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _operationMessage.value = "User tidak terautentikasi"
            return
        }

        // Validation
        when {
            debtData.contact == null || debtData.contactId.isBlank() -> {
                _operationMessage.value = "Silakan pilih kontak terlebih dahulu"
                return
            }
            debtData.amount <= 0 -> {
                _operationMessage.value = "Jumlah harus lebih dari 0"
                return
            }
        }

        viewModelScope.launch {
            _operationLoading.value = true
            try {
                val currentTime = Timestamp.now()
                val transactionTimestamp = debtData.dueDate?.let { Timestamp(it) } ?: currentTime

                // Verify contact exists in our current state
                val contact = _contacts.value.find { it.contactId == debtData.contactId }
                if (contact == null) {
                    _operationMessage.value = "Kontak tidak ditemukan, silakan refresh dan coba lagi"
                    return@launch
                }

                when (debtData.debtType) {
                    DebtType.GIVE_MONEY -> {
                        processNewTransaction(
                            contact = contact,
                            amount = debtData.amount,
                            timestamp = transactionTimestamp,
                            description = debtData.description ?: "Pinjaman kepada ${contact.name}",
                            isGivingMoney = true
                        )
                    }
                    DebtType.RECEIVE_MONEY -> {
                        processNewTransaction(
                            contact = contact,
                            amount = debtData.amount,
                            timestamp = transactionTimestamp,
                            description = debtData.description ?: "Uang diterima dari ${contact.name}",
                            isGivingMoney = false
                        )
                    }
                }
            } catch (e: Exception) {
                _operationMessage.value = "Terjadi kesalahan: ${e.message}"
            } finally {
                _operationLoading.value = false
            }
        }
    }

    /**
     * ✅ ENHANCED: Process payment dengan better error handling
     */
    fun processPayment(contactId: String, paymentData: PaymentData) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _operationMessage.value = "User tidak terautentikasi"
            return
        }

        val amount = paymentData.amount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _operationMessage.value = "Jumlah tidak valid"
            return
        }

        // Verify contact exists
        val contact = _contacts.value.find { it.contactId == contactId }
        if (contact == null) {
            _operationMessage.value = "Kontak tidak ditemukan"
            return
        }

        viewModelScope.launch {
            _operationLoading.value = true
            try {
                val currentTime = Timestamp.now()
                val paymentTimestamp = parseDate(paymentData.date) ?: currentTime

                val summaryResult = debtRepository.getContactSummary(contactId)
                summaryResult.onSuccess { summary ->
                    when (paymentData.paymentType) {
                        PaymentType.RECEIVE -> {
                            processReceivePayment(summary, amount, paymentTimestamp, paymentData.description)
                        }
                        PaymentType.GIVE -> {
                            processGivePayment(summary, amount, paymentTimestamp, paymentData.description)
                        }
                    }
                }.onFailure { exception ->
                    _operationMessage.value = "Gagal mengambil data kontak: ${exception.message}"
                }
            } catch (e: Exception) {
                _operationMessage.value = "Terjadi kesalahan: ${e.message}"
            } finally {
                _operationLoading.value = false
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // PRIVATE HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * ✅ Process new transaction dengan FirestoreContact
     */
    private suspend fun processNewTransaction(
        contact: FirestoreContact,
        amount: Double,
        timestamp: Timestamp,
        description: String,
        isGivingMoney: Boolean
    ) {
        val userId = auth.currentUser?.uid ?: return

        val existingResult = debtRepository.getContactSummary(contact.contactId)

        if (existingResult.isSuccess) {
            val existing = existingResult.getOrNull()!!

            when {
                existing.debtorType == DebtorType.NO_DEBT -> {
                    createNewDebtRecord(contact, amount, timestamp, description, isGivingMoney)
                }

                isGivingMoney && existing.debtorType == DebtorType.BUSINESS_OWES -> {
                    offsetExistingDebt(existing, amount, timestamp, description, isCustomerDebt = true)
                }

                !isGivingMoney && existing.debtorType == DebtorType.CUSTOMER_OWES -> {
                    offsetExistingDebt(existing, amount, timestamp, description, isCustomerDebt = false)
                }

                else -> {
                    createNewDebtRecord(contact, amount, timestamp, description, isGivingMoney)
                }
            }
        } else {
            createNewDebtRecord(contact, amount, timestamp, description, isGivingMoney)
        }
    }

    private suspend fun processReceivePayment(
        summary: ContactDebtSummary,
        amount: Double,
        timestamp: Timestamp,
        description: String
    ) {
        when (summary.debtorType) {
            DebtorType.CUSTOMER_OWES -> {
                if (amount <= summary.absoluteBalance) {
                    addPaymentToMostRecentDebt(summary, amount, timestamp, description, "CUSTOMER_OWES")
                    _operationMessage.value = "Pembayaran ${formatRupiah(amount)} berhasil dicatat"
                } else {
                    val excess = amount - summary.absoluteBalance
                    addPaymentToMostRecentDebt(summary, summary.absoluteBalance, timestamp, description, "CUSTOMER_OWES")

                    val contact = _contacts.value.find { it.contactId == summary.contactId }
                    if (contact != null) {
                        createNewDebtRecord(
                            contact = contact,
                            amount = excess,
                            timestamp = timestamp,
                            description = "Kelebihan bayar - ${description.ifBlank { "pembayaran" }}",
                            isGivingMoney = false
                        )
                    }
                    _operationMessage.value = "Pembayaran berhasil. Kelebihan ${formatRupiah(excess)} dicatat sebagai hutang Anda"
                }
            }

            DebtorType.BUSINESS_OWES -> {
                val contact = _contacts.value.find { it.contactId == summary.contactId }
                if (contact != null) {
                    createNewDebtRecord(
                        contact = contact,
                        amount = amount,
                        timestamp = timestamp,
                        description = description.ifBlank { "Uang diterima tambahan" },
                        isGivingMoney = false
                    )
                }
                _operationMessage.value = "Penerimaan ${formatRupiah(amount)} dicatat sebagai hutang tambahan"
            }

            DebtorType.NO_DEBT -> {
                val contact = _contacts.value.find { it.contactId == summary.contactId }
                if (contact != null) {
                    createNewDebtRecord(
                        contact = contact,
                        amount = amount,
                        timestamp = timestamp,
                        description = description.ifBlank { "Uang diterima" },
                        isGivingMoney = false
                    )
                }
                _operationMessage.value = "Penerimaan ${formatRupiah(amount)} dicatat sebagai hutang baru"
            }
        }
    }

    private suspend fun processGivePayment(
        summary: ContactDebtSummary,
        amount: Double,
        timestamp: Timestamp,
        description: String
    ) {
        when (summary.debtorType) {
            DebtorType.BUSINESS_OWES -> {
                if (amount <= summary.absoluteBalance) {
                    addPaymentToMostRecentDebt(summary, amount, timestamp, description, "BUSINESS_OWES")
                    _operationMessage.value = "Pelunasan ${formatRupiah(amount)} berhasil dicatat"
                } else {
                    val excess = amount - summary.absoluteBalance
                    addPaymentToMostRecentDebt(summary, summary.absoluteBalance, timestamp, description, "BUSINESS_OWES")

                    val contact = _contacts.value.find { it.contactId == summary.contactId }
                    if (contact != null) {
                        createNewDebtRecord(
                            contact = contact,
                            amount = excess,
                            timestamp = timestamp,
                            description = "Kelebihan bayar - ${description.ifBlank { "pelunasan" }}",
                            isGivingMoney = true
                        )
                    }
                    _operationMessage.value = "Pelunasan berhasil. Kelebihan ${formatRupiah(excess)} dicatat sebagai pinjaman"
                }
            }

            DebtorType.CUSTOMER_OWES -> {
                val contact = _contacts.value.find { it.contactId == summary.contactId }
                if (contact != null) {
                    createNewDebtRecord(
                        contact = contact,
                        amount = amount,
                        timestamp = timestamp,
                        description = description.ifBlank { "Pinjaman tambahan" },
                        isGivingMoney = true
                    )
                }
                _operationMessage.value = "Pinjaman ${formatRupiah(amount)} dicatat sebagai hutang tambahan"
            }

            DebtorType.NO_DEBT -> {
                val contact = _contacts.value.find { it.contactId == summary.contactId }
                if (contact != null) {
                    createNewDebtRecord(
                        contact = contact,
                        amount = amount,
                        timestamp = timestamp,
                        description = description.ifBlank { "Pinjaman baru" },
                        isGivingMoney = true
                    )
                }
                _operationMessage.value = "Pinjaman ${formatRupiah(amount)} dicatat sebagai hutang baru"
            }
        }
    }

    /**
     * ✅ Create new debt record menggunakan FirestoreContact
     */
    private suspend fun createNewDebtRecord(
        contact: FirestoreContact,
        amount: Double,
        timestamp: Timestamp,
        description: String,
        isGivingMoney: Boolean
    ) {
        val userId = auth.currentUser?.uid ?: return

        val debt = Debt(
            debtId = "",
            userId = userId,
            contactId = contact.contactId,
            description = description,
            totalAmount = amount,
            paidAmount = 0.0,
            createdAt = timestamp,
            updatedAt = timestamp,
            debtType = "MANUAL",
            debtDirection = if (isGivingMoney) "CUSTOMER_OWES" else "BUSINESS_OWES",
            transactionId = "",
            businessUnitId = ""
        )

        debtRepository.addDebt(debt)
    }

    private suspend fun addPaymentToMostRecentDebt(
        summary: ContactDebtSummary,
        amount: Double,
        timestamp: Timestamp,
        description: String,
        debtDirection: String
    ) {
        val userId = auth.currentUser?.uid ?: return

        val targetDebt = summary.debts
            .filter { it.debtDirection == debtDirection && it.totalAmount > it.paidAmount }
            .maxByOrNull { it.createdAt.seconds }

        if (targetDebt != null) {
            val payment = DebtPayment(
                paymentId = UUID.randomUUID().toString(),
                userId = userId,
                debtId = targetDebt.debtId,
                contactId = summary.contactId,
                amount = amount,
                description = description.ifBlank { "Pembayaran" },
                paidAt = timestamp
            )

            debtRepository.addDebtPayment(payment)
        }
    }

    private suspend fun offsetExistingDebt(
        summary: ContactDebtSummary,
        amount: Double,
        timestamp: Timestamp,
        description: String,
        isCustomerDebt: Boolean
    ) {
        val currentBalance = summary.absoluteBalance

        if (amount <= currentBalance) {
            val targetDirection = if (isCustomerDebt) "BUSINESS_OWES" else "CUSTOMER_OWES"
            addPaymentToMostRecentDebt(summary, amount, timestamp, description, targetDirection)
            _operationMessage.value = "Transaksi ${formatRupiah(amount)} berhasil di-offset dengan hutang existing"
        } else {
            val excess = amount - currentBalance
            val targetDirection = if (isCustomerDebt) "BUSINESS_OWES" else "CUSTOMER_OWES"
            addPaymentToMostRecentDebt(summary, currentBalance, timestamp, description, targetDirection)

            val contact = _contacts.value.find { it.contactId == summary.contactId }
            if (contact != null) {
                createNewDebtRecord(
                    contact = contact,
                    amount = excess,
                    timestamp = timestamp,
                    description = description,
                    isGivingMoney = isCustomerDebt
                )
            }
            _operationMessage.value = "Hutang existing lunas. Sisa ${formatRupiah(excess)} dicatat sebagai hutang baru"
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // SEARCH & UTILITY FUNCTIONS
    // ═══════════════════════════════════════════════════════════════════

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
    }

    private fun normalizePhone(phone: String): String {
        return phone.replace(Regex("[^\\d+]"), "")
            .replace(Regex("^(\\+?62)"), "62")
            .replace(Regex("^0"), "62")
    }

    fun getContactById(contactId: String): FirestoreContact? {
        return _contacts.value.find { it.contactId == contactId }
    }

    fun getContactSummaryById(contactId: String): ContactDebtSummary? {
        return _contactSummaries.value.find { it.contactId == contactId }
    }

    // ═══════════════════════════════════════════════════════════════════
    // DEBT MANAGEMENT FUNCTIONS
    // ═══════════════════════════════════════════════════════════════════

    fun deleteDebt(debtId: String) {
        viewModelScope.launch {
            _operationLoading.value = true
            try {
                val result = debtRepository.deleteDebt(debtId)
                if (result.isSuccess) {
                    _operationMessage.value = "Hutang berhasil dihapus"
                } else {
                    _operationMessage.value = "Gagal menghapus hutang: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _operationMessage.value = "Terjadi kesalahan: ${e.message}"
            } finally {
                _operationLoading.value = false
            }
        }
    }

    fun refreshData() {
        loadContactsAndDebts()
    }


    // ═══════════════════════════════════════════════════════════════════
    // UTILITY FUNCTIONS
    // ═══════════════════════════════════════════════════════════════════

    private fun parseDate(dateString: String): Timestamp? {
        return try {
            if (dateString.isBlank()) return null

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val selectedDate = dateFormat.parse(dateString) ?: return null

            val currentCalendar = Calendar.getInstance()
            val selectedCalendar = Calendar.getInstance().apply {
                time = selectedDate
                set(Calendar.HOUR_OF_DAY, currentCalendar.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, currentCalendar.get(Calendar.MINUTE))
                set(Calendar.SECOND, currentCalendar.get(Calendar.SECOND))
                set(Calendar.MILLISECOND, currentCalendar.get(Calendar.MILLISECOND))
            }

            Timestamp(selectedCalendar.time)
        } catch (e: Exception) {
            null
        }
    }

    private fun formatRupiah(amount: Double): String {
        return "Rp ${String.format("%,.0f", amount)}"
    }

    fun clearOperationMessage() {
        _operationMessage.value = null
    }

    fun clearError() {
        _error.value = null
    }

    // ═══════════════════════════════════════════════════════════════════
    // DEBUG & ANALYTICS FUNCTIONS
    // ═══════════════════════════════════════════════════════════════════

    fun getDebugInfo(): String {
        return buildString {
            appendLine("=== ContactDebtViewModel Debug Info ===")
            appendLine("Contacts loaded: ${_contacts.value.size}")
            appendLine("Contact summaries: ${_contactSummaries.value.size}")
            appendLine("Search query: '${_searchQuery.value}'")
            appendLine("Filtered contacts: ${filteredContacts.value.size}")
            appendLine("Loading states:")
            appendLine("  - Main loading: ${_loading.value}")
            appendLine("  - Contacts loading: ${_isLoadingContacts.value}")
            appendLine("  - Creating contact: ${_isCreatingContact.value}")
            appendLine("  - Importing contacts: ${_isImportingContacts.value}")
            appendLine("  - Operation loading: ${_operationLoading.value}")
            appendLine("Current error: ${_error.value}")
            appendLine("Current operation message: ${_operationMessage.value}")
            appendLine()
            appendLine("Contacts:")
            _contacts.value.forEach { contact ->
                appendLine("  - ${contact.name} (${contact.contactId}) - ${contact.source}")
            }
            appendLine()
            appendLine("Contact Summaries:")
            _contactSummaries.value.forEach { summary ->
                appendLine("  - ${summary.contactName} - Balance: ${formatRupiah(summary.netBalance)} - Type: ${summary.debtorType}")
            }
        }
    }
}