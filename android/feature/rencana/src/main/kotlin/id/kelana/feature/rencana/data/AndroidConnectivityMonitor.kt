package id.kelana.feature.rencana.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import id.kelana.core.model.ConnectivityMonitor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/** Network state from [ConnectivityManager]. Needs ACCESS_NETWORK_STATE, declared in this module's manifest. */
@Singleton
class AndroidConnectivityMonitor @Inject constructor(
    @ApplicationContext context: Context,
) : ConnectivityMonitor {
    private val manager = context.getSystemService(ConnectivityManager::class.java)

    override fun isOnlineNow(): Boolean = manager?.let { hasInternet(it, it.activeNetwork) } ?: false

    override val isOnline: Flow<Boolean> = callbackFlow {
        val cm = manager
        if (cm == null) {
            trySend(false)
            close()
            return@callbackFlow
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(hasInternet(cm, network))
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(capabilities.has())
            }

            override fun onLost(network: Network) {
                trySend(isOnlineNow())
            }
        }
        trySend(isOnlineNow())
        cm.registerDefaultNetworkCallback(callback)
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    private fun hasInternet(cm: ConnectivityManager, network: Network?): Boolean =
        network?.let { cm.getNetworkCapabilities(it)?.has() } ?: false

    private fun NetworkCapabilities.has(): Boolean =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
