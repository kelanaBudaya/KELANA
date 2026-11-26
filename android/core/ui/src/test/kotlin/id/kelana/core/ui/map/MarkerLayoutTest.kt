package id.kelana.core.ui.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkerLayoutTest {
    private val w = 76f
    private val h = 72f
    private val step = 76f

    private fun p(key: String, x: Float, y: Float) = ScreenPoint(key, x, y)

    private fun layout(
        points: List<ScreenPoint>,
        selectedKey: String? = null,
        mode: MarkerOverlap = MarkerOverlap.Cluster,
    ) = layoutMarkers(points, selectedKey, mode, w, h, step)

    // 1. Empty input.
    @Test fun emptyInputGivesEmptyListInBothModes() {
        assertEquals(emptyList<PlacedItem>(), layout(emptyList(), mode = MarkerOverlap.Cluster))
        assertEquals(emptyList<PlacedItem>(), layout(emptyList(), mode = MarkerOverlap.Spread))
    }

    // 2. Two overlapping pins merge into one group at the centroid.
    @Test fun clusterMergesOverlappingPinsAtCentroid() {
        val out = layout(listOf(p("A", 0f, 0f), p("B", 10f, 10f)))
        assertEquals(listOf<PlacedItem>(PlacedItem.Group(listOf("A", "B"), 5f, 5f)), out)
    }

    // 3. Width boundary: exactly one box width apart does not collide.
    @Test fun clusterWidthBoundaryIsStrict() {
        val apart = layout(listOf(p("A", 0f, 0f), p("B", 76f, 0f)))
        assertEquals(listOf<PlacedItem>(PlacedItem.Single("A", 0f, 0f), PlacedItem.Single("B", 76f, 0f)), apart)

        val close = layout(listOf(p("A", 0f, 0f), p("B", 75.9f, 0f)))
        assertEquals(1, close.size)
        assertTrue(close[0] is PlacedItem.Group)
    }

    // 4. Height boundary: exactly one box height apart does not collide.
    @Test fun clusterHeightBoundaryIsStrict() {
        val apart = layout(listOf(p("A", 0f, 0f), p("B", 0f, 72f)))
        assertEquals(listOf<PlacedItem>(PlacedItem.Single("A", 0f, 0f), PlacedItem.Single("B", 0f, 72f)), apart)

        val close = layout(listOf(p("A", 0f, 0f), p("B", 0f, 71f)))
        assertEquals(1, close.size)
        assertTrue(close[0] is PlacedItem.Group)
    }

    // 5. The selected pin is never clustered and comes last.
    @Test fun selectedPinIsNeverClustered() {
        val out = layout(listOf(p("A", 0f, 0f), p("B", 10f, 0f), p("C", 20f, 0f)), selectedKey = "A")
        assertEquals(
            listOf<PlacedItem>(PlacedItem.Group(listOf("B", "C"), 15f, 0f), PlacedItem.Single("A", 0f, 0f)),
            out,
        )
    }

    // 6. Selected pin with one neighbour: no group, selected pin drawn last.
    @Test fun selectedPinWithOneNeighbourGivesNoGroup() {
        val out = layout(listOf(p("A", 0f, 0f), p("B", 10f, 0f)), selectedKey = "A")
        assertEquals(listOf<PlacedItem>(PlacedItem.Single("B", 10f, 0f), PlacedItem.Single("A", 0f, 0f)), out)
    }

    // 7. Identical coordinates all merge.
    @Test fun identicalCoordinatesMergeIntoOneGroup() {
        val out = layout(listOf(p("A", 5f, 5f), p("B", 5f, 5f), p("C", 5f, 5f)))
        assertEquals(listOf<PlacedItem>(PlacedItem.Group(listOf("A", "B", "C"), 5f, 5f)), out)
    }

    // 8. Only the seed is compared: C is 120 px from A, so it stays out even though it is 60 px from B.
    @Test fun overlapIsComparedWithSeedOnly() {
        val out = layout(listOf(p("A", 0f, 0f), p("B", 60f, 0f), p("C", 120f, 0f)))
        assertEquals(listOf<PlacedItem>(PlacedItem.Group(listOf("A", "B"), 30f, 0f), PlacedItem.Single("C", 120f, 0f)), out)
    }

    // 9. Determinism: the same input gives equal output on repeated calls.
    // Shuffled input may give a different grouping (seed-only comparison), so that is not tested.
    @Test fun sameInputGivesSameOutput() {
        val input = listOf(p("A", 0f, 0f), p("B", 60f, 0f), p("C", 120f, 0f), p("D", 300f, 300f))
        assertEquals(layout(input), layout(input))
    }

    // 10. Spread: two pins on the same spot fan out 76 dp apart, centred on the spot.
    @Test fun spreadFansOverlappingPairSideBySide() {
        val out = layout(listOf(p("A", 100f, 100f), p("B", 100f, 100f)), mode = MarkerOverlap.Spread)
        assertEquals(
            listOf<PlacedItem>(
                PlacedItem.Single("A", 62f, 100f, 100f, 100f),
                PlacedItem.Single("B", 138f, 100f, 100f, 100f),
            ),
            out,
        )
        out.forEach { assertTrue((it as PlacedItem.Single).displaced) }
    }

    // 11. Spread: three pins on one spot are centred on it.
    @Test fun spreadCentresThreePinsOnSeed() {
        val out = layout(
            listOf(p("A", 0f, 0f), p("B", 0f, 0f), p("C", 0f, 0f)),
            mode = MarkerOverlap.Spread,
        ).map { it as PlacedItem.Single }
        assertEquals(listOf(-76f, 0f, 76f), out.map { it.x })
        assertEquals(listOf(0f, 0f, 0f), out.map { it.y })
        assertEquals(listOf(0f, 0f, 0f), out.map { it.anchorX })
        assertEquals(listOf(0f, 0f, 0f), out.map { it.anchorY })
    }

    // 12. Spread: no collision means no displacement.
    @Test fun spreadLeavesNonOverlappingPinsInPlace() {
        val out = layout(listOf(p("A", 0f, 0f), p("B", 200f, 0f)), mode = MarkerOverlap.Spread)
        assertEquals(listOf<PlacedItem>(PlacedItem.Single("A", 0f, 0f), PlacedItem.Single("B", 200f, 0f)), out)
        out.forEach { assertFalse((it as PlacedItem.Single).displaced) }
    }

    // 13. Spread never produces a group, even with a selected pin on the same spot.
    @Test fun spreadNeverProducesGroup() {
        val out = layout(listOf(p("A", 50f, 50f), p("B", 50f, 50f)), selectedKey = "A", mode = MarkerOverlap.Spread)
        assertTrue(out.all { it is PlacedItem.Single })
        assertEquals(2, out.size)
    }

    // 14. Selected key not among the points: same result as selectedKey = null.
    @Test fun unknownSelectedKeyActsLikeNoSelection() {
        val input = listOf(p("A", 0f, 0f), p("B", 10f, 10f), p("C", 300f, 300f))
        assertEquals(layout(input, selectedKey = null), layout(input, selectedKey = "ZZZ"))
    }

    // 15. shouldListCluster: zoom and tiny-box rules.
    @Test fun shouldListClusterFollowsZoomAndPointRules() {
        assertTrue(shouldListCluster(listOf(-7.75 to 110.49, -7.76 to 110.50), zoom = 18.0))
        assertTrue(shouldListCluster(listOf(-7.75 to 110.49, -7.76 to 110.50), zoom = 17.5))
        assertFalse(shouldListCluster(listOf(-7.75 to 110.49, -7.76 to 110.50), zoom = 15.0))
        assertTrue(shouldListCluster(listOf(-7.75 to 110.49, -7.75 to 110.49), zoom = 10.0))
        assertFalse(shouldListCluster(emptyList(), zoom = 18.0))
    }
}
