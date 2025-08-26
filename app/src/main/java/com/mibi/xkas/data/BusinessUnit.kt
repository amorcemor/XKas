package com.mibi.xkas.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

// Enum untuk tipe BusinessUnit (Anda bisa sesuaikan/tambahkan nanti)
enum class BusinessUnitType {
    PULSA_COUNTER,      // Konter Pulsa
    COPY_CENTER,        // Fotokopi
    WIFI_PROVIDER,      // Penyedia Layanan WiFi
    RETAIL_SHOP,        // Toko Kelontong/Ritel
    FOOD_STALL,         // Warung Makan
    SERVICE_PROVIDER,   // Penyedia Jasa Umum Lainnya
    OTHER               // Lainnya
}

data class BusinessUnit(
    @DocumentId // Annotation ini penting agar Firestore otomatis mengisi field ini dengan ID dokumen jika kita menyimpannya dengan string kosong
    val businessUnitId: String = "", // ID unik, akan diisi oleh Firestore jika string kosong saat menyimpan dokumen baru
    val userId: String = "",          // ID pengguna yang memiliki Business Unit ini
    val name: String = "",            // Nama yang diberikan pengguna (e.g., "MIBI CELL")
    val type: BusinessUnitType = BusinessUnitType.OTHER, // Tipe dari Business Unit
    val description: String? = null,    // Deskripsi opsional
    val initialBalance: Double = 0.0, // Saldo awal saat Business Unit ini dibuat
    val createdAt: Timestamp = Timestamp.now(), // Tanggal dan waktu dibuat (gunakan Timestamp Firestore)
    val updatedAt: Timestamp? = null,        // Tanggal dan waktu terakhir diperbarui
//    val isDefault: Boolean = false
    @get:PropertyName("default")
    @set:PropertyName("default")
    var isDefault: Boolean = false // harus var jika pakai @PropertyName pada setter
)