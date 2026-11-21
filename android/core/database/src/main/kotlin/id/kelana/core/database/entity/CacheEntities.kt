package id.kelana.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Offline cache of public content only (regions, places and their official facts, events). It never stores
 * user location or any visitor content (stamps, postcards, videos, reports). Dates stay as the raw server
 * strings inside the json columns so a missing check date can never turn into a made-up one.
 */

@Entity(tableName = "cache_region")
data class RegionEntity(
    @PrimaryKey val code: String,
    val name: String,
    /** Contract value of `region_status`: available, upcoming or not_open. */
    val status: String,
    val sortOrder: Int,
)

@Entity(tableName = "cache_place")
data class PlaceEntity(
    @PrimaryKey val id: String,
    val slug: String,
    val name: String,
    val category: String,
    val capCode: String,
    val lat: Double,
    val lng: Double,
    val address: String,
    val description: String,
    val coverKey: String,
    val passportOrder: Int?,
)

/** One official fact row of a place. [kind] is `hours`, `closure` or `ticket`; [json] is the server row. */
@Entity(tableName = "cache_place_fact", indices = [Index("placeId")])
data class PlaceFactEntity(
    @PrimaryKey val rowKey: String,
    val placeId: String,
    val kind: String,
    val json: String,
)

@Entity(tableName = "cache_event")
data class EventEntity(
    @PrimaryKey val id: String,
    val json: String,
)

/** A published closure, kept next to events so cancelled dates can be removed from the agenda offline. */
@Entity(tableName = "cache_event_closure", indices = [Index("placeId")])
data class EventClosureEntity(
    @PrimaryKey val rowKey: String,
    val placeId: String,
    val json: String,
)
