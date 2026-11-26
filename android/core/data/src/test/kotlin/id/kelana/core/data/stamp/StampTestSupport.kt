package id.kelana.core.data.stamp

import id.kelana.core.database.dao.OwnStampDao
import id.kelana.core.database.dao.PendingStampDao
import id.kelana.core.database.entity.OwnStampEntity
import id.kelana.core.database.entity.PendingStampEntity
import id.kelana.core.model.AuthResult
import id.kelana.core.model.SessionRepository
import id.kelana.core.model.SessionState
import id.kelana.core.model.SessionUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class MutableClock(var now: Instant) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId?): Clock = this
    override fun instant(): Instant = now
}

class FakePendingDao : PendingStampDao() {
    val row = MutableStateFlow<PendingStampEntity?>(null)
    override fun observe(): Flow<PendingStampEntity?> = row
    override suspend fun get(): PendingStampEntity? = row.value
    override suspend fun put(row: PendingStampEntity) { this.row.value = row }
    override suspend fun clear() { row.value = null }
}

class FakeOwnStampDao : OwnStampDao() {
    val rows = MutableStateFlow<List<OwnStampEntity>>(emptyList())
    override fun observe(userId: String): Flow<List<OwnStampEntity>> =
        rows.map { list -> list.filter { it.userId == userId } }
    override suspend fun clear(userId: String) { rows.value = rows.value.filterNot { it.userId == userId } }
    override suspend fun insertAll(rows: List<OwnStampEntity>) { this.rows.value = this.rows.value + rows }
    override suspend fun insertIfAbsent(row: OwnStampEntity) {
        if (rows.value.none { it.userId == row.userId && it.placeId == row.placeId }) rows.value = rows.value + row
    }
    override suspend fun replaceForUser(userId: String, rows: List<OwnStampEntity>) {
        this.rows.value = this.rows.value.filterNot { it.userId == userId } + rows
    }
}

class FakeStampRemote : StampRemote {
    override var isConfigured: Boolean = true
    var serverStamps: List<StampDto> = emptyList()
    var fetchFails = false
    var outcome: VerifyOutcome = VerifyOutcome.Answer("stamped", null, null)
    val sent = mutableListOf<VerifyStampBody>()

    override suspend fun fetchOwnStamps(): List<StampDto> {
        if (fetchFails) throw IOException("offline")
        return serverStamps
    }

    override suspend fun verify(body: VerifyStampBody): VerifyOutcome {
        sent += body
        return outcome
    }
}

class FakeIntegrity(var result: IntegrityTokenResult = IntegrityTokenResult.Token("fake-token")) :
    IntegrityTokenProvider {
    val nonces = mutableListOf<String>()
    override suspend fun requestToken(nonce: String): IntegrityTokenResult {
        nonces += nonce
        return result
    }
}

class FakeSession(initial: SessionState) : SessionRepository {
    val state = MutableStateFlow(initial)
    override val sessionState: Flow<SessionState> = state
    override suspend fun signInWithGoogle(idToken: String, nonce: String): AuthResult = AuthResult.Success
    override suspend fun updateDisplayName(name: String): AuthResult = AuthResult.Success
    override suspend fun signOut() { state.value = SessionState.Guest }
}

class FakeScheduler : StampSendScheduler {
    var scheduled = 0
    var cancelled = 0
    override fun schedule() { scheduled++ }
    override fun cancel() { cancelled++ }
}

class FakeNetwork(var online: Boolean = true) : NetworkMonitor {
    override fun isOnline(): Boolean = online
}

fun signedIn(id: String = "user-1") = SessionState.SignedIn(SessionUser(id, null, null))
