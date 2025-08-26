package com.mibi.xkas.ui.auth

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.mibi.xkas.R
import com.mibi.xkas.ui.theme.MyNewButtonBackgroundColor
import com.mibi.xkas.ui.theme.MyNewButtonContentColor
import com.mibi.xkas.ui.theme.XKasTheme

@Composable
fun WelcomeScreen(
    onNavigateToHome: () -> Unit, // <-- Ganti nama parameter menjadi onNavigateToHome
    modifier: Modifier = Modifier
) {
    val offWhite = Color(0xFFF0F0F0)
    val context = LocalContext.current // <-- Dapatkan Context
    val firebaseAuth = FirebaseAuth.getInstance() // <-- Dapatkan instance FirebaseAuth

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Bagian Atas: Hanya Gambar Ilustrasi
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 58.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_onboarding_final),
                    contentDescription = "Get Started Illustration",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f / 1f),
                    contentScale = ContentScale.Fit
                )
            }

            // Area Bawah dengan Latar Belakang Putih UTAMA
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .width(375.dp)
                    .height(338.dp)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(Color.White)
                    .padding(top = 32.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val userName = firebaseAuth.currentUser?.displayName?.split(" ")?.firstOrNull() ?: "Anda"
                Text(
                    text = "Siap Memulai, $userName?", // <-- Bisa tambahkan nama pengguna jika mau
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 26.dp)
                )

                Card(
                    modifier = Modifier
                        .width(372.dp)
                        .height(192.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = offWhite
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Satu langkah lagi untuk mengelola keuangan Anda dengan lebih baik bersama kami.",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
                        )

                        Button(
                            onClick = {
                                // Simpan status bahwa welcome screen sudah ditampilkan
                                // Ini penting agar layar ini tidak muncul lagi untuk pengguna ini
                                firebaseAuth.currentUser?.uid?.let { userId ->
                                    val sharedPrefs = context.getSharedPreferences(
                                        "app_prefs_$userId", // SharedPreferences unik per pengguna
                                        Context.MODE_PRIVATE
                                    )
                                    sharedPrefs.edit().putBoolean("welcome_shown", true).apply()
                                }
                                onNavigateToHome() // Panggil callback untuk navigasi
                            },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .width(309.dp)
                                .height(92.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MyNewButtonBackgroundColor,
                                contentColor = MyNewButtonContentColor
                            ),
                            contentPadding = PaddingValues(horizontal = 24.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                Text(
                                    text = "Mulai Sekarang",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MyNewButtonContentColor,
                                    modifier = Modifier.align(Alignment.CenterStart)
                                )
                                Image(
                                    painter = painterResource(id = R.drawable.ic_rocket_launch),
                                    contentDescription = "Ilustrasi Mulai",
                                    modifier = Modifier
                                        .size(42.dp)
                                        .align(Alignment.CenterEnd)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=375dp,height=812dp,dpi=480")
@Preview(showBackground = true, name = "Welcome Screen (Figma Specs)")
@Composable
fun WelcomeScreenFigmaSpecsPreview() {
    XKasTheme {
        WelcomeScreen(
            onNavigateToHome = { /* Preview action */ } // <-- Sesuaikan nama callback di preview
        )
    }
}