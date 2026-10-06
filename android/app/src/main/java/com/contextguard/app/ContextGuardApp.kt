package com.contextguard.app

import android.app.Application
import com.contextguard.app.core.logging.AppLogger

class ContextGuardApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLogger.i("ContextGuard Application initialized successfully")
    }
}
