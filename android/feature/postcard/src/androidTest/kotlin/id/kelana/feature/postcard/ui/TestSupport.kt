package id.kelana.feature.postcard.ui

import androidx.annotation.StringRes
import androidx.test.platform.app.InstrumentationRegistry
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory

/** Reads a string from the app resources in the device locale, so tests do not hardcode copy. */
internal fun str(@StringRes id: Int, vararg args: Any): String =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

internal val TestPlace = Place(
    id = "museum-sonobudoyo",
    slug = "museum-sonobudoyo",
    name = "Museum Sonobudoyo",
    category = PlaceCategory.Museum,
    capCode = "YK-02",
    lat = 0.0,
    lng = 0.0,
    address = "",
    description = "",
    coverKey = "museum",
)
