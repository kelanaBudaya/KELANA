package id.kelana.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import id.kelana.core.database.dao.EventDao
import id.kelana.core.database.dao.OwnStampDao
import id.kelana.core.database.dao.PendingStampDao
import id.kelana.core.database.dao.PlaceDao
import id.kelana.core.database.dao.RegionDao
import id.kelana.core.database.entity.EventClosureEntity
import id.kelana.core.database.entity.EventEntity
import id.kelana.core.database.entity.OwnStampEntity
import id.kelana.core.database.entity.PendingStampEntity
import id.kelana.core.database.entity.PlaceEntity
import id.kelana.core.database.entity.PlaceFactEntity
import id.kelana.core.database.entity.RegionEntity

/** Version 2 adds the public-content cache, version 3 the pending stamp row and the own-stamp cache. Dropped on upgrade (a pending fix lives 30 minutes at most). */
@Database(
    entities = [
        PlaceholderEntity::class,
        RegionEntity::class,
        PlaceEntity::class,
        PlaceFactEntity::class,
        EventEntity::class,
        EventClosureEntity::class,
        PendingStampEntity::class,
        OwnStampEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class KelanaDatabase : RoomDatabase() {
    abstract fun placeholderDao(): PlaceholderDao
    abstract fun regionDao(): RegionDao
    abstract fun placeDao(): PlaceDao
    abstract fun eventDao(): EventDao
    abstract fun pendingStampDao(): PendingStampDao
    abstract fun ownStampDao(): OwnStampDao
}
