package com.mibi.xkas.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mibi.xkas.R
import com.mibi.xkas.ui.theme.XKasTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(), // Inject ViewModel
    onNavigateUp: () -> Unit,
    onEditProfileClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onNavigateToLogin: () -> Unit // Callback untuk navigasi setelah logout
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil Saya") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(32.dp))
            } else if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage ?: "Terjadi kesalahan pada profil.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.CenterHorizontally)
                )
                // Pertimbangkan menambahkan tombol untuk "Coba Lagi" yang memanggil viewModel.loadUserProfile()
                // atau tombol untuk kembali/login.
                if (uiState.errorMessage?.contains("login", ignoreCase = true) == true) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onNavigateToLogin) {
                        Text("Login Sekarang")
                    }
                }
            } else {
                // Foto Profil
                Spacer(modifier = Modifier.height(24.dp))
                if (!uiState.userProfileImageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = uiState.userProfileImageUrl,
                        contentDescription = "Foto Profil",
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.ic_rocket_launch), // Placeholder Anda
                        error = painterResource(id = R.drawable.ic_rocket_launch), // Gambar jika error memuat
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_rocket_launch), // Gambar default
                        contentDescription = "Foto Profil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Nama Pengguna
                Text(
                    text = uiState.userName ?: "Nama Pengguna Tidak Tersedia",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Email Pengguna
                Text(
                    text = uiState.userEmail ?: "Email Tidak Tersedia",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))

                // Item Menu Profil
                ProfileMenuItem(
                    icon = Icons.Default.Person,
                    text = "Edit Profil",
                    onClick = onEditProfileClicked
                )
                ProfileMenuItem(
                    icon = Icons.Default.Settings,
                    text = "Pengaturan Aplikasi",
                    onClick = onSettingsClicked
                )

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                ProfileMenuItem(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    text = "Logout",
                    onClick = {
                        viewModel.logout {
                            onNavigateToLogin() // Panggil navigasi setelah logout selesai
                        }
                    },
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit, // Callback setelah login berhasil
    onNavigateToRegister: () -> Unit // Contoh jika ada tombol register
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    // val firebaseAuth = FirebaseAuth.getInstance() // Jika login langsung di sini

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Login Screen")
        Spacer(modifier = Modifier.height(16.dp))
        TextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = password, onValueChange = { password = it }, label = { Text("Password") })
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            // TODO: Implement Firebase Login Logic here or in ViewModel
            // Jika berhasil:
            onLoginSuccess()
        }) {
            Text("Login")
        }
        // Button(onClick = onNavigateToRegister) { Text("Belum punya akun? Daftar") }
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    tint: Color = LocalContentColor.current
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = tint
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    XKasTheme {
        // Untuk Preview, ViewModel tidak akan ter-inject dengan benar tanpa setup tambahan.
        // Preview ini akan menampilkan state loading atau error awal dari ViewModel.
        // Anda bisa membuat ProfileViewModel palsu untuk preview jika ingin mengontrol state yang ditampilkan.
        ProfileScreen(
            onNavigateUp = {},
            onEditProfileClicked = {},
            onSettingsClicked = {},
            onNavigateToLogin = {} // Preview tidak akan benar-benar logout
        )
    }
}