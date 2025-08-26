package com.mibi.xkas.ui.addedit

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.mibi.xkas.data.InterpretedDomainType
import com.mibi.xkas.data.Transaction
import com.mibi.xkas.data.model.Debt
import com.mibi.xkas.data.repository.DebtRepository
import com.mibi.xkas.data.repository.TransactionRepository
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
    private val savedStateHandle: SavedStateHandle,
    private val firebaseAuth: FirebaseAuth // Pastikan ini di-inject
) : ViewModel() {

    val transactionId: String? = savedStateHandle[Screen.AddEditTransaction.ARG_TRANSACTION_ID]
    val businessUnitId: String? = savedStateHandle[Screen.AddEditTransaction.ARG_BUSINESS_UNIT_ID]

    // --- PERBAIKAN 1: Inisialisasi 'isEditMode' langsung saat deklarasi ---
    val isEditMode: Boolean = transactionId != null

    // --- PERBAIKAN 2: Inisialisasi '_saveTransactionUiState' dengan nilai awal ---
    private val _saveTransactionUiState = MutableStateFlow<SaveTransactionUiState>(SaveTransactionUiState.Idle)
    val saveTransactionUiState: StateFlow<SaveTransactionUiState> = _saveTransactionUiState.asStateFlow()

    // State untuk input fields
    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _sellingPrice = MutableStateFlow("")
    val sellingPrice: StateFlow<String> = _sellingPrice.asStateFlow()

    private val _isDebt = MutableStateFlow(false)
    val isDebt: StateFlow<Boolean> = _isDebt.asStateFlow()

    private val _debtorName = MutableStateFlow("")
    val debtorName: StateFlow<String> = _debtorName.asStateFlow()

    private val _debtorPhone = MutableStateFlow("")
    val debtorPhone: StateFlow<String> = _debtorPhone.asStateFlow()

    fun onDebtCheckedChange(checked: Boolean) {
        _isDebt.value = checked
        if (!checked) {
            _debtorName.value = ""
            _debtorPhone.value = ""
        }
    }

    fun onDebtorNameChange(name: String) {
        _debtorName.value = name
    }

    fun onDebtorPhoneChange(phone: String) {
        _debtorPhone.value = phone
    }


    private val uiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    // Deklarasikan debugDateFormat di sini
    private val debugDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()) // Contoh format debug

    private val _transactionDate = MutableStateFlow(uiDateFormat.format(Date()))
    val transactionDate: StateFlow<String> = _transactionDate.asStateFlow()

    private val _selectedUITransactionType = MutableStateFlow(TransactionType.PEMASUKAN)
    val selectedUITransactionType: StateFlow<TransactionType> = _selectedUITransactionType.asStateFlow()

    private val numberFormatter = DecimalFormat("#.##")

    init {
        Log.d("AddEditVM", "ViewModel Dibuat. ID Transaksi: $transactionId, ID Unit Bisnis: $businessUnitId")
        if (isEditMode) {
            // transactionId tidak mungkin null di sini karena pengecekan isEditMode
            loadTransactionDetails(transactionId!!)
        } else if (businessUnitId == null) {
            Log.e("AddEditVM", "Kondisi Kritis: Mode Tambah tetapi businessUnitId adalah null!")
            _saveTransactionUiState.value = SaveTransactionUiState.Error("ID Unit Bisnis tidak ditemukan. Silakan kembali.")
        }
    }

    // Fungsi helper untuk memformat Double menjadi String tanpa .0 jika tidak perlu
    private fun formatDoubleToString(value: Double?): String {
        return value?.let {
            if (it % 1.0 == 0.0) { // Cek apakah ini bilangan bulat
                it.toLong().toString() // Konversi ke Long lalu ke String
            } else {
                numberFormatter.format(it) // Gunakan formatter untuk kasus desimal lain
                // atau cukup it.toString() jika numberFormatter tidak diinginkan untuk ini
            }
        } ?: ""
    }

    private fun loadTransactionDetails(id: String) {
        viewModelScope.launch {
            transactionRepository.getTransactionById(id).firstOrNull()?.let { transaction ->
                _description.value = transaction.description

                if (transaction.type.equals("income", ignoreCase = true)) {
                    _selectedUITransactionType.value = TransactionType.PEMASUKAN
                    // Gunakan fungsi format
                    _sellingPrice.value = formatDoubleToString(transaction.sellingPrice)
                    _amount.value = formatDoubleToString(transaction.amount) // Harga modal
                } else { // Asumsikan "expense"
                    _selectedUITransactionType.value = TransactionType.PENGELUARAN
                    // Gunakan fungsi format
                    _amount.value = formatDoubleToString(transaction.amount) // Nominal pengeluaran
                    _sellingPrice.value = "" // Pastikan kosong untuk pengeluaran
                }
                _transactionDate.value = uiDateFormat.format(transaction.date)

            } ?: run {
                _saveTransactionUiState.value = SaveTransactionUiState.Error("Gagal memuat detail transaksi.")
                Log.e("ViewModel", "Transaksi dengan ID $id tidak ditemukan.")
            }
        }
    }


    // Fungsi untuk mengubah state dari UI
    fun onDescriptionChange(newDescription: String) {
        _description.value = newDescription
        // Jika ada pesan error terkait deskripsi, Anda bisa reset di sini
        if (_saveTransactionUiState.value is SaveTransactionUiState.Error) {
            // Cek apakah error terkait deskripsi, atau reset saja semua error
            // resetUiState() // Atau logika yang lebih spesifik
        }
    }

    fun onAmountChange(newAmount: String) {
        _amount.value = newAmount
    }

    fun onSellingPriceChange(newSellingPrice: String) {
        _sellingPrice.value = newSellingPrice
    }

    fun onDateChange(newDateString: String) { // Terima String dari DatePicker
        _transactionDate.value = newDateString
    }

    // Dipanggil dari UI ketika Tab Pemasukan/Pengeluaran diubah
    fun onTransactionTypeChanged(selectedUIType: TransactionType) {
        _selectedUITransactionType.value = selectedUIType // Update state internal ViewModel
        if (selectedUIType == TransactionType.PENGELUARAN) {
            _sellingPrice.value = ""
        }
        // Tidak perlu memanggil viewModel.onTransactionTypeChanged(selectedTransactionType) dari UI lagi
        // karena UI akan mengobservasi _selectedUITransactionType dari ViewModel
    }

    fun onIsDebtChanged(value: Boolean) {
        _isDebt.value = value
    }

    fun onDebtorNameChanged(value: String) {
        _debtorName.value = value
    }

    fun onDebtorPhoneChanged(value: String) {
        _debtorPhone.value = value
    }

    fun attemptSaveOrUpdateTransaction() {
        val currentDescription = _description.value
        val currentAmountStr = _amount.value
        val currentTransactionInputType = _selectedUITransactionType.value
        val currentSellingPriceStr = if (currentTransactionInputType == TransactionType.PEMASUKAN) _sellingPrice.value else null
        val currentDateStr = _transactionDate.value // Ini adalah "yyyy-MM-dd"

        viewModelScope.launch {
            _saveTransactionUiState.value = SaveTransactionUiState.Loading

            // --- Validasi userId dan businessUnitId di awal ---
            val currentUserId = firebaseAuth.currentUser?.uid
            if (currentUserId.isNullOrBlank()) {
                _saveTransactionUiState.value = SaveTransactionUiState.Error("Pengguna tidak terautentikasi.")
                return@launch
            }

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
            calendarForSelectedDate.set(Calendar.MILLISECOND, currentTimeCalendar.get(Calendar.MILLISECOND)) // Tambahkan ini untuk presisi lebih

            val finalTransactionDateTime: Date = calendarForSelectedDate.time

            // <<<<<<<<<<<<<<<<<<< TAMBAHKAN LOG DI SINI >>>>>>>>>>>>>>>>>>>>>
            Log.d("AddTransactionVM", "1. Current Date String from UI: $currentDateStr")
            Log.d("AddTransactionVM", "2. Parsed Date Only (should have 00:00:00 time): ${debugDateFormat.format(parsedDateOnly)}")
            Log.d("AddTransactionVM", "3. Final Transaction DateTime to be saved: ${debugDateFormat.format(finalTransactionDateTime)}")
            // <<<<<<<<<<<<<<<<<<<<<<<<<< END OF LOGS >>>>>>>>>>>>>>>>>>>>>>>>>>

            val domainInterpretedType: InterpretedDomainType
            val domainAmountForFirestore: Double
            val domainSellingPriceForFirestore: Double?
            val firestoreTypeString: String

            // ... (logika penentuan Pemasukan/Pengeluaran tetap sama) ...
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
            } else { // PENGELUARAN
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

                val transactionToUpdate = originalTransaction.copy(
                    type = firestoreTypeString,
                    amount = domainAmountForFirestore,
                    sellingPrice = domainSellingPriceForFirestore,
                    description = currentDescription,
                    date = finalTransactionDateTime,
                    updatedAt = Date(),
                    interpretedType = domainInterpretedType
                )
                // <<<<<<<<<<< LOG SEBELUM UPDATE >>>>>>>>>>>>>
                Log.d("AddTransactionVM", "Updating transaction. Date being sent: ${debugDateFormat.format(transactionToUpdate.date)}")
                // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
                val result = transactionRepository.updateTransaction(transactionToUpdate)
                result.fold(
                    onSuccess = {
                        _saveTransactionUiState.value = SaveTransactionUiState.Success(isEditMode = true)
                    },
                    onFailure = { exception ->
                        _saveTransactionUiState.value = SaveTransactionUiState.Error(
                            exception.message ?: "Gagal memperbarui transaksi."
                        )
                    }
                )
            } else {// --- PENGUATAN LOGIKA ADD ---
                if (businessUnitId.isNullOrBlank()) {
                    _saveTransactionUiState.value = SaveTransactionUiState.Error("ID Unit Bisnis tidak valid. Tidak bisa menyimpan.")
                    Log.e("AddTransactionVM", "Gagal menyimpan: businessUnitId null atau kosong saat proses save.")
                    return@launch
                }
                val transactionToSaveDomain = Transaction(
                    transactionId = "", // Firestore akan generate ID, ini oke
                    userId = "", // TODO: Ganti dengan ID pengguna yang sedang login
                    businessUnitId = businessUnitId, // <<<< TAMBAHKAN INI >>>>
                    type = firestoreTypeString,
                    amount = domainAmountForFirestore,
                    sellingPrice = domainSellingPriceForFirestore,
                    description = currentDescription,
                    date = finalTransactionDateTime,
                    createdAt = Date(),
                    updatedAt = null,
                    interpretedType = domainInterpretedType
                )
                // <<<<<<<<<<< LOG SEBELUM SAVE >>>>>>>>>>>>>
                Log.d("AddTransactionVM", "Saving new transaction. BU ID: $businessUnitId. Date: ${debugDateFormat.format(transactionToSaveDomain.date)}")
                val result = transactionRepository.saveTransaction(transactionToSaveDomain)
                result.fold(
                    onSuccess = { newTransactionId ->
                        // Simpan hutang jika dicentang
                        if (_isDebt.value && _debtorName.value.isNotBlank() && _debtorPhone.value.isNotBlank()) {
                            val debt = Debt(
                                userId = currentUserId,
                                contactName = _debtorName.value,
                                contactPhone = _debtorPhone.value,
                                totalAmount = domainSellingPriceForFirestore ?: domainAmountForFirestore,
                                paidAmount = 0.0,
                                transactionId = newTransactionId,
                                businessUnitId = businessUnitId
                            )
                            debtRepository.addDebt(debt)
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

    // 5. Fungsi untuk membersihkan field input setelah sukses
    fun clearInputFields() {
        _description.value = ""
        _amount.value = ""
        _sellingPrice.value = ""
        _transactionDate.value = uiDateFormat.format(Date()) // Reset ke tanggal hari ini
        _selectedUITransactionType.value = TransactionType.PEMASUKAN
    }

    fun resetUiState() {
        _saveTransactionUiState.value = SaveTransactionUiState.Idle
    }
}
