package id.kelana.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import id.kelana.core.database.entity.OwnStampEntity
import id.kelana.core.database.entity.PendingStampEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PendingStampDao {
    @Query("SELECT * FROM pending_stamp WHERE id = 1")
    abstract fun observe(): Flow<PendingStampEntity?>

    @Query("SELECT * FROM pending_stamp WHERE id = 1")
    abstract suspend fun get(): PendingStampEntity?

    /** Replaces any earlier pending request: there is at most one. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun put(row: PendingStampEntity)

    @Query("DELETE FROM pending_stamp")
    abstract suspend fun clear()
}

@Dao
abstract class OwnStampDao {
    @Query("SELECT * FROM own_stamp WHERE userId = :userId ORDER BY stampedAtMillis")
    abstract fun observe(userId: String): Flow<List<OwnStampEntity>>

    @Query("DELETE FROM own_stamp WHERE userId = :userId")
    protected abstract suspend fun clear(userId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAll(rows: List<OwnStampEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertIfAbsent(row: OwnStampEntity)

    /** Replaces the cache of [userId] with the server's list in one transaction. */
    @Transaction
    open suspend fun replaceForUser(userId: String, rows: List<OwnStampEntity>) {
        clear(userId)
        insertAll(rows)
    }
}
