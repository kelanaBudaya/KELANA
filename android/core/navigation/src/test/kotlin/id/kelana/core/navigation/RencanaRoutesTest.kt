package id.kelana.core.navigation

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class RencanaRoutesTest {
    private fun <T> roundTrip(serializer: KSerializer<T>, value: T) {
        assertEquals(value, Json.decodeFromString(serializer, Json.encodeToString(serializer, value)))
    }

    @Test
    fun everyRencanaRouteRoundTrips() {
        roundTrip(SusunGraphRoute.serializer(), SusunGraphRoute)
        roundTrip(SusunTanggalRoute.serializer(), SusunTanggalRoute)
        roundTrip(SusunTemanRoute.serializer(), SusunTemanRoute)
        roundTrip(SusunMinatRoute.serializer(), SusunMinatRoute)
        roundTrip(SusunWaktuRoute.serializer(), SusunWaktuRoute)
        roundTrip(SusunKendaraanRoute.serializer(), SusunKendaraanRoute)
        roundTrip(SusunKebutuhanRoute.serializer(), SusunKebutuhanRoute)
        roundTrip(MenyusunRoute.serializer(), MenyusunRoute)
        roundTrip(RencanaOfflineRoute.serializer(), RencanaOfflineRoute)
        roundTrip(PetaRuteRoute.serializer(), PetaRuteRoute)
        roundTrip(PengingatRoute.serializer(), PengingatRoute("roro-jonggrang-jumat"))
        roundTrip(TempatTutupRoute.serializer(), TempatTutupRoute("keraton-yogyakarta", "2026-11-20"))
    }

    @Test
    fun argumentsUseTheFieldNamesViewModelsReadFromTheSavedStateHandle() {
        assertEquals(
            """{"placeId":"keraton-yogyakarta","date":"2026-11-20"}""",
            Json.encodeToString(TempatTutupRoute.serializer(), TempatTutupRoute("keraton-yogyakarta", "2026-11-20")),
        )
        assertEquals("""{"eventId":"e1"}""", Json.encodeToString(PengingatRoute.serializer(), PengingatRoute("e1")))
    }
}
