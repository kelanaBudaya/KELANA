package id.kelana.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TopLevelTabTest {
    @Test
    fun tabsAreInFigmaOrder() {
        assertEquals(
            listOf("Jelajah", "Agenda", "Rencana", "Cerita"),
            TopLevelTab.entries.map { it.name },
        )
    }

    @Test
    fun everyTabHasDistinctRouteAndIcons() {
        val tabs = TopLevelTab.entries
        assertEquals(tabs.size, tabs.map { it.routeClass }.toSet().size)
        assertEquals(tabs.size, tabs.map { it.icon.active }.toSet().size)
        tabs.forEach { assertNotEquals(it.icon.active, it.icon.inactive) }
    }
}
