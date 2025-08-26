package com.mibi.xkas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.mibi.xkas.ui.MainAppScaffold
import com.mibi.xkas.ui.navigation.Screen
import com.mibi.xkas.ui.theme.XKasTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance() // Inisialisasi FirebaseAuth

        setContent {
            XKasTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController() // Buat NavController di sini

                    // Tentukan start destination berdasarkan status login pengguna
                    val startDestination = if (firebaseAuth.currentUser != null) {
                        // Pengguna sudah login, arahkan ke layar utama
                        Screen.MainTransactions.route
                    } else {
                        // Pengguna belum login, arahkan ke layar login
                        Screen.Login.route // Ganti dengan route yang sesuai
                    }

                    // Panggil MainAppScaffold dengan NavController dan startDestination yang sudah ditentukan
                    MainAppScaffold(
                        navController = navController,
                        startDestinationOuter = startDestination
                    )
                }
            }
        }
    }
}
