package com.mibi.xkas.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

// Enum untuk tipe BusinessUnit (Anda bisa sesuaikan/tambahkan nanti)
enum class BusinessUnitType {
    RETAIL_BUSINESS,      // Usaha Retail
    CULUNARY_BUSINESS,    // Usaha Kuliner
    SERVICE_BUSINESS,   // Penyedia Jasa
    ONLINE_BUSINESS,    // Bisnis Online
    OTHER               // Lainnya
}

data class BusinessUnit(
    @DocumentId // Annotation ini penting agar Firestore otomatis mengisi field ini dengan ID dokumen jika kita menyimpannya dengan string kosong
    val businessUnitId: String = "", // ID unik, akan diisi oleh Firestore jika string kosong saat menyimpan dokumen baru
    val userId: String = "",          // ID pengguna yang memiliki Business Unit ini
    val name: String = "",            // Nama yang diberikan pengguna (e.g., "MIBI CELL")
    val type: BusinessUnitType = BusinessUnitType.OTHER, // Tipe dari Business Unit
    val customTypeName: String? = null,
    val description: String? = null,    // Deskripsi opsional
    val initialBalance: Double = 0.0, // Saldo awal saat Business Unit ini dibuat
    val createdAt: Timestamp = Timestamp.now(), // Tanggal dan waktu dibuat (gunakan Timestamp Firestore)
    val updatedAt: Timestamp? = null,        // Tanggal dan waktu terakhir diperbarui
    @get:PropertyName("default")
    @set:PropertyName("default")
    var isDefault: Boolean = false // harus var jika pakai @PropertyName pada setter
)

fun BusinessUnit.getDisplayTypeName(): String {
    return if (type == BusinessUnitType.OTHER && !customTypeName.isNullOrBlank()) {
        customTypeName
    } else {
        when (type) {
            BusinessUnitType.RETAIL_BUSINESS -> "Usaha Retail"
            BusinessUnitType.CULUNARY_BUSINESS -> "Usaha Kuliner"
            BusinessUnitType.SERVICE_BUSINESS -> "Penyedia Jasa"
            BusinessUnitType.ONLINE_BUSINESS -> "Bisnis Online"
            BusinessUnitType.OTHER -> "Lainnya"
        }
    }
}