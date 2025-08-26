package com.mibi.xkas.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.mibi.xkas.data.repository.UserRepository
import com.mibi.xkas.utils.AvatarUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// State untuk UI profile
data class ProfileUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val userName: String? = null,
    val userEmail: String? = null,
    val userAvatarType: String = "initial",
    val userAvatarValue: String = "",
    val userAvatarColor: String = ""
)

class ProfileViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
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
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                userName = user.displayName,
                                userEmail = user.email,
                                userAvatarType = user.avatarType,
                                userAvatarValue = user.avatarValue,
                                userAvatarColor = user.avatarColor
                            )
                        } else {
                            // Fallback to Firebase Auth data with default avatar
                            val defaultAvatar = AvatarUtils.generateInitialAvatar(
                                currentUser.displayName ?: currentUser.email ?: "User"
                            )

                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                userName = currentUser.displayName,
                                userEmail = currentUser.email,
                                userAvatarType = defaultAvatar.type,
                                userAvatarValue = defaultAvatar.value,
                                userAvatarColor = defaultAvatar.color
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Gagal memuat profil: ${result.exceptionOrNull()?.message}"
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

    fun refreshProfile() {
        loadUserProfile()
    }
}