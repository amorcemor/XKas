package com.mibi.xkas.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Data class untuk representasi contact dari telepon
 */
data class Contact(
    @DocumentId val contactId: String = "",
    val name: String = "",
    val phoneNumber: String = ""
)