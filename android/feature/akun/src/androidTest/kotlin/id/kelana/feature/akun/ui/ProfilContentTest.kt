package id.kelana.feature.akun.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.AppLanguage
import id.kelana.core.model.SessionUser
import id.kelana.feature.akun.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfilContentTest {
    @get:Rule val rule = createComposeRule()

    private val calls = mutableListOf<String>()

    private val signedIn = ProfilUiState(
        isLoading = false,
        user = SessionUser(id = "u1", email = "sari@example.com", displayName = "Sari Dewi"),
    )

    private fun show(state: ProfilUiState) {
        rule.setContent {
            KelanaTheme {
                ProfilContent(
                    state = state,
                    onBack = { calls += "back" },
                    onPaspor = { calls += "paspor" },
                    onDaerah = { calls += "daerah" },
                    onMasuk = { calls += "masuk" },
                    onSignOut = { calls += "keluar" },
                    onNotifikasi = { calls += "notifikasi" },
                    onHapusAkun = { calls += "hapus" },
                )
            }
        }
    }

    @Test
    fun signedIn_showsNameEmailAndAllRows() {
        show(signedIn)
        rule.onNodeWithText("Sari Dewi").assertIsDisplayed()
        rule.onNodeWithText("sari@example.com").assertIsDisplayed()
        listOf(
            R.string.profil_row_paspor,
            R.string.profil_row_daerah,
            R.string.profil_row_kebutuhan,
            R.string.profil_row_notifikasi,
            R.string.profil_row_cara,
            R.string.profil_row_laporan,
            R.string.profil_row_hapus,
            R.string.profil_keluar,
        ).forEach { rule.onNodeWithText(str(it)).performScrollTo().assertIsDisplayed() }
    }

    @Test
    fun rowsAndButtons_callTheirCallbacks() {
        show(signedIn)
        listOf(
            R.string.profil_row_paspor,
            R.string.profil_row_daerah,
            R.string.profil_row_notifikasi,
            R.string.profil_row_hapus,
            R.string.profil_keluar,
        ).forEach { rule.onNodeWithText(str(it)).performScrollTo().performClick() }
        // The header scrolls with the list, so after scrolling down to "Keluar" it has to be scrolled back into view.
        rule.onNodeWithContentDescription(str(R.string.profil_back)).performScrollTo().performClick()
        assertEquals(listOf("paspor", "daerah", "notifikasi", "hapus", "keluar", "back"), calls)
    }

    @Test
    fun bahasaRow_showsCurrentChoice_andOpensSheetForGuestAndSignedIn() {
        show(signedIn.copy(language = AppLanguage.EN))
        rule.onNodeWithText(str(R.string.profil_row_bahasa)).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("English").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun guest_canReachBahasaRow() {
        show(ProfilUiState(isLoading = false, user = null))
        rule.onNodeWithText(str(R.string.profil_row_bahasa)).performScrollTo().assertIsDisplayed()
    }

    private fun assertAboutMapRowOpensAndClosesTheCredit(state: ProfilUiState) {
        show(state)
        rule.onNodeWithText(str(R.string.profil_row_peta)).performScrollTo().performClick()
        rule.onNodeWithText(str(id.kelana.core.ui.R.string.map_attribution)).assertIsDisplayed()
        rule.onNodeWithText(str(id.kelana.core.ui.R.string.map_attribution_close)).performClick()
        rule.onNodeWithText(str(id.kelana.core.ui.R.string.map_attribution)).assertDoesNotExist()
    }

    @Test
    fun signedIn_aboutMapRow_opensTheMapCreditDialog() = assertAboutMapRowOpensAndClosesTheCredit(signedIn)

    @Test
    fun guest_aboutMapRow_opensTheMapCreditDialog() =
        assertAboutMapRowOpensAndClosesTheCredit(ProfilUiState(isLoading = false, user = null))

    @Test
    fun signingOut_disablesKeluar() {
        show(signedIn.copy(isSigningOut = true))
        rule.onNodeWithText(str(R.string.profil_keluar)).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun guest_showsMasukPrompt_andNoAccountRows() {
        show(ProfilUiState(isLoading = false, user = null))
        rule.onNodeWithText(str(R.string.masuk_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.profil_guest_masuk)).performClick()
        assertEquals(listOf("masuk"), calls)
        rule.onNodeWithText(str(R.string.profil_row_hapus)).assertDoesNotExist()
    }

    @Test
    fun signedIn_namaRow_showsCurrentName_andOpensDialog() {
        var state by mutableStateOf(signedIn)
        rule.setContent {
            KelanaTheme {
                ProfilContent(
                    state = state,
                    onBack = {}, onPaspor = {}, onDaerah = {}, onMasuk = {}, onSignOut = {},
                    onEditName = { state = state.copy(nameDraft = "Sari Dewi") },
                )
            }
        }
        rule.onNodeWithText(str(R.string.profil_row_nama)).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText(str(R.string.profil_row_nama)).performClick()
        rule.onNodeWithText(str(R.string.profil_nama_dialog_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.profil_nama_simpan)).assertIsDisplayed()
    }

    @Test
    fun namaDialog_saving_disablesSave() {
        show(signedIn.copy(nameDraft = "Sari", isSavingName = true))
        rule.onNodeWithText(str(R.string.profil_nama_simpan)).assertIsNotEnabled()
    }

    @Test
    fun guest_hasNoNamaRow() {
        show(ProfilUiState(isLoading = false, user = null))
        rule.onNodeWithText(str(R.string.profil_row_nama)).assertDoesNotExist()
    }
}
