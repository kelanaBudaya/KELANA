package id.kelana.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import id.kelana.core.database.entity.EventClosureEntity
import id.kelana.core.database.entity.EventEntity
import id.kelana.core.database.entity.PlaceEntity
import id.kelana.core.database.entity.PlaceFactEntity
import id.kelana.core.database.entity.RegionEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RegionDao {
    @Query("SELECT * FROM cache_region ORDER BY sortOrder, name")
    abstract fun observeAll(): Flow<List<RegionEntity>>

    @Query("SELECT * FROM cache_region ORDER BY sortOrder, name")
    abstract suspend fun getAll(): List<RegionEntity>

    @Query("DELETE FROM cache_region")
    protected abstract suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAll(rows: List<RegionEntity>)

    /** Replaces the whole cache in one transaction. */
    @Transaction
    open suspend fun replaceAll(rows: List<RegionEntity>) {
        clear()
        insertAll(rows)
    }
}

@Dao
abstract class PlaceDao {
    @Query("SELECT * FROM cache_place ORDER BY passportOrder IS NULL, passportOrder, name")
    abstract fun observePlaces(): Flow<List<PlaceEntity>>

    @Query("SELECT * FROM cache_place_fact")
    abstract fun observeFacts(): Flow<List<PlaceFactEntity>>

    @Query("DELETE FROM cache_place")
    protected abstract suspend fun clearPlaces()

    @Query("DELETE FROM cache_place_fact")
    protected abstract suspend fun clearFacts()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertPlaces(rows: List<PlaceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertFacts(rows: List<PlaceFactEntity>)

    /** Replaces places and their facts together, so a place never shows facts from another refresh. */
    @Transaction
    open suspend fun replaceAll(places: List<PlaceEntity>, facts: List<PlaceFactEntity>) {
        clearPlaces()
        clearFacts()
        insertPlaces(places)
        insertFacts(facts)
    }
}

@Dao
abstract class EventDao {
    @Query("SELECT * FROM cache_event")
    abstract fun observeEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM cache_event_closure")
    abstract fun observeClosures(): Flow<List<EventClosureEntity>>

    @Query("DELETE FROM cache_event")
    protected abstract suspend fun clearEvents()

    @Query("DELETE FROM cache_event_closure")
    protected abstract suspend fun clearClosures()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertEvents(rows: List<EventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertClosures(rows: List<EventClosureEntity>)

    @Transaction
    open suspend fun replaceAll(events: List<EventEntity>, closures: List<EventClosureEntity>) {
        clearEvents()
        clearClosures()
        insertEvents(events)
        insertClosures(closures)
    }
}
