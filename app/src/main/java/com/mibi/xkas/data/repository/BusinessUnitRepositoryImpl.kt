package com.mibi.xkas.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import com.mibi.xkas.data.BusinessUnit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // Jika Anda menggunakan Hilt
class BusinessUnitRepositoryImpl @Inject constructor( // Jika Anda menggunakan Hilt
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : BusinessUnitRepository {

    private val TAG = "BusinessUnitRepo"
    private val businessUnitCollection = firestore.collection("businessUnit")

    override suspend fun createBusinessUnit(businessUnit: BusinessUnit): Result<String> {
        return try {
            if (businessUnit.userId.isBlank()) {
                Log.e(TAG, "UserID kosong saat membuat BusinessUnit.")
                return Result.failure(IllegalArgumentException("UserID tidak boleh kosong untuk membuat BusinessUnit."))
            }
            // Jika businessUnit.businessUnitId masih kosong, .add() akan membuat ID baru.
            // Firestore KTX akan mengisi @DocumentId pada objek yang dikembalikan jika kita mengambilnya lagi,
            // tapi .add() mengembalikan DocumentReference yang memiliki ID.
            val documentReference = businessUnitCollection.add(businessUnit).await()
            Log.d(TAG, "BusinessUnit dibuat dengan ID: ${documentReference.id}")
            Result.success(documentReference.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error membuat BusinessUnit", e)
            Result.failure(e)
        }
    }

    override fun getUserBusinessUnit(userId: String): Flow<List<BusinessUnit>> = callbackFlow {
        // Log userId yang diterima fungsi
        Log.d(TAG, "BusinessUnitRepositoryImpl: getUserBusinessUnit dipanggil dengan userId: '$userId'")

        if (userId.isBlank()) {
            Log.w(TAG, "UserID kosong saat getBusinessUnit, mengembalikan flow kosong.")
            trySend(emptyList()).isSuccess // Kirim list kosong
            close() // Tutup flow karena tidak valid
            return@callbackFlow
        }

        // Nama koleksi - pastikan ini "businessUnit" atau "businessUnits" SESUAI DENGAN YANG DI FIREBASE CONSOLE & INDEKS
        val currentCollectionName = "businessUnit" // <--- GANTI INI JIKA NAMA KOLEKSI ANDA BERBEDA
        Log.d(TAG, "Mendengarkan BusinessUnit dari koleksi '$currentCollectionName' untuk userID: $userId")

        val listenerRegistration = firestore.collection(currentCollectionName)
            .whereEqualTo("userId", userId) // Filter berdasarkan userId
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    // Log jika ada error dari listener Firestore
                    Log.e(TAG, "BusinessUnitRepositoryImpl: Error listener untuk userID: $userId dari koleksi '$currentCollectionName'", error)
                    close(error) // Tutup flow dengan error
                    return@addSnapshotListener
                }

                if (snapshots == null) {
                    // Log jika snapshot null (seharusnya jarang terjadi jika tidak ada error)
                    Log.d(TAG, "BusinessUnitRepositoryImpl: Snapshot null untuk userID $userId dari koleksi '$currentCollectionName' (tanpa error eksplisit).")
                    trySend(emptyList()).isSuccess
                    return@addSnapshotListener
                }

                // Log jumlah dokumen mentah yang diterima dari Firestore SEBELUM konversi
                Log.d(TAG, "BusinessUnitRepositoryImpl: Snapshot DITERIMA untuk userID $userId dari koleksi '$currentCollectionName'. Jumlah dokumen di snapshot: ${snapshots.size()}")

                // Lakukan konversi ke List<BusinessUnit>
                val units = snapshots.toObjects<BusinessUnit>() // Menggunakan KTX toObjects

                // Log jumlah objek setelah konversi dan data mentah objek Kotlin
                Log.d(TAG, "BusinessUnitRepositoryImpl: Hasil toObjects untuk userID $userId dari koleksi '$currentCollectionName': ${units.size} unit. Data objek: $units")

                // Log tambahan jika hasil konversi kosong padahal snapshot ada isinya (indikasi masalah deserialisasi)
                if (units.isEmpty() && !snapshots.isEmpty) {
                    Log.w(TAG, "BusinessUnitRepositoryImpl: units KOSONG (${units.size}) meskipun snapshot TIDAK KOSONG (${snapshots.size()}) dari koleksi '$currentCollectionName'. Kemungkinan masalah deserialisasi/pemetaan field.")
                    // Cetak detail setiap dokumen mentah dari snapshot jika terjadi kasus di atas
                    snapshots.documents.forEachIndexed { index, documentSnapshot ->
                        Log.w(TAG, "Dokumen mentah [$index] dari snapshot: ID=${documentSnapshot.id}, Data=${documentSnapshot.data}")
                    }
                }
                // Kirim hasil (bisa berupa list kosong jika memang tidak ada data atau konversi gagal)
                trySend(units).isSuccess
            }

        // Menunggu hingga flow ditutup (misalnya, saat scope coroutine pengumpul dibatalkan)
        awaitClose {
            Log.d(TAG, "Menutup listener getUserBusinessUnit untuk userID: $userId dari koleksi '$currentCollectionName'")
            listenerRegistration.remove() // Hapus listener Firestore untuk menghindari memory leak
        }
    }

    override fun getBusinessUnit(businessUnitId: String): Flow<BusinessUnit?> = callbackFlow {
        if (businessUnitId.isBlank()) {
            Log.w(TAG, "businessUnitId kosong, mengembalikan flow null.")
            trySend(null).isSuccess
            awaitClose { }
            return@callbackFlow
        }
        Log.d(TAG, "Mendengarkan BusinessUnit dengan ID: $businessUnitId")
        val listenerRegistration = businessUnitCollection.document(businessUnitId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listener di getBusinessUnit untuk ID: $businessUnitId", error)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val unit = snapshot.toObject<BusinessUnit>()
                    Log.d(TAG, "BusinessUnit diterima untuk ID $businessUnitId: ${unit?.name}")
                    trySend(unit).isSuccess
                } else {
                    Log.d(TAG, "Dokumen BusinessUnit tidak ditemukan untuk ID: $businessUnitId")
                    trySend(null).isSuccess
                }
            }
        awaitClose {
            Log.d(TAG, "Menutup listener getBusinessUnit untuk ID: $businessUnitId")
            listenerRegistration.remove()
        }
    }

    override suspend fun updateBusinessUnit(businessUnit: BusinessUnit): Result<Unit> {
        return try {
            if (businessUnit.businessUnitId.isBlank()) {
                Log.e(TAG, "businessUnitId kosong saat update.")
                return Result.failure(IllegalArgumentException("BusinessUnit ID tidak boleh kosong untuk update."))
            }
            if (businessUnit.userId.isBlank()) { // Tambahan validasi
                Log.e(TAG, "userId kosong saat update BusinessUnit.")
                return Result.failure(IllegalArgumentException("UserID tidak boleh kosong untuk update BusinessUnit."))
            }
            Log.d(TAG, "Mengupdate BusinessUnit dengan ID: ${businessUnit.businessUnitId}")
            businessUnitCollection.document(businessUnit.businessUnitId)
                .set(businessUnit, SetOptions.merge()).await() // Gunakan merge untuk update parsial
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error mengupdate BusinessUnit ID: ${businessUnit.businessUnitId}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteBusinessUnit(businessUnitId: String): Result<Unit> {
        return try {
            if (businessUnitId.isBlank()) {
                Log.e(TAG, "businessUnitId kosong saat delete.")
                return Result.failure(IllegalArgumentException("BusinessUnit ID tidak boleh kosong untuk delete."))
            }
            // PERINGATAN: Ini hanya menghapus dokumen BusinessUnit.
            // Transaksi yang terkait dengan businessUnitId ini TIDAK akan terhapus otomatis.
            // Anda perlu strategi lain jika ingin menghapus/mengarsipkan transaksi tersebut.
            Log.d(TAG, "Menghapus BusinessUnit dengan ID: $businessUnitId")
            businessUnitCollection.document(businessUnitId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error menghapus BusinessUnit ID: $businessUnitId", e)
            Result.failure(e)
        }
    }
}
