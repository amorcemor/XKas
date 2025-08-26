package com.mibi.xkas.ui.home

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.mibi.xkas.data.BusinessUnit
import com.mibi.xkas.data.BusinessUnitType
import com.mibi.xkas.data.Transaction
import com.mibi.xkas.data.repository.BusinessUnitRepository
import com.mibi.xkas.data.repository.TransactionRepository
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
    private val savedStateHandle: SavedStateHandle,
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

    private val _isCreatingBusinessUnit = MutableStateFlow(false)
    val isCreatingBusinessUnit: StateFlow<Boolean> = _isCreatingBusinessUnit.asStateFlow()

    private val _businessUnitUiState = MutableStateFlow<BusinessUnitUiState>(BusinessUnitUiState.Loading)
    val businessUnitUiState: StateFlow<BusinessUnitUiState> = _businessUnitUiState.asStateFlow()

    private val _selectedBusinessUnit = MutableStateFlow<BusinessUnit?>(null)
    val selectedBusinessUnit: StateFlow<BusinessUnit?> = _selectedBusinessUnit.asStateFlow()

    private val _showBusinessUnitSelectionDialog = MutableStateFlow(false)
    val showBusinessUnitSelectionDialog: StateFlow<Boolean> = _showBusinessUnitSelectionDialog.asStateFlow()

    // PERBAIKAN: Tambahkan flag untuk mengecek apakah ini benar-benar pertama kali aplikasi dibuka
    private var hasEverLoadedBusinessUnits = false
    private var isReallyFirstTime = true

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
            } else if (isLoadingTrans && rawTransactions.isEmpty() && _businessUnitUiState.value !is BusinessUnitUiState.Empty) {
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
    }

    // PERBAIKAN: Fungsi baru untuk initial check yang benar-benar hanya untuk pertama kali
    fun triggerInitialBusinessUnitCheck() {
        Log.d("HomeViewModel", "triggerInitialBusinessUnitCheck called, isReallyFirstTime: $isReallyFirstTime")

        // Hanya lakukan initial check jika benar-benar pertama kali
        if (isReallyFirstTime) {
            isReallyFirstTime = false // Set ke false setelah pertama kali
            loadUserBusinessUnitAndDecideDialog(isInitialCheck = true)
        } else {
            // Jika bukan pertama kali, hanya refresh data tanpa dialog
            loadUserBusinessUnitAndDecideDialog(isInitialCheck = false, showDialogOnRefresh = false)
        }
    }

    // PERBAIKAN: Fungsi baru untuk navigasi dari menu lain
    fun onNavigatedFromOtherMenu() {
        Log.d("HomeViewModel", "onNavigatedFromOtherMenu called")
        // Ketika navigasi dari menu lain, refresh data tapi jangan tampilkan dialog
        loadUserBusinessUnitAndDecideDialog(isInitialCheck = false, showDialogOnRefresh = false)
    }

    // PERBAIKAN: Update fungsi loadUserBusinessUnitAndDecideDialog dengan parameter tambahan
    private fun loadUserBusinessUnitAndDecideDialog(
        isInitialCheck: Boolean = false,
        forceRefresh: Boolean = false,
        showDialogOnRefresh: Boolean = true
    ) {
        val userId = firebaseAuth.currentUser?.uid
        if (userId.isNullOrBlank()) {
            Log.w("HomeViewModel", "UserID tidak tersedia.")
            _businessUnitUiState.value = BusinessUnitUiState.Error("Pengguna belum login atau ID tidak valid.")
            _selectedBusinessUnit.value = null
            _rawTransactionsForSelectedBU.value = emptyList()
            return
        }

        Log.d("HomeViewModel", "loadUserBusinessUnitAndDecideDialog called for userId: $userId, isInitialCheck: $isInitialCheck, forceRefresh: $forceRefresh")

        viewModelScope.launch {
            // Set loading hanya jika initial check atau force refresh
            if (isInitialCheck || forceRefresh || _businessUnitUiState.value !is BusinessUnitUiState.Success) {
                _businessUnitUiState.value = BusinessUnitUiState.Loading
            }

            try {
                Log.d("HomeViewModel", "Mencoba mengumpulkan business units untuk userId: $userId")
                businessUnitRepository.getUserBusinessUnit(userId).collectLatest { buList ->
                    Log.d("HomeViewModel", "Business Units diterima: ${buList.size} unit.")

                    hasEverLoadedBusinessUnits = true
                    val previousSelectedBuId = _selectedBusinessUnit.value?.businessUnitId
                    var newSelectedBuCandidate: BusinessUnit? = _selectedBusinessUnit.value

                    // Validasi BU yang terpilih sebelumnya
                    if (newSelectedBuCandidate != null && buList.none { it.businessUnitId == newSelectedBuCandidate.businessUnitId }) {
                        Log.d("HomeViewModel", "BU terpilih (${newSelectedBuCandidate.name}) sudah tidak valid. Mereset.")
                        newSelectedBuCandidate = null
                    }

                    if (buList.isNotEmpty()) {
                        _businessUnitUiState.value = BusinessUnitUiState.Success(buList)

                        // PERBAIKAN: Logic dialog yang lebih tepat
                        if (isInitialCheck) {
                            // Hanya untuk initial check (pertama kali app dibuka)
                            if (newSelectedBuCandidate == null) {
                                if (buList.size == 1) {
                                    newSelectedBuCandidate = buList.first()
                                    _showBusinessUnitSelectionDialog.value = false
                                } else {
                                    _showBusinessUnitSelectionDialog.value = true
                                }
                            } else {
                                _showBusinessUnitSelectionDialog.value = false
                            }
                        } else if (forceRefresh && showDialogOnRefresh) {
                            // Untuk refresh dari SavedStateHandle (BU baru ditambah)
                            if (newSelectedBuCandidate == null && buList.size > 1) {
                                _showBusinessUnitSelectionDialog.value = true
                            }
                        }
                        // Untuk kasus lain (navigasi dari menu lain), tidak tampilkan dialog

                    } else {
                        // buList is empty
                        _businessUnitUiState.value = BusinessUnitUiState.Empty
                        newSelectedBuCandidate = null
                        _rawTransactionsForSelectedBU.value = emptyList()

                        // Hanya tampilkan dialog jika initial check atau explicit refresh
                        if (isInitialCheck || (forceRefresh && showDialogOnRefresh)) {
                            _showBusinessUnitSelectionDialog.value = true
                        }
                    }

                    // Update selected business unit
                    if (previousSelectedBuId != newSelectedBuCandidate?.businessUnitId ||
                        (_selectedBusinessUnit.value == null && newSelectedBuCandidate != null)) {
                        Log.d("HomeViewModel", "Memperbarui _selectedBusinessUnit ke: ${newSelectedBuCandidate?.name}")
                        _selectedBusinessUnit.value = newSelectedBuCandidate
                    } else if (newSelectedBuCandidate == null && previousSelectedBuId != null) {
                        Log.d("HomeViewModel", "Mereset _selectedBusinessUnit menjadi null")
                        _selectedBusinessUnit.value = null
                        _rawTransactionsForSelectedBU.value = emptyList()
                    }

                    Log.d("HomeViewModel", "_showBusinessUnitSelectionDialog.value: ${_showBusinessUnitSelectionDialog.value}")
                    Log.d("HomeViewModel", "_selectedBusinessUnit.value: ${_selectedBusinessUnit.value?.name}")
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error mengumpulkan business units", e)
                _businessUnitUiState.value = BusinessUnitUiState.Error(e.message)
                _selectedBusinessUnit.value = null
                _rawTransactionsForSelectedBU.value = emptyList()
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
            _businessUnitUiState.value = BusinessUnitUiState.Loading
            Log.d("HomeViewModel", "onTopBarBusinessUnitClicked: State diatur ke Loading.")

            try {
                val buList = businessUnitRepository.getUserBusinessUnit(userId).firstOrNull() ?: emptyList()
                Log.d("HomeViewModel", "onTopBarBusinessUnitClicked: Menerima ${buList.size} BU.")

                if (buList.isNotEmpty()) {
                    _businessUnitUiState.value = BusinessUnitUiState.Success(buList)
                    Log.d("HomeViewModel", "onTopBarBusinessUnitClicked: State diatur ke Success dengan ${buList.size} BU.")
                    // Selalu tampilkan dialog ketika user klik top bar (ini adalah tindakan eksplisit)
                    _showBusinessUnitSelectionDialog.value = true
                    Log.d("HomeViewModel", "onTopBarBusinessUnitClicked: Menampilkan dialog.")
                } else {
                    _businessUnitUiState.value = BusinessUnitUiState.Empty
                    Log.d("HomeViewModel", "onTopBarBusinessUnitClicked: State diatur ke Empty.")
                    _showBusinessUnitSelectionDialog.value = true
                    Log.d("HomeViewModel", "onTopBarBusinessUnitClicked: Menampilkan dialog karena BU kosong.")
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "onTopBarBusinessUnitClicked: Error mengambil BU", e)
                _businessUnitUiState.value = BusinessUnitUiState.Error("Gagal memuat unit bisnis: ${e.message}")
                _showBusinessUnitSelectionDialog.value = false
            }
        }
    }

    fun setSelectedBusinessUnit(businessUnit: BusinessUnit?) {
        if (_selectedBusinessUnit.value?.businessUnitId != businessUnit?.businessUnitId) {
            Log.d("HomeViewModel", "Business Unit dipilih: ${businessUnit?.name ?: "Tidak ada (null)"}")
            _selectedBusinessUnit.value = businessUnit
        } else if (businessUnit == null && _selectedBusinessUnit.value != null) {
            _selectedBusinessUnit.value = null
            Log.d("HomeViewModel", "Business Unit di-reset ke null.")
        }
    }

    fun onBusinessUnitDialogDismiss() {
        _showBusinessUnitSelectionDialog.value = false
        Log.d("HomeViewModel", "Dialog BU ditutup (Dismiss).")

        // PERBAIKAN: Hanya auto-select jika benar-benar initial check dan belum ada yang terpilih
        val currentBuState = _businessUnitUiState.value
        if (isReallyFirstTime && _selectedBusinessUnit.value == null &&
            currentBuState is BusinessUnitUiState.Success && currentBuState.businessUnit.isNotEmpty()) {
            // Auto select first BU jika ini pertama kali dan belum ada yang dipilih
            setSelectedBusinessUnit(currentBuState.businessUnit.first())
        }
    }

    // Sisa fungsi tetap sama...
    fun deleteBusinessUnit(businessUnitId: String) {
        viewModelScope.launch {
            try {
                val deleteResult = businessUnitRepository.deleteBusinessUnit(businessUnitId)
                if (deleteResult.isSuccess) {
                    // Refresh setelah delete
                    loadUserBusinessUnitAndDecideDialog(forceRefresh = true, showDialogOnRefresh = false)

                    if (_selectedBusinessUnit.value?.businessUnitId == businessUnitId) {
                        _selectedBusinessUnit.value = null
                    }
                } else {
                    Log.e("HomeViewModel", "Gagal menghapus unit bisnis: ${deleteResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error saat menghapus unit bisnis", e)
            }
        }
    }

    fun updateBusinessUnitName(businessUnitId: String, newName: String) {
        viewModelScope.launch {
            val currentBusinessUnit = (_businessUnitUiState.value as? BusinessUnitUiState.Success)
                ?.businessUnit
                ?.find { it.businessUnitId == businessUnitId }

            if (currentBusinessUnit != null) {
                val updatedBusinessUnit = currentBusinessUnit.copy(name = newName)
                try {
                    val updateResult = businessUnitRepository.updateBusinessUnit(updatedBusinessUnit)
                    if (updateResult.isSuccess) {
                        // Refresh setelah update
                        loadUserBusinessUnitAndDecideDialog(forceRefresh = true, showDialogOnRefresh = false)

                        if (_selectedBusinessUnit.value?.businessUnitId == businessUnitId) {
                            _selectedBusinessUnit.value = updatedBusinessUnit
                        }
                    } else {
                        Log.e("HomeViewModel", "Gagal update nama unit bisnis: ${updateResult.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Error saat update nama unit bisnis", e)
                }
            } else {
                Log.e("HomeViewModel", "Tidak dapat menemukan unit bisnis untuk diupdate dengan ID: $businessUnitId")
            }
        }
    }

    fun updateBusinessUnit(businessUnitId: String, newName: String, newDescription: String?, newType: BusinessUnitType, customTypeName: String? = null) {
        viewModelScope.launch {
            val currentBusinessUnit = (_businessUnitUiState.value as? BusinessUnitUiState.Success)
                ?.businessUnit
                ?.find { it.businessUnitId == businessUnitId }

            if (currentBusinessUnit != null) {
                val updatedBusinessUnit = currentBusinessUnit.copy(
                    name = newName,
                    description = newDescription,
                    type = newType,
                    customTypeName = if (newType == BusinessUnitType.OTHER) customTypeName else null,
                    updatedAt = Timestamp.now()
                )
                try {
                    val updateResult = businessUnitRepository.updateBusinessUnit(updatedBusinessUnit)
                    if (updateResult.isSuccess) {
                        loadUserBusinessUnitAndDecideDialog(forceRefresh = true, showDialogOnRefresh = false)

                        if (_selectedBusinessUnit.value?.businessUnitId == businessUnitId) {
                            _selectedBusinessUnit.value = updatedBusinessUnit
                        }
                    } else {
                        Log.e("HomeViewModel", "Gagal update unit bisnis: ${updateResult.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Error saat update unit bisnis", e)
                }
            } else {
                Log.e("HomeViewModel", "Tidak dapat menemukan unit bisnis untuk diupdate dengan ID: $businessUnitId")
            }
        }
    }

    fun createBusinessUnit(
        name: String,
        type: BusinessUnitType,
        description: String?,
        initialBalance: Double,
        customTypeName: String? = null
    ) {
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId.isNullOrBlank()) {
            Log.e("HomeViewModel", "User tidak terautentikasi saat membuat unit bisnis")
            return
        }

        viewModelScope.launch {
            _isCreatingBusinessUnit.value = true

            try {
                val newBusinessUnit = BusinessUnit(
                    businessUnitId = "",
                    userId = currentUserId,
                    name = name.trim(),
                    type = type,
                    customTypeName = if (type == BusinessUnitType.OTHER) customTypeName else null,
                    description = if (description.isNullOrBlank()) null else description.trim(),
                    initialBalance = initialBalance,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now(),
                    isDefault = false
                )

                val createResult = businessUnitRepository.createBusinessUnit(newBusinessUnit)

                if (createResult.isSuccess) {
                    Log.d("HomeViewModel", "Unit bisnis berhasil dibuat: ${createResult.getOrNull()}")
                    loadUserBusinessUnitAndDecideDialog(forceRefresh = true, showDialogOnRefresh = false)
                    _showBusinessUnitSelectionDialog.value = false
                } else {
                    Log.e("HomeViewModel", "Gagal membuat unit bisnis: ${createResult.exceptionOrNull()?.message}")
                }

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error saat membuat unit bisnis", e)
            } finally {
                _isCreatingBusinessUnit.value = false
            }
        }
    }

    // Fungsi helper tanggal dan filter tetap sama...
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
                    rawTransactions
                }
            }
        }

        Log.d("HomeViewModel", "Filter result: type=$filterType, rawCount=${rawTransactions.size}, filteredCount=${filteredList.size}")

        return if (filteredList.isNotEmpty()) {
            TransactionListUiState.Success(filteredList)
        } else {
            if (rawTransactions.isNotEmpty()) {
                TransactionListUiState.NoResultsForFilter
            } else {
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