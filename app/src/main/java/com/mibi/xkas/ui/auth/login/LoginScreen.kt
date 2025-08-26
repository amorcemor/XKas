package com.mibi.xkas.ui.auth.login // Pastikan package ini sesuai

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.remember


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    // Ganti onLoginClicked dengan onLoginSuccess yang tidak memerlukan parameter
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier,
    loginViewModel: LoginViewModel = viewModel() // Inject ViewModel
) {
    // State email dan password sekarang dikelola oleh ViewModel
    // var email by rememberSaveable { mutableStateOf("") } // Pindahkan ke ViewModel
    // var password by rememberSaveable { mutableStateOf("") } // Pindahkan ke ViewModel
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var backPressedTime by remember { mutableStateOf(0L) }

    val context = LocalContext.current // Untuk menampilkan Toast
    val loginState = loginViewModel.loginResultState // Amati state dari ViewModel

    // ✅ TAMBAHKAN BackHandler INI
    BackHandler {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            (context as? ComponentActivity)?.finishAffinity()
        } else {
            Toast.makeText(
                context,
                "Tekan sekali lagi untuk keluar aplikasi",
                Toast.LENGTH_SHORT
            ).show()
            backPressedTime = System.currentTimeMillis()
        }
    }

    // Handle efek samping dari perubahan loginState
    LaunchedEffect(loginState) {
        when (loginState) {
            is LoginResultState.Success -> {
                Toast.makeText(context, "Login Berhasil!", Toast.LENGTH_SHORT).show()
                onLoginSuccess()
                loginViewModel.resetLoginResultState()
            }
            is LoginResultState.Error -> {
                Toast.makeText(context, loginState.message, Toast.LENGTH_LONG).show()
            }
            else -> { /* Idle atau Loading */ }
        }
    }

    val screenBackgroundColor = MaterialTheme.colorScheme.background
    val formContainerColor = Color.White // Anda bisa juga menggunakan MaterialTheme.colorScheme.surface

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
                contentDescription = "Login Illustration",
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .aspectRatio(1f / 1f) // Menjaga rasio aspek gambar
                    .padding(top = 32.dp, bottom = 16.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(42.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    // .height(470.dp) // Tinggi tetap mungkin kurang responsif, pertimbangkan weight atau wrapContent
                    .padding(bottom = 0.dp), // Beri padding bawah jika Surface tidak mengisi semua sisa ruang
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
                            text = "Masuk ke Akun Anda",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 34.dp)
                        )

                        OutlinedTextField(
                            value = loginViewModel.email, // Gunakan state email dari ViewModel
                            onValueChange = { loginViewModel.onEmailChange(it) }, // Panggil fungsi ViewModel
                            label = { Text("Email") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            isError = loginState is LoginResultState.Error // Tampilkan error jika state adalah Error
                        )

                        OutlinedTextField(
                            value = loginViewModel.password, // Gunakan state password dari ViewModel
                            onValueChange = { loginViewModel.onPasswordChange(it) }, // Panggil fungsi ViewModel
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                val imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                val description = if (passwordVisible) "Sembunyikan password" else "Tampilkan password"
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(imageVector = imageVector, description)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            isError = loginState is LoginResultState.Error // Tampilkan error jika state adalah Error
                        )

                        // Menampilkan pesan error di bawah field (opsional)
                        if (loginState is LoginResultState.Error) {
                            Text(
                                text = loginState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .padding(top = 0.dp, bottom = 8.dp)
                                    .fillMaxWidth() // Agar rata kiri jika perlu
                            )
                        }


                        Spacer(modifier = Modifier.height(8.dp)) // Spacer sebelum tombol

                        Button(
                            onClick = {
                                loginViewModel.loginUser() // Panggil fungsi login di ViewModel
                            },
                            modifier = Modifier
                                // .align(Alignment.BottomCenter) // Tidak perlu jika tombol ada di dalam Column utama
                                // .padding(bottom = 32.dp + 16.dp + 24.dp, start = 24.dp, end = 24.dp)
                                .width(242.dp)
                                .height(65.dp)
                                .padding(top = 16.dp), // Beri jarak dari field terakhir atau pesan error
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MyNewButtonBackgroundColor,
                                contentColor = MyNewButtonContentColor
                            ),
                            enabled = loginState !is LoginResultState.Loading, // Disable tombol saat loading
                            contentPadding = PaddingValues(horizontal = 24.dp) // Pastikan ini tidak memotong konten
                        ) {
                            if (loginState is LoginResultState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MyNewButtonContentColor // Atau warna yang sesuai
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize()) { // Box di dalam button
                                    Text(
                                        text = "MASUK",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.align(Alignment.CenterStart)
                                    )
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_rocket_launch), // Ganti dengan ikon yang sesuai jika perlu
                                        contentDescription = "Login Action Icon",
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .size(32.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f)) // Mendorong link "Daftar di sini" ke bawah jika masih di dalam Column ini

                        Row(
                            modifier = Modifier
                                // .align(Alignment.BottomCenter) // Tidak perlu jika di dalam Column utama
                                .padding(top = 24.dp, bottom = 32.dp) // Jarak dari tombol dan tepi bawah Surface
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Belum punya akun? ")
                            ClickableText(

                                text = AnnotatedString("Daftar di sini"),
                                onClick = { offset ->
                                    Log.d("LoginScreen", "Daftar di sini diklik!")
                                    if (loginState !is LoginResultState.Loading) {
                                        Log.d("LoginScreen", "Memanggil onNavigateToRegister...")
                                        onNavigateToRegister()
                                    } else {
                                        Log.d("LoginScreen", "Navigasi ke register diblokir karena sedang loading.")
                                    }
                                },
                                style = TextStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                    // Tombol dan Link Daftar bisa diletakkan di luar Column utama form jika ingin tetap di bawah Box
                    // Namun, dengan memasukkannya ke dalam Column utama form dan menggunakan Spacer(Modifier.weight(1f))
                    // akan lebih mudah mengatur layoutnya secara keseluruhan.

            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=375dp,height=812dp,dpi=480")
@Composable
fun LoginScreenLightFixedHeightPreview() {
    XKasTheme {
        LoginScreen(
            onLoginSuccess = { },
            onNavigateToRegister = {},
        )
    }
}