package id.kelana.feature.paspor.ui

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.test.platform.app.InstrumentationRegistry
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory

/** Reads a string from the app resources in the device locale, so tests do not hardcode copy. */
internal fun str(@StringRes id: Int, vararg args: Any): String =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

internal fun plural(@PluralsRes id: Int, quantity: Int, vararg args: Any): String =
    InstrumentationRegistry.getInstrumentation().targetContext.resources.getQuantityString(id, quantity, *args)

internal fun testPlace(
    id: String,
    name: String,
    category: PlaceCategory = PlaceCategory.Museum,
    code: String = "YK-02",
) = Place(
    id = id,
    slug = id,
    name = name,
    category = category,
    capCode = code,
    lat = 0.0,
    lng = 0.0,
    address = "",
    description = "",
    coverKey = id,
)
