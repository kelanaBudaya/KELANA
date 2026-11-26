package id.kelana.core.data.remote

import id.kelana.core.data.repository.closureDto
import id.kelana.core.data.repository.eventDto
import id.kelana.core.data.repository.hoursDto
import id.kelana.core.data.repository.placeDto
import id.kelana.core.data.repository.ticketDto
import id.kelana.core.model.EventTag
import id.kelana.core.model.FactDisplay
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.Recurrence
import id.kelana.core.model.RegionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class MappersTest {

    @Test
    fun checkedAt_nullStaysNull_andShowsChecking() {
        assertNull(parseCheckedAt(null))
        assertNull(parseCheckedAt(""))
        assertNull(parseCheckedAt("bukan tanggal"))
        val fact = hoursDto(checkedAt = null).toFact()!!
        assertNull(fact.checkedAt)
        assertEquals(FactDisplay.Checking, fact.display())
        assertEquals(FactDisplay.Checking, ticketDto(checkedAt = null).toFact().display())
        assertEquals(FactDisplay.Checking, closureDto(checkedAt = null).toFact()!!.display())
    }

    @Test
    fun checkedAt_isConvertedToJakartaDate() {
        // 20:00 UTC on the 20th is already 03:00 on the 21st in Jakarta (UTC+7).
        assertEquals(LocalDate.of(2026, 11, 21), parseCheckedAt("2026-11-20T20:00:00+00:00"))
        assertEquals(LocalDate.of(2026, 11, 20), parseCheckedAt("2026-11-20T03:00:00.123456+00:00"))
        assertEquals(LocalDate.of(2026, 11, 20), parseCheckedAt("2026-11-20"))
    }

    @Test
    fun hours_mapWeekdayAndTimes() {
        val sunday = hoursDto(weekday = 7).toFact()!!
        assertEquals(DayOfWeek.SUNDAY, sunday.value.weekday)
        assertEquals(LocalTime.of(8, 0), sunday.value.opens)
        assertEquals("https://example.test/jam", sunday.sourceUrl)
        assertEquals(DayOfWeek.MONDAY, hoursDto(weekday = 1).toFact()!!.value.weekday)
        assertNull(hoursDto(weekday = 0).toFact())
        assertNull(hoursDto(weekday = 8).toFact())
    }

    @Test
    fun closedDay_hasNoTimes() {
        val dto = hoursDto().copy(isClosed = true, opensAt = null, closesAt = null)
        val v = dto.toFact()!!.value
        assertTrue(v.isClosed)
        assertNull(v.opens)
        assertNull(v.closes)
    }

    @Test
    fun place_usesLatLngAndSlugCoverFallback() {
        val e = placeDto().toEntity()!!
        assertEquals(-7.75, e.lat, 0.0)
        assertEquals("slug-p1", e.coverKey)
        assertEquals("cover/x", placeDto().copy(coverPath = "cover/x").toEntity()!!.coverKey)
        assertNull(placeDto().copy(lat = null).toEntity())
    }

    @Test
    fun places_attachTheirFactsOnly() {
        val payload = PlacesPayload(
            places = listOf(placeDto("p1"), placeDto("p2", "Museum")),
            hours = listOf(hoursDto("p1", 2), hoursDto("p1", 1), hoursDto("p2", 3)),
            closures = listOf(closureDto("p2")),
            tickets = listOf(ticketDto("p1")),
        )
        val places = toPlaces(payload.places.mapNotNull { it.toEntity() }, payload.toFactEntities())
        val p1 = places.first { it.id == "p1" }
        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY), p1.hours.map { it.value.weekday })
        assertEquals(1, p1.tickets.size)
        assertTrue(p1.closures.isEmpty())
        assertEquals(1, places.first { it.id == "p2" }.closures.size)
        assertEquals(PlaceCategory.Candi, p1.category)
    }

    @Test
    fun cachedPlaceWithLegacyCategoryKeyMapsToTheRenamedCategory() {
        val cached = placeDto().copy(category = "topeng").toEntity()!!
        assertEquals(PlaceCategory.Pentas, toPlaces(listOf(cached), emptyList()).single().category)
        val gamelan = placeDto().copy(category = "gamelan").toEntity()!!
        assertEquals(PlaceCategory.SeniMusik, toPlaces(listOf(gamelan), emptyList()).single().category)
    }

    @Test
    fun event_weeklyUsesIsoWeekday() {
        val ev = eventDto(recurrence = "weekly", weekday = 1).toEvent()!!
        assertEquals(Recurrence.Weekly(DayOfWeek.MONDAY), ev.schedule.value.recurrence)
        assertEquals(Recurrence.Weekly(DayOfWeek.SUNDAY), eventDto(weekday = 7).toEvent()!!.schedule.value.recurrence)
        assertNull(eventDto(weekday = null).toEvent())
        assertNull(eventDto(weekday = 8).toEvent())
    }

    @Test
    fun event_onceDailyAndInvalidRecurrence() {
        val once = eventDto(recurrence = "once", weekday = null, date = "2026-12-24").toEvent()!!
        assertEquals(Recurrence.Once(LocalDate.of(2026, 12, 24)), once.schedule.value.recurrence)
        assertNull(eventDto(recurrence = "once", weekday = null, date = null).toEvent())
        assertEquals(Recurrence.Daily, eventDto(recurrence = "daily", weekday = null).toEvent()!!.schedule.value.recurrence)
        assertNull(eventDto(recurrence = "monthly").toEvent())
        assertNull(eventDto(start = "bukan jam").toEvent())
    }

    @Test
    fun event_mapsFieldsAndKeepsKnownTagsOnly() {
        val ev = eventDto().toEvent()!!
        assertEquals("Candi Prambanan", ev.placeName)
        assertEquals("", ev.description)
        assertEquals(PlaceCategory.Tari, ev.category)
        assertEquals(setOf(EventTag.Tari), ev.tags)
        assertEquals(LocalTime.of(19, 30), ev.schedule.value.start)
        assertEquals(LocalTime.of(21, 30), ev.schedule.value.end)
        assertEquals(150000L, ev.ticket.value.priceFromIdr)
        assertEquals(LocalDate.of(2026, 11, 20), ev.schedule.checkedAt)
        assertEquals(30, ev.arrivalAdviceMin)
    }

    @Test
    fun onSiteAvailable_defaultsToFalseAndIsMapped() {
        assertEquals(false, ticketDto().toFact().value.onSiteAvailable)
        assertEquals(true, ticketDto().copy(onSiteAvailable = true).toFact().value.onSiteAvailable)
        assertEquals(false, eventDto().toEvent()!!.ticket.value.onSiteAvailable)
        assertEquals(true, eventDto().copy(onSiteAvailable = true).toEvent()!!.ticket.value.onSiteAvailable)
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val missing = json.decodeFromString<TicketPriceDto>("""{"place_id":"p","label":"x"}""")
        assertEquals(false, missing.onSiteAvailable)
    }

    @Test
    fun event_withoutCheckDate_isChecking() {
        val ev = eventDto().copy(checkedAt = null).toEvent()!!
        assertEquals(FactDisplay.Checking, ev.schedule.display())
        assertEquals(FactDisplay.Checking, ev.ticket.display())
    }

    @Test
    fun cancelledDays_ignoreUncheckedClosures() {
        val rows = listOf(closureDto("p1"), closureDto("p2", checkedAt = null)).mapIndexed { i, c -> c.toEventClosureEntity(i) }
        val map = toCancelledDays(rows)
        assertEquals(setOf("p1"), map.keys)
    }

    @Test
    fun regionStatus_mapping() {
        assertEquals(RegionStatus.Available, parseRegionStatus("available"))
        assertEquals(RegionStatus.Upcoming, parseRegionStatus("upcoming"))
        assertEquals(RegionStatus.NotOpen, parseRegionStatus("not_open"))
        assertEquals("DIY", RegionDto("DIY", "DI Yogyakarta", "available", 1).toEntity().toModel().id)
    }

    @Test
    fun regionShortName_presentNullBlank() {
        assertEquals("Jogja", RegionDto("DIY", "DI Yogyakarta", "available", 1, "Jogja").toEntity().toModel().shortName)
        assertEquals("DI Yogyakarta", RegionDto("DIY", "DI Yogyakarta", "available", 1, null).toEntity().toModel().shortName)
        assertEquals("DI Yogyakarta", RegionDto("DIY", "DI Yogyakarta", "available", 1, "  ").toEntity().toModel().shortName)
    }
}
