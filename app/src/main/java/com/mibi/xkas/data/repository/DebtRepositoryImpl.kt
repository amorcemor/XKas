package com.mibi.xkas.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mibi.xkas.data.model.Debt
import com.mibi.xkas.data.model.DebtPayment
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class DebtRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : DebtRepository {

    private fun getUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User belum login")
    }

    override fun getAllDebts(userId: String): Flow<List<Debt>> = callbackFlow {
        val ref = firestore.collection("debts")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.toObjects(Debt::class.java) ?: emptyList()
            trySend(list)
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
            val list = snapshot?.toObjects(Debt::class.java) ?: emptyList()
            trySend(list)
        }

        awaitClose { listener.remove() }
    }

    override suspend fun addDebt(debt: Debt): Result<Unit> = runCatching {
        val docRef = firestore.collection("debts").document()
        val newDebt = debt.copy(debtId = docRef.id, userId = getUserId())
        docRef.set(newDebt).await()
    }

    override suspend fun updateDebt(debt: Debt): Result<Unit> = runCatching {
        firestore.collection("debts")
            .document(debt.debtId)
            .set(debt.copy(updatedAt = com.google.firebase.Timestamp.now()))
            .await()
    }

    override suspend fun deleteDebt(debtId: String): Result<Unit> = runCatching {
        firestore.collection("debts").document(debtId).delete().await()
    }

    override suspend fun getDebtById(debtId: String): Result<Debt> = runCatching {
        val doc = firestore.collection("debts")
            .document(debtId)
            .get()
            .await()

        doc.toObject(Debt::class.java) ?: throw Exception("Data hutang tidak ditemukan.")
    }

    override suspend fun addDebtPayment(payment: DebtPayment): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Belum login"))
        return try {
            val ref = firestore.collection("users")
                .document(user.uid)
                .collection("debts")
                .document(payment.debtId)
                .collection("payments")
                .document()

            val paymentWithId = payment.copy(paymentId = ref.id, userId = user.uid)
            ref.set(paymentWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getDebtPayments(debtId: String): Flow<List<DebtPayment>> = callbackFlow {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            close(Exception("User belum login"))
            return@callbackFlow
        }

        val paymentsRef = firestore.collection("users")
            .document(currentUser.uid)
            .collection("debts")
            .document(debtId)
            .collection("payments")
            .orderBy("paymentDate", Query.Direction.DESCENDING)

        val listener = paymentsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val payments = snapshot?.toObjects(DebtPayment::class.java) ?: emptyList()
            trySend(payments).isSuccess
        }

        awaitClose { listener.remove() }
    }

    override fun getDebtByIdFlow(debtId: String): Flow<Result<Debt>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(Result.failure(Exception("User belum login")))
            close()
            return@callbackFlow
        }

        val docRef = firestore.collection("users")
            .document(userId)
            .collection("debts")
            .document(debtId)

        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Result.failure(error))
                return@addSnapshotListener
            }

            val debt = snapshot?.toObject(Debt::class.java)?.copy(debtId = snapshot.id)
            if (debt != null) {
                trySend(Result.success(debt))
            } else {
                trySend(Result.failure(Exception("Debt not found")))
            }
        }

        awaitClose { listener.remove() }
    }


    override fun getDebtPaymentsFlow(debtId: String): Flow<Result<List<DebtPayment>>> {
        val currentUser = auth.currentUser ?: return flowOf(Result.failure(Exception("User not logged in")))
        return callbackFlow {
            val paymentsRef = firestore.collection("users")
                .document(currentUser.uid)
                .collection("debts")
                .document(debtId)
                .collection("payments")
                .orderBy("paidAt", Query.Direction.DESCENDING)

            val listener = paymentsRef.addSnapshotListener { snapshots, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val payments = snapshots?.documents?.mapNotNull { doc ->
                    doc.toObject(DebtPayment::class.java)?.copy(paymentId = doc.id)
                } ?: emptyList()

                trySend(Result.success(payments))
            }

            awaitClose { listener.remove() }
        }
    }


}
