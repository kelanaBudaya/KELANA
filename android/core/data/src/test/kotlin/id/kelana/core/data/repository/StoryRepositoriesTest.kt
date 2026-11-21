package id.kelana.core.data.repository

import id.kelana.core.model.PostcardStory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class StoryRepositoriesTest {
    private fun story(id: String, at: String) = PostcardStory(
        id = id, placeId = "p", placeName = "Tempat Uji", authorName = "Penulis Uji", createdAt = Instant.parse(at),
    )

    @Test
    fun productionBinding_servesNothing() = runTest {
        val repo = EmptyStoryRepository()
        assertTrue(repo.stories().first().isEmpty())
        assertNull(repo.story("any").first())
    }

    @Test
    fun fake_startsEmpty_sortsNewestFirst_andPostIsLive() = runTest {
        val repo = FakeStoryRepository()
        assertTrue(repo.stories().first().isEmpty())
        repo.post(story("old", "2026-11-18T10:00:00Z"))
        repo.post(story("new", "2026-11-20T10:00:00Z"))
        assertEquals(listOf("new", "old"), repo.stories().first().map { it.id })
        assertEquals("old", repo.story("old").first()?.id)
        assertNull(repo.story("missing").first())
    }
}
