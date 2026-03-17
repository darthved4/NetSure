package com.example.netsure.security

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Process-wide in-memory app lock.
 *
 * Security note: this intentionally does NOT persist across process death.
 */
object AppLockManager {
    private val unlocked = AtomicBoolean(false)
    private val launchingLock = AtomicBoolean(false)

    fun init(app: Application) {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                // App went to background.
                unlocked.set(false)
            }

            override fun onStart(owner: LifecycleOwner) {
                // App came to foreground.
                // If any activity resumes without unlocking, we route to AppLockActivity.
                launchingLock.set(false)
            }
        })
    }

    fun isUnlocked(): Boolean = unlocked.get()

    fun markUnlocked() {
        unlocked.set(true)
    }

    /**
     * Call from Activities' onResume() to ensure content isn't accessible without auth.
     */
    fun ensureUnlocked(activity: Activity) {
        if (isUnlocked()) return
        if (!launchingLock.compareAndSet(false, true)) return

        val intent = Intent(activity, AppLockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        activity.startActivity(intent)
        activity.overridePendingTransition(0, 0)
    }
}

