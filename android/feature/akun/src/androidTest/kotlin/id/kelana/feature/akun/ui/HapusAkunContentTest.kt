package id.kelana.feature.akun.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.ACCOUNT_DELETION_CONFIRM_WORD
import id.kelana.feature.akun.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HapusAkunContentTest {
    @get:Rule val rule = createComposeRule()

    private var deletes = 0
    private var cancels = 0

    /** Holds the typed text like the ViewModel does, so the button reacts to real input. */
    private fun show(initial: HapusAkunUiState = HapusAkunUiState()) {
        rule.setContent {
            KelanaTheme {
                var state by remember { mutableStateOf(initial) }
                HapusAkunContent(
                    state = state,
                    onConfirmationChange = { state = state.copy(confirmation = it) },
                    onDelete = { deletes++ },
                    onCancel = { cancels++ },
                )
            }
        }
    }

    @Test
    fun showsTitleBodyAndConfirmLabel() {
        show()
        rule.onNodeWithText(str(R.string.hapus_akun_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.hapus_akun_body)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.hapus_akun_confirm_label, ACCOUNT_DELETION_CONFIRM_WORD)).assertIsDisplayed()
    }

    @Test
    fun deleteButton_disabledUntilExactWord() {
        show()
        val delete = rule.onNodeWithText(str(R.string.hapus_akun_submit))
        delete.assertIsNotEnabled()
        rule.onNode(hasSetTextAction()).performTextInput("hapus")
        delete.assertIsNotEnabled()
    }

    @Test
    fun deleteButton_enabledAfterExactWord_andCallsBack() {
        show()
        rule.onNode(hasSetTextAction()).performTextInput(ACCOUNT_DELETION_CONFIRM_WORD)
        rule.onNodeWithText(str(R.string.hapus_akun_submit)).assertIsEnabled().performClick()
        assertEquals(1, deletes)
    }

    @Test
    fun batal_callsOnCancel_notOnDelete() {
        show()
        rule.onNodeWithText(str(R.string.hapus_akun_cancel)).performClick()
        assertEquals(1, cancels)
        assertEquals(0, deletes)
    }

    @Test
    fun deleting_disablesBothButtons() {
        show(HapusAkunUiState(confirmation = ACCOUNT_DELETION_CONFIRM_WORD, isDeleting = true))
        rule.onNodeWithText(str(R.string.hapus_akun_cancel)).assertIsNotEnabled()
        rule.onNodeWithText(str(R.string.hapus_akun_deleting)).assertIsNotEnabled()
    }

    @Test
    fun error_showsMessage() {
        show(HapusAkunUiState(error = HapusAkunError.Network))
        rule.onNodeWithText(str(R.string.hapus_akun_error_network)).assertIsDisplayed()
    }
}
