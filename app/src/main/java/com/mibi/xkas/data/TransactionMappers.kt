package com.mibi.xkas.data

import com.google.firebase.Timestamp // Pastikan import Timestamp dari com.google.firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TransactionMappers {

    // Format tanggal yang Anda gunakan untuk field 'date' (String) di Firestore
    // Pastikan ini konsisten dengan cara Anda menyimpan dan membaca field 'date'
    private val firestoreStringDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun firestoreToDomain(fs: TransactionFirestore): Transaction {
        val parsedDate: Date = try {
            if (fs.date.isNotBlank()) {
                firestoreStringDateFormat.parse(fs.date) ?: Date() // Default ke Date() jika parse gagal
            } else {
                Date() // Default ke Date() jika string kosong
            }
        } catch (e: Exception) {
            // Log error jika perlu: Log.e("TransactionMappers", "Error parsing date string: ${fs.date}", e)
            Date() // Fallback ke Date() jika ada exception
        }

        val currentSellingPrice = fs.sellingPrice // Buat salinan lokal
        val currentAmount = fs.amount // Bisa juga buat salinan lokal untuk amount jika diperlukan konsistensi

        val domainInterpretedType: InterpretedDomainType = when {
            fs.type.equals("INCOME", ignoreCase = true) && currentSellingPrice != null && currentSellingPrice > 0 && currentAmount > 0 -> InterpretedDomainType.SALE_WITH_COST
            fs.type.equals("INCOME", ignoreCase = true) -> InterpretedDomainType.PURE_INCOME // Jika INCOME tapi tidak SALE_WITH_COST
            fs.type.equals("EXPENSE", ignoreCase = true) -> InterpretedDomainType.PURE_EXPENSE
            else -> InterpretedDomainType.PURE_EXPENSE // Default jika type tidak dikenal atau kondisi lain
        }

        return Transaction(
            transactionId = fs.transactionId,
            businessUnitId = fs.businessUnitId, // <-- MAP businessUnitId
            userId = fs.userId,
            type = fs.type,
            amount = fs.amount,
            sellingPrice = fs.sellingPrice,
            description = fs.description,
            date = parsedDate,
            createdAt = fs.createdAt?.toDate() ?: Date(), // Konversi Timestamp ke Date, default jika null
            updatedAt = fs.updatedAt?.toDate(),      // Konversi Timestamp ke Date (bisa null)
            interpretedType = domainInterpretedType
        )
    }

    fun domainToFirestore(domain: Transaction): TransactionFirestore {
        // Saat menyimpan createdAt dan updatedAt, jika kita ingin @ServerTimestamp bekerja,
        // kita perlu mengirim null untuk field tersebut saat membuat item baru atau saat kita ingin server mengisinya.
        // Jika ini adalah update dan kita ingin mempertahankan nilai createdAt yang ada,
        // kita tidak boleh mengirim null untuk createdAt.

        // Logika untuk createdAt:
        // - Jika domain.createdAt adalah nilai default (misalnya, sangat baru atau representasi 'belum diset'), kirim null.
        // - Jika sudah ada nilai valid, konversi ke Timestamp.
        // Untuk kesederhanaan, kita akan selalu mengirim Timestamp dari domain jika ada,
        // dan mengandalkan logika di repository untuk set null pada pembuatan jika perlu.
        // ATAU, kita set null di sini untuk pembuatan baru.

        // Untuk field 'date' (String)
        val dateString = firestoreStringDateFormat.format(domain.date)

        return TransactionFirestore(
            transactionId = domain.transactionId, // Akan diisi/di-override di repository jika perlu
            businessUnitId = domain.businessUnitId, // <-- MAP businessUnitId
            userId = domain.userId,                 // Akan diisi/di-override di repository
            type = domain.type,
            amount = domain.amount,
            sellingPrice = domain.sellingPrice,
            description = domain.description,
            date = dateString,                      // Simpan sebagai String sesuai format
            createdAt = if (domain.transactionId.isBlank()) null else domain.createdAt.let { Timestamp(it) }, // Kirim null untuk baru, atau Timestamp untuk update
            updatedAt = domain.updatedAt?.let { Timestamp(it) } // Selalu kirim Timestamp jika ada, atau null (akan dihandle @ServerTimestamp)
        )
    }
}