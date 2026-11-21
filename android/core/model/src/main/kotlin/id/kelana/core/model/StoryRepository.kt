package id.kelana.core.model

import kotlinx.coroutines.flow.Flow

/**
 * Read side of visitor stories. Both members are Flows so a Supabase Realtime source can replace the first
 * implementation without changing callers: every new story must be re-emitted as a new list.
 *
 * Access control is enforced by Postgres RLS (any logged-in user reads). Callers must still not subscribe
 * while the session is a guest. A failing flow (network, auth) surfaces as an error state in the UI.
 */
interface StoryRepository {
    /** All stories, newest first by [Story.createdAt]. Empty list when there are none. */
    fun stories(): Flow<List<Story>>

    /** One story, or null when no story has this id (deleted or hidden). */
    fun story(id: String): Flow<Story?>
}
