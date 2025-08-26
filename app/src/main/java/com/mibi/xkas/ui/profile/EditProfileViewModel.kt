package com.mibi.xkas.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.mibi.xkas.data.repository.UserRepository
import com.mibi.xkas.utils.AvatarUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.mibi.xkas.data.repository.TransactionRepository
import com.mibi.xkas.data.repository.DebtRepositoryImpl
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// State untuk UI edit profile
data class EditProfileUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val originalName: String = "",
    val originalEmail: String = "",
    val originalAvatarType: String = "initial",
    val originalAvatarValue: String = "",
    val originalAvatarColor: String = ""
)

// State untuk validasi form
data class FormValidationState(
    val isNameValid: Boolean = true,
    val nameErrorMessage: String? = null
)

// Avatar selection state
data class AvatarSelection(
    val type: String = "initial", // "initial" atau "preset"
    val value: String = "", // inisial atau ID preset avatar
    val color: String = "" // warna untuk avatar inisial
)

class EditProfileViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val transactionRepository: TransactionRepository = TransactionRepository(FirebaseFirestore.getInstance(), FirebaseAuth.getInstance()),
    private val debtRepository: DebtRepositoryImpl = DebtRepositoryImpl(FirebaseFirestore.getInstance(), FirebaseAuth.getInstance())
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    // Form fields - editable
    var editedName by mutableStateOf("")
        private set
    var editedEmail by mutableStateOf("")
        private set

    // Avatar selection
    var selectedAvatar by mutableStateOf(AvatarSelection())
        private set

    // Form validation
    private val _validationState = MutableStateFlow(FormValidationState())
    val validationState: StateFlow<FormValidationState> = _validationState.asStateFlow()

    init {
        loadCurrentUserData()
    }

    private fun loadCurrentUserData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val currentUser = auth.currentUser
            if (currentUser != null) {
                try {
                    // Get user data from Firestore
                    val result = userRepository.getUserProfile(currentUser.uid)

                    if (result.isSuccess) {
                        val user = result.getOrNull()
                        if (user != null) {
                            // Set original data
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                originalName = user.displayName,
                                originalEmail = user.email,
                                originalAvatarType = user.avatarType,
                                originalAvatarValue = user.avatarValue,
                                originalAvatarColor = user.avatarColor
                            )

                            // Set editable fields
                            editedName = user.displayName
                            editedEmail = user.email
                            selectedAvatar = AvatarSelection(
                                type = user.avatarType,
                                value = user.avatarValue,
                                color = user.avatarColor
                            )
                        } else {
                            // Fallback to Firebase Auth data with default avatar
                            val defaultAvatar = AvatarUtils.generateInitialAvatar(
                                currentUser.displayName ?: currentUser.email ?: "User"
                            )

                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                originalName = currentUser.displayName ?: "",
                                originalEmail = currentUser.email ?: "",
                                originalAvatarType = defaultAvatar.type,
                                originalAvatarValue = defaultAvatar.value,
                                originalAvatarColor = defaultAvatar.color
                            )

                            editedName = currentUser.displayName ?: ""
                            editedEmail = currentUser.email ?: ""
                            selectedAvatar = defaultAvatar
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Gagal memuat data profil: ${result.exceptionOrNull()?.message}"
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Terjadi kesalahan: ${e.message}"
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "User tidak login"
                )
            }
        }
    }

    fun onNameChange(newName: String) {
        editedName = newName
        validateName(newName)
        clearMessages()

        // Update avatar jika menggunakan initial avatar
        if (selectedAvatar.type == "initial") {
            val newInitialAvatar = AvatarUtils.generateInitialAvatar(newName.ifBlank { "User" })
            selectedAvatar = newInitialAvatar
        }
    }

    fun onAvatarSelected(avatarSelection: AvatarSelection) {
        selectedAvatar = avatarSelection
        clearMessages()
    }

    private fun validateName(name: String): Boolean {
        return when {
            name.isBlank() -> {
                _validationState.value = FormValidationState(
                    isNameValid = false,
                    nameErrorMessage = "Nama tidak boleh kosong"
                )
                false
            }

            name.length < 2 -> {
                _validationState.value = FormValidationState(
                    isNameValid = false,
                    nameErrorMessage = "Nama minimal 2 karakter"
                )
                false
            }

            name.length > 50 -> {
                _validationState.value = FormValidationState(
                    isNameValid = false,
                    nameErrorMessage = "Nama maksimal 50 karakter"
                )
                false
            }

            else -> {
                _validationState.value = FormValidationState(
                    isNameValid = true,
                    nameErrorMessage = null
                )
                true
            }
        }
    }

    fun saveProfile(onSuccess: () -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "User tidak login"
            )
            return
        }

        // Validate form
        if (!validateName(editedName)) {
            return
        }

        // Check if there are any changes
        val nameChanged = editedName.trim() != _uiState.value.originalName
        val avatarChanged = selectedAvatar.type != _uiState.value.originalAvatarType ||
                selectedAvatar.value != _uiState.value.originalAvatarValue ||
                selectedAvatar.color != _uiState.value.originalAvatarColor

        if (!nameChanged && !avatarChanged) {
            _uiState.value = _uiState.value.copy(
                successMessage = "Tidak ada perubahan untuk disimpan"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                errorMessage = null,
                successMessage = null
            )

            try {
                // Update profile dengan avatar baru
                val updateResult = userRepository.updateUserProfile(
                    userId = currentUser.uid,
                    displayName = editedName.trim(),
                    avatarType = selectedAvatar.type,
                    avatarValue = selectedAvatar.value,
                    avatarColor = selectedAvatar.color
                )

                if (updateResult.isSuccess) {
                    // Update original data with new values
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        originalName = editedName.trim(),
                        originalAvatarType = selectedAvatar.type,
                        originalAvatarValue = selectedAvatar.value,
                        originalAvatarColor = selectedAvatar.color,
                        successMessage = "Profil berhasil disimpan"
                    )

                    // Call success callback
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = "Gagal menyimpan profil: ${updateResult.exceptionOrNull()?.message}"
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Terjadi kesalahan: ${e.message}"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    fun resetForm() {
        editedName = _uiState.value.originalName
        editedEmail = _uiState.value.originalEmail
        selectedAvatar = AvatarSelection(
            type = _uiState.value.originalAvatarType,
            value = _uiState.value.originalAvatarValue,
            color = _uiState.value.originalAvatarColor
        )
        _validationState.value = FormValidationState()
        clearMessages()
    }

    // Helper function to check if form has changes
    fun hasChanges(): Boolean {
        val nameChanged = editedName.trim() != _uiState.value.originalName
        val avatarChanged = selectedAvatar.type != _uiState.value.originalAvatarType ||
                selectedAvatar.value != _uiState.value.originalAvatarValue ||
                selectedAvatar.color != _uiState.value.originalAvatarColor
        return nameChanged || avatarChanged
    }

    // Get current avatar for display
    fun getCurrentAvatar(): AvatarSelection {
        return selectedAvatar
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "User tidak login")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value =
                    _uiState.value.copy(isSaving = true, errorMessage = null, successMessage = null)

                val userId = currentUser.uid
                val db = FirebaseFirestore.getInstance()

                // 1. Hapus semua transaksi user (kalau disimpan di subcollection "transactions" dalam users)
                val transactionsSnap = db.collection("users")
                    .document(userId)
                    .collection("transactions")
                    .get().await()
                for (doc in transactionsSnap.documents) {
                    doc.reference.delete().await()
                }

                // 2. Hapus semua debts milik user
                val debtsSnap = db.collection("debts")
                    .whereEqualTo("userId", userId)
                    .get().await()
                for (doc in debtsSnap.documents) {
                    doc.reference.delete().await()
                }

                // 3. Hapus semua businessUnit milik user
                val businessUnitsSnap = db.collection("businessUnit")
                    .whereEqualTo("userId", userId)
                    .get().await()
                for (doc in businessUnitsSnap.documents) {
                    doc.reference.delete().await()
                }

                // 4. Hapus semua contacts milik user (kalau ada koleksi contacts)
                val contactsSnap = db.collection("contacts")
                    .whereEqualTo("userId", userId)
                    .get().await()
                for (doc in contactsSnap.documents) {
                    doc.reference.delete().await()
                }

                // 5. Hapus dokumen user dari koleksi "users"
                db.collection("users").document(userId).delete().await()

                // 6. Hapus akun Firebase Auth
                currentUser.delete().await()

                _uiState.value = _uiState.value.copy(isSaving = false)
                onSuccess()

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Gagal menghapus akun: ${e.message}"
                )
            }
        }
    }
}