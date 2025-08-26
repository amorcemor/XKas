package com.mibi.xkas.ui.home

import android.util.Log
import androidx.lifecycle.SavedStateHandle // Pastikan ini di-import
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.mibi.xkas.data.BusinessUnit
import com.mibi.xkas.data.Transaction
import com.mibi.xkas.data.repository.BusinessUnitRepository
import com.mibi.xkas.data.repository.TransactionRepository
import com.mibi.xkas.ui.addedit.BUSINESS_UNIT_ADDED_KEY // <--- TAMBAHKAN IMPORT INI
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject


enum class DateFilterType {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    ALL_TIME,
    CUSTOM_RANGE
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle, // Sudah ada
    private val transactionRepository: TransactionRepository,
    private val businessUnitRepository: BusinessUnitRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    sealed interface BusinessUnitUiState {
        data class Success(val businessUnit: List<BusinessUnit>) : BusinessUnitUiState
        data class Error(val message: String?) : BusinessUnitUiState
        object Loading : BusinessUnitUiState
        object Empty : BusinessUnitUiState
    }

    private val _businessUnitUiState = MutableStateFlow<BusinessUnitUiState>(BusinessUnitUiState.Loading)
    val businessUnitUiState: StateFlow<BusinessUnitUiState> = _businessUnitUiState.asStateFlow()

    private val _selectedBusinessUnit = MutableStateFlow<BusinessUnit?>(null)
    val selectedBusinessUnit: StateFlow<BusinessUnit?> = _selectedBusinessUnit.asStateFlow()

    private val _showBusinessUnitSelectionDialog = MutableStateFlow(false)
    val showBusinessUnitSelectionDialog: StateFlow<Boolean> = _showBusinessUnitSelectionDialog.asStateFlow()

    private var isInitialBusinessUnitCheck = false


    private val _rawTransactionsForSelectedBU = MutableStateFlow<List<Transaction>>(emptyList())
    private val _currentDateFilter = MutableStateFlow(DateFilterType.THIS_MONTH)
    val currentDateFilterType: StateFlow<DateFilterType> = _currentDateFilter.asStateFlow()
    private val _selectedStartDate = MutableStateFlow<Long?>(null)
    val selectedStartDate: StateFlow<Long?> = _selectedStartDate.asStateFlow()
    private val _selectedEndDate = MutableStateFlow<Long?>(null)
    val selectedEndDate: StateFlow<Long?> = _selectedEndDate.asStateFlow()
    private val _isLoadingTransactions = MutableStateFlow(false)
    private val _showAddFirstTransactionDialog = MutableStateFlow(false)
    val showAddFirstTransactionDialog: StateFlow<Boolean> = _showAddFirstTransactionDialog.asStateFlow()
    private var isPotentiallyFirstTimeUserSession = true


