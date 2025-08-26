package com.mibi.xkas.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mibi.xkas.data.Transaction
import com.mibi.xkas.data.TransactionFirestore
import com.mibi.xkas.data.TransactionMappers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    private val mappers: TransactionMappers = TransactionMappers // Tetap
    private val TAG = "TransactionRepository"

    // --- Fungsi untuk menyimpan transaksi baru ---
    suspend fun saveTransaction(transaction: Transaction): Result<String> {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            return Result.failure(Exception("Pengguna belum login untuk menyimpan transaksi."))
        }
        // Validasi tambahan: businessUnitId tidak boleh kosong
        if (transaction.businessUnitId.isBlank()) {
            Log.e(TAG, "BusinessUnitID kosong saat menyimpan transaksi.")
            return Result.failure(IllegalArgumentException("BusinessUnitID tidak boleh kosong untuk transaksi."))
        }

        return try {
            val transactionsCollection = db.collection("users")
                .document(currentUser.uid)
                .collection("transactions")

            val documentReference = transactionsCollection.document() // Buat ID baru

            val finalFsTransaction = mappers.domainToFirestore(transaction.copy(
                transactionId = documentReference.id,
                userId = currentUser.uid
            ))

            documentReference.set(finalFsTransaction).await()
            Log.d(TAG, "Transaksi disimpan untuk BU: ${transaction.businessUnitId}, TransID: ${documentReference.id}")
            Result.success(documentReference.id) // ← Return ID transaksi (String)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving transaction", e)
            Result.failure(e)
        }
    }

    // --- getUserTransactionsFlow DIUBAH menjadi getTransactionsForBusinessUnitFlow ---
    fun getTransactionsForBusinessUnitFlow(businessUnitId: String): Flow<Result<List<Transaction>>> {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            return flowOf(Result.failure(Exception("Pengguna belum login.")))
        }
        if (businessUnitId.isBlank()) {
            Log.w(TAG, "businessUnitId kosong untuk getTransactionsForBusinessUnitFlow.")
            return flowOf(Result.success(emptyList())) // Atau failure, tergantung kebutuhan
        }

        return callbackFlow {
            Log.d(TAG, "Mendengarkan transaksi untuk User: ${currentUser.uid}, BU: $businessUnitId")
            val query = db.collection("users")
                .document(currentUser.uid)
                .collection("transactions")
                .whereEqualTo("businessUnitId", businessUnitId) // <-- FILTER BERDASARKAN businessUnitId
                .orderBy("date", Query.Direction.DESCENDING)
                .orderBy("createdAt", Query.Direction.DESCENDING)

            val listenerRegistration = query.addSnapshotListener { querySnapshot, firestoreException ->
                if (firestoreException != null) {
                    Log.e(TAG, "Firestore error in getTransactionsForBusinessUnitFlow for BU: $businessUnitId", firestoreException)
                    trySend(Result.failure(firestoreException))
                    return@addSnapshotListener
                }

                if (querySnapshot != null) {
                    val transactionsListDomain = mutableListOf<Transaction>()
                    for (document in querySnapshot.documents) {
                        try {
                            val fsTransaction = document.toObject(TransactionFirestore::class.java)
                            if (fsTransaction != null) {
                                // transactionId sudah di-copy di fsTransaction dari TransactionFirestore.kt jika @DocumentId digunakan
                                // atau kita bisa set manual dari document.id jika tidak.
                                // Untuk konsistensi dengan kode lama Anda:
                                val fsTransactionWithId = fsTransaction.copy(transactionId = document.id)
                                transactionsListDomain.add(mappers.firestoreToDomain(fsTransactionWithId))
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing document ${document.id} in getTransactionsForBusinessUnitFlow", e)
                            // Pertimbangkan untuk tidak menghentikan seluruh flow karena satu dokumen error
                        }
                    }
                    Log.d(TAG, "Transaksi diterima untuk BU $businessUnitId: ${transactionsListDomain.size} transaksi.")
                    trySend(Result.success(transactionsListDomain))
                } else {
                    trySend(Result.success(emptyList()))
                }
            }
            awaitClose {
                Log.d(TAG, "Menutup listener getTransactionsForBusinessUnitFlow untuk BU: $businessUnitId")
                listenerRegistration.remove()
            }
        }
    }


    // --- getUserTransactions DIUBAH menjadi getTransactionsForBusinessUnit ---
    suspend fun getTransactionsForBusinessUnit(businessUnitId: String): Result<List<Transaction>> {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            return Result.failure(Exception("Pengguna belum login."))
        }
        if (businessUnitId.isBlank()) {
            Log.w(TAG, "businessUnitId kosong untuk getTransactionsForBusinessUnit.")
            return Result.success(emptyList()) // Atau failure
        }

        Log.d(TAG, "Mengambil transaksi (one-shot) untuk User: ${currentUser.uid}, BU: $businessUnitId")
        return try {
            val querySnapshot = db.collection("users")
                .document(currentUser.uid)
                .collection("transactions")
                .whereEqualTo("businessUnitId", businessUnitId) // <-- FILTER BERDASARKAN businessUnitId
                .orderBy("date", Query.Direction.DESCENDING)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val transactionsDomain = querySnapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(TransactionFirestore::class.java)?.let { fsTransaction ->
                        val fsTransactionWithId = fsTransaction.copy(transactionId = document.id)
                        mappers.firestoreToDomain(fsTransactionWithId)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing document ${document.id} in getTransactionsForBusinessUnit", e)
                    null
                }
            }
            Log.d(TAG, "Transaksi (one-shot) diterima untuk BU $businessUnitId: ${transactionsDomain.size} transaksi.")
            Result.success(transactionsDomain)
        } catch (e: Exception) {
            Log.e(TAG, "Error in getTransactionsForBusinessUnit for BU: $businessUnitId", e)
            Result.failure(e)
        }
    }

    fun getTransactionById(transactionId: String): Flow<Transaction?> {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w(TAG, "getTransactionById: Pengguna belum login.")
            return flowOf(null)
        }
        if (transactionId.isBlank()) {
            Log.w(TAG, "getTransactionById: Transaction ID kosong atau tidak valid.")
            return flowOf(null)
        }
        return callbackFlow {
            Log.d(TAG, "getTransactionById: Membuat listener untuk transactionId: $transactionId")
            val documentRef = db.collection("users")
                .document(currentUser.uid)
                .collection("transactions")
                .document(transactionId)

            val listenerRegistration = documentRef.addSnapshotListener { documentSnapshot, firestoreException ->
                if (firestoreException != null) {
                    Log.e(TAG, "Error listening to transaction $transactionId", firestoreException)
                    close(firestoreException) // Menutup flow dengan error
                    return@addSnapshotListener
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    try {
                        val fsTransaction = documentSnapshot.toObject(TransactionFirestore::class.java)
                        val domainTransaction = fsTransaction?.let {
                            val fsTransactionWithCorrectId = it.copy(transactionId = documentSnapshot.id)
                            mappers.firestoreToDomain(fsTransactionWithCorrectId)
                        }
                        trySend(domainTransaction).isSuccess
                        Log.d(TAG, "getTransactionById: Data diterima untuk $transactionId. Data: $domainTransaction")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing transaction $transactionId", e)
                        close(e) // Menutup flow dengan error
                    }
                } else {
                    Log.d(TAG, "getTransactionById: Dokumen $transactionId tidak ditemukan.")
                    trySend(null).isSuccess
                }
            }
            awaitClose {
                Log.d(TAG, "getTransactionById: Closing Firestore listener for $transactionId")
                listenerRegistration.remove()
            }
        }
    }


    // --- updateTransaction ---
    suspend fun updateTransaction(transaction: Transaction): Result<Unit> {
        val currentUser = auth.currentUser
        if (currentUser == null || transaction.transactionId.isBlank()) {
            return Result.failure(Exception("Data tidak valid untuk update transaksi."))
        }
        // Validasi tambahan: businessUnitId tidak boleh kosong
        if (transaction.businessUnitId.isBlank()) {
            Log.e(TAG, "BusinessUnitID kosong saat mengupdate transaksi.")
            return Result.failure(IllegalArgumentException("BusinessUnitID tidak boleh kosong untuk transaksi."))
        }

        // Mapper akan menyertakan businessUnitId dari objek domain
        val fsTransactionToUpdate = mappers.domainToFirestore(transaction)
        return try {
            // Pastikan transactionId dan userId dari domain yang diutamakan (atau setidaknya userId dari auth)
            val finalFsTransaction = fsTransactionToUpdate.copy(
                userId = currentUser.uid, // Selalu gunakan userId dari sesi saat ini
                transactionId = transaction.transactionId // Pastikan ID dari domain tidak berubah
                // businessUnitId sudah di-set oleh mapper
            )
            db.collection("users")
                .document(currentUser.uid)
                .collection("transactions")
                .document(transaction.transactionId)
                .set(finalFsTransaction) // .set() akan menimpa seluruh dokumen. Jika ingin merge, gunakan SetOptions.merge()
                .await()
            Log.d(TAG, "Transaksi diupdate: ${transaction.transactionId} untuk BU: ${transaction.businessUnitId}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating transaction ${transaction.transactionId}", e)
            Result.failure(e)
        }
    }

    // --- deleteTransaction ---
    // Tidak perlu diubah secara fungsional, karena penghapusan berdasarkan transactionId unik.
    suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        val currentUser = auth.currentUser
        if (currentUser == null || transactionId.isBlank()) {
            return Result.failure(Exception("Data tidak valid untuk menghapus transaksi."))
        }
        return try {
            db.collection("users")
                .document(currentUser.uid)
                .collection("transactions")
                .document(transactionId)
                .delete()
                .await()
            Log.d(TAG, "Transaksi dihapus: $transactionId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting transaction $transactionId", e)
            Result.failure(e)
        }
    }

    suspend fun deleteTransactionAndRelatedDebt(transactionId: String): Result<Unit> {
        val currentUser = auth.currentUser ?: return Result.failure(Exception("User belum login."))

        return try {
            // 1. Hapus transaksi
            val deleteTransactionTask = db.collection("users")
                .document(currentUser.uid)
                .collection("transactions")
                .document(transactionId)
                .delete()

            // 2. Cari dan hapus debt yang terkait
            val debtQuerySnapshot = db.collection("debts")
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("transactionId", transactionId)
                .get()
                .await()

            val deleteDebtTasks = debtQuerySnapshot.documents.map { doc ->
                db.collection("debts").document(doc.id).delete()
            }

            // Tunggu semua penghapusan selesai
            deleteTransactionTask.await()
            deleteDebtTasks.forEach { it.await() }

            Log.d(TAG, "Transaksi dan hutang terkait berhasil dihapus. Transaksi ID: $transactionId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menghapus transaksi dan/atau hutang: $transactionId", e)
            Result.failure(e)
        }
    }
}
