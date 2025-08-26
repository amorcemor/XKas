package com.mibi.xkas.data.repository

import com.mibi.xkas.data.model.Debt
import com.mibi.xkas.data.model.DebtPayment
import kotlinx.coroutines.flow.Flow

interface DebtRepository {
    fun getAllDebts(userId: String): Flow<List<Debt>>
    fun getDebtsByBusinessUnit(userId: String, businessUnitId: String): Flow<List<Debt>>

    suspend fun addDebt(debt: Debt): Result<Unit>
    suspend fun updateDebt(debt: Debt): Result<Unit>
    suspend fun deleteDebt(debtId: String): Result<Unit>
    suspend fun getDebtById(debtId: String): Result<Debt>

    suspend fun addDebtPayment(payment: DebtPayment): Result<Unit>
    fun getDebtPayments(debtId: String): Flow<List<DebtPayment>>

    fun getDebtByIdFlow(debtId: String): Flow<Result<Debt>>
    fun getDebtPaymentsFlow(debtId: String): Flow<Result<List<DebtPayment>>>

}