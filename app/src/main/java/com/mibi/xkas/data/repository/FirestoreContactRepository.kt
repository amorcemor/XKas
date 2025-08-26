package com.mibi.xkas.data.repository

import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mibi.xkas.data.model.Contact
import com.mibi.xkas.data.model.ContactSource
import com.mibi.xkas.data.model.FirestoreContact
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreContactRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) {

    private val TAG = "FirestoreContactRepo"
    private val CONTACTS_COLLECTION = "contacts"

    private fun getUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User belum login")
    }

    // ============================================================================
    // MAIN FUNCTIONS - Firestore First
    // ============================================================================

    /**
     * ✅ UTAMA: Load semua contacts dari Firestore (real-time sync)
     */
    fun getUserContacts(userId: String = getUserId()): Flow<List<FirestoreContact>> = callbackFlow {
        val ref = firestore.collection(CONTACTS_COLLECTION)
            .whereEqualTo("userId", userId)
            .orderBy("name")

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error loading user contacts", error)
                close(error)
                return@addSnapshotListener
            }

            val contacts = snapshot?.toObjects(FirestoreContact::class.java) ?: emptyList()
            trySend(contacts)
        }

        awaitClose { listener.remove() }
    }

    /**
     * ✅ SEARCH: Real-time search dari Firestore
     */
    fun searchContacts(userId: String = getUserId(), query: String): Flow<List<FirestoreContact>> {
        return getUserContacts(userId).map { contacts ->
            if (query.isBlank()) {
                contacts
            } else {
                contacts.filter { contact ->
                    contact.name.contains(query, ignoreCase = true) ||
                            contact.phoneNumber.contains(query) ||
                            contact.normalizedPhone.contains(normalizePhone(query))
                }
            }
        }
    }

    /**
     * ✅ CREATE: Buat contact baru dengan optional duplication check
     */
    suspend fun createContact(
        userId: String = getUserId(),
        name: String,
        phone: String,
        checkDuplication: Boolean = true
    ): Result<FirestoreContact> = withContext(Dispatchers.IO) {
        try {
            val normalized = normalizePhone(phone)
            val trimmedName = name.trim()

            // Optional: Check for duplicates
            if (checkDuplication) {
                val existing = findSimilarContact(userId, trimmedName, normalized)
                if (existing != null) {
                    Log.i(TAG, "Found similar contact, returning existing: ${existing.name}")
                    return@withContext Result.success(existing)
                }
            }

            // Create new contact
            val docRef = firestore.collection(CONTACTS_COLLECTION).document()
            val contact = FirestoreContact(
                contactId = docRef.id,
                userId = userId,
                name = trimmedName,
                phoneNumber = phone.trim(),
                normalizedPhone = normalized,
                source = ContactSource.MANUAL.name
            )

            docRef.set(contact).await()
            Log.i(TAG, "Created new contact: ${contact.name}")

            Result.success(contact)

        } catch (e: Exception) {
            Log.e(TAG, "Error creating contact", e)
            Result.failure(e)
        }
    }

    /**
     * ✅ FIND OR CREATE: Find existing atau create baru
     */
    suspend fun findOrCreateContact(
        userId: String = getUserId(),
        name: String,
        phone: String
    ): Result<FirestoreContact> = withContext(Dispatchers.IO) {
        val normalized = normalizePhone(phone)

        // Try to find existing first
        val existing = findSimilarContact(userId, name.trim(), normalized)
        if (existing != null) {
            return@withContext Result.success(existing)
        }

        // Create new if not found
        return@withContext createContact(userId, name, phone, checkDuplication = false)
    }

    // ============================================================================
    // DEVICE CONTACT IMPORT (Optional per-device)
    // ============================================================================

    /**
     * ✅ IMPORT: Import contacts dari device ke Firestore
     */
    suspend fun importDeviceContacts(
        userId: String = getUserId(),
        selectedContacts: List<Contact>? = null // null = import all
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val deviceContacts = selectedContacts ?: getDeviceContactsLocal()
            val existingContacts = getFirestoreContactsOnce(userId)

            var importedCount = 0

            deviceContacts.forEach { deviceContact ->
                val normalized = normalizePhone(deviceContact.phoneNumber)

                // Check duplikasi dengan existing Firestore contacts
                val isDuplicate = existingContacts.any { existing ->
                    existing.normalizedPhone == normalized ||
                            existing.name.equals(deviceContact.name, ignoreCase = true) ||
                            existing.deviceContactId == deviceContact.contactId
                }

                if (!isDuplicate && deviceContact.name.isNotBlank() && deviceContact.phoneNumber.isNotBlank()) {
                    val firestoreContact = FirestoreContact(
                        userId = userId,
                        name = deviceContact.name.trim(),
                        phoneNumber = deviceContact.phoneNumber,
                        normalizedPhone = normalized,
                        source = ContactSource.DEVICE.name,
                        deviceContactId = deviceContact.contactId
                    )

                    val docRef = firestore.collection(CONTACTS_COLLECTION).document()
                    val contactWithId = firestoreContact.copy(contactId = docRef.id)

                    docRef.set(contactWithId).await()
                    importedCount++
                }
            }

            Log.i(TAG, "Imported $importedCount contacts from device")
            Result.success(importedCount)

        } catch (e: Exception) {
            Log.e(TAG, "Error importing device contacts", e)
            Result.failure(e)
        }
    }

    /**
     * ✅ DEVICE CONTACTS: Get contacts dari device (local only)
     */
    suspend fun getDeviceContacts(): List<Contact> = withContext(Dispatchers.IO) {
        getDeviceContactsLocal()
    }

    // ============================================================================
    // HELPER FUNCTIONS
    // ============================================================================

    private suspend fun getDeviceContactsLocal(): List<Contact> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<Contact>()
        val contentResolver: ContentResolver = context.contentResolver

        try {
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )

            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameColumn = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val phoneColumn = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (it.moveToNext()) {
                    val cid = it.getString(idColumn)
                    val name = it.getString(nameColumn) ?: ""
                    val phone = it.getString(phoneColumn) ?: ""
                    val cleanPhone = phone.replace(Regex("[^+\\d]"), "")

                    if (name.isNotBlank() && cleanPhone.isNotBlank()) {
                        contacts.add(Contact(contactId = cid, name = name, phoneNumber = cleanPhone))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading device contacts", e)
        }

        contacts.distinctBy { "${it.name}_${it.phoneNumber}" }
    }

    private suspend fun getFirestoreContactsOnce(userId: String): List<FirestoreContact> {
        return try {
            firestore.collection(CONTACTS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .toObjects(FirestoreContact::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting firestore contacts", e)
            emptyList()
        }
    }

    private suspend fun findSimilarContact(
        userId: String,
        name: String,
        normalizedPhone: String
    ): FirestoreContact? {
        return try {
            val contacts = getFirestoreContactsOnce(userId)

            // 1. Exact phone match (priority)
            contacts.find { it.normalizedPhone == normalizedPhone }
                ?:
                // 2. Exact name match with similar phone
                contacts.find {
                    it.name.equals(name, ignoreCase = true) &&
                            isSimilarPhone(it.normalizedPhone, normalizedPhone)
                }
                ?:
                // 3. Very similar name with exact phone
                contacts.find {
                    calculateNameSimilarity(it.name, name) > 0.8 &&
                            it.normalizedPhone == normalizedPhone
                }

        } catch (e: Exception) {
            Log.e(TAG, "Error finding similar contact", e)
            null
        }
    }

    private fun normalizePhone(phone: String): String {
        // Simple but effective normalization
        return phone.replace(Regex("[^\\d+]"), "")
            .replace(Regex("^(\\+?62)"), "62")
            .replace(Regex("^0"), "62")
    }

    private fun isSimilarPhone(phone1: String, phone2: String): Boolean {
        // Remove all non-digits and compare last 8-10 digits
        val clean1 = phone1.replace(Regex("[^\\d]"), "")
        val clean2 = phone2.replace(Regex("[^\\d]"), "")

        return clean1.takeLast(8) == clean2.takeLast(8)
    }

    private fun calculateNameSimilarity(name1: String, name2: String): Double {
        // Simple Levenshtein distance implementation
        val s1 = name1.lowercase()
        val s2 = name2.lowercase()

        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                dp[i][j] = if (s1[i - 1] == s2[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }

        val maxLen = maxOf(s1.length, s2.length)
        return if (maxLen == 0) 1.0 else 1.0 - (dp[s1.length][s2.length].toDouble() / maxLen)
    }

    // ============================================================================
    // ADDITIONAL FUNCTIONS (Optional)
    // ============================================================================

    suspend fun updateContact(contact: FirestoreContact): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updatedContact = contact.copy(updatedAt = com.google.firebase.Timestamp.now())
            firestore.collection(CONTACTS_COLLECTION)
                .document(contact.contactId)
                .set(updatedContact)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating contact", e)
            Result.failure(e)
        }
    }

    suspend fun deleteContact(contactId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection(CONTACTS_COLLECTION)
                .document(contactId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting contact", e)
            Result.failure(e)
        }
    }

    suspend fun getContactById(contactId: String): Result<FirestoreContact> = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection(CONTACTS_COLLECTION)
                .document(contactId)
                .get()
                .await()

            val contact = doc.toObject(FirestoreContact::class.java)
                ?: throw Exception("Contact not found")

            Result.success(contact)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting contact by ID", e)
            Result.failure(e)
        }
    }
}