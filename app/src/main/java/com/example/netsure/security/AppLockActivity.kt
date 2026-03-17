package com.example.netsure.security

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.netsure.MainActivity
import com.example.netsure.ui.theme.NetSureTheme

class AppLockActivity : FragmentActivity() {
    private val keyguardManager by lazy {
        getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    }

    private var authLaunched = false
    private var biometricFailCount = 0

    private val deviceCredentialLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                onAuthenticated()
            } else {
                Toast.makeText(this, "Authentication required", Toast.LENGTH_SHORT).show()
                blockAccess()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContent {
            NetSureTheme(dynamicColor = true) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    LockShield()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (AppLockManager.isUnlocked()) {
            proceedToApp()
            return
        }
        if (!authLaunched) {
            authLaunched = true
            launchBiometricOrFallback()
        }
    }

    private fun launchBiometricOrFallback() {
        val biometricManager = BiometricManager.from(this)
        val canBio = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)

        if (canBio == BiometricManager.BIOMETRIC_SUCCESS) {
            launchBiometric()
        } else {
            launchDeviceCredential()
        }
    }

    private fun launchBiometric() {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onAuthenticated()
                }

                override fun onAuthenticationFailed() {
                    biometricFailCount += 1
                    if (biometricFailCount >= 2) {
                        launchDeviceCredential()
                    } else {
                        Toast.makeText(this@AppLockActivity, "Try again", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // Any error/cancel/unavailable should fall back to device credentials.
                    launchDeviceCredential()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("NetSure Security")
            .setSubtitle("Authenticate to continue")
            .setConfirmationRequired(false)
            .setNegativeButtonText("Use device lock")
            .build()

        prompt.authenticate(promptInfo)
    }

    private fun launchDeviceCredential() {
        if (!keyguardManager.isDeviceSecure) {
            Toast.makeText(
                this,
                "No screen lock set. Set a PIN/pattern/password to use NetSure.",
                Toast.LENGTH_LONG
            ).show()
            blockAccess()
            return
        }

        val intent = keyguardManager.createConfirmDeviceCredentialIntent(
            "NetSure Security",
            "Authenticate to continue"
        )

        if (intent == null) {
            Toast.makeText(this, "Authentication unavailable", Toast.LENGTH_LONG).show()
            blockAccess()
            return
        }

        deviceCredentialLauncher.launch(intent)
    }

    private fun onAuthenticated() {
        AppLockManager.markUnlocked()
        proceedToApp()
    }

    private fun proceedToApp() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
        )
        overridePendingTransition(0, 0)
        finish()
    }

    private fun blockAccess() {
        finishAffinity()
    }
}

@Composable
private fun LockShield() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "NetSure", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Authentication required", style = MaterialTheme.typography.bodyMedium)
    }
}

