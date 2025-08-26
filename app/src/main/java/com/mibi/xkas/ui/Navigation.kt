package com.mibi.xkas.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.ui.graphics.vector.ImageVector
import com.mibi.xkas.ui.navigation.Screen


data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
//    val isDevelopment: Boolean = false // Tambahan: Flag untuk menandai fitur dalam pengembangan
)

/**
 * Daftar item yang akan ditampilkan di Bottom Navigation Bar.
 */
val bottomNavItems = listOf(
    BottomNavItem(
        label = "Transaksi",
        icon = Icons.Filled.AccountBalanceWallet, // Menggunakan ikon Home standar untuk Transaksi
        route = Screen.MainTransactions.route
    ),
    BottomNavItem(
        label = "Hutang",
        icon = Icons.Filled.Wallet, // atau
//        icon = Icons.Filled.AccountBalanceWallet,
        route = Screen.MainDebt.route,
//        isDevelopment = true // Tandai item ini sebagai dalam pengembangan
    )

)