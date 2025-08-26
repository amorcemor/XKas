package com.mibi.xkas.data.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.mibi.xkas.data.model.User
import com.mibi.xkas.utils.AvatarUtils
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    companion object {
        private const val TAG = "UserRepository"
    }

    /**
     * Membuat profile user baru di Firestore setelah registrasi
     */
    suspend fun createUserProfile(
        userId: String,
        displayName: String,
        email: String,
        avatarType: String = "initial",
        avatarValue: String = "",
        avatarColor: String = ""
    ): Result<Unit> {
        return try {
            val user = User(
                userId = userId,
                displayName = displayName,
                email = email,
                avatarType = avatarType,
                avatarValue = avatarValue,
                avatarColor = avatarColor,
                createdAt = null, // Will be set by @ServerTimestamp
                updatedAt = null  // Will be set by @ServerTimestamp
            )

            firestore.collection("users")
                .document(userId)
                .set(user)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mengambil data user dari Firestore
     */
    suspend fun getUserProfile(userId: String): Result<User?> {
        return try {
            val document = usersCollection.document(userId).get().await()
            val user = document.toObject(User::class.java)
            Log.d(TAG, "User profile retrieved: $userId")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user profile", e)
            Result.failure(e)
        }
    }

    /**
     * Update profile user di Firestore dan Firebase Auth
     */
    suspend fun updateUserProfile(
        userId: String,
        displayName: String,
        avatarType: String,
        avatarValue: String,
        avatarColor: String
    ): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any>(
                "displayName" to displayName,
                "avatarType" to avatarType,
                "avatarValue" to avatarValue,
                "avatarColor" to avatarColor,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            firestore.collection("users")
                .document(userId)
                .update(updates)
                .await()

            // Also update Firebase Auth profile
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                currentUser.updateProfile(profileUpdates).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update avatar user (initial atau preset)
     */
    suspend fun updateUserAvatar(
        userId: String,
        avatarType: String, // "initial" atau "preset"
        avatarValue: String, // inisial atau preset ID
        avatarColor: String? = null // hanya untuk type "initial"
    ): Result<String> {
        return try {
            Log.d(TAG, "Updating avatar for user: $userId, type: $avatarType, value: $avatarValue")

            val updateData = mutableMapOf<String, Any>(
                "avatarType" to avatarType,
                "avatarValue" to avatarValue,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            // Tambahkan warna jika avatar type adalah initial
            if (avatarType == "initial" && avatarColor != null) {
                updateData["avatarColor"] = avatarColor
            }

            usersCollection.document(userId).update(updateData).await()
            Log.d(TAG, "Avatar updated successfully for user: $userId")

            Result.success("Avatar berhasil diupdate")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating avatar", e)
            Result.failure(e)
        }
    }
}