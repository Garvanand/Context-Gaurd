package com.contextguard.app

import android.app.Application
import com.contextguard.app.core.engine.OnDeviceDecisionEngine
import com.contextguard.app.core.engine.ScamMessageClassifier
import com.contextguard.app.core.engine.UrlTreeInferenceEngine
import com.contextguard.app.core.logging.AppLogger
import com.contextguard.app.core.notification.NotificationThreatAnalyzer

class ContextGuardApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLogger.i("ContextGuard Application initialized successfully")
        try {
            val urlEngine = UrlTreeInferenceEngine.getInstance(this)
            val msgClassifier = ScamMessageClassifier.getInstance(this)
            OnDeviceDecisionEngine.setUrlEngine(urlEngine)
            NotificationThreatAnalyzer.setModels(urlEngine, msgClassifier)
            AppLogger.i("On-device ML models loaded into runtime engines successfully")
        } catch (e: Exception) {
            AppLogger.e("Error initializing on-device ML models: ${e.message}")
        }
    }
}
