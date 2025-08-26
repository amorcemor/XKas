package com.mibi.xkas.ui.addedit

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.mibi.xkas.data.InterpretedDomainType
import com.mibi.xkas.data.Transaction
import com.mibi.xkas.data.model.Debt
import com.mibi.xkas.data.repository.DebtRepository
import com.mibi.xkas.data.repository.TransactionRepository
import com.mibi.xkas.data.repository.FirestoreContactRepository
import com.mibi.xkas.data.model.FirestoreContact
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import com.mibi.xkas.ui.navigation.Screen
import com.mibi.xkas.model.ContactDebtSummary
import android.content.SharedPreferences

sealed class SaveTransactionUiState {
    object Idle : SaveTransactionUiState()
    object Loading : SaveTransactionUiState()
    data class Success(val isEditMode: Boolean) : SaveTransactionUiState()
    data class Error(val message: String) : SaveTransactionUiState()
}

@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val debtRepository: DebtRepository,
    private val firestoreContactRepository: FirestoreContactRepository,
    private val savedStateHandle: SavedStateHandle,
    private val firebaseAuth: FirebaseAuth,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    val transactionId: String? = savedStateHandle[Screen.AddEditTransaction.ARG_TRANSACTION_ID]
    val businessUnitId: String? = savedStateHandle[Screen.AddEditTransaction.ARG_BUSINESS_UNIT_ID]
    val isEditMode: Boolean = transactionId != null

    private val _saveTransactionUiState = MutableStateFlow<SaveTransactionUiState>(SaveTransactionUiState.Idle)
    val saveTransactionUiState: StateFlow<SaveTransactionUiState> = _saveTransactionUiState.asStateFlow()

    private var editingTransactionBusinessUnitId: String? = null

    // UI State
    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _sellingPrice = MutableStateFlow("")
    val sellingPrice: StateFlow<String> = _sellingPrice.asStateFlow()

    private val _isDebt = MutableStateFlow(false)
    val isDebt: StateFlow<Boolean> = _isDebt.asStateFlow()

    // ✅ Updated to FirestoreContact
    private val _selectedContact = MutableStateFlow<FirestoreContact?>(null)
    val selectedContact: StateFlow<FirestoreContact?> = _selectedContact.asStateFlow()

    // Contact picker states
    private val _showContactPicker = MutableStateFlow(false)
    val showContactPicker: StateFlow<Boolean> = _showContactPicker.asStateFlow()

    private val _contacts = MutableStateFlow<List<FirestoreContact>>(emptyList())
    val contacts: StateFlow<List<FirestoreContact>> = _contacts.asStateFlow()

    private val _contactSearchQuery = MutableStateFlow("")
    val contactSearchQuery: StateFlow<String> = _contactSearchQuery.asStateFlow()

    private val _isLoadingContacts = MutableStateFlow(false)
    val isLoadingContacts: StateFlow<Boolean> = _isLoadingContacts.asStateFlow()

    // ✅ NEW: Contact creation states
    private val _isCreatingContact = MutableStateFlow(false)
    val isCreatingContact: StateFlow<Boolean> = _isCreatingContact.asStateFlow()

    private val _isImportingContacts = MutableStateFlow(false)
    val isImportingContacts: StateFlow<Boolean> = _isImportingContacts.asStateFlow()

    private val PREF_KEY_DEVICE_CONTACTS_IMPORTED = "device_contacts_imported_"

    private val uiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val debugDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    private val _transactionDate = MutableStateFlow(uiDateFormat.format(Date()))
    val transactionDate: StateFlow<String> = _transactionDate.asStateFlow()

    private val _selectedUITransactionType = MutableStateFlow(TransactionType.PEMASUKAN)
    val selectedUITransactionType: StateFlow<TransactionType> = _selectedUITransactionType.asStateFlow()

    private val numberFormatter = DecimalFormat("#.##")

    init {
        if (isEditMode) {
            loadTransactionDetails(transactionId!!)
        } else if (businessUnitId == null) {
            _saveTransactionUiState.value = SaveTransactionUiState.Error("ID Unit Bisnis tidak ditemukan.")
        }
    }

    private fun formatDoubleToString(value: Double?): String =
        value?.let { if (it % 1.0 == 0.0) it.toLong().toString() else numberFormatter.format(it) } ?: ""

    private fun loadTransactionDetails(id: String) {
        viewModelScope.launch {
            transactionRepository.getTransactionById(id).firstOrNull()?.let { transaction ->
                editingTransactionBusinessUnitId = transaction.businessUnitId
                _description.value = transaction.description
                if (transaction.type.equals("income", true)) {
                    _selectedUITransactionType.value = TransactionType.PEMASUKAN
                    _sellingPrice.value = formatDoubleToString(transaction.sellingPrice)
                    _amount.value = formatDoubleToString(transaction.amount)
                } else {
                    _selectedUITransactionType.value = TransactionType.PENGELUARAN
                    _amount.value = formatDoubleToString(transaction.amount)
                }
                _transactionDate.value = uiDateFormat.format(transaction.date)
                loadDebtDataForTransaction(id)
            } ?: run {
                _saveTransactionUiState.value = SaveTransactionUiState.Error("Gagal memuat detail transaksi.")
            }
        }
    }

    /**
     * ✅ Updated: Load debt data dengan FirestoreContact
     */
    private fun loadDebtDataForTransaction(transactionId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = firebaseAuth.currentUser?.uid ?: return@launch
                val debt = debtRepository.getDebtByTransactionId(transactionId, currentUserId)
                if (debt == null) {
                    _isDebt.value = false
                    _selectedContact.value = null
                    return@launch
                }

                // Try to get contact by ID
                val contactResult = firestoreContactRepository.getContactById(debt.contactId)
                if (contactResult.isSuccess) {
                    _selectedContact.value = contactResult.getOrNull()
                    _isDebt.value = true
                } else {
                    Log.w("AddEditVM", "Contact not found for debt: ${debt.contactId}")
                    _isDebt.value = false
                    _selectedContact.value = null
                }
            } catch (e: Exception) {
                Log.e("AddEditVM", "Error loading debt data", e)
                _isDebt.value = false
                _selectedContact.value = null
            }
        }
    }

    // ===================================================================
    // ✅ UPDATED: Contact Management Functions
    // ===================================================================

    fun onContactSearchQueryChange(query: String) {
        _contactSearchQuery.value = query
        loadContactsWithSearch(query)
    }

    fun showContactPicker() {
        _showContactPicker.value = true
        loadContactsWithAutoImport()
    }

    fun hideContactPicker() {
        _showContactPicker.value = false
        _contactSearchQuery.value = ""
    }

    fun selectContact(contact: FirestoreContact) {
        _selectedContact.value = contact
        hideContactPicker()
    }

    fun onDebtCheckedChange(checked: Boolean) {
        _isDebt.value = checked
        if (!checked) {
            _selectedContact.value = null
        }
    }

    /**
     * ✅ NEW: Load contacts dengan auto-import strategy
     */
    private fun loadContactsWithAutoImport() {
        viewModelScope.launch {
            _isLoadingContacts.value = true

            try {
                val currentUserId = firebaseAuth.currentUser?.uid ?: return@launch

                // 1. Load Firestore contacts first (instant)
                firestoreContactRepository.getUserContacts(currentUserId)
                    .collect { firestoreContacts ->
                        _contacts.value = firestoreContacts

                        // 2. Check if device import needed (once per user)
                        val importKey = PREF_KEY_DEVICE_CONTACTS_IMPORTED + currentUserId
                        val hasImported = sharedPreferences.getBoolean(importKey, false)

                        if (!hasImported && !_isImportingContacts.value) {
                            // 3. Auto-import in background
                            importDeviceContactsInBackground(currentUserId, importKey)
                        } else {
                            _isLoadingContacts.value = false
                        }
                    }

            } catch (e: Exception) {
                Log.e("AddEditVM", "Error loading contacts", e)
                _isLoadingContacts.value = false
            }
        }
    }

    /**
     * ✅ NEW: Background import with retry mechanism
     */
    private fun importDeviceContactsInBackground(userId: String, importKey: String) {
        viewModelScope.launch {
            _isImportingContacts.value = true

            try {
                val result = firestoreContactRepository.importDeviceContacts(userId)

                if (result.isSuccess) {
                    val importedCount = result.getOrNull() ?: 0
                    Log.i("AddEditVM", "Auto-imported $importedCount device contacts")

                    // Mark as imported
                    sharedPreferences.edit()
                        .putBoolean(importKey, true)
                        .apply()
                } else {
                    Log.w("AddEditVM", "Auto-import failed: ${result.exceptionOrNull()?.message}")
                    // Don't mark as imported, will retry next time
                }

            } catch (e: Exception) {
                Log.e("AddEditVM", "Error during auto-import", e)
                // Don't mark as imported, will retry next time
            } finally {
                _isImportingContacts.value = false
                _isLoadingContacts.value = false
            }
        }
    }

    /**
     * ✅ NEW: Load contacts with search filter
     */
    private fun loadContactsWithSearch(query: String) {
        viewModelScope.launch {
            try {
                val currentUserId = firebaseAuth.currentUser?.uid ?: return@launch

                firestoreContactRepository.searchContacts(currentUserId, query)
                    .collect { filteredContacts ->
                        _contacts.value = filteredContacts
                    }

            } catch (e: Exception) {
                Log.e("AddEditVM", "Error searching contacts", e)
            }
        }
    }

    /**
     * ✅ NEW: Manual retry import
     */
    fun onImportDeviceContacts() {
        viewModelScope.launch {
            _isImportingContacts.value = true

            try {
                val currentUserId = firebaseAuth.currentUser?.uid ?: return@launch
                val result = firestoreContactRepository.importDeviceContacts(currentUserId)

                if (result.isSuccess) {
                    val importedCount = result.getOrNull() ?: 0
                    Log.i("AddEditVM", "Manually imported $importedCount device contacts")

                    // Mark as imported
                    val importKey = PREF_KEY_DEVICE_CONTACTS_IMPORTED + currentUserId
                    sharedPreferences.edit()
                        .putBoolean(importKey, true)
                        .apply()
                } else {
                    Log.e("AddEditVM", "Manual import failed: ${result.exceptionOrNull()?.message}")
                }

            } catch (e: Exception) {
                Log.e("AddEditVM", "Error during manual import", e)
            } finally {
                _isImportingContacts.value = false
            }
        }
    }

    /**
     * ✅ NEW: Create new contact
     */
    fun onCreateNewContact(name: String, phoneNumber: String) {
        viewModelScope.launch {
            _isCreatingContact.value = true

            try {
                val currentUserId = firebaseAuth.currentUser?.uid ?: return@launch
                val result = firestoreContactRepository.createContact(
                    userId = currentUserId,
                    name = name,
                    phone = phoneNumber,
                    checkDuplication = true
                )

                if (result.isSuccess) {
                    val newContact = result.getOrNull()
                    if (newContact != null) {
                        Log.i("AddEditVM", "Created new contact: ${newContact.name}")
                        selectContact(newContact)
                    }
                } else {
                    Log.e("AddEditVM", "Failed to create contact: ${result.exceptionOrNull()?.message}")
                }

            } catch (e: Exception) {
                Log.e("AddEditVM", "Error creating contact", e)
            } finally {
                _isCreatingContact.value = false
            }
        }
    }

    // ===================================================================
    // EXISTING FUNCTIONS (unchanged)
    // ===================================================================

    fun onDescriptionChange(value: String) { _description.value = value }
    fun onAmountChange(value: String) { _amount.value = value }
    fun onSellingPriceChange(value: String) { _sellingPrice.value = value }
    fun onDateChange(value: String) { _transactionDate.value = value }
    fun onTransactionTypeChanged(type: TransactionType) { _selectedUITransactionType.value = type }

    fun attemptSaveOrUpdateTransaction() {
        val currentDescription = _description.value
        val currentAmountStr = _amount.value
        val currentTransactionInputType = _selectedUITransactionType.value
        val currentSellingPriceStr = if (currentTransactionInputType == TransactionType.PEMASUKAN) _sellingPrice.value else null
        val currentDateStr = _transactionDate.value

        viewModelScope.launch {
            _saveTransactionUiState.value = SaveTransactionUiState.Loading

            val currentUserId = firebaseAuth.currentUser?.uid
            if (currentUserId.isNullOrBlank()) {
                _saveTransactionUiState.value = SaveTransactionUiState.Error("Pengguna tidak terautentikasi.")
                return@launch
            }

            // Validasi input
            if (currentDescription.isBlank()) {
                _saveTransactionUiState.value = SaveTransactionUiState.Error("Deskripsi tidak boleh kosong.")
                return@launch
            }
            if (currentDateStr.isBlank()) {
                _saveTransactionUiState.value = SaveTransactionUiState.Error("Tanggal tidak boleh kosong.")
                return@launch
            }

            val parsedDateOnly: Date? = try {
                uiDateFormat.parse(currentDateStr)
            } catch (e: Exception) {
                Log.e("AddTransactionVM", "Error parsing date string: $currentDateStr", e)
                _saveTransactionUiState.value = SaveTransactionUiState.Error("Format tanggal tidak valid.")
                return@launch
            }

            if (parsedDateOnly == null) {
                _saveTransactionUiState.value = SaveTransactionUiState.Error("Tanggal tidak dapat diproses.")
                return@launch
            }

            val calendarForSelectedDate = Calendar.getInstance()
            calendarForSelectedDate.time = parsedDateOnly
            val currentTimeCalendar = Calendar.getInstance()
            calendarForSelectedDate.set(Calendar.HOUR_OF_DAY, currentTimeCalendar.get(Calendar.HOUR_OF_DAY))
            calendarForSelectedDate.set(Calendar.MINUTE, currentTimeCalendar.get(Calendar.MINUTE))
            calendarForSelectedDate.set(Calendar.SECOND, currentTimeCalendar.get(Calendar.SECOND))
            calendarForSelectedDate.set(Calendar.MILLISECOND, currentTimeCalendar.get(Calendar.MILLISECOND))
            val finalTransactionDateTime: Date = calendarForSelectedDate.time

            val domainInterpretedType: InterpretedDomainType
            val domainAmountForFirestore: Double
            val domainSellingPriceForFirestore: Double?
            val firestoreTypeString: String

            // Logika penentuan Pemasukan/Pengeluaran
            if (currentTransactionInputType == TransactionType.PEMASUKAN) {
                if (currentSellingPriceStr.isNullOrBlank() || currentAmountStr.isBlank()) {
                    _saveTransactionUiState.value = SaveTransactionUiState.Error("Harga Jual dan Harga Modal tidak boleh kosong untuk Pemasukan.")
                    return@launch
                }
                val costPrice = currentAmountStr.toDoubleOrNull()
                val sellingPriceValue = currentSellingPriceStr.toDoubleOrNull()

                if (costPrice == null || costPrice <= 0) {
                    _saveTransactionUiState.value = SaveTransactionUiState.Error("Harga Modal tidak valid atau harus lebih dari 0.")
                    return@launch
                }
                if (sellingPriceValue == null || sellingPriceValue <= 0) {
                    _saveTransactionUiState.value = SaveTransactionUiState.Error("Harga Jual tidak valid atau harus lebih dari 0.")
                    return@launch
                }

                domainInterpretedType = InterpretedDomainType.SALE_WITH_COST
                domainAmountForFirestore = costPrice
                domainSellingPriceForFirestore = sellingPriceValue
                firestoreTypeString = "income"
            } else {
                if (currentAmountStr.isBlank()) {
                    _saveTransactionUiState.value = SaveTransactionUiState.Error("Nominal Pengeluaran tidak boleh kosong.")
                    return@launch
                }
                val expenseAmount = currentAmountStr.toDoubleOrNull()
                if (expenseAmount == null || expenseAmount <= 0) {
                    _saveTransactionUiState.value = SaveTransactionUiState.Error("Nominal Pengeluaran tidak valid atau harus lebih dari 0.")
                    return@launch
                }
                domainInterpretedType = InterpretedDomainType.PURE_EXPENSE
                domainAmountForFirestore = expenseAmount
                domainSellingPriceForFirestore = null
                firestoreTypeString = "expense"
            }

            if (isEditMode && transactionId != null) {
                val originalTransaction = transactionRepository.getTransactionById(transactionId).firstOrNull()

                if (originalTransaction == null) {
                    _saveTransactionUiState.value = SaveTransactionUiState.Error("Gagal memuat data asli untuk update.")
                    return@launch
                }

                val businessUnitIdToUse = editingTransactionBusinessUnitId ?: originalTransaction.businessUnitId

                if (businessUnitIdToUse.isBlank()) {
                    _saveTransactionUiState.value = SaveTransactionUiState.Error("ID Unit Bisnis tidak valid untuk update.")
                    return@launch
                }

                val transactionToUpdate = originalTransaction.copy(
                    businessUnitId = businessUnitIdToUse,
                    type = firestoreTypeString,
                    amount = domainAmountForFirestore,
                    sellingPrice = domainSellingPriceForFirestore,
                    description = currentDescription,
                    date = finalTransactionDateTime,
                    updatedAt = Date(),
                    interpretedType = domainInterpretedType
                )

                Log.d("AddTransactionVM", "Updating transaction. Date being sent: ${debugDateFormat.format(transactionToUpdate.date)}")

                val result = transactionRepository.updateTransaction(transactionToUpdate)
                result.fold(
                    onSuccess = {
                        // Handle debt update/creation/deletion
                        handleDebtForEditedTransaction(transactionId, currentUserId, businessUnitIdToUse, domainSellingPriceForFirestore ?: domainAmountForFirestore, firestoreTypeString)
                        _saveTransactionUiState.value = SaveTransactionUiState.Success(isEditMode = true)
                    },
                    onFailure = { exception ->
                        _saveTransactionUiState.value = SaveTransactionUiState.Error(
                            exception.message ?: "Gagal memperbarui transaksi."
                        )
                    }
                )
            } else {
                // Mode ADD
                if (businessUnitId.isNullOrBlank()) {
                    _saveTransactionUiState.value = SaveTransactionUiState.Error("ID Unit Bisnis tidak valid. Tidak bisa menyimpan.")
                    Log.e("AddTransactionVM", "Gagal menyimpan: businessUnitId null atau kosong saat proses save.")
                    return@launch
                }

                val transactionToSaveDomain = Transaction(
                    transactionId = "",
                    userId = "",
                    businessUnitId = businessUnitId,
                    type = firestoreTypeString,
                    amount = domainAmountForFirestore,
                    sellingPrice = domainSellingPriceForFirestore,
                    description = currentDescription,
                    date = finalTransactionDateTime,
                    createdAt = Date(),
                    updatedAt = null,
                    interpretedType = domainInterpretedType
                )

                Log.d("AddTransactionVM", "Saving new transaction. BU ID: $businessUnitId. Date: ${debugDateFormat.format(transactionToSaveDomain.date)}")

                val result = transactionRepository.saveTransaction(transactionToSaveDomain)
                result.fold(
                    onSuccess = { newTransactionId ->
                        if (_isDebt.value && _selectedContact.value != null) {
                            handleDebtForNewTransaction(
                                newTransactionId,
                                currentUserId,
                                businessUnitId,
                                domainSellingPriceForFirestore ?: domainAmountForFirestore,
                                firestoreTypeString,
                                Timestamp(finalTransactionDateTime)
                            )
                        }
                        _saveTransactionUiState.value = SaveTransactionUiState.Success(isEditMode = false)
                    },
                    onFailure = { exception ->
                        Log.e("AddTransactionVM", "Gagal menyimpan transaksi baru: ${exception.message}", exception)
                        _saveTransactionUiState.value = SaveTransactionUiState.Error(
                            exception.message ?: "Gagal menyimpan transaksi."
                        )
                    }
                )
            }
        }
    }

    /**
     * ✅ Updated: Handle debt untuk transaksi baru dengan FirestoreContact
     */
    private suspend fun handleDebtForNewTransaction(
        transactionId: String,
        currentUserId: String,
        businessUnitId: String,
        totalAmount: Double,
        transactionType: String,
        timestamp: Timestamp
    ) {
        val contact = _selectedContact.value ?: return
        try {
            val existingSummaryResult = debtRepository.getContactSummary(contact.contactId)
            if (existingSummaryResult.isSuccess) {
                val summary = existingSummaryResult.getOrNull()
                if (summary != null && summary.hasActiveDebt) {
                    handleTransactionWithExistingDebt(contact, summary, totalAmount, timestamp, transactionId, businessUnitId, currentUserId)
                } else {
                    createNewTransactionDebt(contact, totalAmount, timestamp, transactionId, businessUnitId, currentUserId)
                }
            } else {
                createNewTransactionDebt(contact, totalAmount, timestamp, transactionId, businessUnitId, currentUserId)
            }
        } catch (e: Exception) {
            Log.e("AddEditVM", "Error handling new debt", e)
        }
    }

    /**
     * ✅ Updated: menggunakan FirestoreContact
     */
    private suspend fun handleTransactionWithExistingDebt(
        contact: FirestoreContact,
        summary: ContactDebtSummary,
        amount: Double,
        timestamp: Timestamp,
        transactionId: String,
        businessUnitId: String,
        userId: String
    ) {
        when (summary.debtorType) {
            com.mibi.xkas.model.DebtorType.BUSINESS_OWES -> {
                val currentBalance = summary.businessOwesAmount

                if (amount <= currentBalance) {
                    addPaymentToMostRecentBusinessDebt(summary, amount, timestamp, userId)
                    Log.d("AddEditVM", "Offset ${amount} against existing business debt of ${currentBalance}")
                } else {
                    val excess = amount - currentBalance

                    // Lunasi debt kita dulu
                    addPaymentToMostRecentBusinessDebt(summary, currentBalance, timestamp, userId)

                    // Sisa jadi hutang customer
                    createNewTransactionDebt(
                        contact,
                        excess,
                        timestamp,
                        transactionId,
                        businessUnitId,
                        userId
                    )

                    Log.d("AddEditVM", "Offset full business debt ${currentBalance}, remaining ${excess} as new customer debt")
                }
            }

            com.mibi.xkas.model.DebtorType.CUSTOMER_OWES -> {
                createNewTransactionDebt(
                    contact,
                    amount,
                    timestamp,
                    transactionId,
                    businessUnitId,
                    userId
                )
                Log.d("AddEditVM", "Added ${amount} to existing customer debt")
            }

            com.mibi.xkas.model.DebtorType.NO_DEBT -> {
                createNewTransactionDebt(
                    contact,
                    amount,
                    timestamp,
                    transactionId,
                    businessUnitId,
                    userId
                )
                Log.d("AddEditVM", "Created new customer debt ${amount}")
            }
        }
    }

    /**
     * ✅ Updated: menggunakan FirestoreContact
     */
    private suspend fun createNewTransactionDebt(
        contact: FirestoreContact,
        amount: Double,
        timestamp: Timestamp,
        transactionId: String,
        businessUnitId: String,
        userId: String
    ) {
        val debt = Debt(
            debtId = "",
            userId = userId,
            contactId = contact.contactId,
            description = "Transaksi: ${_description.value}",
            totalAmount = amount,
            paidAmount = 0.0,
            createdAt = timestamp,
            updatedAt = timestamp,
            debtType = "TRANSACTION",
            debtDirection = "CUSTOMER_OWES",
            transactionId = transactionId,
            businessUnitId = businessUnitId
        )
        debtRepository.addDebt(debt)
    }

    private suspend fun handleDebtForEditedTransaction(
        transactionId: String,
        currentUserId: String,
        businessUnitId: String,
        totalAmount: Double,
        transactionType: String
    ) {
        try {
            val existingDebt = debtRepository.getDebtByTransactionId(transactionId, currentUserId)
            val contact = _selectedContact.value
            if (_isDebt.value && contact != null) {
                if (existingDebt != null) {
                    val updatedDebt = existingDebt.copy(
                        contactId = contact.contactId,
                        totalAmount = totalAmount,
                        debtDirection = "CUSTOMER_OWES",
                        updatedAt = Timestamp.now()
                    )
                    debtRepository.updateDebt(updatedDebt)
                } else {
                    handleDebtForNewTransaction(transactionId, currentUserId, businessUnitId, totalAmount, transactionType, Timestamp.now())
                }
            } else {
                if (existingDebt != null) debtRepository.deleteDebt(existingDebt.debtId)
            }
        } catch (e: Exception) {
            Log.e("AddEditVM", "Error handling edited debt", e)
        }
    }

    private suspend fun addPaymentToMostRecentBusinessDebt(
        summary: ContactDebtSummary,
        amount: Double,
        timestamp: Timestamp,
        userId: String
    ) {
        // Cari business debt yang belum lunas (kita berhutang ke dia)
        val targetDebt = summary.debts
            .filter { it.debtDirection == "BUSINESS_OWES" && it.totalAmount > it.paidAmount }
            .maxByOrNull { it.createdAt.seconds }

        if (targetDebt != null) {
            val payment = com.mibi.xkas.data.model.DebtPayment(
                paymentId = java.util.UUID.randomUUID().toString(),
                userId = userId,
                debtId = targetDebt.debtId,
                contactId = summary.contactId,
                amount = amount,
                description = "Offset dari transaksi: ${_description.value}",
                paidAt = timestamp
            )

            debtRepository.addDebtPayment(payment)
        }
    }

    fun clearInputFields() {
        _description.value = ""
        _amount.value = ""
        _sellingPrice.value = ""
        _transactionDate.value = uiDateFormat.format(Date())
        _selectedUITransactionType.value = TransactionType.PEMASUKAN
        _isDebt.value = false
        _selectedContact.value = null
    }

    fun resetUiState() { _saveTransactionUiState.value = SaveTransactionUiState.Idle }
}