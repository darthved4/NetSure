package com.example.netsure

import android.app.Application
import com.example.netsure.security.AppLockManager

class NetSureApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLockManager.init(this)
    }
}

