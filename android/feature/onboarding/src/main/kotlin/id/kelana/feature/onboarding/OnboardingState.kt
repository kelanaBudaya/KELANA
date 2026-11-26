package id.kelana.feature.onboarding

import androidx.annotation.StringRes
import id.kelana.core.model.Region
import id.kelana.core.model.RegionStatus

/**
 * Page numbering of `IntroRoute(page)`: 0 is Mulai, 4 is Pilih bahasa (Figma 01.01), 1..3 are Kenalan 1..3 (01.02 to 01.04).
 * PROVISIONAL: the route's own KDoc says 0..2; the Mulai frame needs its own page, so pages 0..3 are used.
 * Only this module creates `IntroRoute`s after the first one, so no shared file changes.
 */
internal const val MulaiPage = 0
internal const val LastIntroPage = 3

/**
 * Pilih bahasa step, shown after Mulai and before Kenalan 1. It has its own page id instead of renumbering Kenalan,
 * so Kenalan keeps pages 1..3 (and its indicator 1..3). The order comes from navigation: Mulai, 4, then 1.
 */
internal const val LanguagePage = 4

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
    val isLanguage: Boolean get() = page == LanguagePage

    /** Route page of the "Lanjut" button. The last intro has no next intro. */
    val nextPage: Int? get() = if (intro == null || intro.isLast) null else intro.page + 1

    companion object {
        fun forPage(page: Int) = IntroUiState(page = page, intro = IntroPage.of(page))
    }
}

/**
 * Pin position on the illustrated map, in the artifact's map units (the 350 x 210 map drawing, tip of the pin).
 * [left] and [below] say where the zoomed label goes (PilihDaerah.dc.html region list).
 * PROVISIONAL: Region has no coordinates yet, so positions are keyed by region code in this module.
 */
internal data class PinPosition(val x: Float, val y: Float, val left: Boolean = false, val below: Boolean = false)

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
    /** Places of each available region by region id. Regions that are not open have no entry. */
    val placesByRegion: Map<String, RegionPlaces> = emptyMap(),
    /** Set once an available region was chosen; the screen then calls `onFinished`. */
    val finished: Boolean = false,
) {
    val selectedRegion: Region? get() = regions.firstOrNull { it.id == selectedRegionId }
    val availableRegions: List<Region> get() = regions.filter { it.status == RegionStatus.Available }
    val upcomingCount: Int get() = regions.count { it.status == RegionStatus.Upcoming }
    val notOpenCount: Int get() = regions.count { it.status == RegionStatus.NotOpen }

    /** Places of [regionId]; empty (no names, zero count) for a region without data. */
    fun placesOf(regionId: String): RegionPlaces = placesByRegion[regionId] ?: RegionPlaces()
}

/** Highlight names shown per region card. */
internal const val HighlightCount = 4

/** Published places of one region: [count] is the real total, [names] the first [HighlightCount]. */
internal data class RegionPlaces(val names: List<String> = emptyList(), val count: Int = 0) {
    val moreCount: Int get() = (count - names.size).coerceAtLeast(0)
}
