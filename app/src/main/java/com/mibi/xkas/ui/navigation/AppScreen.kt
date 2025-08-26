package com.mibi.xkas.ui.navigation

sealed class Screen(val route: String) {

    // == Authentication Screens ==
    object Welcome : Screen("welcome_screen")
    object Login : Screen("login_screen")
    object Register : Screen("register_screen")

    // == Main Application Screens (Bottom Navigation & Core Features) ==
    object MainTransactions : Screen("main_transactions_screen") // Layar utama untuk daftar transaksi (dulu Home/Transactions)
    object MainDebt : Screen("contact_debt_screen")
    object EditProfile : Screen("edit_profile_screen")       // Layar profil pengguna
    object Settings : Screen("settings_screen")


    // == Feature-Specific Screens (Diakses dari layar utama atau lainnya) ==

    /**
     * Layar untuk menambah atau mengedit transaksi.
     * Menggunakan query parameters agar argumen bersifat opsional.
     * - `transactionId`: ID transaksi untuk mode edit (opsional untuk mode tambah).
     * - `businessUnitId`: ID unit bisnis, wajib untuk transaksi baru, bisa opsional untuk edit jika BU tidak berubah.
     */
    object AddEditTransaction : Screen(
        route = "add_edit_transaction_screen?transactionId={transactionId}&businessUnitId={businessUnitId}"
    ) {
        const val ARG_TRANSACTION_ID = "transactionId"
        const val ARG_BUSINESS_UNIT_ID = "businessUnitId"

        fun createRouteForNew(businessUnitId: String): String {
            return "add_edit_transaction_screen?$ARG_BUSINESS_UNIT_ID=$businessUnitId"
        }

        fun createRouteForEdit(transactionId: String): String {
            return "add_edit_transaction_screen?$ARG_TRANSACTION_ID=$transactionId"
        }

        fun createRouteForEditWithBu(transactionId: String, businessUnitId: String): String {
            return "add_edit_transaction_screen?$ARG_TRANSACTION_ID=$transactionId&$ARG_BUSINESS_UNIT_ID=$businessUnitId"
        }
    }

    /**
     * Layar untuk detail transaksi.
     * Menggunakan path parameter untuk transactionId.
     */
    object TransactionDetail : Screen("transaction_detail_screen/{transactionId}") {
        const val ARG_TRANSACTION_ID = "transactionId"
        fun createRoute(transactionId: String) = "transaction_detail_screen/$transactionId"
    }

    /**
     * Layar untuk menambah atau mengedit unit bisnis.
     */
    object AddEditBusinessUnit : Screen("add_edit_business_unit_screen")

    object DebtDetail : Screen("debt_detail_screen/{debtId}") {
        const val ARG_DEBT_ID = "debtId"
        fun createRoute(debtId: String) = "debt_detail_screen/$debtId"
    }

    object ContactDebtDetail : Screen("contact_debt_detail_screen/{contactId}") {
        const val ARG_CONTACT_ID = "contactId"
        fun createRoute(contactId: String) = "contact_debt_detail_screen/$contactId"
    }

    object AddContact : Screen("add_contact_screen")

    object AddManualDebt : Screen("add_manual_debt_screen/{contactId}") {
        const val ARG_CONTACT_ID = "contactId"
        fun createRoute(contactId: String) = "add_manual_debt_screen/$contactId"
    }

    object AddManualDebtDetail : Screen("add_manual_debt_detail_screen/{debtId}") {
        const val ARG_DEBT_ID = "debtId"
    }

}
