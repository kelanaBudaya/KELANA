package id.kelana.feature.akun.ui

import androidx.annotation.StringRes
import androidx.test.platform.app.InstrumentationRegistry

/** Reads a string from the app resources in the device locale, so tests do not hardcode copy. */
internal fun str(@StringRes id: Int, vararg args: Any): String =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)
