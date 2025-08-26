package com.mibi.xkas.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.mibi.xkas.ui.addedit.AddEditTransactionScreen
import com.mibi.xkas.ui.auth.WelcomeScreen
import com.mibi.xkas.ui.auth.login.LoginScreen
import com.mibi.xkas.ui.auth.register.RegisterScreen
import com.mibi.xkas.ui.detail.TransactionDetailScreen
import com.mibi.xkas.ui.home.MainHomeScreen
import com.mibi.xkas.ui.navigation.Screen
import com.mibi.xkas.ui.profile.EditProfileScreen
import kotlin.math.atan2
import coil.compose.AsyncImage
import com.mibi.xkas.ui.debt.EnhancedContactDebtDetailScreen
import com.mibi.xkas.ui.debt.EnhancedContactDebtScreen
import com.mibi.xkas.ui.home.HomeViewModel
import com.mibi.xkas.ui.profile.EditProfileScreen
import com.mibi.xkas.ui.bottomNavItems
import com.google.firebase.auth.FirebaseAuth
import com.mibi.xkas.ui.settings.SettingsScreen


const val MAIN_APP_SCAFFOLD_TAG = "MainAppScaffold"
val FAB_SIZE: Dp = 58.dp
val BOTTOM_APP_BAR_HEIGHT: Dp = 68.dp
val ROUNDED_CORNER_RADIUS: Dp = 10.dp
val CUTOUT_OFFSET_Y: Dp = 29.dp

