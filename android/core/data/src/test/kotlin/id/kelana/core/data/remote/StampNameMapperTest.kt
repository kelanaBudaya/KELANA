package id.kelana.core.data.remote

import id.kelana.core.data.repository.placeDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StampNameMapperTest {
    @Test
    fun dtoWithoutStampName_mapsToNullAndTheCodeLabel() {
        val dto = placeDto()
        assertNull(dto.stampName)
        val place = toPlaces(listOf(dto.toEntity()!!), emptyList()).single()
        assertNull(place.stampName)
        assertEquals("YK-01", place.stampLabel)
    }

    @Test
    fun stampName_travelsFromDtoThroughTheCacheEntityToPlace() {
        val dto = placeDto().copy(stampName = "Penjaga Ramayana")
        val entity = dto.toEntity()!!
        assertEquals("Penjaga Ramayana", entity.stampName)
        val place = toPlaces(listOf(entity), emptyList()).single()
        assertEquals("Penjaga Ramayana", place.stampName)
        assertEquals("Penjaga Ramayana", place.stampLabel)
    }
}
