package id.kelana.core.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Runs the network refresh for one kind of content. A refresh that fails (offline, server error, not configured)
 * changes nothing: the cached rows, and with them every `checkedAt`, stay as they were. A successful refresh is
 * not repeated for [minIntervalMs], so several collectors do not each hit the network.
 */
internal class RefreshGate(
    private val minIntervalMs: Long = DEFAULT_MIN_INTERVAL_MS,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()
    private var lastSuccess: Long? = null

    /** True once the first refresh attempt, successful or not, has finished. */
    val attempted = MutableStateFlow(false)

    suspend fun run(refresh: suspend () -> Unit) {
        mutex.withLock {
            val last = lastSuccess
            if (last == null || now() - last >= minIntervalMs) {
                try {
                    refresh()
                    lastSuccess = now()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Offline or failed: keep the cache untouched.
                }
            }
            attempted.value = true
        }
    }

    companion object {
        const val DEFAULT_MIN_INTERVAL_MS = 60_000L
    }
}

/**
 * Emits what the cache holds first, then again whenever a refresh changes it. An empty cache is not emitted
 * until the first refresh attempt has finished, so a screen does not flash "nothing found" on a cold start.
 */
internal fun <C, T> cacheThenNetwork(
    cache: Flow<C>,
    hasData: (C) -> Boolean,
    gate: RefreshGate,
    refresh: suspend () -> Unit,
    map: (C) -> T,
): Flow<T> = channelFlow {
    var refreshStarted = false
    combine(cache, gate.attempted) { rows, attempted -> rows to attempted }
        .collect { (rows, attempted) ->
            if (attempted || hasData(rows)) send(map(rows))
            // The refresh starts only after the cached rows were handed out, so the cache is always first.
            if (!refreshStarted) {
                refreshStarted = true
                launch { gate.run(refresh) }
            }
        }
}