// Kelas BottomAppBarCutoutShape tetap sama, tidak perlu diubah.
class BottomAppBarCutoutShape(
    private val fabSizePx: Float,
    private val cornerRadiusPx: Float,
    private val cutoutOffsetYPx: Float,
    private val fabActualOffsetYPx: Float = 0f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val cutoutPaddingPx = density.run { 4.dp.toPx() }
            val cutoutDiameterWithPadding = fabSizePx + cutoutPaddingPx * 2
            val cutoutRadiusWithPadding = cutoutDiameterWithPadding / 2f
            val cutoutCenterX = size.width / 2f
            val appBarTopY = 0f // Y-coordinate dari bagian atas AppBar

            // Ini adalah radius dari lengkungan bahu itu sendiri.
            // Kita gunakan cornerRadiusPx sebagai basis, tapi tidak boleh lebih besar dari radius FAB.
            val shoulderRadiusPx = cornerRadiusPx.coerceAtMost(cutoutRadiusWithPadding)

            // Titik X di mana lengkungan 'U' FAB dimulai pada sisi kiri.
            val uArcStartX = cutoutCenterX - cutoutRadiusWithPadding
            // Titik X di mana lengkungan 'U' FAB berakhir pada sisi kanan.
            val uArcEndX = cutoutCenterX + cutoutRadiusWithPadding

            // Titik Y di mana bahu bertemu dengan lengkungan 'U' FAB.
            // Jika appBarTopY = 0, maka ini adalah shoulderRadiusPx.
            val shoulderEndY = appBarTopY + shoulderRadiusPx


            // Mulai dari bawah sudut kiri atas bar
            // (0, cornerRadiusPx)
            moveTo(0f, appBarTopY + cornerRadiusPx)

            // 1. Sudut kiri atas BottomAppBar (seperempat lingkaran)
            // Dari (0, cornerRadiusPx) ke (cornerRadiusPx, 0)
            arcTo(
                rect = Rect(left = 0f, top = appBarTopY, right = cornerRadiusPx * 2, bottom = appBarTopY + cornerRadiusPx * 2),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            // Path sekarang berada di (cornerRadiusPx, appBarTopY)

            // 2. Garis lurus dari akhir sudut kiri atas ke awal lengkungan bahu kiri.
            // Titik X di mana lengkungan bahu kiri akan dimulai.
            // Ini adalah uArcStartX dikurangi dengan seberapa "mundur" bahu tersebut.
            // Jika bahu adalah seperempat lingkaran dengan radius shoulderRadiusPx,
            // maka ia mundur sejauh shoulderRadiusPx dari uArcStartX.
            val leftShoulderStartX = (uArcStartX - shoulderRadiusPx).coerceAtLeast(cornerRadiusPx)
            lineTo(x = leftShoulderStartX, y = appBarTopY)
            // Path sekarang berada di (leftShoulderStartX, appBarTopY)

            // 3. Kurva Bezier untuk bahu kiri.
            // Dimulai dari: (leftShoulderStartX, appBarTopY)
            // Berakhir di: (uArcStartX, shoulderEndY)
            // Titik kontrol yang membuat transisi mulus dari horizontal ke vertikal: (uArcStartX, appBarTopY)
            quadraticBezierTo(
                x1 = uArcStartX, y1 = appBarTopY,         // Titik kontrol
                x2 = uArcStartX, y2 = shoulderEndY        // Titik akhir bahu kiri (awal lengkungan U)
            )
            // Path sekarang berada di (uArcStartX, shoulderEndY)

            // --- BAGIAN LENGKUNGAN U DAN SISI KANAN TETAP SAMA SEPERTI KODE ANDA SEBELUMNYA ---
            val fabCutoutActualTopY = appBarTopY - cutoutOffsetYPx + fabActualOffsetYPx

            if (cutoutRadiusWithPadding > shoulderRadiusPx && cutoutOffsetYPx > 0) {
                val fabCircleCenterX = cutoutCenterX
                val fabCircleCenterY = fabCutoutActualTopY + cutoutRadiusWithPadding

                val startAngleRad = atan2(
                    y = shoulderEndY - fabCircleCenterY,
                    x = uArcStartX - fabCircleCenterX
                )
                var startAngleDeg = Math.toDegrees(startAngleRad.toDouble()).toFloat()

                val endAngleRad = atan2(
                    y = shoulderEndY - fabCircleCenterY,
                    x = uArcEndX - fabCircleCenterX
                )
                var endAngleDeg = Math.toDegrees(endAngleRad.toDouble()).toFloat()

                var sweepAngleDeg = endAngleDeg - startAngleDeg
                if (sweepAngleDeg > 180f) {
                    sweepAngleDeg -= 360f
                } else if (sweepAngleDeg < -180f) {
                    sweepAngleDeg += 360f
                }

                if (shoulderEndY < fabCircleCenterY && sweepAngleDeg > 0) {
                    sweepAngleDeg -=360f
                } else if (shoulderEndY > fabCircleCenterY && sweepAngleDeg < 0) {
                    // (Situasi tidak umum)
                }

                val fabBoundingRect = Rect(
                    left = cutoutCenterX - cutoutRadiusWithPadding,
                    top = fabCutoutActualTopY,
                    right = cutoutCenterX + cutoutRadiusWithPadding,
                    bottom = fabCutoutActualTopY + cutoutDiameterWithPadding
                )

                if (kotlin.math.abs(sweepAngleDeg) > 0.1f) {
                    arcTo(
                        rect = fabBoundingRect,
                        startAngleDegrees = startAngleDeg,
                        sweepAngleDegrees = sweepAngleDeg,
                        forceMoveTo = false
                    )
                } else {
                    lineTo(x = uArcEndX, y = shoulderEndY)
                }
            } else {
                lineTo(x = uArcEndX, y = shoulderEndY)
            }
            // Path sekarang berada di (uArcEndX, shoulderEndY)

            // 5. Kurva Bezier untuk bahu kanan
            val rightShoulderEndX = (uArcEndX + shoulderRadiusPx).coerceAtMost(size.width - cornerRadiusPx)
            quadraticBezierTo(
                x1 = uArcEndX, y1 = appBarTopY,        // Titik kontrol
                x2 = rightShoulderEndX, y2 = appBarTopY   // Titik akhir bahu kanan
            )
            // Path sekarang berada di (rightShoulderEndX, appBarTopY)

            // 6. Garis lurus ke awal sudut kanan atas
            lineTo(x = size.width - cornerRadiusPx, y = appBarTopY)

            // 7. Sudut kanan atas
            arcTo(
                rect = Rect(left = size.width - (cornerRadiusPx * 2), top = appBarTopY, right = size.width, bottom = appBarTopY + cornerRadiusPx * 2),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // 8. Selesaikan path
            lineTo(x = size.width, y = size.height)
            lineTo(x = 0f, y = size.height)
            close()
        }
        return Outline.Generic(path)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    navController: NavHostController,
    startDestinationOuter: String,
    isUserLoggedIn: Boolean = true
) {
    val homeViewModel: HomeViewModel = hiltViewModel()

    // ✅ TAMBAHKAN INI: Listen perubahan auth state untuk redirect
    LaunchedEffect(isUserLoggedIn) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route

        if (!isUserLoggedIn && currentRoute != Screen.Login.route &&
            currentRoute != Screen.Register.route && currentRoute != Screen.Welcome.route) {
            // User logout, redirect ke login
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        } else if (isUserLoggedIn && (currentRoute == Screen.Login.route ||
                    currentRoute == Screen.Register.route)) {
            // User login, redirect ke main screen
            navController.navigate(Screen.MainTransactions.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val density = LocalDensity.current
    val fabSizePx = with(density) { FAB_SIZE.toPx() }
    val cornerRadiusPx = with(density) { ROUNDED_CORNER_RADIUS.toPx() }
    val cutoutOffsetYPx = with(density) { CUTOUT_OFFSET_Y.toPx() }

    val bottomAppBarShape = remember(fabSizePx, cornerRadiusPx, cutoutOffsetYPx) {
        BottomAppBarCutoutShape(fabSizePx, cornerRadiusPx, cutoutOffsetYPx)
    }

    val noBottomBarRoutes = listOf(
        Screen.Login.route,
        Screen.Register.route,
        Screen.Welcome.route,
        Screen.AddEditBusinessUnit.route,
        Screen.AddEditTransaction.route,
        Screen.AddContact.route,
        Screen.AddManualDebt.route.replace("/{contactId}", "/"),
        Screen.EditProfile.route,
        Screen.Settings.route
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBarAndFab = noBottomBarRoutes.none { noBarRoute ->
        currentRoute?.startsWith(noBarRoute.split("?")[0]) == true
    }

    var fabOnClick by remember { mutableStateOf<() -> Unit>({}) }

    // ✅ TAMBAHKAN LOADING STATE SAAT AUTH BELUM DETERMINED
    if (!isUserLoggedIn && startDestinationOuter == Screen.MainTransactions.route) {
        // Show loading atau redirect ke login
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    Scaffold(
        floatingActionButton = {
            if (showBottomBarAndFab) {
                FloatingActionButton(
                    onClick = fabOnClick,
                    shape = CircleShape,
                    containerColor = Color(0xFFFFC727),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .size(FAB_SIZE)
                        .offset(y = 49.dp)
                        .border(2.dp, Color.White, CircleShape)
                ) {
                    Icon(Icons.Filled.Add, "Tambah Transaksi")
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        bottomBar = {
            if (showBottomBarAndFab) {
                BottomAppBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BOTTOM_APP_BAR_HEIGHT)
                        .clip(bottomAppBarShape),
                    containerColor = Color.White,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    NavigationBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp
                    ) {
                        val items = bottomNavItems
                        val itemsCount = items.size
                        val itemColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0f)
                        )

                        if (itemsCount == 2) {
                            val screen1 = items[0]
                            val isSelected1 = currentRoute?.startsWith(screen1.route) == true
                            NavigationBarItem(
                                icon = { Icon(screen1.icon, contentDescription = screen1.label) },
                                label = { Text(screen1.label) },
                                selected = isSelected1,
                                onClick = { navigateToBottomNavItem(navController, screen1.route) },
                                colors = itemColors
                            )
                            Spacer(Modifier.weight(1f))
                            val screen2 = items[1]
                            val isSelected2 = currentRoute?.startsWith(screen2.route) == true
                            NavigationBarItem(
                                icon = { Icon(screen2.icon, contentDescription = screen2.label) },
                                label = { Text(screen2.label) },
                                selected = isSelected2,
                                onClick = { navigateToBottomNavItem(navController, screen2.route) },
                                colors = itemColors
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestinationOuter,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.MainTransactions.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) }
                )
            }
            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }
            composable(Screen.Welcome.route) {
                WelcomeScreen(
                    onNavigateToHome = {
                        navController.navigate(Screen.MainTransactions.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.MainTransactions.route) {
                MainHomeScreen(
                    navController = navController,
                    homeViewModel = homeViewModel,
                    onFabActionReady = { action -> fabOnClick = action }
                )
            }

            composable(Screen.MainDebt.route) {
                EnhancedContactDebtScreen(
                    onContactClick = { contactId ->
                        navController.navigate(Screen.ContactDebtDetail.createRoute(contactId))
                    },
                    onFabActionReady = { action -> fabOnClick = action }
                )
            }


            composable(Screen.ContactDebtDetail.route) { backStackEntry ->
                val contactId =
                    backStackEntry.arguments?.getString(Screen.ContactDebtDetail.ARG_CONTACT_ID)
                        ?: ""
                EnhancedContactDebtDetailScreen(
                    contactId = contactId,
                    onBack = { navController.popBackStack() },
                    onEditDebt = { debtId ->
                        navController.navigate("edit_debt_screen/$debtId")
                    },
                    onDeleteDebt = { debtId ->
                        // bisa tampilkan konfirmasi atau aksi lain
                    },
                    onFabActionReady = { action -> fabOnClick = action }
                )
            }


            composable(Screen.EditProfile.route) {
                EditProfileScreen(
                    onNavigateUp = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() },
                    onAccountDeleted = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    }
                )
            }


            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.AddEditTransaction.route,
                arguments = listOf(
                    navArgument(Screen.AddEditTransaction.ARG_TRANSACTION_ID) {
                        type = NavType.StringType
                        nullable = true
                    },
                    navArgument(Screen.AddEditTransaction.ARG_BUSINESS_UNIT_ID) {
                        type = NavType.StringType
                        nullable = true
                    }
                )
            ) {
                AddEditTransactionScreen(
                    onNavigateUp = { navController.popBackStack() },
                    onTransactionSavedSuccessfully = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.TransactionDetail.route,
                arguments = listOf(navArgument(Screen.TransactionDetail.ARG_TRANSACTION_ID) {
                    type = NavType.StringType
                })
            ) {
                TransactionDetailScreen(navController = navController)
            }

            composable("edit_debt_screen/{debtId}") { backStackEntry ->
                val debtId = backStackEntry.arguments?.getString("debtId") ?: ""
                // TODO: Tambahkan EditDebtScreen jika tersedia
            }
        }
    }
}

private fun navigateToBottomNavItem(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(0) { inclusive = true } // buang semua layar di back stack
        launchSingleTop = true
    }
}
