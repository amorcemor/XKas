package com.mibi.xkas.ui.auth.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Untuk merepresentasikan state hasil login
sealed class LoginResultState {
    object Idle : LoginResultState() // Keadaan awal atau setelah proses selesai tanpa error navigasi
    object Loading : LoginResultState()
    data class Success(val user: FirebaseUser) : LoginResultState()
    data class Error(val message: String) : LoginResultState()
}

class LoginViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // State untuk input fields (bisa juga di-hoist ke Composable jika preferensi)
    var email by mutableStateOf("")
    var password by mutableStateOf("")

    // State untuk hasil proses login
    var loginResultState by mutableStateOf<LoginResultState>(LoginResultState.Idle)
        private set


    fun onEmailChange(newEmail: String) {
        email = newEmail
        if (loginResultState !is LoginResultState.Idle && loginResultState !is LoginResultState.Loading) {
            loginResultState = LoginResultState.Idle // Reset error jika user mulai mengetik lagi
        }
    }

    fun onPasswordChange(newPassword: String) {
        password = newPassword
        if (loginResultState !is LoginResultState.Idle && loginResultState !is LoginResultState.Loading) {
            loginResultState = LoginResultState.Idle // Reset error jika user mulai mengetik lagi
        }
    }

    fun loginUser() {
        if (email.isBlank() || password.isBlank()) {
            loginResultState = LoginResultState.Error("Email dan password tidak boleh kosong.")
            return
        }

        loginResultState = LoginResultState.Loading
        viewModelScope.launch {
            try {
                val authResult = auth.signInWithEmailAndPassword(email.trim(), password.trim()).await()
                authResult.user?.let {
                    loginResultState = LoginResultState.Success(it)
                } ?: run {
                    loginResultState = LoginResultState.Error("Gagal login, pengguna tidak ditemukan.")
                }
            } catch (e: Exception) {
                // Tangani berbagai jenis exception dari Firebase
                val errorMessage = when (e) {
                    is FirebaseAuthInvalidUserException -> "Email tidak terdaftar."
                    is FirebaseAuthInvalidCredentialsException -> "Password salah."
                    else -> "Gagal login: ${e.message}"
                }
                loginResultState = LoginResultState.Error(errorMessage)
            }
        }
    }

    // Fungsi untuk mereset state setelah navigasi atau pesan error ditampilkan
    fun resetLoginResultState() {
        loginResultState = LoginResultState.Idle
    }
}