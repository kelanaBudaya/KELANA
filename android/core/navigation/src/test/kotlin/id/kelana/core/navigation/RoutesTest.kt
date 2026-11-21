package id.kelana.core.navigation

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RoutesTest {
    private val json = Json

    @Test
    fun tempatRoute_roundTripsWithItsArgument() {
        val route = TempatRoute(placeId = "candi-prambanan")
        val encoded = json.encodeToString(TempatRoute.serializer(), route)
        assertEquals("""{"placeId":"candi-prambanan"}""", encoded)
        assertEquals(route, json.decodeFromString(TempatRoute.serializer(), encoded))
    }

    @Test
    fun dataObjectRoute_roundTripsToTheSameInstance() {
        val encoded = json.encodeToString(JelajahRoute.serializer(), JelajahRoute)
        assertEquals(JelajahRoute, json.decodeFromString(JelajahRoute.serializer(), encoded))
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
