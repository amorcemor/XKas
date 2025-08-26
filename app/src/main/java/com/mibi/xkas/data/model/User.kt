package com.mibi.xkas.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class User(
    @DocumentId
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val avatarType: String = "initial", // "initial" atau "preset"
    val avatarValue: String = "", // inisial atau ID preset avatar
    val avatarColor: String = "", // warna hex untuk avatar inisial
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    // Constructor tanpa parameter untuk Firestore
    constructor() : this("", "", "", "initial", "", "", null, null)

    // Helper function untuk konversi ke Map
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "displayName" to displayName,
            "email" to email,
            "avatarType" to avatarType,
            "avatarValue" to avatarValue,
            "avatarColor" to avatarColor,
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
    }
}