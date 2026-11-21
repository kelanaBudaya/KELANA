package id.kelana.core.model

import kotlinx.coroutines.flow.Flow

/** Remembers whether the first-launch flow (Mulai, Kenalan, Pilih daerah) has been completed. */
interface OnboardingRepository {
    val completed: Flow<Boolean>

    suspend fun markCompleted()
}
