package com.example.netsure

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.netsure.navigation.AppNavHost
import com.example.netsure.security.SecureActivity
import com.example.netsure.ui.theme.NetSureTheme
import androidx.compose.ui.graphics.Color

class MainActivity : SecureActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NetSureTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    AppNavHost()
                }
            }
        }
    }
}