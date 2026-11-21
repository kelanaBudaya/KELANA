package id.kelana.core.ui.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import id.kelana.core.model.SessionState
import id.kelana.core.navigation.MasukRoute

/** What to do when a user triggers an action that needs an account. */
enum class LoginAction {
    /** Signed in: run the action. */
    Run,

    /** Guest: open Masuk. */
    GoToLogin,

    /** Session still loading: do nothing yet. */
    Wait,
}

fun decideLoginAction(state: SessionState): LoginAction = when (state) {
    is SessionState.SignedIn -> LoginAction.Run
    SessionState.Guest -> LoginAction.GoToLogin
    SessionState.Loading -> LoginAction.Wait
}

/**
 * Hook for features that need an account (Ambil cap, Postcard, Laporkan, ...):
 *
 * ```
 * val requireLogin = rememberRequireLogin(navController)
 * Button(onClick = { requireLogin { startStamp() } })
 * ```
 *
 * A guest is sent to Masuk by route object, so no feature imports :feature:akun.
 */
@Composable
fun rememberRequireLogin(
    navController: NavController,
    viewModel: SessionViewModel = hiltViewModel(),
): (action: () -> Unit) -> Unit {
    val state by viewModel.sessionState.collectAsState()
    val latest = rememberUpdatedState(state)
    return remember(navController) {
        { action ->
            when (decideLoginAction(latest.value)) {
                LoginAction.Run -> action()
                LoginAction.GoToLogin -> navController.navigate(MasukRoute)
                LoginAction.Wait -> Unit
            }
        }
    }
}
