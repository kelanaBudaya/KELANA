package id.kelana.feature.onboarding

import androidx.annotation.StringRes
import id.kelana.core.model.Region
import id.kelana.core.model.RegionStatus

/**
 * Page numbering of `IntroRoute(page)`: 0 is Mulai (Figma 01.01), 1..3 are Kenalan 1..3 (01.02 to 01.04).
 * PROVISIONAL: the route's own KDoc says 0..2; the Mulai frame needs its own page, so pages 0..3 are used.
 * Only this module creates `IntroRoute`s after the first one, so no shared file changes.
 */
internal const val MulaiPage = 0
internal const val LastIntroPage = 3

/** The three Kenalan pages. [step] is 0-based (indicator position). */
internal enum class IntroPage(
    val page: Int,
    @StringRes val title: Int,
    @StringRes val body: Int,
) {
    OfficialInfo(1, R.string.intro1_title, R.string.intro1_body),
    ShowSchedule(2, R.string.intro2_title, R.string.intro2_body),
    Stamp(3, R.string.intro3_title, R.string.intro3_body),
    ;

    val step: Int get() = page - 1
    val isLast: Boolean get() = page == LastIntroPage

    companion object {
        fun of(page: Int): IntroPage? = entries.firstOrNull { it.page == page }
    }
}

/** What the intro screen shows for a route page. */
internal data class IntroUiState(
    val page: Int,
    val intro: IntroPage?,
    val stepCount: Int = IntroPage.entries.size,
) {
    val isMulai: Boolean get() = page == MulaiPage

    /** Route page of the "Lanjut" button. The last intro has no next intro. */
    val nextPage: Int? get() = if (intro == null || intro.isLast) null else intro.page + 1

    companion object {
        fun forPage(page: Int) = IntroUiState(page = page, intro = IntroPage.of(page))
    }
}

/** Pin position on the illustrated map, 0..1 of width and height. PROVISIONAL: Region has no coordinates yet. */
internal data class PinPosition(val x: Float, val y: Float)

internal data class RegionPin(
    val region: Region,
    val position: PinPosition,
    val selected: Boolean,
)

/** Pilih daerah screen state. */
internal data class PilihDaerahUiState(
    val loading: Boolean = true,
    val regions: List<Region> = emptyList(),
    val selectedRegionId: String? = null,
    /** Names of the places shown in the "Sudah bisa dijelajahi" card, first four. */
    val placeNames: List<String> = emptyList(),
    val placeCount: Int = 0,
    /** Set once an available region was chosen; the screen then calls `onFinished`. */
    val finished: Boolean = false,
) {
    val selectedRegion: Region? get() = regions.firstOrNull { it.id == selectedRegionId }
    val availableRegions: List<Region> get() = regions.filter { it.status == RegionStatus.Available }
    val upcomingCount: Int get() = regions.count { it.status == RegionStatus.Upcoming }
    val notOpenCount: Int get() = regions.count { it.status == RegionStatus.NotOpen }
    val moreCount: Int get() = (placeCount - placeNames.size).coerceAtLeast(0)
}
