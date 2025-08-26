package com.mibi.xkas.data.model

import com.google.firebase.Timestamp

data class DebtPayment(
    val paymentId : String = "",
    val userId: String = "",
    val debtId: String = "",
    val contactId: String = "", // 🔄 tambahan
    val amount: Double = 0.0,
    val description: String = "", // 🔄 tambahan
    val paidAt: Timestamp = Timestamp.now()
)
