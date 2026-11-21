package id.kelana.feature.jelajah

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import id.kelana.core.designsystem.component.PlaceCard
import id.kelana.core.designsystem.component.PlaceCardSize
import id.kelana.core.designsystem.component.VerifiedBadge
import id.kelana.core.designsystem.theme.KelanaColors
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.core.navigation.MasukRoute
import id.kelana.core.ui.session.LoginAction
import id.kelana.core.ui.session.SessionViewModel
import id.kelana.core.ui.session.decideLoginAction

/** Soft background per category. PROVISIONAL: colours picked from the Figma tokens, not read per illustration. */
internal fun PlaceCategory.tint(colors: KelanaColors): Color = when (this) {
    PlaceCategory.Museum -> colors.skyText
    PlaceCategory.Candi -> colors.creamYellow
    PlaceCategory.Tari -> colors.creamYellowAlt
    PlaceCategory.Topeng -> colors.backgroundWarm
    PlaceCategory.Gamelan -> colors.creamYellow
    PlaceCategory.Kerajinan -> colors.successSurface
    PlaceCategory.Lainnya -> colors.backgroundWarm
}

/**
 * PLACEHOLDER cover: the illustrated covers (assets/illustrations/cover) are not yet vector drawables in
 * :core:designsystem, so this draws a tinted tile with a soft circle. Replace when the drawables exist.
 */
@Composable
internal fun PlaceCover(category: PlaceCategory, modifier: Modifier = Modifier) {
    val colors = KelanaTheme.colors
    Box(modifier.fillMaxSize().background(category.tint(colors)), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize(0.55f)
                .clip(CircleShape)
                .background(colors.white.copy(alpha = 0.6f)),
        )
    }
}

/** "Diperiksa <tanggal>" when any hour fact was checked, otherwise "Sedang dicek". Never a value without a date. */
@Composable
internal fun PlaceCheckedBadge(place: Place) {
    val checkedAt = place.latestCheckedAt()
    if (checkedAt != null) {
        VerifiedBadge(checkedAt = checkedAt)
    } else {
        Text(
            text = stringResource(R.string.badge_checking),
            style = MaterialTheme.typography.bodySmall,
            color = KelanaTheme.colors.warnText,
        )
    }
}

@Composable
internal fun PlaceItem(
    place: Place,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: PlaceCardSize = PlaceCardSize.List,
) {
    PlaceCard(
        title = place.name,
        subtitle = place.description,
        onClick = onClick,
        modifier = modifier,
        size = size,
        badge = { PlaceCheckedBadge(place) },
        cover = { PlaceCover(place.category) },
    )
}

/**
 * Login hook for actions that need an account. [androidx.navigation.NavController] is not handed to features by
 * `jelajahScreens`, so this uses the same session decision as `rememberRequireLogin` from :core:ui and sends a
 * guest to Masuk through [navigate] (route object only, no :feature:akun import).
 */
@Composable
internal fun rememberRequireLoginVia(
    navigate: (Any) -> Unit,
    viewModel: SessionViewModel = hiltViewModel(),
): (action: () -> Unit) -> Unit {
    val state by viewModel.sessionState.collectAsState()
    val latestState = rememberUpdatedState(state)
    val latestNavigate = rememberUpdatedState(navigate)
    return remember {
        { action ->
            when (decideLoginAction(latestState.value)) {
                LoginAction.Run -> action()
                LoginAction.GoToLogin -> latestNavigate.value(MasukRoute)
                LoginAction.Wait -> Unit
            }
        }
    }
}
