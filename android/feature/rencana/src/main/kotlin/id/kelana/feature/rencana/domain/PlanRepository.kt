package id.kelana.feature.rencana.domain

import id.kelana.core.model.PlanChoices
import id.kelana.core.model.PlanStop
import id.kelana.core.model.ReminderChoice
import id.kelana.core.model.TripPlan
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Result of building a plan from the wizard answers. */
sealed interface BuildResult {
    data class Built(val trip: TripPlan) : BuildResult

    /** There is no place data on the device (and none arrived in time), so nothing can be planned. The cached plan is kept. */
    data object NoData : BuildResult

    /** The builder threw unexpectedly. The cached plan is kept. */
    data object Failed : BuildResult

    /** Official facts leave no place or show that fits the answers. The cached plan is left untouched. */
    data object NothingFits : BuildResult
}

/** Result of applying a live re-plan. */
sealed interface ReplanOutcome {
    /** [replacement] is the new stop that took the place of the closed one, or null when the stop was only removed. */
    data class Applied(val trip: TripPlan, val replacement: PlanStop?) : ReplanOutcome

    /** The re-plan threw unexpectedly. The cached plan is kept. */
    data object Failed : ReplanOutcome

    /** The plan or the stop is no longer there. */
    data object NotFound : ReplanOutcome
}

/**
 * Source of plans. The implementation today ([id.kelana.feature.rencana.data.LocalPlanRepository]) runs
 * [PlanBuilder] on the device; a server planner (Edge Function) replaces it behind this interface later.
 *
 * Offline rule (AGENTS.md): everything works offline from the on-device cache ([trip], [closedStops], [build],
 * [applyReplan]). Without any cached place data [build] answers `NoData`; unexpected errors answer `Failed`.
 */
interface PlanRepository {
    /** The last plan, kept for offline reading. Null before the first plan. */
    val trip: Flow<TripPlan?>

    suspend fun build(choices: PlanChoices): BuildResult

    /** Stops of the plan of [date] that current official facts say are closed. Reads cached data only. */
    suspend fun closedStops(date: LocalDate): List<ClosedStop>

    /** The replacement the builder would offer for [closedPlaceId] on [date], without changing anything. */
    suspend fun previewReplacement(date: LocalDate, closedPlaceId: String): PlanStop?

    /** Replace the closed place with another one ([replace] true) or just take it out of the plan. */
    suspend fun applyReplan(date: LocalDate, closedPlaceId: String, replace: Boolean): ReplanOutcome

    /** Reminder choice per event id. */
    val reminders: Flow<Map<String, ReminderChoice>>

    suspend fun saveReminder(eventId: String, choice: ReminderChoice)
}
