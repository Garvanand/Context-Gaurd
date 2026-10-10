package com.contextguard.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.contextguard.app.core.accessibility.ScreenGuardStateManager
import com.contextguard.app.core.logging.AppLogger
import com.contextguard.app.core.notification.NotificationGuardStateManager
import com.contextguard.app.core.sharesheet.SharesheetPayloadResolver
import com.contextguard.app.core.state.AppEntryState
import com.contextguard.app.navigation.ContextGuardNavGraph
import com.contextguard.app.navigation.Screen
import com.contextguard.app.theme.ContextGuardTheme
import com.contextguard.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.i("MainActivity created. Single-Activity architecture active.")

        handleIncomingIntent(intent)

        setContent {
            ContextGuardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Observe pending navigation triggered by Sharesheet or ViewModel
                    val pendingRoute by viewModel.pendingNavigation.collectAsState()
                    LaunchedEffect(pendingRoute) {
                        pendingRoute?.let { route ->
                            navController.navigate(route) {
                                launchSingleTop = true
                            }
                            viewModel.consumeNavigation()
                        }
                    }

                    ContextGuardNavGraph(
                        navController = navController,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            setIntent(it)
            handleIncomingIntent(it)
        }
    }

    private fun handleIncomingIntent(incomingIntent: Intent?) {
        if (incomingIntent == null) return
        val action = incomingIntent.action

        when {
            action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE -> {
                AppLogger.i("Intercepted Sharesheet action: $action, type=${incomingIntent.type}")
                lifecycleScope.launch {
                    val payload = SharesheetPayloadResolver.resolve(applicationContext, incomingIntent)
                    viewModel.processSharesheetPayload(payload)
                }
                // Reset intent to prevent re-processing on activity configuration recreation
                setIntent(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER))
            }
            incomingIntent.getBooleanExtra("EXTRA_REVIEW_RISK", false) || action == "com.contextguard.app.ACTION_REVIEW_RISK" -> {
                val targetPkg = incomingIntent.getStringExtra("EXTRA_TARGET_PACKAGE") ?: ""
                val rationale = incomingIntent.getStringExtra("EXTRA_RATIONALE") ?: ""
                val riskScore = incomingIntent.getFloatExtra("EXTRA_RISK_SCORE", 0.75f)
                val intervention = incomingIntent.getStringExtra("EXTRA_INTERVENTION") ?: "WARN"
                val candidateAction = incomingIntent.getStringExtra("EXTRA_CANDIDATE_ACTION") ?: "ACTION"
                AppLogger.i("Handling JIT overlay risk review for $targetPkg [$intervention]")
                viewModel.prepareRiskReview(
                    targetPackage = targetPkg,
                    rationale = rationale,
                    riskScore = riskScore,
                    intervention = intervention,
                    candidateAction = candidateAction
                )
                setIntent(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER))
            }
            action == Intent.ACTION_MAIN || action == null -> {
                val isScreenEnabled = ScreenGuardStateManager.isAccessibilityEnabledInSettings(this)
                val isNotifEnabled = NotificationGuardStateManager.isNotificationAccessGranted(this)
                val isScreenConnected = ScreenGuardStateManager.isServiceConnected.value
                val isNotifConnected = NotificationGuardStateManager.isListenerConnected.value
                val isPaused = ScreenGuardStateManager.isMonitoringPaused.value || NotificationGuardStateManager.isProtectionPaused.value

                val entryState = when {
                    !isScreenEnabled && !isNotifEnabled -> AppEntryState.SetupRequired
                    isPaused -> AppEntryState.ProtectionPaused
                    isScreenConnected || isNotifConnected -> AppEntryState.ProtectionActive
                    else -> AppEntryState.NormalLaunch
                }
                viewModel.setEntryState(entryState)
                AppLogger.i("Normal launcher entry resolved to: $entryState")
            }
        }
    }
}

