package com.mibi.xkas.model

import com.mibi.xkas.data.model.Debt
import com.mibi.xkas.data.model.DebtPayment
import com.mibi.xkas.data.model.FirestoreContact
import com.google.firebase.Timestamp

enum class DebtorType {
    CUSTOMER_OWES,
    BUSINESS_OWES,
    NO_DEBT
}

data class ContactDebtSummary(
    val contactId: String,
    val contact: FirestoreContact? = null,   // 🔑 ambil dari FirestoreContactRepository
    val debts: List<Debt>,
    val payments: List<DebtPayment>,
    val lastTransactionDate: Timestamp? = null,
    val totalOutstanding: Double
) {
    private fun Debt.getSafeDebtDirection(): String {
        return if (this.debtDirection.isBlank()) "CUSTOMER_OWES" else this.debtDirection
    }

    val contactName: String
        get() = contact?.name ?: "Tanpa Nama"

    val contactPhone: String
        get() = contact?.phoneNumber ?: ""

    val totalDebtAmount: Double
        get() = debts.sumOf { it.totalAmount }

    val totalPaidAmount: Double
        get() = debts.sumOf { it.paidAmount }

    val customerOwesAmount: Double
        get() = debts.filter { it.getSafeDebtDirection() == "CUSTOMER_OWES" }
            .sumOf { maxOf(0.0, it.totalAmount - it.paidAmount) }

    val businessOwesAmount: Double
        get() = debts.filter { it.getSafeDebtDirection() == "BUSINESS_OWES" }
            .sumOf { maxOf(0.0, it.totalAmount - it.paidAmount) }

    val netBalance: Double
        get() = customerOwesAmount - businessOwesAmount

    val absoluteBalance: Double
        get() = kotlin.math.abs(netBalance)

    val debtorType: DebtorType
        get() = when {
            netBalance > 0.01 -> DebtorType.CUSTOMER_OWES
            netBalance < -0.01 -> DebtorType.BUSINESS_OWES
            else -> DebtorType.NO_DEBT
        }

    val debtorLabel: String
        get() = when (debtorType) {
            DebtorType.CUSTOMER_OWES -> "Total Hutang"
            DebtorType.BUSINESS_OWES -> "Total Piutang"
            DebtorType.NO_DEBT -> "Lunas"
        }

    val hasActiveDebt: Boolean
        get() = debtorType != DebtorType.NO_DEBT

    fun debugInfo(): String {
        return buildString {
            appendLine("Contact: $contactName ($contactId)")
            appendLine("Phone: $contactPhone")
            appendLine("Total debts: ${debts.size}")
            appendLine("Total payments: ${payments.size}")
            appendLine("Customer owes: $customerOwesAmount")
            appendLine("Business owes: $businessOwesAmount")
            appendLine("Net balance: $netBalance")
            appendLine("Debtor type: $debtorType")
            appendLine("Last transaction: ${lastTransactionDate?.toDate()}")
            appendLine("Debts detail:")
            debts.forEach { d ->
                appendLine(" - ${d.debtId} dir=${d.debtDirection.ifBlank { "CUSTOMER_OWES" }} total=${d.totalAmount} paid=${d.paidAmount}")
            }
        }
    }
}
