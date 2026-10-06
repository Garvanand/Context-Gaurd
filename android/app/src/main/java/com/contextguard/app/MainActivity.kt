package com.contextguard.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.contextguard.app.core.logging.AppLogger
import com.contextguard.app.navigation.ContextGuardNavGraph
import com.contextguard.app.theme.ContextGuardTheme
import com.contextguard.app.ui.viewmodel.MainViewModel

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

    private fun handleIncomingIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND) {
            val type = intent.type ?: ""
            AppLogger.i("Intercepted ACTION_SEND with MIME type: $type")
            if (type.startsWith("image/")) {
                intent.clipData?.let { clipData ->
                    if (clipData.itemCount > 0) {
                        val uri = clipData.getItemAt(0).uri
                        AppLogger.i("Sharesheet incoming image URI: $uri")
                    }
                }
            } else if (type == "text/plain") {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                AppLogger.i("Sharesheet incoming text: ${sharedText?.take(30)}...")
            }
        }
    }
}
