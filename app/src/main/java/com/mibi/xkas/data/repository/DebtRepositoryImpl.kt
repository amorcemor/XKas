package com.mibi.xkas.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mibi.xkas.data.model.Debt
import com.mibi.xkas.data.model.DebtPayment
import com.mibi.xkas.model.ContactDebtSummary
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.*


class DebtRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : DebtRepository {

    private fun Debt.getDebtDirection(): String {
        return if (this.debtDirection.isBlank()) "CUSTOMER_OWES" else this.debtDirection
    }
    private fun getUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User belum login")
    }

    // === HUTANG ===
    override fun getAllDebts(userId: String): Flow<List<Debt>> = callbackFlow {
        val ref = firestore.collection("debts")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObjects(Debt::class.java) ?: emptyList())
        }

        awaitClose { listener.remove() }
    }

    override fun getDebtsByBusinessUnit(userId: String, businessUnitId: String): Flow<List<Debt>> = callbackFlow {
        val ref = firestore.collection("debts")
            .whereEqualTo("userId", userId)
            .whereEqualTo("businessUnitId", businessUnitId)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObjects(Debt::class.java) ?: emptyList())
        }

        awaitClose { listener.remove() }
    }

    override suspend fun addDebt(debt: Debt): Result<Unit> = runCatching {
        val docRef = firestore.collection("debts").document()
        val newDebt = debt.copy(debtId = docRef.id, userId = getUserId())
        docRef.set(newDebt).await()
    }

    override suspend fun updateDebt(debt: Debt): Result<Unit> = runCatching {
        if (debt.debtId.isBlank()) throw IllegalArgumentException("Debt ID tidak boleh kosong")
        firestore.collection("debts").document(debt.debtId).set(debt).await()
    }

    override suspend fun deleteDebt(debtId: String): Result<Unit> = runCatching {
        firestore.collection("debts").document(debtId).delete().await()
    }

    override suspend fun getDebtById(debtId: String): Result<Debt> = runCatching {
        val doc = firestore.collection("debts").document(debtId).get().await()
        doc.toObject(Debt::class.java) ?: throw Exception("Data hutang tidak ditemukan.")
    }

    override fun getDebtByIdFlow(debtId: String): Flow<Result<Debt>> = callbackFlow {
        if (debtId.isBlank()) {
            trySend(Result.failure(Exception("Debt ID is empty")))
            close()
            return@callbackFlow
        }

        val docRef = firestore.collection("debts").document(debtId)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Result.failure(error))
                return@addSnapshotListener
            }

            val debt = snapshot?.toObject(Debt::class.java)
            if (debt != null) trySend(Result.success(debt))
            else trySend(Result.failure(Exception("Debt not found")))
        }

        awaitClose { listener.remove() }
    }

    override suspend fun getDebtByTransactionId(transactionId: String, userId: String): Debt? {
        return try {
            val querySnapshot = firestore.collection("debts")
                .whereEqualTo("userId", userId)
                .whereEqualTo("transactionId", transactionId)
                .limit(1)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                val document = querySnapshot.documents.first()
                document.toObject(Debt::class.java)?.copy(debtId = document.id)
            } else null
        } catch (e: Exception) {
            Log.e("DebtRepository", "Error getting debt by transaction ID: $transactionId", e)
            null
        }
    }

    // === PEMBAYARAN ===
    override suspend fun addDebtPayment(payment: DebtPayment): Result<Unit> = runCatching {
        val user = auth.currentUser ?: throw IllegalStateException("User belum login")

        val debtRef = firestore.collection("debts").document(payment.debtId)
        val paymentRef = debtRef.collection("payments").document()

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(debtRef)
            val currentDebt = snapshot.toObject(Debt::class.java)
                ?: throw IllegalStateException("Dokumen utang tidak ditemukan.")

            val newPaidAmount = currentDebt.paidAmount + payment.amount
            if (newPaidAmount > currentDebt.totalAmount) {
                throw IllegalStateException("Jumlah pembayaran melebihi sisa utang.")
            }

            val paymentWithId = payment.copy(
                paymentId = paymentRef.id,
                userId = user.uid,
                paidAt = Timestamp.now()
            )

            transaction.set(paymentRef, paymentWithId)
            transaction.update(debtRef, mapOf(
                "paidAmount" to newPaidAmount,
                "updatedAt" to Timestamp.now()
            ))
        }.await()
    }

    override suspend fun deleteDebtPayment(debtId: String, paymentId: String, amount: Double): Result<Unit> = runCatching {
        val user = auth.currentUser ?: throw IllegalStateException("User belum login")

        val debtRef = firestore.collection("debts").document(debtId)
        val paymentRef = debtRef.collection("payments").document(paymentId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(debtRef)
            val currentDebt = snapshot.toObject(Debt::class.java)
                ?: throw IllegalStateException("Dokumen utang tidak ditemukan.")

            val paymentSnapshot = transaction.get(paymentRef)
            if (!paymentSnapshot.exists()) throw IllegalStateException("Pembayaran tidak ditemukan.")

            val newPaidAmount = currentDebt.paidAmount - amount
            if (newPaidAmount < 0) throw IllegalStateException("Saldo tidak valid.")

            transaction.delete(paymentRef)
            transaction.update(debtRef, mapOf(
                "paidAmount" to newPaidAmount,
                "updatedAt" to Timestamp.now()
            ))
        }.await()
    }

    override fun getDebtPayments(debtId: String): Flow<List<DebtPayment>> = callbackFlow {
        val paymentsRef = firestore.collection("debts")
            .document(debtId)
            .collection("payments")
            .orderBy("paidAt", Query.Direction.DESCENDING)

        val listener = paymentsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val payments = snapshot?.toObjects(DebtPayment::class.java) ?: emptyList()
            trySend(payments)
        }

        awaitClose { listener.remove() }
    }

    override fun getDebtPaymentsFlow(debtId: String): Flow<Result<List<DebtPayment>>> = callbackFlow {
        val paymentsRef = firestore.collection("debts")
            .document(debtId)
            .collection("payments")
            .orderBy("paidAt", Query.Direction.DESCENDING)

        val listener = paymentsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Result.failure(error))
                return@addSnapshotListener
            }

            val payments = snapshot?.toObjects(DebtPayment::class.java) ?: emptyList()
            trySend(Result.success(payments))
        }

        awaitClose { listener.remove() }
    }

    override fun getDebtsByContact(userId: String, contactId: String): Flow<List<Debt>> = callbackFlow {
        val ref = firestore.collection("debts")
            .whereEqualTo("userId", userId)
            .whereEqualTo("contactId", contactId)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val debts = snapshot?.toObjects(Debt::class.java) ?: emptyList()
            trySend(debts)
        }

        awaitClose { listener.remove() }
    }

    override suspend fun addManualDebt(debt: Debt): Result<String> = runCatching {
        val docRef = firestore.collection("debts").document()
        val newDebt = debt.copy(debtId = docRef.id, userId = getUserId())
        docRef.set(newDebt).await()
        updateContactTotals(debt.contactId)
        return@runCatching docRef.id
    }

    override suspend fun updateContactTotals(contactId: String): Result<Unit> = runCatching {
        val userId = getUserId()

        val debts = firestore.collection("debts")
            .whereEqualTo("userId", userId)
            .whereEqualTo("contactId", contactId)
            .get()
            .await()
            .toObjects(Debt::class.java)

        val totalAmount = debts.sumOf { it.totalAmount }
        val paidAmount = debts.sumOf { it.paidAmount }

        val latestDate: Timestamp = debts.maxOfOrNull { debt ->
            debt.updatedAt ?: debt.createdAt
        } ?: Timestamp.now()

        // Kalau kamu butuh mengembalikan hasil, sebaiknya buat method lain yang mengembalikan data,
        // karena sekarang tidak ada target penulisan. Bisa diubah signature kalau perlu.
        Log.d(
            "DebtRepo",
            "Contact $contactId totals => totalAmount=$totalAmount, paidAmount=$paidAmount, latest=$latestDate"
        )
    }

    // ✅ MODIFIKASI UTAMA: Hapus filter hasActiveDebt agar semua kontak ditampilkan
    override fun getGroupedDebtsByContact(userId: String): Flow<List<ContactDebtSummary>> = callbackFlow {
        val ref = firestore.collection("debts")
            .whereEqualTo("userId", userId)

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("DebtRepo", "Firestore error: ${error.message}")
                close(error)
                return@addSnapshotListener
            }

            val debtList = snapshot?.toObjects(Debt::class.java).orEmpty()
            val filteredDebts = debtList.filter { it.contactId.isNotBlank() }

            val grouped = filteredDebts.groupBy { it.contactId }

            val summaries = grouped.map { (contactId, debts) ->
                val lastTransactionDate: Timestamp? = debts.maxOfOrNull { debt ->
                    debt.updatedAt ?: debt.createdAt
                }

                // hitung total outstanding
                val customerOwes = debts.filter { it.debtDirection.ifBlank { "CUSTOMER_OWES" } == "CUSTOMER_OWES" }
                    .sumOf { maxOf(0.0, it.totalAmount - it.paidAmount) }

                val businessOwes = debts.filter { it.debtDirection.ifBlank { "CUSTOMER_OWES" } == "BUSINESS_OWES" }
                    .sumOf { maxOf(0.0, it.totalAmount - it.paidAmount) }

                ContactDebtSummary(
                    contactId = contactId,
                    contact = null, // 🔑 akan diisi di ViewModel
                    debts = debts,
                    payments = emptyList(),
                    totalOutstanding = customerOwes - businessOwes,
                    lastTransactionDate = lastTransactionDate
                )
            }.sortedWith(
                compareByDescending<ContactDebtSummary> { it.hasActiveDebt }
                    .thenByDescending { it.lastTransactionDate }
            )

            trySend(summaries)
        }

        awaitClose { listener.remove() }
    }


    override suspend fun getContactSummary(contactId: String): Result<ContactDebtSummary> = runCatching {
        val userId = getUserId()
        val debtsSnap = firestore.collection("debts")
            .whereEqualTo("userId", userId)
            .whereEqualTo("contactId", contactId)
            .get()
            .await()

        val debts = debtsSnap.toObjects(Debt::class.java)

        if (debts.isEmpty()) throw Exception("Data utang untuk kontak ini tidak ditemukan.")

        val payments = debts.flatMap { debt ->
            firestore.collection("debts")
                .document(debt.debtId)
                .collection("payments")
                .get()
                .await()
                .toObjects(DebtPayment::class.java)
        }

        val totalOutstanding = debts.sumOf { it.totalAmount - it.paidAmount }
        val lastTransactionDate: Timestamp? = debts.maxOfOrNull { it.updatedAt ?: it.createdAt }

        ContactDebtSummary(
            contactId = contactId,
            contact = null, // 🔑 biar konsisten, isi di ViewModel
            debts = debts,
            payments = payments,
            totalOutstanding = totalOutstanding,
            lastTransactionDate = lastTransactionDate
        )
    }

    // ✅ PERBAIKAN PADA getContactSummaryFlow untuk memastikan real-time updates

    override fun getContactSummaryFlow(contactId: String): Flow<Result<ContactDebtSummary>> {
        val userId = getUserId()

        val debtsFlow = callbackFlow {
            val ref = firestore.collection("debts")
                .whereEqualTo("userId", userId)
                .whereEqualTo("contactId", contactId)
                .orderBy("createdAt", Query.Direction.DESCENDING)

            val listener = ref.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure<List<Debt>>(error))
                    return@addSnapshotListener
                }
                val debts = snapshot?.toObjects(Debt::class.java) ?: emptyList()
                trySend(Result.success(debts))
            }

            awaitClose { listener.remove() }
        }

        val paymentsFlow = debtsFlow.flatMapLatest { debtsResult ->
            when {
                debtsResult.isFailure -> flowOf(Result.failure(debtsResult.exceptionOrNull()!!))
                debtsResult.getOrNull()?.isEmpty() == true -> flowOf(Result.success(emptyList<DebtPayment>()))
                else -> {
                    val debts = debtsResult.getOrNull()!!
                    val paymentFlows = debts.map { debt ->
                        callbackFlow {
                            val paymentsRef = firestore.collection("debts")
                                .document(debt.debtId)
                                .collection("payments")
                                .orderBy("paidAt", Query.Direction.DESCENDING)

                            val listener = paymentsRef.addSnapshotListener { snapshot, error ->
                                if (error != null) {
                                    trySend(emptyList<DebtPayment>())
                                    return@addSnapshotListener
                                }
                                val payments = snapshot?.toObjects(DebtPayment::class.java) ?: emptyList()
                                trySend(payments)
                            }

                            awaitClose { listener.remove() }
                        }
                    }

                    if (paymentFlows.isEmpty()) {
                        flowOf(Result.success(emptyList()))
                    } else {
                        combine(paymentFlows) { arrays ->
                            Result.success(arrays.flatMap { it })
                        }
                    }
                }
            }
        }

        return combine(debtsFlow, paymentsFlow) { debtsResult, paymentsResult ->
            try {
                when {
                    debtsResult.isFailure -> debtsResult as Result<ContactDebtSummary>
                    paymentsResult.isFailure -> paymentsResult as Result<ContactDebtSummary>
                    else -> {
                        val debts = debtsResult.getOrNull() ?: emptyList()
                        val payments = paymentsResult.getOrNull() ?: emptyList()

                        if (debts.isEmpty()) {
                            Result.failure(Exception("Data utang untuk kontak ini tidak ditemukan."))
                        } else {
                            val firstDebt = debts.first()
                            val totalOutstanding = debts.sumOf { it.totalAmount - it.paidAmount }
                            val lastTransactionDate = debts.maxOfOrNull { it.updatedAt ?: it.createdAt }

                            val summary = ContactDebtSummary(
                                contactId = firstDebt.contactId,
                                contact = null, // 🔑 diisi di ViewModel
                                debts = debts,
                                payments = payments,
                                totalOutstanding = totalOutstanding,
                                lastTransactionDate = lastTransactionDate
                            )

                            Result.success(summary)
                        }
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    // Tambahkan fungsi lain di bawah ini jika perlu
}