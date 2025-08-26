package com.mibi.xkas.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Model data piutang/hutang yang disimpan di Firestore
 */
data class Debt(
    @DocumentId val debtId: String = "",
    val userId: String = "", // UID dari user pemilik data (login Firebase)
    val contactName: String = "", // Nama orang yang berhutang
    val contactPhone: String = "", // Nomor telepon
    val totalAmount: Double = 0.0, // Total nominal hutang
    val paidAmount: Double = 0.0, // Total yang sudah dibayar (cicilan)
    val transactionId: String = "", // Transaksi asal hutang
    val businessUnitId: String = "", // Unit usaha terkait
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp? = null // Terisi jika ada update pembayaran
)
