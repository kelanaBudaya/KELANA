package id.kelana.core.data.repository

import id.kelana.core.model.Story
import id.kelana.core.model.StoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PRODUCTION binding until the Supabase-backed repository exists. It always serves an empty feed and no story,
 * so a real build can never show invented content. It holds no data at all.
 */
@Singleton
class EmptyStoryRepository @Inject constructor() : StoryRepository {
    override fun stories(): Flow<List<Story>> = flowOf(emptyList())
    override fun story(id: String): Flow<Story?> = flowOf(null)
}

/**
 * In-memory repository for tests and previews only. It is NOT bound in Hilt and starts empty:
 * callers pass the stories they need, and [post] simulates a live insert (a Realtime event).
 */
class FakeStoryRepository(initial: List<Story> = emptyList()) : StoryRepository {
    private val all = MutableStateFlow(initial)

    override fun stories(): Flow<List<Story>> = all.map { list -> list.sortedByDescending { it.createdAt } }

    override fun story(id: String): Flow<Story?> = all.map { list -> list.firstOrNull { it.id == id } }

    fun post(story: Story) {
        all.value = all.value + story
    }
}
