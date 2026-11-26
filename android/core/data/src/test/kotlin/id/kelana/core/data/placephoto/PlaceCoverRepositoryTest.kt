package id.kelana.core.data.placephoto

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceCoverRepositoryTest {
    private val base = "https://abc.supabase.co/"

    private fun row(
        place: String,
        path: String,
        attribution: String = "Foto: A · CC BY 4.0",
        source: String = "https://commons.wikimedia.org/wiki/File:A.jpg",
        license: String = "https://creativecommons.org/licenses/by/4.0/",
    ) = PlaceCoverRowDto(place, path, attribution, license, source)

    @Test
    fun first_row_per_place_wins() {
        val covers = selectCovers(listOf(row("p1", "a_t.jpg"), row("p1", "b_t.jpg"), row("p2", "c_t.jpg")), base)
        assertEquals("https://abc.supabase.co/storage/v1/object/public/place-photos/a_t.jpg", covers["p1"])
        assertEquals(2, covers.size)
    }

    @Test
    fun place_without_rows_is_absent() {
        assertEquals(emptyMap<String, String>(), selectCovers(emptyList(), base))
    }

    @Test
    fun unusable_first_row_falls_through_to_next() {
        val covers = selectCovers(
            listOf(row("p1", "a_t.jpg", attribution = " "), row("p1", "b_t.jpg", source = "http://x"), row("p1", "c_t.jpg")),
            base,
        )
        assertEquals("https://abc.supabase.co/storage/v1/object/public/place-photos/c_t.jpg", covers["p1"])
    }

    @Test
    fun blank_base_url_gives_nothing() {
        assertEquals(emptyMap<String, String>(), selectCovers(listOf(row("p1", "a_t.jpg")), ""))
    }

    private class Remote(var rows: List<PlaceCoverRowDto>, var fail: Boolean = false) : PlaceCoverRemote {
        override suspend fun fetchRows(): List<PlaceCoverRowDto> = if (fail) error("offline") else rows
    }

    @Test
    fun repository_emits_empty_then_covers_and_survives_failure() = runTest {
        val remote = Remote(listOf(row("p1", "a_t.jpg")))
        val repo = SupabasePlaceCoverRepository(remote, base)
        val first = repo.covers().toList()
        assertEquals(listOf(emptyMap(), mapOf("p1" to "https://abc.supabase.co/storage/v1/object/public/place-photos/a_t.jpg")), first)
        remote.fail = true
        val second = repo.covers().toList()
        assertEquals(1, second.size)
        assertEquals(first.last(), second.single())
    }
}
