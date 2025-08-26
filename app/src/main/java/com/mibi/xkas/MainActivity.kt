package com.mibi.xkas

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
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
    private var backPressedTime: Long = 0
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()

        // Setup auth state listener
        authStateListener = FirebaseAuth.AuthStateListener { auth ->
            // Ini akan dipanggil setiap kali auth state berubah
            // Termasuk saat app pertama kali dibuka dan Firebase selesai restore session
        }

        setContent {
            XKasTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // ✅ GUNAKAN STATE YANG REACTIVE UNTUK AUTH
                    var isUserLoggedIn by remember { mutableStateOf(firebaseAuth.currentUser != null) }
                    var isAuthChecked by remember { mutableStateOf(false) }

                    // Listen perubahan auth state
                    DisposableEffect(Unit) {
                        val listener = FirebaseAuth.AuthStateListener { auth ->
                            isUserLoggedIn = auth.currentUser != null
                            isAuthChecked = true
                        }
                        firebaseAuth.addAuthStateListener(listener)
                        onDispose {
                            firebaseAuth.removeAuthStateListener(listener)
                        }
                    }

                    // Tentukan start destination berdasarkan auth state
                    val startDestination = if (isUserLoggedIn) {
                        Screen.MainTransactions.route
                    } else {
                        Screen.Login.route
                    }

                    // Handle tombol back
                    BackHandler {
                        val currentRoute = navController.currentBackStackEntry?.destination?.route
                        when (currentRoute) {
                            Screen.Login.route -> {
                                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                                    finishAffinity()
                                } else {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Tekan sekali lagi untuk keluar aplikasi",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    backPressedTime = System.currentTimeMillis()
                                }
                            }
                            Screen.MainTransactions.route,
                            Screen.MainDebt.route -> {
                                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                                    finish()
                                } else {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Tekan sekali lagi untuk keluar aplikasi",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    backPressedTime = System.currentTimeMillis()
                                }
                            }
                            else -> {
                                navController.popBackStack()
                            }
                        }
                    }

                    // Tampilkan scaffold hanya setelah auth state di-check
                    if (isAuthChecked) {
                        MainAppScaffold(
                            navController = navController,
                            startDestinationOuter = startDestination,
                            isUserLoggedIn = isUserLoggedIn // Pass auth state
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
        firebaseAuth.removeAuthStateListener(authStateListener)
    }

    fun handleLogout() {
        firebaseAuth.signOut()
        val sharedPref = getSharedPreferences("user_session", MODE_PRIVATE)
        sharedPref.edit().clear().apply()
        recreate()
    }
}