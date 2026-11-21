package id.kelana.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import id.kelana.core.database.dao.EventDao
import id.kelana.core.database.dao.PlaceDao
import id.kelana.core.database.dao.RegionDao
import id.kelana.core.database.entity.EventClosureEntity
import id.kelana.core.database.entity.EventEntity
import id.kelana.core.database.entity.PlaceEntity
import id.kelana.core.database.entity.PlaceFactEntity
import id.kelana.core.database.entity.RegionEntity

/** Version 2 adds the public-content cache. The cache is rebuilt from the server, so no migration is kept. */
@Database(
    entities = [
        PlaceholderEntity::class,
        RegionEntity::class,
        PlaceEntity::class,
        PlaceFactEntity::class,
        EventEntity::class,
        EventClosureEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class KelanaDatabase : RoomDatabase() {
    abstract fun placeholderDao(): PlaceholderDao
    abstract fun regionDao(): RegionDao
    abstract fun placeDao(): PlaceDao
    abstract fun eventDao(): EventDao
}
