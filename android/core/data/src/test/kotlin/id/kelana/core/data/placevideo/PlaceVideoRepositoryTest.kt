package id.kelana.core.data.placevideo

import id.kelana.core.model.PlaceVideo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceVideoRepositoryTest {
    private fun dto(id: String, videoId: String, url: String = "https://www.tiktok.com/@a/video/$videoId", caption: String? = null) =
        PlaceVideoDto(id, "p1", url, videoId, caption)

    private class FakeRemote(val rows: List<PlaceVideoDto>) : PlaceVideoRemote {
        override suspend fun fetch(placeId: String) = rows
    }

    @Test
    fun maps_rows_and_drops_unsafe_ones() = runTest {
        val repo = SupabasePlaceVideoRepository(
            FakeRemote(
                listOf(
                    dto("1", "123", caption = "  "),
                    dto("2", "12a"),
                    dto("3", "456", url = "javascript:alert(1)"),
                    dto("4", "789", caption = "Pentas"),
                ),
            ),
        )
        val list = repo.videos("p1").first()
        assertEquals(listOf("1", "4"), list.map { it.id })
        assertEquals(null, list[0].caption)
        assertEquals("Pentas", list[1].caption)
    }

    @Test
    fun empty_remote_gives_empty_list() = runTest {
        assertTrue(SupabasePlaceVideoRepository(FakeRemote(emptyList())).videos("p1").first().isEmpty())
    }

    @Test
    fun fake_filters_by_place() = runTest {
        val repo = FakePlaceVideoRepository(listOf(PlaceVideo("1", "p1", "https://t", "1", null), PlaceVideo("2", "p2", "https://t", "2", null)))
        assertEquals(listOf("1"), repo.videos("p1").first().map { it.id })
    }
}
