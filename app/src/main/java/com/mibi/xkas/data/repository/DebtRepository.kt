package com.mibi.xkas.data.repository

import com.mibi.xkas.data.model.Debt
import com.mibi.xkas.data.model.DebtPayment
import com.mibi.xkas.model.ContactDebtSummary
import kotlinx.coroutines.flow.Flow

interface DebtRepository {
    fun getAllDebts(userId: String): Flow<List<Debt>>
    fun getDebtsByBusinessUnit(userId: String, businessUnitId: String): Flow<List<Debt>>
    suspend fun addDebt(debt: Debt): Result<Unit>
    suspend fun updateDebt(debt: Debt): Result<Unit>
    suspend fun deleteDebt(debtId: String): Result<Unit>
    suspend fun getDebtById(debtId: String): Result<Debt>
    suspend fun addDebtPayment(payment: DebtPayment): Result<Unit>
    suspend fun deleteDebtPayment(debtId: String, paymentId: String, amount: Double): Result<Unit> // Tambah fungsi baru
    fun getDebtPayments(debtId: String): Flow<List<DebtPayment>>
    fun getDebtByIdFlow(debtId: String): Flow<Result<Debt>>
    fun getDebtPaymentsFlow(debtId: String): Flow<Result<List<DebtPayment>>>
    suspend fun getDebtByTransactionId(transactionId: String, userId: String): Debt?

    // Untuk manajemen kontak hutang
    fun getGroupedDebtsByContact(userId: String): Flow<List<ContactDebtSummary>>
    // Untuk pengelompokan hutang berdasarkan kontak
    fun getDebtsByContact(userId: String, contactId: String): Flow<List<Debt>>
    suspend fun addManualDebt(debt: Debt): Result<String>
    suspend fun updateContactTotals(contactId: String): Result<Unit>

    // Summary untuk detail layar kontak
    suspend fun getContactSummary(contactId: String): Result<ContactDebtSummary>
    fun getContactSummaryFlow(contactId: String): Flow<Result<ContactDebtSummary>>
}

