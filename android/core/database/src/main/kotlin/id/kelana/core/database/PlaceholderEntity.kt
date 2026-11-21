package id.kelana.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// PLACEHOLDER until the data contract lands
@Entity(tableName = "placeholder")
data class PlaceholderEntity(
    @PrimaryKey val id: String,
)
