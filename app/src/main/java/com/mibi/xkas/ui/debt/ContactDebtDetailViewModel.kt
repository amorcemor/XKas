package com.mibi.xkas.ui.debt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.mibi.xkas.data.model.Debt
import com.mibi.xkas.data.model.DebtPayment
import com.mibi.xkas.data.repository.DebtRepository
import com.mibi.xkas.model.ContactDebtSummary
import com.mibi.xkas.model.DebtorType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ContactDebtDetailViewModel @Inject constructor(
    private val debtRepository: DebtRepository
) : ViewModel() {

    private val _contactSummary = MutableStateFlow<ContactDebtSummary?>(null)
    val contactSummary: StateFlow<ContactDebtSummary?> = _contactSummary.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _operationMessage = MutableStateFlow<String?>(null)
    val operationMessage: StateFlow<String?> = _operationMessage.asStateFlow()

    private val _isProcessingPayoff = MutableStateFlow(false)
    val isProcessingPayoff: StateFlow<Boolean> = _isProcessingPayoff.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ✅ PERBAIKAN STEP 6: Gunakan Flow untuk real-time updates
    fun loadSummary(contactId: String, userId: String) {
        viewModelScope.launch {
            _error.value = null
            _isLoading.value = true

            try {
                // ✅ Gunakan Flow untuk mendapatkan update real-time
                debtRepository.getContactSummaryFlow(contactId).collect { result ->
                    result.onSuccess { summary ->
                        _contactSummary.value = summary
                        _isLoading.value = false
                    }.onFailure { exception ->
                        _error.value = exception.message ?: "Gagal memuat ringkasan kontak"
                        _isLoading.value = false
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Gagal memuat ringkasan kontak"
                _isLoading.value = false
            }
        }
    }

    // ✅ PERBAIKAN: Manual refresh function yang lebih efisien
    fun refreshSummary(contactId: String, userId: String) {
        viewModelScope.launch {
            _error.value = null
            _isLoading.value = true

            try {
                val result = debtRepository.getContactSummary(contactId)
                result.onSuccess { summary ->
                    _contactSummary.value = summary
                }.onFailure { exception ->
                    _error.value = exception.message ?: "Gagal memuat ringkasan kontak"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Gagal memuat ringkasan kontak"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * ✅ PERBAIKAN STEP 6: Simplifikasi payoff logic dengan konsep saldo neto
     * Lunasi semua hutang untuk kontak berdasarkan debtorType
     */
    fun payOffAllDebt(contactId: String, userId: String) {
        viewModelScope.launch {
            _operationMessage.value = null
            _error.value = null
            _isProcessingPayoff.value = true

            try {
                val summaryResult = debtRepository.getContactSummary(contactId)
                summaryResult.onSuccess { summary ->

                    // ✅ VALIDASI: Pastikan ada hutang yang perlu dilunasi
                    if (summary.debtorType == DebtorType.NO_DEBT) {
                        _operationMessage.value = "Tidak ada hutang yang perlu dilunasi"
                        _isProcessingPayoff.value = false
                        return@onSuccess
                    }

                    when (summary.debtorType) {
                        DebtorType.CUSTOMER_OWES -> {
                            // Customer berhutang ke kita, customer bayar semua
                            payOffCustomerDebts(summary, userId)
                        }
                        DebtorType.BUSINESS_OWES -> {
                            // Kita berhutang ke customer, kita bayar semua
                            payOffBusinessDebts(summary, userId)
                        }
                        DebtorType.NO_DEBT -> {
                            // Sudah tidak ada hutang
                            _operationMessage.value = "Tidak ada hutang yang perlu dilunasi"
                        }
                    }

                }.onFailure {
                    _error.value = it.message ?: "Gagal mengambil ringkasan sebelum pelunasan"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Terjadi kesalahan saat melunasi hutang"
            } finally {
                _isProcessingPayoff.value = false
            }
        }
    }

    /**
     * ✅ BARU: Lunasi hutang customer (customer bayar ke kita)
     */
    private suspend fun payOffCustomerDebts(summary: ContactDebtSummary, userId: String) {
        val customerDebts = summary.debts.filter {
            it.getSafeDebtDirection() == "CUSTOMER_OWES" &&
                    it.totalAmount > it.paidAmount
        }

        if (customerDebts.isEmpty()) {
            _operationMessage.value = "Tidak ada hutang customer yang perlu dilunasi"
            return
        }

        var successCount = 0
        var totalPaidOff = 0.0

        for (debt in customerDebts) {
            val remaining = debt.totalAmount - debt.paidAmount
            if (remaining <= 0) continue

            val payment = DebtPayment(
                paymentId = UUID.randomUUID().toString(),
                userId = userId,
                debtId = debt.debtId,
                contactId = summary.contactId,
                amount = remaining,
                description = "Pelunasan penuh oleh ${summary.contactName}",
                paidAt = Timestamp.now()
            )

            val payResult = debtRepository.addDebtPayment(payment)
            if (payResult.isSuccess) {
                successCount++
                totalPaidOff += remaining
            } else {
                _error.value = "Gagal mencatat pembayaran: ${payResult.exceptionOrNull()?.message}"
                return
            }
        }

        _operationMessage.value = "Berhasil melunasi $successCount hutang customer. Total: ${formatRupiah(totalPaidOff)}"
        refreshSummary(summary.contactId, userId)
    }

    /**
     * ✅ BARU: Lunasi hutang bisnis (kita bayar ke customer)
     */
    private suspend fun payOffBusinessDebts(summary: ContactDebtSummary, userId: String) {
        val businessDebts = summary.debts.filter {
            it.getSafeDebtDirection() == "BUSINESS_OWES" &&
                    it.totalAmount > it.paidAmount
        }

        if (businessDebts.isEmpty()) {
            _operationMessage.value = "Tidak ada hutang bisnis yang perlu dilunasi"
            return
        }

        var successCount = 0
        var totalPaidOff = 0.0

        for (debt in businessDebts) {
            val remaining = debt.totalAmount - debt.paidAmount
            if (remaining <= 0) continue

            val payment = DebtPayment(
                paymentId = UUID.randomUUID().toString(),
                userId = userId,
                debtId = debt.debtId,
                contactId = summary.contactId,
                amount = remaining,
                description = "Pelunasan penuh kepada ${summary.contactName}",
                paidAt = Timestamp.now()
            )

            val payResult = debtRepository.addDebtPayment(payment)
            if (payResult.isSuccess) {
                successCount++
                totalPaidOff += remaining
            } else {
                _error.value = "Gagal mencatat pembayaran: ${payResult.exceptionOrNull()?.message}"
                return
            }
        }

        _operationMessage.value = "Berhasil melunasi $successCount hutang bisnis. Total: ${formatRupiah(totalPaidOff)}"
        refreshSummary(summary.contactId, userId)
    }

    /**
     * ✅ PERBAIKAN: Delete debt dengan validasi yang lebih baik
     */
    fun deleteDebt(debtId: String) {
        viewModelScope.launch {
            _operationMessage.value = null
            _error.value = null

            try {
                // ✅ Validasi: Cek apakah debt memiliki payment sebelum dihapus
                val debtResult = debtRepository.getDebtById(debtId)
                debtResult.onSuccess { debt ->
                    if (debt.paidAmount > 0) {
                        _error.value = "Tidak dapat menghapus hutang yang sudah memiliki pembayaran. Hapus pembayaran terlebih dahulu."
                        return@onSuccess
                    }

                    val deleteResult = debtRepository.deleteDebt(debtId)
                    if (deleteResult.isSuccess) {
                        _operationMessage.value = "Catatan hutang berhasil dihapus"
                        // ✅ Refresh data setelah delete
                        _contactSummary.value?.let { summary ->
                            refreshSummary(summary.contactId, "")
                        }
                    } else {
                        _error.value = deleteResult.exceptionOrNull()?.message ?: "Gagal menghapus hutang"
                    }
                }.onFailure { exception ->
                    _error.value = exception.message ?: "Gagal mengambil data hutang"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Terjadi kesalahan saat menghapus hutang"
            }
        }
    }

    /**
     * ✅ BARU: Edit payment functionality
     */
    fun editPayment(paymentId: String, newAmount: Double, newDescription: String) {
        viewModelScope.launch {
            _operationMessage.value = null
            _error.value = null

            try {
                // TODO: Implement edit payment logic
                // Untuk saat ini, tampilkan pesan bahwa fitur belum tersedia
                _operationMessage.value = "Fitur edit pembayaran akan segera tersedia"
            } catch (e: Exception) {
                _error.value = e.message ?: "Terjadi kesalahan saat mengedit pembayaran"
            }
        }
    }

    /**
     * ✅ BARU: Delete payment functionality
     */
    fun deletePayment(paymentId: String, debtId: String, amount: Double) {
        viewModelScope.launch {
            _operationMessage.value = null
            _error.value = null

            try {
                val result = debtRepository.deleteDebtPayment(debtId, paymentId, amount)
                if (result.isSuccess) {
                    _operationMessage.value = "Pembayaran berhasil dihapus"
                    // Refresh data setelah delete payment
                    _contactSummary.value?.let { summary ->
                        refreshSummary(summary.contactId, "")
                    }
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Gagal menghapus pembayaran"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Terjadi kesalahan saat menghapus pembayaran"
            }
        }
    }

    // ✅ HELPER FUNCTIONS

    /**
     * Helper function untuk backward compatibility dengan debt direction
     */
    private fun Debt.getSafeDebtDirection(): String {
        return if (this.debtDirection.isBlank()) "CUSTOMER_OWES" else this.debtDirection
    }

    /**
     * Helper function untuk format rupiah
     */
    private fun formatRupiah(amount: Double): String {
        return "Rp ${String.format("%,.0f", amount)}"
    }

    // ✅ CLEAR FUNCTIONS
    fun clearOperationMessage() {
        _operationMessage.value = null
    }

    fun clearError() {
        _error.value = null
    }

    /**
     * ✅ BARU: Get detailed debt info untuk debugging
     */
    fun getDetailedDebtInfo(): String? {
        return _contactSummary.value?.debugInfo()
    }

    /**
     * ✅ BARU: Validate debt data consistency
     */
    fun validateDataConsistency(): Boolean {
        val summary = _contactSummary.value ?: return false

        return try {
            // Check if all debts have valid debt direction
            val hasInvalidDirection = summary.debts.any {
                it.debtDirection.isNotBlank() &&
                        it.debtDirection !in listOf("CUSTOMER_OWES", "BUSINESS_OWES")
            }

            // Check if amounts are valid
            val hasInvalidAmounts = summary.debts.any {
                it.totalAmount < 0 || it.paidAmount < 0 || it.paidAmount > it.totalAmount
            }

            !hasInvalidDirection && !hasInvalidAmounts
        } catch (e: Exception) {
            false
        }
    }
}