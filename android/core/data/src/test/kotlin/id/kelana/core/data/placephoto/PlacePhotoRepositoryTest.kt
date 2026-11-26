package id.kelana.core.data.placephoto

import id.kelana.core.database.dao.PlacePhotoDao
import id.kelana.core.database.entity.PlacePhotoEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
            FakePlacePhotoDao(), base, 0L,
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
        assertTrue(SupabasePlacePhotoRepository(FakeRemote(listOf(dto("1"))), FakePlacePhotoDao(), "", 0L).photos("p1").first().isEmpty())
    }

    @Test
    fun failure_without_cache_propagates() = runTest {
        val repo = SupabasePlacePhotoRepository(FakeRemote(emptyList(), fail = true), FakePlacePhotoDao(), base, 0L)
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
        val repo = SupabasePlacePhotoRepository(remote, FakePlacePhotoDao(), base, 0L)
        repo.photos("p1").first()
        remote.fail = true
        val emissions = repo.photos("p1").toList()
        assertEquals(1, emissions.size)
        assertEquals(listOf("1"), emissions[0].map { it.id })
    }


    @Test
    fun offline_start_with_persisted_cache_returns_cached_list() = runTest {
        val dao = FakePlacePhotoDao()
        val online = SupabasePlacePhotoRepository(FakeRemote(listOf(dto("1"), dto("2"))), dao, base, 0L)
        online.photos("p1").toList()
        // New repository instance (process restart) sharing only the database, with no network.
        val restarted = SupabasePlacePhotoRepository(FakeRemote(emptyList(), fail = true), dao, base, 0L)
        val emissions = restarted.photos("p1").toList()
        assertEquals(1, emissions.size)
        assertEquals(listOf("1", "2"), emissions[0].map { it.id })
    }

    @Test
    fun refresh_emits_cache_first_then_replaces_rows_removed_upstream() = runTest {
        val dao = FakePlacePhotoDao()
        val remote = FakeRemote(listOf(dto("1"), dto("2")))
        val repo = SupabasePlacePhotoRepository(remote, dao, base, 0L)
        repo.photos("p1").toList()
        remote.rows = listOf(dto("2"), dto("3"))
        val emissions = repo.photos("p1").toList()
        assertEquals(listOf(listOf("1", "2"), listOf("2", "3")), emissions.map { l -> l.map { it.id } })
        assertEquals(listOf("2", "3"), dao.get("p1").map { it.id })
    }

    @Test
    fun failed_refresh_keeps_persisted_rows_and_other_places() = runTest {
        val dao = FakePlacePhotoDao()
        val remote = FakeRemote(listOf(dto("1")))
        val repo = SupabasePlacePhotoRepository(remote, dao, base, 0L)
        repo.photos("p1").toList()
        remote.fail = true
        repo.photos("p1").toList()
        assertEquals(listOf("1"), dao.get("p1").map { it.id })
    }

    @Test
    fun empty_fresh_list_clears_the_place_after_successful_refresh() = runTest {
        val dao = FakePlacePhotoDao()
        val remote = FakeRemote(listOf(dto("1")))
        val repo = SupabasePlacePhotoRepository(remote, dao, base, 0L)
        repo.photos("p1").toList()
        remote.rows = emptyList()
        repo.photos("p1").toList()
        assertTrue(dao.get("p1").isEmpty())
    }

    @Test
    fun gate_skips_second_refresh_within_interval() = runTest {
        val dao = FakePlacePhotoDao()
        val remote = CountingRemote(listOf(dto("1")))
        val repo = SupabasePlacePhotoRepository(remote, dao, base, 60_000L)
        repo.photos("p1").toList()
        val second = repo.photos("p1").toList()
        assertEquals(1, remote.count)
        assertEquals(1, second.size)
    }

    private class CountingRemote(var rows: List<PlacePhotoDto>) : PlacePhotoRemote {
        var count = 0
        override suspend fun fetch(placeId: String): List<PlacePhotoDto> { count++; return rows }
    }
    @Test
    fun fake_filters_by_place() = runTest {
        val repo = FakePlacePhotoRepository(FakePlacePhotoRepository.samplePhotos("p1") + FakePlacePhotoRepository.samplePhotos("p2"))
        assertEquals(4, repo.photos("p1").first().size)
        assertTrue(repo.photos("zzz").first().isEmpty())
    }
}

class FakePlacePhotoDao : PlacePhotoDao() {
    private val rows = MutableStateFlow<List<PlacePhotoEntity>>(emptyList())
    override fun observe(placeId: String): Flow<List<PlacePhotoEntity>> =
        rows.map { l -> l.filter { it.placeId == placeId }.sortedBy { it.position } }
    override suspend fun get(placeId: String): List<PlacePhotoEntity> = observe(placeId).first()
    override suspend fun clear(placeId: String) { rows.value = rows.value.filter { it.placeId != placeId } }
    override suspend fun insertAll(rows: List<PlacePhotoEntity>) { this.rows.value = this.rows.value + rows }
    override suspend fun replaceForPlace(placeId: String, rows: List<PlacePhotoEntity>) {
        this.rows.value = this.rows.value.filter { it.placeId != placeId } + rows
    }
}
