package id.kelana.core.data.route

import id.kelana.core.model.RoutePoint
import id.kelana.core.model.TravelMode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private class FakeRemote(var failing: Boolean = false, var badCount: Boolean = false) : RouteRemote {
    val calls = mutableListOf<Pair<String, List<RoutePoint>>>()

    override suspend fun fetch(profile: String, points: List<RoutePoint>): RouteResponseDto? {
        calls += profile to points
        if (failing) return null
        val legs = (0 until points.size - 1).map { i ->
            val a = points[i]
            val b = points[i + 1]
            RouteLegDto(60, 100, encodePolyline(listOf(a, RoutePoint((a.lat + b.lat) / 2, a.lng + 0.001), b)))
        }
        return RouteResponseDto(if (badCount) legs.drop(1) else legs, "valhalla")
    }
}

private fun pts(n: Int) = (0 until n).map { RoutePoint(-7.8 + it * 0.001, 110.36 + it * 0.001) }

class SupabaseRouteRepositoryTest {
    @Test
    fun modesMapToServerProfiles() {
        assertEquals("car", profileOf(TravelMode.Car))
        assertEquals("car", profileOf(TravelMode.Transit))
        assertEquals("motorcycle", profileOf(TravelMode.RideHailing))
        assertEquals("walk", profileOf(TravelMode.Walk))
    }

    @Test
    fun chunksOverlapOnOnePointAndCoverEveryLeg() {
        assertEquals(emptyList<IntRange>(), chunkRanges(1))
        assertEquals(listOf(0..1), chunkRanges(2))
        assertEquals(listOf(0..9), chunkRanges(10))
        assertEquals(listOf(0..9, 9..10), chunkRanges(11))
        assertEquals(listOf(0..9, 9..18, 18..19), chunkRanges(20))
        // Total legs = size - 1.
        assertEquals(24, chunkRanges(25).sumOf { it.last - it.first })
    }

    @Test
    fun returnsOneDecodedLegPerPairAndSendsTheProfile() = runTest {
        val remote = FakeRemote()
        val legs = SupabaseRouteRepository(remote).route(TravelMode.RideHailing, pts(3))!!
        assertEquals(2, legs.size)
        assertEquals(3, legs[0].path.size)
        assertEquals(1, remote.calls.size)
        assertEquals("motorcycle", remote.calls[0].first)
        assertEquals(3, remote.calls[0].second.size)
    }

    @Test
    fun longRoutesAreSplitIntoRequestsOfAtMostTenPoints() = runTest {
        val remote = FakeRemote()
        val legs = SupabaseRouteRepository(remote).route(TravelMode.Car, pts(12))!!
        assertEquals(11, legs.size)
        assertEquals(listOf(10, 3), remote.calls.map { it.second.size })
    }

    @Test
    fun secondCallWithSameRouteIsServedFromCache() = runTest {
        val remote = FakeRemote()
        val repo = SupabaseRouteRepository(remote)
        repo.route(TravelMode.Car, pts(3))
        repo.route(TravelMode.Car, pts(3))
        assertEquals(1, remote.calls.size)
        repo.route(TravelMode.Walk, pts(3)) // another profile is another key
        assertEquals(2, remote.calls.size)
    }

    @Test
    fun anyFailureGivesNullAndIsNotCached() = runTest {
        val remote = FakeRemote(failing = true)
        val repo = SupabaseRouteRepository(remote)
        assertNull(repo.route(TravelMode.Car, pts(3)))
        remote.failing = false
        assertEquals(2, repo.route(TravelMode.Car, pts(3))!!.size)
    }

    @Test
    fun wrongLegCountIsRejected() = runTest {
        assertNull(SupabaseRouteRepository(FakeRemote(badCount = true)).route(TravelMode.Car, pts(3)))
    }

    @Test
    fun lessThanTwoPointsIsNull() = runTest {
        assertNull(SupabaseRouteRepository(FakeRemote()).route(TravelMode.Car, pts(1)))
    }
}
