package id.kelana.core.database

import androidx.room.Dao
import androidx.room.Query

// PLACEHOLDER until the data contract lands
@Dao
interface PlaceholderDao {
    @Query("SELECT * FROM placeholder")
    suspend fun getAll(): List<PlaceholderEntity>
}
