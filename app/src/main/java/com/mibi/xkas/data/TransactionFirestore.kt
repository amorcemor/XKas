package com.mibi.xkas.data

import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.Timestamp // Pastikan import Timestamp ada

data class TransactionFirestore(
    var transactionId: String = "",
    var businessUnitId: String = "", // <-- TAMBAHKAN FIELD INI
    var userId: String = "",         // userId sudah ada, bagus
    var type: String = "",
    var amount: Double = 0.0,
    var sellingPrice: Double? = null,
    var description: String = "",
    var date: String = "",             // date tetap String seperti sebelumnya

    @ServerTimestamp
    var createdAt: Timestamp? = null, // Diubah menjadi var agar bisa di-set oleh Firestore jika null

    @ServerTimestamp
    var updatedAt: Timestamp? = null  // Diubah menjadi var agar bisa di-set oleh Firestore jika null
) {
    // Konstruktor tanpa argumen dibutuhkan oleh Firestore
    constructor() : this(
        transactionId = "",
        businessUnitId = "",      // <-- TAMBAHKAN DI KONSTRUKTOR JUGA
        userId = "",
        type = "",
        amount = 0.0,
        sellingPrice = null,
        description = "",
        date = "",
        createdAt = null,
        updatedAt = null
    )
}