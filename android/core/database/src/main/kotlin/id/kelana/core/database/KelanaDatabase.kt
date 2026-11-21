package id.kelana.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PlaceholderEntity::class], version = 1, exportSchema = false)
abstract class KelanaDatabase : RoomDatabase() {
    abstract fun placeholderDao(): PlaceholderDao
}
