package com.contextguard.app.core.network

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.contextguard.app.R

/**
 * Opt-In Android VpnService Extension for Domain-Level Protection.
 *
 * Implements:
 * 1. Explicit user-controlled lifecycle (start / single-tap stop).
 * 2. Graceful revocation handling via onRevoke() when conflicting VPNs start.
 * 3. Network interface monitoring (Wi-Fi <-> Cellular handover) via NetworkCallback.
 * 4. Critical Safety Guardrail:
 *    Refuses to create an unforwarded "blackhole" TUN interface that would silently drop packets.
 *    If a native C user-space TCP/IP forwarder is not linked, it safely abstains from blackholing traffic,
 *    preserving normal device connectivity at all times.
 */
class DomainFilteringVpnService : VpnService() {

    companion object {
        private const val TAG = "ContextGuardVPN"
        const val ACTION_START = "com.contextguard.app.action.START_VPN"
        const val ACTION_STOP = "com.contextguard.app.action.STOP_VPN"
        private const val NOTIFICATION_ID = 2002
        private const val CHANNEL_ID = "contextguard_network_protection"

        /**
         * Checks whether the app has platform permission to establish a VpnService.
         * Returns null if already prepared/authorized, or an Intent to request user permission.
         */
        fun getPrepareIntent(context: Context): Intent? {
            return prepare(context)
        }
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate() {
        super.onCreate()
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        registerNetworkCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_STOP -> {
                stopVpn()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                startVpn()
            }
        }

        return START_STICKY
    }

    /**
     * Starts the domain protection service with persistent foreground status.
     */
    private fun startVpn() {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification("Active — On-device domain safety active"))

        DomainProtectionManager.setStatus(ProtectionStatus.PREPARING)

        // Safety Invariant: Check if reliable native forwarding stack is available
        val hasNativeForwarder = checkNativeForwarderAvailability()

        if (!hasNativeForwarder) {
            // SAFEGUARD: Do not create a TUN interface that consumes packets and silently drops traffic.
            // Maintain safe diagnostic fallback state to ensure normal browsing is NOT broken.
            Log.w(TAG, "Native TCP/IP user-space forwarder not linked; avoiding packet-dropping TUN blackhole to preserve device connectivity.")
            DomainProtectionManager.setStatus(ProtectionStatus.FALLBACK_INACTIVE)
            updateNotification("Diagnostic Mode — Preserving device connectivity")
            return
        }

        try {
            // If reliable forwarding engine is available, establish protected TUN interface
            val builder = Builder()
                .setSession("ContextGuard Domain Protection")
                .addDnsServer("1.1.1.1")
                .addRoute("1.1.1.1", 32) // Route strictly targeted DNS IP; NEVER capture all 0.0.0.0/0 without tun2socks

            vpnInterface = builder.establish()
            DomainProtectionManager.setStatus(ProtectionStatus.ACTIVE)
            Log.i(TAG, "Domain filtering VPN successfully established.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to establish VPN interface: ${e.message}", e)
            DomainProtectionManager.setStatus(ProtectionStatus.FALLBACK_INACTIVE)
        }
    }

    /**
     * Single-tap stop control: tears down VPN interface cleanly.
     */
    private fun stopVpn() {
        DomainProtectionManager.stopProtection()
        cleanupVpnInterface()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Android platform callback triggered when another VPN service is activated.
     */
    override fun onRevoke() {
        Log.w(TAG, "VPN permission revoked by OS (conflicting VPN started).")
        DomainProtectionManager.onVpnRevoked()
        cleanupVpnInterface()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun cleanupVpnInterface() {
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN interface: ${e.message}")
        }
    }

    override fun onDestroy() {
        unregisterNetworkCallback()
        cleanupVpnInterface()
        super.onDestroy()
    }

    /**
     * Checks if native packet forwarding (e.g. tun2socks / lwIP) is linked.
     */
    private fun checkNativeForwarderAvailability(): Boolean {
        // Pure Kotlin build does not bundle compiled native tun2socks binaries.
        // Returning false safely prevents establishing a drop-all blackhole TUN interface.
        return false
    }

    /**
     * Monitors active network capabilities for seamless Wi-Fi <-> Cellular handovers.
     */
    private fun registerNetworkCallback() {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    val isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    DomainProtectionManager.onNetworkTransition(isWifi = isWifi, isCellular = isCellular)
                }

                override fun onLost(network: Network) {
                    DomainProtectionManager.onNetworkTransition(isWifi = false, isCellular = false)
                }
            }

            networkCallback?.let {
                connectivityManager?.registerNetworkCallback(request, it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback: ${e.message}")
        }
    }

    private fun unregisterNetworkCallback() {
        try {
            networkCallback?.let {
                connectivityManager?.unregisterNetworkCallback(it)
            }
            networkCallback = null
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering network callback: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ContextGuard Network Domain Protection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Displays persistent status for opt-in domain network protection"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(statusText: String): Notification {
        val stopIntent = Intent(this, DomainFilteringVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("ContextGuard Network Protection")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_notification_contextguard)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Protection", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildForegroundNotification(statusText))
    }
}
