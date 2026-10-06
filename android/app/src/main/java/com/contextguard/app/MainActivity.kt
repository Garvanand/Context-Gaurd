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
import com.contextguard.app.core.logging.AppLogger
import com.contextguard.app.core.sharesheet.SharesheetPayloadResolver
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
        intent?.let { handleIncomingIntent(it) }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        if (action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE) {
            AppLogger.i("Intercepted Sharesheet action: $action, type=${intent.type}")
            lifecycleScope.launch {
                val payload = SharesheetPayloadResolver.resolve(applicationContext, intent)
                viewModel.processSharesheetPayload(payload)
            }
        }
    }
}

