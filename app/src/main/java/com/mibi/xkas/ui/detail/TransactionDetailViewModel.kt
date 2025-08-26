package com.mibi.xkas.ui.detail // Sesuaikan dengan package Anda

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibi.xkas.data.Transaction
import com.mibi.xkas.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

// Definisikan UI State untuk TransactionDetailScreen
sealed interface TransactionDetailUiState {
    data class Success(val transaction: Transaction) : TransactionDetailUiState
    data class Error(val message: String) : TransactionDetailUiState
    object Loading : TransactionDetailUiState
    object NotFound : TransactionDetailUiState // Jika transaksi tidak ditemukan
    object Deleting : TransactionDetailUiState // Opsional, jika ingin menampilkan loading saat delete
    object DeleteSuccess : TransactionDetailUiState
    data class DeleteError(val message: String) : TransactionDetailUiState // Menyimpan pesan error
}

@HiltViewModel
class TransactionDetailViewModel @Inject constructor( // Tambahkan @Inject
    private val savedStateHandle: SavedStateHandle, // Hilt akan menyediakan ini
    private val transactionRepository: TransactionRepository // Hilt akan menyediakan ini
) : ViewModel() {

    private val transactionId: String = checkNotNull(savedStateHandle["transactionId"]) {
        "transactionId is missing from SavedStateHandle" // Pesan error yang lebih deskriptif
    }

    private val _uiState = MutableStateFlow<TransactionDetailUiState>(TransactionDetailUiState.Loading)
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    init {
        if (transactionId.isNotEmpty()) { // Pastikan transactionId tidak kosong sebelum load
            loadTransactionDetails()
        } else {
            // Kasus jika transactionId entah bagaimana kosong, meskipun checkNotNull seharusnya menangkapnya
            _uiState.value = TransactionDetailUiState.Error("ID Transaksi tidak valid.")
        }
    }

    fun loadTransactionDetails() {
        viewModelScope.launch {
            // Pastikan transactionRepository.getTransactionById mengembalikan Flow<Transaction?>
            transactionRepository.getTransactionById(transactionId)
                .onStart { _uiState.value = TransactionDetailUiState.Loading }
                .catch { exception ->
                    _uiState.value = TransactionDetailUiState.Error(exception.message ?: "Terjadi kesalahan tidak diketahui saat mengambil data.")
                    // Log error untuk debugging lebih lanjut
                    // Log.e("TransactionDetailVM", "Error loading transaction", exception)
                }
                .collect { transaction ->
                    if (transaction != null) {
                        _uiState.value = TransactionDetailUiState.Success(transaction)
                    } else {
                        _uiState.value = TransactionDetailUiState.NotFound
                    }
                }
        }
    }

    fun deleteCurrentTransaction() {
        val currentState = _uiState.value
        if (currentState is TransactionDetailUiState.Success) {
            val transactionIdToDelete = currentState.transaction.transactionId
            viewModelScope.launch {
                _uiState.value = TransactionDetailUiState.Deleting // Opsional
                val deleteResult = transactionRepository.deleteTransactionAndRelatedDebt(transactionIdToDelete) // Panggil fungsi repository
                deleteResult.fold(
                    onSuccess = {
                        _uiState.value = TransactionDetailUiState.DeleteSuccess
                    },
                    onFailure = { exception ->
                        _uiState.value = TransactionDetailUiState.DeleteError(exception.message ?: "Gagal menghapus transaksi.")
                    }
                )
            }
        } else {
            // Jika state bukan Success, tidak seharusnya tombol delete bisa diakses
            // Atau jika bisa, tampilkan error yang sesuai
            _uiState.value = TransactionDetailUiState.DeleteError("Tidak ada transaksi yang valid untuk dihapus.")
        }
    }

    // Fungsi untuk kembali ke state detail setelah error delete, jika diperlukan
    // atau untuk navigasi setelah sukses delete
    fun acknowledgeDeleteResult() {
        val currentState = _uiState.value
        if (currentState is TransactionDetailUiState.DeleteError || currentState is TransactionDetailUiState.DeleteSuccess) {
            // Jika error, mungkin muat ulang detail atau biarkan pengguna navigasi
            // Jika sukses, pengguna harus navigasi kembali dari detail screen
            // Untuk sekarang, kita bisa coba muat ulang jika masih error
            if (currentState is TransactionDetailUiState.DeleteError) {
                // Jika ingin kembali ke state detail setelah error:
                // loadTransactionDetails()
                // Atau, jika ingin UI menampilkan pesan error dan membiarkan pengguna yang navigasi:
                // Biarkan saja di state DeleteError, dan UI akan menampilkannya.
            }
            // Jika DeleteSuccess, screen akan diobservasi dan akan melakukan navigasi up.
        }
    }

    fun refreshDetails() {
        if (transactionId.isNotEmpty()) {
            loadTransactionDetails()
        }
    }
}


