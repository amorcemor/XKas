package com.mibi.xkas.ui.auth.register // Sesuaikan dengan package Anda

import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// State untuk hasil proses registrasi
sealed class RegisterResultState {
    object Idle : RegisterResultState()
    object Loading : RegisterResultState()
    data class Success(val user: FirebaseUser) : RegisterResultState()
    data class Error(val message: String) : RegisterResultState()
}

class RegisterViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth

    var name by mutableStateOf("") // Tambahkan state untuk nama
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")

    var registerResultState by mutableStateOf<RegisterResultState>(RegisterResultState.Idle)
        private set

    fun onNameChange(newName: String) { // Tambahkan fungsi untuk nama
        name = newName
        resetErrorStateIfNeeded()
    }

    fun onEmailChange(newEmail: String) {
        email = newEmail
        resetErrorStateIfNeeded()
    }

    fun onPasswordChange(newPassword: String) {
        password = newPassword
        resetErrorStateIfNeeded()
    }

    fun onConfirmPasswordChange(newConfirmPassword: String) {
        confirmPassword = newConfirmPassword
        resetErrorStateIfNeeded()
    }

    private fun resetErrorStateIfNeeded() {
        if (registerResultState is RegisterResultState.Error || registerResultState is RegisterResultState.Success) {
            registerResultState = RegisterResultState.Idle
        }
    }

    fun registerUser() {
        if (name.isBlank()) { // Validasi nama
            registerResultState = RegisterResultState.Error("Nama lengkap harus diisi.")
            return
        }
        if (email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            registerResultState = RegisterResultState.Error("Email, password, dan konfirmasi password harus diisi.")
            return
        }
        if (password != confirmPassword) {
            registerResultState = RegisterResultState.Error("Password dan konfirmasi password tidak cocok.")
            return
        }
        if (password.length < 6) {
            registerResultState = RegisterResultState.Error("Password minimal harus 6 karakter.")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            registerResultState = RegisterResultState.Error("Format email tidak valid.")
            return
        }

        registerResultState = RegisterResultState.Loading
        viewModelScope.launch {
            try {
                val authResult = auth.createUserWithEmailAndPassword(email.trim(), password.trim()).await()
                authResult.user?.let { firebaseUser ->
                    // Set display name untuk pengguna baru
                    val userProfileChangeRequest = UserProfileChangeRequest.Builder()
                        .setDisplayName(name.trim())
                        .build()
                    firebaseUser.updateProfile(userProfileChangeRequest).await() // Tunggu sampai update profile selesai

                    registerResultState = RegisterResultState.Success(firebaseUser)
                } ?: run {
                    registerResultState = RegisterResultState.Error("Gagal membuat akun, pengguna tidak valid.")
                }
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is FirebaseAuthWeakPasswordException -> "Password terlalu lemah."
                    is FirebaseAuthInvalidCredentialsException -> "Format email tidak valid atau password salah." // Bisa juga email
                    is FirebaseAuthUserCollisionException -> "Email sudah terdaftar. Silakan gunakan email lain atau login."
                    else -> "Gagal registrasi: ${e.localizedMessage ?: "Terjadi kesalahan tidak diketahui."}"
                }
                registerResultState = RegisterResultState.Error(errorMessage)
            }
        }
    }

    fun resetRegisterResultState() {
        registerResultState = RegisterResultState.Idle
    }
}