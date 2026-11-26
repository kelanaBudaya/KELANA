package id.kelana.core.ui.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThinPathTest {
    @Test fun shortPathIsUntouched() {
        val path = (0 until 100).map { it.toDouble() to 0.0 }
        assertEquals(path, thinPath(path))
    }

    @Test fun longPathIsThinnedAndKeepsFirstAndLast() {
        val path = (0 until 7_001).map { it.toDouble() to 1.0 }
        val thin = thinPath(path)
        assertTrue(thin.size <= 2_100)
        assertEquals(path.first(), thin.first())
        assertEquals(path.last(), thin.last())
    }
}
