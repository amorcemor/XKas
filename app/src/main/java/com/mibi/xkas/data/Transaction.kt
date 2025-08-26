package com.mibi.xkas.data

import java.util.Date

// Enum ini tetap sangat disarankan untuk kejelasan di domain,
// meskipun tidak secara langsung ada di TransactionFirestore.kt.
// Ini akan membantu logika bisnis Anda.
enum class InterpretedDomainType {
    SALE_WITH_COST,
    PURE_INCOME,
    PURE_EXPENSE
}

data class Transaction(
    // Field yang namanya sama dan tipenya mirip dengan TransactionFirestore
    val transactionId: String,       // Sama seperti di Firestore
    val businessUnitId: String = "",       // <-- BARU: ID dari BusinessUnit tempat transaksi ini terjadi
    val userId: String,          // Sama
    val type: String,              // Sama (String "income", "expense", dll.)
    val amount: Double,            // Sama (Ini adalah 'harga modal' atau 'pengeluaran')
    val sellingPrice: Double?,     // Sama (Ini adalah 'harga jual')
    val description: String,       // Sama
    val date: Date,                // Diubah ke Date untuk kemudahan di domain (dari String di Firestore)

    // Timestamp dikonversi ke Date
    val createdAt: Date,
    val updatedAt: Date?,

    // Properti tambahan di domain untuk membantu interpretasi, TIDAK disimpan di Firestore
    // Ini adalah cara untuk mendapatkan kejelasan tanpa mengubah struktur dasar.
    @get:JvmName("getInterpretedType") // Menghindari konflik nama jika ada fungsi dengan nama sama
    val interpretedType: InterpretedDomainType // Dihitung berdasarkan sellingPrice dan amount
) {
    // Konstruktor sekunder atau companion object function untuk membuat 'interpretedType'
    // akan ada di mapper. Di sini, kita asumsikan 'interpretedType' di-passing saat konstruksi.
}
