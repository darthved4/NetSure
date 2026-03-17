package com.example.netsure.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.netsure.ui.screens.ConfirmPaymentScreen
import com.example.netsure.ui.screens.HomeScreen
import com.example.netsure.ui.screens.QRScannerScreen
import com.example.netsure.viewmodel.NetworkViewModel
import com.example.netsure.viewmodel.PaymentViewModel
import com.example.netsure.ui.screens.TransactionHistoryScreen

/**
 * Central navigation graph for the prototype.
 *
 * Defines three routes:
 *  - HomeScreen
 *  - QRScannerScreen
 *  - ConfirmPaymentScreen
 */

object Routes {
    const val HOME = "home"
    const val QR_SCANNER = "qrScanner"
    const val CONFIRM_PAYMENT = "confirmPayment"

    const val TRANSACTION_HISTORY = "transaction_history"
}

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    // ViewModels are scoped to the NavHost / activity.
    val networkViewModel: NetworkViewModel = viewModel()
    val paymentViewModel: PaymentViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                networkViewModel = networkViewModel,
                onScanClick = {
                    navController.navigate(Routes.QR_SCANNER)
                },
                onTransactionHistoryClick = {
                    navController.navigate(Routes.TRANSACTION_HISTORY)
                }
            )
        }

        // 👇 ADD THIS HERE
        composable(Routes.TRANSACTION_HISTORY) {
            TransactionHistoryScreen()
        }

        composable(Routes.QR_SCANNER) {
            QRScannerScreen(
                paymentViewModel = paymentViewModel,
                onBack = { navController.popBackStack() },
                onUpiDetected = { upiId ->
                    paymentViewModel.onUpiScanned(upiId)
                    navController.navigate(Routes.CONFIRM_PAYMENT) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                }
            )
        }

        composable(Routes.CONFIRM_PAYMENT) {
            ConfirmPaymentScreen(
                paymentViewModel = paymentViewModel,
                onBack = { navController.popBackStack(Routes.HOME, inclusive = false) }
            )
        }
    }
}