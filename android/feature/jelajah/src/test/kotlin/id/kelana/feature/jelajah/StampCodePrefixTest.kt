package id.kelana.feature.jelajah

import id.kelana.core.data.repository.FakePlaceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StampCodePrefixTest {
    private suspend fun places() = FakePlaceRepository().places().first().let { list -> list }

    private fun withCode(code: String) = PreviewData.place("X", id.kelana.core.model.PlaceCategory.Museum).copy(capCode = code)

    @Test fun prefixIsTheTextBeforeTheDash() {
        assertEquals("YK", stampCodePrefix(listOf(withCode("YK-02")), "DIY"))
        assertEquals("JTG", stampCodePrefix(listOf(withCode("JTG-05")), "JTG"))
        assertEquals("JTM", stampCodePrefix(listOf(withCode("JTM-03")), "JTM"))
        assertEquals("JBR", stampCodePrefix(listOf(withCode("JBR-01")), "JBR"))
        assertEquals("JKT", stampCodePrefix(listOf(withCode("JKT-07")), "JKT"))
        assertEquals("BTN", stampCodePrefix(listOf(withCode("BTN-02")), "BTN"))
    }

    @Test fun noPlacesFallsBackToRegionId() {
        assertEquals("JTM", stampCodePrefix(emptyList(), "JTM"))
    }

    @Test fun malformedCodeFallsBackToRegionId() {
        assertEquals("JTM", stampCodePrefix(listOf(withCode("JTM03")), "JTM"))
        assertEquals("JTM", stampCodePrefix(listOf(withCode("-03")), "JTM"))
        assertEquals("JTM", stampCodePrefix(listOf(withCode("")), "JTM"))
    }

    @Test fun sampleDataUsesYk() = runTest {
        assertEquals("YK", stampCodePrefix(places(), "DIY"))
    }
}
