package id.kelana.feature.paspor

import id.kelana.core.model.AuthResult
import id.kelana.core.model.PendingStamp
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.Region
import id.kelana.core.model.RegionRepository
import id.kelana.core.model.RegionStatus
import id.kelana.core.model.PlaceRepository
import id.kelana.core.model.SessionRepository
import id.kelana.core.model.SessionState
import id.kelana.core.model.Stamp
import id.kelana.core.model.StampRepository
import id.kelana.core.model.StampRequest
import id.kelana.core.model.StampResult
import id.kelana.feature.paspor.data.LocationFix
import id.kelana.feature.paspor.data.LocationFixSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant

fun testPlace(id: String, code: String, category: PlaceCategory = PlaceCategory.Museum) =
    Place(id, id, "Tempat $id", category, code, 0.0, 0.0, "Jl. Uji", "", id)

/**
 * A stamp repository the test controls. [requestStamp] answers [nextResult] and records the request;
 * [queued] / [confirm] / [clearPending] simulate what the real repository and its WorkManager job do later.
 */
class ScriptedStampRepository(
    initialStamps: List<Stamp> = emptyList(),
    var nextResult: StampResult = StampResult.Stamped,
) : StampRepository {
    val stampsState = MutableStateFlow(initialStamps)
    val pendingState = MutableStateFlow<PendingStamp?>(null)
    val requests = mutableListOf<StampRequest>()
    var cancelCalls = 0

    /** When set, [requestStamp] suspends until it completes (to test taps while a request is in flight). */
    var gate: CompletableDeferred<Unit>? = null

    override fun stamps(): Flow<List<Stamp>> = stampsState

    override fun pending(): Flow<PendingStamp?> = pendingState

    override suspend fun requestStamp(request: StampRequest): StampResult {
        requests += request
        gate?.await()
        when (nextResult) {
            // A queued request stays in the local queue until the server answers.
            StampResult.Queued, StampResult.NetworkError -> pendingState.value = PendingStamp(request.placeId, request.capturedAt)
            // The real repository deletes the row after ANY answer; a confirmed answer also fills the stamp cache.
            StampResult.Stamped, StampResult.AlreadyStamped ->
                stampsState.value = stampsState.value + Stamp(request.placeId, request.capturedAt)
            else -> Unit
        }
        return nextResult
    }

    override suspend fun cancelPending() {
        cancelCalls++
        pendingState.value = null
    }

    /** The server answered a queued request with a stamp later (worker): row deleted, stamp cached. */
    fun confirmLater(placeId: String, at: Instant) {
        pendingState.value = null
        stampsState.value = stampsState.value + Stamp(placeId, at)
    }
}

class ScriptedLocation(var next: LocationFix) : LocationFixSource {
    var reads = 0
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun currentFix(): LocationFix {
        reads++
        gate?.await()
        return next
    }
}

fun fix(accuracyM: Double = 12.3, isMock: Boolean = false) =
    LocationFix.Fix(lat = -7.8013, lng = 110.3641, accuracyM = accuracyM, isMock = isMock)

class FakeSession(initial: SessionState) : SessionRepository {
    val state = MutableStateFlow(initial)
    override val sessionState: Flow<SessionState> = state

    override suspend fun signInWithGoogle(idToken: String, nonce: String): AuthResult = AuthResult.Success
    override suspend fun updateDisplayName(name: String): AuthResult = AuthResult.Success

    override suspend fun signOut() {
        state.value = SessionState.Guest
    }
}

/** Places of one or more regions, as the repository would answer. */
class ListPlaces(private val list: List<Place>) : PlaceRepository {
    override fun places(): Flow<List<Place>> = flowOf(list)
    override fun place(id: String): Flow<Place?> = flowOf(list.firstOrNull { it.id == id })
    override fun placesByCategory(category: PlaceCategory): Flow<List<Place>> = flowOf(emptyList())
    override fun search(query: String): Flow<List<Place>> = flowOf(emptyList())
}

/** Regions with a short name, and a selection the test sets directly. */
class FakeRegions(selected: String? = null) : RegionRepository {
    val selectedState = MutableStateFlow(selected)
    private val all = listOf(
        Region("DIY", "DI Yogyakarta", RegionStatus.Available, 1, "Jogja"),
        Region("JTM", "Jawa Timur", RegionStatus.Available, 2, "Jatim"),
        Region("JTG", "Jawa Tengah", RegionStatus.Available, 3, "Jateng"),
    )

    override fun regions(): Flow<List<Region>> = flowOf(all)
    override val selectedRegionId: Flow<String?> = selectedState
    override suspend fun select(regionId: String) {
        selectedState.value = regionId
    }
}
