package id.kelana.core.data.placephoto

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PlacePhotoRepositoryTest {
    private val base = "https://abc.supabase.co/"

    private fun dto(
        id: String,
        attribution: String = "Foto: A · CC BY 4.0 · Wikimedia Commons",
        source: String = "https://commons.wikimedia.org/wiki/File:A.jpg",
        license: String = "https://creativecommons.org/licenses/by/4.0/",
        caption: String? = null,
    ) = PlacePhotoDto(id, "p1", "diy/x/$id.jpg", "diy/x/${id}_t.jpg", 1600, 1000, caption, attribution, "CC-BY-4.0", license, source)

    private class FakeRemote(var rows: List<PlacePhotoDto>, var fail: Boolean = false) : PlacePhotoRemote {
        override suspend fun fetch(placeId: String): List<PlacePhotoDto> {
            if (fail) error("offline")
            return rows
        }
    }

    @Test
    fun maps_rows_builds_public_urls_and_drops_unsafe_ones() = runTest {
        val repo = SupabasePlacePhotoRepository(
            FakeRemote(
                listOf(
                    dto("1", caption = "  "),
                    dto("2", attribution = "   "),
                    dto("3", source = "http://commons.wikimedia.org/x"),
                    dto("4", license = "javascript:alert(1)"),
                    dto("5", caption = "Gerbang"),
                ),
            ),
            base,
        )
        val list = repo.photos("p1").first()
        assertEquals(listOf("1", "5"), list.map { it.id })
        assertEquals("https://abc.supabase.co/storage/v1/object/public/place-photos/diy/x/1.jpg", list[0].url)
        assertEquals("https://abc.supabase.co/storage/v1/object/public/place-photos/diy/x/1_t.jpg", list[0].thumbUrl)
        assertEquals(null, list[0].caption)
        assertEquals("Gerbang", list[1].caption)
    }

    @Test
    fun not_configured_url_gives_no_photos() = runTest {
        assertTrue(SupabasePlacePhotoRepository(FakeRemote(listOf(dto("1"))), "").photos("p1").first().isEmpty())
    }

    @Test
    fun failure_without_cache_propagates() = runTest {
        val repo = SupabasePlacePhotoRepository(FakeRemote(emptyList(), fail = true), base)
        try {
            repo.photos("p1").first()
            fail("expected exception")
        } catch (e: IllegalStateException) {
            assertEquals("offline", e.message)
        }
    }

    @Test
    fun failure_with_cache_keeps_cached_list() = runTest {
        val remote = FakeRemote(listOf(dto("1")))
        val repo = SupabasePlacePhotoRepository(remote, base)
        repo.photos("p1").first()
        remote.fail = true
        val emissions = repo.photos("p1").toList()
        assertEquals(1, emissions.size)
        assertEquals(listOf("1"), emissions[0].map { it.id })
    }

    @Test
    fun fake_filters_by_place() = runTest {
        val repo = FakePlacePhotoRepository(FakePlacePhotoRepository.samplePhotos("p1") + FakePlacePhotoRepository.samplePhotos("p2"))
        assertEquals(4, repo.photos("p1").first().size)
        assertTrue(repo.photos("zzz").first().isEmpty())
    }
}
