package com.mibi.xkas.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.mibi.xkas.data.model.ContactSource

/**
 * Unified Contact entity yang disimpan di Firestore
 * Single source of truth untuk semua contact data
 */
data class FirestoreContact(
    @DocumentId val contactId: String = "",
    val userId: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val normalizedPhone: String = "",
    val source: String = ContactSource.MANUAL.name,
    val deviceContactId: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

enum class ContactSource {
    MANUAL,     // Dibuat manual di app
    DEVICE,     // Diimport dari device contacts
    SHARED      // Dibuat di device lain (multi-device sync)
}