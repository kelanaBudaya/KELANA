package id.kelana.core.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SelectedRegionTest {
    private val diy = Region("DIY", "DI Yogyakarta", RegionStatus.Available, 1, "Jogja")
    private val bali = Region("BALI", "Bali", RegionStatus.Available, 2)

    private class Repo(list: List<Region>, selected: String?) : RegionRepository {
        val sel = MutableStateFlow(selected)
        private val all = list
        override fun regions(): Flow<List<Region>> = flowOf(all)
        override val selectedRegionId: Flow<String?> = sel
        override suspend fun select(regionId: String) { sel.value = regionId }
    }

    @Test
    fun selectedIdFound_returnsRegion() = runTest {
        assertEquals(diy, Repo(listOf(bali, diy), "DIY").selectedRegion().first())
    }

    @Test
    fun unknownId_returnsNull() = runTest {
        assertNull(Repo(listOf(diy), "XX").selectedRegion().first())
    }

    @Test
    fun noSelection_returnsNull() = runTest {
        assertNull(Repo(listOf(diy), null).selectedRegion().first())
    }

    @Test
    fun shortName_defaultsToName() {
        assertEquals("Bali", bali.shortName)
        assertEquals("Jogja", diy.shortName)
    }
}
