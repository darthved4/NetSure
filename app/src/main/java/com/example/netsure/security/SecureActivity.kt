package com.example.netsure.security

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity

abstract class SecureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Prevent screenshots/screen recording for all derived activities.
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        AppLockManager.ensureUnlocked(this)
    }
}

