package id.kelana.core.navigation

import id.kelana.core.model.PlaceCategory
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RoutesTest {
    private val json = Json

    private fun <T> roundTrip(serializer: KSerializer<T>, value: T) {
        assertEquals(value, json.decodeFromString(serializer, json.encodeToString(serializer, value)))
    }

    @Test
    fun detailTempatRoute_roundTripsWithItsArgument() {
        val route = DetailTempatRoute(placeId = "candi-prambanan")
        val encoded = json.encodeToString(DetailTempatRoute.serializer(), route)
        assertEquals("""{"placeId":"candi-prambanan"}""", encoded)
        assertEquals(route, json.decodeFromString(DetailTempatRoute.serializer(), encoded))
    }

    @Test
    fun dataObjectRoute_roundTripsToTheSameInstance() {
        roundTrip(JelajahRoute.serializer(), JelajahRoute)
    }

    @Test
    fun everyPhase2RouteRoundTrips() {
        roundTrip(IntroRoute.serializer(), IntroRoute(2))
        roundTrip(PilihDaerahRoute.serializer(), PilihDaerahRoute)
        roundTrip(CariRoute.serializer(), CariRoute)
        roundTrip(KategoriRoute.serializer(), KategoriRoute.of(PlaceCategory.Museum))
        roundTrip(SemuaTempatRoute.serializer(), SemuaTempatRoute)
        roundTrip(FotoResmiRoute.serializer(), FotoResmiRoute("p"))
        roundTrip(LaporkanRoute.serializer(), LaporkanRoute("p"))
        roundTrip(AgendaRoute.serializer(), AgendaRoute)
        roundTrip(DetailPentasRoute.serializer(), DetailPentasRoute("e"))
        roundTrip(OfficialSiteRoute.serializer(), OfficialSiteRoute("https://example.org/a?b=1&c=2", "Situs resmi"))
    }

    @Test
    fun kategoriRoute_mapsBackToItsCategory() {
        assertEquals(PlaceCategory.Candi, KategoriRoute.of(PlaceCategory.Candi).placeCategory)
        assertEquals(null, KategoriRoute("bukan-kategori").placeCategory)
    }

    @Test
    fun introRoute_defaultsToFirstPage() {
        assertEquals(0, IntroRoute().page)
    }

    @Test
    fun tabRoutesHaveDistinctSerialNames() {
        val names = listOf(
            JelajahRoute.serializer(),
            AgendaRoute.serializer(),
            RencanaRoute.serializer(),
            CeritaRoute.serializer(),
        ).map { it.descriptor.serialName }
        assertEquals(names.size, names.toSet().size)
        assertNotEquals(names[0], names[1])
    }
}
