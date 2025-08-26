package com.mibi.xkas.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Data class untuk merepresentasikan state UI profil
data class UserProfileUiState(
    val userName: String? = null,
    val userEmail: String? = null,
    val userProfileImageUrl: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class ProfileViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null) // Reset error message
            val currentUser: FirebaseUser? = auth.currentUser
            if (currentUser != null) {
                _uiState.value = UserProfileUiState(
                    userName = currentUser.displayName,
                    userEmail = currentUser.email,
                    userProfileImageUrl = currentUser.photoUrl?.toString(),
                    isLoading = false
                )
            } else {
                // Ini bisa terjadi jika pengguna tidak login atau sesi telah berakhir
                _uiState.value = UserProfileUiState(
                    isLoading = false,
                    errorMessage = "Pengguna tidak login. Silakan login terlebih dahulu."
                )
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        auth.signOut()
        // Setelah sign out, currentUser akan menjadi null.
        // Anda mungkin ingin mengosongkan UI state atau menavigasi.
        _uiState.value = UserProfileUiState(isLoading = false, errorMessage = "Anda telah logout.") // Contoh update state
        onComplete() // Panggil callback untuk navigasi
    }
}