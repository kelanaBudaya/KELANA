package id.kelana.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 4 to 5 adds the place photo cache. A real migration (not the destructive fallback) because the same database
 * holds the pending stamp queue, which must survive an app update.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `cache_place_photo` (`id` TEXT NOT NULL, `placeId` TEXT NOT NULL, " +
                "`position` INTEGER NOT NULL, `url` TEXT NOT NULL, `thumbUrl` TEXT NOT NULL, `width` INTEGER NOT NULL, " +
                "`height` INTEGER NOT NULL, `caption` TEXT, `attributionText` TEXT NOT NULL, `licenseCode` TEXT NOT NULL, " +
                "`licenseUrl` TEXT NOT NULL, `sourcePageUrl` TEXT NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cache_place_photo_placeId` ON `cache_place_photo` (`placeId`)")
    }
}

/** 5 to 6 adds the nullable place stamp name; the cached place rows stay (they refresh on the next sync). */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `cache_place` ADD COLUMN `stampName` TEXT")
    }
}
