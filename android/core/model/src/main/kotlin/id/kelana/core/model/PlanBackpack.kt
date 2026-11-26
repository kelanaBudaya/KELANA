package id.kelana.core.model

import kotlinx.coroutines.flow.Flow

/**
 * The "ransel": places the user added with "Masuk rencana" on Detail tempat. Kept on the device, no account needed,
 * and it survives a process restart. It only holds place ids, in the order they were added; the place itself is
 * read from [PlaceRepository]. The 6-step planner is separate and does not read or write this list yet.
 */
interface PlanBackpackRepository {
    /** Ids of the added places, oldest first. Emits again after every change. */
    fun placeIds(): Flow<List<String>>

    /** No-op when [placeId] is already in the backpack. */
    suspend fun addPlace(placeId: String)

    /** No-op when [placeId] is not in the backpack. */
    suspend fun removePlace(placeId: String)
}
