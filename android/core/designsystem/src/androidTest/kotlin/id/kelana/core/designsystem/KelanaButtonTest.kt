package id.kelana.core.designsystem

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.kelana.core.designsystem.component.KelanaButton
import id.kelana.core.designsystem.theme.KelanaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Smoke test for the Compose UI test setup: proves androidTest compiles and runs on a device. */
@RunWith(AndroidJUnit4::class)
class KelanaButtonTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun click_callsOnClick() {
        var clicks = 0
        rule.setContent { KelanaTheme { KelanaButton(text = "Buka situs", onClick = { clicks++ }) } }
        rule.onNodeWithText("Buka situs").assertHasClickAction().performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun disabled_isNotEnabled() {
        rule.setContent { KelanaTheme { KelanaButton(text = "Hapus akun", onClick = {}, enabled = false) } }
        rule.onNodeWithText("Hapus akun").assertIsNotEnabled()
    }
}
