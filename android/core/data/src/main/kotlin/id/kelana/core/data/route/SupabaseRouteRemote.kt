package id.kelana.core.data.route

import id.kelana.core.data.auth.SupabaseClientProvider
import id.kelana.core.model.RoutePoint
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calls the `route` Edge Function through the supabase-kt client (it attaches the apikey and the session or anon
 * bearer). Only public place coordinates are sent. Not covered by unit tests (no live calls).
 */
@Singleton
class SupabaseRouteRemote @Inject constructor(
    private val provider: SupabaseClientProvider,
) : RouteRemote {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetch(profile: String, points: List<RoutePoint>): RouteResponseDto? {
        val client = provider.client ?: return null
        return try {
            val body = RouteRequestDto(profile, points.map { RoutePointDto(it.lat, it.lng) })
            val response = client.functions.invoke(function = "route", body = body)
            if (response.status.value !in 200..299) return null
            json.decodeFromString<RouteResponseDto>(response.bodyAsText())
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }
}
