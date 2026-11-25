package id.kelana.core.data.account

import id.kelana.core.data.stamp.StampSendScheduler
import id.kelana.core.database.dao.OwnStampDao
import id.kelana.core.database.dao.PendingStampDao
import id.kelana.core.model.AccountDeletionRepository
import id.kelana.core.model.AccountDeletionResult
import id.kelana.core.model.AccountDeletionSummary
import id.kelana.core.model.PlanCache
import id.kelana.core.model.SessionRepository
import id.kelana.core.model.SessionState
import id.kelana.core.model.UserDataCleaner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Hapus akun" on top of the `delete-account` Edge Function. The server deletes the JWT's user; this class only
 * asks, and after a confirmed deletion clears what the device keeps for that user:
 * - the pending stamp request (Room `pending_stamp`, and its WorkManager send job),
 * - the passport cache (Room `own_stamp` rows of that user),
 * - today's plan ([PlanCache]),
 * - every feature's own data through its [UserDataCleaner] (notification centre, reminders, ...),
 * - the Supabase session (last, so the app sees a guest only once the data is gone).
 * On any other outcome nothing local is touched.
 */
@Singleton
class SupabaseAccountDeletionRepository @Inject constructor(
    private val session: SessionRepository,
    private val remote: AccountRemote,
    private val pendingDao: PendingStampDao,
    private val ownStampDao: OwnStampDao,
    private val scheduler: StampSendScheduler,
    private val planCache: PlanCache,
    private val cleaners: Set<@JvmSuppressWildcards UserDataCleaner>,
) : AccountDeletionRepository {

    override suspend fun summary(): AccountDeletionSummary {
        val userId = signedInUserId() ?: return AccountDeletionSummary()
        if (!remote.isConfigured) return AccountDeletionSummary()
        return coroutineScope {
            val stamps = async { remote.countOwn(OwnTable.Stamps, userId) }
            val postcards = async { remote.countOwn(OwnTable.Postcards, userId) }
            val videos = async { remote.countOwn(OwnTable.Videos, userId) }
            AccountDeletionSummary(stamps.await(), postcards.await(), videos.await())
        }
    }

    override suspend fun deleteAccount(): AccountDeletionResult {
        val userId = signedInUserId() ?: return AccountDeletionResult.NotSignedIn
        if (!remote.isConfigured) return AccountDeletionResult.NotConfigured
        // Not cancellable: once the request is out, leaving the screen must not stop the local clean-up after a
        // deletion the server already did.
        return withContext(NonCancellable) {
            when (remote.deleteAccount()) {
                DeleteAccountOutcome.Deleted -> {
                    clearLocal(userId)
                    AccountDeletionResult.Deleted
                }
                DeleteAccountOutcome.Unreachable -> AccountDeletionResult.NetworkError
                DeleteAccountOutcome.Refused -> AccountDeletionResult.Failed
            }
        }
    }

    private suspend fun signedInUserId(): String? =
        (session.sessionState.first { it !is SessionState.Loading } as? SessionState.SignedIn)?.user?.id

    /** Every step runs even if an earlier one fails: the account is already gone on the server. */
    private suspend fun clearLocal(userId: String) {
        attempt { scheduler.cancel() }
        attempt { pendingDao.clear() }
        attempt { ownStampDao.replaceForUser(userId, emptyList()) }
        attempt { planCache.clear() }
        for (cleaner in cleaners) attempt { cleaner.clear() }
        remote.clearLocalSession()
    }

    private inline fun attempt(block: () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Keep going; see clearLocal.
        }
    }
}
