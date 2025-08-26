package com.mibi.xkas.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Model data piutang/hutang yang disimpan di Firestore
 * ✅ UPDATED: Hanya reference ke FirestoreContact, no redundant contact data
 */
data class Debt(
    @DocumentId val debtId: String = "",
    val userId: String = "",
    val contactId: String = "",
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val transactionId: String = "",
    val businessUnitId: String = "",
    val description: String = "",
    val debtType: String = "MANUAL",
    val debtDirection: String = "CUSTOMER_OWES",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp? = null
)