    val transactionsUiState: StateFlow<TransactionListUiState> =
        combine(
            _rawTransactionsForSelectedBU,
            _currentDateFilter,
            _selectedStartDate,
            _selectedEndDate,
            _isLoadingTransactions,
            _selectedBusinessUnit
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            val rawTransactions = values[0] as List<Transaction>
            val filterType = values[1] as DateFilterType
            val startDateMillis = values[2] as Long?
            val endDateMillis = values[3] as Long?
            val isLoadingTrans = values[4] as Boolean
            val currentBU = values[5] as BusinessUnit?

            if (currentBU == null) {
                TransactionListUiState.SelectBusinessUnit
            } else if (isLoadingTrans && rawTransactions.isEmpty() && _businessUnitUiState.value !is BusinessUnitUiState.Empty ) { // Tambahan cek agar tidak loading jika BU memang empty
                TransactionListUiState.Loading
            } else {
                filterTransactions(rawTransactions, filterType, startDateMillis, endDateMillis, isLoadingTrans)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = TransactionListUiState.SelectBusinessUnit
        )


    init {
        // Observasi perubahan dari SavedStateHandle untuk penambahan/edit BU
        viewModelScope.launch {
            savedStateHandle.getStateFlow<Boolean?>(BUSINESS_UNIT_ADDED_KEY, null) // Gunakan KEY yang di-import
                .collectLatest { buAddedOrEdited ->
                    if (buAddedOrEdited == true) {
                        Log.d("HomeViewModel", "$BUSINESS_UNIT_ADDED_KEY diterima: true. Memuat ulang unit bisnis.")
                        isInitialBusinessUnitCheck = false // Ini bukan lagi pengecekan awal
                        loadUserBusinessUnitAndDecideDialog()
                        savedStateHandle[BUSINESS_UNIT_ADDED_KEY] = null // Reset flag
                    }
                }
        }

        // Logika untuk memuat transaksi berdasarkan _selectedBusinessUnit
        viewModelScope.launch {
            _selectedBusinessUnit
                .flatMapLatest { businessUnit ->
                    if (businessUnit == null || businessUnit.businessUnitId.isBlank()) {
                        flowOf(Result.success(emptyList<Transaction>()))
                    } else {
                        _isLoadingTransactions.value = true
                        Log.d("HomeViewModel", "Mulai mengamati transaksi untuk BU: ${businessUnit.businessUnitId}")
                        transactionRepository.getTransactionsForBusinessUnitFlow(businessUnit.businessUnitId)
                    }
                }
                .collectLatest { result: Result<List<Transaction>> ->
                    _isLoadingTransactions.value = true
                    result.fold(
                        onSuccess = { domainTransactions ->
                            Log.d("HomeViewModel", "Transaksi diterima untuk BU aktif: ${domainTransactions.size} items")
                            _rawTransactionsForSelectedBU.value = domainTransactions
                        },
                        onFailure = { exception ->
                            Log.e("HomeViewModel", "Error loading transactions from flow", exception)
                            _rawTransactionsForSelectedBU.value = emptyList()
                        }
                    )
                    _isLoadingTransactions.value = false
                }
        }
        // Pemanggilan awal tidak dilakukan di sini lagi, tapi melalui triggerInitialBusinessUnitCheck()
        // atau dari SavedStateHandle di atas.
    }

    fun triggerInitialBusinessUnitCheck() {
        Log.d("HomeViewModel", "triggerInitialBusinessUnitCheck called")
        // Hanya set isInitialBusinessUnitCheck jika belum ada BU yang dipilih
        // ATAU jika _businessUnitUiState masih loading/error awal.
        // Ini untuk mencegah re-trigger dialog pemilihan jika user sudah memilih BU
        // dan hanya kembali ke home tanpa ada perubahan dari SavedStateHandle.
        if (_selectedBusinessUnit.value == null || _businessUnitUiState.value is BusinessUnitUiState.Loading || _businessUnitUiState.value is BusinessUnitUiState.Error) {
            isInitialBusinessUnitCheck = true
        } else {
            isInitialBusinessUnitCheck = false // Jika sudah ada BU terpilih, anggap bukan initial check lagi.
        }
        loadUserBusinessUnitAndDecideDialog()
    }

    private fun loadUserBusinessUnitAndDecideDialog() {
        val userId = firebaseAuth.currentUser?.uid
        if (userId.isNullOrBlank()) {
            Log.w("HomeViewModel", "UserID tidak tersedia.")
            _businessUnitUiState.value = BusinessUnitUiState.Error("Pengguna belum login atau ID tidak valid.")
            _selectedBusinessUnit.value = null
            _rawTransactionsForSelectedBU.value = emptyList()
            return
        }

        Log.d("HomeViewModel", "loadUserBusinessUnitAndDecideDialog called for userId: $userId, isInitial: $isInitialBusinessUnitCheck")
        viewModelScope.launch {
            // Jangan langsung set ke Loading jika sudah Success atau Empty, kecuali jika isInitialBusinessUnitCheck
            // Ini untuk menghindari kedipan loading jika hanya refresh minor
            if (isInitialBusinessUnitCheck || _businessUnitUiState.value !is BusinessUnitUiState.Success) {
                _businessUnitUiState.value = BusinessUnitUiState.Loading
            }
            try {
                Log.d("HomeViewModel", "Mencoba mengumpulkan business units untuk userId: $userId")
                businessUnitRepository.getUserBusinessUnit(userId).collectLatest { buList -> // INI BLOK YANG DIPERTAHANKAN
                    Log.d("HomeViewModel", "Business Units diterima: ${buList.size} unit.")

                    val previousSelectedBuId = _selectedBusinessUnit.value?.businessUnitId
                    var newSelectedBuCandidate: BusinessUnit? = _selectedBusinessUnit.value

                    // Jika BU yang terpilih sebelumnya sudah tidak ada di list baru, reset.
                    if (newSelectedBuCandidate != null && buList.none { it.businessUnitId == newSelectedBuCandidate.businessUnitId }) {
                        Log.d("HomeViewModel", "BU terpilih (${newSelectedBuCandidate.name}) sudah tidak valid. Mereset.")
                        newSelectedBuCandidate = null
                    }

                    if (buList.isNotEmpty()) {
                        _businessUnitUiState.value = BusinessUnitUiState.Success(buList)

                        if (isInitialBusinessUnitCheck) {
                            isInitialBusinessUnitCheck = false // Reset flag penting di sini
                            if (newSelectedBuCandidate == null) { // Jika belum ada yang terpilih (atau yang lama tidak valid)
                                if (buList.size == 1) {
                                    newSelectedBuCandidate = buList.first()
                                    _showBusinessUnitSelectionDialog.value = false
                                } else { // Lebih dari 1 BU
                                    _showBusinessUnitSelectionDialog.value = true
                                }
                            } else {
                                // BU yang terpilih sebelumnya masih valid, biarkan.
                                _showBusinessUnitSelectionDialog.value = false
                            }
                        } else { // Bukan initial check (misalnya, refresh dari SavedStateHandle atau TopAppBar)
                            if (newSelectedBuCandidate == null) { // Jika tidak ada BU yang terpilih (mungkin karena direset, atau memang belum pernah)
                                if (buList.size == 1) {
                                    newSelectedBuCandidate = buList.first()
                                    _showBusinessUnitSelectionDialog.value = false
                                } else if (buList.size > 1) {
                                    _showBusinessUnitSelectionDialog.value = true
                                }
                            } else {
                                // Jika sudah ada BU terpilih DAN dialog tidak sedang ingin ditampilkan secara eksplisit (misalnya karena klik TopAppBar)
                                // maka jangan ubah state dialog. State dialog akan dikontrol oleh onTopBarBusinessUnitClicked.
                                // Jika _showBusinessUnitSelectionDialog.value sudah true (misal dari onTopBarBusinessUnitClicked), biarkan.
                                // Jika false, biarkan false.
                                // Jadi, baris ini _showBusinessUnitSelectionDialog.value = false bisa jadi redundant atau
                                // malah menutup dialog yang seharusnya terbuka karena klik top bar.
                                // Untuk amannya, kita hanya set false jika memang belum ada yg terpilih dan hanya ada 1 BU.
                                // Atau jika memang sudah ada yang terpilih dan ini bukan initial check.
                                if (!_showBusinessUnitSelectionDialog.value) { // Hanya set false jika memang tidak sedang diminta untuk show.
                                    _showBusinessUnitSelectionDialog.value = false
                                }
                            }
                        }
                    } else { // buList is empty
                        _businessUnitUiState.value = BusinessUnitUiState.Empty
                        newSelectedBuCandidate = null
                        _rawTransactionsForSelectedBU.value = emptyList() // Pastikan transaksi dikosongkan
                        if (isInitialBusinessUnitCheck) {
                            isInitialBusinessUnitCheck = false
                        }
                        // Jika daftar BU kosong, selalu tampilkan dialog untuk memungkinkan pengguna menambah BU baru.
                        _showBusinessUnitSelectionDialog.value = true
                    }

                    // Logika pembaruan _selectedBusinessUnit setelah semua kondisi dialog dan BU diperiksa
                    if (previousSelectedBuId != newSelectedBuCandidate?.businessUnitId || (_selectedBusinessUnit.value == null && newSelectedBuCandidate != null)) {
                        Log.d("HomeViewModel", "Memperbarui _selectedBusinessUnit ke: ${newSelectedBuCandidate?.name}")
                        _selectedBusinessUnit.value = newSelectedBuCandidate
                        // Pemuatan transaksi akan dipicu oleh perubahan _selectedBusinessUnit di kolektor lain.
                    } else if (newSelectedBuCandidate == null && previousSelectedBuId != null) {
                        // Ini terjadi jika BU yang dipilih sebelumnya dihapus, dan tidak ada BU lain yang otomatis dipilih.
                        Log.d("HomeViewModel", "Mereset _selectedBusinessUnit menjadi null karena BU tidak ada atau menjadi tidak valid.")
                        _selectedBusinessUnit.value = null
                        _rawTransactionsForSelectedBU.value = emptyList()
                    }

                    Log.d("HomeViewModel", "_showBusinessUnitSelectionDialog.value diatur menjadi: ${_showBusinessUnitSelectionDialog.value} pada akhir collectLatest")
                    Log.d("HomeViewModel", "_selectedBusinessUnit.value saat ini: ${_selectedBusinessUnit.value?.name}")

                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error mengumpulkan business units", e)
                _businessUnitUiState.value = BusinessUnitUiState.Error(e.message)
                _selectedBusinessUnit.value = null // Reset BU terpilih jika ada error
                _rawTransactionsForSelectedBU.value = emptyList() // Reset transaksi jika ada error
                // Pertimbangkan apakah dialog harus ditampilkan dalam kasus error.
                // Mungkin tidak, karena pengguna tidak bisa memilih apa-apa.
                _showBusinessUnitSelectionDialog.value = false
            }
        }
    }


    fun onTopBarBusinessUnitClicked() {
        val userId = firebaseAuth.currentUser?.uid
        if (userId.isNullOrBlank()) {
            _businessUnitUiState.value = BusinessUnitUiState.Error("Pengguna tidak valid.")
            Log.d("HomeViewModel", "onTopBarBusinessUnitClicked: Pengguna tidak valid.")
            return
        }
        Log.d("HomeViewModel", "onTopBarBusinessUnitClicked called")

        viewModelScope.launch {
            // Selalu ambil daftar BU terbaru sebelum memutuskan
            val currentBuState = _businessUnitUiState.value
            // Untuk sementara, selalu set Loading agar terlihat ada aksi
            // if (currentBuState !is BusinessUnitUiState.Success || currentBuState.businessUnit.isEmpty()) {
            _businessUnitUiState.value = BusinessUnitUiState.Loading // Set Loading di awal
            Log.d("HomeViewModel", "onTopBarBusinessUnitClicked: State diatur ke Loading.")
            // }

            try { // Tambahkan try-catch di sini
                // Menggunakan .first() atau .firstOrNull() akan mengambil data sekali dan menutup Flow.
                // Ini mungkin yang Anda inginkan untuk klik, agar tidak ada listener yang bocor.
                val buList = businessUnitRepository.getUserBusinessUnit(userId).firstOrNull() ?: emptyList()
                Log.d("HomeViewModel", "onTopBarBusinessUnitClicked: Menerima ${buList.size} BU.")

                if (buList.isNotEmpty()) {
                    _businessUnitUiState.value = BusinessUnitUiState.Success(buList)
                    Log.d("HomeViewModel", "onTopBarBusinessUnitClicked: State diatur ke Success dengan ${buList.size} BU.")
                    if (buList.size > 1) {
                        _showBusinessUnitSelectionDialog.value = true
                        Log.d("HomeViewModel", "onTopBarBusinessUnitClicked: Menampilkan dialog karena >1 BU.")
                    } else { // Hanya 1 BU
                        // Jika ada 1 BU, apakah kita mau langsung pilih atau tetap tampilkan dialog?
                        // Untuk saat ini, kita tampilkan dialog agar pengguna bisa melihat opsi "Tambah Baru" atau detail BU tersebut.
                        // Jika Anda ingin memilihnya secara otomatis, Anda bisa memanggil setSelectedBusinessUnit di sini.
                        _showBusinessUnitSelectionDialog.value = true // Tetap tampilkan dialog
                        Log.d("HomeViewModel", "onTopBarBusinessUnitClicked: Menampilkan dialog untuk 1 BU (untuk opsi tambah/lihat).")
                        // Jika Anda ingin memilihnya dan menutup dialog:
                        // if (_selectedBusinessUnit.value?.businessUnitId != buList.first().businessUnitId) {
                        //     setSelectedBusinessUnit(buList.first())
                        // }
                        // _showBusinessUnitSelectionDialog.value = false
                    }
                } else { // buList is empty
                    _businessUnitUiState.value = BusinessUnitUiState.Empty
                    // setSelectedBusinessUnit(null) // selectedBusinessUnit akan di-handle oleh kolektor loadUserBusinessUnit
                    Log.d("HomeViewModel", "onTopBarBusinessUnitClicked: State diatur ke Empty.")
                    _showBusinessUnitSelectionDialog.value = true // Tampilkan dialog untuk tambah baru
                    Log.d("HomeViewModel", "onTopBarBusinessUnitClicked: Menampilkan dialog karena BU kosong.")
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "onTopBarBusinessUnitClicked: Error mengambil BU", e)
                _businessUnitUiState.value = BusinessUnitUiState.Error("Gagal memuat unit bisnis: ${e.message}")
                _showBusinessUnitSelectionDialog.value = false // Jangan tampilkan dialog jika ada error
            }
        }
    }

    fun setSelectedBusinessUnit(businessUnit: BusinessUnit?) {
        // Cek apakah benar-benar ada perubahan
        if (_selectedBusinessUnit.value?.businessUnitId != businessUnit?.businessUnitId) {
            Log.d("HomeViewModel", "Business Unit dipilih: ${businessUnit?.name ?: "Tidak ada (null)"}")
            _selectedBusinessUnit.value = businessUnit
            // Jika BU baru dipilih (bukan null), tutup dialog.
            // Jika businessUnit adalah null (misalnya, dari onTopBarBusinessUnitClicked dan tidak ada BU),
            // dialog mungkin perlu tetap terbuka atau dibuka oleh logika di onTopBar...
            // Jadi, _showBusinessUnitSelectionDialog diatur oleh pemanggil setSelectedBusinessUnit atau logika lain.
        } else if (businessUnit == null && _selectedBusinessUnit.value != null) {
            // Kasus di mana businessUnit di-set ke null dan sebelumnya ada isinya.
            _selectedBusinessUnit.value = null
            Log.d("HomeViewModel", "Business Unit di-reset ke null.")
        }
        // Jangan otomatis tutup dialog di sini, biarkan pemanggil yang mengontrol.
        // _showBusinessUnitSelectionDialog.value = false
    }


    fun onBusinessUnitDialogDismiss() {
        _showBusinessUnitSelectionDialog.value = false
        Log.d("HomeViewModel", "Dialog BU ditutup (Dismiss).")
        // Jika dialog ditutup dan ini adalah initial check dan belum ada BU terpilih,
        // dan ADA business unit yang tersedia, pertimbangkan untuk memilih yang pertama.
        // Ini adalah pilihan UX.
        val currentBuState = _businessUnitUiState.value
        if (isInitialBusinessUnitCheck && _selectedBusinessUnit.value == null &&
            currentBuState is BusinessUnitUiState.Success && currentBuState.businessUnit.isNotEmpty()) {
            // Log.d("HomeViewModel", "Dialog awal ditutup tanpa memilih, memilih BU pertama: ${currentBuState.businessUnit.first().name}")
            // setSelectedBusinessUnit(currentBuState.businessUnit.first()) // OPSI: pilih yang pertama
        }
        isInitialBusinessUnitCheck = false // Pastikan flag direset setelah interaksi dialog awal
    }

    fun deleteBusinessUnit(businessUnitId: String) {
        viewModelScope.launch {
            // Tampilkan state loading jika perlu (misalnya, pada BusinessUnitUiState)
            // _businessUnitUiState.value = BusinessUnitUiState.Loading // Contoh

            try {
                // Panggil metode repository untuk menghapus unit bisnis
                // Ganti 'businessUnitRepository.deleteBusinessUnit' dengan metode yang sesuai di repository Anda
                val deleteResult = businessUnitRepository.deleteBusinessUnit(businessUnitId)

                if (deleteResult.isSuccess) { // Asumsi repository mengembalikan semacam Result wrapper
                    // Penghapusan berhasil

                    // 1. Perbarui daftar unit bisnis
                    // Cara paling sederhana adalah memicu ulang pengambilan data unit bisnis
                    triggerInitialBusinessUnitCheck() // atau fungsi lain yang me-refresh daftar

                    // 2. Tangani jika unit bisnis yang dihapus adalah yang sedang dipilih
                    if (_selectedBusinessUnit.value?.businessUnitId == businessUnitId) {
                        _selectedBusinessUnit.value = null // Atur ke null atau pilih BU lain
                        // Jika Anda ingin otomatis memilih BU lain:
                        // val currentList = (_businessUnitUiState.value as? BusinessUnitUiState.Success)?.businessUnit
                        // _selectedBusinessUnit.value = currentList?.firstOrNull()
                    }

                    // Opsional: Tampilkan pesan sukses jika perlu
                    // _snackbarMessage.value = "Unit bisnis berhasil dihapus"

                } else {
                    // Penghapusan gagal dari repository
                    // _businessUnitUiState.value = BusinessUnitUiState.Error("Gagal menghapus unit bisnis.") // Contoh
                    // _snackbarMessage.value = deleteResult.exceptionOrNull()?.message ?: "Gagal menghapus unit bisnis"
                    Log.e("HomeViewModel", "Gagal menghapus unit bisnis: ${deleteResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                // Tangani error jaringan atau exception lain
                // _businessUnitUiState.value = BusinessUnitUiState.Error("Terjadi kesalahan: ${e.message}") // Contoh
                // _snackbarMessage.value = "Terjadi kesalahan: ${e.message}"
                Log.e("HomeViewModel", "Error saat menghapus unit bisnis", e)
            }
        }
    }
    fun updateBusinessUnitName(businessUnitId: String, newName: String) {
        viewModelScope.launch {
            // Asumsi Anda punya fungsi di repository untuk update nama, atau update keseluruhan objek
            // Jika repository Anda memerlukan seluruh objek BusinessUnit:
            val currentBusinessUnit = (_businessUnitUiState.value as? BusinessUnitUiState.Success)
                ?.businessUnit
                ?.find { it.businessUnitId == businessUnitId }

            if (currentBusinessUnit != null) {
                val updatedBusinessUnit = currentBusinessUnit.copy(name = newName)
                try {
                    // Panggil metode repository untuk update (misalnya, updateBusinessUnit)
                    // Ganti dengan metode yang sesuai di repository Anda
                    val updateResult = businessUnitRepository.updateBusinessUnit(updatedBusinessUnit)

                    if (updateResult.isSuccess) {
                        // Update berhasil
                        triggerInitialBusinessUnitCheck() // Refresh daftar

                        // Jika BU yang diupdate adalah yang sedang dipilih, perbarui juga state selectedBusinessUnit
                        if (_selectedBusinessUnit.value?.businessUnitId == businessUnitId) {
                            _selectedBusinessUnit.value = updatedBusinessUnit
                        }
                        // _snackbarMessage.value = "Nama unit bisnis berhasil diperbarui"
                    } else {
                        // Update gagal dari repository
                        Log.e("HomeViewModel", "Gagal update nama unit bisnis: ${updateResult.exceptionOrNull()?.message}")
                        // _snackbarMessage.value = updateResult.exceptionOrNull()?.message ?: "Gagal update nama"
                    }
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Error saat update nama unit bisnis", e)
                    // _snackbarMessage.value = "Terjadi kesalahan: ${e.message}"
                }
            } else {
                Log.e("HomeViewModel", "Tidak dapat menemukan unit bisnis untuk diupdate dengan ID: $businessUnitId")
                // _snackbarMessage.value = "Unit bisnis tidak ditemukan"
            }
        }
    }


    // ... (sisa fungsi filterTransactions, onDialogDismissed, onDialogConfirmed, setDateFilter, setCustomDateRange, refreshTransactionsForCurrentBusinessUnit, dan helper tanggal TETAP SAMA)
    // Fungsi helper tanggal tetap sama
    private fun getStartOfDay(date: Date, cal: Calendar): Date {
        cal.time = date
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    private fun getEndOfDay(date: Date, cal: Calendar): Date {
        cal.time = date
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.time
    }

    private fun getStartOfWeek(date: Date, cal: Calendar): Date {
        cal.time = date
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        return getStartOfDay(cal.time, cal)
    }

    private fun getEndOfWeek(date: Date, cal: Calendar): Date {
        cal.time = date
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.add(Calendar.DAY_OF_WEEK, 6)
        return getEndOfDay(cal.time, cal)
    }

    private fun getStartOfMonth(date: Date, cal: Calendar): Date {
        cal.time = date
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return getStartOfDay(cal.time, cal)
    }

    private fun getEndOfMonth(date: Date, cal: Calendar): Date {
        cal.time = date
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        return getEndOfDay(cal.time, cal)
    }

    // Fungsi filterTransactions, onDialogDismissed, onDialogConfirmed, setDateFilter, dll.
    // tidak saya sertakan lagi di sini karena tidak ada perubahan langsung untuk SavedStateHandle.
    // Pastikan mereka ada di file asli Anda.
    private fun filterTransactions(
        rawTransactions: List<Transaction>,
        filterType: DateFilterType,
        startDateMillis: Long?,
        endDateMillis: Long?,
        isLoadingTransactions: Boolean
    ): TransactionListUiState {
        Log.d("HomeViewModel", "filterTransactions called. isLoading: $isLoadingTransactions, rawTransactions: ${rawTransactions.size}, filter: $filterType")

        if (isLoadingTransactions && rawTransactions.isEmpty() && _businessUnitUiState.value !is BusinessUnitUiState.Empty) {
            Log.d("HomeViewModel", "Returning: Loading (transactions for selected BU)")
            return TransactionListUiState.Loading
        }

        if (!isLoadingTransactions && rawTransactions.isEmpty() && isPotentiallyFirstTimeUserSession && _selectedBusinessUnit.value != null) {
            val isDefaultFilterActive = filterType == DateFilterType.THIS_MONTH || filterType == DateFilterType.ALL_TIME
            if (isDefaultFilterActive && !_showAddFirstTransactionDialog.value) {
                Log.d("HomeViewModel", "Condition for first time user (for this BU) met. Setting showAddFirstTransactionDialog to true.")
                viewModelScope.launch { _showAddFirstTransactionDialog.emit(true) }
            }
            // Tetap return Empty agar UI konsisten jika dialog first transaction muncul.
            return TransactionListUiState.Empty
        }

        val calendar = Calendar.getInstance()
        val now = Date()

        val filteredList = when (filterType) {
            DateFilterType.TODAY -> {
                val startOfDay = getStartOfDay(now, calendar)
                val endOfDay = getEndOfDay(now, calendar)
                rawTransactions.filter { it.date in startOfDay..endOfDay }
            }
            DateFilterType.THIS_WEEK -> {
                val startOfWeek = getStartOfWeek(now, calendar)
                val endOfWeek = getEndOfWeek(now, calendar)
                rawTransactions.filter { it.date in startOfWeek..endOfWeek }
            }
            DateFilterType.THIS_MONTH -> {
                val startOfMonth = getStartOfMonth(now, calendar)
                val endOfMonth = getEndOfMonth(now, calendar)
                rawTransactions.filter { it.date in startOfMonth..endOfMonth }
            }
            DateFilterType.ALL_TIME -> rawTransactions
            DateFilterType.CUSTOM_RANGE -> {
                if (startDateMillis != null && endDateMillis != null) {
                    val customStartDate = Date(startDateMillis)
                    val customEndDate = Date(endDateMillis)
                    rawTransactions.filter { it.date in customStartDate..customEndDate }
                } else {
                    rawTransactions // Jika custom range tidak valid, tampilkan semua dari BU yang dipilih
                }
            }
        }

        Log.d("HomeViewModel", "Filter result: type=$filterType, rawCount=${rawTransactions.size}, filteredCount=${filteredList.size}")

        return if (filteredList.isNotEmpty()) {
            TransactionListUiState.Success(filteredList)
        } else {
            // Jika rawTransactions (untuk BU yang dipilih) ada isinya tapi filter tidak menghasilkan apa-apa
            if (rawTransactions.isNotEmpty()) {
                TransactionListUiState.NoResultsForFilter
            } else {
                // Jika rawTransactions (untuk BU yang dipilih) memang kosong
                TransactionListUiState.Empty
            }
        }
    }

    fun onDialogDismissed() {
        _showAddFirstTransactionDialog.value = false
        isPotentiallyFirstTimeUserSession = false
        Log.d("HomeViewModel", "AddFirstTransactionDialog dismissed.")
    }

    fun onDialogConfirmed() {
        _showAddFirstTransactionDialog.value = false
        isPotentiallyFirstTimeUserSession = false
        Log.d("HomeViewModel", "AddFirstTransactionDialog confirmed.")
    }

    fun setDateFilter(filterType: DateFilterType) {
        _currentDateFilter.value = filterType
        if (filterType != DateFilterType.CUSTOM_RANGE) {
            _selectedStartDate.value = null
            _selectedEndDate.value = null
        }
        Log.d("HomeViewModel", "Date filter set to: $filterType")
    }

    fun setCustomDateRange(startDateMillis: Long?, endDateMillis: Long?) {
        _selectedStartDate.value = startDateMillis
        _selectedEndDate.value = endDateMillis
        if (startDateMillis != null && endDateMillis != null) {
            _currentDateFilter.value = DateFilterType.CUSTOM_RANGE
        }
        Log.d("HomeViewModel", "Custom date range set: Start=$startDateMillis, End=$endDateMillis")
    }

    fun refreshTransactionsForCurrentBusinessUnit() {
        val currentBuId = _selectedBusinessUnit.value?.businessUnitId
        if (currentBuId.isNullOrBlank()) {
            Log.w("HomeViewModel", "Tidak ada Business Unit dipilih untuk di-refresh.")
            return
        }

        viewModelScope.launch {
            _isLoadingTransactions.value = true
            val result: Result<List<Transaction>> = transactionRepository.getTransactionsForBusinessUnit(currentBuId)

            result.fold(
                onSuccess = { domainTransactionsList ->
                    Log.d("HomeViewModel", "Refreshed transactions for BU $currentBuId: ${domainTransactionsList.size} items")
                    _rawTransactionsForSelectedBU.value = domainTransactionsList
                },
                onFailure = { exception ->
                    Log.e("HomeViewModel", "Error refreshing transactions for BU $currentBuId", exception)
                }
            )
            _isLoadingTransactions.value = false
        }
    }
}
