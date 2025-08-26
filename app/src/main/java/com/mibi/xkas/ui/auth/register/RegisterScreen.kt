package com.mibi.xkas.ui.auth.register

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mibi.xkas.R
import com.mibi.xkas.ui.theme.MyNewButtonBackgroundColor
import com.mibi.xkas.ui.theme.MyNewButtonContentColor
import com.mibi.xkas.ui.theme.XKasTheme


const val REGISTER_SCREEN_TAG = "RegisterScreen" // Tag untuk Logging

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    registerViewModel: RegisterViewModel = viewModel()
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val registerState = registerViewModel.registerResultState

    LaunchedEffect(registerState) {
        when (registerState) {
            is RegisterResultState.Success -> {
                Toast.makeText(
                    context,
                    "Registrasi berhasil! Akun untuk ${registerState.user.displayName ?: registerState.user.email} telah dibuat.",
                    Toast.LENGTH_LONG
                ).show()
                // --- TAMBAHKAN LOG DI SINI ---
                Log.d(REGISTER_SCREEN_TAG, "RegisterResultState adalah Success. Memanggil onRegisterSuccess().")
                onRegisterSuccess() // Panggil callback untuk navigasi
                registerViewModel.resetRegisterResultState()
            }
            is RegisterResultState.Error -> {
                Log.e(REGISTER_SCREEN_TAG, "RegisterResultState adalah Error: ${registerState.message}")
                // Pesan error sekarang akan ditampilkan di bawah field atau sebagai Toast
                // Toast.makeText(context, registerState.message, Toast.LENGTH_LONG).show()
            }
            is RegisterResultState.Loading -> {
                Log.d(REGISTER_SCREEN_TAG, "RegisterResultState adalah Loading.")
            }
            is RegisterResultState.Idle -> {
                Log.d(REGISTER_SCREEN_TAG, "RegisterResultState adalah Idle.")
            }
        }
    }

    val screenBackgroundColor = MaterialTheme.colorScheme.background
    val formContainerColor = Color.White

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = screenBackgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_login_illustration),
                contentDescription = "Register Illustration",
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(1f / 0.8f)
                    .padding(top = 32.dp, bottom = 16.dp),
                contentScale = ContentScale.Fit
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = formContainerColor,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Buat Akun",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    OutlinedTextField(
                        value = registerViewModel.name,
                        onValueChange = { registerViewModel.onNameChange(it) },
                        label = { Text("Nama Lengkap") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words // Untuk kapitalisasi setiap kata
                            // capitalization = KeyboardCapitalization.Sentences // Jika ingin kapitalisasi awal kalimat
                            // capitalization = KeyboardCapitalization.Characters // Jika ingin semua karakter kapital
                            // capitalization = KeyboardCapitalization.None // Jika tidak ingin kapitalisasi otomatis
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        isError = registerState is RegisterResultState.Error && registerState.message.contains("Nama", ignoreCase = true)
                    )

                    OutlinedTextField(
                        value = registerViewModel.email,
                        onValueChange = { registerViewModel.onEmailChange(it) },
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        isError = registerState is RegisterResultState.Error && registerState.message.contains("Email", ignoreCase = true)
                    )

                    OutlinedTextField(
                        value = registerViewModel.password,
                        onValueChange = { registerViewModel.onPasswordChange(it) },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, if (passwordVisible) "Sembunyikan password" else "Tampilkan password")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        isError = registerState is RegisterResultState.Error && registerState.message.contains("Password", ignoreCase = true)
                    )

                    OutlinedTextField(
                        value = registerViewModel.confirmPassword,
                        onValueChange = { registerViewModel.onConfirmPasswordChange(it) },
                        label = { Text("Konfirmasi Password") },
                        singleLine = true,
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            val image = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(imageVector = image, if (confirmPasswordVisible) "Sembunyikan password" else "Tampilkan password")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        isError = registerState is RegisterResultState.Error && (registerState.message.contains("cocok", ignoreCase = true) || registerState.message.contains("Konfirmasi", ignoreCase = true))
                    )

                    if (registerState is RegisterResultState.Error) {
                        Text(
                            text = registerState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = {
                            Log.d(REGISTER_SCREEN_TAG, "Tombol DAFTAR diklik. Memanggil registerViewModel.registerUser().")
                            registerViewModel.registerUser()
                        },
                        modifier = Modifier
                            .width(242.dp)
                            .height(65.dp)
                            .padding(top = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MyNewButtonBackgroundColor,
                            contentColor = MyNewButtonContentColor
                        ),
                        enabled = registerState !is RegisterResultState.Loading
                    ) {
                        if (registerState is RegisterResultState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MyNewButtonContentColor
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Text(
                                    "DAFTAR",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.align(Alignment.CenterStart)
                                )
                                Image(
                                    painter = painterResource(id = R.drawable.ic_rocket_launch),
                                    contentDescription = "Ikon Roket",
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .size(42.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sudah punya akun? ")
                        ClickableText(
                            text = AnnotatedString("Masuk di sini"),
                            onClick = {
                                if (registerState !is RegisterResultState.Loading) {
                                    Log.d(REGISTER_SCREEN_TAG, "Teks 'Masuk di sini' diklik. Memanggil onNavigateToLogin().")
                                    onNavigateToLogin()
                                }
                            },
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=375dp,height=812dp,dpi=480")
@Composable
fun RegisterScreenPreviewWithViewModel() {
    XKasTheme {
        RegisterScreen(
            onRegisterSuccess = { Log.d("RegisterPreview", "onRegisterSuccess dipanggil") },
            onNavigateToLogin = { Log.d("RegisterPreview", "onNavigateToLogin dipanggil") }
        )
    }
}